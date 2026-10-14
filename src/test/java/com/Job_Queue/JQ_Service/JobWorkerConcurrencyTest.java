package com.Job_Queue.JQ_Service;

import com.Job_Queue.JQ_Service.core.JobRepository;
import com.Job_Queue.JQ_Service.core.JobWorker;
import com.Job_Queue.JQ_Service.model.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Testcontainers
class JobWorkerConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("jobqueue")
            .withUsername("jobqueue")
            .withPassword("jobqueue");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobWorker jobWorker;

    @BeforeEach
    void cleanUp() {
        jobRepository.deleteAll();
    }

    @Test
    void threeWorkersShouldNotProcessSameJobTwice() throws Exception {
        // insert 10 jobs
        for (int i = 0; i < 10; i++) {
            Job job = new Job();
            job.setType("SEND_EMAIL");
            job.setPayload("{\"to\":\"test@example.com\"}");
            job.setStatus("PENDING");
            job.setAttempts(0);
            job.setMaxAttempts(5);
            job.setRunAt(LocalDateTime.now());
            job.setCreatedAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());
            jobRepository.save(job);
        }

        // 3 concurrent workers
        ExecutorService pool = Executors.newFixedThreadPool(3);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            futures.add(pool.submit(() -> {
                for (int j = 0; j < 10; j++) {
                    jobWorker.pollOnce();
                }
            }));
        }

        // wait for all workers to finish
        for (Future<?> f : futures) {
            f.get();
        }

        pool.shutdown();

        // every job processed exactly once
        assertThat(jobRepository.countByStatus("DONE")).isEqualTo(10);
        assertThat(jobRepository.countByStatus("PENDING")).isEqualTo(0);
    }
}