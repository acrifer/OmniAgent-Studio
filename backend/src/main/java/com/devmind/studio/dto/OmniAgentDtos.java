package com.devmind.studio.dto;

import com.devmind.studio.entity.*;

import java.util.List;
import java.util.Map;

public class OmniAgentDtos {
    public record CreateConversationRequest(String title, String mode, Long knowledgeBaseId, String modelName) {}
    public record CreateMessageRequest(String content, String metadataJson) {}
    public record StartAgentRunRequest(Long conversationId, Long messageId, String question, String mode, Long knowledgeBaseId, String modelName) {}
    public record CreateKnowledgeBaseRequest(String name, String description) {}
    public record ModelProviderRequest(String provider, String modelName, String baseUrl, Boolean enabled, String configJson) {}
    public record ToolConfigRequest(String toolType, String name, String endpoint, Boolean enabled, String configJson) {}
    public record SkillConfigRequest(String name, String description, Boolean enabled, String configJson) {}
    public record UploadedFileView(Long id, String fileName, String fileType, Long fileSize, String parseStatus, String parsedTextPreview, String errorMessage) {}

    public record ConversationDetailResponse(
            Conversation conversation,
            List<Message> messages,
            List<UploadedFileView> files,
            AgentRun latestRun
    ) {}

    public record AgentAnswerResponse(
            String answerMarkdown,
            List<Map<String, Object>> citations,
            List<String> usedTools,
            Double confidence,
            List<String> followUpQuestions
    ) {}
}
