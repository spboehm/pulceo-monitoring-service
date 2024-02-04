package dev.pulceo.pms.model.metricrequests;

import dev.pulceo.pms.dto.metricrequests.CreateNewMetricRequestIcmpRttDTO;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Objects;
import java.util.UUID;

@Entity
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public class IcmpRttMetricRequest extends AbstractMetricRequest {
    private UUID linkUUID; // remote link UUID
    private String type;
    private String recurrence;
    private boolean enabled;

    public static IcmpRttMetricRequest fromCreateNewMetricRequestIcmpRttDTO(CreateNewMetricRequestIcmpRttDTO createNewMetricRequestIcmpRttDTO) {
        return IcmpRttMetricRequest.builder()
                .linkUUID(createNewMetricRequestIcmpRttDTO.getLinkUUID())
                .type(createNewMetricRequestIcmpRttDTO.getType())
                .recurrence(createNewMetricRequestIcmpRttDTO.getRecurrence())
                .enabled(createNewMetricRequestIcmpRttDTO.isEnabled())
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IcmpRttMetricRequest that = (IcmpRttMetricRequest) o;

        if (enabled != that.enabled) return false;
        if (!Objects.equals(linkUUID, that.linkUUID)) return false;
        if (!Objects.equals(type, that.type)) return false;
        return Objects.equals(recurrence, that.recurrence);
    }

    @Override
    public int hashCode() {
        int result = linkUUID != null ? linkUUID.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (recurrence != null ? recurrence.hashCode() : 0);
        result = 31 * result + (enabled ? 1 : 0);
        return result;
    }
}