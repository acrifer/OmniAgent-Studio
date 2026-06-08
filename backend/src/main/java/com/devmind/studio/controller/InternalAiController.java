package com.devmind.studio.controller;

import com.devmind.studio.dto.AiDtos.TaskEventRequest;
import com.devmind.studio.dto.AiDtos.OmniAgentEventRequest;
import com.devmind.studio.dto.ApiResponse;
import com.devmind.studio.service.AgentTaskService;
import com.devmind.studio.service.OmniAgentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;

@RestController
@RequestMapping("/api/internal/ai")
public class InternalAiController {
    private final AgentTaskService agentTaskService;
    private final OmniAgentService omniAgentService;
    private final String internalToken;

    public InternalAiController(AgentTaskService agentTaskService,
                                OmniAgentService omniAgentService,
                                @Value("${app.internal-token}") String internalToken) {
        this.agentTaskService = agentTaskService;
        this.omniAgentService = omniAgentService;
        this.internalToken = internalToken;
    }

    @PostMapping("/task-events")
    public ApiResponse<Boolean> event(@RequestHeader(value = "X-Internal-Token", required = false) String token,
                                      @RequestBody TaskEventRequest request) {
        if (!internalToken.equals(token)) {
            throw new IllegalArgumentException("内部 token 无效");
        }
        agentTaskService.handleEvent(request);
        return ApiResponse.ok(true);
    }

    @PostMapping("/omni-events")
    public ApiResponse<Boolean> omniEvent(@RequestHeader(value = "X-Internal-Token", required = false) String token,
                                          @RequestBody OmniAgentEventRequest request) {
        if (!internalToken.equals(token)) {
            throw new IllegalArgumentException("内部 token 无效");
        }
        omniAgentService.handleEvent(request);
        return ApiResponse.ok(true);
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<Resource> downloadFile(@RequestHeader(value = "X-Internal-Token", required = false) String token,
                                                 @PathVariable Long id) throws MalformedURLException {
        if (!internalToken.equals(token)) {
            throw new IllegalArgumentException("内部 token 无效");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(omniAgentService.downloadFile(id));
    }
}
