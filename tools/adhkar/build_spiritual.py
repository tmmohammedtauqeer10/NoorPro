"""1.2.0 (116): rebuild content/ruhani.jgz and content/darood.jgz with NoorPro's own translations (+ Hindi) and
generate content/adhkar.jgz (morning / evening / after-salah / daily masnoon) from adhkar_data.py.
Every hadith-based Arabic text is checked against the bundled hadith corpus; Quranic text is taken from quran_core.jgz.
Run: python3 tools/adhkar/build_spiritual.py   (idempotent: originals are read from tools/adhkar/orig/)"""
import gzip, json, os, re, shutil, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from corpus import A, ayah, contains, norm
import tr_ruhani, tr_darood, tr_extra, adhkar_data

HERE = os.path.dirname(os.path.abspath(__file__))
CONTENT = os.path.join(A, 'content')
ORIG = os.path.join(HERE, 'orig')
os.makedirs(ORIG, exist_ok=True)
errors = []

def load_orig(name):
    o = os.path.join(ORIG, name + '.json')
    if not os.path.exists(o):
        d = json.load(gzip.open(os.path.join(CONTENT, name + '.jgz'), 'rt', encoding='utf-8'))
        json.dump(d, open(o, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
    return json.load(open(o, encoding='utf-8'))

def save(name, d):
    with gzip.open(os.path.join(CONTENT, name + '.jgz'), 'wt', encoding='utf-8', compresslevel=9) as f:
        json.dump(d, f, ensure_ascii=False, separators=(',', ':'))

def fix_source(s):
    s = s.replace("English: Ahmed Raza Khan translation", "English and Hindi: NoorPro")
    s = re.sub(r"Arabic and English: (sunnah\.com/\S+?)(\.?)$", r"Arabic: \1; translation: NoorPro", s)
    s = re.sub(r"Arabic and English: (sunnah\.com/\S+)", r"Arabic: \1; translation: NoorPro", s)
    s = re.sub(r"Text and translation: (\S+)", r"Arabic text: \1; translation: NoorPro", s)
    s = re.sub(r"Text, transliteration, translation(?: and virtues)?: (\S+)", r"Arabic text and transliteration: \1; translation: NoorPro", s)
    s = re.sub(r"Text and virtues: (\S+)", r"Arabic text and reported virtues: \1; translation: NoorPro", s)
    s = re.sub(r"English: Shaykh Zakariyya Kandhlawi's translat[^.]*\.?", "Chapter summaries: NoorPro.", s)
    s = s.replace("English rendering by NoorPro from the Arabic wording.", "Translation: NoorPro.")
    if 'NoorPro' not in s:
        s = s.rstrip('. ') + '. Translation: NoorPro.'
    return s

def apply(d, table, section):
    for c in d['categories']:
        for itm in c['items']:
            k = c['id'] + '/' + itm['id']
            t = table.get(k)
            if t:
                itm['english'] = t['en']; itm['hindi'] = t['hi']
                if 'ur' in t: itm['urdu'] = t['ur']
                if 'tr' in t: itm['translit'] = t['tr']
            itm['source'] = fix_source(itm.get('source', ''))

# ---------- Ruhani Ilaj ----------
ru = load_orig('ruhani')
apply(ru, tr_ruhani.T, 'ruhani')
for c in ru['categories']:
    for itm in c['items']:
        if c['id'] == 'burda':
            b = tr_extra.BURDA[itm['id']]
            itm['subtitle'] = b['sub']
            itm['english'] = 'Chapter summary: ' + b['en']; itm['urdu'] = 'باب کا خلاصہ: ' + b['ur']; itm['hindi'] = 'बाब का ख़ुलासा: ' + b['hi']
            itm['when'] = itm.get('when') or "Recited in gatherings of salawat and dhikr; many read one chapter a day, or the whole qasida on Thursday night or Friday."
        elif itm['id'] == 'hizbbahr':
            h = tr_extra.HIZB
            itm['english'], itm['urdu'], itm['hindi'] = h['en'], h['ur'], h['hi']
            itm['source'] = "Hizb al-Bahr by Imam Abu al-Hasan ash-Shadhili (d. 656 AH). Arabic text: shazellia.com (Awrad of the Shadhili Tariqa). Summary: NoorPro (no full translation bundled)."
        elif itm['id'] == 'asma_note':
            itm['urdu'], itm['hindi'] = tr_extra.ASMA_NOTE['ur'], tr_extra.ASMA_NOTE['hi']
            itm['source'] = "Sahih al-Bukhari 2736; Quran 7:180."
ru['disclaimer'] = ("Ruqyah and duas are a spiritual means, not a substitute for medical treatment. If you or someone you love is ill, "
    "please see a qualified doctor and take the treatment prescribed; recite ruqyah alongside it. For religious questions consult a qualified "
    "Sunni scholar. NoorPro does not sell or endorse amulets (ta'wiz), paid 'healing' services or promises of guaranteed cures, and nothing here "
    "involves harming anyone.")
ru['intro'] = ("Quranic ayat and masnoon supplications for protection, healing and needs, as taught in the Sunnah and practised by Ahl as-Sunnah "
    "wa al-Jama'ah. Each item gives the reference; counts and timings are shown only where the cited source states them. "
    "English, Urdu and Hindi meanings are NoorPro's own; Quranic Urdu is Kanz-ul-Iman (Imam Ahmad Raza Khan).")
save('ruhani', ru)

# ---------- Darood ----------
da = load_orig('darood')
apply(da, tr_darood.T, 'darood')
FRIDAY = {'id': 'friday', 'title': 'Friday (Jumu\'ah) Darood', 'items': [
 dict(id='jumuah', title='Send abundant salawat on Friday',
      arabic="إِنَّ مِنْ أَفْضَلِ أَيَّامِكُمْ يَوْمَ الْجُمُعَةِ … فَأَكْثِرُوا عَلَيَّ مِنَ الصَّلَاةِ فِيهِ، فَإِنَّ صَلَاتَكُمْ مَعْرُوضَةٌ عَلَيَّ",
      translit="Inna min afḍali ayyāmikum yawma l-jumuʿah… fa-akthirū ʿalayya mina ṣ-ṣalāti fīh, fa-inna ṣalātakum maʿrūḍatun ʿalayy.",
      english="'Among the best of your days is Friday… so send abundant blessings upon me on it, for your blessings are presented to me.'",
      urdu="'تمہارے دنوں میں سب سے افضل جمعہ کا دن ہے… لہٰذا اس دن مجھ پر کثرت سے درود بھیجو، کیونکہ تمہارا درود مجھ پر پیش کیا جاتا ہے۔'",
      hindi="'तुम्हारे दिनों में सबसे अफ़ज़ल जुमे का दिन है… इसलिए उस दिन मुझ पर ज़्यादा से ज़्यादा दुरूद भेजो, क्योंकि तुम्हारा दुरूद मुझ पर पेश किया जाता है।'",
      when="Throughout Friday and the night before it (Thursday after Maghrib).", virtue="", count=0,
      source="Sunan Abi Dawud 1047; Sunan an-Nasa'i 1374; Sunan Ibn Majah 1085 (sahih). Translation: NoorPro.",
      V=[('abudawud', 1047, 'فاكثروا علي من الصلاه فيه فان صلاتكم معروضه علي')]),
 dict(id='friday_ibrahimi', title='Durood Ibrahimi on Friday',
      arabic="اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ، كَمَا صَلَّيْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ، إِنَّكَ حَمِيدٌ مَجِيدٌ، اللَّهُمَّ بَارِكْ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ، كَمَا بَارَكْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ، إِنَّكَ حَمِيدٌ مَجِيدٌ",
      translit="Allāhumma ṣalli ʿalā Muḥammadin wa ʿalā āli Muḥammad, kamā ṣallayta ʿalā Ibrāhīma wa ʿalā āli Ibrāhīm, innaka ḥamīdun majīd. Allāhumma bārik ʿalā Muḥammadin wa ʿalā āli Muḥammad, kamā bārakta ʿalā Ibrāhīma wa ʿalā āli Ibrāhīm, innaka ḥamīdun majīd.",
      english=tr_darood.T['sunnah/ibrahimi']['en'], urdu=tr_darood.T['sunnah/ibrahimi']['ur'], hindi=tr_darood.T['sunnah/ibrahimi']['hi'],
      when="The salawat taught by the Prophet ﷺ himself, so the best form to repeat on Friday; set your own daily count (e.g. 100) and keep to it.", virtue="", count=100,
      source="Sahih al-Bukhari 3370. Count is a personal wird, not fixed by the hadith. Translation: NoorPro.",
      V=[('bukhari', 3370, 'اللهم صل علي محمد وعلي ال محمد كما صليت علي ابراهيم')]),
 dict(id='friday_80', title='Salawat after \'Asr on Friday (80 times)',
      arabic="اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ النَّبِيِّ الْأُمِّيِّ وَعَلَى آلِهِ وَسَلِّمْ تَسْلِيمًا",
      translit="Allāhumma ṣalli ʿalā Muḥammadini n-nabiyyi l-ummiyyi wa ʿalā ālihī wa sallim taslīmā.",
      english="O Allah, send blessings upon Muhammad, the Unlettered Prophet, and upon his family, and grant them abundant peace.",
      urdu="اے اللہ! نبیِ امی محمد ﷺ اور ان کی آل پر رحمت نازل فرما اور خوب سلامتی بھیج۔",
      hindi="ऐ अल्लाह! नबी-ए-उम्मी मुहम्मद ﷺ और उनकी आल पर रहमत नाज़िल कर और ख़ूब सलामती भेज।",
      when="Friday after 'Asr, before getting up from one's place, 80 times.", count=80,
      virtue="Reported that eighty years of sins are forgiven. The chain is weak; scholars such as al-Sakhawi (al-Qawl al-Badi') and al-Haytami cite it for virtuous deeds (fada'il), which is the accepted Sunni practice for such reports.",
      source="Reported by ad-Daraqutni and al-Khatib al-Baghdadi; cited in al-Sakhawi, al-Qawl al-Badi'. Translation: NoorPro."),
 dict(id='ten_ranks', title='Ten blessings, ten sins removed, ten ranks',
      arabic="مَنْ صَلَّى عَلَيَّ صَلَاةً وَاحِدَةً صَلَّى اللَّهُ عَلَيْهِ عَشْرَ صَلَوَاتٍ، وَحُطَّتْ عَنْهُ عَشْرُ خَطِيئَاتٍ، وَرُفِعَتْ لَهُ عَشْرُ دَرَجَاتٍ",
      translit="Man ṣallā ʿalayya ṣalātan wāḥidatan ṣalla Llāhu ʿalayhi ʿashra ṣalawāt, wa ḥuṭṭat ʿanhu ʿashru khaṭī'āt, wa rufiʿat lahū ʿashru darajāt.",
      english="'Whoever sends one blessing upon me, Allah sends ten blessings upon him, ten sins are removed from him and he is raised ten ranks.'",
      urdu="'جو مجھ پر ایک بار درود بھیجے، اللہ اس پر دس رحمتیں نازل فرماتا ہے، اس کے دس گناہ مٹا دیے جاتے ہیں اور اس کے دس درجے بلند کیے جاتے ہیں۔'",
      hindi="'जो मुझ पर एक बार दुरूद भेजे, अल्लाह उस पर दस रहमतें नाज़िल करता है, उसके दस गुनाह मिटा दिए जाते हैं और उसके दस दर्जे बुलंद किए जाते हैं।'",
      when="", virtue="", count=0, source="Sunan an-Nasa'i 1297 (sahih). Translation: NoorPro.",
      V=[('nasai', 1297, 'صلي الله عليه عشر صلوات')]),
 dict(id='salam_returned', title='The Prophet ﷺ returns the greeting',
      arabic="مَا مِنْ أَحَدٍ يُسَلِّمُ عَلَيَّ إِلَّا رَدَّ اللَّهُ عَلَيَّ رُوحِي حَتَّى أَرُدَّ عَلَيْهِ السَّلَامَ",
      translit="Mā min aḥadin yusallimu ʿalayya illā radda Llāhu ʿalayya rūḥī ḥattā arudda ʿalayhi s-salām.",
      english="'No one sends greetings of peace upon me but Allah returns my soul to me so that I return the greeting to him.'",
      urdu="'جو کوئی مجھ پر سلام بھیجتا ہے، اللہ میری روح مجھ پر لوٹا دیتا ہے یہاں تک کہ میں اس کے سلام کا جواب دیتا ہوں۔'",
      hindi="'जो कोई मुझ पर सलाम भेजता है, अल्लाह मेरी रूह मुझ पर लौटा देता है यहाँ तक कि मैं उसके सलाम का जवाब देता हूँ।'",
      when="", virtue="", count=0, source="Sunan Abi Dawud 2041 (hasan). Translation: NoorPro.",
      V=[('abudawud', 2041, 'ما من احد يسلم علي الا رد الله علي روحي حتي ارد عليه السلام')]),
]}
da['categories'] = [c for c in da['categories'] if c['id'] != 'friday']
da['categories'].insert(1, FRIDAY)
da['intro'] = ("Salawat upon the Prophet ﷺ: from the Quran and Sunnah, Friday salawat, and the classical duroods of Ahl as-Sunnah (Dala'il al-Khayrat and others). "
    "Each item cites its source. Virtues of the classical duroods are reports from scholars and awliya, not hadith, and are shown only where the source states them. "
    "English, Urdu and Hindi meanings are NoorPro's own.")
for c in FRIDAY['items']:
    for b, n, ph in c.pop('V', []):
        if not contains(b, n, ph): errors.append(f'darood/{c["id"]}: {b} {n} missing "{ph}"')
save('darood', da)

# ---------- Adhkar ----------
QS = [7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,98,135,112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,75,85,54,53,89,59,37,35,38,29,18,45,60,49,62,55,78,96,29,22,24,13,14,11,11,18,12,12,30,52,52,44,28,28,20,56,40,31,50,40,46,42,29,19,36,25,22,17,19,26,30,20,15,21,11,8,8,19,5,8,8,11,11,8,3,9,5,4,7,3,6,3,5,4,5,6]
SURAH = {112: 'Al-Ikhlas', 113: 'Al-Falaq', 114: 'An-Nas'}
def quran_block(Q):
    if isinstance(Q[0], int):
        a = ayah(*Q); return a[4], a[5]
    ar, ur = [], []
    for s, f, t in Q:
        rows = [ayah(s, i) for i in range(f, t + 1)]
        ar.append(' '.join(r[4] for r in rows)); ur.append(f'({SURAH.get(s, s)}) ' + ' '.join(r[5] for r in rows))
    return '\n۞\n'.join(ar), '\n'.join(ur)

cats = []
total = 0
for cid, title, fn in adhkar_data.CATS:
    items = []
    for x in fn():
        x = dict(x)
        Q, V = x.pop('Q'), x.pop('V')
        if Q is not None:
            ar, ur = quran_block(Q)
            x['arabic'] = ar
            if not x.get('urdu'): x['urdu'] = ur
            x['source'] = x['source'] + ' Quranic Urdu: Kanz-ul-Iman.'
        for b, n, ph in V:
            if b == 'quran':
                if norm(ph) not in norm(ayah(*n)[4]): errors.append(f'adhkar/{x["id"]}: Quran {n} missing "{ph}"')
            elif not contains(b, n, ph):
                errors.append(f'adhkar/{x["id"]}: {b} {n} missing "{ph}"')
        x['when'] = x.get('when') or ''
        x['source'] = x['source'] + ' Translation: NoorPro.'
        for f in ('arabic', 'translit', 'english', 'urdu', 'hindi', 'source'):
            if not x.get(f): errors.append(f'adhkar/{x["id"]}: empty {f}')
        x['verified'] = bool(V) or Q is not None
        items.append(x)
    total += len(items)
    cats.append(dict(id=cid, title=title, items=items))
sec = dict(adhkar_data.SECTION); sec['categories'] = cats
save('adhkar', sec)

# ---------- completeness ----------
for name, d in (('ruhani', ru), ('darood', da)):
    for c in d['categories']:
        for itm in c['items']:
            for f in ('english', 'urdu', 'hindi', 'source'):
                if not itm.get(f): errors.append(f'{name}/{c["id"]}/{itm["id"]}: empty {f}')
            if any(x in itm.get('source', '') for x in ('Ahmed Raza Khan translation', 'Kandhlawi', 'Arabic and English')):
                errors.append(f'{name}/{itm["id"]}: third-party translation still credited')
print('ruhani', sum(len(c['items']) for c in ru['categories']), 'darood', sum(len(c['items']) for c in da['categories']), 'adhkar', total,
      {c['id']: len(c['items']) for c in cats})
if errors:
    print('\n'.join(errors)); sys.exit(1)
print('OK')
