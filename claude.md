In our work together:

Divide complex tasks into smaller, manageable, sequential prompts rather than one large, ambiguous request.
Surface tradeoffs and ask clarifying questions before committing to a direction.
Treat this as a collaborative partnership where we iterate and refine together.

**Think Before Coding**

Don't assume. Don't hide confusion.
Before implementing:
State your assumptions explicitly. If uncertain, ask. If something is unclear, stop. Name what's confusing.
If multiple interpretations exist, present them - don't pick silently.
If a simpler approach exists, say so. Push back when warranted.

**Surgical Changes**

When editing existing code:
Don't "improve" adjacent code, comments, or formatting. If a simpler approach exists, say so. 
Don't refactor things that aren't broken.
Match existing style, even if you'd do it differently. Push back when warranted.
If you notice unrelated dead code, mention it - don't delete it. 
When your changes create orphans remove imports/variables/functions that YOUR changes made unused.

**Simplicity First**

Minimum code that solves the problem.
No abstractions for single-use code.
No "flexibility" or "configurability" that wasn't requested.
No error handling for impossible scenarios.
If you write 200 lines and it could be 50, rewrite it.

**Learning-First Development Philosophy**

**File Organization**
Follow standard Maven project structure:
src/
├── main/
│   └── java/
│       └── com/example/project/
└── test/
    └── java/
        └── com/example/project/

Test classes should mirror the package structure of the production code.
        Example:
            src/main/java/com/farm/ChickenCoop.java
            src/test/java/com/farm/ChickenCoopTest.java

**Naming Conventions**
Production Class:
    ChickenCoop

Test Class:
    ChickenCoopTest

Test Method:
    shouldAddChickenToCoopWhenChickenIsAdded()

**Reference Documentation Governance**

Throughout this project:

When solving a problem or discovering a new implementation approach, document the reasoning in comments or summary notes.

For complex code, object-oriented design decisions, architectural choices, and any new or modified source file, maintain a complete annotated reference copy in the references/ directory (for example, ChickenCoop_annotated.java).

Annotated reference copies should fully mirror the production source file rather than providing partial summaries.
Break annotated references into clearly labeled sections that explain:
    What the code is doing
    Why the implementation was chosen
    How the classes and methods interact
    Alternative approaches that were considered (when relevant)

**Annotated mirrors must match production before a slice is considered complete, but they do not have to be updated during unrelated feature work.**

The obligation is scoped to the files a slice touches. If a slice modifies a source file, that file's mirror must be in sync before the slice is done. Pre-existing drift in files the slice did not touch is NOT that slice's blocker — it is debt owned by whichever slice next touches those files. Report it; do not fix it opportunistically.

This replaces the earlier "synchronize immediately whenever production code changes," which in practice pulled unrelated files into every slice and made the mirrors a tax on all work rather than a record of the work being done.

Use the references/ directory consistently so it becomes a reliable long-term learning resource for understanding previous design decisions.

Document recurring patterns, common mistakes, debugging discoveries, and lessons learned so future development benefits from past experience.

When working with inheritance, interfaces, abstraction, polymorphism, collections, or design patterns, explain the object-oriented reasoning behind the solution.

When introducing new classes, explain their responsibilities and how they fit within the overall system design.

**Goal-Driven Execution**

**Testing Code is Essential**
Write reliable tests. Test often, test early.
Testing is how we verify that Java applications behave correctly, prevent regressions, and support confident refactoring.

**Structure and Scope**
AAA Pattern (Arrange, Act, Assert): Set up objects and test data, execute the method under test, verify the result.
One Thing Only: Each unit test should validate a single behavior or requirement.

Independent Tests: Tests should run successfully in any order and never depend on other tests.

Isolate Dependencies: Use mocks, stubs, or fakes (such as Mockito) to isolate code from databases, web services, file systems, and other external dependencies.

Test Public Behavior: Verify what a class does through its public API rather than its internal implementation.

**Testing Scenarios (What to Test)**
Positive Scenarios: Verify correct behavior when valid inputs are provided.
Negative Scenarios: Verify that invalid inputs are handled correctly and expected exceptions are thrown.

**Negative tests must verify the intended failure path, not merely assert that invalid input produces some failure.** A green negative test only tells you that *something* rejected the input — not that the rule you were testing is the thing that rejected it. When validation rules overlap, isolate the rule under test or assert its specific diagnostic (the exact message, the exact exception type, the exact status code).

This is not hypothetical. A validator rule shipped dead in Slice I and its own negative test passed anyway: an earlier rule caught the bad input first, the exit code was non-zero as expected, and the rule under test never ran. The test asserted failure, not the failure it was written for.

Boundary Conditions: Test edge cases such as empty collections, zero values, maximum values, and null references when applicable.

Object Relationships: Verify associations between objects, collection management, inheritance behavior, and composition relationships.


State Changes: Verify that object state changes correctly after method execution.

**Test Quality and Maintenance**
Fast & Reliable: Unit tests should execute quickly and produce consistent results.

Descriptive Naming: Use clear names that explain the expected behavior.
    Examples:
        shouldReturnAllVehiclesWhenGetVehiclesIsCalled()

        shouldAddChickenToCoopWhenChickenIsAdded()

        shouldThrowExceptionWhenNullVehicleIsAdded()

Treat Tests as Production Code. Follow clean code principles in test classes.

Avoid Complex Logic in Tests: Keep test methods simple and easy to understand.

One Assertion Theme Per Test: Focus each test on a single expected outcome.

**Workflow**
Test-Driven Development (TDD): Consider writing the test before implementing the feature.
Run Tests Frequently: Execute tests after each meaningful change.
Fix Failing Tests Immediately: A failing test should be investigated and resolved before continuing development.
Test New Features and Bug Fixes: Every new requirement or bug fix should include corresponding tests.
Refactor with Confidence: Comprehensive tests allow code improvements without fear of breaking functionality.

**JUnit Best Practices**
Use JUnit 5 annotations:
@BeforeEach
void setUp() {
    coop = new ChickenCoop();
}

@Test
void shouldAddChickenToCoopWhenChickenIsAdded() {
    Chicken chicken = new Chicken();

    coop.add(chicken);

    assertTrue(coop.getAnimals().contains(chicken));
}

**Exception Testing**
@Test
void shouldThrowExceptionWhenChickenIsNull() {
    assertThrows(
        IllegalArgumentException.class,
        () -> coop.add(null)
    );
}

**What Not to Test**
Simple getters and setters with no logic.
Constants.
Trivial constructors that only assign fields.
Private methods directly.
Java library functionality (e.g., ArrayList, HashMap, String).
Instead, test the public behavior that uses those components.
If you write a new class, method, or feature, there probably should usually be a JUnit test. If you cannot easily test your code, consider simplifying the design or reducing coupling between classes.

**Object-Oriented Design Testing**
For Java applications, verify:
Encapsulation is preserved.
Inheritance behaves correctly.
Interfaces are implemented properly.
Polymorphism works as expected.
Collections contain the correct objects.
Domain objects maintain valid state.
    Example:
        @Test
            void shouldStoreTractorInFarmVehicles() {
                Tractor tractor = new Tractor();

                farm.addVehicle(tractor);

                assertTrue(farm.getVehicles().contains(tractor));
            }

**Continuous Integration**
Run all tests before committing code.
Ensure the project builds successfully with Maven or Gradle.
Keep the test suite green at all times.
Add regression tests whenever a bug is discovered and fixed.

**Kanban Card and GitHub Project Automation Prompts**

When managing project tasks, always clarify user intent before acting

**Kanban Card Conversion Flow**

1. **Prompt:** "Would you like me to convert these checklist tasks to Kanban cards?"
   - If yes: Convert each checklist item into a main issue (epic) and sub-issue cards as appropriate.
   - If no: Do not create cards; ask if the user wants a summary or export instead.

2. **Prompt:** "Would you like me to upload these cards directly to your GitHub repo?"
   - If yes: Ask for the GitHub repository URL (e.g., `https://github.com/owner/repo`).
   - If no: Offer to export the cards as markdown, CSV, or another format.

3. **Prompt:** If the site is blocked by sign-in (e.g., GitHub login required):
   - Explain the authentication barrier to the user.
   - Walk the user through the manual sign-in process:
     - Instruct them to open the site in their browser and sign in.
     - After sign-in, return to the automation flow.
   - If automation is not possible, offer a manual step-by-step guide for creating cards/issues.

**Reference for Future Automation**

Use this prompt sequence whenever converting tasks/checklists to Kanban or GitHub issues:
- Always confirm before uploading or modifying a remote repository.
- If blocked by authentication, pause and guide the user through sign-in, then resume.
- Document any manual steps taken for traceability.

**Best Practices**

- Process one epic at a time; reload the page between epics to get a clean DOM state.
- Sub-issues already converted disappear from the checklist automatically — the same label will not be found twice.
- After finishing all epics, scan all pages again for duplicates introduced during the batch conversion run.

