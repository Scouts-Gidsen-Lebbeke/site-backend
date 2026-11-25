# Modulith People Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract the `people` module (users, contacts, medical records, siblings, staff data). Move role start/end out of `UserDataProvider`. Let `access` own the user-level lookup and the branch staff list.

**Architecture:** Same layout as plans 1–3. `people` is a strict module that depends only on `shared`. Legacy code (membership, activity, belcotax, Groepsadmin sync, initial run) keeps using people internals (`User` entity, `UserRepository`) until its own step, as the boundary rule allows. Legacy implementations of people's extension points (`ExternalUserDataProvider`, `CheckForInitialRunInternal/External`) implement interfaces in `people.application`, which is tolerated. `access` gains a dependency on `people` through a small `UserApi`.

**Tech Stack:** Kotlin, Spring Boot 4.1, Spring Modulith 2.1.3, Hibernate, MariaDB, JUnit 5, Mockito (BDD), AssertJ, Testcontainers.

**Spec:** `docs/superpowers/specs/2026-11-25-modulith-ddd-design.md` (rollout step 4). Read `docs/superpowers/handover/2026-11-25-modulith.md` too.

## Global Constraints

- No code comments except `// TODO:`. Drop KDoc from files you move.
- Tests follow `.claude/rules/testing.md`:
  - backtick `should … when …` names, with `// given` / `// when` / `// then`
  - BDDMockito, AssertJ and test mothers
  - no per-class `@MockitoBean` and no extra contexts
  - test classes go in the production class's package, and mothers move with their entity into `<module>/domain`
- The REST contract stays unchanged except for the one accepted API change in Task 2 (branch staff).
- No schema change and no new Flyway migration. Table names stay as they are.
- `people` → `allowedDependencies = ["shared"]`. `access` → `["shared", "organization", "people", "setting"]`.
- Strict modules import only their own packages, other modules' **root** packages and `shared`.
- `ModularityTest` fails on violations whose source is a strict module, on cycles through one, and on unrecognised wording. Add `"people"`.
- Maven: `/c/Users/robin/.m2/wrapper/dists/apache-maven-3.9.11-bin/6mqf5t809d9geo83kj4ttckcbc/apache-maven-3.9.11/bin/mvn.cmd`. Docker must be running. Run `mvn clean` after moves. Run the full suite once per task. It's memory heavy, so if it's killed, report that and don't loop.
- Branch `refactor/modulith-people`, based on `refactor/modulith-calendar-shop`.
  - Stage by explicit path and never push.
  - Never stage `images/` or `src/main/resources/openapi/_merged_spec.yaml`.
  - Use `git mv`.
  - Don't sed-rewrite string literals in `ViolationFilterTest.kt`.

## Decisions made in this plan (open to owner correction)

- **`User.level` keeps its read-only `@Formula` until the membership and activity steps.** Their DTOs embed `UserDTO` built from the `User` entity, so dropping it now would mean threading a level through every legacy DTO factory that those steps rewrite anyway. Security checks stop using it: `access` implements `UserLevelProvider` (Task 2), and people's `ContactService` asks `UserLevelProvider`. Update the formula's `// TODO:` to name the activity step.
- **The `CheckForInitialRun` interface moves to `people.application`.** `UserService` calls it, and its two implementations stay legacy because they touch roles and Groepsadmin.
- **Role start/end become a legacy `RoleAssigner`.** The local part goes through `AccessApi` and the external part adds the Groepsadmin calls. It moves to `groepsadmin` in step 8.
- **`RemoteFile` moves to `shared.image`** (image controllers and the profile picture use it).
- **The empty, tracked `service/BranchService.kt` is deleted.**

## Review Focus

1. **Security after the level switch.** `@OnlyAdmin` / `@OnlyStaff` still allow admins and staff and deny guests, and an unknown username is denied. Pinned in `AccessLevelProviderTest` and the existing controller tests (Task 2).
2. **Contact access.** A user still reads their own contacts, an admin reads anyone's, and another user is refused. Pinned in `ContactServiceTest` (Task 1).
3. **Branch staff listing.** `GET /branches/{id}/staff` returns the staff, with each one's nickname for that branch, returns an empty list when `STAFF_VISIBLE` is false, and returns 404 for an unknown branch. `GET /branches/{id}` no longer has `staff`. Pinned in `BranchStaffControllerTest` (Task 2).
4. **Role assignment side effects are unchanged.**
   - The membership payment still assigns the member role.
   - In external mode, a non-member role still creates the Groepsadmin functions, and ending a role still ends them.
   - Pinned in `ExternalRoleAssignerTest` and `MembershipServiceTest` (Task 1).
5. **First login still works.** `GET /users/me` for an unknown user still runs the initial-run hook. Pinned in `UserServiceTest` (Task 1).

---

### Task 1: Extract the `people` module

**Files:**
- Create: `people/PeopleModule.kt` (`allowedDependencies = ["shared"]`), `people/UserApi.kt`, `people/StaffMember.kt`
- Move to `people/domain/`: `User`, `Contact`, `ContactRole`, `MedicalRecord`, `BloodGroup`, `SiblingRelation`, `StaffData`, `UserRepository`, `ContactRepository`, `MedicalRecordRepository`, `SiblingRepository`
- Move to `people/application/`: `UserService`, `ContactService`, `MedicalRecordService`, `UserDataProvider`, `InternalUserDataProvider`, `inital/CheckForInitialRun.kt`
- Create in `people/application/`: `UserApiImpl`
- Move to `people/web/`: `UserController`, `ContactController`, `MedicalRecordController`, `UserDTO`, `ContactDTO`, `CreateOrUpdateContactRequest`, `MedicalRecordDTO`, `UpdateUserRequest`
- Stay legacy:
  - `ExternalUserDataProvider` (stays in `service/user`, extends `people.application.UserDataProvider`)
  - `CheckForInitialRunInternal/External`
  - all of `service/user/sync`
  - `dto/user/ExternalMember`
  - `dto/user/StaffDTO`, `controller/branch/BranchStaffController`, `service/branch/GetBranchWithStaff`, `dto/branch/BranchWithStaff` (Task 2 removes them)
  - `RepositoryUserLevelProvider` (Task 2 removes it)
- Create (legacy):
  - `service/role/RoleAssigner.kt`: `@Component @ForInternalOrganization open class RoleAssigner(accessApi)` with `open fun startRole(user: User, role: Role): Boolean` and `open fun endRole(user: User, role: Role)`, both delegating to `AccessApi`
  - `service/role/ExternalRoleAssigner.kt`: `@ForExternalOrganization`, extends `RoleAssigner` and carries over today's Groepsadmin logic from `ExternalUserDataProvider.startRole/endRole`, including the member-role skip and the missing-externalId skip
- Modify:
  - `UserDataProvider`: drop `startRole` / `endRole`; `ExternalUserDataProvider` drops its overrides
  - `UserRoleService` and `MembershipService` use `RoleAssigner` for role start/end, while `MembershipService` keeps `UserDataProvider.acceptRegistration`
  - `StaffData.nicknames` becomes `Map<Int, String>` with `@MapKeyColumn(name = "branch_id")` on table `user_branch_nicknames`
  - `StaffDTO.of(user, branchId)` reads `nicknames[branchId]`
  - `ContactService.validateOwnerOrAdmin` uses `UserLevelProvider.levelOf(currentUsername) == RoleLevel.ADMIN` in place of `currentUser.level`
  - `User.level` formula: TODO text becomes `// TODO: drop with the activity step; read levels from UserLevelProvider`
- Move: `dto/RemoteFile.kt` → `shared/image/RemoteFile.kt`
- Delete: `service/BranchService.kt` (empty file)
- Modify: `ModularityTest.STRICT_MODULES` (+ `"people"`)
- Tests:
  - move the user tests and mothers (`UserTest`, `UserTestMother`, `ContactTestMother`, `StaffDataTestMother`, sibling, medical and contact-request mothers, `UserServiceTest`, `ContactServiceTest`, `MedicalRecordServiceTest` if present, `InternalUserDataProviderTest`, `UserRepositoryTest`) into the matching `people/...` packages
  - split the role cases out of `ExternalUserDataProviderTest` into `ExternalRoleAssignerTest`
  - new: `RoleAssignerTest`, `UserApiImplTest`

**Interfaces:**
- Produces (root package `be.sgl.backend.people`):
  ```kotlin
  interface UserApi {
      fun findIdByUsername(username: String): Int?
      fun findStaff(userIds: Collection<Int>, branchId: Int): List<StaffMember>
  }
  data class StaffMember(val name: String, val firstName: String, val image: String?, val nickname: String?, val totem: String?)
  ```
- `findStaff` loads the users with one `findAllById`. It skips users without a `staffData` row, keeping the current `GetBranchWithStaff` behaviour, and takes `nickname` from `nicknames[branchId]`.
- `RoleAssigner.startRole` returns false when the role is already assigned.

- [ ] **Step 1: Write the failing tests:**
  - `UserApiImplTest`
    - `should return id when username exists`
    - `should return null when username is unknown`
    - `should map staff with branch nickname when finding staff`
    - `should skip users without staff data when finding staff`
  - `RoleAssignerTest`
    - `should assign through access when starting a role`
    - `should return false when role already assigned`
    - `should revoke through access when ending a role`
  - `ExternalRoleAssignerTest`: the cases moved from `ExternalUserDataProviderTest` (Groepsadmin functions created for a non-member role, skipped for a member role or a missing externalId, ended on `endRole`)
  - `ContactServiceTest`: `should allow admin when user level provider reports admin` and `should refuse when another non-admin user reads contacts`, rewritten against a `UserLevelProvider` mock
- [ ] **Step 2: Run.** Command: `mvn test -Dtest='UserApiImplTest,RoleAssignerTest,ExternalRoleAssignerTest,ContactServiceTest'`. Expected: FAIL.
- [ ] **Step 3: Implement** the moves and changes above. Add `"people"` to `STRICT_MODULES`.
- [ ] **Step 4: Verify the boundary.** Command: `mvn clean test -Dtest=ModularityTest`. Expected: PASS. Then run `grep -rn "import be.sgl.backend\.\(controller\|service\|repository\|entity\|dto\)\." src/main/kotlin/be/sgl/backend/people`. Expected: no matches.
- [ ] **Step 5: Run the full suite.** Command: `mvn test`. Expected: all green.
- [ ] **Step 6: Commit.** Message: `Extract people module`.

---

### Task 2: `access` owns user levels and the branch staff list

**Files:**
- Create:
  - `access/application/AccessLevelProvider.kt`: implements `shared.security.UserLevelProvider`
  - `access/web/BranchStaffController.kt`: `GET /branches/{id}/staff`, `@Public`
  - `access/web/StaffDTO.kt`: `@Schema(name = "Staff")` with the same fields as today's `StaffDTO`, built from `StaffMember`
- Modify:
  - `access/AccessModule.kt`: `allowedDependencies = ["shared", "organization", "people", "setting"]`
  - `organization/web/BranchController.kt`: `GET /branches/{id}`, `@Public`, returns `BranchDTO` from `BranchService.getBranch(id)`, with the same OpenAPI docs minus the staff mention
- Delete (legacy):
  - `service/user/RepositoryUserLevelProvider.kt`
  - `controller/branch/BranchStaffController.kt`
  - `service/branch/GetBranchWithStaff.kt`
  - `dto/branch/BranchWithStaff.kt`
  - `dto/user/StaffDTO.kt`
  - their tests: move what still applies into the new tests, and delete `RepositoryUserLevelProviderTest`
- Tests: `AccessLevelProviderTest`, `access/web/BranchStaffControllerTest` (`@IntegrationTest`), `organization/web/BranchControllerTest` (`@IntegrationTest`, at least the `GET /{id}` cases)

**Interfaces:**
- Consumes: `UserApi`, `StaffMember` (Task 1); `RoleApi`, `BranchApi` (organization root); `SettingApi`, `SettingId` (setting root).
- `AccessLevelProvider.levelOf(username)` resolves `userApi.findIdByUsername(username)`. It returns null when the username is unknown, and otherwise the max `level` over `roleApi.findRoles(roleIdsOf(userId))`, or `RoleLevel.GUEST` when the user holds no roles.
- `GET /branches/{id}/staff`:
  - 404 (`BranchNotFoundException`) when `branchApi.findSummaries(listOf(id))` has no entry
  - `emptyList()` when `settingApi.getOrDefault(SettingId.STAFF_VISIBLE.name, true)` is false
  - otherwise `userApi.findStaff(accessApi.staffUserIdsOf(id), id).map(StaffDTO::of)`
- **API change (accepted by the owner in the spec):** `GET /branches/{id}` no longer embeds `staff`; clients call `GET /branches/{id}/staff`.

- [ ] **Step 1: Write the failing tests:**
  - `AccessLevelProviderTest`
    - `should return null when username is unknown`
    - `should return guest when user has no roles`
    - `should return highest level when user has several roles`
  - `BranchStaffControllerTest`
    - `should list staff with branch nickname when staff is visible`
    - `should return empty list when STAFF_VISIBLE is false`
    - `should return 404 when branch id is unknown`
  - `BranchControllerTest`
    - `should return branch without staff field when getting a branch`
    - `should return 404 when branch id is unknown`
- [ ] **Step 2: Run.** Command: `mvn test -Dtest='AccessLevelProviderTest,BranchStaffControllerTest,BranchControllerTest'`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Verify.** Command: `mvn clean test -Dtest=ModularityTest`. Expected: PASS. Then run `mvn test`. Expected: all green. In particular, every existing `@OnlyAdmin` / `@OnlyStaff` controller test still passes through `AccessLevelProvider`.
- [ ] **Step 5: Commit.** Message: `Let access own user levels and branch staff`. The body states the API change.

---

### Task 3: Docs and full build

**Files:** `CLAUDE.md` (migrated modules list + `people`), `docs/superpowers/handover/2026-11-25-modulith.md`. The handover update covers:
- plan 4 status, branch and commits
- rollout step "people" marked done
- the decisions above
- the branch staff API change for the PR
- what's still open for membership and activity (`User.level` formula, `UserTestMother.level()` reflection, legacy `@ManyToOne User` associations)
- the stale progress-ledger pointer fixed: rulings live in the final reports, since ledgers are temporary
- next action: plan 5 (payment)

- [ ] **Step 1: Update both docs.**
- [ ] **Step 2: Run the full build.** Command: `mvn clean package`. Expected: BUILD SUCCESS.
- [ ] **Step 3: Commit.** Message: `Update docs for people module`.
