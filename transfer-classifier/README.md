# Transfer Classifier Service

## Service Description and Purpose

The Transfer Classifier Service is a microservice within the PiggyBank system that analyzes bank transfers and classifies them into different categories based on their purpose. This service helps users understand their spending patterns by automatically categorizing their transactions.

The service works by listening for transfer events from RabbitMQ, processing them through various classifiers, and then publishing the classification results back to RabbitMQ for other services to consume.

## Features

- Asynchronous processing of transfer events via RabbitMQ
- Extensible classification system with pluggable classifiers
- Currently supports the following classifications:
  - **Grocery**: Identifies transfers related to grocery shopping (Aldi, Lidl, Edeka)
  - **Holiday**: Identifies transfers related to vacations and travel (hotels, rentals, resorts)
- Publishes classification results to RabbitMQ for consumption by other services

## API Endpoints

This service does not expose REST APIs. Instead, it communicates with other services through RabbitMQ:

### Incoming Messages

- **Exchange**: `piggybank.transfers`
- **Routing Key**: `transfer.event`
- **Queue**: `piggybank.transfers.classifier`
- **Message Format**:
  ```json
  {
    "id": "UUID",
    "sourceAccount": {
      "id": "UUID",
      "name": "String",
      "iban": "String"
    },
    "targetAccount": {
      "id": "UUID",
      "name": "String",
      "iban": "String"
    },
    "amount": {
      "value": "BigDecimal",
      "currency": "String"
    },
    "valuationTimestamp": "ISO-8601 timestamp",
    "purpose": "String"
  }
  ```

### Outgoing Messages

- **Exchange**: `piggybank.classifications`
- **Routing Key**: `classification.event`
- **Message Format**:
  ```json
  {
    "transferId": "UUID",
    "classifications": ["Grocery", "Holiday"]
  }
  ```

## Configuration Options

The service can be configured through the following properties in `application.properties` or `application.yml`:

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

# Logging configuration
logging:
  level:
    de.rwth.swc.piggybank.transferclassifier: INFO
```

## Running Instructions

### Prerequisites

- Java 17 or higher
- RabbitMQ server running

### Running with Maven

```bash
cd transfer-classifier
mvn spring-boot:run
```

### Running with Docker

```bash
docker build -t piggybank/transfer-classifier .
docker run -p 8080:8080 piggybank/transfer-classifier
```

### Running with docker-compose

From the root of the PiggyBank project:

```bash
docker-compose up -d transfer-classifier
```

## Integration Details with Other Services

The Transfer Classifier Service integrates with other PiggyBank services as follows:

1. **Transfer Gateway Service**: The Transfer Gateway Service publishes transfer events to the `piggybank.transfers` exchange, which are then consumed by this service.

2. **Account Twin Service**: The Account Twin Service may consume the classification results from the `piggybank.classifications` exchange to enrich the account transaction history with category information.

3. **Notification Service**: The Notification Service may consume the classification results to send notifications to users about specific types of transactions.

## Adding New Classifiers

To add a new classifier:

1. Create a new class that implements the `TransferClassifier` interface
2. Provide a unique name for the classifier
3. Implement the `classify` method to determine if a transfer matches your classification criteria
4. Annotate the class with `@Component` to make it available to the `ClassificationService`

Example:

```kotlin
@Component
class RestaurantClassifier : TransferClassifier {
    override val name: String = "Restaurant"

    private val restaurantKeywords = listOf("restaurant", "cafe", "diner")

    override fun classify(transfer: Transfer): Boolean {
        val purpose = transfer.purpose.lowercase()
        return restaurantKeywords.any { keyword -> purpose.contains(keyword.lowercase()) }
    }
}
```
