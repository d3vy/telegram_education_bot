package com.newtelegrambot.devyNewBot.repositories;

import com.newtelegrambot.devyNewBot.models.Response;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResponseRepository extends CrudRepository<Response, Long> {
}
