# Modulith Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Lay the foundation of the modulith restructuring: Flyway-managed schema, Spring Modulith with a boundary test that is strict for migrated modules, an open `shared` module, and the first two real modules (`setting`, `news`).

**Architecture:** Spring Modulith treats every direct sub-package of `be.sgl.backend` as a module. The legacy layer packages (`controller`, `service`, `entity`, …) are modules too during the migration, and their violations are tolerated. `ModularityTest` fails only on violations that involve a module in its `STRICT_MODULES` list, and every later plan adds its modules to that list. Inside a module, the root package is the public API, and the sub-packages `web` (controllers, request/response DTOs), `application` (services, use cases, API implementations) and `domain` (entities, repositories, module exceptions) are internal.

**Tech Stack:** Kotlin 2.1, Spring Boot 4.1, Spring Modulith 2.x (BOM matching Boot 4.1), Flyway (`spring-boot-starter-flyway` + `flyway-mysql`), MariaDB, JUnit 5, Testcontainers.

**Spec:** `docs/superpowers/specs/2026-11-25-modulith-ddd-design.md` (rollout steps 0–1; calendar and shop move to later plans, see the spec's rollout section).

## Global Constraints

- No code comments except `// TODO:` (memory rule). This applies to moved files too, so drop any KDoc you touch only if the file is otherwise edited. Don't add any.
- Keep the REST contract unchanged in this plan. URLs, DTO field names and security annotations stay the same.
- `@PreAuthorize("@levelSecurityService…")` references the bean by name, so the class stays named `LevelSecurityService`.
- Maven: `~/.m2/wrapper/dists/apache-maven-3.9.11-bin/*/apache-maven-3.9.11/bin/mvn.cmd` (there's no `mvn` on PATH). Docker must be running for tests.
- Work on a feature branch (`refactor/modulith-foundation`), stage new files immediately, and never push.
- Source files are CRLF. Prefer IntelliJ (MCP `rename_refactoring` / move) or the Edit tool over sed for package moves.

## Review Focus

1. **Existing prod/dev database at first boot with Flyway.** The app must start, record V1 as the baseline and not run it. Pinned by `FlywayBaselineTest` (Task 1).
2. **Schema drift between V1 and entities.** With `ddl-auto: validate` everywhere, a missing column must fail the test context, not prod. Pinned by every `@IntegrationTest` now running Flyway + validate (Task 1).
3. **Security still applied after the move.** `@OnlyAdmin` on `PUT /settings/{id}` must still reject a non-admin with 403. Pinned in `SettingControllerTest` (Task 4).
4. **Mail sender when no owner organization exists.** It still throws `IncompleteConfigurationException("No organization configured!")`. Pinned in `OwnerOrganizationSenderProviderTest` (Task 3).
5. **A violation in a strict module must fail the build.** Pinned by `ModularityTest.strictModulesMustExist` and a deliberate-violation check during Task 2 (Step 5).

---

### Task 1: Flyway with baseline

**Files:**
- Modify: `pom.xml` (dependencies)
- Create: `src/main/resources/db/migration/V1__baseline.sql`
- Modify: `src/main/resources_filtered/application.yml`, `application-local.yml`; `src/test/resources/application-test.yml`
- Test: `src/test/kotlin/be/sgl/backend/config/FlywayBaselineTest.kt`

**Interfaces:**
- Produces: `classpath:db/migration`. Later plans add `V2__…` onwards. Every profile runs with `spring.jpa.hibernate.ddl-auto: validate`.

- [ ] **Step 1: Add dependencies.** Add `org.springframework.boot:spring-boot-starter-flyway` and `org.flywaydb:flyway-mysql` to `pom.xml`. Both are version-managed by the Boot parent.

- [ ] **Step 2: Create `V1__baseline.sql`.**
  - **Preferred:** use the schema-only prod dump the repo owner provides at `src/main/resources/db/migration/V1__baseline.sql`. Strip `AUTO_INCREMENT=n` table options, `DEFINER` clauses and `/*!…*/` session-variable lines, and remove any `CREATE DATABASE`/`USE` statements.
  - **If the dump isn't there yet:** generate a provisional one by starting the app against an empty MariaDB with `spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create` and `…scripts.create-target=target/schema.sql`, and copy that file. Put `-- TODO: replace with prod schema dump before deploying` as the first line. The task is not done for deploy until the owner's dump replaces it.

- [ ] **Step 3: Configure.**
  - In `application.yml`: `spring.flyway.baseline-on-migrate: true`, `spring.flyway.baseline-version: 1`, `spring.jpa.hibernate.ddl-auto: validate`.
  - In `application-local.yml` and `application-test.yml`: change `ddl-auto: create-drop` to `validate`.

- [ ] **Step 4: Write the failing test** `FlywayBaselineTest`. It's an `@IntegrationTest` with two tests:
  - `migrationsApplyOnEmptyDatabase`: inject `Flyway` and assert `flyway.info().current().version.version == "1"` and `flyway.info().pending().isEmpty()`.
  - `existingSchemaIsBaselinedNotMigrated`: on the test DataSource, create a fresh schema (`CREATE DATABASE baseline_check`), run V1 into it by hand with a plain `Flyway.configure().dataSource(url-with-baseline_check, user, pw).baselineOnMigrate(false).load().migrate()`, then `DROP TABLE flyway_schema_history`. Configure a second Flyway on it with `baselineOnMigrate(true).baselineVersion("1")` and call `migrate()`. Assert that `info().applied()` contains exactly one entry, of type `BASELINE` and version `1`, and that `migrationsExecuted == 0`.

- [ ] **Step 5: Run it.** Command: `mvn test -Dtest=FlywayBaselineTest`. Expected: FAIL before Steps 1–3 are in place, PASS after.

- [ ] **Step 6: Run the full suite.** Command: `mvn test`. Expected: all green. Any failure from `validate` means V1 and the entities disagree. Fix V1 (provisional) or report the drift (owner's dump), and don't touch entities.

- [ ] **Step 7: Run locally.** Run `docker compose down -v && docker compose up -d`, then `mvn spring-boot:run -Dspring-boot.run.profiles=local`. Expected: the log shows `Successfully applied 1 migration` and the app starts.

- [ ] **Step 8: Commit.** Message: `Manage schema with Flyway`.

---

### Task 2: Spring Modulith and boundary test

**Files:**
- Modify: `pom.xml` (import the `org.springframework.modulith:spring-modulith-bom` in `dependencyManagement`; add `spring-modulith-starter-core` and, in test scope, `spring-modulith-starter-test`)
- Create: `src/main/kotlin/be/sgl/backend/openapi/OpenApiModule.kt`
- Test: `src/test/kotlin/be/sgl/backend/ModularityTest.kt`

**Interfaces:**
- Produces: `ModularityTest.STRICT_MODULES: List<String>`, which every later task and plan extends.

- [ ] **Step 1: Add Modulith.**
  - Set the BOM version property `spring-modulith.version` to the newest 2.x release whose docs list Spring Boot 4.1 compatibility (check https://spring.io/projects/spring-modulith).
  - Don't add `spring-modulith-starter-jpa` (the event registry) yet. It arrives with the payment plan.

- [ ] **Step 2: Mark the generated client as an open module.**
  - In `OpenApiModule.kt` (package `be.sgl.backend.openapi`), declare `@org.springframework.modulith.PackageInfo @org.springframework.modulith.ApplicationModule(type = ApplicationModule.Type.OPEN) class OpenApiModule`.
  - Without this, every use of `openapi.api`/`openapi.model` counts as access to internals.

- [ ] **Step 3: Write `ModularityTest`.** It's a plain JUnit test with no Spring context.

```kotlin
class ModularityTest {
    private val modules = ApplicationModules.of(SiteBackendApplication::class.java)

    @Test
    fun strictModulesMustExist() {
        val names = modules.map { it.identifier.toString() }
        assertTrue(names.containsAll(STRICT_MODULES), "Unknown strict modules: ${STRICT_MODULES - names.toSet()}")
    }

    @Test
    fun strictModulesHaveNoViolations() {
        val offending = modules.detectViolations().messages.filter { message ->
            STRICT_MODULES.any { m -> message.contains("'$m'") || message.contains("Slice $m") || message.contains("be.sgl.backend.$m.") }
        }
        assertTrue(offending.isEmpty(), offending.joinToString("\n\n"))
    }

    companion object {
        val STRICT_MODULES = listOf<String>()
    }
}
```

- [ ] **Step 4: Run it.** Command: `mvn test -Dtest=ModularityTest`. Expected: PASS, because the list is empty.

- [ ] **Step 5: Check that a strict violation fails the build.** Temporarily set `STRICT_MODULES = listOf("controller")` and run again. Expected: `strictModulesHaveNoViolations` FAILS and lists messages naming `'controller'`. Then revert to the empty list. This proves the message filter matches the Modulith version in use. If nothing is reported, adjust the three `contains` patterns to the actual message format and repeat.

- [ ] **Step 6: Commit.** Message: `Add Spring Modulith boundary test`.

---

### Task 3: `shared` open module

**Files:**
- Move `config/AppConfig.kt`, `CustomUserDetails.kt`, `LocaleConfig.kt`, `LocalizedExceptionHandler.kt`, `OpenApiConfig.kt`, `SpringSecurityAuditorAware.kt`, `WebConfig.kt` → `shared/config/`
- Move `config/security/*` → `shared/security/`
- Move `util/*` → `shared/util/`, `exception/*` → `shared/exception/`, `alert/*` → `shared/alert/`
- Move `entity/Auditable.kt` → `shared/persistence/`
- Move `service/ImageService.kt` → `shared/image/`, `service/MailService.kt` → `shared/mail/`, `service/SseService.kt` → `shared/sse/`
- Move `entity/user/RoleLevel.kt` → `shared/security/RoleLevel.kt`
- Stay in legacy `config`: `GroepsAdminConfig.kt` (later the `groepsadmin` plan), `PaymentConfig.kt` (later the `payment` plan)
- Create: `shared/SharedModule.kt`, `shared/security/UserLevelProvider.kt`, `shared/mail/DefaultSenderProvider.kt`
- Create: `service/user/RepositoryUserLevelProvider.kt`, `service/organization/OwnerOrganizationSenderProvider.kt`
- Move the matching tests under `src/test/kotlin/be/sgl/backend/shared/...`, including the `util/IntegrationTest.kt`, `MockitoKotlin.kt` and `config/*Configuration*.kt` test helpers
- Test: `src/test/kotlin/be/sgl/backend/service/user/RepositoryUserLevelProviderTest.kt`, `src/test/kotlin/be/sgl/backend/service/organization/OwnerOrganizationSenderProviderTest.kt`

**Interfaces:**
- Produces:
  - `be.sgl.backend.shared.security.RoleLevel` (same constants as today)
  - `interface UserLevelProvider { fun levelOf(username: String): RoleLevel? }`
  - `interface DefaultSenderProvider { fun defaultSenderEmail(): String }`
  - `shared.persistence.Auditable`, `shared.util.Usecase`, `shared.exception.LocalizedException` / `NotFoundException`, `shared.security.Public` / `OnlyAuthenticated` / `OnlyStaff` / `OnlyAdmin`, `shared.image.ImageService`, `shared.mail.MailService`
- Consumes: nothing from earlier tasks besides `ModularityTest`

- [ ] **Step 1: Write the failing provider tests.** These are plain Mockito tests.
  - `RepositoryUserLevelProviderTest.levelIsHighestRoleLevel`: a user from `UserTestMother` with STAFF and ADMIN roles, found by `findByUsername("jdoe")`, gives `levelOf("jdoe") == RoleLevel.ADMIN`.
  - `RepositoryUserLevelProviderTest.unknownUserHasNoLevel`: `levelOf("ghost") == null`.
  - `OwnerOrganizationSenderProviderTest.returnsOwnerEmail`: an owner organization with an email contact returns that email.
  - `OwnerOrganizationSenderProviderTest.missingOwnerThrows`: `getByType(OWNER)` returns null, so it throws `IncompleteConfigurationException` with the message `"No organization configured!"`.
  - `OwnerOrganizationSenderProviderTest.ownerWithoutEmailThrows`: the message is `"No organization email configured!"`.

- [ ] **Step 2: Run them.** Command: `mvn test -Dtest='RepositoryUserLevelProviderTest,OwnerOrganizationSenderProviderTest'`. Expected: compilation FAIL (the classes don't exist).

- [ ] **Step 3: Move the files.**
  - Move the files to `be.sgl.backend.shared.*` as listed, using IntelliJ Move Class/Package so the imports update everywhere, main and test.
  - `IntegrationTest` and the container configurations move to `be.sgl.backend.shared` test packages.

- [ ] **Step 4: Introduce the SPIs and implementations.**
  - `LevelSecurityService` depends on `UserLevelProvider` instead of `UserRepository`. The check is `(provider.levelOf(username) ?: return false) >= level`.
  - `RepositoryUserLevelProvider` (`@Component`, in `service/user`) holds the current lookup: `userRepository.findByUsername(username)?.level`.
  - `MailService.MailBuilder.fromDefault()` calls `DefaultSenderProvider.defaultSenderEmail()`.
  - `OwnerOrganizationSenderProvider` (`@Component`, in `service/organization`) holds the current lookup and its two exceptions.

- [ ] **Step 5: Mark `shared` as open.** In `shared/SharedModule.kt`, declare `@PackageInfo @ApplicationModule(type = ApplicationModule.Type.OPEN) class SharedModule`.

- [ ] **Step 6: Make it strict.** Add `"shared"` to `ModularityTest.STRICT_MODULES`.

- [ ] **Step 7: Run tests.** Command: `mvn test`. Expected: all green, including `LevelSecurityServiceTest` (now mocking `UserLevelProvider`) and `ModularityTest`. If `ModularityTest` reports `shared` depending on a legacy package, move that dependency behind an SPI the same way. Don't add it to an allow-list.

- [ ] **Step 8: Commit.** Two commits:
  - `Move cross-cutting code into shared module` (the moves only)
  - `Decouple shared from user and organization` (the SPIs, the providers and the strict entry)

---

### Task 4: `setting` module

**Files:**
- Create: `setting/SettingApi.kt`
- Move `entity/setting/SettingId.kt` → `setting/SettingId.kt`
- Move `entity/setting/Setting.kt` → `setting/domain/Setting.kt`
- Move `repository/SettingRepository.kt` → `setting/domain/SettingRepository.kt`
- Move `service/SettingService.kt` → `setting/application/SettingService.kt`
- Move `controller/setting/SettingController.kt` → `setting/web/SettingController.kt`
- Modify: `service/belcotax/FilterIntoValidFormData.kt`, `service/branch/BranchService.kt`, `service/organization/GetRepresentativeForOwner.kt` and their tests (depend on `SettingApi`)
- Move tests: `SettingTestMother`/`SettingTest` → `test/.../setting/domain/`, `SettingServiceTest` → `test/.../setting/application/`
- Test: `src/test/kotlin/be/sgl/backend/setting/web/SettingControllerTest.kt`, `src/test/kotlin/be/sgl/backend/setting/SettingModuleTest.kt`

**Interfaces:**
- Produces: `be.sgl.backend.setting.SettingApi`

```kotlin
interface SettingApi {
    fun get(id: String): String?
    fun getOrDefault(id: String, default: String): String
    fun getOrDefault(id: String, default: Double): Double
    fun getOrDefault(id: String, default: Boolean): Boolean
}
```

  `SettingService : SettingApi` keeps `update(id, value)` as an internal method used only by the controller. `SettingId` stays public at the module root.

- [ ] **Step 1: Write the failing tests.**
  - `SettingControllerTest`, an `@IntegrationTest` with MockMvc:
    - `getIsPublic`: seed a `Setting("CALENDAR_NAME", "Kalender")`; `GET /settings/CALENDAR_NAME` returns 200 with body `Kalender` and no auth.
    - `updateRequiresAdmin`: a `PUT /settings/CALENDAR_NAME?value=x` with a JWT for a user without roles returns 403.
    - `updateAsAdminStoresValue`: the same request as an admin user returns 200, and a subsequent `SettingApi.get("CALENDAR_NAME") == "x"`.
    - `updateWithoutValueDeletes`: `PUT` without `value` makes `get(...)` return null.
  - `SettingModuleTest`, `@ApplicationModuleTest` with the `test` profile and the DB container configuration imported:
    - `moduleBootsStandalone`: the context loads and the injected `SettingApi` is a `SettingService`.

- [ ] **Step 2: Run them.** Command: `mvn test -Dtest='SettingControllerTest,SettingModuleTest'`. Expected: compilation FAIL (`be.sgl.backend.setting.SettingApi` is missing).

- [ ] **Step 3: Move and wire.** Move the files and create `SettingApi`. The three consumers and their tests inject and mock `SettingApi` instead of `SettingService`.

- [ ] **Step 4: Make it strict.** Add `"setting"` to `STRICT_MODULES`.

- [ ] **Step 5: Run tests.** Command: `mvn test`. Expected: all green.

- [ ] **Step 6: Commit.** Message: `Extract setting module`.

---

### Task 5: `news` module

**Files:**
- Move `entity/news/NewsItem.kt` → `news/domain/NewsItem.kt`
- Move `repository/news/NewsItemRepository.kt` → `news/domain/`
- Move `NewsItemNotFoundException` (from `shared/exception/NotFoundException.kt`) → `news/domain/NewsItemNotFoundException.kt`
- Move `service/news/NewsItemService.kt` → `news/application/`
- Move `controller/news/NewsItemController.kt`, `dto/news/*` → `news/web/`
- Move tests: `NewsItemTestMother` → `test/.../news/domain/`, `NewsItemServiceTest` → `test/.../news/application/`
- Modify: `CLAUDE.md` (Architecture section)
- Test: `src/test/kotlin/be/sgl/backend/news/NewsModuleTest.kt`

**Interfaces:**
- Produces: none. `news` has no public API, so its root package stays empty apart from tests.

- [ ] **Step 1: Write the failing test.**
  - `NewsModuleTest`, an `@ApplicationModuleTest` like `SettingModuleTest`, with `ImageService` mocked via `@MockitoBean` because `shared` isn't bootstrapped.
  - `moduleBootsStandalone` asserts that the context loads and that a `NewsItemService` bean exists.
  - `visibleItemsOnly` saves one visible and one hidden `NewsItem` (from the mother) and asserts that `getVisibleItems()` returns only the visible one.

- [ ] **Step 2: Run it.** Command: `mvn test -Dtest=NewsModuleTest`. Expected: compilation FAIL.

- [ ] **Step 3: Move the files** listed above.

- [ ] **Step 4: Make it strict.** Add `"news"` to `STRICT_MODULES`.

- [ ] **Step 5: Update `CLAUDE.md`.** Replace the layered-architecture paragraph with:
  - the module layout: root = API, `web`/`application`/`domain` internal
  - the `shared` open module and its SPIs (`UserLevelProvider`, `DefaultSenderProvider`)
  - the `ModularityTest.STRICT_MODULES` rule: new or migrated modules must be added, and violations are never allow-listed
  - Flyway: every schema change is a `V<n>__*.sql` in `src/main/resources/db/migration`, and `ddl-auto` is `validate` everywhere

- [ ] **Step 6: Run tests.** Command: `mvn clean package`. Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit.** Message: `Extract news module`.
