package org.firststep.backend.category.selection;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Persisted actual catalog collisions; shuffling never adds invented dates or role evidence. */
class Im11RealCollisionRegressionTest {
    private final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    private JsonNode fixture() throws Exception {
        try(var input=getClass().getResourceAsStream("/im11/real-housing-collisions.json")) {
            assertNotNull(input);return mapper.readTree(input);
        }
    }
    private FeaturedItemSelector.Selection items(String name) throws Exception {
        return mapper.treeToValue(fixture().path("itemsByTopic").path(name),FeaturedItemSelector.Selection.class);
    }
    private FeaturedItemSelector.Selection order(FeaturedItemSelector.Selection recorded,List<FeaturedItemSelector.Candidate> candidates) {
        var initial=recorded.diagnostics().stream().filter(d->List.of("CLASSIFICATION_EVIDENCE_LIMITATION","FLYER_HELP_ACCESS_REQUIRED").contains(d.code())).toList();
        return FeaturedItemSelector.order("wilmington-de","housing",recorded.topic(),recorded.selectionDate(),candidates,initial);
    }
    @Test void im11T06RealFourWayHomeownershipDurableCollisionReachesCanonicalIdAndScopedDiagnostic() throws Exception {
        var recorded=items("Homeownership");
        assertEquals(List.of("HA-011","HA-013","HA-021","HA-022"),recorded.candidates().stream().map(c->c.id()).toList());
        for(var candidate:recorded.candidates()) {
            assertEquals(java.util.Set.of(FeaturedRoleQualification.Role.DURABLE),candidate.roles());
            assertTrue(candidate.actionable());assertNull(candidate.meaningfulDate());assertNull(candidate.catalogOrApprovalDate());
            assertTrue(candidate.limitations().contains("no_authentic_catalog_date:"+candidate.id()));
        }
        var result=order(recorded,recorded.candidates());
        assertEquals(List.of("HA-011","HA-013","HA-021"),result.positions().stream().map(p->p.item().id()).toList());
        assertEquals("STABLE_CANONICAL_ID",result.positions().get(0).tieStage());
        assertTrue(result.diagnostics().stream().anyMatch(d->d.code().equals("STABLE_ITEM_ID_FALLBACK")
                &&d.explanation().startsWith("DURABLE")&&d.itemIds().equals(List.of("HA-011","HA-013","HA-021","HA-022"))
                &&d.communityId().equals("wilmington-de")&&d.categoryKey().equals("housing")&&d.topic().equals("Homeownership")));
        assertEquals(recorded,result);
    }
    @Test void im11T03T07RealTopicTiesStrictTopFourCutoffAndAllEvidenceSurviveOneHundredShuffles() throws Exception {
        List<FeaturedTopicSelector.TopicEvidence> candidates=mapper.convertValue(fixture().path("topics"),new TypeReference<List<FeaturedTopicSelector.TopicEvidence>>(){});
        var expected=FeaturedTopicSelector.order("wilmington-de","housing",LocalDate.of(2026,10,16),candidates);
        assertEquals(List.of("transitional-housing","emergency-shelter","homeownership","rental-assistance"),expected.selected().stream().map(t->t.key()).toList());
        assertEquals(4,expected.candidates().get(3).actionableCount());assertEquals(3,expected.candidates().get(4).actionableCount());
        assertTrue(expected.diagnostics().stream().anyMatch(d->d.topicKeys().equals(List.of("homeownership","rental-assistance"))));
        assertTrue(expected.diagnostics().stream().anyMatch(d->d.topicKeys().equals(List.of("senior-housing","youth-housing"))));
        var shuffled=new ArrayList<>(candidates);
        for(int seed=0;seed<100;seed++){Collections.shuffle(shuffled,new Random(seed));assertEquals(expected,FeaturedTopicSelector.order("wilmington-de","housing",LocalDate.of(2026,10,16),shuffled));}
    }
    @Test void im11T05T07AllNineRealItemSelectionsAndFallbackCollisionsSurviveOneHundredShuffles() throws Exception {
        for(String name:List.of("Transitional Housing","Emergency Shelter","Homeownership","Rental Assistance","Sober Living","Senior Housing","Youth Housing","Public Housing","Eviction Prevention")) {
            var recorded=items(name);var shuffled=new ArrayList<>(recorded.candidates());
            for(int seed=0;seed<100;seed++){Collections.shuffle(shuffled,new Random(seed));assertEquals(recorded,order(recorded,shuffled),name);}
        }
    }
    @Test void im11T13RepeatedProjectionOfRealCanonicalIdCannotCreateExtraSlotsOrFalseRoles() throws Exception {
        var recorded=items("Homeownership");var repeated=new ArrayList<>(recorded.candidates());repeated.add(recorded.candidates().get(0));
        var result=order(recorded,repeated);assertEquals(recorded,result);assertEquals(4,result.candidates().size());
        assertEquals(List.of(FeaturedItemSelector.PositionType.DURABLE,FeaturedItemSelector.PositionType.FALLBACK,FeaturedItemSelector.PositionType.FALLBACK),result.positions().stream().map(p->p.type()).toList());
        assertEquals(1,items("Public Housing").positions().size());assertEquals(2,items("Youth Housing").positions().size());
        assertEquals(FeaturedItemSelector.PositionType.FALLBACK,items("Eviction Prevention").positions().get(0).type());
    }
}
