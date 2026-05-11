package com.example.icsm.config;

import com.example.icsm.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationEvents {

    private final UserService userService;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent success) {
        Object principal = success.getAuthentication().getPrincipal();
        
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            userService.updateLastLogin(email);
        } else if (principal instanceof String) {
            userService.updateLastLogin((String) principal);
        }
    }
}
