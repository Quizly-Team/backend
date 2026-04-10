package org.quizly.quizly.configuration;

import org.quizly.quizly.external.openai.property.OpenAiProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatbotConfig {

    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiProperty openAiProperty) {
        OpenAiApi openAiApi = OpenAiApi.builder()
            .apiKey(openAiProperty.getKey())
            .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .model(openAiProperty.getModel())
            .temperature(0.7)
            .build();

        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(options)
            .build();
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }
}
