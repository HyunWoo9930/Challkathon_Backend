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

	/**
	 * ChatGPT에게 system + user prompt로 요청하고, 응답 content만 String으로 반환
	 */
	public String callChatGPT(String systemPrompt, String userPrompt) {
		try {
			// 메시지 구성
			Map<String, String> systemMessage = Map.of(
				"role", "system",
				"content", systemPrompt
			);
			Map<String, String> userMessage = Map.of(
				"role", "user",
				"content", userPrompt
			);
			List<Map<String, String>> messages = List.of(systemMessage, userMessage);

			// 요청 본문 구성
			Map<String, Object> requestBodyMap = new HashMap<>();
			requestBodyMap.put("model", model);
			requestBodyMap.put("messages", messages);
			requestBodyMap.put("max_tokens", 1000);
			requestBodyMap.put("temperature", 1.0);

			// JSON 문자열로 변환
			String requestBody = objectMapper.writeValueAsString(requestBodyMap);
			HttpEntity<String> entity = new HttpEntity<>(requestBody, chatGPTConfig.httpHeaders());

			// API 호출
			ChatGPTResponseDTO response = restTemplate.postForObject(apiURL, entity, ChatGPTResponseDTO.class);

			// 응답 파싱
			return response.getChoices().get(0).getMessage().getContent();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
