package com.edenilson.product_service.service.impl;

import com.edenilson.product_service.dto.ProductResponse;
import com.edenilson.product_service.exception.ExternalApiException;
import com.edenilson.product_service.exception.ProductNotFoundException;
import com.edenilson.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final RestTemplate restTemplate;

    @Value("${api.fakestore.base-url}")
    private String baseUrl;

    @Override
    public List<ProductResponse> getAllProducts(Integer limit, String sort) {
        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl + "/products");

            if (limit != null) {
                uriBuilder.queryParam("limit", limit);
            }
            if (sort != null) {
                uriBuilder.queryParam("sort", sort);
            }

            ResponseEntity<List<ProductResponse>> response = restTemplate.exchange(
                    uriBuilder.toUriString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to fetch products from FakeStoreAPI: {}", e.getMessage());
            throw new ExternalApiException("Failed to fetch products from external API", e);
        }
    }

    @Override
    public ProductResponse getProductById(Long id) {
        try {
            ResponseEntity<ProductResponse> response = restTemplate.getForEntity(
                    baseUrl + "/products/{id}", ProductResponse.class, id);
            ProductResponse body = response.getBody();
            if (body == null || body.getId() == null) {
                throw new ProductNotFoundException("Product with id " + id + " not found");
            }
            return body;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ProductNotFoundException("Product with id " + id + " not found");
        } catch (RestClientException e) {
            log.error("Failed to fetch product {} from FakeStoreAPI: {}", id, e.getMessage());
            throw new ExternalApiException("Failed to fetch product from external API", e);
        }
    }

    @Override
    public List<String> getAllCategories() {
        try {
            ResponseEntity<List<String>> response = restTemplate.exchange(
                    baseUrl + "/products/categories",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to fetch categories from FakeStoreAPI: {}", e.getMessage());
            throw new ExternalApiException("Failed to fetch categories from external API", e);
        }
    }

    @Override
    public List<ProductResponse> getProductsByCategory(String category) {
        try {
            ResponseEntity<List<ProductResponse>> response = restTemplate.exchange(
                    baseUrl + "/products/category/{category}",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {},
                    category
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to fetch products for category {} from FakeStoreAPI: {}", category, e.getMessage());
            throw new ExternalApiException("Failed to fetch products by category from external API", e);
        }
    }
}
