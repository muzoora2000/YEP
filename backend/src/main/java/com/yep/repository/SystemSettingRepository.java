package com.yep.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.platform.SystemSetting;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
}
