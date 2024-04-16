package dev.pulceo.pms.model.metricexports;

import dev.pulceo.pms.model.BaseEntity;
import dev.pulceo.pms.model.metric.MetricType;
import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.proxy.HibernateProxy;
import org.modelmapper.internal.bytebuddy.implementation.bind.annotation.Super;

import java.util.Objects;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class MetricExport extends BaseEntity {

    private MetricType metricType;
    private long numberOfRecords;
    private String filename;
    @Builder.Default
    private MetricExportState metricExportState = MetricExportState.PENDING;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        MetricExport that = (MetricExport) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
