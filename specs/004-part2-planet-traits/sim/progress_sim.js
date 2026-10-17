// 2부 진행 시뮬레이션 — 티어 해금 조건(특성 발견 이력) × 플레이어 유형 → 해금일·장 전환일·이야기 정체 구간
const { PL, FINAL } = require("./final.json");
// mulberry32 — 예전 LCG(s * 1103515245)는 곱이 2^53을 넘어 JS double에서 정밀도가 깨져 난수 품질이 나빴다(2026-10-14 교체)
let s = 777; const R = () => { s |= 0; s = (s + 0x6D2B79F5) | 0; let t = Math.imul(s ^ (s >>> 15), 1 | s); t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t; return ((t ^ (t >>> 14)) >>> 0) / 4294967296; };
const pickW = w => { let x = R() * w.reduce((a, b) => a + b, 0); for (let i = 0; i < w.length; i++) { x -= w[i]; if (x <= 0) return i; } return w.length - 1; };

const TIER = {
  11: { min: 480, w: [12, 27, 30, 18, 13], two: 0.00, st: [85, 15, 0], badP: 0.25 },
  12: { min: 540, w: [11, 26, 30, 19, 14], two: 0.15, st: [70, 27, 3], badP: 0.22 },
  13: { min: 600, w: [10, 25, 30, 20, 15], two: 0.30, st: [55, 37, 8], badP: 0.18 },
  14: { min: 660, w: [9, 24, 30, 21, 16], two: 0.50, st: [45, 40, 15], badP: 0.14 },
  15: { min: 720, w: [8, 22, 30, 22, 18], two: 0.70, st: [35, 43, 22], badP: 0.10 },
};
const RAR = ["C", "U", "R", "E", "L"];
const byRar = Object.fromEntries(RAR.map(r => [r, PL.filter(p => p.rar === r)]));
const GN = ["높은 생산력", "안정된 환경", "튼튼한 구조", "호황 체질", "풍부한 자원", "강화 적성"];
const BN = ["낮은 생산력", "불안정한 환경", "약한 구조", "메마른 자원", "강화 부적성"];
const OPP = { "높은 생산력": "낮은 생산력", "안정된 환경": "불안정한 환경", "튼튼한 구조": "약한 구조", "풍부한 자원": "메마른 자원", "강화 적성": "강화 부적성" };
const conflict = (a, b) => a === b || OPP[a] === b || OPP[b] === a;

function rollTraits(tier) {
  const d = TIER[tier], rar = RAR[pickW(d.w)], p = byRar[rar][Math.floor(R() * byRar[rar].length)];
  const n = R() < d.two ? 2 : 1, out = [];
  while (out.length < n) {
    const bad = R() < d.badP, t = bad ? BN[Math.floor(R() * 5)] : GN[Math.floor(R() * 6)];
    if (out.some(q => conflict(q.t, t))) continue;
    out.push(bad ? { t, bad } : { t, bad, st: pickW(d.st) + 1, aff: FINAL[p.n][t].a });
  }
  return out;
}

const PROFILES = {
  "가벼운 유저": { ships: 2, speed: 0.30, succ: 0.85, disc: 0.30, lag: 4 },
  "보통 유저": { ships: 4, speed: 0.35, succ: 0.90, disc: 0.40, lag: 2 },
  "헤비 유저": { ships: 6, speed: 0.45, succ: 0.93, disc: 0.55, lag: 0.7 },
};
const POOL = 16;

function play(prof, cond, maxDays = 150) {
  // 상태: 해금 티어, 발견 이력, 장(6~10), 장 내 기록 수
  let unlocked = 11; const hist = { traitPlanets: 0, hiGood: 0, hiII: 0, III: 0, hiIII: 0, twoGood: 0 };
  let chapter = 6, inCh = 0, done = false; const succ = {};
  const unlockDay = { 11: 0 }, chEnd = {}, stall = {}, stallRec = {}; let stallStart = null, stallCnt = 0, records = 0, lastT = 1;
  // 우주선별 다음 완료 시각
  const ships = Array.from({ length: prof.ships }, () => ({ t: R() * 2, tier: 11 }));
  const dur = tier => TIER[tier].min / 60 * (1 - prof.speed);
  ships.forEach(sh => sh.t += dur(11));
  const exitMet = c => c === 6 ? true : c === 7 ? succ[11] : c === 8 ? succ[12] : c === 9 ? succ[13] : succ[15];
  while (!done) {
    ships.sort((a, b) => a.t - b.t); const sh = ships[0], now = sh.t;
    if (now / 24 > maxDays) break;
    const ok = R() < prof.succ;
    if (ok) {
      for (let t = 11; t <= sh.tier; t++) succ[t] = true;
      if (R() < prof.disc) {
        const tr = rollTraits(sh.tier); hist.traitPlanets++;
        const good = tr.filter(q => !q.bad);
        if (good.some(q => q.aff === "상")) hist.hiGood++;
        if (good.some(q => q.aff === "상" && q.st >= 2)) hist.hiII++;
        if (good.some(q => q.st === 3)) hist.III++;
        if (good.some(q => q.aff === "상" && q.st === 3)) hist.hiIII++;
        if (good.length === 2) hist.twoGood++;
        for (let t = unlocked + 1; t <= 15; t++) { if (cond[t](hist)) { unlocked = t; unlockDay[t] = now / 24; } else break; }
      }
    }
    // 탐사 기록 = 스토리 진행
    records++; lastT = now;
    const poolDone = inCh >= POOL;
    if (poolDone && exitMet(chapter)) {
      chEnd[chapter] = now / 24;
      if (stallStart !== null) { stall[chapter] = now / 24 - stallStart; stallRec[chapter] = stallCnt; stallStart = null; stallCnt = 0; } else { stall[chapter] = 0; stallRec[chapter] = 0; }
      if (chapter === 10) { done = true; break; }
      chapter++; inCh = 0;
    } else {
      inCh++; if (stallStart !== null) stallCnt++;
      if (inCh >= POOL && !exitMet(chapter) && stallStart === null) stallStart = now / 24;
    }
    sh.tier = unlocked; sh.t = now + dur(unlocked) + R() * prof.lag * 2;
  }
  return { unlockDay, chEnd, stall, stallRec, done, recPerDay: records / (lastT / 24) };
}

const CANDIDATES = {
  "E1": { 12: h => h.traitPlanets >= 8, 13: h => h.hiII >= 1, 14: h => h.traitPlanets >= 30, 15: h => h.hiIII >= 1 },
  "E2": { 12: h => h.traitPlanets >= 10, 13: h => h.hiII >= 2, 14: h => h.traitPlanets >= 40, 15: h => h.hiIII >= 1 },
  "E3": { 12: h => h.traitPlanets >= 10, 13: h => h.hiII >= 2, 14: h => h.traitPlanets >= 40, 15: h => h.hiIII >= 2 },
  "E4": { 12: h => h.traitPlanets >= 12, 13: h => h.hiII >= 3, 14: h => h.traitPlanets >= 50, 15: h => h.hiIII >= 2 },
};

const pct = (a, q) => { const b = [...a].sort((x, y) => x - y); return b[Math.floor(q * (b.length - 1))]; };
const fmt = a => a.length ? `${pct(a, .5).toFixed(1)}일 (${pct(a, .1).toFixed(1)}~${pct(a, .9).toFixed(1)})` : "-";
const which = process.argv[2];
for (const [cn, cond] of Object.entries(CANDIDATES)) {
  if (which && !cn.startsWith(which)) continue;
  console.log(`\n### 조건안 ${cn}\n`);
  console.log("| 유저 | T12 해금 | T13 해금 | T14 해금 | T15 해금 | 2부 완주 |\n|---|---|---|---|---|---|");
  const stallRows = [];
  for (const [pn, prof] of Object.entries(PROFILES)) {
    const recs = [], srec = { 8: [], 9: [], 10: [] }; const N = 1500, U = { 12: [], 13: [], 14: [], 15: [] }, fin = [], st = { 6: [], 7: [], 8: [], 9: [], 10: [] }, stallAny = { 8: 0, 9: 0, 10: 0 };
    let unfinished = 0;
    for (let i = 0; i < N; i++) {
      const r = play(prof, cond);
      for (const t of [12, 13, 14, 15]) if (r.unlockDay[t] !== undefined) U[t].push(r.unlockDay[t]);
      if (r.done) fin.push(r.chEnd[10]); else unfinished++; recs.push(r.recPerDay); for (const c of [8, 9, 10]) if (r.stallRec[c] !== undefined) srec[c].push(r.stallRec[c]);
      for (const c of [6, 7, 8, 9, 10]) if (r.stall[c] !== undefined) { st[c].push(r.stall[c]); if (c >= 8 && r.stall[c] > 0.05) stallAny[c]++; }
    }
    console.log(`| ${pn} | ${fmt(U[12])} | ${fmt(U[13])} | ${fmt(U[14])} | ${fmt(U[15])} | ${fmt(fin)}${unfinished ? ` · 150일 내 미완주 ${(unfinished / N * 100).toFixed(0)}%` : ""} |`);
    stallRows.push(`| ${pn} (하루 기록 ${pct(recs, .5).toFixed(0)}개) | ` + [8, 9, 10].map(c => `${(stallAny[c] / N * 100).toFixed(0)}% · ${pct(st[c], .5).toFixed(1)}일 / 정체 기록 ${pct(srec[c], .5)}개`).join(" | ") + " |");
  }
  console.log("\n이야기 정체(장 기록을 다 봤는데 조건 미달) — 정체가 생길 확률 · 정체 길이 중앙값\n");
  console.log("| 유저 | 8장 (T12 성공 필요) | 9장 (T13 성공 필요) | 10장 (T15 성공 필요) |\n|---|---|---|---|");
  console.log(stallRows.join("\n"));
}
