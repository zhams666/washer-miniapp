package com.washer.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("cloudbase")
@SpringBootTest(properties = {
    "cloudbase.pg.env-id=washer-test-example",
    "cloudbase.pg.api-key=server-key",
    "spring.task.scheduling.enabled=false"
})
class CloudBaseProfileContextTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void cloudbaseProfileDoesNotCreateJdbcDataSource() {
        assertEquals(0, context.getBeansOfType(HikariDataSource.class).size());
    }
}
