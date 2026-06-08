package com.devmind.studio.service;

import com.devmind.studio.dto.ProjectDtos.CreateProjectRequest;
import com.devmind.studio.dto.ProjectDtos.ProjectDetailResponse;
import com.devmind.studio.dto.ProjectDtos.RequirementRequest;
import com.devmind.studio.dto.ProjectDtos.RequirementDocumentView;
import com.devmind.studio.entity.AgentTask;
import com.devmind.studio.entity.FinalReport;
import com.devmind.studio.entity.Project;
import com.devmind.studio.entity.ProjectRequirement;
import com.devmind.studio.entity.RequirementDocument;
import com.devmind.studio.repository.AgentTaskRepository;
import com.devmind.studio.repository.FinalReportRepository;
import com.devmind.studio.repository.ProjectRepository;
import com.devmind.studio.repository.ProjectRequirementRepository;
import com.devmind.studio.repository.RequirementDocumentRepository;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ProjectService {
    private final ProjectRepository projects;
    private final ProjectRequirementRepository requirements;
    private final RequirementDocumentRepository documents;
    private final AgentTaskRepository tasks;
    private final FinalReportRepository reports;
    private final Tika tika = new Tika();
    private final Path uploadDir;
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("txt", "md", "pdf", "docx");

    public ProjectService(ProjectRepository projects,
                          ProjectRequirementRepository requirements,
                          RequirementDocumentRepository documents,
                          AgentTaskRepository tasks,
                          FinalReportRepository reports,
                          @Value("${app.upload-dir}") String uploadDir) {
        this.projects = projects;
        this.requirements = requirements;
        this.documents = documents;
        this.tasks = tasks;
        this.reports = reports;
        this.uploadDir = Path.of(uploadDir);
    }

    public List<Project> list(Long userId) {
        return projects.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public Project create(Long userId, CreateProjectRequest request) {
        Project project = new Project();
        project.setUserId(userId);
        project.setName(request.name());
        project.setDescription(request.description());
        return projects.save(project);
    }

    public ProjectRequirement saveRequirement(Long userId, Long projectId, RequirementRequest request) {
        Project project = ensureOwner(userId, projectId);
        if (request.requirementText() == null || request.requirementText().isBlank()) {
            throw new IllegalArgumentException("需求内容不能为空");
        }
        int nextVersion = requirements.findTopByProjectIdOrderByVersionDesc(projectId).map(r -> r.getVersion() + 1).orElse(1);
        ProjectRequirement requirement = new ProjectRequirement();
        requirement.setProjectId(projectId);
        requirement.setRequirementText(request.requirementText());
        requirement.setVersion(nextVersion);
        ProjectRequirement saved = requirements.save(requirement);
        project.setUpdatedAt(LocalDateTime.now());
        projects.save(project);
        return saved;
    }

    public RequirementDocumentView uploadDocument(Long userId, Long projectId, MultipartFile file) throws IOException {
        Project project = ensureOwner(userId, projectId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String originalName = safeFileName(file.getOriginalFilename() == null ? "requirement-document" : file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 txt、md、pdf、docx 格式的需求文档");
        }
        Files.createDirectories(uploadDir.resolve(projectId.toString()));
        Path target = uploadDir.resolve(projectId.toString()).resolve(System.currentTimeMillis() + "-" + originalName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        RequirementDocument document = new RequirementDocument();
        document.setProjectId(projectId);
        document.setFileName(originalName);
        document.setFilePath(target.toString());
        document.setFileType(file.getContentType());
        document.setFileSize(file.getSize());
        try (InputStream inputStream = Files.newInputStream(target)) {
            String text = tika.parseToString(inputStream);
            document.setParsedText(text == null ? "" : text.trim());
            document.setParseStatus(document.getParsedText().isBlank() ? "FAILED" : "PARSED");
            if (document.getParsedText().isBlank()) {
                document.setErrorMessage("未能从文档中提取到有效文本");
            }
        } catch (TikaException | RuntimeException exception) {
            document.setParseStatus("FAILED");
            document.setErrorMessage(exception.getMessage());
            document.setParsedText("");
        }
        RequirementDocument saved = documents.save(document);
        project.setUpdatedAt(LocalDateTime.now());
        projects.save(project);
        return toDocumentView(saved);
    }

    public void archive(Long userId, Long projectId) {
        Project project = ensureOwner(userId, projectId);
        project.setStatus("ARCHIVED");
        project.setArchivedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        projects.save(project);
    }

    public Project ensureOwner(Long userId, Long projectId) {
        return projects.findByIdAndUserId(projectId, userId).orElseThrow(() -> new IllegalArgumentException("项目不存在或无权限"));
    }

    public ProjectRequirement latestRequirement(Long projectId) {
        return requirements.findTopByProjectIdOrderByVersionDesc(projectId).orElseThrow(() -> new IllegalArgumentException("请先填写项目需求"));
    }

    public List<RequirementDocument> documents(Long projectId) {
        return documents.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    public ProjectDetailResponse detail(Long userId, Long projectId) {
        Project project = ensureOwner(userId, projectId);
        ProjectRequirement latestRequirement = requirements.findTopByProjectIdOrderByVersionDesc(projectId).orElse(null);
        List<RequirementDocumentView> documentViews = documents(projectId).stream().map(this::toDocumentView).toList();
        AgentTask latestTask = tasks.findTopByProjectIdOrderByCreatedAtDesc(projectId).orElse(null);
        FinalReport latestReport = reports.findTopByProjectIdOrderByVersionDesc(projectId).orElse(null);
        return new ProjectDetailResponse(project, latestRequirement, documentViews, latestTask, latestReport);
    }

    public RequirementDocumentView toDocumentView(RequirementDocument document) {
        String text = document.getParsedText();
        String preview = text == null ? "" : text.substring(0, Math.min(text.length(), 800));
        return new RequirementDocumentView(
                document.getId(),
                document.getFileName(),
                document.getFileType(),
                document.getFileSize(),
                document.getParseStatus(),
                preview,
                document.getErrorMessage()
        );
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
}
