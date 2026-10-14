// 확정 수치 재검증 — 다른 난수·더 많은 반복·레벨/정비 습관 시나리오 + 티어별 발견 행성을 실제 조합으로 시뮬레이션
const { PL, FINAL } = require("./final.json");
let seed;
const rnd = () => { seed = (seed * 1664525 + 1013904223) % 4294967296; return seed / 4294967296; };
const uni = (a, b) => a + (b - a) * rnd();
const clamp = (v, lo, hi) => Math.min(Math.max(v, lo), hi);
const SELL = { IRON_ORE: 9, MAGMA_STONE: 14, CRYSTAL: 12, RARE_EARTH: 29, BIOMASS: 12, COOLANT: 17, ENERGY_CORE: 18, LIFE_CRYSTAL: 40, NANOBOT: 32, DATA_CORE: 35, QUANTUM_CORE: 60, UNKNOWN_MATTER: 65 };
const RR = { C: 1.0, U: 1.3, R: 1.8, E: 2.4, L: 3.2 };
const MR = { C: { IRON_ORE: 15 }, U: { IRON_ORE: 20, COOLANT: 15 }, R: { IRON_ORE: 25, COOLANT: 20, NANOBOT: 6 }, E: { IRON_ORE: 30, ENERGY_CORE: 20, NANOBOT: 8 }, L: { IRON_ORE: 35, ENERGY_CORE: 25, NANOBOT: 10 } };

function sim(p, mods = {}, o = {}) {
  const { level = 10, days = 90, runs = 600, rep = [0.5, 8], s = 99991 } = o;
  seed = s;
  const LVL = 1 + (level - 1) * 0.1;
  const base = p.prod * 0.15 * LVL * (1 + (mods.prod || 0));
  const bad = p.bad + (mods.bad || 0), th = 0.15 + (mods.th || 0), gm = mods.good || 1;
  const maintRes = Object.entries(MR[p.rar]).reduce((a, [r, n]) => a + n * LVL * LVL * SELL[r], 0);
  const drop = Object.entries(p.drops).reduce((a, [r, c]) => a + c / 100 * RR[p.rar] * LVL * SELL[r] * 2, 0) * (mods.drop || 1);
  const iv = 6.0 - (p.risk - 2) * 0.0306, end = days * 24;
  let total = 0, breaks = 0, ceilT = 0, upT = 0, dropSum = 0;
  for (let i = 0; i < runs; i++) {
    let mult = 1, loss = 0, t = 0, coins = 0, ra = null;
    while (t < end) {
      const nxt = t + iv;
      if (ra !== null && ra < nxt) { coins -= base * mult * loss * (ra - t) * 60 + base * mult * 90 + maintRes; loss = 0; t = ra; ra = null; continue; }
      const seg = Math.min(nxt, end) - t;
      coins += (loss > 0 ? -base * mult * loss : base * mult + drop) * seg * 60;
      if (loss === 0) { upT += seg; dropSum += drop * seg * 60; if (mult >= 1.999) ceilT += seg; }
      t = nxt; if (t >= end) break;
      const isBad = rnd() * 100 < bad, d = uni(0.03, 0.30);
      if (loss > 0) loss = clamp(loss + (isBad ? d : -d), 0.5, 2.0);
      else { mult = clamp(mult + (isBad ? -d : d * gm), 0.5, 2.0); if (isBad && d >= th) { loss = 1; breaks++; ra = t + uni(rep[0], rep[1]); } }
    }
    total += coins;
  }
  return { v: total / runs / days, breaks: breaks / runs / days, ceil: ceilT / upT, dropShare: dropSum / total };
}
const MODS = {
  "높은 생산력": x => ({ prod: x }), "안정된 환경": x => ({ bad: -x }), "튼튼한 구조": x => ({ th: x }),
  "호황 체질": x => ({ good: 1 + x }), "풍부한 자원": x => ({ drop: 1 + x }),
};
const BAD = { "낮은 생산력": { prod: -0.10 }, "불안정한 환경": { bad: 3 }, "약한 구조": { th: -0.025 }, "메마른 자원": { drop: 0.5 } };
const merge = ms => ms.reduce((a, m) => { for (const [k, v] of Object.entries(m)) a[k] = (k === "good" || k === "drop") ? (a[k] ?? 1) * v : (a[k] ?? 0) + v; return a; }, {});
const SUCC = { 1: .95, 2: .90, 3: .85, 4: .80, 5: .75, 6: .70, 7: .65, 8: .60, 9: .55, 10: .50, 11: .45, 12: .40, 13: .35, 14: .30, 15: .25, 16: .20, 17: .15, 18: .10, 19: .08 };
const DROPC = { 11: .5, 12: .45, 13: .4, 14: .35, 15: .3, 16: .25, 17: .2, 18: .15, 19: .1 };
const attempts = m => { const E = {}; let s = 0; for (let n = 1; n < 20; n++) { const p = Math.min(SUCC[n] * m, .99), q = (1 - p) * (DROPC[n] || 0); E[n] = (1 + q * (E[n - 1] || 0)) / p; s += E[n]; } return s; };
const A0 = attempts(1);

// ── 1. 궁합 밴드 검증: 시나리오별 Ⅲ 체감치가 궁합 등급 순서(상 > 중 > 하)를 지키는지 ──
const SCEN = [
  ["Lv10·보통 정비(0.5~8h)", { level: 10 }],
  ["Lv1", { level: 1 }],
  ["Lv20", { level: 20 }],
  ["빠른 정비(0.2~2h)", { rep: [0.2, 2] }],
  ["느린 정비(4~12h)", { rep: [4, 12] }],
];
const BAND = { "상": [25, 45], "중": [10, 25], "하": [0, 10] };
console.log("## 1. 궁합 밴드 검증 — Ⅲ 강도의 하루 순수익 증가율 (밴드: ★★★ 25~45% / ★★ 10~25% / ★ 0~10%)\n");
const out = {}; let miss = [];
for (const [sn, so] of SCEN) {
  const row = { "상": [], "중": [], "하": [] };
  for (const p of PL) {
    const b = sim(p, {}, so).v;
    for (const [t, f] of Object.entries(MODS)) {
      const F = FINAL[p.n][t];
      const v = (sim(p, f(F.xs[2]), so).v / b - 1) * 100;
      row[F.a].push(v);
      const [lo, hi] = BAND[F.a];
      if (sn.startsWith("Lv10") && (v < lo - 0.5 || v > hi + 0.5)) miss.push(`${p.n} ${t} ${F.a} ${v.toFixed(0)}%`);
    }
  }
  const stat = a => `평균 +${(a.reduce((x, y) => x + y, 0) / a.length).toFixed(0)}% (${Math.min(...a).toFixed(0)}~${Math.max(...a).toFixed(0)})`;
  out[sn] = row;
  console.log(`- **${sn}**: ★★★ ${stat(row["상"])} · ★★ ${stat(row["중"])} · ★ ${stat(row["하"])}`);
}
console.log("\n밴드 밖(Lv10 기준): " + (miss.length ? miss.join(", ") : "없음"));
// 강화 적성
const up = { "상": [], "중": [], "하": [] };
for (const p of PL) { const F = FINAL[p.n]["강화 적성"]; up[F.a].push((1 - attempts(1 + F.xs[2]) / A0) * 100); }
console.log(`강화 적성 Ⅲ (Lv1→20 시도 감소): ★★★ −${up["상"][0].toFixed(0)}% · ★★ −${up["중"][0].toFixed(0)}% · ★ −${up["하"][0].toFixed(0)}%, 강화 부적성 +${((attempts(0.95) / A0 - 1) * 100).toFixed(0)}%`);

// ── 2. 부작용 (Ⅲ, 느린 정비 포함 최악 조건) ──
console.log("\n## 2. 부작용 — Ⅲ 특성일 때\n");
console.log("| 행성 | 튼튼한 구조 Ⅲ 고장 빈도(기준 대비) | 안정된 환경 Ⅲ 고장 빈도 | 호황 Ⅲ 상한 붙음(기준) | 풍부한 자원 Ⅲ 자원 비중(기준) |\n|---|---|---|---|---|");
for (const p of PL) {
  const b = sim(p), F = FINAL[p.n];
  const a = sim(p, MODS["튼튼한 구조"](F["튼튼한 구조"].xs[2])), c = sim(p, MODS["안정된 환경"](F["안정된 환경"].xs[2]));
  const g = sim(p, MODS["호황 체질"](F["호황 체질"].xs[2])), d = sim(p, MODS["풍부한 자원"](F["풍부한 자원"].xs[2]));
  console.log(`| ${p.n} | ${(a.breaks / b.breaks * 100).toFixed(0)}% | ${(c.breaks / b.breaks * 100).toFixed(0)}% | ${(g.ceil * 100).toFixed(0)}% (${(b.ceil * 100).toFixed(0)}%) | ${(d.dropShare * 100).toFixed(0)}% (${(b.dropShare * 100).toFixed(0)}%) |`);
}

// ── 3. 등급 역전 (최고 조합 Lv10 vs 다음 등급 행성 최저 기준, 같은 레벨) ──
console.log("\n## 3. 등급 역전 — 각 행성 최고 조합이 한 등급 위 행성들의 기본 수익을 넘는지\n");
const RANK = { C: 0, U: 1, R: 2, E: 3, L: 4 };
const baseOf = Object.fromEntries(PL.map(p => [p.n, sim(p).v]));
const inv = [];
for (const p of PL) {
  const inc = Object.keys(MODS).filter(t => FINAL[p.n][t].a === "상");
  const best = sim(p, merge(inc.map(t => MODS[t](FINAL[p.n][t].xs[2])))).v;
  const nextMin = Math.min(...PL.filter(q => RANK[q.rar] === RANK[p.rar] + 1).map(q => baseOf[q.n]));
  if (isFinite(nextMin) && best > nextMin) inv.push(`${p.n} 최고 ${Math.round(best).toLocaleString()} > 다음 등급 최저 ${Math.round(nextMin).toLocaleString()}`);
}
console.log(inv.length ? inv.join("\n") : "역전 없음 (블랙홀 < 용암은 특성과 무관하게 원래 그런 구조)");

// ── 4. 티어별 발견 행성 — 실제 조합 시뮬레이션 ──
const TIER = {
  11: { two: 0.00, s: [85, 15, 0], badP: 0.25 },
  12: { two: 0.15, s: [70, 27, 3], badP: 0.22 },
  13: { two: 0.30, s: [55, 37, 8], badP: 0.18 },
  14: { two: 0.50, s: [45, 40, 15], badP: 0.14 },
  15: { two: 0.70, s: [35, 43, 22], badP: 0.10 },
};
const W = { 11: [12, 27, 30, 18, 13], 12: [11, 26, 30, 19, 14], 13: [10, 25, 30, 20, 15], 14: [9, 24, 30, 21, 16], 15: [8, 22, 30, 22, 18] };
const byRar = { C: PL.filter(p => p.rar === "C"), U: PL.filter(p => p.rar === "U"), R: PL.filter(p => p.rar === "R"), E: PL.filter(p => p.rar === "E"), L: PL.filter(p => p.rar === "L") };
const OPP = { "높은 생산력": "낮은 생산력", "안정된 환경": "불안정한 환경", "튼튼한 구조": "약한 구조", "풍부한 자원": "메마른 자원", "강화 적성": "강화 부적성" };
const GN = [...Object.keys(MODS), "강화 적성"], BN = [...Object.keys(BAD), "강화 부적성"];
let r2 = 2024; const R = () => { r2 = (r2 * 1103515245 + 12345) % 2147483648; return r2 / 2147483648; };
const pickW = w => { let x = R() * w.reduce((a, b) => a + b, 0); for (let i = 0; i < w.length; i++) { x -= w[i]; if (x <= 0) return i; } return w.length - 1; };
const conflict = (a, b) => a === b || OPP[a] === b || OPP[b] === a;
// 강화 적성/부적성은 수익이 아니라 강화비라, 수익 환산 시 시도 감소율의 40%로 보수 평가
const UPW = 0.4;
console.log("\n## 4. 티어별 발견 행성 — 특성 구성과 결과 (티어당 2,000개 행성을 실제로 조합 시뮬레이션)\n");
console.log("| 티어 | 특성 2개 | 좋은 특성만 | 나쁜 특성 포함 | ★★★ 특성 포함 | Ⅲ 포함 | 💀 꽝(<0%) | 😐 무난(0~10%) | 🙂 좋음(10~20%) | 🤩 대박(≥20%) | 평균 |");
console.log("|---|---|---|---|---|---|---|---|---|---|---|");
const baseFast = Object.fromEntries(PL.map(p => [p.n, sim(p, {}, { runs: 120, days: 60, s: 4242 }).v]));
for (const [tier, d] of Object.entries(TIER)) {
  const N = 2000; let two = 0, onlyGood = 0, hasBad = 0, hasHi = 0, hasIII = 0, c = [0, 0, 0, 0], sum = 0;
  for (let i = 0; i < N; i++) {
    const rar = "CURE L".replace(" ", "")[pickW(W[tier])];
    const pool = byRar[rar]; const p = pool[Math.floor(R() * pool.length)];
    const cnt = R() < d.two ? 2 : 1; if (cnt === 2) two++;
    const picked = [];
    while (picked.length < cnt) {
      const isBad = R() < d.badP;
      const t = isBad ? BN[Math.floor(R() * BN.length)] : GN[Math.floor(R() * GN.length)];
      if (picked.some(q => conflict(q.t, t))) continue;
      picked.push({ t, isBad, s: isBad ? null : pickW(d.s) });
    }
    const anyBad = picked.some(q => q.isBad); if (anyBad) hasBad++; else onlyGood++;
    if (picked.some(q => !q.isBad && FINAL[p.n][q.t].a === "상")) hasHi++;
    if (picked.some(q => q.s === 2)) hasIII++;
    const mods = [], upAdj = [];
    for (const q of picked) {
      if (q.t === "강화 적성") upAdj.push((1 - attempts(1 + FINAL[p.n][q.t].xs[q.s]) / A0) * UPW);
      else if (q.t === "강화 부적성") upAdj.push(-(attempts(0.95) / A0 - 1) * UPW);
      else if (q.isBad) mods.push(BAD[q.t]);
      else mods.push(MODS[q.t](FINAL[p.n][q.t].xs[q.s]));
    }
    let m = mods.length ? sim(p, merge(mods), { runs: 120, days: 60, s: 4242 }).v / baseFast[p.n] : 1;
    for (const u of upAdj) m *= 1 + u;
    const e = (m - 1) * 100; sum += e;
    c[e < 0 ? 0 : e < 10 ? 1 : e < 20 ? 2 : 3]++;
  }
  const P = x => (x / N * 100).toFixed(0) + "%";
  console.log(`| T${tier} | ${P(two)} | ${P(onlyGood)} | ${P(hasBad)} | ${P(hasHi)} | ${P(hasIII)} | ${P(c[0])} | ${P(c[1])} | ${P(c[2])} | ${P(c[3])} | +${(sum / N).toFixed(1)}% |`);
}
