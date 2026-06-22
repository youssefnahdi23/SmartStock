#!/bin/bash

# SmartStock Development Setup Script
# This script prepares the development environment

set -e

echo "🚀 SmartStock Development Setup"
echo "=================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Check Java
echo "Checking Java..."
if ! command -v java &> /dev/null; then
    echo -e "${RED}✗ Java not found${NC}"
    echo "Please install Java 21 or later"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "\K[0-9]+')
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo -e "${RED}✗ Java 21+ required (found: $JAVA_VERSION)${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Java $JAVA_VERSION$(NC)"
echo ""

# Check Maven
echo "Checking Maven..."
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}✗ Maven not found${NC}"
    echo "Please install Maven 3.8 or later"
    exit 1
fi
echo -e "${GREEN}✓ Maven installed${NC}"
echo ""

# Check Docker
echo "Checking Docker..."
if ! command -v docker &> /dev/null; then
    echo -e "${RED}✗ Docker not found${NC}"
    echo "Please install Docker"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}✗ Docker Compose not found${NC}"
    echo "Please install Docker Compose"
    exit 1
fi
echo -e "${GREEN}✓ Docker & Docker Compose installed${NC}"
echo ""

# Create .env if not exists
if [ ! -f .env ]; then
    echo "Creating .env file..."
    cat > .env <<EOF
# Database
DB_PASSWORD=smartstock123
DB_USER=smartstock

# Redis
REDIS_PASSWORD=smartstock123

# MinIO
MINIO_USER=minioadmin
MINIO_PASSWORD=minioadmin

# JWT
JWT_SECRET=your-secret-key-min-256-bits-long-for-production
JWT_EXPIRY_MS=900000

# App
APP_NAME=SmartStock
APP_VERSION=1.0.0
ENVIRONMENT=development
EOF
    echo -e "${GREEN}✓ .env file created${NC}"
else
    echo -e "${YELLOW}→ .env already exists${NC}"
fi
echo ""

echo -e "${GREEN}✅ Setup complete!${NC}"
echo ""
echo "Next steps:"
echo "  1. Start services: ${YELLOW}docker-compose up -d${NC}"
echo "  2. Build project: ${YELLOW}mvn clean install${NC}"
echo "  3. Run tests: ${YELLOW}mvn test${NC}"
echo ""
echo "For more info, see QUICK_START_GUIDE.md"
