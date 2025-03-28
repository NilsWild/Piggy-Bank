#!/bin/bash

# start-services.sh - Script to start all PiggyBank services
# 
# This script starts all PiggyBank services using Docker Compose.
# It ensures that all services are started in the correct order with proper dependencies.
# By default, it only rebuilds services that have changed since the last build,
# which significantly improves startup time during development.
#
# The script uses timestamp files (.service_name_last_build) to track when each
# service was last built. It compares file modification times with these timestamps
# to determine if a service needs to be rebuilt.
#
# Usage:
#   ./start-services.sh          # Start services, only rebuilding those that have changed
#   ./start-services.sh --force  # Force rebuild of all services regardless of changes
#   ./start-services.sh --clean  # Remove timestamp files and exit (forces rebuild on next run)
#   ./start-services.sh --help   # Show this help message
#
# Services managed:
#   - transfer-gateway: Spring Boot service for transfer operations
#   - account-twin-service: Spring Boot service for account management
#   - notification-service: Spring Boot service for user notifications
#   - piggybank-ui: Frontend UI service
#   - rabbitmq: Message broker (always uses the pre-built image)

set -e  # Exit immediately if a command exits with a non-zero status

# Parse command line arguments
FORCE_REBUILD=false
SHOW_HELP=false
CLEAN_TIMESTAMPS=false

for arg in "$@"; do
  case $arg in
    --force)
      FORCE_REBUILD=true
      ;;
    --help)
      SHOW_HELP=true
      ;;
    --clean)
      CLEAN_TIMESTAMPS=true
      ;;
    *)
      echo "Unknown argument: $arg"
      SHOW_HELP=true
      ;;
  esac
done

if [[ "$SHOW_HELP" == "true" ]]; then
  echo "Usage:"
  echo "  ./start-services.sh          # Start services, only rebuilding those that have changed"
  echo "  ./start-services.sh --force  # Force rebuild of all services"
  echo "  ./start-services.sh --clean  # Remove timestamp files and exit (forces rebuild on next run)"
  echo "  ./start-services.sh --help   # Show this help message"
  exit 0
fi

# Handle clean option
if [[ "$CLEAN_TIMESTAMPS" == "true" ]]; then
  echo "Removing timestamp files to force rebuild on next run..."
  rm -f .transfer-gateway_last_build .account-twin-service_last_build .notification-service_last_build .piggybank-ui_last_build
  echo "Timestamp files removed. Run the script again without --clean to start services."
  exit 0
fi

# Display banner
echo "====================================================="
echo "  Starting PiggyBank Services"
echo "====================================================="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
  echo "Error: Docker is not running or not installed."
  echo "Please start Docker and try again."
  exit 1
fi

# Check if docker-compose is available
if ! command -v docker-compose > /dev/null 2>&1; then
  echo "Error: docker-compose is not installed."
  echo "Please install docker-compose and try again."
  exit 1
fi

if [[ "$FORCE_REBUILD" == "true" ]]; then
  echo "Force rebuild requested, will rebuild all services."
  rebuild_services="transfer-gateway account-twin-service notification-service piggybank-ui"
else
  echo "Checking for changes in services..."

  # Function to check if a service needs to be rebuilt
  needs_rebuild() {
    local service=$1
    local changed=false
    local timestamp_file=".${service}_last_build"

    # If timestamp file doesn't exist, service needs to be rebuilt
    if [[ ! -f "$timestamp_file" ]]; then
      changed=true
    else
      # Check if the service directory exists
      if [[ ! -d "$service" ]]; then
        # If service directory doesn't exist, always rebuild
        changed=true
      else
        # Check if any files in the service directory have been modified since last build
        if [[ $(find $service -type f -newer "$timestamp_file" 2>/dev/null) ]]; then
          changed=true
        # For Java services, also check if pom.xml has been modified
        elif [[ $service == *"-service" && -f "pom.xml" && $(find pom.xml -newer "$timestamp_file" 2>/dev/null) ]]; then
          changed=true
        fi
      fi
    fi

    echo $changed
  }

  # Determine which services need to be rebuilt
  rebuild_services=""
  for service in transfer-gateway account-twin-service notification-service piggybank-ui; do
    if [[ $(needs_rebuild $service) == "true" ]]; then
      echo "Changes detected in $service, will rebuild."
      rebuild_services="$rebuild_services $service"
    else
      echo "No changes detected in $service, skipping rebuild."
    fi
  done
fi

echo "Starting services using Docker Compose..."
echo "This may take a few minutes for the first run as images need to be built."

if [[ -z "$rebuild_services" ]]; then
  # No services need to be rebuilt, just start them
  echo "No changes detected, starting services without rebuilding."
  docker-compose up -d
else
  # Start all services, but only rebuild the ones that have changed
  echo "Rebuilding services: $rebuild_services"
  # First, build the services that need to be rebuilt
  for service in $rebuild_services; do
    docker-compose build $service
    # Update timestamp files for rebuilt services
    echo "Updating timestamp for $service"
        date +%s > ".${service}_last_build"
  done
  # Then start all services
  docker-compose up -d
fi

# Display status
echo "====================================================="
echo "  PiggyBank Services Status"
echo "====================================================="
docker-compose ps

echo ""
echo "Services are now running!"
echo ""
echo "Access points:"
echo "- PiggyBank UI: http://localhost:3000"
echo "- Account Twin Service API: http://localhost:8081"
echo "- Transfer Gateway API: http://localhost:8080"
echo "- Notification Service API: http://localhost:8082"
echo "- RabbitMQ Management UI: http://localhost:15672 (guest/guest)"
echo ""
echo "To stop all services, run: docker-compose down"
echo "====================================================="
