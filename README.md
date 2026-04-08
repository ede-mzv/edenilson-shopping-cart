# Shopping Cart - Microservices

Backend solution built with **Spring Boot 3.5** and **Java 17**, following a microservices architecture. The system manages products, orders, and payments across three independent services.

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ product-service  │     │  order-service   │     │ payment-service  │
│    (port 8081)   │◄────│   (port 8082)    │◄────│   (port 8083)    │
│                  │     │                  │     │                  │
│ Proxy to         │     │ Orders + Auth    │     │ Payment          │
│ FakeStoreAPI     │     │ JWT Provider     │     │ Simulation       │
│ PUBLIC           │     │ SECURED          │     │ SECURED          │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

### Inter-service Communication
- **order-service → product-service**: Validates products and fetches prices when creating orders
- **payment-service → order-service**: Validates order status and updates it to PAID after successful payment

## Tech Stack

- Java 17
- Spring Boot 3.5
- Spring Security + JWT (JJWT 0.12.5)
- Spring Data JPA
- H2 In-Memory Database
- Lombok
- Bean Validation
- RestTemplate

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+

### Running the Services

Start each service in a separate terminal, in this order:

```bash
# 1. Product Service
cd product-service
./mvnw spring-boot:run

# 2. Order Service
cd order-service
./mvnw spring-boot:run

# 3. Payment Service
cd payment-service
./mvnw spring-boot:run
```

### H2 Console
- Order Service: http://localhost:8082/h2-console (JDBC URL: `jdbc:h2:mem:orderdb`)
- Payment Service: http://localhost:8083/h2-console (JDBC URL: `jdbc:h2:mem:paymentdb`)

## API Endpoints

### Product Service (port 8081) - Public

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/products` | Get all products (supports `?limit=N&sort=asc\|desc`) |
| GET | `/api/products/{id}` | Get product by ID |
| GET | `/api/products/categories` | Get all categories |
| GET | `/api/products/category/{name}` | Get products by category |

### Order Service (port 8082) - Secured

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new customer (public) |
| POST | `/api/auth/login` | Login and get JWT token (public) |
| GET | `/api/orders` | Get all orders |
| GET | `/api/orders/{id}` | Get order by ID |
| POST | `/api/orders` | Create a new order |
| PUT | `/api/orders/{id}` | Update an order |
| DELETE | `/api/orders/{id}` | Delete an order |
| PATCH | `/api/orders/{id}/status` | Update order status |

### Payment Service (port 8083) - Secured

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payments` | Process a payment |
| GET | `/api/payments` | Get all payments |
| GET | `/api/payments/{id}` | Get payment by ID |
| GET | `/api/payments/order/{orderId}` | Get payment by order ID |

## Authentication Flow

1. **Register** a customer via `POST /api/auth/register`
2. **Login** via `POST /api/auth/login` to get a JWT token
3. Include the token in subsequent requests: `Authorization: Bearer <token>`
4. Token is automatically forwarded from payment-service to order-service

## Testing with Postman

1. Import the collection from `postman/Edenilson-Shopping-Cart.postman_collection.json`
2. Import the environment from `postman/Edenilson-Shopping-Cart.postman_environment.json`
3. Select the **Shopping Cart - Local** environment
4. Run requests in order:
   - Register → Login (token auto-saved)
   - Create Order (order_id and customer_id auto-saved)
   - Process Payment (payment_id auto-saved)
   - Verify order status changed to PAID

## Payment Simulation Logic

- Orders under $10,000: always succeeds
- Orders $10,000+: 90% success rate
- On success: payment status = COMPLETED, order status updated to PAID
- On failure: payment status = FAILED, order status unchanged

## Design Patterns

| Pattern | Usage |
|---------|-------|
| Proxy | product-service proxies FakeStoreAPI |
| Repository | JPA repositories for data access |
| DTO | Strict entity/DTO separation |
| Builder | Lombok @Builder on entities and DTOs |
| Filter Chain | JWT authentication filter |
| Service Layer | Business logic separated from controllers |

## Project Structure

Each service follows the same layered structure:
```
com.edenilson.{service}/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── dto/             # Request/Response DTOs
├── entity/          # JPA entities
├── exception/       # Custom exceptions + global handler
├── repository/      # JPA repositories
├── security/        # JWT security (order & payment)
└── service/         # Business logic (interface + impl)
```
