---
paths:
  - "src/test/**"
  - "src/main/kotlin/**"
---

# Testing rules

These rules apply whenever you write or change tests, and whenever you change production code, because that code needs tests too.
The existing tests were written before these rules. When you touch one of them, bring it in line.

## 1. What to test: aim for full coverage
- Every `@Usecase`, `*Service`, controller and entity method that has logic gets tests. Cover:
  - the happy path
  - **every branch** (each `if`/`when` arm and each `?.let`/`?:` fallback)
  - **every failure**: `check`, `require`, thrown exceptions, `LocalizedException`
  - boundaries: limits reached (`registrationLimit`), age edges, empty lists, `null` optionals, prices that get clamped to 0
- Bug fixes start with a failing test that reproduces the bug.
- A change to production code ships with its tests in the same commit.
- Don't test generated code (`be.sgl.backend.openapi.*` or the JAXB classes), plain getters and setters, or Spring itself.
- There's no coverage tool configured yet. Ask before adding JaCoCo, or any other new test dependency, to `pom.xml`.

## 2. Unit tests first
Prefer the cheapest test that proves the behavior:

| What | Test type |
|---|---|
| Entity/DTO logic, `util/*` functions | Plain unit test, no Spring and no mocks |
| `@Usecase` / `*Service` logic | Unit test with `@ExtendWith(MockitoExtension::class)`, `@Mock` collaborators and `@InjectMocks lateinit var testee` |
| Custom repository queries (`@Query`, derived queries with logic) | Repository slice (see §3) |
| Endpoint mapping, validation, security annotations, JSON shape | Web slice or `@IntegrationTest` (see §3) |
| Wiring across layers, transactions, mail, end-to-end flows | `@IntegrationTest` |

- Unit tests mock only direct collaborators. Never mock the class under test or entities. Build those with test mothers instead.
- Mockito runs with strict stubs, so don't leave unused stubbing behind.
- Make time deterministic: pass dates explicitly (many use cases take `at: LocalDate = LocalDate.now()`) instead of relying on `now()`.

## 3. Integration tests: keep the number of Spring contexts small
Each distinct context configuration starts a new Spring context and new containers, which is slow.
- **Every full-context test uses `@IntegrationTest`** (`src/test/kotlin/be/sgl/backend/util/IntegrationTest.kt`) and nothing else at class level.
- On individual test classes, **don't add** any of these, because each one forks a new context:
  - `@MockitoBean`, `@MockBean` or `@SpyBean`
  - `@TestPropertySource` or `@DynamicPropertySource`
  - `@DirtiesContext`
  - a different `@ActiveProfiles`
  - an extra `@Import`
- If an integration test needs a replacement bean (for example `MockedCheckoutProvider` for `CheckoutProvider`), put it in a shared test configuration that `@IntegrationTest` imports, so every integration test shares one context.
- Isolate tests through data, not through contexts. Each test creates the data it needs (with test mothers, saved via repositories) and must not depend on other tests or their order. Clean up in `@AfterEach`, or use `@Transactional` when the flow under test doesn't depend on commit behavior.
- **Slices:** when a narrower context is enough, define a **meta-annotation once** in `be.sgl.backend.util` and reuse it, just like `@IntegrationTest`:
  - `@RepositoryTest` = `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + `@ActiveProfiles("test")` + `@Import(DatabaseContainerConfiguration::class)`. There's no embedded DB on the classpath and the queries target MariaDB, so plain `@DataJpaTest` (as in `ActivityRepositoryTest`) doesn't work.
  - `@ControllerTest` = `@WebMvcTest` + the security config and the shared mocks the controllers need.

  Never put slice annotations directly on test classes. Only add a new slice when none of the existing ones fit.
- Security: authorization goes through `LevelSecurityService`, which looks up the JWT's username in the DB. Authenticate with spring-security-test's `jwt()` request post-processor, and make sure a user with that username and the right role exists. Test each `@OnlyStaff`/`@OnlyAdmin` endpoint for both the allowed and the denied case.

## 4. BDD style: names and structure
- Test class: `<ClassUnderTest>Test`, in the **same package as the production class**. For example, `ValidateAndCreateActivityRegistration` lives in `service.registrable.activity`, so its test belongs there too.
- The object under test is always called `testee`.
- Name test methods with Kotlin backtick names in the form `should <expected outcome> when <condition>`:
  ```kotlin
  @Test
  fun `should apply sibling discount when a sibling is already registered`() {
  ```
- Group scenarios per method or feature with `@Nested inner class` (for example `inner class WhenPeriodIsFull`) once a class has more than a handful of tests.
- The body always has `// given`, `// when` and `// then` sections, in that order, each with a blank line before it. Use one `// when` per test. If there are no preconditions, write `// given` anyway and leave it empty, or merge it as `// when / then` for one-line exception checks.
- Use BDDMockito so stubbing reads like the comments, and to avoid the backticked `` `when` ``:
  ```kotlin
  given(siblingRepository.getByUser(user)).willReturn(listOf(siblingRelation))   // given
  then(mailService).should().send(any())                                           // then
  then(registrationRepository).shouldHaveNoMoreInteractions()
  ```
- Assert with AssertJ: `assertThat(...)`, `assertThatThrownBy { ... }.isInstanceOf(...).hasMessageContaining(...)`, and `assertThatCode { ... }.doesNotThrowAnyException()`. Assert on the outcome (return values, saved state, interactions that matter), and assert one behavior per test.
- Derive expected values from the inputs (`activity.price - activity.siblingReduction`) instead of hard-coding unexplained numbers. If a literal has to stay, give it a name.

## 5. Test data: always use test mothers
- **Never create entities with their constructors or `apply { }` blocks in tests.** Use the test mothers in `src/test/kotlin/be/sgl/backend/entity/**`, for example `user().build()`, `activity().price(500.0).build()` and `membership().user(user).build()`. The same goes for non-trivial DTOs and requests.
- If a mother is missing, **create it** before writing the test (for example `SiblingRelation(user, sibling)` in `ValidateAndCreateActivityRegistrationTest` needs a `SiblingRelationTestMother`). Follow the existing conventions:
  - `object <Entity>TestMother` in the **entity's package**, with a factory method `fun <entity>() = <Entity>Builder()`
  - a builder with private `var` fields that have **valid, sensible defaults** (id `1`, a non-null name, nested entities built from their own mothers), so `build()` with no arguments gives a usable object
  - fluent setters `fun x(x: T) = apply { this.x = x }`, no-arg flag methods for booleans and statuses (`hasReduction()`, `active()`, `hidden()`), and `vararg` for collections
  - for inheritance hierarchies, self-typed abstract builders (`PayableBuilder<T, R>` with `self()` and `create()`), as in `PayableTestMother` and `PaymentTestMother`
  - `build()` sets every field and wires bidirectional relations (as `UserBuilder` does with `staffData.user`)
- In a test, set only the fields that matter to that test. Everything else stays at its default, which keeps the test focused on what it's about.
- Keep mothers correct. A wrong default or a wrong field mapping silently invalidates every test that uses it.

## 6. Running
- `mvn test -Dtest=<TestClass>` or `mvn test -Dtest='<TestClass>#<method name>'`. Integration and repository tests need Docker for Testcontainers.
- Run the affected tests before committing, and the full suite (`mvn test`) before saying a feature is done. Report failures honestly.
