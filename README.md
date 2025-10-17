# Apache Paho MQTT Subscriber Example

This project demonstrates how to build a simple MQTT subscriber with Java 21, Maven and the [Eclipse Paho](https://www.eclipse.org/paho/) client.  A development container definition is included to provide a ready-to-use Ubuntu environment alongside the required Java tooling.

The primary workspace container is based on the `mcr.microsoft.com/devcontainers/java:1-21-bullseye` image, so both the Java 21 toolchain and Maven are available on the command line as soon as the container starts. Mosquitto CLI tools are also preinstalled for quick manual testing.

## Prerequisites

* Java 21
* Apache Maven 3.9+
* Docker Engine 20.10+ (on your host machine if you choose to run an MQTT broker with Docker)
* An MQTT broker - choose one:
  * [Mosquitto](#running-mosquitto-with-docker) (lightweight, MQTT-only)
  * [IBM MQ](#running-ibm-mq-with-docker) (enterprise messaging with MQTT support)

## Building the project

```bash
mvn -ntp clean package
```

The build produces a runnable fat JAR at `target/java_maven_poc_mqtt_subscriber_simple-1.0.0.RELEASE-jar-with-dependencies.jar`.

## Running the subscriber

1. Ensure that an MQTT broker is available. Start the broker manually on your host machine before attaching to the dev container:
   * **Mosquitto**: `tcp://localhost:1883` (default)
   * **IBM MQ**: `tcp://localhost:1883` (MQTT channel)

2. Launch the subscriber:

    ```bash
    java -jar target/java_maven_poc_mqtt_subscriber_simple-1.0.0.RELEASE-jar-with-dependencies.jar
    ```

   Customise the connection via JVM system properties if required:

    * `-Dmqtt.broker=tcp://localhost:1883` (default for both Mosquitto and IBM MQ)
    * `-Dmqtt.clientId=mqtt_subscriber_simple`
    * `-Dmqtt.topic=mqtt_simple_topic`
    * `-Dmqtt.qos=0`

   **Examples:**

   ```bash
   # Connect to Mosquitto (default)
   java -jar target/java_maven_poc_mqtt_subscriber_simple-1.0.0.RELEASE-jar-with-dependencies.jar

   # Connect to IBM MQ (same port, different broker)
   java -jar target/java_maven_poc_mqtt_subscriber_simple-1.0.0.RELEASE-jar-with-dependencies.jar

   # Connect to IBM MQ with custom topic
   java -Dmqtt.topic=dev/test -jar target/java_maven_poc_mqtt_subscriber_simple-1.0.0.RELEASE-jar-with-dependencies.jar
   ```

From outside the dev container target `localhost` as well. The subscriber logs the received payload and QoS using Log4j2.

## MQTT Broker Options

Choose one of the following MQTT brokers. Both work with the same Java subscriber application without code changes.

---

## Running Mosquitto with Docker

Mosquitto is a lightweight, open-source MQTT broker ideal for development and testing.

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

### Testing pub/sub flow with Mosquitto

1. **Terminal 1** - Start your Java subscriber:
   ```bash
   java -jar target/java_maven_poc_mqtt_subscriber_simple-1.0.0.RELEASE-jar-with-dependencies.jar
   ```

2. **Terminal 2** - Publish a message using Mosquitto CLI:
   ```bash
   docker exec mosquitto-dev mosquitto_pub -h localhost -t mqtt_simple_topic -m "Hello from Mosquitto"
   ```

You should see the message appear in Terminal 1 (Java subscriber logs).

### Other useful Mosquitto commands

- Subscribe to all topics: `mosquitto_sub -h localhost -t '#'`
- Subscribe with verbose output: `mosquitto_sub -h localhost -t mqtt_simple_topic -v`
- Shell into Mosquitto container: `docker exec -it mosquitto-dev /bin/sh`

---

## Running IBM MQ with Docker

IBM MQ is an enterprise-grade messaging platform that includes MQTT support via its telemetry channel. This allows you to use the same Eclipse Paho MQTT client to connect to IBM MQ.

### Quick Start with Docker

1. Launch IBM MQ Developer Edition with MQTT enabled:

   ```bash
   docker run -d --name ibmmq-dev \
     -p 1883:1883 \
     -p 9443:9443 \
     -e LICENSE=accept \
     -e MQ_QMGR_NAME=QM1 \
     -e MQ_APP_PASSWORD=passw0rd \
     -e MQ_ENABLE_METRICS=true \
     icr.io/ibm-messaging/mq:latest
   ```

   **Ports exposed:**
   - `1883`: MQTT (same as Mosquitto)
   - `9443`: IBM MQ Web Console (https://localhost:9443/ibmmq/console)

2. Verify it's running:

   ```bash
   docker logs ibmmq-dev
   ```

   Wait until you see: `Started web server`

3. Access the IBM MQ Web Console (optional):
   - URL: https://localhost:9443/ibmmq/console
   - Username: `admin`
   - Password: `passw0rd`

4. When you are finished developing, stop and remove the container:

   ```bash
   docker rm -f ibmmq-dev
   ```

### Testing pub/sub flow with IBM MQ

1. **Terminal 1** - Start your Java subscriber:
   ```bash
   java -jar target/java_maven_poc_mqtt_subscriber_simple-1.0.0.RELEASE-jar-with-dependencies.jar
   ```

2. **Terminal 2** - Publish a message using IBM MQ's sample MQTT publisher:
   ```bash
   docker exec ibmmq-dev /opt/mqm/samp/bin/amqspub mqtt_simple_topic QM1 <<EOF
   Hello from IBM MQ
   EOF
   ```

   Or use mosquitto_pub if you have it installed locally:
   ```bash
   mosquitto_pub -h localhost -p 1883 -t mqtt_simple_topic -m "Hello from IBM MQ"
   ```

You should see the message appear in Terminal 1 (Java subscriber logs).

### IBM MQ MQTT Configuration Details

IBM MQ automatically creates an MQTT channel on port 1883 when started with the developer image. The configuration includes:

- **Queue Manager**: QM1
- **MQTT Port**: 1883 (same as Mosquitto)
- **Authentication**: Anonymous connections allowed in dev mode
- **Topics**: IBM MQ automatically maps MQTT topics to MQ topics

### Useful IBM MQ commands

- View queue manager status:
  ```bash
  docker exec ibmmq-dev dspmq
  ```

- Display MQTT service status:
  ```bash
  docker exec ibmmq-dev echo "DISPLAY SERVICE(SYSTEM.MQTT.SERVICE)" | runmqsc QM1
  ```

- View MQTT channel status:
  ```bash
  docker exec ibmmq-dev echo "DISPLAY CHANNEL(SYSTEM.DEF.MQTT)" | runmqsc QM1
  ```

- Shell into IBM MQ container:
  ```bash
  docker exec -it ibmmq-dev /bin/bash
  ```

---

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

