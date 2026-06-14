package com.syncadmaximo.web.security;

import com.syncadmaximo.model.AdUser;
import com.syncadmaximo.service.DirectoryService;
import com.syncadmaximo.util.StringSanitizer;

import java.util.Optional;

public class DirectoryBackedWebAuthenticator implements WebAuthenticator {

    private final DirectoryService directoryService;
    private final String allowedUser;

    public DirectoryBackedWebAuthenticator(DirectoryService directoryService, String allowedUser) {
        this.directoryService = directoryService;
        this.allowedUser = StringSanitizer.normalizeUserName(allowedUser);
    }

    @Override
    public Optional<WebPrincipal> authenticate(String username, String password) {
        String normalizedUser = StringSanitizer.normalizeUserName(username);
        String normalizedPassword = StringSanitizer.trimToNull(password);
        if (normalizedUser == null || normalizedPassword == null) {
            return Optional.empty();
        }
        if (allowedUser != null && !allowedUser.equalsIgnoreCase(normalizedUser)) {
            return Optional.empty();
        }

        if (directoryService == null) {
            return Optional.of(new WebPrincipal(normalizedUser, normalizedUser, allowedUser == null ? "maxadmin" : allowedUser));
        }

        Optional<AdUser> user = directoryService.findByUserName(normalizedUser)
                .filter(AdUser::isEnabled);
        if (!user.isPresent()) {
            return Optional.empty();
        }

        AdUser adUser = user.get();
        String displayName = StringSanitizer.trimToNull(adUser.getDisplayName());
        if (displayName == null) {
            displayName = StringSanitizer.trimToNull(adUser.getGivenName());
        }
        if (displayName == null) {
            displayName = normalizedUser;
        }
        return Optional.of(new WebPrincipal(normalizedUser, displayName, allowedUser == null ? "maxadmin" : allowedUser));
    }
}
