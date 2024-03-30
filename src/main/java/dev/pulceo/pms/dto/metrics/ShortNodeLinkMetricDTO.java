package dev.pulceo.pms.dto.metrics;

import com.influxdb.query.FluxRecord;
import dev.pulceo.pms.model.metricrequests.MetricRequest;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString
public class ShortNodeLinkMetricDTO {

    private UUID metricUUID;
    private String metricType;
    private String sourceNode;
    private String destinationNode;
    private String startTime;
    private String endTime;
    private double value;
    private String unit;

    public static ShortNodeLinkMetricDTO fromFluxRecord(FluxRecord record, String unit) {
        return ShortNodeLinkMetricDTO.builder()
                .metricUUID(UUID.fromString(record.getValueByKey("metricUUID").toString()))
                .metricType(record.getValueByKey("metricType").toString())
                .sourceNode(record.getValueByKey("sourceHost").toString())
                .destinationNode(record.getValueByKey("destinationHost").toString())
                .startTime(record.getValueByKey("startTime").toString())
                .endTime(record.getValueByKey("endTime").toString())
                .value(Double.valueOf(record.getValueByKey("_value").toString()))
                .unit(unit)
                .build();
    }

}
