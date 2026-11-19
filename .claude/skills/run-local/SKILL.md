---
name: run-local
description: Start this backend locally with its supporting services (MariaDB, MailHog, Keycloak) using the `local` profile, check that it's healthy, and get a bearer token for calling secured endpoints. Use when asked to run, start, boot or try out the app, to call or test an endpoint against a running instance, to check emails the app sent, or to confirm a change works in the real app rather than only in tests.
---

# Run the backend locally

This only covers the `local` profile (`src/main/resources_filtered/application-local.yml`). Other environments are out of scope.

## What runs where

| Service | Started by | Address |
|---|---|---|
| Backend | `mvn spring-boot:run` | http://localhost:8080/api (Swagger UI at `/api/`, OpenAPI JSON at `/api/api-docs`) |
| MariaDB | `compose.yaml` | `localhost:3308`, db `application`, user `user` / `secret` (created by `init/init.sql`) |
| MailHog (SMTP) | `compose.yaml` | SMTP `localhost:1025`, web UI http://localhost:8025 |
| Keycloak | `compose.yaml` | http://localhost:8090, realm `master`, admin `admin` / `admin` |

The `local` profile's behavior:
- **`ddl-auto: create-drop`**: the schema is rebuilt on every start, so **all data is lost on restart**.
- **Internal organization mode**: `organization.external.id` isn't set, so `@ForInternalOrganization` beans are active and Groepsadmin sync is off.
- **Mollie key is `invalid-key`**: `MollieCheckout` is wired, but real payment calls fail. Expect errors on checkout flows.
- **Alerts are disabled** and `be.sgl.backend` logs at TRACE, on the console only.
- **CORS** allows any origin.

## 1. Start the supporting services
Docker must be running.

```bash
docker compose up -d          # uses compose.yaml
docker compose ps             # mariadb, smtp and keycloak should be "running"
```

Keycloak takes about 20–60 s to boot. Before you request a token, wait until http://localhost:8090/realms/master returns 200.
Don't use `compose-local.yml` for this flow. It runs the backend itself in a container and doesn't include Keycloak.

## 2. Start the backend
Run it **in the background** (it doesn't exit), then wait for the health check instead of sleeping:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

- Ready means `curl -s http://localhost:8080/api/actuator/health` returns `{"status":"UP"}`. Wait for that with a monitor or until-loop. Don't poll the log blindly.
- If it doesn't come up, read the log output for the first `Caused by:`. Common causes:
  - **DB connection refused**: MariaDB isn't up yet, or port 3308 is taken.
  - **Issuer/JWK errors on first authenticated call**: Keycloak isn't ready yet.
  - **Port 8080 in use**: another instance is still running. Stop that one rather than starting a second.
- After code changes, stop the running instance and start it again (the data resets, see above).

## 3. Get a bearer token
Secured endpoints (`@OnlyAuthenticated`, `@OnlyStaff`, `@OnlyAdmin`) need a JWT from local Keycloak. `CustomUserDetails` **requires** the `given_name`, `family_name` and `email` claims and fails without them. The bootstrap `admin` user has none of these by default, so set them once per Keycloak container:

```bash
docker exec keycloak /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 --realm master --user admin --password admin
ADMIN_ID=$(docker exec keycloak /opt/keycloak/bin/kcadm.sh get users -r master -q username=admin --fields id --format csv --noquotes)
docker exec keycloak /opt/keycloak/bin/kcadm.sh update users/$ADMIN_ID -r master \
  -s firstName=Local -s lastName=Admin -s email=admin@localhost -s emailVerified=true
```

(If you prefer the UI: http://localhost:8090 → Users → admin → set first name, last name and email.)

Request a token:

```bash
TOKEN=$(curl -s -X POST http://localhost:8090/realms/master/protocol/openid-connect/token \
  -d grant_type=password -d client_id=admin-cli -d username=admin -d password=admin \
  | sed -E 's/.*"access_token":"([^"]+)".*/\1/')
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/<endpoint>
```

Tokens expire after a few minutes, so request a new one when you get a 401.

**First login becomes admin:** on an empty database (which every local start produces), the first authenticated user is created and the Admin role is set up (`CheckForInitialRunInternal`). Make one authenticated call right after startup so the following calls have admin rights.

## 4. Check side effects
- **Emails**: open http://localhost:8025, or call `curl -s http://localhost:8025/api/v2/messages`.
- **Database**: `docker exec -it mariadb mariadb -uuser -psecret application`
- **Uploaded images**: these are written under the repo's `images/` folder. Don't commit files created there during a test run (see the `git-workflow` skill).
- Some flows need an OWNER (and for certificates, a CERTIFIER) organization in the DB. Without one they throw `IncompleteConfigurationException("No organization configured!")`. Create it through the organization endpoints first.

## 5. Stop
- Stop the backend process you started (the background task). Leave the user's own instances alone.
- `docker compose stop` keeps the containers (and the Keycloak user changes). `docker compose down` removes them, and the Keycloak claim setup from step 3 has to be redone.
- Only stop or remove services you started in this session, unless the user asks.

## Reporting
Tell the user what you started, the URLs, what you verified (health, the endpoint response, the email in MailHog), and anything that's still running.
