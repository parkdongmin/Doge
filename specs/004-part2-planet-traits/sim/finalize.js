// 행성 12종 × 특성 궁합 보정 — 목표 체감 수익(하루 순수익 증가율)에 맞도록 특성 수치를 행성별로 탐색하고,
// 수치가 상한(부작용 방지선)에 걸리면 표시한다. 같은 난수열(공통 난수)을 써서 탐색 노이즈를 줄인다
let seed;
const rnd = () => { seed = (seed * 1664525 + 1013904223) % 4294967296; return seed / 4294967296; };
const uni = (a, b) => a + (b - a) * rnd();
const clamp = (v, lo, hi) => Math.min(Math.max(v, lo), hi);
const SELL = { IRON_ORE: 9, MAGMA_STONE: 14, CRYSTAL: 12, RARE_EARTH: 29, BIOMASS: 12, COOLANT: 17, ENERGY_CORE: 18, LIFE_CRYSTAL: 40, NANOBOT: 32, DATA_CORE: 35, QUANTUM_CORE: 60, UNKNOWN_MATTER: 65 };
const RR = { C: 1.0, U: 1.3, R: 1.8, E: 2.4, L: 3.2 };
const MR = { C: { IRON_ORE: 15 }, U: { IRON_ORE: 20, COOLANT: 15 }, R: { IRON_ORE: 25, COOLANT: 20, NANOBOT: 6 }, E: { IRON_ORE: 30, ENERGY_CORE: 20, NANOBOT: 8 }, L: { IRON_ORE: 35, ENERGY_CORE: 25, NANOBOT: 10 } };
const LVL = 1.9; // Lv10

function sim(p, mods = {}, days = 60, runs = 300) {
  seed = 12345;
  const base = p.prod * 0.15 * LVL * (1 + (mods.prod || 0));
  const bad = p.bad + (mods.bad || 0), th = 0.15 + (mods.th || 0), gm = mods.good || 1, lm = mods.loss ?? 1;
  const maintRes = Object.entries(MR[p.rar]).reduce((s, [r, a]) => s + a * LVL * LVL * SELL[r], 0);
  const drop = Object.entries(p.drops).reduce((s, [r, c]) => s + c / 100 * RR[p.rar] * LVL * SELL[r] * 2, 0) * (mods.drop || 1);
  const iv = 6.0 - (p.risk - 2) * 0.0306, end = days * 24;
  let total = 0, breaks = 0, ceilT = 0, upT = 0;
  for (let i = 0; i < runs; i++) {
    let mult = 1, loss = 0, t = 0, coins = 0, rep = null;
    while (t < end) {
      const nxt = t + iv;
      if (rep !== null && rep < nxt) { coins -= base * mult * loss * lm * (rep - t) * 60 + (base * mult * 90 + maintRes) * lm; loss = 0; t = rep; rep = null; continue; }
      const seg = Math.min(nxt, end) - t;
      coins += (loss > 0 ? -base * mult * loss * lm : base * mult + drop) * seg * 60;
      if (loss === 0) { upT += seg; if (mult >= 1.999) ceilT += seg; }
      t = nxt; if (t >= end) break;
      const isBad = rnd() * 100 < bad, d = uni(0.03, 0.30);
      if (loss > 0) loss = clamp(loss + (isBad ? d : -d), 0.5, 2.0);
      else { mult = clamp(mult + (isBad ? -d : d * gm), 0.5, 2.0); if (isBad && d >= th) { loss = 1; breaks++; rep = t + uni(0.5, 8); } }
    }
    total += coins / days;
  }
  return { v: total / runs, breaks: breaks / runs / days, ceil: ceilT / upT, dropShare: drop * 60 * 24 / (total / runs) };
}

// 특성: 수치(param) → mods, 상한(부작용 방지선)
const GOOD = {
  '높은 생산력': { mods: x => ({ prod: x }), max: 0.40, unit: x => `생산량 +${(x * 100).toFixed(0)}%` },
  '안정된 환경': { mods: x => ({ bad: -x }), max: 10, unit: x => `악재 확률 −${x.toFixed(1)}%p` },
  '튼튼한 구조': { mods: x => ({ th: x }), max: 0.07, unit: x => `고장 기준 15→${(15 + x * 100).toFixed(1)}%p` },

  '호황 체질': { mods: x => ({ good: 1 + x }), max: 0.6, unit: x => `호재폭 ×${(1 + x).toFixed(2)}` },
  '풍부한 자원': { mods: x => ({ drop: 1 + x }), max: 1.5, unit: x => `자원 드롭 ×${(1 + x).toFixed(2)}` },
};
// 목표 하루 순수익 증가율(%) — 궁합 × 강도
const TARGET = { '상': [12, 22, 35], '중': [6, 12, 18], '하': [2, 4, 6] };
// 강화 적성 목표: Lv1→20 기대 강화 시도 감소율(%)
const UP_TARGET = { '상': [12, 22, 32], '중': [6, 12, 18], '하': [2, 4, 6] };

const PL = [
  { n: '습지', rar: 'C', prod: 110, risk: 10, bad: 50, drops: { COOLANT: 8, BIOMASS: 8, LIFE_CRYSTAL: 2 }, hi: ['풍부한 자원', '호황 체질'], lo: ['튼튼한 구조', '강화 적성'] },
  { n: '사막', rar: 'C', prod: 90, risk: 22, bad: 50, drops: { IRON_ORE: 8, CRYSTAL: 2 }, hi: ['튼튼한 구조', '높은 생산력'], lo: ['풍부한 자원', '호황 체질'] },
  { n: '군도', rar: 'C', prod: 100, risk: 15, bad: 50, drops: { BIOMASS: 7, ENERGY_CORE: 3, CRYSTAL: 1 }, hi: ['호황 체질', '안정된 환경'], lo: ['튼튼한 구조', '강화 적성'] },
  { n: '무대기', rar: 'C', prod: 60, risk: 6, bad: 50, drops: { IRON_ORE: 7, CRYSTAL: 2, NANOBOT: 1 }, hi: ['강화 적성', '튼튼한 구조'], lo: ['호황 체질', '높은 생산력'] },
  { n: '가스 I', rar: 'U', prod: 200, risk: 30, bad: 44, drops: { ENERGY_CORE: 8, COOLANT: 3 }, hi: ['높은 생산력', '안정된 환경'], lo: ['튼튼한 구조', '풍부한 자원'] },
  { n: '가스 II', rar: 'U', prod: 125, risk: 15, bad: 44, drops: { COOLANT: 6, DATA_CORE: 2, CRYSTAL: 1 }, hi: ['안정된 환경', '풍부한 자원'], lo: ['높은 생산력', '강화 적성'] },
  { n: '소행성', rar: 'U', prod: 130, risk: 6, bad: 44, drops: { IRON_ORE: 9, RARE_EARTH: 2 }, hi: ['풍부한 자원', '강화 적성'], lo: ['호황 체질', '안정된 환경'] },
  { n: '빙하', rar: 'R', prod: 230, risk: 20, bad: 38, drops: { COOLANT: 8, CRYSTAL: 3 }, hi: ['튼튼한 구조', '안정된 환경'], lo: ['호황 체질', '높은 생산력'] },
  { n: '용암', rar: 'R', prod: 300, risk: 50, bad: 38, drops: { MAGMA_STONE: 8, CRYSTAL: 3 }, hi: ['높은 생산력', '강화 적성'], lo: ['안정된 환경', '호황 체질'] },
  { n: '블랙홀', rar: 'E', prod: 450, risk: 90, bad: 50, drops: { ENERGY_CORE: 6, UNKNOWN_MATTER: 2, QUANTUM_CORE: 1 }, hi: ['튼튼한 구조', '안정된 환경'], lo: ['풍부한 자원', '강화 적성'] },
  { n: '은하', rar: 'E', prod: 360, risk: 10, bad: 33, drops: { ENERGY_CORE: 5, QUANTUM_CORE: 3, CRYSTAL: 2 }, hi: ['높은 생산력', '강화 적성'], lo: ['호황 체질', '튼튼한 구조'] },
  { n: '항성', rar: 'L', prod: 550, risk: 14, bad: 28, drops: { ENERGY_CORE: 7, QUANTUM_CORE: 3, CRYSTAL: 1 }, hi: ['높은 생산력', '풍부한 자원'], lo: ['호황 체질', '안정된 환경'] },
];
const aff = (p, t) => p.hi.includes(t) ? '상' : p.lo.includes(t) ? '하' : '중';

// 강화
const SUCC = { 1: .95, 2: .90, 3: .85, 4: .80, 5: .75, 6: .70, 7: .65, 8: .60, 9: .55, 10: .50, 11: .45, 12: .40, 13: .35, 14: .30, 15: .25, 16: .20, 17: .15, 18: .10, 19: .08 };
const DROPC = { 11: .5, 12: .45, 13: .4, 14: .35, 15: .3, 16: .25, 17: .2, 18: .15, 19: .1 };
function attempts(m) { const E = {}; let s = 0; for (let n = 1; n < 20; n++) { const p = Math.min(SUCC[n] * m, .99), q = (1 - p) * (DROPC[n] || 0); E[n] = (1 + q * (E[n - 1] || 0)) / p; s += E[n]; } return s; }
const A0 = attempts(1);
const upCut = m => (1 - attempts(m) / A0) * 100;

function search(f, target, max) { // f 단조 증가 가정
  if (f(max) < target) return { x: max, capped: true };
  let lo = 0, hi = max;
  for (let i = 0; i < 18; i++) { const mid = (lo + hi) / 2; if (f(mid) < target) lo = mid; else hi = mid; }
  return { x: (lo + hi) / 2, capped: false };
}

const result = {};
const report = [];
for (const p of PL) {
  const b = sim(p);
  result[p.n] = { base: b, traits: {} };
  for (const [t, def] of Object.entries(GOOD)) {
    const a = aff(p, t);
    const lv = TARGET[a].map(tg => search(x => (sim(p, def.mods(x)).v / b.v - 1) * 100, tg, def.max));
    const r3 = sim(p, def.mods(lv[2].x));
    result[p.n].traits[t] = { a, lv, side: { breaks: r3.breaks / b.breaks, ceil: r3.ceil, dropShare: r3.dropShare } };
  }
  const a = aff(p, '강화 적성');
  result[p.n].traits['강화 적성'] = { a, lv: UP_TARGET[a].map(tg => search(x => upCut(1 + x), tg, 0.25)) };
}


// ── 확정: Ⅲ 수치 = 목표 탐색값(상한 이내)을 깔끔한 단위로 반올림, Ⅰ/Ⅱ = Ⅲ의 35% / 65% ──
const ROUND = { "높은 생산력": [0.01, 100, "%"], "안정된 환경": [0.5, 1, "%p"], "튼튼한 구조": [0.005, 100, "%p"], "빠른 복구": [0.05, 100, "%"], "호황 체질": [0.05, 1, ""], "풍부한 자원": [0.1, 1, ""], "강화 적성": [0.01, 100, "%"] };
const rnd2 = (x, step) => Math.max(step, Math.round(x / step) * step);
const desc = (t, x) => ({
  "높은 생산력": "생산량 +" + Math.round(x * 100) + "%",
  "안정된 환경": "악재 확률 −" + x + "%p",
  "튼튼한 구조": "고장 기준 +" + (x * 100).toFixed(1) + "%p",
  "빠른 복구": "고장 손해·정비비 −" + Math.round(x * 100) + "%",
  "호황 체질": "호재폭 +" + Math.round(x * 100) + "%",
  "풍부한 자원": "자원 +" + Math.round(x * 100) + "%",
  "강화 적성": "성공률 ×" + (1 + x).toFixed(2),
})[t];
const FINAL = {};
const lines = [];
lines.push("| 행성 | 기준/일 | " + [...Object.keys(GOOD), "강화 적성"].join(" | ") + " |");
lines.push("|---|---|" + [...Object.keys(GOOD), "강화 적성"].map(() => "---").join("|") + "|");
const valLines = [...lines];
for (const p of PL) {
  const R = result[p.n]; FINAL[p.n] = {};
  const cells = [], vcells = [];
  for (const t of [...Object.keys(GOOD), "강화 적성"]) {
    const T = R.traits[t];
    const step = ROUND[t][0];
    const x3 = rnd2(T.lv[2].x, step);
    const x1 = rnd2(x3 * 0.35, step); const x2 = Math.max(rnd2(x3 * 0.65, step), x1 + step); const xs = [x1, x2, Math.max(x3, x2 + step)];
    const vals = xs.map(x => t === "강화 적성" ? upCut(1 + x) : (sim(p, GOOD[t].mods(x)).v / R.base.v - 1) * 100);
    FINAL[p.n][t] = { a: T.a, xs, vals };
    const star = { "상": "★★★", "중": "★★", "하": "★" }[T.a];
    cells.push(star + " " + xs.map(x => desc(t, x).replace(/^[^+−×]*/, "")).join(" / "));
    vcells.push(star + " " + vals.map(v => (t === "강화 적성" ? "−" : "+") + v.toFixed(0) + "%").join(" / "));
  }
  lines.push("| " + p.n + "(" + p.rar + ") | " + Math.round(R.base.v).toLocaleString() + " | " + cells.join(" | ") + " |");
  valLines.push("| " + p.n + "(" + p.rar + ") | " + Math.round(R.base.v).toLocaleString() + " | " + vcells.join(" | ") + " |");
}
console.log("## 확정 수치 (Ⅰ / Ⅱ / Ⅲ)\n"); console.log(lines.join("\n"));
console.log("\n## 하루 순수익 증가 (강화 적성은 Lv1→20 강화 시도 감소)\n"); console.log(valLines.join("\n"));

// 부작용 재검증 (Ⅲ)
console.log("\n## Ⅲ 부작용 체크\n");
const chk = [];
for (const p of PL) {
  const F = FINAL[p.n], b = result[p.n].base;
  const s1 = sim(p, GOOD["튼튼한 구조"].mods(F["튼튼한 구조"].xs[2]));
  const s2 = sim(p, GOOD["호황 체질"].mods(F["호황 체질"].xs[2]));
  const s3 = sim(p, GOOD["풍부한 자원"].mods(F["풍부한 자원"].xs[2]));
  chk.push(p.n + ": 고장 " + Math.round(s1.breaks / b.breaks * 100) + "% · 상한 " + Math.round(s2.ceil * 100) + "%(기준 " + Math.round(b.ceil * 100) + "%) · 자원비중 " + Math.round(s3.dropShare * 100) + "%");
}
console.log(chk.join("\n"));

// 나쁜 특성
const BAD = { "낮은 생산력": { prod: -0.10 }, "불안정한 환경": { bad: 3 }, "약한 구조": { th: -0.025 }, "메마른 자원": { drop: 0.5 } };
const badVal = {};
for (const p of PL) badVal[p.n] = Object.fromEntries(Object.entries(BAD).map(([k, m]) => [k, (sim(p, m).v / result[p.n].base.v - 1) * 100]));

// 최고 조합
console.log("\n## 최고 조합 (상 특성 2개 Ⅲ)\n");
const best = [];
for (const p of PL) {
  const inc = p.hi.filter(t => t !== "강화 적성"); let mods = {};
  for (const t of inc) Object.assign(mods, GOOD[t].mods(FINAL[p.n][t].xs[2]));
  best.push(p.n + " ×" + (sim(p, mods).v / result[p.n].base.v).toFixed(2) + (inc.length < 2 ? "(+강화)" : ""));
}
console.log(best.join(" · "));

// 티어 기대값
const TIER = { 11: { two: 0, s: [85, 15, 0], badP: 0.25 }, 12: { two: .15, s: [70, 27, 3], badP: 0.22 }, 13: { two: .30, s: [55, 37, 8], badP: 0.18 }, 14: { two: .50, s: [45, 40, 15], badP: 0.14 }, 15: { two: .70, s: [35, 43, 22], badP: 0.10 } };
const OPP = { "높은 생산력": "낮은 생산력", "안정된 환경": "불안정한 환경", "튼튼한 구조": "약한 구조", "풍부한 자원": "메마른 자원", "강화 적성": "강화 부적성" };
const gN = Object.keys(FINAL[PL[0].n]), bN = [...Object.keys(BAD), "강화 부적성"];
const pickW = w => { let r = Math.random() * w.reduce((a, b) => a + b, 0); for (let i = 0; i < w.length; i++) { r -= w[i]; if (r <= 0) return i; } return 2; };
console.log("\n## 티어별 발견 행성 (1만 회)\n\n| 티어 | 나쁜 특성 비율 | 평균 | 꽝(마이너스) | +20% 이상 |\n|---|---|---|---|---|");
for (const [tier, d] of Object.entries(TIER)) {
  let sum = 0, neg = 0, great = 0; const N = 10000;
  for (let i = 0; i < N; i++) {
    const p = PL[Math.floor(Math.random() * PL.length)]; const cnt = Math.random() < d.two ? 2 : 1; let m = 1; const used = [];
    for (let k = 0; k < cnt; k++) {
      let t, v;
      do {
        if (Math.random() < d.badP) { t = bN[Math.floor(Math.random() * bN.length)]; v = t === "강화 부적성" ? -4 : badVal[p.n][t]; }
        else { t = gN[Math.floor(Math.random() * gN.length)]; const F = FINAL[p.n][t]; const sI = pickW(d.s); v = t === "강화 적성" ? F.vals[sI] * 0.4 : F.vals[sI]; }
      } while (used.includes(t) || used.some(u => OPP[u] === t || OPP[t] === u));
      used.push(t); m *= 1 + v / 100;
    }
    sum += m - 1; if (m < 1) neg++; if (m >= 1.2) great++;
  }
  console.log("| T" + tier + " | " + d.badP * 100 + "% | +" + (sum / N * 100).toFixed(1) + "% | " + (neg / N * 100).toFixed(0) + "% | " + (great / N * 100).toFixed(1) + "% |");
}

fs_ = require("fs");
fs_.writeFileSync("final.json", JSON.stringify({ PL, FINAL: Object.fromEntries(Object.entries(FINAL).map(([n, T]) => [n, Object.fromEntries(Object.entries(T).map(([t, v]) => [t, { a: v.a, xs: v.xs }]))])) }, null, 1));
