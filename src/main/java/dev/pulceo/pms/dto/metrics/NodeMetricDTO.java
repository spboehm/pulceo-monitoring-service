package dev.pulceo.pms.dto.metrics;

import com.influxdb.query.FluxRecord;
import dev.pulceo.pms.model.BaseEntity;
import dev.pulceo.pms.model.metric.NodeMetric;
import dev.pulceo.pms.model.metricrequests.MetricRequest;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString
public class NodeMetricDTO extends BaseEntity {
    // TODO: add additional metadata
    private UUID metricUUID;
    private String metricType;
    private String metricRequestUUID;
    private String nodeUUID;
    private String time;
    private double val;
    private String unit;

    public static NodeMetricDTO fromFluxRecord(FluxRecord record, MetricRequest metricRequest, String unit) {
        return NodeMetricDTO.builder()
                .metricUUID(UUID.fromString(record.getValueByKey("metricUUID").toString()))
                .metricType(record.getValueByKey("metricType").toString())
                .metricRequestUUID(metricRequest.getUuid().toString())
                .nodeUUID(metricRequest.getLinkUUID().toString())
                .time(record.getValueByKey("timestamp").toString())
                .val(Double.valueOf(record.getValueByKey("_value").toString()))
                .unit(unit)
                .build();
    }

}
