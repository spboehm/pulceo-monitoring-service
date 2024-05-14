package dev.pulceo.pms.model.metricexports;

import dev.pulceo.pms.dto.metricexports.MetricExportRequestDTO;
import dev.pulceo.pms.model.BaseEntity;
import dev.pulceo.pms.model.metric.MetricType;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
public class MetricExportRequest extends BaseEntity {

    private MetricType metricType;

    public static MetricExportRequest fromMetricExportRequestDTO(MetricExportRequestDTO metricExportRequestDTO) {
        return MetricExportRequest.builder()
                .metricType(metricExportRequestDTO.getMetricType())
                .build();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        MetricExportRequest that = (MetricExportRequest) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
