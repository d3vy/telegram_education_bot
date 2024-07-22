package com.newtelegrambot.devyNewBot.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name = "survey_questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long id;

    @Column(name = "question_text")
    private String questionText;

    @OneToMany(mappedBy = "question")
    private List<Answer> answers;

}
