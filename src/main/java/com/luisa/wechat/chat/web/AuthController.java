package com.luisa.wechat.chat.web;

import com.luisa.wechat.chat.persistence.model.UserRecord;
import com.luisa.wechat.chat.security.AuthTokenService;
import com.luisa.wechat.chat.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ChatService chatService;
    private final AuthenticationManager authenticationManager;
    private final AuthTokenService authTokenService;

    public AuthController(ChatService chatService,
                          AuthenticationManager authenticationManager,
                          AuthTokenService authTokenService) {
        this.chatService = chatService;
        this.authenticationManager = authenticationManager;
        this.authTokenService = authTokenService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatService.UserProfile register(@RequestBody RegisterRequest request,
                                            HttpServletRequest httpRequest,
                                            HttpServletResponse httpResponse) {
        ChatService.UserProfile profile = chatService.register(request.username(), request.displayName(), request.password());
        issueTokenCookie(request.username(), request.password(), httpRequest, httpResponse);
        return profile;
    }

    @PostMapping("/login")
    public ChatService.UserProfile login(@RequestBody LoginRequest request,
                                         HttpServletRequest httpRequest,
                                         HttpServletResponse httpResponse) {
        chatService.authenticate(request.username(), request.password());
        Authentication authentication = issueTokenCookie(request.username(), request.password(), httpRequest, httpResponse);
        UserRecord user = chatService.requireUser(authentication.getName());
        return new ChatService.UserProfile(user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl());
    }

    private Authentication issueTokenCookie(String username,
                                            String password,
                                            HttpServletRequest httpRequest,
                                            HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        String token = authTokenService.createToken(authentication.getName());
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, authTokenService.createCookie(token, httpRequest.isSecure()).toString());
        return authentication;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, authTokenService.clearCookie(request.isSecure()).toString());
    }

    @GetMapping("/me")
    public ChatService.UserProfile me(Authentication authentication) {
        return chatService.bootstrap(authentication.getName()).me();
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@RequestBody ChangePasswordRequest request, Authentication authentication) {
        chatService.changePassword(
                authentication.getName(),
                request.currentPassword(),
                request.newPassword(),
                request.confirmPassword()
        );
    }

    public record RegisterRequest(String username, String displayName, String password) {
    }

    public record LoginRequest(String username, String password) {
    }

    public record ChangePasswordRequest(String currentPassword, String newPassword, String confirmPassword) {
    }
}
