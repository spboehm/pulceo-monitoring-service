package dev.pulceo.pms.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Service
public class InfluxDBService {

    @Value("${influxdb.token}")
    private String token;

    @Value("${influxdb.org}")
    private String org;

    @Value("${influxdb.bucket}")
    private String bucket;

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
        threadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        threadPoolTaskExecutor.setAwaitTerminationSeconds(30);
    }

    @PreDestroy
    public void preDestroy() {
        isRunning = false;
        threadPoolTaskExecutor.shutdown();
    }

    public void init() {
        try(InfluxDBClient influxDBClient = InfluxDBClientFactory.create("http://localhost:8086", token.toCharArray(), org, bucket)) {
            //
            // Write data
            //
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            while(isRunning) {
                System.out.println(Thread.currentThread().getName() + ":" + Thread.currentThread().getId());
                Message<?> message = mqttBlockingQueue.take();
                //
                // Write by Data Point
                //
                Point point = Point.measurement("temperature")
                        .addTag("location", "west")
                        .addField("value", 55D)
                        .time(Instant.now().toEpochMilli(), WritePrecision.MS);

                writeApi.writePoint(point);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
