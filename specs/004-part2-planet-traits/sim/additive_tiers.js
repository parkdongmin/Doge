// 티어별 발견 행성 결과를 "앱 계산 방식"(특성 효과 % 단순 합 = 특성 시세 보정)으로 다시 뽑는다.
// verify.js 4절과 같은 뽑기 절차·난수(r2 = 2024)를 쓰고, 효과 값은 balance.md 5절 하단 표·6절 나쁜 특성 표를 그대로 읽는다.
// 실행: node additive_tiers.js   → balance.md 7절 "앱 계산 기준" 표, PlanetTraitRollerTest 기대값
const fs = require("fs"), path = require("path");
const { PL, FINAL } = require("./final.json");
const md = fs.readFileSync(path.join(__dirname, "..", "balance.md"), "utf8").split("\n");
const num = s => parseFloat(s.replace("−", "-").replace("%", ""));
// 5절 하단 표: | 행성 | 기준 | 높은 생산력 | 안정된 환경 | 튼튼한 구조 | 호황 체질 | 풍부한 자원 | 강화 적성 |
const GOOD_COLS = ["높은 생산력", "안정된 환경", "튼튼한 구조", "호황 체질", "풍부한 자원", "강화 적성"];
const BAD_COLS = ["낮은 생산력", "불안정한 환경", "약한 구조", "메마른 자원"];
const good = {}, bad = {};
let mode = null;
for (const line of md) {
  if (line.startsWith("| 행성 | 기준 순수익/일")) { mode = "good"; continue; }
  if (line.startsWith("| 행성 | 낮은 생산력")) { mode = "bad"; continue; }
  if (!line.startsWith("|") || line.startsWith("|---")) { if (!line.startsWith("|---")) mode = null; continue; }
  const c = line.split("|").slice(1, -1).map(s => s.trim());
  if (mode === "good") good[c[0]] = Object.fromEntries(GOOD_COLS.map((t, i) => [t, c[i + 2].split("/").map(num)]));
  if (mode === "bad") bad[c[0]] = Object.fromEntries(BAD_COLS.map((t, i) => [t, num(c[i + 1])]));
}
const UPW = 0.4, INAPT = -8;
const effect = (pn, q) => {
  if (q.t === "강화 적성") return -good[pn][q.t][q.s] * UPW;   // 시도 감소율(음수)의 40%를 이득으로
  if (q.t === "강화 부적성") return INAPT;
  return q.isBad ? bad[pn][q.t] : good[pn][q.t][q.s];
};
const TIER = { 11: { two: 0.00, s: [85, 15, 0], badP: 0.25 }, 12: { two: 0.15, s: [70, 27, 3], badP: 0.22 }, 13: { two: 0.30, s: [55, 37, 8], badP: 0.18 }, 14: { two: 0.50, s: [45, 40, 15], badP: 0.14 }, 15: { two: 0.70, s: [35, 43, 22], badP: 0.10 } };
const W = { 11: [12, 27, 30, 18, 13], 12: [11, 26, 30, 19, 14], 13: [10, 25, 30, 20, 15], 14: [9, 24, 30, 21, 16], 15: [8, 22, 30, 22, 18] };
const byRar = Object.fromEntries(["C", "U", "R", "E", "L"].map(r => [r, PL.filter(p => p.rar === r)]));
const OPP = { "높은 생산력": "낮은 생산력", "안정된 환경": "불안정한 환경", "튼튼한 구조": "약한 구조", "풍부한 자원": "메마른 자원", "강화 적성": "강화 부적성" };
const GN = GOOD_COLS, BN = [...BAD_COLS, "강화 부적성"];
// mulberry32 — verify.js의 LCG(r2 * 1103515245)는 곱이 2^53을 넘어 JS double에서 정밀도가 깨져 난수 품질이 나빴다(2026-10-14 발견)
let r2 = 2024; const R = () => { r2 |= 0; r2 = (r2 + 0x6D2B79F5) | 0; let t = Math.imul(r2 ^ (r2 >>> 15), 1 | r2); t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t; return ((t ^ (t >>> 14)) >>> 0) / 4294967296; };
const pickW = w => { let x = R() * w.reduce((a, b) => a + b, 0); for (let i = 0; i < w.length; i++) { x -= w[i]; if (x <= 0) return i; } return w.length - 1; };
const conflict = (a, b) => a === b || OPP[a] === b || OPP[b] === a;
const N = Number(process.argv[2] || 20000);
console.log(`| 티어 | 꽝(<0%) | 무난(0~10%) | 좋음(10~20%) | 대박(≥20%) | 평균 | 최소 | 최대 |\n|---|---|---|---|---|---|---|---|`);
for (const [tier, d] of Object.entries(TIER)) {
  const c = [0, 0, 0, 0]; let sum = 0, mn = 1e9, mx = -1e9;
  for (let i = 0; i < N; i++) {
    const rar = "CUREL"[pickW(W[tier])];
    const pool = byRar[rar]; const p = pool[Math.floor(R() * pool.length)];
    const cnt = R() < d.two ? 2 : 1; const picked = [];
    while (picked.length < cnt) {
      const isBad = R() < d.badP;
      const t = isBad ? BN[Math.floor(R() * BN.length)] : GN[Math.floor(R() * GN.length)];
      if (picked.some(q => conflict(q.t, t))) continue;
      picked.push({ t, isBad, s: isBad ? null : pickW(d.s) });
    }
    const e = picked.reduce((a, q) => a + effect(p.n, q), 0);
    sum += e; mn = Math.min(mn, e); mx = Math.max(mx, e);
    c[e < 0 ? 0 : e < 10 ? 1 : e < 20 ? 2 : 3]++;
  }
  const P = x => (x / N * 100).toFixed(0) + "%";
  console.log(`| T${tier} | ${P(c[0])} | ${P(c[1])} | ${P(c[2])} | ${P(c[3])} | +${(sum / N).toFixed(1)}% | ${mn.toFixed(0)}% | +${mx.toFixed(0)}% |`);
}
