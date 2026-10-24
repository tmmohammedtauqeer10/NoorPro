from qlib import find, NAMES, AY
def A(ar, label=''):                      # non-Quran Arabic (letters/syllables): no audio
    return {'ar': ar, 'label': label}
def Q(s, a, skel, label='', rule=None):   # Quran word(s), verified against bundled quran_core
    d = {'ar': find(s, a, skel), 'label': label, 'ref': f'{NAMES[s-1]} {s}:{a}', 's': s, 'a': a}
    if rule: d['rule'] = rule
    return d
def QA(s, a, label=''):                   # whole ayah
    t = AY[(s, a)]
    B = 'بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ '
    from qlib import norm
    w = t.split()
    if a == 1 and s != 1 and len(w) > 4 and norm(' '.join(w[:4])) == norm(B): t = ' '.join(w[4:])
    return {'ar': t, 'label': label, 'ref': f'{NAMES[s-1]} {s}:{a}', 's': s, 'a': a}
def M(q, opts, ans, ar=None):
    d = {'q': q, 'opts': opts, 'a': ans}
    if ar: d['ar'] = ar
    return d
def L(id, title, goal, en, ur, points=(), ex=(), quiz=(), kind='lesson', **kw):
    d = {'id': id, 'title': title, 'goal': goal, 'en': en, 'ur': ur, 'points': list(points),
         'examples': list(ex), 'quiz': list(quiz), 'kind': kind}
    d.update(kw)
    return d
