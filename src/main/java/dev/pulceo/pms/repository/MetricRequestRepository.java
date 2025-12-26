package dev.pulceo.pms.repository;

import dev.pulceo.pms.model.metricrequests.MetricRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetricRequestRepository extends CrudRepository<MetricRequest, Long> {
    MetricRequest findByUuid(UUID metricRequestUUID);

    Iterable<MetricRequest> findByLinkUUID(UUID linkUUID);

    boolean existsMetricRequestByLinkUUIDAndType(UUID linkUUID, String type);

    Optional<MetricRequest> findByLinkUUIDAndType(UUID linkUUID, String type);
}
