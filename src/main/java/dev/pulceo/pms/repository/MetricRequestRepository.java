package dev.pulceo.pms.repository;

import dev.pulceo.pms.model.metricrequests.AbstractMetricRequest;
import dev.pulceo.pms.model.metricrequests.MetricRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetricRequestRepository extends CrudRepository<MetricRequest, Long> {
}
