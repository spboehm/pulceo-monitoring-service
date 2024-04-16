package dev.pulceo.pms.repository;

import dev.pulceo.pms.model.metricexports.MetricExport;
import dev.pulceo.pms.model.metricexports.MetricExportRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetricExportRepository extends CrudRepository<MetricExport, Long> {

}
