package dev.pulceo.pms.dto.metricexports;

import dev.pulceo.pms.model.metric.MetricType;
import dev.pulceo.pms.model.metricexports.MetricExportRequest;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class MetricExportRequestDTO {

    private MetricType metricType;

    public static MetricExportRequestDTO fromMetricExportRequestDTO(MetricExportRequest metricExportRequest) {
        return MetricExportRequestDTO.builder()
                .metricType(metricExportRequest.getMetricType())
                .build();
    }

}
