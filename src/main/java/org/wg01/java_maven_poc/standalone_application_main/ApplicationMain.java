package org.wg01.java_maven_poc.standalone_application_main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public final class ApplicationMain {

    private static final Logger LOGGER = LogManager.getLogger(ApplicationMain.class);

    private static final String DEFAULT_BROKER_URI = "tcp://localhost:1883";
    private static final String DEFAULT_CLIENT_ID = "mqtt_subscriber_simple";
    private static final String DEFAULT_TOPIC = "mqtt_simple_topic";
    private static final int DEFAULT_QOS = 0;

    private ApplicationMain() {
        // Utility class
    }

    public static void main(String[] args) {
        String brokerUri = System.getProperty("mqtt.broker", DEFAULT_BROKER_URI);
        String clientId = System.getProperty("mqtt.clientId", DEFAULT_CLIENT_ID);
        String topic = System.getProperty("mqtt.topic", DEFAULT_TOPIC);
        int qos = parseQos(System.getProperty("mqtt.qos"), DEFAULT_QOS);

        MqttClient mqttClient = null;
        try {
            LOGGER.info("Starting MQTT subscriber using broker {} and topic '{}' (QoS {}).", brokerUri, topic, qos);

            MemoryPersistence persistence = new MemoryPersistence();
            mqttClient = new MqttClient(brokerUri, clientId, persistence);

            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);
            connectOptions.setAutomaticReconnect(true);

            mqttClient.setCallback(new LoggingMqttCallback());
            mqttClient.connect(connectOptions);
            mqttClient.subscribe(topic, qos);

            LOGGER.info("Subscription established. Press ENTER to exit.");
            waitForExitSignal();
            LOGGER.info("Shutdown signal received. Disconnecting MQTT client.");
        } catch (Exception exception) {
            LOGGER.error("Failed to run MQTT subscriber.", exception);
        } finally {
            if (mqttClient != null) {
                disconnectQuietly(mqttClient);
                closeQuietly(mqttClient);
            }
        }
    }

    private static int parseQos(String qosValue, int defaultQos) {
        if (qosValue == null || qosValue.isBlank()) {
            return defaultQos;
        }

        try {
            int parsedValue = Integer.parseInt(qosValue.trim());
            if (parsedValue < 0 || parsedValue > 2) {
                LOGGER.warn("QoS value '{}' is out of range (0-2). Falling back to {}.", qosValue, defaultQos);
                return defaultQos;
            }
            return parsedValue;
        } catch (NumberFormatException exception) {
            LOGGER.warn("Unable to parse QoS value '{}'. Falling back to {}.", qosValue, defaultQos);
            return defaultQos;
        }
    }

    private static void waitForExitSignal() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        reader.readLine();
    }

    private static void disconnectQuietly(MqttClient mqttClient) {
        try {
            if (mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } catch (Exception exception) {
            LOGGER.warn("Unable to disconnect MQTT client cleanly.", exception);
        }
    }

    private static void closeQuietly(MqttClient mqttClient) {
        try {
            mqttClient.close();
        } catch (Exception exception) {
            LOGGER.warn("Unable to close MQTT client cleanly.", exception);
        }
    }

    private static final class LoggingMqttCallback implements MqttCallback {

        @Override
        public void connectionLost(Throwable cause) {
            if (cause == null) {
                LOGGER.warn("Connection to MQTT broker lost.");
            } else {
                LOGGER.warn("Connection to MQTT broker lost: {}", cause.getMessage(), cause);
            }
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            LOGGER.info("Received message on topic '{}' with QoS {}: {}", topic, message.getQos(), payload);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            boolean completed = token != null && token.isComplete();
            LOGGER.debug("Delivery complete (isComplete={}).", completed);
        }
    }
}
