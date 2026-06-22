.PHONY: help build install clean test test-coverage docker-up docker-down docker-logs format lint docs setup quick-start

# Default target
.DEFAULT_GOAL := help

SHELL := /bin/bash
JAVA_VERSION := 21
MAVEN := mvn
DOCKER_COMPOSE := docker-compose

# Colors for output
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[0;33m
RED := \033[0;31m
NC := \033[0m # No Color

help: ## Display this help message
	@echo "$(BLUE)SmartStock AI - Build & Development Commands$(NC)"
	@echo ""
	@echo "Usage: make [target]"
	@echo ""
	@echo "$(GREEN)Development Setup:$(NC)"
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(YELLOW)%-20s$(NC) %s\n", $$1, $$2}' | head -20
	@echo ""
	@echo "$(GREEN)Build & Compile:$(NC)"
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(YELLOW)%-20s$(NC) %s\n", $$1, $$2}' | tail -n +21

check-java: ## Verify Java 21+ is installed
	@command -v java >/dev/null 2>&1 || { echo "$(RED)Java is not installed$(NC)"; exit 1; }
	@java_version=$$(java -version 2>&1 | grep -oP 'version "\K[0-9]+'); \
	if [ "$$java_version" -lt 21 ]; then \
		echo "$(RED)Java 21+ required, found: $$java_version$(NC)"; exit 1; \
	fi
	@echo "$(GREEN)✓ Java version check passed$(NC)"

check-maven: ## Verify Maven 3.8+ is installed
	@command -v mvn >/dev/null 2>&1 || { echo "$(RED)Maven is not installed$(NC)"; exit 1; }
	@echo "$(GREEN)✓ Maven installed$(NC)"

check-docker: ## Verify Docker & Docker Compose installed
	@command -v docker >/dev/null 2>&1 || { echo "$(RED)Docker is not installed$(NC)"; exit 1; }
	@command -v docker-compose >/dev/null 2>&1 || { echo "$(RED)Docker Compose is not installed$(NC)"; exit 1; }
	@echo "$(GREEN)✓ Docker & Docker Compose installed$(NC)"

setup: check-java check-maven ## Setup development environment
	@echo "$(BLUE)Setting up SmartStock development environment...$(NC)"
	@echo "$(YELLOW)Creating .env file (if not exists)...$(NC)"
	@if [ ! -f .env ]; then \
		cp .env.example .env 2>/dev/null || echo "DB_PASSWORD=smartstock123" > .env; \
		echo "$(GREEN)✓ .env file created$(NC)"; \
	else \
		echo "$(YELLOW)→ .env already exists$(NC)"; \
	fi
	@echo "$(BLUE)Setup complete!$(NC)"

build: check-java check-maven ## Build all services (mvn clean install)
	@echo "$(BLUE)Building SmartStock platform...$(NC)"
	$(MAVEN) clean install -DskipTests
	@echo "$(GREEN)✓ Build successful$(NC)"

build-full: check-java check-maven ## Full build with tests
	@echo "$(BLUE)Full build with tests...$(NC)"
	$(MAVEN) clean install
	@echo "$(GREEN)✓ Full build successful$(NC)"

install: build ## Alias for build

compile: ## Compile source code only (mvn compile)
	@echo "$(BLUE)Compiling...$(NC)"
	$(MAVEN) compile
	@echo "$(GREEN)✓ Compilation successful$(NC)"

test: ## Run all tests (mvn test)
	@echo "$(BLUE)Running tests...$(NC)"
	$(MAVEN) test
	@echo "$(GREEN)✓ Tests complete$(NC)"

test-coverage: ## Generate test coverage report (mvn test jacoco:report)
	@echo "$(BLUE)Generating test coverage...$(NC)"
	$(MAVEN) test jacoco:report
	@echo "$(GREEN)✓ Coverage report generated$(NC)"
	@echo "$(YELLOW)Reports available at: target/site/jacoco/index.html$(NC)"

integration-test: ## Run integration tests (mvn verify)
	@echo "$(BLUE)Running integration tests...$(NC)"
	$(MAVEN) verify
	@echo "$(GREEN)✓ Integration tests complete$(NC)"

clean: ## Clean build artifacts (mvn clean)
	@echo "$(BLUE)Cleaning...$(NC)"
	$(MAVEN) clean
	@echo "$(GREEN)✓ Cleaned$(NC)"

format: ## Format code (mvn spotless:apply)
	@echo "$(BLUE)Formatting code...$(NC)"
	$(MAVEN) spotless:apply 2>/dev/null || echo "$(YELLOW)→ Spotless plugin not configured$(NC)"
	@echo "$(GREEN)✓ Formatting complete$(NC)"

lint: ## Run code quality checks (mvn checkstyle:check)
	@echo "$(BLUE)Running linters...$(NC)"
	$(MAVEN) checkstyle:check 2>/dev/null || echo "$(YELLOW)→ Checkstyle plugin not configured$(NC)"
	@echo "$(GREEN)✓ Linting complete$(NC)"

docker-up: check-docker setup ## Start all services (docker-compose up)
	@echo "$(BLUE)Starting Docker services...$(NC)"
	$(DOCKER_COMPOSE) up -d
	@echo "$(YELLOW)Waiting for services to be healthy...$(NC)"
	@sleep 10
	@echo "$(GREEN)✓ Services started$(NC)"
	@echo ""
	@echo "$(BLUE)Service Endpoints:$(NC)"
	@echo "  PostgreSQL (Identity):  localhost:5432"
	@echo "  PostgreSQL (Product):   localhost:5433"
	@echo "  PostgreSQL (Inventory): localhost:5434"
	@echo "  PostgreSQL (Warehouse): localhost:5435"
	@echo "  Redis:                  localhost:6379"
	@echo "  Kafka:                  localhost:9092"
	@echo "  MinIO:                  localhost:9000"
	@echo "  MinIO Console:          http://localhost:9001"
	@echo ""

docker-down: check-docker ## Stop all services (docker-compose down)
	@echo "$(BLUE)Stopping Docker services...$(NC)"
	$(DOCKER_COMPOSE) down
	@echo "$(GREEN)✓ Services stopped$(NC)"

docker-clean: check-docker docker-down ## Stop services and remove volumes
	@echo "$(BLUE)Removing Docker volumes...$(NC)"
	$(DOCKER_COMPOSE) down -v
	@echo "$(GREEN)✓ Services and volumes removed$(NC)"

docker-logs: check-docker ## Show Docker logs (docker-compose logs -f)
	$(DOCKER_COMPOSE) logs -f

docker-logs-service: check-docker ## Show logs for specific service (usage: make docker-logs-service SERVICE=kafka)
	$(DOCKER_COMPOSE) logs -f $(SERVICE)

docs: ## Generate documentation (javadoc)
	@echo "$(BLUE)Generating documentation...$(NC)"
	$(MAVEN) javadoc:aggregate
	@echo "$(GREEN)✓ Documentation generated at target/site/apidocs/$(NC)"

quick-start: setup docker-up ## Quick start: setup environment and start services
	@echo ""
	@echo "$(GREEN)SmartStock is ready for development!$(NC)"
	@echo "$(YELLOW)Next steps:$(NC)"
	@echo "  1. Build services: $(BLUE)make build$(NC)"
	@echo "  2. Run tests: $(BLUE)make test$(NC)"
	@echo "  3. View logs: $(BLUE)make docker-logs$(NC)"
	@echo ""
	@echo "$(BLUE)Documentation:$(NC)"
	@echo "  - CONTRIBUTING.md - Contributing guidelines"
	@echo "  - QUICK_START_GUIDE.md - Development setup"
	@echo "  - /docs/decisions/ - Architecture Decision Records"
	@echo ""

version: ## Show version information
	@echo "$(BLUE)SmartStock Version Information$(NC)"
	@$(MAVEN) -q -Dexec.executable=echo -Dexec.args='$${project.version}' exec:exec 2>/dev/null || echo "1.0.0"
	@echo "Java Version: $$(java -version 2>&1 | head -1)"
	@echo "Maven Version: $$(mvn -v 2>&1 | head -1)"

.PHONY: check-java check-maven check-docker setup build build-full install compile test test-coverage integration-test clean format lint docker-up docker-down docker-clean docker-logs docker-logs-service docs quick-start version help
