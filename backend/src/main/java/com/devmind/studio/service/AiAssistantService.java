package com.devmind.studio.service;

import com.devmind.studio.client.FastApiClient;
import com.devmind.studio.dto.AssistantDtos.RequirementDraftRequest;
import com.devmind.studio.dto.AssistantDtos.RequirementDraftResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class AiAssistantService {
    private final FastApiClient fastApiClient;
    private final ObjectMapper objectMapper;

    public AiAssistantService(FastApiClient fastApiClient, ObjectMapper objectMapper) {
        this.fastApiClient = fastApiClient;
        this.objectMapper = objectMapper;
    }

    public RequirementDraftResponse generateRequirementDraft(RequirementDraftRequest request) {
        String modelName = request.modelName() == null || request.modelName().isBlank() ? "deepseek-v4-flash" : request.modelName();
        RequirementDraftRequest normalized = new RequirementDraftRequest(
                request.projectTitle(),
                request.projectDescription(),
                request.techStack(),
                modelName
        );
        try {
            return fastApiClient.generateRequirementDraft(normalized);
        } catch (WebClientResponseException exception) {
            String body = exception.getResponseBodyAsString();
            if (body != null && !body.isBlank()) {
                throw new IllegalArgumentException(extractDetail(body));
            }
            throw exception;
        }
    }

    private String extractDetail(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.has("detail")) {
                return node.get("detail").asText();
            }
        } catch (Exception ignored) {
            return body;
        }
        return body;
    }
}
