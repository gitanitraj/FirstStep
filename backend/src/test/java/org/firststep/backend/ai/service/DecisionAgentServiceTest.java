package org.firststep.backend.ai.service;

import org.firststep.backend.ai.dto.DecisionRequest;
import org.firststep.backend.ai.dto.DecisionResponse;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.shared.model.ContentSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DecisionAgentServiceTest {

    /** Fake AiAssistant that returns a canned raw response, bypassing any real provider. */
    static class FakeAiAssistant implements AiAssistant {
        private final String raw;
        FakeAiAssistant(String raw) {
            this.raw = raw;
        }
        @Override
        public String generate(String prompt, double temperature) {
            return raw;
        }
    }

    private DecisionAgentService serviceReturning(String rawModelOutput) {
        return new DecisionAgentService(
                true,                                  // aiEnabled
                () -> List.<Resource>of(),             // ResourceServiceLike
                () -> List.<NewsItem>of(),             // NewsServiceLike
                new FakeAiAssistant(rawModelOutput));
    }

    @Test
    void shouldSalvageCompleteStepsWhenModelOutputIsTruncatedMidArray() {
        // JSON cut off mid-third-step: the array and root object are never closed.
        String truncated =
                "{\"answerTitle\":\"Food Assistance\",\"steps\":[" +
                "{\"order\":1,\"title\":\"A\",\"action\":\"do a\",\"why\":\"because a\"}," +
                "{\"order\":2,\"title\":\"B\",\"action\":\"do b\",\"why\":\"because b\"}," +
                "{\"order\":3,\"title\":\"C\",\"action\":\"do c\",\"wh";

        DecisionResponse resp = serviceReturning(truncated).decide(new DecisionRequest());

        assertEquals("Food Assistance", resp.answerTitle);
        assertEquals(2, resp.steps.size(), "should recover the two complete steps");
        assertNotEquals("Unable to generate guidance", resp.answerTitle);
    }

    @Test
    void shouldParseWellFormedResponseUnchanged() {
        String good =
                "{\"answerTitle\":\"Housing Help\",\"steps\":[" +
                "{\"order\":1,\"title\":\"A\",\"action\":\"do a\",\"why\":\"because a\"}]," +
                "\"citations\":[],\"notes\":\"\"}";

        DecisionResponse resp = serviceReturning(good).decide(new DecisionRequest());

        assertEquals("Housing Help", resp.answerTitle);
        assertEquals(1, resp.steps.size());
    }

    private static Resource resourceWithSource(String id, String organization, String sourceName) {
        Resource resource = new Resource();
        resource.id = id;
        resource.organization = organization;
        ContentSource contentSource = new ContentSource();
        contentSource.name = sourceName;
        resource.contentSource = contentSource;
        return resource;
    }

    @Test
    void shouldAttachContentSourceToCitationWhenMatchingResourceExists() {
        Resource resource = resourceWithSource("CI-001", "Test Directory", "FIRST Community Services Directory");
        DecisionAgentService service = new DecisionAgentService(
                true,
                () -> List.of(resource),
                () -> List.<NewsItem>of(),
                new FakeAiAssistant(
                        "{\"answerTitle\":\"Test\",\"steps\":[]," +
                        "\"citations\":[{\"sourceType\":\"resource\",\"id\":\"CI-001\",\"label\":\"Test Directory\"}]," +
                        "\"notes\":\"\"}"));

        DecisionRequest request = new DecisionRequest();
        request.userQuery = "test directory"; // must score > 0 to be retrieved into topResources

        DecisionResponse resp = service.decide(request);

        assertEquals(1, resp.citations.size());
        assertNotNull(resp.citations.get(0).contentSource, "citation should be linked to the resource's real ContentSource");
        assertEquals("FIRST Community Services Directory", resp.citations.get(0).contentSource.name);
    }

    @Test
    void shouldLeaveCitationSourceNullWhenNoMatchingIdFound() {
        Resource resource = resourceWithSource("CI-001", "Test Directory", "FIRST Community Services Directory");
        DecisionAgentService service = new DecisionAgentService(
                true,
                () -> List.of(resource),
                () -> List.<NewsItem>of(),
                new FakeAiAssistant(
                        "{\"answerTitle\":\"Test\",\"steps\":[]," +
                        "\"citations\":[{\"sourceType\":\"resource\",\"id\":\"HALLUCINATED-ID\",\"label\":\"Made Up\"}]," +
                        "\"notes\":\"\"}"));

        DecisionRequest request = new DecisionRequest();
        request.userQuery = "test directory";

        DecisionResponse resp = service.decide(request);

        assertEquals(1, resp.citations.size());
        assertNull(resp.citations.get(0).contentSource, "no retrieved item matched this id, so contentSource must stay null");
    }
}
