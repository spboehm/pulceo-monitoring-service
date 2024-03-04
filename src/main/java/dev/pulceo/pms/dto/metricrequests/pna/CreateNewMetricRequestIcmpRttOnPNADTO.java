package dev.pulceo.pms.dto.metricrequests.pna;

import dev.pulceo.pms.dto.metricrequests.CreateNewAbstractMetricRequestDTO;
import dev.pulceo.pms.dto.metricrequests.IPVersionDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateNewMetricRequestIcmpRttOnPNADTO extends CreateNewAbstractMetricRequestDTO {
    private UUID linkUUID;
    private String type;
    private String recurrence;
    private boolean enabled;
    @Builder.Default
    private IPVersionDTO ipVersion = IPVersionDTO.IPv4;
    @Builder.Default
    private int count = 10;
    @Builder.Default
    private int dataLength = 66;
    @Builder.Default
    private String iface = "lo";

}
