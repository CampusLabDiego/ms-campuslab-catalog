package cl.campuslab.catalog.messaging;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockResultOutboxRepository
        extends JpaRepository<StockResultOutbox, String> {

    List<StockResultOutbox>
    findTop20ByStateAndNextAttemptAtLessThanEqualOrderByProcessedAtAsc(
            StockResultOutbox.State state,
            Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select result
            from StockResultOutbox result
            where result.operationId = :operationId
            """)
    Optional<StockResultOutbox> findLockedByOperationId(
            @Param("operationId") String operationId);
}