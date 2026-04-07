package com.edenilson.order_service.service;

import com.edenilson.order_service.dto.request.CreateOrderRequest;
import com.edenilson.order_service.dto.request.UpdateOrderRequest;
import com.edenilson.order_service.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {

    List<OrderResponse> getAllOrders();

    OrderResponse getOrderById(Long id);

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse updateOrder(Long id, UpdateOrderRequest request);

    void deleteOrder(Long id);

    OrderResponse updateOrderStatus(Long id, String status);
}
