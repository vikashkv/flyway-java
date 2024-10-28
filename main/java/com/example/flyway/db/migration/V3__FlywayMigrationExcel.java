package com.example.flyway.db.migration;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

@Component
public class V3__FlywayMigrationExcel extends BaseJavaMigration {

    private static final String DIRECTORY_PATH = "rules/";

    @Override
    public void migrate(Context context) throws Exception {
        // Retrieve the DataSource from the migration context
        DataSource dataSource = context.getConfiguration().getDataSource();

        // Create JdbcTemplate using the retrieved DataSource
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // Process all unprocessed Excel versions
        List<Path> excelFiles = getExcelFiles();
        for (Path file : excelFiles) {
            System.out.println("Processing Excel file: " + file.getFileName());
            processExcelFile(file, jdbcTemplate);
        }
    }

    private List<Path> getExcelFiles() throws Exception {
        // Find all Excel files matching version pattern in directory
        Path directoryPath = Paths.get(getClass().getClassLoader().getResource(DIRECTORY_PATH).toURI());
        return Files.list(directoryPath)
                .filter(file -> file.getFileName().toString().matches("rules_v\\d+\\.xlsx"))
                .sorted(Comparator.comparing(f -> f.getFileName().toString()))
                .toList();
    }

    private void processExcelFile(Path file, JdbcTemplate jdbcTemplate) throws Exception {
        try (InputStream inputStream = Files.newInputStream(file);
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row

                int id = (int) row.getCell(0).getNumericCellValue();
                String name = row.getCell(1).getStringCellValue();
                String condition = row.getCell(2).getStringCellValue();
                String action = row.getCell(3).getStringCellValue();

                applyRuleToDatabase(jdbcTemplate, id, name, condition, action);
            }
        }
    }
    private void applyRuleToDatabase(JdbcTemplate jdbcTemplate, int id, String name, String condition, String action) {
        String sql = "INSERT INTO rules (id, name, condition, action) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, id, name, condition, action);
    }
}
