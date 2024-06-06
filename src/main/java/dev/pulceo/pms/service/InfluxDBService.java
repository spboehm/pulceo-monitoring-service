package dev.pulceo.pms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import dev.pulceo.pms.dto.metrics.NodeMetricDTO;
import dev.pulceo.pms.model.metricrequests.MetricRequest;
import dev.pulceo.pms.repository.NodeLinkMetricRepository;
import dev.pulceo.pms.repository.NodeMetricRepository;
import dev.pulceo.pms.util.JsonToInfluxDataConverter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class InfluxDBService {

    private final Logger logger = LoggerFactory.getLogger(InfluxDBService.class);

    @Value("${orchestration.id}")
    private String orchestrationId;

    @Value("${orchestration.name}")
    private String orchestrationName;

    @Value("${influxdb.token}")
    private String token;

    @Value("${influxdb.org}")
    private String org;

    @Value("${influxdb.bucket}")
    private String bucket;

    @Value("${influxdb.url}")
    private String influxDBUrl;

    private final BlockingQueue<Message<?>> mqttBlockingQueue;

    private final BlockingQueue<Message<?>> mqttBlockingQueueEvent;

    private final BlockingQueue<Message<?>> mqttBlockingQueueRequest;

    private final AtomicBoolean atomicBoolean = new AtomicBoolean(true);

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    private final SimpMessagingTemplate simpMessagingTemplate;

    private final SimpMessageHandler simpMessageHandler;

    private final ConcurrentHashMap<UUID, MetricRequest> metricRequests = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final NodeMetricRepository nodeMetricRepository;

    private final NodeLinkMetricRepository nodeLinkMetricRepository;

    @Autowired
    public InfluxDBService(BlockingQueue<Message<?>> mqttBlockingQueue,
                           ThreadPoolTaskExecutor threadPoolTaskExecutor,
                           SimpMessagingTemplate simpMessagingTemplate,
                           NodeLinkMetricRepository nodeLinkMetricRepository,
                           NodeMetricRepository nodeMetricRepository,
                           BlockingQueue<Message<?>> mqttBlockingQueueEvent,
                           BlockingQueue<Message<?>> mqttBlockingQueueRequest,
                           SimpMessageHandler simpMessageHandler) {
        this.mqttBlockingQueue = mqttBlockingQueue;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.nodeLinkMetricRepository = nodeLinkMetricRepository;
        this.nodeMetricRepository = nodeMetricRepository;
        this.mqttBlockingQueueEvent = mqttBlockingQueueEvent;
        this.mqttBlockingQueueRequest = mqttBlockingQueueRequest;
        this.simpMessageHandler = simpMessageHandler;
    }

    @PostConstruct
    private void postConstruct() {
        threadPoolTaskExecutor.submit(this::listenForMetrics);
        threadPoolTaskExecutor.submit(this::listenForEvents);
        threadPoolTaskExecutor.submit(this::listenForRequests);
    }

    @PreDestroy
    private void preDestroy() throws InterruptedException {
        this.atomicBoolean.set(false);
        this.mqttBlockingQueue.put(new GenericMessage<>("STOP"));
        this.mqttBlockingQueueEvent.put(new GenericMessage<>("STOP"));
        this.mqttBlockingQueueRequest.put(new GenericMessage<>("STOP"));
        threadPoolTaskExecutor.shutdown();
    }

    private void listenForRequests() {
        try(InfluxDBClient influxDBClient = InfluxDBClientFactory.create(influxDBUrl, token.toCharArray(), org, bucket)) {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            while (atomicBoolean.get()) {
                try {
                    logger.info("InfluxDBService is listening for requests...");
                    Message<?> message = mqttBlockingQueueRequest.take();
                    if ("STOP".equals(message.getPayload())) {
                        logger.info("InfluxDBService received termination signal by poison pill...shutdown initiated");
                        return;
                    }
                    // otherwise process workload
                    String payLoadAsJson = (String) message.getPayload();
                    JsonNode jsonNode = this.objectMapper.readTree(payLoadAsJson);
                    writeApi.writePoints(JsonToInfluxDataConverter.convertRequest(jsonNode.toString()));
                    logger.info("Successfully wrote request to InfluxDB: " + payLoadAsJson);
                } catch (InterruptedException e) {
                    logger.info("InfluxDBService received termination signal...shutdown initiated");
                    this.atomicBoolean.set(false);
                } catch (JsonProcessingException e) {
                    logger.error("Could not convert message to InfluxDB point: " + e.getMessage());
                } catch (Exception e) {
                    logger.error("An error occurred while processing event: " + e.getMessage());
                }
            }
        }
    }

    private void listenForEvents() {
        try(InfluxDBClient influxDBClient = InfluxDBClientFactory.create(influxDBUrl, token.toCharArray(), org, bucket)) {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            while (atomicBoolean.get()) {
                try {
                    logger.info("InfluxDBService is listening for events...");
                    Message<?> message = mqttBlockingQueueEvent.take();
                    if ("STOP".equals(message.getPayload())) {
                        logger.info("InfluxDBService received termination signal by poison pill...shutdown initiated");
                        return;
                    }
                    // otherwise process workload
                    String payLoadAsJson = (String) message.getPayload();
                    JsonNode jsonNode = this.objectMapper.readTree(payLoadAsJson);
                    writeApi.writePoints(JsonToInfluxDataConverter.convertEvent(jsonNode.toString()));
                    logger.info("Successfully wrote event to InfluxDB: " + payLoadAsJson);
                } catch (InterruptedException e) {
                    this.atomicBoolean.set(false);
                } catch (JsonProcessingException e) {
                    logger.error("Could not convert message to InfluxDB point: " + e.getMessage());
                } catch (Exception e) {
                    logger.error("An error occurred while processing event: " + e.getMessage());
                }
            }
        }
    }

    private void listenForMetrics() {
        try(InfluxDBClient influxDBClient = InfluxDBClientFactory.create(influxDBUrl, token.toCharArray(), org, bucket)) {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            while (atomicBoolean.get()) {
                try {
                    logger.info("InfluxDBService is listening for metrics...");
                    //
                    int number = 0;
                    while (true) {
                        NodeMetricDTO nodeMetricDTOtest = NodeMetricDTO.builder()
                                .orchestrationId(this.orchestrationId)
                                .orchestrationName(this.orchestrationName)
                                .metricUUID(UUID.randomUUID())
                                .metricType("CPU_UTIL")
                                .metricRequestUUID(UUID.randomUUID().toString())
                                .nodeUUID("e95c1183-58dd-482f-a07f-e4e5ac30657c")
                                .nodeName("fog1")
                                .time("2024-05-24T11:39:53Z")
                                .val(number)
                                .unit("%")
                                .build();
                        NodeMetricDTO nodeMetricDTOtest2 = NodeMetricDTO.builder()
                                .orchestrationId(this.orchestrationId)
                                .orchestrationName(this.orchestrationName)
                                .metricUUID(UUID.randomUUID())
                                .metricType("CPU_UTIL")
                                .metricRequestUUID(UUID.randomUUID().toString())
                                .nodeUUID("f326d5bd-65f5-4bff-a099-7aeb8a546000")
                                .nodeName("fog2")
                                .time("2024-05-24T11:39:53Z")
                                .val(number)
                                .unit("%")
                                .build();
                        NodeMetricDTO nodeMetricDTOtest3 = NodeMetricDTO.builder()
                                .orchestrationId(this.orchestrationId)
                                .orchestrationName(this.orchestrationName)
                                .metricUUID(UUID.randomUUID())
                                .metricType("CPU_UTIL")
                                .metricRequestUUID(UUID.randomUUID().toString())
                                .nodeUUID("g326d5bd-65f5-4bff-a099-7aeb8a546000")
                                .nodeName("fog3")
                                .time("2024-05-24T11:39:53Z")
                                .val(number)
                                .unit("%")
                                .build();
                        this.simpMessageHandler.send(nodeMetricDTOtest);
                        this.simpMessageHandler.send(nodeMetricDTOtest2);
                        this.simpMessageHandler.send(nodeMetricDTOtest3);
                        Thread.sleep(5000);
                        number++;
                    }

                } catch (InterruptedException e) {
                    logger.info("InfluxDBService received termination signal...shutdown initiated");
                    this.atomicBoolean.set(false);
                } catch (JsonProcessingException e) {
                    logger.error("Could not convert message to InfluxDB point: " + e.getMessage());
                } catch (Exception e) {
                    logger.error("An error occurred while processing metric: " + e.getMessage());
                }
            }
        }
    }

    private String resolveDeviceIdByPNAUUID(String pnaUUID) {
        logger.debug("Resolving device ID by PNA UUID: " + pnaUUID);
        return "fog1";
    }

    public void notifyAboutNewMetricRequest(MetricRequest metricRequest) {
        logger.info("Notified about a new metric request: " + metricRequest.getUuid());
        this.metricRequests.put(metricRequest.getRemoteMetricRequestUUID(), metricRequest);
    }
}
