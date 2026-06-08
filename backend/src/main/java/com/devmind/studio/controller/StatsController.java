package com.devmind.studio.controller;

import com.devmind.studio.dto.ApiResponse;
import com.devmind.studio.service.AgentTaskService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController extends BaseController {
    private final AgentTaskService agentTaskService;

    public StatsController(AgentTaskService agentTaskService) {
        this.agentTaskService = agentTaskService;
    }

    @GetMapping("/tokens")
    public ApiResponse<Map<String, Object>> tokens(Authentication authentication, @RequestParam Long projectId) {
        return ApiResponse.ok(agentTaskService.tokenStats(currentUserId(authentication), projectId));
    }
}
