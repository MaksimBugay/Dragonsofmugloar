# Dragons of Mugloar

With Java 25 and Maven installed, run from the project root:

```sh
./run.sh
```

The script builds both modules with `mvn -B -ntp -pl gateway-service -am -DskipTests package`, producing the executable Spring Boot fat JAR at `gateway-service/target/gateway-service-0.0.1-SNAPSHOT.jar`. It starts the server using the local profile, waits for readiness, and executes the equivalent of:

```sh
curl --request POST http://localhost:8080/bmv/dragonsofmugloar/api/v1/autoplay \
  --header 'Content-Type: application/json' \
  --header 'Accept: application/json' \
  --data '{}'
```

The JSON response is printed in the terminal. The server stays running until Ctrl+C; logs are written to `gateway-service/target/server-8080.log`. The local profile supplies the development cluster secret.

To stop the server automatically after the request:

```sh
./run.sh --once
```

To choose another port or customize the game request:

```sh
PORT=8081 REQUEST_BODY='{"timeLimitMillis":60000}' ./run.sh --once
```

`STARTUP_TIMEOUT` defaults to 60 seconds and `REQUEST_TIMEOUT` to 660 seconds. Tests are skipped by the launcher; run `mvn verify` to execute them.
