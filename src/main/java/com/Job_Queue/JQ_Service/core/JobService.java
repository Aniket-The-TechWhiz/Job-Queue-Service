package com.Job_Queue.JQ_Service.core;

import com.Job_Queue.JQ_Service.model.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;

    @Transactional
    public Optional<Job> claimNextJob() {
        return jobRepository.findNextJob().map(job -> {
            job.setStatus("RUNNING");
            job.setUpdatedAt(LocalDateTime.now());
            return jobRepository.save(job);
        });
    }

    @Transactional
    public void markDone(Job job) {
        job.setStatus("DONE");
        job.setCompletedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    @Transactional
    public void markFailed(Job job, String error) {
        job.setAttempts(job.getAttempts() + 1);
        job.setLastError(error);
        job.setUpdatedAt(LocalDateTime.now());

        if (job.getAttempts() >= job.getMaxAttempts()) {
            job.setStatus("DEAD");
            log.warn("Job {} moved to DEAD after {} attempts", job.getId(), job.getAttempts());
        } else {
            job.setStatus("FAILED");
            long delaySeconds = (long) Math.pow(2, job.getAttempts());
            job.setRunAt(LocalDateTime.now().plusSeconds(delaySeconds));
            log.info("Job {} failed, retrying in {}s", job.getId(), delaySeconds);
        }

        jobRepository.save(job);
    }

    @Transactional
    public Job submit(String type, String payload) {
        Job job = new Job();
        job.setType(type);
        job.setPayload(payload);
        job.setStatus("PENDING");
        job.setAttempts(0);
        job.setMaxAttempts(5);
        job.setRunAt(LocalDateTime.now());
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        return jobRepository.save(job);
    }

    public Optional<Job> findById(UUID id) {
        return jobRepository.findById(id);
    }

    public List<Job> findJobs(String status, String type, int limit) {
        return jobRepository.findJobsFiltered(status, type, limit);
    }

    @Transactional
    public Optional<Job> resubmit(UUID id) {
        return jobRepository.findById(id).map(job -> {
            job.setStatus("PENDING");
            job.setAttempts(0);
            job.setRunAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());
            return jobRepository.save(job);
        });
    }

    @Transactional
    public Optional<Job> cancel(UUID id) {
        return jobRepository.findById(id).map(job -> {
            if (job.getStatus().equals("PENDING")) {
                job.setStatus("CANCELLED");
                job.setUpdatedAt(LocalDateTime.now());
                return jobRepository.save(job);
            }
            return job;
        });
    }
}
