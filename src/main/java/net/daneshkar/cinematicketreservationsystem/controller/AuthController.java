package net.daneshkar.cinematicketreservationsystem.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import net.daneshkar.cinematicketreservationsystem.dto.ApiResponse;
import net.daneshkar.cinematicketreservationsystem.dto.auth.CsrfResponseDto;
import net.daneshkar.cinematicketreservationsystem.dto.auth.LoginRequestDto;
import net.daneshkar.cinematicketreservationsystem.dto.auth.RegisterRequestDto;
import net.daneshkar.cinematicketreservationsystem.dto.auth.UserResponseDto;
import net.daneshkar.cinematicketreservationsystem.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;


    private final AuthService authService;

    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<CsrfResponseDto>> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(new ApiResponse<>("توکن CSRF", new CsrfResponseDto(
                csrfToken.getHeaderName(), csrfToken.getParameterName(), csrfToken.getToken())));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@Valid @RequestBody LoginRequestDto loginRequestDto,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {

        // ۱. اعتبارسنجی کاربر
        Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(
                loginRequestDto.username(), loginRequestDto.password());

        Authentication authenticationResponse = this.authenticationManager.authenticate(authenticationRequest);

        sessionAuthenticationStrategy.onAuthentication(
                authenticationResponse,
                request,
                response
        );


        // ۲. ایجاد SecurityContext جدید و مقداردهی
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authenticationResponse);
        securityContextHolderStrategy.setContext(context);

        // ۳. ذخیره کانتکست در HttpSession و ست شدن خودکار کوکی JSESSIONID
        securityContextRepository.saveContext(context, request, response);

        return ResponseEntity.ok(new ApiResponse<>("ورود موفقیت‌آمیز بود.", null));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDto>> register(@Valid @RequestBody RegisterRequestDto registerRequestDto){
        UserResponseDto registered = authService.register(registerRequestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>("ثبت‌نام با موفقیت انجام شد.", registered));

    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> me(Authentication authentication){
        UserResponseDto currentUser = authService.getCurrentUser(authentication.getName());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse<>("کاربر یافت شد.", currentUser));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {

        SecurityContextLogoutHandler logoutHandler =
                new SecurityContextLogoutHandler();

        logoutHandler.logout(request, response, authentication);

        return ResponseEntity.ok(
                new ApiResponse<>("خروج با موفقیت انجام شد.", null));
    }

}
