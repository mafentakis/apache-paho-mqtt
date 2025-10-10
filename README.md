# Apache Paho MQTT Subscriber Example

This project demonstrates how to build a simple MQTT subscriber with Java 21, Maven and the [Eclipse Paho](https://www.eclipse.org/paho/) client.  A development container definition is included to provide a ready-to-use Ubuntu environment alongside the required Java tooling.

The primary workspace container is based on the `mcr.microsoft.com/devcontainers/java:1-21-bullseye` image, so both the Java 21 toolchain and Maven are available on the command line as soon as the container starts. Mosquitto CLI tools are also preinstalled for quick manual testing.

## Prerequisites

* Java 21
* Apache Maven 3.9+
* Docker Engine 20.10+ (on your host machine if you choose to run Mosquitto with Docker)
* An MQTT broker (see [Running Mosquitto manually](#running-mosquitto-manually) for quick setup instructions)

## Building the project

```bash
mvn -ntp clean package
```

The build produces a runnable fat JAR at `target/java_maven_poc_mqtt_subscriber_simple-1.0.0.RELEASE-jar-with-dependencies.jar`.

## Running the subscriber

1. Ensure that an MQTT broker is available. Start the broker manually on your host machine before attaching to the dev container. The default configuration below exposes Mosquitto at:
   * `tcp://localhost:1883` from both the dev container and your host machine

2. Launch the subscriber:

    ```bash
    java -jar target/java_maven_poc_mqtt_subscriber_simple-1.0.0.RELEASE-jar-with-dependencies.jar
    ```

   Customise the connection via JVM system properties if required:

    * `-Dmqtt.broker=tcp://localhost:1883`
    * `-Dmqtt.clientId=mqtt_subscriber_simple`
    * `-Dmqtt.topic=mqtt_simple_topic`
    * `-Dmqtt.qos=0`

    When running inside the dev container, keep `-Dmqtt.broker=tcp://localhost:1883` to target the Mosquitto broker you started locally.


From outside the dev container target `localhost` as well.  The subscriber logs the received payload and QoS using Log4j2.

## Running Mosquitto manually

The dev container does not manage or start Mosquitto for you. Start the broker on your host machine before attaching to the dev container (or from another terminal on the host).

### Quick Start with Docker (Recommended)

**Important:** Mosquitto 2.x requires explicit configuration to allow connections. A `mosquitto.conf` file is provided in the repository.

1. Launch Mosquitto with the provided configuration:

   ```bash
   docker run -d --name mosquitto-dev -p 1883:1883 -v ${PWD}/mosquitto.conf:/mosquitto/config/mosquitto.conf eclipse-mosquitto:2
   ```

2. Verify it's running:

   ```bash
   docker logs mosquitto-dev
   ```

   You should see: `mosquitto version 2.0.x running`

3. When you are finished developing, stop and remove the container:

   ```bash
   docker rm -f mosquitto-dev
   ```

### Testing pub/sub flow

1. **Terminal 1** - Start subscriber (keeps running):
   ```bash
   docker exec -it mosquitto-dev mosquitto_sub -h localhost -t mqtt_simple_topic
   ```

2. **Terminal 2** - Publish a message:
   ```bash
   docker exec mosquitto-dev mosquitto_pub -h localhost -t mqtt_simple_topic -m "Hello from Mosquitto"
   ```

You should see the message appear in Terminal 1.

### Other useful commands

- Subscribe to all topics: `mosquitto_sub -h localhost -t '#'`
- Subscribe with verbose output: `mosquitto_sub -h localhost -t mqtt_simple_topic -v`
- Shell into Mosquitto container: `docker exec -it mosquitto-dev /bin/sh`

## Troubleshooting

### Connection Refused or Connection Lost Errors

If you see errors like:
```
org.eclipse.paho.client.mqttv3.MqttException: Connection lost
Caused by: java.io.EOFException
```

**Common causes and solutions:**

1. **Mosquitto is not running**
   ```bash
   # Check if container is running
   docker ps | grep mosquitto

   # If not, start it with the config file
   docker run -d --name mosquitto-dev -p 1883:1883 -v ${PWD}/mosquitto.conf:/mosquitto/config/mosquitto.conf eclipse-mosquitto:2
   ```

2. **Mosquitto is in "local only mode"**
   - Check the logs: `docker logs mosquitto-dev`
   - If you see "Starting in local only mode", the broker needs the `mosquitto.conf` file
   - Restart with: `docker rm -f mosquitto-dev` then use the command above

3. **Port 1883 is already in use**
   ```bash
   # Check what's using the port
   netstat -an | grep 1883
   # or on Linux/Mac
   lsof -i :1883
   ```

4. **Network connectivity from dev container**
   - If running inside a dev container, ensure the broker is accessible
   - Try connecting to `host.docker.internal` instead of `localhost`:
     ```bash
     java -Dmqtt.broker=tcp://host.docker.internal:1883 -jar target/...jar
     ```

### Testing Broker Connectivity

Quick test to verify the broker is working:
```bash
# This should complete without errors
mosquitto_pub -h localhost -p 1883 -t test -m "hello"

# Listen for the test message
mosquitto_sub -h localhost -p 1883 -t test
```

