"""Offline verification corpus: the app's bundled Quran (assets/quran/quran_core.jgz) and hadith Arabic (assets/hadith/ara-*)."""
import gzip, json, os, re
A = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'app', 'src', 'main', 'assets')
_H, _Q = {}, None
DIAC = re.compile(r'[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED\u0640\u200f\u200e]')
def norm(s):
    s = DIAC.sub('', s)
    s = re.sub('[إأآٱا]', 'ا', s); s = s.replace('ى', 'ي').replace('ة', 'ه').replace('ؤ', 'و').replace('ئ', 'ي').replace('ء', '')
    s = re.sub(r'[^\u0621-\u064A ]', ' ', s)
    return re.sub(r'\s+', ' ', s).strip()
def book(name):
    if name not in _H:
        d = os.path.join(A, 'hadith', 'ara-' + name); out = {}
        for f in os.listdir(d):
            for h in json.load(gzip.open(os.path.join(d, f), 'rt', encoding='utf-8'))['hadiths']:
                out.setdefault(h['hadithnumber'], h['text'])
        _H[name] = out
    return _H[name]
def quran():
    global _Q
    if _Q is None:
        d = json.load(gzip.open(os.path.join(A, 'quran', 'quran_core.jgz'), 'rt', encoding='utf-8'))
        counts = [286 if False else None]
        _Q = d
    return _Q
SURAH_AYAT = [7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,98,135,112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,75,85,54,53,89,59,37,35,38,29,18,45,60,49,62,55,78,96,29,22,24,13,14,11,11,18,12,12,30,52,52,44,28,28,20,56,40,31,50,40,46,42,29,19,36,25,22,17,19,26,30,20,15,21,11,8,8,19,5,8,8,11,11,8,3,9,5,4,7,3,6,3,5,4,5,6]
def ayah(s, a):
    idx = sum(SURAH_AYAT[:s - 1]) + a - 1
    return quran()['a'][idx]
def find(name, text, limit=5):
    """Hadith numbers in collection `name` whose Arabic contains the normalised `text`."""
    t = norm(text)
    return [n for n, h in book(name).items() if t in norm(h)][:limit]
def contains(name, num, text):
    h = book(name).get(num)
    return h is not None and norm(text) in norm(h)
