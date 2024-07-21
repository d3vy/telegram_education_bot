package com.newtelegrambot.devyNewBot.service;

import com.newtelegrambot.devyNewBot.config.BotConfig;
import com.newtelegrambot.devyNewBot.models.User;
import com.newtelegrambot.devyNewBot.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.http.util.TimeStamp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

	final BotConfig config;
	private final UserRepository userRepository;

	@Value("${help.text}")
	static final String HELP_TEXT =

			"""
					This bot is created to help pupils save their homework, check it later and then delete.\s
					
					You can execute commands from the main manu on the left or by typing a command:
					
					Type /start to see a welcome message
					
					Type /mydata to see data stored about yourself
					
					Type /help to see this message again)
					
					Type /clear to clear chat history
					""";

	public TelegramBot(BotConfig config, UserRepository userRepository) {
		this.config = config;
		this.userRepository = userRepository;
		List<BotCommand> listOfCommands = new ArrayList<>();
		listOfCommands.add(new BotCommand("/start", "get a welcome message"));
		listOfCommands.add(new BotCommand("/help", "info about the bot"));
		listOfCommands.add(new BotCommand("/clear", "clear bot history"));
		listOfCommands.add(new BotCommand("/mydata", "get your data stored"));
		listOfCommands.add(new BotCommand("/deletedata", "delete my data"));
		listOfCommands.add(new BotCommand("/settings", "set your preferences"));
		try {
			this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
		} catch (TelegramApiException e) {
			log.error(e.getMessage());
		}
	}

	@Override
	public String getBotUsername() {
		return config.getBotName();
	}

	@Override
	public String getBotToken() {
		return config.getToken();
	}

	@Override
	public void onUpdateReceived(Update update) {

		if (update.hasMessage() && update.getMessage().hasText()) {
			String message = update.getMessage().getText();
			Long chatId = update.getMessage().getChatId();

			switch (message) {
				case "/start":

					registerUser(update.getMessage());

					startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
					break;
				case "/help":
					sendMessage(chatId, HELP_TEXT);
					break;
				case "/clear":
					clearChatHistory(chatId);
					break;
				default:
					sendMessage(chatId, "Command wasn't recognized");


			}
		}
	}

	private void registerUser(Message message) {
		if (userRepository.findById(message.getChatId()).isEmpty()) {

			var chatId = message.getChatId();
			var chat = message.getChat();

			User user = new User();

			user.setChatId(chatId);
			user.setFirstname(chat.getFirstName());
			user.setLastname(chat.getLastName());
			user.setUsername(chat.getUserName());
			user.setRegisterAt(System.currentTimeMillis());

			userRepository.save(user);

            log.info("User {} saved", user);
		}
	}

	private void startCommandReceived(long chatId, String name) {

		String answer = "Hello, " + name + ", nice to meet you!";
		log.info("Replied to user " + name + " with answer: " + answer);


		sendMessage(chatId, answer);
	}

	private void sendMessage(long chatId, String messageToSend) {
		SendMessage message = new SendMessage();

		//Может вызвать ошибку. Можно заменить на String.valueOf(chatId)
		message.setChatId(chatId);
		message.setText(messageToSend);

		try {
			execute(message);
		} catch (TelegramApiException e) {
			log.error(e.getMessage());
		}

	}

	private void clearChatHistory(long chatId) {
		sendMessage(chatId, "Messages have been deleted");
	}
}
