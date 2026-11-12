package com.yep.domain.user;

import com.yep.domain.enums.AccountStatus;
import com.yep.domain.enums.Role;
import com.yep.domain.platform.SystemSetting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "administrator")
@PrimaryKeyJoinColumn(name = "admin_id")
public class Administrator extends User {

    @Column(name = "department", length = 190)
    private String department;

    public Administrator() {
        super();
        setRole(Role.ADMIN);
    }

    public void manageUsers(User user, AccountStatus status) {
        user.setStatus(status);
    }

    public void managePlatform(SystemSetting setting, String value) {
        setting.update(value, getUserId());
    }

    public String generateReports(String reportType) {
        return reportType == null ? "" : reportType.trim().toLowerCase();
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
