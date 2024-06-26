package dev.pulceo.pms.config;

import dev.pulceo.pms.model.event.PulceoEvent;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
public class MQTTConfig {

    @Value("${pna.mqtt.broker.url}")
    private String mqttBrokerURL;
    @Value("${pna.mqtt.client.username}")
    private String mqttBrokerUsername;
    @Value("${pna.mqtt.client.password}")
    private String mqttBrokerPassword;

    /* Outbound */
    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new PublishSubscribeChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler =
                new MqttPahoMessageHandler(UUID.randomUUID().toString(), mqttClientFactory());
        messageHandler.setAsync(true);
        messageHandler.setDefaultTopic("dt/pulceo/events");
        messageHandler.setConverter(new DefaultPahoMessageConverter());
        return messageHandler;
    }

    /* Inbound */
    @Bean
    public BlockingQueue<Message<?>> mqttBlockingQueue() {
        return new LinkedBlockingQueue<>();
    }

    @Bean
    public BlockingQueue<Message<?>> mqttBlockingQueueEvent() { return new LinkedBlockingQueue<>(); }

    @Bean
    public BlockingQueue<Message<?>> mqttBlockingQueueRequest() { return new LinkedBlockingQueue<>(); }

    /* Inbound */
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttInputChannelForEvent() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttInputChannelForRequest() {
        return new DirectChannel();
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[] {mqttBrokerURL});
        options.setUserName(mqttBrokerUsername);
        options.setPassword(mqttBrokerPassword.toCharArray());
        options.setAutomaticReconnect(true);
        options.setSSLProperties(new Properties());
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageProducer inbound() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(UUID.randomUUID().toString(), mqttClientFactory(),
                        "dt/+/metrics");
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.connectComplete(true);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    @Bean
    public MessageProducer inboundEvent() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(UUID.randomUUID().toString(), mqttClientFactory(),
                        "dt/pulceo/events");
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.connectComplete(true);
        adapter.setOutputChannel(mqttInputChannelForEvent());
        return adapter;
    }

    @Bean
    public MessageProducer inboundRequest() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(UUID.randomUUID().toString(), mqttClientFactory(),
                        "dt/pulceo/requests");
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.connectComplete(true);
        adapter.setOutputChannel(mqttInputChannelForRequest());
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler handler() {
        return message -> {
            try {
                mqttBlockingQueue().put(message);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannelForEvent")
    public MessageHandler handlerForEvent() {
        return message -> {
            try {
                mqttBlockingQueueEvent().put(message);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannelForRequest")
    public MessageHandler handlerForRequest() {
        return message -> {
            try {
                mqttBlockingQueueRequest().put(message);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
    }

}
