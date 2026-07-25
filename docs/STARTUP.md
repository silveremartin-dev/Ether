# Ether Simulation - Startup Scripts

This directory contains convenience scripts for starting the simulation with its dependencies.

## Windows Scripts

### `start-docker.bat` - Full Startup (Recommended)
Starts PostgreSQL database via Docker Compose and launches the application.

**Usage:**
```batch
start-docker.bat
```

**What it does:**
1. Checks if Docker is running
2. Starts PostgreSQL + PostGIS container
3. Waits for database to be ready (health check)
4. Launches JavaFX application

### `start-no-db.bat` - Quick Start
Launches application without database (for development/testing).

**Usage:**
```batch
start-no-db.bat
```

### `stop.bat` - Shutdown
Stops the PostgreSQL database container.

**Usage:**
```batch
stop.bat
```

### `database-status.bat` - Status Check
Shows database container status.

**Usage:**
```batch
database-status.bat
```

## Prerequisites

- Docker Desktop installed and running
- Maven installed
- Java 21 installed

## Database Connection

When using `start-docker.bat`, the application will have access to:
- **Host:** localhost
- **Port:** 54320
- **Database:** ether_simulation
- **Username:** ether
- **Password:** dev_password

## Troubleshooting

### "Docker is not running"
- Open Docker Desktop
- Wait for it to fully start
- Run `start-docker.bat` again

### "Database is not ready" (timeout)
- Check Docker Desktop for errors
- Run `docker-compose logs postgres` to see database logs
- Try `docker-compose down` then `start-docker.bat` again

### Application fails to connect
- Verify database is running: `database-status.bat`
- Check port 54320 is not in use by another application
- Review application logs for connection errors

## Manual Commands

If you prefer manual control:

```batch
# Start database only
docker-compose up -d

# Check database status
docker-compose ps

# View database logs
docker-compose logs -f postgres

# Stop database
docker-compose down

# Run application only
mvn javafx:run
```
