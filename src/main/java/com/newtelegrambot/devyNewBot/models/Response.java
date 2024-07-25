package com.newtelegrambot.devyNewBot.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "responses")
public class Response {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String question;
    private String answer;
}
