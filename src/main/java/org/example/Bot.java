package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;

public class Bot extends TelegramLongPollingBot {
    private final String BOT_TOKEN = "<your TG-bot-TOKEN>";
    private final String BOT_NAME = "<your bot-name>";
    private final SpeechToText speechToText;
    public String getBotToken() {
        return BOT_TOKEN;
    }
    @Override
    public String getBotUsername() {
        return BOT_NAME;
    }

    public Bot()
    {
        speechToText = new SpeechToText();
    }

    private void sendMessage(long chatId, String message)
    {
        try {
            execute(SendMessage.builder()
                    .text(message)
                    .chatId(chatId)
                    .build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendReplyToMessage(long chatId, String message, int messageId)
    {
      try {
          execute(SendMessage.builder()
                  .text("Расшифровка:\n" + message)
                  .chatId(chatId)
                  .replyToMessageId(messageId)
                  .build());
      } catch (TelegramApiException e) {
          throw new RuntimeException(e);
      }
    }

    @Override
    public void onUpdateReceived(Update update)
    {
        if(update.hasMessage())
        {
            if(update.getMessage().hasText()) textMessageHandler(update.getMessage());
            else if(update.getMessage().hasVoice()) voiceMessageHandler(update.getMessage());
        }
    }
    private InputStream getInputStreamFromVoice(Voice voice)
    {
        GetFile getFile = new GetFile();
        getFile.setFileId(voice.getFileId());
        String filePath = null;
        try {
            filePath = execute(getFile).getFilePath();
            return downloadFileAsStream(filePath);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
    private void voiceMessageHandler(Message message)
    {
        String textFromSpeech;
        if(message.getVoice().getDuration() < 30)
        {
            textFromSpeech = speechToText.synchronousRecognition(getInputStreamFromVoice(message.getVoice()));
            if (textFromSpeech != null)
            {
                sendReplyToMessage(message.getChatId(), textFromSpeech, message.getMessageId());
            }
        }
        else if(message.getVoice().getDuration() >=30 && message.getVoice().getDuration()<180)
        {
            textFromSpeech = speechToText.asynchronousRecognition(getInputStreamFromVoice(message.getVoice()));
            sendReplyToMessage(message.getChatId(),textFromSpeech, message.getMessageId());
        }
    }
    private void textMessageHandler(Message message) {
        switch (message.getText())
        {
            case "/start" -> sendMessage(message.getChatId(), "Добро пожаловать в бота\nОн создан, чтобы переводить голосовые сообщения в текст\nПросто отравьте или перешлите в этот чат голосовое сообщение");
        }
    }
}
