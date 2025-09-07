# Backend-Guide-Bot


---

# 🖥️ 서버 설명

- LGCMS의 FAQ를 기반으로 답변해주는 RAG(Retrieval-Augmented Generation) 기반의 AI 챗봇 서버입니다

- 자주 묻는 질문(FAQ) 데이터를 Vector DB에 저장하여 사용자의 질문과 가장 유사한 문서를 효율적으로 검색합니다. 검색된 문서를 기반으로 LLM(Large Language Model)이 자연스러운 답변을 생성하여 사용자가 서비스 이용을 편리하게 하는 것을 목표로 합니다.
  
- 패키지 구조
```
lgcns-final-lgcms-backend-guide-bot/
    ├── src/
    │   ├── main/
    │   │   ├── java/
    │   │   │   └── com/
    │   │   │       └── lgcms/
    │   │   │           └── backendguidebot/
    │   │   │               ├── BackendGuideBotApplication.java
    │   │   │               ├── advice/
    │   │   │               │   └── GuideControllerAdvice.java
    │   │   │               ├── api/
    │   │   │               │   └── open/
    │   │   │               │       ├── ChatController.java
    │   │   │               │       └── VectorStoreController.java
    │   │   │               ├── common/
    │   │   │               │   ├── annotation/
    │   │   │               │   │   └── DistributedLock.java
    │   │   │               │   ├── aspect/
    │   │   │               │   │   ├── DistributedLockAspect.java
    │   │   │               │   │   └── TokenMetricsAspect.java
    │   │   │               │   └── dto/
    │   │   │               │       ├── BaseResponse.java
    │   │   │               │       └── exception/
    │   │   │               │           ├── BaseException.java
    │   │   │               │           ├── DataError.java
    │   │   │               │           ├── ErrorCode.java
    │   │   │               │           ├── ErrorCodeInterface.java
    │   │   │               │           ├── LockError.java
    │   │   │               │           └── QnaError.java
    │   │   │               ├── config/
    │   │   │               │   └── redis/
    │   │   │               │       ├── RedissonConfig.java
    │   │   │               │       └── RedissonProperties.java
    │   │   │               ├── domain/
    │   │   │               │   ├── advisor/
    │   │   │               │   │   ├── QueryExpansionAdvisor.java
    │   │   │               │   │   └── ReRankAdvisor.java
    │   │   │               │   ├── dto/
    │   │   │               │   │   └── ChatResponse.java
    │   │   │               │   └── service/
    │   │   │               │       ├── ai/
    │   │   │               │       │   └── local/
    │   │   │               │       │       └── ChatService.java
    │   │   │               │       └── vectorDb/
    │   │   │               │           ├── VectorStoreInitRunner.java
    │   │   │               │           ├── VectorStoreInitService.java
    │   │   │               │           └── VectorStoreService.java
    │   │   │               └── remote/
    │   │   │                   └── core/
    │   │   │                       ├── RemoteFaqService.java
    │   │   │                       └── dto/
    │   │   │                           └── FaqResponse.java
    │   │   └── resources/
    │   │       ├── application-local.yaml
    │   │       ├── application-test.yaml
    │   │       ├── application.yaml
    │   │       └── prompts/
    │   │           └── rag-prompt.st
    │   └── test/
    │       └── java/
    │           └── com/
    │               └── lgcms/
    │                   └── backendguidebot/
    │                       └── BackendGuideBotApplicationTests.java
    └── .github/
        ├── pull_request_template.md
        └── workflows/
            ├── githubCD.yaml
            └── githubCI.yaml

```

---

# **👨🏻‍💻** 담당자

| 이름 | 역할 |
| --- | --- |
| 김선호 | 가이드 봇 개발 |
| 이재원 | CI/CD, 모니터링 |

---

# 🛠️ 기술 스택

### Languages
<img src="https://img.shields.io/badge/java-007396?style=for-the-badge&logo=java&logoColor=white">

### Framework
<img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
<img src="https://img.shields.io/badge/Spring_AI-6DB33F?style=for-the-badge&logo=spring&logoColor=white">

### Middleware



### Database
<img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white">
<img src="https://img.shields.io/badge/pgvector-4169E1?style=for-the-badge&logo=postgresql&logoColor=white">

---

# 📌 기능

- FAQ 기반 질의응답

시퀀스: 사용자 질문 → Vector DB 유사도 검색 → 검색된 문서 기반 프롬프트 생성 → LLM 답변 생성 → 사용자에게 답변

설명: 사용자의 질문 의도를 파악하여 PostgreSQL(pgvector)에 저장된 FAQ 벡터 데이터와 비교하고, 가장 유사한 문서를 찾아 LLM에게 전달하여 최종 답변을 생성합니다.

- Vector DB 데이터 관리

시퀀스: 서버 시작 시 데이터 유무 확인 → Core 서버에서 FAQ 데이터 호출 → 데이터 임베딩 → Vector DB 저장

설명: 서버가 시작될 때 Vector DB가 비어있는 경우에만 Core 서버로부터 최신 FAQ 데이터를 가져와 임베딩하고 Vector DB에 저장합니다. 이 과정은 분산 락을 통해 여러 인스턴스가 동시에 실행되는 것을 방지합니다.

- LLM 토큰 사용량 모니터링

시퀀스: LLM API 호출 → AOP를 통해 응답 가로채기 → 토큰 사용량 추출 → Prometheus로 메트릭 전송

설명: Spring AOP를 활용하여 LLM API 호출의 응답을 가로채고, 사용된 토큰 정보를 추출합니다. 이 데이터는 모니터링 도구(Prometheus)로 전송되어 AI 모델 사용 비용을 추적하고 관리하는 데 사용됩니다.

---

# **📜** 주요 기능
- 질문 확장(Query Expansion) 기능: 사용자의 질문이 모호하더라도, LLM을 통해 질문을 여러 개로 확장하여 검색 정확도를 높입니다.

- 답변 재정렬(Re-ranking) 기능: Vector DB에서 검색된 여러 문서 중 가장 질문과 관련성이 높은 문서를 다시 정렬하여 답변의 품질을 향상시킵니다.

---

# ⚡ 트러블슈팅

---

# 💡 느낀점
