package com.edenilson.payment_service.service.impl;

import com.edenilson.payment_service.dto.external.OrderResponse;
import com.edenilson.payment_service.dto.request.PaymentRequest;
import com.edenilson.payment_service.dto.response.PaymentResponse;
import com.edenilson.payment_service.entity.Payment;
import com.edenilson.payment_service.entity.enums.PaymentMethod;
import com.edenilson.payment_service.entity.enums.PaymentStatus;
import com.edenilson.payment_service.exception.ExternalServiceException;
import com.edenilson.payment_service.exception.PaymentNotFoundException;
import com.edenilson.payment_service.exception.PaymentProcessingException;
import com.edenilson.payment_service.repository.PaymentRepository;
import com.edenilson.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate;
    private final Random random = new Random();

    @Value("${service.order.url}")
    private String orderServiceUrl;

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PaymentProcessingException("Invalid payment method: " + request.getPaymentMethod());
        }

        OrderResponse order = fetchOrder(request.getOrderId());

        if (!"PENDING".equals(order.getStatus()) && !"CONFIRMED".equals(order.getStatus())) {
            throw new PaymentProcessingException(
                    "Order is not eligible for payment. Current status: " + order.getStatus());
        }

        Payment payment = Payment.builder()
                .orderId(order.getId())
                .amount(order.getTotalAmount())
                .paymentMethod(method)
                .build();

        boolean success = simulatePayment(order.getTotalAmount());

        if (success) {
            payment.setStatus(PaymentStatus.COMPLETED);
            Payment saved = paymentRepository.save(payment);
            updateOrderStatus(order.getId(), "PAID");
            log.info("Payment {} completed for order {}", saved.getId(), order.getId());
            return mapToResponse(saved);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            Payment saved = paymentRepository.save(payment);
            log.warn("Payment {} failed for order {}", saved.getId(), order.getId());
            return mapToResponse(saved);
        }
    }

    @Override
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment with id " + id + " not found"));
        return mapToResponse(payment);
    }

    @Override
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment for order " + orderId + " not found"));
        return mapToResponse(payment);
    }

    @Override
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private boolean simulatePayment(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.valueOf(10000)) < 0) {
            return true;
        }
        return random.nextDouble() < 0.9;
    }

    private OrderResponse fetchOrder(Long orderId) {
        try {
            ResponseEntity<OrderResponse> response = restTemplate.getForEntity(
                    orderServiceUrl + "/api/orders/{id}", OrderResponse.class, orderId);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new PaymentProcessingException("Order with id " + orderId + " not found");
        } catch (RestClientException e) {
            log.error("Failed to fetch order {}: {}", orderId, e.getMessage());
            throw new ExternalServiceException("Order service is unavailable");
        }
    }

    private void updateOrderStatus(Long orderId, String status) {
        try {
            restTemplate.patchForObject(
                    orderServiceUrl + "/api/orders/{id}/status",
                    java.util.Map.of("status", status),
                    Void.class,
                    orderId);
        } catch (RestClientException e) {
            log.error("Failed to update order {} status: {}", orderId, e.getMessage());
        }
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod().name())
                .paymentDate(payment.getPaymentDate())
                .status(payment.getStatus().name())
                .transactionId(payment.getTransactionId())
                .build();
    }
}
