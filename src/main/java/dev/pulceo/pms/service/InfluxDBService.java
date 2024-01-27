package dev.pulceo.pms.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class InfluxDBService {

    @Value("${influxdb.token}")
    private String token;

    @Value("${influxdb.org}")
    private String org;

    @Value("${influxdb.bucket}")
    private String bucket;

    public void callInfluxDb() {

        try(InfluxDBClient influxDBClient = InfluxDBClientFactory.create("http://localhost:8086", token.toCharArray(), org, bucket)) {

            //
            // Write data
            //
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

            //
            // Write by Data Point
            //
            Point point = Point.measurement("temperature")
                    .addTag("location", "west")
                    .addField("value", 55D)
                    .time(Instant.now().toEpochMilli(), WritePrecision.MS);

            writeApi.writePoint(point);

            //
            // Write by LineProtocol
            //
            writeApi.writeRecord(WritePrecision.NS, "temperature,location=north value=60.0");


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
