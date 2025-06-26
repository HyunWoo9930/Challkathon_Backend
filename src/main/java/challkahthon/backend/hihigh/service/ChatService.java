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
    private final ChatContextService chatContextService;

    @Value("${chatgpt.model}")
    private String model;

    @Value("${chatgpt.url}")
    private String apiURL;

    @Transactional
    public Chat createChat(String username) {
        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        Chat existingChat = chatRepository.findByUser(user);
        if (existingChat != null) {
            return existingChat;
        }
        
        Chat chat = Chat.builder()
                .user(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return chatRepository.save(chat);
    }

    @Transactional
    public List<Chat> getAllChats(String username) {
        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        Chat existingChat = chatRepository.findByUser(user);
        if (existingChat == null) {
            createChat(username);
        }

        return chatRepository.findByUserOrderByUpdatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public Chat getChatById(String username) {
        User user = userRepository.findByLoginId(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        Chat chat = chatRepository.findByUser(user);

        if (chat.getUser() == null || !chat.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied: Chat does not belong to the user");
        }

        return chat;
    }

    @Transactional
    public ChatMessage sendMessage(String userMessage, String username) {
        User user = userRepository.findByLoginId(username)
            .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        Chat chat = chatRepository.findByUser(user);

        ChatMessage userChatMessage = ChatMessage.builder()
                .chat(chat)
                .role("user")
                .content(userMessage)
                .timestamp(LocalDateTime.now())
                .build();
        chatMessageRepository.save(userChatMessage);

        List<ChatMessage> allMessages = chatMessageRepository.findByChatIdOrderByTimestamp(chat.getId());
        
        List<ChatMessage> previousMessages = allMessages.stream()
                .filter(msg -> !msg.getId().equals(userChatMessage.getId()))
                .skip(Math.max(0, allMessages.size() - 21))
                .collect(Collectors.toList());

        List<Map<String, String>> messages = new ArrayList<>();

        String personalizedSystemPrompt = chatContextService.generatePersonalizedSystemPrompt(user);
        messages.add(Map.of("role", "system", "content", personalizedSystemPrompt));

        String aiPersona = chatContextService.generateAIPersona(user);
        messages.add(Map.of("role", "system", "content", aiPersona));

        if (!previousMessages.isEmpty()) {
            String chatContext = chatContextService.generateChatContext(previousMessages);
            if (!chatContext.isEmpty()) {
                messages.add(Map.of("role", "system", "content", chatContext));
            }

            if (previousMessages.size() >= 4) {
                String userPattern = chatContextService.analyzeUserPattern(allMessages, user);
                if (!userPattern.isEmpty()) {
                    messages.add(Map.of("role", "system", "content", userPattern));
                }
            }

            int recentLimit = Math.min(6, previousMessages.size());
            List<ChatMessage> recentMessages = previousMessages.subList(
                Math.max(0, previousMessages.size() - recentLimit), 
                previousMessages.size()
            );

            for (ChatMessage msg : recentMessages) {
                messages.add(Map.of(
                    "role", msg.getRole(),
                    "content", msg.getContent()
                ));
            }
        }

        messages.add(Map.of("role", "user", "content", userMessage));

        try {
            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("model", model);
            requestBodyMap.put("messages", messages);
            requestBodyMap.put("max_tokens", 2000);
            requestBodyMap.put("temperature", 0.8);
            requestBodyMap.put("top_p", 0.9);
            requestBodyMap.put("frequency_penalty", 0.1);
            requestBodyMap.put("presence_penalty", 0.1);

            String requestBody = objectMapper.writeValueAsString(requestBodyMap);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, chatGPTConfig.httpHeaders());

            String response = restTemplate.postForObject(apiURL, entity, String.class);

            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            Map<String, Object> choice = choices.get(0);
            Map<String, Object> messageMap = (Map<String, Object>) choice.get("message");
            String content = (String) messageMap.get("content");

            ChatMessage assistantMessage = ChatMessage.builder()
                    .chat(chat)
                    .role("assistant")
                    .content(content)
                    .timestamp(LocalDateTime.now())
                    .build();
            chatMessageRepository.save(assistantMessage);

            chat.setUpdatedAt(LocalDateTime.now());
            chatRepository.save(chat);

            return assistantMessage;
        } catch (Exception e) {
            log.error("Error calling ChatGPT API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get response from ChatGPT", e);
        }
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getChatMessages(String username) {
        User user = userRepository.findByLoginId(username)
            .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        Chat chat = chatRepository.findByUser(user);
        return chatMessageRepository.findByChatIdOrderByTimestamp(chat.getId());
    }

    @Transactional(readOnly = true)
    public String getChatSummary(String username) {
        User user = userRepository.findByLoginId(username)
            .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        Chat chat = chatRepository.findByUser(user);
        List<ChatMessage> messages = chatMessageRepository.findByChatIdOrderByTimestamp(chat.getId());

        if (messages.isEmpty()) {
            return "이전 대화 기록이 없습니다.";
        }

        return chatContextService.generateChatContext(messages);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserChatStats(String username) {
        User user = userRepository.findByLoginId(username)
            .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        Chat chat = chatRepository.findByUser(user);
        List<ChatMessage> messages = chatMessageRepository.findByChatIdOrderByTimestamp(chat.getId());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMessages", messages.size());
        stats.put("userMessages", messages.stream().filter(m -> "user".equals(m.getRole())).count());
        stats.put("assistantMessages", messages.stream().filter(m -> "assistant".equals(m.getRole())).count());
        
        if (!messages.isEmpty()) {
            stats.put("firstMessageDate", messages.get(0).getTimestamp());
            stats.put("lastMessageDate", messages.get(messages.size() - 1).getTimestamp());
        }

        return stats;
    }
}
