# Transfer Gateway Service

The Transfer Gateway (TG) service is part of the PiggyBank microservice system. It receives transfers from external banking systems via REST, maintains a list of monitored accounts, and processes transfers involving monitored accounts.

## Features

- Receive transfers from external banking systems
- Maintain a list of monitored accounts
- Process transfers involving monitored accounts:
  - Send transfer events to RabbitMQ
  - Split transfers into credit and debit transactions
  - Send transactions to the AccountTwinService

## API Endpoints

### Transfers

#### Receive a Transfer

```
POST /api/transfers
```

Request Body:
```json
{
  "sourceAccountType": "BankAccount",
  "sourceAccountId": "DE123456789",
  "targetAccountType": "PayPal",
  "targetAccountId": "user@example.com",
  "amount": 100.00,
  "currency": "EUR",
  "valuationTimestamp": "2023-03-27T10:15:30Z",
  "purpose": "Payment for services"
}
```

Response:
- `201 Created`: Transfer processed successfully
- `400 Bad Request`: Invalid transfer request
- `500 Internal Server Error`: Error processing transfer

### Accounts

#### Get All Monitored Accounts

```
GET /api/accounts
```

Response:
```json
[
  {
    "type": "BankAccount",
    "identifier": "DE123456789"
  },
  {
    "type": "PayPal",
    "identifier": "user@example.com"
  }
]
```

#### Add a Monitored Account

```
POST /api/accounts
```

Request Body:
```json
{
  "type": "BankAccount",
  "identifier": "DE123456789"
}
```

Response:
- `201 Created`: Account added successfully
- `400 Bad Request`: Invalid account request
- `409 Conflict`: Account already exists

#### Remove a Monitored Account

```
DELETE /api/accounts
```

Request Body:
```json
{
  "type": "BankAccount",
  "identifier": "DE123456789"
}
```

Response:
- `204 No Content`: Account removed successfully
- `400 Bad Request`: Invalid account request
- `404 Not Found`: Account not found

## Configuration

The service can be configured using the `application.yml` file:

```yaml
server:
  port: 8080

spring:
  application:
    name: transfer-gateway
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

account-twin-service:
  url: http://localhost:8081
```

## Running the Service

### Using Maven

To run the service using Maven, use the following command:

```bash
mvn spring-boot:run -pl transfer-gateway
```

### Using Docker

To run the service using Docker, you can use the provided Dockerfile and docker-compose.yml:

#### Building and Running with Docker Compose

From the root directory of the project:

```bash
docker-compose up -d
```

This will start the Transfer Gateway service along with RabbitMQ and a placeholder for the AccountTwinService.

#### Building and Running the Service Only

If you want to build and run just the Transfer Gateway service:

```bash
docker build -f transfer-gateway/Dockerfile -t transfer-gateway .
docker run -p 8080:8080 transfer-gateway
```

Note: When running the service in isolation, you'll need to configure the RabbitMQ and AccountTwinService URLs using environment variables:

```bash
docker run -p 8080:8080 \
  -e SPRING_RABBITMQ_HOST=your-rabbitmq-host \
  -e SPRING_RABBITMQ_PORT=5672 \
  -e SPRING_RABBITMQ_USERNAME=guest \
  -e SPRING_RABBITMQ_PASSWORD=guest \
  -e ACCOUNT_TWIN_SERVICE_URL=http://your-account-twin-service:8081 \
  transfer-gateway
```

## Testing

To run the tests, use the following command:

```bash
mvn test -pl transfer-gateway
```
