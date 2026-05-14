package com.leadspark.ai;

import org.springframework.stereotype.Component;

@Component
public class MockAiProvider implements AiProvider {
    @Override
    public ChatResult chat(ChatRequest request) {
        return new ChatResult("Mock AI response: " + request.prompt(), "mock", 0, 0);
    }
}
