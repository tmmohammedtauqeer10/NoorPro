"""Converts the Deenflow Arabic Wikisource transcriptions (CC BY-SA 4.0, see manifest.json) into the app's book format
(assets/content/books/<id>.ar.jgz) and registers them in index.jgz. Text is not edited; only markup is removed."""
import gzip, html, json, os, re
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', '..', 'app', 'src', 'main', 'assets', 'content', 'books')
ATTR = ('Arabic text: Wikisource contributors (ar.wikisource.org, revision {rev}), licensed CC BY-SA 4.0 '
        '(creativecommons.org/licenses/by-sa/4.0). Markup and navigation removed; text unchanged. Via Deenflow.')

def clean(fragment):
    t = re.sub(r'<[^>]+>', '', fragment)
    return re.sub(r'\s+', ' ', html.unescape(t)).strip()

def body(path):
    s = open(path, encoding='utf-8').read()
    s = re.sub(r'<!--.*?-->', '', s, flags=re.S)
    s = re.sub(r'<(style|aside|head)[^>]*>.*?</\1>', '', s, flags=re.S)
    return s

HEADINGS = ['القسم الأول في الطاعات', 'فصل في آداب الاستيقاظ من النوم', 'باب آداب دخول الخلاء', 'باب آداب الوضوء', 'آداب الغسل',
    'آداب التيمم', 'آداب الخروج إلى المسجد', 'آداب دخول المسجد', 'آداب ما بعد طلوع الشمس إلى الزوال', 'آداب الاستعداد لسائر الصلوات',
    'آداب النوم', 'آداب الصلاة', 'آداب الإمامة والقدوة', 'آداب الجمعة', 'آداب الصيام', 'القسم الثاني القول في اجتناب المعاصى',
    'آداب العين', 'آداب الأذن', 'آداب اللسان', 'آداب البطن', 'آداب الفرج', 'آداب اليدين', 'آداب الرجلين', 'القول في معاصى القلب',
    'القسم الثالث القول في آداب الصحبة', 'آداب العالم', 'آداب المتعلم', 'آداب الولد مع الوالدين', 'آداب العلاقة بالعوام المجهولين',
    'آداب العلاقة بالاخوان والأصدقاء', 'آداب العلاقة بالمعارف']

def paras(text, size=700):
    """Splits a long run of text into readable paragraphs at sentence ends."""
    out, buf = [], ''
    for sent in re.split(r'(?<=[.!؟])\s+', text):
        buf = (buf + ' ' + sent).strip()
        if len(buf) >= size: out.append(buf); buf = ''
    if buf: out.append(buf)
    return out

def beginning():
    s = body(os.path.join(HERE, 'beginning-ar.html'))
    text = ' '.join(p for p in (clean(x) for x in re.findall(r'<p[^>]*>(.*?)</p>', s, flags=re.S)) if p)
    cuts, pos = [], 0
    for h in HEADINGS:
        i = text.find(h, pos); assert i >= 0, h
        cuts.append((i, h)); pos = i + len(h)
    sections = [{'t': 'المقدمة', 'l': 1, 'p': paras(text[:cuts[0][0]])}]
    for n, (i, h) in enumerate(cuts):
        j = cuts[n + 1][0] if n + 1 < len(cuts) else len(text)
        lvl = 1 if h.startswith('القسم') else 2
        sections.append({'t': h, 'l': lvl, 'p': paras(text[i:j])})
    return dict(id='bidaya', lang='ar', title='بداية الهداية', rtl=True, author='الإمام أبو حامد الغزالي',
                sections=sections, attribution=ATTR.format(rev='305860'))

def nawawi():
    s = body(os.path.join(HERE, 'nawawi-ar.html'))
    tokens = re.findall(r'<(h[34])[^>]*>(.*?)</\1>|<p[^>]*>(.*?)</p>', s, flags=re.S)
    sections, cur = [], {'t': 'المقدمة', 'l': 1, 'p': []}
    for tag, h, p in tokens:
        if tag:
            if cur['p']: sections.append(cur)
            title = clean(h)
            if tag == 'h4': title = title + ' (من صفحة ويكي مصدر، ليس من كلام النووي)'
            cur = {'t': title, 'l': 1 if tag == 'h3' else 2, 'p': []}
        else:
            t = clean(p)
            if t: cur['p'].append(t)
    if cur['p']: sections.append(cur)
    return dict(id='nawawi40', lang='ar', title='الأربعون النووية', rtl=True, author='الإمام يحيى بن شرف النووي',
                sections=sections, attribution=ATTR.format(rev='500107'))

def write(book):
    with gzip.open(os.path.join(OUT, f"{book['id']}.ar.jgz"), 'wt', encoding='utf-8') as f:
        json.dump(book, f, ensure_ascii=False, separators=(',', ':'))
    return book

if __name__ == '__main__':
    books = [write(beginning()), write(nawawi())]
    idx_path = os.path.join(OUT, 'index.jgz')
    idx = json.load(gzip.open(idx_path, 'rt', encoding='utf-8'))
    idx = [b for b in idx if b['id'] not in ('bidaya', 'nawawi40')]
    idx.append(dict(id='bidaya', title="Bidayat al-Hidayah (The Beginning of Guidance)", author="Imam Abu Hamid al-Ghazali",
                    editions=[dict(lang='ar', file='bidaya.ar.jgz', sections=len(books[0]['sections']),
                                   completeness='Complete Arabic text as transcribed on Wikisource (rev. 305860); not a critical edition. CC BY-SA 4.0.')]))
    idx.append(dict(id='nawawi40', title="al-Arba'in an-Nawawiyyah (Forty Hadith)", author="Imam Yahya ibn Sharaf an-Nawawi",
                    editions=[dict(lang='ar', file='nawawi40.ar.jgz', sections=len(books[1]['sections']),
                                   completeness='All 42 hadith in Arabic (Wikisource rev. 500107) with the page\'s short commentary, marked as not an-Nawawi\'s own words. CC BY-SA 4.0.')]))
    with gzip.open(idx_path, 'wt', encoding='utf-8') as f:
        json.dump(idx, f, ensure_ascii=False)
    for b in books: print(b['id'], len(b['sections']), [s['t'] for s in b['sections']][:14])
