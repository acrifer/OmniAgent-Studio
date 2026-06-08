package com.devmind.studio.controller;

import com.devmind.studio.dto.ApiResponse;
import com.devmind.studio.dto.OmniAgentDtos.*;
import com.devmind.studio.entity.*;
import com.devmind.studio.service.OmniAgentService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
public class OmniAgentController extends BaseController {
    private final OmniAgentService omniAgentService;

    public OmniAgentController(OmniAgentService omniAgentService) {
        this.omniAgentService = omniAgentService;
    }

    @GetMapping("/api/conversations")
    public ApiResponse<List<Conversation>> conversations(Authentication authentication) {
        return ApiResponse.ok(omniAgentService.conversations(currentUserId(authentication)));
    }

    @PostMapping("/api/conversations")
    public ApiResponse<Conversation> createConversation(Authentication authentication,
                                                        @RequestBody CreateConversationRequest request) {
        return ApiResponse.ok(omniAgentService.createConversation(currentUserId(authentication), request));
    }

    @GetMapping("/api/conversations/{id}")
    public ApiResponse<ConversationDetailResponse> conversation(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(omniAgentService.conversationDetail(currentUserId(authentication), id));
    }

    @PostMapping("/api/conversations/{id}/messages")
    public ApiResponse<Message> createMessage(Authentication authentication,
                                              @PathVariable Long id,
                                              @RequestBody CreateMessageRequest request) {
        return ApiResponse.ok(omniAgentService.createMessage(currentUserId(authentication), id, request));
    }

    @PostMapping("/api/conversations/{id}/files")
    public ApiResponse<UploadedFileView> uploadFile(Authentication authentication,
                                                    @PathVariable Long id,
                                                    @RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(omniAgentService.uploadConversationFile(currentUserId(authentication), id, file));
    }

    @PostMapping("/api/agent-runs")
    public ApiResponse<AgentRun> startRun(Authentication authentication, @RequestBody StartAgentRunRequest request) {
        return ApiResponse.ok(omniAgentService.startRun(currentUserId(authentication), request));
    }

    @GetMapping("/api/agent-runs/{id}/status")
    public ApiResponse<AgentRun> runStatus(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(omniAgentService.runStatus(currentUserId(authentication), id));
    }

    @GetMapping("/api/agent-runs/{id}/steps")
    public ApiResponse<List<AgentStep>> runSteps(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(omniAgentService.runSteps(currentUserId(authentication), id));
    }

    @GetMapping("/api/agent-runs/{id}/answer")
    public ApiResponse<AgentAnswerResponse> answer(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(omniAgentService.answer(currentUserId(authentication), id));
    }

    @GetMapping("/api/knowledge-bases")
    public ApiResponse<List<KnowledgeBase>> knowledgeBases(Authentication authentication) {
        return ApiResponse.ok(omniAgentService.knowledgeBases(currentUserId(authentication)));
    }

    @PostMapping("/api/knowledge-bases")
    public ApiResponse<KnowledgeBase> createKnowledgeBase(Authentication authentication,
                                                          @RequestBody CreateKnowledgeBaseRequest request) {
        return ApiResponse.ok(omniAgentService.createKnowledgeBase(currentUserId(authentication), request));
    }

    @GetMapping("/api/knowledge-bases/{id}/documents")
    public ApiResponse<List<KnowledgeDocument>> knowledgeDocuments(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(omniAgentService.knowledgeDocuments(currentUserId(authentication), id));
    }

    @PostMapping("/api/knowledge-bases/{id}/documents")
    public ApiResponse<KnowledgeDocument> uploadKnowledgeDocument(Authentication authentication,
                                                                  @PathVariable Long id,
                                                                  @RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(omniAgentService.uploadKnowledgeDocument(currentUserId(authentication), id, file));
    }

    @GetMapping("/api/model-providers")
    public ApiResponse<List<ModelProvider>> modelProviders(Authentication authentication) {
        return ApiResponse.ok(omniAgentService.modelProviders(currentUserId(authentication)));
    }

    @PostMapping("/api/model-providers")
    public ApiResponse<ModelProvider> saveModelProvider(Authentication authentication,
                                                        @RequestBody ModelProviderRequest request) {
        return ApiResponse.ok(omniAgentService.saveModelProvider(currentUserId(authentication), request));
    }

    @GetMapping("/api/tools")
    public ApiResponse<List<ToolConfig>> tools(Authentication authentication) {
        return ApiResponse.ok(omniAgentService.toolConfigs(currentUserId(authentication)));
    }

    @PostMapping("/api/tools")
    public ApiResponse<ToolConfig> saveTool(Authentication authentication, @RequestBody ToolConfigRequest request) {
        return ApiResponse.ok(omniAgentService.saveToolConfig(currentUserId(authentication), request));
    }

    @GetMapping("/api/skills")
    public ApiResponse<List<SkillConfig>> skills(Authentication authentication) {
        return ApiResponse.ok(omniAgentService.skillConfigs(currentUserId(authentication)));
    }

    @PostMapping("/api/skills")
    public ApiResponse<SkillConfig> saveSkill(Authentication authentication, @RequestBody SkillConfigRequest request) {
        return ApiResponse.ok(omniAgentService.saveSkillConfig(currentUserId(authentication), request));
    }

    @GetMapping("/api/omni/stats/tokens")
    public ApiResponse<Map<String, Object>> tokenStats(Authentication authentication,
                                                       @RequestParam Long conversationId) {
        return ApiResponse.ok(omniAgentService.tokenStats(currentUserId(authentication), conversationId));
    }
}
