# KodCart Backend

REST API for KodCart, an e-commerce application built with Spring Boot. The backend provides authentication, product browsing, cart management, orders, Razorpay payments, and administrator operations.

## Features

- User registration, login, logout, and JWT-based authentication
- Product and category management
- Product image support
- Shopping cart operations and cart item counts
- Order history and order processing
- Razorpay payment integration
- Administrator APIs for users, products, and business operations
- MySQL persistence through Spring Data JPA
- Unit and integration tests with Spring Boot Test and Mockito

## Technology Stack

- Java 17
- Spring Boot 3.4.1
- Spring Web
- Spring Data JPA / Hibernate
- MySQL
- JSON Web Tokens (JJWT 0.11.5)
- Razorpay Java SDK
- Maven

## Project Structure

```text
src/main/java/com/example/demo/
├── admincontrollers/   Administrator REST controllers
├── adminservices/      Administrator business logic
├── controller/         Customer-facing REST controllers
├── dto/                Request and response DTOs
├── entity/             JPA entities and enums
├── filter/             Request authentication filter
├── repository/         Spring Data repositories
└── service/             Application business logic
```

## Prerequisites

- JDK 17 or newer
- MySQL 8 or compatible MySQL server
- Maven 3.9+ (or use the included Maven Wrapper)
- Razorpay test credentials for payment features

## Database Setup

Create a database before starting the application:

```sql
CREATE DATABASE kodcart;
```

The application currently validates the existing schema at startup with `spring.jpa.hibernate.ddl-auto=validate`. Make sure the required tables already exist, or change this setting only for local development and understand the migration implications.

## Configuration

Create or update `src/main/resources/application.properties` with local values. Do not commit passwords, JWT secrets, or payment credentials.

```properties
spring.application.name=kodcart
server.port=9090

spring.datasource.url=jdbc:mysql://localhost:3306/kodcart
spring.datasource.username=<database-user>
spring.datasource.password=<database-password>
spring.jpa.hibernate.ddl-auto=validate

jwt.secret=<long-random-secret>
jwt.expiration=3600000

razorpay.key_id=<razorpay-key-id>
razorpay.key_secret=<razorpay-key-secret>

spring.web.cors.allowed-origins=http://localhost:5174
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
spring.web.cors.allowed-headers=Content-Type,Authorization
spring.web.cors.allow-credentials=true
```

For production, use environment-specific configuration or a secrets manager, enable HTTPS, use secure cookies, restrict CORS to trusted origins, and rotate any credentials that may have been exposed during development.

## Running the Application

Using the Maven Wrapper:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The API is available at `http://localhost:9090` by default.

## API Overview

All request and response bodies use JSON unless noted otherwise.

### Authentication

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/login` | Authenticate a user and issue an auth cookie |
| `POST` | `/api/auth/logout` | Log out the authenticated user |

### Products

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/products` | List products; optionally filter with `?category=<category>` |

### Cart

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/cart/items` | Get the authenticated user's cart |
| `GET` | `/api/cart/items/count?username=<username>` | Get the cart item count |
| `POST` | `/api/cart/add` | Add a product to the cart |
| `PUT` | `/api/cart/update` | Update a cart item quantity |
| `DELETE` | `/api/cart/delete` | Remove a product from the cart |

### Orders and Payments

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/orders` | Get successful orders for the authenticated user |
| `*` | `/api/payment/...` | Payment operations exposed by `PaymentController` |

### Administrator APIs

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/admin/products/add` | Add a product and image |
| `DELETE` | `/admin/products/delete` | Delete a product |
| `*` | `/admin/users/...` | Administrator user operations |
| `*` | `/admin/business/...` | Administrator business operations |

For exact request payloads and additional payment, user, and business routes, see the controller classes under `src/main/java/com/example/demo/`.

## Testing

Run the test suite with:

```bash
./mvnw test
```

Build the application without running it with:

```bash
./mvnw clean package
```

## Development Notes

- The frontend CORS configuration currently allows `http://localhost:5174`; update it for another frontend origin.
- Authentication is implemented by `AuthenticationFilter` and the auth cookie created by `AuthController`.
- Keep API secrets outside version control and use separate credentials for development, testing, and production.
- Prefer database migrations for shared or production environments rather than relying on Hibernate schema generation.

## License

No license has been specified yet. Add a `LICENSE` file before distributing this project.
