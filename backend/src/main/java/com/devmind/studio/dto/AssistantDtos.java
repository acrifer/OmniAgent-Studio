package com.devmind.studio.dto;

import java.util.List;

public class AssistantDtos {
    public record RequirementDraftRequest(
            String projectTitle,
            String projectDescription,
            String techStack,
            String modelName
    ) {}

    public record RequirementDraftResponse(
            String requirementText,
            List<String> outline,
            List<String> suggestedModules,
            List<String> techRisks
    ) {}
}
