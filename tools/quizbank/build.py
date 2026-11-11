"""Builds app/src/main/assets/quiz/bank_v1.json from the curated bank_*.py sources. Run: python3 tools/quizbank/build.py"""
import json, hashlib, random, os
from collections import Counter
from common import ITEMS
import bank_a, bank_b, bank_c, bank_d, bank_e  # noqa: F401 (registers questions)

out, seen = [], set()
for it in ITEMS:
    key = (it['cat'], it['q']); assert key not in seen, key; seen.add(key)
    qid = it['cat'][:3] + '_' + hashlib.sha1(it['q'].encode()).hexdigest()[:8]
    if 'options' in it:
        opts, ans = it['options'], it['answers']
        assert set(ans) == {'hanafi', 'shafii', 'maliki', 'hanbali'} and all(0 <= v < len(opts) for v in ans.values())
        out.append(dict(id=qid, c=it['cat'], l=it['lvl'], q={'en': it['q']}, o={'en': opts}, a=ans['hanafi'], e={'en': it['expl']['hanafi']}, s=it['src'],
                        m={k: {'a': ans[k], 'e': {'en': it['expl'][k]}} for k in ['shafii', 'maliki', 'hanbali']}))
    else:
        assert len(it['wrong']) == 3 and it['correct'] not in it['wrong'] and len(set(it['wrong'])) == 3, it['q']
        opts = [it['correct']] + list(it['wrong']); random.Random(qid).shuffle(opts)
        out.append(dict(id=qid, c=it['cat'], l=it['lvl'], q={'en': it['q']}, o={'en': opts}, a=opts.index(it['correct']), e={'en': it['e']}, s=it['src']))
import deenflow_merge
curated = len(out)
out += deenflow_merge.merged()
assert len({o['id'] for o in out}) == len(out)
print(curated, 'curated +', len(out) - curated, 'Deenflow =', len(out), Counter(o['c'] for o in out))
dst = os.path.join(os.path.dirname(__file__), '..', '..', 'app', 'src', 'main', 'assets', 'quiz', 'bank_v1.json')
json.dump({'version': 1, 'questions': out}, open(dst, 'w'), ensure_ascii=False, separators=(',', ':'))
