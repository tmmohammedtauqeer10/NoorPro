# 1.1.3 — In-app photo & reel editor (composer)

Opened from the composer: **Edit** badge on every photo, **Edit reel** button on reels/stories.
The editor only produces the final media file; upload/post code is unchanged.

## Photo editor (`NoorPhotoEditor`, `PhotoEditorHost`)
- Crop: Free, 1:1, 4:5, 16:9; drag to move, pinch to zoom; rotate 90°.
- Filters (NoorPro names): Original, Fajr, Noor, Madinah, Sahara, Layl, Zamzam, Andalus, Qamar, Rawdah + strength.
- Adjust: brightness, contrast, warmth, saturation, vignette.
- Text: Classic (Libre Caslon), Modern / Strong (Plus Jakarta Sans), Naskh (Noto Naskh Arabic, auto for Arabic);
  9 colours, Plain / Pill / Soft background; drag, pinch-scale, two-finger rotate; word-wraps.
- Stickers (drawn in code, original art): crescent, lantern, mosque, Jumu'ah Mubarak, Ramadan Kareem,
  Alhamdulillah, SubhanAllah, location (composer location), time, hashtag (first hashtag).
- Draw: brush colours, width, glow.
- Undo / redo / reset (50 steps). Done renders a JPEG (≤2048 px, q92) into cache and replaces the photo uri.
  Re-editing starts from the original (kept in `photoOrigins`).

## Reel editor (`NoorReelEditor`, `ReelEditorHost`)
- Trim (range slider on a filmstrip), split at playhead, delete clip.
- Speed 0.5x / 1x / 1.5x / 2x (text timings follow the speed).
- Filters + strength + vignette, timed text and stickers (on-screen range per layer).
- Original sound: keep / volume / mute.
- Optional sound: only Al-Noor Audio tracks licensed for reuse (Public domain / CC0 / CC BY, direct https file,
  never YouTube, never stream-only SOURCE_TERMS, never podcast RSS). Credit is written to the reel's
  existing "Sound: …" credit field.
- Cover frame.
- Export with Media3 Transformer 1.4.1 (H.264/AAC, long side ≤1280) with progress + cancel.
  A plain single trim skips export and keeps the 1.1.1 trim-at-post path.

## Limits
- Preview is a scrubbed frame at the playhead (no live playback inside the editor).
- Drafts keep the edited file, not the edit steps.
- Brush strokes are relative to the cropped area (changing the crop afterwards moves them).

No new dependencies, no new permissions, no backend change.
