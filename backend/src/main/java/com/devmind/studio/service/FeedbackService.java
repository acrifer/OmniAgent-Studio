package com.devmind.studio.service;

import com.devmind.studio.client.FastApiClient;
import com.devmind.studio.dto.AiDtos.LangfuseScoreRequest;
import com.devmind.studio.dto.ProjectDtos.FeedbackRequest;
import com.devmind.studio.entity.AgentTask;
import com.devmind.studio.entity.AgentRun;
import com.devmind.studio.entity.UserFeedback;
import com.devmind.studio.repository.AgentRunRepository;
import com.devmind.studio.repository.AgentTaskRepository;
import com.devmind.studio.repository.ConversationRepository;
import com.devmind.studio.repository.UserFeedbackRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class FeedbackService {
    private final UserFeedbackRepository feedbacks;
    private final AgentTaskRepository tasks;
    private final AgentRunRepository runs;
    private final ConversationRepository conversations;
    private final ProjectService projectService;
    private final FastApiClient fastApiClient;

    public FeedbackService(UserFeedbackRepository feedbacks,
                           AgentTaskRepository tasks,
                           AgentRunRepository runs,
                           ConversationRepository conversations,
                           ProjectService projectService,
                           FastApiClient fastApiClient) {
        this.feedbacks = feedbacks;
        this.tasks = tasks;
        this.runs = runs;
        this.conversations = conversations;
        this.projectService = projectService;
        this.fastApiClient = fastApiClient;
    }

    public UserFeedback create(Long userId, FeedbackRequest request) {
        ensureFeedbackOwner(userId, request.projectId());
        UserFeedback feedback = new UserFeedback();
        feedback.setUserId(userId);
        feedback.setProjectId(request.projectId());
        feedback.setTaskId(request.taskId());
        feedback.setTargetType(request.targetType());
        feedback.setTargetId(request.targetId());
        feedback.setRating(request.rating());
        feedback.setComment(request.comment());
        UserFeedback saved = feedbacks.save(feedback);
        submitLangfuseScore(saved);
        return saved;
    }

    private void submitLangfuseScore(UserFeedback feedback) {
        String traceId = null;
        if (feedback.getTaskId() != null) {
            traceId = tasks.findById(feedback.getTaskId()).map(AgentTask::getLangfuseTraceId).orElse(null);
            if (traceId == null) {
                traceId = runs.findById(feedback.getTaskId()).map(AgentRun::getLangfuseTraceId).orElse(null);
            }
        }
        if (traceId == null || feedback.getRating() == null) {
            return;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("projectId", feedback.getProjectId());
        metadata.put("taskId", feedback.getTaskId());
        metadata.put("targetType", feedback.getTargetType());
        metadata.put("targetId", feedback.getTargetId());
        fastApiClient.submitScore(new LangfuseScoreRequest(
                traceId,
                null,
                "user_feedback",
                feedback.getRating() / 5.0,
                feedback.getComment(),
                metadata
        ));
    }

    private void ensureFeedbackOwner(Long userId, Long projectOrConversationId) {
        if (projectOrConversationId == null) {
            throw new IllegalArgumentException("反馈目标不能为空");
        }
        try {
            projectService.ensureOwner(userId, projectOrConversationId);
            return;
        } catch (RuntimeException ignored) {
        }
        conversations.findByIdAndUserId(projectOrConversationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("反馈目标不存在或无权限"));
    }
}
