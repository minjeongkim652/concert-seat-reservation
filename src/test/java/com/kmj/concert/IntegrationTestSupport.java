package com.kmj.concert;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
public abstract class IntegrationTestSupport {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("postgres:16-alpine")
            )
                    .withDatabaseName("concert")
                    .withUsername("concert")
                    .withPassword("concert");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE seats, holds, performances
                RESTART IDENTITY CASCADE
                """);

        jdbcTemplate.update("""
                INSERT INTO performances (name)
                VALUES ('Test Concert')
                """);

        jdbcTemplate.execute("""
                INSERT INTO seats (performance_id, label, status)
                SELECT 1, 'A-' || seat_number, 'AVAILABLE'
                FROM generate_series(1, 20) AS seat_number
                """);
    }
}