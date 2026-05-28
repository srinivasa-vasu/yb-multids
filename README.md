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
- YugabyteDB installed and running, unless you use the Codespaces or devcontainer stack described below.

## Dev Containers, Codespaces, and DevPod

This repository now includes a dev container definition in [`.devcontainer/devcontainer.json`](./.devcontainer/devcontainer.json) so you can open it in:

- GitHub Codespaces
- VS Code Dev Containers
- DevPod via [`./devpod.sh`](./devpod.sh)

The app container is built from [`.devcontainer/Dockerfile`](./.devcontainer/Dockerfile), which uses a lightweight Eclipse Temurin Java 25 base with Maven plus YugabyteDB client tools. `ysqlsh` comes from the `yugabyte-client-2025.2.3.0-b149-linux-x86_64.tar.gz` release tarball; `yb-admin` and `yb-ts-cli` come from the matching `yugabytedb/yugabyte:2025.2.3.0-b149` image because they are not included in the client tarball. The stack also starts one YugabyteDB universe with a one-node primary cluster and a one-node read replica cluster, then launches the Spring Boot app after both YSQL endpoints are reachable. The app is exposed on port `8080`; primary YugabyteDB ports are forwarded for inspection, while read replica ports stay internal to the compose network.

Use at least a 4-core Codespace for the full stack. Each bundled YugabyteDB node is capped for development use, with the YB-Master limited to 512 MiB and YB-TServer limited to 1 GiB.

The Spring Boot app is started by [`.devcontainer/start-app.sh`](./.devcontainer/start-app.sh). Startup logs are written to `/tmp/multids.log` inside the app container.

The bundled YugabyteDB service uses an ephemeral data directory under `/tmp/yb_data` inside the database container. Rebuilding the Codespace recreates the sample database from the Flyway migrations.

Open a YSQL shell to the primary node from the app container terminal with:

```bash
ysqlsh
```

VS Code also opens this shell automatically on folder open using [`.vscode/tasks.json`](./.vscode/tasks.json). If automatic tasks are disabled in your client, run the `Open YSQL Shell` task manually.

The helper defaults to username/password/database `yugabyte`.

YugabyteDB admin tooling is also available:

```bash
yb-admin list_all_masters
yb-ts-cli list_tablets
```

`yb-ts-cli` defaults to `yb-primary:9100`.

The datasource URLs can still be overridden with environment variables if you want to point at another cluster:

- `SPRING_DATASOURCE_RW_URL`
- `SPRING_DATASOURCE_RO_URL`

## How to Build and Run

1.  **Clone the repository:**

    ```bash
    git clone https://github.com/srinivasa-vasu/yb-multids.git
    cd yb-multids
    ```

2.  **Configure the database:**

    The default Codespaces/devcontainer stack already points the application at the bundled YugabyteDB service. Set `SPRING_DATASOURCE_RW_URL` and `SPRING_DATASOURCE_RO_URL` only if you want to target a different database.

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
        minimum-idle: 2
        maximum-pool-size: 2
        auto-commit: false
        keepalive-time: 120000
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
        minimum-idle: 1
        maximum-pool-size: 1
        auto-commit: false
        keepalive-time: 120000
        connection-timeout: 15000
        data-source-properties:
          ApplicationName: multids-ro
          socketTimeout: 15
          yb-servers-refresh-interval: 180
          loginTimeout: 10
          connectTimeout: 5
```
