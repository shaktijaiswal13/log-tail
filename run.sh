#!/bin/bash

# run.sh - Run script for tail_logs
# Checks if JAR is up to date and builds if necessary
# Supports both Linux and macOS

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Tail Logs - Run Script${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed or not in PATH${NC}"
    exit 1
fi

# Change to project directory
cd "$SCRIPT_DIR"

JAR_FILE="target/log-tail.jar"

# Function to check if rebuild is needed
needs_rebuild() {
    # If JAR doesn't exist, rebuild needed
    if [ ! -f "$JAR_FILE" ]; then
        return 0
    fi

    # Get JAR modification time
    JAR_MTIME=$(stat -f %m "$JAR_FILE" 2>/dev/null || stat -c %Y "$JAR_FILE" 2>/dev/null)

    # Find the most recent modification time in source files
    # Check src directory for any changes
    LATEST_SRC_MTIME=$(find src -type f \( -name "*.java" -o -name "*.fxml" -o -name "*.css" \) -exec stat -f %m {} \; 2>/dev/null | sort -rn | head -1 || \
                       find src -type f \( -name "*.java" -o -name "*.fxml" -o -name "*.css" \) -exec stat -c %Y {} \; 2>/dev/null | sort -rn | head -1)

    # Also check pom.xml
    POM_MTIME=$(stat -f %m pom.xml 2>/dev/null || stat -c %Y pom.xml 2>/dev/null)

    # Get the latest modification time
    LATEST_MTIME=$(echo -e "$LATEST_SRC_MTIME\n$POM_MTIME" | sort -rn | head -1)

    # If source files are newer than JAR, rebuild is needed
    if [ "$LATEST_MTIME" -gt "$JAR_MTIME" ]; then
        return 0
    fi

    return 1
}

# Check if rebuild is needed
if needs_rebuild; then
    if [ ! -f "$JAR_FILE" ]; then
        echo -e "${YELLOW}JAR file not found. Building...${NC}"
    else
        echo -e "${YELLOW}Source code updated. Rebuilding...${NC}"
    fi
    echo ""

    # Run build script
    bash "$SCRIPT_DIR/build.sh"

    if [ ! -f "$JAR_FILE" ]; then
        echo -e "${RED}Build failed!${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✓ Using latest build ($(stat -f "%Sa" "$JAR_FILE" 2>/dev/null || stat -c "%y" "$JAR_FILE" 2>/dev/null | cut -d. -f1))${NC}"
fi

echo ""
echo -e "${YELLOW}Starting Tail Logs...${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Run the JAR
exec java -jar "$JAR_FILE"
