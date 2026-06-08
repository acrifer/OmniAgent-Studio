package com.devmind.studio.service;

import com.devmind.studio.dto.ProjectDtos.PromptRequest;
import com.devmind.studio.entity.PromptTemplate;
import com.devmind.studio.repository.PromptTemplateRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PromptService {
    private final PromptTemplateRepository prompts;

    public PromptService(PromptTemplateRepository prompts) {
        this.prompts = prompts;
    }

    public List<PromptTemplate> list() {
        return prompts.findAllByOrderByAgentTypeAscCreatedAtDesc();
    }

    public PromptTemplate create(Long userId, PromptRequest request) {
        PromptTemplate prompt = new PromptTemplate();
        prompt.setAgentType(request.agentType());
        prompt.setName(request.name());
        prompt.setContent(request.content());
        prompt.setVersion(request.version());
        prompt.setModelName(request.modelName());
        prompt.setTemperature(request.temperature() == null ? BigDecimal.valueOf(0.2) : BigDecimal.valueOf(request.temperature()));
        prompt.setCreatedBy(userId);
        return prompts.save(prompt);
    }
}
