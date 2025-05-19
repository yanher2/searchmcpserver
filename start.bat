@echo off
echo Starting JD Laptop Search Service...

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo Error: Java is not installed or not in PATH
    pause
    exit /b 1
)

REM Check if PostgreSQL is running
pg_isready -h localhost -p 5432 >nul 2>&1
if errorlevel 1 (
    echo Warning: PostgreSQL is not running
    echo Please make sure PostgreSQL is installed and running
    choice /C YN /M "Do you want to continue anyway"
    if errorlevel 2 exit /b 1
)

REM Build the project
echo Building the project...
call mvn clean package -DskipTests

if errorlevel 1 (
    echo Error: Build failed
    pause
    exit /b 1
)

REM Run the application
echo Starting the application...
java -jar target/java-search-mcp-server-1.0-SNAPSHOT.jar

pause
