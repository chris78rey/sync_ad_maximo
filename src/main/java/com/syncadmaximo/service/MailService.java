package com.syncadmaximo.service;

import com.syncadmaximo.model.SyncExecution;

public interface MailService {

    void sendExecutionSummary(SyncExecution execution, String body);
}
