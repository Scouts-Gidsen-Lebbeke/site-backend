# Modulith Calendar + Shop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract the `calendar` module, with branches referenced by ID only, and the parked `shop` module (entities only, with no payment superclasses).

**Architecture:** Same layout as plans 1–2: the root package is the public API, and `web` / `application` / `domain` are internal. Calendar is a leaf: no other domain uses it, so the only cross-module seam is `Calendar.branch`. That field becomes `branchId`, and branch names and images come from a new `organization` read model, `BranchSummary`, served by `BranchApi`. Shop has no service or controller. Its entities move as they are, `Payable` / `Payment` become inlined columns and `ShopOrder.user` becomes `userId`.

**Tech Stack:** Kotlin, Spring Boot 4.1, Spring Modulith 2.1.3, Hibernate, MariaDB, JUnit 5, Mockito (BDD), AssertJ, Testcontainers.

**Spec:** `docs/superpowers/specs/2026-11-25-modulith-ddd-design.md` (rollout step 3). Read `docs/superpowers/handover/2026-11-25-modulith.md` too.

## Global Constraints

- No code comments except `// TODO:`. Drop KDoc from files you move, such as `Payable`'s field docs when they're inlined.
- Tests follow `.claude/rules/testing.md`:
  - backtick `should … when …` names, with `// given` / `// when` / `// then`
  - BDDMockito, AssertJ and test mothers
  - no per-class `@MockitoBean` and no extra Spring contexts
  - test classes go in the production class's package, and mothers move with their entity into `<module>/domain`
- The REST contract stays unchanged:
  - URLs, JSON field names and values, status codes and security annotations stay as they are
  - the `branch` objects in calendar responses keep exactly `id`, `name`, `image`
  - the only accepted difference is the OpenAPI schema name of that nested object (`BranchBase` → `BranchSummary`)
- No schema change and no new Flyway migration. `calendar.branch_id` and `shop_order.user_id` keep their FKs, and only the JPA mapping changes. Table names stay as they are.
- `calendar` → `allowedDependencies = ["shared", "organization"]` (organization root types only). `shop` → `allowedDependencies = ["shared"]`.
- `ModularityTest` fails on violations whose source is a strict module, on cycles through one, and on unrecognised wording. Add `"calendar"` and `"shop"` to `STRICT_MODULES`.
- Calendar-related `NotFoundException`s stay in `shared/exception`.
- Maven: `/c/Users/robin/.m2/wrapper/dists/apache-maven-3.9.11-bin/6mqf5t809d9geo83kj4ttckcbc/apache-maven-3.9.11/bin/mvn.cmd`. Docker must be running. Run `mvn clean` after moves.
- Branch `refactor/modulith-calendar-shop`, based on `refactor/modulith-organization-access`.
  - Stage by explicit path and never push.
  - Never stage `images/` or `src/main/resources/openapi/_merged_spec.yaml`.
- Use `git mv` for moves. Files are CRLF. Don't sed-rewrite string literals in `ViolationFilterTest.kt`.

## Review Focus

1. **Calendar JSON after the ID switch.** `GET /calendars` and `GET /calendars/{id}` still return `branch: {id, name, image}` with the branch's real name and image. Pinned in `CalendarControllerTest` (Task 1).
2. **Creating a calendar period.** It still creates one calendar per ACTIVE branch, and none for PASSIVE, MEMBER or HIDDEN branches. Pinned in `CalendarPeriodServiceTest` and `CalendarPeriodControllerTest` (Task 1).
3. **Default items are not persisted.** `GET /calendars/{id}?withDefaults=true` fills empty Sundays without saving them, and a second plain `GET` shows only the real items. Pinned in `CalendarControllerTest` (Task 1).
4. **Calendars of several branches in one listing.** Every calendar gets its own branch summary, with no mix-ups, and the summaries come from one batched lookup. Pinned in `CalendarServiceTest` (Task 1).
5. **The parked shop still validates against the schema.** With `ddl-auto: validate`, the context starts with the inlined columns. Pinned by every `@IntegrationTest` (Task 2) and `ShopOrderTest`.

---

### Task 1: Extract the `calendar` module

**Files:**
- Create in organization:
  - `organization/BranchSummary.kt`: `@Schema(name = "BranchSummary") data class BranchSummary(val id: Int, val name: String, val image: String)`
  - extend `organization/BranchApi.kt` and `organization/application/BranchApiImpl.kt`
- Create: `calendar/CalendarModule.kt` (`allowedDependencies = ["shared", "organization"]`)
- Move to `calendar/domain/`: `Calendar`, `CalendarItem`, `CalendarPeriod`, `CalendarRepository`, `CalendarItemRepository`, `CalendarPeriodRepository`
- Move to `calendar/application/`: `CalendarService`, `CalendarPeriodService`
- Move to `calendar/web/`: `CalendarController`, `CalendarPeriodController`, and all eight `dto/calendar/*` classes
- Modify: `ModularityTest.STRICT_MODULES` (+ `"calendar"`)
- Tests:
  - move `CalendarTestMother`, `CalendarItemTestMother`, `CalendarPeriodTestMother` and `CalendarItemTest` to `calendar/domain`
  - move `CalendarRequestTestMother` to `calendar/web`
  - move `CalendarServiceTest` and `CalendarPeriodServiceTest` to `calendar/application`
  - extend `BranchApiImplTest`
  - new: `calendar/web/CalendarControllerTest` and `calendar/web/CalendarPeriodControllerTest` (`@IntegrationTest`)

**Interfaces:**
- Produces, in `be.sgl.backend.organization`:
  ```kotlin
  interface BranchApi {
      fun isActive(branchId: Int): Boolean
      fun findSummaries(ids: Collection<Int>): Map<Int, BranchSummary>
      fun findBranchesWithCalendar(): List<BranchSummary>
  }
  ```
  - `findSummaries` makes one repository call (`findAllById`), and unknown ids are absent from the map.
  - `findBranchesWithCalendar` returns the branches of the existing `BranchRepository.getBranchesWithCalendar()` (status ACTIVE).
- `Calendar.branch: Branch` becomes `@Column(name = "branch_id", nullable = false) var branchId: Int = 0`, with constructor `Calendar(period: CalendarPeriod, branchId: Int)`. The table's unique constraint and index annotations stay.
- DTO factories:
  - `CalendarBaseDTO.of(calendar: Calendar, branch: BranchSummary)` and `CalendarDTO.of(calendar: Calendar, branch: BranchSummary)`
  - `CalendarItemWithCalendarsDTO.of(item: CalendarItem, branches: Map<Int, BranchSummary>)`
  - the field `branch` is typed `BranchSummary`
  - `CalendarItemDTO` and `CalendarPeriodDTO` are unchanged
- Services resolve summaries with a single `branchApi.findSummaries(…)` call per request over all involved `branchId`s.
- `CalendarPeriodService.createCalendarPeriod` creates `Calendar(newPeriod, it.id)` for each `branchApi.findBranchesWithCalendar()`, and no longer uses `BranchRepository`.
- `CalendarItemRepository.findByClosedFalseAndStartAfterAndEndBefore` has no callers. Remove it.

- [ ] **Step 1: Write the failing tests:**
  - `BranchApiImplTest`
    - `should map id name and image when finding summaries`
    - `should omit unknown ids when finding summaries`
    - `should return only active branches when finding branches with calendar`
  - `CalendarServiceTest`: `should give each calendar its own branch summary when listing current calendars` (two calendars, two branches, a single `findSummaries` call)
  - `CalendarPeriodServiceTest`: `should create one calendar per branch with calendar when creating a period` (update the existing case to `BranchApi`)
  - `CalendarControllerTest`
    - `should return branch id name and image when getting current calendars`
    - `should fill empty sundays without persisting them when requesting defaults` (second plain GET has only the real items)
    - `should return 404 when calendar id is unknown`
    - `should return 403 when a non-staff user updates a calendar`, using whatever annotation the endpoint has today
  - `CalendarPeriodControllerTest`
    - `should create calendars only for active branches when staff creates a period`
    - `should return 400 when the new period overlaps an existing one`
- [ ] **Step 2: Run.** Command: `mvn test -Dtest='BranchApiImplTest,CalendarServiceTest,CalendarPeriodServiceTest,CalendarControllerTest,CalendarPeriodControllerTest'`. Expected: FAIL (missing types or signatures).
- [ ] **Step 3: Implement** the moves and changes above. Add `"calendar"` to `STRICT_MODULES`.
- [ ] **Step 4: Verify the boundary.** Command: `mvn clean test -Dtest=ModularityTest`. Expected: PASS. Then run `grep -rn "be.sgl.backend.organization\.\(domain\|application\|web\)" src/main/kotlin/be/sgl/backend/calendar`. Expected: no matches.
- [ ] **Step 5: Run the full suite.** Command: `mvn test`. Expected: all green.
- [ ] **Step 6: Commit.** Message: `Extract calendar module`.

---

### Task 2: Extract the parked `shop` module

**Files:**
- Create: `shop/ShopModule.kt` (`allowedDependencies = ["shared"]`)
- Move to `shop/domain/`: `Shop`, `ShopItem`, `ShopOrder`, `OrderItem`, `DeliveryOption`
- Modify:
  - `Shop` extends `Auditable` and inlines the `Payable` fields: `@Id @GeneratedValue(IDENTITY) var id: Int?`, `lateinit var name`, `@Lob lateinit var description`, `var open: LocalDateTime`, `var closed: LocalDateTime`, with the same defaults
  - `ShopOrder` extends `Auditable` and inlines the `Payment` fields: `id`, `paid`, `price`, `@Column(unique = true) paymentId`
  - `ShopOrder.user: User?` becomes `@Column(name = "user_id") var userId: Int? = null`
  - `getDescription()` stays as a plain function, and the `markPaid` / `markRefunded` helpers are not carried over
- Tests:
  - move the shop builders out of `entity/PayableTestMother.kt` and `entity/PaymentTestMother.kt` into `shop/domain/ShopTestMother.kt` and `shop/domain/ShopOrderTestMother.kt`, following the mother conventions with `userId` in place of `user`
  - move `entity/shop/ShopOrderTest.kt` to `shop/domain`
  - remove the shop imports from the legacy mothers
- Modify: `ModularityTest.STRICT_MODULES` (+ `"shop"`)

**Interfaces:**
- Produces: nothing other modules use. `Payable` and `Payment` stay in `entity/` for the legacy registrable and membership code, and the payment step removes them.

- [ ] **Step 1: Write the failing test.** `ShopOrderTest`: `should describe order with shop name and id` (existing assertion, now built with `ShopOrderTestMother`).
- [ ] **Step 2: Run.** Command: `mvn test -Dtest=ShopOrderTest`. Expected: FAIL (mother missing).
- [ ] **Step 3: Implement** the moves and the inlined fields. Add `"shop"` to `STRICT_MODULES`.
- [ ] **Step 4: Verify.** Command: `mvn clean test -Dtest='ModularityTest,ShopOrderTest'`. Expected: PASS. Then run `mvn test`. Expected: all green (the context validates the inlined columns against `shop` / `shop_order`).
- [ ] **Step 5: Commit.** Message: `Extract parked shop module`.

---

### Task 3: Docs and full build

**Files:**
- Modify: `CLAUDE.md` (migrated modules list: `shared`, `setting`, `news`, `organization`, `access`, `calendar`, `shop`)
- Modify: `docs/superpowers/handover/2026-11-25-modulith.md`
  - plan 3 status, branch and commits
  - rollout step "calendar + shop" marked done
  - `BranchSummary` / `BranchApi.findSummaries` available for later steps (membership and activity DTOs will need them)
  - the OpenAPI schema-name note for the PR
  - next action: plan 4 (people)

- [ ] **Step 1: Update both docs.**
- [ ] **Step 2: Run the full build.** Command: `mvn clean package`. Expected: BUILD SUCCESS. If it's killed for low memory, report that and don't loop.
- [ ] **Step 3: Commit.** Message: `Update docs for calendar and shop modules`.
