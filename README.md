# Web Portal (Thymeleaf)

Backoffice + Storefront stile OpenCart.

## Run locale

```
mvn spring-boot:run
```

Variabili utili:
- `GATEWAY_BASE_URL`
- `KEYCLOAK_ISSUER_URI`
- `KEYCLOAK_CLIENT_ID`
- `KEYCLOAK_CLIENT_SECRET`

## Deploy OpenShift

```
oc apply -f deploy/openshift/web-portal.yaml
```
