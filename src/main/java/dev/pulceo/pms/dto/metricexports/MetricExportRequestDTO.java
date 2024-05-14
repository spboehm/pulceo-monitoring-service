package dev.pulceo.pms.dto.metricexports;

import dev.pulceo.pms.model.metric.MetricType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class MetricExportRequestDTO {

    @NotNull
    private MetricType metricType;

}
