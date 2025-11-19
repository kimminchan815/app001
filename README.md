# 대중교통 목적지 근접 알림 앱

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)

## 📱 앱 소개
대중교통 이용 시 목적지에 근접하면 알림을 울려주는 안드로이드 앱입니다.
버스나 지하철에서 졸거나 다른 일을 하다가 목적지를 지나치는 것을 방지합니다.

## ✨ 주요 기능
- 🗺️ **지도 기반 목적지 설정** - Google Maps로 직관적인 목적지 선택
- 🔔 **점진적 알림 시스템** - 3단계로 나누어 음량이 점점 커지는 알림
- 📍 **백그라운드 위치 추적** - 앱을 닫아도 계속 추적
- ⚙️ **커스터마이징 가능** - 알림 거리, 음량, 진동 설정 조절
- 🔋 **배터리 최적화** - 효율적인 위치 업데이트

## 🚀 빠른 시작
상세한 설치 및 사용 방법은 [GUIDE.md](GUIDE.md)를 참조하세요.

### 필수 요구사항
- Android Studio Hedgehog 이상
- Android 7.0 (API 24) 이상
- Google Maps API 키

### 설치
1. 프로젝트 클론
2. `secrets.properties` 파일에 Google Maps API 키 추가
3. Android Studio에서 빌드 및 실행

## 📖 사용 방법
1. 지도에서 목적지 터치
2. 설정에서 알림 거리 조정
3. "추적 시작" 버튼 클릭
4. 목적지 근처에서 자동 알림 수신

## 🛠️ 기술 스택
- Kotlin
- Google Maps SDK
- Google Play Services Location API
- Material Design Components
- Foreground Service

## 📄 라이선스
개인/교육 목적 자유 사용

---
자세한 내용은 [사용 가이드](GUIDE.md)를 확인하세요.
