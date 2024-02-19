package dev.pulceo.pms.dto.metricrequests.pna;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ShortNodeMetricResponseDTO {
    private UUID remoteMetricRequestUUID;
    private UUID remoteNodeUUID; // local on device
    private String type;
    private String recurrence;
    private boolean enabled;
}
