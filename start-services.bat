@echo off
REM start-services.bat - Script to start all PiggyBank services for Windows
REM 
REM This script starts all PiggyBank services using Docker Compose.
REM It ensures that all services are started in the correct order with proper dependencies.
REM By default, it only rebuilds services that have changed since the last build,
REM which significantly improves startup time during development.
REM
REM The script uses timestamp files (.service_name_last_build) to track when each
REM service was last built. It compares file modification times with these timestamps
REM to determine if a service needs to be rebuilt.
REM
REM Usage:
REM   start-services.bat          - Start services, only rebuilding those that have changed
REM   start-services.bat --force  - Force rebuild of all services regardless of changes
REM   start-services.bat --clean  - Remove timestamp files and exit (forces rebuild on next run)
REM   start-services.bat --help   - Show this help message

setlocal enabledelayedexpansion

REM Parse command line arguments
set FORCE_REBUILD=false
set SHOW_HELP=false
set CLEAN_TIMESTAMPS=false

REM Check for arguments with quotes or spaces
set ARG1=%~1

REM Explicitly set SHOW_HELP to false by default
set SHOW_HELP=false

if "!ARG1!"=="--force" (
    set FORCE_REBUILD=true
) else if "!ARG1!"=="--help" (
    set SHOW_HELP=true
) else if "!ARG1!"=="--clean" (
    set CLEAN_TIMESTAMPS=true
) else if not "!ARG1!"=="" (
    echo Unknown argument: !ARG1!
    set SHOW_HELP=true
)
REM If no arguments provided, continue with default behavior (SHOW_HELP remains false)

if "!SHOW_HELP!"=="true" (
    echo Usage:
    echo   start-services.bat          - Start services, only rebuilding those that have changed
    echo   start-services.bat --force  - Force rebuild of all services
    echo   start-services.bat --clean  - Remove timestamp files and exit ^(forces rebuild on next run^)
    echo   start-services.bat --help   - Show this help message
    exit /b 0
)

REM Handle clean option
if "!CLEAN_TIMESTAMPS!"=="true" (
    echo Removing timestamp files to force rebuild on next run...
    if exist .transfer-gateway_last_build del .transfer-gateway_last_build
    if exist .account-twin-service_last_build del .account-twin-service_last_build
    if exist .notification-service_last_build del .notification-service_last_build
    if exist .goal-service_last_build del .goal-service_last_build
    if exist .transfer-classifier_last_build del .transfer-classifier_last_build
    if exist .piggybank-ui_last_build del .piggybank-ui_last_build
    echo Timestamp files removed. Run the script again without --clean to start services.
    exit /b 0
)

REM Display banner
echo =====================================================
echo   Starting PiggyBank Services
echo =====================================================

REM Check if Docker is running
docker info > nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo Error: Docker is not running or not installed.
    echo Please start Docker and try again.
    exit /b 1
)

REM Check if docker compose is available
docker compose version > nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo Error: docker compose is not available.
    echo Please ensure Docker is installed with Docker Compose V2 support.
    exit /b 1
)

if "!FORCE_REBUILD!"=="true" (
    set "rebuild_services=transfer-gateway account-twin-service notification-service goal-service transfer-classifier piggybank-ui"
) else (
    echo Checking for changes in services...

    REM Determine which services need to be rebuilt
    set "rebuild_services="

    for %%s in (transfer-gateway account-twin-service notification-service goal-service transfer-classifier piggybank-ui) do (
        set "service=%%s"
        set "timestamp_file=.!service!_last_build"
        set "needs_rebuild=false"

        REM If timestamp file doesn't exist, service needs to be rebuilt
        if not exist "!timestamp_file!" (
            set "needs_rebuild=true"
        ) else (
            REM Check if the service directory exists
            if not exist "!service!\" (
                set "needs_rebuild=true"
            ) else (
                REM Check if any files in the service directory have been modified since last build
                REM Using PowerShell for this check since batch doesn't have good file comparison tools
                powershell -Command "try { $files = Get-ChildItem -Path '!service!' -Recurse -File | Where-Object {$_.LastWriteTime -gt (Get-Item '!timestamp_file!').LastWriteTime}; if ($files) {exit 1} else {exit 0} } catch { exit 1 }"
                if !ERRORLEVEL! neq 0 (
                    set "needs_rebuild=true"
                ) else if "!service:~-8!"=="-service" (
                    REM For Java services, also check if pom.xml has been modified
                    if exist "pom.xml" (
                        powershell -Command "try { if ((Get-Item 'pom.xml').LastWriteTime -gt (Get-Item '!timestamp_file!').LastWriteTime) {exit 1} else {exit 0} } catch { exit 1 }"
                        if !ERRORLEVEL! neq 0 (
                            set "needs_rebuild=true"
                        )
                    )
                )
            )
        )

        if "!needs_rebuild!"=="true" (
            echo Changes detected in !service!, will rebuild.
            set "rebuild_services=!rebuild_services! !service!"
        ) else (
            echo No changes detected in !service!, skipping rebuild.
        )
    )
)

echo Starting services using Docker Compose...
echo This may take a few minutes for the first run as images need to be built.

if "!rebuild_services!"=="" (
    REM No services need to be rebuilt, just start them
    docker compose up -d
) else (
    REM Start all services, but only rebuild the ones that have changed
    echo Rebuilding services: !rebuild_services!
    REM First, build the services that need to be rebuilt
    for %%s in (!rebuild_services!) do (
        set "service=%%s"
        docker compose build %%s
        REM Update timestamp files for rebuilt services
        powershell -Command "try { Get-Date -UFormat %%s | Out-File -FilePath '.!service!_last_build' -Encoding ASCII } catch { Write-Error 'Failed to create timestamp file for !service!'; exit 1 }"
    )
    REM Then start all services
    docker compose up -d
)

REM Display status
echo =====================================================
echo   PiggyBank Services Status
echo =====================================================
docker compose ps

echo.
echo Services are now running!
echo.
echo Access points:
echo - PiggyBank UI: http://localhost:3000
echo - Account Twin Service API: http://localhost:8081
echo - Transfer Gateway API: http://localhost:8080
echo - Notification Service API: http://localhost:8082
echo - Goal Service API: http://localhost:8083
echo - Transfer Classifier Service API: http://localhost:8084
echo - RabbitMQ Management UI: http://localhost:15672 (guest/guest)
echo.
echo To stop all services, run: docker compose down
echo =====================================================
