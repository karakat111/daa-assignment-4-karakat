# Own tests

- `stress.py` — 1024 randomized/edge/scale requests through the real JSON-Lines program, checked by independent brute force (fixed seed 2026).
  Run: `python tests/student/stress.py`
- `Bench.java` — naive/KMP/RK comparison used in ANALYSIS.md.
  Run: `javac -d /tmp/b Java/source/*.java tests/student/Bench.java && java -cp /tmp/b Bench`

Expected/actual results and the defect each case detects are in ANALYSIS.md ("Own tests").
