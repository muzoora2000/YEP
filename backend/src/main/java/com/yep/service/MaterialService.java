package com.yep.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.enums.Role;
import com.yep.domain.learning.LearningMaterial;
import com.yep.domain.user.User;
import com.yep.dto.LearningDtos.MaterialRequest;
import com.yep.dto.LearningDtos.MaterialResponse;
import com.yep.exception.ForbiddenException;
import com.yep.exception.NotFoundException;
import com.yep.repository.EnrollmentRepository;
import com.yep.repository.LearningMaterialRepository;
import com.yep.repository.TeamRepository;
import com.yep.repository.UserRepository;
import com.yep.security.SecurityUtils;

@Service
public class MaterialService {

    private final LearningMaterialRepository materialRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final ActivityService activityService;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    public MaterialService(LearningMaterialRepository materialRepository, EnrollmentRepository enrollmentRepository,
            UserRepository userRepository, TeamRepository teamRepository, ActivityService activityService,
            SecurityUtils securityUtils, AuditService auditService) {
        this.materialRepository = materialRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.activityService = activityService;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
    }

    @Transactional
    public MaterialResponse create(Long activityId, MaterialRequest request) {
        LearningActivity activity = activityService.find(activityId);
        activityService.requireManage(activity);
        User actor = currentUser();
        LearningMaterial material = new LearningMaterial(activity, request.title().trim(), request.materialType(),
                request.url().trim(), actor);
        material.setDescription(trimToNull(request.description()));
        material.setPublished(request.published() == null || request.published());
        material = materialRepository.save(material);
        auditService.record(actor, "MATERIAL_CREATED", "LearningMaterial", material.getMaterialId(), material.getTitle());
        return toResponse(material);
    }

    @Transactional
    public MaterialResponse update(Long materialId, MaterialRequest request) {
        LearningMaterial material = find(materialId);
        activityService.requireManage(material.getActivity());
        material.setTitle(request.title().trim());
        material.setMaterialType(request.materialType());
        material.setUrl(request.url().trim());
        material.setDescription(trimToNull(request.description()));
        if (request.published() != null) {
            material.setPublished(request.published());
        }
        materialRepository.save(material);
        auditService.record(currentUser(), "MATERIAL_UPDATED", "LearningMaterial", materialId, material.getTitle());
        return toResponse(material);
    }

    @Transactional
    public void delete(Long materialId) {
        LearningMaterial material = find(materialId);
        activityService.requireManage(material.getActivity());
        materialRepository.delete(material);
        auditService.record(currentUser(), "MATERIAL_DELETED", "LearningMaterial", materialId, material.getTitle());
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> list(Long activityId) {
        LearningActivity activity = activityService.find(activityId);
        User actor = currentUser();
        boolean manager = actor.getRole() == Role.ADMIN
                || (actor.getRole() == Role.FACILITATOR && activity.getFacilitator().getUserId().equals(actor.getUserId()));
        if (manager) {
            return materialRepository.findByActivityActivityIdOrderByUploadedAtAsc(activityId).stream()
                    .map(this::toResponse).toList();
        }
        boolean mentor = actor.getRole() == Role.MENTOR
                && teamRepository.existsByActivityActivityIdAndMentorUserId(activityId, actor.getUserId());
        if (!mentor && !enrollmentRepository.existsByYouthUserIdAndActivityActivityId(actor.getUserId(), activityId)) {
            throw new ForbiddenException("ENROLLMENT_REQUIRED", "Enroll in this activity to view its materials.");
        }
        return materialRepository.findByActivityActivityIdAndPublishedTrueOrderByUploadedAtAsc(activityId).stream()
                .map(this::toResponse).toList();
    }

    public LearningMaterial find(Long materialId) {
        return materialRepository.findById(materialId)
                .orElseThrow(() -> new NotFoundException("MATERIAL_NOT_FOUND", "Learning material not found."));
    }

    public MaterialResponse toResponse(LearningMaterial material) {
        return new MaterialResponse(material.getMaterialId(), material.getActivity().getActivityId(),
                material.getTitle(), material.getMaterialType(), material.getUrl(), material.getDescription(),
                material.getUploadedBy().getName(), material.getUploadedAt(), material.isPublished());
    }

    private User currentUser() {
        return userRepository.findById(securityUtils.currentUserId())
                .orElseThrow(() -> new ForbiddenException("FORBIDDEN", "Account not found."));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
