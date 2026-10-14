package com.Job_Queue.JQ_Service.handlers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
@Slf4j
public class PdfHandler implements JobHandler {

    @Override
    public void handle(String payload) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(payload);

        String title = json.get("title").asText();
        String content = json.get("content").asText();

        String fileName = "/tmp/report_" + UUID.randomUUID() + ".txt";
        String fileContent = "Title: " + title + "\n\nContent: " + content;

        Files.writeString(Path.of(fileName), fileContent);

        log.info("PDF: report generated at {}", fileName);
    }
}
