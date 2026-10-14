package com.Job_Queue.JQ_Service.core;

import com.Job_Queue.JQ_Service.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    @Query(value = """
        SELECT * FROM jobs
        WHERE status IN ('PENDING', 'FAILED')
        AND run_at <= NOW()
        ORDER BY created_at ASC
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<Job> findNextJob();

    int countByStatus(String status);

    @Query(value = """
    SELECT * FROM jobs
    WHERE (:status IS NULL OR status = :status)
    AND (:type IS NULL OR type = :type)
    ORDER BY created_at DESC
    LIMIT :limit
    """, nativeQuery = true)
    List<Job> findJobsFiltered(
            @Param("status") String status,
            @Param("type") String type,
            @Param("limit") int limit);
}