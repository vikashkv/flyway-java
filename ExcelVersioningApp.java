package com.example;

import com.example.migration.RuleMigration;
import org.flywaydb.core.Flyway;

public class ExcelVersioningApp {
    public static void main(String[] args) {
        // Set up the Flyway instance
        Flyway flyway = Flyway.configure()
                .dataSource("jdbc:h2:mem:testdb", "sa", null)
                .load();

        // Start the migration process
        flyway.migrate();

        // Create and run the RuleMigration to process Excel files
        RuleMigration migration = new RuleMigration();
        migration.loadRulesFromExcel("src/main/resources/rules/rules_v1.xlsx");
        migration.loadRulesFromExcel("src/main/resources/rules/rules_v2.xlsx");

        System.out.println("Migration completed successfully.");
    }
}
