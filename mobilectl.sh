#!/bin/bash

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Path to the JAR
JAR_PATH="$SCRIPT_DIR/cli/build/libs/cli-0.1.0.jar"

# Check if JAR exists, build if needed
if [ ! -f "$JAR_PATH" ]; then
    echo "🔨 Building mobilectl..."
    cd "$SCRIPT_DIR"
    ./gradlew cli:build -q

    if [ ! -f "$JAR_PATH" ]; then
        echo "❌ Build failed"
        exit 1
    fi
fi

# Run mobilectl with all arguments
java -jar "$JAR_PATH" "$@"
