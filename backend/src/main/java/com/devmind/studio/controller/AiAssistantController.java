package com.devmind.studio.controller;

import com.devmind.studio.dto.ApiResponse;
import com.devmind.studio.dto.AssistantDtos.RequirementDraftRequest;
import com.devmind.studio.dto.AssistantDtos.RequirementDraftResponse;
import com.devmind.studio.service.AiAssistantService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiAssistantController {
    private final AiAssistantService assistantService;

    public AiAssistantController(AiAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/requirement-draft")
    public ApiResponse<RequirementDraftResponse> requirementDraft(@RequestBody RequirementDraftRequest request) {
        if (request.projectTitle() == null || request.projectTitle().isBlank()) {
            throw new IllegalArgumentException("项目标题不能为空");
        }
        return ApiResponse.ok(assistantService.generateRequirementDraft(request));
    }
}
