/*
 * WHAT: Complete annotated mirror of FeaturedItemAssignmentTest.java.
 * WHY: IM-07 selects distinct item roles without changing resident eligibility or public DTOs.
 * INTERACTION: Current authored inventory intersects existing projections before role matching.
 * ALTERNATIVES: Greedy slots lose role coverage; weighted scores obscure approved tie stages.
 * TEST STRATEGY: Service fixtures exercise gates; exhaustive oracle checks assignments;
 * real catalog integration pins actual collisions. Production remains internal, read-only.
 */
package org.firststep.backend.category.selection;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.firststep.backend.category.selection.FeaturedRoleQualification.Role;
import org.firststep.backend.shared.dto.ContentItem;
import org.firststep.backend.shared.model.ContentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Exhaustive independent oracle for small inventories: coverage first, then role tuple. */
class FeaturedItemAssignmentTest {
    private FeaturedItemSelector.Candidate candidate(String id, Set<Role> roles) {
        return new FeaturedItemSelector.Candidate(new ContentItem(ContentType.RESOURCE, id, id, null,
                null, null, null, null, null, null, null),
                new FeaturedItemSelector.Provenance("fixture", null, null, null, null, null),
                true, roles, null, null, List.of("assignment_fixture"), List.of());
    }

    @Test void t11GlobalMaximumAndLexicographicTupleMatchExhaustiveOracleAcrossRoleGraphs() {
        Random random = new Random(7);
        for (int scenario = 0; scenario < 200; scenario++) {
            List<FeaturedItemSelector.Candidate> candidates = new ArrayList<>();
            for (int item = 0; item < 5; item++) {
                Set<Role> roles = EnumSet.noneOf(Role.class);
                for (Role role : Role.values()) if (random.nextBoolean()) roles.add(role);
                candidates.add(candidate("ID-" + item, roles));
            }
            int bestCount = -1; String bestTuple = null;
            for (int t = -1; t < 5; t++) for (int d = -1; d < 5; d++) for (int v = -1; v < 5; v++) {
                if ((t >= 0 && (t == d || t == v)) || (d >= 0 && d == v)) continue;
                if (t >= 0 && !candidates.get(t).roles().contains(Role.TIMELY)) continue;
                if (d >= 0 && !candidates.get(d).roles().contains(Role.DURABLE)) continue;
                if (v >= 0 && !candidates.get(v).roles().contains(Role.VISUAL)) continue;
                int count = (t >= 0 ? 1 : 0) + (d >= 0 ? 1 : 0) + (v >= 0 ? 1 : 0);
                String tuple = (t < 0 ? "~" : "ID-" + t) + "/" + (d < 0 ? "~" : "ID-" + d) + "/" + (v < 0 ? "~" : "ID-" + v);
                if (count > bestCount || (count == bestCount && tuple.compareTo(bestTuple) < 0)) { bestCount = count; bestTuple = tuple; }
            }
            Collections.shuffle(candidates, random);
            var actual = FeaturedItemSelector.order("fixture", "housing", "Emergency Shelter", LocalDate.of(2026, 10, 16), candidates, List.of());
            List<String> tuple = new ArrayList<>(); int count = 0;
            for (Role role : Role.values()) {
                var position = actual.positions().stream().filter(p -> p.type().name().equals(role.name())).findFirst();
                tuple.add(position.map(p -> p.item().id()).orElse("~")); if (position.isPresent()) count++;
            }
            assertEquals(bestCount, count, "scenario " + scenario); assertEquals(bestTuple, String.join("/", tuple), "scenario " + scenario);
            assertEquals(actual.positions().size(), actual.positions().stream().map(p -> p.item().id()).distinct().count());
        }
    }
}
