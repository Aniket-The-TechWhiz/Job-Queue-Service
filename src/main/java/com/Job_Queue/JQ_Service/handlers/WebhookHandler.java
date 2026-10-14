package com.Job_Queue.JQ_Service.handlers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WebhookHandler implements JobHandler {

    private final RestClient restClient;

    public WebhookHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    public void handle(String payload) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(payload);

        String url = json.get("url").asText();
        JsonNode body = json.get("body");

        log.info("WEBHOOK: posting to {}", url);

        restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body.toString())
                .retrieve()
                .toBodilessEntity();

        log.info("WEBHOOK: successfully posted to {}", url);
    }
}