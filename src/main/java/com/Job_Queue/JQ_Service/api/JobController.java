package com.Job_Queue.JQ_Service.api;
import com.Job_Queue.JQ_Service.core.JobService;
import com.Job_Queue.JQ_Service.model.Job;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<?> submitJob(@RequestBody Map<String, Object> request) {
        String type = (String) request.get("type");
        Object payload = request.get("payload");

        if (type == null || type.isBlank()) {
            return ResponseEntity.badRequest().body("type is required");
        }

        List<String> knownTypes = List.of("SEND_EMAIL", "SEND_WEBHOOK", "GENERATE_PDF");
        if (!knownTypes.contains(type)) {
            return ResponseEntity.badRequest().body("Unknown job type: " + type);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            String payloadJson = mapper.writeValueAsString(payload);
            Job job = jobService.submit(type, payloadJson);
            return ResponseEntity.accepted().body(Map.of(
                    "jobId", job.getId(),
                    "status", job.getStatus(),
                    "createdAt", job.getCreatedAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to submit job");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getJob(@PathVariable UUID id) {
        return jobService.findById(id)
                .map(job -> ResponseEntity.ok().body(Map.of(
                        "jobId", job.getId(),
                        "type", job.getType(),
                        "status", job.getStatus(),
                        "attempts", job.getAttempts(),
                        "createdAt", job.getCreatedAt(),
                        "updatedAt", job.getUpdatedAt(),
                        "completedAt", job.getCompletedAt() != null ? job.getCompletedAt() : "N/A",
                        "lastError", job.getLastError() != null ? job.getLastError() : "N/A"
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}