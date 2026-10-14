package com.Job_Queue.JQ_Service.api;
import com.Job_Queue.JQ_Service.core.JobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/jobs")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final JobService jobService;

    @GetMapping
    public ResponseEntity<?> listJobs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(jobService.findJobs(status, type, limit));
    }

    @PostMapping("/{id}/resubmit")
    public ResponseEntity<?> resubmit(@PathVariable UUID id) {
        return jobService.resubmit(id)
                .map(job -> ResponseEntity.ok().body(Map.of(
                        "jobId", job.getId(),
                        "status", job.getStatus(),
                        "attempts", job.getAttempts()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancel(@PathVariable UUID id) {
        return jobService.cancel(id)
                .map(job -> ResponseEntity.ok().body(Map.of(
                        "jobId", job.getId(),
                        "status", job.getStatus()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}