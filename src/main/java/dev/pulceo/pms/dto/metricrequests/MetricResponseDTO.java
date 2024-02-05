package dev.pulceo.pms.dto.metricrequests;

import dev.pulceo.pms.model.metricrequests.MetricRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetricResponseDTO {
    // TODO: rename to jobUUID
    private UUID uuid;
    private UUID linkUUID;
    private String type;
    private String recurrence;
    private boolean enabled;

    public static MetricResponseDTO fromMetricRequest(MetricRequest metricRequest) {
        return new MetricResponseDTO(
                metricRequest.getUuid(),
                metricRequest.getRemoteLinkUUID(),
                metricRequest.getType(),
                metricRequest.getRecurrence(),
                metricRequest.isEnabled()
        );
    }
}
