# Web Portal (Thymeleaf)

Backoffice + Storefront stile OpenCart.

## Run locale

```bash
mvn spring-boot:run
```

Variabili utili:
- `GATEWAY_BASE_URL`
- `APP_STORE_URL`
- `KEYCLOAK_ISSUER_URI`
- `KEYCLOAK_CLIENT_ID`
- `KEYCLOAK_CLIENT_SECRET`

Con profilo `local` il portal usa automaticamente:
- cookie sessione non `secure`
- `APP_STORE_URL=http://localhost:${SERVER_PORT:8080}` come fallback

Per testare checkout con provider sandbox:
- PayPal usa il redirect browser verso il portal e la capture server-side nel `payment-service`
- Fabrick usa redirect browser e, se configurato, webhook server-to-server verso `payment-service`
- per i webhook reali da sandbox esterna serve un URL HTTPS pubblico, non `localhost`

L'esempio condiviso di configurazione si trova in:
- [`/Users/andreaterrasi/Desktop/sviluppo/workspace-codex/ecommerce/.env.local.example`](/Users/andreaterrasi/Desktop/sviluppo/workspace-codex/ecommerce/.env.local.example)

## Deploy OpenShift

```bash
oc apply -f deploy/openshift/web-portal.yaml
```
