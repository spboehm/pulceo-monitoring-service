package dev.pulceo.pms.repository;

import dev.pulceo.pms.model.metricrequests.AbstractMetricRequest;
import dev.pulceo.pms.model.metricrequests.MetricRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MetricRequestRepository extends CrudRepository<MetricRequest, Long> {
    MetricRequest findByUuid(UUID metricRequestUUID);
}
