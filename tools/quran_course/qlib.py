import gzip, json, re
D = json.load(gzip.open(''+__import__('os').path.join(__import__('os').path.dirname(__file__),'../../app/src/main/assets')+'/quran/quran_core.jgz'))
AY = {(r[0], r[1]): r[4].lstrip('\ufeff') for r in D['a']}
NAMES = [s[0] for s in D['s']]
DIAC = re.compile('[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED\u0640]')
def norm(s):
    s = DIAC.sub('', s)
    for a, b in (('ء',''),('ٱ','ا'),('أ','ا'),('إ','ا'),('آ','ا'),('ى','ي'),('ئ','ي'),('ؤ','و'),('ة','ه')):
        s = s.replace(a, b)
    return s.replace(' ', '')
def find(s, a, skel):
    """Return the exact Uthmani word(s) of ayah s:a whose skeleton equals skel (space = several words)."""
    words = AY[(s, a)].split()
    n = len(skel.split())
    target = norm(skel)
    for i in range(len(words) - n + 1):
        cand = ' '.join(words[i:i+n])
        if norm(cand) == target:
            return cand
    for i in range(len(words) - n + 1):
        cand = ' '.join(words[i:i+n])
        if target in norm(cand):
            return cand
    raise ValueError(f'{s}:{a} no match for {skel!r}: {AY[(s,a)]}')
