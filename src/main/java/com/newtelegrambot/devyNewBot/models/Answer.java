package com.newtelegrambot.devyNewBot.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "survey_answers")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "answer_text")
    private String answerText;

    @ManyToOne(fetch = FetchType.EAGER) // Указываем связь с сущностью Question
    @JoinColumn(name = "question_id", nullable = false) // Указываем имя колонки, которая будет хранить внешний ключ
    private Question question;


}
