package com.newtelegrambot.devyNewBot.service;

import com.newtelegrambot.devyNewBot.config.BotConfig;
import com.newtelegrambot.devyNewBot.models.Answer;
import com.newtelegrambot.devyNewBot.models.Question;
import com.newtelegrambot.devyNewBot.models.Response;
import com.newtelegrambot.devyNewBot.models.User;
import com.newtelegrambot.devyNewBot.repositories.*;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig config;

    private final UserRepository userRepository;
    private final AdvertisementRepository advertisementRepository;
    private final QuestionRepository questionRepository;
    private final AnswersRepository answersRepository;
    private final ResponseRepository responseRepository;

    private final Map<Long, String> questionsFromDatabase = new HashMap<>();
    private final Map<Long, String> answersFromDatabase = new HashMap<>();

    private String surveyState = ReadyForQuestion;
    private long questionNumber = 1;

    //Константы для survey.
    static final String WaitingForAnswer = "WAITING_FOR_ANSWER";
    static final String ReadyForQuestion = "READY_FOR_QUESTION";

    //Констаны для идентификаторов кнопок.
    static final String noButtonId = "NO_BUTTON";
    static final String yesButtonId = "YES_BUTTON";

    //Константа для команды /help: выводить пользователю все возможные команды.
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
                     
                     Type /survey to take a small poll about ...
                    """;


    //Создание бота.
    //Здесь определяются команды для использования бота.
    public TelegramBot(BotConfig config, UserRepository userRepository, AdvertisementRepository advertisementRepository, QuestionRepository questionRepository, AnswersRepository answersRepository, ResponseRepository responseRepository) {
        this.config = config;
        this.userRepository = userRepository;
        this.advertisementRepository = advertisementRepository;
        this.questionRepository = questionRepository;
        this.answersRepository = answersRepository;
        this.responseRepository = responseRepository;

        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/help", "info about the bot"));
        listOfCommands.add(new BotCommand("/survey", "take a small poll about ..."));
        listOfCommands.add(new BotCommand("/mydata", "get your data stored"));
        listOfCommands.add(new BotCommand("/deletedata", "delete my data (bot history will be deleted with it)"));

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

                    case "/survey":
                        startSurvey(chatId);
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


                    default:
                        prepareAndSendMessage(chatId, "Command wasn't recognized");
                }
            }
        }
        //Проверка на то, нет ли в update ID кнопки.
        //В отличие от первой проверки здесь нет message,
        //поэтому используем CallBackQuery вместо него.
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
            } else {
                handleAnswer(chatId, callbackData, messageId);
            }
        }
    }

    private void startSurvey(Long chatId) {

        var questions = questionRepository.findAll();
        var answers = answersRepository.findAll();

        getQuestionsFromDatabase(questions);
        getAnswerOptionsFromDatabase(answers);


        askQuestion(chatId, questionsFromDatabase.get(questionNumber));

    }


    private void getQuestionsFromDatabase(Iterable<Question> questions) {
        for (Question question : questions) {
            questionsFromDatabase.put(question.getId(), question.getQuestionText());
        }
    }

    private void getAnswerOptionsFromDatabase(Iterable<Answer> answers) {
        for (Answer answer : answers) {
            answersFromDatabase.put(answer.getId(), answer.getAnswerText());
        }
    }

    private void askQuestion(long chatId, String question) {
        //Инициализация сообщения.
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(question);
        //Создание клавиатуры с ответами из базы данных.
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = getInlineRowsForKeyboard(answersFromDatabase);
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        message.setReplyMarkup(inlineKeyboardMarkup);


        if (surveyState.equals(ReadyForQuestion)) {
            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }
            surveyState = WaitingForAnswer;
        }


    }

    private void handleAnswer(long chatId, String callbackData, long messageId) {

        switch (callbackData) {
            case "ANSWER_Good", "ANSWER_Bad", "ANSWER_Average" -> {
                Response response = new Response();
                response.setAnswer(callbackData.substring(7));
                response.setQuestion(questionsFromDatabase.get(questionNumber));
                responseRepository.save(response);
            }
        }
        surveyState = ReadyForQuestion;

        askNextQuestion(chatId);
    }

    private void askNextQuestion(long chatId) {
        if (questionNumber <= questionsFromDatabase.size()) {
            questionNumber++;
            askQuestion(chatId, questionsFromDatabase.get(questionNumber));
        }
    }


    private static List<List<InlineKeyboardButton>> getInlineRowsForKeyboard(Map<Long, String> answersFromDatabase) {
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (int i = 1; i <= answersFromDatabase.size(); i++) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            var answer = answersFromDatabase.get((long) i);

            var button = new InlineKeyboardButton();
            button.setText(answer);
            button.setCallbackData("ANSWER_" + answer);
            rowInline.add(button);
            rowsInline.add(rowInline);
        }
        return rowsInline;
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

        //Отправка сообщения пользователю.
        executeMessage(message);

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
