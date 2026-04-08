package com.edenilson.order_service.service;

import com.edenilson.order_service.dto.request.LoginRequest;
import com.edenilson.order_service.dto.request.RegisterRequest;
import com.edenilson.order_service.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
