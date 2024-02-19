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
        property = "metricRequestDTOType",
        use = JsonTypeInfo.Id.NAME,
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreateNewMetricRequestIcmpRttDTO.class, name = "ICMP_RTT"),
        @JsonSubTypes.Type(value = CreateNewMetricRequestTcpBwDTO.class, name = "TCP_BW"),
        @JsonSubTypes.Type(value = CreateNewMetricRequestCPUUtilDTO.class, name = "CPU_UTIL")
})
public abstract class CreateNewAbstractMetricRequestDTO {
    private MetricRequestDTOType metricRequestDTOType;
}
