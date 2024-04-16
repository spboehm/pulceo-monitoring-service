package dev.pulceo.pms.service;

import com.influxdb.client.*;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import dev.pulceo.pms.exception.MetricsQueryServiceException;
import dev.pulceo.pms.model.metric.MetricType;
import dev.pulceo.pms.model.metricexports.MetricExport;
import dev.pulceo.pms.model.metricexports.MetricExportRequest;
import dev.pulceo.pms.model.metricexports.MetricExportState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class MetricsQueryServiceTest {

    private static String token = "token";
    private static String org = "org";
    private static String bucket = "test-bucket";
    private static String influxDBUrl = "http://localhost:8086?readTimeout=30000&writeTimeout=30000&connectTimeout=30000";

    @Autowired
    MetricsQueryService metricsQueryService;

    @BeforeAll
    static void setupClass() throws InterruptedException, IOException {
        try(InfluxDBClient influxDBClient = InfluxDBClientFactory.create(influxDBUrl, token.toCharArray(), org)) {

            // delete bucket
            BucketsApi bucketsApi = influxDBClient.getBucketsApi();
            Bucket foundBucket = bucketsApi.findBucketByName(bucket);
            bucketsApi.deleteBucket(foundBucket);

            // create bucket
            OrganizationsApi organizationsApi = influxDBClient.getOrganizationsApi();
            String orgId = organizationsApi.findOrganizations().stream().findFirst().orElseThrow().getId();
            bucketsApi.createBucket(bucket, orgId);

            // import data
            List<String> listOffiles = Files.readAllLines(new File("src/test/resources/metricexports/cpu_util.csv").toPath(), Charset.defaultCharset());
            for (String line : listOffiles) {

            }
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

}
