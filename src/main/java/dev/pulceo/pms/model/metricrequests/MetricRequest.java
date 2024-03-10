package dev.pulceo.pms.model.metricrequests;

import dev.pulceo.pms.dto.metricrequests.pna.ShortNodeMetricResponseDTO;
import dev.pulceo.pms.model.BaseEntity;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.geo.Metric;

import java.util.Objects;
import java.util.UUID;

@Entity
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@ToString
// TODO change to network metric request
public class MetricRequest extends BaseEntity {
    // TODO: change with job id
    // uuid in superclass
    private UUID remoteMetricRequestUUID; // jobUUID on device
    private UUID linkUUID; // global link UUID or UUID of the resource
    private UUID remoteLinkUUID; // remoteLinkUUID on device
    private String type;
    private String recurrence;
    // TODO: add transformer
    private boolean enabled;

    public static MetricRequest fromShortNodeMetricResponseDTO(ShortNodeMetricResponseDTO shortNodeMetricResponseDTO) {
        return MetricRequest.builder()
                .remoteMetricRequestUUID(shortNodeMetricResponseDTO.getRemoteMetricRequestUUID())
                .linkUUID(shortNodeMetricResponseDTO.getRemoteNodeUUID())
                .remoteLinkUUID(shortNodeMetricResponseDTO.getRemoteNodeUUID())
                .type(shortNodeMetricResponseDTO.getType())
                .recurrence(shortNodeMetricResponseDTO.getRecurrence())
                .enabled(shortNodeMetricResponseDTO.isEnabled())
                .build();
    }

}
