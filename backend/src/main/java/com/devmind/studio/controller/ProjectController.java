package com.devmind.studio.controller;

import com.devmind.studio.dto.ApiResponse;
import com.devmind.studio.dto.ProjectDtos.CreateProjectRequest;
import com.devmind.studio.dto.ProjectDtos.ProjectDetailResponse;
import com.devmind.studio.dto.ProjectDtos.RequirementRequest;
import com.devmind.studio.dto.ProjectDtos.RequirementDocumentView;
import com.devmind.studio.entity.Project;
import com.devmind.studio.entity.ProjectRequirement;
import com.devmind.studio.repository.FinalReportRepository;
import com.devmind.studio.service.ProjectService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController extends BaseController {
    private final ProjectService projectService;
    private final FinalReportRepository reports;

    public ProjectController(ProjectService projectService, FinalReportRepository reports) {
        this.projectService = projectService;
        this.reports = reports;
    }

    @GetMapping
    public ApiResponse<List<Project>> list(Authentication authentication) {
        return ApiResponse.ok(projectService.list(currentUserId(authentication)));
    }

    @PostMapping
    public ApiResponse<Project> create(Authentication authentication, @RequestBody CreateProjectRequest request) {
        return ApiResponse.ok(projectService.create(currentUserId(authentication), request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectDetailResponse> detail(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(projectService.detail(currentUserId(authentication), id));
    }

    @PutMapping("/{id}/requirement")
    public ApiResponse<ProjectRequirement> saveRequirement(Authentication authentication, @PathVariable Long id, @RequestBody RequirementRequest request) {
        return ApiResponse.ok(projectService.saveRequirement(currentUserId(authentication), id, request));
    }

    @PostMapping("/{id}/documents")
    public ApiResponse<RequirementDocumentView> upload(Authentication authentication, @PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(projectService.uploadDocument(currentUserId(authentication), id, file));
    }

    @GetMapping("/{id}/reports/latest")
    public ApiResponse<Object> latestReport(Authentication authentication, @PathVariable Long id) {
        projectService.ensureOwner(currentUserId(authentication), id);
        return ApiResponse.ok(reports.findTopByProjectIdOrderByVersionDesc(id).orElse(null));
    }

    @PostMapping("/{id}/archive")
    public ApiResponse<Boolean> archive(Authentication authentication, @PathVariable Long id) {
        projectService.archive(currentUserId(authentication), id);
        return ApiResponse.ok(true);
    }
}
