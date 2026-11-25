/**
 * Motor Topográfico 3D Interactivo — Dioramas de Biomas para Brote
 * Maquetas arquitectónicas 1:5.500 enmarcadas (cuadrícula 18×18) con rotación 360°, zoom,
 * 5 biomas (incluida Jungla Lluviosa y Kioto renovado con almendros rosas) y 8 niveles de mejora.
 */
window.BroteMascot = (function () {
  const N = 18;
  const MAX_POINTS = 80;
  const POINTS_PER_LEVEL = 10;

  const BIOMES = [
    {
      id: 'valle',
      name: 'Alpine Valley',
      shortName: 'Valley',
      subtitle: 'Green valley, alpine lake & snowy peaks',
      palette: ['#569B58', '#6CC6DF', '#F8FAFC'],
      skyGlow: 'rgba(248, 228, 182, 0.26)'
    },
    {
      id: 'jungla',
      name: 'Rainy Jungle',
      shortName: 'Jungle',
      subtitle: 'Emerald canyon, waterfall, temples & rain',
      palette: ['#1B472C', '#56AEBC', '#2E6943'],
      skyGlow: 'rgba(182, 224, 214, 0.28)'
    },
    {
      id: 'costa',
      name: 'Mediterranean Coast',
      shortName: 'Coast',
      subtitle: 'Warm cliffs, blue cove & lighthouse',
      palette: ['#77C4D4', '#E6CFA5', '#D97757'],
      skyGlow: 'rgba(252, 232, 185, 0.28)'
    },
    {
      id: 'kioto',
      name: 'Kyoto Valley',
      shortName: 'Kyoto',
      subtitle: 'Pink blossoms, wooden bridge & pagoda',
      palette: ['#F7CED9', '#E5BDD0', '#6DA6AD'],
      skyGlow: 'rgba(250, 222, 232, 0.28)'
    },
    {
      id: 'desierto',
      name: 'Oasis & Dunes',
      shortName: 'Oasis',
      subtitle: 'Sculpted dunes, turquoise pool & stone arch',
      palette: ['#E2BA7A', '#4EB2B0', '#C38E4B'],
      skyGlow: 'rgba(250, 222, 174, 0.28)'
    },
    {
      id: 'arrozales',
      name: 'Bali Terraces',
      shortName: 'Bali',
      subtitle: 'Flooded rice terraces, bamboo & temple',
      palette: ['#4E9952', '#5DC4D2', '#74C16B'],
      skyGlow: 'rgba(206, 238, 198, 0.28)'
    },
    {
      id: 'otono',
      name: 'Autumn Woods',
      shortName: 'Autumn',
      subtitle: 'Red, orange & golden maples by the lake',
      palette: ['#D9342B', '#F27B24', '#F8C73A'],
      skyGlow: 'rgba(246, 206, 168, 0.28)'
    },
    {
      id: 'fiordo',
      name: 'Nordic Fjord',
      shortName: 'Fjord',
      subtitle: 'Wide fjord, sea ice, snowy pines & longship',
      palette: ['#417FA3', '#E8F1F8', '#B84438'],
      skyGlow: 'rgba(170, 232, 224, 0.28)'
    },
    {
      id: 'atolon',
      name: 'Coral Atoll',
      shortName: 'Atoll',
      subtitle: 'Turquoise lagoon, stilt huts & coral reef',
      palette: ['#4CC7C4', '#F4E5C8', '#EE7A74'],
      skyGlow: 'rgba(182, 242, 242, 0.30)'
    },
    {
      id: 'toscana',
      name: 'Tuscan Hills',
      shortName: 'Tuscany',
      subtitle: 'Cypresses, Roman fountains & sunlit villa',
      palette: ['#8DA45A', '#DFB66A', '#C96D4B'],
      skyGlow: 'rgba(250, 228, 178, 0.28)'
    }
  ];

  const STAGES = [
    { stage: 1, title: 'Lvl. 1 · Base Terrain', desc: 'Initial soft relief and water basin' },
    { stage: 2, title: 'Lvl. 2 · First Sprouts', desc: 'Richer terrain, boulders & first trees' },
    { stage: 3, title: 'Lvl. 3 · Lush Flora', desc: 'Dense groves and blooming trees' },
    { stage: 4, title: 'Lvl. 4 · Paths & Bridges', desc: 'Cobble trails and bridges over water' },
    { stage: 5, title: 'Lvl. 5 · Mini Village', desc: 'First miniature cottages appear' },
    { stage: 6, title: 'Lvl. 6 · Living Settlement', desc: 'Boats on the water & warm lanterns' },
    { stage: 7, title: 'Lvl. 7 · Grand Landmark', desc: 'Hero 3D monument of the biome' },
    { stage: 8, title: 'Lvl. 8 · Legendary Biome', desc: 'Maximum elevation, shimmers & details' }
  ];

  const BIOME_I18N = {
    valle: {
      es: { name: 'Valle Alpino', shortName: 'Valle', subtitle: 'Valle verde, lago alpino y cima nevada' },
      en: { name: 'Alpine Valley', shortName: 'Valley', subtitle: 'Green valley, alpine lake & snowy peaks' }
    },
    jungla: {
      es: { name: 'Jungla Lluviosa', shortName: 'Jungla', subtitle: 'Cañón esmeralda, cascada, templos y lluvia' },
      en: { name: 'Rainy Jungle', shortName: 'Jungle', subtitle: 'Emerald canyon, waterfall, temples & rain' }
    },
    costa: {
      es: { name: 'Costa Mediterránea', shortName: 'Costa', subtitle: 'Acantilados crema, cala y faro' },
      en: { name: 'Mediterranean Coast', shortName: 'Coast', subtitle: 'Warm cliffs, blue cove & lighthouse' }
    },
    kioto: {
      es: { name: 'Valle de Kioto', shortName: 'Kioto', subtitle: 'Almendros rosas, puente y pagoda' },
      en: { name: 'Kyoto Valley', shortName: 'Kyoto', subtitle: 'Pink blossoms, wooden bridge & pagoda' }
    },
    desierto: {
      es: { name: 'Oasis y Dunas', shortName: 'Oasis', subtitle: 'Dunas esculpidas, poza turquesa y arco' },
      en: { name: 'Oasis & Dunes', shortName: 'Oasis', subtitle: 'Sculpted dunes, turquoise pool & stone arch' }
    },
    arrozales: {
      es: { name: 'Terrazas de Bali', shortName: 'Bali', subtitle: 'Arrozales inundados, bambú y templo' },
      en: { name: 'Bali Terraces', shortName: 'Bali', subtitle: 'Flooded rice terraces, bamboo & temple' }
    },
    otono: {
      es: { name: 'Bosque de Otoño', shortName: 'Otoño', subtitle: 'Arces rojos, naranjas, dorados y lago' },
      en: { name: 'Autumn Woods', shortName: 'Autumn', subtitle: 'Red, orange & golden maples by the lake' }
    },
    fiordo: {
      es: { name: 'Fiordo Nórdico', shortName: 'Fiordo', subtitle: 'Fiordo amplio, hielo, abetos nevados y navío' },
      en: { name: 'Nordic Fjord', shortName: 'Fjord', subtitle: 'Wide fjord, sea ice, snowy pines & longship' }
    },
    atolon: {
      es: { name: 'Atolón de Coral', shortName: 'Atolón', subtitle: 'Laguna turquesa, palafitos y arrecife' },
      en: { name: 'Coral Atoll', shortName: 'Atoll', subtitle: 'Turquoise lagoon, stilt huts & coral reef' }
    },
    toscana: {
      es: { name: 'Colinas de Toscana', shortName: 'Toscana', subtitle: 'Cipreses, fuentes romanas y villa' },
      en: { name: 'Tuscan Hills', shortName: 'Tuscany', subtitle: 'Cypresses, Roman fountains & sunlit villa' }
    }
  };

  const STAGE_I18N = {
    1: { es: { title: 'Nv. 1 · Terreno base', desc: 'Relieve suave inicial y cuenca de agua' }, en: { title: 'Lvl. 1 · Base Terrain', desc: 'Initial soft relief and water basin' } },
    2: { es: { title: 'Nv. 2 · Primeros brotes', desc: 'Más relieve, rocas y primeros árboles' }, en: { title: 'Lvl. 2 · First Sprouts', desc: 'Richer terrain, boulders & first trees' } },
    3: { es: { title: 'Nv. 3 · Flora abundante', desc: 'Arboledas densas y almendros en flor' }, en: { title: 'Lvl. 3 · Lush Flora', desc: 'Dense groves and blooming trees' } },
    4: { es: { title: 'Nv. 4 · Caminos y puentes', desc: 'Senderos y puentes sobre el agua' }, en: { title: 'Lvl. 4 · Paths & Bridges', desc: 'Cobble trails and bridges over water' } },
    5: { es: { title: 'Nv. 5 · Aldea 1:5.500', desc: 'Primeras construcciones en miniatura' }, en: { title: 'Lvl. 5 · Mini Village', desc: 'First miniature cottages appear' } },
    6: { es: { title: 'Nv. 6 · Asentamiento vivo', desc: 'Más vida, barcas y farolillos cálidos' }, en: { title: 'Lvl. 6 · Living Settlement', desc: 'Boats on the water & warm lanterns' } },
    7: { es: { title: 'Nv. 7 · Gran monumento', desc: 'Monumento principal 3D del bioma' }, en: { title: 'Lvl. 7 · Grand Landmark', desc: 'Hero 3D monument of the biome' } },
    8: { es: { title: 'Nv. 8 · Bioma legendario', desc: 'Elevación máxima, brillos y detalles extra' }, en: { title: 'Lvl. 8 · Legendary Biome', desc: 'Maximum elevation, shimmers & details' } }
  };

  function applyLanguage(lang) {
    const useEn = lang ? lang === 'en' : !!(window.SplashI18n && window.SplashI18n.isEn());
    BIOMES.forEach(b => {
      const pack = BIOME_I18N[b.id] && (useEn ? BIOME_I18N[b.id].en : BIOME_I18N[b.id].es);
      if (pack) {
        b.name = pack.name;
        b.shortName = pack.shortName;
        b.subtitle = pack.subtitle;
      }
    });
    STAGES.forEach(s => {
      const pack = STAGE_I18N[s.stage] && (useEn ? STAGE_I18N[s.stage].en : STAGE_I18N[s.stage].es);
      if (pack) {
        s.title = pack.title;
        s.desc = pack.desc;
      }
    });
  }
  applyLanguage();

  // Vector solar fijo en el mundo (normalizado)
  const SUN_RAW = { x: 0.55, y: -0.45, z: 0.70 };
  const SUN_LEN = Math.hypot(SUN_RAW.x, SUN_RAW.y, SUN_RAW.z);
  const SUN = { x: SUN_RAW.x / SUN_LEN, y: SUN_RAW.y / SUN_LEN, z: SUN_RAW.z / SUN_LEN };

  let dioramaCanvas = null;
  let dioramaCtx = null;
  let particleCanvas = null;
  let particleCtx = null;
  let particles = [];
  let animFrameId = null;
  const layoutCache = {};
  let lastRenderOpts = {
    biome: 'valle',
    points: 0,
    isAwake: false,
    yaw: Math.PI / 4,
    pitch: 0.60,
    zoom: 1.0,
    time: 0.5
  };

  function getLevelFromPoints(points) {
    return Math.min(8, 1 + Math.floor(Math.max(0, points || 0) / POINTS_PER_LEVEL));
  }

  function getSubProgress(points) {
    const p = Math.max(0, points || 0);
    return p >= MAX_POINTS ? POINTS_PER_LEVEL : p % POINTS_PER_LEVEL;
  }

  function getPointsForStagePreset(stage) {
    const presets = [0, 15, 26, 36, 46, 57, 68, 80];
    const idx = Math.max(0, Math.min(presets.length - 1, (stage || 1) - 1));
    return presets[idx];
  }

  function getBiome(id) {
    return BIOMES.find(b => b.id === id) || BIOMES[0];
  }

  function getStageInfo(stage) {
    const idx = Math.max(0, Math.min(STAGES.length - 1, (stage || 1) - 1));
    return STAGES[idx];
  }

  function getNextBiomeId(currentId, delta) {
    const idx = BIOMES.findIndex(b => b.id === currentId);
    const start = idx === -1 ? 0 : idx;
    const next = (start + delta + BIOMES.length) % BIOMES.length;
    return BIOMES[next].id;
  }

  function hexToRgb(hex) {
    const clean = hex.replace('#', '');
    const num = parseInt(clean, 16);
    return {
      r: (num >> 16) & 255,
      g: (num >> 8) & 255,
      b: num & 255
    };
  }

  function shadeColor(hex, percent, warmBoost = 0) {
    const { r, g, b } = hexToRgb(hex);
    const factor = 1 + percent / 100;
    const nr = Math.min(255, Math.max(0, Math.round(r * factor + warmBoost)));
    const ng = Math.min(255, Math.max(0, Math.round(g * factor + warmBoost * 0.8)));
    const nb = Math.min(255, Math.max(0, Math.round(b * factor + warmBoost * 0.35)));
    return `rgb(${nr}, ${ng}, ${nb})`;
  }

  function lambertShade(hex, nx, ny, nz, isAwake) {
    const dot = nx * SUN.x + ny * SUN.y + nz * SUN.z;
    const pct = dot * 26 - 10;
    const warm = isAwake && dot > 0.2 ? 9 : 0;
    return shadeColor(hex, pct, warm);
  }

  function screenSignedArea(pts) {
    let area = 0;
    for (let i = 0; i < pts.length; i++) {
      const a = pts[i];
      const b = pts[(i + 1) % pts.length];
      area += a.x * b.y - b.x * a.y;
    }
    return area;
  }

  function fillPoly(ctx, pts, fill, strokeColor, lineWidth = 0.7) {
    ctx.beginPath();
    ctx.moveTo(pts[0].x, pts[0].y);
    for (let i = 1; i < pts.length; i++) {
      ctx.lineTo(pts[i].x, pts[i].y);
    }
    ctx.closePath();
    ctx.fillStyle = fill;
    ctx.fill();
    if (strokeColor) {
      ctx.strokeStyle = strokeColor;
      ctx.lineWidth = lineWidth;
      ctx.lineJoin = 'round';
      ctx.stroke();
    }
  }

  function cellHash(x, y, seed = 17) {
    const n = Math.sin(x * 127.1 + y * 311.7 + seed * 74.7) * 43758.5453123;
    return n - Math.floor(n);
  }

  function getHeightScaleFromPoints(points) {
    const t = Math.min(1, Math.max(0, points || 0) / MAX_POINTS);
    return 0.50 + t * 0.72;
  }

  // Relieve y color por celda en coordenadas de mundo (unidades de cuadrícula)
  function getCellData(biomeId, x, y, points, showPathHere = false) {
    const nx = x / (N - 1);
    const ny = y / (N - 1);
    const noise = (cellHash(x, y, 3) - 0.5) * 0.17;
    const hScale = getHeightScaleFromPoints(points);

    if (biomeId === 'valle') {
      const riverCenter = 0.50 + 0.08 * Math.sin(ny * 4.6);
      const distRiver = Math.abs(nx - riverCenter);
      const lakeDist = Math.hypot(nx - 0.50, ny - 0.72);
      const isWater = (ny > 0.34 && distRiver < 0.064) || lakeDist < 0.145;

      const hasPotentialPath = !isWater && ny > 0.42 && ny < 0.88 && Math.abs(nx - 0.28) < 0.042;
      const isPath = hasPotentialPath && showPathHere;

      const valleyWalls = Math.pow(Math.abs(nx - 0.50) * 1.85, 1.55) * 0.95;
      const backSlope = Math.max(0, 1 - ny) * 0.65;
      const mainPeak = Math.max(0, 1 - Math.hypot((nx - 0.50) * 1.15, (ny - 0.12) * 1.35) * 1.85);
      let rawH = 0.68 + valleyWalls + backSlope + Math.pow(mainPeak, 1.25) * 2.55 + noise * 0.42;
      if (lakeDist < 0.20) rawH = Math.min(rawH, 0.92);

      const stepped = Math.round(rawH * 4.4) / 4.4;
      const wz = isWater ? 0.34 : Math.max(0.54, stepped * hScale);

      let color = '#6AB168';
      if (isWater) {
        color = lakeDist < 0.085 ? '#46A4C2' : '#6CC6DF';
      } else if (isPath) {
        color = '#DFD6C3';
      } else if (rawH > 3.12 && points >= 10) {
        color = '#F8FAFC';
      } else if (rawH > 2.75) {
        color = '#9AA7AE';
      } else if (rawH > 1.85) {
        color = '#44824A';
      } else if (rawH > 1.30) {
        color = '#569B58';
      } else if (lakeDist < 0.21 || distRiver < 0.12) {
        color = '#82C67C';
      }

      return { wz, color, isWater, hasPotentialPath, isPath, rawH, distRiver };
    }

    if (biomeId === 'costa') {
      const coastLine = 0.44 + 0.14 * Math.sin(ny * Math.PI * 1.4) + 0.08 * ny;
      const isWater = nx > coastLine;
      const depth = nx - coastLine;
      const hasPotentialPath = !isWater && Math.abs(nx - 0.18) < 0.048 && ny > 0.14 && ny < 0.84;
      const isPath = hasPotentialPath && showPathHere;

      let rawH = 0.95 + Math.max(0, coastLine - nx) * 3.4 + (1 - ny) * 0.72 + noise;
      const stepped = Math.round(rawH * 4.2) / 4.2;
      const wz = isWater
        ? Math.max(0.22, 0.40 - depth * 0.32)
        : Math.max(0.54, stepped * hScale);

      let color = '#CCA976';
      if (isWater) {
        color = depth > 0.28 ? '#4B9FB5' : depth > 0.12 ? '#77C4D4' : '#9FDCE6';
      } else if (isPath) {
        color = '#EAD6B1';
      } else if (depth > -0.11) {
        color = '#EEDBB8';
      } else if (rawH > 2.25) {
        color = '#E6CFA5';
      } else if (rawH > 1.65) {
        color = '#D9BC8D';
      }

      return { wz, color, isWater, hasPotentialPath, isPath, rawH, depth };
    }

    if (biomeId === 'kioto') {
      const streamCenter = 0.36 + 0.15 * Math.cos(ny * Math.PI * 1.25);
      const pondDist = Math.hypot(nx - 0.32, ny - 0.68);
      const isWater = Math.abs(nx - streamCenter) < 0.072 || pondDist < 0.142;

      const pathCenter = 0.66 - 0.12 * Math.sin(nx * Math.PI);
      const hasPotentialPath = !isWater && Math.abs(ny - pathCenter) < 0.052;
      const isPath = hasPotentialPath && showPathHere;

      const hillBack = Math.hypot(nx - 0.78, ny - 0.18);
      const hillLeft = Math.hypot(nx - 0.14, ny - 0.24);
      let rawH =
        0.85 +
        Math.max(0, 1 - hillBack * 1.68) * 2.35 +
        Math.max(0, 1 - hillLeft * 2.2) * 1.05 +
        (1 - ny) * 0.4 +
        noise * 0.6;
      const stepped = Math.round(rawH * 4.4) / 4.4;
      const wz = isWater ? 0.34 : Math.max(0.54, stepped * hScale);

      let color = '#F3D9E3';
      if (isWater) {
        color = '#6DA6AD';
      } else if (isPath) {
        color = '#F1E9E2';
      } else if (rawH > 2.28) {
        color = '#C99FB1';
      } else if (rawH > 1.72) {
        color = '#E5BDD0';
      } else if (cellHash(x, y, 29) > 0.46) {
        color = '#F9E7EE';
      }

      return { wz, color, isWater, hasPotentialPath, isPath, rawH };
    }

    if (biomeId === 'jungla') {
      const fallAxis = Math.abs(nx - ny);
      const inCliff = nx < 0.36 && ny < 0.36;
      const isWaterfall = nx < 0.39 && ny < 0.39 && fallAxis < 0.095 && (nx + ny) > 0.20;

      const riverCurve = 0.50 + 0.13 * Math.sin(nx * Math.PI * 1.5);
      const poolDist = Math.hypot(nx - 0.38, ny - 0.38);
      const inPool = poolDist < 0.135;
      const inRiver = nx >= 0.30 && Math.abs(ny - riverCurve) < 0.075;
      const isWater = inPool || inRiver;

      const cliffHeight = inCliff ? (1 - Math.hypot(nx - 0.14, ny - 0.14) * 1.32) * 2.95 : 0;
      const canyonL = Math.max(0, 1 - Math.hypot(nx - 0.80, ny - 0.20) * 1.65) * 0.92;
      const canyonR = Math.max(0, 1 - Math.hypot(nx - 0.24, ny - 0.82) * 1.75) * 0.72;
      let rawH = 0.78 + cliffHeight + canyonL + canyonR + noise * 0.65;
      if (isWaterfall) {
        rawH = Math.max(0.62, 2.95 - (nx + ny) * 3.35);
      }

      const stepped = Math.round(rawH * 4.5) / 4.5;
      const wz = isWater
        ? 0.34
        : isWaterfall
          ? Math.max(0.45, stepped * hScale)
          : Math.max(0.54, stepped * hScale);

      let color = '#245838';
      if (isWaterfall) {
        color = cellHash(x, y, 13) > 0.38 ? '#E6FAFD' : '#7AD4E2';
      } else if (isWater) {
        color = inPool ? '#52AEBE' : '#368796';
      } else if (rawH > 2.05) {
        color = '#133822';
      } else if (rawH > 1.45) {
        color = '#1B472C';
      } else if (cellHash(x, y, 23) > 0.50) {
        color = '#2E6943';
      }

      return { wz, color, isWater, isWaterfall, hasPotentialPath: false, isPath: false, rawH, riverDist: Math.abs(ny - riverCurve) };
    }

    if (biomeId === 'arrozales') {
      const poolDist = Math.hypot(nx - 0.72, ny - 0.70);
      const templePlinth = Math.hypot(nx - 12 / 17, ny - 12 / 17) < 0.068;
      const isCanal = nx >= 0.28 && nx <= 0.56 && Math.abs(ny - nx) < 0.055;
      const isWater = !templePlinth && (poolDist < 0.23 || isCanal);

      const hasPotentialPath = !isWater && !templePlinth && nx < 0.54 && Math.abs(ny - 0.70) < 0.042;
      const isPath = hasPotentialPath && showPathHere;

      const rSlope = Math.hypot(nx * 0.94, ny * 0.94);
      let rawH = 0.76 + Math.max(0, 1.15 - rSlope) * 1.92;
      if (templePlinth) rawH = 1.04;

      const stepped = Math.round(rawH * 2.8) / 2.8;
      const ringPhase = (rSlope * 3.4) % 1;
      const isFloodedPaddy = !isWater && !templePlinth && !hasPotentialPath && rSlope > 0.14 && rSlope < 0.96 && ringPhase > 0.36;
      const wz = isWater ? 0.34 : Math.max(0.50, (isFloodedPaddy ? stepped - 0.05 : stepped) * hScale);

      let color = '#7AC372';
      if (isWater) {
        color = poolDist < 0.13 ? '#3FA4B5' : '#5EC2D0';
      } else if (isPath) {
        color = '#A6BC9E';
      } else if (templePlinth) {
        color = '#7E867E';
      } else if (isFloodedPaddy) {
        color = cellHash(x, y, 21) > 0.45 ? '#5BC1CF' : '#70CFDC';
      } else if (rawH > 1.88) {
        color = '#3E8745';
      } else if (rawH > 1.38) {
        color = '#529E54';
      } else if (rawH > 0.98) {
        color = '#68B464';
      }

      return { wz, color, isWater, isFloodedPaddy, hasPotentialPath, isPath, rawH };
    }

    if (biomeId === 'otono') {
      const streamCenter = 0.48 + 0.14 * Math.sin(nx * 4.1);
      const distStream = Math.abs(ny - streamCenter);
      const lakeDist = Math.hypot(nx - 0.64, ny - 0.66);
      const isWater = distStream < 0.078 || lakeDist < 0.19;

      const hasPotentialPath = !isWater && Math.abs(nx - 0.32 - ny * 0.28) < 0.045;
      const isPath = hasPotentialPath && showPathHere;

      const peak1 = Math.hypot(nx - 0.22, ny - 0.18);
      const peak2 = Math.hypot(nx - 0.84, ny - 0.22);
      let rawH = 0.88 + Math.max(0, 1 - peak1 * 1.65) * 2.15 + Math.max(0, 1 - peak2 * 1.9) * 1.55;
      rawH += Math.sin(nx * 4.5) * 0.18 + noise * 0.55;

      const stepped = Math.round(rawH * 4.2) / 4.2;
      const wz = isWater ? 0.34 : Math.max(0.54, stepped * hScale);

      let color = '#E05D38';
      if (isWater) {
        color = lakeDist < 0.11 ? '#3F7FA3' : '#5C9EC0';
      } else if (isPath) {
        color = '#EFE0C8';
      } else if (rawH > 2.25) {
        color = '#D44735';
      } else if (rawH > 1.72) {
        color = '#EB7E30';
      } else if (cellHash(x, y, 31) > 0.52) {
        color = '#F4C348';
      } else if (cellHash(x, y, 31) > 0.24) {
        color = '#EE9A3A';
      }

      return { wz, color, isWater, hasPotentialPath, isPath, rawH };
    }

    if (biomeId === 'fiordo') {
      const isWaterfall = nx >= 0.04 && nx < 0.15 && Math.abs(ny - 0.54) < 0.072;
      const fjordCenter = 0.54 + 0.06 * Math.sin(nx * 3.4);
      const fjordWidth = 0.20 + 0.06 * nx;
      const isWater = !isWaterfall && nx >= 0.14 && Math.abs(ny - fjordCenter) < fjordWidth;

      const hasPotentialPath = !isWater && !isWaterfall && ny < 0.28 && Math.abs(ny - 0.20) < 0.040;
      const isPath = hasPotentialPath && showPathHere;

      const northBank = Math.max(0, (fjordCenter - fjordWidth - ny) * 3.4);
      const southBank = Math.max(0, (ny - (fjordCenter + fjordWidth)) * 3.0);
      const backWall = Math.max(0, (0.20 - nx) * 4.5);
      let rawH = 0.78 + northBank + southBank + backWall + Math.sin(nx * 3.2) * 0.14 + noise * 0.28;
      if (isWaterfall) {
        rawH = Math.max(0.62, 2.55 - nx * 13.0);
      }

      const stepped = Math.round(rawH * 3.6) / 3.6;
      const wz = isWater
        ? 0.34
        : isWaterfall
          ? Math.max(0.45, stepped * hScale)
          : Math.max(0.54, stepped * hScale);

      let color = '#9FB2C2';
      if (isWaterfall) {
        color = cellHash(x, y, 13) > 0.42 ? '#EFF9FD' : '#A4D8EB';
      } else if (isWater) {
        const dF = Math.abs(ny - fjordCenter);
        color = dF < fjordWidth * 0.46 ? '#2B6284' : dF < fjordWidth * 0.78 ? '#417FA3' : '#7DB3CE';
      } else if (isPath) {
        color = '#D4E0E9';
      } else if (rawH > 1.75) {
        color = '#F4F8FC';
      } else if (rawH > 1.25) {
        color = '#E3EDF5';
      } else if (cellHash(x, y, 33) > 0.52) {
        color = '#CBDCE8';
      }

      return { wz, color, isWater, isWaterfall, hasPotentialPath, isPath, rawH };
    }

    if (biomeId === 'atolon') {
      const lagDist = Math.hypot(nx - 0.58, ny - 0.56);
      const isLagoon = lagDist < 0.275;
      const isChannel = nx > 0.72 && ny > 0.68 && Math.abs(nx - ny) < 0.16;
      const isOuterSea = lagDist > 0.53 && !(nx < 0.40 && ny < 0.40);
      const isWater = isLagoon || isOuterSea || isChannel;

      const hasPotentialPath = !isWater && Math.abs(lagDist - 0.34) < 0.042 && ny < 0.62;
      const isPath = hasPotentialPath && showPathHere;

      const volcPeak = Math.max(0, 1 - Math.hypot(nx - 0.24, ny - 0.26) * 2.15) * 2.25;
      const eastKnoll = Math.max(0, 1 - Math.hypot(nx - 0.80, ny - 0.24) * 2.7) * 0.85;
      const rawH = 0.68 + volcPeak + eastKnoll + noise * 0.35;

      const stepped = Math.round(rawH * 4.4) / 4.4;
      const wz = isWater ? 0.34 : Math.max(0.54, stepped * hScale);

      let color = '#F4E5C8';
      if (isLagoon) {
        color = lagDist < 0.14 ? '#3AC0BD' : '#5BDAD3';
      } else if (isOuterSea || isChannel) {
        color = '#318BA6';
      } else if (isPath) {
        color = '#E5D5BA';
      } else if (rawH > 1.85) {
        color = '#55875A';
      } else if (rawH > 1.05 || volcPeak > 0.45) {
        color = '#6AA35D';
      }

      return { wz, color, isWater, isLagoon, hasPotentialPath, isPath, rawH };
    }

    if (biomeId === 'toscana') {
      const riverCenter = 0.60 + 0.12 * Math.cos(nx * 4.2);
      const distRiver = Math.abs(ny - riverCenter);
      const isWater = distRiver < 0.072;

      const hasPotentialPath = !isWater && ny < 0.54 && Math.abs(nx - 0.30 - ny * 0.18) < 0.044;
      const isPath = hasPotentialPath && showPathHere;

      const hill1 = Math.hypot(nx - 0.28, ny - 0.26);
      const hill2 = Math.hypot(nx - 0.78, ny - 0.32);
      let rawH = 0.88 + Math.max(0, 1 - hill1 * 1.45) * 2.15 + Math.max(0, 1 - hill2 * 1.65) * 1.5;
      rawH += Math.sin(nx * 3.8 + ny * 2.5) * 0.18 + noise * 0.4;

      const stepped = Math.round(rawH * 4.2) / 4.2;
      const wz = isWater ? 0.34 : Math.max(0.54, stepped * hScale);
      const isVineyard = !isWater && !hasPotentialPath && rawH > 1.1 && rawH < 2.25 && ((x + y) % 2 === 0) && cellHash(x, y, 63) > 0.42;

      let color = '#DFB66A';
      if (isWater) {
        color = distRiver < 0.035 ? '#5BA8C2' : '#74BAD1';
      } else if (isPath) {
        color = '#EFE7D6';
      } else if (isVineyard) {
        color = '#7C964A';
      } else if (rawH > 1.85) {
        color = '#97AD5B';
      } else if (cellHash(x, y, 27) > 0.5) {
        color = '#C9B362';
      }

      return { wz, color, isWater, isVineyard, hasPotentialPath, isPath, rawH };
    }

    // 'desierto' — Oasis y Dunas (laguna amplia visible, laterales bajos, crema fuerte, 0 verde)
    const oasisDist = Math.hypot((nx - 0.52) * 0.92, (ny - 0.56) * 1.02);
    const isWater = oasisDist < 0.235;
    const isOasisRim = !isWater && oasisDist < 0.31;
    const hasPotentialPath = !isWater && ny < 0.56 && Math.abs(nx - ny * 0.82 - 0.08) < 0.045;
    const isPath = hasPotentialPath && showPathHere;

    const backFactor = Math.max(0, 1 - (nx * 0.52 + ny * 0.58));
    const northDune = Math.max(0, 1 - Math.hypot((nx - 0.22) * 1.1, (ny - 0.16) * 1.35) * 1.45) * 1.65;
    const northEastDune = Math.max(0, 1 - Math.hypot((nx - 0.78) * 1.25, (ny - 0.14) * 1.55) * 1.65) * 1.15;
    const gentleWave = Math.sin(nx * 4.2 + ny * 2.2) * 0.22 + Math.cos(ny * 3.8 - nx * 1.4) * 0.16;
    let rawH = 0.74 + backFactor * 0.95 + northDune + northEastDune + gentleWave + noise * 0.16;
    const frontOpen = Math.max(0, (nx + ny - 0.95) * 0.65);
    rawH = Math.max(0.68, rawH - frontOpen * 0.60);
    if (ny > 0.58 || nx > 0.74) rawH = Math.min(rawH, 1.16);
    if (isOasisRim) rawH = Math.min(rawH, 0.78);

    const isMesa = false;
    const stepped = Math.round(rawH * 4.2) / 4.2;
    const wz = isWater ? 0.34 : Math.max(0.54, stepped * hScale);

    let color = '#EACA90';
    if (isWater) {
      color = oasisDist < 0.125 ? '#359CA0' : '#55C4C2';
    } else if (isPath) {
      color = '#EED3A1';
    } else if (isOasisRim) {
      color = '#D6AA69';
    } else if (rawH > 2.35) {
      color = '#C38E4B';
    } else if (rawH > 1.75) {
      color = '#D4A45E';
    } else if (rawH > 1.25) {
      color = '#E2BA7A';
    }

    return { wz, color, isWater, isOasisRim, isMesa, hasPotentialPath, isPath, rawH, oasisDist };
  }

  // Ranking determinista de celdas para progresión suave misión a misión
  function getBiomeLayout(biomeId) {
    if (layoutCache[biomeId]) return layoutCache[biomeId];

    const landmarkCell = {
      jungla: { x: 12, y: 4 },
      costa: { x: 6, y: 3 },
      kioto: { x: 13, y: 4 },
      valle: { x: 4, y: 12 },
      desierto: { x: 13, y: 12 },
      arrozales: { x: 12, y: 12 },
      otono: { x: 5, y: 5 },
      fiordo: { x: 13, y: 2 },
      atolon: { x: 5, y: 5 },
      toscana: { x: 5, y: 4 }
    }[biomeId];

    const toriiCell = { x: 8, y: 11 };
    const secondToriiCell = { x: 5, y: 13 };
    const costaWindmillCell = { x: 2, y: 4 };
    const desertRuinsCell = { x: 3, y: 3 };
    const toscanaFountains = [
      { x: 12, y: 4 },
      { x: 4, y: 14 }
    ];
    const secondaryCells = {
      costa: costaWindmillCell,
      desierto: desertRuinsCell,
      arrozales: { x: 4, y: 4 },
      otono: { x: 4, y: 12 },
      fiordo: { x: 4, y: 15 },
      atolon: { x: 14, y: 4 },
      toscana: { x: 13, y: 12 }
    };
    const secondaryCell = secondaryCells[biomeId] || null;

    const treeList = [];
    const rockList = [];
    const pathList = [];
    const tentList = [];
    const houseList = [];
    const overwaterList = [];
    const lanternList = [];
    const shimmerList = [];

    const maxHouseH = (biomeId === 'costa' || biomeId === 'desierto' || biomeId === 'atolon' || biomeId === 'toscana') ? 3.8 : 2.25;
    const houseBaseThresh = biomeId === 'costa' ? 0.58 : biomeId === 'toscana' ? 0.77 : (biomeId === 'atolon' ? 0.85 : 0.83);
    const rockThresh = (biomeId === 'desierto' || biomeId === 'fiordo') ? 0.90 : 0.92;

    for (let x = 0; x < N; x++) {
      for (let y = 0; y < N; y++) {
        const key = `${x},${y}`;
        const cell = getCellData(biomeId, x, y, MAX_POINTS, true);

        if (cell.isWater) {
          const isBridgeTile =
            (biomeId === 'valle' && x === 9 && y === 8) ||
            (biomeId === 'kioto' && y === 11 && x === 7) ||
            (biomeId === 'jungla' && x === 10 && cell.riverDist < 0.06) ||
            (biomeId === 'costa' && x === 11 && y === 8) ||
            (biomeId === 'desierto' && x === 8 && y === 10) ||
            (biomeId === 'arrozales' && x === 10 && y === 12) ||
            (biomeId === 'otono' && x === 8 && y === 10) ||
            (biomeId === 'fiordo' && x === 4 && y === 9) ||
            (biomeId === 'atolon' && x === 9 && y === 8) ||
            (biomeId === 'toscana' && x === 9 && y === 9);
          const isBoatTile =
            (biomeId === 'valle' && ((x === 9 && y === 12) || (x === 8 && y === 13))) ||
            (biomeId === 'arrozales' && (x === 14 && y === 12)) ||
            (biomeId === 'otono' && ((x === 10 && y === 11) || (x === 12 && y === 11))) ||
            (biomeId === 'fiordo' && ((y === 10 && x >= 10 && x <= 12) || (x === 15 && y === 11))) ||
            (biomeId === 'atolon' && ((x === 11 && y === 11) || (x === 14 && y === 13))) ||
            (biomeId === 'toscana' && ((x === 13 && y === 8) || (x === 5 && y === 11))) ||
            (['jungla', 'costa', 'kioto'].includes(biomeId) && ((x === 13 && y === 11) || (x === 14 && y === 6)));
          if (!isBridgeTile && !isBoatTile && biomeId === 'atolon' && cell.isLagoon && cellHash(x, y, 73) > 0.72) {
            overwaterList.push({ key, score: cellHash(x, y, 74) });
            continue;
          }
          if (!isBridgeTile && !isBoatTile && cellHash(x, y, 41) > 0.56) {
            shimmerList.push({ key, score: cellHash(x, y, 42) });
          }
          continue;
        }

        if (cell.isWaterfall) continue;

        const isLandmark = landmarkCell && landmarkCell.x === x && landmarkCell.y === y;
        const isTorii = biomeId === 'kioto' && ((x === toriiCell.x && y === toriiCell.y) || (x === secondToriiCell.x && y === secondToriiCell.y));
        const isSecondary = secondaryCell && secondaryCell.x === x && secondaryCell.y === y;
        const isExtraFountain = biomeId === 'toscana' && toscanaFountains.some(f => f.x === x && f.y === y);

        if (isLandmark || isTorii || isSecondary || isExtraFountain) continue;

        if (cell.hasPotentialPath) {
          const orderScore = (biomeId === 'costa' || biomeId === 'toscana') ? y : (biomeId === 'kioto' || biomeId === 'fiordo') ? x : x + y;
          pathList.push({ key, score: orderScore });
          continue;
        }

        const hVal = cellHash(x, y, 99);

        if (biomeId === 'arrozales' && cell.isFloodedPaddy) continue;

        if (biomeId === 'desierto' && !cell.isOasisRim && cellHash(x, y, 59) > 0.92) {
          tentList.push({ key, score: cellHash(x, y, 60) });
          continue;
        }

        const inCostaVillage = biomeId !== 'costa' || (x / (N - 1) < 0.44 && y / (N - 1) > 0.14 && y / (N - 1) < 0.86 && Math.hypot(x - 6, y - 3) >= 1.8);
        if (inCostaVillage && hVal > houseBaseThresh && cell.rawH < maxHouseH) {
          houseList.push({ key, score: cellHash(x, y, 107), x, y });
          continue;
        }

        if (cellHash(x, y, 37) > 0.92 && cell.rawH < maxHouseH) {
          lanternList.push({ key, score: cellHash(x, y, 38), x, y });
          continue;
        }

        if (cellHash(x, y, 19) > rockThresh && (cell.rawH > 1.35 || biomeId === 'desierto' || biomeId === 'atolon')) {
          rockList.push({ key, score: cellHash(x, y, 20), x, y });
          continue;
        }

        let isTreeEligible = false;
        if (biomeId === 'kioto') isTreeEligible = hVal > 0.28;
        else if (biomeId === 'jungla') isTreeEligible = hVal > 0.30 && !(x <= 5 && y <= 5 && Math.abs(x - y) <= 2);
        else if (biomeId === 'valle') isTreeEligible = hVal > 0.46 && cell.rawH < 2.65;
        else if (biomeId === 'costa') isTreeEligible = hVal > 0.62;
        else if (biomeId === 'desierto') isTreeEligible = cell.isOasisRim && hVal > 0.54;
        else if (biomeId === 'arrozales') isTreeEligible = !cell.isFloodedPaddy && hVal > 0.36;
        else if (biomeId === 'otono') isTreeEligible = hVal > 0.33;
        else if (biomeId === 'fiordo') isTreeEligible = hVal > 0.50 && cell.rawH < 2.60;
        else if (biomeId === 'atolon') isTreeEligible = hVal > 0.42;
        else if (biomeId === 'toscana') isTreeEligible = cell.isVineyard ? hVal > 0.58 : hVal > 0.52;

        if (isTreeEligible) {
          treeList.push({ key, score: cellHash(x, y, 201), x, y });
        }
      }
    }

    function toRankMap(arr, maxCount = Infinity, minDist = 0) {
      arr.sort((a, b) => a.score - b.score);
      let picked;
      if (minDist > 0) {
        picked = [];
        for (let i = 0; i < arr.length && picked.length < maxCount; i++) {
          const item = arr[i];
          const tooClose = picked.some(p => Math.hypot(p.x - item.x, p.y - item.y) < minDist);
          if (!tooClose) picked.push(item);
        }
      } else {
        picked = Number.isFinite(maxCount) ? arr.slice(0, maxCount) : arr;
      }
      const map = {};
      picked.forEach((item, idx) => {
        map[item.key] = idx + 1;
      });
      return { ranks: map, total: picked.length };
    }

    const treeCaps = {
      kioto: 28,
      jungla: 30,
      otono: 34,
      valle: 24,
      toscana: 24,
      fiordo: 16,
      arrozales: 18,
      costa: 10,
      desierto: 5
    };
    const treeMinDist = {
      kioto: 1.65,
      jungla: 1.48,
      valle: 1.45,
      arrozales: 1.45
    };
    const houseCaps = {
      costa: 11,
      toscana: 7,
      jungla: 5,
      atolon: 8,
      valle: 6,
      otono: 6,
      fiordo: 5,
      arrozales: 4,
      desierto: 3
    };
    const houseMinDist = {
      jungla: 2.2,
      kioto: 1.9,
      costa: 1.65
    };
    const rockCaps = {
      desierto: 3,
      arrozales: 3,
      toscana: 4,
      fiordo: 4,
      otono: 4,
      costa: 5,
      jungla: 5
    };
    const rockMinDist = {
      jungla: 2.0
    };
    const lanternCaps = {
      desierto: 3,
      arrozales: 4,
      toscana: 4,
      fiordo: 4,
      otono: 5,
      costa: 5,
      jungla: 4
    };
    const lanternMinDist = {
      jungla: 2.4
    };

    const treesObj = toRankMap(treeList, treeCaps[biomeId] || 36, treeMinDist[biomeId] || 0);
    const rocksObj = toRankMap(rockList, rockCaps[biomeId] || 10, rockMinDist[biomeId] || 0);
    const pathsObj = toRankMap(pathList, Infinity);
    const tentsObj = toRankMap(tentList, biomeId === 'desierto' ? 2 : 6);
    const housesObj = toRankMap(houseList, houseCaps[biomeId] || 10, houseMinDist[biomeId] || 0);
    const overwaterObj = toRankMap(overwaterList, 6);
    const lanternsObj = toRankMap(lanternList, lanternCaps[biomeId] || 8, lanternMinDist[biomeId] || 0);
    const shimmersObj = toRankMap(shimmerList, 12);

    const layout = {
      treeRanks: treesObj.ranks,
      treeTotal: treesObj.total,
      rockRanks: rocksObj.ranks,
      rockTotal: rocksObj.total,
      pathRanks: pathsObj.ranks,
      pathTotal: pathsObj.total,
      tentRanks: tentsObj.ranks,
      tentTotal: tentsObj.total,
      houseRanks: housesObj.ranks,
      houseTotal: housesObj.total,
      overwaterRanks: overwaterObj.ranks,
      overwaterTotal: overwaterObj.total,
      lanternRanks: lanternsObj.ranks,
      lanternTotal: lanternsObj.total,
      shimmerRanks: shimmersObj.ranks,
      shimmerTotal: shimmersObj.total,
      landmarkCell,
      toriiCell,
      secondToriiCell,
      costaWindmillCell,
      desertRuinsCell,
      toscanaFountains,
      secondaryCell
    };
    layoutCache[biomeId] = layout;
    return layout;
  }

  function getBudgetsForPoints(points, layout) {
    const p = Math.max(0, Math.min(MAX_POINTS, points || 0));

    const trees = p === 0 ? 0 : Math.min(layout.treeTotal, Math.max(1, Math.round((p / 74) * layout.treeTotal)));
    const rocks = p === 0 ? 0 : Math.min(layout.rockTotal, Math.round((p / 56) * layout.rockTotal));
    const paths = p < 18 ? 0 : Math.round(Math.min(1, (p - 18) / 24) * layout.pathTotal);
    const showBridge = p >= 32;
    const tents = p < 28 ? 0 : Math.min(layout.tentTotal, Math.round(Math.min(1, (p - 28) / 36) * layout.tentTotal));
    const houses = p < 36 ? 0 : Math.min(layout.houseTotal, Math.round(Math.min(1, (p - 36) / 36) * layout.houseTotal));
    const overwater = p < 30 ? 0 : Math.min(layout.overwaterTotal || 0, Math.round(Math.min(1, (p - 30) / 36) * (layout.overwaterTotal || 0)));
    const lanterns = p < 46 ? 0 : Math.min(layout.lanternTotal, Math.round(Math.min(1, (p - 46) / 30) * layout.lanternTotal));
    const boats = p >= 72 ? 2 : p >= 52 ? 1 : 0;
    const showMistAndPetals = p >= 56;
    const monumentPhase = p < 58 ? 0 : p < 64 ? 1 : p < 70 ? 2 : 3;
    const shimmers = p < 66 ? 0 : Math.min(layout.shimmerTotal, Math.round(Math.min(1, (p - 66) / 14) * layout.shimmerTotal));
    const showSecondary = p >= 74;
    const showLegendGlow = p >= 78;

    return {
      trees,
      rocks,
      paths,
      showBridge,
      tents,
      houses,
      overwater,
      lanterns,
      boats,
      showMistAndPetals,
      monumentPhase,
      shimmers,
      showSecondary,
      showLegendGlow
    };
  }

  function getBiomeSceneData(biomeId, points = 80) {
    const id = biomeId || 'costa';
    const pts = Math.max(0, Math.min(MAX_POINTS, points || 0));
    const layout = getBiomeLayout(id);
    const budgets = getBudgetsForPoints(pts, layout);
    const cells = [];
    for (let ix = 0; ix < N; ix++) {
      const col = [];
      for (let iy = 0; iy < N; iy++) {
        const key = `${ix},${iy}`;
        const showPathHere = Boolean(layout.pathRanks[key] && layout.pathRanks[key] <= budgets.paths);
        const cell = getCellData(id, ix, iy, pts, showPathHere);
        col.push({ ix, iy, key, ...cell });
      }
      cells.push(col);
    }
    return { N, biomeId: id, points: pts, layout, budgets, cells };
  }

  // Prisma 3D universal con selección automática de caras visibles mediante winding en pantalla
  function draw3DPrism(ctx, project, x0, x1, y0, y1, zBot, zTop, baseHex, isAwake, strokeJoin = true) {
    const t00 = project(x0, y0, zTop);
    const t10 = project(x1, y0, zTop);
    const t11 = project(x1, y1, zTop);
    const t01 = project(x0, y1, zTop);

    const b00 = project(x0, y0, zBot);
    const b10 = project(x1, y0, zBot);
    const b11 = project(x1, y1, zBot);
    const b01 = project(x0, y1, zBot);

    const faces = [
      { pts: [t10, b10, b11, t11], nx: 1, ny: 0, nz: 0 },
      { pts: [t01, b01, b00, t00], nx: -1, ny: 0, nz: 0 },
      { pts: [t11, b11, b01, t01], nx: 0, ny: 1, nz: 0 },
      { pts: [t00, b00, b10, t10], nx: 0, ny: -1, nz: 0 },
      { pts: [t00, t10, t11, t01], nx: 0, ny: 0, nz: 1 }
    ];

    for (let i = 0; i < faces.length; i++) {
      const f = faces[i];
      if (screenSignedArea(f.pts) > 0.02) {
        const col = lambertShade(baseHex, f.nx, f.ny, f.nz, isAwake);
        fillPoly(ctx, f.pts, col, strokeJoin ? col : null, 0.7);
      }
    }
  }

  function drawMicroRock3D(ctx, project, wx0, wy0, zBase, scale, hexColor, isAwake) {
    const h = 0.22 * scale;
    draw3DPrism(
      ctx,
      project,
      wx0 + 0.26,
      wx0 + 0.72,
      wy0 + 0.28,
      wy0 + 0.70,
      zBase,
      zBase + h,
      hexColor,
      isAwake,
      true
    );
  }

  function drawMicroPine3D(ctx, project, cx, cy, zBase, scale, baseHex, isAwake) {
    const r = 0.35 * scale;
    const h = 1.02 * scale;
    const apex = project(cx, cy, zBase + h);
    const c0 = project(cx - r, cy - r, zBase + 0.06);
    const c1 = project(cx + r, cy - r, zBase + 0.06);
    const c2 = project(cx + r, cy + r, zBase + 0.06);
    const c3 = project(cx - r, cy + r, zBase + 0.06);

    const sh = project(cx - 0.1, cy + 0.1, zBase + 0.02);
    ctx.fillStyle = 'rgba(28, 26, 23, 0.14)';
    ctx.beginPath();
    ctx.ellipse(sh.x, sh.y, 4.0 * scale, 2.0 * scale, 0, 0, Math.PI * 2);
    ctx.fill();

    const sides = [
      { pts: [apex, c0, c1], nx: 0, ny: -0.8, nz: 0.55 },
      { pts: [apex, c1, c2], nx: 0.8, ny: 0, nz: 0.55 },
      { pts: [apex, c2, c3], nx: 0, ny: 0.8, nz: 0.55 },
      { pts: [apex, c3, c0], nx: -0.8, ny: 0, nz: 0.55 }
    ];
    sides.forEach(s => {
      if (screenSignedArea(s.pts) > 0.01) {
        const col = lambertShade(baseHex, s.nx, s.ny, s.nz, isAwake);
        fillPoly(ctx, s.pts, col, col, 0.45);
      }
    });
  }

  function drawMicroCanopy3D(ctx, project, unitScale, cx, cy, zBase, rad, color, isAwake) {
    const base = project(cx, cy, zBase);
    const top = project(cx, cy, zBase + rad * 1.9);
    const pxR = Math.max(2.0, rad * unitScale);

    ctx.fillStyle = 'rgba(28, 26, 23, 0.12)';
    ctx.beginPath();
    ctx.ellipse(base.x - 1, base.y + 0.5, pxR * 0.9, pxR * 0.45, 0, 0, Math.PI * 2);
    ctx.fill();

    ctx.strokeStyle = '#6E5947';
    ctx.lineWidth = Math.max(1.2, pxR * 0.34);
    ctx.beginPath();
    ctx.moveTo(base.x, base.y);
    ctx.lineTo(top.x, top.y);
    ctx.stroke();

    ctx.fillStyle = lambertShade(color, 0, 0, 1, isAwake);
    ctx.beginPath();
    ctx.arc(top.x, top.y, pxR, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = shadeColor(color, 12, isAwake ? 8 : 0);
    ctx.beginPath();
    ctx.arc(top.x + pxR * 0.25, top.y - pxR * 0.25, pxR * 0.5, 0, Math.PI * 2);
    ctx.fill();
  }

  // Almendro rosa en flor (3 copas esponjosas superpuestas para Kioto)
  function drawBlossomTree3D(ctx, project, unitScale, cx, cy, zBase, scale, variant, isAwake) {
    const base = project(cx, cy, zBase);
    const top = project(cx, cy, zBase + 0.62 * scale);
    const pxR = Math.max(2.2, 0.29 * scale * unitScale);

    ctx.fillStyle = 'rgba(28, 26, 23, 0.13)';
    ctx.beginPath();
    ctx.ellipse(base.x - 1, base.y + 0.5, pxR * 1.15, pxR * 0.55, 0, 0, Math.PI * 2);
    ctx.fill();

    ctx.strokeStyle = '#634E42';
    ctx.lineWidth = Math.max(1.3, pxR * 0.36);
    ctx.beginPath();
    ctx.moveTo(base.x, base.y);
    ctx.lineTo(top.x, top.y);
    ctx.stroke();

    const palettes = [
      ['#E495AB', '#F7CED9', '#FDE6EC'],
      ['#DC89A1', '#EEB0C1', '#FBE0E8'],
      ['#E69EB3', '#F5C4D2', '#FFF0F4']
    ];
    const pal = palettes[variant % palettes.length];

    // Copa izquierda, derecha y superior esponjosas
    ctx.fillStyle = lambertShade(pal[0], -0.4, 0.3, 0.85, isAwake);
    ctx.beginPath();
    ctx.arc(top.x - pxR * 0.48, top.y + pxR * 0.14, pxR * 0.82, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = lambertShade(pal[1], 0.3, -0.2, 0.9, isAwake);
    ctx.beginPath();
    ctx.arc(top.x + pxR * 0.45, top.y + pxR * 0.08, pxR * 0.8, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = lambertShade(pal[1], 0, 0, 1, isAwake);
    ctx.beginPath();
    ctx.arc(top.x, top.y - pxR * 0.24, pxR * 0.96, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = pal[2];
    ctx.beginPath();
    ctx.arc(top.x + pxR * 0.22, top.y - pxR * 0.44, pxR * 0.44, 0, Math.PI * 2);
    ctx.fill();
  }

  // Palmera 3D con tronco curvo y 4 frondas radiantes (Oasis, Atolón y Jungla)
  function drawPalmTree3D(ctx, project, unitScale, cx, cy, zBase, scale, isAwake, frondHex = '#568F5A') {
    const base = project(cx, cy, zBase);
    const mid = project(cx + 0.1, cy - 0.05, zBase + 0.48 * scale);
    const crown = project(cx + 0.18, cy - 0.08, zBase + 0.92 * scale);

    ctx.strokeStyle = '#8B6C4E';
    ctx.lineWidth = Math.max(1.3, unitScale * 0.11 * scale);
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(base.x, base.y);
    ctx.quadraticCurveTo(mid.x, mid.y, crown.x, crown.y);
    ctx.stroke();

    const frondColor = lambertShade(frondHex, 0.1, -0.1, 0.95, isAwake);
    ctx.strokeStyle = frondColor;
    ctx.lineWidth = Math.max(1.4, unitScale * 0.10 * scale);
    const dirs = [
      [0.38, 0.12, -0.14],
      [-0.34, 0.22, -0.16],
      [0.14, -0.36, -0.14],
      [-0.18, -0.32, -0.12]
    ];
    dirs.forEach(d => {
      const tip = project(cx + 0.18 + d[0] * scale, cy - 0.08 + d[1] * scale, zBase + (0.92 + d[2]) * scale);
      ctx.beginPath();
      ctx.moveTo(crown.x, crown.y);
      ctx.lineTo(tip.x, tip.y);
      ctx.stroke();
    });
  }

  // Árbol emergente de selva tropical multi-capa
  function drawJungleTree3D(ctx, project, unitScale, cx, cy, zBase, scale, isAwake) {
    const base = project(cx, cy, zBase);
    const top1 = project(cx, cy, zBase + 0.65 * scale);
    const top2 = project(cx + 0.06, cy - 0.04, zBase + 0.94 * scale);
    const pxR = Math.max(2.4, 0.33 * scale * unitScale);

    ctx.strokeStyle = '#594738';
    ctx.lineWidth = Math.max(1.5, pxR * 0.32);
    ctx.beginPath();
    ctx.moveTo(base.x, base.y);
    ctx.lineTo(top2.x, top2.y);
    ctx.stroke();

    ctx.fillStyle = lambertShade('#1D4E30', -0.2, 0.2, 0.9, isAwake);
    ctx.beginPath();
    ctx.ellipse(top1.x, top1.y, pxR * 1.15, pxR * 0.68, 0, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = lambertShade('#29653F', 0, 0, 1, isAwake);
    ctx.beginPath();
    ctx.ellipse(top2.x, top2.y, pxR * 0.95, pxR * 0.56, 0, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = shadeColor('#3B7E52', 10, isAwake ? 8 : 0);
    ctx.beginPath();
    ctx.ellipse(top2.x + pxR * 0.18, top2.y - pxR * 0.12, pxR * 0.48, pxR * 0.28, 0, 0, Math.PI * 2);
    ctx.fill();
  }

  function drawLantern3D(ctx, project, unitScale, cx, cy, zBase, isAwake) {
    const base = project(cx, cy, zBase);
    const top = project(cx, cy, zBase + 0.42);
    ctx.strokeStyle = '#8C867B';
    ctx.lineWidth = Math.max(1.2, unitScale * 0.11);
    ctx.beginPath();
    ctx.moveTo(base.x, base.y);
    ctx.lineTo(top.x, top.y);
    ctx.stroke();

    ctx.fillStyle = isAwake ? '#FFD573' : '#F4C150';
    ctx.beginPath();
    ctx.arc(top.x, top.y, Math.max(1.5, unitScale * 0.12), 0, Math.PI * 2);
    ctx.fill();
  }

  // Ciprés mediterráneo alto y esbelto
  function drawCypress3D(ctx, project, cx, cy, zBase, scale, isAwake) {
    const r = 0.20 * scale;
    const h = 1.25 * scale;
    const apex = project(cx, cy, zBase + h);
    const c0 = project(cx - r, cy - r, zBase + 0.12);
    const c1 = project(cx + r, cy - r, zBase + 0.12);
    const c2 = project(cx + r, cy + r, zBase + 0.12);
    const c3 = project(cx - r, cy + r, zBase + 0.12);

    const sides = [
      { pts: [apex, c0, c1], nx: 0, ny: -0.8, nz: 0.45 },
      { pts: [apex, c1, c2], nx: 0.8, ny: 0, nz: 0.45 },
      { pts: [apex, c2, c3], nx: 0, ny: 0.8, nz: 0.45 },
      { pts: [apex, c3, c0], nx: -0.8, ny: 0, nz: 0.45 }
    ];
    sides.forEach(s => {
      if (screenSignedArea(s.pts) > 0.01) {
        const col = lambertShade('#3E6242', s.nx, s.ny, s.nz, isAwake);
        fillPoly(ctx, s.pts, col, col, 0.45);
      }
    });
  }

  // Arbusto de buganvilla en flor para acantilados de Costa
  function drawBougainvillea3D(ctx, project, unitScale, cx, cy, zBase, isAwake) {
    const base = project(cx, cy, zBase + 0.18);
    const pxR = Math.max(1.8, 0.22 * unitScale);
    ctx.fillStyle = lambertShade('#D45D86', 0, 0, 1, isAwake);
    ctx.beginPath();
    ctx.arc(base.x, base.y, pxR, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = '#E57EA0';
    ctx.beginPath();
    ctx.arc(base.x + pxR * 0.28, base.y - pxR * 0.25, pxR * 0.56, 0, Math.PI * 2);
    ctx.fill();
  }

  // Molino mediterráneo blanco en lo alto del acantilado (Costa Nv.8)
  function drawWindmill3D(ctx, project, unitScale, wx0, wy0, zBase, isAwake) {
    drawArchBlock3D(ctx, project, wx0, wy0, zBase, 0.64, 0.95, '#FFFFFF', isAwake);
    drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.95, 0.68, 0.26, '#3574B5', isAwake);
    const hub = project(wx0 + 0.82, wy0 + 0.5, zBase + 0.82);
    const arm = Math.max(4.5, unitScale * 0.44);
    ctx.strokeStyle = '#8F6A48';
    ctx.lineWidth = Math.max(1.4, unitScale * 0.11);
    ctx.lineCap = 'round';
    [0.45, 2.02, 3.59, 5.16].forEach(ang => {
      ctx.beginPath();
      ctx.moveTo(hub.x, hub.y);
      ctx.lineTo(hub.x + Math.cos(ang) * arm, hub.y + Math.sin(ang) * arm);
      ctx.stroke();
    });
  }

  // Tienda nómada con dosel terracota o índigo (Oasis Nv.4+)
  function drawDesertTent3D(ctx, project, wx0, wy0, zBase, variant, isAwake) {
    const tentHex = variant % 2 === 0 ? '#C6583A' : '#2F6878';
    const r = 0.38;
    const cx = wx0 + 0.5;
    const cy = wy0 + 0.5;
    const apex = project(cx, cy, zBase + 0.62);
    const c0 = project(cx - r, cy - r, zBase + 0.04);
    const c1 = project(cx + r, cy - r, zBase + 0.04);
    const c2 = project(cx + r, cy + r, zBase + 0.04);
    const c3 = project(cx - r, cy + r, zBase + 0.04);

    const sides = [
      { pts: [apex, c0, c1], nx: 0, ny: -0.75, nz: 0.65 },
      { pts: [apex, c1, c2], nx: 0.75, ny: 0, nz: 0.65 },
      { pts: [apex, c2, c3], nx: 0, ny: 0.75, nz: 0.65 },
      { pts: [apex, c3, c0], nx: -0.75, ny: 0, nz: 0.65 }
    ];
    sides.forEach(s => {
      if (screenSignedArea(s.pts) > 0.01) {
        const col = lambertShade(tentHex, s.nx, s.ny, s.nz, isAwake);
        fillPoly(ctx, s.pts, col, col, 0.5);
      }
    });
  }

  // Ruinas y columnata de arenisca en lo alto (Oasis Nv.8)
  function drawDesertRuins3D(ctx, project, wx0, wy0, zBase, isAwake) {
    drawArchBlock3D(ctx, project, wx0, wy0, zBase, 0.84, 0.16, '#DECDB5', isAwake);
    draw3DPrism(ctx, project, wx0 + 0.14, wx0 + 0.34, wy0 + 0.20, wy0 + 0.40, zBase + 0.16, zBase + 0.88, '#E9CFA9', isAwake, true);
    draw3DPrism(ctx, project, wx0 + 0.64, wx0 + 0.84, wy0 + 0.58, wy0 + 0.78, zBase + 0.16, zBase + 0.88, '#E9CFA9', isAwake, true);
    draw3DPrism(ctx, project, wx0 + 0.12, wx0 + 0.86, wy0 + 0.18, wy0 + 0.80, zBase + 0.88, zBase + 1.06, '#D5BA92', isAwake, true);
  }

  // Tallos de bambú esbeltos con penacho de hojas para Terrazas de Bali
  function drawBamboo3D(ctx, project, unitScale, cx, cy, zBase, scale, isAwake) {
    const offsets = [
      { dx: -0.14, dy: -0.06, h: 1.05 * scale },
      { dx: 0.12, dy: 0.08, h: 1.24 * scale },
      { dx: -0.02, dy: 0.15, h: 0.90 * scale }
    ];
    const lw = Math.max(1.3, unitScale * 0.10);
    offsets.forEach((st, idx) => {
      const p0 = project(cx + st.dx, cy + st.dy, zBase);
      const p1 = project(cx + st.dx * 1.15, cy + st.dy * 1.15, zBase + st.h);
      ctx.strokeStyle = lambertShade(idx % 2 === 0 ? '#74B25C' : '#5E9E4B', 0.1, -0.1, 0.9, isAwake);
      ctx.lineWidth = lw;
      ctx.lineCap = 'round';
      ctx.beginPath();
      ctx.moveTo(p0.x, p0.y);
      ctx.lineTo(p1.x, p1.y);
      ctx.stroke();

      ctx.fillStyle = lambertShade('#8CCF72', 0, 0, 1, isAwake);
      ctx.beginPath();
      ctx.arc(p1.x, p1.y, Math.max(1.6, 0.14 * unitScale * scale), 0, Math.PI * 2);
      ctx.fill();
    });
  }

  // Árbol frangipani tropical en flor para Bali
  function drawFrangipani3D(ctx, project, unitScale, cx, cy, zBase, scale, isAwake) {
    const trunkB = project(cx, cy, zBase);
    const trunkT = project(cx, cy, zBase + 0.44 * scale);
    ctx.strokeStyle = '#7C5D43';
    ctx.lineWidth = Math.max(1.5, unitScale * 0.12);
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(trunkB.x, trunkB.y);
    ctx.lineTo(trunkT.x, trunkT.y);
    ctx.stroke();

    const r = Math.max(2.6, 0.30 * unitScale * scale);
    ctx.fillStyle = lambertShade('#5EA152', 0.05, -0.05, 0.95, isAwake);
    ctx.beginPath();
    ctx.arc(trunkT.x, trunkT.y, r, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = '#FCF9EE';
    ctx.beginPath();
    ctx.arc(trunkT.x - r * 0.32, trunkT.y - r * 0.18, r * 0.36, 0, Math.PI * 2);
    ctx.arc(trunkT.x + r * 0.34, trunkT.y - r * 0.08, r * 0.30, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = '#F3CF55';
    ctx.beginPath();
    ctx.arc(trunkT.x - r * 0.32, trunkT.y - r * 0.18, r * 0.14, 0, Math.PI * 2);
    ctx.fill();
  }

  // Arce otoñal rojo / naranja / amarillo vivo para Bosque de Otoño
  function drawMapleTree3D(ctx, project, unitScale, cx, cy, zBase, variant, isAwake) {
    const palette = ['#D9342B', '#F27B24', '#F8C73A', '#E84F32', '#F5A623'];
    const leafHex = palette[Math.abs(variant) % palette.length];
    const scale = 0.88 + ((variant % 3) * 0.11);
    const b = project(cx, cy, zBase);
    const t = project(cx, cy, zBase + 0.46 * scale);
    ctx.strokeStyle = '#6A4C38';
    ctx.lineWidth = Math.max(1.4, unitScale * 0.11);
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(b.x, b.y);
    ctx.lineTo(t.x, t.y);
    ctx.stroke();

    const r = Math.max(2.6, 0.33 * unitScale * scale);
    ctx.fillStyle = lambertShade(leafHex, 0, 0, 1, isAwake);
    ctx.beginPath();
    ctx.arc(t.x, t.y, r, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = shadeColor(leafHex, 14, isAwake ? 8 : 0);
    ctx.beginPath();
    ctx.arc(t.x - r * 0.25, t.y - r * 0.28, r * 0.56, 0, Math.PI * 2);
    ctx.fill();
  }

  // Abeto nórdico con copa nevada (en Fiordo usa base azul hielo sin verde)
  function drawSnowPine3D(ctx, project, cx, cy, zBase, scale, isAwake, baseHex = '#5B7B8F') {
    drawMicroPine3D(ctx, project, cx, cy, zBase, scale, baseHex, isAwake);
    const r = 0.26 * scale;
    const snowBase = zBase + 0.48 * scale;
    const apex = project(cx, cy, zBase + 1.18 * scale);
    const c0 = project(cx - r, cy - r, snowBase);
    const c1 = project(cx + r, cy - r, snowBase);
    const c2 = project(cx + r, cy + r, snowBase);
    const c3 = project(cx - r, cy + r, snowBase);
    [
      { pts: [apex, c0, c1], nx: 0, ny: -0.75, nz: 0.65 },
      { pts: [apex, c1, c2], nx: 0.75, ny: 0, nz: 0.65 },
      { pts: [apex, c2, c3], nx: 0, ny: 0.75, nz: 0.65 },
      { pts: [apex, c3, c0], nx: -0.75, ny: 0, nz: 0.65 }
    ].forEach(s => {
      if (screenSignedArea(s.pts) > 0.01) {
        const col = lambertShade('#F2F7FB', s.nx, s.ny, s.nz, isAwake);
        fillPoly(ctx, s.pts, col, '#DCE8F2', 0.45);
      }
    });
  }

  // Hibisco coral en flor para islas de Atolón
  function drawHibiscus3D(ctx, project, unitScale, cx, cy, zBase, isAwake) {
    const p = project(cx, cy, zBase + 0.16);
    const r = Math.max(2.0, 0.24 * unitScale);
    ctx.fillStyle = lambertShade('#3F8D59', 0, 0, 1, isAwake);
    ctx.beginPath();
    ctx.arc(p.x, p.y, r, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = '#EE7A74';
    ctx.beginPath();
    ctx.arc(p.x + r * 0.28, p.y - r * 0.22, r * 0.44, 0, Math.PI * 2);
    ctx.arc(p.x - r * 0.26, p.y - r * 0.12, r * 0.34, 0, Math.PI * 2);
    ctx.fill();
  }

  // Palafito sobre la laguna turquesa del Atolón de Coral
  function drawOverwaterBungalow3D(ctx, project, wx0, wy0, zWater, isAwake) {
    draw3DPrism(ctx, project, wx0 + 0.24, wx0 + 0.76, wy0 + 0.24, wy0 + 0.76, zWater, zWater + 0.22, '#6A4C35', isAwake, true);
    draw3DPrism(ctx, project, wx0 + 0.14, wx0 + 0.86, wy0 + 0.14, wy0 + 0.86, zWater + 0.22, zWater + 0.30, '#DFC8A4', isAwake, true);
    drawArchBlock3D(ctx, project, wx0, wy0, zWater + 0.30, 0.56, 0.36, '#F4EAD6', isAwake);
    const apex = project(wx0 + 0.5, wy0 + 0.5, zWater + 1.02);
    const r = 0.42;
    const c0 = project(wx0 + 0.5 - r, wy0 + 0.5 - r, zWater + 0.65);
    const c1 = project(wx0 + 0.5 + r, wy0 + 0.5 - r, zWater + 0.65);
    const c2 = project(wx0 + 0.5 + r, wy0 + 0.5 + r, zWater + 0.65);
    const c3 = project(wx0 + 0.5 - r, wy0 + 0.5 + r, zWater + 0.65);
    [
      { pts: [apex, c0, c1], nx: 0, ny: -0.75, nz: 0.65 },
      { pts: [apex, c1, c2], nx: 0.75, ny: 0, nz: 0.65 },
      { pts: [apex, c2, c3], nx: 0, ny: 0.75, nz: 0.65 },
      { pts: [apex, c3, c0], nx: -0.75, ny: 0, nz: 0.65 }
    ].forEach(s => {
      if (screenSignedArea(s.pts) > 0.01) {
        const col = lambertShade('#CBA464', s.nx, s.ny, s.nz, isAwake);
        fillPoly(ctx, s.pts, col, col, 0.45);
      }
    });
  }

  // Hilera de viñedo toscano con racimos púrpuras
  function drawVineyardTuft3D(ctx, project, unitScale, cx, cy, zBase, isAwake) {
    draw3DPrism(ctx, project, cx - 0.30, cx + 0.30, cy - 0.11, cy + 0.11, zBase, zBase + 0.24, '#6E8F45', isAwake, true);
    const p = project(cx + 0.1, cy, zBase + 0.26);
    ctx.fillStyle = '#7D4B82';
    ctx.beginPath();
    ctx.arc(p.x, p.y, Math.max(1.2, unitScale * 0.09), 0, Math.PI * 2);
    ctx.fill();
  }

  // Fuente romana clásica de travertino para Toscana
  function drawTuscanFountain3D(ctx, project, unitScale, wx0, wy0, zBase, isAwake) {
    const cx = wx0 + 0.5;
    const cy = wy0 + 0.5;
    drawArchBlock3D(ctx, project, wx0, wy0, zBase, 0.86, 0.22, '#EFE7D8', isAwake);
    drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.22, 0.64, 0.10, '#63B8D4', isAwake);
    drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.32, 0.24, 0.42, '#E5DCCB', isAwake);
    drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.74, 0.44, 0.12, '#EFE7D8', isAwake);
    const jet = project(cx, cy, zBase + 0.94);
    ctx.fillStyle = '#A8E4F5';
    ctx.beginPath();
    ctx.arc(jet.x, jet.y, Math.max(1.6, unitScale * 0.13), 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = '#FFFFFF';
    ctx.beginPath();
    ctx.arc(jet.x, jet.y - unitScale * 0.04, Math.max(1.0, unitScale * 0.06), 0, Math.PI * 2);
    ctx.fill();
  }

  // Monumento secundario Nv.8 para los biomas nuevos
  function drawSecondaryLandmark3D(ctx, project, unitScale, biomeId, wx0, wy0, zBase, isAwake) {
    const cx = wx0 + 0.5;
    const cy = wy0 + 0.5;
    if (biomeId === 'arrozales') {
      // Puerta partida balinesa Candi Bentar
      draw3DPrism(ctx, project, wx0 + 0.12, wx0 + 0.88, wy0 + 0.16, wy0 + 0.84, zBase, zBase + 0.16, '#8D877E', isAwake, true);
      draw3DPrism(ctx, project, wx0 + 0.16, wx0 + 0.40, wy0 + 0.26, wy0 + 0.56, zBase + 0.16, zBase + 0.92, '#777168', isAwake, true);
      draw3DPrism(ctx, project, wx0 + 0.60, wx0 + 0.84, wy0 + 0.44, wy0 + 0.74, zBase + 0.16, zBase + 0.92, '#777168', isAwake, true);
      draw3DPrism(ctx, project, wx0 + 0.20, wx0 + 0.36, wy0 + 0.30, wy0 + 0.52, zBase + 0.92, zBase + 1.12, '#5D5750', isAwake, true);
      draw3DPrism(ctx, project, wx0 + 0.64, wx0 + 0.80, wy0 + 0.48, wy0 + 0.70, zBase + 0.92, zBase + 1.12, '#5D5750', isAwake, true);
    } else if (biomeId === 'otono') {
      // Mirador de madera con tejado carmesí junto al lago
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, 0.72, 0.18, '#8E7D6F', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.18, 0.52, 0.46, '#7C5438', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.64, 0.84, 0.24, '#C95D4B', isAwake);
    } else if (biomeId === 'fiordo') {
      // Faro y baliza rúnica del fiordo
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, 0.74, 0.28, '#667380', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.28, 0.46, 0.88, '#E9F0F5', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + 1.16, 0.52, 0.22, '#B6463B', isAwake);
      const glow = project(cx, cy, zBase + 1.50);
      ctx.fillStyle = '#FFE082';
      ctx.beginPath();
      ctx.arc(glow.x, glow.y, Math.max(2.0, unitScale * 0.17), 0, Math.PI * 2);
      ctx.fill();
    } else if (biomeId === 'atolon') {
      // Pérgola de coral y embarcadero del este
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, 0.80, 0.16, '#E8D8BE', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.16, 0.56, 0.44, '#B08458', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.60, 0.82, 0.26, '#D4AE6A', isAwake);
    } else if (biomeId === 'toscana') {
      // Fuente romana de travertino
      drawTuscanFountain3D(ctx, project, unitScale, wx0, wy0, zBase, isAwake);
    }
  }

  function drawArchBlock3D(ctx, project, wx0, wy0, zBase, frac, heightUnits, wallHex, isAwake) {
    const pad = (1 - frac) * 0.5;
    draw3DPrism(
      ctx,
      project,
      wx0 + pad,
      wx0 + 1 - pad,
      wy0 + pad,
      wy0 + 1 - pad,
      zBase,
      zBase + heightUnits,
      wallHex,
      isAwake,
      true
    );
  }

  function drawSettlementHouse3D(ctx, project, biomeId, wx0, wy0, zBase, frac, hUnits, isAwake) {
    if (biomeId === 'kioto') {
      // Casa de té tradicional oscura
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, frac, hUnits * 0.72, '#E8E2D5', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + hUnits * 0.72, frac + 0.12, hUnits * 0.30, '#46433F', isAwake);
    } else if (biomeId === 'jungla') {
      // Palafito tropical de madera sobre pilotes
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, 0.34, 0.24, '#6E5138', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.24, frac, hUnits * 0.68, '#A9825A', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.24 + hUnits * 0.68, frac + 0.10, 0.20, '#8A6744', isAwake);
    } else if (biomeId === 'desierto') {
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, frac, hUnits, '#E8D8BE', isAwake);
    } else if (biomeId === 'valle') {
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, frac, hUnits * 0.75, '#F5F2EB', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + hUnits * 0.75, frac + 0.06, 0.22, '#B58965', isAwake);
    } else if (biomeId === 'arrozales') {
      // Bale balinés de madera de teca y paja oscura
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, frac, 0.14, '#8A847B', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.14, frac * 0.86, hUnits * 0.58, '#A97F54', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.14 + hUnits * 0.58, frac + 0.10, 0.24, '#4B463F', isAwake);
    } else if (biomeId === 'otono') {
      // Cabaña de cedro con tejado ocre-carmesí
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, frac, hUnits * 0.72, '#EFE6D8', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + hUnits * 0.72, frac + 0.08, 0.26, '#B85D3E', isAwake);
    } else if (biomeId === 'fiordo') {
      // Cabaña rorbu noruega roja o albero con tejado nevado
      const isOchre = cellHash(Math.round(wx0 + 10), Math.round(wy0 + 10), 53) > 0.68;
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, frac, hUnits * 0.72, isOchre ? '#D49442' : '#A94235', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + hUnits * 0.72, frac + 0.08, 0.22, '#E9EFF4', isAwake);
    } else if (biomeId === 'atolon') {
      // Choza polinesia playera de paja dorada
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, frac, hUnits * 0.66, '#F2E7D2', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + hUnits * 0.66, frac + 0.12, 0.26, '#CDA462', isAwake);
    } else if (biomeId === 'toscana') {
      // Villa toscana de estuco crema y teja terracota
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, frac, hUnits * 0.76, '#F2E5C9', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + hUnits * 0.76, frac + 0.08, 0.20, '#C96D4B', isAwake);
    } else {
      // Costa mediterránea: casas crema con techos crema, cúpulas azul cobalto o tejados terracota
      const styleRoll = cellHash(Math.round(wx0 + 10), Math.round(wy0 + 10), 47);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, frac, hUnits, '#F2E2C4', isAwake);
      if (styleRoll > 0.68) {
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + hUnits, frac * 0.72, 0.22, '#3574B5', isAwake);
      } else if (styleRoll > 0.36) {
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + hUnits, frac + 0.04, 0.14, '#C96D4B', isAwake);
      }
    }
  }

  function drawBridge3D(ctx, project, biomeId, wx0, wy0, zWater, isAwake) {
    if (biomeId === 'kioto') {
      // Puente arqueado bermellón taiko-bashi
      draw3DPrism(ctx, project, wx0 + 0.12, wx0 + 0.88, wy0 - 0.1, wy0 + 1.1, zWater, zWater + 0.24, '#C95D4B', isAwake, true);
      draw3DPrism(ctx, project, wx0 + 0.22, wx0 + 0.78, wy0 + 0.12, wy0 + 0.88, zWater + 0.24, zWater + 0.38, '#D66E5C', isAwake, true);
    } else if (biomeId === 'jungla') {
      // Puente colgante tropical
      draw3DPrism(ctx, project, wx0 + 0.15, wx0 + 0.85, wy0 - 0.12, wy0 + 1.12, zWater + 0.14, zWater + 0.28, '#9C7853', isAwake, true);
    } else if (biomeId === 'costa') {
      // Muelle de madera en la cala
      draw3DPrism(ctx, project, wx0 - 0.2, wx0 + 1.1, wy0 + 0.26, wy0 + 0.74, zWater, zWater + 0.20, '#B7926A', isAwake, true);
    } else if (biomeId === 'desierto') {
      // Piedras de paso en el oasis
      draw3DPrism(ctx, project, wx0 + 0.25, wx0 + 0.75, wy0 + 0.22, wy0 + 0.78, zWater, zWater + 0.14, '#E8CCA0', isAwake, true);
    } else if (biomeId === 'arrozales') {
      // Puente de bambú sobre el estanque sagrado
      draw3DPrism(ctx, project, wx0 + 0.16, wx0 + 0.84, wy0 - 0.08, wy0 + 1.08, zWater + 0.08, zWater + 0.24, '#C2A373', isAwake, true);
    } else if (biomeId === 'otono') {
      // Puente arqueado de madera de cedro
      draw3DPrism(ctx, project, wx0 + 0.14, wx0 + 0.86, wy0 - 0.10, wy0 + 1.10, zWater, zWater + 0.24, '#8E6242', isAwake, true);
      draw3DPrism(ctx, project, wx0 + 0.22, wx0 + 0.78, wy0 + 0.16, wy0 + 0.84, zWater + 0.24, zWater + 0.34, '#A67550', isAwake, true);
    } else if (biomeId === 'fiordo') {
      // Pasarela robusta de madera oscura sobre el fiordo
      draw3DPrism(ctx, project, wx0 + 0.16, wx0 + 0.84, wy0 - 0.12, wy0 + 1.12, zWater + 0.10, zWater + 0.28, '#66584D', isAwake, true);
    } else if (biomeId === 'atolon') {
      // Embarcadero de madera blanqueada sobre la laguna
      draw3DPrism(ctx, project, wx0 - 0.18, wx0 + 1.12, wy0 + 0.24, wy0 + 0.76, zWater + 0.10, zWater + 0.24, '#DFC8A4', isAwake, true);
    } else if (biomeId === 'toscana') {
      // Puente romano de piedra travertino
      draw3DPrism(ctx, project, wx0 + 0.14, wx0 + 0.86, wy0 - 0.10, wy0 + 1.10, zWater, zWater + 0.26, '#EAE1D0', isAwake, true);
      draw3DPrism(ctx, project, wx0 + 0.22, wx0 + 0.78, wy0 + 0.10, wy0 + 0.90, zWater + 0.26, zWater + 0.36, '#C96D4B', isAwake, true);
    } else {
      // Valle: puente de madera cruzando el río alpino
      draw3DPrism(ctx, project, wx0 - 0.18, wx0 + 1.18, wy0 + 0.20, wy0 + 0.80, zWater, zWater + 0.24, '#B58B65', isAwake, true);
    }
  }

  function drawBoat3D(ctx, project, biomeId, wx0, wy0, zWater, isAwake) {
    if (biomeId === 'costa' || biomeId === 'atolon') {
      const hullHex = biomeId === 'atolon' ? '#4CC7C4' : '#F5F2EB';
      draw3DPrism(ctx, project, wx0 + 0.26, wx0 + 0.74, wy0 + 0.34, wy0 + 0.66, zWater, zWater + 0.14, hullHex, isAwake, true);
      const mB = project(wx0 + 0.5, wy0 + 0.5, zWater + 0.14);
      const mT = project(wx0 + 0.5, wy0 + 0.5, zWater + 0.78);
      const mS = project(wx0 + 0.74, wy0 + 0.5, zWater + 0.22);
      fillPoly(ctx, [mT, mB, mS], '#FFFFFF', '#E5E0D5', 0.4);
    } else if (biomeId === 'fiordo') {
      // Drakkar nórdico de casco oscuro y vela carmesí
      draw3DPrism(ctx, project, wx0 + 0.22, wx0 + 0.78, wy0 + 0.36, wy0 + 0.64, zWater, zWater + 0.15, '#594435', isAwake, true);
      const mB = project(wx0 + 0.5, wy0 + 0.5, zWater + 0.15);
      const mT = project(wx0 + 0.5, wy0 + 0.5, zWater + 0.74);
      const mS = project(wx0 + 0.72, wy0 + 0.5, zWater + 0.24);
      fillPoly(ctx, [mT, mB, mS], '#B6463B', '#EFE7DC', 0.45);
    } else if (biomeId === 'arrozales') {
      // Jukung balinés con vela blanca
      draw3DPrism(ctx, project, wx0 + 0.24, wx0 + 0.76, wy0 + 0.38, wy0 + 0.62, zWater, zWater + 0.12, '#C8B08E', isAwake, true);
      const mB = project(wx0 + 0.5, wy0 + 0.5, zWater + 0.12);
      const mT = project(wx0 + 0.5, wy0 + 0.5, zWater + 0.64);
      const mS = project(wx0 + 0.70, wy0 + 0.5, zWater + 0.20);
      fillPoly(ctx, [mT, mB, mS], '#FAF7F0', '#DDD5C4', 0.4);
    } else {
      // Canoa / barca de madera
      draw3DPrism(ctx, project, wx0 + 0.24, wx0 + 0.76, wy0 + 0.38, wy0 + 0.62, zWater, zWater + 0.12, '#8F6A48', isAwake, true);
    }
  }

  // Gran barco nórdico navegando el fiordo (eslora de ~2.1 celdas)
  function drawLargeFjordShip3D(ctx, project, unitScale, wx0, wy0, zWater, isAwake) {
    // Casco principal de roble oscuro
    draw3DPrism(ctx, project, wx0 - 0.55, wx0 + 1.55, wy0 + 0.14, wy0 + 0.86, zWater, zWater + 0.26, '#5A402E', isAwake, true);
    // Proa y popa elevadas
    draw3DPrism(ctx, project, wx0 - 0.55, wx0 - 0.18, wy0 + 0.22, wy0 + 0.78, zWater + 0.26, zWater + 0.52, '#71513A', isAwake, true);
    draw3DPrism(ctx, project, wx0 + 1.18, wx0 + 1.55, wy0 + 0.22, wy0 + 0.78, zWater + 0.26, zWater + 0.48, '#71513A', isAwake, true);
    // Cubierta de madera clara y cabina nórdica roja con techo nevado
    draw3DPrism(ctx, project, wx0 - 0.14, wx0 + 1.14, wy0 + 0.22, wy0 + 0.78, zWater + 0.26, zWater + 0.34, '#CDB99C', isAwake, true);
    draw3DPrism(ctx, project, wx0 - 0.10, wx0 + 0.32, wy0 + 0.28, wy0 + 0.72, zWater + 0.34, zWater + 0.64, '#A83F33', isAwake, true);
    draw3DPrism(ctx, project, wx0 - 0.12, wx0 + 0.34, wy0 + 0.26, wy0 + 0.74, zWater + 0.64, zWater + 0.74, '#EEF4F8', isAwake, true);

    // Mástil y verga
    const mBase = project(wx0 + 0.62, wy0 + 0.50, zWater + 0.34);
    const mTop = project(wx0 + 0.62, wy0 + 0.50, zWater + 1.92);
    ctx.strokeStyle = '#4A3425';
    ctx.lineWidth = Math.max(1.8, unitScale * 0.12);
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(mBase.x, mBase.y);
    ctx.lineTo(mTop.x, mTop.y);
    ctx.stroke();

    const yL = project(wx0 + 0.62, wy0 + 0.04, zWater + 1.66);
    const yR = project(wx0 + 0.62, wy0 + 0.96, zWater + 1.66);
    ctx.lineWidth = Math.max(1.4, unitScale * 0.09);
    ctx.beginPath();
    ctx.moveTo(yL.x, yL.y);
    ctx.lineTo(yR.x, yR.y);
    ctx.stroke();

    // Gran vela cuadrada con franja central carmesí
    const sTL = project(wx0 + 0.62, wy0 + 0.08, zWater + 1.66);
    const sTR = project(wx0 + 0.62, wy0 + 0.92, zWater + 1.66);
    const sBR = project(wx0 + 0.76, wy0 + 0.90, zWater + 0.62);
    const sBL = project(wx0 + 0.76, wy0 + 0.10, zWater + 0.62);
    fillPoly(ctx, [sTL, sTR, sBR, sBL], '#FAF6EE', '#DDD5C7', 0.45);

    const stTL = project(wx0 + 0.62, wy0 + 0.36, zWater + 1.66);
    const stTR = project(wx0 + 0.62, wy0 + 0.64, zWater + 1.66);
    const stBR = project(wx0 + 0.76, wy0 + 0.62, zWater + 0.62);
    const stBL = project(wx0 + 0.76, wy0 + 0.38, zWater + 0.62);
    fillPoly(ctx, [stTL, stTR, stBR, stBL], '#B84438', null);

    // Banderín en la cima del mástil
    const fTip = project(wx0 + 0.92, wy0 + 0.50, zWater + 1.84);
    const fLow = project(wx0 + 0.62, wy0 + 0.50, zWater + 1.76);
    fillPoly(ctx, [mTop, fTip, fLow], '#B84438', null);
  }

  function drawLandmark3D(ctx, project, unitScale, biomeId, wx0, wy0, zBase, monumentPhase, showLegendGlow, isAwake) {
    const cx = wx0 + 0.5;
    const cy = wy0 + 0.5;

    if (biomeId === 'valle') {
      if (monumentPhase === 1) {
        drawArchBlock3D(ctx, project, wx0, wy0, zBase, 0.86, 0.24, '#D5CFC4', isAwake);
        return;
      }
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, 0.82, 0.88, '#F5F2EB', isAwake);
      if (monumentPhase >= 3) {
        const r = 0.46;
        const apex = project(cx, cy, zBase + 1.44);
        const c0 = project(cx - r, cy - r, zBase + 0.86);
        const c1 = project(cx + r, cy - r, zBase + 0.86);
        const c2 = project(cx + r, cy + r, zBase + 0.86);
        const c3 = project(cx - r, cy + r, zBase + 0.86);
        const roofSides = [
          { pts: [apex, c0, c1], nx: 0, ny: -0.75, nz: 0.65 },
          { pts: [apex, c1, c2], nx: 0.75, ny: 0, nz: 0.65 },
          { pts: [apex, c2, c3], nx: 0, ny: 0.75, nz: 0.65 },
          { pts: [apex, c3, c0], nx: -0.75, ny: 0, nz: 0.65 }
        ];
        roofSides.forEach(s => {
          if (screenSignedArea(s.pts) > 0.01) {
            const col = lambertShade('#B8896B', s.nx, s.ny, s.nz, isAwake);
            fillPoly(ctx, s.pts, col, col, 0.5);
          }
        });
        if (showLegendGlow) {
          const glow = project(cx + 0.15, cy - 0.1, zBase + 1.54);
          ctx.fillStyle = '#FFD573';
          ctx.beginPath();
          ctx.arc(glow.x, glow.y, Math.max(1.8, unitScale * 0.14), 0, Math.PI * 2);
          ctx.fill();
        }
      }
    } else if (biomeId === 'costa') {
      // Faro protagonista mediterráneo en tonos crema cálido
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, 0.84, 0.44, '#E4CFA9', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.44, 0.54, 0.68, '#F5E9D3', isAwake);
      if (monumentPhase >= 2) {
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + 1.12, 0.50, 0.88, '#F5E9D3', isAwake);
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + 1.38, 0.52, 0.18, '#D97757', isAwake);
      }
      if (monumentPhase >= 3) {
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + 2.00, 0.58, 0.24, '#D97757', isAwake);
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + 2.24, 0.42, 0.18, '#FFF4C4', isAwake);
        const top = project(cx, cy, zBase + 2.52);
        ctx.fillStyle = 'rgba(255, 224, 130, 0.38)';
        ctx.beginPath();
        ctx.arc(top.x, top.y, Math.max(3.4, unitScale * (showLegendGlow ? 0.36 : 0.28)), 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = '#FFD573';
        ctx.beginPath();
        ctx.arc(top.x, top.y, Math.max(2.2, unitScale * (showLegendGlow ? 0.22 : 0.17)), 0, Math.PI * 2);
        ctx.fill();
      }
    } else if (biomeId === 'kioto') {
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, 0.84, 0.36, '#E8E2D5', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.36, 0.94, 0.16, '#3D3A37', isAwake);
      if (monumentPhase >= 2) {
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.52, 0.62, 0.34, '#E8E2D5', isAwake);
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.86, 0.74, 0.15, '#3D3A37', isAwake);
      }
      if (monumentPhase >= 3) {
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + 1.01, 0.42, 0.28, '#C95D4B', isAwake);
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + 1.29, 0.52, 0.14, '#3D3A37', isAwake);
        const spireB = project(cx, cy, zBase + 1.43);
        const spireT = project(cx, cy, zBase + 1.82);
        ctx.strokeStyle = '#E5B24C';
        ctx.lineWidth = Math.max(1.5, unitScale * 0.11);
        ctx.beginPath();
        ctx.moveTo(spireB.x, spireB.y);
        ctx.lineTo(spireT.x, spireT.y);
        ctx.stroke();
      }
    } else if (biomeId === 'jungla') {
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, 0.92, 0.42, '#6E756B', isAwake);
      if (monumentPhase >= 2) {
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.42, 0.68, 0.42, '#838A7F', isAwake);
      }
      if (monumentPhase >= 3) {
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.84, 0.44, 0.36, '#5C735B', isAwake);
        if (showLegendGlow) {
          const flame = project(cx, cy, zBase + 1.32);
          ctx.fillStyle = '#FFB74D';
          ctx.beginPath();
          ctx.arc(flame.x, flame.y, Math.max(2, unitScale * 0.15), 0, Math.PI * 2);
          ctx.fill();
        }
      }
    } else if (biomeId === 'arrozales') {
      // Templo Meru balinés de múltiples tejados oscuros sobre el agua
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, 0.86, 0.34, '#8A847B', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.34, 0.92, 0.18, '#3B3834', isAwake);
      if (monumentPhase >= 2) {
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.52, 0.64, 0.30, '#A0998E', isAwake);
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.82, 0.72, 0.18, '#3B3834', isAwake);
      }
      if (monumentPhase >= 3) {
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + 1.00, 0.44, 0.26, '#3B3834', isAwake);
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + 1.26, 0.26, 0.20, '#3B3834', isAwake);
        const lotus = project(cx, cy, zBase + 1.56);
        ctx.fillStyle = '#F5CF6B';
        ctx.beginPath();
        ctx.arc(lotus.x, lotus.y, Math.max(1.8, unitScale * (showLegendGlow ? 0.17 : 0.12)), 0, Math.PI * 2);
        ctx.fill();
      }
    } else if (biomeId === 'otono') {
      // Molino de agua y refugio otoñal de cedro
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, 0.84, 0.28, '#8E8073', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.28, 0.76, 0.58, '#EFE6D8', isAwake);
      if (monumentPhase >= 2) {
        draw3DPrism(ctx, project, wx0 + 0.04, wx0 + 0.20, wy0 + 0.24, wy0 + 0.76, zBase + 0.14, zBase + 0.76, '#6E482C', isAwake, true);
      }
      if (monumentPhase >= 3) {
        const r = 0.46;
        const apex = project(cx, cy, zBase + 1.44);
        const c0 = project(cx - r, cy - r, zBase + 0.86);
        const c1 = project(cx + r, cy - r, zBase + 0.86);
        const c2 = project(cx + r, cy + r, zBase + 0.86);
        const c3 = project(cx - r, cy + r, zBase + 0.86);
        [
          { pts: [apex, c0, c1], nx: 0, ny: -0.75, nz: 0.65 },
          { pts: [apex, c1, c2], nx: 0.75, ny: 0, nz: 0.65 },
          { pts: [apex, c2, c3], nx: 0, ny: 0.75, nz: 0.65 },
          { pts: [apex, c3, c0], nx: -0.75, ny: 0, nz: 0.65 }
        ].forEach(s => {
          if (screenSignedArea(s.pts) > 0.01) {
            const col = lambertShade('#BA4E3B', s.nx, s.ny, s.nz, isAwake);
            fillPoly(ctx, s.pts, col, col, 0.5);
          }
        });
        if (showLegendGlow) {
          const lantern = project(cx + 0.18, cy, zBase + 1.52);
          ctx.fillStyle = '#FFD573';
          ctx.beginPath();
          ctx.arc(lantern.x, lantern.y, Math.max(2, unitScale * 0.15), 0, Math.PI * 2);
          ctx.fill();
        }
      }
    } else if (biomeId === 'fiordo') {
      // Iglesia nórdica de madera (Stavkirke) con tejados escalonados
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, 0.86, 0.28, '#6B7682', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.28, 0.72, 0.46, '#48382E', isAwake);
      if (monumentPhase >= 2) {
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.74, 0.82, 0.22, '#342720', isAwake);
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.96, 0.50, 0.34, '#48382E', isAwake);
      }
      if (monumentPhase >= 3) {
        const r = 0.34;
        const apex = project(cx, cy, zBase + 1.88);
        const c0 = project(cx - r, cy - r, zBase + 1.30);
        const c1 = project(cx + r, cy - r, zBase + 1.30);
        const c2 = project(cx + r, cy + r, zBase + 1.30);
        const c3 = project(cx - r, cy + r, zBase + 1.30);
        [
          { pts: [apex, c0, c1], nx: 0, ny: -0.75, nz: 0.65 },
          { pts: [apex, c1, c2], nx: 0.75, ny: 0, nz: 0.65 },
          { pts: [apex, c2, c3], nx: 0, ny: 0.75, nz: 0.65 },
          { pts: [apex, c3, c0], nx: -0.75, ny: 0, nz: 0.65 }
        ].forEach(s => {
          if (screenSignedArea(s.pts) > 0.01) {
            const col = lambertShade('#382B23', s.nx, s.ny, s.nz, isAwake);
            fillPoly(ctx, s.pts, col, '#DFE8EE', 0.4);
          }
        });
        if (showLegendGlow) {
          const aura = project(cx, cy, zBase + 1.98);
          ctx.fillStyle = '#8CF2D8';
          ctx.beginPath();
          ctx.arc(aura.x, aura.y, Math.max(2, unitScale * 0.16), 0, Math.PI * 2);
          ctx.fill();
        }
      }
    } else if (biomeId === 'atolon') {
      // Santuario Tiki y Perla de Coral sobre terraza blanca
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, 0.88, 0.24, '#EFE7D8', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.24, 0.68, 0.56, '#966A43', isAwake);
      if (monumentPhase >= 2) {
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.80, 0.86, 0.24, '#D6AE66', isAwake);
      }
      if (monumentPhase >= 3) {
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + 1.04, 0.52, 0.34, '#C49A54', isAwake);
        const pearl = project(cx, cy, zBase + 1.50);
        ctx.fillStyle = showLegendGlow ? '#FF9A8B' : '#FFF1D0';
        ctx.beginPath();
        ctx.arc(pearl.x, pearl.y, Math.max(2.1, unitScale * (showLegendGlow ? 0.18 : 0.14)), 0, Math.PI * 2);
        ctx.fill();
      }
    } else if (biomeId === 'toscana') {
      // Villa toscana con torre campanile medieval
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, 0.88, 0.34, '#EFE5D2', isAwake);
      drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.34, 0.72, 0.50, '#E9D3AA', isAwake);
      if (monumentPhase >= 2) {
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.84, 0.42, 0.74, '#DFB478', isAwake);
      }
      if (monumentPhase >= 3) {
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + 1.58, 0.48, 0.24, '#C96D4B', isAwake);
        if (showLegendGlow) {
          const sunBell = project(cx, cy, zBase + 1.92);
          ctx.fillStyle = '#FFE082';
          ctx.beginPath();
          ctx.arc(sunBell.x, sunBell.y, Math.max(2, unitScale * 0.16), 0, Math.PI * 2);
          ctx.fill();
        }
      }
    } else {
      drawArchBlock3D(ctx, project, wx0, wy0, zBase, 0.80, 0.70, '#F4EDE1', isAwake);
      if (monumentPhase >= 2) {
        drawArchBlock3D(ctx, project, wx0, wy0, zBase + 0.70, 0.46, 0.62, '#EFE7D8', isAwake);
      }
      if (monumentPhase >= 3) {
        const dome = project(cx, cy, zBase + 1.46);
        ctx.fillStyle = '#E5A03D';
        ctx.beginPath();
        ctx.arc(dome.x, dome.y, Math.max(2, unitScale * (showLegendGlow ? 0.19 : 0.15)), 0, Math.PI * 2);
        ctx.fill();
      }
    }
  }

  function drawTorii3D(ctx, project, unitScale, wx0, wy0, zBase) {
    const pL = project(wx0 + 0.2, wy0 + 0.5, zBase);
    const pR = project(wx0 + 0.8, wy0 + 0.5, zBase);
    const tL = project(wx0 + 0.2, wy0 + 0.5, zBase + 0.65);
    const tR = project(wx0 + 0.8, wy0 + 0.5, zBase + 0.65);
    ctx.strokeStyle = '#C95D4B';
    ctx.lineWidth = Math.max(1.8, unitScale * 0.13);
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(pL.x, pL.y);
    ctx.lineTo(tL.x, tL.y);
    ctx.moveTo(pR.x, pR.y);
    ctx.lineTo(tR.x, tR.y);
    ctx.moveTo(tL.x, tL.y);
    ctx.lineTo(tR.x, tR.y);
    ctx.stroke();
  }

  // Capa atmosférica animada: lluvia y bruma en Jungla / pétalos de almendro rosa en Kioto
  function drawWaterfallAndRainOverlay(ctx, project, w, h, zoom, points, showMistAndPetals, showLegendGlow, time, biome) {
    if (biome === 'jungla') {
      if (showMistAndPetals) {
        const mistPos = project(-2.2, -2.2, 0.75);
        const mistGrad = ctx.createRadialGradient(mistPos.x, mistPos.y, 2, mistPos.x, mistPos.y, 34 * zoom);
        mistGrad.addColorStop(0, 'rgba(240, 248, 247, 0.36)');
        mistGrad.addColorStop(1, 'rgba(240, 248, 247, 0)');
        ctx.fillStyle = mistGrad;
        ctx.beginPath();
        ctx.arc(mistPos.x, mistPos.y, 34 * zoom, 0, Math.PI * 2);
        ctx.fill();
      }

      const dropCount = showMistAndPetals ? 34 : 18 + Math.floor((points || 0) / 5);
      ctx.strokeStyle = 'rgba(202, 228, 234, 0.52)';
      ctx.lineWidth = 1.05;
      ctx.lineCap = 'round';
      for (let i = 0; i < dropCount; i++) {
        const seedX = cellHash(i, 1, 77);
        const seedY = cellHash(i, 2, 88);
        const speed = 0.65 + (i % 5) * 0.11;
        const progress = (seedY + time * speed) % 1;
        const rx = w * (0.18 + seedX * 0.64) - progress * 22 * zoom;
        const ry = h * (0.10 + progress * 0.72);
        const len = (7 + (i % 3) * 3) * Math.min(1.4, zoom);
        ctx.beginPath();
        ctx.moveTo(rx, ry);
        ctx.lineTo(rx - len * 0.25, ry + len);
        ctx.stroke();
      }
    } else if (biome === 'kioto' && showMistAndPetals) {
      const petalCount = showLegendGlow ? 20 : 12;
      for (let i = 0; i < petalCount; i++) {
        const sx = cellHash(i, 3, 61);
        const sy = cellHash(i, 4, 73);
        const prog = (sy + time * (0.14 + (i % 4) * 0.03)) % 1;
        const px = w * (0.20 + sx * 0.60) + Math.sin(time * 1.8 + i) * 10 * zoom;
        const py = h * (0.14 + prog * 0.68);
        ctx.fillStyle = i % 2 === 0 ? 'rgba(247, 206, 217, 0.85)' : 'rgba(238, 176, 193, 0.85)';
        ctx.beginPath();
        ctx.ellipse(px, py, 2.2 * Math.min(1.3, zoom), 1.3 * Math.min(1.3, zoom), time + i, 0, Math.PI * 2);
        ctx.fill();
      }
    } else if (biome === 'arrozales' && showMistAndPetals) {
      const fireflyCount = showLegendGlow ? 16 : 9;
      for (let i = 0; i < fireflyCount; i++) {
        const sx = cellHash(i, 5, 43);
        const sy = cellHash(i, 6, 51);
        const fx = w * (0.22 + sx * 0.56) + Math.sin(time * 1.3 + i * 2) * 8 * zoom;
        const fy = h * (0.28 + sy * 0.44) + Math.cos(time * 1.6 + i) * 6 * zoom;
        const pulse = 0.45 + 0.55 * Math.sin(time * 3.1 + i * 1.7);
        ctx.fillStyle = `rgba(218, 248, 146, ${(0.75 * pulse).toFixed(2)})`;
        ctx.beginPath();
        ctx.arc(fx, fy, (1.8 + pulse * 0.9) * Math.min(1.3, zoom), 0, Math.PI * 2);
        ctx.fill();
      }
    } else if (biome === 'otono' && showMistAndPetals) {
      const leafCount = showLegendGlow ? 18 : 11;
      for (let i = 0; i < leafCount; i++) {
        const sx = cellHash(i, 7, 37);
        const sy = cellHash(i, 8, 41);
        const prog = (sy + time * (0.12 + (i % 4) * 0.028)) % 1;
        const lx = w * (0.18 + sx * 0.64) + Math.sin(time * 1.5 + i) * 12 * zoom;
        const ly = h * (0.14 + prog * 0.68);
        ctx.fillStyle = i % 3 === 0 ? 'rgba(217, 75, 56, 0.85)' : i % 3 === 1 ? 'rgba(235, 178, 68, 0.85)' : 'rgba(230, 126, 53, 0.82)';
        ctx.beginPath();
        ctx.ellipse(lx, ly, 2.4 * Math.min(1.3, zoom), 1.4 * Math.min(1.3, zoom), time * 1.4 + i, 0, Math.PI * 2);
        ctx.fill();
      }
    } else if (biome === 'fiordo') {
      if (showMistAndPetals) {
        ctx.save();
        const aurGrad = ctx.createLinearGradient(w * 0.18, h * 0.10, w * 0.82, h * 0.28);
        aurGrad.addColorStop(0, 'rgba(92, 232, 188, 0)');
        aurGrad.addColorStop(0.35, showLegendGlow ? 'rgba(92, 232, 188, 0.24)' : 'rgba(92, 232, 188, 0.16)');
        aurGrad.addColorStop(0.68, showLegendGlow ? 'rgba(138, 126, 242, 0.20)' : 'rgba(138, 126, 242, 0.12)');
        aurGrad.addColorStop(1, 'rgba(138, 126, 242, 0)');
        ctx.strokeStyle = aurGrad;
        ctx.lineWidth = 14 * Math.min(1.4, zoom);
        ctx.lineCap = 'round';
        ctx.beginPath();
        const wave = Math.sin(time * 0.9) * 6 * zoom;
        ctx.moveTo(w * 0.20, h * 0.22 + wave);
        ctx.bezierCurveTo(w * 0.38, h * 0.10 - wave, w * 0.62, h * 0.26 + wave, w * 0.80, h * 0.14);
        ctx.stroke();
        ctx.restore();
      }
      const snowCount = showMistAndPetals ? 26 : 10 + Math.floor((points || 0) / 8);
      for (let i = 0; i < snowCount; i++) {
        const sx = cellHash(i, 9, 91);
        const sy = cellHash(i, 10, 97);
        const prog = (sy + time * (0.20 + (i % 4) * 0.04)) % 1;
        const fx = w * (0.16 + sx * 0.68) + Math.sin(time * 1.4 + i) * 7 * zoom;
        const fy = h * (0.12 + prog * 0.70);
        ctx.fillStyle = 'rgba(244, 249, 255, 0.82)';
        ctx.beginPath();
        ctx.arc(fx, fy, 1.5 * Math.min(1.3, zoom), 0, Math.PI * 2);
        ctx.fill();
      }
    } else if (biome === 'atolon' && showMistAndPetals) {
      const glintCount = showLegendGlow ? 16 : 9;
      for (let i = 0; i < glintCount; i++) {
        const sx = cellHash(i, 11, 23);
        const sy = cellHash(i, 12, 29);
        const pulse = Math.max(0, Math.sin(time * 2.6 + i * 1.9));
        const gx = w * (0.30 + sx * 0.44);
        const gy = h * (0.42 + sy * 0.32);
        ctx.fillStyle = `rgba(255, 255, 255, ${(0.72 * pulse).toFixed(2)})`;
        ctx.beginPath();
        ctx.arc(gx, gy, (1.4 + pulse * 1.1) * Math.min(1.25, zoom), 0, Math.PI * 2);
        ctx.fill();
      }
    } else if (biome === 'toscana' && showMistAndPetals) {
      const moteCount = showLegendGlow ? 15 : 8;
      for (let i = 0; i < moteCount; i++) {
        const sx = cellHash(i, 13, 17);
        const sy = cellHash(i, 14, 19);
        const mx = w * (0.22 + sx * 0.56) + Math.sin(time * 0.9 + i) * 6 * zoom;
        const my = h * (0.22 + sy * 0.46) + Math.cos(time * 0.7 + i) * 5 * zoom;
        ctx.fillStyle = 'rgba(255, 232, 166, 0.74)';
        ctx.beginPath();
        ctx.arc(mx, my, 1.6 * Math.min(1.25, zoom), 0, Math.PI * 2);
        ctx.fill();
      }
    }
  }

  function getTimeOfDayPhase(date = new Date()) {
    const h = date.getHours();
    if (h >= 6 && h <= 8) return 'dawn';
    if (h >= 9 && h <= 17) return 'day';
    if (h >= 18 && h <= 20) return 'sunset';
    return 'night';
  }

  const LOCKED_CLOUD_PUFFS = [
    { wx: -5.8, wy: -4.2, wz: 1.5, r: 2.3 },
    { wx: -2.4, wy: -5.6, wz: 1.6, r: 2.5 },
    { wx: 1.5, wy: -5.2, wz: 1.55, r: 2.4 },
    { wx: 5.2, wy: -3.8, wz: 1.45, r: 2.2 },
    { wx: -6.4, wy: 0.4, wz: 1.55, r: 2.4 },
    { wx: -3.1, wy: -1.8, wz: 2.05, r: 2.9 },
    { wx: 0.4, wy: -2.0, wz: 2.2, r: 3.1 },
    { wx: 3.9, wy: -1.3, wz: 1.95, r: 2.7 },
    { wx: 6.2, wy: 1.6, wz: 1.5, r: 2.2 },
    { wx: -5.2, wy: 4.1, wz: 1.5, r: 2.3 },
    { wx: -1.6, wy: 1.5, wz: 2.35, r: 3.2 },
    { wx: 2.1, wy: 1.8, wz: 2.15, r: 2.85 },
    { wx: 4.8, wy: 4.3, wz: 1.45, r: 2.2 },
    { wx: -2.2, wy: 5.2, wz: 1.65, r: 2.5 },
    { wx: 1.4, wy: 5.4, wz: 1.55, r: 2.35 },
    { wx: 0.1, wy: 0.1, wz: 2.6, r: 3.3 },
    { wx: -4.1, wy: 2.0, wz: 1.8, r: 2.5 },
    { wx: 3.4, wy: -4.2, wz: 1.7, r: 2.3 }
  ];

  function drawLockedCloudDiorama(ctx, project, unitScale, cells, frameBottom, rimHeight, cosY, sinY, camDistY) {
    for (let ci = 0; ci < cells.length; ci++) {
      const { ix, iy, wx0, wx1, wy0, wy1 } = cells[ci];
      const isFrame = ix === -1 || ix === N || iy === -1 || iy === N;
      if (isFrame) {
        draw3DPrism(ctx, project, wx0, wx1, wy0, wy1, frameBottom, rimHeight, '#262624', false, true);
      } else {
        const shadeJitter = ((ix + iy) % 2 === 0) ? '#E1DDD4' : '#DAD6CD';
        draw3DPrism(ctx, project, wx0, wx1, wy0, wy1, frameBottom + 0.08, 0.28, shadeJitter, false, true);
      }
    }

    const sortedPuffs = LOCKED_CLOUD_PUFFS.map(p => {
      const rx = p.wx * cosY - p.wy * sinY;
      const ry = p.wx * sinY + p.wy * cosY;
      const distSq = rx * rx + (ry - camDistY) * (ry - camDistY);
      return { ...p, distSq };
    }).sort((a, b) => b.distSq - a.distSq);

    sortedPuffs.forEach(p => {
      const sh = project(p.wx - 0.35, p.wy + 0.35, 0.32);
      const rad = Math.max(8, p.r * unitScale);
      ctx.fillStyle = 'rgba(48, 45, 40, 0.10)';
      ctx.beginPath();
      ctx.ellipse(sh.x, sh.y, rad * 0.92, rad * 0.56, 0, 0, Math.PI * 2);
      ctx.fill();
    });

    sortedPuffs.forEach(p => {
      const pos = project(p.wx, p.wy, p.wz);
      const rad = Math.max(8, p.r * unitScale);
      const grad = ctx.createRadialGradient(
        pos.x - rad * 0.28,
        pos.y - rad * 0.32,
        rad * 0.12,
        pos.x,
        pos.y,
        rad
      );
      grad.addColorStop(0, '#FFFFFF');
      grad.addColorStop(0.58, '#F2EFE9');
      grad.addColorStop(1, '#D5D0C6');
      ctx.fillStyle = grad;
      ctx.beginPath();
      ctx.arc(pos.x, pos.y, rad, 0, Math.PI * 2);
      ctx.fill();
    });
  }

  function drawRadiantArcBehindDiorama(ctx, w, h, zoom, resolvedPhase) {
    if (resolvedPhase === 'night') return;
    ctx.save();
    const cx = w * 0.5;
    const cy = h * 0.56;
    const baseR = Math.min(w, h) * 0.44 * Math.min(1.18, zoom);
    const bands = [
      'rgba(235, 134, 118, 0.16)',
      'rgba(245, 198, 108, 0.16)',
      'rgba(126, 204, 146, 0.16)',
      'rgba(120, 184, 232, 0.16)'
    ];
    bands.forEach((col, i) => {
      ctx.strokeStyle = col;
      ctx.lineWidth = Math.max(2.2, 3.2 * zoom);
      ctx.lineCap = 'round';
      ctx.beginPath();
      ctx.arc(cx, cy, baseR - i * 3.6 * zoom, Math.PI * 1.13, Math.PI * 1.87);
      ctx.stroke();
    });
    ctx.restore();
  }

  function drawLivingTimeAndWeatherOverlay(ctx, project, w, h, zoom, unitScale, resolvedPhase, weatherMode, time) {
    const auraCx = w * 0.5;
    const auraCy = h * 0.48;
    const auraR = Math.min(w, h) * 0.48;
    if (resolvedPhase === 'sunset') {
      const warmGrad = ctx.createRadialGradient(auraCx, auraCy, 6, auraCx, auraCy, auraR);
      warmGrad.addColorStop(0, 'rgba(255, 164, 92, 0.14)');
      warmGrad.addColorStop(0.68, 'rgba(255, 148, 86, 0.06)');
      warmGrad.addColorStop(1, 'rgba(255, 140, 82, 0)');
      ctx.fillStyle = warmGrad;
      ctx.beginPath();
      ctx.arc(auraCx, auraCy, auraR, 0, Math.PI * 2);
      ctx.fill();
    } else if (resolvedPhase === 'night') {
      const nightGrad = ctx.createRadialGradient(auraCx, auraCy, 6, auraCx, auraCy, auraR);
      nightGrad.addColorStop(0, 'rgba(24, 38, 72, 0.22)');
      nightGrad.addColorStop(0.70, 'rgba(20, 32, 62, 0.10)');
      nightGrad.addColorStop(1, 'rgba(18, 29, 58, 0)');
      ctx.fillStyle = nightGrad;
      ctx.beginPath();
      ctx.arc(auraCx, auraCy, auraR, 0, Math.PI * 2);
      ctx.fill();

      // Farolillos y luciérnagas nocturnas cálidas
      for (let i = 0; i < 16; i++) {
        const fx = (cellHash(i, 3, 41) - 0.5) * 14.5;
        const fy = (cellHash(i, 7, 53) - 0.5) * 14.5;
        const fz = 1.1 + cellHash(i, 11, 61) * 1.8;
        const p = project(fx, fy, fz);
        const pulse = 0.55 + 0.45 * Math.sin(time * 2.2 + i * 1.4);
        const r = Math.max(3.5, unitScale * 0.55) * (0.85 + pulse * 0.35);
        const glow = ctx.createRadialGradient(p.x, p.y, 0.5, p.x, p.y, r * 2.2);
        glow.addColorStop(0, `rgba(255, 238, 158, ${(0.88 * pulse).toFixed(2)})`);
        glow.addColorStop(0.5, `rgba(255, 198, 92, ${(0.34 * pulse).toFixed(2)})`);
        glow.addColorStop(1, 'rgba(255, 198, 92, 0)');
        ctx.fillStyle = glow;
        ctx.beginPath();
        ctx.arc(p.x, p.y, r * 2.2, 0, Math.PI * 2);
        ctx.fill();
      }
    }

    if (weatherMode === 'radiant') {
      for (let i = 0; i < 10; i++) {
        const sx = cellHash(i, 5, 101);
        const sy = cellHash(i, 8, 107);
        const pulse = 0.45 + 0.55 * Math.sin(time * 2.4 + i * 1.7);
        const px = w * (0.20 + sx * 0.60) + Math.sin(time + i) * 5 * zoom;
        const py = h * (0.18 + sy * 0.46) + Math.cos(time * 0.8 + i) * 4 * zoom;
        ctx.fillStyle = `rgba(255, 232, 146, ${(0.75 * pulse).toFixed(2)})`;
        ctx.beginPath();
        ctx.arc(px, py, (1.5 + pulse * 1.2) * Math.min(1.25, zoom), 0, Math.PI * 2);
        ctx.fill();
      }
    } else if (weatherMode === 'mist') {
      // Neblina tranquila y reversible: nunca daña ni oculta construcciones
      for (let i = 0; i < 5; i++) {
        const drift = Math.sin(time * 0.35 + i * 1.6) * 14 * zoom;
        const mx = w * (0.28 + (i % 3) * 0.22) + drift;
        const my = h * (0.44 + (i % 2) * 0.14);
        const mr = Math.min(w, h) * (0.24 + (i % 2) * 0.06);
        const mGrad = ctx.createRadialGradient(mx, my, 2, mx, my, mr);
        mGrad.addColorStop(0, 'rgba(238, 241, 243, 0.38)');
        mGrad.addColorStop(1, 'rgba(238, 241, 243, 0)');
        ctx.fillStyle = mGrad;
        ctx.beginPath();
        ctx.arc(mx, my, mr, 0, Math.PI * 2);
        ctx.fill();
      }
    }
  }

  function renderToCanvas(canvas, opts = {}) {
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const biome = opts.biome || 'valle';
    const points = typeof opts.points === 'number'
      ? Math.max(0, Math.min(MAX_POINTS, opts.points))
      : getPointsForStagePreset(opts.stage || 1);
    const isAwake = Boolean(opts.isAwake);
    const lockedClouds = Boolean(opts.lockedClouds);
    const timePhaseOpt = opts.timePhase || 'auto';
    const resolvedPhase = timePhaseOpt === 'auto' ? getTimeOfDayPhase() : timePhaseOpt;
    const weatherMode = opts.weatherMode || 'clear';
    const yaw = typeof opts.yaw === 'number' ? opts.yaw : Math.PI / 4;
    const pitch = typeof opts.pitch === 'number' ? opts.pitch : 0.60;
    const zoom = typeof opts.zoom === 'number' ? opts.zoom : 1.0;
    const time = typeof opts.time === 'number' ? opts.time : 0.5;

    const biomeMeta = getBiome(biome);
    const layout = getBiomeLayout(biome);
    const budgets = getBudgetsForPoints(points, layout);

    const maxDpr = typeof opts.maxDpr === 'number' ? opts.maxDpr : 1.5;
    const dpr = Math.min(window.devicePixelRatio || 1, maxDpr);
    const parent = canvas.parentElement;
    let w = parent && parent.clientWidth > 10 ? parent.clientWidth : 0;
    let h = parent && parent.clientHeight > 10 ? parent.clientHeight : 0;
    if (!w || !h) {
      const rect = parent ? parent.getBoundingClientRect() : { width: 350, height: 250 };
      w = rect.width > 10 ? rect.width : 350;
      h = rect.height > 10 ? rect.height : 250;
    }

    if (canvas.width !== Math.round(w * dpr) || canvas.height !== Math.round(h * dpr)) {
      canvas.width = Math.round(w * dpr);
      canvas.height = Math.round(h * dpr);
    }

    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, w, h);

    if (!lockedClouds) {
      const skyGlowColor =
        resolvedPhase === 'dawn' ? 'rgba(255, 198, 146, 0.34)' :
        resolvedPhase === 'sunset' ? 'rgba(255, 152, 90, 0.36)' :
        resolvedPhase === 'night' ? 'rgba(104, 140, 214, 0.22)' :
        (isAwake ? biomeMeta.skyGlow : 'rgba(255, 255, 255, 0)');
      const auraCx = w * 0.5;
      const auraCy = h * 0.44;
      const auraR = Math.min(w, h) * 0.47;
      const sunGrad = ctx.createRadialGradient(auraCx, auraCy, 6, auraCx, auraCy, auraR);
      sunGrad.addColorStop(0, skyGlowColor);
      sunGrad.addColorStop(0.62, skyGlowColor.replace(/[\d.]+\)$/, '0.08)'));
      sunGrad.addColorStop(1, 'rgba(255, 255, 255, 0)');
      ctx.fillStyle = sunGrad;
      ctx.beginPath();
      ctx.arc(auraCx, auraCy, auraR, 0, Math.PI * 2);
      ctx.fill();

      if (resolvedPhase === 'night') {
        for (let i = 0; i < 14; i++) {
          const sx = w * (0.08 + cellHash(i, 1, 131) * 0.84);
          const sy = h * (0.06 + cellHash(i, 2, 137) * 0.30);
          ctx.fillStyle = 'rgba(232, 240, 255, 0.45)';
          ctx.beginPath();
          ctx.arc(sx, sy, 1.1, 0, Math.PI * 2);
          ctx.fill();
        }
      }

      if (weatherMode === 'radiant') {
        drawRadiantArcBehindDiorama(ctx, w, h, zoom, resolvedPhase);
      }
    }

    const cosY = Math.cos(yaw);
    const sinY = Math.sin(yaw);
    const cosP = Math.cos(pitch);
    const sinP = Math.sin(pitch);

    const baseScale = Math.min(w * 0.0315, h * 0.052);
    const unitScale = baseScale * zoom;
    const cx = w * 0.5;
    const cy = h * 0.515;
    const perspK = 0.0105;

    function project(wx, wy, wz = 0) {
      const rx = wx * cosY - wy * sinY;
      const ry = wx * sinY + wy * cosY;
      const depth = ry * cosP + wz * sinP;
      const persp = 1 / (1 - Math.max(-26, Math.min(26, depth)) * perspK);
      return {
        x: cx + rx * unitScale * persp,
        y: cy + (ry * sinP - wz * cosP) * unitScale * persp
      };
    }

    const pad = 0.85;
    const frameBottom = -1.1;
    const rimHeight = 0.75;
    const halfN = N / 2;

    // 1. Sombra suave de estudio
    const shQuad = [
      project(-halfN - pad, -halfN - pad, frameBottom),
      project(halfN + pad, -halfN - pad, frameBottom),
      project(halfN + pad, halfN + pad, frameBottom),
      project(-halfN - pad, halfN + pad, frameBottom)
    ];
    [
      { dx: -15 * zoom, dy: 11 * zoom, alpha: 0.05 },
      { dx: -11 * zoom, dy: 8 * zoom, alpha: 0.065 },
      { dx: -7 * zoom, dy: 6 * zoom, alpha: 0.08 }
    ].forEach(s => {
      fillPoly(
        ctx,
        shQuad.map(p => ({ x: p.x + s.dx, y: p.y + s.dy })),
        `rgba(28, 26, 23, ${s.alpha})`,
        null
      );
    });

    // 2 y 3. Ordenación exacta por distancia a la cámara en perspectiva (20×20)
    const camDistY = (1 / perspK) * cosP;
    const cells = [];
    for (let ix = -1; ix <= N; ix++) {
      const wx0 = ix === -1 ? -halfN - pad : ix === N ? halfN : ix - halfN;
      const wx1 = ix === -1 ? -halfN : ix === N ? halfN + pad : ix - halfN + 1;
      const mx = (wx0 + wx1) * 0.5;
      for (let iy = -1; iy <= N; iy++) {
        const wy0 = iy === -1 ? -halfN - pad : iy === N ? halfN : iy - halfN;
        const wy1 = iy === -1 ? -halfN : iy === N ? halfN + pad : iy - halfN + 1;
        const my = (wy0 + wy1) * 0.5;
        const rx = mx * cosY - my * sinY;
        const ry = mx * sinY + my * cosY;
        const distSq = rx * rx + (ry - camDistY) * (ry - camDistY);
        cells.push({ ix, iy, wx0, wx1, wy0, wy1, distSq });
      }
    }
    cells.sort((a, b) => b.distSq - a.distSq);

    if (lockedClouds) {
      drawLockedCloudDiorama(ctx, project, unitScale, cells, frameBottom, rimHeight, cosY, sinY, camDistY);
      const lr00 = project(-halfN - pad, -halfN - pad, rimHeight);
      const lr10 = project(halfN + pad, -halfN - pad, rimHeight);
      const lr11 = project(halfN + pad, halfN + pad, rimHeight);
      const lr01 = project(-halfN - pad, halfN + pad, rimHeight);
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.14)';
      ctx.lineWidth = 0.85;
      ctx.beginPath();
      ctx.moveTo(lr00.x, lr00.y);
      ctx.lineTo(lr10.x, lr10.y);
      ctx.lineTo(lr11.x, lr11.y);
      ctx.lineTo(lr01.x, lr01.y);
      ctx.closePath();
      ctx.stroke();
      return;
    }

    let fjordShipTarget = null;
    let fjordShipSpanCount = 0;

    for (let ci = 0; ci < cells.length; ci++) {
      const { ix, iy, wx0, wx1, wy0, wy1 } = cells[ci];
      const isFrame = ix === -1 || ix === N || iy === -1 || iy === N;
      if (isFrame) {
        draw3DPrism(ctx, project, wx0, wx1, wy0, wy1, frameBottom, rimHeight, '#262624', isAwake, true);
        continue;
      }

      const key = `${ix},${iy}`;
      const showPathHere = Boolean(layout.pathRanks[key] && layout.pathRanks[key] <= budgets.paths);
      const cell = getCellData(biome, ix, iy, points, showPathHere);

        draw3DPrism(
          ctx,
          project,
          wx0,
          wx1,
          wy0,
          wy1,
          frameBottom + 0.08,
          cell.wz,
          cell.color,
          isAwake,
          true
        );

        // Puentes, palafitos, barcas y reflejos en agua
        if (cell.isWater) {
          if (budgets.showBridge) {
            const isBridgeTile =
              (biome === 'valle' && ix === 9 && iy === 8) ||
              (biome === 'kioto' && iy === 11 && ix === 7) ||
              (biome === 'jungla' && ix === 10 && cell.riverDist < 0.06) ||
              (biome === 'costa' && ix === 11 && iy === 8) ||
              (biome === 'desierto' && ix === 8 && iy === 10) ||
              (biome === 'arrozales' && ix === 10 && iy === 12) ||
              (biome === 'otono' && ix === 8 && iy === 10) ||
              (biome === 'fiordo' && ix === 4 && iy === 9) ||
              (biome === 'atolon' && ix === 9 && iy === 8) ||
              (biome === 'toscana' && ix === 9 && iy === 9);
            if (isBridgeTile) {
              drawBridge3D(ctx, project, biome, wx0, wy0, cell.wz, isAwake);
            }
          }
          if (biome === 'fiordo' && points >= 30 && iy === 10 && ix >= 10 && ix <= 12) {
            if (ix === 11) {
              fjordShipTarget = { wx0, wy0, wz: cell.wz };
            }
            fjordShipSpanCount++;
            if (fjordShipSpanCount === 3 && fjordShipTarget) {
              drawLargeFjordShip3D(ctx, project, unitScale, fjordShipTarget.wx0, fjordShipTarget.wy0, fjordShipTarget.wz, isAwake);
            }
            continue;
          }
          const isBoat1 =
            (biome === 'valle' && ix === 9 && iy === 12) ||
            (biome === 'arrozales' && ix === 14 && iy === 12) ||
            (biome === 'otono' && ix === 10 && iy === 11) ||
            (biome === 'atolon' && ix === 11 && iy === 11) ||
            (biome === 'toscana' && ix === 13 && iy === 8) ||
            (['jungla', 'costa', 'kioto'].includes(biome) && ix === 13 && iy === 11);
          const isBoat2 =
            (biome === 'valle' && ix === 8 && iy === 13) ||
            (biome === 'otono' && ix === 12 && iy === 11) ||
            (biome === 'fiordo' && ix === 15 && iy === 11) ||
            (biome === 'atolon' && ix === 14 && iy === 13) ||
            (biome === 'toscana' && ix === 5 && iy === 11) ||
            (['jungla', 'costa', 'kioto'].includes(biome) && ix === 14 && iy === 6);
          if ((budgets.boats >= 1 && isBoat1) || (budgets.boats >= 2 && isBoat2)) {
            drawBoat3D(ctx, project, biome, wx0, wy0, cell.wz, isAwake);
          }
          if (biome === 'atolon' && layout.overwaterRanks[key] && layout.overwaterRanks[key] <= budgets.overwater) {
            drawOverwaterBungalow3D(ctx, project, wx0, wy0, cell.wz, isAwake);
            continue;
          }
          if (layout.shimmerRanks[key] && layout.shimmerRanks[key] <= budgets.shimmers) {
            const w1 = project(wx0 + 0.25, wy0 + 0.5, cell.wz + 0.02);
            const w2 = project(wx0 + 0.72, wy0 + 0.5, cell.wz + 0.02);
            ctx.strokeStyle = 'rgba(255, 255, 255, 0.48)';
            ctx.lineWidth = Math.max(0.8, unitScale * 0.08);
            ctx.beginPath();
            ctx.moveTo(w1.x, w1.y);
            ctx.lineTo(w2.x, w2.y);
            ctx.stroke();
          }
          continue;
        }

        if (cell.isWaterfall || cell.isPath) continue;

        const isLandmarkPos = layout.landmarkCell && layout.landmarkCell.x === ix && layout.landmarkCell.y === iy;
        const isToriiPos = biome === 'kioto' && ix === layout.toriiCell.x && iy === layout.toriiCell.y;
        const isSecondTorii = biome === 'kioto' && budgets.showSecondary && ix === layout.secondToriiCell.x && iy === layout.secondToriiCell.y;
        const isCostaWindmill = biome === 'costa' && budgets.showSecondary && ix === layout.costaWindmillCell.x && iy === layout.costaWindmillCell.y;
        const isDesertRuins = biome === 'desierto' && budgets.showSecondary && ix === layout.desertRuinsCell.x && iy === layout.desertRuinsCell.y;
        const isCustomSecondary = budgets.showSecondary && !isCostaWindmill && !isDesertRuins && layout.secondaryCell && ix === layout.secondaryCell.x && iy === layout.secondaryCell.y;

        if (budgets.monumentPhase >= 1 && isLandmarkPos) {
          drawLandmark3D(ctx, project, unitScale, biome, wx0, wy0, cell.wz, budgets.monumentPhase, budgets.showLegendGlow, isAwake);
          continue;
        }
        if (isCostaWindmill) {
          drawWindmill3D(ctx, project, unitScale, wx0, wy0, cell.wz, isAwake);
          continue;
        }
        if (isDesertRuins) {
          drawDesertRuins3D(ctx, project, wx0, wy0, cell.wz, isAwake);
          continue;
        }
        if (isCustomSecondary) {
          drawSecondaryLandmark3D(ctx, project, unitScale, biome, wx0, wy0, cell.wz, isAwake);
          continue;
        }
        if ((budgets.monumentPhase >= 2 && isToriiPos) || isSecondTorii) {
          drawTorii3D(ctx, project, unitScale, wx0, wy0, cell.wz);
          continue;
        }
        if (biome === 'toscana' && layout.toscanaFountains) {
          const isFountain1 = ix === layout.toscanaFountains[0].x && iy === layout.toscanaFountains[0].y && points >= 24;
          const isFountain2 = ix === layout.toscanaFountains[1].x && iy === layout.toscanaFountains[1].y && points >= 46;
          if (isFountain1 || isFountain2) {
            drawTuscanFountain3D(ctx, project, unitScale, wx0, wy0, cell.wz, isAwake);
            continue;
          }
        }

        if (layout.tentRanks[key] && layout.tentRanks[key] <= budgets.tents) {
          drawDesertTent3D(ctx, project, wx0, wy0, cell.wz, ix + iy, isAwake);
          continue;
        }
        if (layout.houseRanks[key] && layout.houseRanks[key] <= budgets.houses) {
          const bh = 0.36 + cellHash(ix, iy, 7) * 0.38;
          drawSettlementHouse3D(ctx, project, biome, wx0, wy0, cell.wz, 0.56, bh, isAwake);
          continue;
        }
        if (layout.lanternRanks[key] && layout.lanternRanks[key] <= budgets.lanterns) {
          drawLantern3D(ctx, project, unitScale, wx0 + 0.5, wy0 + 0.5, cell.wz, isAwake);
          continue;
        }
        if (layout.rockRanks[key] && layout.rockRanks[key] <= budgets.rocks) {
          const rockHex =
            biome === 'jungla' ? '#637563' :
            biome === 'desierto' ? '#D4B284' :
            biome === 'fiordo' ? '#7C8C99' :
            biome === 'toscana' ? '#DCD2BE' :
            biome === 'arrozales' ? '#797E71' :
            biome === 'otono' ? '#8C8074' :
            biome === 'atolon' ? '#DCD6C8' : '#C9C4B8';
          drawMicroRock3D(ctx, project, wx0, wy0, cell.wz, 0.85, rockHex, isAwake);
          continue;
        }

        if (layout.treeRanks[key] && layout.treeRanks[key] <= budgets.trees) {
          if (biome === 'kioto') {
            const scale = 0.78 + cellHash(ix, iy, 63) * 0.24;
            drawBlossomTree3D(ctx, project, unitScale, wx0 + 0.5, wy0 + 0.5, cell.wz, scale, ix + iy, isAwake);
          } else if (biome === 'jungla') {
            const jKind = cellHash(ix, iy, 83);
            if (jKind > 0.56) {
              drawPalmTree3D(ctx, project, unitScale, wx0 + 0.5, wy0 + 0.5, cell.wz, 0.96, isAwake, '#2E6B42');
            } else if (jKind > 0.24) {
              drawBamboo3D(ctx, project, unitScale, wx0 + 0.5, wy0 + 0.5, cell.wz, 1.02, isAwake);
            } else {
              const scale = 0.85 + cellHash(ix, iy, 63) * 0.35;
              drawJungleTree3D(ctx, project, unitScale, wx0 + 0.5, wy0 + 0.5, cell.wz, scale, isAwake);
            }
          } else if (biome === 'valle') {
            drawMicroPine3D(ctx, project, wx0 + 0.5, wy0 + 0.5, cell.wz, 0.90, '#3A7042', isAwake);
          } else if (biome === 'costa') {
            const kind = cellHash(ix, iy, 61);
            if (kind > 0.56) {
              drawBougainvillea3D(ctx, project, unitScale, wx0 + 0.5, wy0 + 0.5, cell.wz, isAwake);
            } else if (kind > 0.28) {
              drawCypress3D(ctx, project, wx0 + 0.5, wy0 + 0.5, cell.wz, 0.95, isAwake);
            } else {
              drawMicroCanopy3D(ctx, project, unitScale, wx0 + 0.5, wy0 + 0.5, cell.wz, 0.21, '#809472', isAwake);
            }
          } else if (biome === 'desierto') {
            if (cell.isOasisRim) {
              drawPalmTree3D(ctx, project, unitScale, wx0 + 0.5, wy0 + 0.5, cell.wz, 0.88, isAwake, '#C89F63');
            }
          } else if (biome === 'arrozales') {
            const kind = cellHash(ix, iy, 65);
            if (kind > 0.25) {
              drawBamboo3D(ctx, project, unitScale, wx0 + 0.5, wy0 + 0.5, cell.wz, 1.02, isAwake);
            } else if (kind > 0.10) {
              drawPalmTree3D(ctx, project, unitScale, wx0 + 0.5, wy0 + 0.5, cell.wz, 0.90, isAwake, '#4B9650');
            } else {
              drawFrangipani3D(ctx, project, unitScale, wx0 + 0.5, wy0 + 0.5, cell.wz, 0.92, isAwake);
            }
          } else if (biome === 'otono') {
            drawMapleTree3D(ctx, project, unitScale, wx0 + 0.5, wy0 + 0.5, cell.wz, ix * 3 + iy, isAwake);
          } else if (biome === 'fiordo') {
            drawSnowPine3D(ctx, project, wx0 + 0.5, wy0 + 0.5, cell.wz, 0.94, isAwake, '#5B7B8F');
          } else if (biome === 'atolon') {
            if (cellHash(ix, iy, 69) > 0.36) {
              drawPalmTree3D(ctx, project, unitScale, wx0 + 0.5, wy0 + 0.5, cell.wz, 0.96, isAwake);
            } else {
              drawHibiscus3D(ctx, project, unitScale, wx0 + 0.5, wy0 + 0.5, cell.wz, isAwake);
            }
          } else if (biome === 'toscana') {
            if (cell.isVineyard) {
              drawVineyardTuft3D(ctx, project, unitScale, wx0 + 0.5, wy0 + 0.5, cell.wz, isAwake);
            } else if (cellHash(ix, iy, 71) > 0.44) {
              drawCypress3D(ctx, project, wx0 + 0.5, wy0 + 0.5, cell.wz, 1.02, isAwake);
            } else {
              drawMicroCanopy3D(ctx, project, unitScale, wx0 + 0.5, wy0 + 0.5, cell.wz, 0.27, '#758E4F', isAwake);
            }
          }
        }
    }

    // 4. Línea sutil de bisel superior en el marco exterior
    const r00 = project(-halfN - pad, -halfN - pad, rimHeight);
    const r10 = project(halfN + pad, -halfN - pad, rimHeight);
    const r11 = project(halfN + pad, halfN + pad, rimHeight);
    const r01 = project(-halfN - pad, halfN + pad, rimHeight);

    ctx.strokeStyle = 'rgba(255, 255, 255, 0.14)';
    ctx.lineWidth = 0.85;
    ctx.beginPath();
    ctx.moveTo(r00.x, r00.y);
    ctx.lineTo(r10.x, r10.y);
    ctx.lineTo(r11.x, r11.y);
    ctx.lineTo(r01.x, r01.y);
    ctx.closePath();
    ctx.stroke();

    // 5. Lluvia animada en Jungla / Pétalos rosas en Kioto
    drawWaterfallAndRainOverlay(ctx, project, w, h, zoom, points, budgets.showMistAndPetals, budgets.showLegendGlow, time, biome);

    // 6. Luz en tiempo real (amanecer/tarde/noche) y clima vivo (radiante / neblina suave)
    drawLivingTimeAndWeatherOverlay(ctx, project, w, h, zoom, unitScale, resolvedPhase, weatherMode, time);
  }

  function initDiorama(canvas) {
    dioramaCanvas = canvas;
    if (!dioramaCanvas) return;
    dioramaCtx = dioramaCanvas.getContext('2d');
  }

  function renderDiorama(opts = {}) {
    lastRenderOpts = { ...lastRenderOpts, ...opts };
    if (!dioramaCanvas) return;
    renderToCanvas(dioramaCanvas, lastRenderOpts);
  }

  function initParticles(canvas) {
    particleCanvas = canvas;
    if (!particleCanvas) return;
    particleCtx = particleCanvas.getContext('2d');
    resizeCanvas();
  }

  function resizeCanvas() {
    if (particleCanvas && particleCanvas.parentElement) {
      const parent = particleCanvas.parentElement;
      const dpr = Math.min(window.devicePixelRatio || 1, 1.5);
      let w = parent.clientWidth > 10 ? parent.clientWidth : 0;
      let h = parent.clientHeight > 10 ? parent.clientHeight : 0;
      if (!w || !h) {
        const rect = parent.getBoundingClientRect();
        w = rect.width > 10 ? rect.width : 350;
        h = rect.height > 10 ? rect.height : 220;
      }
      particleCanvas.width = Math.round(w * dpr);
      particleCanvas.height = Math.round(h * dpr);
      if (particleCtx) {
        particleCtx.setTransform(dpr, 0, 0, dpr, 0, 0);
      }
    }
    if (dioramaCanvas) {
      renderDiorama();
    }
  }

  function spawnParticles(count, colorPalette) {
    if (!particleCanvas) return;
    const w = particleCanvas.clientWidth || 340;
    const h = particleCanvas.clientHeight || 180;
    const colors = colorPalette || ['#E5A03D', '#7BC47F', '#6BBBD6'];
    for (let i = 0; i < count; i++) {
      particles.push({
        x: w * 0.5 + (Math.random() - 0.5) * 110,
        y: h * 0.44 + (Math.random() - 0.5) * 40,
        vx: (Math.random() - 0.5) * 1.5,
        vy: -Math.random() * 1.7 - 0.35,
        size: Math.random() * 2.8 + 1.6,
        alpha: 1,
        color: colors[Math.floor(Math.random() * colors.length)],
        life: 0,
        maxLife: 42 + Math.random() * 22
      });
    }
    if (!animFrameId) {
      animFrameId = requestAnimationFrame(loopParticles);
    }
  }

  function loopParticles() {
    if (particleCtx && particleCanvas) {
      const w = particleCanvas.clientWidth || 340;
      const h = particleCanvas.clientHeight || 180;
      particleCtx.clearRect(0, 0, w, h);

      for (let i = particles.length - 1; i >= 0; i--) {
        const p = particles[i];
        p.x += p.vx;
        p.y += p.vy;
        p.vy += 0.02;
        p.life++;
        p.alpha = Math.max(0, 1 - p.life / p.maxLife);

        particleCtx.save();
        particleCtx.globalAlpha = p.alpha;
        particleCtx.fillStyle = p.color;
        particleCtx.beginPath();
        particleCtx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
        particleCtx.fill();
        particleCtx.restore();

        if (p.life >= p.maxLife) {
          particles.splice(i, 1);
        }
      }
    }

    if (particles.length > 0) {
      animFrameId = requestAnimationFrame(loopParticles);
    } else {
      animFrameId = null;
    }
  }

  return {
    BIOMES,
    STAGES,
    applyLanguage,
    MAX_POINTS,
    POINTS_PER_LEVEL,
    getBiome,
    getStageInfo,
    getNextBiomeId,
    getLevelFromPoints,
    getSubProgress,
    getPointsForStagePreset,
    getTimeOfDayPhase,
    getBiomeSceneData,
    cellHash,
    lambertShade,
    shadeColor,
    renderToCanvas,
    initDiorama,
    renderDiorama,
    initParticles,
    resizeCanvas,
    spawnParticles
  };
})();
