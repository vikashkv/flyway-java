package com.example.controller;

import com.example.model.Rule;
import com.example.repository.RuleRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RuleController {
    private final RuleRepository ruleRepository;

    public RuleController(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @GetMapping("/rules")
    public List<Rule> getRules() {
        return ruleRepository.findAll();
    }
}
