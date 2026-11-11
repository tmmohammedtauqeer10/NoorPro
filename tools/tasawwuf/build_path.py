"""Builds assets/content/tasawwuf_path.json from Deenflow's tasawwuf_api.json (MIT, github.com/tmmohammedtauqeer10/Deenflow).
Lesson text, checks, glossary, FAQs and sources are taken unchanged; NoorPro adds the Ahl as-Sunnah orientation card and
maps the books that are bundled offline to the in-app reader."""
import json, os
HERE = os.path.dirname(os.path.abspath(__file__))
src = json.load(open(os.path.join(HERE, 'tasawwuf_api.json'), encoding='utf-8'))
READER = {'tas_book_beginning': 'bidaya', 'tas_book_nawawi': 'nawawi40', 'tas_book_alchemy': 'kimiya'}
ORIENTATION = {
    'title': 'In the way of Ahl as-Sunnah wa al-Jama\'ah',
    'body': ('Tasawwuf is the science of ihsan: purifying the heart and perfecting worship within the Shariah, upon the creed of '
             'Ahl as-Sunnah (Ash\'ari and Maturidi) and the four madhhabs. The great silsilas — Qadiri, Chishti, Naqshbandi and '
             'Suhrawardi — are chains of teachers reaching back to the Prophet ﷺ, and the awliya of Allah are loved and respected '
             '(Quran 10:62-63). This path is a gentle introduction; those who wish to go further traditionally take a qualified '
             'shaykh of a recognised silsila, and every practice is weighed by the Quran and Sunnah.'),
    'sources': ['Quran 10:62-63', 'Sahih Muslim 8 (Hadith of Jibril)', 'al-Qushayri, al-Risala', 'al-Hujwiri, Kashf al-Mahjub'],
}
out = {k: src[k] for k in ['title', 'description', 'estimated_weeks', 'lessons_per_week', 'weekly_plan', 'learning_outcomes', 'stages',
                           'modules', 'lessons', 'glossary', 'faqs', 'sources', 'teacher_guidance', 'review_note', 'editorial_status']}
out['orientation'] = ORIENTATION
out['credit'] = 'Learning path from Deenflow by T.M Mohammed Tauqeer (MIT licence). Original educational summaries; not a fatwa.'
out['books'] = [dict(id=b['id'], title=b['title'], author=b['author'], level=b.get('level', ''), description=b.get('description', ''),
                     guidance=b.get('reading_guidance', ''), url=b.get('url') or '', reader=READER.get(b['id'], ''))
                for b in src['books']]
ids = {l['id'] for l in out['lessons']}
assert all(i in ids for m in out['modules'] for i in m['lesson_ids'])
assert all(0 <= l['checkpoint']['correct_index'] < len(l['checkpoint']['options']) for l in out['lessons'])
dst = os.path.join(HERE, '..', '..', 'app', 'src', 'main', 'assets', 'content', 'tasawwuf_path.json')
json.dump(out, open(dst, 'w', encoding='utf-8'), ensure_ascii=False, separators=(',', ':'))
print(len(out['lessons']), 'lessons', len(out['modules']), 'modules', len(out['glossary']), 'terms', len(out['faqs']), 'faqs', len(out['books']), 'books', os.path.getsize(dst), 'bytes')
