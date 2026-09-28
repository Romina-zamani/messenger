# Lesson 5: TLS encryption (HTTPS and WSS)

TLS protects bytes while they travel between client and server. Without TLS, someone able to observe the network can read or alter chat traffic. TLS provides encryption, integrity, and server identity. It does **not** create user accounts or decide who may enter the room.

## Handshake

```text
Client                         Server
  | ---- supported TLS options --> |
  | <--- certificate + public key - |
  | verifies hostname and trust     |
  | ==== encrypted session keys === |
  | ---- HTTP Upgrade request ----> |
  | <--- 101 Switching Protocols -- |
  | <==== WebSocket frames (WSS) ==>|
```

The PKCS12 file contains the server certificate and its private key. Never commit or share the private key. The certificate includes `localhost` and `127.0.0.1` as Subject Alternative Names, which modern TLS clients check.

## 1. Generate a development certificate

From the project root:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
.\scripts\generate-dev-certificate.ps1
```

It creates two ignored files: `localhost.p12` holds the server private key, while `localhost-truststore.p12` holds only the public certificate trusted by the learning client. The learning password is `changeit`; production secrets must come from a secret manager.

## 2. Start PostgreSQL and the TLS backend

```powershell
docker compose up -d postgres
$env:TLS_KEY_STORE_PASSWORD='changeit'
mvn -f backend/pom.xml spring-boot:run "-Dspring-boot.run.profiles=tls"
```

The server now listens at `https://localhost:8443`, not HTTP port 8080.

## 3. Observe trust before bypassing it

A self-signed certificate is not automatically trusted. This command should report a certificate trust error, which proves verification is active:

```powershell
Invoke-RestMethod https://localhost:8443/actuator/health
```

For this local experiment only, inspect the endpoint while explicitly accepting the self-signed certificate (PowerShell 7):

```powershell
Invoke-RestMethod https://localhost:8443/actuator/health -SkipCertificateCheck
```

Never disable certificate checking in production.

### This computer: Avast HTTPS scanning

Avast Web/Mail Shield currently intercepts even `localhost` and replaces the certificate with one issued by `Avast Web/Mail Shield Self-signed Root`. That is why a Java client may still report `PKIX path building failed` even though `localhost-truststore.p12` contains the Spring certificate. To test certificate identity end to end, add `localhost`/`127.0.0.1` as an HTTPS-scanning exclusion in Avast, or temporarily disable only Avast HTTPS scanning while doing this local lesson. Do not add an unknown replacement certificate to the project and do not disable Java hostname verification.

## 4. Connect with secure WebSocket

Let Java use the local PKCS12 certificate as a temporary trust store for this learning command:

```powershell
$env:CHAT_TRUST_STORE=(Resolve-Path 'backend/src/main/resources/certs/localhost-truststore.p12').Path
$env:CHAT_TRUST_STORE_PASSWORD='changeit'
$env:MAVEN_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'
mvn -f backend/pom.xml compile exec:java "-Dexec.mainClass=dev.mahdi.chat.websocket.WebSocketConsoleClient" "-Dexec.args=wss://localhost:8443/ws/chat"
```

Run it in two terminals. Messages remain JSON as before, but their network transport is encrypted.

## Exercises

1. Try `wss://127.0.0.1:8443/ws/chat`; it works because the IP is in the certificate SAN.
2. Try another hostname pointing to this computer; hostname verification should reject it.
3. Compare `ws://` and `wss://`: WebSocket behavior is unchanged, only transport security changes.
