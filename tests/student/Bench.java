import java.util.*;

/** Search comparison. Build: javac -d /tmp/b Java/source/*.java tests/student/Bench.java ; run: java -cp /tmp/b Bench
 *  Scope: one call of Algorithms.naive/kmp/rk on already-parsed strings (no JSON, no JVM start, no compile).
 *  Warm-up: 30 untimed calls per method/case. Run = mean of REPS calls; 3 runs per method; median reported (microseconds). */
public class Bench {
    static final int REPS = 200, RUNS = 3, WARM = 30;
    static String rnd(Random r, int n, String al) { StringBuilder b = new StringBuilder(); for (int i = 0; i < n; i++) b.append(al.charAt(r.nextInt(al.length()))); return b.toString(); }
    static String rep(char c, int n) { char[] a = new char[n]; Arrays.fill(a, c); return new String(a); }

    public static void main(String[] a) {
        long seed = 4242;
        int[][] sizes = {{500, 10}, {1500, 30}, {3000, 100}};
        System.out.println("Java " + System.getProperty("java.version") + ", seed=" + seed);
        System.out.println("case|n|m|algo|run1_us|run2_us|run3_us|median_us|outputs_equal");
        for (String kind : new String[]{"random", "repetitive"})
            for (int[] sz : sizes) {
                int n = sz[0], m = sz[1];
                Random r = new Random(seed + n);
                String text, pat;
                if (kind.equals("random")) { text = rnd(r, n, "abcd"); int s = r.nextInt(n - m); pat = text.substring(s, s + m); }
                else { text = rep('a', n); pat = rep('a', m - 1) + "b"; }
                Map<String, Object> in = new HashMap<>(); in.put("text", text); in.put("pattern", pat);
                Object pn = Algorithms.naive(in).get("positions"), pk = Algorithms.kmp(in).get("positions"), pr = Algorithms.rk(in).get("positions");
                boolean eq = pn.equals(pk) && pk.equals(pr);                 // correctness BEFORE timing
                String[] names = {"naive", "kmp", "rk"};
                for (String name : names) {
                    for (int i = 0; i < WARM; i++) call(name, in);
                    double[] t = new double[RUNS];
                    for (int k = 0; k < RUNS; k++) {
                        long t0 = System.nanoTime();
                        for (int i = 0; i < REPS; i++) call(name, in);
                        t[k] = (System.nanoTime() - t0) / 1000.0 / REPS;
                    }
                    double[] s = t.clone(); Arrays.sort(s);
                    System.out.printf("%s|%d|%d|%s|%.1f|%.1f|%.1f|%.1f|%s%n", kind, n, m, name, t[0], t[1], t[2], s[1], eq ? "yes(" + ((List<?>) pn).size() + " starts)" : "NO");
                }
            }
    }
    static Object call(String n, Map<String, Object> in) {
        switch (n) { case "naive": return Algorithms.naive(in); case "kmp": return Algorithms.kmp(in); default: return Algorithms.rk(in); }
    }
}
