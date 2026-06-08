package com.devmind.studio.dto;

import java.util.List;
import java.util.Map;

public class AiDtos {
    public record StartWorkflowRequest(
            Long projectId,
            Long taskId,
            String projectName,
            String requirementText,
            List<String> documents,
            String techStack,
            String modelName,
            String callbackUrl,
            Map<String, String> previousResults
    ) {}

    public record StartWorkflowResponse(String aiTaskId, String status) {}

    public record RegenerateWorkflowRequest(
            Long projectId,
            Long taskId,
            String projectName,
            String requirementText,
            List<String> documents,
            String techStack,
            String modelName,
            String callbackUrl,
            String targetAgent,
            Map<String, String> previousResults
    ) {}

    public record LangfuseScoreRequest(
            String traceId,
            String observationId,
            String name,
            Double value,
            String comment,
            Map<String, Object> metadata
    ) {}

    public record TaskEventRequest(
            String aiTaskId,
            Long taskId,
            Long projectId,
            String eventType,
            String agentType,
            String status,
            Integer progress,
            String contentJson,
            String contentMarkdown,
            String errorMessage,
            String modelName,
            String promptVersion,
            String langfuseTraceId,
            String traceObservationId,
            Integer promptTokens,
            Integer completionTokens,
            Integer latencyMs,
            Map<String, Object> metadata
    ) {}

    public record StartOmniAgentRunRequest(
            Long conversationId,
            Long runId,
            Long messageId,
            String question,
            String mode,
            Long knowledgeBaseId,
            List<String> documents,
            List<Map<String, Object>> files,
            String modelName,
            String callbackUrl
    ) {}

    public record StartOmniAgentRunResponse(String aiTaskId, String status) {}

    public record OmniAgentEventRequest(
            String aiTaskId,
            Long runId,
            Long conversationId,
            Long messageId,
            String eventType,
            String agentType,
            String status,
            Integer progress,
            String inputJson,
            String outputJson,
            String contentMarkdown,
            String citationsJson,
            String errorMessage,
            String modelName,
            String promptVersion,
            String langfuseTraceId,
            String traceObservationId,
            Integer promptTokens,
            Integer completionTokens,
            Integer latencyMs,
            Map<String, Object> metadata
    ) {}

    public record KnowledgeIngestRequest(Long knowledgeBaseId, Long documentId, String text) {}
    public record KnowledgeIngestResponse(String status, Integer chunkCount, String message) {}
}
