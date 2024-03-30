package dev.pulceo.pms.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import dev.pulceo.pms.dto.metrics.ShortNodeLinkMetricDTO;
import dev.pulceo.pms.exception.MetricsQueryServiceException;
import dev.pulceo.pms.util.InfluxQueryBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

import static dev.pulceo.pms.model.metricrequests.InternalMetricType.UDP_BW;

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

    private InfluxDBClient influxDBClient;

    @PostConstruct
    public void init() {
        influxDBClient = InfluxDBClientFactory.create(influxDBUrl, token.toCharArray(), org);
    }

    @PreDestroy
    public void close() {
        influxDBClient.close();
    }

    public ShortNodeLinkMetricDTO queryRangeNodeLinkMetrics(String metricType, String aggregation) throws MetricsQueryServiceException {
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
        for (FluxTable table : nodeLinkMetricsTables) {
            List<FluxRecord> records = table.getRecords();
            for (FluxRecord record : records) {
                return ShortNodeLinkMetricDTO.fromFluxRecord(record, unit);
            }
        }
        throw new MetricsQueryServiceException();
    }

}
