package com.devmind.studio.controller;

import com.devmind.studio.dto.ApiResponse;
import com.devmind.studio.dto.ProjectDtos.RegenerateRequest;
import com.devmind.studio.dto.ProjectDtos.StartAgentTaskRequest;
import com.devmind.studio.entity.AgentResult;
import com.devmind.studio.entity.AgentTask;
import com.devmind.studio.service.AgentTaskService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AgentTaskController extends BaseController {
    private final AgentTaskService agentTaskService;

    public AgentTaskController(AgentTaskService agentTaskService) {
        this.agentTaskService = agentTaskService;
    }

    @PostMapping("/projects/{projectId}/agent-tasks")
    public ApiResponse<AgentTask> start(Authentication authentication, @PathVariable Long projectId, @RequestBody StartAgentTaskRequest request) {
        return ApiResponse.ok(agentTaskService.start(currentUserId(authentication), projectId, request));
    }

    @GetMapping("/projects/{projectId}/agent-tasks/latest")
    public ApiResponse<AgentTask> latest(Authentication authentication, @PathVariable Long projectId) {
        return ApiResponse.ok(agentTaskService.latest(currentUserId(authentication), projectId));
    }

    @GetMapping("/agent-tasks/{taskId}/status")
    public ApiResponse<AgentTask> status(Authentication authentication, @PathVariable Long taskId) {
        return ApiResponse.ok(agentTaskService.status(currentUserId(authentication), taskId));
    }

    @GetMapping("/agent-tasks/{taskId}/results")
    public ApiResponse<List<AgentResult>> results(Authentication authentication, @PathVariable Long taskId) {
        return ApiResponse.ok(agentTaskService.results(currentUserId(authentication), taskId));
    }

    @PostMapping("/agent-tasks/{taskId}/regenerate")
    public ApiResponse<AgentTask> regenerate(Authentication authentication, @PathVariable Long taskId, @RequestBody RegenerateRequest request) {
        return ApiResponse.ok(agentTaskService.regenerate(currentUserId(authentication), taskId, request));
    }
}
