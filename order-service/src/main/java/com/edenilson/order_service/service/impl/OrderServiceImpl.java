package com.edenilson.order_service.service.impl;

import com.edenilson.order_service.dto.external.ProductResponse;
import com.edenilson.order_service.dto.request.CreateOrderRequest;
import com.edenilson.order_service.dto.request.OrderDetailRequest;
import com.edenilson.order_service.dto.request.UpdateOrderRequest;
import com.edenilson.order_service.dto.response.CustomerResponse;
import com.edenilson.order_service.dto.response.OrderDetailResponse;
import com.edenilson.order_service.dto.response.OrderResponse;
import com.edenilson.order_service.entity.Customer;
import com.edenilson.order_service.entity.Order;
import com.edenilson.order_service.entity.OrderDetail;
import com.edenilson.order_service.entity.enums.OrderStatus;
import com.edenilson.order_service.exception.BadRequestException;
import com.edenilson.order_service.exception.ExternalServiceException;
import com.edenilson.order_service.exception.ResourceNotFoundException;
import com.edenilson.order_service.repository.CustomerRepository;
import com.edenilson.order_service.repository.OrderRepository;
import com.edenilson.order_service.service.OrderService;
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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final RestTemplate restTemplate;

    @Value("${service.product.url}")
    private String productServiceUrl;

    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + id + " not found"));
        return mapToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer with id " + request.getCustomerId() + " not found"));

        Order order = Order.builder()
                .customer(customer)
                .totalAmount(BigDecimal.ZERO)
                .build();

        List<OrderDetail> details = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderDetailRequest item : request.getItems()) {
            ProductResponse product = fetchProduct(item.getProductId());

            BigDecimal unitPrice = BigDecimal.valueOf(product.getPrice());
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            OrderDetail detail = OrderDetail.builder()
                    .order(order)
                    .productId(product.getId())
                    .productName(product.getTitle())
                    .quantity(item.getQuantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();

            details.add(detail);
            totalAmount = totalAmount.add(subtotal);
        }

        order.setOrderDetails(details);
        order.setTotalAmount(totalAmount);

        Order saved = orderRepository.save(order);
        log.info("Order created with id: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(Long id, UpdateOrderRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + id + " not found"));

        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.SHIPPED) {
            throw new BadRequestException("Cannot update an order that has been paid or shipped");
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer with id " + request.getCustomerId() + " not found"));

        order.setCustomer(customer);
        order.getOrderDetails().clear();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderDetailRequest item : request.getItems()) {
            ProductResponse product = fetchProduct(item.getProductId());

            BigDecimal unitPrice = BigDecimal.valueOf(product.getPrice());
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            OrderDetail detail = OrderDetail.builder()
                    .order(order)
                    .productId(product.getId())
                    .productName(product.getTitle())
                    .quantity(item.getQuantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();

            order.getOrderDetails().add(detail);
            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);
        Order saved = orderRepository.save(order);
        log.info("Order {} updated", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + id + " not found"));

        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.SHIPPED) {
            throw new BadRequestException("Cannot delete an order that has been paid or shipped");
        }

        orderRepository.delete(order);
        log.info("Order {} deleted", id);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + id + " not found"));

        try {
            OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());
            order.setStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid order status: " + status);
        }

        Order saved = orderRepository.save(order);
        log.info("Order {} status updated to {}", id, status);
        return mapToResponse(saved);
    }

    private ProductResponse fetchProduct(Long productId) {
        try {
            ResponseEntity<ProductResponse> response = restTemplate.getForEntity(
                    productServiceUrl + "/api/products/{id}", ProductResponse.class, productId);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new BadRequestException("Product with id " + productId + " not found");
        } catch (RestClientException e) {
            log.error("Failed to fetch product {}: {}", productId, e.getMessage());
            throw new ExternalServiceException("Product service is unavailable");
        }
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderDetailResponse> details = order.getOrderDetails().stream()
                .map(d -> OrderDetailResponse.builder()
                        .id(d.getId())
                        .productId(d.getProductId())
                        .productName(d.getProductName())
                        .quantity(d.getQuantity())
                        .unitPrice(d.getUnitPrice())
                        .subtotal(d.getSubtotal())
                        .build())
                .toList();

        Customer customer = order.getCustomer();
        CustomerResponse customerResponse = CustomerResponse.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .build();

        return OrderResponse.builder()
                .id(order.getId())
                .customer(customerResponse)
                .orderDate(order.getOrderDate())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .orderDetails(details)
                .build();
    }
}
