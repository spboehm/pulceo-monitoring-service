package dev.pulceo.pms.model.metric;

import dev.pulceo.pms.dto.metrics.NodeLinkMetricDTO;
import dev.pulceo.pms.dto.metrics.NodeMetricDTO;
import dev.pulceo.pms.model.BaseEntity;
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
public class NodeMetric extends BaseEntity {
    // TODO: add additional metadata
    private UUID metricUUID;
    private String metricType;
    private String metricRequestUUID;
    private String nodeUUID;
    private String time;
    private double val;
    private String unit;

    public static NodeMetric fromNodeLinkMetricDTO(NodeMetricDTO nodeMetricDTO) {
        return NodeMetric.builder()
                .metricUUID(nodeMetricDTO.getMetricUUID())
                .metricType(nodeMetricDTO.getMetricType())
                .metricRequestUUID(nodeMetricDTO.getMetricRequestUUID())
                .nodeUUID(nodeMetricDTO.getNodeUUID())
                .time(nodeMetricDTO.getTime())
                .val(nodeMetricDTO.getVal())
                .unit(nodeMetricDTO.getUnit())
                .build();
    }
}
