package com.yep.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yep.domain.enums.LevelOneTopic;
import com.yep.domain.exchange.LevelOneExchange;

public interface LevelOneExchangeRepository extends JpaRepository<LevelOneExchange, Long> {

    List<LevelOneExchange> findByTopic(LevelOneTopic topic);

    long countByFacilitatorId(Long facilitatorId);
}
