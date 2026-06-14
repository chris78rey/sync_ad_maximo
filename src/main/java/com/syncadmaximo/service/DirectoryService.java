package com.syncadmaximo.service;

import com.syncadmaximo.model.AdUser;

import java.util.List;
import java.util.Optional;

public interface DirectoryService {

    List<AdUser> findEnabledUsers();

    List<AdUser> findDisabledUsers();

    Optional<AdUser> findByUserName(String userName);
}
