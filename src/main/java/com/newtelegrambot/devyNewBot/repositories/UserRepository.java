package com.newtelegrambot.devyNewBot.repositories;

import com.newtelegrambot.devyNewBot.models.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {


}
