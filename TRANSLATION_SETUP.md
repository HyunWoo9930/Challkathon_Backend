# 무료 번역 서비스 설정 가이드

Google Cloud API 결제 없이도 사용할 수 있는 무료 번역 서비스들을 설정하는 방법입니다.

## 🚀 지원하는 번역 서비스

### 1. LibreTranslate (완전 무료)
- ✅ **API 키 불필요**
- ✅ **사용량 제한 없음**
- ✅ **오픈소스**
- ⚠️ 품질은 Google/Microsoft보다 다소 낮음

### 2. Microsoft Translator (무료 플랜)
- ✅ **월 2백만 문자 무료**
- ✅ **높은 번역 품질**
- ✅ **113개 언어 지원**
- ❌ API 키 필요 (무료 발급)

### 3. MyMemory (백업용)
- ✅ **API 키 불필요**
- ✅ **완전 무료**
- ⚠️ 일일 사용량 제한 있음

## 🔧 설정 방법

### 1. 기본 설정 (LibreTranslate만 사용)
아무 설정 없이도 바로 작동합니다!

```yaml
# application.yml에서 기본 설정 사용
translation:
  service:
    provider: libretranslate
```

### 2. Microsoft Translator 추가 설정 (권장)

#### 2.1 Microsoft Azure 계정 생성
1. https://azure.microsoft.com/free/ 에서 무료 계정 생성
2. Azure Portal에 로그인

#### 2.2 Translator 리소스 생성
1. Azure Portal에서 "리소스 만들기" 클릭
2. "Translator" 검색 후 선택
3. 다음 정보로 생성:
   - **구독**: 무료 구독 선택
   - **리소스 그룹**: 새로 생성
   - **지역**: Korea Central 권장
   - **이름**: 원하는 이름
   - **가격 책정 계층**: F0 (무료)

#### 2.3 API 키 확인
1. 생성된 Translator 리소스로 이동
2. "키 및 엔드포인트" 메뉴 클릭
3. **키 1** 복사

#### 2.4 환경변수 설정
```bash
# Linux/Mac
export MICROSOFT_TRANSLATE_KEY="your_api_key_here"
export MICROSOFT_TRANSLATE_REGION="koreacentral"

# Windows
set MICROSOFT_TRANSLATE_KEY="your_api_key_here"
set MICROSOFT_TRANSLATE_REGION="koreacentral"
```

또는 application.yml에 직접 설정:
```yaml
translation:
  microsoft:
    api-key: "your_api_key_here"
    region: "koreacentral"
```

## 🧪 테스트 방법

### 1. 번역 테스트 API 호출
```bash
curl "http://localhost:8080/api/career-news/test-translation?text=Hello world"
```

### 2. 응답 예시
```json
{
  "originalText": "Hello world",
  "translatedText": "안녕하세요 세계",
  "status": "success"
}
```

## 📊 번역 서비스 우선순위

현재 코드는 다음 순서로 번역을 시도합니다:

1. **Microsoft Translator** (API 키가 있는 경우)
2. **LibreTranslate** (무료 미러들)
3. **MyMemory** (백업용)

## 🔍 트러블슈팅

### LibreTranslate 연결 실패
- 여러 미러를 자동으로 시도하므로 대부분 해결됨
- 로그에서 어떤 미러가 작동하는지 확인 가능

### Microsoft Translator 오류
- API 키가 올바른지 확인
- 지역 설정이 맞는지 확인
- 무료 한도(월 2백만 문자)를 초과했는지 확인

### 번역 품질 개선
1. Microsoft Translator 사용 (가장 좋음)
2. 긴 텍스트는 문장별로 분할해서 번역
3. 전문 용어는 용어집 기능 활용

## 💰 비용 비교

| 서비스 | 무료 한도 | 초과 시 비용 | 품질 |
|--------|-----------|-------------|------|
| LibreTranslate | 무제한 | 무료 | ⭐⭐⭐ |
| Microsoft | 2백만 문자/월 | $10/백만 문자 | ⭐⭐⭐⭐⭐ |
| Google | 50만 문자/월 | $20/백만 문자 | ⭐⭐⭐⭐⭐ |

**권장**: Microsoft Translator (무료 한도가 크고 품질이 좋음)

## 🚀 추가 기능

### 사용량 모니터링
Azure Portal에서 Microsoft Translator 사용량을 실시간으로 확인할 수 있습니다.

### 다중 언어 지원
현재 영어→한국어 번역만 구현되어 있지만, 쉽게 다른 언어로 확장 가능합니다.

### 캐싱
자주 번역되는 텍스트는 DB에 캐싱해서 API 호출을 줄일 수 있습니다.
