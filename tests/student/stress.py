"""Own tests: random cross-checks against brute force + full-domain scale cases.
Run from repo root:  python tests/student/stress.py   (uses submission.json language)"""
import itertools, json, pathlib, random, subprocess, sys, time
ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import run as launcher                       # reuse build()/execute()
from public_validate import subsequence

R = random.Random(2026)                      # fixed seed -> reproducible
def rs(n, alpha='ab'): return ''.join(R.choice(alpha) for _ in range(n))

def lcs_len(a, b):
    d = [[0]*(len(b)+1) for _ in range(len(a)+1)]
    for i in range(len(a)):
        for j in range(len(b)):
            d[i+1][j+1] = d[i][j]+1 if a[i]==b[j] else max(d[i][j+1], d[i+1][j])
    return d[-1][-1]
def merge_cost(s):                           # independent memoised interval DP
    from functools import lru_cache
    @lru_cache(None)
    def f(l, r):
        return 0 if l==r else min(f(l,k)+f(k+1,r) for k in range(l,r))+sum(s[l:r+1])
    return f(0, len(s)-1) if len(s)>1 else 0
def tree_best(n, w, edges):                  # subset brute force
    best = 0
    for mask in range(1<<n):
        if any(mask>>u&1 and mask>>v&1 for u,v in edges): continue
        best = max(best, sum(w[i] for i in range(n) if mask>>i&1))
    return best
def occ(t, p): return [i for i in range(len(t)-len(p)+1) if t.startswith(p, i)]
def cnt(t, p): return len(occ(t, p))

reqs, check = [], []
def add(req, fn): reqs.append(req); check.append(fn)

for _ in range(150):                         # small random cases
    a, b = rs(R.randint(0,9),'abc'), rs(R.randint(0,9),'abc')
    add({'task':'lcs','a':a,'b':b}, lambda y,a=a,b=b: y['length']==lcs_len(a,b)==len(y['sequence']) and subsequence(y['sequence'],a) and subsequence(y['sequence'],b))
for _ in range(100):
    s = [R.randint(0,20) for _ in range(R.randint(0,8))]
    def chk(y, s=s):
        blocks=[(i,i) for i in range(len(s))]; c=0
        for l,k,r in y['plan']:
            j=blocks.index((l,k)); assert blocks[j+1]==(k+1,r); blocks[j:j+2]=[(l,r)]; c+=sum(s[l:r+1])
        return c==y['cost']==merge_cost(tuple(s)) and len(y['plan'])==max(0,len(s)-1)
    add({'task':'merge','sizes':s}, chk)
for _ in range(150):
    n = R.randint(0,10); w=[R.randint(-5,9) for _ in range(n)]
    edges=[[R.randint(0,i-1),i] if R.random()<.5 else [i,R.randint(0,i-1)] for i in range(1,n)]
    def chk(y, n=n, w=w, e=edges):
        sel=set(y['selected'])
        return len(sel)==len(y['selected']) and not any(u in sel and v in sel for u,v in e) and sum(w[v] for v in sel)==y['weight']==tree_best(n,w,e)
    add({'task':'tree','n':n,'weights':w,'edges':edges}, chk)
for task in ('naive','kmp','rk'):
    for _ in range(150):
        t,p = rs(R.randint(0,14)), rs(R.randint(0,4))
        add({'task':task,'text':t,'pattern':p}, lambda y,t=t,p=p: y['positions']==occ(t,p))
for _ in range(100):
    t = rs(R.randint(0,12),'abc')
    def chk(y, t=t):
        sa = sorted(range(len(t)), key=lambda i: t[i:])
        lcp = ([0] if t else [])+[next((k for k in range(min(len(t)-sa[i-1],len(t)-sa[i])) if t[sa[i-1]+k]!=t[sa[i]+k]), min(len(t)-sa[i-1],len(t)-sa[i])) for i in range(1,len(t))]
        best = max([0]+lcp)
        return y['sa']==sa and y['lcp']==lcp and len(y['repeated'])==best and (best==0 or cnt(t,y['repeated'])>=2)
    add({'task':'suffix','text':t}, chk)
for _ in range(60):
    pats=[rs(R.randint(1,3)) for _ in range(R.randint(0,5))]; texts=[rs(R.randint(0,10)) for _ in range(R.randint(0,3))]
    add({'task':'aho','patterns':pats,'texts':texts}, lambda y,P=pats,T=texts: y['counts']==[[cnt(t,p) for p in P] for t in T])

# fixed edge cases (name, request, check)
add({'task':'rk','text':'abdj','pattern':'ab'}, lambda y: y['positions']==[0])           # collision pair (75/75)
add({'task':'rk','text':'dj','pattern':'ab'}, lambda y: y['positions']==[])             # equal hash, no match
add({'task':'tree','n':4,'weights':[-1,-2,-3,-4],'edges':[[0,1],[1,2],[2,3]]}, lambda y: y['weight']==0 and y['selected']==[])
add({'task':'aho','patterns':['a','a','aa'],'texts':['aaaa','']}, lambda y: y['counts']==[[4,4,3],[0,0,0]])
add({'task':'aho','patterns':[],'texts':['x','']}, lambda y: y['counts']==[[],[]])
add({'task':'merge','sizes':[10**9]*200}, lambda y: y['cost']>0 and len(y['plan'])==199)

# scale cases (full published domains)
n=1000; a,b=rs(n,'ab'),rs(1000,'ab')
add({'task':'lcs','a':a,'b':b}, lambda y,a=a,b=b: y['length']==lcs_len(a,b))
nn=100000; w=[R.randint(-10**9,10**9) for _ in range(nn)]; chain=[[i,i+1] for i in range(nn-1)]
def chk(y):                                  # deep chain: linear DP check
    tk,sk=0,0
    for x in w: tk,sk=sk+x,max(tk,sk)
    sel=set(y['selected']); return y['weight']==max(tk,sk)==sum(w[v] for v in sel) and not any(i in sel and i+1 in sel for i in range(nn-1))
add({'task':'tree','n':nn,'weights':w,'edges':chain}, chk)
T='a'*200000
add({'task':'kmp','text':T,'pattern':'a'*100000}, lambda y: len(y['positions'])==100001)
T2='a'*3000
add({'task':'naive','text':T2,'pattern':'a'*1500}, lambda y: len(y['positions'])==1501)
add({'task':'rk','text':'a'*4000,'pattern':'a'*4000}, lambda y: y['positions']==[0])
add({'task':'suffix','text':'a'*50000}, lambda y: y['sa']==list(range(49999,-1,-1)) and y['lcp']==[0]+list(range(1,50000)) and len(y['repeated'])==49999)
add({'task':'aho','patterns':['a'*i for i in range(1,446)],'texts':['a'*2000]*100}, lambda y: len(y['counts'])==100 and y['counts'][0][0]==2000 and y['counts'][0][444]==2000-444)
add({'task':'aho','patterns':['a']*10000,'texts':['a'*2000]*100}, lambda y: all(v==2000 for row in y['counts'] for v in row))

cmd = launcher.build(launcher.select_language(None))
bad = 0
for i in range(0, len(reqs), 1):
    pass
t0=time.time()
answers = launcher.execute(cmd, reqs) if len(json.dumps(reqs))<8_000_000 else None
for r, y, f in zip(reqs, answers, check):
    try: ok = f(y)
    except Exception as e: ok = False
    if not ok: bad += 1; print('FAIL', r['task'], str(r)[:120])
print(f'{len(reqs)-bad}/{len(reqs)} own tests passed in {time.time()-t0:.1f}s')
sys.exit(1 if bad else 0)
