package dev.pulceo.pms.util;

import dev.pulceo.pms.model.metricrequests.InternalMetricType;
import dev.pulceo.pms.model.metricrequests.MetricRequest;
import org.springframework.beans.factory.annotation.Value;

public class InfluxQueryBuilder {

    public static String queryLastRawRecord(String bucket, String measurement, String field, String jobUUID) {
        return "from(bucket: \"" + bucket + "\")\n" +
                "  |> range(start: 0)\n" +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"" + measurement + "\" and r[\"_field\"] == \"" + field + "\" and r[\"jobUUID\"] == \"" + jobUUID + "\")\n" +
                "  |> group(columns: [])\n" +
                "  |> last()";
    }

}
