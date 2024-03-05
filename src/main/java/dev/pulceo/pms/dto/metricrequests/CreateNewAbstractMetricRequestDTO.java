package dev.pulceo.pms.dto.metricrequests;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonTypeInfo(
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        use = JsonTypeInfo.Id.NAME,
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreateNewMetricRequestIcmpRttDTO.class, name = "icmp-rtt"),
        @JsonSubTypes.Type(value = CreateNewMetricRequestTcpUdpRttDTO.class, name = "tcp-rtt"),
        @JsonSubTypes.Type(value = CreateNewMetricRequestTcpUdpRttDTO.class, name = "udp-rtt"),
        @JsonSubTypes.Type(value = CreateNewMetricRequestTcpBwDTO.class, name = "tcp-bw"),
        @JsonSubTypes.Type(value = CreateNewMetricRequestTcpBwDTO.class, name = "udp-bw"),
        @JsonSubTypes.Type(value = CreateNewMetricRequestResourceUtilizationDTO.class, name = "cpu-util"),
        @JsonSubTypes.Type(value = CreateNewMetricRequestResourceUtilizationDTO.class, name = "mem-util"),
        @JsonSubTypes.Type(value = CreateNewMetricRequestResourceUtilizationDTO.class, name = "storage-util"),
        @JsonSubTypes.Type(value = CreateNewMetricRequestResourceUtilizationDTO.class, name = "net-util")
})
public abstract class CreateNewAbstractMetricRequestDTO {
    private String type;
}
