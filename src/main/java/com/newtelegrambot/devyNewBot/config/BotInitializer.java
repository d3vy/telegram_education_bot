package com.newtelegrambot.devyNewBot.config;

import com.newtelegrambot.devyNewBot.service.TelegramBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Component
public class BotInitializer {

	final TelegramBot bot;

	public BotInitializer(TelegramBot bot) {
		this.bot = bot;
	}


	//Создание бота(стоит получше разобратсья в этом коде)
	@EventListener({ContextRefreshedEvent.class})
	public void init() throws TelegramApiException {
		TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
		try {
			telegramBotsApi.registerBot(bot);
		}catch (TelegramApiException e) {
			log.error(e.getMessage());
		}

	}

}
