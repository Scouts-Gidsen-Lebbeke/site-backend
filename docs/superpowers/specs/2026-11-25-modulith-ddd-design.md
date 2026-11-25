# Modulith / DDD restructuring — design

Date: 2026-11-25
Status: draft, awaiting approval

## Intent

The backend is organised as horizontal layers (`controller/ service/ repository/ entity/ dto/`, each split per domain). Two problems drive this change:

1. **Cross-domain dependencies.** Domains reach into each other's entities and repositories freely. There are cycles (`user ↔ membership`, `user ↔ role`, `user ↔ branch`, `user ↔ organization`), `registrable` depends on nearly every other domain, and eight services outside `user` use `UserRepository` directly.
2. **Large services.** Several services (`DispatchService`, `ActivityRegistrationService`, `EventRegistrationService`, `SyncService`, `MembershipService`, `ActivityService`, `RoleService`) mix many responsibilities.

The goal is single responsibility applied as far as reasonable: each domain module owns its data and logic, talks to other modules only through a small explicit API or through events, and large services are split into single-purpose use cases. The code stays one repository and one deployable.

## Goals

- Restructure into Spring Modulith application modules with **enforced** boundaries (`ApplicationModules.verify()` in the build).
- No direct cross-module JPA associations: modules reference each other's aggregates **by ID only** — this applies to `User` and to `Branch`.
- Cross-module side effects run through (durable) application events instead of direct calls where the caller doesn't need an answer.
- Split large services into `@Usecase` classes, one per command/query; move state transitions onto the aggregates (richer entities).
- Introduce Flyway so every schema change is versioned and shipped with the code.

## Non-goals

- Splitting into multiple Maven modules or deployables (rejected, see Approaches).
- Rewriting business rules. Behaviour stays the same unless a split forces a change.
- Changing the frontend. The REST contract stays stable except where explicitly listed under *API changes*.
- Removing database foreign keys. Columns like `user_id` keep their FK constraints; only the JPA mapping changes.

## Anti-goals (would count as failure even if it compiles)

- A "big bang" PR touching every module at once.
- An `allowedDependencies`/named-interface escape hatch that re-exposes `User` or `Branch` entities across modules — that's the coupling we're removing.
- Events used for request/response flows where a direct API call is clearer.
- Losing payment state or payment events during migration.

## Constraints

- Kotlin, Spring Boot 4.1, Java 21, Maven, MariaDB. Prod and dev use `ddl-auto: validate`; there is currently no migration tool.
- Existing conventions remain: `@Usecase` classes with `execute(...)`, `@ForExternalOrganization` / `@ForInternalOrganization` / `@WhenNotBlank` feature switching, per-endpoint security annotations, `LocalizedException`, Object Mothers for test data, no code comments except `// TODO:`.
- Generated code (Groepsadmin client, Belcotax JAXB) stays generated; its packages are treated as owned by the module that uses it.
- Every push to `main` deploys to prod, so each step must be independently deployable.

## Approaches considered

| | Approach | Trade-off | Decision |
|---|---|---|---|
| A | **Spring Modulith**, package-per-module, enforced via `verify()`, ID references + events | Most work (mainly untangling `User`), but boundaries are checked on every build | **Chosen** |
| B | Package-by-feature only, keep JPA associations, no enforcement | Cheap, but doesn't remove the cross-domain dependencies; they creep back | Rejected |
| C | Maven multi-module (jar per domain) | Compiler-enforced, but heavy build plumbing, entity scanning across jars, generated client placement — overkill for ~4k lines of services | Rejected |

Sub-decisions made in conversation:

- **Cross-module references:** ID-only for both `User` and `Branch` (rejected: shared-kernel entities via named interface; mixed approach).
- **Branch & roles:** `Branch` and `Role` definitions live in `organization`; only user↔role assignment lives in `access`.
- **Payment:** a dedicated `payment` module owns a single `payment` table (rejected: per-module `@Embeddable PaymentInfo` with a stateless gateway, which would duplicate paid/refunded handling in four modules).
- **Schema migrations:** add Flyway.

## Module map

Base package `be.sgl.backend.<module>`. Each module contains its own controllers, use cases, entities, repositories and DTOs; only types in the module's root package (or an explicit `api` named interface) are visible to other modules.

| Module | Owns | Public API / events | May depend on |
|---|---|---|---|
| `shared` (open module) | config, security plumbing, mail, PDF utils, SSE, images, exceptions, alerts, `Auditable`, `Conditionals`, `Usecase` | — | — |
| `setting` | `Setting`, `SettingId` | `SettingApi` | — |
| `people` | `User`, `Contact`, `MedicalRecord`, `Address`, `SiblingRelation`, `StaffData` | `UserApi` → read models (`UserSummary`: id, username, name, birthdate/age, sex, NIS, account, reduction flags, home address) | `setting` |
| `organization` | `Organization`, `ContactMethod`, `Branch`, `BranchStatus`, `Role`, `OrganizationProvider` | `OrganizationApi` (owner/certifier), `BranchApi`, `RoleApi` | `people`, `setting` |
| `access` | `UserRole(userId, roleId)`; level computation; `@Public/@OnlyAuthenticated/@OnlyStaff/@OnlyAdmin` checks | `AccessApi` (level of user, staff of branch, assign/revoke); listens to `MembershipPaid` | `organization`, `people` |
| `payment` | `Payment` (amount, status, provider id, `ownerType`, `ownerId`), `CheckoutProvider` + Mollie/Payconiq, webhook, refunds, refund cost | `PaymentApi.startCheckout/refund/status`; publishes `PaymentCompleted`, `PaymentCanceled`, `PaymentRefunded` | `people` |
| `membership` | `Membership`, `MembershipPeriod`, `MembershipRestriction`, membership certificates | `MembershipApi`; publishes `MembershipPaid`; listens to payment events | `people`, `organization`, `payment`, `setting` |
| `activity` | `Activity`, `Event`, `ActivityRegistration`, `EventRegistration`, `ActivityRestriction`, `Registrable`, `Registration`, certificates (sub-packages `activity` / `event`) | `ActivityApi` (registrations for Belcotax); listens to payment events | `people`, `organization`, `membership`, `payment` |
| `calendar` | `Calendar`, `CalendarItem`, `CalendarPeriod` | — | `organization` |
| `news` | `NewsItem` | — | — |
| `shop` | `Shop`, `ShopItem`, `ShopOrder`, `OrderItem`, `DeliveryOption` (entities only, parked) | — | `people` |
| `groepsadmin` | everything in today's `service/user/sync`, `ExternalUserDataProvider`, `CheckForInitialRunExternal`, generated Groepsadmin client | sync endpoints (SSE) | `people`, `organization`, `access`, `membership` |
| `belcotax` | `DispatchService` split into use cases, JAXB classes, XML/PDF output | Belcotax endpoints | `activity`, `organization`, `people` |

The dependency graph above is acyclic. `groepsadmin` and `belcotax` are deliberately "outer" modules that orchestrate others; nothing depends on them.

### Notable relocations

- `RoleLevel` moves from `entity.user` to `shared.security` (not `organization`: `people` needs it for `UserDTO.level`, and `organization` depends on `people`).
- `MailService` gets its default sender through a `DefaultSenderProvider` SPI in `shared`, implemented by `organization` (owner organization email).
- `User.roles` and `User.level` are removed. `access` computes level as max level over the user's roles. The security annotations ask `AccessApi`.
- `RoleService` splits: role CRUD (`createMemberRole`, `createStaffRole`, `update*`, `deleteRole`, `getRoleToSync*`) → `organization`; assignment (`markUserAsStaff`, `markUserAsAdmin`, `assignRole`, `deassignRoleFromUser`, `getUserRolesByRole`) and `FindStaffBranchForUser` → `access`.
- Inside `organization`, `Role → Branch` stays a real JPA association (same module).
- `Payable` (name, description, open/closed window) is inlined into `Registrable` and `Shop`; it's pricing/availability owned by those modules.
- **Reading the user's level outside `access`.** `ContactService` (admin check), `UserDTO.level` (frontend-visible) and activity cancellation (`level < STAFF`) need the level, but `people`/`activity` may not depend on `access` (cycle). Resolution: a `CurrentUserLevel` / `UserLevelProvider` SPI in `shared`, implemented by `access`. The security annotations stay in `shared` and use the same SPI. `UserDTO.level` is kept.
- Role start/end currently in `UserDataProvider` moves out of `people`: local assignment to `access` (internal mode), Groepsadmin function start/end to `groepsadmin` (external mode).
- **Shop is parked.** It only has entities (no repository/service/controller) and was meant to become a Shopify-like item-with-options store that never shipped. Move the entities as-is into `shop`, replace the `Payment` superclass with inlined columns so `payment` doesn't need to know about it, no payment listener, not included in the payment backfill. Designing the shop is a separate future project.
- `UserDataProvider` / `InternalUserDataProvider` stay in `people`; `ExternalUserDataProvider` moves to `groepsadmin` and implements the `people` SPI, keeping the `@ForExternalOrganization` switch.
- Generic `PaymentService<T>` with overridable `handlePayment*` hooks is removed; each module has a listener use case (e.g. `MarkMembershipPaid`) reacting to payment events filtered by `ownerType`.

### API changes (frontend-visible)

- Branch responses no longer embed `staff`; staff of a branch is served by `access` (e.g. `GET /branches/{id}/staff`).
- Any other unavoidable contract change must be listed in the PR that introduces it.

## Cross-module communication

- **Queries** go through the target module's API interface, returning read models / DTOs, never entities.
- **Side effects** go through Spring application events published by the owning aggregate's use case, consumed with `@ApplicationModuleListener` (async, after commit, own transaction).
- **Durability:** Spring Modulith JPA event publication registry (`event_publication` table) so failed listeners are retried, not lost. Incomplete publications are resubmitted on restart.
- **Groepsadmin and events.** The Groepsadmin client forwards the caller's bearer token, which async listeners don't have. Rules:
  - The Groepsadmin membership-request (`LidAanvraag`) endpoint is public and needs no token, so it may be called from an event listener. That listener must be idempotent (e.g. record that the request was sent for this membership and skip on retry), since the publication registry may redeliver.
  - All other Groepsadmin calls (functions/roles, member data) only happen in the admin-triggered sync endpoints, synchronously, with the admin's token. Event listeners never call authenticated Groepsadmin endpoints. A service account is not available.
- Example flow: `StartMembershipCheckout` → `PaymentApi.startCheckout(ownerType=MEMBERSHIP, ownerId=42, amount)` → Mollie webhook → `payment` marks paid, publishes `PaymentCompleted(ownerType, ownerId)` → `membership` marks membership paid, publishes `MembershipPaid(userId, branchId)` → `access` assigns the branch's member role; membership certificate creation listens too.

## Domain model style

- Aggregates own their state transitions (`membership.markPaid()`, `registration.cancel()`), guarding invariants with `check(...)`/`LocalizedException`.
- Services that only route to use cases may remain as thin façades for controllers, or controllers call use cases directly — builder's call per module, but no service may hold business rules that belong to a single use case.
- Use cases stay named as verb phrases with a single `execute(...)`.

## Persistence & migrations (Flyway)

- **Step 0:** add Flyway. Baseline `src/main/resources/db/migration/V1__baseline.sql` (Flyway's default `classpath:db/migration`), a schema-only dump of prod (`mysqldump --no-data`) **provided by the repo owner**. Prod and dev adopt it with `baseline-on-migrate` / `baseline-version: 1` so it isn't re-run there. Before step 0 ships, start the app against dev with `ddl-auto: validate` to catch drift between dev and prod; if they differ, fix dev by hand first. Tests and `local` run all migrations from scratch; `local` switches from `create-drop` to Flyway + `validate`.
- Each module PR ships its own migrations (e.g. `V3__payment_table.sql` creating `payment` and backfilling from `membership`, `activity_registration`, `event_registration`, then dropping old payment columns in a later migration once verified).
- `event_publication` table is created by a Flyway migration (not Modulith's auto-init).
- Table names may stay as they are; renaming tables is out of scope.

## Rollout

One module per PR, each independently deployable and green:

0. Flyway baseline; add `spring-modulith-starter-core`, `-starter-test`; add `ModularityTest` that is strict for a growing `STRICT_MODULES` list and tolerates violations in the legacy layer packages. (`-starter-jpa` / event registry comes with step 5, the first step that publishes events.)
1. `shared` (open, with `UserLevelProvider` and `DefaultSenderProvider` SPIs) + leaves: `setting`, `news`.
2. `organization` (branch + role definitions) and `access`; `access` implements `UserLevelProvider`; branch staff API change.
3. `calendar` (needs ID-only branch from step 2), `shop` (parked, entities only).
4. `people` (drop `roles`/`level` from `User`; `UserApi` read models; remove external `UserRepository` usages).
5. `payment` (new table + backfill, events, listeners in membership/activity; remove `PaymentService<T>`). Keep the Mollie webhook URL unchanged so in-flight payments still resolve.
6. `membership`.
7. `activity` (split `ActivityService`, `ActivityRegistrationService`, `EventRegistrationService`).
8. `groepsadmin` (split `SyncService`).
9. `belcotax` (split `DispatchService`).
10. Remove the violation allow-list; `verify()` fully strict. Add generated module documentation (Modulith `Documenter`) to `docs/`.

## Testing

- `ModularityTest` (`ApplicationModules.of(...).verify()`) runs in CI from step 0.
- New/moved tests prefer `@ApplicationModuleTest` (bootstrap one module, mock neighbours' APIs) and Modulith `Scenario` for event flows; full `@IntegrationTest` remains for end-to-end paths (checkout → webhook → membership paid → role assigned).
- Existing tests are moved with their code; Object Mothers move to their module's test package.
- Payment backfill migration gets a test against a seeded schema.

## Left to the builder

- Exact read-model shapes for `UserApi`, `BranchApi`, `RoleApi`, `ActivityApi`.
- Whether thin `*Service` façades stay or controllers call use cases directly, per module.
- Event class names and payloads beyond the flows listed.
- How the known-violations allow-list is expressed in `ModularityTest`.
- Whether `ownerType` is an enum in `payment` (then known module types live there) or a free string — prefer the option that keeps `payment` from depending on its callers.
