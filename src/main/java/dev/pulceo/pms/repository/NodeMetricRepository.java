package dev.pulceo.pms.repository;

import dev.pulceo.pms.model.metric.NodeMetric;
import org.springframework.data.repository.CrudRepository;

public interface NodeMetricRepository extends CrudRepository<NodeMetric, Long> {
}
