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

IBM MQ is an enterprise-grade messaging platform that includes MQTT support via its telemetry service. This allows you to use the same Eclipse Paho MQTT client to connect to IBM MQ.

**Important:** MQTT configuration files are provided in the `ibmmq/` directory to enable MQTT support automatically.

### Option 1: Automatic Configuration (Recommended)

Build a custom IBM MQ image with MQTT pre-configured:

1. Build the IBM MQ image with MQTT enabled:

   ```bash
   docker build -f ibmmq/Dockerfile -t ibmmq-mqtt:latest ibmmq
   ```

2. Run the container:

   ```bash
   docker run -d --name ibmmq-dev \
     -p 1883:1883 \
     -p 1414:1414 \
     -p 9443:9443 \
     -e LICENSE=accept \
     -e MQ_QMGR_NAME=QM1 \
     -e MQ_APP_PASSWORD=passw0rd \
     ibmmq-mqtt:latest
   ```

3. Wait for IBM MQ to start and auto-configure MQTT (check logs):

   ```bash
   docker logs -f ibmmq-dev
   ```

   Wait until you see: `IBM MQ with MQTT is ready on port 1883`

4. Your Java subscriber can now connect immediately:

   ```bash
   java -jar target/java_maven_poc_mqtt_subscriber_simple-1.0.0.RELEASE-jar-with-dependencies.jar
   ```

**Ports exposed:**
- `1883`: MQTT (same as Mosquitto)
- `1414`: IBM MQ native protocol
- `9443`: IBM MQ Web Console (https://localhost:9443/ibmmq/console)

**Web Console Access:**
- URL: https://localhost:9443/ibmmq/console
- Username: `admin`
- Password: `passw0rd`

**Cleanup:**
```bash
docker rm -f ibmmq-dev
```

---

### Option 2: Manual Configuration

If you prefer not to build a custom image:

1. Launch IBM MQ Developer Edition with MQTT configuration:

   ```bash
   docker run -d --name ibmmq-dev \
     -p 1883:1883 \
     -p 1414:1414 \
     -p 9443:9443 \
     -e LICENSE=accept \
     -e MQ_QMGR_NAME=QM1 \
     -e MQ_APP_PASSWORD=passw0rd \
     -v ${PWD}/ibmmq/ibmmq-mqtt.mqsc:/etc/mqm/ibmmq-mqtt.mqsc \
     icr.io/ibm-messaging/mq:latest
   ```

2. Wait for IBM MQ to start:

   ```bash
   docker logs ibmmq-dev
   ```

   Wait until you see: `Started web server`

3. Apply the MQTT configuration:

   ```bash
   docker exec ibmmq-dev bash -c "cat /etc/mqm/ibmmq-mqtt.mqsc | runmqsc QM1"
   ```

4. Verify MQTT service is running:

   ```bash
   docker exec ibmmq-dev bash -c "echo 'DISPLAY SERVICE(SYSTEM.MQTT.SERVICE)' | runmqsc QM1"
   ```

   Look for `SERVSTATUS(RUNNING)` in the output.

5. Cleanup:

   ```bash
   docker rm -f ibmmq-dev
   ```

### Testing pub/sub flow with IBM MQ

1. **Terminal 1** - Start your Java subscriber:
   ```bash
   java -jar target/java_maven_poc_mqtt_subscriber_simple-1.0.0.RELEASE-jar-with-dependencies.jar
   ```

2. **Terminal 2** - Publish a message to the topic. Choose one of these methods:

   **Option A: Using IBM MQ's amqspub command (native MQ)**
   ```bash
   docker exec ibmmq-dev bash -c "echo 'Hello from IBM MQ' | /opt/mqm/samp/bin/amqspub mqtt_simple_topic QM1"
   ```

   **Option B: Using IBM MQ's MQTT sample publisher**
   ```bash
   docker exec ibmmq-dev bash -c "/opt/mqm/samp/bin/amqspub -m 'Hello from IBM MQ MQTT' -t mqtt_simple_topic -h localhost -p 1883"
   ```

   **Option C: Using mosquitto_pub from inside the container**

   First, install mosquitto-clients in the IBM MQ container:
   ```bash
   docker exec -u root ibmmq-dev bash -c "microdnf install -y mosquitto && microdnf clean all"
   ```

   Then publish:
   ```bash
   docker exec ibmmq-dev mosquitto_pub -h localhost -p 1883 -t mqtt_simple_topic -m "Hello from IBM MQ via Mosquitto"
   ```

   **Option D: Using mosquitto_pub from your host (if installed)**
   ```bash
   mosquitto_pub -h localhost -p 1883 -t mqtt_simple_topic -m "Hello from IBM MQ"
   ```

   **Option E: Using Python script inside container**
   ```bash
   docker exec ibmmq-dev bash -c "cat > /tmp/mqtt_pub.py << 'PYEOF'
import socket
import time

# Simple MQTT CONNECT and PUBLISH
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect(('localhost', 1883))

# MQTT CONNECT packet
connect = bytearray([0x10, 0x10, 0x00, 0x04, 0x4d, 0x51, 0x54, 0x54, 0x04, 0x02, 0x00, 0x3c, 0x00, 0x04, 0x74, 0x65, 0x73, 0x74])
sock.send(connect)
time.sleep(0.1)

# MQTT PUBLISH packet for topic 'mqtt_simple_topic' with message 'Hello from IBM MQ Python'
topic = b'mqtt_simple_topic'
message = b'Hello from IBM MQ Python'
publish = bytearray([0x30])  # PUBLISH
remaining_length = 2 + len(topic) + len(message)
publish.append(remaining_length)
publish.extend(len(topic).to_bytes(2, 'big'))
publish.extend(topic)
publish.extend(message)
sock.send(publish)
time.sleep(0.1)

sock.close()
print('Message published')
PYEOF
python3 /tmp/mqtt_pub.py"
   ```

You should see the message appear in Terminal 1 (Java subscriber logs).

**Recommended**: Use Option A (amqspub) or Option D (mosquitto_pub from host) for simplicity.

### IBM MQ MQTT Configuration Details

The MQTT service (`SYSTEM.MQTT.SERVICE`) runs the MQ Telemetry (MQXR) service which:

- **Queue Manager**: QM1
- **MQTT Port**: 1883 (same as Mosquitto)
- **Service**: Runs `/opt/mqm/bin/runMQXRService` to handle MQTT protocol
- **Authentication**: Anonymous connections allowed in dev mode
- **Topics**: IBM MQ automatically maps MQTT topics to MQ topics

**Note**: The MQTT service must be explicitly defined and started (as shown above) - it's not enabled by default in the IBM MQ Docker image.

### Useful IBM MQ commands

- View queue manager status:
  ```bash
  docker exec ibmmq-dev dspmq
  ```

- Display MQTT service status:
  ```bash
  docker exec ibmmq-dev echo "DISPLAY SERVICE(SYSTEM.MQTT.SERVICE)" | runmqsc QM1
  ```

- View all channels (to find MQTT-related channels):
  ```bash
  docker exec ibmmq-dev echo "DISPLAY CHANNEL(*)" | runmqsc QM1
  ```

- View listener status (MQTT listens on port 1883):
  ```bash
  docker exec ibmmq-dev echo "DISPLAY LISTENER(*)" | runmqsc QM1
  ```

- View MQTT topics:
  ```bash
  docker exec ibmmq-dev echo "DISPLAY TOPIC(*)" | runmqsc QM1
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

