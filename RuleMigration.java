package com.example.migration;

import com.example.model.Rule;
import com.example.repository.RuleRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.io.InputStream;
import java.util.Iterator;

public class RuleMigration extends BaseJavaMigration {
    private final RuleRepository ruleRepository;

    public RuleMigration(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Override
    public void migrate(Context context) throws Exception {
        loadRulesFromExcel("rules/rules_v1.xlsx");
        loadRulesFromExcel("rules/rules_v2.xlsx");
    }

    private void loadRulesFromExcel(String filePath) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            var sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            rowIterator.next(); // Skip header

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Rule rule = new Rule();
                rule.setName(row.getCell(0).getStringCellValue());
                rule.setDescription(row.getCell(1).getStringCellValue());
                ruleRepository.save(rule);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
