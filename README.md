# PiggyBank Microservice System

PiggyBank is a microservice system for managing personal finances. It consists of multiple services that work together to provide a comprehensive personal finance management solution.

## Services

The PiggyBank system consists of the following services:

1. **PiggyBank UI**: A modern web interface for monitoring accounts and managing transfers, built with React and Material-UI.
2. **TransferGateway**: Receives transfers from external banking systems, maintains a list of monitored accounts, and processes transfers involving monitored accounts.
3. **AccountTwinService**: Maintains a digital twin of each monitored account, tracking all transactions and providing account balance information.
4. **GoalService**: Manages financial goals, tracking progress and providing recommendations.
5. **TransferClassifier**: Classifies transfers into categories for better financial insights.
6. **NotificationService**: Sends notifications to users about important financial events, such as account balance updates.

## Technology Stack

### Backend
- **Language**: Kotlin
- **Build Tool**: Maven
- **Framework**: Spring Boot
- **Messaging**: RabbitMQ
- **Testing**: JUnit 5, Instancio

### Frontend
- **Language**: TypeScript
- **Framework**: React 18
- **Routing**: React Router 6
- **UI Components**: Material-UI
- **Build Tool**: Vite
- **HTTP Client**: Axios

## Getting Started

### Prerequisites

For backend services:
- Java 21 or higher
- Maven 3.6 or higher
- RabbitMQ 3.8 or higher

For the UI module:
- Node.js 16 or higher
- npm or yarn

### Building the Project

To build the project, run the following command:

```bash
mvn clean install
```

### Running Tests

#### Running All Tests

To run all tests in all modules:

```bash
mvn test
```

#### Running InterACtTests

To run all InterACtTests across all modules:

```bash
mvn test -P interact-tests
```

This command uses the `interact-tests` Maven profile, which configures the Surefire plugin to only run test classes with names ending in 'InterACtTest'. These tests verify the interactions between microservices using the InterACt framework.

#### Running All Tests Except InterACtTests

To run all tests except InterACtTests across all modules:

```bash
mvn test -P non-interact-tests
```

This command uses the `non-interact-tests` Maven profile, which configures the Surefire plugin to exclude test classes with names ending in 'InterACtTest'. This is useful when you want to run only the unit and integration tests without the InterACt tests.

### Running the Services

#### Backend Services

##### Using Maven

To run a specific backend service using Maven, use the following command:

```bash
mvn spring-boot:run -pl <service-name>
```

For example, to run the TransferGateway service:

```bash
mvn spring-boot:run -pl transfer-gateway
```

##### Using Docker

The backend services can also be run using Docker. To build and run the services using Docker, follow these steps:

1. Using the start-services scripts (recommended):

For Linux/Mac:
```bash
./start-services.sh
```

For Windows:
```
start-services.bat
```

These scripts will check if Docker and docker-compose are available, rebuild containers if anything has changed, start all services, and display their status.

The Windows script (start-services.bat) supports the following options:
- `start-services.bat` - Start services, only rebuilding those that have changed
- `start-services.bat --force` - Force rebuild of all services regardless of changes
- `start-services.bat --clean` - Remove timestamp files and exit (forces rebuild on next run)
- `start-services.bat --help` - Show the help message

The script uses timestamp files (.service_name_last_build) to track when each service was last built and only rebuilds services that have changed since the last build, which significantly improves startup time during development.

**Troubleshooting the Windows script:**
- If you see only the help message when running the script without arguments, make sure you're running it from the repository root directory.
- If Docker isn't starting, ensure Docker Desktop is running before executing the script.
- If you encounter errors about services not being rebuilt correctly, try running `start-services.bat --clean` followed by `start-services.bat` to force a complete rebuild.
- For permission issues, try running the Command Prompt or PowerShell as Administrator.

2. Alternatively, you can use docker-compose directly:

```bash
docker-compose up -d
```

3. To stop the services:

```bash
docker-compose down
```

4. To rebuild the services after making changes:

```bash
docker-compose up -d --build
```

The following backend services will be available:

- TransferGateway: http://localhost:8080
- AccountTwinService: http://localhost:8081
- NotificationService: http://localhost:8082
- RabbitMQ Management UI: http://localhost:15672 (username: guest, password: guest)

#### UI Module

To run the UI module, navigate to the piggybank-ui directory and run the following commands:

1. Install dependencies:

```bash
cd piggybank-ui
npm install
```

or

```bash
cd piggybank-ui
yarn
```

2. Start the development server:

```bash
npm run dev
```

or

```bash
yarn dev
```

The UI will be available at http://localhost:3000.

## Project Structure

The project follows a multi-module structure:

```
PiggyBank/
├── pom.xml                  # Parent POM
├── README.md                # This file
├── docker-compose.yml       # Docker Compose configuration
├── Dockerfile               # Docker configuration for backend services
├── start-services.sh        # Script to start all services
├── piggybank-ui/            # PiggyBank UI module
│   ├── package.json         # UI dependencies and scripts
│   ├── README.md            # UI documentation
│   ├── Dockerfile           # Docker configuration for UI
│   ├── nginx.conf           # Nginx configuration for UI
│   ├── public/              # Static assets
│   └── src/                 # UI source code
├── transfer-gateway/        # TransferGateway service
│   ├── pom.xml              # Service POM
│   ├── README.md            # Service documentation
│   └── src/                 # Service source code
├── account-twin-service/    # AccountTwinService
│   ├── pom.xml              # Service POM
│   ├── README.md            # Service documentation
│   └── src/                 # Service source code
├── notification-service/    # NotificationService
│   ├── pom.xml              # Service POM
│   ├── README.md            # Service documentation
│   └── src/                 # Service source code
├── goal-service/            # GoalService
└── transfer-classifier/     # TransferClassifier
```

## Documentation

Each service has its own README.md file with detailed documentation:

- [PiggyBank UI](piggybank-ui/README.md)
- [TransferGateway](transfer-gateway/README.md)
- [AccountTwinService](account-twin-service/README.md)
- [NotificationService](notification-service/README.md)
- [GoalService](goal-service/README.md)
- [TransferClassifier](transfer-classifier/README.md)

## Development Guidelines

This project follows specific development guidelines to ensure code quality and consistency. Please refer to the [Development Guidelines](docs/DEVELOPMENT_GUIDELINES.md) for detailed information on:

- Development standards for backend (Kotlin/Spring Boot) and frontend (TypeScript/React)
- Code style and formatting
- Documentation practices
- Testing requirements
- Contribution guidelines

## License

This project is licensed under the MIT License - see the LICENSE file for details.
