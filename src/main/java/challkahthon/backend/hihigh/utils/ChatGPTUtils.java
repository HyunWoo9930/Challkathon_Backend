package challkahthon.backend.hihigh.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import challkahthon.backend.hihigh.config.ChatGPTConfig;
import challkahthon.backend.hihigh.dto.ChatGPTResponseDTO;

@Component
public class ChatGPTUtils {

	private final ChatGPTConfig chatGPTConfig;
	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${chatgpt.model}")
	private String model;

	@Value("${chatgpt.url}")
	private String apiURL;

	public ChatGPTUtils(ChatGPTConfig chatGPTConfig) {
		this.chatGPTConfig = chatGPTConfig;
	}

	public String callChatGPT(String systemPrompt, String userPrompt) {
		try {
			Map<String, String> systemMessage = Map.of(
				"role", "system",
				"content", systemPrompt
			);
			Map<String, String> userMessage = Map.of(
				"role", "user",
				"content", userPrompt
			);
			List<Map<String, String>> messages = List.of(systemMessage, userMessage);

			Map<String, Object> requestBodyMap = new HashMap<>();
			requestBodyMap.put("model", model);
			requestBodyMap.put("messages", messages);
			requestBodyMap.put("max_tokens", 1000);
			requestBodyMap.put("temperature", 1.0);

			String requestBody = objectMapper.writeValueAsString(requestBodyMap);
			HttpEntity<String> entity = new HttpEntity<>(requestBody, chatGPTConfig.httpHeaders());

			ChatGPTResponseDTO response = restTemplate.postForObject(apiURL, entity, ChatGPTResponseDTO.class);

			return response.getChoices().get(0).getMessage().getContent();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
