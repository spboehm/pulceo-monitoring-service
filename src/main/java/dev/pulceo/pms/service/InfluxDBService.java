package dev.pulceo.pms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import dev.pulceo.pms.dto.metrics.NodeLinkMetricDTO;
import dev.pulceo.pms.dto.metrics.NodeMetricDTO;
import dev.pulceo.pms.model.metric.NodeLinkMetric;
import dev.pulceo.pms.model.metric.NodeMetric;
import dev.pulceo.pms.model.metricrequests.MetricRequest;
import dev.pulceo.pms.repository.NodeLinkMetricRepository;
import dev.pulceo.pms.repository.NodeMetricRepository;
import dev.pulceo.pms.util.InfluxQueryBuilder;
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

import java.util.List;
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
                    Message<?> message = mqttBlockingQueue.take();
                    if ("STOP".equals(message.getPayload())) {
                        logger.info("InfluxDBService received termination signal by poison pill...shutdown initiated");
                        return;
                    }
                    // otherwise process workload
                    String payLoadAsJson = (String) message.getPayload();
                    writeApi.writePoints(JsonToInfluxDataConverter.convertMetric(payLoadAsJson));
                    logger.info("Successfully wrote message to InfluxDB: " + payLoadAsJson);
                    // TODO: check for running metric-requests, send raw data
                    JsonNode metric = this.objectMapper.readTree(payLoadAsJson);
                    // convert from jobUUID to remoteLinkUUID
                    UUID remoteMetricRequestUUID = UUID.fromString(metric.get("metric").get("jobUUID").asText());
                    if (metricRequests.containsKey(remoteMetricRequestUUID)) {
                        MetricRequest metricRequest = metricRequests.get(remoteMetricRequestUUID);
                        // TODO: determine type of metric, transformer and send to endpoint
                        switch (metricRequest.getType()) {
                            case "cpu-util":
                                // TODO: implement here
                                String queryStringForCPUUtil = InfluxQueryBuilder.queryLastRawRecord(bucket, "CPU_UTIL", "usageCPUPercentage", metricRequest.getRemoteMetricRequestUUID().toString());
                                QueryApi queryApiForCPUUtil = influxDBClient.getQueryApi();
                                List<FluxTable> cpuUtilTables = queryApiForCPUUtil.query(queryStringForCPUUtil);
                                for (FluxTable table : cpuUtilTables) {
                                    List<FluxRecord> records = table.getRecords();
                                    for (FluxRecord record : records) {
                                        NodeMetricDTO nodeMetricDTO = NodeMetricDTO.fromFluxRecord(record, metricRequest, "%");
                                        simpMessagingTemplate.convertAndSend("/metrics/+", this.objectMapper.writeValueAsString(nodeMetricDTO));
                                        // store for archiving purposes and fast access
                                        this.nodeMetricRepository.save(NodeMetric.fromNodeLinkMetricDTO(nodeMetricDTO));
                                    }
                                }
                                break;
                            case "mem-util":
                                // TODO: implement here
                                String queryStringForMemUtil = InfluxQueryBuilder.queryLastRawRecord(bucket, "MEM_UTIL", "usageMemoryPercentage", metricRequest.getRemoteMetricRequestUUID().toString());
                                QueryApi queryApiForMEMUtil = influxDBClient.getQueryApi();
                                List<FluxTable> memUtilTables = queryApiForMEMUtil.query(queryStringForMemUtil);
                                for (FluxTable table : memUtilTables) {
                                    List<FluxRecord> records = table.getRecords();
                                    for (FluxRecord record : records) {
                                        NodeMetricDTO nodeMetricDTO = NodeMetricDTO.fromFluxRecord(record, metricRequest, "%");
                                        simpMessagingTemplate.convertAndSend("/metrics/+", this.objectMapper.writeValueAsString(nodeMetricDTO));
                                        // store for archiving purposes and fast access
                                        this.nodeMetricRepository.save(NodeMetric.fromNodeLinkMetricDTO(nodeMetricDTO));
                                    }
                                }
                                break;
                            case "storage-util":
                                String queryStringForStorageUtil = InfluxQueryBuilder.queryLastRawRecord(bucket, "STORAGE_UTIL", "usageStoragePercentage", metricRequest.getRemoteMetricRequestUUID().toString());
                                QueryApi queryApiForStorageUtil = influxDBClient.getQueryApi();
                                List<FluxTable> storageUtilTables = queryApiForStorageUtil.query(queryStringForStorageUtil);
                                for (FluxTable table : storageUtilTables) {
                                    List<FluxRecord> records = table.getRecords();
                                    for (FluxRecord record : records) {
                                        NodeMetricDTO nodeMetricDTO = NodeMetricDTO.fromFluxRecord(record, metricRequest, "%");
                                        simpMessagingTemplate.convertAndSend("/metrics/+", this.objectMapper.writeValueAsString(nodeMetricDTO));
                                        // store for archiving purposes and fast access
                                        this.nodeMetricRepository.save(NodeMetric.fromNodeLinkMetricDTO(nodeMetricDTO));
                                    }
                                }
                                break;
                            case "net-util":
                                for (String field : List.of("txBytes", "rxBytes")) {
                                    String queryStringForNetUtilTx = InfluxQueryBuilder.queryLastRawRecord(bucket, "NET_UTIL", field, metricRequest.getRemoteMetricRequestUUID().toString());
                                    QueryApi queryApiForNetUtilTx = influxDBClient.getQueryApi();
                                    List<FluxTable> netUtilTxTables = queryApiForNetUtilTx.query(queryStringForNetUtilTx);
                                    for (FluxTable table : netUtilTxTables) {
                                        List<FluxRecord> records = table.getRecords();
                                        for (FluxRecord record : records) {
                                            NodeMetricDTO nodeMetricDTO = NodeMetricDTO.fromFluxRecord(record, metricRequest, "bytes");
                                            nodeMetricDTO.setMetricType("NET_UTIL_" + field.toUpperCase());
                                            simpMessagingTemplate.convertAndSend("/metrics/+", this.objectMapper.writeValueAsString(nodeMetricDTO));
                                            // store for archiving purposes and fast access
                                            this.nodeMetricRepository.save(NodeMetric.fromNodeLinkMetricDTO(nodeMetricDTO));
                                        }
                                    }
                                }
                                break;
                            case "icmp-rtt":
                                String queryString = InfluxQueryBuilder.queryLastRawRecord(bucket, "ICMP_RTT", "rttAvg", metricRequest.getRemoteMetricRequestUUID().toString());
                                QueryApi queryApi = influxDBClient.getQueryApi();
                                List<FluxTable> tables = queryApi.query(queryString);
                                for (FluxTable table : tables) {
                                    List<FluxRecord> records = table.getRecords();
                                    for (FluxRecord record : records) {
                                        NodeLinkMetricDTO nodeLinkMetricDTO = NodeLinkMetricDTO.fromFluxRecord(record, metricRequest, "ms");
                                        simpMessagingTemplate.convertAndSend("/metrics/+", this.objectMapper.writeValueAsString(nodeLinkMetricDTO));
                                        // store for archiving purposes and fast access
                                        this.nodeLinkMetricRepository.save(NodeLinkMetric.fromNodeLinkMetricDTO(nodeLinkMetricDTO));
                                    }
                                }
                                break;
                            case "tcp-rtt":
                                String queryStringTcpRtt = InfluxQueryBuilder.queryLastRawRecord(bucket, "TCP_RTT", "avgRTT", metricRequest.getRemoteMetricRequestUUID().toString());
                                QueryApi queryApiTcpRtt = influxDBClient.getQueryApi();
                                List<FluxTable> tablesTcpRTT = queryApiTcpRtt.query(queryStringTcpRtt);
                                for (FluxTable table : tablesTcpRTT) {
                                    List<FluxRecord> records = table.getRecords();
                                    for (FluxRecord record : records) {
                                        NodeLinkMetricDTO nodeLinkMetricDTO = NodeLinkMetricDTO.fromFluxRecord(record, metricRequest, "ms");
                                        simpMessagingTemplate.convertAndSend("/metrics/+", this.objectMapper.writeValueAsString(nodeLinkMetricDTO));
                                        // store for archiving purposes and fast access
                                        this.nodeLinkMetricRepository.save(NodeLinkMetric.fromNodeLinkMetricDTO(nodeLinkMetricDTO));
                                    }
                                }
                                // TODO: implement this
                                break;
                            case "udp-rtt":
                                String queryStringUdpRtt = InfluxQueryBuilder.queryLastRawRecord(bucket, "UDP_RTT", "avgRTT", metricRequest.getRemoteMetricRequestUUID().toString());
                                QueryApi queryApiUdpRtt = influxDBClient.getQueryApi();
                                List<FluxTable> tablesUdpRTT = queryApiUdpRtt.query(queryStringUdpRtt);
                                for (FluxTable table : tablesUdpRTT) {
                                    List<FluxRecord> records = table.getRecords();
                                    for (FluxRecord record : records) {
                                        NodeLinkMetricDTO nodeLinkMetricDTO = NodeLinkMetricDTO.fromFluxRecord(record, metricRequest, "ms");
                                        simpMessagingTemplate.convertAndSend("/metrics/+", this.objectMapper.writeValueAsString(nodeLinkMetricDTO));
                                        // store for archiving purposes and fast access
                                        this.nodeLinkMetricRepository.save(NodeLinkMetric.fromNodeLinkMetricDTO(nodeLinkMetricDTO));
                                    }
                                }
                                break;
                            case "tcp-bw":
                                String queryStringTcpBw = InfluxQueryBuilder.queryLastRawRecord(bucket, "TCP_BW", "bitrate", metricRequest.getRemoteMetricRequestUUID().toString());
                                QueryApi queryApiTcpBw = influxDBClient.getQueryApi();
                                List<FluxTable> tablesTcpBw = queryApiTcpBw.query(queryStringTcpBw);
                                for (FluxTable table : tablesTcpBw) {
                                    List<FluxRecord> records = table.getRecords();
                                    for (FluxRecord record : records) {
                                        NodeLinkMetricDTO nodeLinkMetricDTO = NodeLinkMetricDTO.fromFluxRecord(record, metricRequest, "Mbit/s");
                                        simpMessagingTemplate.convertAndSend("/metrics/+", this.objectMapper.writeValueAsString(nodeLinkMetricDTO));
                                        // store for archiving purposes and fast access
                                        this.nodeLinkMetricRepository.save(NodeLinkMetric.fromNodeLinkMetricDTO(nodeLinkMetricDTO));
                                    }
                                }
                                break;
                            case "udp-bw":
                                String queryStringUdpBw = InfluxQueryBuilder.queryLastRawRecord(bucket, "TCP_BW", "bitrate", metricRequest.getRemoteMetricRequestUUID().toString());
                                QueryApi queryApiUdpBw = influxDBClient.getQueryApi();
                                List<FluxTable> tablesUdpBw = queryApiUdpBw.query(queryStringUdpBw);
                                for (FluxTable table : tablesUdpBw) {
                                    List<FluxRecord> records = table.getRecords();
                                    for (FluxRecord record : records) {
                                        NodeLinkMetricDTO nodeLinkMetricDTO = NodeLinkMetricDTO.fromFluxRecord(record, metricRequest, "Mbit/s");
                                        simpMessagingTemplate.convertAndSend("/metrics/+", this.objectMapper.writeValueAsString(nodeLinkMetricDTO));
                                        // store for archiving purposes and fast access
                                        this.nodeLinkMetricRepository.save(NodeLinkMetric.fromNodeLinkMetricDTO(nodeLinkMetricDTO));
                                    }
                                }
                                break;
                            default:
                                logger.error("Unknown metric type: " + metricRequest.getType());
                                break;
                        }
                    } else {
                        logger.error("Unkown metric request UUID: " + remoteMetricRequestUUID);
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
