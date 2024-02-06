package dev.pulceo.pms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import dev.pulceo.pms.dto.metrics.NodeLinkMetricDTO;
import dev.pulceo.pms.model.metricrequests.MetricRequest;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class InfluxDBService {

    private final Logger logger = LoggerFactory.getLogger(InfluxDBService.class);

    @Value("${influxdb.token}")
    private String token;

    @Value("${influxdb.org}")
    private String org;

    @Value("${influxdb.bucket}")
    private String bucket;

    @Value("${influxdb.url}")
    private String influxDBUrl;

    private final BlockingQueue<Message<?>> mqttBlockingQueue;

    private boolean isRunning = true;

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    private final SimpMessagingTemplate simpMessagingTemplate;

    private final ConcurrentHashMap<UUID, MetricRequest> metricRequests = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public InfluxDBService(BlockingQueue<Message<?>> mqttBlockingQueue, ThreadPoolTaskExecutor threadPoolTaskExecutor, SimpMessagingTemplate simpMessagingTemplate) {
        this.mqttBlockingQueue = mqttBlockingQueue;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @PostConstruct
    private void postConstruct() {
        threadPoolTaskExecutor.execute(this::init);
    }

    @PreDestroy
    private void preDestroy() throws InterruptedException {
        isRunning = false;
        this.mqttBlockingQueue.put(new GenericMessage<>("STOP"));
        threadPoolTaskExecutor.shutdown();
    }

    private void init() {
        try(InfluxDBClient influxDBClient = InfluxDBClientFactory.create(influxDBUrl, token.toCharArray(), org, bucket)) {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            while(isRunning) {
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
                        case "icmp-rtt":
                            String queryString = InfluxQueryBuilder.queryLastRawRecord(bucket, "ICMP_RTT", "rttAvg", metricRequest.getRemoteMetricRequestUUID().toString());
                            QueryApi queryApi = influxDBClient.getQueryApi();
                            List<FluxTable> tables = queryApi.query(queryString);
                            for (FluxTable table : tables) {
                                List<FluxRecord> records = table.getRecords();
                                for (FluxRecord record : records) {
                                    simpMessagingTemplate.convertAndSend("/metrics/+", this.objectMapper.writeValueAsString(NodeLinkMetricDTO.fromFluxRecord(record, metricRequest)));
                                    // TODO: store back to influxDB for achiving purposes

                                    // TODO: store for fast access in a cache stat it can be retrieved by the frontend
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
            }
        } catch (InterruptedException e) {
            logger.info("InfluxDBService received termination signal...shutdown initiated");
            Thread.currentThread().interrupt();
        } catch (JsonProcessingException e) {
            logger.error("Could not convert message to InfluxDB point: " + e.getMessage());
        }
    }

    private void queryData(String query) {
        try(InfluxDBClient influxDBClient = InfluxDBClientFactory.create(influxDBUrl, token.toCharArray(), org, bucket)) {
            QueryApi queryApi = influxDBClient.getQueryApi();

            List<FluxTable> tables = queryApi.query(InfluxQueryBuilder.queryLastRawRecord(bucket, "ICMP_RTT", "rttAvg", "54d9ce34-46af-49af-b612-6164270d7a24"));
            for (FluxTable table : tables) {
                List<FluxRecord> records = table.getRecords();
                for (FluxRecord record : records) {
                    logger.info("Record: " + record.getValues());
                }
            }
        }
    }

    public void notifyAboutNewMetricRequest(MetricRequest metricRequest) {
        System.out.println("added " + metricRequest.getRemoteMetricRequestUUID());
        this.metricRequests.put(metricRequest.getRemoteMetricRequestUUID(), metricRequest);
    }
}
