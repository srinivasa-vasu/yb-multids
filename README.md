# Spring Boot Multi-Datasource with YugabyteDB

This is a sample project that demonstrates how to configure multiple datasources (read-write and read-only) in a Spring Boot application with YugabyteDB.

## Project Structure

The project has the following structure:

- `src/main/java/io/data/`: Contains the main application code.
  - `MultiDSApplication.java`: The main Spring Boot application class.
  - `DataSourceConfig.java`: Configures the read-write and read-only datasources.
  - `DataSourceAspect.java`: An aspect to route the queries to the appropriate datasource.
  - `ReadOnly.java`: An annotation to mark a method as read-only.
  - `KVController.java`: A REST controller to handle the key-value operations.
  - `KVService.java`: A service class that contains the business logic.
  - `KVRepository.java`: A JPA repository to interact with the database.
  - `KeyValue.java`: A JPA entity to represent the key-value pair.
  - `KVRetryPolicy.java`: A custom retry policy for handling transaction errors.
- `src/main/resources/`: Contains the application resources.
  - `application.yaml`: The application configuration file.
  - `db/migration/`: Contains the Flyway database migration scripts.
    - `V1_0_0__create.sql`: The script to create the `kvinfo` table.
    - `V1_0_1__data.sql`: The script to insert some initial data into the `kvinfo` table.
- `pom.xml`: The Maven project object model file.

## Prerequisites

- Java 25 or higher
- Maven 3.6.3 or higher
- YugabyteDB installed and running.

## How to Build and Run

1.  **Clone the repository:**

    ```bash
    git clone https://github.com/srinivasa-vasu/multids.git
    cd multids
    ```

2.  **Update the `application.yaml` file:**

    Update the `spring.datasource.rw.url` and `spring.datasource.ro.url` properties in the `src/main/resources/application.yaml` file with the correct database connection details.

3.  **Build the project:**

    ```bash
    mvn clean install
    ```

4.  **Run the application:**

    ```bash
    java -jar target/multids-1.0.0.jar
    ```

    The application will be available at `http://localhost:8080`.

## REST API Endpoints

The application exposes the following REST API endpoints:

- `GET /keys`: Returns all the keys.
- `GET /keys/{key}`: Returns the value for the given key.
- `POST /keys`: Creates a new key-value pair. The request body should be a JSON object with `key` and `value` fields.
- `PUT /keys/{key}`: Updates the value for the given key. The request body should be a JSON object with the new `value`.
- `DELETE /keys/{key}`: Deletes the key-value pair for the given key.

## Database Schema

The application uses a single table named `kvinfo` with the following schema:

```sql
CREATE TABLE IF NOT EXISTS kvinfo
(
    key           uuid PRIMARY KEY,
    value         text
) SPLIT INTO 1 TABLETS;
```

## Datasource Configuration

The application is configured with two datasources:

- **Read-write datasource:** This datasource is used for all the write operations (create, update, and delete).
- **Read-only datasource:** This datasource is used for all the read operations.

The `DataSourceConfig` class configures the two datasources. The `DataSourceAspect` class is used to route the queries to the appropriate datasource based on the `@ReadOnly` annotation.

### Read-Write Datasource Configuration

```yaml
spring:
  datasource:
    rw:
      driver-class-name: com.yugabyte.Driver
      url: jdbc:yugabytedb://127.0.0.2:5433/yugabyte?load-balance=true&topology-keys=ybcloud.ap-south-1.ap-south-1c
      username: yugabyte
      password: yugabyte
      hikari:
        pool-name: rw-pool
        minimum-idle: 3
        maximum-pool-size: 3
        auto-commit: false
        keepalive-time: 120000
        connection-init-sql: "prepare warmup as SELECT 1; execute warmup; commit;"
        connection-timeout: 15000
        data-source-properties:
          ApplicationName: multids-rw
          socketTimeout: 15
          yb-servers-refresh-interval: 180
          loginTimeout: 10
          connectTimeout: 5
```

### Read-Only Datasource Configuration

```yaml
spring:
  datasource:
    ro:
      driver-class-name: com.yugabyte.Driver
      url: jdbc:yugabytedb://127.0.0.2:5433/yugabyte?load-balance=true&topology-keys=ybcloud.ap-south-1.ap-south-1b
      username: yugabyte
      password: yugabyte
      hikari:
        pool-name: ro-pool
        minimum-idle: 6
        maximum-pool-size: 6
        auto-commit: false
        keepalive-time: 120000
        connection-init-sql: "set default_transaction_read_only=on; set yb_read_from_followers=on; prepare warmup as SELECT 1; execute warmup; commit;"
        connection-timeout: 15000
        data-source-properties:
          ApplicationName: multids-ro
          socketTimeout: 15
          yb-servers-refresh-interval: 180
          loginTimeout: 10
          connectTimeout: 5
```
