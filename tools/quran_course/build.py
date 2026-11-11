import json, gzip, sys
from l1a import L1; from l1b import L1b; from l1c import L1c; from l2 import L2; from l3 import L3; from l45 import L4, L5
from qlib import AY
RULES = {'ghunna','ikhfa','idgham','iqlab','izhar','qalqala','madd','tafkheem','tarqeeq','waqf','hamzawasl','special'}
course = {'v': 1, 'title': 'Quran Reading Course', 'sources': ['Noorani Qaida (Noor Muhammad Haqqani)', 'Tuhfat al-Atfal (Sulayman al-Jamzuri)', 'al-Muqaddimah al-Jazariyyah (Ibn al-Jazari)', 'Hidayat al-Mustafid fi Ahkam at-Tajwid (Muhammad al-Mahmud)', 'Tajweed Rules of the Qur\'an (Kareema Carol Czerepinski)'],
  'levels': [
   {'id':'L1','title':'Level 1 · Qaida','subtitle':'Letters, vowels and first reading','lessons': L1+L1b+L1c},
   {'id':'L2','title':'Level 2 · Tajweed basics','subtitle':'Makharij, noon/meem rules, qalqala, heavy & light','lessons': L2},
   {'id':'L3','title':'Level 3 · Intermediate','subtitle':'Madd types, waqf signs, hamzatul wasl','lessons': L3},
   {'id':'L4','title':'Level 4 · Practice','subtitle':'Juz Amma reading, hifz and self-test','lessons': L4},
   {'id':'L5','title':'Level 5 · Advanced','subtitle':'Rare rules, revision and etiquette','lessons': L5}]}
err = []; ids = set(); nq = 0; nex = 0
for lv in course['levels']:
    for l in lv['lessons']:
        if l['id'] in ids: err.append('dup '+l['id'])
        ids.add(l['id'])
        for k in ('title','goal','en','ur'):
            if not l[k].strip(): err.append(f"{l['id']} empty {k}")
        if not l['quiz']: err.append(l['id']+' no quiz')
        for q in l['quiz']:
            if not (0 <= q['a'] < len(q['opts'])) or any(not o.strip() for o in q['opts']): err.append(l['id']+' bad quiz '+q['q'])
        for e in l['examples']:
            nex += 1
            if not e['ar'].strip(): err.append(l['id']+' empty ar')
            if 's' in e:
                nq += 1
                if e['ar'] not in AY[(e['s'], e['a'])]: err.append(f"{l['id']} mismatch {e['ref']}")
            if 'rule' in e and e['rule'] not in RULES: err.append(l['id']+' rule '+e['rule'])
        if 'v' in sys.argv: print(l['id'], ' | '.join(f"{e['ar']} [{e.get('ref','-')}]" for e in l['examples'] if 's' in e))
print('levels', len(course['levels']), 'lessons', len(ids), 'examples', nex, 'quran', nq, 'quiz', sum(len(l['quiz']) for lv in course['levels'] for l in lv['lessons']), 'errors', err)
if not err:
    out = ''+__import__('os').path.join(__import__('os').path.dirname(__file__),'../../app/src/main/assets')+'/content/quran_course.jgz'
    with gzip.open(out, 'wt', encoding='utf-8') as f: json.dump(course, f, ensure_ascii=False, separators=(',',':'))
    print('wrote', out)
