package com.edenilson.order_service.service.impl;

import com.edenilson.order_service.dto.request.LoginRequest;
import com.edenilson.order_service.dto.request.RegisterRequest;
import com.edenilson.order_service.dto.response.AuthResponse;
import com.edenilson.order_service.entity.Customer;
import com.edenilson.order_service.exception.BadRequestException;
import com.edenilson.order_service.exception.DuplicateEmailException;
import com.edenilson.order_service.repository.CustomerRepository;
import com.edenilson.order_service.security.JwtTokenProvider;
import com.edenilson.order_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already registered: " + request.getEmail());
        }

        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        customerRepository.save(customer);
        String token = jwtTokenProvider.generateToken(customer.getEmail());

        log.info("Customer registered: {}", customer.getEmail());

        return AuthResponse.builder()
                .token(token)
                .customerId(customer.getId())
                .email(customer.getEmail())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BadRequestException("Invalid email or password");
        }

        Customer customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        String token = jwtTokenProvider.generateToken(customer.getEmail());

        return AuthResponse.builder()
                .token(token)
                .customerId(customer.getId())
                .email(customer.getEmail())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .build();
    }
}
