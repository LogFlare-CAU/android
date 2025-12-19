# LogFlare Android

모바일로 서버 에러 로그를 확인하고 알림을 받을 수 있는 LogFlare 전용 안드로이드 앱입니다.

---

## 📱 다운로드
APK는 GitHub Releases에서 바로 받을 수 있습니다:  
https://github.com/LogFlare-CAU/android/releases

---

## 🔍 주요 기능
- 로그인 후 프로젝트별 로그 확인
- 최근 에러 발생 내역 열람
- 모바일 알림으로 즉각 대응
- 간단한 UI 기반 조작

---

## ▶️ 실행 흐름
1. 로그인  
2. 프로젝트 선택  
3. 로그 확인  
4. 알림 수신  

---

## 🗂 저장소 구조
- `app` — 메인 앱 로직 + UI
- `core` — 데이터 모델 및 네트워크 구성 요소
- `docs` — Swagger 기반 DTO 문서 포함

---

## 🌐 백엔드
서버는 별도 저장소에 있으며, 이 앱은 해당 서버와 연결되어 로그를 전달받습니다:  
https://github.com/LogFlare-CAU/Backend

