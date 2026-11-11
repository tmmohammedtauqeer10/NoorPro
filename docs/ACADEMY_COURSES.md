# NoorPro Academy

NoorPro now has a native Android course hub, accessible from **Explore → NoorPro Academy**, the Islamic Learning screen, and the **Courses** tile in Knowledge & Growth.

## Initial release

- Eleven course/program entries: one original offline reading course and ten official external course, lesson, or curriculum links.
- Twenty-four offline Ihsan and character lessons, twelve modules, primary references and twenty-four knowledge checks, adapted from the user's Deenflow learning content.
- Four suggested learning paths; search; subject and language filters; My learning.
- Course overview, provider attribution, requirements, reading lists and classroom links.
- Account-scoped local enrollment, completion marks and optional notes. Reading lessons require a correct knowledge check before the first completion mark.
- External course study is explicitly self-reported. The feature does not receive provider attendance, exam scores or certificates.
- A personal study record can be shared after all original reading lessons are completed. It is not an accredited certificate, an ijazah, or a provider credential.

The classroom interface and original reading course are in English. External offerings include English, Urdu and Arabic. Language filters describe the course language, not a translation of the entire NoorPro interface.

## Free sources checked on 11 November 2026

| Provider | Included offerings | Access | Integration used |
| --- | --- | --- | --- |
| [Understand Al Quran Academy](https://understandquran.com/) | Quran & Salah Course 1 in English and Urdu; Seerah in English; Tajweed in Urdu | Listed in its free-course catalog. Provider login is needed for enrollment. Paid tutoring, products and books are separate. | Official classroom links; short original catalog descriptions and attribution. |
| [Madinah Arabic](https://madinaharabic.com/free-content/reading) | Arabic reading and grammar | Free self-study pages. One-to-one tutoring is a separate service. | Official course links. |
| [Learning Islam / Taa](https://learningislam.com/) | Essential knowledge learning path; Hadith & Sunnah public lessons | Public lesson pages; provider accounts handle its progress, competitions and certificates. | Official learning-path/lesson links. |
| [SeekersGuidance](https://academy.seekersguidance.org/) | Islamic Studies Curriculum directory | The academy describes free curricula and provider-issued certificates after its own assessments. Individual course enrollment can vary. | Directory link, clearly identified as a program rather than a guaranteed-open individual class. |
| [Harvard University / edX](https://pll.harvard.edu/course/islam-through-its-scriptures) | Islam Through Its Scriptures | Free audit option; verified certificate is paid. edX controls enrollment and audit limits. | Official Harvard listing link, explicitly labeled academic religious studies. |

### Exact course/lesson destinations

1. [Understand Quran & Salah, Course 1 — English](https://understandquran.com/courses/course-1-understand-al-quran-salah-the-easy-way/)
2. [Understand Quran & Salah, Course 1 — Urdu](https://understandquran.com/courses/course-1-understand-al-quran-salah-the-easy-way-urdu/)
3. [Seerah — English](https://understandquran.com/courses/seerah-english/)
4. [Tajweed — Urdu](https://understandquran.com/courses/learn-tajweed-the-easy-way-urdu/)
5. [Arabic Reading](https://madinaharabic.com/free-content/reading)
6. [Arabic Grammar](https://madinaharabic.com/free-content/grammar)
7. [Essential Islamic Knowledge — Arabic learning path](https://learningislam.com/track/1)
8. [Hadith & Sunnah — Arabic lesson collection](https://learningislam.com/topic/20/lessons)
9. [SeekersGuidance curriculum directory](https://academy.seekersguidance.org/)
10. [Islam Through Its Scriptures — Harvard/edX](https://pll.harvard.edu/course/islam-through-its-scriptures)

Some older SeekersGuidance links were excluded after their current destinations reported unavailable courses or disabled enrollment. These included the old Meccan Dawn and Medinan Lights links and the older Ascent to Felicity enrollment page. They are not advertised as currently accessible courses in the app. Availability and provider terms can change, so recheck before each catalog release.

## What can be applied to our platform?

The implemented route is a curated discovery and personal study layer: NoorPro introduces a course, identifies its provider, opens the official classroom and lets the learner keep a personal log. Provider enrollment happens on that provider's own site. NoorPro does not submit applications, create external accounts, collect provider passwords or claim an institutional relationship.

Free access does not establish permission to copy lectures, subtitles, thumbnails, textbooks or exams. This release includes no copied third-party video/audio, scraped classroom transcripts, provider branding assets, or circumvention of login controls. The original offline reading course is clearly described as reading; it is not presented as a recorded scholar lecture series.

For future native playback, use media only when an explicit license or written agreement allows the proposed embedding, distribution, caching and commercial use. Retain attribution and any restrictions per asset. An embeddable player alone does not establish download or rehosting rights.

[Learning Islam publishes an API integration reference](https://learningislam.com/api-documentation). Its linked API access currently leads to account login. This is a potential direct integration, but API credentials, content permissions, attribution, redistribution rights, rate limits and terms must be established before implementing a client. No API credentials or undocumented assumptions are included in NoorPro.

No provider has endorsed NoorPro, and no message requesting a partnership has been sent.

## Code and data

- `app/src/main/assets/learning/catalog.json`: versioned offline catalog, original reading lessons, checkpoints, resources, provider access notes and suggested paths.
- `learning/LearningCatalog.kt`: typed models, parser, cross-reference checks, safe HTTPS validation and progress calculations.
- `learning/LearningProgressStore.kt`: account-scoped SharedPreferences store with copied sets, bounded notes and course-specific reset.
- `learning/AcademyScreen.kt`: discovery, paths, My learning, course overview, study room and local record sharing.
- `DeenScreen.ACADEMY` and `MainActivity`: app navigation route.

No new Gradle dependency, Firebase rule or production deployment is required. Catalog changes ship with the Android app. Remote course publishing, instructor uploads, live classes, payments and server-verified accreditation are outside this initial release.

## Storage and privacy

Each Firebase user gets a separate local preference namespace derived from a SHA-256 of their user ID. Guests share the local guest namespace. Account changes dispose the previous account's learning UI before loading the new account's state. This feature does not sync progress or notes to a provider or a NoorPro server.

Optional notes are limited to 2,000 characters per learning activity and require an explicit save. Unsaved notes are not automatically committed. Resetting a course deletes its completion marks and notes while retaining its My learning entry. Standard device backup and storage behavior follow the host app's existing Android settings; the notes are not a secure diary. Opening provider websites is subject to those sites' policies.

## Adding a course

1. Verify the official destination and record the check date.
2. Use original short descriptive text. Name the provider and instructor accurately; do not invent duration, ratings, enrollment numbers or endorsements.
3. Label a program directory as `directory`; do not pretend it is one course or promise enrollment availability.
4. Set `reuse` to `external_link_only` unless actual content rights have been established. External lessons need safe HTTPS links; do not supply copied media through this format.
5. For an original reading lesson, include references and a four-option knowledge check with a zero-based `correct_index`.
6. Keep stable IDs and valid provider/path references. Do not reuse another course's lesson IDs.
7. Run the feature tests and Android build before pushing.

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests 'com.noorpro.app.learning.*' :app:assembleDebug --console=plain --no-daemon --no-configuration-cache
```

The tests exercise catalog parsing, external-resource boundaries, safe URLs, completion calculations, persisted progress, account isolation, note limits, scoped reset, search and quiz-gated completion. With `-Proborazzi.test.record=true`, UI tests capture light/dark catalog and reading-room screenshots under `app/build/outputs/academy/` for visual review.

The original reading content remains an introductory editorial draft. A qualified scholarly review is still needed before describing it as an endorsed curriculum. Public provider accounts and their protected lectures were not accessed on the user's behalf.

## Clean-checkout build repairs

The initial build exposed existing source-tree issues outside Academy: adaptive launcher XML referenced a missing mipmap instead of the existing drawable; an audio screen passed the named parameter `Modifier` instead of `modifier`; several screens referenced artwork absent from the repository. The small build-repair changes correct the references and supply neutral vector fallbacks through `drawable_fallbacks.xml`. These are generic illustrations, not reciter portraits. Replace aliases with approved artwork if the original assets become available. No third-party image downloads were introduced.

To record screenshots explicitly:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests 'com.noorpro.app.learning.AcademyScreenTest' '-Proborazzi.test.record=true' --no-configuration-cache
```

## Verification on 11 November 2026

- `:app:assembleDebug` passed and produced a debug APK containing all eleven catalog entries.
- All ten Academy tests passed: eight catalog/progress/privacy checks and two native Compose interaction tests.
- Screenshot recording passed separately for both Compose tests. Light/dark catalog screens and the dark study room were visually inspected at a 412 x 915 dp viewport.
- No physical-device install, live provider login, external enrollment or production release was performed.
