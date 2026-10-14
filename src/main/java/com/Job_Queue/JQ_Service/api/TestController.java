package com.Job_Queue.JQ_Service.api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@Slf4j
public class TestController {

    @PostMapping("/test/receive")
    public ResponseEntity<?> receive(@RequestBody Map<String, Object> body) {
        log.info("Webhook received: {}", body);
        return ResponseEntity.ok().build();
    }
}