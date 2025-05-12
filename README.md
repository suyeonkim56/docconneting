## 🩺 시간과 공간의 제약이 없는 의사와의 채팅 & 게시글 서비스

## 1. 프로젝트 개요

개발 기간 : 2025.04.01 - 2025.05.06

![Image](https://github.com/user-attachments/assets/2852290e-c2e6-45b0-9f3f-92cd43b0c206)

**Docconneting은, 의료 상담을 더 가깝고 편리하게 만드는 서비스입니다. 🩺**

**시간과 장소에 구애받지 않고**, 언제 어디서든 의사와 채팅하거나 게시글로 건강 고민을 나눌 수 있는

**비대면 건강 상담 플랫폼으로** 단순한 정보 검색을 넘어, **실제 의료진과의 1:1 실시간 채팅**과 **게시글을 통한 질문, 답변 방식의 상담**을 통해 내 건강을 보다 정확하게, 편리하게 관리할 수 있습니다.

당신의 건강, 이제는 Docconneting에서 시작하세요 👨‍⚕️❤️

## 2. 프로젝트 핵심 목표


1️⃣ **운영 및 배포 효율화**

- GitHub Actions 기반의 CI/CD 파이프라인 구축으로 배포 자동화 실현 - Docker 기반 컨테이너화를 통해 환경 이식성 및 확장성 강화

2️⃣ **FCM을 적용한 비동기 알림 처리**

- sendEachForMulticast메서드를 사용하여 100건 단위로 Batch 처리하여 안정적으로 대량의 알림을 전송
- 알림 전송 시 @Async 비동기 처리를 적용해 서버 응답 지연 없이 알림 발송 수행
- 알람 전송 히스토리의 경우 Bulk Insert 방식으로 대량 저장하여 성능 저하 없이 처리

3️⃣ **웹소켓 기반의 STOMP를 활용한 1:1 채팅 구현**

- STOMP를 통해서 실시간 양방향 채팅 시스템 구현
- RabbitMQ를 통하여 분산 환경에서의 안정적인 메시지 처리

4️⃣ **쿠폰 발급 시 대규모 동시 요청 처리**

- RabbitMQ를 이용한 메시지 큐 기반의 비동기 처리로 1000명 동시 쿠폰 발급 요청에서도 안정적인 요청 수용 및 데이터 정합성 보장
- API 서버와 DB 간 직접 처리 대신 큐 기반 처리를 통해 트래픽 급증 시 부하를 분산하고 서버 리소스 소모 최소화
- 큐에 순차적으로 쌓인 요청을 Consumer에서 직렬 처리하여 중복 발급 및 재고 초과 문제 방지

5️⃣ **PG결제 처리 신뢰성 확보 및 채팅방 생성 로직 비동기 처리**

- 포트원(PG)과의 외부 통신을 통해 채팅 결제 및 포인트 충전 기반의 결제 기능 구현
- 동시에 다수의 결제 요청이 들어올 때 발생하는 중복 주문 문제를 해결하기 위해 Redisson 기반 분산 락을 적용
- 채팅방 생성 로직을 @Async 기반 비동기 처리로 분리하여, 결제 응답 속도를 개선하고 트랜잭션 안정성을 확보

6️⃣ **포인트 환불 자동화 및 동시성 제어**

- Spring Scheduler 기반으로 1분마다 만료된 유료 게시글을 조회하여, 미응답 게시글에 대해 자동 포인트 환불 처리
- 만료 게시글 인덱싱 적용으로 주기적인 대량 조회에도 DB 부하 최소화 및 조회 성능 향상
- Redis 기반 분산 락 적용으로 같은 사용자가 환불과 동시에 포인트를 사용하는 상황에서도 정합성 보장

7️⃣ **로그 기반 이력 관리 및 데이터 분산 처리**

- 포인트 사용, 쿠폰 사용, 포인트 환불 시 로깅 처리 후, ELK Stack 전송
- 복잡한 SQL 없이도 Kibana 대시보드로 실시간 이력 모니터링 가능, 운영 효율성 향상

## 3. 주요 기능


📌 알람 전송 : @Async와 Bulk Insert 활용

- 주요 흐름 : 유료 질문 게시물 등록 → 해당 게시물에 해당되는 전공의 리스트 조회 -> 전공의 id와 FCM 토큰을 가지고 메시지 생성 -> FCM 서버로 전송 -> 알람 전송 히스토리 Bulk Insert를 사용하여 저장 
- 대량 알림 전송과 히스토리 저장을 비동기·일괄 처리함으로써 성능 저하 없이 빠르고 안정적인 알림 서비스를 구현

📌 분산 환경에서의 채팅 처리 : RabbitMQ 활용

- 주요 흐름 : 서버로 메시지 전달 -> RabbitMQ의 서버별 큐에 메시지 전달 -> 각 서버가 메시지 소모 -> 메시지 기반으로 구독 경로 생성 -> 해당 구독 경로 구독한 세션들에게 전파
- 서버 별로 큐를 생성하여 분산 환경에서의 채팅 서비스 구현

📌 포트원 외부 통신을 통한 채팅 결제&포인트 충전 : 비동기 처리 + Redisson 분산 락 적용

- PG사와의 외부 통신을 통해 결제 기능을 구현하고, 주문 생성 후 채팅방 생성을 비동기로 분리, 중복 주문은 락으로 안전하게 차단하여 응답 속도 약 33% 개선, 실패율 74.5% 감소로 결제 성능 및 안정성 향상

📌 게시글 등록 및 포인트 환불 : Redis 기반 분산 락 적용

- 주요 흐름 : 1분마다 만료 게시글 조회→ Redis 분산 락으로 환불·사용 간 동시성 제어 → 미응답 게시글에 대해 포인트 자동 환불 처리
- 환불·사용 간 동시성 제어 및 데이터 정합성 보장

📌 쿠폰 발급 동시성 문제 해결 : RabbitMQ 활용

- 주요 흐름: 사용자가 쿠폰 발급 API 요청 → 서버가 발급 요청 메시지를 큐에 적재 → Consumer가 메시지를 순차적으로 처리 → 발급 성공 시 DB에 쿠폰 발급 정보 저장 및 수량 차감
- 대량 요청 상황에서도 중복 발급 및 수량 초과를 방지하며 안정적이고 일관된 쿠폰 발급 처리제공

## 4. 기능별 흐름도

<details>
  <summary>기능 흐름도</summary>

  <details>
    <summary>회원가입 및 로그인</summary>
    <p><img src="https://github.com/user-attachments/assets/d1f5132b-a4a9-4f80-bad5-a10eb5892534"></p>
    <p><img src="https://github.com/user-attachments/assets/42e3b1c2-cace-41b3-927a-d3b5551161a4"></p>
  </details>

  <details>
    <summary>비밀번호, 의사 이미지 변경</summary>
    <p><img src="https://github.com/user-attachments/assets/38a8d145-97ba-4112-91f3-d0e5d2913d3c"></p>
    <p><img src="https://github.com/user-attachments/assets/14a19b7c-7544-4a1c-8130-7a13a11a3705"></p>
  </details>

  <details>
    <summary>게시글</summary>
    <p><img src="https://github.com/user-attachments/assets/9f410a66-0cc2-4dc5-ae76-fa955baa7a95"></p>
    <p><img src="https://github.com/user-attachments/assets/4546128b-dbcd-4b0d-abc8-5973573eb872" width="580px"></p>
    <p><img src="https://github.com/user-attachments/assets/abcd1e21-2847-46e2-b205-f1e74986c941"></p>
    <p><img src="https://github.com/user-attachments/assets/f292589e-174a-410a-aa26-76c22a3ca361"></p>
    <p><img src="https://github.com/user-attachments/assets/a115064e-de4a-4078-942f-932f9ceb5fdb"></p>
  </details>

  <details>
    <summary>알람</summary>
    <p><img src="https://github.com/user-attachments/assets/41156ef9-b88b-4ab4-a02b-2f9175145349" width="580px"></p>
    <p><img src="https://github.com/user-attachments/assets/a679bdec-7efb-4354-b382-1b5c5a7b3a3b"></p>
    <p><img src="https://github.com/user-attachments/assets/57dc8a5e-c33f-4dd0-88ba-73388e49d599"></p>
  </details>

  <details>
    <summary>채팅방</summary>
    <p><img src="https://github.com/user-attachments/assets/0dd05bd2-4923-432a-b967-e2b732b468bd"></p>
    <p><img src="https://github.com/user-attachments/assets/27c8c7f1-ca48-4e9f-ab1a-807a107f7b7c"></p>
    <p><img src="https://github.com/user-attachments/assets/92da760e-aa1f-46a9-9135-b57dd9581865"></p>
  </details>

  <details>
    <summary>결제</summary>
    <p><img src="https://github.com/user-attachments/assets/174a220b-081f-4afa-bec0-7c0f5de6d190"></p>
    <p><img src="https://github.com/user-attachments/assets/42a234f7-ce73-4551-ba92-7754f09d56d3"></p>
  </details>

  <details>
    <summary>쿠폰</summary>
    <p><img src="https://github.com/user-attachments/assets/debdf332-c871-49aa-97a3-36e87ad05c10"></p>
  </details>

</details>

## 5. 서비스 플로우

1. **회원가입**  
   사용자는 의사 회원, 환자 회원 2가지로 나누어져 있습니다.

1. **의사조회**  
   사용자는 의사들의 정보를 조회하면서 채팅하고 싶은 의사를 고를 수 있습니다.

1. **결제**  
   결제는 총 2가지 방식(채팅결제, 포인트 충전)이 존재합니다. 의사와의 채팅을 하기 위해서 사용자는 소지금으로 결제하고, 유료 게시물 같은 경우는 오직 포인트를 사용해서 결제할 수 있기 때문에 포인트를 충전하기 위한 결제가 존재합니다.

1. **채팅**  
   사용자는 원하는 의사를 고른  후, 결제를 완료하면 해당 의사와의 채팅방이 생성되며 이때 의사는 채팅진료 요청이 들어왔다는 알람을 받습니다.

1. **게시글**  
   게시글은 2가지 종류가 있습니다. 유료게시글의 경우, 진료 고민을 입력하고 게시하면 해당 게시글들의 카테고리에 해당되는 의사들에게 알람이 가며, 24시간 이내에 아무런 답변이 오지 않는다면 결제에 사용된 포인트는 환불됩니다. 무료게시글의 경우는 알람이 가지 않고 게시판에 업로드만 됩니다.

1. **쿠폰**  
   사용자는 한정된 수량의 쿠폰을 발급받아 유료 게시글을 올릴 때 포인트 대신 사용할 수 있습니다. 또한 자신이 소유하고 있는 쿠폰 목록을 조회할 수 있습니다.

<h3>[알림 로직]</h3>
<p align="center">
  <img src="https://github.com/user-attachments/assets/718dc357-ede2-4b6d-9da3-c7f162cca1ff" width="580px">
</p>

<h3>[채팅 로직]</h3>
<p align="center">
  <img src="https://github.com/user-attachments/assets/422d223d-cb1f-4e02-a583-14d500635757" width="580px">
</p>

<h3>[게시물 등록 로직]</h3>
<p align="center">
  <img src="https://github.com/user-attachments/assets/9eb9590f-2abc-4b9f-a7ac-f12460fe9ac0" width="580px">
</p>

<h3>[결제 로직]</h3>
<p align="center">
  <img src="https://github.com/user-attachments/assets/cc141ac0-a27d-441a-b276-01e450e16beb" width="580px">
</p>

<h3>[쿠폰 로직]</h3>
<p align="center">
  <img src="https://github.com/user-attachments/assets/43e094e9-97ef-430e-a5b3-a8dc6332f339" width="580px">
</p>


## 6. 주요 기술 스택


<h3 align="center">애플리케이션</h3>

<p align="center">
  <img src="https://img.shields.io/badge/JAVA-FF7F00?style=for-the-badge&logo=&logoColor=white">
  <img src="https://img.shields.io/badge/Spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white">
  <img src="https://img.shields.io/badge/JPA-808080?style=for-the-badge&logo=&logoColor=white">
</p>

<h3 align="center">인증 및 보안</h3>

<p align="center">
  <img src="https://img.shields.io/badge/JWT-000?style=for-the-badge&logo=jsonwebtokens&logoColor=white">
</p>

<h3 align="center">메시징</h3>

<p align="center">
  <img src="https://img.shields.io/badge/RABBITMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=FFFFFF">
  <img src="https://img.shields.io/badge/Websocket-5cffd1?style=for-the-badge&logo=&logoColor=white">
</p>

<h3 align="center">데이터베이스</h3>

<p align="center">
  <img src="https://img.shields.io/badge/MYSQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white">
  <img src="https://img.shields.io/badge/Elasticsearch-005571?style=for-the-badge&logo=elasticsearch&logoColor=white">
  <img src="https://img.shields.io/badge/redis-%23DD0031.svg?style=for-the-badge&logo=redis&logoColor=white">
</p>

<h3 align="center">클라우드 및 CI/CD</h3>

<p align="center">
  <img src="https://img.shields.io/badge/DOCKER-2496ED?style=for-the-badge&logo=docker&logoColor=white">
  <img src="https://img.shields.io/badge/githubactions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white">
  <img src="https://img.shields.io/badge/AMAZON S3-569A31?style=for-the-badge&logo=amazonwebservices&logoColor=white">
  <img src="https://img.shields.io/badge/AMAZON EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white">
</p>

<h3 align="center">로그관리</h3>

<p align="center">
  <img src="https://img.shields.io/badge/Elasticsearch-005571?style=for-the-badge&logo=elasticsearch&logoColor=white">
  <img src="https://img.shields.io/badge/logstash-005571?style=for-the-badge&logo=logstash&logoColor=white">
  <img src="https://img.shields.io/badge/kibana-005571?style=for-the-badge&logo=kibana&logoColor=white">
</p>

<h3 align="center">협업 도구</h3>

<p align="center">
  <img src="https://img.shields.io/badge/Slack-4A154B?style=for-the-badge&logo=Slack&logoColor=white">
  <img src="https://img.shields.io/badge/ Git-F05032?style=for-the-badge&logo=git&logoColor=white">
  <img src="https://img.shields.io/badge/ GitHub-000000?style=for-the-badge&logo=GitHub&logoColor=white">
  <img src="https://img.shields.io/badge/ Notion-E92063?style=for-the-badge&logo=Notion&logoColor=white">
</p>


## 7. 아키텍쳐
<p align="center">
  <img src="https://github.com/user-attachments/assets/c185947c-fb91-48fd-8171-33266f7145cf" width="580px">
</p>


## 8. ERD
<p align="center">
  <img src="https://github.com/user-attachments/assets/badcb072-b906-4e6f-9283-b1efb3328586" width="580px">
</p>



## 9. [API 명세서](https://linen-town-985.notion.site/API-1e68f60328ad80699326f21c4a97ef3e)


## 10. 기술적 의사결정

<details>
    <summary>알람 전송 기술로 FCM을 선택한 이유</summary>

### 도입배경

- 해당 서비스에서는 알람이 호출되는 상황이 총 3가지 발생함
    - 첫 번째, 환자가 유료게시물을 올렸을 때
    - 두 번째, 의사가 환자의 게시물에 댓글을 달았을 때
    - 세 번째, 환자가 채팅 결제를 완료했을 때
- 알람의 경우 자주 호출되는 기능이고, 대량의 알람이 사용자가 유료 게시물을 올릴 때마다 전송되기 때문에 서버에서 많은 리소스를 소모할 수 밖에 없음
- 그렇기 때문에 서버의 자원을 제일 덜 소모하는 기술을 선택하기로 결정

### 선택지

- Short Polling
    - 클라이언트가 일정한 짧은 주기로 지속해서 요청을 보내고 서버에서 줄 데이터가 없는 경우엔 빈 응답, 데이터가 있는 경우에는 데이터를 담은 응답을 보내주는 형식
    - 서버에서 줄 데이터가 없어도 요청과 응답 작업을 반복하기 때문에 불필요한 트래픽이 많이 발생함
    - 일정한 주기로 요청과 응답이 오가기 때문에 서버에서 알람을 전송해도 클라이언트는 즉각적으로 알람을 수신할 수 없음
- Long Polling
    - 서버에서 클라이언트와 커넥션을 계속 유지하며 서버에서 줄 데이터가 생겼을 때 응답을 보내는 방식
    - Short Polling에 비해서 이벤트의 실시간성이 잘 보장되지만 클라이언트와 연결을 계속 유지하는 동안 서버 자원을 지속해서 소모하고 있게 됨
- Websoket
    - 클라이언트와 서버가 한 번 연결을 맺고 나면 해당 연결이 계속 유지되고 이를 통해 클라이언트와 서버는 양방향 통신을 할 수 있음
    - 서버에서 이벤트가 발생한 경우, 서버에서 먼저 데이터를 전송할 수 있기 때문에 실시간 알림기능을 구현하기 적합함
    - 서버와 클라이언트는 연결을 계속 유지하고 있다는 점에서 Long Polling 방식이 가지고 있는 서버 자원 소모 문제점을 동일하게 가지고 있음
- Server Sent Events
    - 클라이언트에서 서버로 요청을 보내면 일정 시간동안 연결을 유지하면서 서버에서 이벤트가 발생했을 때 실시간으로 클라이언트에게 데이터를 넘겨주는 실시간 단방향 통신 방법
    - Long Polling과 다르게 일정 시간동안 연결을 끊지 않고 계속 유지함
    - Spring에서 개발한다면 별도의 라이브러리 없이 SSE를 지원하는 도구를 제공하여 개발에 편함
- Firebase Cloud Messaging
    - 무료로 메시지를 보낼 수 있는 교차 플랫폼 메시징 클라우드 서버
    - Long Polling, Web Socket, SSE는 클라이언트와 서버가 연결을 유지하는 동안 서버의 자원을 지속해서 소모함
    - 이러한 상호작용으로 인해서 서버는 다른 기술들과 다르게 서버 자원 고갈 문제를 해결하고 클라이언트 또한 최적화 기능을 제공하는 fcm클라우드 서버와 연결하기 때문에 비교적 적은 배터리와 네트워크 사용만으로 알람을 수신할 수 있음
    - Google의 인프라를 기반으로 하여, 대량의 메시지를 안정적으로 처리할 수 있음
    - Google 서비스에 의존적이므로, Google 서비스가 제한된 지역에서는 사용이 어려울 수 있음
    - 사용자 기기에서 푸시 알림을 비활성화하면, 메시지를 전달할 수 없음

### 최종결정

- 해당 서비스에서는 알람을 전송하는 경우가 총 3가지가 있고, 그 중에서 대량 알람을 전송하는 요구사항이 있었기 때문에 클라이언트와 서버가 연결을 유지하는 동안 서버의 자원을 지속해서 소모하는 Long Polling, Web Socket, SSE는 적절하지 않다고 판단
- 또한, 환자가 유료 질문 게시물을 올리고 24시간 이내에 의사가 한명이라도 댓글을 달아주지 않는다면 환불이 되기 때문에 실시간성이 중요하므로 Short Polling은 적절하지 않다고 판단
- 이러한 이유로, 알람 전송 기술로 FCM 채택

  </details>
<details>
    <summary>FCM 토큰 관리 방식으로 RDB를 선택한 이유</summary>

### 도입배경

- FCM을 사용하여 서버에서 사용자들에게 알림을 전송하도록 결정
- 클라이언트가 먼저 FCM 서버로부터 토큰을 발급받고, 이를 로그인할 때 서버에 전송하는 식으로 관리하도록 구현을 진행함
- FCM토큰은 특정 기기로 푸시 알림을 보내기 위해 Firebase가 발급하는 고유 식별자이므로 이를 어떤 데이터베이스에 넣어서 저장하고 관리할지 고민하게 됨

### 선택지

- Redis
    - FCM 토큰은 결국 사용자를 식별하는 토큰이라고 생각이 들어서 Refresh 토큰처럼 사용자가 로그인을 할 때 저장하고, 사용자가 로그아웃을 할 때 삭제해주는 식으로 관리하면 좋을 것이라고 생각함
    - 또한 인메모리 데이터베이스이기 때문에 RDB보다 빠르고 TTL을 지정해 줄 수 있기 때문에 사용자가 로그아웃을 하지 않는다고 가정해도, 로그인 할 때마다 토큰을 받아오기 때문에 토큰의 최신화를 관리하기 쉽다고 판단함
    - Redis가 다운된다면 사용자의 모든 FCM토큰이 사라지게 되고, RDB에 비해서 적은 용량의 데이터를 저장할 수밖에 없음
    - 비용 측면에서 RDB보다 훨씬 비쌈
- RDB
    - Redis보다 조회 속도가 느리지만 훨씬 많은 양의 데이터를 저장할 수 있음
    - 디스크 기반 저장이기 때문에 데이터베이스가 다운되더라도 안전하게 복구할 수 있음
    - FCM토큰의 최신화 관리의 경우 사용자가 로그인할 때 기존 토큰과 다르다면 업데이트하고 동일하다면 유지하면서 관리할 수 있음


### 최종결정

- Redis는 인메모리 데이터베이스이기 때문에 용량이 적고 비용이 비싸기 때문에 유저의 모든 FCM 토큰을 저장하는 것은 불가능하다고 판단
- 또한 Refresh 토큰과 다르게 FCM 토큰은 자주 호출되고 손실되어서는 안 되는 중요한 정보이기 때문에 RDB에 넣어서 관리하는 것이 적절하다고 판단
- 이러한 이유로, FCM 토큰 관리 데이터베이스로 RDB 채택

  </details>
<details>
    <summary>kibana와 엘라스틱 서치의 public or private 설정에서 private를 선택한 이유</summary>

### 도입배경

- ElasticSearch 로그를 Kibana로 시각화해야 했음
- 하지만 Kibana는 프라이빗 서브넷에 있었고, 직접 접근 권한이 없는 상황에 직면함
- 팀원이 일시적으로 Kibana를 써야 하는 일이 생겨, 보안 유지하면서 접근성 주는 방법을 고민하게 됨

### 선택지

- Kibana 인스턴스를 퍼블릭 서브넷으로 옮기는 건 설정은 간단하지만 보안상 위험했음. 실수로 방치하면 외부 공격에 노출될 수 있어 제외
- Bastion Host 통해 포트 포워딩하는 방식은 Kibana는 프라이빗에 그대로 두고, 퍼블릭한 Bastion에서 SSH 터널 열어 접근하게 하는 구조라서 보안은 유지되지만, 접근자가 `ssh -L` 명령어 써야 해서 번거롭고 가이드를 공유해야 했음
- ALB에 인증 붙여서 접근 제한하는 방법은 가장 안전하지만, 설정 복잡하고 단기 접근용으론 과함

### 최종결정

- 결론적으로 Bastion 통한 포트 포워딩이 가장 합리적이라 판단함. 퍼블릭 전환 없이도 접근 가능했고, 보안도 일정 수준 지킬 수 있었음
- 접근자에게 명령어 안내 필요했지만, 단기 상황엔 충분히 감수할 만했음
- 다만, 장기적으로 팀원 여러 명이 접근해야 한다면 ALB 인증 설정도 고려할 수 있음

  </details>
<details>
    <summary>비관적 락의 데드락 문제 해결을 위한 동시성 제어 전략 선택한 이유</summary>

### 도입 배경

- 기존에 비관적 락(Pessimistic Lock)으로 User 한 명을 조회하고 있었는데, 이는 다른 트랜잭션은 이 row를 읽거나 수정할 수 없다는 의미
- 만약 `findByMajor` 처럼 여러 유저를 조회할 때, 동시에 실행되고 조회 대상이 겹치는 경우, 데드락이 발생할 수 있음
- 이를 해결하기 위해 비관적 락을 제외한 동시성 제어 전략을 선택해야 했음

### 선택지

- 낙관적 락(Optimistic Lock)
    - 낙관적 락은 데이터를 수정할 때 충돌이 발생하지 않을 것이라 가정하고 처리하는 방식
    - 이는 수정 시점에 버전 번호(@Version)를 비교해 변경 충돌이 있으면 예외 발생
    - 낙관적 락은 락을 걸지 않기 때문에 성능이 우수하지만 충돌 발생 시 예외가 발생하므로 재시도 로직이 필요하다는 단점이 있음

- 분산 락(Distributed Lock)
    - 분산 락은 단일 DB 락이 아닌 Redis, ZooKeeper 등 외부 시스템을 통해 락을 관리하는 방식
    - 이는 여러 대의 DB가 존재하는 분산 환경에서 동시성 문제를 해결할 수 있다는 장점이 있음
    - 하지만 네트워크 지연 및 Redis 장애 발생 시 락 획득 및 해제 지연이 될 수 있음

### 최종 결정

- 성능을 생각하면 낙관적 락이 좋다고 생각하지만 예외가 발생하면 재시도 로직을 구현해야하기 때문에 구현 복잡도가 올라갈 수 있음
- 만약 분산 락을 적용하면 Redisson 라이브러리를 사용하여 재시도 로직을 구현하지 않아도 자동으로 redis가 획득가능여부를 체크해줌
- 또한 현재는 단일 서버에서 DB를 사용하고 있지만 추후 확장성을 고려하여 비관적 락의 단점인 데드락을 해결하고자 분산 락을 선택

  </details>
<details>
    <summary>주문 후 채팅방 생성 비동기 처리 도입</summary>

### 도입배경

- 채팅 상담 주문 결제 후 해당 주문에 대한 채팅방 생성
- 채팅방 생성은 외부 API 호출 및 DB 저장을 포함하는 작업
- 채팅방 생성으로 인해 주문 응답 속도 지연 발생
- 실제 사용자는 결제 성공 여부만 확인하면 됨
- 채팅방 생성은 응답 이후 별도로 처리하는 것이 적절하다고 판단

### 선택지

- 주문 응답 전 채팅방 생성 완료까지 대기
    - 채팅방 생성 완료 후 주문 응답 반환
    - 응답 지연 발생
    - 채팅방 생성 실패 시 주문 전체 실패로 이어질 수 있음

- 별도 Kafka나 SQS 등 메시지 큐를 통해 비동기 처리
    - 안정성과 확장성 측면에서 가장 이상적임
    - 메시지 큐 시스템 구성 및 운영에 시간과 리소스 필요
    - 채팅 기능 전체가 아닌 채팅방 생성만 분리하려는 상황에서는 과하다고 판단

- Spring의 @Async 어노테이션을 활용한 비동기 처리
    - 설정 비용 적음
    - 백그라운드 작업을 손쉽게 구현 가능
    - 실패 시 로깅 및 재시도 설계 가능
    - JVM 내 동작으로 완전한 메시징 시스템만큼의 확장성은 없음

### 최종결정

- Spring의 @Async를 이용해 주문 생성과 채팅방 생성을 분리
- 주문 생성 완료 후 비동기 스레드에서 채팅방 생성 로직 실행
- 채팅방 ID는 주문 조회 시 확인 가능
- 장점: 응답 속도 향상, 로직 분리에 따른 유지보수 용이
- 단점: 비동기 처리 실패 시 재처리 설계 필요
- 향후 채팅방 생성 로직이 복잡해지거나 채팅 기능을 독립 서비스로 운영할 경우 Kafka 기반 아키텍처 확장 고려

  </details>
<details>
    <summary>실제 결제 흐름 인프라 구성 위한 ngrok를 선택한 이유</summary>

### 도입배경

- PortOne 결제 시스템에서는 결제 완료 후 서버로 Webhook 콜백을 전송함
- 초기에는 모든 서비스가 로컬 환경에서 개발되고 있었음
- 외부 서비스인 PortOne에서 콜백을 수신할 수 있는 URL이 존재하지 않았음

### 선택지

- Webhook 콜백 생략하고 서버 측에서만 결제 상태 검증
    - 사용자 응답 흐름에는 문제가 없음
    - Webhook 미활용 시 결제 실패나 중복 호출에 대한 처리가 누락될 수 있음
    - 서버 장애 시 발생한 결제 결과를 수신하지 못해 상태 불일치 가능성이 존재함

- 외부 개발 서버를 미리 구축하여 도메인 연결
    - 사전 배포 환경 필요
    - 개발 속도보다 인프라 구축 속도가 늦을 수 있음

- ngrok을 이용해 로컬 서버를 외부에 노출
    - 외부 HTTPS 주소를 빠르게 발급받을 수 있음
    - PortOne Webhook 설정에 즉시 적용 가능함
    - 개발 환경에서도 실제 결제 흐름과 동일하게 Webhook 테스트 가능함

### 최종결정

- 포트원 관리자 설정에 위와 같이 Webhook URL을 등록할 수 있음
- PortOne Webhook 수신을 위한 공용 HTTPS 엔드포인트 확보 목적으로 ngrok 도입
- 단순 테스트용이 아닌, 실제 결제 성공 및 실패에 따른 상태 처리를 위해 Webhook 흐름을 구성
- 개발 환경에서도 Webhook 수신 → 주문 완료 → 채팅방 생성까지 전체 흐름 검증 가능
- 이를 통해 결제 처리의 신뢰성 확보

  </details>
<details>
    <summary>STOMP를 통한 채팅 기능 구현</summary>

### 도입배경

- 의사와 유저의 1대1 채팅을 구현하기 위한 기술이 필요했음

### 선택지

- HTTP
    - 초기에는 HTTP로 채팅을 받고 전송하는 형태의 채팅 흐름을 구상했지만 다음과 같은 한계점으로 사용하지 않음
    - HTTP는 비연결성이므로 연결이 지속되지 않아 요청마다 TCP 핸드쉐이크 연결 과정이 필요함
    - 하지만 채팅과 같이 짧은 시간에 반복적으로 메시지를 주고받아야 하는 상황에서는 매번 연결을 맺는 과정에서 지연이 발생하므로, 실시간성이 중요한 환경에 부적합
    - HTTP 헤더는 무겁고 요청과 응답마다 불필요한 메타데이터가 반복적으로 전송되므로 가벼운 메시지를 전송하는 채팅에는 필요 이상의 오버헤드가 발생
    - HTTP는 요청-응답 기반 구조로 서버가 먼저 응답을 보낼 수 없으므로 상대방이 보낸 메시지를 실시간으로 수신하는 양방향 통신 구조를 만들 수 없음

- HTTP Polling
    - 실시간 네트워킹에서 HTTP 프로토콜의 문제를 해결하기 위한 방안으로 일정한 주기로 서버에 요청을 보내는 방법
    - 하지만 요청을 보낼 때마다 연결을 맺어야 함
        - HTTP 통신이므로 헤더가 불필요하게 커서 주기가 짧아지고 클라이언트가 많아질수록 서버에 부하가 커짐
    - 결국 일정 주기마다 요청을 보내는 것이므로 실시간이라고 보기는 어려움

- Long-Polling
    - Polling과 유사하지만 서버에서 응답해줄 데이터가 생길 때 까지 기다리는 방식
    - 타임아웃이 있어서 기다리는 시간에 제한을 줄 수 있음
    - 하나의 요청마다 서버 스레드를 점유하는 단점 존재
    - Polling 방식과 동일하게 HTTP 요청을 날려야 하므로 HTTP 헤더에 의한 오버헤드가 발생
    - 데이터가 빈번하게 업데이트 되는 거라면 Polling과 별 차이 없음

- HTTP Streaming
    - 서버가 클라이언트와 연결되고 데이터를 클라이언트로 푸시하는 방식
    - 연결 시 헤더가 한 번만 전송되므로 오버헤드가 적고, 요청을 반복할 필요가 없음
    - 서버의 상태 변경이 잦은 경우에 유리
    - 하지만 양방향 통신이 안되므로 채팅보다는 주식 시세, 뉴스 피드 등 단방향 실시간 데이터 전송에 적합

- WebSocket
    - 서버와 클라이언트가 지속적인 연결 상태를 유지
        - 한번의 TCP 연결이 이루어지면 이후에는 별도의 연결 과정 없이 양방향으로 메시지를 주고받을 수 있음
    - 헤더가 최소화 되어서 통신 오버헤드가 적음
    - 프레임 단위의 간결한 포맷으로 메시지를 주고받아서 가벼운 메시지를 주로 주고 받는 채팅 환경에 효율적
    - 양방향 통신이 가능
    - 서버는 클라이언트의 요청 없이도 메시지를 보낼 수 있어서 상대방이 보낸 메시지를 실시간으로 수신하는데 적합

### 최종결정

- 위와 같은 이유로 헤더가 가볍고 양방향으로 통신이 가능한 웹소켓을 사용하기로 결정
- 이때 WebSocket 기반의 STOMP를 이용하여 구현하기로 결정
- STOMP는 WebSocket 위에서 메시지의 구조와 송수신 방식을 정의한 pub/sub 기반의 포로토콜로 발신자와 수신자의 역할을 분리할 수 있어 채팅 시스템에 적합

  </details>
<details>
    <summary>채팅 메시지 검색 ElasticSearch 도입</summary>

### 도입배경

- 채팅방에 대한 메시지는 굉장히 많아질 수 있고 과거 메시지에 대한 검색이 빈번히 일어남
- 따라서, 사용자가 원하는 메시지를 찾는 검색 기능과 빠른 응답 속도가 필요

### 선택지

- **RDB**
    - RDBMS에서 LIKE 기반 검색은 인덱스를 무시하고 **Full Table Scan** 이 발생할 수 있어 데이터가 많아질수록 성능이 급격히 저하됨
    - 일부 RDBMS는 **Full Text Search(FTS)** 기능을 제공하지만
        - 기본적으로 **띄어쓰기 기반 토큰화**만 지원
        - **영어권 텍스트**에는 최적화되어 있으나 **한국어**처럼 어절 분리가 어려운 언어는 제대로 지원하지 못함
        - 형태소 분석이 없어 **의미 단위 검색**이 어려움

- **Elasticsearch**
    - ES는 기본적으로 **역색인 구조(Inverted Index)** 를 사용해, 특정 단어 검색 시 **RDB보다 훨씬 빠른 성능**을 제공
        - 역색인은 색인과 다르게 단어를 중심으로 해당 단어가 어디에 있는지 기록한 것을 의미
    - **형태소 분석기(Nori 등)** 를 적용해 **한국어처럼 복잡한 언어**도 의미 단위로 세밀하게 분석할 수 있음
    - 문장을 **토큰화**할 때, 분석기/필터/토크나이저를 커스터마이징하여 다양한 방식으로 쪼개고 검색할 수 있음
    - 다양한 **검색 쿼리를** 제공하여, 단순 키워드 매칭을 넘어, **유사도 검색(BM25)**, **Synonym 확장 검색**, **범위 검색**, **복합 쿼리** 등이 가능
    - 분산 아키텍처를 기반으로 해서 노드를 추가하는 것만으로 읽기/쓰기 처리량을 수평 확장할 수 있음

### 최종결정

- 의사와 환자가 얘기한 채팅 내역에서 단순히 문자가 아닌 의미로 문장을 검색 할 수 있도록 RDB 보다는 ES를 도입하여 검색의 질과 속도를 개선하는 것이 좋다고 생각하여 **Elasticsearch** 도입을 결정

  </details>
<details>
    <summary>채팅 시스템 RabbitMQ 도입</summary>

### 도입배경

- 초기에는 인메모리 기반 메시지 브로커를 사용하여 메시지를 처리하고 있었음
- 이때 서버가 여러 대로 확장될 경우, 같은 경로를 구독 중인 클라이언트들이 서로 다른 서버로 연결됨
- 메시지를 발행한 서버에 연결된 클라이언트만 메시지를 수신하게 되고 다른 서버와 연결된 클라이언트들은 메시지를 받지 못함
- 서버가 수평 확장되더라도 모든 서버가 동일한 메시지를 공유하고 처리할 수 있도록, 외부 메시지 브로커를 도입해서 큐를 중앙에서 관리하고, 각 서버로 메시지를 일관되게 분배하도록 구성

### 선택지

- Redis Pub/Sub
    - 메시지를 발행하면 즉시 구독자들에게 전파되는 낮은 지연 시간을 가짐
    - 메시지를 일시적으로만 유지하므로 구독자가 일시적으로 끊기면 메시지를 놓칠 수 있음
    - 복잡한 라우팅이나 메시지 보장 기능 부족

- Kafka
    - 고성능 대규모 메시지 스트리밍에 적합
    - 설정과 운영이 복잡하여 대규모 프로젝트에 어울림
    - 짧은 지연 시간보다 높은 처리량과 안정성에 중심을 두어서 실시간 보다는 대규모 데이터 파이프라인이나, 로그 수집에 적합

- RabbitMQ
    - AMQP 기반으로 메시지를 큐에 저장하여 안정적으로 전달
    - Exchange→Queue→Consumer 구조로 복잡한 라우팅, fanout 등 유연한 메시지 흐름을 구현할 수 있음

### 결론

- Redis는 실시간 전파는 빠르지만, 메시지의 보장과 복잡한 라우팅 구성이 부족하여 신뢰성이 필요한 채팅 시스템에는 적합하지 않음
- Kafka는 대규모 데이터 처리에 특화되어 있으나, 설정의 복잡성과 높은 운영 비용 측면에서 비교적 소규모인 채팅 시스템에는 과도함
- 반면 RabbitMQ는 메시지의 보장, 유연한 라우팅 전략, 적당한 복잡도와 실시간 처리 성능을 모두 갖추고 있어, 실시간 채팅 시스템에 가장 적합한 외부 메시지 브로커라고 판단
- 이에 따라 RabbitMQ를 메시지 브로커로 선택함

  </details>
<details>
    <summary>쿠폰 발급 동시성 제어 RabbitMQ 선택한 이유</summary>

### **도입 배경**

- 쿠폰 발급 기능은 다수의 사용자가 동시에 발급 요청을 보내는 높은 동시성 환경에서 동작함
- 수량이 제한된 쿠폰의 중복 발급 방지, 안정적인 처리, 실패 시 복구 가능성이 필수적임
- 동시성 제어 및 안정적인 발급 처리 방법을 찾기 위해 여러 기술을 시도하여 차이점과 가장 적합한 것이 무엇인지 경험하고 싶었음

### **선택지**

낙관적 락

- **방법**: JPA의 `@Version` 필드로 충돌 감지 후 예외 발생 시 재시도
- **장점**: 트랜잭션 충돌이 적은 환경에서 성능 우수함
- **문제점**:
    - 트래픽 증가 시 `ObjectOptimisticLockingFailureException` 빈발
    - “선착순”이지만 실제 순서 보장이 안 됨 → 가장 큰 문제

---

비관적 락

- **방법**: JPA의 `PESSIMISTIC_WRITE`로 select 시점부터 다른 트랜잭션 블로킹
- **장점**: 중복 발급 방지 확실함
- **문제점**:
    - 성능 저하, 데드락 가능성
    - 대량 트래픽 시 DB 부하 심함
- **개선 시도**: `@QueryHints`로 락 타임아웃 설정 → 불필요한 대기 제거

---

Redis 분산 락

- **방법**: Redisson, Lettuce 기반 Redis에 키를 두고 분산 락 처리
- **장점**: 빠른 성능, 분산 환경 대응 가능
- **문제점**:
    - 락 해제 실패 시 별도 설정 없으면 락 유지됨
    - 재시도 로직 필요 → 구현 복잡성 증가
- **개선 시도**: `tryLock`의 대기 시간, 유지 시간 조정 (예: 30→15초)

---

RabbitMQ

- **방법**: 요청을 메세지로 큐에 보내어 단일 스레드 직렬 처리
- **장점**:
    - 락 없이 동시성 문제 해결
    - 각 요청을 큐에 쌓고 하나씩 처리 → 중복 발급 방지 가능
    - 사용자 빠른 응답 → 서버에서 비동기 처리
    - 실패 시 DLQ(Dead Letter Queue) 로 메세지 후처리
    - DB 부하 감소 (Consumer가 순서대로 DB 접근)
- **추가 효과**:
    - 재시도 큐 구성 → 자동 복구 가능
    - 메세지 유실 방지 → 장애 복구 용이

### **최종 결정**

- 쿠폰 발급 환경은 발급은 나중에 처리해도 되지만, 중복 발급은 절대 일어나면 안 된다는 요구사항이 가장 중요
- RabbitMQ는
    - 비동기 + 직렬 처리 → 동시성 문제 해결
    - 락 없는 구조 → 성능 저하 없이 안정성 확보
    - DLQ → 실패한 요청 추적·복구 가능
    - 빠른 응답 속도 → 사용자 경험 개선
- 위와 같은 이유로 락 기반 방식보다 구조적으로 안정적이고 확장 가능하며 복원력 있는 RabbitMQ 기반 구조가 가장 적합하다고 판단하여 최종 선택

  </details>

## 11. [트러블 슈팅 & 최적화 전략]
<details>
   <summary>다건 알람(1만개) 전송 시간 개선</summary>
  
## 문제 상황

- 기존 MVC 기반으로 `sendEachForMulticast` 메서드를 사용하여 100개씩 Batch로 1만개의 알람을 전송했을 때, 2분 43초라는 매우 긴 시간이 걸림
- 또한 알람이 전송되는 동안, 서버에서 다른 여러가지 요청에 대한 처리를 하는데 지장이 생김

```java
for (int i = 0; i < fcmTokenList.size(); i += 100) {
      fcmTokenBatches.add(fcmTokenList.subList(i, Math.min(fcmTokenList.size(), i + 100)));
}
for (List<String> fcmTokenBatche : fcmTokenBatches) {
     /*
      * sendMulticastAlarm 메서드에 @Async 존재 x
      */
      alarmSenderService.sendMulticastAlarm(fcmTokenBatche, alarmMessage);
}
alarmHistoriesBulkRepository.batchUpdate(userIdList, alarmType, alarmMessage);
```

## 해결방안

- `alarmSenderService.sendMulticastAlarm` 메서드에 @Async를 붙여서 메인스레드로부터 분리되어 별도의 스레드에서 동작하도록 비동기 처리
- Executor를 목적에 맞게 관리할 수 있고, 스레드의 수를 제한하여 OutOfMemoryError를 막기 위해서 `AsyncConfig` 적용

```java
@Async("fcmExecutor")
    public void sendMulticastAlarm(List<String> fcmTokenBatche, String content) {
        try {
            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(Notification.builder()
                            .setTitle("Docconneting")
                            .setBody(content)
                            .build())
                    .addAllTokens(fcmTokenBatche)
                    .build();

            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            int successCount = response.getSuccessCount();
            int failureCount = response.getFailureCount();

            log.info("알림 전송 완료 - 성공횟수: {}, 실패횟수: {}", successCount, failureCount);
        } catch (FirebaseMessagingException e) {
            throw new RuntimeException(e);
        }
    }
```

## 도입 전후 비교

- 시나리오 : 1만건의 알람을 `sendEachForMulticast` 메서드를 사용하여 100개씩 Batch 전송한다고 가정

**📌 도입 전 성능 테스트 결과**

![Image](https://github.com/user-attachments/assets/95c020d4-247f-4e2a-961b-1e4d4a0b3184)

| 처리 방식 | 실행 시간 |
| --- | --- |
| MVC기반 @Async 미적용 | 2분 51초 |

**📌  도입 후 성능 테스트 결과**

시작 시간

![Image](https://github.com/user-attachments/assets/3a89a162-353d-464f-85a2-0ed03f987453)

종료 시간

![Image](https://github.com/user-attachments/assets/541583cb-fd55-4d4a-b669-a62986d39a36)

| 처리 방식 | 실행 시간 |
| --- | --- |
| MVC기반 @Async 적용 | 1분 40초 |

**성능 개선 요약**

**📌 요약 그래프**

<img src="https://github.com/user-attachments/assets/645d7697-33eb-45cb-8201-c24dbff16206" width="300" height="300">

| 처리 방식 | 실행 시간  |
| --- | --- |
| MVC기반 @Async 미적용 | 2분 52초 |
| MVC기반 @Async 적용 | 1분 40초 |
- **평균 응답 시간 :** 171초 → 100초로 약 **41.5%** 감소
</details>

<details>
  <summary>분산 환경에서 메시지 전달에서의 문제 해결</summary>

![Image](https://github.com/user-attachments/assets/fee3492c-e935-4797-b0cd-d9b29dcfa4da)

- 초기에는 인메모리 기반 메시지 브로커를 사용하여 메시지를 처리하고 있었음
- 이때 서버가 여러 대로 확장될 경우, 같은 경로를 구독 중인 클라이언트들이 서로 다른 서버로 연결되게 됨
- 메시지를 발행한 서버에 연결된 클라이언트만 메시지를 수신하게 되고 다른 서버와 연결된 클라이언트들은 메시지를 받지 못하게 됨

## 해결 방안

![Image](https://github.com/user-attachments/assets/d5b33917-3a89-40ce-a1cb-58f54e946ac1)

- 서버가 수평 확장되더라도 모든 서버가 동일한 메시지를 공유하고 처리할 수 있도록, 외부 메시지 브로커를 도입해서 큐를 중앙에서 관리하고, 각 서버로 메시지를 일관되게 분배하도록 구성함

### 외부 메시지 브로커 RabbitMQ 사용

![Image](https://github.com/user-attachments/assets/c3bc3988-59bf-479f-9009-e4a2b5f3a6ea)

- RabbitMQ는 서버와 서버 사이에서 메시지 전달을 중개하는 역할
- 이를 통해 서버 간 메시지를 실시간으로 전파할 수 있음
- 메시지의 발행과 소비를 분리하여 비동기적으로 처리함으로써 서버의 부하를 줄일 수 있음

### 메시지 전달 흐름
![image](https://github.com/user-attachments/assets/a9e644b9-bceb-4ccf-9db3-ed9d3dea9a60)

### 문제 상황

- 만약 STOMP로 통신하게 되면 웹소켓 연결 세션별로 계속해서 임시적인 큐가 생성됨
- 그러면 큐에 대한 구독자 목록, 바인딩 정보와 같은 메타데이터를 저장해야 하므로 메모리 사용량이 증가하게 되고 큐에 대한 관리가 어려워 질 것으로 생각함
- 큐마다 메시지를 전파해야 되므로 메시지를 처리하는 오버헤드가 증가하게 됨

⇒ RabbitMQ 부하가 가증되어 처리 효율이 떨어질 수 있음

## 해결 방안

- STOMP로 RabbitMQ 와 통신하지 않고 AMQP로 RabbitMQ와 통신하도록 결정

![Image](https://github.com/user-attachments/assets/2526595c-0f2d-481e-9e63-57356a82acb5)

- 위와 같이 하나의 큐를 생성하고 RabbitMQ로 메시지를 보내고 서버에서 큐에 대해 리스너를 작성하여 메시지를 읽어온 뒤 SimpMessagingTemplate 을 통해서 메시지를 전파하면 내부 SimpBrokerMessageHandler에서 해당 경로를 구독하는 세션들에게 메시지가 전파됨

즉 세션에 대한 구독정보는 내장 메시지 브로커가 관리하고 메시지를 다른 서버로 전달하기 위해서 RabbitMQ를 사용하는 형태임

```java
@RabbitListener(queues = {"${chat.queue}"})
    public void consume(MessageResponse messageResponse){
        System.out.println("1번 리스너 동작");
        simpMessagingTemplate.convertAndSend("*/sub/chattingRooms/1*", messageResponse);
    }
```

- 위와 같이 /sub/chattingRooms/1 경로 구독하는 세션들에게 전파함

## 문제 상황

- 서버가 여러 대면 서버마다 리스너가 존재함
- rabbitMQ는 리스너들에게 메시지를 라운드 로빈 형식으로 번갈아 가며 전파함
- 현재 구조로는 하나의 서버가 리스너를 통해 메시지를 받으면 다른 서버들은 받지 못하는 상태

## 해결 방안

- 서버마다 고유한 큐를 바인딩 해야겠다고 생각함
- fanout exchange 전략을 사용하기로 결정

![Image](https://github.com/user-attachments/assets/84938d21-c331-42b2-9594-fbfce6230f27)

1. 서버마다 큐가 생성
2. 서버 1로 메시지를 발행
3. 메시지가 RabbitMQ로 전달
4. fanout exchagne에 의해서 연결된 모든 큐에 메시지 전달
5. 서버들은 각 서버의 큐를 구독하고 있으므로 메시지를 소비
6. 메시지를 받아와서 구독 경로를 만들고 SimpBrokerMessageHandler가 해당 경로를 구독하고 있는 세션에 전파

```llvm
    @Bean
    public Queue queue(){
        return new Queue(queue + "." + id);
    }
    
    @Bean
    public FanoutExchange exchange(){
        return new FanoutExchange(exchange);
    }

    @Bean
    public Binding binding(){
        return BindingBuilder
                .bind(queue())
                .to(exchange());
    }
```

- 위와 같이 서버별로 큐가 생성되도록 빈을 등록했고 FanoutExchange와 Binding 규칙을 정의함

```llvm

    @Transactional
    public void createMessage(MessageRequest messageRequest, Long userId, Long chattingRoomId){

        User findUser = userRepository.findById(userId).orElseThrow(() -> new ClientException(ErrorCode.USER_NOT_FOUND));
        if(findUser.getIsDeleted()){
            throw new ClientException(ErrorCode.USER_NOT_FOUND);
        }

        ChattingRoom findChattingRoom = chattingRoomRepository.findById(chattingRoomId).orElseThrow(() -> new ClientException(ErrorCode.CHATTING_ROOM_NOT_FOUND));
        if(!findChattingRoom.getIsActive()){
            throw new ClientException(ErrorCode.INACTIVE_CHATTING_ROOM);
        }

        Message message = Message.of(findUser, findChattingRoom, messageRequest.getContents());

        Message savedMessage = messageRepository.save(message);

        MessageQueuePayload messageQueuePayload = MessageQueuePayload.of(chattingRoomId, userId, savedMessage.getContents(), savedMessage.getCreatedAt());

        rabbitTemplate.convertAndSend(exchange, "", messageQueuePayload);
    }
```

- 메시지를 저장하는 로직에서 rabbitTemplate.convertAndSend(exchange, "", messageQueuePayload); 통해서 rabbitMQ로 메시지를 전달했고

```llvm

    @RabbitListener(queues = "#{queue.name}")
    public void consume(MessageQueuePayload messageQueuePayload){

        MessageResponse messageResponse = MessageResponse.of(messageQueuePayload.getUserId(), messageQueuePayload.getContents(), messageQueuePayload.getCreatedAt());

        simpMessagingTemplate.convertAndSend("/sub/chattingRooms/" + messageQueuePayload.getChattingRoomId(), messageResponse);
    }

```

- 각 서버는 자신이 등록한 큐에 대해서 메시지를 소비할 수 있도록 @RabbitListener를 지정함
- 메시지를 받아와 구독 경로를 만들어서 해당 경로를 구독하는 세션들에게 메시지를 전파함

</details>

<details>
  <summary>메시지 리스트 조회 성능 개선</summary>

## 문제상황 1

- 채팅방에서 이전 메시지 목록은 자주 발생하는 요청이므로 사용자 경험 저하 없이 빠르게 응답할 수 있도록 성능을 개선할 필요가 있었음

### 테스트 환경

- 총 5,000,000개의 메시지
- 100명의 유저가 100번 요청

### 인덱스 적용 전

```llvm
@Query("SELECT m FROM Message m JOIN FETCH m.user WHERE m.chattingRoom.id = :chattingRoomId ORDER BY m.createdAt DESC")
Page<Message> findAllMessagesWithUser(Long chattingRoomId, Pageable pageable);
```

![Image](https://github.com/user-attachments/assets/dbef399a-7d18-4146-8dc1-9d6a1b97c029)

- Using filesort를 통해서 created_at으로 정렬하기 위한 작업을 따로 수행하는 것을 확인 할 수 있음
- 평균 365ms

### 인덱스 적용

## 문제 상황 2

- 현재 chatting_room_id는 외래키로 인덱스가 존재하므로 해당 인덱스로 해당되는 message를 찾지만 created_at으로 정렬하기 위해서 따로 정렬 작업을 수행하는 것이 문제

## 해결 방안

```llvm
@Entity
@Table(name = "messages", indexes = {@Index(name = "idx_chatting_room_id_created_at", columnList = "chatting_room_id, created_at")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Message {
    ...
}
```

- chatting_room_id, created_at 복합 인덱스를 만들어서 인덱스를 통해 chatting_room_id에 대한 where 검사와 created_at에 대한 정렬을 인덱스에서 수행하도록 수정

![Image](https://github.com/user-attachments/assets/da254892-191d-499a-84a9-1448a013f1b7)

- Backword index scan을 통해서 따로 정렬하는 작업을 수행하지 않고 인덱스를 역방향으로 탐색하여 결과를 찾는 것을 확인
- 평균 263ms
- 27.95% 개선

### Projection 적용

## 문제 상황 3

```llvm
 public static List<MessageListResponse> toMessageListResponses(List<Message> messages){
        return messages.stream().map(message ->
                        new MessageListResponse(
                                message.getUser().getId(),
                                message.getContents(),
                                message.getCreatedAt())
                )
                .collect(Collectors.toList());
    }
```

- 현재 MessageListResponse 에서 응답을 만들 때 UserId가 필요하므로 N+1이 발생하지 않도록 fetch join을 수행하고 있어서 쿼리문에 join이 수행
- users 테이블과 join 하기 위해서 users 테이블 스캔이 추가적으로 필요

## 해결 방안

- Projection은 엔티티 전체가 아닌 필요한 값만 추출하는 방식
- 이걸 통해 현재 필요한 user_id, cotents, created_at 필드의 값만 가져올 수 있음
- 또한 users 테이블과 join 할 필요가 사라지므로 users 테이블을 조회하는 과정이 생략되어 성능 개선을 기대할 수 있음

```llvm
public interface MessageList {
    Long getUserId();
    String getContents();
    LocalDateTime getCreatedAt();
}
```

```llvm
@Query("SELECT m.user.id AS userId, m.contents AS contents, m.createdAt AS createdAt " +
            "FROM Message m " +
            "WHERE m.chattingRoom.id = :chattingRoomId " +
            "ORDER BY m.createdAt DESC")
    Page<MessageList> findAllMessagesWithUser(Long chattingRoomId, Pageable pageable);
```

- 인터페이스를 정의하고 Repository 에서 반환 타입으로 지정하면 스프링 데이터 JPA에서 구현체를 만들어 줌

![Image](https://github.com/user-attachments/assets/ffadce89-60a3-4798-aa08-cf54d4a07985)

- Backword Index scan 을 통해서 인덱스를 역방향으로 탐색하여 찾는 걸 확인 할 수 있음
- 평균 91ms

## 도입 전후 비교

![Image](https://github.com/user-attachments/assets/0c828330-93fa-4d91-ba33-1e53bf1303ec)

- 인덱스, Projection 사용하지 않았을 때보다 75.07% 개선
- Projection 사용하지 않았을 때보다 65.39% 개선

</details>

<details>
  <summary>분산 락 적용 후 테스트 과정에서의 문제 해결</summary>

## 문제 상황

분산 락을 적용하여 정합성 보장 테스트를 하는 상황에서 테스트 결과가 예상하던 결과와 다르게 나타나는 문제가 발생

테스트 코드는 아래와 같음

```java
@Test
    @DisplayName("포인트 환불과 동시에 게시글 등록 상황에서 분산락으로 정합성 보장 테스트")
    void distributedLockTest() throws InterruptedException{
        // given
        User user = User.of("test@example.com", "password", "username", 1000, false, UserRole.PATIENT);
        userRepository.save(user);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        // when
        executorService.submit(() -> {
            try {
                pointService.usePoint(user, 1L); // user 객체를 받아서 넘김
            } catch (Exception e) {
                System.out.println("포인트 사용 실패: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                pointService.refundPoint(user.getId(), 3L, 1000); // 임의의 postId
            } catch (Exception e) {
                System.out.println("포인트 환불 실패: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        latch.await();

        // then
        User updateduser = userRepository.findById(user.getId()).orElseThrow();
        int point = updateduser.getPoint();

        // 환불 1000
        assertThat(point).isGreaterThanOrEqualTo(1000);
        assertThat(point).isEqualTo(1000);
    }
```

이 테스트 코드를 실행했을 때

예상되는 결과 : 1000 (`환불 → 사용` 또는 `사용 → 환불`이 되어도 결과는 1000으로 똑같아야 함)

실제 결과 : 0

## 원인

테스트 코드를 보면 given에서 `userRepository.save(user)` 로 저장 후, `executorService.submit()` 를 사용하여 user를 다른 스레드로 넘김

JPA 입장에서 user를 받은 스레드는 다른 영속성 컨텍스트이기 때문에 user 객체는 영속 상태가 아니게 됨. 그러므로 update SQL이 발생하지 않음

여기서 각자 다른 스레드인 `usePoint`와 `refundPoint`의 차이점이 보이는데, 바로 매개 변수로 user에 대한 정보를 객체로 넘겨주는지, 아니면 id 값을 넘겨주는지에 대한 차이임

id로 넘겨주게 되면 refundPoint 메서드 안에서 `userRepository.findById` 를 하여 user를 찾게 되고, 이 객체는 영속 상태가 됨. 그러므로 `user.decreasePoint()` 처럼 user의 값을 수정할 때 값의 변경을 감지하여 update 쿼리문이 나가게 됨

하지만 객체를 넘겨주게 되면 넘겨받은 user 객체는 준영속 상태 즉, Detached 상태가 되므로 값을 수정해도 JPA가 변경 감지를 하지 못하게 되어 update 쿼리문이 나가지 않게 됨

## 해결 방안

`usePoint` 에서 user 객체를 보내지 않고, `user.getId()` 형식으로 Id만 넘기게 한 후, `usePoint` 내부에서 `userRepository.findById` 를 하여 user를 찾게 하면 그 객체는 영속 상태가 되므로 문제가 해결이 됨

아래의 코드는 수정한 usePoint 메서드

```java
@DistributedLock(value = "#userId")
    public void usePoint(Long userId, Long postId) {
        User user = userRepository.findUserByIdAndUserRole(userId, UserRole.PATIENT).orElseThrow(() ->
                new ClientException(ErrorCode.USER_NOT_FOUND));

        validateHasPoint(user);
        user.decreasePoint(POST_POINT_COST); // Update SQL 발생
        userRepository.save(user);
        
        ... 생략
    }
```

또한 테스트 코드에도 usePoint의 매개변수에 userId를 받아서 넘기게 수정

```java
... 생략

// when
        executorService.submit(() -> {
            try {
                pointService.usePoint(user.getId, 1L);
            } catch (Exception e) {
                System.out.println("포인트 사용 실패: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
... 생략
```

## 결과

여러 번 테스트 코드를 실행해도 항상 같은 결과를 얻게 됨

![Image](https://github.com/user-attachments/assets/2bc77479-e70b-4c49-923c-e9a0f650dae6)

</details>

<details>
  <summary>쿠폰 발급 및 사용</summary>

## 문제 상황

- 모든 서버가 퍼블릭 서브넷에 위치하고 있었고, 보안 그룹도 `0.0.0.0/0`으로 설정되어 누구나 접근이 가능한 매우 취약한 상태였음

### 원인

- 빠른 배포를 위해 퍼블릭 서브넷에 서버를 구성하고 접근 제어 없이 설정한 것이 원인이 되었음
- ALB, EC2, RDS 등 주요 인프라가 모두 외부에 노출된 상태였음

## 해결 과정

1. **보안 구조 재설계**
    - 기존 퍼블릭 서브넷 외에 새로운 **프라이빗 서브넷**을 생성하여 EC2, RDS 등을 모두 이동시킴
    - 퍼블릭하게 접근이 필요한 리소스는 **ALB**로 한정함
2. **Bastion Host 구성**
    - 내부 자원에 접근하기 위한 중간 지점으로 **퍼블릭 EC2 인스턴스 (Bastion Host)**를 설정함
3. **IAM 역할 및 접근 제어**
    - Bastion에 접속할 수 있는 IAM 권한을 제한적으로 부여하고, 접근 계정을 별도 관리
4. **보안 그룹 정리**
    - EC2, RDS 등 모든 인스턴스는 **내부 통신만 허용**하도록 보안 그룹을 구성함
    - ALB → EC2, Bastion → EC2 (SSH), EC2 → RDS (DB 연결) 등 최소 허용 규칙만 적용

### 결과

- ALB를 제외한 모든 자원이 외부로부터 차단되어 **외부 공격에 대한 노출을 크게 줄임**
- Bastion을 통해 내부 접근을 제한적으로 허용함으로써 **운영 및 유지보수 효율성도 확보**
- 팀원 외에는 내부 인프라에 접근할 수 없도록 구성되어 **보안 수준이 크게 향상**
</details>


<details>
  <summary>GitHub Actions 기반 CI/CD 구성</summary>

### Git action Deploy Flow

![Image](https://github.com/user-attachments/assets/ad321682-02c6-491f-b586-a2988cd5b032)

### Git action Test Flow

![Image](https://github.com/user-attachments/assets/2b0bb9e1-9fc7-42e4-b06e-9137de9518fc)

### 기능 설명

- GitHub Actions 기반 CI/CD 구성
- PR 생성이나 커밋 시 테스트가 자동 실행되어 정상 동작을 검증하고, Dev 브랜치에 머지되면 워크플로우가 트리거되어 Docker 이미지를 빌드하고 ECR에 업로드
- 서버에서는 이미지를 pull 받아 컨테이너로 실행하여 배포 자동 완료
- 외부에 노출되는 구성은 ALB 하나로 하여, 퍼블릭 서브넷에 배치해 사용자 요청을 수신
- 나머지 EC2 인스턴스 및 주요 서비스(Gateway, Main, Alarm, Eureka 등)는 모두 프라이빗 서브넷에 위치시켜 외부 접근을 차단
- Redis, Elasticsearch, RDS 등 인프라도 프라이빗 서브넷에 배치

### 효과

- 개발자는 코드 푸시만으로 테스트부터 배포까지 자동화된 흐름을 경험할 수 있어 생산성과 안정성이 크게 향상
- 민감한 데이터 보호 및 운영 신뢰성을 확보
- 전체 시스템이 GitHub 기반으로 관리되므로, 버전 관리 및 변경 이력 추적이 명확
- PR 시점에 테스트 코드가 자동 실행되어, 병합 전 코드의 품질을 검증함으로써 사전 오류 예방 및 안정성 확보
</details>

<details>
  <summary>JWT를 사용한 인증/인가</summary>

### 기능 설명

- 사용자는 이메일과 비밀번호를 입력하여 회원가입을 진행하고, 로그인 시 JWT(Json Web Token)를 발급받아 인증
- 로그인 성공 시 클라이언트에 액세스 토큰과 리프레시 토큰을 전달하며, 액세스 토큰은 요청 시 HTTP 헤더에 포함하여 인증 처리
- 서버는 토큰에 담긴 사용자 정보를 바탕으로 권한(Role)에 따라 요청을 인가하고, 리프레시 토큰을 이용한 토큰 재발급도 가능
- 회원 정보는 암호화된 비밀번호(BCrypt)를 포함해 안전하게 DB에 저장

### 사용 기술

- JWT 기반의 인증/인가
- BCryptPasswordEncoder를 활용한 비밀번호 해싱
- MySQL을 활용한 사용자 정보 저장
- Filter 기반 JWT 인증 필터 구현
- Role 기반 권한 부여 (예: PATIENT, DOCTOR, ADMIN)

### 인증/인가 흐름

**📌 인증/인가 흐름도**

![Image](https://github.com/user-attachments/assets/11de4566-f0c2-4295-8b45-cc1db22a1402)

</details>

<details>
  <summary>결제 테스트 실패에서의 문제 해결</summary>

  ### 문제 상황

  - 중복 merchant_uid 로 인한 테스트 실패
  ```java
  org.springframework.dao.DataIntegrityViolationException:
Duplicate entry 'merchant_123456' for key 'orders.uq_merchant_uid'
  ```

### 원인

- Order 엔티티에서 merchant_uid에 @Column(unique = true) 제약이 존재
- 테스트 코드에서 동일한 merchant_uid 값(예: merchant_123456)을 반복 사용
- 테스트 반복 실행 시 DB 충돌 발생

### 해결 방안

 ```java
  String uniqueMerchantUid = "merchant_" + UUID.randomUUID();
Order order = Order.ofChatOrder(patient, OrderProduct.CHAT_3000, doctor.getId(), uniqueMerchantUid);
 ```

- 매 테스트마다 고유한 merchant_uid를 생성하여 충돌 회피

### 문제 상황

- JMeter 테스트 시 PortOne 결제 실패 (400 Bad Request)

### 원인

- 결제 검증 로직은 실제 결제된 imp_uid를 기반으로 PG 서버에 상태 검증
- 테스트 환경에서는 실제 결제가 없으므로, 존재하지 않는 imp_uid로 검증 시도 → 실패

### 해결 방안

```java
  if (request.getImpUid().startsWith("TEST_")) {
 
}
```

- imp_uid가 TEST_로 시작하면 테스트 모드로 간주
- 실제 PG 검증을 건너뛰고, 모킹된 응답을 통해 흐름 테스트 가능하게 구성
</details>

## 12. 팀원소개

| 역할    | 이름   | 주요 담당 업무 |
|--------|--------|----------------|
| 팀장    | 김예나 | 알람 |
| 부팀장 | 김민재 | 채팅 |
| 팀원 | 김수연 | 인증인가, CI/CD |
| 팀원    | 반효승 | 쿠폰, 동시성 제어, 비동기 |
| 팀원    | 원채빈 | PG, 동시성 제어, 비동기 |
| 팀원    | 홍수경 | 유료 질문 게시판, 포인트, 동시성 제어, 로그관리 |
