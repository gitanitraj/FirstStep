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
