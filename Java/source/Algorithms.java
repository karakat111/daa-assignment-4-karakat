import java.util.*;

/** Assignment 4: DP and string algorithms. Field contracts: docs/guide/CONTRACTS.md. */
public final class Algorithms {

    // ---------- helpers ----------
    private static String str(Map<String, Object> in, String key) { return (String) in.get(key); }

    private static Map<String, Object> out(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    // ---------- Part 1: LCS ----------
    /** dp[i][j] = LCS length of a[0..i) and b[0..j). Stored flat: (n+1)*(m+1) <= ~1e6 ints. */
    static Map<String, Object> lcs(Map<String, Object> input) {
        String a = str(input, "a"), b = str(input, "b");
        int n = a.length(), m = b.length(), w = m + 1;
        int[] dp = new int[(n + 1) * w];                       // row 0 / column 0 = base case 0
        for (int i = 1; i <= n; i++)
            for (int j = 1; j <= m; j++)
                dp[i * w + j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? dp[(i - 1) * w + j - 1] + 1
                        : Math.max(dp[(i - 1) * w + j], dp[i * w + j - 1]);
        StringBuilder sb = new StringBuilder();                // walk back from (n,m)
        int i = n, j = m;
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) { sb.append(a.charAt(i - 1)); i--; j--; }
            else if (dp[(i - 1) * w + j] >= dp[i * w + j - 1]) i--;
            else j--;
        }
        return out("length", dp[n * w + m], "sequence", sb.reverse().toString());
    }

    // ---------- Part 2: ordered merge (interval DP) ----------
    /** cost[l][r] = min cost to merge blocks l..r into one; split[l][r] = best last split k. */
    static Map<String, Object> merge(Map<String, Object> input) {
        List<?> raw = (List<?>) input.get("sizes");
        int n = raw.size();
        List<List<Long>> plan = new ArrayList<>();
        if (n <= 1) return out("cost", 0L, "plan", plan);
        long[] pre = new long[n + 1];
        for (int i = 0; i < n; i++) pre[i + 1] = pre[i] + ((Number) raw.get(i)).longValue();
        long[][] cost = new long[n][n];
        int[][] split = new int[n][n];
        for (int len = 2; len <= n; len++)
            for (int l = 0; l + len - 1 < n; l++) {
                int r = l + len - 1;
                long best = Long.MAX_VALUE; int bk = l;
                for (int k = l; k < r; k++) {
                    long c = cost[l][k] + cost[k + 1][r];
                    if (c < best) { best = c; bk = k; }
                }
                cost[l][r] = best + pre[r + 1] - pre[l];       // pay combined size of [l..r]
                split[l][r] = bk;
            }
        emit(0, n - 1, split, plan);
        return out("cost", cost[0][n - 1], "plan", plan);
    }

    /** Post-order: children first, then the parent merge (depth <= n-1 <= 199). */
    private static void emit(int l, int r, int[][] split, List<List<Long>> plan) {
        if (l == r) return;
        int k = split[l][r];
        emit(l, k, split, plan);
        emit(k + 1, r, split, plan);
        plan.add(Arrays.asList((long) l, (long) k, (long) r));
    }

    // ---------- Part 3: tree maximum-weight independent set ----------
    /** take[v] = best in subtree if v chosen; skip[v] = best if v not chosen. Iterative (no recursion). */
    static Map<String, Object> tree(Map<String, Object> input) {
        int n = ((Number) input.get("n")).intValue();
        List<Integer> selected = new ArrayList<>();
        if (n == 0) return out("weight", 0L, "selected", selected);
        List<?> ws = (List<?>) input.get("weights");
        List<?> es = (List<?>) input.get("edges");
        long[] w = new long[n];
        for (int i = 0; i < n; i++) w[i] = ((Number) ws.get(i)).longValue();
        int[] head = new int[n], nxt = new int[2 * es.size()], to = new int[2 * es.size()];
        Arrays.fill(head, -1);
        int c = 0;
        for (Object o : es) {
            List<?> e = (List<?>) o;
            int u = ((Number) e.get(0)).intValue(), v = ((Number) e.get(1)).intValue();
            to[c] = v; nxt[c] = head[u]; head[u] = c++;
            to[c] = u; nxt[c] = head[v]; head[v] = c++;
        }
        int[] order = new int[n], parent = new int[n];
        parent[0] = -1; order[0] = 0;
        int qh = 0, qt = 1;
        while (qh < qt) {                                       // BFS order: parent before child
            int v = order[qh++];
            for (int e = head[v]; e != -1; e = nxt[e])
                if (to[e] != parent[v]) { parent[to[e]] = v; order[qt++] = to[e]; }
        }
        long[] take = new long[n], skip = new long[n];
        for (int v = 0; v < n; v++) take[v] = w[v];
        for (int idx = n - 1; idx >= 0; idx--) {                // reverse BFS = children before parent
            int v = order[idx], p = parent[v];
            if (p >= 0) {
                take[p] += skip[v];
                skip[p] += Math.max(take[v], skip[v]);
            }
        }
        boolean[] chosen = new boolean[n];
        for (int idx = 0; idx < n; idx++) {                     // top-down reconstruction
            int v = order[idx], p = parent[v];
            boolean parentTaken = p >= 0 && chosen[p];
            if (!parentTaken && take[v] > skip[v]) { chosen[v] = true; selected.add(v); }
        }
        return out("weight", Math.max(take[0], skip[0]), "selected", selected);
    }

    // ---------- Part 4a: naive ----------
    static Map<String, Object> naive(Map<String, Object> input) {
        String t = str(input, "text"), p = str(input, "pattern");
        int n = t.length(), m = p.length();
        List<Integer> pos = new ArrayList<>();
        for (int s = 0; s + m <= n; s++) {                      // last valid start = n-m (also n for m=0)
            int j = 0;
            while (j < m && t.charAt(s + j) == p.charAt(j)) j++;
            if (j == m) pos.add(s);
        }
        return out("positions", pos);
    }

    // ---------- Part 4b: KMP ----------
    static Map<String, Object> kmp(Map<String, Object> input) {
        String t = str(input, "text"), p = str(input, "pattern");
        int n = t.length(), m = p.length();
        List<Integer> pos = new ArrayList<>();
        if (m == 0) { for (int i = 0; i <= n; i++) pos.add(i); return out("positions", pos); }
        int[] pi = new int[m];                                  // pi[i] = longest proper border of p[0..i]
        for (int i = 1, k = 0; i < m; i++) {
            while (k > 0 && p.charAt(i) != p.charAt(k)) k = pi[k - 1];
            if (p.charAt(i) == p.charAt(k)) k++;
            pi[i] = k;
        }
        for (int i = 0, k = 0; i < n; i++) {
            while (k > 0 && t.charAt(i) != p.charAt(k)) k = pi[k - 1];
            if (t.charAt(i) == p.charAt(k)) k++;
            if (k == m) { pos.add(i - m + 1); k = pi[k - 1]; }  // fallback keeps overlapping matches
        }
        return out("positions", pos);
    }

    // ---------- Part 4c: Rabin-Karp (base 31, mod 101) ----------
    static Map<String, Object> rk(Map<String, Object> input) {
        final int B = 31, M = 101;
        String t = str(input, "text"), p = str(input, "pattern");
        int n = t.length(), m = p.length();
        List<Integer> pos = new ArrayList<>();
        if (m == 0) { for (int i = 0; i <= n; i++) pos.add(i); return out("positions", pos); }
        if (m > n) return out("positions", pos);
        int hp = 0, ht = 0, high = 1;                           // high = B^(m-1) mod M
        for (int i = 0; i < m; i++) {
            hp = (hp * B + p.charAt(i)) % M;
            ht = (ht * B + t.charAt(i)) % M;
            if (i > 0) high = high * B % M;
        }
        for (int s = 0; ; s++) {
            if (hp == ht) {                                     // candidate only: verify characters
                int j = 0;
                while (j < m && t.charAt(s + j) == p.charAt(j)) j++;
                if (j == m) pos.add(s);
            }
            if (s + m >= n) break;
            ht = ((ht - t.charAt(s) * high) % M + M) % M;       // drop leftmost, normalise negative
            ht = (ht * B + t.charAt(s + m)) % M;                // append next char
        }
        return out("positions", pos);
    }

    // ---------- Part 5: suffix array (prefix doubling) + Kasai ----------
    static Map<String, Object> suffix(Map<String, Object> input) {
        String s = str(input, "text");
        int n = s.length();
        if (n == 0) return out("sa", new int[0], "lcp", new int[0], "repeated", "");
        Integer[] boxed = new Integer[n];
        int[] rank = new int[n], tmp = new int[n];
        for (int i = 0; i < n; i++) { boxed[i] = i; rank[i] = s.charAt(i); }
        for (int k = 1; ; k <<= 1) {                            // rank = order by first k chars
            final int kk = k; final int[] r = rank;
            Comparator<Integer> cmp = (x, y) -> {
                if (r[x] != r[y]) return Integer.compare(r[x], r[y]);
                int rx = x + kk < n ? r[x + kk] : -1, ry = y + kk < n ? r[y + kk] : -1;
                return Integer.compare(rx, ry);
            };
            Arrays.sort(boxed, cmp);                            // O(n log n) per round -> O(n log^2 n)
            tmp[boxed[0]] = 0;
            for (int i = 1; i < n; i++) tmp[boxed[i]] = tmp[boxed[i - 1]] + (cmp.compare(boxed[i - 1], boxed[i]) < 0 ? 1 : 0);
            int[] sw = rank; rank = tmp; tmp = sw;
            if (rank[boxed[n - 1]] == n - 1 || k >= n) break;   // all ranks distinct
        }
        int[] sa = new int[n], pos = new int[n];
        for (int i = 0; i < n; i++) { sa[i] = boxed[i]; pos[sa[i]] = i; }
        int[] lcp = new int[n];                                 // Kasai: h drops by at most 1 per step
        for (int i = 0, h = 0; i < n; i++) {
            if (pos[i] > 0) {
                int j = sa[pos[i] - 1];
                while (i + h < n && j + h < n && s.charAt(i + h) == s.charAt(j + h)) h++;
                lcp[pos[i]] = h;
                if (h > 0) h--;
            } else h = 0;
        }
        int best = 0, at = 0;
        for (int i = 1; i < n; i++) if (lcp[i] > best) { best = lcp[i]; at = sa[i]; }
        return out("sa", sa, "lcp", lcp, "repeated", s.substring(at, at + best));
    }

    // ---------- Part 6: Aho-Corasick ----------
    static Map<String, Object> aho(Map<String, Object> input) {
        List<?> pats = (List<?>) input.get("patterns"), texts = (List<?>) input.get("texts");
        int p = pats.size(), q = texts.size();
        final int A = 95;                                       // ASCII 32..126
        int total = 1;
        for (Object o : pats) total += ((String) o).length();
        int[] go = new int[total * A];                          // 0 = absent (root is never a child)
        int[] fail = new int[total], nodeOf = new int[p];
        int states = 1;
        for (int i = 0; i < p; i++) {                           // trie; duplicates share a terminal node
            int v = 0;
            for (char ch : ((String) pats.get(i)).toCharArray()) {
                int c = ch - 32;
                if (go[v * A + c] == 0) go[v * A + c] = states++;
                v = go[v * A + c];
            }
            nodeOf[i] = v;
        }
        int[] order = new int[states];
        int qh = 0, qt = 0;
        for (int c = 0; c < A; c++) {                           // BFS: complete transitions + failure links
            int u = go[c];
            if (u != 0) { fail[u] = 0; order[qt++] = u; }
        }
        while (qh < qt) {
            int v = order[qh++];
            for (int c = 0; c < A; c++) {
                int u = go[v * A + c];
                if (u != 0) { fail[u] = go[fail[v] * A + c]; order[qt++] = u; }
                else go[v * A + c] = go[fail[v] * A + c];
            }
        }
        List<long[]> counts = new ArrayList<>();
        long[] visit = new long[states];
        for (Object o : texts) {
            Arrays.fill(visit, 0);                              // reset per independent text
            int v = 0;
            for (char ch : ((String) o).toCharArray()) { v = go[v * A + (ch - 32)]; visit[v]++; }
            for (int i = qt - 1; i >= 0; i--) { int u = order[i]; visit[fail[u]] += visit[u]; } // deeper first
            long[] row = new long[p];
            for (int i = 0; i < p; i++) row[i] = visit[nodeOf[i]];
            counts.add(row);
        }
        return out("counts", counts);
    }
}
