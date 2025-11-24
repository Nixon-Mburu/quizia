package com.example.quizia_backend.controller;

import com.example.quizia_backend.model.Question;
import com.example.quizia_backend.repository.QuestionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = "*")
public class QuestionController {

    private final QuestionRepository questionRepository;

    public QuestionController(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @GetMapping
    public List<Question> getQuestions(@RequestParam String topic, @RequestParam(required = false, defaultValue = "30") int limit) {
        return questionRepository.findByTopic(topic, limit);
    }
}