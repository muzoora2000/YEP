package com.yep.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yep.domain.exchange.VirtualExchange;

/**
 * All activity search runs as one native query because both exchange levels
 * live in the single {@code virtual_exchanges} table - the level specific
 * columns ({@code topic}, {@code focus_area}) are therefore addressable
 * without any polymorphic join gymnastics.
 *
 * <p>The query is intentionally unsorted: ordering is fixed by
 * {@code start_date DESC} so the native statement can be paginated as is.</p>
 */
public interface VirtualExchangeRepository extends JpaRepository<VirtualExchange, Long> {

    /**
     * Returns ids only: a native {@code SELECT *} would have to hydrate the
     * inheritance discriminator itself, which Hibernate does not do reliably for
     * native SQL. The service re-loads the entities through JPA (which is fully
     * polymorphic) and restores the native ordering.
     */
    @Query(value = """
            SELECT e.id FROM virtual_exchanges e
             WHERE (:level IS NULL OR e.exchange_level = :level)
               AND (:topic IS NULL OR e.topic = :topic)
               AND (:focus IS NULL OR e.focus_area = :focus)
               AND (:status IS NULL OR e.status = :status)
               AND (:q IS NULL
                    OR LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(e.description) LIKE LOWER(CONCAT('%', :q, '%')))
               AND (:fromDate IS NULL OR e.start_date >= :fromDate)
               AND (:toDate IS NULL OR e.start_date <= :toDate)
            ORDER BY e.start_date DESC
            """, countQuery = """
            SELECT COUNT(*) FROM virtual_exchanges e
             WHERE (:level IS NULL OR e.exchange_level = :level)
               AND (:topic IS NULL OR e.topic = :topic)
               AND (:focus IS NULL OR e.focus_area = :focus)
               AND (:status IS NULL OR e.status = :status)
               AND (:q IS NULL
                    OR LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(e.description) LIKE LOWER(CONCAT('%', :q, '%')))
               AND (:fromDate IS NULL OR e.start_date >= :fromDate)
               AND (:toDate IS NULL OR e.start_date <= :toDate)
            """, nativeQuery = true)
    Page<Long> searchIds(
            @Param("level") String level,
            @Param("topic") String topic,
            @Param("focus") String focus,
            @Param("status") String status,
            @Param("q") String query,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    List<VirtualExchange> findByStatus(com.yep.domain.enums.ExchangeStatus status);

    @Query("SELECT e FROM VirtualExchange e WHERE e.organiserId = :userId ORDER BY e.startDate DESC")
    List<VirtualExchange> findByOrganiser(@Param("userId") Long userId);
}
