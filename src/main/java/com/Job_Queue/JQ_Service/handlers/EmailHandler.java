package com.Job_Queue.JQ_Service.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailHandler implements JobHandler {

    @Override
    public void handle(String payload) throws Exception {
        log.info("EMAIL job executed with payload: {}", payload);
    }
}