package dev.pulceo.pms.repository;

import dev.pulceo.pms.model.metric.NodeLinkMetric;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface NodeLinkMetricRepository extends CrudRepository<NodeLinkMetric, Long>{

    @Query("SELECT n FROM NodeLinkMetric n WHERE n.linkUUID = ?1 AND n.metricType = ?2 ORDER BY n.id DESC LIMIT 1")
    NodeLinkMetric findLastLinkUUIDAndByMetricType(String linkUUID, String metricType);

    @Query("SELECT DISTINCT metricType FROM NodeLinkMetric")
    List<String> findDistinctMetricTypes();

}
