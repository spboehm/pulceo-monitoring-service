package dev.pulceo.pms.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import dev.pulceo.pms.dto.metrics.ShortNodeLinkMetricDTO;
import dev.pulceo.pms.exception.MetricsQueryServiceException;
import dev.pulceo.pms.model.metric.MetricType;
import dev.pulceo.pms.model.metricexports.MetricExport;
import dev.pulceo.pms.model.metricexports.MetricExportRequest;
import dev.pulceo.pms.model.metricexports.MetricExportState;
import dev.pulceo.pms.repository.MetricExportRepository;
import dev.pulceo.pms.util.InfluxQueryBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetricsQueryService {

    private final Logger logger = LoggerFactory.getLogger(MetricsQueryService.class);

    @Value("${influxdb.token}")
    private String token;

    @Value("${influxdb.org}")
    private String org;

    @Value("${influxdb.bucket}")
    private String bucket;

    @Value("${influxdb.url}")
    private String influxDBUrl;

    @Value("${influxdb.read.timeout}")
    private String readTimeout;

    @Value("${pms.data.dir}")
    private String pmsDatDir;

    @Value("${pulceo.lb.endpoint}")
    private String pulceoLBEndpoint;

    private InfluxDBClient influxDBClient;

    private final AtomicBoolean atomicBoolean = new AtomicBoolean(true);

    private final BlockingQueue<Long> metricExportQueue = new LinkedBlockingQueue<>();

    private final MetricExportRepository metricExportRepository;

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Autowired
    public MetricsQueryService(MetricExportRepository metricExportRepository, ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        this.metricExportRepository = metricExportRepository;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
    }

    @PostConstruct
    private void postConstruct() {
        this.influxDBClient = InfluxDBClientFactory.create(influxDBUrl + "?readTimeout=" + readTimeout, token.toCharArray(), org);
        this.threadPoolTaskExecutor.submit(this::waitForMetricExports);
    }

    @PreDestroy
    private void preDestroy() throws InterruptedException {
        this.atomicBoolean.set(false);
        this.metricExportQueue.put(-1L);
        this.threadPoolTaskExecutor.shutdown();
        this.influxDBClient.close();
    }

    private void waitForMetricExports() {
        while (atomicBoolean.get()) {
            long metricExportId = -1;
            try {
                logger.info("MetricsQueryService is listening for metric exports requests...");
                metricExportId = metricExportQueue.take();
                logger.info("MetricsQueryService received metric export request with id {}", metricExportId);
                if (metricExportId == -1) {
                    logger.info("MetricsQueryService received termination signal by poison pill...shutdown initiated");
                    return;
                }
                MetricExport metricExport = metricExportRepository.findById(metricExportId).orElseThrow();
                metricExport.setMetricExportState(MetricExportState.RUNNING);
                this.getMeasurementAsCSV(metricExport.getMetricType(), metricExport.getFilename());
                metricExport.setMetricExportState(MetricExportState.COMPLETED);
            } catch (InterruptedException e) {
                logger.info("MetricsQueryService was interrupted while waiting for metric exports");
                this.atomicBoolean.set(false);
                this.markMetricExportAsFailed(metricExportId);
            } catch (MetricsQueryServiceException e) {
                logger.error("Could not get measurements as CSV", e);
                this.markMetricExportAsFailed(metricExportId);
            } catch (IOException e) {
                logger.error("Could not write measurements as CSV", e);
                this.markMetricExportAsFailed(metricExportId);
            } catch (NoSuchElementException e) {
                logger.error("Could not find metric export request", e);
            }
        }
        logger.info("MetricsQueryService successfully stopped!");
    }

    private void markMetricExportAsFailed(long metricExportId) {
        MetricExport metricExport = this.metricExportRepository.findById(metricExportId).orElseThrow();
        metricExport.setMetricExportState(MetricExportState.FAILED);
        this.metricExportRepository.save(metricExport);
    }

    public List<ShortNodeLinkMetricDTO> queryRangeNodeLinkMetrics(String metricType, String aggregation) throws MetricsQueryServiceException {
        QueryApi queryApi = influxDBClient.getQueryApi();
        String influxQuery;
        String unit;
        switch (metricType) {
            case "ICMP_RTT":
                influxQuery = InfluxQueryBuilder.queryNodeLinkRttMetricWithAggregation(bucket, metricType, "rttAvg", aggregation);
                unit = "ms";
                break;
            case "TCP_BW":
                influxQuery = InfluxQueryBuilder.queryNodeLinkBwMetricWithAggregation(bucket, metricType, "bitrate", "SENDER", aggregation);
                unit = "Mbit/s";
                break;
            case "UDP_BW":
                influxQuery = InfluxQueryBuilder.queryNodeLinkBwMetricWithAggregation(bucket, metricType, "bitrate", "RECEIVER", aggregation);
                unit = "Mbit/s";
                break;
            default:
                throw new MetricsQueryServiceException("Invalid metric type");
        }
        List<FluxTable> nodeLinkMetricsTables = queryApi.query(influxQuery);
        List<ShortNodeLinkMetricDTO> shortNodeLinkMetricDTOs = new ArrayList<>();
        for (FluxTable table : nodeLinkMetricsTables) {
            List<FluxRecord> records = table.getRecords();
            for (FluxRecord record : records) {
                shortNodeLinkMetricDTOs.add(ShortNodeLinkMetricDTO.fromFluxRecord(record, unit));
            }
        }
        return shortNodeLinkMetricDTOs;
    }

    public MetricExport createMetricExport(MetricExportRequest metricExportRequest) throws MetricsQueryServiceException {
        long numberOfRecords = this.getNumberOfRecords(metricExportRequest.getMetricType());
        if (numberOfRecords == 0) {
            throw new MetricsQueryServiceException("No records found for metric type %s".formatted(metricExportRequest.getMetricType()));
        }
        String filename = metricExportRequest.getMetricType() + "-" + UUID.randomUUID() + ".csv";
        MetricExport metricExport = MetricExport.builder()
                .metricType(metricExportRequest.getMetricType())
                .numberOfRecords(numberOfRecords)
                .build();
        metricExport.setUrl(pulceoLBEndpoint + "/api/v1/metric-exports/" + metricExport.getUuid() + "/blobs/" + filename);
        MetricExport savedMetricExport = this.metricExportRepository.save(metricExport);
        try {
            this.metricExportQueue.put(savedMetricExport.getId());
        } catch (InterruptedException e) {
            logger.error("Could not put metric export in queue", e);
            throw new MetricsQueryServiceException("Could not put metric export in queue!", e);
        }
        return savedMetricExport;
    }

    public List<MetricExport> readAllMetricExports() {
        List<MetricExport> metricExportList = new ArrayList<>();
        this.metricExportRepository.findAll().forEach(metricExportList::add);
        return metricExportList;
    }

    private void getMeasurementAsCSV(MetricType measurement, String filename) throws InterruptedException, IOException, MetricsQueryServiceException {
        QueryApi queryApi = influxDBClient.getQueryApi();
        String influxQuery = InfluxQueryBuilder.queryCPUUtil(bucket);
        long numberOfMeasurements = this.getNumberOfRecords(measurement);
        CountDownLatch countDownLatch = new CountDownLatch(1); // influxdb thread
        AtomicLong count = new AtomicLong(0);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.pmsDatDir + "/" + filename, true))) {
            queryApi.queryRaw(influxQuery, (cancellable, line) -> {
                try {
                    writer.write(line);
                    writer.newLine();
                    if (count.incrementAndGet() == numberOfMeasurements) {
                        countDownLatch.countDown();
                        cancellable.cancel();
                    }
                } catch (IOException e) {
                    logger.error("Could not write {}!", filename, e);
                }
            });
            countDownLatch.await(); // wait for influxdb thread to finish
            logger.info("{} successfully written", filename);
        }
    }

    private long getNumberOfRecords(MetricType measurement) throws MetricsQueryServiceException {
        QueryApi queryApi = influxDBClient.getQueryApi();
        String influxQuery = InfluxQueryBuilder.queryMeasurementCount(bucket, measurement.toString());
        List<FluxTable> tables = queryApi.query(influxQuery);
        for (FluxTable table : tables) {
            List<FluxRecord> records = table.getRecords();
            for (FluxRecord record : records) {
                logger.info("Measurement {} count: {}", measurement, record.getValueByKey("_value"));
                try {
                    return Long.parseLong(Objects.requireNonNull(record.getValueByKey("_value")).toString());
                } catch (NumberFormatException e) {
                    throw new MetricsQueryServiceException("Could not parse measurement count", e);
                }
            }
        }
        throw new MetricsQueryServiceException("Measurement %s not found!".formatted(measurement));
    }
}
