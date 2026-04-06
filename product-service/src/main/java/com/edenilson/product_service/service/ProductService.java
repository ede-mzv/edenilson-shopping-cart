package com.edenilson.product_service.service;

import com.edenilson.product_service.dto.ProductResponse;

import java.util.List;

public interface ProductService {

    List<ProductResponse> getAllProducts(Integer limit, String sort);

    ProductResponse getProductById(Long id);

    List<String> getAllCategories();

    List<ProductResponse> getProductsByCategory(String category);
}
