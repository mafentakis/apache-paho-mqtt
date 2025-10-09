package org.wg01.java_maven_poc.standalone_application_main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class ApplicationMain {
	
	private static Logger LOGGER = LogManager.getLogger(ApplicationMain.class);
	
	public static void main(String[] args) {
		MqttClient subscriberMqttClient = null;
		try {
			LOGGER.debug("begin ApplicationMain -> static void main(String args[]");
			
			String mqttBroker = "tcp://localhost:1883";
			String subscriberMqttClientId = "mqtt_subscriber_simple";
			MemoryPersistence mqttPersistence = new MemoryPersistence();

			subscriberMqttClient = new MqttClient(mqttBroker, subscriberMqttClientId, mqttPersistence);

			MqttConnectOptions subscriberMqttConnectOptions = new MqttConnectOptions();
			subscriberMqttConnectOptions.setCleanSession(true);

			
			MqttCallback subscriberMqttCallback = new MqttCallback() {
				public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.println("topic: " + topic);
                    System.out.println("qos: " + message.getQos());
                    System.out.println("message content: " + new String(message.getPayload()));
                }

				@Override
				public void connectionLost(Throwable cause) {
					System.out.println("connectionLost: " + cause.getMessage());
				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken token) {
					System.out.println("deliveryComplete: " + token.isComplete());
				}
			};
			subscriberMqttClient.setCallback(subscriberMqttCallback);
			subscriberMqttClient.connect();
			
			String mqttTopic = "mqtt_simple_topic";
			int mqttQosLevel = 0;
			subscriberMqttClient.subscribe(mqttTopic, mqttQosLevel);
			
			System.out.println("Eingabe um zu beenden");
			System.in.read();
			
			LOGGER.debug("finish ApplicationMain -> static void main(String args[]");
		} catch (Exception e) {
			LOGGER.error("exception in ApplicationMain -> static void main(String args[])", e);
		} finally {
			try {
				if(subscriberMqttClient != null) {
					subscriberMqttClient.disconnect();
				}
			} catch (Exception e1) {
				LOGGER.error("exception in ApplicationMain -> static void main(String args[])", e1);
			}
			try {
				if(subscriberMqttClient != null) {
					subscriberMqttClient.close();
				}
			} catch (Exception e1) {
				LOGGER.error("exception in ApplicationMain -> static void main(String args[])", e1);
			}
			subscriberMqttClient = null;
		}
	}


}
