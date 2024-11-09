package com.example.flyway.db.migration;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

@Component
public class V2__FlywayMigrationExcel extends BaseJavaMigration {

    private static final String DIRECTORY_PATH = "rules/";
    private static final String CHECK_SQL_PATH = "classpath:sql/check_rule_exists.sql";
    private static final String INSERT_SQL_PATH = "classpath:sql/insert_rule.sql";
    private static final String UPDATE_SQL_PATH = "classpath:sql/update_rule.sql";
    private final ResourceLoader resourceLoader;

    public V2__FlywayMigrationExcel(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void migrate(Context context) throws Exception {
        DataSource dataSource = context.getConfiguration().getDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<Path> excelFiles = getExcelFiles();
        for (Path file : excelFiles) {
            System.out.println("Processing Excel file: " + file.getFileName());
            processExcelFile(file, jdbcTemplate);
        }
    }

    private List<Path> getExcelFiles() throws Exception {
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

    private void applyRuleToDatabase(JdbcTemplate jdbcTemplate, int id, String name, String condition, String action) throws IOException {
        String checkSql = loadSqlFromFile(CHECK_SQL_PATH);
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, id);
        if (count != null && count > 0) {
            // Update the existing record
            String updateSql = loadSqlFromFile(UPDATE_SQL_PATH);
            jdbcTemplate.update(updateSql, name, condition, action, id);
        } else {
            // Insert a new record
            String insertSql = loadSqlFromFile(INSERT_SQL_PATH);
            jdbcTemplate.update(insertSql, id, name, condition, action);
        }
    }

    private String loadSqlFromFile(String path) throws IOException {
        Resource resource = resourceLoader.getResource(path);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
