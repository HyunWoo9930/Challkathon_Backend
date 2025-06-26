# HiHigh - AI 기반 커리어 뉴스 플랫폼

AI 채팅과 개인화된 뉴스 큐레이션을 제공하는 커리어 상담 서비스입니다.

## 🎯 서비스 소개

- **AI 채팅**: ChatGPT 기반 커리어 상담
- **개인화 뉴스**: 사용자 관심사에 맞는 커리어 뉴스 크롤링 및 제공
- **사용자 관리**: Google OAuth 로그인 및 프로필 관리

## 👥 개발자

- **Backend**: Spring Boot 기반 REST API 개발
- **이름**: 오현우
- **연락처**: hw62459930@gmail.com

## 🛠️ 기술 스택

### Backend
- **Framework**: Spring Boot 3.5.3
- **Database**: MySQL + JPA
- **Authentication**: JWT + OAuth2 (Google)
- **AI**: OpenAI ChatGPT API
- **Web Crawling**: Jsoup
- **Build**: Gradle

### 의존성
- Spring Security
- SpringDoc (Swagger)
- Lombok
- Jackson

## 📁 프로젝트 구조

```
src/main/java/challkahthon/backend/hihigh/
├── config/          # 설정 클래스
├── controller/      # REST API 컨트롤러
├── service/         # 비즈니스 로직
├── repository/      # 데이터 접근 계층
├── domain/          # 엔티티 및 DTO
├── jwt/             # JWT 인증
└── utils/           # 유틸리티 클래스
```

## ⚡ 주요 기능

### 1. 사용자 관리
- Google OAuth2 로그인
- JWT 토큰 기반 인증
- 사용자 프로필 및 관심사 관리

### 2. AI 채팅
- ChatGPT API 연동
- 개인화된 대화 컨텍스트 관리
- 사용자 맞춤형 응답

### 3. 뉴스 크롤링
- NewsAPI, GNews API 연동
- RSS 피드 크롤링
- 사용자 관심사 기반 필터링

### 4. 관리자 기능
- 뉴스 관리
- 사용자 통계
- 시스템 모니터링