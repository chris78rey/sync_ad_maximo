package com.syncadmaximo.service;

import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncResult;

public interface SyncService {

    SyncResult execute(SyncExecution execution);
}
