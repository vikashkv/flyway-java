import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;

@Component
public class RuleMigration {
    @Autowired
    private RuleRepository ruleRepository;

    @PostConstruct
    public void migrateRules() {
        loadRulesFromExcel("src/main/resources/rules/rules_v1.xlsx");
        loadRulesFromExcel("src/main/resources/rules/rules_v2.xlsx");
    }

    public void loadRulesFromExcel(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            // Get the first sheet (assumed to contain the rules)
            var sheet = workbook.getSheetAt(0);
            int version = extractVersionFromFileName(filePath);

            // Read rows from the sheet
            for (Row row : sheet) {
                String name = row.getCell(0).getStringCellValue(); // Assuming name is in the first column
                String description = row.getCell(1).getStringCellValue(); // Assuming description is in the second column
                insertRule(name, description, version);
            }
        } catch (Exception e) {
            System.err.println("Error loading rules from Excel file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int extractVersionFromFileName(String filePath) {
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        String versionPart = fileName.replaceAll("rules_v", "").replaceAll("\\.xlsx", "");
        return Integer.parseInt(versionPart);
    }

    private void insertRule(String name, String description, int version) {
        Rule rule = new Rule(null, name, description, version);
        ruleRepository.save(rule);
        System.out.println("Inserted rule: " + name);
    }
}
