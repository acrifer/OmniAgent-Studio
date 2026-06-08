package com.devmind.studio.dto;

import com.devmind.studio.entity.*;

import java.util.List;

public class ProjectDtos {
    public record CreateProjectRequest(String name, String description) {}
    public record RequirementRequest(String requirementText) {}
    public record StartAgentTaskRequest(String modelName, String techStack) {}
    public record RegenerateRequest(String agentType) {}
    public record PromptRequest(String agentType, String name, String content, String version, String modelName, Double temperature) {}
    public record FeedbackRequest(Long projectId, Long taskId, String targetType, Long targetId, Integer rating, String comment) {}

    public record RequirementDocumentView(Long id, String fileName, String fileType, Long fileSize, String parseStatus, String parsedTextPreview, String errorMessage) {}

    public record ProjectDetailResponse(
            Project project,
            ProjectRequirement latestRequirement,
            List<RequirementDocumentView> documents,
            AgentTask latestTask,
            FinalReport latestReport
    ) {}
}
