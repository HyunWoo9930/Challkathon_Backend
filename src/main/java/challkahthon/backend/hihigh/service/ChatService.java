package challkahthon.backend.hihigh.service;

import challkahthon.backend.hihigh.domain.entity.Chat;
import challkahthon.backend.hihigh.domain.entity.ChatMessage;
import challkahthon.backend.hihigh.domain.entity.User;
import challkahthon.backend.hihigh.dto.Message;
import challkahthon.backend.hihigh.repository.ChatMessageRepository;
import challkahthon.backend.hihigh.repository.ChatRepository;
import challkahthon.backend.hihigh.repository.UserRepository;
import challkahthon.backend.hihigh.utils.ChatGPTUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChatGPTUtils chatGPTUtils;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final challkahthon.backend.hihigh.config.ChatGPTConfig chatGPTConfig;

    @Value("${chatgpt.model}")
    private String model;

    @Value("${chatgpt.url}")
    private String apiURL;

    /**
     * Create a new chat conversation
     * @param username The username of the user creating the chat
     * @return The created chat
     */
    @Transactional
    public Chat createChat(String username) {
        // Find the user by username (loginId)
        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        // Check if the user already has a chat
        Chat existingChat = chatRepository.findByUser(user);
        if (existingChat != null) {
            return existingChat;
        }
        // Create a new chat for the user
        Chat chat = Chat.builder()
                .user(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return chatRepository.save(chat);
    }

    /**
     * Get all chats for a user
     * @param username The username of the user
     * @return List of all chats for the user
     */
    @Transactional
    public List<Chat> getAllChats(String username) {
        // Find the user by username (loginId)
        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        // Check if the user has a chat
        Chat existingChat = chatRepository.findByUser(user);
        if (existingChat == null) {
            // Create a new chat for the user if they don't have one
            createChat(username);
        }

        // Find all chats for the user
        return chatRepository.findByUserOrderByUpdatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public Chat getChatById(String username) {
        // Find the user by username (loginId)
        User user = userRepository.findByLoginId(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        Chat chat = chatRepository.findByUser(user);

        // Check if the chat belongs to the user
        if (chat.getUser() == null || !chat.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied: Chat does not belong to the user");
        }

        return chat;
    }

    /**
     * Send a message to a chat and get a response from ChatGPT
     * @param chatId The ID of the chat
     * @param userMessage The message from the user
     * @param username The username of the user
     * @return The response from ChatGPT
     */
    @Transactional
    public ChatMessage sendMessage(String userMessage, String username) {
        // Find the user by username (loginId)
        User user = userRepository.findByLoginId(username)
            .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        Chat chat = chatRepository.findByUser(user);

        // Save user message
        ChatMessage userChatMessage = ChatMessage.builder()
                .chat(chat)
                .role("user")
                .content(userMessage)
                .timestamp(LocalDateTime.now())
                .build();
        chatMessageRepository.save(userChatMessage);

        // Get all messages for this chat
        List<ChatMessage> chatMessages = chatMessageRepository.findByChatIdOrderByTimestamp(chat.getId());

        // Convert to format expected by ChatGPT API
        List<Map<String, String>> messages = new ArrayList<>();

        // Add system message first
        messages.add(Map.of(
            "role", "system",
            "content", "You are a helpful assistant. Respond in Korean."
        ));

        // Add all previous messages
        for (ChatMessage message : chatMessages) {
            messages.add(Map.of(
                "role", message.getRole(),
                "content", message.getContent()
            ));
        }

        try {
            // Prepare request body
            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("model", model);
            requestBodyMap.put("messages", messages);
            requestBodyMap.put("max_tokens", 1000);
            requestBodyMap.put("temperature", 1.0);

            // Convert to JSON
            String requestBody = objectMapper.writeValueAsString(requestBodyMap);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, chatGPTConfig.httpHeaders());

            // Call API
            String response = restTemplate.postForObject(apiURL, entity, String.class);

            // Parse response
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            Map<String, Object> choice = choices.get(0);
            Map<String, Object> messageMap = (Map<String, Object>) choice.get("message");
            String content = (String) messageMap.get("content");

            // Save assistant message
            ChatMessage assistantMessage = ChatMessage.builder()
                    .chat(chat)
                    .role("assistant")
                    .content(content)
                    .timestamp(LocalDateTime.now())
                    .build();
            chatMessageRepository.save(assistantMessage);

            // Update chat's updated time
            chat.setUpdatedAt(LocalDateTime.now());
            chatRepository.save(chat);

            return assistantMessage;
        } catch (Exception e) {
            log.error("Error calling ChatGPT API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get response from ChatGPT", e);
        }
    }

    /**
     * Get all messages for a chat
     * @param chatId The ID of the chat
     * @return List of all messages for the chat
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getChatMessages(String username) {

        // Find the user by username (loginId)
        User user = userRepository.findByLoginId(username)
            .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        Chat chat = chatRepository.findByUser(user);

        return chatMessageRepository.findByChatIdOrderByTimestamp(chat.getId());
    }
}
