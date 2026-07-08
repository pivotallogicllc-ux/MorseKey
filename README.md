Morse Key: Native Android IME & Accessibility Framework
​Developed by Pivotal Logic
​Overview
​Morse Key is a high-performance Android communication framework designed to translate low-latency, timing-based inputs into alphanumeric characters. Built to empower users with limited mobility, the project bridges a custom native Android InputMethodService (IME) with a secure telephony and SMS management system.
​Rather than relying on standard UI paradigms, this architecture solves complex temporal and spatial constraints, delivering a zero-latency experience through strict thread management and dynamic state observation.
​Core Infrastructure & Architecture
​Custom IME Framework (InputMethodService): Engineered a system-wide custom keyboard architecture from the ground up in Kotlin, allowing the custom engine to operate globally across the Android OS.
​Zero-Latency Rendering Loop: Bypassed standard Android XML state delays by implementing a custom 16ms (60fps) visual feedback loop on the main thread via Handler and Looper, ensuring immediate proprioceptive feedback for users.
​Concurrency & Race Condition Prevention: Developed a strict activeView multithreading lock system to safely manage multi-touch hardware inputs, preventing runaway background timers and memory leaks during simultaneous asynchronous gesture events.
​Adaptive Timing Engine: Abstracted temporal logic away from hardcoded milliseconds into a dynamic tUnit mathematical engine. The framework actively profiles and calculates a weighted average of the user's input speed, scaling the entire system's thresholds in real-time to match user proficiency.
​Extensible Modality Abstraction: Built a scalable Mode state-machine (Single, Split, Swipe) that maps diverse geographic and temporal gestures (Tap, Hold, Swipe Left/Right) into a unified buffer stream.
​Application Layer Features
​Direct Telephony Integration: Securely handles READ_CONTACTS, SEND_SMS, and CALL_PHONE system permissions to provide a seamless, native communication bridge.  
​Hybrid Frontend UI: The standalone application layer features an HTML5/CSS3/JavaScript interface rendered via a highly optimized native WebView bridge.  
​Inclusive UI Skinning Engine: Supports dynamic visual state management, offering diverse themes like Day Mode (High Contrast) and Calm Mode (Low Sensitivity) to support varied sensory needs.  
​Development & Testing Tools
​Presentation Sandboxing: Includes a specialized IS_DEMO_MODE architectural flag. When toggled, the application dynamically injects mock contact data and fictional "555" numbers into the UI state, allowing for safe CI/CD testing, screen recording, and technical presentations without exposing real user data.

