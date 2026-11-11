# Licensed Academy lectures

Checked 2026-11-11. The catalog now has 12 courses/program entries: 24 original offline reading lessons, one licensed in-app lecture, and 10 external classrooms/directories.

## How to Seek Knowledge in Islam

- Creator/attribution: **Al Madrasatu Al Umariyyah**.
- Original publication: https://www.youtube.com/watch?v=wx-X0wK4khs
- Media and licensing evidence: https://commons.wikimedia.org/wiki/File:How_To_Seek_Knowledge_in_Islam-.webm
- License: **CC BY 3.0**, https://creativecommons.org/licenses/by/3.0/
- Commons records a YouTubeReviewBot license check on 2020-12-31. This is a license check, not a complete independent copyright audit.
- Original duration: 19 min 1 sec. WebM VP9/Opus, approximately 97 MB (101,199,553 bytes).
- Video unchanged. NoorPro adds a separate written reflection prompt and personal study log. No endorsement or partnership is claimed.
- Media streams directly from Wikimedia after an explicit user tap. The video is not bundled in the APK or Git repository. Network availability and device codec support affect playback.

The study room displays the creator, license, change notice and clickable source/license links. Media3 provides play/pause and seeking. Playback pauses when the app loses foreground and the player releases on leaving the lesson. Completion is self-reported, not inferred from playback or certified by the creator.

New media needs its own verified rights record; this license does not apply to the creator's other videos. The parser currently accepts only CC BY 3.0 WebM media on upload.wikimedia.org with Commons evidence. The existing external courses remain links, with no media extracted or rehosted.

## Verification

Run the learning catalog/progress and Compose tests plus `:app:assembleDebug`. The lecture screen test checks playback entry, attribution/license access and initial completion gating without downloading media. The remote original returned HTTP 200, `video/webm`, byte-range support and the expected content length on the check date. Real-device decoding and full playback are not covered by the desktop tests.
