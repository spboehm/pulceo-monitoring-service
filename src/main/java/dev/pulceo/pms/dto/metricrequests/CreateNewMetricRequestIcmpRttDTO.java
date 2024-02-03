package dev.pulceo.pms.dto.metricrequests;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateNewMetricRequestIcmpRttDTO extends CreateNewAbstractMetricRequestDTO {
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

    public static CreateNewMetricRequestIcmpRttDTO fromAbstractMetricRequestDTO(CreateNewAbstractMetricRequestDTO createNewAbstractMetricRequestDTO) {
        CreateNewMetricRequestIcmpRttDTO createNewMetricRequestIcmpRttDTO = (CreateNewMetricRequestIcmpRttDTO) createNewAbstractMetricRequestDTO;
        return CreateNewMetricRequestIcmpRttDTO.builder()
                .linkUUID(createNewMetricRequestIcmpRttDTO.getLinkUUID())
                .type(createNewMetricRequestIcmpRttDTO.getType())
                .recurrence(createNewMetricRequestIcmpRttDTO.getRecurrence())
                .enabled(createNewMetricRequestIcmpRttDTO.isEnabled())
                .ipVersion(createNewMetricRequestIcmpRttDTO.getIpVersion())
                .count(createNewMetricRequestIcmpRttDTO.getCount())
                .dataLength(createNewMetricRequestIcmpRttDTO.getDataLength())
                .iface(createNewMetricRequestIcmpRttDTO.getIface())
                .build();
    }
}
