---
description: Testing is mandatory. It is the primary mechanism for preventing regressions and validating behavior.
# applyTo: 'Development Workflow'. 
' # when provided, instructions will automatically be added to the request context when the pattern matches an attached file
---

Prefer Test-Driven Development when practical.
Write tests alongside implementation.
Execute tests automatically in builds and CI pipelines.
Add tests for every new feature, bug fix, and meaningful edge case.

# Test Design Structure

Use the AAA pattern: Arrange, Act, Assert

# Scope

Each unit test should validate one behavior.
Tests must be independent and order-agnostic.
Isolate external dependencies using mocks, stubs, or test doubles.
Test behavior through public APIs and observable outcomes.
Tests should be treated as production code.

# What to Test

Verify expected behavior with valid inputs. Fix failing tests immediately.
Verify expected failures, exceptions, and validation behavior.
Boundary Conditions

Test edge cases such as:
Empty collections
Null values
Minimum and maximum values
Single-element inputs

Avoid tests for:
Constants
Trivial getters and setters
Simple pass-through methods
Private methods directly

# Test Organization

Organize tests separately from production code.
Mirror the production package structure.
Place unit and integration tests in separate packages when appropriate.
Use clear, consistent naming conventions.
Ensure test locations are predictable and easy to navigate.

