# HTTPS/SSL Server Using Netty


## 1. Description

This repo includes:
* notes about certificate and keystore best practices.
* a reference implementation of a Netty HTTPS/SSL server

The code is based on the [example code that comes with Netty](https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/http/helloworld).
This is just a personal project created for learning and testing.
If you have any question or comment, please use the Issues tab of this git repo.


## 2. Quick Start: Running the Project As-Is

1. Install the SSL certificate as a trusted root certificate in a browser.

    This is necessary if you are using a self-signed root CA certificate (as that provided in the project).
    For e.g., to install the certificate in Mozilla Firefox:
    - Click Preferences > Privacy & Security > View Certificates > Authorities tab > Import
    - Navigate to where you stored the certificate, and select the certificate file (If you are using certificates included in the project, you should select `ca_netty.pem` in src/main/resources/security/* folder).`


2. Build the jar file.

    "Fat-jar" file is created (via gradle [`shadowJar`](https://imperceptiblethoughts.com/shadow/) plugin) when the project is built.
    Run the following command in the project root directory.
    ````bash
./gradle clean build
    ````
    The "fat-jar" will be created as `build/libs//nettyHttps-<version>-all.jar`.

3. Start the HTTPS Server.

    Update the application.properties file in the src/main/resources folder with the path of the Keystore configuration file, and the server port.

    To start the HTTPS server run the below command.
    ````bash
java -jar nettyHttps-<version>-all.jar
    ````

4. Access the Server.

    Open browser on page *https://localhost:8443* (or *https://127.0.0.1:8443*).

    If everything is correct one should see a "Hello world!" message in your browser and a gray padlock (indicating a trustworthy SSL/TLS connection).
    If something is wrong, the browser will display the "Warning: Potential Security Risk Ahead" error page with error codes (that should help to trace the error's cause.).
    (see [How do I tell if my connection to a website is secure?](https://support.mozilla.org/en-US/kb/how-do-i-tell-if-my-connection-is-secure))


A production project __SHOULD NOT INCLUDE__ keystore or keystore configuration parameters.
Including them in the project is against the certificate and keystore best practices.
The *src/main/resources/security* directory contains the keystore (*svr_netty.p12* - containing the server private key, and certificate), and Keystore configuration file (*security-config.properties*).
These files were only included for allowing to quickly test the server.

The keys, certificates, and keystore in *src/main/resources/security* directory were generated as described in section 3.2.


## 3. Security Best Practices and Netty HTTPS Server

Best practices for certificates and security [[1](README.md#4-references), [2](README.md#4-references)] is to secure the private keys.
HSM (hardware security module), secret manager, and keystore files can be used to secure the keys.
Keystore files are the de-facto “secrets repository” for Java/JVM-based applications.

Other relevant certificates and security best practices [[1](README.md#4-references), [2](README.md#4-references), [3](README.md#4-references), [4](README.md#4-references)] include:
- Do not package keystore files and keystore configuration files (including passwords) inside Jar files/application archive files.
- Use standard operating system access security protocols to protect keystore files and keystore configuration files (including passwords).
- Use the PKCS12 Format for keystores.

The repository source code is intended to be used as reference for implementing a Netty HTTPS/SSL server using keystore.
The following topics provide additional information necessary to implement a Netty HTTPS server based on best practices.


### 3.1 How to to Create a Netty HTTPS Server
To create a Netty HTTPS server, the following is required:

- Keystore with server private key and certificate signed by CA. In the project, *src/main/resources/security/svr_netty.p12* is the keystore file.
- Keystore configuration file containing the data needed to access the keystore data. The file should contain the keystore file path, the keystore password, entry alias (for private key and certificate pair), and private key password. In the project, *src/main/resources/security/config.properties* is the keystore configuration file.
- File containing path of the keystore configuration file, and logic to extract this path. In the project, the path is contained in *src/main/resources/application.properties*, and the *io.netty.example.https.helloworld.HttpsHelloWorldServer* class extracts the path using *io.netty.example.https.util.ApplicationProperties* class.
- Logic to extract the private key and certificate from keystore using the data in keystore configuration file. The key and certificate are necessary to create the *io.netty.handler.ssl.SslContext* for the Netty server. In the project, *io.netty.example.https.security.KeyStoreData* class extracts the private key and certificate, and *io.netty.handler.ssl.SslContext* is created in the *io.netty.example.https.helloworld.HttpsHelloWorldServer* class.

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

**Note:** : The keystore only worked for me when the key password (`$SVR_KEY_PWD`) was equal to keystore password (`STORE_PWD`). It seems that Java doesn't support different keystore and key passwords in PKCS12 [[5](README.md#4-references)].


### 3.2 How to Create & Use Self-Signed Certificate Chain

The following is required for creating a self-signed certificate chain:

- Self-signed root CA certificate.  
- Server certificate signed with the self-signed root CA certificate.

The root CA certificate must be installed on the browser (as described in section 2.), and the server certificate should be installed in the server (as described in section 3.).

**Note:** : The below steps create certificates to be used in tests with *localhost*/*127.0.0.1* domain, and browser. For other scenarios refer to OpenSSL Manpages [[6](README.md#4-references)], RFC 5280 [[7](README.md#4-references)], or other online references (e.g.: "A Web PKI x509 certificate primer" from Mozilla [[8](README.md#4-references)]).


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

## 4. References

[1] [Certificates and Security Best Practices](https://myarch.com/cert-book/index.html)

[2] [Cryptographic Key Storage Options & Best Practices](https://www.globalsign.com/en/blog/cryptographic-key-management-and-storage-best-practice/)

[3] [Where to store Java keystore password?](https://security.stackexchange.com/questions/31050/where-to-store-java-keystore-password)

[4] [How can I protect MySQL username and password from decompiling?](https://stackoverflow.com/questions/442862/how-can-i-protect-mysql-username-and-password-from-decompiling/442872#442872)

[5] [Java PKCS12 Keystore generated with openssl BadPaddingException](https://stackoverflow.com/questions/32850783/java-pkcs12-keystore-generated-with-openssl-badpaddingexception)

[6] [OpenSSL Manpages](https://www.openssl.org/docs/manpages.html)

[7] [RFC 5280](https://tools.ietf.org/html/rfc5280)

[8] [A Web PKI x509 certificate primer](https://developer.mozilla.org/en-US/docs/Mozilla/Security/x509_Certificates)
