package dev.pulceo.pms.util;

public class InfluxQueryBuilder {

    public static String queryLastRawRecord(String bucket, String measurement, String field, String jobUUID) {
        return "from(bucket: \"" + bucket + "\")\n" +
                "  |> range(start: 0)\n" +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"" + measurement + "\" and r[\"_field\"] == \"" + field + "\" and r[\"jobUUID\"] == \"" + jobUUID + "\")\n" +
                "  |> group(columns: [])\n" +
                "  |> last()";
    }

    public static String queryNodeLinkRttMetricWithAggregation(String bucket, String measurement, String field, String aggregation) {
        return  "from(bucket: \"" + bucket + "\")\n" +
                "  |> range(start: 0)\n" +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"" + measurement + "\" and r[\"_field\"] == \"" + field + "\")\n" +
                "  |> group(columns: [\"jobUUID\"])\n" +
                "  |> " + aggregation + "\n";
    }

    public static String queryNodeLinkBwMetricWithAggregation(String bucket, String measurement, String field, String iperfRole, String aggregation) {
        return  "from(bucket: \"" + bucket + "\")\n" +
                "  |> range(start: 0)\n" +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"" + measurement + "\" and r[\"_field\"] == \"" + field + "\" and r[\"iperfRole\"] == \"" + iperfRole + "\")\n" +
                "  |> group(columns: [\"jobUUID\"])\n" +
                "  |> " + aggregation + "\n";
    }

    public static String queryMeasurementCount(String bucket, String measurement) {
        return "from(bucket: \"" + bucket + "\")\n" +
                "  |> range(start: 0)\n" +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"" + measurement + "\")\n" +
                "  |> group() " +
                "  |> count()";
    }

    public static String queryUtilMetrics(String bucket, String measurement) {
        return "from(bucket: \"" + bucket + "\")\n" +
                " |> range(start: 0)\n" +
                " |> filter(fn: (r) => r[\"_measurement\"] == \"" + measurement + "\")\n" +
                " |> toFloat()\n" +
                " |> drop(columns:[\"_start\",\"_stop\", \"_time\"])";
    }

    public static String queryEvents(String bucket) {
        return "from(bucket: \"" + bucket + "\")\n" +
                " |> range(start: 0)\n" +
                " |> filter(fn: (r) => r[\"_measurement\"] == \"EVENT\")\n" +
                " |> drop(columns:[\"_start\",\"_stop\", \"_time\"])";
    }

}
