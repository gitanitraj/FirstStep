/*
 * IM-01 annotated test reference — full mirror of backend/src/test/java/org/firststep/backend/shared/lifecycle/LifecycleFixture.java
 * WHAT: Lifecycle/projection regression evidence.
 * WHY: prove behavior using deterministic time and preserve unrelated test expectations.
 * INTERACTION: see individual test names and fixtures; only lifecycle dependencies were
 * added to existing service construction. No assertion was weakened to hide expiry failures.
 * ALTERNATIVE: machine-clock fixtures would make future runs depend on the date of execution.
 */
package org.firststep.backend.shared.lifecycle;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Stable test time for existing service fixtures; never consult the machine clock. */
public final class LifecycleFixture {
    private LifecycleFixture() {}

    public static EditorialLifecycle fixed() {
        return new EditorialLifecycle(Clock.fixed(Instant.parse("2026-10-15T16:00:00Z"), ZoneOffset.UTC),
                new EditorialExpiryConfiguration());
    }
}
