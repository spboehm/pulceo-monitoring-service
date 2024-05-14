package dev.pulceo.pms.service;

import com.influxdb.client.*;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.WriteConsistency;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.client.write.WriteParameters;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import dev.pulceo.pms.dto.metrics.ShortNodeLinkMetricDTO;
import dev.pulceo.pms.exception.MetricsQueryServiceException;
import dev.pulceo.pms.model.metric.MetricType;
import dev.pulceo.pms.model.metricexports.MetricExport;
import dev.pulceo.pms.model.metricexports.MetricExportRequest;
import dev.pulceo.pms.model.metricexports.MetricExportState;
import dev.pulceo.pms.util.InfluxQueryBuilder;
import org.codehaus.groovy.transform.SourceURIASTTransformation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import javax.xml.crypto.Data;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class MetricsQueryServiceTest {

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
            File file = new File("src/test/resources/metricexports/cpu_util.csv");
            writeCSVFileToInfluxDB(file);
        }
    }

    @Test
    @Disabled
    public void testGetMeasurementAsCSV() throws InterruptedException, IOException, MetricsQueryServiceException {
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
        try {
            String influxWriteCMD = String.format("INFLUX_ORG=org INFLUX_TOKEN=token /usr/local/bin/influx write --bucket %s --file %s", bucket, file.getAbsolutePath());
            ProcessBuilder processBuilder = new ProcessBuilder("/usr/local/bin/influx", "write", "--bucket", "test-bucket", "--file", "/home/sebastian.boehm/git/dissertation/pulceo-monitoring-service/src/test/resources/metricexports/cpu_util.csv");
            processBuilder.environment().clear();
            processBuilder.environment().put("INFLUX_ORG", org);
            processBuilder.environment().put("INFLUX_TOKEN", token);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.to(new File("/tmp/influx_write.log")));
            processBuilder.redirectError(ProcessBuilder.Redirect.to(new File("/tmp/influx_write_error.log")));
            Process influxWriteCMDProcess = processBuilder.start();
            influxWriteCMDProcess.waitFor(10, TimeUnit.SECONDS);
            if (influxWriteCMDProcess.exitValue() != 0) {
                List<String> strings = new ArrayList<>();
                strings.add("Could not write data to InfluxDB");
                strings.addAll(Files.readAllLines(Paths.get("/tmp/influx_write_error.log"), Charset.defaultCharset()));
                throw new IOException(strings.toString());
            }
            closeProcess(influxWriteCMDProcess);
        } catch (Exception e) {
            e.printStackTrace();
        }

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
        }
    }

}
