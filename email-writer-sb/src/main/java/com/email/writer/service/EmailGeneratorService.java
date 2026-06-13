package com.email.writer.service;

import com.email.writer.dto.EmailRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    /**
     * STEPS:
     * 1. Build the prompt
     * 2. Craft a request
     * 3. Do request and get Response
     * 4. Return Response
     * @param emailRequest
     * @return
     */

    public String generateEmailReply(EmailRequest emailRequest){

        //1. Build the prompt
        String prompt=buildPrompt(emailRequest);

        //2. Craft a request
        Map<String, Object> requestBody=Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        //3. Do request and get Response
        String response = webClient.post()
                .uri(geminiApiUrl + geminiApiKey)
                .header("Content-Type", "application/json")
                .body(Mono.just(requestBody), Map.class)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        //4. Extract Response and Return
        return extractResponseContent(response);
    }

    private String extractResponseContent(String response) {
        try {
            ObjectMapper mapper=new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);

            return rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text").asString();
        } catch (Exception e) {
            return "Error processing the request: " + e.getMessage();
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt=new StringBuilder();

        prompt.append("Generate a professional email reply for the following email content. Please don't generate a subject line ");
        if(emailRequest.getTone()!=null && !emailRequest.getTone().isEmpty()){
            prompt.append("Use a ").append(emailRequest.getTone()).append(" tone.");
        }
        prompt.append("\nOriginal Email Content: \n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}
