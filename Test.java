import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.apache.poi.ss.usermodel.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class V1__DynamicExcelMigration extends BaseJavaMigration {

    private static final String DIRECTORY_PATH = "rules/";
    private static final String VERSION_TRACKING_TABLE = "flyway_schema_history";

    @Override
    public void migrate(Context context) throws Exception {
        List<Path> newVersionFiles = getNewVersionFiles(context.getConnection());

        // Create a DataSource instance using H2's DriverManagerDataSource
        DataSource dataSource = createDataSource();

        // Use JdbcTemplate with the created DataSource
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        for (Path file : newVersionFiles) {
            System.out.println("Processing new file: " + file.getFileName());
            processExcelFile(file, jdbcTemplate);
            markVersionAsProcessed(jdbcTemplate, extractVersion(file));
        }
    }

    private DataSource createDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb"); // Update with your H2 database URL
        dataSource.setUsername("sa");
        dataSource.setPassword(""); // Update with your H2 database password if needed
        return dataSource;
    }

    private List<Path> getNewVersionFiles(Connection connection) throws Exception {
        Path directoryPath = Paths.get(getClass().getClassLoader().getResource(DIRECTORY_PATH).toURI());
        
        // List all files matching version pattern and sort them
        List<Path> allFiles = Files.list(directoryPath)
                .filter(file -> file.getFileName().toString().matches("rules_v\\d+\\.xlsx"))
                .sorted(Comparator.comparing(this::extractVersion))
                .collect(Collectors.toList());

        List<Integer> processedVersions = getProcessedVersions(connection);

        // Filter files that haven't been processed based on version
        return allFiles.stream()
                .filter(file -> !processedVersions.contains(extractVersion(file)))
                .collect(Collectors.toList());
    }

    private int extractVersion(Path file) {
        String fileName = file.getFileName().toString();
        return Integer.parseInt(fileName.replaceAll("[^0-9]", ""));
    }

    private List<Integer> getProcessedVersions(Connection connection) throws Exception {
        String sql = "SELECT script FROM " + VERSION_TRACKING_TABLE + " WHERE script LIKE 'rules_v%.xlsx'";
        
        try (PreparedStatement statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {

            return resultSet.next()
                ? List.of(resultSet.getString(1).replaceAll("[^0-9]", "")).stream().map(Integer::parseInt).toList()
                : List.of();
        }
    }

    private void processExcelFile(Path file, JdbcTemplate jdbcTemplate) throws Exception {
        try (InputStream inputStream = Files.newInputStream(file)) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row

                String ruleId = row.getCell(0).getStringCellValue();
                String condition = row.getCell(1).getStringCellValue();
                String action = row.getCell(2).getStringCellValue();

                applyRuleToDatabase(jdbcTemplate, ruleId, condition, action);
            }
            workbook.close();
        }
    }

    private void applyRuleToDatabase(JdbcTemplate jdbcTemplate, String ruleId, String condition, String action) {
        String sql = "INSERT INTO rules (rule_id, condition, action) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, ruleId, condition, action);
    }

    private void markVersionAsProcessed(JdbcTemplate jdbcTemplate, int version) throws Exception {
        String sql = "INSERT INTO " + VERSION_TRACKING_TABLE + " (script, installed_rank) VALUES (?, ?)";
        jdbcTemplate.update(sql, "rules_v" + version + ".xlsx", version);
    }
}
