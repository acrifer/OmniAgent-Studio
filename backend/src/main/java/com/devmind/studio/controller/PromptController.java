package com.devmind.studio.controller;

import com.devmind.studio.dto.ApiResponse;
import com.devmind.studio.dto.ProjectDtos.PromptRequest;
import com.devmind.studio.entity.PromptTemplate;
import com.devmind.studio.service.PromptService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prompts")
public class PromptController extends BaseController {
    private final PromptService promptService;

    public PromptController(PromptService promptService) {
        this.promptService = promptService;
    }

    @GetMapping
    public ApiResponse<List<PromptTemplate>> list() {
        return ApiResponse.ok(promptService.list());
    }

    @PostMapping
    public ApiResponse<PromptTemplate> create(Authentication authentication, @RequestBody PromptRequest request) {
        return ApiResponse.ok(promptService.create(currentUserId(authentication), request));
    }
}
