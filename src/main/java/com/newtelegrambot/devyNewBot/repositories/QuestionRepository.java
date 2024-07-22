package com.newtelegrambot.devyNewBot.repositories;

import com.newtelegrambot.devyNewBot.models.Question;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends CrudRepository<Question, Long> {
}
