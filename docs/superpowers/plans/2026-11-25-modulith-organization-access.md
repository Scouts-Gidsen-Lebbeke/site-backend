# Modulith Organization + Access Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract the `organization` module (organizations, branches, role definitions) and the `access` module (user↔role assignment), with `UserRole` holding IDs only and `User.roles` gone.

**Architecture:** Same module layout as plan 1: the root package is the public API, and `web` / `application` / `domain` are internal. `ModularityTest` changes from "fail on any violation that names a strict module" to "fail on any violation whose **source** is a strict module, and on any cycle through a strict module". Legacy layer packages may keep using `organization` and `access` internals until their own rollout step converts them to IDs. That tolerance only applies to code in legacy packages. Strict modules never depend on legacy code or on another module's internals.

**Tech Stack:** Kotlin, Spring Boot 4.1, Spring Modulith 2.1.3, Hibernate (`@Formula`, `@IdClass`), MariaDB, JUnit 5, Mockito (BDD), AssertJ, Testcontainers.

**Spec:** `docs/superpowers/specs/2026-11-25-modulith-ddd-design.md` (rollout step 2). Read `docs/superpowers/handover/2026-11-25-modulith.md` too.

## Global Constraints

- No code comments except `// TODO:`.
- Tests follow `.claude/rules/testing.md`: backtick `should … when …` names, `// given` / `// when` / `// then`, BDDMockito, AssertJ, test mothers, and no per-class `@MockitoBean` or extra contexts. Test classes sit in the production class's package, and mothers move with their entity into `<module>/domain`.
- The REST contract stays unchanged except for the one change listed in Task 5. URLs, DTO field names, status codes and security annotations stay as they are.
- No schema change. FK columns (`branch_id`, `staff_branch_id`, `user_id`, `role_id`) keep their constraints, and only the JPA mapping changes. No new Flyway migration.
- `organization` → `allowedDependencies = ["shared"]`. `access` → `allowedDependencies = ["shared", "organization"]`.
- Legacy packages may use `organization`/`access` internals. Prefer the root APIs where they exist, and don't add new legacy usages of internals beyond what this plan lists.
- Maven: `~/.m2/wrapper/dists/apache-maven-3.9.11-bin/*/apache-maven-3.9.11/bin/mvn.cmd`. Docker must be running. Run `mvn clean` after package moves, because stale classes in `target/` cause false results.
- Branch `refactor/modulith-organization-access`, based on `refactor/modulith-foundation`. Stage new files immediately and never push.
- Source files are CRLF. For package moves, use IntelliJ (MCP `rename_refactoring` / move) or a script that rewrites `package` and `import` lines. Watch for star imports.

## Decisions made in this plan (open to owner correction)

- **Legacy → module tolerance:** chosen by the owner on 2026-11-25 to keep this step's PR small.
- **`Address`, `AddressDTO`, `AddressRepository` and `Sex` go to `shared`, not `people`.** Organization, calendar, activity, contact and user all use them, and `organization` (strict) cannot depend on legacy code.
- **The branch-staff API change moves to the people step.** Staff listings need user data (names, totem, nicknames), and `access` has no people API to get it from yet. `GET /branches/{id}` keeps embedding `staff`, served by a legacy controller.
- **`User.level` becomes a read-only `@Formula`** over `user_role` ⋈ `role` until the people step, where `access` takes over `UserLevelProvider`. This avoids changing `UserDTO.of` and its callers twice.
- **Role deletion uses a synchronous event.** `organization` publishes `RoleDeleting(roleId)` before deleting the role, and `access` deletes the assignments in the same transaction with a plain `@EventListener`. A synchronous listener is needed because the `user_role.role_id` FK would reject an after-commit listener. No event registry is needed yet.
- `OrganizationApi` isn't added yet. Legacy belcotax and certificate code keeps using `OrganizationProvider` (tolerated), and their own steps add read models. `GetRepresentativeForOwner`, `FindExternalFunctions`, `Representative`, `ExternalFunction`, `UserRoleDTO`, `StaffLinkRequest`, `StaffDTO` and `BranchWithStaff` stay in their legacy packages.

## Review Focus

1. **Deleting a role that users hold.** The role and its `user_role` rows go away in one transaction, with no FK error. Pinned in `RoleControllerTest` (Task 3, legacy listener) and again in Task 4 (access listener).
2. **Revoking the last admin role, or a member role,** is still refused with the existing messages ("At least one admin should exist!", "Member roles can't be manually deassigned!"). Pinned in `RevokeRoleTest` (Task 4).
3. **Assigning a role the user already holds** still gives 400 "User already has this role!". Pinned in `AssignRoleTest` and `UserRoleServiceTest` (Task 4).
4. **A user's level right after a role change in the same request.** The `@Formula` value is read at load time, so the first user of an internal install must come back from `CheckForInitialRunInternal` with level `ADMIN`. Pinned in `CheckForInitialRunInternalTest` (Task 4) as an `@IntegrationTest` case.
5. **`GET /branches/{id}` after the split.** It returns staff, hides staff when `STAFF_VISIBLE` is false, and returns 404 for an unknown id. Pinned in `BranchStaffControllerTest` (Task 3, re-run in Task 4).

---

### Task 1: Tolerate legacy-sourced violations in `ModularityTest`

**Files:**
- Create: `src/test/kotlin/be/sgl/backend/ViolationFilter.kt`
- Modify: `src/test/kotlin/be/sgl/backend/ModularityTest.kt`
- Test: `src/test/kotlin/be/sgl/backend/ViolationFilterTest.kt`
- Modify: `CLAUDE.md` ("Boundaries are tested" bullet)

**Interfaces:**
- Produces: `object ViolationFilter { fun isOffending(message: String, strictModules: Collection<String>): Boolean }`. `ModularityTest` uses it in place of `String.involves`.

- [ ] **Step 1: Capture real messages.** Add three temporary violations, run `mvn test -Dtest=ModularityTest`, copy the three message texts verbatim and then remove the violations:
  - a legacy class referencing `be.sgl.backend.news.domain.NewsItem`
  - a `news` class referencing a legacy entity
  - a `news` ↔ legacy cycle
- [ ] **Step 2: Write the failing test** `ViolationFilterTest`, using the captured texts as constants:
  - `should flag violation when a strict module is the source`
  - `should flag cycle when it passes through a strict module`
  - `should tolerate violation when a legacy module depends on a strict module's internals`
  - `should tolerate violation when only legacy modules are involved`
- [ ] **Step 3: Run it.** Command: `mvn test -Dtest=ViolationFilterTest`. Expected: FAIL (`ViolationFilter` doesn't exist).
- [ ] **Step 4: Implement `ViolationFilter.isOffending`.**
  - The source is the module named in the message's leading `Module '<name>'`.
  - A cycle message (one listing `Slice <name>` entries) is offending when any slice is strict.
  - Base both rules on the captured wording, not on guesses.
- [ ] **Step 5: Run.** Command: `mvn test -Dtest=ViolationFilterTest+ModularityTest`. Expected: PASS.
- [ ] **Step 6: Update CLAUDE.md.** Change the rule sentence to: "fails on any Modulith violation whose source is a module in `STRICT_MODULES` and on any cycle through one; legacy packages may still use strict modules' internals until their own step".
- [ ] **Step 7: Commit.** Message: `Only fail modularity on strict-sourced violations`.

---

### Task 2: Move `Address` and `Sex` into `shared`

**Files:**
- Move: `entity/Address.kt`, `dto/AddressDTO.kt`, `repository/AddressRepository.kt` → `shared/address/` (package `be.sgl.backend.shared.address`)
- Move: `entity/user/Sex.kt` → `shared/Sex.kt` (package `be.sgl.backend.shared`)
- Modify: every importer (about 25 files, main and test). Find them with `grep -rl 'entity.Address\b\|dto.AddressDTO\|repository.AddressRepository\|entity.user.Sex'`.
- Move: any `AddressTestMother` next to the new package

**Interfaces:**
- Produces: `be.sgl.backend.shared.address.Address`, `.AddressDTO`, `.AddressRepository`, `be.sgl.backend.shared.Sex`. Task 3 depends on these.

- [ ] **Step 1: Move the files** and rewrite their imports. This is a pure move with no behaviour change.
- [ ] **Step 2: Verify.** Command: `mvn clean test -Dtest=ModularityTest` (expected: PASS, so `shared` still has no domain dependency), then `mvn test`. Expected: all green.
- [ ] **Step 3: Commit.** Message: `Move Address and Sex into shared`.

---

### Task 3: Extract the `organization` module

**Files (new paths under `src/main/kotlin/be/sgl/backend/organization/`):**
- Create: `OrganizationModule.kt` (`allowedDependencies = ["shared"]`), `RoleApi.kt`, `RoleSummary.kt`, `BranchApi.kt`, `RoleDeleting.kt`
- Move to `domain/`: `Organization`, `OrganizationType`, `ContactMethod`, `ContactMethodType`, `Branch` (plus `isSameAs`), `BranchStatus`, `Role`, `OrganizationRepository`, `BranchRepository`, `RoleRepository`
- Move to `application/`: `OrganizationService`, `OrganizationProvider`, `OwnerOrganizationSenderProvider`, `BranchService`, `RoleService` (role definitions only)
- Create in `application/`: `RoleApiImpl`, `BranchApiImpl`
- Move to `web/`: `OrganizationController`, `BranchController` (without `GET /{id}`), `RoleController` (without `/functions` and `/paid-functions`), `OrganizationDTO`, `ContactMethodDTO`, `CreateOrUpdateOrganizationRequest`, `BranchDTO`, `BranchBaseDTO`, `CreateOrUpdateBranchRequest`, `RoleDTO`, `MemberRoleChangeRequest`, `StaffRoleChangeRequest`
- Create (legacy):
  - `controller/branch/BranchStaffController.kt`: `GET /branches/{id}`, `@Public`, returns `BranchWithStaff`
  - `service/branch/GetBranchWithStaff.kt` (`@Usecase`)
  - `controller/role/ExternalFunctionController.kt`: `@RequestMapping("/roles")`, the two function endpoints, unchanged
  - `service/role/UserRoleService.kt`: `markUserAsStaff`, `markUserAsAdmin`, `deassignRoleFromUser`, `getUserRolesByRole`, moved from `RoleService` unchanged
  - `service/role/DeleteUserRolesOnRoleDeleting.kt`: `@EventListener` that calls `userRoleRepository.deleteUserRolesByRole`
- Modify: `UserRoleController` (now uses `UserRoleService`), `GetCurrentValidBranchesForUser` (uses `matches`), `ModularityTest.STRICT_MODULES` (+ `"organization"`)
- Tests: move the tests and mothers for the moved classes into the matching `organization/...` packages (`BranchTest`, `BranchTestMother`, `OrganizationTest`, `OrganizationTestMother`, `RoleTest`, `RoleTestMother`, `OrganizationServiceTest`, `OrganizationProviderTest`, `OwnerOrganizationSenderProviderTest`, `BranchServiceTest`, `RoleServiceTest`, `CreateOrUpdateBranchRequestTestMother`, `RoleChangeRequestTestMother`). Split `RoleServiceTest` along the method split into `RoleServiceTest` + legacy `UserRoleServiceTest`.
- New tests: `RoleApiImplTest`, `BranchApiImplTest`, `GetBranchWithStaffTest`, `organization/web/RoleControllerTest` (`@IntegrationTest`), `controller/branch/BranchStaffControllerTest` (`@IntegrationTest`)

**Interfaces:**
- Consumes: `shared.address.*` and `shared.Sex` (Task 2).
- Produces (root package `be.sgl.backend.organization`):
  ```kotlin
  data class RoleSummary(val id: Int, val name: String, val level: RoleLevel, val branchId: Int?, val staffBranchId: Int?,
                         val externalId: String?, val backupExternalId: String?) {
      val forExternalSync: Boolean   // same rules as Role
      val memberRole: Boolean
      val staffRole: Boolean
      val adminRole: Boolean
  }
  interface RoleApi {
      fun findRoles(ids: Collection<Int>): List<RoleSummary>
      fun findStaffRoles(branchId: Int): List<RoleSummary>
  }
  interface BranchApi {
      fun isActive(branchId: Int): Boolean
  }
  data class RoleDeleting(val roleId: Int)
  ```
- `Branch.matchesUser(user: User)` becomes `Branch.matches(age: Int, sex: Sex): Boolean`. The caller passes `user.getAge() + user.ageDeviation` and `user.sex`.
- `Branch.staff` (`@Transient`) is removed.
- `RoleService.deleteRole(id)` checks "Admin roles can't be deleted!", publishes `RoleDeleting(id)` through `ApplicationEventPublisher`, then deletes the role.
- `RoleRepository` gains `fun findByStaffBranchId(branchId: Int): List<Role>`.
- `GetBranchWithStaff.execute(id: Int): BranchWithStaff` gets the branch from `BranchService`, keeps the `STAFF_VISIBLE` setting check, and loads staff with `userRepository.getStaffForBranch(branchId: Int)`. Change that query to compare `r.staffBranch.id = :branchId`. `BranchWithStaff.of(branch: BranchDTO, staff: List<StaffDTO>)`. `StaffDTO.of(user, branchId: Int)` looks the nickname up by `key.id == branchId`.

- [ ] **Step 1: Write the failing tests:**
  - `RoleApiImplTest`
    - `should map role fields when finding roles by id`
    - `should return all staff roles when a branch has several`
  - `BranchApiImplTest`
    - `should report active when branch status is ACTIVE`
    - `should report inactive when branch is unknown`
  - `RoleSummary` unit tests mirroring `RoleTest` for the four flags
  - `RoleControllerTest`
    - `should delete role and its user roles when an admin deletes an assigned role` (assign via `UserRoleRepository`, then expect 200 and no `user_role` rows)
    - `should return 403 when a non-admin deletes a role`
  - `BranchStaffControllerTest`
    - `should embed staff when staff is visible`
    - `should omit staff when STAFF_VISIBLE is false`
    - `should return 404 when branch id is unknown`
  - `BranchTest` covers `matches` (the age edges and sex `null` / mismatch carried over from the `matchesUser` tests)
- [ ] **Step 2: Run.** Command: `mvn test -Dtest='RoleApiImplTest,BranchApiImplTest,RoleControllerTest,BranchStaffControllerTest'`. Expected: FAIL (classes missing).
- [ ] **Step 3: Move and create the files** listed above. Add `"organization"` to `STRICT_MODULES`.
- [ ] **Step 4: Verify the boundary.** Command: `mvn clean test -Dtest=ModularityTest`. Expected: PASS. A failure means `organization` still reaches into legacy code (for example `User`, `UserRepository`, `SettingApi` left in `BranchService`). Fix the dependency, and don't widen `allowedDependencies`.
- [ ] **Step 5: Run the full suite.** Command: `mvn test`. Expected: all green, with the new tests passing.
- [ ] **Step 6: Commit.** Message: `Extract organization module`.

---

### Task 4: Extract the `access` module and drop `User.roles`

**Files (new paths under `src/main/kotlin/be/sgl/backend/access/`):**
- Create: `AccessModule.kt` (`allowedDependencies = ["shared", "organization"]`), `AccessApi.kt`
- Create: `domain/UserRole.kt`, which replaces `entity/role/UserRole.kt` on table `user_role`:
  - `@IdClass(UserRole.Key::class)`
  - `@Id userId: Int`, `@Id roleId: Int`
  - extends `Auditable`
  - `Key(userId, roleId) : Serializable`
- Create: `domain/UserRoleRepository.kt` (`JpaRepository<UserRole, UserRole.Key>`), which replaces `repository/role/UserRoleRepository.kt`
- Create: `application/AccessService.kt` (implements `AccessApi`), `application/AssignRole.kt`, `application/RevokeRole.kt`, `application/FindStaffBranchForUser.kt` (replaces `service/role/FindStaffBranchForUser.kt`), `application/DeleteAssignmentsOnRoleDeleting.kt` (replaces the Task 3 legacy listener)
- Modify (legacy):
  - `entity/user/User.kt`
    - remove `roles`
    - add `@Formula("(select coalesce(max(r.level), 0) from user_role ur join role r on r.id = ur.role_id where ur.user_id = id)") private var levelOrdinal: Int = 0`
    - `val level: RoleLevel get() = RoleLevel.entries[levelOrdinal]`
    - add `// TODO: read the level from UserLevelProvider once people is extracted`
  - `service/user/UserDataProvider.kt`
    - `startRole(user: User, role: Role): Boolean` calls `accessApi.assign` and keeps the warn log when already assigned
    - `endRole(user: User, role: Role)` calls `accessApi.revoke`
  - `ExternalUserDataProvider` start/end overrides: same signatures, same Groepsadmin side effects
  - `service/role/UserRoleService.kt`
    - `assignRole` returns `UserRoleDTO.of(user, role)` and keeps throwing `checkNotNull(...) { "User already has this role!" }` when `startRole` returns false
    - `deassignRoleFromUser` per Task 5
  - `dto/role/UserRoleDTO.kt`: `of(user: User, role: Role)`
  - `MembershipService.handlePaymentPaid`: no logic change, only the new `startRole` return type
  - `service/user/inital/CheckForInitialRunInternal.kt`: assign through `accessApi.assign(user.id!!, adminRole.id!!)`, then `entityManager.refresh(user)` so `level` is `ADMIN`
  - `service/user/sync/CreateUserForExternalMember.kt`: `translateFunction` returns `Role?`, and roles are assigned through `accessApi.assign` after `userRepository.save`
  - `service/user/sync/AcceptExternalMembershipRequest.kt`, `CheckOutOfSyncExternalFunctions.kt`: use `roleApi.findRoles(accessApi.roleIdsOf(user.id!!))` (`RoleSummary` has every field they read)
  - `service/user/sync/EndExternalFunctions.kt`: the new root admin comes from `accessApi.userIdsWithRole(adminRole.id!!)` + `userRepository.findAllById`
  - `repository/user/UserRepository.kt`: remove `getStaffForBranch`
  - `service/branch/GetBranchWithStaff.kt`: staff = `userRepository.findAllById(accessApi.staffUserIdsOf(id))`
  - `controller/role/UserRoleController.kt`: `/staff-branch` resolves username → `user.id`, then `accessApi.staffBranchIdOf(userId)`, then `BranchService` → `BranchDTO`. It returns 204 when there's none.
  - delete `service/role/DeleteUserRolesOnRoleDeleting.kt`
- Modify: `ModularityTest.STRICT_MODULES` (+ `"access"`)
- Tests:
  - move `UserRoleTestMother` to `access/domain` (builder with `userId`, `roleId`)
  - update `UserTestMother`, `RoleTestMother` and `PersistedUsers` to assign roles through `UserRoleRepository` after saving
  - move `FindStaffBranchForUserTest` to `access/application` and rewrite it against `RoleApi` / `BranchApi` mocks
  - update the sync, provider and initial-run tests for the new signatures

**Interfaces:**
- Consumes: `RoleApi`, `RoleSummary`, `BranchApi`, `RoleDeleting` (Task 3).
- Produces (root package `be.sgl.backend.access`):
  ```kotlin
  interface AccessApi {
      fun roleIdsOf(userId: Int): List<Int>
      fun userIdsWithRole(roleId: Int): List<Int>
      fun staffUserIdsOf(branchId: Int): List<Int>   // holders of any role in roleApi.findStaffRoles(branchId)
      fun staffBranchIdOf(userId: Int): Int?         // first staffBranchId among the user's roles whose branch isActive
      fun assign(userId: Int, roleId: Int): Boolean  // false when already assigned
      fun revoke(userId: Int, roleId: Int)
  }
  ```
- `RevokeRole.execute(userId, roleId)`:
  - throws `UserRoleNotFoundException` when there's no such assignment
  - `check(!role.memberRole) { "Member roles can't be manually deassigned!" }`
  - `check(!role.adminRole || userIdsWithRole(roleId).any { it != userId }) { "At least one admin should exist!" }`
  - then deletes the assignment

- [ ] **Step 1: Write the failing tests:**
  - `AssignRoleTest`
    - `should save assignment when user lacks the role`
    - `should return false when user already has the role`
  - `RevokeRoleTest`
    - `should delete assignment when revoking a staff role`
    - `should refuse when revoking a member role`
    - `should refuse when revoking the last admin`
    - `should allow when another admin exists`
    - `should throw not found when assignment is missing`
  - `FindStaffBranchForUserTest`
    - `should return active staff branch`
    - `should skip inactive branch`
    - `should return null when user has no staff role`
  - `AccessServiceTest`: `staffUserIdsOf` across several staff roles
  - `DeleteAssignmentsOnRoleDeletingTest`
  - `CheckForInitialRunInternalTest`: `should return first user with admin level when no users exist` (`@IntegrationTest` case, Review Focus 4)
  - `UserTest`: `level` is no longer computed in memory, so replace its level tests with a `UserRepository` query test that asserts the `@Formula` value for GUEST, STAFF and ADMIN holders. Put it in the repository slice if one exists, otherwise in an `@IntegrationTest`.
- [ ] **Step 2: Run.** Command: `mvn test -Dtest='AssignRoleTest,RevokeRoleTest,FindStaffBranchForUserTest,AccessServiceTest'`. Expected: FAIL.
- [ ] **Step 3: Implement** the access module and update the legacy callers listed above. Add `"access"` to `STRICT_MODULES`.
- [ ] **Step 4: Verify.** Command: `mvn clean test -Dtest=ModularityTest`. Expected: PASS. Then run `grep -rn '\.roles\b' src/main/kotlin`. Expected: no `User.roles` usages left.
- [ ] **Step 5: Run the full suite.** Command: `mvn test`. Expected: all green, including `RoleControllerTest` and `BranchStaffControllerTest` from Task 3.
- [ ] **Step 6: Commit.** Message: `Extract access module with ID-only user roles`.

---

### Task 5: Fix role revocation endpoint (API change)

`DELETE /user-roles/{id}` cannot work today:
- `UserRole` has a composite key (`user_id`, `role_id`), but the repository is typed `JpaRepository<UserRole, Int>`.
- `UserRoleDTO.id` is always `null`, so clients never get an id to send.

This task replaces it. **The owner must confirm this API change at plan review.** If they don't, drop the task and leave the endpoint as it is.

**Files:**
- Modify: `controller/role/UserRoleController.kt`: `DELETE /user-roles/role/{roleId}/user/{username}`, `@OnlyAdmin`
- Modify: `service/role/UserRoleService.kt`: `deassignRoleFromUser(roleId: Int, username: String)` resolves the user and role, then calls `userDataProvider.endRole(user, role)`
- Test: `controller/role/UserRoleControllerTest.kt` (`@IntegrationTest`)

- [ ] **Step 1: Write the failing tests:**
  - `should remove staff role when admin deassigns it` (200, assignment gone)
  - `should return 400 when deassigning the last admin`
  - `should return 403 when a non-admin deassigns`
  - `should return 404 when the user does not hold the role`
- [ ] **Step 2: Run.** Command: `mvn test -Dtest=UserRoleControllerTest`. Expected: FAIL.
- [ ] **Step 3: Implement** the new endpoint and remove the old one.
- [ ] **Step 4: Run.** Command: `mvn test -Dtest=UserRoleControllerTest`. Expected: PASS.
- [ ] **Step 5: Commit.** Message: `Deassign user roles by role and username`. The body states the API change for the PR.

---

### Task 6: Docs and full build

**Files:**
- Modify: `CLAUDE.md` (migrated modules list: `shared`, `setting`, `news`, `organization`, `access`)
- Modify: `docs/superpowers/handover/2026-11-25-modulith.md`: plan 2 status, the decisions above, and the people step now carrying the branch-staff API change, `UserLevelProvider` → `access` and removal of the `User.level` formula

- [ ] **Step 1: Update both docs.**
- [ ] **Step 2: Run the full build.** Command: `mvn clean package`. Expected: BUILD SUCCESS. If it's killed for low memory, rerun it in the foreground and don't report success without a green run.
- [ ] **Step 3: Commit.** Message: `Update docs for organization and access modules`.
