package com.yep.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.enums.LevelTwoFocusArea;
import com.yep.domain.exchange.LevelTwoExchange;

public interface LevelTwoExchangeRepository extends JpaRepository<LevelTwoExchange, Long> {

    List<LevelTwoExchange> findByFocusArea(LevelTwoFocusArea focusArea);

    List<LevelTwoExchange> findByApplicationsOpenTrue();
}
