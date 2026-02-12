# Morse Key (Native Android Accessibility Tool)
**Developed by Pivotal Logic LLC**

### Overview
**Morse Key** is a native Android application designed to empower users with limited mobility by providing an efficient communication bridge. By utilizing a high-performance Accessibility Service model, the app enables users to manage contacts, send SMS messages, and initiate phone calls using specialized Morse code inputs and intuitive gesture navigation.

### Key Features
* **Morse-to-Text Engine:** Translates real-time, timing-based user inputs into alphanumeric characters using an adaptive logic engine.
* **Gesture-Driven Navigation:** Implements custom swipe detection to cycle through contact lists and trigger actions without requiring high-precision tapping.
* **Direct Telephony Integration:** Securely handles `READ_CONTACTS`, `SEND_SMS`, and `CALL_PHONE` permissions to provide a seamless communication experience.
* **Adaptive Timing Logic:** Features a dynamic speed system that adjusts input thresholds (currently targeting 150ms) to match the user's proficiency.
* **Inclusive UI Skins:** Offers multiple visual themes, including Day Mode (High Contrast) and Calm Mode (Low Sensitivity), to support diverse sensory needs.

### Technical Architecture
* **Platform:** Native Android (Kotlin & Java).
* **Frontend:** HTML5/CSS3/JavaScript rendered via a native WebView bridge.
* **Communication:** A robust `ContactInterface` acts as a secure bridge between the web-based UI and native Android OS services.
* **Workflow:** Built using an AI-augmented design approach, leveraging LLMs to optimize complex event-handling loops and accelerate development speed.

### Demo Mode
The application includes a specialized `IS_DEMO_MODE` toggle. When enabled, the app utilizes mock contact data and fictional "555" numbers to facilitate safe screen recordings and technical presentations without exposing real user data.

### Installation & Deployment
Currently in the final staging phase for Google Play Store release. **Pivotal Logic LLC** is in the process of completing D-U-N-S registration for official distribution.
