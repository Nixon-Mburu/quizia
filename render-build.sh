#!/bin/bash
set -e

# Install Java 21 if not available
if ! command -v java &> /dev/null; then
    echo "Installing Java 21..."
    apt-get update
    apt-get install -y openjdk-21-jdk
fi

# Build the backend
cd quizia_backend
./gradlew clean build -x test
