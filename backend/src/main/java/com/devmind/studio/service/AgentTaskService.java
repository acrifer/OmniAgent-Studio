package com.devmind.studio.service;

import com.devmind.studio.client.FastApiClient;
import com.devmind.studio.dto.AiDtos.RegenerateWorkflowRequest;
import com.devmind.studio.dto.AiDtos.StartWorkflowRequest;
import com.devmind.studio.dto.AiDtos.TaskEventRequest;
import com.devmind.studio.dto.ProjectDtos.RegenerateRequest;
import com.devmind.studio.dto.ProjectDtos.StartAgentTaskRequest;
import com.devmind.studio.entity.*;
import com.devmind.studio.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AgentTaskService {
    private final AgentTaskRepository tasks;
    private final AgentResultRepository results;
    private final FinalReportRepository reports;
    private final TokenUsageRecordRepository tokenRecords;
    private final ProjectService projectService;
    private final FastApiClient fastApiClient;

    @Value("${server.port}")
    private String serverPort;

    public AgentTaskService(AgentTaskRepository tasks,
                            AgentResultRepository results,
                            FinalReportRepository reports,
                            TokenUsageRecordRepository tokenRecords,
                            ProjectService projectService,
                            FastApiClient fastApiClient) {
        this.tasks = tasks;
        this.results = results;
        this.reports = reports;
        this.tokenRecords = tokenRecords;
        this.projectService = projectService;
        this.fastApiClient = fastApiClient;
    }

    @Transactional
    public AgentTask start(Long userId, Long projectId, StartAgentTaskRequest request) {
        Project project = projectService.ensureOwner(userId, projectId);
        ProjectRequirement requirement = projectService.latestRequirement(projectId);
        AgentTask task = new AgentTask();
        task.setProjectId(projectId);
        task.setUserId(userId);
        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        task = tasks.save(task);

        List<String> docs = projectService.documents(projectId).stream()
                .filter(document -> "PARSED".equals(document.getParseStatus()))
                .map(RequirementDocument::getParsedText)
                .filter(text -> text != null && !text.isBlank())
                .toList();
        String callback = "http://localhost:" + serverPort + "/api/internal/ai/task-events";
        try {
            var response = fastApiClient.startWorkflow(new StartWorkflowRequest(
                    projectId,
                    task.getId(),
                    project.getName(),
                    requirement.getRequirementText(),
                    docs,
                    normalizeTechStack(request.techStack()),
                    normalizeModel(request.modelName()),
                    callback,
                    Map.of()
            ));
            task.setFastapiTaskId(response.aiTaskId());
        } catch (RuntimeException exception) {
            markFailed(task, readableAiError(exception));
        }
        return tasks.save(task);
    }

    @Transactional
    public AgentTask regenerate(Long userId, Long taskId, RegenerateRequest request) {
        AgentTask oldTask = tasks.findById(taskId).orElseThrow();
        Project project = projectService.ensureOwner(userId, oldTask.getProjectId());
        ProjectRequirement requirement = projectService.latestRequirement(oldTask.getProjectId());
        if (request.agentType() == null || request.agentType().isBlank()) {
            throw new IllegalArgumentException("请选择需要重生成的 Agent");
        }
        AgentTask task = new AgentTask();
        task.setProjectId(oldTask.getProjectId());
        task.setUserId(userId);
        task.setTaskType("REGENERATE");
        task.setTargetAgent(request.agentType());
        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        task = tasks.save(task);

        List<String> docs = projectService.documents(oldTask.getProjectId()).stream()
                .filter(document -> "PARSED".equals(document.getParseStatus()))
                .map(RequirementDocument::getParsedText)
                .filter(text -> text != null && !text.isBlank())
                .toList();
        String callback = "http://localhost:" + serverPort + "/api/internal/ai/task-events";
        try {
            var response = fastApiClient.regenerateWorkflow(
                    oldTask.getFastapiTaskId() == null ? "new" : oldTask.getFastapiTaskId(),
                    new RegenerateWorkflowRequest(
                            oldTask.getProjectId(),
                            task.getId(),
                            project.getName(),
                            requirement.getRequirementText(),
                            docs,
                            normalizeTechStack(null),
                            inferModelName(oldTask.getId()),
                            callback,
                            request.agentType(),
                            previousResults(oldTask.getProjectId())
                    )
            );
            task.setFastapiTaskId(response.aiTaskId());
        } catch (RuntimeException exception) {
            markFailed(task, readableAiError(exception));
        }
        return tasks.save(task);
    }

    public AgentTask latest(Long userId, Long projectId) {
        projectService.ensureOwner(userId, projectId);
        return tasks.findTopByProjectIdOrderByCreatedAtDesc(projectId).orElse(null);
    }

    public AgentTask status(Long userId, Long taskId) {
        AgentTask task = tasks.findById(taskId).orElseThrow();
        projectService.ensureOwner(userId, task.getProjectId());
        return task;
    }

    public List<AgentResult> results(Long userId, Long taskId) {
        AgentTask task = status(userId, taskId);
        return results.findByTaskIdOrderByCreatedAtAsc(task.getId());
    }

    @Transactional
    public void handleEvent(TaskEventRequest event) {
        AgentTask task = tasks.findById(event.taskId()).orElseGet(() -> tasks.findByFastapiTaskId(event.aiTaskId()).orElseThrow());
        task.setFastapiTaskId(event.aiTaskId());
        task.setProgress(event.progress() == null ? task.getProgress() : event.progress());
        task.setLangfuseTraceId(event.langfuseTraceId() == null ? task.getLangfuseTraceId() : event.langfuseTraceId());
        task.setUpdatedAt(LocalDateTime.now());

        if ("TASK_FINISHED".equals(event.eventType())) {
            task.setStatus("SUCCESS");
            task.setProgress(100);
            task.setFinishedAt(LocalDateTime.now());
        } else if ("TASK_FAILED".equals(event.eventType())) {
            task.setStatus("FAILED");
            task.setErrorMessage(event.errorMessage());
            task.setFinishedAt(LocalDateTime.now());
        }
        tasks.save(task);

        if (event.agentType() != null && event.contentMarkdown() != null) {
            AgentResult result = new AgentResult();
            result.setTaskId(task.getId());
            result.setProjectId(task.getProjectId());
            result.setAgentType(event.agentType());
            result.setStatus(event.status() == null ? "SUCCESS" : event.status());
            result.setContentJson(event.contentJson());
            result.setContentMarkdown(event.contentMarkdown());
            result.setModelName(event.modelName());
            result.setPromptVersion(event.promptVersion());
            result.setTraceObservationId(event.traceObservationId());
            result.setErrorMessage(event.errorMessage());
            result.setQualityScore(BigDecimal.valueOf(0.85));
            results.save(result);
        }

        if ("SUMMARY".equals(event.agentType()) && event.contentMarkdown() != null) {
            FinalReport report = new FinalReport();
            report.setProjectId(task.getProjectId());
            report.setTaskId(task.getId());
            report.setTitle("智能研发方案报告");
            report.setContentMarkdown(event.contentMarkdown());
            report.setContentJson(event.contentJson());
            report.setVersion(reports.findTopByProjectIdOrderByVersionDesc(task.getProjectId()).map(r -> r.getVersion() + 1).orElse(1));
            reports.save(report);
        }

        if (event.promptTokens() != null || event.completionTokens() != null) {
            TokenUsageRecord record = new TokenUsageRecord();
            record.setTaskId(task.getId());
            record.setProjectId(task.getProjectId());
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

    public Map<String, Object> tokenStats(Long userId, Long projectId) {
        projectService.ensureOwner(userId, projectId);
        List<TokenUsageRecord> records = tokenRecords.findByProjectId(projectId);
        int totalTokens = records.stream().map(TokenUsageRecord::getTotalTokens).filter(v -> v != null).mapToInt(Integer::intValue).sum();
        Map<String, Integer> byAgent = records.stream()
                .filter(record -> record.getAgentType() != null)
                .collect(Collectors.groupingBy(
                        TokenUsageRecord::getAgentType,
                        LinkedHashMap::new,
                        Collectors.summingInt(record -> record.getTotalTokens() == null ? 0 : record.getTotalTokens())
                ));
        return Map.of("totalTokens", totalTokens, "records", records, "byAgent", byAgent);
    }

    private Map<String, String> previousResults(Long projectId) {
        Map<String, String> context = new LinkedHashMap<>();
        for (AgentResult result : results.findByProjectIdOrderByCreatedAtDesc(projectId)) {
            if ("SUCCESS".equals(result.getStatus()) && result.getContentJson() != null && !context.containsKey(result.getAgentType())) {
                context.put(result.getAgentType(), result.getContentJson());
            }
        }
        return context;
    }

    private String inferModelName(Long taskId) {
        return tokenRecords.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(TokenUsageRecord::getModelName)
                .filter(model -> model != null && !model.isBlank())
                .findFirst()
                .orElse("deepseek-v4-flash");
    }

    private String normalizeModel(String modelName) {
        return modelName == null || modelName.isBlank() ? "deepseek-v4-flash" : modelName;
    }

    private String normalizeTechStack(String techStack) {
        return techStack == null || techStack.isBlank()
                ? "Vue 3 + Element Plus, Spring Boot, MySQL, Redis, FastAPI, LangGraph, Langfuse"
                : techStack;
    }

    private void markFailed(AgentTask task, String message) {
        task.setStatus("FAILED");
        task.setProgress(100);
        task.setErrorMessage(message);
        task.setFinishedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
    }

    private String readableAiError(RuntimeException exception) {
        if (exception instanceof WebClientResponseException responseException) {
            String body = responseException.getResponseBodyAsString();
            if (body != null && !body.isBlank()) {
                return "AI 服务调用失败：" + body;
            }
            return "AI 服务调用失败：" + responseException.getStatusCode();
        }
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "AI 服务调用失败，请检查 FastAPI、DeepSeek Key 和模型配置。" : message;
    }
}
