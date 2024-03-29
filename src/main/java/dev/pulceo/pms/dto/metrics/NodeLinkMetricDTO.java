package dev.pulceo.pms.dto.metrics;

import com.influxdb.query.FluxRecord;
import dev.pulceo.pms.model.metric.NodeLinkMetric;
import dev.pulceo.pms.model.metricrequests.MetricRequest;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.modelmapper.internal.bytebuddy.implementation.bind.annotation.Super;
import org.springframework.data.geo.Metric;
import org.springframework.jmx.support.MetricType;

import java.util.UUID;

@Data
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public class NodeLinkMetricDTO {

    private UUID metricUUID;
    private String metricType;
    private String metricRequestUUID;
    private String linkUUID;
    private String startTime;
    private String endTime;
    private double value;
    private String unit;

    public static NodeLinkMetricDTO fromFluxRecord(FluxRecord record, MetricRequest metricRequest, String unit) {
        return NodeLinkMetricDTO.builder()
                .metricUUID(UUID.fromString(record.getValueByKey("metricUUID").toString()))
                .metricType(record.getValueByKey("metricType").toString())
                .metricRequestUUID(metricRequest.getUuid().toString())
                .linkUUID(metricRequest.getLinkUUID().toString())
                .startTime(record.getValueByKey("startTime").toString())
                .endTime(record.getValueByKey("endTime").toString())
                .value(Double.valueOf(record.getValueByKey("_value").toString()))
                .unit(unit)
                .build();
    }

    public static NodeLinkMetricDTO fromNodeLinkMetric(NodeLinkMetric nodeLinkMetric) {
        return NodeLinkMetricDTO.builder()
                .metricUUID(nodeLinkMetric.getUuid())
                .metricType(nodeLinkMetric.getMetricType())
                .metricRequestUUID(nodeLinkMetric.getMetricRequestUUID())
                .linkUUID(nodeLinkMetric.getLinkUUID())
                .startTime(nodeLinkMetric.getStartTime())
                .endTime(nodeLinkMetric.getEndTime())
                .value(nodeLinkMetric.getVal())
                .unit(nodeLinkMetric.getUnit())
                .build();
    }
}
