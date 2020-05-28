# HTTPS/SSL Server Using Netty


## 1. Description

This repo includes:
* notes about SSL/TLS and keystore best practices (those relevant for server implementation)
* a sample implementation of a Netty HTTPS/SSL server using keystore

The code is based on the [example code that comes with Netty](https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/http/helloworld).
This is just a personal project created for learning and testing.
If you have any question or comment, please use the Issues tab of this git repo.


## 2. Building, Running, Testing

Follow below steps to test the sample implementation:

### 1. Install the SSL certificate as a trusted root certificate in a browser.
This is necessary if you are using a self-signed root CA certificate (as that included in the project).
For e.g., to install the certificate in Mozilla Firefox:
- Click Preferences > Privacy & Security > View Certificates > Authorities tab > Import
- Navigate to where you stored the certificate, and select the certificate file (If you are using certificates included in the project, you should select `ca_netty.pem` in src/main/resources/security/* folder).`

### 2. Build the jar file.
"Fat-jar" file is created (via gradle [`shadowJar`](https://imperceptiblethoughts.com/shadow/) plugin) when the project is built.
Run the following command in the project root directory.
````bash
./gradle clean build
````
The "fat-jar" will be created as `build/libs//nettyHttps-<version>-all.jar`.

### 3. Start the HTTPS Server.
Update the application.properties file in the src/main/resources folder with the path of the Keystore configuration file, and the server port.

To start the HTTPS server run the below command.
````bash
java -jar nettyHttps-<version>-all.jar
````

### 4. Access the Server.
Open browser on page *https://localhost:8443* (or *https://127.0.0.1:8443*).

If everything is correct one should see a "Hello world!" message in your browser and a gray padlock (indicating a trustworthy SSL/TLS connection).
If something is wrong, the browser will display the "Warning: Potential Security Risk Ahead" error page with error codes (see [How do I tell if my connection to a website is secure?](https://support.mozilla.org/en-US/kb/how-do-i-tell-if-my-connection-is-secure)).
<br/><br/>

**Note:** : A production project __SHOULD NOT INCLUDE__ keystore or keystore configuration parameters.
Including them in Jar files/application archive files is against the SSL/TLS and keystore best practices.
The *src/main/resources/security* directory contains the keystore (*svr_netty.p12* - containing the server private key, and certificate), and Keystore configuration file (*security-config.properties*).
These files were included in the sample project to enable quick tests and experiments.

The keys, certificates, and keystore in *src/main/resources/security* directory were generated as described in section 3.4.


## 3. How to to Create a Netty HTTPS Server

### 3.1 SSL/TLS and Keystore Best Practices

One fundamental best practice for SSL/TLS is to keep the private keys as secure as possible (see for e.g. [Certificates and Security Best Practices](https://myarch.com/cert-book/index.html), and [SSL/TLS Best Practices](https://www.ssl.com/guide/ssl-best-practices/)).
HSM (hardware security module), secret manager, and keystore files can be used to secure the keys.

Keystore files are the de-facto “secrets repository” for Java/JVM-based applications (see [Certificates and Security Best Practices](https://myarch.com/cert-book/index.html)).
This repository addresses the implementation of a Netty HTTPS/SSL server using keystore.

Other relevant SSL/TLS and keystore best practices include:

- Do not package keystore files and keystore configuration files (including passwords) inside Jar files/application archive files (see: [Certificates and Security Best Practices](https://myarch.com/cert-book/index.html), and [How can I protect MySQL username and password from decompiling?](https://stackoverflow.com/questions/442862/how-can-i-protect-mysql-username-and-password-from-decompiling/442872#442872)).
- Use standard operating system access security protocols to protect keystore files and keystore configuration files (see: [Certificates and Security Best Practices](https://myarch.com/cert-book/index.html), and [Keystore best practices](https://www.ibm.com/support/knowledgecenter/en/SSEPGG_11.1.0/com.ibm.db2.luw.admin.sec.doc/doc/t0062034.html)).
- Use the PKCS12 Format for keystores (see: [Certificates and Security Best Practices](https://myarch.com/cert-book/index.html)).

The following subsections provide additional information necessary to implement a Netty HTTPS server compliant with the best practices.


### 3.2 General Structure of a Netty HTTPS Server

The following is required to create a Netty HTTPS server,

1. Keystore with server private key and certificate signed by CA.
2. Keystore configuration file containing the data needed to access the keystore data. The file should contain the keystore file path, the keystore password, entry alias (for private key and certificate pair), and private key password.
3. Logic to acquire the path of the keystore configuration file.
4. Logic to extract the private key and certificate from keystore, and keystore configuration file. The key and certificate are necessary to create the `io.netty.handler.ssl.SslContext` for the Netty server.

In the project, these correspond to:

1. `src/main/resources/security/svr_netty.p12` file.
2. `src/main/resources/security/config.properties` file.
3. the path is extracted from `src/main/resources/application.properties` using the `from` method in the `io.netty.https.util.ApplicationProperties` class.
4. the private key and certificate are extracted by the methods in `io.netty.https.App` and `io.netty.https.security.KeyStoreData` classes.
`io.netty.handler.ssl.SslContext` is created by the `getSslContext(KeyStoreData keyStoreData)` method of the `io.netty.https.App` class.


### 3.3 Creating and Managing the Keystore
The keystore file and keystore parameters configuration file should be external to the Java/Netty project.
Also, these files should be owned by and accessible only to the account used to run the server.
This can be achieved changing the file owners, and making them read-only:

```bash
chown account_id config.properties svr_netty.p12
chmod 400 config.properties svr_netty.p12
```

To obtain a CA signed SSL certificate, you must create a private key, use that key to create a CSR (certificate signing request), and submit the CSR to be signed by an authorized CA.
Follow the below steps to create CSR using the OpenSSL:

1. Create the server's private key:

    ```bash
    openssl genpkey -algorithm RSA -out svr_key.pem -pkeyopt rsa_keygen_bits:2048 -aes-128-cbc -pass pass:$SVR_KEY_PWD
    ```    
    **Note:** : I usually generate "strong passwords" using for example: `openssl rand -base64 12`

2. Create server CSR:

    ```bash
    openssl req -new -key svr_key.pem -extensions v3_ca -batch -out svr_netty.csr -utf8 -subj '/C=JP/CN=www.example.com' -passin pass:$SVR_KEY_PWD
    ```

After getting the signed certificate from the CA, it should be imported with the private key into keystore.
The key and certificate can be imported using the OpenSSL:
```bash
openssl pkcs12 -export -name netty -inkey svr_key.pem -in svr_netty.pem -out svr_netty.p12 -passin pass:$SVR_KEY_PWD -passout pass:$STORE_PWD
```

**Note:** : The keystore only worked for me when the key password (`$SVR_KEY_PWD`) was equal to keystore password (`STORE_PWD`). It seems that Java doesn't support different keystore and key passwords in PKCS12
(see: [Java PKCS12 Keystore generated with openssl BadPaddingException](https://stackoverflow.com/questions/32850783/java-pkcs12-keystore-generated-with-openssl-badpaddingexception)).


### 3.4 How to Create & Use Self-Signed Certificate Chain

The following is required for creating a self-signed certificate chain:

- Self-signed root CA certificate.  
- Server certificate signed with the self-signed root CA certificate.

The root CA certificate must be installed on the browser (as described in section 2.), and the server certificate should be installed in the server (as described in section 3.).

**Note:** : The below steps create certificates to be used in tests with *localhost*/*127.0.0.1* domain, and browser.
For other scenarios refer to [OpenSSL Manpages](https://www.openssl.org/docs/manpages.html), [RFC 5280](https://tools.ietf.org/html/rfc5280), or other online references
(e.g.: [A Web PKI x509 certificate primer](https://developer.mozilla.org/en-US/docs/Mozilla/Security/x509_Certificates) from  Mozilla).

Self-signed root/CA certificate can be created using OpenSSL commands by following the below steps:

1. Create the root CA private key:

    ```bash
    openssl genpkey -algorithm RSA -out ca_key.pem -pkeyopt rsa_keygen_bits:4096 -aes-128-cbc -pass pass:$CA_KEY_PWD
    ```

2. Create root CA CSR:

    ```bash
    openssl req -new -key ca_key.pem -extensions v3_ca -batch -out ca_netty.csr -utf8 -subj '/C=JP/O=orgname/OU=someinternalname' -passin pass:$CA_KEY_PWD
    ```

3. Create the extension file with name ca_netty.cnf, and the following contents:

    ```bash
    basicConstraints = critical, CA:TRUE
    keyUsage = keyCertSign, cRLSign
    subjectKeyIdentifier = hash
    ```

4. Self-sign CSR (using SHA256) and append the extensions described in the file

    ```bash
    openssl x509 -req -sha256 -days 3650 -in ca_netty.csr -signkey ca_key.pem -set_serial $ANY_SMALL_INTEGER -extfile ca_netty.cnf -out ca_netty.pem -passin pass:$CA_KEY_PWD
    ```

Server certificate signed with the above created certificate can be created using OpenSSL commands by following the below steps:


1. Create the server's private key:

    ```bash
    openssl genpkey -algorithm RSA -out svr_key.pem -pkeyopt rsa_keygen_bits:2048 -aes-128-cbc -pass pass:$SVR_KEY_PWD
    ```

2. Create server CSR:

    ```bash
    openssl req -new -key svr_key.pem -extensions v3_ca -batch -out svr_netty.csr -utf8 -subj '/C=JP' -passin pass:$SVR_KEY_PWD
    ```

3. Create the extension file with name svr_netty.cnf, and the following contents:

    ```bash
    basicConstraints = CA:FALSE
    subjectAltName = DNS:localhost,IP:127.0.0.1
    extendedKeyUsage = serverAuth
    ```

4. Sign (using SHA256) the CSR using root CA's, and append the extensions in svr_netty.cnf.

    ```bash
    openssl x509 -req -sha256 -days 1096 -in svr_netty.csr -CAkey ca_key.pem -CA ca_netty.pem -set_serial $SOME_LARGE_INTEGER -out svr_netty.pem -extfile svr_netty.cnf -passin pass:$CA_KEY_PWD
    ```

5. Create the PKCS12 Keystore from server's private key and certificate

    ```bash
    openssl pkcs12 -export -name netty -inkey svr_key.pem -in svr_netty.pem -out svr_netty.p12 -passin pass:$SVR_KEY_PWD -passout pass:$STORE_PWD
    ```





