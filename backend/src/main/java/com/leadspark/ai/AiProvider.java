package com.leadspark.ai;

public interface AiProvider {
    ChatResult chat(ChatRequest request);

    record ChatRequest(String prompt, String model) {
    }

    record ChatResult(String content, String model, int inputTokens, int outputTokens) {
    }
}
