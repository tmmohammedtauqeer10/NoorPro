# NoorPro 1.1.3 — editor pro (photo + reel)

Builds on the 1.1.3 in-app editor (7aa1fd5). Everything runs on the phone, offline, with NoorPro's own look
(emerald / gold / cream, Libre Caslon titles). This adds no dependencies and no permissions. Nothing is deployed.

## Reel
- **Live preview**: an ExoPlayer (Media3, already in the app) plays the edit. It plays the clipped playlist at each
  segment's speed and holds on freeze frames. Filters are applied live through an RgbMatrix effect. Music and
  voiceover play with their gains and fades. Text and stickers are drawn on top by Compose. When paused, the editor
  shows the exact still frame. If the device's player fails, the preview falls back to playing without effects.
- **Multi-clip**: add clips from the gallery (copied into the app cache) or record one with the camera app (the
  existing CAMERA flow). Reorder clips with Move earlier / Move later. Split, delete and trim each clip.
- **Transitions**: Cut, Fade, Slide or Zoom, set per cut or for all cuts at once. Shown on the timeline.
- **Speed**: 0.5×–2× plus ramp presets (Montage, Hero, Bullet, Flash in, Flash out), with a ramp graph.
- **Freeze frame**: a 0.5–5 s hold at the playhead.
- **Voiceover**: record takes at the playhead. RECORD_AUDIO is already declared and is only asked for at that point.
  Takes are saved as WAV, are mixed on export, and each has its own volume.
- **Audio mix**: original-sound volume, Al-Noor sound volume, and sound fade in / fade out. Sounds come only from
  the licensed Al-Noor tracks, with an automatic credit.
- **Captions**: manual timed captions. "Spread over reel" splits pasted text into timed lines. Styles: Clean,
  Boxed, Noor, Pop.
- **Text animations**: Typewriter, Fade, Pop, Slide up.
- **Templates**: Reminder, Ayah of the day, Du'a, Jumu'ah, Countdown, Hijri date.
- **Export quality**: Data saver (960p, 2 Mbps), 720p (default), 1080p.

## Photo
- Highlights, shadows, sharpen, fade, grain and tint.
- HSL per colour band.
- Tone curve with 5 points and presets.
- Auto-enhance, which reads the image histogram.
- Tilt-shift and blur brush.
- Layout / collage for 2–6 photos: 7 layouts, spacing, corners, background, and 4:5, 1:1 or 16:9. The collage
  replaces the photos it uses.
- Text:
  - More fonts: italic, light, serif, mono, script, condensed.
  - Styles: Plain, Outline, Shadow, Neon, Gradient.
  - Alignment: left, centre, right.
  - Animations: on a story, animated text is exported as a short motion story.

## Stickers (all offline, no GIPHY, no keys)
- Ayah card: from the app's offline Quran data. Search by text or by `s:a`, with a daily pick.
- Du'a card: from DuaData.
- Hadith card: from the bundled Bukhari files.
- Hijri date, next prayer time (the same snapshot the prayer widgets use), and countdowns to Jumu'ah, Ramadan,
  Eid al-Fitr and Eid al-Adha.
- @mention and #hashtag.
- **Poll / Question / Quiz**: these sit behind `StoryInteractive.enabled = false`, so they are hidden. The
  additive rules and emulator tests are ready, but nothing is deployed.

## General
- Layers panel: reorder, lock and delete layers.
- Snapping guides for centre lines and 0°/90°, with a haptic tick when a guide engages.
- Undo and redo.
- Drafts keep the editing steps (`editJson`, via EditCodec). The originals, extra clips and voice takes are copied
  with the draft, and older drafts still open.

## Not done
- **Reverse playback**: Media3 1.4 has no reverse API. Doing it ourselves would mean decoding every frame to disk.
- **Automatic captions**: no free, offline, file-based speech-to-text works on all supported Android versions
  without a new dependency or model download. Manual timed captions are offered instead.
- **Story-side voting UI for poll/quiz**: this needs the rules to be deployed first.

## Backend (when you choose to enable interactive stickers)
- Deploy the additive `ummah_stories` rules:
  - a third `create` variant with `interactive`;
  - a `responses/{uid}` subcollection.
- Run the tests in `tools/rules-tests/stories-interactive-1.1.3.test.js` (40/40 pass on the emulator).
- Build the viewer UI, then flip `StoryInteractive.enabled`.
