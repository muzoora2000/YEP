package com.yep.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.learning.Certificate;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    List<Certificate> findByYouthUserId(Long youthId);

    Optional<Certificate> findByCertificateNumberIgnoreCase(String certificateNumber);

    boolean existsByCertificateNumberIgnoreCase(String certificateNumber);

    boolean existsByYouthUserIdAndActivityActivityId(Long youthId, Long activityId);

    boolean existsByYouthUserIdAndProjectProjectId(Long youthId, Long projectId);
}
