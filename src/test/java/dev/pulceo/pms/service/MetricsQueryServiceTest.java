package dev.pulceo.pms.service;

import com.influxdb.client.BucketsApi;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.OrganizationsApi;
import com.influxdb.client.domain.Bucket;
import dev.pulceo.pms.exception.MetricsQueryServiceException;
import dev.pulceo.pms.model.metric.MetricType;
import dev.pulceo.pms.model.metricexports.MetricExport;
import dev.pulceo.pms.model.metricexports.MetricExportRequest;
import dev.pulceo.pms.model.metricexports.MetricExportState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class MetricsQueryServiceTest {

    private final static Logger logger = Logger.getLogger(MetricsQueryServiceTest.class.getName());

    private final static String influxDBHost= "http://localhost:8086";
    private final static String token = "token";
    private final static String org = "org";
    private final static String bucket = "test-bucket";
    private final static String influxDBUrl = influxDBHost + "?readTimeout=30000&writeTimeout=30000&connectTimeout=30000";

    @Autowired
    MetricsQueryService metricsQueryService;

    @BeforeAll
    static void setupClass() throws InterruptedException, IOException {
        try(InfluxDBClient influxDBClient = InfluxDBClientFactory.create(influxDBUrl, token.toCharArray(), org)) {

            // delete bucket
            BucketsApi bucketsApi = influxDBClient.getBucketsApi();
            Optional<Bucket> foundBucket = Optional.ofNullable(bucketsApi.findBucketByName(bucket));
            if (foundBucket.isPresent()) {
                bucketsApi.deleteBucket(foundBucket.get());
            }

            // create bucket
            OrganizationsApi organizationsApi = influxDBClient.getOrganizationsApi();
            String orgId = organizationsApi.findOrganizations().stream().findFirst().orElseThrow().getId();
            bucketsApi.createBucket(bucket, orgId);

            // import data
            writeCSVFileToInfluxDB(new File("src/test/resources/metricexports/cpu_util.csv"));
        }
    }

    @Test
    @Disabled
    public void testGetMeasurementAsCSV() throws MetricsQueryServiceException {
        // given
        MetricExportRequest metricExportRequest = MetricExportRequest
                .builder()
                .metricType(MetricType.CPU_UTIL)
                .build();

        // when
        MetricExport metricExport = this.metricsQueryService.createMetricExport(metricExportRequest);

        // then
        assertEquals(metricExportRequest.getMetricType(), metricExport.getMetricType());
        assertEquals(MetricExportState.PENDING, metricExport.getMetricExportState());

    }

    private static void writeCSVFileToInfluxDB(File file) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", String.format("INFLUX_ORG=org INFLUX_TOKEN=token /usr/local/bin/influx write --bucket %s --file %s", bucket, file.getAbsolutePath()));
        processBuilder.inheritIO();
        Process influxWriteCMDProcess = processBuilder.start();
        influxWriteCMDProcess.waitFor(5, TimeUnit.SECONDS);
        if (influxWriteCMDProcess.exitValue() != 0) {
            throw new IOException("Could not write CSV file to InfluxDB: " + influxWriteCMDProcess.exitValue());
        }
        closeProcess(influxWriteCMDProcess);
    }

    private static void closeProcess(Process process) throws IOException {
        if (process != null) {
            process.getInputStream().close();
            process.getOutputStream().close();
            process.getErrorStream().close();
            process.destroy();
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        } else {
            // do nothing
            logger.info("Process already closed, nothing to close");
        }
    }

}
