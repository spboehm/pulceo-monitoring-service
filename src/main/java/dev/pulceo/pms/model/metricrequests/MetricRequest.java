package dev.pulceo.pms.model.metricrequests;

import com.fasterxml.jackson.databind.ser.Serializers;
import dev.pulceo.pms.model.BaseEntity;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.modelmapper.internal.bytebuddy.implementation.bind.annotation.Super;

import java.util.UUID;

@Entity
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
// TODO change to network metric request
public class MetricRequest extends BaseEntity {
    // TODO: change with job id
    private UUID uuid;
    private UUID linkUUID; // remote link UUID
    private String type;
    private String recurrence;
    // TODO: add transformer
    private boolean enabled;
}
