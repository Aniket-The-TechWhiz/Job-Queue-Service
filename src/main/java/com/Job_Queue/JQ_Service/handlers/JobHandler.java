package com.Job_Queue.JQ_Service.handlers;

public interface JobHandler {
    void handle(String payload) throws Exception;
}