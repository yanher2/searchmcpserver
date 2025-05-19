#!/bin/bash
echo "Starting JD Laptop Search Service..."

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    exit 1
fi

# Check if PostgreSQL is running
if ! command -v pg_isready &> /dev/null; then
    echo "Warning: pg_isready command not found"
    echo "Please make sure PostgreSQL is installed and running"
    read -p "Do you want to continue anyway? [y/N] " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    if ! pg_isready -h localhost -p 5432 &> /dev/null; then
        echo "Warning: PostgreSQL is not running"
        echo "Please make sure PostgreSQL is installed and running"
        read -p "Do you want to continue anyway? [y/N] " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
fi

# Build the project
echo "Building the project..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Error: Build failed"
    exit 1
fi

# Run the application
echo "Starting the application..."
java -jar target/java-math-server-1.0-SNAPSHOT.jar
