package com.devmind.studio.client;

import com.devmind.studio.dto.AssistantDtos.RequirementDraftRequest;
import com.devmind.studio.dto.AssistantDtos.RequirementDraftResponse;
import com.devmind.studio.dto.AiDtos.KnowledgeIngestRequest;
import com.devmind.studio.dto.AiDtos.KnowledgeIngestResponse;
import com.devmind.studio.dto.AiDtos.LangfuseScoreRequest;
import com.devmind.studio.dto.AiDtos.RegenerateWorkflowRequest;
import com.devmind.studio.dto.AiDtos.StartOmniAgentRunRequest;
import com.devmind.studio.dto.AiDtos.StartOmniAgentRunResponse;
import com.devmind.studio.dto.AiDtos.StartWorkflowRequest;
import com.devmind.studio.dto.AiDtos.StartWorkflowResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class FastApiClient {
    private final WebClient webClient;
    private final String internalToken;

    public FastApiClient(WebClient.Builder builder,
                         @Value("${app.ai-service-base-url}") String baseUrl,
                         @Value("${app.internal-token}") String internalToken) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.internalToken = internalToken;
    }

    public StartWorkflowResponse startWorkflow(StartWorkflowRequest request) {
        return webClient.post()
                .uri("/ai/workflows/start")
                .header("X-Internal-Token", internalToken)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(StartWorkflowResponse.class)
                .block();
    }

    public RequirementDraftResponse generateRequirementDraft(RequirementDraftRequest request) {
        return webClient.post()
                .uri("/ai/assistants/requirement-draft")
                .header("X-Internal-Token", internalToken)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RequirementDraftResponse.class)
                .block();
    }

    public StartWorkflowResponse regenerateWorkflow(String aiTaskId, RegenerateWorkflowRequest request) {
        return webClient.post()
                .uri("/ai/workflows/{aiTaskId}/regenerate", aiTaskId)
                .header("X-Internal-Token", internalToken)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(StartWorkflowResponse.class)
                .block();
    }

    public void submitScore(LangfuseScoreRequest request) {
        try {
            webClient.post()
                    .uri("/ai/observability/score")
                    .header("X-Internal-Token", internalToken)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (RuntimeException ignored) {
        }
    }

    public StartOmniAgentRunResponse startOmniAgentRun(StartOmniAgentRunRequest request) {
        return webClient.post()
                .uri("/ai/agent-runs/start")
                .header("X-Internal-Token", internalToken)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(StartOmniAgentRunResponse.class)
                .block();
    }

    public KnowledgeIngestResponse ingestKnowledge(KnowledgeIngestRequest request) {
        return webClient.post()
                .uri("/ai/knowledge/ingest")
                .header("X-Internal-Token", internalToken)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(KnowledgeIngestResponse.class)
                .block();
    }
}
