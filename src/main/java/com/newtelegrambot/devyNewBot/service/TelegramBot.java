package com.newtelegrambot.devyNewBot.service;

import com.newtelegrambot.devyNewBot.config.BotConfig;
import com.newtelegrambot.devyNewBot.models.Advertisement;
import com.newtelegrambot.devyNewBot.models.User;
import com.newtelegrambot.devyNewBot.repositories.AdvertisementRepository;
import com.newtelegrambot.devyNewBot.repositories.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
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
import java.util.Objects;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    final BotConfig config;
    private final UserRepository userRepository;
    private final AdvertisementRepository advertisementRepository;

    static final String noButtonId = "NO_BUTTON";
    static final String yesButtonId = "YES_BUTTON";

    //Константа для команды /help: выводить пользователю все возможные команды.
    @Value("${help.text}")
    static final String HELP_TEXT =

            """
                     This bot is created to help pupils save their homework, check it later and then delete.

                     You can execute commands from the main manu on the left or by typing a command:

                     Type /start to see a welcome message

                     Type /help to see this message again)

                     Type /mydata to see data stored about yourself
                     
                     Type /deletedata to delete info about you from bot
                     
                     Type /settings to set your preferences.
                                       
                     Type /register to register yourself in bot
                    """;


    //Создание бота.
    //Здесь определяются команды для использования бота.
    public TelegramBot(BotConfig config, UserRepository userRepository, AdvertisementRepository advertisementRepository) {
        this.config = config;
        this.userRepository = userRepository;
        this.advertisementRepository = advertisementRepository;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/help", "info about the bot"));
        listOfCommands.add(new BotCommand("/mydata", "get your data stored"));
        listOfCommands.add(new BotCommand("/deletedata", "delete my data (bot history will be deleted with it)"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        listOfCommands.add(new BotCommand("/register", "you can register yourself in bot"));

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

            if (message.contains("/send") && Objects.equals(config.getOwnerId(), chatId)) {
                //Выбираем это сообщение, которое следует за /send и пробелом после него.
                var textToSend = EmojiParser.parseToUnicode(message.substring(message.indexOf(" ")));
                var users = userRepository.findAll();

                //В цикле отправляем нужное нам сообщение каждому пользователю.
                for (User user : users) {
                    prepareAndSendMessage(user.getChatId(), textToSend);
                }
            } else {
                switch (message) {
                    case "/start":
                        registerUser(update.getMessage());
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;

                    case "/help":
                        prepareAndSendMessage(chatId, HELP_TEXT);
                        break;

                    case "/mydata":
                        String userData = getUserData(chatId);
                        prepareAndSendMessage(chatId, userData);
                        break;

                    case "/deletedata":
                        deleteUserData(chatId);
                        //На доработке
//                        deleteChatHistory(chatId, update);
                        prepareAndSendMessage(chatId, "All your data has been deleted.\n" +
                                "If you wanna register again - type /start");
                        break;

                    case "/register":
                        register(chatId);
                        break;

                    default:
                        prepareAndSendMessage(chatId, "Command wasn't recognized");
                }
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
                executeEditMessageText(text, chatId, messageId);
            } else if (callbackData.equals(noButtonId)) {
                String text = "You pressed NO button";
                executeEditMessageText(text, chatId, messageId);
            }
        }
    }

    private void register(long chatId) {
        //Создание сообщения.
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Do you really want to register?");

        //Создание клавиатуры с кнопками, которая будет появляться под сообщениями бота.
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        //Создание ряда клавиатуры.
        List<List<InlineKeyboardButton>> rowsInline = getInlineRowsForKeyboard();

        //Присваиваем ряд кнопок клавиатуре.
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        //Добавляем клавиатуру к сообщению бота.
        message.setReplyMarkup(inlineKeyboardMarkup);

        //Отпарвка сообщения пользователю.
        executeMessage(message);
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
        log.info("Replied to user {} with answer: {}", name, answer);

        sendMessage(chatId, answer);
    }

    //Метод, отвечающий за отпарвку определенного сообщения от бота пользователю.
    private void sendMessage(long chatId, String messageToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageToSend);

        //Создание клавиатуры для выбора сдедующего действия.
//        ReplyKeyboardMarkup keyboardMarkup = getReplyKeyboardMarkup();

        //Привязка клавиатуры к сообщению,
        // чтобы бот показывал ее в ответ на сообщение пользователя.
//        message.setReplyMarkup(keyboardMarkup);

        //Отпарвка сообщения пользователю.
        executeMessage(message);

    }

    //Метод для создания клавиатуры с кнопками (рядом с полем ввода сообщения пользователем)
    private static ReplyKeyboardMarkup getReplyKeyboardMarkup() {
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
        return keyboardMarkup;
    }

    private static List<List<InlineKeyboardButton>> getInlineRowsForKeyboard() {
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
        return rowsInline;
    }

    //Метод для отправки EditMessageText пользователю.
    private void executeEditMessageText(String text, long chatId, long messageId) {
        //Метод позволяет изменить сообщение при знании ID сообщения.
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setText(text);
        editMessageText.setMessageId((int) messageId);

        try {
            //Отпарвка сообщения.
            execute(editMessageText);
        } catch (TelegramApiException e) {
            //Вывод логов при ошибке.
            log.error(e.getMessage());
        }
    }

    //Метод для отправки Message пользователю.
    private void executeMessage(SendMessage message) {
        try {
            //Отпарвка сообщения.
            execute(message);
        } catch (TelegramApiException e) {
            //Вывод логов при ошибке.
            log.error(e.getMessage());
        }
    }

    //Метод, создающий сообщение для дальнейше отправки пользователю.
    private void prepareAndSendMessage(long chatId, String messageToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageToSend);
        executeMessage(message);
    }

    private String getUserData(long chatId) {

        User user = userRepository.findById(chatId).orElse(null);
        StringBuilder userData;
        if (user == null) {
            return "No data found for you";
        } else {
            userData = new StringBuilder();
            userData.append("Your Data:\n");
            userData.append("Chat ID: ").append(user.getChatId()).append("\n");
            userData.append("First Name: ").append(user.getFirstname()).append("\n");
            userData.append("Last Name: ").append(user.getLastname()).append("\n");
            userData.append("Username: ").append(user.getUsername()).append("\n");
            userData.append("Date Of Registration: ").append(user.getRegisterAt()).append("\n");
        }


        return userData.toString();
    }

    //Метод для удаления пользователя из базы данных.
    private void deleteUserData(long chatId) {
        userRepository.deleteById(chatId);
    }

    //Метод для очистки чата с пользователем, который решил удалить свои данные из базы данных бота.
    //Нужно доработать: пока что удаляется только последнее сообщение.
//    private void deleteChatHistory(long chatId, Update update) {
//        long messageId = update.getMessage().getMessageId();
//        while (messageId > 0) {
//            DeleteMessage deleteMessage = new DeleteMessage();
//            deleteMessage.setChatId(chatId);
//            deleteMessage.setMessageId((int) messageId);
//            try {
//                execute(deleteMessage);
//            }catch (TelegramApiException e) {
//                log.error(e.getMessage());
//                break;
//            }
//            messageId=update.getMessage().getMessageId();
//        }
//    }

    //cron - это сервис, который в определенное время отправляет нечто(из Linux)
    //Имеет 6 параметров: секуны, минуты, часы, дата, месяц, день недели
    //Метод, отвечающий за отправку рекламных сообщений.
//    @Scheduled(cron = "${cron.scheduler}")
//    private void sendAdvertisements(){
//
//        var ads = advertisementRepository.findAll();
//        var users = userRepository.findAll();
//
//        //Отпарвка рекламных сообщений всем пользователям через цикл.
//        for (Advertisement ad : ads) {
//            for (User user : users) {
//                prepareAndSendMessage(user.getChatId(), ad.getAdvertisementText());
//            }
//
//        }
//    }

}
