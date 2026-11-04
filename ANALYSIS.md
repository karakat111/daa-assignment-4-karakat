# Assignment 4 — analysis

**Utembayeva Karakat, group SE-2535** — DAA Assignment 4: Dynamic Programming and String Processing

## Environment and commands

- Language: **Java** (`submission.json`: `"language": "Java"`). Runtime used for the measurements: OpenJDK 21.0.12.1; sources compile with `-source/-target 17` (the launcher's `--release 17` is used by the official checker).
- Machine: Linux 6.18, Intel Xeon @ 2.10 GHz, **1 vCPU**, ~4 GB RAM (a shared virtual machine, so timings are noisy).
- Commands (repository root):
  - `python tools/run.py --language Java --setup`
  - `python tools/run.py --language Java --check` → all 44 public cases + repeated batch pass.
  - `python tests/student/stress.py` → 1024/1024 own tests pass (1.6 s).
  - Benchmark: `javac -d /tmp/b Java/source/*.java tests/student/Bench.java && java -Xmx384m -cp /tmp/b Bench`
- Final commit URL is submitted in Moodle.

## Algorithm explanations

### lcs
- **State:** `dp[i][j]` = LCS length of `a[0..i)` and `b[0..j)`. **Base:** `dp[0][*] = dp[*][0] = 0` (also covers empty inputs).
- **Transition:** equal last chars → `dp[i-1][j-1]+1`; otherwise `max(dp[i-1][j], dp[i][j-1])` (the optimum ends without `a[i-1]` or without `b[j-1]`).
- **Order:** rows ascending, columns ascending; all three predecessors are already filled.
- **Reconstruction:** walk from `(n,m)`: equal chars → take it and go diagonally; else move toward the larger neighbour; reverse the collected chars. Ties go up, any optimal answer is accepted.
- **Cost:** O((n+1)(m+1)) time and memory (flat `int[]`).
- **Boundary test:** `a="tea_time", b="team"` → `team` (subsequence, not substring); empty input → length 0.

### merge (interval DP)
- **State:** `cost[l][r]` = minimum cost to turn blocks `l..r` into one block; `split[l][r]` = last split `k` (merge `[l,k]` and `[k+1,r]` last).
- **Transition:** `cost[l][r] = min_k cost[l][k] + cost[k+1][r] + sum(l..r)` (prefix sums, `long`). **Base:** `cost[i][i] = 0`.
- **Order:** by increasing interval length, so both sub-intervals are known.
- **Plan:** recursive post-order `emit(l,r)`: `emit(l,k)`, `emit(k+1,r)`, then `[l,k,r]` — children merges must happen before the parent merge exists as a block. Depth ≤ 199. `n<=1` → cost 0, empty plan.
- **Cost:** O(n³) time, O(n²) memory; max total ≈ 200·10⁹·199 fits in `long`.
- **Boundary test:** `[2,3,4]` → 14 (the other order gives 16), detects wrong greedy/non-adjacent merging.

### tree (maximum-weight independent set)
- **State:** `take[v]` = best weight of v's subtree if v is chosen; `skip[v]` = best if v is not chosen. Initial `take[v]=w[v]`, `skip[v]=0`.
- **Update (child c into parent p):** `take[p] += skip[c]`; `skip[p] += max(take[c], skip[c])`.
- **Order:** BFS from vertex 0 gives an order where parents precede children; processing it in reverse = post-order **without recursion** (100 000-vertex chain is safe).
- **Reconstruction:** top-down in BFS order: if the parent is chosen → skip; else choose `v` iff `take[v] > skip[v]`. The empty set is allowed, so all-negative weights → 0 and `[]`.
- **Cost:** O(n) time and memory (adjacency in arrays). Totals are `long`.
- **Boundary test:** all-negative chain → weight 0; 100 000-vertex chain checked against a linear DP.

### naive
- Try every start `s` from 0 to `n-m` (inclusive), compare char by char, record all full matches (overlaps included). `m=0` → starts `0..n` (n+1 boundaries); `m>n` → loop does not run → `[]`.
- **Cost:** O(n·m) time, O(1) auxiliary memory (+ output).
- **Boundary test:** `aaaaa`/`aaa` → `[0,1,2]`; detects a wrong last start or non-overlapping search.

### kmp
- `pi[i]` = length of the longest proper prefix of `p[0..i]` that is also its suffix. Built with the standard fallback `k = pi[k-1]` on mismatch.
- Search: on mismatch fall back by `pi`; after a full match record `i-m+1` and set `k = pi[k-1]`, so overlapping occurrences stay possible. `m=0` is handled before touching the pattern.
- **Cost:** O(n+m+z) time (each step either advances `i` or strictly decreases `k`, and `k` grows at most once per `i`), O(m) auxiliary memory.
- **Boundary test:** `abababa`/`aba` → `[0,2,4]`; detects resetting `k=0` after a match.

### rk (base 31, modulus 101)
- Window hash `h = Σ code·31^(m-1-i) mod 101` (leftmost char has the highest power). Rolling update: `h = ((h − code[s]·31^(m−1)) mod 101 + 101) mod 101`, then `h = (h·31 + code[s+m]) mod 101`. Negative remainders are normalised.
- Equal hash is only a **candidate**: characters are compared before a start is reported. `m=0` and `m>n` are handled before any window is built.
- **Cost:** O(n+m+c·m) time, where c = number of equal-hash windows; with modulus 101 this can degrade to O(n·m), so no linear worst-case claim. O(1) auxiliary memory (+ output).
- **Boundary test:** `abdj`/`ab` — both `ab` and `dj` hash to 75; only position 0 is reported. `dj`/`ab` → `[]` (equal hash, no match).

### suffix
- **Prefix doubling:** `rank` after round k orders suffixes by their first k chars; round `2k` sorts by the pair `(rank[i], rank[i+k] or −1)` and re-ranks equal pairs equally. Stop when all ranks are distinct. I use comparison sorting (`Arrays.sort` on `Integer[]`), so O(n log² n); suffix strings are never materialised.
- **Kasai:** `pos[i]` is the rank of suffix i. For suffix `i` compare with the previous suffix in `sa`; if the LCP was `h`, suffix `i+1` has LCP ≥ `h−1` with its predecessor, so `h` only decreases by 1 per step and total work is O(n).
- **Repeated substring:** the longest substring occurring at two distinct starts is a common prefix of two suffixes, and any two suffixes with a common prefix of length L are separated in `sa` by suffixes with LCP ≥ L, so the maximum adjacent `lcp` is the answer length; take `text[sa[i] .. sa[i]+max]`. `lcp[0]=0`; empty text → `[]`,`[]`,`""`.
- **Cost:** O(n log² n) time, O(n) memory. **Boundary test:** `banana` → `sa=[5,3,1,0,4,2]`, `lcp=[0,1,3,0,0,2]`, `ana`; `a`×50000 (deep ranks, max lcp).

### aho
- One trie per request over ASCII 32..126 (95 letters), `go[state*95+c]`. Duplicate patterns end in the **same terminal state** and keep their own dictionary index (`nodeOf[i]`), so they get equal counts.
- BFS builds failure links and completes missing transitions (`go[v][c] = go[fail[v]][c]`), so scanning a text is one array lookup per char.
- **Counting:** per text, `visit[state]++` for every visited state, then propagate in reverse BFS order `visit[fail[u]] += visit[u]` (deeper states first). A pattern's count is `visit[nodeOf[i]]`; this counts overlaps and patterns that are suffixes of longer ones (`he` inside `she`). `visit` is cleared for every text (independent texts).
- **Cost:** build O(L + 95·S + p); each text O(|text| + S + p); memory O(95·S + p) (≈ 38 MB for S = 10⁵+1). Empty dictionary → empty row per text; no texts → `[]`.
- **Boundary test:** `["he","she","he"]` on `sheshe` → `[2,2,2]`; patterns `a, a, aa` on `aaaa` → `[4,4,3]`; 10 000 duplicate `a` patterns × 100 texts (dense matches).

## Own tests

`tests/student/stress.py` (fixed seed 2026) runs everything through the real JSON-Lines program in one batch and checks answers with independent brute force.

|Command|Input or fixture path|Expected result|Actual result / log|What it checks|
|---|---|---|---|---|
|lcs|150 random pairs, |a|,|b|≤9, alphabet abc|length = DP oracle; sequence is a subsequence of both|pass|wrong transition, invalid witness|
|lcs|two random strings of length 1000|length = oracle|pass|full-domain scale|
|merge|100 random size lists, n≤8|cost = memoised DP; plan valid, n−1 merges|pass|adjacency, child-before-parent order|
|merge|200 × 10⁹|cost>0, 199 merges|pass|64-bit overflow|
|tree|150 random trees, n≤10, weights −5..9|weight = subset brute force; selected independent|pass|take/skip logic, reconstruction|
|tree|all-negative chain|weight 0, `[]`|pass|empty set allowed|
|tree|chain n=100000, weights ±10⁹|equals linear DP, independent set|pass|recursion depth, large totals|
|naive/kmp/rk|3×150 random text/pattern, empty pattern included|positions = `startswith` scan|pass|overlaps, empty pattern, m>n|
|rk|`abdj`/`ab`, `dj`/`ab`|`[0]`, `[]`|pass|collision verification|
|kmp|`a`×200000 / `a`×100000|100001 starts|pass|linear time at max size|
|naive|`a`×3000 / `a`×1500; rk `a`×4000 pattern `a`×4000|1501 starts; `[0]`|pass|worst-case size|
|suffix|100 random texts, n≤12|sa = sorted suffixes, lcp = oracle, repeated length = max lcp|pass|doubling, Kasai, witness|
|suffix|`a`×50000|sa = n−1..0, lcp = 0,1,…,n−1|pass|max size, deep ranks|
|aho|60 random dictionaries/texts|counts = brute count per pattern/text|pass|overlaps, duplicates, resets|
|aho|`a,a,aa`/`aaaa`,""; empty dictionary|`[[4,4,3],[0,0,0]]`; `[[],[]]`|pass|duplicates, empty cases|
|aho|445 patterns `a^i`, 10 000 × `a`; 100 texts of 2000|correct counts|pass|dense matches (visit propagation)|

Result: **1024/1024 passed**; `tools/run.py --check`: all 44 public cases and the repeated batch passed.

## Search comparison

Inputs are identical for naive/KMP/RK; correctness (`naive = kmp = rk`) is verified **before** timing. Generator: `Random(4242+n)`, text alphabet `abcd`; the repetitive case is `a^n` with pattern `a^(m-1)b`. Timing scope: one call of `Algorithms.naive/kmp/rk` on already-parsed strings (no JSON, no JVM start, no compilation), via `System.nanoTime`. Warm-up: 30 untimed calls per method/case (JIT is not fully settled for the smallest sizes). One run = mean of 200 calls; 3 runs per method; median reported. Unit: microseconds. Hardware: see Environment.

|Case|n|m|Seed/generator|Algorithm|Run 1 (µs)|Run 2 (µs)|Run 3 (µs)|Median (µs)|Correct outputs checked|
|---|---:|---:|---|---|---:|---:|---:|---:|---|
|random|500|10|Random(4242+n), alphabet abcd, pattern = random substring of text|naive|27.9|29.4|38.7|29.4|yes(1 starts); naive=kmp=rk|
|random|500|10|Random(4242+n), alphabet abcd, pattern = random substring of text|kmp|52.0|6.9|6.5|6.9|yes(1 starts); naive=kmp=rk|
|random|500|10|Random(4242+n), alphabet abcd, pattern = random substring of text|rk|37.0|38.3|38.4|38.3|yes(1 starts); naive=kmp=rk|
|random|1500|30|Random(4242+n), alphabet abcd, pattern = random substring of text|naive|2.9|3.0|3.0|3.0|yes(1 starts); naive=kmp=rk|
|random|1500|30|Random(4242+n), alphabet abcd, pattern = random substring of text|kmp|42.2|42.2|45.7|42.2|yes(1 starts); naive=kmp=rk|
|random|1500|30|Random(4242+n), alphabet abcd, pattern = random substring of text|rk|80.1|79.7|79.9|79.9|yes(1 starts); naive=kmp=rk|
|random|3000|100|Random(4242+n), alphabet abcd, pattern = random substring of text|naive|19.0|5.9|26.5|19.0|yes(1 starts); naive=kmp=rk|
|random|3000|100|Random(4242+n), alphabet abcd, pattern = random substring of text|kmp|100.6|106.6|85.7|100.6|yes(1 starts); naive=kmp=rk|
|random|3000|100|Random(4242+n), alphabet abcd, pattern = random substring of text|rk|48.4|28.7|46.2|46.2|yes(1 starts); naive=kmp=rk|
|repetitive|500|10|text a^n, pattern a^(m-1)b (no match, naive worst case)|naive|2.2|2.3|2.0|2.2|yes(0 starts); naive=kmp=rk|
|repetitive|500|10|text a^n, pattern a^(m-1)b (no match, naive worst case)|kmp|1.8|1.7|1.8|1.8|yes(0 starts); naive=kmp=rk|
|repetitive|500|10|text a^n, pattern a^(m-1)b (no match, naive worst case)|rk|5.0|4.9|4.8|4.9|yes(0 starts); naive=kmp=rk|
|repetitive|1500|30|text a^n, pattern a^(m-1)b (no match, naive worst case)|naive|16.7|37.1|38.2|37.1|yes(0 starts); naive=kmp=rk|
|repetitive|1500|30|text a^n, pattern a^(m-1)b (no match, naive worst case)|kmp|5.2|5.2|15.3|5.2|yes(0 starts); naive=kmp=rk|
|repetitive|1500|30|text a^n, pattern a^(m-1)b (no match, naive worst case)|rk|15.4|14.4|14.3|14.4|yes(0 starts); naive=kmp=rk|
|repetitive|3000|100|text a^n, pattern a^(m-1)b (no match, naive worst case)|naive|347.8|359.4|367.4|359.4|yes(0 starts); naive=kmp=rk|
|repetitive|3000|100|text a^n, pattern a^(m-1)b (no match, naive worst case)|kmp|13.6|13.3|13.2|13.3|yes(0 starts); naive=kmp=rk|
|repetitive|3000|100|text a^n, pattern a^(m-1)b (no match, naive worst case)|rk|36.8|36.5|36.0|36.5|yes(0 starts); naive=kmp=rk|

**Observations (not a proof of complexity).** On the repetitive input naive is clearly the slowest and grows fastest with `m` (about 360 µs for n=3000, m=100), while KMP stays smallest and RK is in between — consistent with O(n·m) vs O(n+m) vs RK that never finds equal-hash windows here. On random input all three do about O(n) work and the differences (a few µs to ~100 µs) are dominated by allocation of the result list/`pi` array and JIT/VM noise on a 1-vCPU machine, so no ranking should be read from the random rows (e.g. some single runs are 5–10× above the others). Medians do not determine asymptotic complexity.

**RK with modulus 101.** Only 101 hash values exist, so for long texts roughly 1 window in 101 collides by chance and every such candidate costs up to m comparisons; a repetitive text with a repetitive pattern can make many windows candidates, so the worst case is O(n·m). The supplied pair `ab`/`dj` (both 75) is kept as a correctness test: the character check rejects `dj`. This tiny modulus is a collision laboratory only; it says nothing about production hashes with a large modulus or randomised base, where false candidates are rare.

## Sources and AI assistance

- Sources: course lectures 8–10 (DP patterns, KMP/hash, suffix array/Kasai/Aho–Corasick), the assignment specification and `docs/LECTURE_CLARIFICATIONS_EN_RU.md`.
- AI assistance: **Claude (Anthropic)** explain the task for me.