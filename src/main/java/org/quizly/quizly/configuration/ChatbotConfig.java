package org.quizly.quizly.configuration;

import org.quizly.quizly.external.openai.property.OpenAiProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatbotConfig {

    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiProperty openAiProperty) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .apiKey(openAiProperty.getKey())
            .model(openAiProperty.getModel())
            .reasoningEffort(openAiProperty.getReasoningEffort())
            .build();

        return OpenAiChatModel.builder()
            .options(options)
            .build();
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }
}
