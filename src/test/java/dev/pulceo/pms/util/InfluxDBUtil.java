package dev.pulceo.pms.util;

import com.influxdb.client.BucketsApi;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.OrganizationsApi;
import com.influxdb.client.domain.Bucket;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class InfluxDBUtil {

    private final static Logger logger = Logger.getLogger(InfluxDBUtil.class.getName());

    private final static String influxDBHost= "http://localhost:8086";
    private final static String token = "token";
    private final static String org = "org";
    private final static String bucket = "bucket";
    private final static String influxDBUrl = influxDBHost + "?readTimeout=30000&writeTimeout=30000&connectTimeout=30000";

    public static void initInfluxDBTestEnvironment() {
        try(InfluxDBClient influxDBClient = InfluxDBClientFactory.create(influxDBUrl, token.toCharArray(), org)) {
            // delete bucket
            BucketsApi bucketsApi = influxDBClient.getBucketsApi();
            Optional<Bucket> foundBucket = Optional.ofNullable(bucketsApi.findBucketByName(bucket));
            if (foundBucket.isPresent()) {
                bucketsApi.deleteBucket(foundBucket.get());
            }

            // create bucket
            OrganizationsApi organizationsApi = influxDBClient.getOrganizationsApi();
            String orgId = organizationsApi.findOrganizations().stream().findFirst().orElseThrow().getId();
            bucketsApi.createBucket(bucket, orgId);
        }
    }

    public static void writeCSVFileToInfluxDB(File file) throws IOException, InterruptedException {
        Process influxWriteCMDProcess = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", String.format("INFLUX_ORG=org INFLUX_TOKEN=token /usr/local/bin/influx write --bucket %s --format csv --file %s", bucket, file.getAbsolutePath()));
            processBuilder.inheritIO();
            influxWriteCMDProcess = processBuilder.start();
            influxWriteCMDProcess.waitFor(5, TimeUnit.SECONDS);
            if (influxWriteCMDProcess.exitValue() != 0) {
                throw new IOException("Could not write CSV file to InfluxDB: " + influxWriteCMDProcess.exitValue());
            }
        } finally {
            closeProcess(influxWriteCMDProcess);
        }
    }

    public static void provideInfluxCPUUtilMetrics() throws IOException, InterruptedException {
        writeCSVFileToInfluxDB(new File("src/test/resources/metricexports/cpu_util.csv"));
    }

    private static void closeProcess(Process process) throws IOException {
        if (process != null) {
            process.getInputStream().close();
            process.getOutputStream().close();
            process.getErrorStream().close();
            process.destroy();
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        } else {
            // do nothing
            logger.info("Process already closed, nothing to close");
        }
    }

}
