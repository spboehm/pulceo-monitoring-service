package dev.pulceo.pms.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class InfluxDBServiceIntegrationTests {

    @Autowired
    private InfluxDBService influxDBService;

    @Test
    public void testCallInfluxDB() {
        // given

        // when
        influxDBService.callInfluxDb();
        // then
    }

}
