package com.bookbridge.controller;

import com.bookbridge.dto.*;
import com.bookbridge.security.UserPrincipal;
import com.bookbridge.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication & User Management", description = "Endpoints for student registration, login, profile audits, and verification")
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new student account", description = "Checks duplicate emails and sets default USER role. If email suffix ends with '@bookbridge.com', sets ADMIN role.")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.ok(ApiResponse.success("User registered successfully. An email verification alert has been mock logged.", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user credentials", description = "Checks credentials and returns a secure JWT bearer token.")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest request) {
        JwtResponse response = userService.authenticateUser(request);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful.", response));
    }

    @GetMapping("/profile")
    @Operation(summary = "Retrieve current user profile", description = "Returns active user properties using the JWT bearer token.")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse response = userService.getProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Profile details retrieved successfully.", response));
    }

    @PutMapping("/profile")
    @Operation(summary = "Edit current user profile details", description = "Updates firstName and lastName of active user.")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                                                    @Valid @RequestBody UserUpdateRequest request) {
        UserResponse response = userService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully.", response));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Trigger mock email verification", description = "Verifies active user email status.")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@AuthenticationPrincipal UserPrincipal principal) {
        userService.verifyEmail(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Email verification successful. Your account status is verified."));
    }
}
