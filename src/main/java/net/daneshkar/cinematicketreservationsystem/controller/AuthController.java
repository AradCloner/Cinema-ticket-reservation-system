package net.daneshkar.cinematicketreservationsystem.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {

        // ۱. اعتبارسنجی کاربر
        Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(
                loginRequest.username(), loginRequest.password());

        Authentication authenticationResponse = this.authenticationManager.authenticate(authenticationRequest);

        // ۲. ایجاد SecurityContext جدید و مقداردهی
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authenticationResponse);
        securityContextHolderStrategy.setContext(context);

        // ۳. ذخیره کانتکست در HttpSession و ست شدن خودکار کوکی JSESSIONID
        securityContextRepository.saveContext(context, request, response);

        return ResponseEntity.ok("ورود موفقیت‌آمیز بود.");
    }

    public record LoginRequest(String username, String password) {}

}
