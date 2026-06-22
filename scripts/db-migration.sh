#!/bin/bash

# SmartStock Database Migration Script
# Manages database migrations

set -e

COMMAND=${1:-help}

show_help() {
    echo "SmartStock Database Manager"
    echo "Usage: db-migration.sh [command]"
    echo ""
    echo "Commands:"
    echo "  migrate    - Run migrations (automatic via Flyway)"
    echo "  init       - Initialize databases"
    echo "  clean      - Clean all databases"
    echo "  status     - Show migration status"
    echo ""
    echo "Note: Migrations are managed by Flyway and run automatically"
    echo "on service startup. This script is for manual management."
}

case $COMMAND in
    migrate)
        echo "Running migrations..."
        mvn -DskipTests flyway:migrate
        ;;
    init)
        echo "Initializing databases..."
        # Verify Docker services are running
        if ! docker ps | grep -q smartstock-postgres-identity; then
            echo "Starting Docker services..."
            docker-compose up -d
            sleep 10
        fi
        echo "Databases initialized"
        ;;
    clean)
        echo "Warning: This will drop all data!"
        read -p "Continue? (y/N) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            docker-compose down -v
            docker-compose up -d
            echo "Databases cleaned"
        fi
        ;;
    status)
        echo "Migration status:"
        mvn -DskipTests flyway:info
        ;;
    help|"")
        show_help
        ;;
    *)
        echo "Unknown command: $COMMAND"
        show_help
        exit 1
        ;;
esac
