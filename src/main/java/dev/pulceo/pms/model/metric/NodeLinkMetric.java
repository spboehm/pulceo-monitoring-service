package dev.pulceo.pms.model.metric;

import dev.pulceo.pms.dto.metrics.NodeLinkMetricDTO;
import dev.pulceo.pms.model.BaseEntity;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString
public class NodeLinkMetric extends BaseEntity {
    // TODO: add additional metadata
    private UUID metricUUID;
    private String metricType;
    private String metricRequestUUID;
    private String linkUUID;
    private String startTime;
    private String endTime;
    private double val;
    private String unit;

    public static  NodeLinkMetric fromNodeLinkMetricDTO(NodeLinkMetricDTO nodeLinkMetricDTO) {
        return NodeLinkMetric.builder()
                .metricUUID(nodeLinkMetricDTO.getMetricUUID())
                .metricType(nodeLinkMetricDTO.getMetricType())
                .metricRequestUUID(nodeLinkMetricDTO.getMetricRequestUUID())
                .linkUUID(nodeLinkMetricDTO.getLinkUUID())
                .startTime(nodeLinkMetricDTO.getStartTime())
                .endTime(nodeLinkMetricDTO.getEndTime())
                .val(nodeLinkMetricDTO.getValue())
                .unit(nodeLinkMetricDTO.getUnit())
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeLinkMetric that = (NodeLinkMetric) o;

        if (Double.compare(val, that.val) != 0) return false;
        if (!Objects.equals(metricUUID, that.metricUUID)) return false;
        if (!Objects.equals(metricType, that.metricType)) return false;
        if (!Objects.equals(metricRequestUUID, that.metricRequestUUID))
            return false;
        if (!Objects.equals(linkUUID, that.linkUUID)) return false;
        if (!Objects.equals(startTime, that.startTime)) return false;
        if (!Objects.equals(endTime, that.endTime)) return false;
        return Objects.equals(unit, that.unit);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = metricUUID != null ? metricUUID.hashCode() : 0;
        result = 31 * result + (metricType != null ? metricType.hashCode() : 0);
        result = 31 * result + (metricRequestUUID != null ? metricRequestUUID.hashCode() : 0);
        result = 31 * result + (linkUUID != null ? linkUUID.hashCode() : 0);
        result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
        result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
        temp = Double.doubleToLongBits(val);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (unit != null ? unit.hashCode() : 0);
        return result;
    }
}
