# 앱 아키텍처 및 동작 흐름

## 📐 앱 구조도

```
┌─────────────────────────────────────────────────────────────┐
│                      ProximityAlert App                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ├─────────────────────────────────┐
                              │                                 │
                    ┌─────────▼──────────┐         ┌──────────▼─────────┐
                    │   MainActivity      │◄────────│  SettingsActivity  │
                    │                     │         │                    │
                    │ - Google Maps       │         │ - Preferences      │
                    │ - 목적지 설정        │         │ - 알림 거리 설정    │
                    │ - 권한 관리          │         │ - 음량 조절         │
                    │ - 추적 시작/중지     │         │ - 진동/소리 설정    │
                    └─────────┬──────────┘         └────────────────────┘
                              │
                              │ startService()
                              │
                    ┌─────────▼──────────────────────────────────────┐
                    │     LocationTrackingService                    │
                    │     (Foreground Service)                       │
                    │                                                │
                    │  ┌──────────────────────────────────────────┐ │
                    │  │  Location Updates (10초마다)              │ │
                    │  │                                           │ │
                    │  │  1. FusedLocationProviderClient          │ │
                    │  │  2. 현재 위치 ↔ 목적지 거리 계산          │ │
                    │  │  3. 알림 레벨 판단                       │ │
                    │  │     - Level 1: 1km                       │ │
                    │  │     - Level 2: 500m                      │ │
                    │  │     - Level 3: 200m                      │ │
                    │  │  4. 알림 발송 (소리 + 진동)              │ │
                    │  └──────────────────────────────────────────┘ │
                    │                                                │
                    └────────────────────┬───────────────────────────┘
                                        │
                    ┌───────────────────▼────────────────────────────┐
                    │          Android System Services              │
                    │                                                │
                    │  • NotificationManager (알림 표시)             │
                    │  • AudioManager (음량 제어)                    │
                    │  • Vibrator (진동)                            │
                    │  • LocationManager (GPS)                       │
                    └────────────────────────────────────────────────┘
```

## 🔄 사용자 흐름

```
1. 앱 실행
   │
   ├─→ 권한 요청
   │   ├─ 위치 권한 (필수)
   │   ├─ 백그라운드 위치 권한 (필수, Android 10+)
   │   └─ 알림 권한 (필수, Android 13+)
   │
   ├─→ 지도 표시 (현재 위치)
   │
2. 목적지 설정
   │
   └─→ 지도 터치 → 마커 표시
       │
3. (선택) 설정 조정
   │
   └─→ 설정 버튼 → SettingsActivity
       ├─ 알림 거리 조정
       ├─ 음량 설정
       └─ 진동/소리 on/off
       │
4. 추적 시작
   │
   └─→ "추적 시작" 버튼
       │
       ├─→ LocationTrackingService 시작
       │   └─ Foreground notification 표시
       │
       ├─→ 백그라운드에서 위치 추적
       │   └─ 10초마다 위치 업데이트
       │
       └─→ 거리 계산 및 알림
           │
           ├─ 1km 전: Level 1 알림 (낮은 음량)
           ├─ 500m 전: Level 2 알림 (중간 음량)
           └─ 200m 전: Level 3 알림 (최대 음량)
           │
5. 목적지 도착
   │
   └─→ "추적 중지" 버튼 → 서비스 종료
```

## 🔧 기술적 구현 세부사항

### MainActivity.kt
```kotlin
주요 메서드:
├─ onCreate()
│  ├─ ViewBinding 초기화
│  ├─ FusedLocationProviderClient 생성
│  ├─ MapFragment 초기화
│  └─ 버튼 리스너 설정
│
├─ onMapReady(GoogleMap)
│  ├─ 맵 설정
│  ├─ 현재 위치 활성화
│  └─ 클릭 리스너 설정
│
├─ setDestination(LatLng)
│  ├─ 마커 생성/업데이트
│  └─ UI 업데이트
│
├─ startTracking()
│  ├─ 백그라운드 위치 권한 확인
│  ├─ Intent 생성 (목적지 좌표 포함)
│  └─ Service 시작
│
└─ checkAndRequestPermissions()
   ├─ 위치 권한 확인/요청
   └─ 알림 권한 확인/요청
```

### LocationTrackingService.kt
```kotlin
주요 메서드:
├─ onStartCommand(Intent)
│  ├─ 목적지 좌표 추출
│  ├─ Foreground notification 시작
│  └─ 위치 업데이트 시작
│
├─ startLocationUpdates()
│  ├─ LocationRequest 생성
│  │  ├─ 우선순위: HIGH_ACCURACY
│  │  ├─ 간격: 10초
│  │  └─ 최소 간격: 5초
│  └─ requestLocationUpdates()
│
├─ checkProximity(Location)
│  ├─ 거리 계산 (haversine)
│  ├─ 알림 레벨 판단
│  └─ triggerProximityAlert() 호출
│
├─ triggerProximityAlert(level, distance)
│  ├─ 음량 계산 (레벨별)
│  ├─ Notification 생성
│  ├─ 진동 패턴 실행
│  └─ 소리 재생
│
└─ updateForegroundNotification(distance)
   └─ 실시간 거리 표시 업데이트
```

### SettingsActivity.kt
```kotlin
PreferenceFragmentCompat 사용:
├─ onCreatePreferences()
│  └─ preferences.xml 로드
│
저장되는 설정값:
├─ alert_distance_1 (String → Float)
├─ alert_distance_2 (String → Float)
├─ alert_distance_3 (String → Float)
├─ alert_volume (Int)
├─ vibrate_enabled (Boolean)
└─ sound_enabled (Boolean)
```

## 📊 데이터 흐름

```
User Input → MainActivity
              │
              ├─→ Destination (LatLng)
              │   └─→ Intent → LocationTrackingService
              │
Settings → SharedPreferences
              │
              ├─→ alert_distance_1, 2, 3
              ├─→ alert_volume
              ├─→ vibrate_enabled
              └─→ sound_enabled
              │
GPS → FusedLocationProvider → Location
                                  │
                                  └─→ LocationTrackingService
                                      │
                                      ├─→ Distance Calculation
                                      └─→ Notification Trigger
                                          │
                                          ├─→ NotificationManager
                                          ├─→ AudioManager
                                          └─→ Vibrator
```

## 🔐 권한 흐름

```
앱 시작
  │
  ├─→ ACCESS_FINE_LOCATION
  │   └─ 런타임 요청 (ActivityResultContracts)
  │
  ├─→ ACCESS_COARSE_LOCATION
  │   └─ 런타임 요청 (ActivityResultContracts)
  │
  ├─→ POST_NOTIFICATIONS (Android 13+)
  │   └─ 런타임 요청
  │
  └─→ ACCESS_BACKGROUND_LOCATION (Android 10+)
      └─ 추적 시작 시 요청
         └─ 설명 다이얼로그 → 시스템 설정
```

## 🎯 알림 레벨 시스템

```
거리 측정 (meters)
      │
      ├─→ distance <= alert_distance_3 (200m)
      │   └─→ Level 3
      │       ├─ 음량: 100%
      │       ├─ 진동: 500ms × 3회
      │       └─ 제목: "곧 목적지에 도착합니다"
      │
      ├─→ distance <= alert_distance_2 (500m)
      │   └─→ Level 2
      │       ├─ 음량: 75%
      │       ├─ 진동: 300ms × 2회
      │       └─ 제목: "목적지가 가까워지고 있습니다"
      │
      └─→ distance <= alert_distance_1 (1000m)
          └─→ Level 1
              ├─ 음량: 50%
              ├─ 진동: 200ms × 2회
              └─ 제목: "목적지 접근 중"

참고: 각 레벨은 한 번만 발동 (lastAlertDistance 추적)
```

## 🧩 컴포넌트 간 통신

```
MainActivity ←→ LocationTrackingService
     │              │
     │ startService  │
     ├─────────────►│
     │              │
     │ stopService   │
     ├─────────────►│
     │              │
     │              ├─ Notification 표시
     │              └─ User 클릭 → MainActivity

MainActivity ←→ SettingsActivity
     │              │
     │ startActivity │
     ├─────────────►│
     │              │
     │              └─ SharedPreferences 저장

LocationTrackingService ←→ SharedPreferences
                │              │
                │ 읽기          │
                ├─────────────►│
                │              │
                │              └─ alert_distance_*, alert_volume

LocationTrackingService ←→ Android System
                │              │
                ├─────────────► NotificationManager
                ├─────────────► AudioManager
                ├─────────────► Vibrator
                └─────────────► LocationManager
```

## 📱 UI 상태 관리

```
MainActivity UI States:
├─ 초기 상태
│  ├─ 목적지 미설정
│  ├─ "추적 시작" 비활성화
│  └─ "추적 중지" 비활성화
│
├─ 목적지 설정됨
│  ├─ 마커 표시
│  ├─ "추적 시작" 활성화
│  └─ "추적 중지" 비활성화
│
└─ 추적 중
   ├─ "추적 시작" 비활성화
   └─ "추적 중지" 활성화
```

## 🔄 생명주기 관리

```
MainActivity:
├─ onCreate() → 초기화
├─ onMapReady() → 맵 설정
├─ onRequestPermissionsResult() → 권한 결과 처리
└─ Activity 종료 후에도 Service는 계속 실행

LocationTrackingService:
├─ onCreate() → 서비스 초기화
├─ onStartCommand() → 추적 시작
│  └─ START_STICKY (재시작 보장)
├─ onDestroy() → LocationUpdates 제거
└─ Foreground Service로 시스템 종료 방지

SettingsActivity:
├─ onCreate() → Fragment 로드
└─ PreferenceFragment가 자동으로 저장 관리
```

## 🎨 UI/UX 설계

```
activity_main.xml:
┌────────────────────────────────────┐
│                                    │
│         Google Maps Fragment       │
│         (전체 화면 크기)            │
│                                    │
│                                    │
│          [📍 마커: 목적지]          │
│                                    │
│                                    │
└────────────────────────────────────┘
┌────────────────────────────────────┐
│ 목적지: 37.5665, 126.9780          │
│                                    │
│ [  추적 시작  ] [  추적 중지  ]     │
│ [ 목적지 해제 ] [    설정    ]     │
└────────────────────────────────────┘

settings_activity.xml:
┌────────────────────────────────────┐
│ ← 설정                              │
├────────────────────────────────────┤
│ 알림 거리 설정                      │
│   첫 번째 알림 거리    [1000m]     │
│   두 번째 알림 거리    [ 500m]     │
│   세 번째 알림 거리    [ 200m]     │
│                                    │
│ 알림 설정                          │
│   알림 음량           [====|  ]    │
│   진동 사용           [✓]          │
│   소리 사용           [✓]          │
│                                    │
│ 추가 정보                          │
│   앱 정보                          │
│   사용 방법                        │
└────────────────────────────────────┘
```

## 🚀 성능 최적화

```
위치 업데이트:
├─ 간격: 10초 (배터리 절약)
├─ 최소 간격: 5초 (반응성)
└─ 우선순위: HIGH_ACCURACY (정확도)

메모리:
├─ ViewBinding (findViewById 방지)
├─ lazy initialization
└─ Service에서 불필요한 객체 생성 방지

배터리:
├─ Foreground Service (백그라운드 제한 회피)
├─ 효율적인 위치 업데이트 간격
└─ 추적 불필요 시 Service 종료
```

---

이 아키텍처는 Android 모범 사례를 따르며, 사용자 경험과 성능의 균형을 맞추도록 설계되었습니다.
