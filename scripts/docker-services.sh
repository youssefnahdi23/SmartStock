#!/bin/bash

# SmartStock Docker Management Script
# Manages Docker services

set -e

COMMAND=${1:-help}

show_help() {
    echo "SmartStock Docker Manager"
    echo "Usage: docker-services.sh [command]"
    echo ""
    echo "Commands:"
    echo "  up       - Start all services"
    echo "  down     - Stop all services"
    echo "  clean    - Remove containers and volumes"
    echo "  logs     - Show logs from all services"
    echo "  logs SERVICE - Show logs from specific service"
    echo "  status   - Show service status"
    echo "  restart  - Restart all services"
    echo ""
    echo "Services:"
    echo "  - PostgreSQL (Identity): 5432"
    echo "  - PostgreSQL (Product): 5433"
    echo "  - PostgreSQL (Inventory): 5434"
    echo "  - PostgreSQL (Warehouse): 5435"
    echo "  - Redis: 6379"
    echo "  - Kafka: 9092"
    echo "  - Zookeeper: 2181"
    echo "  - MinIO: 9000"
}

case $COMMAND in
    up)
        echo "Starting services..."
        docker-compose up -d
        echo "Waiting for services..."
        sleep 10
        echo "Services started!"
        docker-compose ps
        ;;
    down)
        echo "Stopping services..."
        docker-compose down
        echo "Services stopped"
        ;;
    clean)
        echo "Removing containers and volumes..."
        docker-compose down -v
        echo "Cleaned"
        ;;
    logs)
        SERVICE=${2:-}
        if [ -z "$SERVICE" ]; then
            docker-compose logs -f
        else
            docker-compose logs -f "$SERVICE"
        fi
        ;;
    status)
        docker-compose ps
        ;;
    restart)
        echo "Restarting services..."
        docker-compose restart
        echo "Services restarted"
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
