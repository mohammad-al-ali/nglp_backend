package com.NGLP.backend.v1.dto;

import com.NGLP.backend.v1.entity.User;

public record AuthResponse(User user, String token) {}
