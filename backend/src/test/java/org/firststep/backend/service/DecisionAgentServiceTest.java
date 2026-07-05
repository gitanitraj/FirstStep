package org.firststep.backend.service;

import org.firststep.backend.dto.DecisionRequest;
import org.firststep.backend.dto.DecisionResponse;
import org.firststep.backend.model.NewsItem;
import org.firststep.backend.model.Resource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DecisionAgentServiceTest {

    /** Fake Ollama that returns a canned raw response, bypassing the network. */
    static class FakeOllama extends OllamaService {
        private final String raw;
        FakeOllama(String raw) {
            super("http://unused", "fake-model");
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
                new FakeOllama(rawModelOutput));
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
}
