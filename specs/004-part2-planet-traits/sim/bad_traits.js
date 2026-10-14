// 나쁜 특성의 행성별 하루 순수익 효과 — 특성 시세 보정(balance.md 6절) 표 생성용. 실행: node bad_traits.js ./final.json
const { PL, FINAL } = require(require("path").resolve(process.argv[2] || "./final.json"));
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
const BAD = { "낮은 생산력": { prod: -0.10 }, "불안정한 환경": { bad: 3 }, "약한 구조": { th: -0.025 }, "메마른 자원": { drop: 0.5 } };
console.log("| 행성 | " + Object.keys(BAD).join(" | ") + " |");
for (const p of PL) { const b = sim(p).v; console.log("| " + p.n + " | " + Object.values(BAD).map(m => ((sim(p, m).v / b - 1) * 100).toFixed(0) + "%").join(" | ") + " |"); }
