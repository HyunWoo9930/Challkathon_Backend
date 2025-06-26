package challkahthon.backend.hihigh.service;

import challkahthon.backend.hihigh.domain.entity.ChatMessage;
import challkahthon.backend.hihigh.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatContextService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final int MAX_CONTEXT_MESSAGES = 8;
    private static final int MAX_MESSAGE_LENGTH = 150;

    public String generatePersonalizedSystemPrompt(User user) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 HiHigh 커리어 상담 전문가입니다. 한국어로 친근하면서도 전문적으로 답변하세요.\n\n");
        
        prompt.append("상담 대상자:\n");
        prompt.append("이름: ").append(user.getName() != null ? user.getName() : "미등록").append("\n");
        
        if (user.getBirthYear() != null) {
            try {
                int birthYear = Integer.parseInt(user.getBirthYear());
                int currentYear = java.time.LocalDateTime.now().getYear();
                int age = currentYear - birthYear + 1;
                prompt.append("나이: ").append(age).append("세\n");
            } catch (NumberFormatException e) {
                log.warn("Invalid birth year: {}", user.getBirthYear());
            }
        }
        
        if (user.getGender() != null) {
            prompt.append("성별: ").append(user.getGender().toString()).append("\n");
        }
        
        if (hasValue(user.getInterests())) {
            prompt.append("관심사: ").append(user.getInterests()).append("\n");
        }
        
        if (hasValue(user.getGoals())) {
            prompt.append("목표: ").append(user.getGoals()).append("\n");
        }
        
        if (hasValue(user.getDesiredOccupation())) {
            prompt.append("희망직종: ").append(user.getDesiredOccupation()).append("\n");
        }
        
        prompt.append("\n상담 방식:\n");
        prompt.append("- 위 프로필을 바탕으로 개인 맞춤형 조언 제공\n");
        prompt.append("- 구체적이고 실행 가능한 커리어 가이드\n");
        prompt.append("- 업계 트렌드 반영한 전문적 답변\n");
        prompt.append("- 격려와 동기부여 포함\n");
        prompt.append("- 이전 대화 내용 기억하여 연속성 유지\n\n");
        
        return prompt.toString();
    }

    public String generateChatContext(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        List<ChatMessage> contextMessages = messages.stream()
                .limit(Math.min(MAX_CONTEXT_MESSAGES, messages.size()))
                .collect(Collectors.toList());

        if (contextMessages.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("최근 대화:\n");
        
        for (ChatMessage message : contextMessages) {
            String role = message.getRole().equals("user") ? "사용자" : "AI";
            String content = truncateMessage(message.getContent());
            String timestamp = message.getTimestamp().format(DATE_FORMATTER);
            
            context.append(String.format("[%s] %s: %s\n", timestamp, role, content));
        }
        
        context.append("\n위 대화를 참고하여 연속성 있는 답변을 해주세요.\n\n");
        
        return context.toString();
    }

    public String analyzeUserPattern(List<ChatMessage> allMessages, User user) {
        if (allMessages == null || allMessages.size() < 4) {
            return "";
        }

        List<ChatMessage> userMessages = allMessages.stream()
                .filter(msg -> "user".equals(msg.getRole()))
                .collect(Collectors.toList());

        if (userMessages.size() < 2) {
            return "";
        }

        StringBuilder pattern = new StringBuilder();
        pattern.append("상담 이력:\n");
        
        String[] careerKeywords = {
            "취업", "이직", "면접", "자소서", "포트폴리오", "스킬", "경력", "성장", 
            "개발", "디자인", "기획", "마케팅", "영업", "회사", "업무", "프로젝트"
        };
        
        int totalMentions = 0;
        for (String keyword : careerKeywords) {
            long mentions = userMessages.stream()
                    .mapToLong(msg -> countKeywordMentions(msg.getContent(), keyword))
                    .sum();
            if (mentions > 0) {
                totalMentions++;
            }
        }
        
        if (totalMentions > 0) {
            pattern.append("- 이전 ").append(userMessages.size()).append("회 상담에서 ")
                   .append(totalMentions).append("개 커리어 관련 주제 논의\n");
        }
        
        if (userMessages.size() >= 3) {
            pattern.append("- 지속적인 커리어 고민 상담 중 (").append(userMessages.size()).append("회차)\n");
        }
        
        pattern.append("- 심화된 맞춤 조언 제공 필요\n\n");
        
        return pattern.toString();
    }

    public String generateAIPersona(User user) {
        StringBuilder persona = new StringBuilder();
        
        persona.append("상담 스타일:\n");
        
        if (user.getBirthYear() != null) {
            try {
                int age = java.time.LocalDateTime.now().getYear() - Integer.parseInt(user.getBirthYear()) + 1;
                if (age <= 25) {
                    persona.append("- 친근하고 격려하는 멘토 톤, 실무 경험 공유\n");
                } else if (age <= 35) {
                    persona.append("- 동료 같은 전문가 톤, 구체적 커리어 전략\n");
                } else {
                    persona.append("- 경험 있는 조언자 톤, 폭넓은 관점 제공\n");
                }
            } catch (NumberFormatException e) {
                persona.append("- 균형 잡힌 전문가 톤 유지\n");
            }
        }
        
        if (hasValue(user.getDesiredOccupation())) {
            persona.append("- ").append(user.getDesiredOccupation())
                   .append(" 분야 전문 지식 활용\n");
        }
        
        persona.append("- 긍정적이고 실행 가능한 솔루션 제공\n");
        persona.append("- 사용자 강점 발견하고 발전 방향 제시\n\n");
        
        return persona.toString();
    }

    private String truncateMessage(String message) {
        if (message == null) return "";
        if (message.length() <= MAX_MESSAGE_LENGTH) return message;
        return message.substring(0, MAX_MESSAGE_LENGTH) + "...";
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private long countKeywordMentions(String content, String keyword) {
        if (content == null || keyword == null) return 0;
        return content.toLowerCase().split(keyword.toLowerCase(), -1).length - 1;
    }
}
