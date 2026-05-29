package com.bookbridge.service;

import com.bookbridge.dto.*;

public interface UserService {
    UserResponse registerUser(RegisterRequest request);
    JwtResponse authenticateUser(LoginRequest request);
    UserResponse getProfile(Long userId);
    UserResponse updateProfile(Long userId, UserUpdateRequest request);
    void verifyEmail(Long userId);
}
