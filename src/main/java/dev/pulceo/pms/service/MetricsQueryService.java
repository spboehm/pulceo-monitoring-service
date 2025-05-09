package dev.pulceo.pms.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import dev.pulceo.pms.api.PsmApi;
import dev.pulceo.pms.api.dto.orchestration.OrchestrationContextFromPsmDTO;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

    private final PsmApi psmApi;

    @Value("${pulceo.data.dir}")
    private String pulceoDataDir;

    @Autowired
    public MetricsQueryService(MetricExportRepository metricExportRepository, ThreadPoolTaskExecutor threadPoolTaskExecutor, PsmApi psmApi) {
        this.metricExportRepository = metricExportRepository;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.psmApi = psmApi;
    }

    @PostConstruct
    private void postConstruct() throws MetricsQueryServiceException {
        this.influxDBClient = InfluxDBClientFactory.create(influxDBUrl + "?readTimeout=" + readTimeout, token.toCharArray(), org);
        createPmsDataDirIfNotExists();
        this.threadPoolTaskExecutor.submit(this::waitForMetricExportRequests);
    }

    @PreDestroy
    private void preDestroy() throws InterruptedException {
        this.logger.info("MetricsQueryService is shutting down...");
        this.atomicBoolean.set(false);
        this.metricExportQueue.put(-1L);
        this.threadPoolTaskExecutor.shutdown();
        this.influxDBClient.close();
    }

    // TODO: refactor to another class
    private void waitForMetricExportRequests() {
        while (atomicBoolean.get()) {
            long metricExportId = -1L;
            try {
                logger.info("MetricsQueryService is listening for metric exports requests...");
                metricExportId = metricExportQueue.take();
                logger.info("MetricsQueryService received metric export request with id {}", metricExportId);
                if (metricExportId == -1L) {
                    logger.info("MetricsQueryService received termination signal by poison pill...shutdown initiated");
                    return;
                }
                // TODO: remove redundant queries
                MetricExport pendingMetricExport = metricExportRepository.findById(metricExportId).orElseThrow();
                pendingMetricExport.setMetricExportState(MetricExportState.RUNNING);
                this.metricExportRepository.save(pendingMetricExport);

                this.getMeasurementAsCSV(pendingMetricExport.getMetricType(), pendingMetricExport.getFilename());

                MetricExport runningMetricRequest = metricExportRepository.findById(metricExportId).orElseThrow();
                runningMetricRequest.setMetricExportState(MetricExportState.COMPLETED);
                this.metricExportRepository.save(runningMetricRequest);
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
        logger.info("Number of records for metric type {} is {}", metricExportRequest.getMetricType(), numberOfRecords);
        if (numberOfRecords == 0) {
            this.logger.warn("No records found for metric type %s".formatted(metricExportRequest.getMetricType()));
        }
        String filename = metricExportRequest.getMetricType() + "-" + UUID.randomUUID() + ".csv";
        MetricExport metricExport = MetricExport.builder()
                .metricType(metricExportRequest.getMetricType())
                .numberOfRecords(numberOfRecords)
                .filename(filename)
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

    public Optional<MetricExport> readMetricExportByUuid(UUID metricExportUUID) {
        return this.metricExportRepository.findByUuid(metricExportUUID);
    }

    private void getMeasurementAsCSV(MetricType measurement, String filename) throws InterruptedException, IOException, MetricsQueryServiceException {
        this.logger.info("Getting measurement metricType={} as CSV", measurement);
        QueryApi queryApi = influxDBClient.getQueryApi();
        String influxQuery = resolveInfluxQuery(measurement);
        long numberOfMeasurements = this.getNumberOfRecords(measurement);
        CountDownLatch countDownLatch = new CountDownLatch(1); // influxdb thread
        AtomicLong count = new AtomicLong(0);
        // pms data dir created during initialization
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.pmsDatDir + "/" + filename, true))) {
            queryApi.queryRaw(influxQuery, (cancellable, line) -> {
                try {
                    writer.write(line);
                    writer.newLine();
                    if (count.getAndIncrement() == numberOfMeasurements) {
                        countDownLatch.countDown();
                        cancellable.cancel();
                    }
                } catch (IOException e) {
                    logger.error("Could not write {}!", filename, e);
                }
            });
            countDownLatch.await(); // wait for influxdb thread to finish
            logger.info("{} successfully written", filename);
        } catch (IOException e) {
            logger.error("Could not write {}!", filename, e);
            throw new MetricsQueryServiceException("Could not write %s!".formatted(filename), e);
        } catch (InterruptedException e) {
            logger.error("Could not wait for influxdb thread to finish", e);
            throw new MetricsQueryServiceException("Could not wait for influxdb thread to finish", e);
        }

        /* Save to pulceo data dir */
        // get the current orchestration-context, maybe remove this, because already set?
        OrchestrationContextFromPsmDTO orchestrationContextFromPsmDTO = this.psmApi.getOrchestrationContext();
        // ensure directories are created
        this.createDirsForOrchestrationData(UUID.fromString(orchestrationContextFromPsmDTO.getUuid()));
        // write to the file
        Files.copy(Path.of(this.pmsDatDir, filename), Path.of(this.pulceoDataDir, "raw", orchestrationContextFromPsmDTO.getUuid(), measurement + ".csv"), StandardCopyOption.REPLACE_EXISTING);
    }

    private String resolveInfluxQuery(MetricType measurement) {
        if (measurement == MetricType.EVENT) {
            return InfluxQueryBuilder.queryEvents(bucket);
        } else {
            return InfluxQueryBuilder.queryUtilMetrics(bucket, measurement.toString());
        }
    }

    private void createPmsDataDirIfNotExists() throws MetricsQueryServiceException {
        try {
            Files.createDirectories(Path.of(this.pmsDatDir));
        } catch (IOException e) {
            logger.error("Could not create PMS data directory", e);
            throw new MetricsQueryServiceException("Could not create PMS data directory", e);
        }
    }

    private void createDirsForOrchestrationData(UUID orchestrationUUID) {
        logger.info("Creating directories for orchestration data with uuid={}", orchestrationUUID);
        try {
            Files.createDirectories(Path.of(this.pulceoDataDir, "raw", orchestrationUUID.toString()));
            Files.createDirectories(Path.of(this.pulceoDataDir, "plots", orchestrationUUID.toString()));
            Files.createDirectories(Path.of(this.pulceoDataDir, "latex", orchestrationUUID.toString()));
            Files.createDirectories(Path.of(this.pulceoDataDir, "reports", orchestrationUUID.toString()));
        } catch (IOException e) {
            logger.error("Could not create directories for orchestration data", e);
        }
    }

    private long getNumberOfRecords(MetricType measurement) throws MetricsQueryServiceException {
        QueryApi queryApi = influxDBClient.getQueryApi();
        String influxQuery = InfluxQueryBuilder.queryMeasurementCount(bucket, measurement.toString());
        List<FluxTable> tables = queryApi.query(influxQuery);
        for (FluxTable table : tables) {
            List<FluxRecord> records = table.getRecords();
            for (FluxRecord record : records) {
                try {
                    return Long.parseLong(Objects.requireNonNull(record.getValueByKey("_value")).toString());
                } catch (NumberFormatException e) {
                    throw new MetricsQueryServiceException("Could not parse measurement count", e);
                }
            }
        }
        return 0;
    }

    public void reset() {
        this.metricExportRepository.deleteAll();
    }
}
