# CommerceCore Frontend

주문 및 재고 관리 시스템의 사용자 인터페이스입니다. React와 TypeScript를 사용하여 안정적인 데이터 흐름을 구현하고, 주문 과정에서의 사용자 경험을 최적화하는 데 집중했습니다.

## 🛠 주요 기술 스택
- **Language**: TypeScript 
- **Framework**: React 18, Vite 
- **Styling**: Tailwind CSS 
- **Communication**: Axios (REST API 연동)
- **State Management**: React Query / Context API (주문 데이터 및 재고 상태 관리)

## 주요 기능 및 구현 포인트
1. **주문 과정 시각화**: 
   - 실시간 재고 상태를 반영하여 주문 가능한 상품인지 즉각적인 피드백 제공
2. **비동기 요청 처리**: 
   - Axios를 활용한 백엔드 API 연동 및 결제 요청 시 네트워크 상태에 따른 로딩/에러 처리
3. **사용자 경험(UX) 최적화**: 
   - Tailwind CSS를 활용한 반응형 웹 구현
   - 중복 클릭 방지 (주문 버튼 비활성화 등 프론트엔드 단에서의 1차 방어)

## 실행 방법
```bash
# 의존성 설치
npm install

# 개발 서버 실행
npm run dev