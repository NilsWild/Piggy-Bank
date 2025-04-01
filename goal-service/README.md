# Goal Service

The Goal Service is a microservice in the PiggyBank system that allows users to create and track financial goals. It integrates with the Account Twin Service and Transfer Classifier to monitor progress towards goals based on account transactions and their classifications.

## Features

- Create and manage spending limit goals (e.g., limit grocery spending to 400€ per month)
- Create and manage savings goals (e.g., save 1000€ for a vacation)
- Track goal progress automatically based on account transactions
- Receive notifications when goals are achieved or failed
- Extensible architecture for adding new goal types

## API Endpoints

### Spending Limit Goals

#### Create a Spending Limit Goal

```
POST /api/goals/spending-limit
```

Request body:
```json
{
  "name": "Grocery Spending Limit",
  "description": "Limit grocery spending to 400 EUR per month",
  "startDate": "2023-04-01T00:00:00",
  "endDate": "2023-04-30T23:59:59",
  "accountId": "account-123",
  "limit": 400.00,
  "currencyCode": "EUR",
  "category": "Grocery"
}
```

Response:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Grocery Spending Limit",
  "description": "Limit grocery spending to 400 EUR per month",
  "type": "SPENDING_LIMIT",
  "status": "ACTIVE",
  "startDate": "2023-04-01T00:00:00",
  "endDate": "2023-04-30T23:59:59",
  "accountId": "account-123",
  "createdAt": "2023-03-25T10:15:30",
  "updatedAt": "2023-03-25T10:15:30",
  "limit": 400.00,
  "currencyCode": "EUR",
  "category": "Grocery",
  "currentSpending": 0.00
}
```

#### Get a Spending Limit Goal

```
GET /api/goals/spending-limit/{id}
```

Response:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Grocery Spending Limit",
  "description": "Limit grocery spending to 400 EUR per month",
  "type": "SPENDING_LIMIT",
  "status": "ACTIVE",
  "startDate": "2023-04-01T00:00:00",
  "endDate": "2023-04-30T23:59:59",
  "accountId": "account-123",
  "createdAt": "2023-03-25T10:15:30",
  "updatedAt": "2023-03-25T10:15:30",
  "limit": 400.00,
  "currencyCode": "EUR",
  "category": "Grocery",
  "currentSpending": 150.00
}
```

#### Get Spending Limit Goals for an Account

```
GET /api/goals/spending-limit/account/{accountId}
```

Response:
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Grocery Spending Limit",
    "description": "Limit grocery spending to 400 EUR per month",
    "type": "SPENDING_LIMIT",
    "status": "ACTIVE",
    "startDate": "2023-04-01T00:00:00",
    "endDate": "2023-04-30T23:59:59",
    "accountId": "account-123",
    "createdAt": "2023-03-25T10:15:30",
    "updatedAt": "2023-03-25T10:15:30",
    "limit": 400.00,
    "currencyCode": "EUR",
    "category": "Grocery",
    "currentSpending": 150.00
  }
]
```

#### Get Spending Limit Goals for a Category

```
GET /api/goals/spending-limit/category/{category}
```

Response:
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Grocery Spending Limit",
    "description": "Limit grocery spending to 400 EUR per month",
    "type": "SPENDING_LIMIT",
    "status": "ACTIVE",
    "startDate": "2023-04-01T00:00:00",
    "endDate": "2023-04-30T23:59:59",
    "accountId": "account-123",
    "createdAt": "2023-03-25T10:15:30",
    "updatedAt": "2023-03-25T10:15:30",
    "limit": 400.00,
    "currencyCode": "EUR",
    "category": "Grocery",
    "currentSpending": 150.00
  }
]
```

### Savings Goals

#### Create a Savings Goal

```
POST /api/goals/savings
```

Request body:
```json
{
  "name": "Vacation Savings",
  "description": "Save 1000 EUR for summer vacation",
  "startDate": "2023-04-01T00:00:00",
  "endDate": "2023-06-30T23:59:59",
  "accountId": "account-123",
  "targetAmount": 1000.00,
  "currencyCode": "EUR"
}
```

Response:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "name": "Vacation Savings",
  "description": "Save 1000 EUR for summer vacation",
  "type": "SAVINGS",
  "status": "ACTIVE",
  "startDate": "2023-04-01T00:00:00",
  "endDate": "2023-06-30T23:59:59",
  "accountId": "account-123",
  "createdAt": "2023-03-25T10:20:30",
  "updatedAt": "2023-03-25T10:20:30",
  "targetAmount": 1000.00,
  "currencyCode": "EUR",
  "currentAmount": 0.00
}
```

#### Get a Savings Goal

```
GET /api/goals/savings/{id}
```

Response:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "name": "Vacation Savings",
  "description": "Save 1000 EUR for summer vacation",
  "type": "SAVINGS",
  "status": "ACTIVE",
  "startDate": "2023-04-01T00:00:00",
  "endDate": "2023-06-30T23:59:59",
  "accountId": "account-123",
  "createdAt": "2023-03-25T10:20:30",
  "updatedAt": "2023-03-25T10:20:30",
  "targetAmount": 1000.00,
  "currencyCode": "EUR",
  "currentAmount": 500.00
}
```

#### Get Savings Goals for an Account

```
GET /api/goals/savings/account/{accountId}
```

Response:
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "name": "Vacation Savings",
    "description": "Save 1000 EUR for summer vacation",
    "type": "SAVINGS",
    "status": "ACTIVE",
    "startDate": "2023-04-01T00:00:00",
    "endDate": "2023-06-30T23:59:59",
    "accountId": "account-123",
    "createdAt": "2023-03-25T10:20:30",
    "updatedAt": "2023-03-25T10:20:30",
    "targetAmount": 1000.00,
    "currencyCode": "EUR",
    "currentAmount": 500.00
  }
]
```

### General Goal Endpoints

#### Get All Goals for an Account

```
GET /api/goals/account/{accountId}
```

Response:
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Grocery Spending Limit",
    "description": "Limit grocery spending to 400 EUR per month",
    "type": "SPENDING_LIMIT",
    "status": "ACTIVE",
    "startDate": "2023-04-01T00:00:00",
    "endDate": "2023-04-30T23:59:59",
    "accountId": "account-123",
    "createdAt": "2023-03-25T10:15:30",
    "updatedAt": "2023-03-25T10:15:30"
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "name": "Vacation Savings",
    "description": "Save 1000 EUR for summer vacation",
    "type": "SAVINGS",
    "status": "ACTIVE",
    "startDate": "2023-04-01T00:00:00",
    "endDate": "2023-06-30T23:59:59",
    "accountId": "account-123",
    "createdAt": "2023-03-25T10:20:30",
    "updatedAt": "2023-03-25T10:20:30"
  }
]
```

#### Get Goals by Status

```
GET /api/goals/status/{status}
```

Response:
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Grocery Spending Limit",
    "description": "Limit grocery spending to 400 EUR per month",
    "type": "SPENDING_LIMIT",
    "status": "ACTIVE",
    "startDate": "2023-04-01T00:00:00",
    "endDate": "2023-04-30T23:59:59",
    "accountId": "account-123",
    "createdAt": "2023-03-25T10:15:30",
    "updatedAt": "2023-03-25T10:15:30"
  }
]
```

#### Update Goal Status

```
PUT /api/goals/{id}/status/{status}
```

Response:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Grocery Spending Limit",
  "description": "Limit grocery spending to 400 EUR per month",
  "type": "SPENDING_LIMIT",
  "status": "CANCELLED",
  "startDate": "2023-04-01T00:00:00",
  "endDate": "2023-04-30T23:59:59",
  "accountId": "account-123",
  "createdAt": "2023-03-25T10:15:30",
  "updatedAt": "2023-03-25T10:25:30"
}
```

#### Delete a Goal

```
DELETE /api/goals/{id}
```

Response: 204 No Content

## Configuration

The Goal Service can be configured using the following environment variables:

- `SERVER_PORT`: The port on which the service listens (default: 8083)
- `SPRING_RABBITMQ_HOST`: The hostname of the RabbitMQ server (default: localhost)
- `SPRING_RABBITMQ_PORT`: The port of the RabbitMQ server (default: 5672)
- `SPRING_RABBITMQ_USERNAME`: The username for the RabbitMQ server (default: guest)
- `SPRING_RABBITMQ_PASSWORD`: The password for the RabbitMQ server (default: guest)

## Running the Service

### Using Docker Compose

The Goal Service is included in the PiggyBank docker-compose.yml file. To run the entire system:

```bash
docker-compose up -d
```

### Running Locally

To run the Goal Service locally:

```bash
cd goal-service
mvn spring-boot:run
```

## Integration with Other Services

The Goal Service integrates with the following services:

### Account Twin Service

The Goal Service listens for account update events from the Account Twin Service to track goal progress. When an account is updated with a new transaction, the Goal Service processes the transaction and updates the relevant goals.

### Transfer Classifier

The Goal Service listens for classification events from the Transfer Classifier to categorize transactions. This allows the Goal Service to track spending in specific categories (e.g., "Grocery", "Holiday") and update the relevant spending limit goals.

### Notification Service

The Goal Service publishes goal update events to RabbitMQ when goals are achieved or failed. The Notification Service can subscribe to these events to send notifications to users.

## Events

### Consumed Events

#### Account Updated Event

The Goal Service consumes account updated events from the Account Twin Service. These events contain information about transactions that have been applied to an account.

```json
{
  "eventType": "ACCOUNT_UPDATED",
  "accountId": "account-123",
  "accountType": "CHECKING",
  "accountIdentifier": "DE123456789",
  "value": "950.00",
  "currencyCode": "EUR",
  "transactionId": "550e8400-e29b-41d4-a716-446655440002",
  "transactionAmount": {
    "value": "-50.00",
    "currencyCode": "EUR"
  },
  "transactionType": "DEBIT",
  "transactionPurpose": "Grocery shopping"
}
```

#### Classification Event

The Goal Service consumes classification events from the Transfer Classifier. These events contain information about the categories that a transaction belongs to.

```json
{
  "transferId": "550e8400-e29b-41d4-a716-446655440002",
  "classifications": ["Grocery"]
}
```

### Published Events

#### Goal Updated Event

The Goal Service publishes goal updated events when a goal's progress is updated.

```json
{
  "eventType": "GOAL_UPDATED",
  "goalId": "550e8400-e29b-41d4-a716-446655440000",
  "goalName": "Grocery Spending Limit",
  "goalType": "SPENDING_LIMIT",
  "goalStatus": "ACTIVE",
  "accountId": "account-123",
  "timestamp": "2023-03-25T10:30:00",
  "progress": 150.00,
  "target": 400.00,
  "currencyCode": "EUR"
}
```

#### Goal Achieved Event

The Goal Service publishes goal achieved events when a goal is achieved.

```json
{
  "eventType": "GOAL_ACHIEVED",
  "goalId": "550e8400-e29b-41d4-a716-446655440001",
  "goalName": "Vacation Savings",
  "goalType": "SAVINGS",
  "goalStatus": "ACHIEVED",
  "accountId": "account-123",
  "timestamp": "2023-06-15T14:30:00"
}
```

#### Goal Failed Event

The Goal Service publishes goal failed events when a goal fails (e.g., the timeframe ends before the goal is achieved).

```json
{
  "eventType": "GOAL_FAILED",
  "goalId": "550e8400-e29b-41d4-a716-446655440000",
  "goalName": "Grocery Spending Limit",
  "goalType": "SPENDING_LIMIT",
  "goalStatus": "FAILED",
  "accountId": "account-123",
  "timestamp": "2023-04-30T23:59:59"
}
```

## Extending the Goal Service

The Goal Service is designed to be extensible. To add a new goal type:

1. Create a new domain class that extends the `Goal` abstract class
2. Implement the `processAccountUpdate` method to define how the goal is updated based on account transactions
3. Create a new repository interface that extends `JpaRepository` for the new goal type
4. Create DTOs for the new goal type
5. Update the service and controller classes to support the new goal type

For example, to add a "Debt Reduction" goal type that tracks progress towards paying off a debt:

```kotlin
@Entity
@DiscriminatorValue("DEBT_REDUCTION")
class DebtReductionGoal(
    name: String,
    description: String? = null,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    accountId: String,

    @Column(nullable = false)
    val initialDebt: BigDecimal,

    @Column(nullable = false)
    val currencyCode: String,

    @Column(nullable = false)
    var currentDebt: BigDecimal
) : Goal(
    name = name,
    description = description,
    type = GoalType.DEBT_REDUCTION,
    startDate = startDate,
    endDate = endDate,
    accountId = accountId
) {
    override fun processAccountUpdate(
        accountId: String,
        transactionAmount: BigDecimal,
        transactionType: String,
        transactionPurpose: String,
        classifications: List<String>
    ): Boolean {
        // Ignore if not for this account or if goal has ended
        if (this.accountId != accountId || hasEnded()) {
            return false
        }

        // Only process payments to debt (negative amounts)
        if (transactionAmount < BigDecimal.ZERO && 
            transactionPurpose.contains("debt", ignoreCase = true)) {

            // Reduce the current debt by the payment amount
            currentDebt = currentDebt.subtract(transactionAmount.abs())
            updatedAt = LocalDateTime.now()

            // Check if the debt is fully paid off
            if (currentDebt <= BigDecimal.ZERO) {
                updateStatus(GoalStatus.ACHIEVED)
                return true
            }
        }

        // Check if the goal's timeframe has expired
        if (isExpired() && isActive()) {
            // If the debt is fully paid off, the goal is achieved
            if (currentDebt <= BigDecimal.ZERO) {
                updateStatus(GoalStatus.ACHIEVED)
                return true
            } else {
                updateStatus(GoalStatus.FAILED)
                return true
            }
        }

        return false
    }
}
```
