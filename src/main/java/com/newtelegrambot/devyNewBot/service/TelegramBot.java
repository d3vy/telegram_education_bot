package com.newtelegrambot.devyNewBot.service;

import com.newtelegrambot.devyNewBot.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

	final BotConfig config;

	public TelegramBot(BotConfig config) {
		this.config = config;
	}

	@Override
	public String getBotUsername() {
		return config.getBotName();
	}

	@Override
	public String getBotToken(){
		return config.getToken();
	}

	@Override
	public void onUpdateReceived(Update update) {

		if (update.hasMessage() && update.getMessage().hasText()) {
			String message = update.getMessage().getText();
			Long chatId = update.getMessage().getChatId();

			switch (message) {
				case "/start":
					startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
					break;
				default:
					sendMessage(chatId, "Command wasn't recognized");


			}
		}
	}

	private void startCommandReceived(long chatId, String name){

		String answer = "Hello, " + name + ", nice to meet you!";
		log.info("Replied to user " + name + " with answer: " + answer);


		sendMessage(chatId, answer);
	}

	private void sendMessage(long chatId, String messageToSend){
		SendMessage message = new SendMessage();

		//Может вызвать ошибку. Можно заменить на String.valueOf(chatId)
		message.setChatId(chatId);
		message.setText(messageToSend);

		try {
			execute(message);
		}catch (TelegramApiException e){
			log.error(e.getMessage());
		}
	}
}
