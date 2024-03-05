package dev.pulceo.pms.model.metricrequests;

import dev.pulceo.pms.dto.metricrequests.CreateNewMetricRequestTcpUdpRttDTO;
import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public class TcpUdpRttMetricRequest extends AbstractMetricRequest {
    private String linkId;
    private String type;
    private String recurrence;
    private boolean enabled;
    // TODO: ipVersion
    @Builder.Default
    private int rounds = 10;

    public static TcpUdpRttMetricRequest fromCreateNewMetricRequestTcpUdpRttDTO(CreateNewMetricRequestTcpUdpRttDTO createNewMetricRequestTcpUdpRttDTO) {
        return TcpUdpRttMetricRequest.builder()
            .linkId(createNewMetricRequestTcpUdpRttDTO.getLinkId())
            .type(createNewMetricRequestTcpUdpRttDTO.getType())
            .recurrence(createNewMetricRequestTcpUdpRttDTO.getRecurrence())
            .enabled(createNewMetricRequestTcpUdpRttDTO.isEnabled())
            .rounds(createNewMetricRequestTcpUdpRttDTO.getRounds())
            .build();
    }
}
