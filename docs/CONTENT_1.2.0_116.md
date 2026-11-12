# NoorPro 1.2.0 (116) — Deenflow merge, Tasawwuf path, Adhkar / Ruhani / Darood upgrade

## Sources and licences
| Content | Source | Licence / basis |
|---|---|---|
| Tasawwuf learning path (24 lessons, 12 weeks, checks, glossary, FAQ, book guide) | Deenflow `tasawwuf_api.json` (user's own repo) + NoorPro orientation card | MIT (tools/tasawwuf/LICENSE-Deenflow-MIT) |
| Quiz: 122 Deenflow questions (98 with native Urdu/Hindi/Arabic) | Deenflow quiz packs | MIT; sources in tools/quizbank/deenflow_src/quiz_sources.md |
| Bidayat al-Hidayah (Arabic), al-Arba'in an-Nawawiyyah (Arabic) | Arabic Wikisource transcriptions | CC BY-SA 4.0, attributed in-app and in NOTICE.txt |
| Adhkar (66 items: morning 18, evening 16, after salah 11, daily 21) | Quran (bundled Uthmani text) and the hadith collections, as compiled in Hisn al-Muslim and an-Nawawi's al-Adhkar | Classical texts (public domain); every hadith text machine-checked against the bundled Arabic corpus by tools/adhkar/build_spiritual.py |
| English / Urdu / Hindi meanings for Adhkar, Ruhani Ilaj, Darood | NoorPro's own translations (tools/adhkar/tr_*.py, adhkar_data.py) | Own work |
| Quranic Urdu | Kanz-ul-Iman, Imam Ahmad Raza Khan (d. 1921) | Public domain |
| Qasidat al-Burda | Arabic of Imam al-Busiri (d. 696 AH); chapter summaries by NoorPro | Public domain + own work |
| Hizb al-Bahr | Arabic of Imam ash-Shadhili (d. 656 AH); summary by NoorPro | Public domain + own work |

Removed in this release: the bundled third-party English (the modern "Ahmed Raza Khan translation" Quran English, sunnah.com hadith English,
salawat.com / pray.islamic.network English, and Shaykh Zakariyya Kandhlawi's Burda English). Transliterations of Quranic items remain from
alquran.cloud (a transliteration of the Arabic, not a translation).

## Deenflow library_api.json / audio_api.json — not adopted
* library_api.json: 16 PDF books (Sealed Nectar, Martin Lings, Ibn Kathir English, Hisn al-Muslim English, Taqi Usmani, etc.) re-hosted on
  Google Drive. These are modern copyrighted editions with no licence to redistribute, so nothing was taken.
* audio_api.json: Archive.org uploads (Sealed Nectar narration, an Urdu Seerah series, Wahiduddin Khan lectures) with no stated licence, plus
  REPLACE_ME placeholders for Tafsir and Naat. Per the brief (no MP3Quran, YouTube rips or Creative Commons audio, licensed only) nothing was taken.

## Ruhani Ilaj policy
Only Quranic ayat, masnoon duas and accepted Sunni practices, each with a reference. The section disclaimer tells users to see a doctor for
illness and states that NoorPro does not sell or endorse amulets, paid healing services or guaranteed-cure claims.

## Rebuild
`python3 tools/adhkar/build_spiritual.py` (originals cached in tools/adhkar/orig/), `python3 tools/tasawwuf/build_path.py`,
`python3 tools/books/deenflow_books.py`, `python3 tools/quizbank/build.py`.

## Release check (12 Nov 2026)
* AAB: NoorPro-1.2.0-v116.aab (app-release.aab, built 12 Nov 2026 16:22 IST), versionCode 116, versionName 1.2.0, signed with the upload key (jarsigner verified).
* SHA-256: daaa35204bf7dbea53f0e50eb1034007d56bc5adf374241b25cc594d5431447e (80,618,258 bytes)
* Tests: v116 + v115 unit/Robolectric 30/30 (after the preview tap fix); Ask a Scholar rules 34/34.
* Firestore rules: v115 rules + storage deployed 11 Nov (approved); isScholar() fix (26e7a8d) deployed 12 Nov ~14:50 IST (approved). No admins/{uid} doc created.
* Previews: 5 key screens (adhkar morning, daily duas in Hindi, Tasawwuf path home, lesson check, darood with Friday section).
* Intended track: Internal testing (upload done separately in Play Console; Production untouched).
