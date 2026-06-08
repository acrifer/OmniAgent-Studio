package com.devmind.studio.controller;

import com.devmind.studio.dto.ApiResponse;
import com.devmind.studio.dto.ProjectDtos.FeedbackRequest;
import com.devmind.studio.entity.UserFeedback;
import com.devmind.studio.service.FeedbackService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController extends BaseController {
    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ApiResponse<UserFeedback> create(Authentication authentication, @RequestBody FeedbackRequest request) {
        return ApiResponse.ok(feedbackService.create(currentUserId(authentication), request));
    }
}
