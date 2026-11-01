# SmartBite - Smart Nutrition & Campus Food Ordering System

SmartBite is a modern, responsive Android application built with **Jetpack Compose** following the **MVVM (Model-View-ViewModel)** architectural pattern. It delivers an intelligent dietary recommendation engine, AI-powered nutritional assistance, hyper-local weather-driven meal suggestions, and streamlined digital food ordering.

---

## 🌟 Key Features

- **Personalized Health-Aware Filtering**: Tailored meal recommendations filtered against chronic conditions (Diabetes, Blood Pressure / Hypertension, High Cholesterol, Alpha-gal Syndrome).
- **Interactive AI Assistant (Smarto)**: Real-time contextual nutritional advisor powered by LLM integration to assist users with healthy dietary choices and allergen queries.
- **Adaptive Climate-Aware Suggestions**: Real-time ambient weather and temperature integration to recommend hydrating, refreshing, or warm meals dynamically.
- **Seamless Checkout & Payment**: Integrated Stripe PaymentSheet along with Cash-On-Delivery (COD) support.
- **Fully Responsive UI**: Built with Jetpack Compose supporting smooth transitions, edge-to-edge layouts, and cross-screen responsiveness across mobile form factors, foldables, and tablets.
- **Dark & Light Mode**: Comprehensive dual-theme support with accessible contrast and custom palette tokens.

---

## 🏗️ Architecture & Tech Stack

SmartBite is developed using modern Android architectural guidelines:

- **Language**: Kotlin 2.1
- **UI Framework**: Jetpack Compose (Material Design tokens)
- **Architecture**: MVVM (Model-View-ViewModel) with unidirectional data flow (UDF)
- **State Management**: Kotlin Coroutines, `StateFlow`, `SharedFlow`, and `collectAsStateWithLifecycle`
- **Dependency & SDK Standards**:
  - Gradle Version Catalog (`libs.versions.toml`)
  - Kotlin Symbol Processing (KSP)
- **Networking & Data Persistence**:
  - Retrofit 2 & Gson
  - Firebase Firestore & Cloud Messaging (FCM)
  - Coil for asynchronous image loading
- **Payments**: Stripe Android PaymentSheet SDK

---

## 📁 Project Structure

```text
app/src/main/java/com/example/smartbitesample/
├── data/                  # Data repositories and networking services
│   ├── ChatRepository.kt # Groq AI chat service integration
│   ├── EmailService.kt   # SMTP email dispatch for OTP verification
│   ├── OrderRepository.kt# Firestore order synchronization
│   └── UserRepository.kt # Firestore user authentication & profiles
├── ui/                    # Presentation layer (MVVM)
│   ├── auth/             # Authentication screens & AuthViewModel
│   ├── chat/             # AI chat UI & ChatViewModel
│   ├── components/       # Reusable responsive Compose components & dialogs
│   ├── home/             # Home dashboard, OrderViewModel & HomeViewModel
│   ├── profile/          # User profile view & ProfileViewModel
│   ├── splash/           # Animated splash entry
│   └── theme/            # Theme, Color palette, and Typography tokens
├── Database.kt            # Room database definitions
├── FoodData.kt            # Food entity models & mock catalogues
├── MainActivity.kt        # Single-activity container & Navigation host
└── WeatherService.kt      # Open-Meteo REST API client
```

---

## 🚀 Getting Started

### Prerequisites

1. Android Studio Ladybug | 2024.2.1 or newer.
2. Android SDK 35 / 36 (`compileSdk = 36`, `minSdk = 24`).
3. JDK 11 or newer configured in Gradle JVM settings.

### Configuration

Create a `local.properties` file in the root directory (never commit this file) with the following structure:

```properties
sdk.dir=/path/to/your/Android/Sdk

# API Credentials & Config
groq.api.key=YOUR_GROQ_API_KEY
gmail.sender.email=your-service-email@gmail.com
gmail.app.password=your-app-specific-password
stripe.publishable.key=pk_test_yourStripeKey
```

Add your `google-services.json` to the `/app` directory for Firebase integration.

---

## 🛡️ License

This project is licensed under the MIT License - see the LICENSE file for details.
