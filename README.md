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

### Getting started

1. Install redis
   
    ```bash
    brew install redis
    ```
   
2. Modify `/opt/homebrew/etc/redis.conf` (`/usr/local/etc/redis.conf` on Intel Macs)
   
    ```bash
    requirepass foobared
    ```
   
3. Install mysql
   
    ```bash
    brew install mysql
    ```
   
4. Modify `/opt/homebrew/etc/my.cnf` (`/usr/local/etc/my.cnf` on Intel Macs)
   
    ```text
    default-time-zone='+00:00'
    ```
   
5. Database setup
   
    ```bash
    sudo mysql -v -uroot --execute="drop user 'uservices'@'localhost'"
    sudo mysql -v -uroot --execute="create user 'uservices'@'localhost' identified by 'uservices';"

    for database_name in 'allocations' 'backlog' 'registration' 'timesheets' 'anime' 'poll'; do
      sudo mysql -v -uroot --execute="drop database if exists ${database_name}_test"
      sudo mysql -v -uroot --execute="create database ${database_name}_test"
      sudo mysql -v -uroot --execute="grant all on  ${database_name}_test.* to 'uservices'@'localhost';"
      sudo mysql -v -uroot --execute="grant select on performance_schema.* to 'uservices'@'localhost';"
    done
    sudo mysql -v -uuservices -puservices registration_test --execute="select now();"
    ```

6. Schema Migrations

   ```bash
   flyway -cleanDisabled=false -user=uservices -password=uservices -url="jdbc:mysql://localhost:3306/allocations_test" -locations=filesystem:databases/allocations-database clean migrate
   flyway -cleanDisabled=false -user=uservices -password=uservices -url="jdbc:mysql://localhost:3306/backlog_test" -locations=filesystem:databases/backlog-database clean migrate
   flyway -cleanDisabled=false -user=uservices -password=uservices -url="jdbc:mysql://localhost:3306/registration_test" -locations=filesystem:databases/registration-database clean migrate
   flyway -cleanDisabled=false -user=uservices -password=uservices -url="jdbc:mysql://localhost:3306/timesheets_test" -locations=filesystem:databases/timesheets-database clean migrate
   flyway -cleanDisabled=false -user=uservices -password=uservices -url="jdbc:mysql://localhost:3306/anime_test" -locations=filesystem:databases/anime-database clean migrate
   flyway -cleanDisabled=false -user=uservices -password=uservices -url="jdbc:mysql://localhost:3306/poll_test" -locations=filesystem:databases/poll-database clean migrate
   ```

7. Run tests

   ```bash
   ./gradlew build
   ```

### Running the servers

Each server is configured through environment variables. Build the runnable jars first:

```bash
./gradlew build
```

Start the discovery server (backed by redis):

```bash
PORT=8888 \
REDIS_HOST=localhost \
REDIS_PASSWORD=foobared \
java -jar applications/discovery-server/build/libs/discovery-server.jar
```

Then start each application server (backed by mysql), pointing it at the discovery server:

```bash
PORT=8881 \
DATABASE_URL="jdbc:mysql://localhost:3306/allocations_test?user=uservices&password=uservices" \
DISCOVERY_SERVER_ENDPOINT=http://localhost:8888 \
java -jar applications/allocations-server/build/libs/allocations-server.jar

PORT=8882 \
DATABASE_URL="jdbc:mysql://localhost:3306/backlog_test?user=uservices&password=uservices" \
DISCOVERY_SERVER_ENDPOINT=http://localhost:8888 \
java -jar applications/backlog-server/build/libs/backlog-server.jar

PORT=8883 \
DATABASE_URL="jdbc:mysql://localhost:3306/registration_test?user=uservices&password=uservices" \
DISCOVERY_SERVER_ENDPOINT=http://localhost:8888 \
java -jar applications/registration-server/build/libs/registration-server.jar

PORT=8884 \
DATABASE_URL="jdbc:mysql://localhost:3306/timesheets_test?user=uservices&password=uservices" \
DISCOVERY_SERVER_ENDPOINT=http://localhost:8888 \
java -jar applications/timesheets-server/build/libs/timesheets-server.jar

PORT=8885 \
DATABASE_URL="jdbc:mysql://localhost:3306/anime_test?user=uservices&password=uservices" \
DISCOVERY_SERVER_ENDPOINT=http://localhost:8888 \
java -jar applications/anime-server/build/libs/anime-server.jar

PORT=8886 \
DATABASE_URL="jdbc:mysql://localhost:3306/poll_test?user=uservices&password=uservices" \
DISCOVERY_SERVER_ENDPOINT=http://localhost:8888 \
java -jar applications/poll-server/build/libs/poll-server.jar

PORT=8887 \
MAL_CLIENT_ID="YOUR_MAL_CLIENT_ID" \
DISCOVERY_SERVER_ENDPOINT=http://localhost:8888 \
java -jar applications/mal-server/build/libs/mal-server.jar
```

| Variable | Used by | Description |
| --- | --- | --- |
| `PORT` | all servers | Port the server listens on |
| `DATABASE_URL` | application servers | JDBC URL for the server's mysql database |
| `REDIS_HOST` / `REDIS_PASSWORD` | discovery server | Redis connection used for the service registry |
| `DISCOVERY_SERVER_ENDPOINT` | application servers | Base URL of the discovery server for heartbeats and lookups |

### License & Attribution
This project is licensed under the Apache License 2.0. 
It is a derivative work based on [application-continuum](https://github.com/initialcapacity/application-continuum) by initialcapacity. 

The original architecture and source files have been modified and adapted for the `malchart` project, with the original commit history fully preserved.
