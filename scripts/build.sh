#!/bin/bash

# SmartStock Build & Test Script
# Builds project and runs tests with coverage

set -e

echo "🔨 Building SmartStock..."
echo "========================="
echo ""

SKIP_TESTS=${1:-false}
COVERAGE=${2:-true}

# Build
echo "Compiling..."
mvn clean compile

# Run tests
if [ "$SKIP_TESTS" != "skip" ]; then
    echo ""
    echo "Running tests..."
    mvn test
    
    # Generate coverage
    if [ "$COVERAGE" = "true" ]; then
        echo ""
        echo "Generating coverage report..."
        mvn jacoco:report
        echo "Coverage report: target/site/jacoco/index.html"
    fi
else
    echo "Skipping tests..."
    mvn package -DskipTests
fi

echo ""
echo "✅ Build successful!"
