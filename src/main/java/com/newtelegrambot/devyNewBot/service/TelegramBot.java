package com.newtelegrambot.devyNewBot.service;

import com.newtelegrambot.devyNewBot.config.BotConfig;
import com.newtelegrambot.devyNewBot.models.User;
import com.newtelegrambot.devyNewBot.repositories.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

	final BotConfig config;
	private final UserRepository userRepository;

	static final String noButtonId = "NO_BUTTON";
	static final String yesButtonId = "YES_BUTTON";

	//Константа для команды /help: выводить пользователю все возможные команды.
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


	//Создание бота.
	//Здесь определяются команды для использования бота.
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


	//Получение имени бота.
	@Override
	public String getBotUsername() {
		return config.getBotName();
	}

	//Получение токена бота.
	@Override
	public String getBotToken() {
		return config.getToken();
	}


	//Бизес-логика для обработки команд.
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
				case "/register":
					register(chatId);
					break;
				default:
					sendMessage(chatId, "Command wasn't recognized");
			}
		}

		//Проверка на то, нет ли в update ID кнопки.
		//В отличие от первой проверки здесь нет message,
		//поэтому используем callbackquery вместо него.
		else if (update.hasCallbackQuery()) {
			//Получаем id нажатой кнопки.
			String callbackData = update.getCallbackQuery().getData();
			//Получаем id сообщения.
			long messageId = update.getCallbackQuery().getMessage().getMessageId();
			//Получаем id чата.
			long chatId = update.getCallbackQuery().getMessage().getChatId();

			if (callbackData.equals(yesButtonId)) {
				String text = "You pressed YES button";
				//Метод позволяет изменить сообщение при знании ID сообщения.
				EditMessageText editMessageText = new EditMessageText();
				editMessageText.setChatId(chatId);
				editMessageText.setText(text);
				editMessageText.setMessageId((int)messageId);

				try {
					//Отпарвка сообщения.
					execute(editMessageText);
				} catch (TelegramApiException e) {
					//Вывод логов при ошибке.
					log.error(e.getMessage());
				}

			}
			else if (callbackData.equals(noButtonId)) {
				String text = "You pressed NO button";
				EditMessageText editMessageText = new EditMessageText();
				editMessageText.setChatId(chatId);
				editMessageText.setText(text);
				editMessageText.setMessageId((int)messageId);

				try {
					//Отпарвка сообщения.
					execute(editMessageText);
				} catch (TelegramApiException e) {
					//Вывод логов при ошибке.
					log.error(e.getMessage());
				}
			}
		}
	}

	private void register(long chatId) {
		//Создание сообщения.
		SendMessage message = new SendMessage();
		message.setChatId(chatId);
		message.setText("Do you really want to register?");

		//Создание клавиатуры с кнопками, которая будет появляться под сообщениями бота.
		InlineKeyboardMarkup inlineKeyboardMarkup= new InlineKeyboardMarkup();
		//Создание ряда этой клавиатуры.
		List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
		//Создание списка кнопок для ряда.
		List<InlineKeyboardButton> rowInline = new ArrayList<>();

		//Создание кнопки.
		var yesButton = new InlineKeyboardButton();
		//Указание текста кнопки.
		yesButton.setText("YES");
		//Установка идентификатора для того, чтобы бот понимал, какая к нопка нажата.
		yesButton.setCallbackData(yesButtonId);

		//Создание второй кнопки.
		var noButton = new InlineKeyboardButton();
		noButton.setText("NO");
		noButton.setCallbackData(noButtonId);

		//Добавляем кнопки в ряд.
		rowInline.add(yesButton);
		rowInline.add(noButton);

		//Добавляем новый ряд в список рядов.
		rowsInline.add(rowInline);

		//Присваиваем ряд кнопок клавиатуре.
		inlineKeyboardMarkup.setKeyboard(rowsInline);
		//Добавляем клавиатуру к сообщению бота.
		message.setReplyMarkup(inlineKeyboardMarkup);

		try {
			//Отпарвка сообщения.
			execute(message);
		} catch (TelegramApiException e) {
			//Вывод логов при ошибке.
			log.error(e.getMessage());
		}
	}

	//Метод, отвечающий за регистрацию пользователя при нажатии на кнопку start.
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


	//Метод, отвечающий за команду /start.
	private void startCommandReceived(long chatId, String name) {

		//Ответ пользователю после нажатия на кнопку start. ":smirk_cat:" - это смайлик.
		String answer = EmojiParser.parseToUnicode("Hello, " + name + " nice to meet you!" + " :smirk_cat:");

		//Вывод логов в определенный файл.
		log.info("Replied to user " + name + " with answer: " + answer);

		sendMessage(chatId, answer);
	}

	//Метод, отвечающий за отпарвку определенного сообщения от бота пользователю.
	private void sendMessage(long chatId, String messageToSend) {
		SendMessage message = new SendMessage();
		message.setChatId(chatId);
		message.setText(messageToSend);

		//Инициализация клавиатуры для выбора сдедующего действия.
		ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

		//Лист, содержащий все кнопки клавиаутры.
		List<KeyboardRow> keyboardRows = new ArrayList<>();

		//Инициализация ряда кнопок.
		KeyboardRow row = new KeyboardRow();

		//Добавление кнопок в ряд.
		row.add("weather");
		row.add("random joke");

		//Добавление ряда в лист рядов с кнопоками.
		//Ряд, который добавляется первым, становится верхним.
		keyboardRows.add(row);

		//Создание нового ряда
		row = new KeyboardRow();
		row.add("register");
		row.add("check my data");
		row.add("delete my data");
		keyboardRows.add(row);

		//Создание клавиатуры.
		keyboardMarkup.setKeyboard(keyboardRows);

		//Привязка клавиатуры к сообщению,
		// чтобы бот отправлял ее в ответ на сообщение пользователя.
		//Одна и та же клавиатура будет отображаться после любого сообщенгия.

		//В будущем хотел бы сделать так, чтобы клавиатура отличалась в зависимости от типа сообщения.
		message.setReplyMarkup(keyboardMarkup);

		try {
			//Отпарвка сообщения.
			execute(message);
		} catch (TelegramApiException e) {
			//Вывод логов при ошибке.
			log.error(e.getMessage());
		}

	}
	//Метод, отвечающий за очистку чата при нажатии на кнопку clear.
	private void clearChatHistory(long chatId) {
		sendMessage(chatId, "Messages have been deleted");
	}
}
