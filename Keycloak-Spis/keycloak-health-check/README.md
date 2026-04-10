# Keycloak Health Check SPI

This project provides a custom **Keycloak SPI (Service Provider Interface)** that exposes a health check endpoint for monitoring Keycloak authentication and token flows.  
It can be integrated with monitoring systems (e.g., Notification Service, Prometheus, or Kubernetes liveness/readiness probes).

---

## 🚀 Features
- **Password Grant Check** – validates a test user login using Resource Owner Password Credentials (ROPC).
- **UMA RPT Check** – validates UMA token issuance for the configured client.
- **Token Exchange Check** – validates token exchange with a subject token.
- **Configurable checks** – run specific checks using a query parameter.
- **Health-friendly responses**:
  - `200 OK` → All requested checks succeeded.
  - `503 Service Unavailable` → One or more checks failed.
  - `400 Bad Request` → Invalid check parameter.
  - `500 Internal Server Error` → Unexpected error during execution.

---

## 📡 Endpoint

The endpoint is exposed at:

```
GET /realms/{realm}/health
```

Example:

```
http://localhost:8080/realms/testing/health
```

---

## 🔍 Query Parameters

| Parameter | Description | Example |
|-----------|-------------|---------|
| `checks`  | Comma-separated list of checks to run. Valid values: `password`, `rpt`, `exchange`, `all`. Defaults to `all`. | `?checks=password,rpt` |

---

## 🧾 Examples

- Run **all checks** (default):  
  ```
  GET /realms/testing/health
  ```

- Run only **Password Grant**:  
  ```
  GET /realms/testing/health?checks=password
  ```

- Run **Password Grant + RPT**:  
  ```
  GET /realms/testing/health?checks=password,rpt
  ```

- Run **Token Exchange** only:  
  ```
  GET /realms/testing/health?checks=exchange
  ```

---

## ⚙️ Configuration

The SPI requires the following configuration parameters:

- `custom-health-checker-keycloak-base-url`
- `custom-health-checker-realm`
- `custom-health-checker-client-id`
- `custom-health-checker-client-secret`
- `custom-health-checker-test-username`
- `custom-health-checker-test-password`
- `custom-health-checker-subject-token` *(for token exchange)*
- `custom-health-checker-subject-issuer` *(for token exchange)*

### How to supply configuration

You can provide these parameters in two ways:

1. **External Config File**  
   Create a file (e.g., `/opt/keycloak/conf/keycloak-spi.conf`) and add the parameters inside:  
   ```properties
   custom-health-checker-keycloak-base-url=https://localhost:8443
   custom-health-checker-realm=testing
   custom-health-checker-client-id=my-client
   custom-health-checker-client-secret=my-secret
   custom-health-checker-test-username=testuser
   custom-health-checker-test-password=testpass
   custom-health-checker-subject-token=dummy-token
   custom-health-checker-subject-issuer=my-issuer
   ```  

   Then, in your `docker-compose.yml`, point Keycloak to this file:  
   ```yaml
   environment:
     - KEYCLOAK_SPI_CONF=/opt/keycloak/conf/keycloak-spi.conf
   volumes:
     - ./keycloak-spi.conf:/opt/bitnami/keycloak/conf/keycloak-spi.conf
   ```

2. **Keycloak Config File (`keycloak.conf`)**  
   Alternatively, add the same parameters directly into Keycloak’s main `keycloak.conf`.

---

## 📦 Build & Deployment

1. Package the project:
   ```bash
   mvn clean package
   ```
2. Copy the JAR to your Keycloak providers directory:
   ```bash
   cp target/keycloak-health-spi-1.0.0.jar /opt/keycloak/providers/
   ```
3. Restart Keycloak:
   ```bash
   ./kc.sh start
   ```

---

## 📈 Integration Notes

- The Notification system or monitoring tool can poll this endpoint at regular intervals (e.g., every 30s).
- If the endpoint returns **503**, raise an alert.
- If only specific flows need monitoring, provide `checks` in the query.

---

## 🤝 Contributing
Pull requests are welcome! Please open an issue first to discuss proposed changes.

---

## 📜 License
This project is licensed under the MIT License.
