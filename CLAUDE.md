## What this is
The Kotlin/Spring Boot 4 backend (Java 21, Maven) for the Scouts & Gidsen Lebbeke website. It handles members, memberships, activities/events and their registrations and payments, calendars, news, and Belcotax tax forms. The REST API runs under the `/api` context path, and Swagger UI is served at `/api/` (API docs at `/api/api-docs`).

## Commands
There is no `mvnw` script (only `.mvn/wrapper`), so use a system `mvn`.

- Build: `mvn clean package` (this is what CI runs, tests included)
- Regenerate code only: `mvn generate-sources` (see "Generated code")
- All tests: `mvn test`. Integration tests use Testcontainers (MariaDB), so **Docker must be running**.
- One test class: `mvn test -Dtest=ValidateAndCreateActivityRegistrationTest`
- One test method: `mvn test -Dtest=ValidateAndCreateActivityRegistrationTest#methodName`
- Local infrastructure: `docker compose up -d` (from `compose.yaml`: MariaDB on host port 3308, MailHog SMTP on 1025 with UI on 8025, Keycloak on 8090)
- Run locally: `mvn spring-boot:run -Dspring-boot.run.profiles=local`. The `local` profile points at the compose services; Flyway builds the schema and Hibernate only validates it. `init/init.sql` only creates the databases. A local schema that has tables but no Flyway history makes startup fail: drop and recreate the local `application` database.
- `compose-local.yml` runs the backend plus MariaDB in Docker. `compose-prod.yml` is the production deploy file.

## Configuration
- Application YAML lives in `src/main/resources_filtered/` (Maven-filtered, so `@project.version@` is substituted). Profiles: `local`, `dev`, `prod`; tests use `test` (`src/test/resources/application-test.yml`).
- Base config leaves secrets blank and expects env vars: `SPRING_DATASOURCE_*`, `SPRING_MAIL_*`, `SPRING_APPLICATION_BASE-URL`, `KEYCLOAK_RESOURCE`, `ORGANIZATION_EXTERNAL_ID`, `ORGANIZATION_API-KEY_MOLLIE`.
- **Feature switching by property presence** (`shared/util/Conditionals.kt`): `@WhenNotBlank("prop")` registers a bean only when the property is set. Two modes are built on it:
  - `@ForExternalOrganization` (when `organization.external.id` is set): Groepsadmin (Scouts en Gidsen Vlaanderen) is the master of user data. The sync use cases in `service/user/sync/` (annotated `@ExternalUsecase`), `ExternalUserDataProvider` and `CheckForInitialRunExternal` are active.
  - `@ForInternalOrganization` (when it is blank): user data stays local through `InternalUserDataProvider` and the like.
  - Payments: `MollieCheckout` is active when `organization.api-key.mollie` is set (a Mollie `Client` bean exists). `PayconiqCheckout` is a stub. Both implement `CheckoutProvider`.
  When you add behavior that depends on the external/internal mode or on a payment provider, use these annotations rather than runtime `if` checks.

## Architecture
Package root: `be.sgl.backend`. The code is migrating from horizontal layers to a Spring Modulith modular monolith (design: `docs/superpowers/specs/2026-11-25-modulith-ddd-design.md`).

- **Modules** (migrated: `shared`, `setting`, `news`, `organization`, `access`, `calendar`, `shop`, `people`): each direct sub-package of `be.sgl.backend` is a module. Its root package is the public API (interfaces such as `SettingApi`, shared enums, events); the sub-packages are internal: `web` (controllers, request/response DTOs), `application` (services, use cases, API implementations) and `domain` (entities, repositories, module exceptions). Other modules use only the root package, reference other modules' aggregates by ID, and never touch their entities or repositories. Each module declares its permitted dependencies in a `<Name>Module.kt` (`@PackageInfo @ApplicationModule(allowedDependencies = [...])`); without it, a dependency on a legacy package goes unnoticed.
- **`shared`** is an open module for cross-cutting code (config, security, util, exceptions, alerts, mail, images, SSE, `Auditable`, `RoleLevel`). It must not depend on any domain code; it asks for what it needs through SPIs that domains implement (`UserLevelProvider`, `DefaultSenderProvider`).
- **Boundaries are tested**: `ModularityTest` fails on any Modulith violation whose source is a module in `STRICT_MODULES` and on any cycle through one; legacy packages may still use strict modules' internals until their own step. Add every new or migrated module to that list; never work around a violation with an allow-list.
- **Legacy layers** (not migrated yet): `controller/<domain>` → `service/<domain>` → `repository/<domain>` → `entity/<domain>`, with request and response objects in `dto/<domain>`. They are tolerated by `ModularityTest` until their domain is extracted.
- **Schema**: Flyway owns the schema. Every change is a new `V<n>__<description>.sql` in `src/main/resources/db/migration` (V1 is the prod baseline); `ddl-auto` is `validate` in every profile. Only `dev` and `prod` baseline an existing schema at V1.

- **Use-case classes**: Besides `*Service` classes, business logic is split into single-purpose classes annotated `@Usecase` (a `@Component` alias in `shared/util/Usecase.kt`). Each one has an `execute(...)` method and a verb-phrase name, for example `ValidateAndCreateMembership` or `CreateCertificateForActivityRegistration`. Services orchestrate these. Follow this pattern for new non-trivial logic.
- **Security** (`shared/security/`): The app is a JWT resource server backed by Keycloak (Scouts en Gidsen Vlaanderen login). `SecurityConfig` permits every request at the filter level, so authorization is done **per endpoint** with the meta-annotations in `LevelSecurityService.kt`: `@Public`, `@OnlyAuthenticated`, `@OnlyStaff`, `@OnlyAdmin`. They compare the user's `RoleLevel`, looked up through `UserLevelProvider`. Every new endpoint needs one of these annotations.
- **Groepsadmin client**: Generated from `src/main/resources/openapi/*.yaml` and wired in `config/GroepsAdminConfig.kt` (`DynamicAuthApiClient` forwards the caller's bearer token).
- **Organization**: `OrganizationProvider` returns the cached OWNER and CERTIFIER organizations from the DB. Many flows (certificates, Belcotax) throw `IncompleteConfigurationException` if these are missing.
- **Documents and mail**: Certificates and forms fill the PDF templates in `src/main/resources/forms/` with PDFBox (`shared/util/PdfUtils.kt`). Emails are Thymeleaf templates in `src/main/resources/templates/`, sent by `MailService`. Belcotax XML (`service/belcotax/`) uses JAXB classes generated from `xsd/Belcotax-2023.xsd`.
- **i18n and errors**: Messages are in `messages.properties` / `messages_nl_BE.properties`. Throw `LocalizedException(messageKey, params)` for user-facing errors. `error-handling-spring-boot-starter` maps exceptions to HTTP statuses (`IllegalStateException` and `check(...)` failures become 400, `TODO()` becomes 418), as configured in `application.yml`.
- **Alerts**: `shared/alert/AlertLogger` emails operational alerts (`sgl.alert.*`, disabled in `local`).
- **Long-running admin operations** (such as user/member sync) stream progress with `SseEmitter` (`SseService`).

## Generated code
`mvn generate-sources` (which also runs on any build) produces:
- The Groepsadmin REST client: packages `be.sgl.backend.openapi.api` and `.model` (Java, RestClient, Jackson 3), from `src/main/resources/openapi/`
- Belcotax JAXB classes from the XSD

These end up under `target/`. Don't edit them. Change the spec or XSD and regenerate.

`src/main/openapi-templates/` overrides generator templates: required, non-nullable spec fields get JSpecify `@NonNull`, which Kotlin enforces (it ignores the generator's default `jakarta.annotation` ones). Optional fields stay platform types. To make a Groepsadmin field non-null in Kotlin, add it to the schema's `required` list in the YAML.

## Tests
- `@IntegrationTest` (`src/test/kotlin/.../shared/util/IntegrationTest.kt`) = `@SpringBootTest` + `test` profile + MockMvc + MariaDB and mail Testcontainers.
- `TestConfigurations` replaces `CheckoutProvider` with `MockedCheckoutProvider`.
- Test data is built with Object Mothers in `src/test/kotlin/.../entity/**/*TestMother.kt` (for migrated modules: next to the entity, in `<module>/domain`). `PersistedUsers` saves users with or without an admin role for security tests. Reuse or extend them instead of building entities inline.

## Deployment
On a push to `main`, `.github/workflows/multi-env-build-push-deploy.yml` builds the jar, pushes a multi-arch image to `ghcr.io/<owner>/backend:latest`, then copies a generated `.env` and `compose-prod.yml` to the prod server over SSH and restarts it with `podman compose`. The Cloud Run steps in README.md (and its badge) are outdated.
