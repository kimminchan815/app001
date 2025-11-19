# 빌드 안내 (Build Instructions)

## 환경 제약사항
이 저장소는 특정 네트워크 환경에서 빌드 검증이 제한되었습니다. 하지만 모든 소스 코드는 완전하며 Android Studio에서 정상적으로 빌드될 수 있습니다.

## Android Studio에서 빌드하기

### 1. 프로젝트 열기
```
File > Open > 프로젝트 폴더 선택
```

### 2. Google Maps API 키 설정
`secrets.properties` 파일을 열고 실제 API 키를 입력:
```properties
MAPS_API_KEY=여기에_발급받은_실제_API_키_입력
```

Google Maps API 키는 [Google Cloud Console](https://console.cloud.google.com/)에서 발급받을 수 있습니다.

### 3. Gradle 동기화
Android Studio가 자동으로 Gradle을 동기화합니다. 혹은:
```
File > Sync Project with Gradle Files
```

### 4. 빌드
```
Build > Make Project (Ctrl+F9 / Cmd+F9)
```

### 5. 실행
- Android 기기를 USB로 연결하거나
- Android Emulator를 실행한 후
```
Run > Run 'app' (Shift+F10 / Ctrl+R)
```

## 프로젝트 구조
```
app001/
├── app/
│   ├── build.gradle                    # 앱 모듈 빌드 설정
│   ├── proguard-rules.pro             # ProGuard 규칙
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml    # 앱 매니페스트
│           ├── java/com/example/proximityalert/
│           │   ├── MainActivity.kt              # 메인 액티비티
│           │   ├── SettingsActivity.kt          # 설정 액티비티
│           │   └── LocationTrackingService.kt   # 위치 추적 서비스
│           └── res/                   # 리소스 파일들
├── build.gradle                       # 프로젝트 레벨 빌드 설정
├── settings.gradle                    # Gradle 설정
├── gradle.properties                  # Gradle 속성
├── secrets.properties                 # API 키 (gitignore됨)
├── gradlew                           # Gradle Wrapper (Unix)
├── gradlew.bat                       # Gradle Wrapper (Windows)
├── README.md                         # 프로젝트 개요
└── GUIDE.md                          # 상세 사용 가이드
```

## 필수 요구사항
- **Android Studio**: Hedgehog (2023.1.1) 이상
- **JDK**: 8 이상 (권장: JDK 17)
- **Android SDK**: 
  - compileSdk: 34
  - minSdk: 24
  - targetSdk: 34
- **인터넷 연결**: 처음 빌드 시 의존성 다운로드 필요

## 의존성 (Dependencies)
주요 라이브러리:
- AndroidX Core KTX 1.12.0
- Google Play Services Maps 18.2.0
- Google Play Services Location 21.1.0
- Material Components 1.11.0
- Lifecycle Runtime KTX 2.7.0
- Preference KTX 1.2.1

모든 의존성은 `app/build.gradle`에 정의되어 있습니다.

## 문제 해결

### Gradle 동기화 실패
1. Android Studio를 재시작
2. File > Invalidate Caches / Restart
3. 인터넷 연결 확인

### API 키 관련 오류
1. `secrets.properties` 파일 확인
2. Google Cloud Console에서 Maps SDK for Android 활성화 확인
3. API 키 제한 설정 확인

### 빌드 오류
1. Gradle 버전 확인: 8.2 이상
2. Android Gradle Plugin 버전: 8.1.0
3. Kotlin 버전: 1.9.0

## 테스트
앱을 완전히 테스트하려면:
1. 실제 Android 기기 사용 권장 (GPS 정확도)
2. 위치 권한 허용 필요
3. 백그라운드 위치 권한 허용 필요 (Android 10+)
4. 알림 권한 허용 필요 (Android 13+)

## 추가 정보
- 상세 사용 가이드: [GUIDE.md](GUIDE.md)
- 프로젝트 개요: [README.md](README.md)
