package com.yep.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.user.Administrator;

public interface AdministratorRepository extends JpaRepository<Administrator, Long> {
}
