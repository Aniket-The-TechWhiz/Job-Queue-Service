package com.Job_Queue.JQ_Service.sdk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
public class JobQueueClient {

    private final RestClient restClient;

    public JobQueueClient(String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public String submit(String type, Map<String, Object> payload) {
        log.info("Submitting job of type {}", type);
        Map<String, Object> request = Map.of(
                "type", type,
                "payload", payload
        );

        return restClient.post()
                .uri("/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);
    }
}