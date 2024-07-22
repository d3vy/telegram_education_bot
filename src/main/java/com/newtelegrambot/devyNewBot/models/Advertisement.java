package com.newtelegrambot.devyNewBot.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "advertisementTable")
@Data
public class Advertisement {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long id;

    private String advertisementText;
}
