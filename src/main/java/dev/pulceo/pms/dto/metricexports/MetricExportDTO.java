package dev.pulceo.pms.dto.metricexports;

import dev.pulceo.pms.model.metric.MetricType;
import dev.pulceo.pms.model.metricexports.MetricExport;
import dev.pulceo.pms.model.metricexports.MetricExportState;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
public class MetricExportDTO {

    private UUID metricExportUUID;
    private MetricType metricType;
    private long numberOfRecords;
    private String url;
    private MetricExportState metricExportState;

    public static MetricExportDTO fromMetricExportDTO(MetricExport metricExport) {
        return MetricExportDTO.builder()
                .metricExportUUID(metricExport.getUuid())
                .metricType(metricExport.getMetricType())
                .numberOfRecords(metricExport.getNumberOfRecords())
                .url(metricExport.getUrl())
                .metricExportState(metricExport.getMetricExportState())
                .build();
    }

}