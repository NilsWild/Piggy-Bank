# Account Twin Service

The Account Twin Service (ATS) is a microservice that keeps "copies" of monitored accounts. It is the owner of monitored accounts and provides functionality to add, remove, and query accounts and their transactions.

## Features

- Add and remove monitored accounts
- Process transactions received from the Transfer Gateway
- Update account balances based on transactions
- Emit account updates via AMQP
- Query account information and transaction history

## API Endpoints

### Account Management

- `POST /api/accounts` - Create a new monitored account
- `GET /api/accounts` - Get all monitored accounts
- `GET /api/accounts/{accountId}` - Get an account by ID
- `GET /api/accounts/by-type-and-identifier` - Get an account by type and identifier
- `GET /api/accounts/{accountId}/balance` - Get an account's balance
- `DELETE /api/accounts/{accountId}` - Delete an account

### Transaction Management

- `POST /api/transactions` - Process a transaction
- `GET /api/transactions/{transactionId}` - Get a transaction by ID
- `GET /api/transactions/by-account/{accountId}` - Get all transactions for an account

## AMQP Events

The service emits the following events on the `piggybank.accounts` exchange:

- `account.created` - When a new account is created
- `account.updated` - When an account's balance is updated
- `account.deleted` - When an account is deleted

## Running the Service

### Prerequisites

- Java 21
- Maven
- RabbitMQ

### Running Locally

```bash
mvn spring-boot:run
```

### Running with Docker

```bash
docker-compose up account-twin-service
```

## Configuration

The service can be configured using the following environment variables:

- `SERVER_PORT` - The port the service listens on (default: 8081)
- `SPRING_RABBITMQ_HOST` - The RabbitMQ host (default: localhost)
- `SPRING_RABBITMQ_PORT` - The RabbitMQ port (default: 5672)
- `SPRING_RABBITMQ_USERNAME` - The RabbitMQ username (default: guest)
- `SPRING_RABBITMQ_PASSWORD` - The RabbitMQ password (default: guest)

## Integration with Transfer Gateway

The Account Twin Service receives transactions from the Transfer Gateway via the `/api/transactions` endpoint. When a transaction is received, the service:

1. Checks if the account exists
2. Updates the account's balance
3. Stores the transaction
4. Emits an account update event

The Transfer Gateway can then subscribe to these events to keep track of account balances.