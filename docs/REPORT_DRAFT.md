# VoxBox Project Report — Working Draft

## Title

**VoxBox: A Hybrid Voice-Command and AI-Assisted System for Creating Structured Visual Notes on Android**

## Abstract — draft

Voice input can capture ideas faster than touch typing, but ordinary speech-to-text systems usually produce flat transcripts that require substantial manual formatting. VoxBox is an Android application designed to convert speech into editable, structured visual notes. The system combines Android's native speech recognition with a deterministic, customizable command language called VoxScript. Spoken commands create headings, lists, checklists, highlights, dividers, charts and bounded diagrams, while ordinary speech remains plain dictation. Notes are represented as typed blocks and stored locally, allowing their content, order, labels, values and styles to be edited after capture. An optional AI enhancement layer is planned for summarization, titling, tagging and user-directed reorganization, with preview and explicit acceptance to reduce silent alteration and hallucination risk. The project will evaluate recognition quality, command intent and slot accuracy, end-to-end latency, persistence integrity and physical-device reliability. The offline-first core is intended to provide a dependable zero-budget demonstration independent of an AI API.

## 1. Introduction — draft

Students, teachers and knowledge workers often capture ideas while reading, attending lectures or revising. Typing and manual formatting can interrupt that thought process. Voice dictation reduces input effort, but the resulting transcript commonly lacks hierarchy, emphasis and visual structure. The user must later add headings, lists, separators, colors and diagrams by hand.

VoxBox addresses this gap by treating selected spoken phrases as commands rather than ordinary content. The project explores whether a small deterministic command language can make voice-created notes immediately usable while retaining editability and predictable behavior. Generative AI is positioned as an optional enhancement for tasks that require semantic interpretation, not as a dependency for basic note creation.

## 2. Problem statement

Existing dictation workflows are effective at capturing words but do not reliably create structured visual notes during speech. Manual post-processing reduces the time advantage of voice capture. A practical mobile solution must also handle speech-service variability, runtime permissions, ambiguous phrases, local persistence, user customization and the risk that AI may change meaning.

## 3. Objectives

1. Build a native Android application that captures speech through visible, bounded sessions.
2. Design VoxScript to distinguish plain dictation from deterministic formatting and visual-note commands.
3. Store notes as editable typed blocks rather than flat formatted text or images.
4. Support structured text, highlights, pie/bar/progress charts and bounded flow/relationship diagrams.
5. Allow configurable wake words, aliases, colors and formatting preferences.
6. Save, organize, search and recover notes locally.
7. Add an optional AI provider for faithful summarization and user-instructed reorganization after the offline core is stable.
8. Evaluate speech, parser, system and later AI quality using documented metrics and two-pass verification.

## 4. Scope and limitations

The first release targets a single Android user and local note storage. Speech recognition availability and offline behavior depend partly on the recognition service installed on the device. Android documents that the offline preference may be ignored by an implementation and that `SpeechRecognizer` is not intended for continuous recognition. Therefore VoxBox uses push-to-talk or bounded listening sessions rather than an always-on background microphone.

To maintain a feasible two-month scope, charts are limited to pie, bar and progress blocks. Diagrams are limited to flowcharts and relationship maps. Arbitrary generated diagrams, cloud synchronization and real-time collaboration are excluded.

## 5. Proposed methodology

### 5.1 Native speech capture

The application requests `RECORD_AUDIO` at the point of use and handles denial without making the editor unusable. It prefers an on-device recognizer when available and otherwise uses the system speech-recognition service. Recognition callbacks expose partial and final transcript state. Recognizer resources are destroyed when no longer needed.

### 5.2 VoxScript parsing

Recognized text is normalized and matched against a versioned set of intents and slot rules. The parser returns plain dictation, a parsed command, an ambiguous result or rejection. Chart commands extract typed values such as percentage, label and color. Invalid or incomplete commands are not guessed silently.

### 5.3 Block-based notes

Notes contain ordered typed blocks. Text, list, chart and diagram payloads are stored separately from presentation style, enabling later edits without repeating the original speech. Room provides local persistence and supports search and recovery after relaunch.

### 5.4 Optional AI enhancement

An interface isolates the AI provider from the note domain. Planned actions include summary, title, tags and instruction-based reorganization. The original transcript and note version remain available, and the user previews a proposed change before applying it. API credentials must not be stored in source control.

## 6. Proposed system architecture

The application uses Jetpack Compose, ViewModels and unidirectional data flow. Use cases coordinate separate speech, parser, renderer, repository and AI-provider interfaces. This separation improves testability and prevents a service change from altering the note model.

## 7. Evaluation plan

The evaluation compares raw transcript capture, deterministic VoxScript formatting and optional AI enhancement. Measures include word error rate, intent accuracy, slot accuracy, structure accuracy, processing latency, persistence/reload integrity and device stability. AI evaluation will add faithfulness, coverage, instruction compliance, hallucination and user acceptance. Results will be reported only after the corpus and protocol are fixed.

## 8. Ethical, privacy and security considerations

Microphone use is visible and limited to an active user session. Notes remain local in the baseline. Any later cloud AI action is opt-in and should send only the content required for the selected operation. The original note is preserved, changes are previewed, and credentials are excluded from version control. The application will disclose that recognition service behavior can vary by device.

## 9. Results

### 9.1 Initial native-speech milestone

The first implementation milestone compiled and passed the debug unit-test and APK-assembly tasks. Six targeted VoxScript tests passed for plain dictation, heading, bullet point, a valid pie-chart command, out-of-range percentage and a missing label; the generated starter test also remained passing.

The exact APK was installed on a `2411DRN47I` running Android 16 / API 36. Permission-denied and granted states rendered correctly. Android reported an on-device recognizer, but its current language was unavailable. This revealed an implementation assumption during physical testing. The controller was corrected to switch to the Android system recognizer and to avoid forcing the offline preference on that fallback. A subsequent device run reached the active `Listening…` state with stop and cancel controls, and the system-service/network possibility was disclosed in the UI.

The deterministic sample `Tejas pie chart 25 percent yellow label wheat` produced a yellow 25% wheat sector and white remainder on the physical device. Human-spoken transcription was not measured during automated ADB operation; therefore no word error rate or spoken-command accuracy is claimed yet.

Screenshots and UI hierarchies are stored under `evidence/speech-milestone/`.

## 10. Conclusion

Pending final evaluation.

## References — initial

1. Android Developers. “SpeechRecognizer.” https://developer.android.com/reference/android/speech/SpeechRecognizer
2. Android Developers. “RecognizerIntent.” https://developer.android.com/reference/android/speech/RecognizerIntent
3. Android Developers. “Request runtime permissions.” https://developer.android.com/training/permissions/requesting
4. Android Developers. “Recommendations for Android architecture.” https://developer.android.com/topic/architecture/recommendations

## Evidence to add continuously

- Architecture and data-model diagrams.
- UI and physical-device screenshots.
- VoxScript grammar/version history.
- Test corpus construction and metrics.
- Build, lint, unit-test and instrumentation results.
- Device model/Android version and speech-service configuration.
- Error analysis and limitations.
- AI comparison protocol and results after provider integration.
