package dev.pulceo.pms.model.metricrequests;

import dev.pulceo.pms.model.BaseEntity;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
// TODO change to network metric request
public class MetricRequest extends BaseEntity {
    // TODO: change with job id
    // uuid in superclass
    private UUID remoteMetricRequestUUID; // jobUUID on device
    private UUID linkUUID; // global link UUID
    private UUID remoteLinkUUID; // remoteLinkUUID on device
    private String type;
    private String recurrence;
    // TODO: add transformer
    private boolean enabled;
}
