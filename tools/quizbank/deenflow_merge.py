"""Merges the Deenflow quiz (github.com/tmmohammedtauqeer10/Deenflow, MIT, same author) into bank_v1.json.
Native Urdu/Hindi/Arabic text comes from quiz_data.{ur,hi,ar}.json, so these questions never need AI translation.
Questions that duplicate the curated 308, or are ambiguous/madhhab-specific without full variants, are skipped (see DROP)."""
import json, os, re
D = os.path.join(os.path.dirname(__file__), 'deenflow_src')
LANGS = ['ur', 'hi', 'ar']

# Duplicates of the curated bank (same question and answer) or of another Deenflow question.
DUP = set('''q_see_001 q_see_002 q_see_003 q_see_005 q_see_006 q_see_007 q_see_008 q_see_009 q_see_010 q_see_011 q_see_012 q_see_013
q_see_014 q_see_015 q_see_017 q_see_019 q_qur_001 q_qur_002 q_qur_003 q_qur_004 q_qur_006 q_qur_009 q_qur_010 q_qur_012 q_qur_013
q_qur_014 q_qur_015 q_qur_017 q_had_002 q_had_004 q_had_005 q_had_006 q_had_007 q_had_013 q_had_014 q_had_016 q_fiq_001 q_fiq_003
q_fiq_006 q_fiq_007 q_fiq_011 q_fiq_012 q_fiq_016 q_fiq_017 q_his_001 q_his_003 q_his_004 q_his_006 q_his_009 q_his_011 q_his_012
q_his_013 q_his_014 q_his_016 q_his_018 q_his_019 q_stories_001 q_stories_002 q_stories_003 q_stories_007 q_stories_012 q_stories_016
q_stories_019 q_stories_020 q_worship_001 q_worship_003 q_worship_007 q_worship_008 q_worship_011 q_worship_013 q_worship_014
q_worship_015 q_sunnah_manners_003 q_surahs_014'''.split())
# Not merged for accuracy: fard acts of wudu differ by madhhab (4 Hanafi, 6 Shafi'i/Hanbali, 7 Maliki);
# "Quran compiled into one book" conflates Abu Bakr's compilation with Uthman's standard copies;
# accidental (non-forgetful) swallowing of water also breaks the fast in the Hanafi school, so 'only intentional eating' misleads;
# the first police force / bayt al-mal attribution is disputed among historians.
AMBIGUOUS = {'q_fiq_002', 'q_fiq_004', 'q_his_005', 'q_his_010'}
DROP = DUP | AMBIGUOUS

# References for the questions that Deenflow's quiz_sources.md does not cover.
SRC = {
 'q_see_004': 'Sahih al-Bukhari 3851; Ibn Hisham, al-Sirah al-Nabawiyyah', 'q_see_016': 'Quran 9:40; Sahih al-Bukhari 3653',
 'q_see_018': 'Ibn Hisham, al-Sirah al-Nabawiyyah (Uhud, Shawwal 3 AH)', 'q_see_020': 'Sahih Muslim 1218 (Hadith of Jabir on the Farewell Hajj)',
 'q_qur_005': 'al-Suyuti, al-Itqan fi Ulum al-Quran; Sahih al-Bukhari 4978', 'q_qur_007': "Sunan al-Tirmidhi 2887 (a weak narration, widely cited in virtues)",
 'q_qur_008': 'Quran 2:97; Quran 26:193-194', 'q_qur_011': 'Order of the Mushaf (Quran 1)', 'q_qur_016': 'Quran 2:142-150',
 'q_qur_018': 'Quran 97', 'q_qur_019': 'Quran 12', 'q_qur_020': 'Quran 96:1-5; Sahih al-Bukhari 3',
 'q_had_001': 'Sahih al-Bukhari 1; Sahih Muslim 1907', 'q_had_003': 'Ibn al-Salah, Muqaddimah (Ulum al-Hadith)',
 'q_had_008': 'Sahih Muslim 2276', 'q_had_009': "Sunan al-Nasa'i 3104", 'q_had_010': 'Ibn al-Salah, Muqaddimah (Ulum al-Hadith)',
 'q_had_011': 'Ibn al-Salah, Muqaddimah (Ulum al-Hadith)', 'q_had_012': 'Ibn al-Salah, Muqaddimah (Ulum al-Hadith)',
 'q_had_015': 'Sunan Abi Dawud (compiled by Abu Dawud Sulayman ibn al-Ash\'ath al-Sijistani, d. 275 AH)',
 'q_had_017': 'Ibn al-Salah, Muqaddimah (Ulum al-Hadith)', 'q_had_018': 'Ibn al-Salah, Muqaddimah (Ulum al-Hadith)',
 'q_had_019': 'Ibn al-Salah, Muqaddimah (Ulum al-Hadith)', 'q_had_020': 'al-Dhahabi, Siyar A\'lam al-Nubala; Ibn Hajar, Tahdhib al-Tahdhib',
 'q_fiq_004': 'Quran 2:187; Sahih al-Bukhari 1933', 'q_fiq_005': 'Quran 5:6', 'q_fiq_008': 'Sahih al-Bukhari 1333; al-Hidayah (Kitab al-Jana\'iz)',
 'q_fiq_009': 'Quran 2:197; Sahih Muslim 1218', 'q_fiq_010': 'Usul al-Fiqh (al-Shashi, Usul al-Shashi)',
 'q_fiq_013': 'Sahih al-Bukhari 350', 'q_fiq_014': 'Sahih al-Bukhari 350', 'q_fiq_015': 'Sahih al-Bukhari 350',
 'q_fiq_018': 'Sahih Muslim 612', 'q_fiq_019': 'Quran 5:6; Sahih al-Bukhari 248', 'q_fiq_020': 'Quran 62:9',
 'q_his_002': 'Sahih al-Bukhari 3757', 'q_his_007': 'al-Tabari, Tarikh al-Rusul wal-Muluk (year 16/17 AH)',
 'q_his_008': 'Ibn Khaldun, Tarikh; al-Maqqari, Nafh al-Tib', 'q_his_015': 'al-Tabari, Tarikh al-Rusul wal-Muluk (year 41 AH)',
 'q_his_017': 'al-Baladhuri, Futuh al-Buldan', 'q_his_020': 'al-Maqrizi, al-Suluk (year 658 AH)',
}

CAT = {'quiz_seerah_01': 'seerah', 'quiz_quran_01': 'quran', 'quiz_hadith_01': 'hadith', 'quiz_fiqh_01': 'fiqh', 'quiz_history_01': 'history',
       'quiz_stories_01': 'quran', 'quiz_ethics_01': 'quran', 'quiz_sunnah_manners_01': 'hadith', 'quiz_surahs_01': 'quran'}
LEVEL = {'Easy': 1, 'Medium': 2, 'Hard': 3}
ANS = {'en': 'The correct answer is “{a}”.', 'ur': 'درست جواب: {a}', 'hi': 'सही उत्तर: {a}', 'ar': 'الإجابة الصحيحة: {a}'}

def cat_of(cid, qid):
    if cid == 'quiz_worship_01':  # Worship & Belief Basics: phrases of dhikr go to Duas, the rest to Fiqh
        return 'duas' if qid in {'q_worship_006', 'q_worship_007', 'q_worship_008', 'q_worship_009', 'q_worship_010',
                                 'q_worship_011', 'q_worship_012', 'q_worship_013', 'q_worship_014'} else 'fiqh'
    return CAT[cid]

def load(name):
    out = {}
    for c in json.load(open(os.path.join(D, name), encoding='utf-8'))['categories']:
        for q in c['questions']: out.setdefault(q['question_id'], (c, q))
    return out

def merged():
    en = load('quiz_data.json'); exp = load('islamic_quiz_expanded.json')
    tr = {l: load(f'quiz_data.{l}.json') for l in LANGS}
    src = dict(re.findall(r'\| `(q_[a-z_0-9]+)` \| \[([^\]]+)\]', open(os.path.join(D, 'quiz_sources.md'), encoding='utf-8').read()))
    ids = list(en) + [i for i in exp if i not in en]
    out = []
    for qid in ids:
        if qid in DROP: continue
        c, q = en.get(qid) or exp[qid]
        s = src.get(qid) or SRC.get(qid)
        assert s, 'missing source ' + qid
        a = q['correct_index']; opts = {'en': q['options']}; text = {'en': q['text']}
        for l in LANGS:
            t = tr[l].get(qid)
            if t and t[1]['correct_index'] == a and len(t[1]['options']) == len(q['options']):
                text[l] = t[1]['text']; opts[l] = t[1]['options']
        expl = {l: ANS[l].format(a=opts[l][a]) for l in text}
        out.append(dict(id='df_' + qid[2:], c=cat_of(c['category_id'], qid), l=LEVEL.get(c['difficulty'], 1), q=text, o=opts, a=a, e=expl,
                        s=s + ' · via Deenflow quiz'))
    return out

if __name__ == '__main__':
    m = merged(); from collections import Counter
    print(len(m), Counter(x['c'] for x in m), 'native ur/hi/ar:', sum(1 for x in m if len(x['q']) == 4))
