# GateAssist Android v1.0.0

Native Android v1 for multilingual airport gate announcements.

## Core flow
Select flight → choose announcement → enter/confirm operational details → safety check → choose language(s) → play/stop.

## v1 features
- Sample HK Express and Japan Airlines demo flights
- Start Boarding
- Final Call
- Passenger Paging
- Flight Delay
- Gate Change
- Volunteer Request
- English, Japanese and Cantonese display text
- Android device TTS with individual Play, Play All, and Stop
- Sequential language playback that changes TTS locale between utterances
- Safety validation blocks playback when required information is missing
- English pronunciation normalization for flight and gate numbers
- Responsive scrollable phone UI
- Unit tests, Android lint and emulator smoke test through GitHub Actions

## Voice note
GateAssist v1 uses Android's installed Text-to-Speech engine to remain free and work without a paid API. English/Japanese/Cantonese voice quality and availability depend on the TTS engine and language packs installed on the phone. Cantonese needs a TTS voice supporting `yue-HK`.

## Operational safety
This v1 is a development/pilot build and is not airline/airport-approved for live PA operation. The bundled schedules are demo data only. Always verify operational facts and airline-approved wording.
