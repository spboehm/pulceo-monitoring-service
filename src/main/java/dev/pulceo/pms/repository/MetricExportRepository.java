package dev.pulceo.pms.repository;

import dev.pulceo.pms.model.metricexports.MetricExport;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetricExportRepository extends CrudRepository<MetricExport, Long> {

    Optional<MetricExport> findByUuid(UUID metricExportId);
}
