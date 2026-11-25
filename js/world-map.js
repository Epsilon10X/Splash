/**
 * Mapa Continental 3D Isometrico Unificado — Brote
 * Renderiza un unico continente continuo en relieve 3D con 4 acabados seleccionables:
 *   1. 'zootropolis': maqueta solar Brote con acantilados profundos, rio central en canon, senderos y puentes 3D
 *   2. 'fortnite': vista aerea tactica 3D con curvas de cota, nubes volumetricas con sombra, balizas POI y red viaria
 *   3. 'atlas': Relieve Suave (continente curvo escultorico, detalles escenicos y etiquetas pastel)
 *   4. 'topo': Fortnite Suave (fusion de Relieve Suave curvo con estetica tactica, cotas y balizas Fortnite)
 */
window.BroteWorldMap = (function () {
  const MAP_WIDTH = 960;
  const MAP_HEIGHT = 680;
  const HIT_STEP = 5;
  const HIT_W = Math.ceil(MAP_WIDTH / HIT_STEP);
  const HIT_H = Math.ceil(MAP_HEIGHT / HIT_STEP);

  const U_MIN = -25;
  const U_MAX = 25;
  const V_MIN = 8;
  const V_MAX = 64;
  const HALF_W = 16.4;
  const HALF_H = 8.95;
  const ORIGIN_X = 480;
  const ORIGIN_Y = 24;

  const SUN_RAW = { x: 0.55, y: -0.45, z: 0.70 };
  const SUN_LEN = Math.hypot(SUN_RAW.x, SUN_RAW.y, SUN_RAW.z);
  const SUN = { x: SUN_RAW.x / SUN_LEN, y: SUN_RAW.y / SUN_LEN, z: SUN_RAW.z / SUN_LEN };

  const ZONES = [
    { id: 'valle',     label: 'VALLEY',   title: 'Alpine Valley',       nx: -0.58, ny: -0.48, cx: 230, cy: 195, smoothCx: 235, smoothCy: 185 },
    { id: 'fiordo',    label: 'FJORD',    title: 'Nordic Fjord',        nx: -0.06, ny: -0.56, cx: 455, cy: 155, smoothCx: 450, smoothCy: 148 },
    { id: 'desierto',  label: 'OASIS',    title: 'Golden Oasis',        nx: 0.36,  ny: -0.50, cx: 635, cy: 182, smoothCx: 638, smoothCy: 176 },
    { id: 'toscana',   label: 'TUSCANY',  title: 'Tuscan Villa',        nx: 0.68,  ny: -0.26, cx: 762, cy: 255, smoothCx: 766, smoothCy: 248 },
    { id: 'jungla',    label: 'JUNGLE',   title: 'Emerald Jungle',      nx: -0.64, ny: 0.04,  cx: 205, cy: 335, smoothCx: 202, smoothCy: 330 },
    { id: 'kioto',     label: 'KYOTO',    title: 'Kyoto Sanctuary',     nx: -0.24, ny: -0.04, cx: 382, cy: 318, smoothCx: 378, smoothCy: 312 },
    { id: 'arrozales', label: 'BALI',     title: 'Bali Terraces',       nx: 0.25,  ny: 0.02,  cx: 585, cy: 334, smoothCx: 588, smoothCy: 330 },
    { id: 'otono',     label: 'AUTUMN',   title: 'Autumn Woods',        nx: 0.68,  ny: 0.18,  cx: 764, cy: 392, smoothCx: 762, smoothCy: 388 },
    { id: 'costa',     label: 'COAST',    title: 'Mediterranean Coast', nx: -0.34, ny: 0.50,  cx: 342, cy: 482, smoothCx: 338, smoothCy: 480 },
    { id: 'atolon',    label: 'ATOLL',    title: 'Coral Atoll',         nx: 0.34,  ny: 0.50,  cx: 626, cy: 482, smoothCx: 632, smoothCy: 480 }
  ];

  const ZONES_I18N = {
    valle:     { es: ['VALLE ALPINO', 'Valle Alpino'],       en: ['VALLEY', 'Alpine Valley'] },
    fiordo:    { es: ['FIORDO',       'Fiordo Nórdico'],     en: ['FJORD', 'Nordic Fjord'] },
    desierto:  { es: ['OASIS',        'Oasis Dorado'],       en: ['OASIS', 'Golden Oasis'] },
    toscana:   { es: ['TOSCANA',      'Villa Toscana'],      en: ['TUSCANY', 'Tuscan Villa'] },
    jungla:    { es: ['JUNGLA',       'Selva Esmeralda'],    en: ['JUNGLE', 'Emerald Jungle'] },
    kioto:     { es: ['KIOTO',        'Santuario Kioto'],    en: ['KYOTO', 'Kyoto Sanctuary'] },
    arrozales: { es: ['BALI',         'Terrazas de Bali'],   en: ['BALI', 'Bali Terraces'] },
    otono:     { es: ['OTOÑO',        'Bosque de Otoño'],    en: ['AUTUMN', 'Autumn Woods'] },
    costa:     { es: ['COSTA',        'Costa Mediterránea'], en: ['COAST', 'Mediterranean Coast'] },
    atolon:    { es: ['ATOLÓN',       'Atolón Coralino'],    en: ['ATOLL', 'Coral Atoll'] }
  };

  function applyLanguage(lang) {
    const useEn = lang ? lang === 'en' : !!(window.SplashI18n && window.SplashI18n.isEn());
    ZONES.forEach(z => {
      const entry = ZONES_I18N[z.id];
      if (entry) {
        const pair = useEn ? entry.en : entry.es;
        z.label = pair[0];
        z.title = pair[1];
      }
    });
  }
  applyLanguage();

  const ROADS = [
    ['valle', 'fiordo', false],
    ['fiordo', 'desierto', true],
    ['desierto', 'toscana', false],
    ['valle', 'jungla', false],
    ['valle', 'kioto', false],
    ['fiordo', 'kioto', true],
    ['kioto', 'arrozales', true],
    ['desierto', 'arrozales', false],
    ['toscana', 'otono', false],
    ['arrozales', 'otono', false],
    ['jungla', 'costa', false],
    ['kioto', 'costa', true],
    ['arrozales', 'atolon', false],
    ['costa', 'atolon', true]
  ];

  const PROGRESSION_ORDER = [
    'valle', 'jungla', 'costa', 'kioto', 'desierto',
    'arrozales', 'otono', 'fiordo', 'atolon', 'toscana'
  ];

  const ZONE_MAP = {};
  const ZONE_INDEX = {};
  ZONES.forEach((z, idx) => {
    ZONE_MAP[z.id] = z;
    ZONE_INDEX[z.id] = idx;
  });

  function getZone(id) {
    return ZONE_MAP[id] || ZONES[0];
  }

  function clamp(v, lo, hi) {
    return Math.max(lo, Math.min(hi, v));
  }

  function hash2D(x, y, seed = 17) {
    const n = Math.sin(x * 127.1 + y * 311.7 + seed * 74.7) * 43758.5453123;
    return n - Math.floor(n);
  }

  function hexToRgb(hex) {
    const raw = String(hex || '#C8D0C8').trim();
    if (raw.startsWith('rgb')) {
      const m = raw.match(/rgba?\(\s*([\d.]+)\s*,\s*([\d.]+)\s*,\s*([\d.]+)/i);
      if (m) {
        return {
          r: clamp(Number(m[1]), 0, 255),
          g: clamp(Number(m[2]), 0, 255),
          b: clamp(Number(m[3]), 0, 255)
        };
      }
    }
    const clean = raw.replace('#', '');
    const full = clean.length === 3 ? clean.split('').map(c => c + c).join('') : clean;
    const num = parseInt(full, 16);
    if (!Number.isFinite(num)) return { r: 200, g: 208, b: 200 };
    return { r: (num >> 16) & 255, g: (num >> 8) & 255, b: num & 255 };
  }

  function rgbStr(r, g, b) {
    return `rgb(${clamp(Math.round(r), 0, 255)}, ${clamp(Math.round(g), 0, 255)}, ${clamp(Math.round(b), 0, 255)})`;
  }

  function mixHex(aHex, bHex, t) {
    const a = hexToRgb(aHex);
    const b = hexToRgb(bHex);
    const k = clamp(t, 0, 1);
    return rgbStr(a.r + (b.r - a.r) * k, a.g + (b.g - a.g) * k, a.b + (b.b - a.b) * k);
  }

  function toLockedGrey(hex, isDark) {
    const { r, g, b } = hexToRgb(hex);
    const lum = 0.299 * r + 0.587 * g + 0.114 * b;
    const scaled = isDark ? (lum * 0.42 + 24) : (lum * 0.62 + 88);
    return rgbStr(scaled, scaled + 1, scaled + 3);
  }

  function lambertShade(hex, nx, ny, nz, boost = 0, shadowBias = 0) {
    const { r, g, b } = hexToRgb(hex);
    const dot = nx * SUN.x + ny * SUN.y + nz * SUN.z;
    const pct = dot * 26 - 10 + boost - shadowBias;
    const f = 1 + pct / 100;
    const warm = dot > 0.2 ? 9 : 0;
    return rgbStr(r * f + warm, g * f + warm * 0.72, b * f + warm * 0.25);
  }

  function riverCenterNx(ny) {
    return -0.04 + 0.085 * Math.sin(ny * 2.5 + 0.35);
  }

  function tribStreamNy(nx) {
    return -0.15 + 0.07 * Math.sin(nx * 4.2 + 0.6);
  }

  function continentBoundaryRadius(nx, ny) {
    const ang = Math.atan2(ny, nx);
    const base =
      0.95 +
      0.06 * Math.cos(ang * 2 - 0.35) +
      0.045 * Math.sin(ang * 3 + 0.75) +
      0.03 * Math.cos(ang * 5 - 1.0);
    const fjordInlet =
      0.20 *
      Math.exp(-Math.pow((nx + 0.04) / 0.14, 2)) *
      clamp((-ny - 0.46) / 0.48, 0, 1);
    const southBay =
      0.19 *
      Math.exp(-Math.pow((nx - 0.04) / 0.19, 2)) *
      clamp((ny - 0.46) / 0.50, 0, 1);
    return base - fjordInlet - southBay;
  }

  function classifyBiome(nx, ny) {
    const wx = nx + 0.035 * Math.sin(ny * 6.2);
    const wy = ny + 0.035 * Math.cos(nx * 5.8);
    let best = ZONES[0];
    let bestD = Infinity;
    let secondD = Infinity;
    for (let i = 0; i < ZONES.length; i++) {
      const z = ZONES[i];
      const d = Math.hypot((wx - z.nx) * 0.96, (wy - z.ny) * 1.06);
      if (d < bestD) {
        secondD = bestD;
        bestD = d;
        best = z;
      } else if (d < secondD) {
        secondD = d;
      }
    }
    return { zone: best, margin: secondD - bestD };
  }

  function buildWorldTerrain() {
    const cells = [];
    const biomeAccum = {};
    ZONES.forEach(z => {
      biomeAccum[z.id] = { sumX: 0, sumY: 0, sumZ: 0, count: 0, peakGx: 0, peakGy: 0, peakZ: -9 };
    });

    for (let v = V_MIN; v <= V_MAX; v++) {
      for (let u = U_MIN; u <= U_MAX; u++) {
        if (((u + v) & 1) !== 0) continue;
        const gx = (u + v) * 0.5;
        const gy = (v - u) * 0.5;
        const nx = u / 22.8;
        const ny = (v - 36) / 25.5;
        const eDist = Math.hypot(nx / 1.02, ny / 0.95);
        const rim = continentBoundaryRadius(nx, ny);
        const shoreDepth = rim - eDist;

        if (shoreDepth < -0.10) continue;

        const { zone, margin } = classifyBiome(nx, ny);
        const bid = zone.id;
        const nVal = (hash2D(gx, gy, 31) - 0.5) * 0.24;
        const localDx = nx - zone.nx;
        const localDy = ny - zone.ny;
        const localDist = Math.hypot(localDx, localDy);

        // Reef lagoon shelf right off the SE Atolon crescent
        const isAtolonShallow =
          shoreDepth < 0 &&
          bid === 'atolon' &&
          shoreDepth > -0.095;

        if (shoreDepth < 0 && !isAtolonShallow) continue;

        const rNx = riverCenterNx(ny);
        const riverDist = Math.abs(nx - rNx);
        const inFjordBay = bid === 'fiordo' && ny < -0.60 && Math.abs(nx + 0.04) < 0.14;
        const isCentralRiver = riverDist < 0.058 && ny > -0.82 && ny < 0.85;

        const streamDist = Math.abs(ny - tribStreamNy(nx));
        const isWestStream =
          nx > -0.74 &&
          nx < rNx &&
          streamDist < 0.046 &&
          (bid === 'jungla' || bid === 'kioto');

        const isRiver = isCentralRiver || isWestStream || inFjordBay;

        let wz = 0.85;
        let color = '#8EC37C';
        let accentFlag = null;

        if (isAtolonShallow) {
          wz = 0.34;
          color = shoreDepth > -0.048 ? '#72DCD6' : '#4BBCC0';
          accentFlag = 'atolon-reef';
        } else if (isRiver) {
          const isWaterfallStep = isWestStream && Math.abs(nx + 0.34) < 0.08;
          wz = isWaterfallStep ? 0.56 : 0.26;
          color = isWaterfallStep ? '#B4F0FA' : inFjordBay ? '#4383A6' : '#53B7CE';
          if (isWaterfallStep) accentFlag = 'waterfall';
        } else {
          // Base biome stepped 3D terrain relief matching mascot.js palettes
          let rawH = 1.05 + Math.max(0, 0.38 - shoreDepth) * 0.15;
          if (bid === 'valle') {
            const peak = Math.max(0, 1 - Math.hypot(nx + 0.72, ny + 0.62) * 2.5);
            const lake = Math.hypot(nx + 0.44, ny + 0.40) < 0.11;
            if (lake) {
              rawH = 0.44;
              color = '#6CC6DF';
            } else {
              rawH = 1.05 + peak * 2.35 + (ny < -0.52 ? 0.42 : 0) + nVal;
              color = rawH > 2.45 ? '#F4F8FC' : rawH > 1.85 ? '#487B50' : rawH > 1.35 ? '#569B58' : '#74B46A';
            }
          } else if (bid === 'fiordo') {
            const peakW = Math.max(0, 1 - Math.hypot(nx + 0.24, ny + 0.56) * 2.9);
            const peakE = Math.max(0, 1 - Math.hypot(nx + 0.06 - 0.18, ny + 0.60) * 2.9);
            rawH = 1.28 + Math.max(peakW, peakE) * 2.45 + nVal;
            color = rawH > 2.48 ? '#EFF6FB' : rawH > 1.75 ? '#8FAABE' : '#6F8B9C';
          } else if (bid === 'desierto') {
            const oasisPool = Math.hypot(nx - 0.40, ny + 0.46) < 0.105;
            if (oasisPool) {
              rawH = 0.38;
              color = '#4DB8B6';
            } else {
              const duneWave = Math.sin(nx * 14 + ny * 9) * 0.34;
              rawH = 1.02 + Math.max(0, 0.32 - localDist) * 1.8 + duneWave + nVal * 0.6;
              color = rawH > 1.78 ? '#E8C587' : rawH > 1.28 ? '#DCAD6A' : '#C38E4B';
            }
          } else if (bid === 'toscana') {
            const hill = Math.max(0, 1 - localDist * 2.1);
            rawH = 1.08 + hill * 1.42 + Math.sin(nx * 10) * 0.14 + nVal;
            const stripe = ((gx + gy) & 1) === 0;
            color = rawH > 1.85 ? '#C7AE64' : stripe ? '#9EB466' : '#DFB66A';
          } else if (bid === 'jungla') {
            const mesa = Math.max(0, 1 - Math.hypot(nx + 0.72, ny - 0.04) * 2.3);
            rawH = 1.24 + mesa * 1.55 + Math.abs(Math.sin(nx * 8 + ny * 8)) * 0.25 + nVal;
            color = rawH > 2.25 ? '#1B472C' : rawH > 1.65 ? '#2E6943' : '#4E8F5B';
          } else if (bid === 'kioto') {
            const shrineHill = Math.max(0, 1 - Math.hypot(nx + 0.22, ny + 0.10) * 2.6);
            rawH = 1.06 + shrineHill * 1.28 + nVal;
            const sakuraPatch = hash2D(gx, gy, 77) > 0.62;
            color = sakuraPatch ? '#F6CBD7' : rawH > 1.75 ? '#88AE7D' : '#6DA370';
          } else if (bid === 'arrozales') {
            const rings = Math.floor(localDist * 22);
            const isFloodStep = rings % 2 === 1 && localDist < 0.34;
            rawH = 0.82 + Math.max(0, 0.36 - localDist) * 3.1 + (isFloodStep ? -0.14 : 0.12);
            color = isFloodStep ? '#5AC2CF' : rawH > 1.58 ? '#4E9952' : '#74C16B';
          } else if (bid === 'otono') {
            const ridge = Math.max(0, 1 - localDist * 2.3);
            rawH = 1.12 + ridge * 1.25 + nVal;
            const roll = hash2D(gx, gy, 53);
            color = roll > 0.66 ? '#DA603A' : roll > 0.33 ? '#EA933E' : '#859E56';
          } else if (bid === 'costa') {
            const cliff = Math.max(0, 0.42 - shoreDepth) * 1.1 + Math.max(0, 1 - localDist * 2.4) * 1.1;
            rawH = 1.15 + cliff + nVal * 0.5;
            color = shoreDepth < 0.08 ? '#F3E3C2' : rawH > 1.8 ? '#DEC293' : '#EEDBB8';
          } else {
            // atolon inner sand + reef ring
            const innerLagoon = Math.hypot(nx - 0.34, ny - 0.50) < 0.11;
            if (innerLagoon) {
              rawH = 0.36;
              color = '#4CC7C4';
            } else {
              rawH = 0.78 + Math.max(0, 0.28 - localDist) * 0.85 + nVal * 0.4;
              color = '#F4E5C8';
            }
          }

          // Taper outer coastal cliff drop smoothly while keeping crisp stepped ledges
          const edgeDrop = shoreDepth < 0.06 ? 0.55 : shoreDepth < 0.13 ? 0.84 : 1.0;
          const stepped = Math.round((rawH * edgeDrop) * 3.2) / 3.2;
          wz = Math.max(0.42, stepped);
        }

        const sx = ORIGIN_X + u * HALF_W;
        const cell = {
          u, v, gx, gy, nx, ny,
          bid,
          margin,
          shoreDepth,
          isWater: isRiver || isAtolonShallow,
          isCoastalEdge: shoreDepth < 0.065,
          accentFlag,
          wz,
          color,
          sx
        };
        cells.push(cell);

        if (!cell.isWater || bid === 'fiordo' || bid === 'atolon') {
          const acc = biomeAccum[bid];
          acc.sumX += gx;
          acc.sumY += gy;
          acc.sumZ += wz;
          acc.count += 1;
          if (wz > acc.peakZ) {
            acc.peakZ = wz;
            acc.peakGx = gx;
            acc.peakGy = gy;
          }
        }
      }
    }

    // Place biome center anchors cleanly on real terrain voxels
    ZONES.forEach(z => {
      const acc = biomeAccum[z.id];
      if (acc.count > 0) {
        z.gxCenter = Math.round(acc.sumX / acc.count);
        z.gyCenter = Math.round(acc.sumY / acc.count);
        z.wzCenter = acc.sumZ / acc.count;
      } else {
        z.gxCenter = 18;
        z.gyCenter = 18;
        z.wzCenter = 1.4;
      }
    });

    return { cells, biomeAccum };
  }

  const TERRAIN = buildWorldTerrain();

  // Compute screen coordinates for each zone centroid with default elevation scale
  function projectIso(gx, gy, wz, elevPx) {
    return {
      x: ORIGIN_X + (gx - gy) * HALF_W,
      y: ORIGIN_Y + (gx + gy) * HALF_H - wz * elevPx
    };
  }

  ZONES.forEach(z => {
    const pt = projectIso(z.gxCenter, z.gyCenter, z.wzCenter, 13.0);
    z.cx = Math.round(pt.x);
    z.cy = Math.round(pt.y);
  });

  // Hit-grid (populated per render for pixel-accurate click & hover tests)
  const hitGrid = new Uint8Array(HIT_W * HIT_H);

  function markHitRect(x0, y0, x1, y1, biomeIdxPlusOne) {
    const ix0 = clamp(Math.floor(x0 / HIT_STEP), 0, HIT_W - 1);
    const ix1 = clamp(Math.ceil(x1 / HIT_STEP), 0, HIT_W - 1);
    const iy0 = clamp(Math.floor(y0 / HIT_STEP), 0, HIT_H - 1);
    const iy1 = clamp(Math.ceil(y1 / HIT_STEP), 0, HIT_H - 1);
    for (let iy = iy0; iy <= iy1; iy++) {
      const rowOff = iy * HIT_W;
      for (let ix = ix0; ix <= ix1; ix++) {
        hitGrid[rowOff + ix] = biomeIdxPlusOne;
      }
    }
  }

  function hitTestBiome(mapX, mapY) {
    const ix = Math.floor(mapX / HIT_STEP);
    const iy = Math.floor(mapY / HIT_STEP);
    if (ix < 0 || ix >= HIT_W || iy < 0 || iy >= HIT_H) return null;
    const code = hitGrid[iy * HIT_W + ix];
    if (!code) return null;
    return ZONES[code - 1] ? ZONES[code - 1].id : null;
  }

  function drawIsoPrism(ctx, gx, gy, widthFrac, zBot, zTop, baseHex, opts) {
    const elevPx = opts.elevPx;
    const m = (1 - widthFrac) * 0.5;
    const x0 = gx - 0.5 + m;
    const x1 = gx + 0.5 - m;
    const y0 = gy - 0.5 + m;
    const y1 = gy + 0.5 - m;

    const pN = projectIso(x0, y0, zTop, elevPx);
    const pE = projectIso(x1, y0, zTop, elevPx);
    const pS = projectIso(x1, y1, zTop, elevPx);
    const pW = projectIso(x0, y1, zTop, elevPx);

    const bE = projectIso(x1, y0, zBot, elevPx);
    const bS = projectIso(x1, y1, zBot, elevPx);
    const bW = projectIso(x0, y1, zBot, elevPx);

    const sideHex = opts.cliffHex || baseHex;
    const boost = opts.boost || 0;
    const shadow = opts.shadowBias || 0;
    const inkStroke = opts.inkStroke || null;
    const contourStroke = opts.contourStroke || null;

    // Left vertical face (+Y front-left facing southwest -> normal (0, +1, 0))
    ctx.beginPath();
    ctx.moveTo(pW.x, pW.y);
    ctx.lineTo(pS.x, pS.y);
    ctx.lineTo(bS.x, bS.y);
    ctx.lineTo(bW.x, bW.y);
    ctx.closePath();
    ctx.fillStyle = lambertShade(sideHex, 0, 1, 0, boost, shadow + 4);
    ctx.fill();
    if (inkStroke) {
      ctx.strokeStyle = inkStroke;
      ctx.lineWidth = 0.85;
      ctx.stroke();
    }

    // Right vertical face (+X front-right facing southeast -> normal (+1, 0, 0))
    ctx.beginPath();
    ctx.moveTo(pS.x, pS.y);
    ctx.lineTo(pE.x, pE.y);
    ctx.lineTo(bE.x, bE.y);
    ctx.lineTo(bS.x, bS.y);
    ctx.closePath();
    ctx.fillStyle = lambertShade(sideHex, 1, 0, 0, boost, shadow - 2);
    ctx.fill();
    if (inkStroke) {
      ctx.strokeStyle = inkStroke;
      ctx.lineWidth = 0.85;
      ctx.stroke();
    }

    // Top horizontal face (normal (0, 0, 1))
    ctx.beginPath();
    ctx.moveTo(pN.x, pN.y);
    ctx.lineTo(pE.x, pE.y);
    ctx.lineTo(pS.x, pS.y);
    ctx.lineTo(pW.x, pW.y);
    ctx.closePath();
    ctx.fillStyle = lambertShade(baseHex, 0, 0, 1, boost + 4, shadow);
    ctx.fill();
    if (contourStroke) {
      ctx.strokeStyle = contourStroke;
      ctx.lineWidth = 0.8;
      ctx.stroke();
    } else if (inkStroke) {
      ctx.strokeStyle = inkStroke;
      ctx.lineWidth = 0.6;
      ctx.stroke();
    }

    if (opts.hitCode) {
      markHitRect(
        Math.min(pW.x, pN.x),
        Math.min(pN.y, pE.y),
        Math.max(pE.x, pS.x),
        bS.y,
        opts.hitCode
      );
    }
  }

  function drawLandmarkAtCell(ctx, cell, style, greyOut, isDark, elevPx, hitCode) {
    const gx = cell.gx;
    const gy = cell.gy;
    const z0 = cell.wz;
    const bid = cell.bid;
    const zZone = getZone(bid);
    const isCenter = Math.hypot(gx - zZone.gxCenter, gy - zZone.gyCenter) < 1.25;
    const r = hash2D(gx, gy, 91);
    const pOpts = {
      elevPx,
      hitCode,
      inkStroke: style === 'atlas' ? 'rgba(94, 68, 46, 0.55)' : null
    };

    const col = hex => (greyOut ? toLockedGrey(hex, isDark) : hex);

    if (cell.isWater) {
      // Sailing ships in Fiordo / Costa / Atolon bays
      if (bid === 'fiordo' && cell.shoreDepth < 0.08 && r > 0.90) {
        drawIsoPrism(ctx, gx, gy, 0.55, z0, z0 + 0.35, col('#704C34'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.14, z0 + 0.35, z0 + 1.05, col('#C75145'), pOpts);
      } else if (bid === 'atolon' && r > 0.84) {
        // Stilt palafito hut over turquoise reef
        drawIsoPrism(ctx, gx, gy, 0.52, z0, z0 + 0.32, col('#8B6544'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.44, z0 + 0.32, z0 + 0.72, col('#FAF0DA'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.56, z0 + 0.72, z0 + 0.96, col('#B97B55'), pOpts);
      } else if (bid === 'costa' && r > 0.88) {
        drawIsoPrism(ctx, gx, gy, 0.44, z0, z0 + 0.22, col('#FAF7F0'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.12, z0 + 0.22, z0 + 0.86, col('#FFFFFF'), pOpts);
      }
      return;
    }

    // Center Hero Monuments (1 per biome, unmistakable 3D voxel silhouette)
    if (isCenter && !cell.heroDrawn) {
      cell.heroDrawn = true;
      if (bid === 'costa') {
        // Tall white & terracotta Lighthouse with warm glowing lantern
        drawIsoPrism(ctx, gx, gy, 0.76, z0, z0 + 0.55, col('#E4CFA9'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.52, z0 + 0.55, z0 + 1.55, col('#F7EBD5'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.56, z0 + 1.10, z0 + 1.32, col('#D97757'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.40, z0 + 1.55, z0 + 1.92, col('#FFF2AA'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.48, z0 + 1.92, z0 + 2.15, col('#D97757'), pOpts);
        return;
      }
      if (bid === 'kioto') {
        // 3-tier Vermilion Pagoda + Cherry Blossom canopy
        drawIsoPrism(ctx, gx, gy, 0.78, z0, z0 + 0.45, col('#C95240'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.62, z0 + 0.45, z0 + 0.95, col('#FAF0E4'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.70, z0 + 0.95, z0 + 1.25, col('#B94232'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.44, z0 + 1.25, z0 + 1.72, col('#C95240'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.18, z0 + 1.72, z0 + 2.10, col('#E8C068'), pOpts);
        return;
      }
      if (bid === 'jungla') {
        // Stepped Mayan Emerald Stone Temple Pyramid
        drawIsoPrism(ctx, gx, gy, 0.86, z0, z0 + 0.48, col('#6D7D72'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.64, z0 + 0.48, z0 + 0.96, col('#7E8E82'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.42, z0 + 0.96, z0 + 1.48, col('#92A395'), pOpts);
        return;
      }
      if (bid === 'valle') {
        // Alpine Chalet + Tall Snow Pine
        drawIsoPrism(ctx, gx, gy, 0.68, z0, z0 + 0.56, col('#FAF0DC'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.78, z0 + 0.56, z0 + 0.92, col('#C46747'), pOpts);
        return;
      }
      if (bid === 'fiordo') {
        // Nordic Red Timber Lodge with Steep Snow Roof
        drawIsoPrism(ctx, gx, gy, 0.72, z0, z0 + 0.54, col('#B54236'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.80, z0 + 0.54, z0 + 0.96, col('#EEF6FC'), pOpts);
        return;
      }
      if (bid === 'desierto') {
        // Golden Sandstone Desert Citadel & Dome
        drawIsoPrism(ctx, gx, gy, 0.74, z0, z0 + 0.68, col('#DFB16D'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.46, z0 + 0.68, z0 + 1.15, col('#FAF0DA'), pOpts);
        return;
      }
      if (bid === 'toscana') {
        // Tuscan Warm Terracotta Villa + Cypress Tower
        drawIsoPrism(ctx, gx, gy, 0.76, z0, z0 + 0.66, col('#F7E8CC'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.84, z0 + 0.66, z0 + 0.96, col('#C86848'), pOpts);
        return;
      }
      if (bid === 'arrozales') {
        // Balinese Tiered Meru Temple Pagoda
        drawIsoPrism(ctx, gx, gy, 0.72, z0, z0 + 0.42, col('#D6BA8D'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.58, z0 + 0.42, z0 + 0.85, col('#3C3834'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.42, z0 + 0.85, z0 + 1.32, col('#4E4842'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.24, z0 + 1.32, z0 + 1.65, col('#322E2A'), pOpts);
        return;
      }
      if (bid === 'otono') {
        // Cozy Autumn Timber Cabin + Brick Chimney
        drawIsoPrism(ctx, gx, gy, 0.70, z0, z0 + 0.56, col('#8B593B'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.78, z0 + 0.56, z0 + 0.94, col('#C4492D'), pOpts);
        return;
      }
      // atolon beach pavilion
      drawIsoPrism(ctx, gx, gy, 0.68, z0, z0 + 0.48, col('#FAF0DC'), pOpts);
      drawIsoPrism(ctx, gx, gy, 0.78, z0 + 0.48, z0 + 0.82, col('#D98752'), pOpts);
      return;
    }

    // Scattered Biome Micro-Trees & Houses
    if (cell.margin < 0.09) return;
    if (bid === 'valle' && r > 0.76) {
      drawIsoPrism(ctx, gx, gy, 0.22, z0, z0 + 0.26, col('#6D523A'), pOpts);
      drawIsoPrism(ctx, gx, gy, 0.52, z0 + 0.26, z0 + 0.72, col('#3C7046'), pOpts);
      drawIsoPrism(ctx, gx, gy, 0.32, z0 + 0.72, z0 + 1.04, col('#4D8856'), pOpts);
    } else if (bid === 'fiordo' && r > 0.78) {
      drawIsoPrism(ctx, gx, gy, 0.22, z0, z0 + 0.24, col('#5E4A3A'), pOpts);
      drawIsoPrism(ctx, gx, gy, 0.50, z0 + 0.24, z0 + 0.68, col('#48685E'), pOpts);
      drawIsoPrism(ctx, gx, gy, 0.34, z0 + 0.68, z0 + 0.96, col('#F2F7FB'), pOpts);
    } else if (bid === 'jungla' && r > 0.66) {
      drawIsoPrism(ctx, gx, gy, 0.22, z0, z0 + 0.32, col('#5A422E'), pOpts);
      drawIsoPrism(ctx, gx, gy, 0.66, z0 + 0.32, z0 + 0.78, col(r > 0.84 ? '#28623D' : '#3C7E4F'), pOpts);
    } else if (bid === 'kioto' && r > 0.72) {
      drawIsoPrism(ctx, gx, gy, 0.20, z0, z0 + 0.26, col('#684C3D'), pOpts);
      drawIsoPrism(ctx, gx, gy, 0.60, z0 + 0.26, z0 + 0.66, col(r > 0.86 ? '#F6B6C9' : '#FAD0DC'), pOpts);
    } else if (bid === 'desierto' && r > 0.82) {
      drawIsoPrism(ctx, gx, gy, 0.18, z0, z0 + 0.52, col('#8B6342'), pOpts);
      drawIsoPrism(ctx, gx, gy, 0.54, z0 + 0.52, z0 + 0.74, col('#558E52'), pOpts);
    } else if (bid === 'toscana' && r > 0.75) {
      // Tall Slender Tuscan Cypress
      drawIsoPrism(ctx, gx, gy, 0.28, z0, z0 + 1.05, col('#4D6D3B'), pOpts);
    } else if (bid === 'otono' && r > 0.68) {
      drawIsoPrism(ctx, gx, gy, 0.20, z0, z0 + 0.26, col('#6D4E38'), pOpts);
      drawIsoPrism(ctx, gx, gy, 0.60, z0 + 0.26, z0 + 0.72, col(r > 0.88 ? '#D84030' : r > 0.78 ? '#EC822E' : '#F4BF42'), pOpts);
    } else if (bid === 'costa' && r > 0.77) {
      // Mediterranean Whitewash Cube + Cobalt Dome or Olive Tree
      if (r > 0.86) {
        drawIsoPrism(ctx, gx, gy, 0.56, z0, z0 + 0.44, col('#F7EBD5'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.40, z0 + 0.44, z0 + 0.64, col('#3574B5'), pOpts);
      } else {
        drawIsoPrism(ctx, gx, gy, 0.18, z0, z0 + 0.22, col('#6E563F'), pOpts);
        drawIsoPrism(ctx, gx, gy, 0.48, z0 + 0.22, z0 + 0.52, col('#789468'), pOpts);
      }
    } else if (bid === 'arrozales' && r > 0.80) {
      drawIsoPrism(ctx, gx, gy, 0.20, z0, z0 + 0.46, col('#6BA65C'), pOpts);
    } else if (bid === 'atolon' && r > 0.78) {
      drawIsoPrism(ctx, gx, gy, 0.18, z0, z0 + 0.48, col('#8B6544'), pOpts);
      drawIsoPrism(ctx, gx, gy, 0.52, z0 + 0.48, z0 + 0.70, col('#539856'), pOpts);
    }
  }

  function getSmoothContinentPts(scale = 1.0, offsetX = 0, offsetY = 0) {
    const pts = [];
    const steps = 120;
    const midX = 482;
    const midY = 336;
    const rxBase = 398 * scale;
    const ryBase = 248 * scale;
    for (let i = 0; i < steps; i++) {
      const t = (i / steps) * Math.PI * 2;
      const cosT = Math.cos(t);
      const sinT = Math.sin(t);
      const wave =
        0.95 +
        0.055 * Math.cos(t * 2 - 0.35) +
        0.042 * Math.sin(t * 3 + 0.75) +
        0.025 * Math.cos(t * 5 - 1.0);
      const fjordNotch = sinT < -0.45
        ? 0.18 * Math.exp(-Math.pow((cosT + 0.04) / 0.16, 2))
        : 0;
      const southBay = sinT > 0.45
        ? 0.16 * Math.exp(-Math.pow((cosT - 0.05) / 0.22, 2))
        : 0;
      const r = wave - fjordNotch - southBay;
      pts.push({
        x: midX + cosT * rxBase * r + offsetX,
        y: midY + sinT * ryBase * r + offsetY
      });
    }
    return pts;
  }

  function traceSmoothClosedPoly(ctx, pts) {
    const n = pts.length;
    if (n < 3) return;
    ctx.beginPath();
    const startMidX = (pts[n - 1].x + pts[0].x) * 0.5;
    const startMidY = (pts[n - 1].y + pts[0].y) * 0.5;
    ctx.moveTo(startMidX, startMidY);
    for (let i = 0; i < n; i++) {
      const next = pts[(i + 1) % n];
      const mx = (pts[i].x + next.x) * 0.5;
      const my = (pts[i].y + next.y) * 0.5;
      ctx.quadraticCurveTo(pts[i].x, pts[i].y, mx, my);
    }
    ctx.closePath();
  }

  function buildOrganicMoundPts(cx, cy, rx, ry, rot = 0, seed = 13, wobble = 0.10) {
    const pts = [];
    const n = 20;
    const cosR = Math.cos(rot);
    const sinR = Math.sin(rot);
    for (let i = 0; i < n; i++) {
      const a = (i / n) * Math.PI * 2;
      const mod = 1 + wobble * Math.sin(a * 3 + seed) + wobble * 0.6 * Math.cos(a * 2 - seed * 0.7);
      const lx = Math.cos(a) * rx * mod;
      const ly = Math.sin(a) * ry * mod;
      pts.push({
        x: cx + lx * cosR - ly * sinR,
        y: cy + lx * sinR + ly * cosR
      });
    }
    return pts;
  }

  function drawSmooth3DTerrace(ctx, cx, cy, rx, ry, rot, elevPx, baseHex, isGrey, isDark, seed = 13, wobble = 0.08) {
    const col = hex => (isGrey ? toLockedGrey(hex, isDark) : hex);
    const main = col(baseHex);
    const topPts = buildOrganicMoundPts(cx, cy - elevPx, rx, ry, rot, seed, wobble);
    const wallPts = buildOrganicMoundPts(cx, cy, rx, ry, rot, seed, wobble);
    const shadowPts = buildOrganicMoundPts(cx + 4, cy + Math.max(4, elevPx * 0.35), rx * 1.03, ry * 1.03, rot, seed, wobble);

    // 1. Sombra suave proyectada hacia SE
    ctx.save();
    traceSmoothClosedPoly(ctx, shadowPts);
    ctx.fillStyle = isDark ? 'rgba(0, 0, 0, 0.28)' : 'rgba(20, 42, 48, 0.16)';
    ctx.fill();

    // 2. Talud 3D redondeado lateral
    const gradWall = ctx.createLinearGradient(cx - rx, cy, cx + rx, cy + elevPx);
    gradWall.addColorStop(0, mixHex(main, '#1E2A26', isDark ? 0.45 : 0.28));
    gradWall.addColorStop(1, mixHex(main, '#0E1714', isDark ? 0.62 : 0.42));
    traceSmoothClosedPoly(ctx, wallPts);
    ctx.fillStyle = gradWall;
    ctx.fill();

    // Banda vertical intermedia para sellar suavemente el perfil entre wallPts y topPts
    if (elevPx > 4) {
      const midPts = buildOrganicMoundPts(cx, cy - elevPx * 0.5, rx, ry, rot, seed, wobble);
      traceSmoothClosedPoly(ctx, midPts);
      ctx.fillStyle = mixHex(main, '#182420', isDark ? 0.38 : 0.24);
      ctx.fill();
    }

    // 3. Meseta superior iluminada desde NW
    const gradTop = ctx.createLinearGradient(cx - rx, cy - elevPx - ry, cx + rx, cy - elevPx + ry);
    gradTop.addColorStop(0, mixHex(main, '#FFFFFF', isDark ? 0.10 : 0.22));
    gradTop.addColorStop(0.55, main);
    gradTop.addColorStop(1, mixHex(main, '#2A3632', isDark ? 0.25 : 0.12));
    traceSmoothClosedPoly(ctx, topPts);
    ctx.fillStyle = gradTop;
    ctx.fill();
    ctx.strokeStyle = isDark ? 'rgba(255, 255, 255, 0.14)' : 'rgba(255, 255, 255, 0.48)';
    ctx.lineWidth = 1.25;
    ctx.stroke();
    ctx.restore();
  }

  function drawSmoothPeak(ctx, cx, cy, baseW, h, capRatio, rockHex, snowHex, isGrey, isDark) {
    const col = hex => (isGrey ? toLockedGrey(hex, isDark) : hex);
    const leftRock = col(mixHex(rockHex, '#FFFFFF', isDark ? 0.08 : 0.18));
    const rightRock = col(mixHex(rockHex, '#1C262E', isDark ? 0.42 : 0.25));
    const leftSnow = col(snowHex);
    const rightSnow = col(mixHex(snowHex, '#8AA8C0', isDark ? 0.36 : 0.22));
    const hw = baseW * 0.5;

    ctx.save();
    // Sombra suave en la base
    ctx.fillStyle = isDark ? 'rgba(0,0,0,0.28)' : 'rgba(18,38,44,0.16)';
    ctx.beginPath();
    ctx.ellipse(cx + 6, cy + 4, hw * 1.04, hw * 0.36, 0.08, 0, Math.PI * 2);
    ctx.fill();

    // Cara izquierda iluminada (curva suave hacia cima redondeada)
    ctx.beginPath();
    ctx.moveTo(cx - hw, cy);
    ctx.quadraticCurveTo(cx - hw * 0.38, cy - h * 0.65, cx, cy - h);
    ctx.quadraticCurveTo(cx - hw * 0.04, cy - h * 0.35, cx, cy + hw * 0.24);
    ctx.closePath();
    ctx.fillStyle = leftRock;
    ctx.fill();

    // Cara derecha sombreada
    ctx.beginPath();
    ctx.moveTo(cx + hw, cy);
    ctx.quadraticCurveTo(cx + hw * 0.38, cy - h * 0.65, cx, cy - h);
    ctx.quadraticCurveTo(cx - hw * 0.04, cy - h * 0.35, cx, cy + hw * 0.24);
    ctx.closePath();
    ctx.fillStyle = rightRock;
    ctx.fill();

    // Casquete nevado si corresponde
    if (capRatio > 0) {
      const sw = hw * capRatio;
      const sh = h * capRatio;
      ctx.beginPath();
      ctx.moveTo(cx - sw, cy - h + sh);
      ctx.quadraticCurveTo(cx - sw * 0.4, cy - h + sh * 0.35, cx, cy - h);
      ctx.lineTo(cx, cy - h + sh * 1.12);
      ctx.quadraticCurveTo(cx - sw * 0.45, cy - h + sh * 0.90, cx - sw, cy - h + sh);
      ctx.closePath();
      ctx.fillStyle = leftSnow;
      ctx.fill();

      ctx.beginPath();
      ctx.moveTo(cx + sw, cy - h + sh);
      ctx.quadraticCurveTo(cx + sw * 0.4, cy - h + sh * 0.35, cx, cy - h);
      ctx.lineTo(cx, cy - h + sh * 1.12);
      ctx.quadraticCurveTo(cx + sw * 0.45, cy - h + sh * 0.90, cx + sw, cy - h + sh);
      ctx.closePath();
      ctx.fillStyle = rightSnow;
      ctx.fill();
    }
    ctx.restore();
  }

  function drawSmoothLandmark(ctx, bid, x, y, isGrey, isDark) {
    const col = hex => (isGrey ? toLockedGrey(hex, isDark) : hex);
    ctx.save();
    // Sombra suave bajo el hito
    ctx.fillStyle = isDark ? 'rgba(0,0,0,0.32)' : 'rgba(18,34,30,0.18)';
    ctx.beginPath();
    ctx.ellipse(x + 2, y + 6, 17, 6.5, 0, 0, Math.PI * 2);
    ctx.fill();

    if (bid === 'costa') {
      // Cluster costero: molino blanco en lo alto, casitas mediterraneas y gran faro con haz calido
      // Molino en ladera izquierda
      ctx.fillStyle = col('#FAF5EA');
      ctx.beginPath();
      ctx.roundRect(x - 36, y - 6, 10, 13, 3);
      ctx.fill();
      ctx.fillStyle = col('#3574B5');
      ctx.beginPath();
      ctx.arc(x - 31, y - 6, 5.2, Math.PI, 0);
      ctx.fill();
      ctx.strokeStyle = col('#FFFFFF');
      ctx.lineWidth = 1.6;
      ctx.beginPath();
      ctx.moveTo(x - 37, y - 8); ctx.lineTo(x - 25, y + 4);
      ctx.moveTo(x - 25, y - 8); ctx.lineTo(x - 37, y + 4);
      ctx.stroke();

      // Haz de luz calido hacia el mar
      if (!isGrey) {
        const beam = ctx.createLinearGradient(x - 9, y - 33, x + 28, y - 20);
        beam.addColorStop(0, 'rgba(255, 240, 150, 0.42)');
        beam.addColorStop(1, 'rgba(255, 240, 150, 0)');
        ctx.fillStyle = beam;
        ctx.beginPath();
        ctx.moveTo(x - 9, y - 33);
        ctx.lineTo(x + 28, y - 42);
        ctx.lineTo(x + 28, y - 16);
        ctx.closePath();
        ctx.fill();
      }

      // Faro cilindrico estilizado
      const gradFaro = ctx.createLinearGradient(x - 16, y - 34, x - 2, y + 2);
      gradFaro.addColorStop(0, col('#FFFDF8'));
      gradFaro.addColorStop(1, col('#E4D2B4'));
      ctx.fillStyle = gradFaro;
      ctx.beginPath();
      ctx.moveTo(x - 15, y + 3);
      ctx.lineTo(x - 12, y - 26);
      ctx.quadraticCurveTo(x - 9, y - 28, x - 6, y - 26);
      ctx.lineTo(x - 3, y + 3);
      ctx.closePath();
      ctx.fill();

      ctx.fillStyle = col('#D97757');
      ctx.beginPath();
      ctx.roundRect(x - 14.5, y - 16, 11, 5, 2.5);
      ctx.roundRect(x - 14, y - 29, 10, 4.5, 2);
      ctx.fill();

      if (!isGrey) {
        ctx.fillStyle = 'rgba(255, 232, 130, 0.36)';
        ctx.beginPath();
        ctx.arc(x - 9, y - 33, 9, 0, Math.PI * 2);
        ctx.fill();
      }
      ctx.fillStyle = col('#FFF2AA');
      ctx.beginPath();
      ctx.arc(x - 9, y - 33, 4.2, 0, Math.PI * 2);
      ctx.fill();

      // Dos casitas curvas mediterraneas
      ctx.fillStyle = col('#F7EBD5');
      ctx.beginPath();
      ctx.roundRect(x + 2, y - 9, 15, 11, 4);
      ctx.roundRect(x + 19, y - 5, 13, 9, 3);
      ctx.fill();
      ctx.fillStyle = col('#3574B5');
      ctx.beginPath();
      ctx.arc(x + 9.5, y - 9, 6, Math.PI, 0);
      ctx.fill();
      ctx.fillStyle = col('#D97757');
      ctx.beginPath();
      ctx.moveTo(x + 17.5, y - 5); ctx.lineTo(x + 25.5, y - 11); ctx.lineTo(x + 33.5, y - 5);
      ctx.closePath(); ctx.fill();
    } else if (bid === 'kioto') {
      // Bosque de cerezos, pagoda de doble tejado y Torii
      [[-24, -4, 9, '#F6B6C9'], [-14, -10, 8, '#FAD0DC'], [21, -5, 9.5, '#F6B6C9'], [29, 1, 7, '#FAD0DC']].forEach(([dx, dy, r, h]) => {
        ctx.fillStyle = col(h);
        ctx.beginPath();
        ctx.arc(x + dx, y + dy, r, 0, Math.PI * 2);
        ctx.fill();
      });

      // Pagoda curva en segundo plano
      ctx.fillStyle = col('#FAF0E4');
      ctx.beginPath();
      ctx.roundRect(x + 5, y - 16, 12, 17, 2.5);
      ctx.fill();
      ctx.fillStyle = col('#C95240');
      ctx.beginPath();
      ctx.moveTo(x + 2, y - 8); ctx.quadraticCurveTo(x + 11, y - 15, x + 20, y - 8); ctx.closePath(); ctx.fill();
      ctx.beginPath();
      ctx.moveTo(x + 4, y - 16); ctx.quadraticCurveTo(x + 11, y - 22, x + 18, y - 16); ctx.closePath(); ctx.fill();

      // Arco Torii protagonista
      ctx.fillStyle = col('#C95240');
      ctx.beginPath();
      ctx.roundRect(x - 16, y - 12, 3.5, 16, 1.5);
      ctx.roundRect(x - 2, y - 12, 3.5, 16, 1.5);
      ctx.fill();
      ctx.beginPath();
      ctx.moveTo(x - 21, y - 14);
      ctx.quadraticCurveTo(x - 7, y - 10, x + 5, y - 14);
      ctx.lineTo(x + 4, y - 10);
      ctx.quadraticCurveTo(x - 7, y - 6.5, x - 20, y - 10);
      ctx.closePath();
      ctx.fill();
    } else if (bid === 'valle') {
      // Chalet alpino + grupo de abetos suaves
      [[-26, 2, 16, '#346840'], [-17, 0, 21, '#3E7347'], [16, 1, 19, '#488252'], [26, 4, 15, '#356941']].forEach(([dx, dy, h, cHex]) => {
        ctx.fillStyle = col(cHex);
        ctx.beginPath();
        ctx.moveTo(x + dx, y + dy - h);
        ctx.quadraticCurveTo(x + dx + 8, y + dy - 2, x + dx + 6.5, y + dy + 3);
        ctx.lineTo(x + dx - 6.5, y + dy + 3);
        ctx.quadraticCurveTo(x + dx - 8, y + dy - 2, x + dx, y + dy - h);
        ctx.closePath();
        ctx.fill();
      });
      ctx.fillStyle = col('#FAF0DC');
      ctx.beginPath();
      ctx.roundRect(x - 9, y - 9, 18, 12, 3);
      ctx.fill();
      ctx.fillStyle = col('#C46747');
      ctx.beginPath();
      ctx.moveTo(x - 12, y - 8);
      ctx.quadraticCurveTo(x, y - 20, x + 12, y - 8);
      ctx.closePath();
      ctx.fill();
    } else if (bid === 'fiordo') {
      // Dos cabanas nordicas + embarcadero y barco vikingo
      ctx.fillStyle = col('#B84438');
      ctx.beginPath();
      ctx.roundRect(x - 19, y - 9, 16, 11, 3);
      ctx.roundRect(x - 1, y - 5, 12, 9, 2.5);
      ctx.fill();
      ctx.fillStyle = col('#F2F7FC');
      ctx.beginPath();
      ctx.moveTo(x - 22, y - 8); ctx.lineTo(x - 11, y - 18); ctx.lineTo(x, y - 8); ctx.closePath();
      ctx.fill();
      ctx.beginPath();
      ctx.moveTo(x - 3, y - 4); ctx.lineTo(x + 5, y - 12); ctx.lineTo(x + 13, y - 4); ctx.closePath();
      ctx.fill();

      // Barco en fiordo con estela
      ctx.strokeStyle = 'rgba(255,255,255,0.45)';
      ctx.lineWidth = 1.4;
      ctx.beginPath();
      ctx.moveTo(x + 11, y + 7); ctx.quadraticCurveTo(x + 18, y + 10, x + 28, y + 8);
      ctx.stroke();
      ctx.fillStyle = col('#704C34');
      ctx.beginPath();
      ctx.moveTo(x + 14, y + 4);
      ctx.quadraticCurveTo(x + 24, y + 9, x + 33, y + 4);
      ctx.closePath();
      ctx.fill();
      ctx.fillStyle = col('#D65B4B');
      ctx.beginPath();
      ctx.moveTo(x + 23, y + 3);
      ctx.quadraticCurveTo(x + 31, y - 6, x + 23, y - 14);
      ctx.closePath();
      ctx.fill();
    } else if (bid === 'desierto') {
      // Cupula de adobe + arco de arenisca + palmeras curvas en oasis
      ctx.fillStyle = col('#DFB16D');
      ctx.beginPath();
      ctx.roundRect(x - 10, y - 10, 20, 12, 4);
      ctx.fill();
      ctx.fillStyle = col('#FAF0DA');
      ctx.beginPath();
      ctx.arc(x, y - 10, 8, Math.PI, 0);
      ctx.fill();

      [[-20, 4, -17, -16], [19, 5, 22, -14], [28, 2, 31, -11]].forEach(([bx, by, tx, ty]) => {
        ctx.strokeStyle = col('#8B6342');
        ctx.lineWidth = 2.4;
        ctx.lineCap = 'round';
        ctx.beginPath();
        ctx.moveTo(x + bx, y + by);
        ctx.quadraticCurveTo(x + bx - 2, y - 7, x + tx, y + ty);
        ctx.stroke();
        ctx.fillStyle = col('#558E52');
        ctx.beginPath();
        ctx.arc(x + tx, y + ty - 1, 5.5, 0, Math.PI * 2);
        ctx.fill();
      });
    } else if (bid === 'toscana') {
      // Villa toscana + avenida de cipreses esbeltos + fuente redonda
      ctx.fillStyle = col('#F7E8CC');
      ctx.beginPath();
      ctx.roundRect(x - 11, y - 11, 22, 14, 4);
      ctx.fill();
      ctx.fillStyle = col('#C86848');
      ctx.beginPath();
      ctx.moveTo(x - 14, y - 10);
      ctx.quadraticCurveTo(x, y - 20, x + 14, y - 10);
      ctx.closePath();
      ctx.fill();
      ctx.fillStyle = col('#4D6D3B');
      [[-24, -4, 11], [-18, -9, 13], [18, -8, 13], [25, -3, 10]].forEach(([dx, dy, ry]) => {
        ctx.beginPath();
        ctx.ellipse(x + dx, y + dy, 3.8, ry, 0, 0, Math.PI * 2);
        ctx.fill();
      });
      ctx.fillStyle = col('#74C6D8');
      ctx.beginPath();
      ctx.ellipse(x + 2, y + 6, 7, 3.5, 0, 0, Math.PI * 2);
      ctx.fill();
    } else if (bid === 'jungla') {
      // Dosel frondoso multicapa + templo de piedra + cascada
      [[-24, 1, 9, '#1F5432'], [-15, -5, 11, '#2B6C42'], [15, -4, 11, '#26613A'], [25, 2, 8.5, '#367C4D']].forEach(([dx, dy, r, h]) => {
        ctx.fillStyle = col(h);
        ctx.beginPath();
        ctx.arc(x + dx, y + dy, r, 0, Math.PI * 2);
        ctx.fill();
      });
      ctx.fillStyle = col('#7D8F82');
      ctx.beginPath();
      ctx.roundRect(x - 12, y - 8, 24, 12, 4);
      ctx.roundRect(x - 8, y - 16, 16, 8, 3);
      ctx.roundRect(x - 4.5, y - 22, 9, 6, 2.5);
      ctx.fill();
    } else if (bid === 'arrozales') {
      // Pabellon balines elegante + tallos de bambu
      ctx.strokeStyle = col('#4B8C4F');
      ctx.lineWidth = 2.2;
      ctx.lineCap = 'round';
      [[-19, 3, -21, -14], [-15, 4, -15, -12], [17, 4, 19, -13]].forEach(([x0, y0, x1, y1]) => {
        ctx.beginPath(); ctx.moveTo(x + x0, y + y0); ctx.lineTo(x + x1, y + y1); ctx.stroke();
      });
      ctx.fillStyle = col('#D6BA8D');
      ctx.beginPath();
      ctx.roundRect(x - 9, y - 7, 18, 10, 3);
      ctx.fill();
      ctx.fillStyle = col('#3E3A36');
      ctx.beginPath();
      ctx.moveTo(x - 13, y - 7);
      ctx.quadraticCurveTo(x, y - 17, x + 13, y - 7);
      ctx.closePath();
      ctx.fill();
      ctx.beginPath();
      ctx.moveTo(x - 9, y - 14);
      ctx.quadraticCurveTo(x, y - 23, x + 9, y - 14);
      ctx.closePath();
      ctx.fill();
    } else if (bid === 'otono') {
      // Tapiz de arces redondeados + cabana con humo suave
      [[-24, -1, 8, '#C83B2B'], [-15, -7, 9.5, '#EC7628'], [16, -6, 9, '#F3B438'], [25, 0, 7.5, '#D84830']].forEach(([dx, dy, r, h]) => {
        ctx.fillStyle = col(h);
        ctx.beginPath();
        ctx.arc(x + dx, y + dy, r, 0, Math.PI * 2);
        ctx.fill();
      });
      ctx.fillStyle = col('#8B593B');
      ctx.beginPath();
      ctx.roundRect(x - 9, y - 9, 18, 12, 3);
      ctx.fill();
      ctx.fillStyle = col('#C4492D');
      ctx.beginPath();
      ctx.moveTo(x - 12, y - 8);
      ctx.quadraticCurveTo(x, y - 18, x + 12, y - 8);
      ctx.closePath();
      ctx.fill();
      ctx.strokeStyle = isDark ? 'rgba(255,255,255,0.30)' : 'rgba(255,255,255,0.75)';
      ctx.lineWidth = 1.8;
      ctx.beginPath();
      ctx.moveTo(x + 5, y - 16);
      ctx.quadraticCurveTo(x + 8, y - 21, x + 5, y - 26);
      ctx.stroke();
    } else {
      // Atolon: pasarela curva con palafitos + palmeras
      ctx.strokeStyle = col('#9B764F');
      ctx.lineWidth = 2.6;
      ctx.beginPath();
      ctx.moveTo(x - 18, y + 3);
      ctx.quadraticCurveTo(x, y + 1, x + 18, y + 4);
      ctx.stroke();

      [[-14, 0], [4, -4]].forEach(([dx, dy]) => {
        ctx.fillStyle = col('#FAF0DC');
        ctx.beginPath();
        ctx.roundRect(x + dx - 7, y + dy - 7, 14, 9, 3);
        ctx.fill();
        ctx.fillStyle = col('#D98752');
        ctx.beginPath();
        ctx.moveTo(x + dx - 10, y + dy - 6);
        ctx.quadraticCurveTo(x + dx, y + dy - 14, x + dx + 10, y + dy - 6);
        ctx.closePath();
        ctx.fill();
      });
      ctx.fillStyle = col('#529655');
      ctx.beginPath();
      ctx.arc(x + 22, y - 10, 6.5, 0, Math.PI * 2);
      ctx.arc(x - 24, y - 8, 5.5, 0, Math.PI * 2);
      ctx.fill();
    }
    ctx.restore();
  }

  function renderSmoothReliefMap(ctx, opts, isFortniteHybrid = false) {
    const showAllColor = opts.showAllColor !== false;
    const unlockedMap = opts.unlockedMap || {};
    const activeBiomeId = opts.activeBiomeId || 'valle';
    const selectedBiomeId = opts.selectedBiomeId || null;
    const isDark = Boolean(opts.isDark);

    // Actualizar anclas suaves para chips y hit-testing
    ZONES.forEach(z => {
      z.cx = z.smoothCx;
      z.cy = z.smoothCy;
    });

    const isGreyBiome = bid => !showAllColor && !unlockedMap[bid];
    const colOf = (bid, hex) => (isGreyBiome(bid) ? toLockedGrey(hex, isDark) : hex);

    // 1. Fondo marino: cuadricula tactica Fortnite o ondas suaves
    if (isFortniteHybrid) {
      ctx.save();
      ctx.strokeStyle = isDark ? 'rgba(130, 196, 224, 0.09)' : 'rgba(255, 255, 255, 0.14)';
      ctx.lineWidth = 1;
      for (let x = 48; x < MAP_WIDTH; x += 64) {
        ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, MAP_HEIGHT); ctx.stroke();
      }
      for (let y = 48; y < MAP_HEIGHT; y += 64) {
        ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(MAP_WIDTH, y); ctx.stroke();
      }
      ctx.font = '700 8.5px "Plus Jakarta Sans", sans-serif';
      ctx.fillStyle = isDark ? 'rgba(170, 215, 236, 0.28)' : 'rgba(255, 255, 255, 0.36)';
      ['A', 'B', 'C', 'D'].forEach((lbl, idx) => {
        ctx.fillText(lbl + '1', 80 + idx * 224, 22);
      });
      ctx.restore();
    } else {
      ctx.save();
      ctx.strokeStyle = isDark ? 'rgba(155, 215, 238, 0.14)' : 'rgba(255, 255, 255, 0.34)';
      ctx.lineWidth = 2;
      ctx.lineCap = 'round';
      [[54, 82], [818, 72], [58, 596], [790, 610]].forEach(([wx, wy]) => {
        ctx.beginPath();
        ctx.moveTo(wx, wy);
        ctx.quadraticCurveTo(wx + 20, wy - 8, wx + 40, wy);
        ctx.quadraticCurveTo(wx + 60, wy - 8, wx + 80, wy);
        ctx.stroke();
      });
      ctx.restore();
    }

    // 2. Tres ondas concentricas suaves abrazando la costa organica
    const rippleSpecs = isFortniteHybrid
      ? [
          [1.068, isDark ? 'rgba(78, 162, 196, 0.18)' : 'rgba(182, 236, 252, 0.24)'],
          [1.036, isDark ? 'rgba(110, 194, 224, 0.24)' : 'rgba(220, 248, 255, 0.40)'],
          [1.014, isDark ? 'rgba(156, 224, 244, 0.30)' : 'rgba(255, 255, 255, 0.64)']
        ]
      : [
          [1.078, isDark ? 'rgba(84, 160, 188, 0.14)' : 'rgba(218, 246, 254, 0.24)'],
          [1.046, isDark ? 'rgba(108, 186, 212, 0.20)' : 'rgba(235, 251, 255, 0.38)'],
          [1.018, isDark ? 'rgba(142, 212, 234, 0.28)' : 'rgba(255, 255, 255, 0.56)']
        ];
    rippleSpecs.forEach(([sc, fillCol]) => {
      ctx.save();
      traceSmoothClosedPoly(ctx, getSmoothContinentPts(sc, 0, 8));
      ctx.fillStyle = fillCol;
      ctx.fill();
      ctx.restore();
    });

    // 3. Zocalo y sombra 3D esculpida del continente continuo
    const topPoly = getSmoothContinentPts(1.0, 0, 0);
    const slabLayers = isFortniteHybrid
      ? [
          { dy: 28, scale: 1.012, fill: isDark ? 'rgba(0,0,0,0.38)' : 'rgba(10, 38, 54, 0.26)' },
          { dy: 20, scale: 1.003, fill: isDark ? '#1D262B' : '#566973' },
          { dy: 12, scale: 1.001, fill: isDark ? '#2B373E' : '#738A94' },
          { dy: 5,  scale: 1.000, fill: isDark ? '#3B4850' : '#AFC1C8' }
        ]
      : [
          { dy: 28, scale: 1.012, fill: isDark ? 'rgba(0,0,0,0.34)' : 'rgba(10, 44, 58, 0.22)' },
          { dy: 20, scale: 1.003, fill: isDark ? '#21272A' : '#967B5E' },
          { dy: 12, scale: 1.001, fill: isDark ? '#2E3639' : '#B79A75' },
          { dy: 5,  scale: 1.000, fill: isDark ? '#3D4649' : '#D9C298' }
        ];
    slabLayers.forEach(sl => {
      ctx.save();
      traceSmoothClosedPoly(ctx, getSmoothContinentPts(sl.scale, sl.dy > 24 ? 5 : 0, sl.dy));
      ctx.fillStyle = sl.fill;
      ctx.fill();
      ctx.restore();
    });

    // 4. Relleno base del continente recortado + 10 regiones continuas suaves
    const SMOOTH_PAL = {
      valle:     ['#76B56E', '#589C5B'],
      fiordo:    ['#DCEAF2', '#B7CFDF'],
      desierto:  ['#ECC68A', '#D6A45E'],
      toscana:   ['#DFCD8E', '#C4AE6A'],
      jungla:    ['#4D935E', '#2F6F45'],
      kioto:     ['#9EC388', '#F2CBD7'],
      arrozales: ['#74C28E', '#53AC96'],
      otono:     ['#E39C62', '#C96D45'],
      costa:     ['#EFE0BE', '#DAC090'],
      atolon:    ['#92DFD8', '#68CAC3']
    };

    ctx.save();
    traceSmoothClosedPoly(ctx, topPoly);
    ctx.clip();

    ctx.fillStyle = isDark ? '#233027' : '#A9CF92';
    ctx.fillRect(0, 0, MAP_WIDTH, MAP_HEIGHT);

    ZONES.forEach((z, idx) => {
      const [c1, c2] = SMOOTH_PAL[z.id];
      const grey = isGreyBiome(z.id);
      const colA = grey ? toLockedGrey(c1, isDark) : (isDark ? mixHex(c1, '#121A17', 0.45) : c1);
      const colB = grey ? toLockedGrey(c2, isDark) : (isDark ? mixHex(c2, '#101815', 0.50) : c2);
      const blobPts = buildOrganicMoundPts(z.smoothCx, z.smoothCy, 195, 138, (idx % 2 ? 0.2 : -0.2), 11 + idx * 7, 0.14);
      const rg = ctx.createRadialGradient(z.smoothCx - 20, z.smoothCy - 18, 12, z.smoothCx, z.smoothCy, 195);
      rg.addColorStop(0, colA);
      rg.addColorStop(0.68, colB);
      rg.addColorStop(1, 'rgba(0,0,0,0)');
      traceSmoothClosedPoly(ctx, blobPts);
      ctx.fillStyle = rg;
      ctx.fill();
    });

    // Terrazas de relieve 3D curvo para cada bioma
    drawSmooth3DTerrace(ctx, 215, 185, 118, 72, -0.18, 14, '#6CAE67', isGreyBiome('valle'), isDark, 17, 0.09);
    drawSmooth3DTerrace(ctx, 185, 165, 76, 48, -0.22, 13, '#4B8854', isGreyBiome('valle'), isDark, 29, 0.10);
    ctx.save();
    ctx.fillStyle = colOf('valle', '#6CC6DF');
    ctx.beginPath();
    ctx.ellipse(275, 206, 26, 13, -0.15, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = isDark ? 'rgba(255,255,255,0.22)' : 'rgba(255,255,255,0.65)';
    ctx.lineWidth = 1.5;
    ctx.stroke();
    ctx.restore();
    drawSmoothPeak(ctx, 165, 154, 72, 46, 0.44, '#5E7C68', '#F8FAFC', isGreyBiome('valle'), isDark);
    drawSmoothPeak(ctx, 210, 144, 58, 38, 0.46, '#587562', '#FFFFFF', isGreyBiome('valle'), isDark);

    drawSmooth3DTerrace(ctx, 452, 148, 120, 62, 0.05, 15, '#BBD2E2', isGreyBiome('fiordo'), isDark, 43, 0.09);
    drawSmoothPeak(ctx, 398, 136, 68, 46, 0.62, '#6E8EA4', '#F3F8FC', isGreyBiome('fiordo'), isDark);
    drawSmoothPeak(ctx, 504, 134, 72, 48, 0.64, '#6E8EA4', '#EBF3FA', isGreyBiome('fiordo'), isDark);
    [[454, 102, 11, 5], [472, 112, 8, 4]].forEach(([fx, fy, rx, ry]) => {
      ctx.fillStyle = colOf('fiordo', '#EDF6FC');
      ctx.beginPath(); ctx.ellipse(fx, fy, rx, ry, 0.2, 0, Math.PI * 2); ctx.fill();
    });

    drawSmooth3DTerrace(ctx, 640, 176, 116, 68, 0.15, 13, '#E8C284', isGreyBiome('desierto'), isDark, 57, 0.08);
    drawSmooth3DTerrace(ctx, 620, 156, 66, 36, -0.24, 11, '#E0AD65', isGreyBiome('desierto'), isDark, 63, 0.11);
    ctx.save();
    ctx.strokeStyle = colOf('desierto', '#F6DEB0');
    ctx.lineWidth = 2.4;
    ctx.lineCap = 'round';
    ctx.beginPath(); ctx.moveTo(575, 168); ctx.quadraticCurveTo(615, 144, 660, 162); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(598, 198); ctx.quadraticCurveTo(638, 180, 682, 198); ctx.stroke();
    ctx.fillStyle = colOf('desierto', '#4EB8B6');
    ctx.beginPath();
    ctx.ellipse(672, 184, 24, 11, 0.1, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = colOf('desierto', '#FAF0D6');
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.restore();

    drawSmooth3DTerrace(ctx, 766, 250, 108, 66, 0.25, 12, '#C8B86E', isGreyBiome('toscana'), isDark, 71, 0.08);
    drawSmooth3DTerrace(ctx, 756, 232, 64, 38, -0.1, 10, '#8FA65C', isGreyBiome('toscana'), isDark, 79, 0.09);
    ctx.save();
    ctx.strokeStyle = colOf('toscana', '#6E8A44');
    ctx.lineWidth = 2;
    ctx.lineCap = 'round';
    [-10, -2, 6].forEach(dy => {
      ctx.beginPath();
      ctx.moveTo(718, 256 + dy);
      ctx.quadraticCurveTo(758, 244 + dy, 804, 260 + dy);
      ctx.stroke();
    });
    ctx.restore();

    drawSmooth3DTerrace(ctx, 198, 336, 116, 76, 0.12, 15, '#3E8251', isGreyBiome('jungla'), isDark, 83, 0.10);
    drawSmooth3DTerrace(ctx, 182, 314, 72, 46, -0.15, 12, '#225937', isGreyBiome('jungla'), isDark, 97, 0.10);
    ctx.save();
    ctx.fillStyle = colOf('jungla', '#90E4F2');
    ctx.beginPath();
    ctx.roundRect(216, 304, 12, 28, 6);
    ctx.fill();
    ctx.fillStyle = colOf('jungla', '#E8FCFF');
    ctx.beginPath();
    ctx.ellipse(222, 332, 13, 5, 0, 0, Math.PI * 2);
    ctx.fill();
    ctx.restore();

    drawSmooth3DTerrace(ctx, 376, 312, 108, 68, -0.08, 12, '#7FAE78', isGreyBiome('kioto'), isDark, 101, 0.08);
    drawSmooth3DTerrace(ctx, 368, 294, 66, 40, 0.18, 9, '#F3CBD7', isGreyBiome('kioto'), isDark, 109, 0.09);
    ctx.save();
    ctx.fillStyle = colOf('kioto', '#6DA6AD');
    ctx.beginPath(); ctx.ellipse(412, 326, 20, 9, -0.12, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = colOf('kioto', '#FFE6EE');
    [[346, 284], [392, 278], [358, 306], [404, 296]].forEach(([px, py]) => {
      ctx.beginPath(); ctx.arc(px, py, 2, 0, Math.PI * 2); ctx.fill();
    });
    ctx.restore();

    drawSmooth3DTerrace(ctx, 592, 332, 118, 72, 0.08, 13, '#58AE68', isGreyBiome('arrozales'), isDark, 113, 0.07);
    drawSmooth3DTerrace(ctx, 590, 322, 92, 54, 0.08, 10, '#59C2CF', isGreyBiome('arrozales'), isDark, 127, 0.06);
    drawSmooth3DTerrace(ctx, 589, 312, 66, 38, 0.08, 8, '#74C776', isGreyBiome('arrozales'), isDark, 131, 0.06);
    drawSmooth3DTerrace(ctx, 588, 302, 40, 22, 0.08, 7, '#64CBD7', isGreyBiome('arrozales'), isDark, 137, 0.05);

    drawSmooth3DTerrace(ctx, 764, 390, 108, 68, -0.22, 13, '#DC7B46', isGreyBiome('otono'), isDark, 139, 0.09);
    drawSmooth3DTerrace(ctx, 754, 372, 66, 40, 0.12, 9, '#EB9F45', isGreyBiome('otono'), isDark, 149, 0.08);
    ctx.save();
    [[724, 392, '#D84030'], [796, 378, '#F6C03C'], [742, 412, '#EB7C2A']].forEach(([lx, ly, lc]) => {
      ctx.fillStyle = colOf('otono', lc);
      ctx.beginPath(); ctx.ellipse(lx, ly, 4.5, 2.5, 0.5, 0, Math.PI * 2); ctx.fill();
    });
    ctx.restore();

    drawSmooth3DTerrace(ctx, 338, 480, 126, 70, 0.12, 13, '#EAD5AD', isGreyBiome('costa'), isDark, 157, 0.08);
    drawSmooth3DTerrace(ctx, 324, 464, 80, 44, -0.10, 10, '#DFBF8D', isGreyBiome('costa'), isDark, 167, 0.08);
    ctx.save();
    ctx.fillStyle = colOf('costa', '#9E774F');
    ctx.beginPath(); ctx.roundRect(386, 496, 28, 7, 3.5); ctx.fill();
    [[378, 526], [422, 514]].forEach(([bx, by]) => {
      ctx.fillStyle = colOf('costa', '#F8F4EC');
      ctx.beginPath(); ctx.ellipse(bx, by, 7, 3, 0, 0, Math.PI * 2); ctx.fill();
      ctx.fillStyle = '#FFFFFF';
      ctx.beginPath(); ctx.moveTo(bx, by - 1); ctx.lineTo(bx + 1, by - 12); ctx.lineTo(bx + 7, by - 2); ctx.closePath(); ctx.fill();
    });
    ctx.restore();

    drawSmooth3DTerrace(ctx, 634, 482, 118, 64, -0.14, 9, '#EFE0BE', isGreyBiome('atolon'), isDark, 179, 0.08);
    ctx.save();
    ctx.fillStyle = colOf('atolon', '#4CC7C4');
    ctx.beginPath();
    ctx.ellipse(638, 488, 54, 24, -0.1, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = colOf('atolon', '#FAF4E4');
    ctx.lineWidth = 2.2;
    ctx.stroke();
    ctx.fillStyle = colOf('atolon', '#EE8B84');
    ctx.beginPath();
    ctx.ellipse(618, 494, 8, 3.5, 0.2, 0, Math.PI * 2);
    ctx.ellipse(664, 482, 7, 3, -0.3, 0, Math.PI * 2);
    ctx.fill();
    ctx.restore();

    // Curvas de cota blancas tacticas sobre el relieve curvo en Fortnite Suave
    if (isFortniteHybrid) {
      ctx.save();
      ctx.strokeStyle = isDark ? 'rgba(255, 255, 255, 0.16)' : 'rgba(255, 255, 255, 0.34)';
      ctx.lineWidth = 1.25;
      ZONES.forEach((z, idx) => {
        const contourPts = buildOrganicMoundPts(z.smoothCx, z.smoothCy - 8, 102, 62, (idx % 2 ? 0.15 : -0.15), 41 + idx * 5, 0.09);
        traceSmoothClosedPoly(ctx, contourPts);
        ctx.stroke();
      });
      ctx.restore();
    }

    // 5. Rio central curvo tallado con profundidad + afluente cascada de Jungla
    const riverColor = isDark ? '#2B7B93' : '#53BBD2';
    const bankColor = isDark ? '#19353F' : '#35798C';
    ctx.save();
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';

    const drawMeanderRiver = (wWater, drawWaterOnly) => {
      ctx.beginPath();
      ctx.moveTo(468, 92);
      ctx.bezierCurveTo(445, 195, 502, 268, 468, 368);
      ctx.bezierCurveTo(440, 452, 500, 520, 496, 598);
      if (!drawWaterOnly) {
        ctx.save();
        ctx.translate(0, -4);
        ctx.strokeStyle = bankColor;
        ctx.lineWidth = wWater + 7;
        ctx.stroke();
        ctx.restore();
      }
      ctx.strokeStyle = riverColor;
      ctx.lineWidth = wWater;
      ctx.stroke();

      ctx.beginPath();
      ctx.moveTo(222, 328);
      ctx.quadraticCurveTo(342, 314, 468, 344);
      if (!drawWaterOnly) {
        ctx.save();
        ctx.translate(0, -3);
        ctx.strokeStyle = bankColor;
        ctx.lineWidth = 15;
        ctx.stroke();
        ctx.restore();
      }
      ctx.strokeStyle = riverColor;
      ctx.lineWidth = 10;
      ctx.stroke();
    };
    drawMeanderRiver(21, false);

    ctx.strokeStyle = 'rgba(255, 255, 255, 0.42)';
    ctx.lineWidth = 2.4;
    ctx.setLineDash([12, 16]);
    ctx.beginPath();
    ctx.moveTo(468, 108);
    ctx.bezierCurveTo(445, 195, 502, 268, 468, 368);
    ctx.bezierCurveTo(440, 452, 500, 520, 496, 582);
    ctx.stroke();
    ctx.setLineDash([]);
    ctx.restore();

    // Resaltar bioma equipado y bioma seleccionado en el borde de su meseta
    [selectedBiomeId, activeBiomeId].forEach((bid, idx) => {
      if (!bid) return;
      const z = getZone(bid);
      ctx.save();
      ctx.strokeStyle = idx === 1 ? '#218A54' : '#FFFFFF';
      ctx.lineWidth = idx === 1 ? 3.2 : 2.2;
      ctx.setLineDash(idx === 1 ? [] : [5, 5]);
      ctx.beginPath();
      ctx.ellipse(z.smoothCx, z.smoothCy - 8, 86, 52, 0, 0, Math.PI * 2);
      ctx.stroke();
      ctx.restore();
    });

    ctx.restore(); // Fin clip del continente

    // Perfil iluminado superior sobre el acantilado costero
    ctx.save();
    traceSmoothClosedPoly(ctx, topPoly);
    ctx.strokeStyle = isFortniteHybrid
      ? (isDark ? 'rgba(255, 255, 255, 0.28)' : 'rgba(255, 255, 255, 0.72)')
      : (isDark ? 'rgba(255, 255, 255, 0.16)' : 'rgba(255, 255, 255, 0.56)');
    ctx.lineWidth = isFortniteHybrid ? 2.2 : 2.0;
    ctx.stroke();
    ctx.restore();

    // Poblar hitGrid por region organica dentro del continente
    const hitMidX = 482;
    const hitMidY = 336;
    for (let iy = 0; iy < HIT_H; iy++) {
      const py = iy * HIT_STEP + HIT_STEP * 0.5;
      for (let ix = 0; ix < HIT_W; ix++) {
        const px = ix * HIT_STEP + HIT_STEP * 0.5;
        const dx = (px - hitMidX) / 398;
        const dy = (py - hitMidY) / 248;
        const t = Math.atan2(dy, dx);
        const wave = 0.95 + 0.055 * Math.cos(t * 2 - 0.35) + 0.042 * Math.sin(t * 3 + 0.75) + 0.025 * Math.cos(t * 5 - 1.0);
        const fjordNotch = Math.sin(t) < -0.45 ? 0.18 * Math.exp(-Math.pow((Math.cos(t) + 0.04) / 0.16, 2)) : 0;
        const southBay = Math.sin(t) > 0.45 ? 0.16 * Math.exp(-Math.pow((Math.cos(t) - 0.05) / 0.22, 2)) : 0;
        if (Math.hypot(dx, dy) <= wave - fjordNotch - southBay + 0.02) {
          let bestIdx = 0;
          let bestD = Infinity;
          for (let k = 0; k < ZONES.length; k++) {
            const d = Math.hypot(px - ZONES[k].smoothCx, (py - ZONES[k].smoothCy) * 1.15);
            if (d < bestD) { bestD = d; bestIdx = k; }
          }
          hitGrid[iy * HIT_W + ix] = bestIdx + 1;
        }
      }
    }

    // 6. Hitos miniatura suaves y curvos sobre cada meseta (ordenados norte -> sur)
    const sortedZones = [...ZONES].sort((a, b) => a.smoothCy - b.smoothCy);
    sortedZones.forEach(z => {
      drawSmoothLandmark(ctx, z.id, z.smoothCx, z.smoothCy - 18, isGreyBiome(z.id), isDark);
    });

    // 7. Senderos tacticos + puentes redondeados sobre cruces del rio
    ctx.save();
    ROADS.forEach(([idA, idB, hasBridge]) => {
      const a = ZONE_MAP[idA];
      const b = ZONE_MAP[idB];
      if (!a || !b) return;
      const unlockedBoth = showAllColor || (unlockedMap[idA] && unlockedMap[idB]);
      const mx = (a.smoothCx + b.smoothCx) * 0.5;
      const my = (a.smoothCy + b.smoothCy) * 0.5 - 10;
      ctx.strokeStyle = !unlockedBoth
        ? 'rgba(130, 140, 148, 0.36)'
        : isFortniteHybrid
          ? 'rgba(255, 255, 255, 0.84)'
          : isDark
            ? 'rgba(255, 255, 255, 0.42)'
            : 'rgba(255, 252, 244, 0.72)';
      ctx.lineWidth = isFortniteHybrid ? 2.3 : 2.2;
      ctx.setLineDash(isFortniteHybrid ? [7, 6] : [5, 6]);
      ctx.beginPath();
      ctx.moveTo(a.smoothCx, a.smoothCy);
      ctx.quadraticCurveTo(mx, my, b.smoothCx, b.smoothCy);
      ctx.stroke();

      if (hasBridge) {
        const bx = mx;
        const by = (a.smoothCy + b.smoothCy) * 0.5 - 5;
        const ang = Math.atan2(b.smoothCy - a.smoothCy, b.smoothCx - a.smoothCx);
        ctx.save();
        ctx.setLineDash([]);
        ctx.translate(bx, by);
        ctx.rotate(ang);
        ctx.fillStyle = isDark ? '#7E6850' : '#D5BF96';
        ctx.strokeStyle = isDark ? '#2B221A' : '#7C5D40';
        ctx.lineWidth = 1.4;
        ctx.beginPath();
        ctx.roundRect(-14, -5, 28, 10, 5);
        ctx.fill();
        ctx.stroke();
        ctx.restore();
      }
    });
    ctx.restore();

    // Nubes volumetricas esponjosas con sombra proyectada estilo Fortnite
    if (isFortniteHybrid) {
      const clouds = [
        [102, 92, 1.0],
        [846, 122, 0.94],
        [118, 534, 1.08],
        [832, 488, 0.90]
      ];
      ctx.save();
      clouds.forEach(([cx, cy, s]) => {
        ctx.fillStyle = 'rgba(10, 24, 34, 0.16)';
        ctx.beginPath();
        ctx.ellipse(cx + 8, cy + 20, 34 * s, 12 * s, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = isDark ? 'rgba(220, 232, 244, 0.36)' : 'rgba(255, 255, 255, 0.76)';
        ctx.beginPath();
        ctx.ellipse(cx, cy, 32 * s, 13 * s, 0, 0, Math.PI * 2);
        ctx.ellipse(cx + 20 * s, cy - 5 * s, 21 * s, 11 * s, 0, 0, Math.PI * 2);
        ctx.ellipse(cx - 18 * s, cy + 2 * s, 18 * s, 9 * s, 0, 0, Math.PI * 2);
        ctx.fill();
      });
      ctx.restore();
    }

    // 8. Etiquetas POI Fortnite o pildoras suaves de Relieve Suave
    ZONES.forEach(z => {
      const isUser = z.id === activeBiomeId;
      const isSel = z.id === selectedBiomeId;
      const grey = isGreyBiome(z.id);
      const lx = z.smoothCx;
      const ly = isFortniteHybrid ? z.smoothCy + 20 : z.smoothCy + 18;

      ctx.save();
      if (isFortniteHybrid) {
        ctx.fillStyle = '#FFFFFF';
        ctx.strokeStyle = isUser ? '#218A54' : '#18242C';
        ctx.lineWidth = isUser ? 2.6 : 2.0;
        ctx.beginPath();
        ctx.arc(z.smoothCx, z.smoothCy + 4, 5.4, 0, Math.PI * 2);
        ctx.fill();
        ctx.stroke();

        ctx.font = '800 11.2px "Plus Jakarta Sans", sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.lineWidth = 3.6;
        ctx.strokeStyle = 'rgba(14, 24, 30, 0.82)';
        ctx.strokeText(z.label, lx, ly + 2);
        ctx.fillStyle = grey ? '#CFD4DC' : '#FFFFFF';
        ctx.fillText(z.label, lx, ly + 2);
      } else {
        const pillW = 94;
        const pillH = 21;
        ctx.fillStyle = isDark ? 'rgba(22, 25, 30, 0.88)' : 'rgba(255, 253, 248, 0.90)';
        ctx.strokeStyle = isUser ? '#1F7A4C' : isSel ? '#FFFFFF' : isDark ? 'rgba(255,255,255,0.18)' : 'rgba(40,38,34,0.12)';
        ctx.lineWidth = isUser ? 2.2 : 1.1;
        ctx.shadowColor = isDark ? 'rgba(0,0,0,0.34)' : 'rgba(20,40,34,0.12)';
        ctx.shadowBlur = 8;
        ctx.shadowOffsetY = 2;
        ctx.beginPath();
        ctx.roundRect(lx - pillW * 0.5, ly - pillH * 0.5, pillW, pillH, 10.5);
        ctx.fill();
        ctx.shadowColor = 'transparent';
        ctx.stroke();

        ctx.font = '800 9.4px "Plus Jakarta Sans", sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillStyle = grey ? '#8A9098' : (isDark ? '#F1F2F4' : '#1E2A22');
        ctx.fillText(z.label, lx, ly + 0.5);
      }
      markHitRect(lx - 48, ly - 12, lx + 48, ly + 12, ZONE_INDEX[z.id] + 1);
      ctx.restore();
    });

    // 9. Brujula minimalista limpia en esquina SE
    ctx.save();
    ctx.translate(890, 610);
    ctx.globalAlpha = isDark ? 0.42 : 0.65;
    ctx.fillStyle = isDark ? 'rgba(255,255,255,0.08)' : 'rgba(255,255,255,0.22)';
    ctx.strokeStyle = '#FAF8F3';
    ctx.lineWidth = 1.4;
    ctx.beginPath();
    ctx.arc(0, 0, 20, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();
    ctx.fillStyle = '#FAF8F3';
    ctx.beginPath();
    ctx.moveTo(0, -15); ctx.lineTo(4, -3.5); ctx.lineTo(0, -1); ctx.lineTo(-4, -3.5);
    ctx.closePath(); ctx.fill();
    ctx.fillStyle = 'rgba(250, 248, 243, 0.50)';
    [[0, 15, 4, 3.5, 0, 1, -4, 3.5], [15, 0, 3.5, 4, 1, 0, 3.5, -4], [-15, 0, -3.5, 4, -1, 0, -3.5, -4]].forEach(pts => {
      ctx.beginPath();
      ctx.moveTo(pts[0], pts[1]); ctx.lineTo(pts[2], pts[3]);
      ctx.lineTo(pts[4], pts[5]); ctx.lineTo(pts[6], pts[7]);
      ctx.closePath(); ctx.fill();
    });
    ctx.restore();
  }

  function renderFortniteSmoothMap(ctx, opts) {
    renderSmoothReliefMap(ctx, opts, true);
  }

  function renderMapInto(canvas, opts = {}) {
    if (!canvas) return;
    const style = ['zootropolis', 'fortnite', 'atlas', 'topo'].includes(opts.style) ? opts.style : 'zootropolis';
    const showAllColor = opts.showAllColor !== false;
    const unlockedMap = opts.unlockedMap || {};
    const activeBiomeId = opts.activeBiomeId || 'valle';
    const selectedBiomeId = opts.selectedBiomeId || null;
    const isDark = Boolean(opts.isDark);

    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    if (canvas.width !== Math.round(MAP_WIDTH * dpr) || canvas.height !== Math.round(MAP_HEIGHT * dpr)) {
      canvas.width = Math.round(MAP_WIDTH * dpr);
      canvas.height = Math.round(MAP_HEIGHT * dpr);
    }
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, MAP_WIDTH, MAP_HEIGHT);
    hitGrid.fill(0);

    if (style === 'atlas') {
      renderSmoothReliefMap(ctx, opts, false);
      return;
    }
    if (style === 'topo') {
      renderFortniteSmoothMap(ctx, opts);
      return;
    }

    const isFort = style === 'fortnite';
    const isAtlas = style === 'atlas';
    const isZoo = style === 'zootropolis';

    const elevPx = isZoo ? 14.0 : isFort ? 13.0 : 12.2;
    const cliffHex = isDark
      ? (isAtlas ? '#382C22' : '#262C28')
      : (isAtlas ? '#9E7854' : isFort ? '#5C6C72' : '#584E43');

    // Update projected zone label centers for this style's elevation
    ZONES.forEach(z => {
      const pt = projectIso(z.gxCenter, z.gyCenter, z.wzCenter, elevPx);
      z.cx = Math.round(pt.x);
      z.cy = Math.round(pt.y);
    });

    // 1. Tactical Ocean Grid / Decorative Sea Waves
    if (isFort) {
      ctx.save();
      ctx.strokeStyle = isDark ? 'rgba(130, 196, 224, 0.08)' : 'rgba(255, 255, 255, 0.14)';
      ctx.lineWidth = 1;
      for (let x = 48; x < MAP_WIDTH; x += 64) {
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, MAP_HEIGHT);
        ctx.stroke();
      }
      for (let y = 48; y < MAP_HEIGHT; y += 64) {
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(MAP_WIDTH, y);
        ctx.stroke();
      }
      ctx.restore();
    } else {
      ctx.save();
      ctx.strokeStyle = isDark ? 'rgba(155, 215, 238, 0.16)' : 'rgba(255, 255, 255, 0.30)';
      ctx.lineWidth = 1.8;
      ctx.lineCap = 'round';
      const wavePts = [[52, 82], [820, 74], [58, 594], [818, 608]];
      wavePts.forEach(([wx, wy]) => {
        ctx.beginPath();
        ctx.moveTo(wx, wy);
        ctx.quadraticCurveTo(wx + 18, wy - 7, wx + 36, wy);
        ctx.quadraticCurveTo(wx + 54, wy - 7, wx + 72, wy);
        ctx.stroke();
      });
      ctx.restore();
    }

    // 2. Drop shadow cast by the entire 3D continent onto the ocean
    const cells = TERRAIN.cells;
    ctx.save();
    for (let i = 0; i < cells.length; i++) {
      const c = cells[i];
      if (!c.isCoastalEdge && c.shoreDepth < 0.08) continue;
      if (c.isCoastalEdge) {
        const sp = projectIso(c.gx, c.gy, -0.82, elevPx);
        ctx.fillStyle = isDark ? 'rgba(0, 0, 0, 0.32)' : 'rgba(10, 46, 62, 0.22)';
        ctx.beginPath();
        ctx.ellipse(sp.x + 3, sp.y + 12, 22, 11, 0, 0, Math.PI * 2);
        ctx.fill();

        // Triple concentric ripples for Atlas Gouache
        if (isAtlas) {
          ctx.fillStyle = isDark ? 'rgba(72, 146, 174, 0.14)' : 'rgba(220, 248, 254, 0.28)';
          ctx.beginPath();
          ctx.ellipse(sp.x, sp.y + 5, 34, 17, 0, 0, Math.PI * 2);
          ctx.fill();
        }
      }
    }
    if (isAtlas) {
      for (let i = 0; i < cells.length; i++) {
        const c = cells[i];
        if (!c.isCoastalEdge) continue;
        const sp = projectIso(c.gx, c.gy, 0.05, elevPx);
        ctx.fillStyle = isDark ? 'rgba(98, 178, 206, 0.16)' : 'rgba(240, 251, 255, 0.42)';
        ctx.beginPath();
        ctx.ellipse(sp.x, sp.y + 2, 24, 12, 0, 0, Math.PI * 2);
        ctx.fill();
      }
    }
    ctx.restore();

    // 3. Back-to-Front 3D Isometric Voxel Terrain + Landmarks
    const activeColor = '#218A54';
    const selColor = '#FFFFFF';
    for (let i = 0; i < cells.length; i++) {
      const c = cells[i];
      c.heroDrawn = false;
      const unlocked = Boolean(unlockedMap[c.bid]);
      const greyOut = !showAllColor && !unlocked;
      const isUserBiome = c.bid === activeBiomeId;
      const isSelBiome = c.bid === selectedBiomeId;
      const zBot = c.isCoastalEdge ? -0.82 : Math.max(-0.35, c.wz - 0.95);

      let topHex = c.color;
      if (greyOut) {
        topHex = toLockedGrey(topHex, isDark);
      } else if (isAtlas && !c.isWater) {
        topHex = mixHex(topHex, '#F4E8D2', 0.14);
      }

      let contourStroke = null;
      if (c.isCoastalEdge && (isUserBiome || isSelBiome)) {
        contourStroke = isUserBiome ? activeColor : selColor;
      } else if (isFort && !c.isWater) {
        contourStroke = 'rgba(255, 255, 255, 0.26)';
      } else if (isAtlas && c.isCoastalEdge) {
        contourStroke = isDark ? '#7B5B40' : '#8F6A47';
      }

      drawIsoPrism(ctx, c.gx, c.gy, 1.0, zBot, c.wz, topHex, {
        elevPx,
        cliffHex: greyOut ? (isDark ? '#2A2D32' : '#84888E') : cliffHex,
        inkStroke: isAtlas ? 'rgba(94, 68, 46, 0.28)' : null,
        contourStroke,
        hitCode: ZONE_INDEX[c.bid] + 1
      });

      drawLandmarkAtCell(ctx, c, style, greyOut, isDark, elevPx, ZONE_INDEX[c.bid] + 1);
    }

    // 4. Tactile Roads & 3D Wooden Bridges over River Crossings
    const roadsList = isAtlas
      ? PROGRESSION_ORDER.slice(0, -1).map((idA, i) => [idA, PROGRESSION_ORDER[i + 1], Math.abs(ZONE_MAP[idA].cx - ZONE_MAP[PROGRESSION_ORDER[i + 1]].cx) > 150])
      : ROADS;

    ctx.save();
    roadsList.forEach(([idA, idB, hasBridge]) => {
      const a = ZONE_MAP[idA];
      const b = ZONE_MAP[idB];
      if (!a || !b) return;
      const unlockedBoth = showAllColor || (unlockedMap[idA] && unlockedMap[idB]);
      ctx.strokeStyle = !unlockedBoth
        ? 'rgba(130, 140, 148, 0.42)'
        : isAtlas
          ? '#D85B44'
          : isFort
            ? 'rgba(255, 255, 255, 0.78)'
            : 'rgba(250, 245, 228, 0.74)';
      ctx.lineWidth = isAtlas ? 3.0 : 2.1;
      ctx.setLineDash([6, 6]);
      ctx.beginPath();
      ctx.moveTo(a.cx, a.cy);
      ctx.lineTo(b.cx, b.cy);
      ctx.stroke();

      if (hasBridge) {
        const mx = (a.cx + b.cx) * 0.5;
        const my = (a.cy + b.cy) * 0.5;
        const ang = Math.atan2(b.cy - a.cy, b.cx - a.cx);
        ctx.save();
        ctx.setLineDash([]);
        ctx.translate(mx, my);
        ctx.rotate(ang);
        ctx.fillStyle = '#C9AC82';
        ctx.strokeStyle = '#6E5138';
        ctx.lineWidth = 1.4;
        ctx.beginPath();
        ctx.roundRect(-14, -5, 28, 10, 3);
        ctx.fill();
        ctx.stroke();
        ctx.beginPath();
        ctx.moveTo(-5, -5);
        ctx.lineTo(-5, 5);
        ctx.moveTo(0, -5);
        ctx.lineTo(0, 5);
        ctx.moveTo(5, -5);
        ctx.lineTo(5, 5);
        ctx.stroke();
        ctx.restore();
      }
    });
    ctx.restore();

    // 5. Soft Fluffy Clouds for Fortnite Style
    if (isFort) {
      const clouds = [
        [106, 98, 1.0],
        [838, 128, 0.92],
        [124, 526, 1.1]
      ];
      ctx.save();
      clouds.forEach(([cx, cy, s]) => {
        ctx.fillStyle = 'rgba(10, 24, 34, 0.16)';
        ctx.beginPath();
        ctx.ellipse(cx + 8, cy + 20, 34 * s, 12 * s, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = isDark ? 'rgba(220, 232, 244, 0.34)' : 'rgba(255, 255, 255, 0.72)';
        ctx.beginPath();
        ctx.ellipse(cx, cy, 32 * s, 13 * s, 0, 0, Math.PI * 2);
        ctx.ellipse(cx + 20 * s, cy - 5 * s, 21 * s, 11 * s, 0, 0, Math.PI * 2);
        ctx.ellipse(cx - 18 * s, cy + 2 * s, 18 * s, 9 * s, 0, 0, Math.PI * 2);
        ctx.fill();
      });
      ctx.restore();
    }

    // 6. Biome Labels on the 3D Continent
    ZONES.forEach(z => {
      const isUser = z.id === activeBiomeId;
      const isSel = z.id === selectedBiomeId;
      const unlocked = Boolean(unlockedMap[z.id]);
      const applyGrey = !showAllColor && !unlocked;
      const lx = z.cx;
      const ly = z.cy + 22;

      ctx.save();
      if (isFort) {
        ctx.fillStyle = '#FFFFFF';
        ctx.strokeStyle = '#18242C';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.arc(z.cx, z.cy + 5, 5.5, 0, Math.PI * 2);
        ctx.fill();
        ctx.stroke();

        ctx.font = '800 11.5px "Plus Jakarta Sans", sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.lineWidth = 3.5;
        ctx.strokeStyle = 'rgba(14, 24, 30, 0.80)';
        ctx.strokeText(z.label, lx, ly + 2);
        ctx.fillStyle = applyGrey ? '#D2D6DC' : '#FFFFFF';
        ctx.fillText(z.label, lx, ly + 2);
      } else {
        const pillW = 94;
        const pillH = 20;
        ctx.fillStyle = isDark ? 'rgba(20, 23, 28, 0.90)' : (isAtlas ? 'rgba(250, 243, 229, 0.94)' : 'rgba(250, 248, 243, 0.92)');
        ctx.strokeStyle = isUser ? '#1F7A4C' : isSel ? '#FFFFFF' : (isAtlas ? '#8C6746' : isDark ? 'rgba(255,255,255,0.18)' : 'rgba(40,38,34,0.16)');
        ctx.lineWidth = isUser ? 2.2 : 1.2;
        ctx.beginPath();
        ctx.roundRect(lx - pillW * 0.5, ly - pillH * 0.5, pillW, pillH, 10);
        ctx.fill();
        ctx.stroke();

        ctx.font = '800 9.4px "Plus Jakarta Sans", sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillStyle = applyGrey ? '#8A9098' : (isDark ? '#F1F2F4' : '#1E2A22');
        ctx.fillText(z.label, lx, ly + 0.5);
      }
      markHitRect(lx - 48, ly - 12, lx + 48, ly + 12, ZONE_INDEX[z.id] + 1);
      ctx.restore();
    });

    // 7. Compass Rose in Ocean Corner (especially prominent in Atlas Gouache)
    ctx.save();
    ctx.translate(888, 608);
    ctx.globalAlpha = isAtlas ? 0.85 : isDark ? 0.35 : 0.52;
    ctx.fillStyle = 'rgba(255, 255, 255, 0.16)';
    ctx.strokeStyle = '#FAF8F3';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.arc(0, 0, 22, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();
    ctx.fillStyle = isAtlas ? '#D85B44' : '#FAF8F3';
    ctx.beginPath();
    ctx.moveTo(0, -17); ctx.lineTo(4.5, -4); ctx.lineTo(0, -1); ctx.lineTo(-4.5, -4);
    ctx.closePath(); ctx.fill();
    ctx.fillStyle = 'rgba(250, 248, 243, 0.58)';
    [[0, 17, 4.5, 4, 0, 1, -4.5, 4], [17, 0, 4, 4.5, 1, 0, 4, -4.5], [-17, 0, -4, 4.5, -1, 0, -4, -4.5]].forEach(pts => {
      ctx.beginPath();
      ctx.moveTo(pts[0], pts[1]); ctx.lineTo(pts[2], pts[3]);
      ctx.lineTo(pts[4], pts[5]); ctx.lineTo(pts[6], pts[7]);
      ctx.closePath(); ctx.fill();
    });
    ctx.restore();
  }

  return {
    MAP_WIDTH,
    MAP_HEIGHT,
    ZONES,
    applyLanguage,
    getZone,
    hitTestBiome,
    renderMapInto
  };
})();
