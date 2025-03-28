# Notification Service

The Notification Service (NS) is a microservice that listens for account update events, checks user notification preferences, generates notifications, and sends them to the UI via AMQP. It allows users to subscribe to notifications for specific accounts and event types.

## Features

- Subscribe to notifications for specific accounts and event types
- Process account update events from RabbitMQ
- Generate notifications based on user preferences
- Send notifications to the UI via RabbitMQ STOMP
- Retrieve and manage notifications via REST API

## API Endpoints

### Notification Management

- `GET /api/notifications` - Get all notifications with pagination
- `GET /api/notifications/account/{accountId}` - Get all notifications for a specific account
- `GET /api/notifications/unread` - Get all unread notifications
- `GET /api/notifications/account/{accountId}/unread` - Get all unread notifications for a specific account
- `GET /api/notifications/count` - Count the number of unread notifications
- `POST /api/notifications/{notificationId}/read` - Mark a notification as read

### Subscription Management

- `POST /api/subscriptions` - Create a new notification subscription
- `GET /api/subscriptions` - Get all active subscriptions
- `GET /api/subscriptions/account/{accountId}` - Get all active subscriptions for an account
- `DELETE /api/subscriptions/{subscriptionId}` - Deactivate a notification subscription

## AMQP Integration

The service listens for account update events on the `piggybank.accounts.updated.notifications.queue` queue and publishes notifications to the `piggybank.notifications` exchange with the routing key `notification.created`.

### WebSocket Integration

The service uses RabbitMQ's STOMP plugin to provide WebSocket access to notifications. The UI connects directly to RabbitMQ's STOMP WebSocket endpoint to receive real-time notifications.

## Running the Service

### Prerequisites

- Java 21
- Maven
- RabbitMQ with STOMP plugin enabled

### Running Locally

```bash
mvn spring-boot:run -pl notification-service
```

### Running with Docker

```bash
docker-compose up notification-service
```

## Configuration

The service can be configured using the following environment variables:

- `SERVER_PORT` - The port the service listens on (default: 8082)
- `SPRING_RABBITMQ_HOST` - The RabbitMQ host (default: localhost)
- `SPRING_RABBITMQ_PORT` - The RabbitMQ port (default: 5672)
- `SPRING_RABBITMQ_USERNAME` - The RabbitMQ username (default: guest)
- `SPRING_RABBITMQ_PASSWORD` - The RabbitMQ password (default: guest)

## Integration with Account Twin Service

The Notification Service listens for account update events from the Account Twin Service via RabbitMQ. When an account update event is received, the service:

1. Checks if there are any active subscriptions for the account and event type
2. If subscriptions exist, generates a notification
3. Saves the notification to the database
4. Publishes the notification to RabbitMQ for real-time delivery to the UI