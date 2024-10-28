package com.example.flyway;

import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@SpringBootApplication
public class FlywayMigrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlywayMigrationApplication.class, args);
    }
}
