package dev.pulceo.pms.service;

import dev.pulceo.pms.exception.MetricsQueryServiceException;
import dev.pulceo.pms.model.metric.MetricType;
import dev.pulceo.pms.model.metricexports.MetricExport;
import dev.pulceo.pms.model.metricexports.MetricExportRequest;
import dev.pulceo.pms.model.metricexports.MetricExportState;
import dev.pulceo.pms.util.InfluxDBUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class MetricsQueryServiceTest {

    @Autowired
    MetricsQueryService metricsQueryService;

    @BeforeAll
    static void setupClass() {
        InfluxDBUtil.initInfluxDBTestEnvironment();
    }

    @Test
    public void testGetMeasurementAsCSV() throws MetricsQueryServiceException, IOException, InterruptedException {
        // import data
        InfluxDBUtil.writeCSVFileToInfluxDB(new File("src/test/resources/metricexports/cpu_util.csv"));
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
