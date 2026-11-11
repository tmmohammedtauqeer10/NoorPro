ITEMS=[]
def Q(cat,lvl,q,correct,wrong,e,src):
    ITEMS.append(dict(cat=cat,lvl=lvl,q=q,correct=correct,wrong=wrong,e=e,src=src))
def V(cat,lvl,q,options,answers,expl,src):
    ITEMS.append(dict(cat=cat,lvl=lvl,q=q,options=options,answers=answers,expl=expl,src=src))
