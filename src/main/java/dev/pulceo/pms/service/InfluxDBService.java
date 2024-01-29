package dev.pulceo.pms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import dev.pulceo.pms.util.JsonToInfluxDataConverter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.BlockingQueue;
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

    @Autowired
    public InfluxDBService(BlockingQueue<Message<?>> mqttBlockingQueue, ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        this.mqttBlockingQueue = mqttBlockingQueue;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
    }

    @PostConstruct
    public void postConstruct() {
        threadPoolTaskExecutor.execute(this::init);
    }

    @PreDestroy
    public void preDestroy() throws InterruptedException {
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
            }
        } catch (InterruptedException e) {
            logger.info("InfluxDBService received termination signal...shutdown initiated");
            Thread.currentThread().interrupt();
        } catch (JsonProcessingException e) {
            logger.error("Could not convert message to InfluxDB point: " + e.getMessage());
        }
    }
}
