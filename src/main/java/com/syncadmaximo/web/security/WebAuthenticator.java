package com.syncadmaximo.web.security;

import java.util.Optional;

public interface WebAuthenticator {

    Optional<WebPrincipal> authenticate(String username, String password);
}
