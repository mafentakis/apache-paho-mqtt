#!/bin/bash
# IBM MQ Startup Script with MQTT Configuration
# This script runs after the queue manager starts and applies MQTT configuration

set -e

echo "Waiting for queue manager QM1 to start..."
until /opt/mqm/bin/dspmq -m QM1 | grep -q "RUNNING"; do
  sleep 2
done

echo "Queue manager QM1 is running. Applying MQTT configuration..."
cat /etc/mqm/ibmmq-mqtt.mqsc | /opt/mqm/bin/runmqsc QM1

echo "MQTT configuration applied successfully."
echo "Verifying MQTT service status..."
echo "DISPLAY SERVICE(SYSTEM.MQTT.SERVICE)" | /opt/mqm/bin/runmqsc QM1

echo "IBM MQ with MQTT is ready on port 1883"

