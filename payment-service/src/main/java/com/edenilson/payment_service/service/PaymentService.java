package com.edenilson.payment_service.service;

import com.edenilson.payment_service.dto.request.PaymentRequest;
import com.edenilson.payment_service.dto.response.PaymentResponse;

import java.util.List;

public interface PaymentService {

    PaymentResponse processPayment(PaymentRequest request);

    PaymentResponse getPaymentById(Long id);

    PaymentResponse getPaymentByOrderId(Long orderId);

    List<PaymentResponse> getAllPayments();
}
