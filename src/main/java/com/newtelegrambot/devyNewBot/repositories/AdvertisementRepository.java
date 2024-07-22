package com.newtelegrambot.devyNewBot.repositories;

import com.newtelegrambot.devyNewBot.models.Advertisement;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdvertisementRepository extends CrudRepository<Advertisement, Long> {
}
