/**
 * Explorador Interactivo 3D en Primera Persona — Valle Alpino & Costa Mediterránea
 * - Renderiza la malla real 18x18 de BroteMascot con iluminación según hora del día y clima vivo
 * - Museo Vivo del Muro Real: cuelga tus fotos reales en marcos de madera 3D dentro de Casa y Refugio
 * - Audio posicional 3D Web Audio según tu posición y orientación
 */
window.BroteExploreCosta = (function () {
  const DEFAULT_N = 18;
  const NEAR = 0.22;
  const SUN = { x: 0.45, y: -0.55, z: 0.70 };
  const ARCH_EDGE = 'rgba(34, 42, 50, 0.16)';

  const BIOME_CONFIGS = {
    costa: {
      id: 'costa',
      title: 'Mediterranean Coast',
      heroHouseCell: { x: 4, y: 10 },
      heroTreeCell: { x: 4, y: 4 },
      defaultSpot: 'faro',
      defaultCaption: 'Drag 360° · Tap Lighthouse, Pier, House, Tree, or wall frames',
      spotOrder: ['faro', 'cala', 'plaza', 'molino'],
      spots: {
        faro: {
          id: 'faro',
          pill: 'Lighthouse',
          label: 'The Lighthouse',
          sub: 'Stone courtyard and coastal tower',
          x: -2.35,
          y: -2.55,
          yaw: -3.08,
          pitch: 0.22
        },
        cala: {
          id: 'cala',
          pill: 'Pier',
          label: 'The Pier',
          sub: 'Wooden boardwalk over the blue cove',
          x: 0.75,
          y: -0.50,
          yaw: 1.44,
          pitch: -0.05
        },
        plaza: {
          id: 'plaza',
          pill: 'House',
          label: 'Mediterranean House',
          sub: 'Real Wall gallery and arches to the sea',
          x: -5.06,
          y: 1.50,
          yaw: 1.57,
          pitch: -0.02
        },
        molino: {
          id: 'molino',
          pill: 'Tree',
          label: 'Overlook Tree',
          sub: 'Olive tree, bougainvillea & windmill views',
          x: -3.65,
          y: -2.65,
          yaw: -2.44,
          pitch: 0.16
        }
      }
    },
    valle: {
      id: 'valle',
      title: 'Alpine Valley',
      heroHouseCell: { x: 4, y: 12 },
      heroViewCell: { x: 5, y: 5 },
      defaultSpot: 'sendero',
      defaultCaption: 'Drag 360° · Visit Trail, Cabin, Bridge, Summit, or wall frames',
      spotOrder: ['sendero', 'refugio', 'puente', 'cima'],
      spots: {
        sendero: {
          id: 'sendero',
          pill: 'Trail',
          label: 'Lakeside Trail',
          sub: 'Alpine lake, wooden cabin & snowy peaks',
          x: -1.85,
          y: 8.20,
          yaw: -2.98,
          pitch: 0.08
        },
        refugio: {
          id: 'refugio',
          pill: 'Cabin',
          label: 'Timber Cabin',
          sub: 'Warm hearth and Real Wall frames',
          x: -5.06,
          y: 3.50,
          yaw: 1.57,
          pitch: -0.02
        },
        puente: {
          id: 'puente',
          pill: 'Bridge',
          label: 'Alpine Bridge',
          sub: 'Timber footbridge over stream & boats',
          x: 0.55,
          y: -0.50,
          yaw: 0.08,
          pitch: -0.06
        },
        cima: {
          id: 'cima',
          pill: 'Summit',
          label: 'Pine Overlook',
          sub: 'Alpine bench beneath snowy peaks',
          x: -3.50,
          y: -3.50,
          yaw: 0.58,
          pitch: -0.14
        }
      }
    }
  };

  function tx(esVal, enVal) {
    return window.SplashI18n ? window.SplashI18n.pick(esVal, enVal) : esVal;
  }

  const BIOME_TEXT_I18N = {
    costa: {
      es: {
        title: 'Costa Mediterránea',
        defaultCaption: 'Arrastra 360° · Toca Faro, Muelle, Casa, Árbol o cuadros',
        spots: {
          faro:   { pill: 'Faro',   label: 'El Faro',             sub: 'Patio de piedra y torre al mar' },
          cala:   { pill: 'Muelle', label: 'El Muelle',           sub: 'Tablones sobre la cala y veleros' },
          plaza:  { pill: 'Casa',   label: 'Casa Mediterránea',   sub: 'Museo del Muro Real y arcos al mar' },
          molino: { pill: 'Árbol',  label: 'El Árbol del Mirador',sub: 'Olivo, buganvilla y vistas al molino' }
        }
      },
      en: {
        title: 'Mediterranean Coast',
        defaultCaption: 'Drag 360° · Tap Lighthouse, Pier, House, Tree, or wall frames',
        spots: {
          faro:   { pill: 'Lighthouse', label: 'The Lighthouse',      sub: 'Stone courtyard and coastal tower' },
          cala:   { pill: 'Pier',       label: 'The Pier',            sub: 'Wooden boardwalk over the blue cove' },
          plaza:  { pill: 'House',      label: 'Mediterranean House', sub: 'Real Wall gallery and arches to the sea' },
          molino: { pill: 'Tree',       label: 'Overlook Tree',       sub: 'Olive tree, bougainvillea & windmill views' }
        }
      }
    },
    valle: {
      es: {
        title: 'Valle Alpino',
        defaultCaption: 'Arrastra 360° · Visita Sendero, Refugio, Puente, Cima o cuadros',
        spots: {
          sendero: { pill: 'Sendero', label: 'Sendero del Lago',   sub: 'Lago alpino, refugio y picos nevados' },
          refugio: { pill: 'Refugio', label: 'Refugio de Madera',  sub: 'Chimenea cálida y cuadros del Muro Real' },
          puente:  { pill: 'Puente',  label: 'Puente Alpino',      sub: 'Pasarela sobre el arroyo y las barcas' },
          cima:    { pill: 'Cima',    label: 'Mirador de Pinos',   sub: 'Banco alpino bajo los picos nevados' }
        }
      },
      en: {
        title: 'Alpine Valley',
        defaultCaption: 'Drag 360° · Visit Trail, Cabin, Bridge, Summit, or wall frames',
        spots: {
          sendero: { pill: 'Trail',  label: 'Lakeside Trail', sub: 'Alpine lake, wooden cabin & snowy peaks' },
          refugio: { pill: 'Cabin',  label: 'Timber Cabin',   sub: 'Warm hearth and Real Wall frames' },
          puente:  { pill: 'Bridge', label: 'Alpine Bridge',  sub: 'Timber footbridge over stream & boats' },
          cima:    { pill: 'Summit', label: 'Pine Overlook',  sub: 'Alpine bench beneath snowy peaks' }
        }
      }
    }
  };

  function applyLanguage(lang) {
    const useEn = lang ? lang === 'en' : !!(window.SplashI18n && window.SplashI18n.isEn());
    ['costa', 'valle'].forEach(bid => {
      const cfg = BIOME_CONFIGS[bid];
      const pack = BIOME_TEXT_I18N[bid] && (useEn ? BIOME_TEXT_I18N[bid].en : BIOME_TEXT_I18N[bid].es);
      if (!cfg || !pack) return;
      cfg.title = pack.title;
      cfg.defaultCaption = pack.defaultCaption;
      Object.keys(pack.spots).forEach(sid => {
        if (cfg.spots[sid]) {
          cfg.spots[sid].pill = pack.spots[sid].pill;
          cfg.spots[sid].label = pack.spots[sid].label;
          cfg.spots[sid].sub = pack.spots[sid].sub;
        }
      });
    });
    if (samplePostcards) {
      samplePostcards = null;
    }
  }

  let isOpen = false;
  let activeBiome = 'costa';
  let spotId = 'faro';
  let sceneData = null;
  let cam = {
    x: -2.35,
    y: -2.55,
    z: 2.8,
    yaw: -3.08,
    pitch: 0.22,
    targetX: -2.35,
    targetY: -2.55,
    targetYaw: -3.08,
    targetPitch: 0.22
  };
  const moveState = { fwd: false, back: false, left: false, right: false };

  let lightMode = 'auto';
  let liveWeather = 'clear';
  let weatherOverride = 'auto';
  let soundOn = false;
  let beaconOn = true;
  let lampOn = true;
  let fireOn = true;
  let beaconPulse = 0;
  let petalTimer = 0;
  let sailAngle = 0.45;
  let windmillSpeed = 0.85;
  let boatPush = [0, 0];
  let userPhotos = [];
  let resolvedGalleryPhotos = [];
  const photoImgCache = new Map();
  let samplePostcards = null;

  let rafId = null;
  let lastFrameMs = 0;
  let pointerId = null;
  let lastPt = null;
  let tapStart = null;
  let hotspots = [];
  let eventsBound = false;

  function clamp(v, lo, hi) {
    return Math.max(lo, Math.min(hi, v));
  }

  function getBiomeConfig() {
    return BIOME_CONFIGS[activeBiome] || BIOME_CONFIGS.costa;
  }

  function getGridN() {
    return (sceneData && sceneData.N) ? sceneData.N : DEFAULT_N;
  }

  function getHalf() {
    return getGridN() * 0.5;
  }

  function resolvePhase() {
    if (lightMode !== 'auto') return lightMode;
    return window.BroteMascot && window.BroteMascot.getTimeOfDayPhase
      ? window.BroteMascot.getTimeOfDayPhase()
      : 'day';
  }

  function resolveWeather() {
    const w = weatherOverride === 'auto' ? liveWeather : weatherOverride;
    if (w === 'radiant' || w === 'mist') return w;
    return 'clear';
  }

  function setCaption(msg) {
    const el = document.getElementById('explore-caption');
    if (el) el.textContent = msg;
  }

  function buildSamplePostcards() {
    if (samplePostcards) return samplePostcards;
    const specs = [
      {
        top: '#82C3E4', hor: '#F2E7CF', hillA: '#558E5B', hillB: '#3D7348',
        water: '#5FB8D1', accent: '#FFFDF8',
        label: tx('Postal alpina', 'Alpine postcard'),
        goalTitle: tx('Sin fotos propias aún', 'No personal photos yet')
      },
      {
        top: '#F4A97E', hor: '#FCE2AE', hillA: '#D9C08C', hillB: '#BD9B66',
        water: '#4F9BB3', accent: '#FFF4C9',
        label: tx('Atardecer en calma', 'Calm sunset'),
        goalTitle: tx('Tu futuro recuerdo', 'Your future memory')
      },
      {
        top: '#9ED1BA', hor: '#EDF5E4', hillA: '#7BAE76', hillB: '#538654',
        water: '#77C7C0', accent: '#F28E76',
        label: tx('Pausa analógica', 'Analog break'),
        goalTitle: tx('Cuaderno y descanso', 'Notebook & rest')
      }
    ];
    samplePostcards = specs.map((sp) => {
      let url = '';
      try {
        const c = document.createElement('canvas');
        c.width = 192;
        c.height = 144;
        const g = c.getContext('2d');
        const sky = g.createLinearGradient(0, 0, 0, 88);
        sky.addColorStop(0, sp.top);
        sky.addColorStop(1, sp.hor);
        g.fillStyle = sky;
        g.fillRect(0, 0, 192, 144);

        g.fillStyle = sp.accent;
        g.beginPath();
        g.arc(140, 38, 16, 0, Math.PI * 2);
        g.fill();

        g.fillStyle = sp.hillB;
        g.beginPath();
        g.moveTo(0, 92);
        g.quadraticCurveTo(54, 48, 114, 88);
        g.quadraticCurveTo(156, 60, 192, 86);
        g.lineTo(192, 144);
        g.lineTo(0, 144);
        g.closePath();
        g.fill();

        g.fillStyle = sp.hillA;
        g.beginPath();
        g.moveTo(0, 104);
        g.quadraticCurveTo(48, 82, 98, 104);
        g.quadraticCurveTo(148, 90, 192, 102);
        g.lineTo(192, 144);
        g.lineTo(0, 144);
        g.closePath();
        g.fill();

        g.fillStyle = sp.water;
        g.fillRect(0, 108, 192, 36);
        g.fillStyle = 'rgba(255,255,255,0.42)';
        g.fillRect(28, 118, 46, 3);
        g.fillRect(102, 127, 54, 3);
        url = c.toDataURL('image/png');
      } catch (_) {}
      return {
        src: url,
        dateKey: '',
        label: sp.label,
        goalTitle: sp.goalTitle,
        isPlaceholder: true
      };
    });
    return samplePostcards;
  }

  function refreshGalleryPhotos(rawPhotos) {
    userPhotos = Array.isArray(rawPhotos) ? rawPhotos.filter(p => p && p.src) : [];
    const samples = buildSamplePostcards();
    const out = [];
    for (let i = 0; i < 3; i++) {
      if (userPhotos[i]) {
        out.push({
          src: userPhotos[i].src,
          dateKey: userPhotos[i].dateKey || '',
          label: userPhotos[i].label || tx('Muro Real', 'Real Wall'),
          goalTitle: userPhotos[i].goalTitle || tx('Momento sin pantallas', 'Screen-free moment'),
          isPlaceholder: false
        });
      } else {
        out.push(samples[i]);
      }
    }
    resolvedGalleryPhotos = out;
    out.forEach(item => {
      if (!item || !item.src || photoImgCache.has(item.src)) return;
      const img = new Image();
      img.src = item.src;
      photoImgCache.set(item.src, img);
    });
  }

  function getCachedPhotoImage(src) {
    if (!src) return null;
    if (!photoImgCache.has(src)) {
      const img = new Image();
      img.src = src;
      photoImgCache.set(src, img);
    }
    return photoImgCache.get(src);
  }

  function openPhotoModal(photo) {
    if (!photo) return;
    const overlay = document.getElementById('explore-photo-overlay');
    const imgEl = document.getElementById('explore-photo-img');
    const dateEl = document.getElementById('explore-photo-date');
    const titleEl = document.getElementById('explore-photo-title');
    const hintEl = document.getElementById('explore-photo-hint');
    if (!overlay || !imgEl) return;

    imgEl.src = photo.src || '';
    if (dateEl) dateEl.textContent = photo.label || tx('Muro Real', 'Real Wall');
    if (titleEl) titleEl.textContent = photo.goalTitle || tx('Tu recuerdo analógico', 'Your analog memory');
    if (hintEl) {
      hintEl.textContent = photo.isPlaceholder
        ? tx('Cuando completes un reto y subas una foto al Muro Real, aparecerá colgada aquí dentro.', 'Once you complete a goal and add a photo to your Real Wall, it will hang here on the wall.')
        : tx('Cuadro colgado automáticamente desde tu calendario de Muro Real.', 'Framed automatically from your Real Wall calendar.');
    }
    overlay.classList.remove('hidden');
  }

  function closePhotoModal() {
    const overlay = document.getElementById('explore-photo-overlay');
    if (overlay) overlay.classList.add('hidden');
  }

  function rebuildSpotsDock() {
    const dock = document.getElementById('explore-spots-dock');
    if (!dock) return;
    const cfg = getBiomeConfig();
    dock.innerHTML = cfg.spotOrder.map(id => {
      const sp = cfg.spots[id];
      const activeCls = id === spotId ? ' active' : '';
      return `<button type="button" class="explore-spot-pill${activeCls}" data-spot="${id}">${sp.pill}</button>`;
    }).join('');
  }

  function updateUI() {
    const cfg = getBiomeConfig();
    const spot = cfg.spots[spotId] || cfg.spots[cfg.defaultSpot];
    const modal = document.getElementById('modal-explore-costa');
    if (modal) modal.dataset.biome = activeBiome;

    const titleEl = document.getElementById('explore-biome-title');
    if (titleEl) titleEl.textContent = cfg.title;

    const subEl = document.getElementById('explore-spot-sub');
    if (subEl && spot) subEl.textContent = `${spot.label} · ${spot.sub}`;

    document.querySelectorAll('#explore-spots-dock .explore-spot-pill').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.spot === spotId);
    });

    const wBtn = document.getElementById('explore-weather-btn');
    const wIcon = document.getElementById('explore-weather-icon');
    const activeW = resolveWeather();
    if (wBtn && wIcon) {
      wBtn.classList.toggle('active', weatherOverride !== 'auto' || activeW !== 'clear');
      wIcon.textContent =
        activeW === 'radiant' ? 'wb_sunny' :
        activeW === 'mist' ? 'foggy' : 'partly_cloudy_day';
    }

    const lightBtn = document.getElementById('explore-light-btn');
    const lightIcon = document.getElementById('explore-light-icon');
    if (lightBtn && lightIcon) {
      lightBtn.classList.toggle('active', lightMode !== 'auto');
      lightIcon.textContent =
        lightMode === 'day' ? 'wb_sunny' :
        lightMode === 'sunset' ? 'wb_twilight' :
        lightMode === 'night' ? 'dark_mode' : 'brightness_auto';
    }

    const soundBtn = document.getElementById('explore-sound-btn');
    const soundIcon = document.getElementById('explore-sound-icon');
    if (soundBtn && soundIcon) {
      soundBtn.classList.toggle('active', soundOn);
      soundIcon.textContent = soundOn ? 'volume_up' : 'volume_off';
    }
  }

  function parseColorToRgb(col) {
    if (!col) return { r: 220, g: 220, b: 220 };
    const s = String(col).trim();
    if (s.startsWith('rgb')) {
      const m = s.match(/rgba?\(\s*([\d.]+)\s*,\s*([\d.]+)\s*,\s*([\d.]+)/i);
      if (m) {
        return {
          r: Math.max(0, Math.min(255, Number(m[1]))),
          g: Math.max(0, Math.min(255, Number(m[2]))),
          b: Math.max(0, Math.min(255, Number(m[3])))
        };
      }
    }
    let clean = s.replace('#', '');
    if (clean.length === 3) {
      clean = clean.split('').map(c => c + c).join('');
    }
    const n = parseInt(clean, 16);
    if (!Number.isFinite(n)) return { r: 220, g: 220, b: 220 };
    return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 };
  }

  function rgbStr(r, g, b, alpha = 1) {
    const rr = Math.max(0, Math.min(255, Math.round(r)));
    const gg = Math.max(0, Math.min(255, Math.round(g)));
    const bb = Math.max(0, Math.min(255, Math.round(b)));
    return alpha >= 0.999 ? `rgb(${rr},${gg},${bb})` : `rgba(${rr},${gg},${bb},${alpha.toFixed(2)})`;
  }

  function fogAndTintColor(baseColor, nx, ny, nz, distSq, phase, fogRgb, unlit = false, weather = 'clear') {
    const rgb = parseColorToRgb(baseColor);
    let r = rgb.r;
    let g = rgb.g;
    let b = rgb.b;

    if (!unlit) {
      const awake = phase !== 'night';
      const dot = nx * SUN.x + ny * SUN.y + nz * SUN.z;
      const pct = dot * 24 - 8;
      const warm = awake && dot > 0.2 ? (weather === 'radiant' ? 12 : 8) : 0;
      const factor = 1 + pct / 100;
      r = clamp(r * factor + warm, 0, 255);
      g = clamp(g * factor + warm * 0.8, 0, 255);
      b = clamp(b * factor + warm * 0.35, 0, 255);

      if (phase === 'dawn') {
        r = r * 1.03 + 12;
        g = g * 0.96 + 5;
        b = b * 0.91;
      } else if (phase === 'sunset') {
        r = r * 1.07 + 16;
        g = g * 0.88 + 2;
        b = b * 0.78;
      } else if (phase === 'night') {
        r = r * 0.56 + 14;
        g = g * 0.62 + 20;
        b = b * 0.85 + 32;
      }
      if (weather === 'mist') {
        r = r * 0.92 + fogRgb[0] * 0.08;
        g = g * 0.92 + fogRgb[1] * 0.08;
        b = b * 0.92 + fogRgb[2] * 0.08;
      }
    }

    const dist = Math.sqrt(Math.max(0, distSq));
    let fogT;
    if (weather === 'mist') {
      fogT = unlit ? clamp((dist - 12) / 22, 0, 0.38) : clamp((dist - 4.4) / 15.0, 0, 0.72);
    } else {
      fogT = unlit ? clamp((dist - 22) / 36, 0, 0.30) : clamp((dist - 16) / 28, 0, 0.48);
    }
    if (fogT > 0.005) {
      r = r + (fogRgb[0] - r) * fogT;
      g = g + (fogRgb[1] - g) * fogT;
      b = b + (fogRgb[2] - b) * fogT;
    }
    return rgbStr(r, g, b);
  }

  function buildFallbackCostaScene() {
    const N = DEFAULT_N;
    const cells = [];
    for (let ix = 0; ix < N; ix++) {
      const col = [];
      const nx = ix / (N - 1);
      for (let iy = 0; iy < N; iy++) {
        const ny = iy / (N - 1);
        const coastLine = 0.44 + 0.14 * Math.sin(ny * Math.PI * 1.4) + 0.08 * ny;
        const isWater = nx > coastLine;
        const depth = nx - coastLine;
        const isPath = !isWater && Math.abs(nx - 0.18) < 0.048 && ny > 0.14 && ny < 0.84;
        const rawH = 0.95 + Math.max(0, coastLine - nx) * 3.4 + (1 - ny) * 0.72;
        const wz = isWater ? Math.max(0.22, 0.40 - depth * 0.32) : Math.max(0.54, rawH);
        const color = isWater
          ? (depth > 0.28 ? '#4B9FB5' : depth > 0.12 ? '#77C4D4' : '#9FDCE6')
          : (isPath ? '#EAD6B1' : (depth > -0.11 ? '#EEDBB8' : '#E6CFA5'));
        col.push({ ix, iy, key: `${ix},${iy}`, wz, color, isWater, isPath, rawH, depth });
      }
      cells.push(col);
    }
    return {
      N,
      biomeId: 'costa',
      points: 80,
      cells,
      layout: {
        landmarkCell: { x: 6, y: 3 },
        costaWindmillCell: { x: 2, y: 4 },
        houseRanks: { '1,8': 1, '2,12': 2, '5,7': 3, '4,10': 4 },
        treeRanks: { '4,4': 1, '5,12': 2, '1,6': 3 },
        rockRanks: { '7,14': 1, '8,5': 2 },
        lanternRanks: { '3,8': 1, '5,5': 2 },
        shimmerRanks: { '12,5': 1, '15,12': 2 }
      },
      budgets: {
        houses: 11, trees: 10, rocks: 5, lanterns: 5, shimmers: 10,
        showBridge: true, showSecondary: true, boats: 2
      }
    };
  }

  function buildFallbackValleScene() {
    const N = DEFAULT_N;
    const cells = [];
    for (let ix = 0; ix < N; ix++) {
      const col = [];
      const nx = ix / (N - 1);
      for (let iy = 0; iy < N; iy++) {
        const ny = iy / (N - 1);
        const riverCenter = 0.50 + 0.08 * Math.sin(ny * 4.6);
        const distRiver = Math.abs(nx - riverCenter);
        const lakeDist = Math.hypot(nx - 0.50, ny - 0.72);
        const isWater = (ny > 0.34 && distRiver < 0.064) || lakeDist < 0.145;
        const isPath = !isWater && ny > 0.42 && ny < 0.88 && Math.abs(nx - 0.28) < 0.042;
        const valleyWalls = Math.pow(Math.abs(nx - 0.50) * 1.85, 1.55) * 0.95;
        const backSlope = Math.max(0, 1 - ny) * 0.65;
        const mainPeak = Math.max(0, 1 - Math.hypot((nx - 0.50) * 1.15, (ny - 0.12) * 1.35) * 1.85);
        let rawH = 0.68 + valleyWalls + backSlope + Math.pow(mainPeak, 1.25) * 2.55;
        if (lakeDist < 0.20) rawH = Math.min(rawH, 0.92);
        const wz = isWater ? 0.34 : Math.max(0.54, rawH * 1.15);
        let color = '#6AB168';
        if (isWater) color = lakeDist < 0.085 ? '#46A4C2' : '#6CC6DF';
        else if (isPath) color = '#DFD6C3';
        else if (rawH > 3.12) color = '#F8FAFC';
        else if (rawH > 2.75) color = '#9AA7AE';
        else if (rawH > 1.85) color = '#44824A';
        else if (rawH > 1.30) color = '#569B58';
        col.push({ ix, iy, key: `${ix},${iy}`, wz, color, isWater, isPath, rawH, distRiver });
      }
      cells.push(col);
    }
    return {
      N,
      biomeId: 'valle',
      points: 80,
      cells,
      layout: {
        landmarkCell: { x: 4, y: 12 },
        houseRanks: { '3,9': 1, '13,11': 2, '14,14': 3 },
        treeRanks: { '3,6': 1, '13,7': 2, '12,13': 3, '3,15': 4 },
        rockRanks: { '6,3': 1, '11,4': 2 },
        lanternRanks: { '5,11': 1, '11,10': 2 },
        shimmerRanks: { '8,11': 1, '10,13': 2 }
      },
      budgets: {
        houses: 9, trees: 20, rocks: 6, lanterns: 6, shimmers: 10,
        showBridge: true, showSecondary: true, boats: 2
      }
    };
  }

  function isBridgeOrPierCell(ix, iy) {
    if (!sceneData || !sceneData.budgets || !sceneData.budgets.showBridge) return false;
    if (activeBiome === 'valle') {
      return iy === 8 && (ix === 8 || ix === 9 || ix === 10);
    }
    return iy === 8 && (ix === 10 || ix === 11 || ix === 12);
  }

  function isClearCourtyardCell(ix, iy) {
    if (activeBiome === 'valle') {
      return (
        (ix === 3 && iy === 12) ||
        (ix === 5 && (iy === 5 || iy === 11 || iy === 12 || iy === 13 || iy === 14 || iy === 15)) ||
        (ix === 6 && (iy === 14 || iy === 15 || iy === 16 || iy === 17)) ||
        (ix === 7 && (iy === 15 || iy === 16 || iy === 17)) ||
        (ix === 8 && iy === 8) ||
        (ix === 10 && iy === 8)
      );
    }
    return (
      (ix === 3 && iy === 10) ||
      (ix === 6 && (iy === 4 || iy === 5 || iy === 6)) ||
      (ix === 5 && (iy === 4 || iy === 5 || iy === 6 || iy === 10)) ||
      (ix === 4 && iy === 5) ||
      (ix === 10 && iy === 8)
    );
  }

  function getGroundInfo(wx, wy) {
    if (!sceneData) return { walkable: true, z: 1.0 };
    const gridN = getGridN();
    const half = gridN * 0.5;
    const ix = clamp(Math.floor(wx + half), 0, gridN - 1);
    const iy = clamp(Math.floor(wy + half), 0, gridN - 1);
    const cell = sceneData.cells[ix] && sceneData.cells[ix][iy];
    if (!cell) return { walkable: true, z: 1.0 };

    const isSpan = isBridgeOrPierCell(ix, iy);
    if (cell.isWater && !isSpan) {
      return { walkable: false, z: cell.wz };
    }
    if (isSpan) {
      return { walkable: true, z: Math.max(cell.wz + 0.30, 0.66) };
    }

    const cfg = getBiomeConfig();
    const hh = cfg.heroHouseCell;
    if (hh) {
      const hwx0 = hh.x - half;
      const hwy0 = hh.y - half;
      if (wx >= hwx0 - 0.24 && wx <= hwx0 + 1.15 && wy >= hwy0 - 0.08 && wy <= hwy0 + 1.08) {
        const baseCell = sceneData.cells[hh.x] && sceneData.cells[hh.x][hh.y];
        const bz = baseCell ? baseCell.wz : cell.wz;
        return { walkable: true, z: bz + 0.07 };
      }
    }
    return { walkable: true, z: cell.wz };
  }

  function isBlockedByBuilding(wx, wy) {
    if (!sceneData) return false;
    const gridN = getGridN();
    const half = gridN * 0.5;
    const { layout, budgets, cells } = sceneData;
    const ix = Math.floor(wx + half);
    const iy = Math.floor(wy + half);
    if (ix < 0 || ix >= gridN || iy < 0 || iy >= gridN) return true;
    const key = `${ix},${iy}`;
    const cell = cells[ix] && cells[ix][iy];
    if (!cell) return true;
    if (cell.isWater && !isBridgeOrPierCell(ix, iy)) return true;

    const fx = (wx + half) - ix;
    const fy = (wy + half) - iy;
    const cfg = getBiomeConfig();

    const hh = cfg.heroHouseCell;
    if (hh) {
      const hwx0 = hh.x - half;
      const hwy0 = hh.y - half;
      if (wx >= hwx0 - 0.30 && wx <= hwx0 + 1.18 && wy >= hwy0 - 0.12 && wy <= hwy0 + 1.12) {
        return wx < hwx0 - 0.12;
      }
    }

    if (cfg.heroTreeCell && ix === cfg.heroTreeCell.x && iy === cfg.heroTreeCell.y) {
      return fx > 0.36 && fx < 0.64 && fy > 0.36 && fy < 0.64;
    }
    if (cfg.heroViewCell && ix === cfg.heroViewCell.x && iy === cfg.heroViewCell.y) {
      return fx < 0.32 && fy < 0.32;
    }
    if (isClearCourtyardCell(ix, iy) || isBridgeOrPierCell(ix, iy)) return false;

    const isLandmark = activeBiome === 'costa' && layout.landmarkCell && layout.landmarkCell.x === ix && layout.landmarkCell.y === iy;
    const isWindmill = activeBiome === 'costa' && layout.costaWindmillCell && layout.costaWindmillCell.x === ix && layout.costaWindmillCell.y === iy && budgets.showSecondary;
    const isHouse = Boolean(layout.houseRanks[key] && layout.houseRanks[key] <= budgets.houses && !cell.isPath && !cell.isWater);

    if (isLandmark || isWindmill || isHouse) {
      if (fx > 0.16 && fx < 0.84 && fy > 0.16 && fy < 0.84) {
        return true;
      }
    }
    return false;
  }

  function getWalkHeightAt(wx, wy) {
    const info = getGroundInfo(wx, wy);
    return (info.walkable ? info.z : 0.95) + 0.98;
  }

  function transformPoint(wx, wy, wz, sinY, cosY, sinP, cosP) {
    const dx = wx - cam.x;
    const dy = wy - cam.y;
    const dz = wz - cam.z;
    const rx = dx * cosY - dy * sinY;
    const ry = dx * sinY + dy * cosY;
    const fz = ry * cosP + dz * sinP;
    const uz = -ry * sinP + dz * cosP;
    return { rx, fz, uz };
  }

  function clipPolyAgainstNear(pts) {
    const out = [];
    for (let i = 0; i < pts.length; i++) {
      const a = pts[i];
      const b = pts[(i + 1) % pts.length];
      const aIn = a.fz >= NEAR;
      const bIn = b.fz >= NEAR;
      if (aIn && bIn) {
        out.push(b);
      } else if (aIn && !bIn) {
        const t = (NEAR - a.fz) / (b.fz - a.fz);
        out.push({
          rx: a.rx + (b.rx - a.rx) * t,
          fz: NEAR,
          uz: a.uz + (b.uz - a.uz) * t
        });
      } else if (!aIn && bIn) {
        const t = (NEAR - a.fz) / (b.fz - a.fz);
        out.push({
          rx: a.rx + (b.rx - a.rx) * t,
          fz: NEAR,
          uz: a.uz + (b.uz - a.uz) * t
        });
        out.push(b);
      }
    }
    return out;
  }

  function projectVert(v, w, h, focal) {
    return {
      x: w * 0.5 + (v.rx / v.fz) * focal,
      y: h * 0.50 - (v.uz / v.fz) * focal
    };
  }

  function projectWorldAnchor(wx, wy, wz, sinY, cosY, sinP, cosP, w, h, focal) {
    const p = transformPoint(wx, wy, wz, sinY, cosY, sinP, cosP);
    if (p.fz < NEAR) return null;
    return projectVert(p, w, h, focal);
  }

  function drawTexturedTri(ctx, img, sx0, sy0, sx1, sy1, sx2, sy2, x0, y0, x1, y1, x2, y2, expandMid) {
    const denom = sx0 * (sy2 - sy1) - sx1 * sy2 + sx2 * sy1 + (sx1 - sx2) * sy0;
    if (Math.abs(denom) < 1e-6) return;
    const a = -(sy0 * (x2 - x1) - sy1 * x2 + sy2 * x1 + (sy1 - sy2) * x0) / denom;
    const b = -(sy0 * (y2 - y1) - sy1 * y2 + sy2 * y1 + (sy1 - sy2) * y0) / denom;
    const c = (sx0 * (x2 - x1) - sx1 * x2 + sx2 * x1 + (sx1 - sx2) * x0) / denom;
    const d = (sx0 * (y2 - y1) - sx1 * y2 + sx2 * y1 + (sx1 - sx2) * y0) / denom;
    const e = (sx0 * (sy2 * x1 - sy1 * x2) + sy0 * (sx1 * x2 - sx2 * x1) + (sx2 * sy1 - sx1 * sy2) * x0) / denom;
    const f = (sx0 * (sy2 * y1 - sy1 * y2) + sy0 * (sx1 * y2 - sx2 * y1) + (sx2 * sy1 - sx1 * sy2) * y0) / denom;

    // Suave solape en la diagonal compartida para evitar costura antialiasing
    let cx1 = x1, cy1 = y1;
    if (expandMid) {
      const mx = (x0 + x2) * 0.5;
      const my = (y0 + y2) * 0.5;
      const dx = x1 - mx;
      const dy = y1 - my;
      const len = Math.hypot(dx, dy) || 1;
      cx1 -= (dx / len) * 0.6;
      cy1 -= (dy / len) * 0.6;
    }

    ctx.save();
    ctx.beginPath();
    ctx.moveTo(x0, y0);
    ctx.lineTo(cx1, cy1);
    ctx.lineTo(x2, y2);
    ctx.closePath();
    ctx.clip();
    ctx.transform(a, b, c, d, e, f);
    ctx.drawImage(img, 0, 0);
    ctx.restore();
  }

  function updateMovement(dt) {
    const half = getHalf();
    const maxX = activeBiome === 'costa' ? half - 1.4 : half - 0.55;
    const spd = 2.8 * dt;
    let moveFwd = 0;
    let moveRight = 0;
    if (moveState.fwd) moveFwd += 1;
    if (moveState.back) moveFwd -= 1;
    if (moveState.right) moveRight += 1;
    if (moveState.left) moveRight -= 1;

    if (moveFwd !== 0 || moveRight !== 0) {
      const mag = Math.hypot(moveFwd, moveRight);
      const nf = moveFwd / mag;
      const nr = moveRight / mag;
      const stepX = (Math.sin(cam.yaw) * nf + Math.cos(cam.yaw) * nr) * spd;
      const stepY = (Math.cos(cam.yaw) * nf - Math.sin(cam.yaw) * nr) * spd;

      const candX = clamp(cam.targetX + stepX, -half + 0.55, maxX);
      const candY = clamp(cam.targetY + stepY, -half + 0.55, half - 0.55);
      const wasBlocked = isBlockedByBuilding(cam.targetX, cam.targetY);
      if (!isBlockedByBuilding(candX, cam.targetY) || wasBlocked) {
        cam.targetX = candX;
      }
      if (!isBlockedByBuilding(cam.targetX, candY) || wasBlocked) {
        cam.targetY = candY;
      }
    }

    cam.x += (cam.targetX - cam.x) * Math.min(1, dt * 8.5);
    cam.y += (cam.targetY - cam.y) * Math.min(1, dt * 8.5);
    cam.yaw += (cam.targetYaw - cam.yaw) * Math.min(1, dt * 11);
    cam.pitch += (cam.targetPitch - cam.pitch) * Math.min(1, dt * 11);
    const goalZ = getWalkHeightAt(cam.x, cam.y);
    cam.z += (goalZ - cam.z) * Math.min(1, dt * 7.5);
  }

  function updateSpatialAudioForCamera() {
    if (!soundOn || !window.BroteAudio || typeof window.BroteAudio.updateSpatial3D !== 'function') return;
    const cfg = getBiomeConfig();
    const half = getHalf();
    const hh = cfg.heroHouseCell;
    const hwx0 = hh.x - half;
    const hwy0 = hh.y - half;

    // Indoor factor suave al acercarse/entrar en la Casa (Costa) o Refugio (Valle)
    const cxH = clamp(cam.x, hwx0 - 0.24, hwx0 + 1.12);
    const cyH = clamp(cam.y, hwy0 - 0.06, hwy0 + 1.06);
    const distHouse = Math.hypot(cam.x - cxH, cam.y - cyH);
    const indoorFactor = clamp(1 - distHouse / 0.85, 0, 1);

    if (activeBiome === 'costa') {
      const distPier = Math.hypot(cam.x - 1.5, cam.y + 0.5);
      const waterProx = clamp(Math.max(1 - distPier / 4.2, (cam.x + 1.2) / 4.8), 0.12, 1.0);
      const treeProx = clamp(1 - Math.hypot(cam.x + 4.5, cam.y + 4.5) / 2.6, 0, 1);
      const seaYaw = Math.atan2(3.5 - cam.x, -0.5 - cam.y);
      const relYaw = ((seaYaw - cam.yaw + Math.PI * 3) % (Math.PI * 2)) - Math.PI;
      const pan = clamp(Math.sin(relYaw) * 0.85, -0.82, 0.82);

      window.BroteAudio.updateSpatial3D({
        biome: 'costa',
        pan,
        waterProx,
        indoorFactor,
        treeProx,
        fireProx: 0
      });
    } else {
      const distLake = Math.hypot(cam.x - 0.0, cam.y - 3.6);
      const distBridge = Math.hypot(cam.x - 0.5, cam.y + 0.5);
      const waterProx = clamp(Math.max(1 - distLake / 4.6, 1 - distBridge / 3.8), 0.10, 1.0);
      const treeProx = clamp(1 - Math.hypot(cam.x + 3.5, cam.y + 3.5) / 3.5, 0.10, 1.0);
      const fireProx = indoorFactor * (fireOn ? 1.0 : 0.08);
      const waterYaw = Math.atan2(0.2 - cam.x, 2.0 - cam.y);
      const relYaw = ((waterYaw - cam.yaw + Math.PI * 3) % (Math.PI * 2)) - Math.PI;
      const pan = clamp(Math.sin(relYaw) * 0.80, -0.80, 0.80);

      window.BroteAudio.updateSpatial3D({
        biome: 'valle',
        pan,
        waterProx,
        indoorFactor,
        treeProx,
        fireProx
      });
    }
  }

  function drawSkyAndBackdrop(ctx, w, h, focal, phase, t, weather) {
    const isValle = activeBiome === 'valle';
    const skyMap = {
      day: {
        top: isValle ? '#57A4DC' : '#5DAEE0',
        mid: isValle ? '#9AD1EE' : '#9CD5EE',
        hor: isValle ? '#DFEFF4' : '#DDF0F6',
        groundFar: isValle ? '#5B985D' : '#469EB7',
        groundMid: isValle ? '#73B270' : '#6BBFD0',
        fog: isValle ? [223, 239, 244] : [221, 240, 246]
      },
      dawn: {
        top: '#6879AC',
        mid: '#C7A6BC',
        hor: '#F8D4BA',
        groundFar: isValle ? '#5F8466' : '#4B829D',
        groundMid: isValle ? '#779E78' : '#6FA9B8',
        fog: [248, 212, 186]
      },
      sunset: {
        top: '#3E3E68',
        mid: '#AF6579',
        hor: '#F29C70',
        groundFar: isValle ? '#4E6B52' : '#395A77',
        groundMid: isValle ? '#668764' : '#597C93',
        fog: [242, 156, 112]
      },
      night: {
        top: '#090F20',
        mid: '#14203C',
        hor: '#223357',
        groundFar: isValle ? '#172E24' : '#102438',
        groundMid: isValle ? '#1F3B2D' : '#18344B',
        fog: [34, 51, 87]
      }
    };
    const skyPal = { ...(skyMap[phase] || skyMap.day) };

    if (weather === 'mist') {
      if (phase === 'night') {
        skyPal.top = '#172234';
        skyPal.mid = '#24344B';
        skyPal.hor = '#354860';
        skyPal.fog = [53, 72, 96];
      } else {
        skyPal.top = '#8AA9B8';
        skyPal.mid = '#B7CAD3';
        skyPal.hor = '#DDE5E8';
        skyPal.groundFar = isValle ? '#709877' : '#719CA9';
        skyPal.groundMid = isValle ? '#85AA88' : '#89B2BD';
        skyPal.fog = [221, 229, 232];
      }
    }

    const horizonY = h * 0.50 + Math.tan(cam.pitch) * focal;
    const hClamped = clamp(horizonY, -h * 0.4, h * 1.4);

    const skyGrad = ctx.createLinearGradient(0, 0, 0, Math.max(h * 0.65, hClamped));
    skyGrad.addColorStop(0, skyPal.top);
    skyGrad.addColorStop(0.55, skyPal.mid);
    skyGrad.addColorStop(1, skyPal.hor);
    ctx.fillStyle = skyGrad;
    ctx.fillRect(0, 0, w, Math.max(h, hClamped + 4));

    if (hClamped < h) {
      const groundGrad = ctx.createLinearGradient(0, Math.max(0, hClamped - 2), 0, h);
      groundGrad.addColorStop(0, skyPal.hor);
      groundGrad.addColorStop(0.14, skyPal.groundFar);
      groundGrad.addColorStop(1, skyPal.groundMid);
      ctx.fillStyle = groundGrad;
      ctx.fillRect(0, Math.max(0, hClamped - 2), w, h - Math.max(0, hClamped - 2));
    }

    const celYaw = phase === 'night' ? -0.6 : 1.15;
    const relYaw = ((celYaw - cam.yaw + Math.PI * 3) % (Math.PI * 2)) - Math.PI;
    if (Math.abs(relYaw) < 1.3 && weather !== 'mist') {
      const cx = w * 0.5 + Math.tan(relYaw) * focal;
      const cy = hClamped - (phase === 'sunset' ? h * 0.14 : h * 0.28);
      if (phase === 'night') {
        ctx.fillStyle = 'rgba(224, 236, 255, 0.20)';
        ctx.beginPath();
        ctx.arc(cx, cy, 26, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = '#F2F6FF';
        ctx.beginPath();
        ctx.arc(cx, cy, 13, 0, Math.PI * 2);
        ctx.fill();
      } else {
        const sunG = ctx.createRadialGradient(cx, cy, 4, cx, cy, weather === 'radiant' ? 54 : 44);
        sunG.addColorStop(0, '#FFFBE0');
        sunG.addColorStop(0.35, 'rgba(255, 232, 165, 0.62)');
        sunG.addColorStop(1, 'rgba(255, 224, 150, 0)');
        ctx.fillStyle = sunG;
        ctx.beginPath();
        ctx.arc(cx, cy, weather === 'radiant' ? 54 : 44, 0, Math.PI * 2);
        ctx.fill();
      }
    }

    // Arcoíris suave + gaviotas/aves planeando cuando el clima está Radiante
    if (weather === 'radiant' && phase !== 'night') {
      const rbWorldYaw = isValle ? -2.92 : 1.48;
      const rbRel = ((rbWorldYaw - cam.yaw + Math.PI * 3) % (Math.PI * 2)) - Math.PI;
      if (Math.abs(rbRel) < 1.35) {
        const rx = w * 0.5 + Math.tan(rbRel) * focal;
        const baseR = Math.max(w * 0.56, 210);
        const bands = ['rgba(244,142,142,0.16)', 'rgba(248,194,126,0.16)', 'rgba(248,232,140,0.15)', 'rgba(148,218,162,0.15)', 'rgba(138,204,240,0.15)', 'rgba(182,162,235,0.14)'];
        ctx.save();
        ctx.lineCap = 'round';
        bands.forEach((col, bi) => {
          ctx.beginPath();
          ctx.arc(rx, hClamped + 18, baseR - bi * 5, Math.PI * 1.06, Math.PI * 1.94);
          ctx.strokeStyle = col;
          ctx.lineWidth = 4.6;
          ctx.stroke();
        });
        ctx.restore();
      }

      // Gaviotas / aves alpinas planeando por el cielo
      ctx.save();
      ctx.strokeStyle = isValle ? 'rgba(54, 70, 78, 0.72)' : 'rgba(255, 255, 255, 0.86)';
      ctx.lineWidth = 1.6;
      ctx.lineCap = 'round';
      for (let b = 0; b < 6; b++) {
        const bYaw = ((b * 0.58 + t * (0.045 + (b % 3) * 0.01) - cam.yaw + Math.PI * 5) % (Math.PI * 2)) - Math.PI;
        if (Math.abs(bYaw) > 1.25) continue;
        const bx = w * 0.5 + Math.tan(bYaw) * focal;
        const by = hClamped - 46 - (b % 4) * 26 + Math.sin(t * 2.1 + b) * 5;
        if (by > hClamped - 8 || by < 24) continue;
        const wing = 5.5 + (b % 2) * 2.2;
        const flap = Math.sin(t * 5.8 + b * 1.3) * 3.0;
        ctx.beginPath();
        ctx.moveTo(bx - wing, by + flap);
        ctx.quadraticCurveTo(bx - wing * 0.45, by - 2.6, bx, by);
        ctx.quadraticCurveTo(bx + wing * 0.45, by - 2.6, bx + wing, by + flap);
        ctx.stroke();
      }
      ctx.restore();
    }

    if (phase === 'night' && weather !== 'mist') {
      for (let i = 0; i < 24; i++) {
        const syaw = ((i * 1.17 - cam.yaw + Math.PI * 5) % (Math.PI * 2)) - Math.PI;
        if (Math.abs(syaw) > 1.2) continue;
        const sx = w * 0.5 + Math.tan(syaw) * focal;
        const sy = clamp(hClamped - 40 - ((i * 53) % 210), 18, h * 0.72);
        ctx.fillStyle = `rgba(235, 242, 255, ${(0.35 + (i % 3) * 0.22).toFixed(2)})`;
        ctx.fillRect(sx, sy, 1.8, 1.8);
      }
    } else if (weather === 'mist') {
      ctx.fillStyle = phase === 'night' ? 'rgba(180, 200, 218, 0.14)' : 'rgba(244, 249, 250, 0.34)';
      for (let m = 0; m < 5; m++) {
        const myaw = ((m * 1.25 + t * 0.008 - cam.yaw + Math.PI * 5) % (Math.PI * 2)) - Math.PI;
        if (Math.abs(myaw) > 1.45) continue;
        const mx = w * 0.5 + Math.tan(myaw) * focal;
        const my = hClamped - 16 - (m % 3) * 24;
        ctx.beginPath();
        ctx.ellipse(mx, my, 92, 16, 0, 0, Math.PI * 2);
        ctx.fill();
      }
    } else {
      ctx.fillStyle = 'rgba(255, 255, 255, 0.42)';
      for (let i = 0; i < 6; i++) {
        const cyaw = ((i * 1.05 + t * 0.015 - cam.yaw + Math.PI * 5) % (Math.PI * 2)) - Math.PI;
        if (Math.abs(cyaw) > 1.35) continue;
        const clX = w * 0.5 + Math.tan(cyaw) * focal;
        const clY = hClamped - 58 - (i % 3) * 38;
        if (clY > hClamped - 10) continue;
        ctx.beginPath();
        ctx.ellipse(clX, clY, 44, 11, 0, 0, Math.PI * 2);
        ctx.ellipse(clX + 20, clY - 6, 26, 9, 0, 0, Math.PI * 2);
        ctx.fill();
      }
    }

    return skyPal;
  }

  function drawScene(nowMs) {
    if (!isOpen) return;
    try {
      const dt = lastFrameMs ? Math.min(0.05, (nowMs - lastFrameMs) / 1000) : 0.016;
      lastFrameMs = nowMs;

      updateMovement(dt);
      updateSpatialAudioForCamera();
      sailAngle += dt * windmillSpeed;
      windmillSpeed += (0.85 - windmillSpeed) * Math.min(1, dt * 1.5);
      if (beaconPulse > 0) beaconPulse = Math.max(0, beaconPulse - dt * 1.1);
      if (petalTimer > 0) petalTimer = Math.max(0, petalTimer - dt * 0.7);
      boatPush[0] *= Math.pow(0.25, dt);
      boatPush[1] *= Math.pow(0.25, dt);

      const canvas = document.getElementById('explore-costa-canvas');
      if (!canvas) {
        rafId = requestAnimationFrame(drawScene);
        return;
      }
      const ctx = canvas.getContext('2d');
      if (!ctx) {
        rafId = requestAnimationFrame(drawScene);
        return;
      }

      const dpr = window.devicePixelRatio || 1;
      const w = canvas.clientWidth || 412;
      const h = canvas.clientHeight || 720;
      if (canvas.width !== Math.round(w * dpr) || canvas.height !== Math.round(h * dpr)) {
        canvas.width = Math.round(w * dpr);
        canvas.height = Math.round(h * dpr);
      }
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

      const t = nowMs / 1000;
      const phase = resolvePhase();
      const weather = resolveWeather();
      const focal = Math.min(w, h) * 0.90;
      const sinY = Math.sin(cam.yaw);
      const cosY = Math.cos(cam.yaw);
      const sinP = Math.sin(cam.pitch);
      const cosP = Math.cos(cam.pitch);

      const skyPal = drawSkyAndBackdrop(ctx, w, h, focal, phase, t, weather);
      const fogRgb = skyPal.fog;
      const faces = [];
      hotspots = [];

      function addQuadWorld(ptsW, nx, ny, nz, hex, unlit = false, edgeStroke = null) {
        let cxW = 0, cyW = 0, czW = 0;
        for (let i = 0; i < ptsW.length; i++) {
          cxW += ptsW[i].x;
          cyW += ptsW[i].y;
          czW += ptsW[i].z;
        }
        cxW /= ptsW.length;
        cyW /= ptsW.length;
        czW /= ptsW.length;

        const vx = cam.x - cxW;
        const vy = cam.y - cyW;
        const vz = cam.z - czW;
        if (vx * nx + vy * ny + vz * nz <= 0.001) return;

        const camVerts = [];
        let allBehind = true;
        for (let i = 0; i < ptsW.length; i++) {
          const tv = transformPoint(ptsW[i].x, ptsW[i].y, ptsW[i].z, sinY, cosY, sinP, cosP);
          if (tv.fz >= NEAR) allBehind = false;
          camVerts.push(tv);
        }
        if (allBehind) return;

        const clipped = clipPolyAgainstNear(camVerts);
        if (clipped.length < 3) return;

        const screenPts = clipped.map(v => projectVert(v, w, h, focal));
        let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
        for (let i = 0; i < screenPts.length; i++) {
          if (screenPts[i].x < minX) minX = screenPts[i].x;
          if (screenPts[i].x > maxX) maxX = screenPts[i].x;
          if (screenPts[i].y < minY) minY = screenPts[i].y;
          if (screenPts[i].y > maxY) maxY = screenPts[i].y;
        }
        if (maxX < -60 || minX > w + 60 || maxY < -60 || minY > h + 60) return;

        const distSq = vx * vx + vy * vy + vz * vz;
        faces.push({
          type: 'poly',
          pts: screenPts,
          distSq,
          fill: fogAndTintColor(hex, nx, ny, nz, distSq, phase, fogRgb, unlit, weather),
          edge: edgeStroke
        });
      }

      function addDoubleTriWorld(ptsW, hex, unlit = false, edgeStroke = null) {
        let cxW = 0, cyW = 0, czW = 0;
        for (let i = 0; i < ptsW.length; i++) {
          cxW += ptsW[i].x; cyW += ptsW[i].y; czW += ptsW[i].z;
        }
        cxW /= ptsW.length; cyW /= ptsW.length; czW /= ptsW.length;
        const camVerts = [];
        let allBehind = true;
        for (let i = 0; i < ptsW.length; i++) {
          const tv = transformPoint(ptsW[i].x, ptsW[i].y, ptsW[i].z, sinY, cosY, sinP, cosP);
          if (tv.fz >= NEAR) allBehind = false;
          camVerts.push(tv);
        }
        if (allBehind) return;
        const clipped = clipPolyAgainstNear(camVerts);
        if (clipped.length < 3) return;
        const screenPts = clipped.map(v => projectVert(v, w, h, focal));
        const distSq = (cam.x - cxW) ** 2 + (cam.y - cyW) ** 2 + (cam.z - czW) ** 2;
        faces.push({
          type: 'poly',
          pts: screenPts,
          distSq,
          fill: fogAndTintColor(hex, 0.2, -0.3, 0.9, distSq, phase, fogRgb, unlit, weather),
          edge: edgeStroke
        });
      }

      function addPrism(x0, x1, y0, y1, z0, z1, hex, unlit = false, edgeStroke = null, skipBottom = false) {
        if (z1 <= z0) return;
        addQuadWorld([
          { x: x0, y: y0, z: z1 }, { x: x1, y: y0, z: z1 },
          { x: x1, y: y1, z: z1 }, { x: x0, y: y1, z: z1 }
        ], 0, 0, 1, hex, unlit, edgeStroke);
        if (!skipBottom) {
          addQuadWorld([
            { x: x0, y: y1, z: z0 }, { x: x1, y: y1, z: z0 },
            { x: x1, y: y0, z: z0 }, { x: x0, y: y0, z: z0 }
          ], 0, 0, -1, hex, unlit, edgeStroke);
        }
        addQuadWorld([
          { x: x1, y: y0, z: z1 }, { x: x1, y: y0, z: z0 },
          { x: x1, y: y1, z: z0 }, { x: x1, y: y1, z: z1 }
        ], 1, 0, 0, hex, unlit, edgeStroke);
        addQuadWorld([
          { x: x0, y: y1, z: z1 }, { x: x0, y: y1, z: z0 },
          { x: x0, y: y0, z: z0 }, { x: x0, y: y0, z: z1 }
        ], -1, 0, 0, hex, unlit, edgeStroke);
        addQuadWorld([
          { x: x1, y: y1, z: z1 }, { x: x1, y: y1, z: z0 },
          { x: x0, y: y1, z: z0 }, { x: x0, y: y1, z: z1 }
        ], 0, 1, 0, hex, unlit, edgeStroke);
        addQuadWorld([
          { x: x0, y: y0, z: z1 }, { x: x0, y: y0, z: z0 },
          { x: x1, y: y0, z: z0 }, { x: x1, y: y0, z: z1 }
        ], 0, -1, 0, hex, unlit, edgeStroke);
      }

      function addArchBlock(wx0, wy0, z0, frac, hU, hex, unlit = false) {
        const m = (1 - frac) * 0.5;
        addPrism(wx0 + m, wx0 + 1 - m, wy0 + m, wy0 + 1 - m, z0, z0 + hU, hex, unlit, ARCH_EDGE);
      }

      // Museo Vivo del Muro Real: marco 3D de madera + paspartú + foto real texturizada en perspectiva
      function addWallPhotoFrame(wallKind, cx, cy, z0, z1, spanW, photoMeta) {
        if (!photoMeta) return;
        const hw = spanW * 0.5;
        const fz0 = z0;
        const fz1 = z1;
        const pz0 = z0 + 0.032;
        const pz1 = z1 - 0.032;
        const pw = Math.max(0.05, hw - 0.032);
        const iz0 = pz0 + 0.022;
        const iz1 = pz1 - 0.022;
        const iw = Math.max(0.04, pw - 0.022);
        let nx = 0, ny = 0, quad = null;

        if (wallKind === 'southWallLookNorth') {
          // Pared en +Y mirando al interior (-Y)
          ny = -1;
          addPrism(cx - hw, cx + hw, cy - 0.024, cy + 0.008, fz0, fz1, '#6C472B', false, ARCH_EDGE);
          addQuadWorld([
            { x: cx - pw, y: cy - 0.026, z: pz1 },
            { x: cx + pw, y: cy - 0.026, z: pz1 },
            { x: cx + pw, y: cy - 0.026, z: pz0 },
            { x: cx - pw, y: cy - 0.026, z: pz0 }
          ], 0, -1, 0, '#EFE7D6', false);
          quad = [
            { x: cx + iw, y: cy - 0.029, z: iz1 },
            { x: cx - iw, y: cy - 0.029, z: iz1 },
            { x: cx - iw, y: cy - 0.029, z: iz0 },
            { x: cx + iw, y: cy - 0.029, z: iz0 }
          ];
        } else if (wallKind === 'northWallLookSouth') {
          // Pared en -Y mirando al interior (+Y)
          ny = 1;
          addPrism(cx - hw, cx + hw, cy - 0.008, cy + 0.024, fz0, fz1, '#6C472B', false, ARCH_EDGE);
          addQuadWorld([
            { x: cx - pw, y: cy + 0.026, z: pz1 },
            { x: cx + pw, y: cy + 0.026, z: pz1 },
            { x: cx + pw, y: cy + 0.026, z: pz0 },
            { x: cx - pw, y: cy + 0.026, z: pz0 }
          ], 0, 1, 0, '#EFE7D6', false);
          quad = [
            { x: cx - iw, y: cy + 0.029, z: iz1 },
            { x: cx + iw, y: cy + 0.029, z: iz1 },
            { x: cx + iw, y: cy + 0.029, z: iz0 },
            { x: cx - iw, y: cy + 0.029, z: iz0 }
          ];
        } else {
          // Pared trasera oeste en -X mirando al interior (+X)
          nx = 1;
          addPrism(cx - 0.008, cx + 0.024, cy - hw, cy + hw, fz0, fz1, '#6C472B', false, ARCH_EDGE);
          addQuadWorld([
            { x: cx + 0.026, y: cy - pw, z: pz1 },
            { x: cx + 0.026, y: cy + pw, z: pz1 },
            { x: cx + 0.026, y: cy + pw, z: pz0 },
            { x: cx + 0.026, y: cy - pw, z: pz0 }
          ], 1, 0, 0, '#EFE7D6', false);
          quad = [
            { x: cx + 0.029, y: cy + iw, z: iz1 },
            { x: cx + 0.029, y: cy - iw, z: iz1 },
            { x: cx + 0.029, y: cy - iw, z: iz0 },
            { x: cx + 0.029, y: cy + iw, z: iz0 }
          ];
        }

        const qx = quad.reduce((s, p) => s + p.x, 0) * 0.25;
        const qy = quad.reduce((s, p) => s + p.y, 0) * 0.25;
        const qz = quad.reduce((s, p) => s + p.z, 0) * 0.25;
        const vx = cam.x - qx;
        const vy = cam.y - qy;
        const vz = cam.z - qz;
        if (vx * nx + vy * ny > 0.001) {
          const tVerts = quad.map(p => transformPoint(p.x, p.y, p.z, sinY, cosY, sinP, cosP));
          if (tVerts.every(v => v.fz >= NEAR)) {
            const sPts = tVerts.map(v => projectVert(v, w, h, focal));
            const distSq = vx * vx + vy * vy + vz * vz;
            const img = getCachedPhotoImage(photoMeta.src);
            faces.push({
              type: 'photo',
              pts: sPts,
              distSq,
              img,
              phase,
              weather
            });
          }
          if (vx * nx + vy * ny > 0.04) {
            const hPos = projectWorldAnchor(qx, qy, qz, sinY, cosY, sinP, cosP, w, h, focal);
            if (hPos) {
              hotspots.push({ id: 'photoFrame', photo: photoMeta, x: hPos.x, y: hPos.y, r: 36, isPhoto: true });
            }
          }
        }
      }

      const gridN = getGridN();
      const half = gridN * 0.5;

      // Entorno exterior alrededor de la maqueta 18x18
      if (activeBiome === 'costa') {
        const outerPatches = [
          [half, half + 22, -half - 14, half + 14],
          [-half - 20, -half, -half - 14, half + 14],
          [-half, half, half, half + 20],
          [-half, half, -half - 20, -half]
        ];
        outerPatches.forEach(([ox0, ox1, oy0, oy1]) => {
          addQuadWorld([
            { x: ox0, y: oy0, z: 0.24 }, { x: ox1, y: oy0, z: 0.24 },
            { x: ox1, y: oy1, z: 0.24 }, { x: ox0, y: oy1, z: 0.24 }
          ], 0, 0, 1, '#4B9FB5', false);
        });
      } else {
        const valleOuter = [
          [half, half + 22, -half - 16, half + 16, '#65A462'],
          [-half - 22, -half, -half - 16, half + 16, '#5D9B5B'],
          [-half, half, half, half + 20, '#6BAE68'],
          [-half, half, -half - 22, -half, '#4B8250']
        ];
        valleOuter.forEach(([ox0, ox1, oy0, oy1, col]) => {
          addQuadWorld([
            { x: ox0, y: oy0, z: 0.50 }, { x: ox1, y: oy0, z: 0.50 },
            { x: ox1, y: oy1, z: 0.50 }, { x: ox0, y: oy1, z: 0.50 }
          ], 0, 0, 1, col, false);
        });
        // Cordillera alpina lejana al norte (-Y)
        const distantPeaks = [
          { cx: -7.5, cy: -14.5, rx: 5.5, hPeak: 6.2 },
          { cx: 0.2, cy: -15.5, rx: 6.8, hPeak: 7.4 },
          { cx: 8.2, cy: -14.2, rx: 5.4, hPeak: 5.8 }
        ];
        distantPeaks.forEach(pk => {
          addDoubleTriWorld([
            { x: pk.cx - pk.rx, y: pk.cy, z: 0.50 },
            { x: pk.cx + pk.rx, y: pk.cy, z: 0.50 },
            { x: pk.cx, y: pk.cy, z: pk.hPeak }
          ], '#5C7B69', false);
          addDoubleTriWorld([
            { x: pk.cx - pk.rx * 0.42, y: pk.cy + 0.08, z: pk.hPeak * 0.62 },
            { x: pk.cx + pk.rx * 0.42, y: pk.cy + 0.08, z: pk.hPeak * 0.62 },
            { x: pk.cx, y: pk.cy + 0.08, z: pk.hPeak }
          ], '#F3F7FB', false);
        });
      }

      if (sceneData) {
        const { layout, budgets, cells } = sceneData;
        const hashFn = (window.BroteMascot && window.BroteMascot.cellHash)
          ? window.BroteMascot.cellHash
          : ((x, y, s = 17) => {
              const v = Math.sin(x * 127.1 + y * 311.7 + s * 74.7) * 43758.5453;
              return v - Math.floor(v);
            });
        const isWarmNight = phase === 'night' || phase === 'sunset';
        const cfg = getBiomeConfig();

        for (let ix = 0; ix < gridN; ix++) {
          const col = cells[ix];
          if (!col) continue;
          const wx0 = ix - half;
          const wx1 = wx0 + 1;
          for (let iy = 0; iy < gridN; iy++) {
            const cell = col[iy];
            if (!cell) continue;
            const wy0 = iy - half;
            const wy1 = wy0 + 1;
            const key = cell.key;

            const waveOff = cell.isWater
              ? Math.sin(t * (activeBiome === 'costa' ? 1.9 : 1.5) + ix * 0.6 + iy * 0.45) * (activeBiome === 'costa' ? 0.045 : 0.025)
              : 0;
            const topZ = cell.wz + waveOff;
            addPrism(wx0, wx1, wy0, wy1, -0.35, topZ, cell.color, false, null, true);

            if (cell.isWater) {
              if (activeBiome === 'costa' && cell.depth < 0.14) {
                addPrism(wx0 + 0.05, wx0 + 0.30, wy0 + 0.14, wy0 + 0.86, topZ, topZ + 0.035, '#F4F9FB', false);
              }
              if (isBridgeOrPierCell(ix, iy)) {
                const deckLo = activeBiome === 'valle' ? Math.max(cell.wz + 0.14, 0.50) : cell.wz + 0.14;
                const deckHi = activeBiome === 'valle' ? Math.max(cell.wz + 0.28, 0.64) : cell.wz + 0.28;
                addPrism(wx0 - 0.04, wx1 + 0.04, wy0 + 0.16, wy1 - 0.16, deckLo, deckHi, '#B7926A', false, ARCH_EDGE);
                for (let p = 0; p < 4; p++) {
                  addPrism(wx0 + 0.04 + p * 0.24, wx0 + 0.20 + p * 0.24, wy0 + 0.18, wy1 - 0.18, deckHi, deckHi + 0.03, '#CCA57B', false, ARCH_EDGE);
                }
                addPrism(wx0 + 0.06, wx0 + 0.18, wy0 + 0.18, wy0 + 0.30, 0.02, deckHi + 0.18, '#7A5B3D', false, ARCH_EDGE);
                addPrism(wx0 + 0.82, wx0 + 0.94, wy0 + 0.18, wy0 + 0.30, 0.02, deckHi + 0.18, '#7A5B3D', false, ARCH_EDGE);
                addPrism(wx0 + 0.06, wx0 + 0.18, wy1 - 0.30, wy1 - 0.18, 0.02, deckHi + 0.18, '#7A5B3D', false, ARCH_EDGE);
                addPrism(wx0 + 0.82, wx0 + 0.94, wy1 - 0.30, wy1 - 0.18, 0.02, deckHi + 0.18, '#7A5B3D', false, ARCH_EDGE);
                if (activeBiome === 'valle') {
                  addPrism(wx0, wx1, wy0 + 0.21, wy0 + 0.27, deckHi + 0.13, deckHi + 0.19, '#9C734B', false, ARCH_EDGE);
                  addPrism(wx0, wx1, wy1 - 0.27, wy1 - 0.21, deckHi + 0.13, deckHi + 0.19, '#9C734B', false, ARCH_EDGE);
                }
              }
              if (layout.shimmerRanks[key] && layout.shimmerRanks[key] <= budgets.shimmers) {
                addPrism(wx0 + 0.22, wx0 + 0.76, wy0 + 0.44, wy0 + 0.56, topZ, topZ + 0.015, '#E4F6FA', true);
              }
              continue;
            }

            if (isBridgeOrPierCell(ix, iy)) {
              const rHi = Math.max(topZ + 0.14, 0.68);
              addPrism(wx0 - 0.06, wx1 + 0.06, wy0 + 0.16, wy1 - 0.16, topZ, rHi, '#B7926A', false, ARCH_EDGE);
              for (let p = 0; p < 4; p++) {
                addPrism(wx0 + 0.04 + p * 0.24, wx0 + 0.20 + p * 0.24, wy0 + 0.18, wy1 - 0.18, rHi, rHi + 0.03, '#CCA57B', false, ARCH_EDGE);
              }
              continue;
            }

            // CASA MEDITERRÁNEA ABIERTA (COSTA) CON MUSEO DEL MURO REAL
            if (activeBiome === 'costa' && ix === cfg.heroHouseCell.x && iy === cfg.heroHouseCell.y) {
              const rx0 = wx0 - 0.20;
              const rx1 = wx1 + 0.08;
              const ry0 = wy0 - 0.04;
              const ry1 = wy1 + 0.04;
              addPrism(rx0, rx1, ry0, ry1, topZ, topZ + 0.06, '#D9B18A', false, ARCH_EDGE);
              addPrism(rx0 + 0.34, rx0 + 0.94, ry0 + 0.24, ry0 + 0.76, topZ + 0.06, topZ + 0.08, '#C86D52', false, ARCH_EDGE);
              // Pared trasera oeste (-X)
              addPrism(rx0, rx0 + 0.12, ry0, ry1, topZ + 0.06, topZ + 1.56, '#F4E6CC', false, ARCH_EDGE);
              addPrism(rx0 + 0.12, rx0 + 0.15, ry0 + 0.58, ry0 + 0.88, topZ + 0.06, topZ + 0.92, '#8F603E', false, ARCH_EDGE);
              // Pared izquierda (-Y) con ventana abierta + cuadro derecho visible al mirar
              addPrism(rx0, rx1, ry0, ry0 + 0.10, topZ + 0.06, topZ + 0.54, '#EFE0C3', false, ARCH_EDGE);
              addPrism(rx0, rx0 + 0.24, ry0, ry0 + 0.10, topZ + 0.54, topZ + 1.12, '#EFE0C3', false, ARCH_EDGE);
              addPrism(rx0 + 0.58, rx1, ry0, ry0 + 0.10, topZ + 0.54, topZ + 1.12, '#EFE0C3', false, ARCH_EDGE);
              addPrism(rx0, rx1, ry0, ry0 + 0.10, topZ + 1.12, topZ + 1.56, '#F4E6CC', false, ARCH_EDGE);
              addPrism(rx0 + 0.22, rx0 + 0.60, ry0 - 0.01, ry0 + 0.11, topZ + 0.50, topZ + 0.56, '#3574B5', false, ARCH_EDGE);
              // Pared derecha (+Y) con ventana + cuadro izquierdo visible al mirar
              addPrism(rx0, rx1, ry1 - 0.10, ry1, topZ + 0.06, topZ + 0.54, '#EFE0C3', false, ARCH_EDGE);
              addPrism(rx0, rx0 + 0.24, ry1 - 0.10, ry1, topZ + 0.54, topZ + 1.12, '#EFE0C3', false, ARCH_EDGE);
              addPrism(rx0 + 0.58, rx1, ry1 - 0.10, ry1, topZ + 0.54, topZ + 1.12, '#EFE0C3', false, ARCH_EDGE);
              addPrism(rx0, rx1, ry1 - 0.10, ry1, topZ + 1.12, topZ + 1.56, '#F4E6CC', false, ARCH_EDGE);
              addPrism(rx0 + 0.22, rx0 + 0.60, ry1 - 0.11, ry1 + 0.01, topZ + 0.50, topZ + 0.56, '#3574B5', false, ARCH_EDGE);
              // Pórtico abierto al mar (+X)
              addPrism(rx1 - 0.12, rx1, ry0, ry0 + 0.14, topZ + 0.06, topZ + 1.40, '#F5E9D3', false, ARCH_EDGE);
              addPrism(rx1 - 0.12, rx1, ry1 - 0.14, ry1, topZ + 0.06, topZ + 1.40, '#F5E9D3', false, ARCH_EDGE);
              addPrism(rx1 - 0.12, rx1, ry0, ry1, topZ + 1.38, topZ + 1.56, '#F4E6CC', false, ARCH_EDGE);
              // Techo + cúpula azul superior
              addPrism(rx0, rx1, ry0, ry1, topZ + 1.56, topZ + 1.72, '#8A603E', false, ARCH_EDGE);
              addArchBlock(wx0, wy0, topZ + 1.72, 0.66, 0.22, '#3574B5');
              // Mobiliario interior
              addPrism(rx1 - 0.36, rx1 - 0.12, ry1 - 0.42, ry1 - 0.16, topZ + 0.06, topZ + 0.38, '#9E734E', false, ARCH_EDGE);
              addPrism(rx1 - 0.32, rx1 - 0.22, ry1 - 0.36, ry1 - 0.26, topZ + 0.38, topZ + 0.54, '#3B9EB0', false, ARCH_EDGE);
              addPrism(rx1 - 0.22, rx1 - 0.13, ry1 - 0.26, ry1 - 0.17, topZ + 0.38, topZ + 0.50, lampOn ? '#FFD878' : '#D97757', lampOn, ARCH_EDGE);

              // 3 Cuadros del Muro Real en las paredes interiores
              addWallPhotoFrame('northWallLookSouth', rx0 + 0.88, ry0 + 0.11, topZ + 0.68, topZ + 1.06, 0.36, resolvedGalleryPhotos[0]);
              addWallPhotoFrame('southWallLookNorth', rx0 + 0.88, ry1 - 0.11, topZ + 0.68, topZ + 1.06, 0.36, resolvedGalleryPhotos[1]);
              addWallPhotoFrame('westWallLookEast', rx0 + 0.13, ry0 + 0.34, topZ + 0.68, topZ + 1.06, 0.34, resolvedGalleryPhotos[2]);
              continue;
            }

            // REFUGIO ALPINO DE MADERA ABIERTO (VALLE) CON CHIMENEA Y MUSEO DEL MURO REAL
            if (activeBiome === 'valle' && ix === cfg.heroHouseCell.x && iy === cfg.heroHouseCell.y) {
              const rx0 = wx0 - 0.20;
              const rx1 = wx1 + 0.08;
              const ry0 = wy0 - 0.04;
              const ry1 = wy1 + 0.04;
              // Suelo de tablones + alfombra tejida cálida
              addPrism(rx0, rx1, ry0, ry1, topZ, topZ + 0.06, '#A8784E', false, ARCH_EDGE);
              addPrism(rx0 + 0.34, rx0 + 0.94, ry0 + 0.24, ry0 + 0.78, topZ + 0.06, topZ + 0.085, '#C45B47', false, ARCH_EDGE);
              // Pared trasera oeste de troncos (-X) + hogar de piedra con fuego
              addPrism(rx0, rx0 + 0.12, ry0, ry1, topZ + 0.06, topZ + 1.56, '#875B3A', false, ARCH_EDGE);
              addPrism(rx0 + 0.10, rx0 + 0.28, ry0 + 0.64, ry0 + 0.96, topZ + 0.06, topZ + 0.96, '#7C858D', false, ARCH_EDGE);
              const fireCol = fireOn ? (Math.sin(t * 9) > 0 ? '#FFA347' : '#FFDC7A') : '#46413C';
              addPrism(rx0 + 0.16, rx0 + 0.29, ry0 + 0.72, ry0 + 0.88, topZ + 0.12, topZ + 0.42, fireCol, fireOn);
              // Pared norte (-Y) de troncos con ventana alpina y espacio para cuadro
              addPrism(rx0, rx1, ry0, ry0 + 0.10, topZ + 0.06, topZ + 0.54, '#926542', false, ARCH_EDGE);
              addPrism(rx0, rx0 + 0.24, ry0, ry0 + 0.10, topZ + 0.54, topZ + 1.12, '#926542', false, ARCH_EDGE);
              addPrism(rx0 + 0.58, rx1, ry0, ry0 + 0.10, topZ + 0.54, topZ + 1.12, '#926542', false, ARCH_EDGE);
              addPrism(rx0, rx1, ry0, ry0 + 0.10, topZ + 1.12, topZ + 1.56, '#875B3A', false, ARCH_EDGE);
              addPrism(rx0 + 0.22, rx0 + 0.60, ry0 - 0.01, ry0 + 0.11, topZ + 0.50, topZ + 0.56, '#5D3F29', false, ARCH_EDGE);
              // Pared sur (+Y) de troncos con ventana alpina y espacio para cuadro
              addPrism(rx0, rx1, ry1 - 0.10, ry1, topZ + 0.06, topZ + 0.54, '#926542', false, ARCH_EDGE);
              addPrism(rx0, rx0 + 0.24, ry1 - 0.10, ry1, topZ + 0.54, topZ + 1.12, '#926542', false, ARCH_EDGE);
              addPrism(rx0 + 0.58, rx1, ry1 - 0.10, ry1, topZ + 0.54, topZ + 1.12, '#926542', false, ARCH_EDGE);
              addPrism(rx0, rx1, ry1 - 0.10, ry1, topZ + 1.12, topZ + 1.56, '#875B3A', false, ARCH_EDGE);
              addPrism(rx0 + 0.22, rx0 + 0.60, ry1 - 0.11, ry1 + 0.01, topZ + 0.50, topZ + 0.56, '#5D3F29', false, ARCH_EDGE);
              // Porche frontal (+X) abierto al lago alpino
              addPrism(rx1 - 0.10, rx1, ry0, ry0 + 0.12, topZ + 0.06, topZ + 1.44, '#764E32', false, ARCH_EDGE);
              addPrism(rx1 - 0.10, rx1, ry1 - 0.12, ry1, topZ + 0.06, topZ + 1.44, '#764E32', false, ARCH_EDGE);
              addPrism(rx1 - 0.10, rx1, ry0, ry1, topZ + 1.40, topZ + 1.56, '#764E32', false, ARCH_EDGE);
              // Techo alpino oscuro con nieve en cumbrera y chimenea
              addPrism(rx0 - 0.04, rx1 + 0.06, ry0 - 0.04, ry1 + 0.04, topZ + 1.56, topZ + 1.72, '#7C4330', false, ARCH_EDGE);
              addPrism(rx0 + 0.08, rx1 - 0.06, ry0 + 0.14, ry1 - 0.14, topZ + 1.72, topZ + 1.92, '#6A3828', false, ARCH_EDGE);
              addPrism(rx0 + 0.14, rx1 - 0.10, ry0 + 0.24, ry1 - 0.24, topZ + 1.92, topZ + 1.98, '#F4F8FC', false, ARCH_EDGE);
              addPrism(rx0 + 0.14, rx0 + 0.30, ry0 + 0.72, ry0 + 0.88, topZ + 1.72, topZ + 2.28, '#7A828A', false, ARCH_EDGE);
              // Mesita de madera + tetera cálida interior
              addPrism(rx1 - 0.38, rx1 - 0.14, ry0 + 0.16, ry0 + 0.42, topZ + 0.06, topZ + 0.38, '#8E6240', false, ARCH_EDGE);
              addPrism(rx1 - 0.30, rx1 - 0.19, ry0 + 0.23, ry0 + 0.34, topZ + 0.38, topZ + 0.50, '#E07855', false, ARCH_EDGE);

              // 3 Cuadros del Muro Real en las paredes interiores del Refugio
              addWallPhotoFrame('northWallLookSouth', rx0 + 0.88, ry0 + 0.11, topZ + 0.68, topZ + 1.06, 0.36, resolvedGalleryPhotos[0]);
              addWallPhotoFrame('southWallLookNorth', rx0 + 0.88, ry1 - 0.11, topZ + 0.68, topZ + 1.06, 0.36, resolvedGalleryPhotos[1]);
              addWallPhotoFrame('westWallLookEast', rx0 + 0.13, ry0 + 0.34, topZ + 0.68, topZ + 1.06, 0.34, resolvedGalleryPhotos[2]);
              continue;
            }

            // COSTA HITO 4: ÁRBOL DEL MIRADOR
            if (activeBiome === 'costa' && cfg.heroTreeCell && ix === cfg.heroTreeCell.x && iy === cfg.heroTreeCell.y) {
              addPrism(wx0 + 0.16, wx0 + 0.82, wy0 + 0.76, wy0 + 0.94, topZ, topZ + 0.26, '#9C7450', false, ARCH_EDGE);
              addPrism(wx0 + 0.40, wx0 + 0.60, wy0 + 0.40, wy0 + 0.60, topZ, topZ + 1.38, '#6E533B', false, ARCH_EDGE);
              addPrism(wx0 + 0.06, wx0 + 0.94, wy0 + 0.06, wy0 + 0.94, topZ + 1.36, topZ + 1.80, '#6D8C5A', false, ARCH_EDGE);
              addPrism(wx0 + 0.16, wx0 + 0.86, wy0 + 0.14, wy0 + 0.86, topZ + 1.80, topZ + 2.18, '#83A36E', false, ARCH_EDGE);
              addPrism(wx0 + 0.26, wx0 + 0.76, wy0 + 0.24, wy0 + 0.76, topZ + 2.18, topZ + 2.42, '#5A7849', false, ARCH_EDGE);
              addPrism(wx0 + 0.18, wx0 + 0.52, wy0 + 0.66, wy0 + 0.96, topZ + 1.18, topZ + 1.48, '#D85B88', false, ARCH_EDGE);
              addPrism(wx0 + 0.58, wx0 + 0.88, wy0 + 0.12, wy0 + 0.42, topZ + 1.24, topZ + 1.52, '#EE88AC', false, ARCH_EDGE);
              continue;
            }

            // VALLE HITO 4: MIRADOR DE PINOS CON BANCO Y FAROL
            if (activeBiome === 'valle' && cfg.heroViewCell && ix === cfg.heroViewCell.x && iy === cfg.heroViewCell.y) {
              addPrism(wx0 + 0.14, wx0 + 0.26, wy0 + 0.14, wy0 + 0.26, topZ, topZ + 0.36, '#6E533B', false, ARCH_EDGE);
              addPrism(wx0 + 0.02, wx0 + 0.38, wy0 + 0.02, wy0 + 0.38, topZ + 0.34, topZ + 0.84, '#2F683A', false, ARCH_EDGE);
              addPrism(wx0 + 0.08, wx0 + 0.32, wy0 + 0.08, wy0 + 0.32, topZ + 0.84, topZ + 1.26, '#3D7E4A', false, ARCH_EDGE);
              addPrism(wx0 + 0.14, wx0 + 0.26, wy0 + 0.14, wy0 + 0.26, topZ + 1.26, topZ + 1.56, '#4B9058', false, ARCH_EDGE);
              // Banco alpino de madera
              addPrism(wx0 + 0.62, wx0 + 0.94, wy0 + 0.16, wy0 + 0.44, topZ, topZ + 0.26, '#9B7049', false, ARCH_EDGE);
              addPrism(wx0 + 0.88, wx0 + 0.95, wy0 + 0.16, wy0 + 0.44, topZ + 0.26, topZ + 0.50, '#7E5838', false, ARCH_EDGE);
              // Farolillo del mirador
              addPrism(wx0 + 0.16, wx0 + 0.24, wy0 + 0.74, wy0 + 0.82, topZ, topZ + 0.38, '#544A40', false, ARCH_EDGE);
              addPrism(wx0 + 0.11, wx0 + 0.29, wy0 + 0.69, wy0 + 0.87, topZ + 0.38, topZ + 0.52, lampOn ? '#FFD878' : '#7C858D', lampOn);
              continue;
            }

            // Remates piramidales nevados sobre los picos altos de Valle
            if (activeBiome === 'valle' && cell.rawH > 3.10) {
              const pH = 0.62 + (cell.rawH - 3.10) * 0.55;
              const cx = wx0 + 0.50;
              const cy = wy0 + 0.50;
              const colSnow = '#F6FAFD';
              addDoubleTriWorld([{ x: wx0 + 0.06, y: wy0 + 0.06, z: topZ }, { x: wx1 - 0.06, y: wy0 + 0.06, z: topZ }, { x: cx, y: cy, z: topZ + pH }], colSnow, false);
              addDoubleTriWorld([{ x: wx1 - 0.06, y: wy0 + 0.06, z: topZ }, { x: wx1 - 0.06, y: wy1 - 0.06, z: topZ }, { x: cx, y: cy, z: topZ + pH }], '#E8EFF5', false);
              addDoubleTriWorld([{ x: wx1 - 0.06, y: wy1 - 0.06, z: topZ }, { x: wx0 + 0.06, y: wy1 - 0.06, z: topZ }, { x: cx, y: cy, z: topZ + pH }], '#DDE6EE', false);
              addDoubleTriWorld([{ x: wx0 + 0.06, y: wy1 - 0.06, z: topZ }, { x: wx0 + 0.06, y: wy0 + 0.06, z: topZ }, { x: cx, y: cy, z: topZ + pH }], '#EEF4F9', false);
              continue;
            }

            if (cell.isPath || isClearCourtyardCell(ix, iy)) continue;

            const isLandmark = activeBiome === 'costa' && layout.landmarkCell && layout.landmarkCell.x === ix && layout.landmarkCell.y === iy;
            const isWindmill = activeBiome === 'costa' && layout.costaWindmillCell && layout.costaWindmillCell.x === ix && layout.costaWindmillCell.y === iy && budgets.showSecondary;

            if (isLandmark) {
              addArchBlock(wx0, wy0, topZ, 0.86, 0.46, '#E4CFA9');
              addPrism(wx0 + 0.34, wx0 + 0.66, wy1 - 0.10, wy1 + 0.02, topZ, topZ + 0.42, '#8F603E', false, ARCH_EDGE);
              addArchBlock(wx0, wy0, topZ + 0.46, 0.54, 0.68, '#F5E9D3');
              addArchBlock(wx0, wy0, topZ + 1.14, 0.50, 0.86, '#F5E9D3');
              addArchBlock(wx0, wy0, topZ + 1.38, 0.52, 0.18, '#D97757');
              addArchBlock(wx0, wy0, topZ + 2.00, 0.60, 0.24, '#D97757');
              addArchBlock(wx0, wy0, topZ + 2.24, 0.42, 0.22, beaconOn ? '#FFF4C4' : '#8A9AA6', beaconOn);
              addArchBlock(wx0, wy0, topZ + 2.46, 0.48, 0.12, '#D97757');
              continue;
            }

            if (isWindmill) {
              addArchBlock(wx0, wy0, topZ, 0.66, 0.96, '#FFFFFF');
              addArchBlock(wx0, wy0, topZ + 0.96, 0.70, 0.28, '#3574B5');
              const hx = wx1 - 0.12;
              const hy = wy0 + 0.50;
              const hz = topZ + 0.76;
              addPrism(hx - 0.05, hx + 0.10, hy - 0.06, hy + 0.06, hz - 0.06, hz + 0.06, '#7C5A3B', false, ARCH_EDGE);
              for (let k = 0; k < 4; k++) {
                const ang = sailAngle + (k * Math.PI) / 2;
                const dy = Math.cos(ang) * 0.56;
                const dz = Math.sin(ang) * 0.56;
                const py = -Math.sin(ang) * 0.11;
                const pz = Math.cos(ang) * 0.11;
                addDoubleTriWorld([
                  { x: hx + 0.08, y: hy, z: hz },
                  { x: hx + 0.08, y: hy + dy, z: hz + dz },
                  { x: hx + 0.08, y: hy + dy + py, z: hz + dz + pz }
                ], '#FAF5EA', false, ARCH_EDGE);
              }
              continue;
            }

            if (layout.houseRanks[key] && layout.houseRanks[key] <= budgets.houses) {
              if (activeBiome === 'valle') {
                const m = 0.22;
                const bh = 0.56 + hashFn(ix, iy, 7) * 0.28;
                addPrism(wx0 + m, wx1 - m, wy0 + m, wy1 - m, topZ, topZ + bh, '#A87D57', false, ARCH_EDGE);
                addPrism(wx0 + m - 0.04, wx1 - m + 0.04, wy0 + m - 0.04, wy1 - m + 0.04, topZ + bh, topZ + bh + 0.20, '#854B37', false, ARCH_EDGE);
                addPrism(wx0 + m + 0.08, wx1 - m - 0.08, wy0 + m + 0.08, wy1 - m - 0.08, topZ + bh + 0.20, topZ + bh + 0.28, '#F2F6FA', false, ARCH_EDGE);
                const winHex = isWarmNight ? '#FFD878' : '#7BA6BC';
                addPrism(wx0 + 0.42, wx0 + 0.58, wy0 + m - 0.02, wy0 + m, topZ + bh * 0.4, topZ + bh * 0.72, winHex, isWarmNight);
              } else {
                const frac = 0.56;
                const bh = (0.36 + hashFn(ix, iy, 7) * 0.38) * 1.85;
                const styleRoll = hashFn(Math.round(wx0 + half), Math.round(wy0 + half), 47);
                const m = (1 - frac) * 0.5;
                addPrism(wx0 + m, wx0 + 1 - m, wy0 + m, wy0 + 1 - m, topZ, topZ + bh, '#F2E2C4', false, ARCH_EDGE);
                if (styleRoll > 0.68) {
                  addArchBlock(wx0, wy0, topZ + bh, frac * 0.72, 0.22, '#3574B5');
                } else if (styleRoll > 0.36) {
                  addArchBlock(wx0, wy0, topZ + bh, frac + 0.04, 0.14, '#C96D4B');
                }
                addPrism(wx0 + 1 - m, wx0 + 1 - m + 0.03, wy0 + 0.42, wy0 + 0.58, topZ, topZ + 0.32, '#7D583B', false, ARCH_EDGE);
                const winHex = isWarmNight ? '#FFD878' : '#4C7A96';
                addPrism(wx0 + 0.42, wx0 + 0.58, wy0 + m - 0.025, wy0 + m, topZ + bh * 0.42, topZ + bh * 0.68, winHex, isWarmNight);
                addPrism(wx0 + 0.42, wx0 + 0.58, wy0 + 1 - m, wy0 + 1 - m + 0.025, topZ + bh * 0.42, topZ + bh * 0.68, winHex, isWarmNight);
              }
              continue;
            }

            if (layout.lanternRanks[key] && layout.lanternRanks[key] <= budgets.lanterns) {
              addPrism(wx0 + 0.46, wx0 + 0.54, wy0 + 0.46, wy0 + 0.54, topZ, topZ + 0.36, '#5A5045', false, ARCH_EDGE);
              addPrism(wx0 + 0.40, wx0 + 0.60, wy0 + 0.40, wy0 + 0.60, topZ + 0.36, topZ + 0.50, '#FFD77A', true);
              continue;
            }

            if (layout.rockRanks[key] && layout.rockRanks[key] <= budgets.rocks) {
              const rockCol = activeBiome === 'valle' ? '#96A3AA' : '#C9C4B8';
              addPrism(wx0 + 0.24, wx0 + 0.72, wy0 + 0.26, wy0 + 0.70, topZ, topZ + 0.24, rockCol, false, ARCH_EDGE);
              continue;
            }

            if (layout.treeRanks[key] && layout.treeRanks[key] <= budgets.trees) {
              if (activeBiome === 'valle') {
                const sc = 0.85 + hashFn(ix, iy, 61) * 0.35;
                addPrism(wx0 + 0.44, wx0 + 0.56, wy0 + 0.44, wy0 + 0.56, topZ, topZ + 0.24 * sc, '#6B513D');
                addPrism(wx0 + 0.24, wx0 + 0.76, wy0 + 0.24, wy0 + 0.76, topZ + 0.22 * sc, topZ + 0.66 * sc, '#336B3F', false, ARCH_EDGE);
                addPrism(wx0 + 0.32, wx0 + 0.68, wy0 + 0.32, wy0 + 0.68, topZ + 0.66 * sc, topZ + 1.06 * sc, '#41814F', false, ARCH_EDGE);
                addPrism(wx0 + 0.40, wx0 + 0.60, wy0 + 0.40, wy0 + 0.60, topZ + 1.06 * sc, topZ + 1.34 * sc, '#529460', false, ARCH_EDGE);
              } else {
                const kind = hashFn(ix, iy, 61);
                if (kind > 0.56) {
                  addPrism(wx0 + 0.45, wx0 + 0.55, wy0 + 0.45, wy0 + 0.55, topZ, topZ + 0.18, '#6E5947');
                  addPrism(wx0 + 0.28, wx0 + 0.72, wy0 + 0.28, wy0 + 0.72, topZ + 0.16, topZ + 0.48, '#D45D86', false, ARCH_EDGE);
                  addPrism(wx0 + 0.36, wx0 + 0.66, wy0 + 0.36, wy0 + 0.66, topZ + 0.44, topZ + 0.58, '#E57EA0', false, ARCH_EDGE);
                } else if (kind > 0.28) {
                  addPrism(wx0 + 0.44, wx0 + 0.56, wy0 + 0.44, wy0 + 0.56, topZ, topZ + 0.24, '#6E5947');
                  addPrism(wx0 + 0.30, wx0 + 0.70, wy0 + 0.30, wy0 + 0.70, topZ + 0.22, topZ + 0.68, '#3E613A', false, ARCH_EDGE);
                  addPrism(wx0 + 0.36, wx0 + 0.64, wy0 + 0.36, wy0 + 0.64, topZ + 0.68, topZ + 1.06, '#4C7347', false, ARCH_EDGE);
                  addPrism(wx0 + 0.42, wx0 + 0.58, wy0 + 0.42, wy0 + 0.58, topZ + 1.06, topZ + 1.32, '#598253', false, ARCH_EDGE);
                } else {
                  addPrism(wx0 + 0.45, wx0 + 0.55, wy0 + 0.45, wy0 + 0.55, topZ, topZ + 0.28, '#6E5947');
                  addPrism(wx0 + 0.26, wx0 + 0.74, wy0 + 0.26, wy0 + 0.74, topZ + 0.26, topZ + 0.60, '#809472', false, ARCH_EDGE);
                }
              }
            }
          }
        }

        // Embarcaciones según bioma
        const boatCoords = activeBiome === 'valle'
          ? [{ ix: 9, iy: 12, idx: 0, phaseOff: 0 }, { ix: 8, iy: 13, idx: 1, phaseOff: 1.8 }]
          : [{ ix: 13, iy: 11, idx: 0, phaseOff: 0 }, { ix: 14, iy: 6, idx: 1, phaseOff: 1.7 }];

        boatCoords.forEach(bc => {
          if ((bc.idx === 0 && budgets.boats >= 1) || (bc.idx === 1 && budgets.boats >= 2)) {
            const bCell = cells[bc.ix] && cells[bc.ix][bc.iy];
            if (!bCell) return;
            const bwZ = bCell.wz + Math.sin(t * 1.8 + bc.phaseOff) * 0.032;
            const bx0 = bc.ix - half + 0.20;
            const by0 = bc.iy - half + 0.32 + boatPush[bc.idx];
            if (activeBiome === 'valle') {
              addPrism(bx0 + 0.04, bx0 + 0.56, by0 + 0.04, by0 + 0.34, bwZ - 0.01, bwZ + 0.14, '#9B6B44', false, ARCH_EDGE);
              addPrism(bx0 + 0.18, bx0 + 0.42, by0 + 0.10, by0 + 0.28, bwZ + 0.14, bwZ + 0.19, '#C49A6E', false, ARCH_EDGE);
            } else {
              addPrism(bx0, bx0 + 0.58, by0, by0 + 0.36, bwZ - 0.02, bwZ + 0.14, '#F5F2EB', false, ARCH_EDGE);
              addPrism(bx0 + 0.26, bx0 + 0.31, by0 + 0.16, by0 + 0.20, bwZ + 0.14, bwZ + 0.84, '#7D5C40', false, ARCH_EDGE);
              addDoubleTriWorld([
                { x: bx0 + 0.28, y: by0 + 0.18, z: bwZ + 0.82 },
                { x: bx0 + 0.28, y: by0 + 0.18, z: bwZ + 0.18 },
                { x: bx0 + 0.60, y: by0 + 0.20, z: bwZ + 0.24 }
              ], '#FFFFFF', false, ARCH_EDGE);
            }
          }
        });
      }

      // Ordenar caras lejanas -> cercanas (Painter's Algorithm) y dibujar
      faces.sort((a, b) => b.distSq - a.distSq);
      ctx.lineJoin = 'round';
      for (let i = 0; i < faces.length; i++) {
        const f = faces[i];
        const pts = f.pts;
        if (f.type === 'photo') {
          if (f.img && f.img.complete && f.img.naturalWidth > 0 && pts.length === 4) {
            const iw = f.img.naturalWidth;
            const ih = f.img.naturalHeight;
            drawTexturedTri(ctx, f.img, 0, 0, iw, 0, iw, ih, pts[0].x, pts[0].y, pts[1].x, pts[1].y, pts[2].x, pts[2].y, true);
            drawTexturedTri(ctx, f.img, 0, 0, iw, ih, 0, ih, pts[0].x, pts[0].y, pts[2].x, pts[2].y, pts[3].x, pts[3].y, false);
            ctx.beginPath();
            ctx.moveTo(pts[0].x, pts[0].y);
            ctx.lineTo(pts[1].x, pts[1].y);
            ctx.lineTo(pts[2].x, pts[2].y);
            ctx.lineTo(pts[3].x, pts[3].y);
            ctx.closePath();
            if (f.phase === 'night') {
              ctx.fillStyle = 'rgba(18, 28, 48, 0.26)';
              ctx.fill();
            } else if (f.phase === 'sunset') {
              ctx.fillStyle = 'rgba(240, 150, 90, 0.12)';
              ctx.fill();
            }
            ctx.strokeStyle = 'rgba(84, 58, 36, 0.42)';
            ctx.lineWidth = 1.0;
            ctx.stroke();
          } else {
            ctx.beginPath();
            ctx.moveTo(pts[0].x, pts[0].y);
            for (let j = 1; j < pts.length; j++) ctx.lineTo(pts[j].x, pts[j].y);
            ctx.closePath();
            ctx.fillStyle = '#D8CEBC';
            ctx.fill();
          }
          continue;
        }
        ctx.beginPath();
        ctx.moveTo(pts[0].x, pts[0].y);
        for (let j = 1; j < pts.length; j++) {
          ctx.lineTo(pts[j].x, pts[j].y);
        }
        ctx.closePath();
        ctx.fillStyle = f.fill;
        ctx.fill();
        ctx.strokeStyle = f.edge || f.fill;
        ctx.lineWidth = f.edge ? 0.85 : 0.65;
        ctx.stroke();
      }

      // Motas de luz cálida en Radiante o velo de neblina tranquila en Neblina suave
      if (weather === 'radiant' && phase !== 'night') {
        ctx.fillStyle = 'rgba(255, 246, 198, 0.42)';
        for (let m = 0; m < 10; m++) {
          const mx = (w * 0.12 + ((m * 113 + t * 18) % (w * 0.78))) + Math.sin(t * 1.4 + m) * 10;
          const my = (h * 0.26 + ((m * 79) % (h * 0.45))) + Math.cos(t * 1.1 + m) * 8;
          ctx.beginPath();
          ctx.arc(mx, my, 1.8 + (m % 2), 0, Math.PI * 2);
          ctx.fill();
        }
      } else if (weather === 'mist') {
        const mistGrad = ctx.createLinearGradient(0, h * 0.34, 0, h * 0.82);
        mistGrad.addColorStop(0, phase === 'night' ? 'rgba(42, 58, 78, 0)' : 'rgba(226, 236, 240, 0)');
        mistGrad.addColorStop(0.5, phase === 'night' ? 'rgba(42, 58, 78, 0.16)' : 'rgba(226, 236, 240, 0.22)');
        mistGrad.addColorStop(1, phase === 'night' ? 'rgba(42, 58, 78, 0)' : 'rgba(226, 236, 240, 0)');
        ctx.fillStyle = mistGrad;
        ctx.fillRect(0, h * 0.34, w, h * 0.48);
      }

      // Pétalos cayendo en Árbol del Mirador
      if (petalTimer > 0) {
        ctx.fillStyle = `rgba(238, 136, 172, ${Math.min(0.9, petalTimer).toFixed(2)})`;
        for (let p = 0; p < 16; p++) {
          const px = ((w * 0.15) + ((p * 97 + t * 54) % (w * 0.72))) + Math.sin(t * 2.4 + p) * 14;
          const py = ((h * 0.18) + ((p * 61 + (1.4 - petalTimer) * 260) % (h * 0.55)));
          ctx.beginPath();
          ctx.ellipse(px, py, 3.6, 2.0, t + p, 0, Math.PI * 2);
          ctx.fill();
        }
      }

      // Hotspots específicos de cada bioma
      if (sceneData) {
        const cfg = getBiomeConfig();
        if (activeBiome === 'costa') {
          const lm = sceneData.layout.landmarkCell;
          const lmCell = lm && sceneData.cells[lm.x] && sceneData.cells[lm.x][lm.y];
          if (lmCell) {
            const lmPos = projectWorldAnchor(
              lm.x - half + 0.5, lm.y - half + 0.5, lmCell.wz + 2.34,
              sinY, cosY, sinP, cosP, w, h, focal
            );
            if (lmPos) {
              hotspots.push({ id: 'faro', x: lmPos.x, y: lmPos.y, r: 46 });
              if (beaconOn) {
                const glowR = 40 + beaconPulse * 28;
                const beamG = ctx.createRadialGradient(lmPos.x, lmPos.y, 2, lmPos.x, lmPos.y, glowR);
                beamG.addColorStop(0, 'rgba(255, 245, 185, 0.72)');
                beamG.addColorStop(0.4, 'rgba(255, 215, 110, 0.28)');
                beamG.addColorStop(1, 'rgba(255, 215, 110, 0)');
                ctx.fillStyle = beamG;
                ctx.beginPath();
                ctx.arc(lmPos.x, lmPos.y, glowR, 0, Math.PI * 2);
                ctx.fill();
              }
            }
          }

          const wm = sceneData.layout.costaWindmillCell;
          const wmCell = wm && sceneData.cells[wm.x] && sceneData.cells[wm.x][wm.y];
          if (wmCell) {
            const wmPos = projectWorldAnchor(
              wm.x - half + 0.7, wm.y - half + 0.5, wmCell.wz + 0.78,
              sinY, cosY, sinP, cosP, w, h, focal
            );
            if (wmPos) hotspots.push({ id: 'molino', x: wmPos.x, y: wmPos.y, r: 44 });
          }

          const houseCell = sceneData.cells[cfg.heroHouseCell.x] && sceneData.cells[cfg.heroHouseCell.x][cfg.heroHouseCell.y];
          if (houseCell) {
            const lampPos = projectWorldAnchor(
              cfg.heroHouseCell.x - half + 0.89, cfg.heroHouseCell.y - half + 0.82, houseCell.wz + 0.44,
              sinY, cosY, sinP, cosP, w, h, focal
            );
            if (lampPos) hotspots.push({ id: 'casaLamp', x: lampPos.x, y: lampPos.y, r: 36 });
          }

          const treeCell = sceneData.cells[cfg.heroTreeCell.x] && sceneData.cells[cfg.heroTreeCell.x][cfg.heroTreeCell.y];
          if (treeCell) {
            const treePos = projectWorldAnchor(
              cfg.heroTreeCell.x - half + 0.5, cfg.heroTreeCell.y - half + 0.5, treeCell.wz + 1.85,
              sinY, cosY, sinP, cosP, w, h, focal
            );
            if (treePos) hotspots.push({ id: 'heroTree', x: treePos.x, y: treePos.y, r: 52 });
          }

          const boat0Cell = sceneData.cells[13] && sceneData.cells[13][11];
          if (boat0Cell) {
            const bPos = projectWorldAnchor(13 - half + 0.5, 11 - half + 0.5 + boatPush[0], boat0Cell.wz + 0.45, sinY, cosY, sinP, cosP, w, h, focal);
            if (bPos) hotspots.push({ id: 'velero0', x: bPos.x, y: bPos.y, r: 44 });
          }
          const boat1Cell = sceneData.cells[14] && sceneData.cells[14][6];
          if (boat1Cell) {
            const b2Pos = projectWorldAnchor(14 - half + 0.5, 6 - half + 0.5 + boatPush[1], boat1Cell.wz + 0.45, sinY, cosY, sinP, cosP, w, h, focal);
            if (b2Pos) hotspots.push({ id: 'velero1', x: b2Pos.x, y: b2Pos.y, r: 44 });
          }
        } else {
          const refCell = sceneData.cells[cfg.heroHouseCell.x] && sceneData.cells[cfg.heroHouseCell.x][cfg.heroHouseCell.y];
          if (refCell) {
            const fPos = projectWorldAnchor(
              cfg.heroHouseCell.x - half + 0.06, cfg.heroHouseCell.y - half + 0.78, refCell.wz + 0.30,
              sinY, cosY, sinP, cosP, w, h, focal
            );
            if (fPos) hotspots.push({ id: 'valleFire', x: fPos.x, y: fPos.y, r: 40 });
          }
          const viewCell = sceneData.cells[cfg.heroViewCell.x] && sceneData.cells[cfg.heroViewCell.x][cfg.heroViewCell.y];
          if (viewCell) {
            const lPos = projectWorldAnchor(
              cfg.heroViewCell.x - half + 0.20, cfg.heroViewCell.y - half + 0.78, viewCell.wz + 0.44,
              sinY, cosY, sinP, cosP, w, h, focal
            );
            if (lPos) hotspots.push({ id: 'valleLantern', x: lPos.x, y: lPos.y, r: 38 });
          }
          const vb0 = sceneData.cells[9] && sceneData.cells[9][12];
          if (vb0) {
            const p0 = projectWorldAnchor(9 - half + 0.5, 12 - half + 0.5 + boatPush[0], vb0.wz + 0.24, sinY, cosY, sinP, cosP, w, h, focal);
            if (p0) hotspots.push({ id: 'valleBoat0', x: p0.x, y: p0.y, r: 42 });
          }
          const vb1 = sceneData.cells[8] && sceneData.cells[8][13];
          if (vb1) {
            const p1 = projectWorldAnchor(8 - half + 0.5, 13 - half + 0.5 + boatPush[1], vb1.wz + 0.24, sinY, cosY, sinP, cosP, w, h, focal);
            if (p1) hotspots.push({ id: 'valleBoat1', x: p1.x, y: p1.y, r: 42 });
          }
        }

        // Indicador sutil pulsante en pantalla
        const pulse = 0.5 + 0.5 * Math.sin(t * 2.6);
        for (let i = 0; i < hotspots.length; i++) {
          const hs = hotspots[i];
          if (hs.x < 18 || hs.x > w - 18 || hs.y < 60 || hs.y > h - 80) continue;
          ctx.save();
          ctx.beginPath();
          ctx.arc(hs.x, hs.y, (hs.isPhoto ? 9 : 11) + pulse * 3.5, 0, Math.PI * 2);
          ctx.strokeStyle = hs.isPhoto
            ? `rgba(255, 226, 154, ${(0.42 + pulse * 0.30).toFixed(2)})`
            : `rgba(255, 255, 255, ${(0.28 + pulse * 0.25).toFixed(2)})`;
          ctx.lineWidth = 1.6;
          ctx.stroke();
          ctx.beginPath();
          ctx.arc(hs.x, hs.y, 3.0, 0, Math.PI * 2);
          ctx.fillStyle = hs.isPhoto ? 'rgba(255, 232, 168, 0.90)' : 'rgba(255, 255, 255, 0.78)';
          ctx.fill();
          ctx.restore();
        }
      }
    } catch (err) {
      if (typeof console !== 'undefined' && console.error) {
        console.error('BroteExploreCosta frame error:', err);
      }
    }

    rafId = requestAnimationFrame(drawScene);
  }

  function handleCanvasTap(px, py) {
    let best = null;
    let bestDist = Infinity;
    hotspots.forEach(hs => {
      const d = Math.hypot(px - hs.x, py - hs.y);
      if (d <= hs.r && d < bestDist) {
        bestDist = d;
        best = hs;
      }
    });
    if (!best) {
      setCaption(tx('Camina con las flechas o desliza para girar 360°', 'Walk with the arrows or drag to look 360°'));
      return;
    }

    if (best.id === 'photoFrame' && best.photo) {
      openPhotoModal(best.photo);
      setCaption(best.photo.isPlaceholder
        ? tx('Marco listo para tus fotos del Muro Real', 'Frame ready for your Real Wall photos')
        : tx(`Recuerdo del Muro Real · ${best.photo.label}`, `Real Wall Memory · ${best.photo.label}`));
    } else if (best.id === 'faro') {
      beaconOn = !beaconOn;
      beaconPulse = 1.0;
      setCaption(beaconOn ? tx('Haz del faro encendido sobre la cala', 'Lighthouse beam shining across the cove') : tx('Luz del faro atenuada', 'Lighthouse beam dimmed'));
    } else if (best.id === 'molino') {
      windmillSpeed = 5.4;
      setCaption(tx('La brisa marina acelera las aspas del molino', 'Sea breeze spins the windmill sails faster'));
    } else if (best.id === 'casaLamp') {
      lampOn = !lampOn;
      setCaption(lampOn ? tx('Lámpara interior de la casa encendida', 'Indoor house lamp turned on') : tx('Lámpara de la casa apagada', 'House lamp turned off'));
    } else if (best.id === 'heroTree') {
      petalTimer = 1.4;
      setCaption(tx('Brisa entre las flores de buganvilla y el olivo', 'Breeze rustling through olive leaves & bougainvillea'));
    } else if (best.id === 'velero0' || best.id === 'velero1') {
      const idx = best.id === 'velero0' ? 0 : 1;
      boatPush[idx] = -0.85;
      setCaption(tx('El velero avanza suave por la cala azul', 'The sailboat glides smoothly across the cove'));
    } else if (best.id === 'valleFire') {
      fireOn = !fireOn;
      setCaption(fireOn ? tx('Chimenea de leña encendida en el refugio', 'Cabin fireplace roaring warmly') : tx('Brasas tranquilas en la chimenea', 'Gentle embers in the hearth'));
    } else if (best.id === 'valleLantern') {
      lampOn = !lampOn;
      setCaption(lampOn ? tx('Farol del mirador encendido', 'Overlook lantern turned on') : tx('Farol del mirador apagado', 'Overlook lantern turned off'));
    } else if (best.id === 'valleBoat0' || best.id === 'valleBoat1') {
      const idx = best.id === 'valleBoat0' ? 0 : 1;
      boatPush[idx] = -0.75;
      setCaption(tx('La barca de madera se desliza por el lago alpino', 'The wooden boat glides across the alpine lake'));
    }
  }

  function bindEvents() {
    if (eventsBound) return;
    eventsBound = true;

    const canvas = document.getElementById('explore-costa-canvas');
    if (canvas) {
      canvas.addEventListener('pointerdown', (e) => {
        canvas.setPointerCapture(e.pointerId);
        pointerId = e.pointerId;
        lastPt = { x: e.clientX, y: e.clientY };
        tapStart = { x: e.clientX, y: e.clientY };
      });

      canvas.addEventListener('pointermove', (e) => {
        if (pointerId !== e.pointerId || !lastPt) return;
        const dx = e.clientX - lastPt.x;
        const dy = e.clientY - lastPt.y;
        lastPt = { x: e.clientX, y: e.clientY };
        cam.targetYaw -= dx * 0.0052;
        cam.targetPitch = clamp(cam.targetPitch + dy * 0.0042, -0.58, 0.58);
      });

      function onPointerEnd(e) {
        if (pointerId !== e.pointerId) return;
        if (e.type === 'pointerup' && tapStart) {
          const moved = Math.hypot(e.clientX - tapStart.x, e.clientY - tapStart.y);
          if (moved <= 9) {
            const rect = canvas.getBoundingClientRect();
            handleCanvasTap(e.clientX - rect.left, e.clientY - rect.top);
          }
        }
        pointerId = null;
        lastPt = null;
        tapStart = null;
      }
      canvas.addEventListener('pointerup', onPointerEnd);
      canvas.addEventListener('pointercancel', onPointerEnd);
    }

    window.addEventListener('keydown', (e) => {
      if (!isOpen) return;
      if (e.key === 'w' || e.key === 'W' || e.key === 'ArrowUp') moveState.fwd = true;
      if (e.key === 's' || e.key === 'S' || e.key === 'ArrowDown') moveState.back = true;
      if (e.key === 'a' || e.key === 'A' || e.key === 'ArrowLeft') moveState.left = true;
      if (e.key === 'd' || e.key === 'D' || e.key === 'ArrowRight') moveState.right = true;
    });
    window.addEventListener('keyup', (e) => {
      if (e.key === 'w' || e.key === 'W' || e.key === 'ArrowUp') moveState.fwd = false;
      if (e.key === 's' || e.key === 'S' || e.key === 'ArrowDown') moveState.back = false;
      if (e.key === 'a' || e.key === 'A' || e.key === 'ArrowLeft') moveState.left = false;
      if (e.key === 'd' || e.key === 'D' || e.key === 'ArrowRight') moveState.right = false;
    });
  }

  function open(opts = {}) {
    applyLanguage();
    bindEvents();
    isOpen = true;
    closePhotoModal();
    activeBiome = opts.biome === 'valle' ? 'valle' : 'costa';

    const pts = Math.max(opts.points || 0, 80);
    if (window.BroteMascot && typeof window.BroteMascot.getBiomeSceneData === 'function') {
      sceneData = window.BroteMascot.getBiomeSceneData(activeBiome, pts);
    } else {
      sceneData = activeBiome === 'valle' ? buildFallbackValleScene() : buildFallbackCostaScene();
    }

    refreshGalleryPhotos(opts.photos);

    const cfg = getBiomeConfig();
    spotId = cfg.defaultSpot;
    const sp = cfg.spots[spotId];
    cam.x = cam.targetX = sp.x;
    cam.y = cam.targetY = sp.y;
    cam.yaw = cam.targetYaw = sp.yaw;
    cam.pitch = cam.targetPitch = sp.pitch;
    cam.z = getWalkHeightAt(sp.x, sp.y);
    moveState.fwd = moveState.back = moveState.left = moveState.right = false;

    lightMode = opts.lightMode || 'auto';
    liveWeather = opts.weatherMode || 'clear';
    weatherOverride = 'auto';
    soundOn = Boolean(opts.soundOn);

    if (soundOn && window.BroteAudio) {
      window.BroteAudio.startBiomeAmbient(activeBiome);
    }

    const activeW = resolveWeather();
    if (activeW === 'mist') {
      setCaption(tx('Neblina suave · Completa 1 reto rápido para despejar el cielo', 'Soft mist · Complete 1 quick goal to clear the sky'));
    } else if (activeW === 'radiant') {
      setCaption(tx('Día radiante · Arrastra 360° o toca un cuadro del Muro Real', 'Radiant day · Drag 360° or tap a Real Wall frame'));
    } else {
      setCaption(cfg.defaultCaption);
    }

    rebuildSpotsDock();
    const modal = document.getElementById('modal-explore-costa');
    if (modal) modal.classList.remove('hidden');
    updateUI();
    lastFrameMs = 0;
    if (rafId) cancelAnimationFrame(rafId);
    rafId = requestAnimationFrame(drawScene);
  }

  function close() {
    isOpen = false;
    closePhotoModal();
    moveState.fwd = moveState.back = moveState.left = moveState.right = false;
    if (rafId) {
      cancelAnimationFrame(rafId);
      rafId = null;
    }
    if (window.BroteAudio) {
      if (typeof window.BroteAudio.resetSpatial3D === 'function') {
        window.BroteAudio.resetSpatial3D();
      }
      if (soundOn) {
        window.BroteAudio.stopAmbient();
      }
    }
    soundOn = false;
    const modal = document.getElementById('modal-explore-costa');
    if (modal) modal.classList.add('hidden');
  }

  function goToSpot(nextSpotId) {
    const cfg = getBiomeConfig();
    const sp = cfg.spots[nextSpotId];
    if (!sp) return;
    closePhotoModal();
    spotId = nextSpotId;
    cam.targetX = sp.x;
    cam.targetY = sp.y;
    cam.targetYaw = sp.yaw;
    cam.targetPitch = sp.pitch;
    setCaption(`${sp.label} · ${sp.sub}`);
    updateUI();
  }

  function setMove(dir, active) {
    if (dir in moveState) {
      moveState[dir] = Boolean(active);
    }
  }

  function cycleLight() {
    const order = ['auto', 'dawn', 'day', 'sunset', 'night'];
    const idx = order.indexOf(lightMode);
    lightMode = order[(idx + 1) % order.length];
    updateUI();
  }

  function cycleWeather() {
    const order = ['auto', 'radiant', 'clear', 'mist'];
    const idx = order.indexOf(weatherOverride);
    weatherOverride = order[(idx + 1) % order.length];
    const w = resolveWeather();
    if (w === 'mist') {
      setCaption(tx('Neblina tranquila · Completa 1 reto rápido para despejar el cielo', 'Calm mist · Complete 1 quick goal to clear the sky'));
    } else if (w === 'radiant') {
      setCaption(tx('Clima radiante · Cielo abierto con arcoíris y aves', 'Radiant weather · Clear sky with rainbow & birds'));
    } else {
      setCaption(tx('Clima en calma · Brisa suave en el bioma', 'Calm weather · Soft breeze across the biome'));
    }
    updateUI();
  }

  function toggleSound() {
    if (!window.BroteAudio) return;
    if (soundOn) {
      if (typeof window.BroteAudio.resetSpatial3D === 'function') {
        window.BroteAudio.resetSpatial3D();
      }
      window.BroteAudio.stopAmbient();
      soundOn = false;
    } else {
      window.BroteAudio.startBiomeAmbient(activeBiome);
      soundOn = true;
      updateSpatialAudioForCamera();
    }
    updateUI();
  }

  applyLanguage();

  const api = {
    open,
    close,
    applyLanguage,
    goToSpot,
    setMove,
    cycleLight,
    cycleWeather,
    toggleSound,
    closePhoto: closePhotoModal
  };
  window.BroteExplore3D = api;
  return api;
})();
