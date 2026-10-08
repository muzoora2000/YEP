package com.yep.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.enums.ExchangeLevel;
import com.yep.domain.enums.ExchangeStatus;
import com.yep.domain.exchange.LevelOneExchange;
import com.yep.domain.exchange.LevelTwoExchange;
import com.yep.domain.exchange.VirtualExchange;
import com.yep.dto.ExchangeDtos;
import com.yep.dto.PageResponse;
import com.yep.exception.BadRequestException;
import com.yep.exception.NotFoundException;
import com.yep.repository.VirtualExchangeRepository;
import com.yep.security.SecurityUtils;

/**
 * Browsing and administration of virtual exchanges.
 * The concrete level is chosen polymorphically from the requested
 * {@link ExchangeLevel}; listing/search works on the abstraction.
 */
@Service
public class ExchangeService {

    private final VirtualExchangeRepository exchangeRepository;

    public ExchangeService(VirtualExchangeRepository exchangeRepository) {
        this.exchangeRepository = exchangeRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ExchangeDtos.ExchangeResponse> search(
            ExchangeLevel level,
            com.yep.domain.enums.LevelOneTopic topic,
            com.yep.domain.enums.LevelTwoFocusArea focusArea,
            ExchangeStatus status,
            String query,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<Long> ids = exchangeRepository.searchIds(
                level != null ? level.name() : null,
                topic != null ? topic.name() : null,
                focusArea != null ? focusArea.name() : null,
                status != null ? status.name() : null,
                query == null || query.isBlank() ? null : query.trim(),
                from,
                to,
                pageable);
        return PageResponse.from(pageOfHydrated(ids, pageable).map(ExchangeDtos.ExchangeResponse::of));
    }

    /**
     * The native search only resolves ids; the entities are loaded through JPA so
     * that the SINGLE_TABLE inheritance discriminator is applied by Hibernate and
     * the native {@code start_date DESC} order is preserved.
     */
    private Page<VirtualExchange> pageOfHydrated(Page<Long> ids, Pageable pageable) {
        if (ids.isEmpty()) {
            return Page.empty(pageable);
        }
        Map<Long, VirtualExchange> byId = exchangeRepository.findAllById(ids.getContent()).stream()
                .collect(Collectors.toMap(VirtualExchange::getId, exchange -> exchange, (left, right) -> left));
        List<VirtualExchange> ordered = ids.getContent().stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
        return new PageImpl<>(ordered, pageable, ids.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ExchangeDtos.ExchangeResponse get(Long id) {
        return ExchangeDtos.ExchangeResponse.of(find(id));
    }

    @Transactional(readOnly = true)
    public VirtualExchange getEntity(Long id) {
        return find(id);
    }

    @Transactional
    public ExchangeDtos.ExchangeResponse create(ExchangeDtos.ExchangeRequest request) {
        VirtualExchange exchange = switch (request.level()) {
            case LEVEL_ONE -> buildLevelOne(request);
            case LEVEL_TWO -> buildLevelTwo(request);
        };
        exchange.setOrganiserId(SecurityUtils.currentUserId());
        if (request.status() != null) {
            exchange.setStatus(request.status());
        }
        return ExchangeDtos.ExchangeResponse.of(exchangeRepository.save(exchange));
    }

    @Transactional
    public ExchangeDtos.ExchangeResponse update(Long id, ExchangeDtos.ExchangeRequest request) {
        VirtualExchange exchange = find(id);

        if (exchange instanceof LevelOneExchange levelOne) {
            if (request.level() != ExchangeLevel.LEVEL_ONE) {
                throw new BadRequestException("This activity is a Level One exchange");
            }
            levelOne.setTopic(request.topic() != null ? request.topic() : levelOne.getTopic());
            levelOne.setMaxParticipants(request.maxParticipants());
            levelOne.setSessionLink(request.sessionLink());
            if (request.facilitatorId() != null) {
                levelOne.setFacilitatorId(request.facilitatorId());
            }
        } else if (exchange instanceof LevelTwoExchange levelTwo) {
            if (request.level() != ExchangeLevel.LEVEL_TWO) {
                throw new BadRequestException("This activity is a Level Two exchange");
            }
            levelTwo.setFocusArea(request.focusArea() != null ? request.focusArea() : levelTwo.getFocusArea());
            levelTwo.setMaxTeams(request.maxTeams());
            if (request.applicationsOpen() != null) {
                levelTwo.setApplicationsOpen(request.applicationsOpen());
            }
        }

        exchange.setTitle(request.title());
        if (request.description() != null) {
            exchange.setDescription(request.description());
        }
        exchange.setStartDate(request.startDate());
        exchange.setEndDate(request.endDate());
        if (request.status() != null) {
            exchange.setStatus(request.status());
        }
        return ExchangeDtos.ExchangeResponse.of(exchangeRepository.save(exchange));
    }

    @Transactional
    public ExchangeDtos.ExchangeResponse updateStatus(Long id, ExchangeStatus status) {
        if (status == null) {
            throw new BadRequestException("Status is required");
        }
        VirtualExchange exchange = find(id);
        exchange.setStatus(status);
        return ExchangeDtos.ExchangeResponse.of(exchangeRepository.save(exchange));
    }

    // ---------------------------------------------------------------------

    private LevelOneExchange buildLevelOne(ExchangeDtos.ExchangeRequest request) {
        if (request.topic() == null) {
            throw new BadRequestException("A Level One exchange requires a topic");
        }
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("End date cannot be before the start date");
        }
        LevelOneExchange exchange = new LevelOneExchange();
        applyCommon(exchange, request);
        exchange.setTopic(request.topic());
        exchange.setMaxParticipants(request.maxParticipants());
        exchange.setSessionLink(request.sessionLink());
        exchange.setFacilitatorId(request.facilitatorId());
        return exchange;
    }

    private LevelTwoExchange buildLevelTwo(ExchangeDtos.ExchangeRequest request) {
        if (request.focusArea() == null) {
            throw new BadRequestException("A Level Two exchange requires a focus area");
        }
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("End date cannot be before the start date");
        }
        LevelTwoExchange exchange = new LevelTwoExchange();
        applyCommon(exchange, request);
        exchange.setFocusArea(request.focusArea());
        exchange.setMaxTeams(request.maxTeams());
        exchange.setApplicationsOpen(request.applicationsOpen() == null || request.applicationsOpen());
        return exchange;
    }

    private void applyCommon(VirtualExchange exchange, ExchangeDtos.ExchangeRequest request) {
        exchange.setTitle(request.title());
        exchange.setDescription(request.description());
        exchange.setStartDate(request.startDate());
        exchange.setEndDate(request.endDate());
        exchange.setStatus(request.status() == null ? ExchangeStatus.OPEN : request.status());
    }

    private VirtualExchange find(Long id) {
        return exchangeRepository.findById(id).orElseThrow(() -> NotFoundException.resource("Exchange", id));
    }
}
