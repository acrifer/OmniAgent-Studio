package com.devmind.studio.service;

import com.devmind.studio.client.FastApiClient;
import com.devmind.studio.dto.AiDtos.KnowledgeIngestRequest;
import com.devmind.studio.dto.AiDtos.OmniAgentEventRequest;
import com.devmind.studio.dto.AiDtos.StartOmniAgentRunRequest;
import com.devmind.studio.dto.OmniAgentDtos.*;
import com.devmind.studio.entity.*;
import com.devmind.studio.repository.*;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.MalformedURLException;
import java.util.*;

@Service
public class OmniAgentService {
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("txt", "md", "pdf", "docx");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final UploadedFileRepository files;
    private final KnowledgeBaseRepository knowledgeBases;
    private final KnowledgeDocumentRepository knowledgeDocuments;
    private final AgentRunRepository runs;
    private final AgentStepRepository steps;
    private final TokenUsageRecordRepository tokenRecords;
    private final ModelProviderRepository modelProviders;
    private final ToolConfigRepository toolConfigs;
    private final SkillConfigRepository skillConfigs;
    private final FastApiClient fastApiClient;
    private final Tika tika = new Tika();
    private final Path uploadDir;

    @Value("${server.port}")
    private String serverPort;

    public OmniAgentService(ConversationRepository conversations,
                            MessageRepository messages,
                            UploadedFileRepository files,
                            KnowledgeBaseRepository knowledgeBases,
                            KnowledgeDocumentRepository knowledgeDocuments,
                            AgentRunRepository runs,
                            AgentStepRepository steps,
                            TokenUsageRecordRepository tokenRecords,
                            ModelProviderRepository modelProviders,
                            ToolConfigRepository toolConfigs,
                            SkillConfigRepository skillConfigs,
                            FastApiClient fastApiClient,
                            @Value("${app.upload-dir}") String uploadDir) {
        this.conversations = conversations;
        this.messages = messages;
        this.files = files;
        this.knowledgeBases = knowledgeBases;
        this.knowledgeDocuments = knowledgeDocuments;
        this.runs = runs;
        this.steps = steps;
        this.tokenRecords = tokenRecords;
        this.modelProviders = modelProviders;
        this.toolConfigs = toolConfigs;
        this.skillConfigs = skillConfigs;
        this.fastApiClient = fastApiClient;
        this.uploadDir = Path.of(uploadDir);
    }

    public List<Conversation> conversations(Long userId) {
        return conversations.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public Conversation createConversation(Long userId, CreateConversationRequest request) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(request.title() == null || request.title().isBlank() ? "新的智能体会话" : request.title());
        conversation.setMode(normalizeMode(request.mode()));
        conversation.setKnowledgeBaseId(request.knowledgeBaseId());
        conversation.setModelName(normalizeModel(request.modelName()));
        return conversations.save(conversation);
    }

    public ConversationDetailResponse conversationDetail(Long userId, Long conversationId) {
        Conversation conversation = ensureConversationOwner(userId, conversationId);
        List<UploadedFileView> fileViews = files.findByConversationIdOrderByCreatedAtDesc(conversationId).stream().map(this::toFileView).toList();
        AgentRun latestRun = runs.findTopByConversationIdOrderByCreatedAtDesc(conversationId).orElse(null);
        return new ConversationDetailResponse(conversation, messages.findByConversationIdOrderByCreatedAtAsc(conversationId), fileViews, latestRun);
    }

    public Message createMessage(Long userId, Long conversationId, CreateMessageRequest request) {
        Conversation conversation = ensureConversationOwner(userId, conversationId);
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setRole("USER");
        message.setContent(request.content());
        message.setMetadataJson(request.metadataJson());
        Message saved = messages.save(message);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversations.save(conversation);
        return saved;
    }

    public UploadedFileView uploadConversationFile(Long userId, Long conversationId, MultipartFile file) throws IOException {
        ensureConversationOwner(userId, conversationId);
        UploadedFile uploaded = parseAndSaveFile(userId, conversationId, null, file);
        return toFileView(uploaded);
    }

    @Transactional
    public AgentRun startRun(Long userId, StartAgentRunRequest request) {
        Conversation conversation = ensureConversationOwner(userId, request.conversationId());
        String question = request.question();
        Long messageId = request.messageId();
        if ((question == null || question.isBlank()) && messageId != null) {
            question = messages.findById(messageId).map(Message::getContent).orElse("");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("请先输入问题");
        }
        AgentRun run = new AgentRun();
        run.setConversationId(conversation.getId());
        run.setUserId(userId);
        run.setMessageId(messageId);
        run.setQuestion(question);
        run.setMode(normalizeMode(request.mode() == null ? conversation.getMode() : request.mode()));
        run.setKnowledgeBaseId(request.knowledgeBaseId() == null ? conversation.getKnowledgeBaseId() : request.knowledgeBaseId());
        run.setModelName(normalizeModel(request.modelName() == null ? conversation.getModelName() : request.modelName()));
        run.setStatus("RUNNING");
        run.setStartedAt(LocalDateTime.now());
        run = runs.save(run);

        List<UploadedFile> conversationFiles = files.findByConversationIdOrderByCreatedAtDesc(conversation.getId());
        List<String> documents = conversationFiles.stream()
                .filter(file -> "PARSED".equals(file.getParseStatus()))
                .map(UploadedFile::getParsedText)
                .filter(text -> text != null && !text.isBlank())
                .toList();
        List<Map<String, Object>> filePayload = conversationFiles.stream().map(this::toAiFilePayload).toList();
        List<Map<String, Object>> toolPayload = toolConfigs.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toToolPayload)
                .toList();
        List<Map<String, Object>> skillPayload = skillConfigs.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toSkillPayload)
                .toList();
        String callback = "http://localhost:" + serverPort + "/api/internal/ai/omni-events";
        try {
            var response = fastApiClient.startOmniAgentRun(new StartOmniAgentRunRequest(
                    userId,
                    conversation.getId(),
                    run.getId(),
                    messageId,
                    question,
                    run.getMode(),
                    run.getKnowledgeBaseId(),
                    documents,
                    filePayload,
                    toolPayload,
                    skillPayload,
                    run.getModelName(),
                    callback
            ));
            run.setAiTaskId(response.aiTaskId());
        } catch (RuntimeException exception) {
            markRunFailed(run, readableAiError(exception));
        }
        return runs.save(run);
    }

    public AgentRun runStatus(Long userId, Long runId) {
        AgentRun run = runs.findById(runId).orElseThrow();
        ensureConversationOwner(userId, run.getConversationId());
        return run;
    }

    public List<AgentStep> runSteps(Long userId, Long runId) {
        AgentRun run = runStatus(userId, runId);
        return steps.findByRunIdOrderByCreatedAtAsc(run.getId());
    }

    public AgentAnswerResponse answer(Long userId, Long runId) {
        AgentRun run = runStatus(userId, runId);
        return new AgentAnswerResponse(
                run.getAnswerMarkdown(),
                parseListOfMaps(run.getCitationsJson()),
                parseStringList(run.getUsedToolsJson()),
                run.getConfidence(),
                parseStringList(run.getFollowUpQuestionsJson())
        );
    }

    public List<KnowledgeBase> knowledgeBases(Long userId) {
        return knowledgeBases.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public KnowledgeBase createKnowledgeBase(Long userId, CreateKnowledgeBaseRequest request) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setUserId(userId);
        kb.setName(request.name() == null || request.name().isBlank() ? "默认知识库" : request.name());
        kb.setDescription(request.description());
        return knowledgeBases.save(kb);
    }

    @Transactional
    public KnowledgeDocument uploadKnowledgeDocument(Long userId, Long knowledgeBaseId, MultipartFile file) throws IOException {
        KnowledgeBase kb = ensureKnowledgeBaseOwner(userId, knowledgeBaseId);
        UploadedFile uploaded = parseAndSaveFile(userId, null, knowledgeBaseId, file);
        KnowledgeDocument document = new KnowledgeDocument();
        document.setUserId(userId);
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setUploadedFileId(uploaded.getId());
        document.setFileName(uploaded.getFileName());
        document.setParsedTextPreview(preview(uploaded.getParsedText(), 800));
        document = knowledgeDocuments.save(document);
        if ("PARSED".equals(uploaded.getParseStatus())) {
            try {
                var response = fastApiClient.ingestKnowledge(new KnowledgeIngestRequest(userId, knowledgeBaseId, document.getId(), uploaded.getFileName(), uploaded.getParsedText()));
                document.setIngestStatus(response.status());
                if (response.message() != null && !"READY".equals(response.status())) {
                    document.setErrorMessage(response.message());
                }
            } catch (RuntimeException exception) {
                document.setIngestStatus("FAILED");
                document.setErrorMessage(readableAiError(exception));
            }
        } else {
            document.setIngestStatus("FAILED");
            document.setErrorMessage(uploaded.getErrorMessage());
        }
        KnowledgeDocument saved = knowledgeDocuments.save(document);
        kb.setVectorStatus("READY".equals(saved.getIngestStatus()) ? "READY" : "FAILED");
        kb.setUpdatedAt(LocalDateTime.now());
        knowledgeBases.save(kb);
        return saved;
    }

    public List<KnowledgeDocument> knowledgeDocuments(Long userId, Long knowledgeBaseId) {
        ensureKnowledgeBaseOwner(userId, knowledgeBaseId);
        return knowledgeDocuments.findByKnowledgeBaseIdOrderByCreatedAtDesc(knowledgeBaseId);
    }

    public List<ModelProvider> modelProviders(Long userId) {
        return modelProviders.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public ModelProvider saveModelProvider(Long userId, ModelProviderRequest request) {
        ModelProvider provider = new ModelProvider();
        provider.setUserId(userId);
        provider.setProvider(request.provider());
        provider.setModelName(normalizeModel(request.modelName()));
        provider.setBaseUrl(request.baseUrl());
        provider.setEnabled(request.enabled() == null || request.enabled());
        provider.setConfigJson(request.configJson());
        return modelProviders.save(provider);
    }

    public List<ToolConfig> toolConfigs(Long userId) {
        return toolConfigs.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public ToolConfig saveToolConfig(Long userId, ToolConfigRequest request) {
        ToolConfig tool = new ToolConfig();
        tool.setUserId(userId);
        tool.setToolType(request.toolType());
        tool.setName(request.name());
        tool.setEndpoint(request.endpoint());
        tool.setEnabled(request.enabled() == null || request.enabled());
        tool.setConfigJson(request.configJson());
        return toolConfigs.save(tool);
    }

    public List<SkillConfig> skillConfigs(Long userId) {
        return skillConfigs.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public SkillConfig saveSkillConfig(Long userId, SkillConfigRequest request) {
        SkillConfig skill = new SkillConfig();
        skill.setUserId(userId);
        skill.setName(request.name());
        skill.setDescription(request.description());
        skill.setEnabled(request.enabled() == null || request.enabled());
        skill.setConfigJson(request.configJson());
        return skillConfigs.save(skill);
    }

    @Transactional
    public void handleEvent(OmniAgentEventRequest event) {
        AgentRun run = event.runId() != null
                ? runs.findById(event.runId()).orElseGet(() -> runs.findByAiTaskId(event.aiTaskId()).orElseThrow())
                : runs.findByAiTaskId(event.aiTaskId()).orElseThrow();
        run.setAiTaskId(event.aiTaskId());
        run.setProgress(event.progress() == null ? run.getProgress() : event.progress());
        run.setLangfuseTraceId(event.langfuseTraceId() == null ? run.getLangfuseTraceId() : event.langfuseTraceId());
        run.setUpdatedAt(LocalDateTime.now());
        if ("TASK_FINISHED".equals(event.eventType())) {
            run.setStatus("SUCCESS");
            run.setProgress(100);
            run.setFinishedAt(LocalDateTime.now());
        } else if ("TASK_FAILED".equals(event.eventType())) {
            markRunFailed(run, event.errorMessage());
        }

        if ("ANSWER".equals(event.agentType()) && event.outputJson() != null) {
            Map<String, Object> answer = parseMap(event.outputJson());
            run.setAnswerMarkdown(asString(answer.get("answerMarkdown"), event.contentMarkdown()));
            run.setCitationsJson(toJson(answer.get("citations")));
            run.setUsedToolsJson(toJson(answer.get("usedTools")));
            run.setConfidence(asDouble(answer.get("confidence")));
            run.setFollowUpQuestionsJson(toJson(answer.get("followUpQuestions")));
            if (run.getAnswerMarkdown() != null) {
                saveAssistantMessage(run, answer);
            }
        }
        runs.save(run);

        if (event.agentType() != null && !"TASK_STARTED".equals(event.eventType())) {
            AgentStep step = new AgentStep();
            step.setRunId(run.getId());
            step.setConversationId(run.getConversationId());
            step.setAgentType(event.agentType());
            step.setStatus(event.status() == null ? "SUCCESS" : event.status());
            step.setInputJson(event.inputJson());
            step.setOutputJson(event.outputJson());
            step.setContentMarkdown(event.contentMarkdown());
            step.setCitationsJson(event.citationsJson());
            step.setModelName(event.modelName());
            step.setPromptVersion(event.promptVersion());
            step.setTraceObservationId(event.traceObservationId());
            step.setPromptTokens(event.promptTokens());
            step.setCompletionTokens(event.completionTokens());
            step.setLatencyMs(event.latencyMs());
            step.setErrorMessage(event.errorMessage());
            steps.save(step);
        }

        if (event.promptTokens() != null || event.completionTokens() != null) {
            TokenUsageRecord record = new TokenUsageRecord();
            record.setRunId(run.getId());
            record.setConversationId(run.getConversationId());
            record.setAgentType(event.agentType());
            record.setModelName(event.modelName());
            record.setPromptTokens(event.promptTokens());
            record.setCompletionTokens(event.completionTokens());
            int total = (event.promptTokens() == null ? 0 : event.promptTokens()) + (event.completionTokens() == null ? 0 : event.completionTokens());
            record.setTotalTokens(total);
            record.setLatencyMs(event.latencyMs());
            tokenRecords.save(record);
        }
    }

    public Map<String, Object> tokenStats(Long userId, Long conversationId) {
        ensureConversationOwner(userId, conversationId);
        List<TokenUsageRecord> records = tokenRecords.findByConversationId(conversationId);
        int totalTokens = records.stream().map(TokenUsageRecord::getTotalTokens).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        return Map.of("totalTokens", totalTokens, "records", records);
    }

    public Resource downloadFile(Long fileId) throws MalformedURLException {
        UploadedFile file = files.findById(fileId).orElseThrow(() -> new IllegalArgumentException("文件不存在"));
        Path path = Path.of(file.getFilePath());
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("文件不存在");
        }
        return new UrlResource(path.toUri());
    }

    private UploadedFile parseAndSaveFile(Long userId, Long conversationId, Long knowledgeBaseId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String originalName = safeFileName(file.getOriginalFilename() == null ? "uploaded-file" : file.getOriginalFilename());
        String extension = extensionOf(originalName);
        boolean isDocument = DOCUMENT_EXTENSIONS.contains(extension);
        boolean isImage = IMAGE_EXTENSIONS.contains(extension);
        if (!isDocument && !isImage) {
            throw new IllegalArgumentException("仅支持 txt、md、pdf、docx、png、jpg、jpeg、webp 文件");
        }
        Path folder = uploadDir.resolve("omni").resolve(userId.toString());
        Files.createDirectories(folder);
        Path target = folder.resolve(System.currentTimeMillis() + "-" + originalName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        UploadedFile uploaded = new UploadedFile();
        uploaded.setUserId(userId);
        uploaded.setConversationId(conversationId);
        uploaded.setKnowledgeBaseId(knowledgeBaseId);
        uploaded.setFileName(originalName);
        uploaded.setFilePath(target.toString());
        uploaded.setFileType(file.getContentType());
        uploaded.setFileSize(file.getSize());
        if (isImage) {
            uploaded.setParseStatus("IMAGE");
            uploaded.setParsedText("");
            return files.save(uploaded);
        }
        try (InputStream inputStream = Files.newInputStream(target)) {
            String text = tika.parseToString(inputStream);
            uploaded.setParsedText(text == null ? "" : text.trim());
            uploaded.setParseStatus(uploaded.getParsedText().isBlank() ? "FAILED" : "PARSED");
            if (uploaded.getParsedText().isBlank()) {
                uploaded.setErrorMessage("未能从文件中提取到有效文本");
            }
        } catch (TikaException | RuntimeException exception) {
            uploaded.setParseStatus("FAILED");
            uploaded.setErrorMessage(exception.getMessage());
            uploaded.setParsedText("");
        }
        return files.save(uploaded);
    }

    private Conversation ensureConversationOwner(Long userId, Long conversationId) {
        return conversations.findByIdAndUserId(conversationId, userId).orElseThrow(() -> new IllegalArgumentException("会话不存在或无权限"));
    }

    private KnowledgeBase ensureKnowledgeBaseOwner(Long userId, Long knowledgeBaseId) {
        return knowledgeBases.findByIdAndUserId(knowledgeBaseId, userId).orElseThrow(() -> new IllegalArgumentException("知识库不存在或无权限"));
    }

    private UploadedFileView toFileView(UploadedFile file) {
        return new UploadedFileView(file.getId(), file.getFileName(), file.getFileType(), file.getFileSize(), file.getParseStatus(), preview(file.getParsedText(), 800), file.getErrorMessage());
    }

    private Map<String, Object> toAiFilePayload(UploadedFile file) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", file.getId());
        payload.put("fileName", file.getFileName());
        payload.put("fileType", file.getFileType());
        payload.put("parseStatus", file.getParseStatus());
        payload.put("fileCategory", "IMAGE".equals(file.getParseStatus()) ? "IMAGE" : "DOCUMENT");
        payload.put("downloadUrl", "http://localhost:" + serverPort + "/api/internal/ai/files/" + file.getId());
        payload.put("parsedTextPreview", preview(file.getParsedText(), 1200));
        return payload;
    }

    private Map<String, Object> toToolPayload(ToolConfig tool) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", tool.getId());
        payload.put("toolType", tool.getToolType());
        payload.put("name", tool.getName());
        payload.put("endpoint", tool.getEndpoint());
        payload.put("enabled", tool.getEnabled());
        payload.put("configJson", tool.getConfigJson());
        return payload;
    }

    private Map<String, Object> toSkillPayload(SkillConfig skill) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", skill.getId());
        payload.put("name", skill.getName());
        payload.put("description", skill.getDescription());
        payload.put("enabled", skill.getEnabled());
        payload.put("configJson", skill.getConfigJson());
        return payload;
    }

    private void saveAssistantMessage(AgentRun run, Map<String, Object> answer) {
        if (messages.findByConversationIdOrderByCreatedAtAsc(run.getConversationId()).stream().anyMatch(message -> Objects.equals(message.getMetadataJson(), "agentRun:" + run.getId()))) {
            return;
        }
        Message message = new Message();
        message.setConversationId(run.getConversationId());
        message.setUserId(run.getUserId());
        message.setRole("ASSISTANT");
        message.setContent(run.getAnswerMarkdown());
        message.setMetadataJson("agentRun:" + run.getId());
        messages.save(message);
    }

    private void markRunFailed(AgentRun run, String message) {
        run.setStatus("FAILED");
        run.setProgress(100);
        run.setErrorMessage(message == null || message.isBlank() ? "智能体运行失败" : message);
        run.setFinishedAt(LocalDateTime.now());
        run.setUpdatedAt(LocalDateTime.now());
    }

    private String readableAiError(RuntimeException exception) {
        if (exception instanceof WebClientResponseException responseException) {
            String body = responseException.getResponseBodyAsString();
            return body == null || body.isBlank() ? "AI 服务调用失败：" + responseException.getStatusCode() : "AI 服务调用失败：" + body;
        }
        return exception.getMessage() == null ? "AI 服务调用失败" : exception.getMessage();
    }

    private String normalizeMode(String mode) {
        return mode == null || mode.isBlank() ? "AUTO" : mode.toUpperCase();
    }

    private String normalizeModel(String modelName) {
        return modelName == null || modelName.isBlank() ? "deepseek-v4-flash" : modelName;
    }

    private String extensionOf(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 ? "" : fileName.substring(index + 1).toLowerCase();
    }

    private String safeFileName(String fileName) {
        String normalized = fileName.replace("\\", "/");
        int index = normalized.lastIndexOf('/');
        String safe = index < 0 ? normalized : normalized.substring(index + 1);
        return safe.replaceAll("[\\r\\n]", "").trim();
    }

    private String preview(String text, int limit) {
        return text == null ? "" : text.substring(0, Math.min(text.length(), limit));
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
        } catch (Exception exception) {
            return String.valueOf(value);
        }
    }

    private Map<String, Object> parseMap(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private List<Map<String, Object>> parseListOfMaps(String json) {
        try {
            return json == null ? List.of() : new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, List.class);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private List<String> parseStringList(String json) {
        try {
            return json == null ? List.of() : new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, List.class);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String asString(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private Double asDouble(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        try {
            return value == null ? null : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
