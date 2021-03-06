/*******************************************************************************
* Copyright Indra Sistemas, S.A.
* 2013-2018 SPAIN
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*      http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************************/

package com.indra.sofia4cities.ssap.kp.implementations.mqtt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.fusesource.mqtt.client.Future;
import org.fusesource.mqtt.client.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.indra.sofia2.ssap.kp.Listener4SIBIndicationNotifications;
import com.indra.sofia2.ssap.kp.implementations.mqtt.MqttConstants;
import com.indra.sofia2.ssap.kp.implementations.utils.IndicationTask;
import com.indra.sofia2.ssap.ssap.SSAPMessage;
import com.indra.sofia2.ssap.ssap.SSAPMessageTypes;

/**
 * This thread will be continuously running to receive any kind of messages from
 * the SIB server.
 */
class MqttSubscriptionThread extends Thread {

	private static final Logger log = LoggerFactory.getLogger(MqttSubscriptionThread.class);

	private Future<Message> receive;
	private Boolean stop;
	private KpMQTTClient kpMqttClient;

	MqttSubscriptionThread(KpMQTTClient kpMqttClient) {
		stop = false;
		this.kpMqttClient = kpMqttClient;
	}

	protected void myStop() {
		log.info("Stopping MQTT subscription thread of the internal MQTT client {}.", kpMqttClient.getMqttClientId());
		this.stop = true;
		if (this.receive != null) {
			this.interrupt();
		}
	}

	protected boolean isStopped() {
		return stop;
	}

	@Override
	public void run() {
		stop = false;
		while (!stop) {
			Message message = null;
			String payload = null;
			try {
				receive = kpMqttClient.getMqttConnection().receive();
				// verify the reception
				message = receive.await();
				// gets the message payload (SSAPMessage)
				payload = new String(message.getPayload());
				log.debug("The internal MQTT client {} has received a message from the SIB server. Payload={}.",
						kpMqttClient.getMqttClientId(), payload);
			} catch (Throwable e) {
				boolean disconnectImmediately = false;
				if (e instanceof InterruptedException) {
					log.info(
							"The MQTT subscription thread of the internal MQTT client {} has been interrupted.",
							kpMqttClient.getMqttClientId());
				} else {
					log.error(
							"An exception has been raised in the MQTT subscription thread of the MQTT client {}. Cause = {}, errorMessage = {}.",
							kpMqttClient.getMqttClientId(), e.getCause(), e.getMessage());
					disconnectImmediately = true;
				}
				if (kpMqttClient.getResponseCallback() != null) {
					kpMqttClient.getResponseCallback().handle(null);
				}
				if (disconnectImmediately || !stop) {
					log.info("Initiating disconnection process of the internal MQTT client {}.",
							kpMqttClient.getMqttClientId());
					stop = true;
					kpMqttClient.disconnect();
				}
			}
			try {
				if (message != null) {
					message.ack();
					String messageTopic = message.getTopic();
					if (messageTopic.equals(MqttConstants.getSsapResponseMqttTopic(kpMqttClient.getMqttClientId()))) {
						if (kpMqttClient.getResponseCallback() != null) {
							kpMqttClient.getResponseCallback().handle(payload);
						}
					} else if (messageTopic
							.equals(MqttConstants.getSsapIndicationMqttTopic(kpMqttClient.getMqttClientId()))) {
						/* Notification for ssap INDICATION message */
						if (kpMqttClient.getCypheredPayloadHandler() != null)
							payload = kpMqttClient.getCypheredPayloadHandler().getDecryptedPayload(payload);
						Collection<IndicationTask> tasks = new ArrayList<IndicationTask>();
						SSAPMessage ssapMessage = SSAPMessage.fromJsonToSSAPMessage(payload);
						if (ssapMessage.getMessageType() == SSAPMessageTypes.INDICATION) {
							String messageId = ssapMessage.getMessageId();
							if (messageId != null) {
								// Notifica a los listener de las
								// suscripciones hechas manualmente
								for (Iterator<Listener4SIBIndicationNotifications> iterator = kpMqttClient
										.getSubscriptionListeners().iterator(); iterator.hasNext();) {
									Listener4SIBIndicationNotifications listener = iterator.next();
									tasks.add(new IndicationTask(listener, messageId, ssapMessage));
								}

								// Notifica a los listener de las
								// autosuscripciones
								if (messageId.equals(kpMqttClient.getBaseCommandRequestSubscriptionId())
										&& kpMqttClient.getListener4BaseCommandRequestNotifications() != null) {
									tasks.add(new IndicationTask(
											kpMqttClient.getListener4BaseCommandRequestNotifications(), messageId,
											ssapMessage));
								} else if (messageId.equals(kpMqttClient.getStatusControlRequestSubscriptionId())
										&& kpMqttClient.getListener4StatusControlRequestNotifications() != null) {
									tasks.add(new IndicationTask(
											kpMqttClient.getListener4StatusControlRequestNotifications(), messageId,
											ssapMessage));
								}
								log.debug(
										"Notifying {} SSAP INDICATION listeners of the internal MQTT client {}. Payload={}.",
										tasks.size(), kpMqttClient.getMqttClientId(), payload);
								kpMqttClient.runIndicationTasks(tasks);
							} else {
								log.warn(
										"The internal MQTT client {} received a SSAP INDICATION message whithout a messageId. "
												+ "It won't be notified to the SSAP INDICATION listeners. Payload={}.",
										kpMqttClient.getMqttClientId(), payload);
							}
						}
					}
				}
			} catch (Exception e) {
				log.error(
						"An exception was raised while the internal MQTT client {} was receiving a message from the SIB server. Cause = {}, errorMessage = {}.",
						kpMqttClient.getMqttClientId(), e.getCause(), e.getMessage());
				if (kpMqttClient.getResponseCallback() != null) {
					kpMqttClient.getResponseCallback().handle("");
				}
			}
		}
	}
}