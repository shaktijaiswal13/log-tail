#!/bin/bash

# build.sh - Build script for tail_logs
# Supports both Linux and macOS

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}  Tail Logs - Build Script${NC}"
echo -e "${YELLOW}========================================${NC}"

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed or not in PATH${NC}"
    exit 1
fi

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Error: Maven is not installed or not in PATH${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Java found: $(java -version 2>&1 | head -n 1)${NC}"
echo -e "${GREEN}✓ Maven found: $(mvn -version 2>&1 | head -n 1)${NC}"
echo ""

# Change to project directory
cd "$SCRIPT_DIR"

echo -e "${YELLOW}Building project...${NC}"
mvn clean package -q

# Check if build was successful
if [ -f "target/log-tail.jar" ]; then
    JAR_SIZE=$(ls -lh target/log-tail.jar | awk '{print $5}')
    echo -e "${GREEN}✓ Build successful!${NC}"
    echo -e "${GREEN}✓ JAR created: target/log-tail.jar (${JAR_SIZE})${NC}"
    echo ""
    echo -e "${YELLOW}To run the application:${NC}"
    echo -e "${GREEN}  java -jar target/log-tail.jar${NC}"
    echo -e "${YELLOW}Or use:${NC}"
    echo -e "${GREEN}  ./run.sh${NC}"
else
    echo -e "${RED}✗ Build failed! JAR not created.${NC}"
    exit 1
fi
