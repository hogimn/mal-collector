# Application Continuum

The evolution of a component-based architecture

See Git tags for step-by-step notes.

```bash
git tag -ln

v1              First commit
v2              Functional groups
v3              Feature groups (Bounded Context)
v4              Components
v5              Applications
v6              Services
v7              Databases
v8              Versioning
v9              Service Discovery
v10             Circuit Breaker
```

## Getting started

1. Install Redis

    ```bash
    sudo apt update
    sudo apt install redis-server -y
    sudo systemctl enable redis-server
    sudo systemctl start redis-server
    ```

2. Modify `/etc/redis/redis.conf`

    ```bash
    sudo vim /etc/redis/redis.conf
    ```

    - Uncomment `requirepass foobared` and set your password

    ```
      sudo systemctl restart redis-server
    ```

3. Install MySQL

    ```bash
    sudo apt update
    sudo apt install mysql-server -y
    sudo systemctl enable mysql
    sudo systemctl start mysql
    ```

4. Modify `/etc/mysql/mysql.conf.d/mysqld.cnf` (or create a custom config file)

    ```text
    default-time-zone='+00:00'
    ```
    - Restart MySQL to apply changes:

    ```bash
    sudo systemctl restart mysql
    ```

5. Database Setup

    ```bash
    sudo mysql -v -uroot --execute="drop user 'uservices'@'localhost'"
    sudo mysql -v -uroot --execute="create user 'uservices'@'localhost' identified by 'uservices';"

    for database_name in  'anime' 'poll'; do
      sudo mysql -v -uroot --execute="drop database if exists test_${database_name}"
      sudo mysql -v -uroot --execute="create database test_${database_name}"
      sudo mysql -v -uroot --execute="grant all on  test_${database_name}.* to 'uservices'@'localhost';"
      sudo mysql -v -uroot --execute="grant select on performance_schema.* to 'uservices'@'localhost';"

      sudo mysql -v -uroot --execute="drop database if exists dev_${database_name}"
      sudo mysql -v -uroot --execute="create database dev_${database_name}"
      sudo mysql -v -uroot --execute="grant all on  dev_${database_name}.* to 'uservices'@'localhost';"
      sudo mysql -v -uroot --execute="grant select on performance_schema.* to 'uservices'@'localhost';"
    done

    sudo mysql -v -uuservices -puservices test_anime --execute="select now();"
    ```

6. Schema Migrations

   ```bash
   flyway -cleanDisabled=false -user=uservices -password=uservices -url="jdbc:mysql://localhost:3306/test_anime" -locations=filesystem:databases/anime-database clean migrate
   flyway -cleanDisabled=false -user=uservices -password=uservices -url="jdbc:mysql://localhost:3306/test_poll" -locations=filesystem:databases/poll-database clean migrate
   flyway -cleanDisabled=false -user=uservices -password=uservices -url="jdbc:mysql://localhost:3306/dev_anime" -locations=filesystem:databases/anime-database clean migrate
   flyway -cleanDisabled=false -user=uservices -password=uservices -url="jdbc:mysql://localhost:3306/dev_poll" -locations=filesystem:databases/poll-database clean migrate
   ```

## Running the servers & Frontend via Makefile

This project provides a Makefile for convenient server management and testing.

### Summary of Main Commands

| Command                | Description                                                             |
|:-----------------------|:------------------------------------------------------------------------|
| `make` (or `make all`) | Build the project and run all backend servers + frontend                |
| `make build`           | Run Gradle build and install/build the frontend (`frontend/`)           |
| `make start`           | Run all services and frontend in the background (logs saved to `logs/`) |
| `make stop`            | Safely terminate all running servers and port processes                 |
| `make status`          | Check ports and PID status for all running instances                    |
| `make restart`         | Restart all servers (`stop` followed by `start`)                        |

### Registered Service Configuration

| Service Name       | Port (Default) | Instance Count     | Description                                    |
|:-------------------|:---------------|:-------------------|:-----------------------------------------------|
| `discovery-server` | `8888`         | 1                  | Redis-based service registry                   |
| `gateway-server`   | `8880`         | 1                  | API gateway                                    |
| `anime-server`     | `8881`         | 2 (`8881`, `8882`) | Anime data management service                  |
| `poll-server`      | `8883`         | 2 (`8883`, `8884`) | Poll data management service                   |
| `mal-server`       | `8885`         | 1                  | MyAnimeList integration and collection service |
| `frontend`         | `3000`         | 1                  | React frontend application                     |

## API Testing Commands

You can easily test API operations from your terminal using the provided Makefile.
(jq is recommended for nicely formatted JSON output).

### 1. Anime API Tests

- Get anime with no poll:
    ```bash
    make test-no-poll
    ```
- Get anime by ID:
    ```bash
    make test-anime-by-id id=12345
    ```
- Get anime by year and season:
    ```bash
    make test-anime-by-season year=2026 season=summer
    ```

### 2. MAL Collector API Tests (Collection Jobs)

- Collect by ID list:
    ```bash
    make test-collect-anime-by-ids ids="1,2,3"
    ```
- Collect by specific season:
    ```bash
    make test-collect-anime-by-season year=2026 season=spring
    ```
- Collect current season automatically:
    ```bash
    make test-collect-anime-current-season
    ```
- Collect archive:
    ```bash
    make test-collect-anime-archive
    ```

## Environment Variables Reference

Reference list of environment variables used for manual execution or custom configuration:

| Variable                        | Used by             | Description                                                 |
|:--------------------------------|:--------------------|:------------------------------------------------------------|
| `PORT`                          | all servers         | Port the server listens on                                  |
| `DATABASE_URL`                  | application servers | JDBC URL for the server's MySQL database                    |
| `REDIS_HOST` / `REDIS_PASSWORD` | discovery server    | Redis connection used for the service registry              |
| `DISCOVERY_SERVER_ENDPOINT`     | application servers | Base URL of the discovery server for heartbeats and lookups |
| `MAL_CLIENT_ID`                 | mal-server          | MyAnimeList API Client ID                                   |
| `MAL_COLLECTOR_GATEWAY_URL`     | frontend            | URL of the API Gateway for frontend requests                |

## License & Attribution

This project is licensed under the **GNU General Public License v2.0 (GPL-2.0)** due to the usage of the
[Mal4J](https://github.com/KatsuteDev/Mal4J) library wrapper.

It is a derivative work based on [application-continuum](https://github.com/initialcapacity/application-continuum) by
initialcapacity.

The original architecture and source files have been modified and adapted for the `mal-collector` project, with the
original commit history fully preserved.
