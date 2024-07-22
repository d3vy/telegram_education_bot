package com.newtelegrambot.devyNewBot.repositories;

import com.newtelegrambot.devyNewBot.models.Answer;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswersRepository extends CrudRepository<Answer, Long> {
}
