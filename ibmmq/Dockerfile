FROM icr.io/ibm-messaging/mq:latest

# Copy MQTT configuration and startup script from ibmmq directory
COPY ibmmq-mqtt.mqsc /etc/mqm/ibmmq-mqtt.mqsc
COPY ibmmq-startup.sh /usr/local/bin/ibmmq-startup.sh

# Make startup script executable
USER root
RUN chmod +x /usr/local/bin/ibmmq-startup.sh
USER 1001

# The startup script will be executed after the container starts
# We'll run it in the background after the main MQ process starts
CMD ["/bin/bash", "-c", "/usr/local/bin/ibmmq-startup.sh & exec runmqdevserver"]

