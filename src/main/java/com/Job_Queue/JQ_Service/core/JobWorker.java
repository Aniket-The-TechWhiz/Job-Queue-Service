package com.Job_Queue.JQ_Service.core;

import com.Job_Queue.JQ_Service.handlers.EmailHandler;
import com.Job_Queue.JQ_Service.handlers.PdfHandler;
import com.Job_Queue.JQ_Service.handlers.WebhookHandler;
import com.Job_Queue.JQ_Service.model.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobWorker {

    private final JobService jobService;
    private final EmailHandler emailHandler;
    private final WebhookHandler webhookHandler;
    private final PdfHandler pdfHandler;

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        pollOnce();
    }

    public void pollOnce() {
        jobService.claimNextJob().ifPresent(job -> {
            log.info("Picked up job {} of type {}", job.getId(), job.getType());
            try {
                dispatch(job);
                jobService.markDone(job);
                log.info("Job {} completed", job.getId());
            } catch (Exception e) {
                log.error("Job {} failed: {}", job.getId(), e.getMessage());
                jobService.markFailed(job, e.getMessage());
            }
        });
    }
    private void dispatch(Job job) throws Exception {
        switch (job.getType()) {
            case "SEND_EMAIL"   -> emailHandler.handle(job.getPayload());
            case "SEND_WEBHOOK" -> webhookHandler.handle(job.getPayload());
            case "GENERATE_PDF" -> pdfHandler.handle(job.getPayload());
            default -> throw new IllegalArgumentException("Unknown job type: " + job.getType());
        }
    }
}