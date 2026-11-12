package com.yep.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yep.dto.LearningDtos.MaterialRequest;
import com.yep.dto.LearningDtos.MaterialResponse;
import com.yep.service.MaterialService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping("/activities/{activityId}/materials")
    public List<MaterialResponse> list(@PathVariable Long activityId) {
        return materialService.list(activityId);
    }

    @PostMapping("/activities/{activityId}/materials")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public ResponseEntity<MaterialResponse> create(@PathVariable Long activityId,
            @Valid @RequestBody MaterialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materialService.create(activityId, request));
    }

    @PutMapping("/materials/{materialId}")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public MaterialResponse update(@PathVariable Long materialId, @Valid @RequestBody MaterialRequest request) {
        return materialService.update(materialId, request);
    }

    @DeleteMapping("/materials/{materialId}")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long materialId) {
        materialService.delete(materialId);
        return ResponseEntity.noContent().build();
    }
}
