# IBM MQ MQTT Configuration

This directory contains configuration files for running IBM MQ with MQTT support.

## Files

### `Dockerfile`
Custom IBM MQ Docker image that automatically configures MQTT on startup.

**Usage:**
```bash
docker build -f ibmmq/Dockerfile -t ibmmq-mqtt:latest ibmmq
docker run -d --name ibmmq-dev \
  -p 1883:1883 \
  -p 1414:1414 \
  -p 9443:9443 \
  -e LICENSE=accept \
  -e MQ_QMGR_NAME=QM1 \
  -e MQ_APP_PASSWORD=passw0rd \
  ibmmq-mqtt:latest
```

### `ibmmq-mqtt.mqsc`
MQSC script that defines and starts the MQTT telemetry service.

**What it does:**
- Defines `SYSTEM.MQTT.SERVICE` to run the MQ Telemetry (MQXR) service
- Starts the MQTT service automatically
- Creates necessary queues for MQTT retained messages
- Configures MQTT to listen on port 1883

### `ibmmq-startup.sh`
Bash script that runs on container startup to apply MQTT configuration.

**What it does:**
- Waits for the queue manager (QM1) to start
- Applies the MQSC configuration from `ibmmq-mqtt.mqsc`
- Verifies the MQTT service is running
- Logs status messages for debugging

## Quick Start

### Automatic Configuration (Recommended)

```bash
# Build the image
docker build -f ibmmq/Dockerfile -t ibmmq-mqtt:latest ibmmq

# Run the container
docker run -d --name ibmmq-dev \
  -p 1883:1883 \
  -p 9443:9443 \
  -e LICENSE=accept \
  -e MQ_QMGR_NAME=QM1 \
  -e MQ_APP_PASSWORD=passw0rd \
  ibmmq-mqtt:latest

# Wait for startup
docker logs -f ibmmq-dev
# Wait for: "IBM MQ with MQTT is ready on port 1883"
```

### Manual Configuration

```bash
# Run standard IBM MQ image with config mounted
docker run -d --name ibmmq-dev \
  -p 1883:1883 \
  -p 9443:9443 \
  -e LICENSE=accept \
  -e MQ_QMGR_NAME=QM1 \
  -e MQ_APP_PASSWORD=passw0rd \
  -v ${PWD}/ibmmq/ibmmq-mqtt.mqsc:/etc/mqm/ibmmq-mqtt.mqsc \
  icr.io/ibm-messaging/mq:latest

# Apply configuration manually
docker exec ibmmq-dev bash -c "cat /etc/mqm/ibmmq-mqtt.mqsc | runmqsc QM1"
```

## Ports

- **1883**: MQTT protocol (same as Mosquitto)
- **1414**: IBM MQ native protocol
- **9443**: IBM MQ Web Console (https://localhost:9443/ibmmq/console)

## Web Console

- **URL**: https://localhost:9443/ibmmq/console
- **Username**: `admin`
- **Password**: `passw0rd`

## Testing

After starting IBM MQ with MQTT, test the connection:

```bash
# Publish a message
docker exec ibmmq-dev bash -c "echo 'Hello from IBM MQ' | /opt/mqm/samp/bin/amqspub mqtt_simple_topic QM1"

# Or use your Java subscriber
java -jar target/java_maven_poc_mqtt_subscriber_simple-1.0.0.RELEASE-jar-with-dependencies.jar
```

## Troubleshooting

### Check if MQTT service is running
```bash
docker exec ibmmq-dev bash -c "echo 'DISPLAY SERVICE(SYSTEM.MQTT.SERVICE)' | runmqsc QM1"
```

Look for `SERVSTATUS(RUNNING)` in the output.

### View MQTT service logs
```bash
docker exec ibmmq-dev cat /var/mqm/errors/mqxr.stdout
docker exec ibmmq-dev cat /var/mqm/errors/mqxr.stderr
```

### Check queue manager status
```bash
docker exec ibmmq-dev dspmq
```

### View all listeners
```bash
docker exec ibmmq-dev bash -c "echo 'DISPLAY LISTENER(*)' | runmqsc QM1"
```

