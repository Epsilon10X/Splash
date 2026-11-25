/**
 * Splash — Main Application Logic (Material 3 Expressive)
 * UX Golden Rule: Open, pick/complete, and exit in < 10 seconds.
 */
(function () {
  const STORAGE_KEY = 'brote_state_v2';
  const FEEDBACK_FORM_URL = 'https://docs.google.com/forms/d/e/1FAIpQLSexZL4gNLcfhxN2Lbal3WaCHdkz8fzFOnr6iZM-VWFrdMiKwA/viewform';

  function tx(esVal, enVal) {
    return window.SplashI18n ? window.SplashI18n.pick(esVal, enVal) : esVal;
  }

  function getLocale() {
    return window.SplashI18n && window.SplashI18n.isEn() ? 'en-US' : 'es-ES';
  }

  const BIOME_PTS = { RAPIDO: 1, MEDIO: 4, LARGO: 8 };
  const CAP_RAPIDO_DAILY = 2;
  const CAP_MEDIO_DAYS = 2;
  const CAP_LARGO_WEEKLY = 2;
  const QUICK_PING_MS = 20 * 60 * 1000;
  const MEDIO_PING_MS = 4 * 60 * 60 * 1000;

  const BIOME_NOTIFS_I18N = {
    valle: {
      es: { quickBadge: 'Brisa del Valle', medioBadge: 'Sendero de Altura', rescueCopy: 'El Valle te espera tranquilo. Sal de la pantalla en menos de 10 segundos:' },
      en: { quickBadge: 'Valley Breeze', medioBadge: 'Highland Trail', rescueCopy: 'The Valley awaits calmly. Step away from your screen in under 10 seconds:' }
    },
    jungla: {
      es: { quickBadge: 'Brote de Jungla', medioBadge: 'Pausa Tropical', rescueCopy: 'Respira aire verde en la Jungla. Desconecta del scroll en menos de 10 segundos:' },
      en: { quickBadge: 'Jungle Sprout', medioBadge: 'Tropical Pause', rescueCopy: 'Breathe fresh green air in the Jungle. Break the scroll in under 10 seconds:' }
    },
    costa: {
      es: { quickBadge: 'Brisa Marina', medioBadge: 'Cala en Calma', rescueCopy: 'Levanta la vista hacia el horizonte de Costa. Sal de la pantalla:' },
      en: { quickBadge: 'Sea Breeze', medioBadge: 'Calm Cove', rescueCopy: 'Lift your eyes to the Coast horizon. Step off-screen:' }
    },
    kioto: {
      es: { quickBadge: 'Ritual de Kioto', medioBadge: 'Pausa Zen', rescueCopy: 'Regálate unos minutos de silencio lejos del móvil:' },
      en: { quickBadge: 'Kyoto Ritual', medioBadge: 'Zen Pause', rescueCopy: 'Give yourself a few quiet minutes away from your phone:' }
    },
    desierto: {
      es: { quickBadge: 'Parada en el Oasis', medioBadge: 'Sombra en Dunas', rescueCopy: 'Descansa la mente del ruido digital en tu Oasis:' },
      en: { quickBadge: 'Oasis Stop', medioBadge: 'Dune Shade', rescueCopy: 'Rest your mind from digital noise in your Oasis:' }
    },
    arrozales: {
      es: { quickBadge: 'Pulso de Bali', medioBadge: 'Ritmo Sereno', rescueCopy: 'Vuelve a tus manos y suelta la pantalla unos minutos:' },
      en: { quickBadge: 'Bali Pulse', medioBadge: 'Serene Rhythm', rescueCopy: 'Return to your hands and put the screen down for a few minutes:' }
    },
    otono: {
      es: { quickBadge: 'Hoja de Otoño', medioBadge: 'Paseo Dorado', rescueCopy: 'Aprovecha la luz cálida de Otoño fuera del teléfono:' },
      en: { quickBadge: 'Autumn Leaf', medioBadge: 'Golden Stroll', rescueCopy: 'Catch the warm Autumn light away from your phone:' }
    },
    fiordo: {
      es: { quickBadge: 'Refugio Nórdico', medioBadge: 'Luz de Fiordo', rescueCopy: 'Baja el ritmo como junto a la hoguera del Fiordo:' },
      en: { quickBadge: 'Nordic Refuge', medioBadge: 'Fjord Light', rescueCopy: 'Slow down as if sitting beside a Fjord hearth:' }
    },
    atolon: {
      es: { quickBadge: 'Marea del Atolón', medioBadge: 'Laguna Viva', rescueCopy: 'Tómate una pausa limpia lejos de notificaciones:' },
      en: { quickBadge: 'Atoll Tide', medioBadge: 'Living Lagoon', rescueCopy: 'Take a clean pause far away from notifications:' }
    },
    toscana: {
      es: { quickBadge: 'Sol de Toscana', medioBadge: 'Tarde sin Prisa', rescueCopy: 'Disfruta un rato lento en el mundo real:' },
      en: { quickBadge: 'Tuscan Sun', medioBadge: 'Slow Afternoon', rescueCopy: 'Enjoy a slow moment in the real world:' }
    }
  };
  const BIOME_NOTIFS = {};

  const SCHED_DAYS_I18N = {
    es: [
      { key: 'mon', label: 'L', name: 'Lunes' },
      { key: 'tue', label: 'M', name: 'Martes' },
      { key: 'wed', label: 'X', name: 'Miércoles' },
      { key: 'thu', label: 'J', name: 'Jueves' },
      { key: 'fri', label: 'V', name: 'Viernes' },
      { key: 'sat', label: 'S', name: 'Sábado' },
      { key: 'sun', label: 'D', name: 'Domingo' }
    ],
    en: [
      { key: 'mon', label: 'M', name: 'Monday' },
      { key: 'tue', label: 'T', name: 'Tuesday' },
      { key: 'wed', label: 'W', name: 'Wednesday' },
      { key: 'thu', label: 'T', name: 'Thursday' },
      { key: 'fri', label: 'F', name: 'Friday' },
      { key: 'sat', label: 'S', name: 'Saturday' },
      { key: 'sun', label: 'S', name: 'Sunday' }
    ]
  };
  const SCHED_DAYS = SCHED_DAYS_I18N.es.map(d => ({ ...d }));

  const SCHED_BANDS_I18N = {
    es: [
      { key: 'morning', label: 'Mañana', hours: [7, 8, 9, 10, 11, 12] },
      { key: 'afternoon', label: 'Tarde', hours: [13, 14, 15, 16, 17, 18] },
      { key: 'night', label: 'Noche', hours: [19, 20, 21, 22, 23] }
    ],
    en: [
      { key: 'morning', label: 'Morning', hours: [7, 8, 9, 10, 11, 12] },
      { key: 'afternoon', label: 'Afternoon', hours: [13, 14, 15, 16, 17, 18] },
      { key: 'night', label: 'Evening', hours: [19, 20, 21, 22, 23] }
    ]
  };
  const SCHED_BANDS = SCHED_BANDS_I18N.es.map(b => ({ ...b, hours: b.hours.slice() }));

  function applyAppConstantsLanguage(lang) {
    const useEn = (lang || (window.SplashI18n && window.SplashI18n.getLang())) === 'en';
    const key = useEn ? 'en' : 'es';
    Object.keys(BIOME_NOTIFS_I18N).forEach(bid => {
      BIOME_NOTIFS[bid] = BIOME_NOTIFS_I18N[bid][key];
    });
    SCHED_DAYS_I18N[key].forEach((d, idx) => {
      SCHED_DAYS[idx].label = d.label;
      SCHED_DAYS[idx].name = d.name;
    });
    SCHED_BANDS_I18N[key].forEach((b, idx) => {
      SCHED_BANDS[idx].label = b.label;
    });
  }
  applyAppConstantsLanguage();

  // Semana actual en Brote: los retos semanales se renuevan el lunes a las 04:00
  function getBroteWeekKey(date = new Date()) {
    const shifted = new Date(date.getTime() - 4 * 60 * 60 * 1000);
    return getWeekKey(shifted);
  }

  function getTodayDayKey() {
    const jsDay = new Date().getDay();
    return ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'][jsDay];
  }

  function timeStrToMinutes(hhmm) {
    if (typeof hhmm !== 'string' || !hhmm.includes(':')) return -1;
    const [h, m] = hhmm.split(':').map(Number);
    if (Number.isNaN(h) || Number.isNaN(m)) return -1;
    return h * 60 + m;
  }

  function normalizeHoursList(hoursArr) {
    const set = new Set();
    (hoursArr || []).forEach(h => {
      const n = Number(h);
      if (Number.isInteger(n) && n >= 7 && n <= 23) set.add(n);
    });
    return Array.from(set).sort((a, b) => a - b);
  }

  function createDefaultWeekSchedule() {
    return {
      mon: { enabled: true, hours: [9, 10, 11, 12, 19, 20, 21] },
      tue: { enabled: true, hours: [9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21] },
      wed: { enabled: true, hours: [9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21] },
      thu: { enabled: true, hours: [9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21] },
      fri: { enabled: true, hours: [9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21] },
      sat: { enabled: false, hours: [10, 11, 12, 13, 14, 15, 16, 17, 18, 19] },
      sun: { enabled: false, hours: [10, 11, 12, 13, 14, 15, 16, 17, 18, 19] }
    };
  }

  function normalizeWeekSchedule(raw) {
    const def = createDefaultWeekSchedule();
    const out = {};
    SCHED_DAYS.forEach(d => {
      const src = raw && raw[d.key];
      if (!src || typeof src !== 'object') {
        out[d.key] = { enabled: def[d.key].enabled, hours: def[d.key].hours.slice() };
        return;
      }
      if (Array.isArray(src.hours)) {
        const hrs = normalizeHoursList(src.hours);
        out[d.key] = {
          enabled: typeof src.enabled === 'boolean' ? src.enabled : hrs.length > 0,
          hours: hrs.length ? hrs : def[d.key].hours.slice()
        };
        return;
      }
      if (Array.isArray(src.ranges)) {
        const hrs = [];
        src.ranges.forEach(r => {
          const s = timeStrToMinutes(r && r.start);
          const e = timeStrToMinutes(r && r.end);
          if (s >= 0 && e > s) {
            for (let h = 7; h <= 23; h++) {
              if (e > h * 60 && s < (h + 1) * 60) hrs.push(h);
            }
          }
        });
        const clean = normalizeHoursList(hrs);
        out[d.key] = {
          enabled: typeof src.enabled === 'boolean' ? src.enabled : clean.length > 0,
          hours: clean.length ? clean : def[d.key].hours.slice()
        };
        return;
      }
      const hrs = [];
      if (src.m) hrs.push(9, 10, 11, 12);
      if (src.t) hrs.push(14, 15, 16, 17, 18);
      if (src.n) hrs.push(19, 20, 21);
      const clean = normalizeHoursList(hrs);
      out[d.key] = {
        enabled: clean.length > 0,
        hours: clean.length ? clean : def[d.key].hours.slice()
      };
    });
    return out;
  }

  function cloneWeekSchedule(weekObj) {
    return normalizeWeekSchedule(weekObj);
  }

  function dayIndexFromDateKey(dateKey) {
    const [y, m, d] = dateKey.split('-').map(Number);
    return Math.floor(Date.UTC(y, m - 1, d) / 86400000);
  }

  function parseLogEntryDate(dateKey, entry) {
    if (entry && entry.timestamp) {
      const d = new Date(entry.timestamp);
      if (!Number.isNaN(d.getTime())) return d;
    }
    return new Date(dateKey + 'T12:00:00');
  }

  function computeLegacyPointsFromLogs(logsByDate) {
    let pts = 0;
    let lastMedioDayIdx = -9999;
    const largoPerWeek = {};
    Object.keys(logsByDate || {}).sort().forEach(dateKey => {
      const day = logsByDate[dateKey];
      if (!day || !Array.isArray(day.completed) || !day.completed.length) return;
      const dayIdx = dayIndexFromDateKey(dateKey);
      let rapidoCount = 0;
      day.completed.forEach(c => {
        if (c.categoria === 'RAPIDO') {
          if (rapidoCount < CAP_RAPIDO_DAILY) {
            rapidoCount++;
            pts += BIOME_PTS.RAPIDO;
          }
        } else if (c.categoria === 'MEDIO') {
          if (dayIdx - lastMedioDayIdx >= CAP_MEDIO_DAYS) {
            lastMedioDayIdx = dayIdx;
            pts += BIOME_PTS.MEDIO;
          }
        } else if (c.categoria === 'LARGO') {
          const wk = getBroteWeekKey(parseLogEntryDate(dateKey, c));
          const used = largoPerWeek[wk] || 0;
          if (used < CAP_LARGO_WEEKLY) {
            largoPerWeek[wk] = used + 1;
            pts += BIOME_PTS.LARGO;
          }
        }
      });
    });
    return Math.min(80, pts);
  }

  function getTodayKey(date = new Date()) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  function getWeekKey(date = new Date()) {
    const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
    const dayNum = d.getUTCDay() || 7;
    d.setUTCDate(d.getUTCDate() + 4 - dayNum);
    const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
    const weekNo = Math.ceil((((d - yearStart) / 86400000) + 1) / 7);
    return `${d.getUTCFullYear()}-W${String(weekNo).padStart(2, '0')}`;
  }

  function iconHTML(name, extraClass = '') {
    const safeName = /^[a-z0-9_]+$/.test(name || '') ? name : 'eco';
    return `<span class="material-symbols-rounded ${extraClass}">${safeName}</span>`;
  }

  function createInitialState() {
    return {
      catalog: JSON.parse(JSON.stringify(window.BROTE_CATALOG || [])),
      logsByDate: {},
      lumi: {
        biome: 'valle',
        biomePoints: { valle: 0 },
        valleStartMigrated: true,
        betaPointsOverride: null,
        gardenItems: 0,
        totalLongs: 0,
        lastAwakeDate: null
      },
      weekly: {
        weekKey: '',
        challengeIds: [],
        rerollsLeft: 1
      },
      suggestions: {
        rapidos: [],
        medios: []
      },
      beRealPing: {
        active: false,
        category: 'RAPIDO',
        goalId: null,
        expiresAt: 0
      },
      notifSchedule: {
        defaultWeek: createDefaultWeekSchedule(),
        weekOverrides: {}
      },
      ui: {
        biomesViewMode: 'grid',
        themeMode: 'system',
        betaMode: true,
        lang: (window.SplashI18n && window.SplashI18n.getLang()) || 'es'
      },
      user: {
        name: ''
      }
    };
  }

  const FALLBACK_BIOME_ORDER = [
    'valle', 'jungla', 'costa', 'kioto', 'desierto',
    'arrozales', 'otono', 'fiordo', 'atolon', 'toscana'
  ];

  function getBiomeOrderIds() {
    if (window.BroteMascot && Array.isArray(window.BroteMascot.BIOMES) && window.BroteMascot.BIOMES.length) {
      return window.BroteMascot.BIOMES.map(b => b.id);
    }
    return FALLBACK_BIOME_ORDER;
  }

  function getCurrentProgressBiomeIdFromPoints(pointsMap) {
    const maxPts = window.BroteMascot ? window.BroteMascot.MAX_POINTS : 80;
    const order = getBiomeOrderIds();
    const map = pointsMap || {};
    const next = order.find(id => (map[id] || 0) < maxPts);
    return next || order[order.length - 1] || 'valle';
  }

  let state = loadState();
  let currentCalendarDate = new Date();
  let catalogFilter = 'ALL';
  let catalogQuery = '';
  let activeHomeBlock = 'RAPIDO';
  let toastTimeout = null;
  let schedMode = 'default';
  let schedWeekOffset = 0;
  let schedSelectedDay = getTodayDayKey();
  let schedDragTargetState = null;
  const showExtraInBlock = { RAPIDO: false, MEDIO: false, LARGO: false };
  let showCompletedDrawer = false;
  let activeMuroView = 'calendario';
  let selectedArchFriendId = null;
  let circleShowNewForm = false;
  let mapCircleFilter = 'ALL';
  let mapDesignStyle = 'zootropolis';
  let mapShowAllColor = true;
  let worldMapScroll = null;
  let pendingPhotoDateKey = null;
  let widgetStudio = { open: false, type: '4x2', timeMode: 'auto', themeMode: 'auto' };

  let activeTimer = {
    goal: null,
    totalSeconds: 15 * 60,
    remainingSeconds: 15 * 60,
    running: false,
    intervalId: null,
    leftForSocial: false
  };

  let activeAnalog = {
    goal: null,
    elapsedSeconds: 0,
    pausedByLeave: false,
    intervalId: null,
    countdownMode: false,
    remainingSeconds: 0,
    running: false
  };

  let cameraStream = null;
  let capturedDataUrl = null;
  let beRealIntervalId = null;

  let viewer3D = {
    open: false,
    biome: 'valle',
    yaw: Math.PI / 4,
    pitch: 0.60,
    zoom: 1.05,
    velYaw: 0,
    velPitch: 0,
    targetZoom: 1.05,
    lightMode: 'auto', // 'auto' | 'day' | 'sunset' | 'night'
    soundOn: false,
    pointers: new Map(),
    lastPinchDist: null,
    targetYaw: null,
    targetPitch: null,
    prevFrameMs: 0,
    rafId: null
  };

  function syncStateCatalogLanguage(targetState) {
    const s = targetState || state;
    if (!s || !window.BROTE_CATALOG) return;
    const baseMap = new Map(window.BROTE_CATALOG.map(item => [item.id, item]));
    (s.catalog || []).forEach(item => {
      const base = baseMap.get(item.id);
      if (base) {
        if (base.titulo) item.titulo = base.titulo;
        if (base.tituloCorto) item.tituloCorto = base.tituloCorto;
        if (base.subcategoria) item.subcategoria = base.subcategoria;
      }
    });
    if (s.logsByDate) {
      Object.values(s.logsByDate).forEach(day => {
        if (day && Array.isArray(day.completed)) {
          day.completed.forEach(c => {
            if (c && c.id === 5 && c.titulo === 'Respiración fisiológica') {
              const b5 = baseMap.get(5);
              c.titulo = (b5 && b5.titulo) || tx('Respirar de forma relajada durante 1 minuto', 'Breathe slowly and calmly for 1 full minute');
            } else if (c && baseMap.has(c.id)) {
              c.titulo = baseMap.get(c.id).titulo;
            }
          });
        }
      });
    }
  }

  function syncViewerBiomePillLabels() {
    if (!window.BroteMascot) return;
    document.querySelectorAll('#viewer-biome-pills .viewer-pill').forEach(pill => {
      const bm = window.BroteMascot.getBiome(pill.dataset.biome);
      if (bm && bm.shortName) {
        pill.textContent = bm.shortName;
      }
    });
  }

  function syncAllModulesLanguage(lang) {
    const resolved = lang === 'en' ? 'en' : 'es';
    if (window.SplashI18n) window.SplashI18n.setLang(resolved);
    if (typeof window.applyCatalogLanguage === 'function') window.applyCatalogLanguage(resolved);
    if (window.BroteAudio && window.BroteAudio.applyLanguage) window.BroteAudio.applyLanguage(resolved);
    if (window.BroteMascot && window.BroteMascot.applyLanguage) window.BroteMascot.applyLanguage(resolved);
    if (window.BroteWorldMap && window.BroteWorldMap.applyLanguage) window.BroteWorldMap.applyLanguage(resolved);
    if (window.BroteSocial && window.BroteSocial.applyLanguage) window.BroteSocial.applyLanguage(resolved);
    if (window.BroteExplore3D && window.BroteExplore3D.applyLanguage) window.BroteExplore3D.applyLanguage(resolved);
    applyAppConstantsLanguage(resolved);
    syncStateCatalogLanguage();
    syncViewerBiomePillLabels();
  }

  function loadState() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return createInitialState();
      const parsed = JSON.parse(raw);
      const REMOVED_BASE_IDS = [12, 28];
      if (!parsed.catalog || !parsed.catalog.length) {
        parsed.catalog = JSON.parse(JSON.stringify(window.BROTE_CATALOG || []));
      } else if (window.BROTE_CATALOG) {
        const baseMap = new Map(window.BROTE_CATALOG.map(item => [item.id, item]));
        const existingIds = new Set();
        parsed.catalog = parsed.catalog.filter(item => !REMOVED_BASE_IDS.includes(item.id));
        parsed.catalog.forEach(item => {
          existingIds.add(item.id);
          const base = baseMap.get(item.id);
          if (base) {
            if (base.titulo) item.titulo = base.titulo;
            if (base.tituloCorto) item.tituloCorto = base.tituloCorto;
            if (base.categoria) item.categoria = base.categoria;
            if (base.subcategoria) item.subcategoria = base.subcategoria;
            if (base.icono) item.icono = base.icono;
            if (base.tipoVerificacion) item.tipoVerificacion = base.tipoVerificacion;
            if (base.franjaHoraria && !item.franjaHoraria) item.franjaHoraria = base.franjaHoraria;
            if (base.biomaExclusivo) item.biomaExclusivo = base.biomaExclusivo;
            if (base.esSecretoBioma) item.esSecretoBioma = true;
          }
        });
        window.BROTE_CATALOG.forEach(base => {
          if (!existingIds.has(base.id)) {
            parsed.catalog.push(JSON.parse(JSON.stringify(base)));
          }
        });
      }
      if (parsed.suggestions) {
        ['rapidos', 'medios', 'largos'].forEach(k => {
          if (Array.isArray(parsed.suggestions[k])) {
            parsed.suggestions[k] = parsed.suggestions[k].filter(id => !REMOVED_BASE_IDS.includes(id));
          }
        });
        if (Array.isArray(parsed.suggestions.medios)) {
          parsed.suggestions.medios = parsed.suggestions.medios.filter(id => id !== 18);
        }
      }
      if (!parsed.lumi) {
        parsed.lumi = { biome: 'valle', biomePoints: { valle: 3 }, valleStartMigrated: true, betaPointsOverride: null, gardenItems: 2, totalLongs: 1, lastAwakeDate: null };
      }
      if (!parsed.lumi.biome) {
        parsed.lumi.biome = 'valle';
      }
      if (!parsed.lumi.biomePoints || typeof parsed.lumi.biomePoints !== 'object') {
        parsed.lumi.biomePoints = {};
        parsed.lumi.biomePoints[parsed.lumi.biome] = computeLegacyPointsFromLogs(parsed.logsByDate || {});
      }
      if (!parsed.lumi.valleStartMigrated) {
        const pts = parsed.lumi.biomePoints;
        const maxPts = window.BroteMascot ? window.BroteMascot.MAX_POINTS : 80;
        if (!pts.valle && pts.jungla > 0 && pts.jungla < maxPts) {
          pts.valle = pts.jungla;
          delete pts.jungla;
        }
        parsed.lumi.biome = getCurrentProgressBiomeIdFromPoints(pts);
        parsed.lumi.valleStartMigrated = true;
      }
      if (typeof parsed.lumi.betaPointsOverride === 'undefined') {
        parsed.lumi.betaPointsOverride = parsed.lumi.betaStageOverride
          ? window.BroteMascot.getPointsForStagePreset(parsed.lumi.betaStageOverride)
          : null;
      }
      delete parsed.lumi.betaStageOverride;
      if (!parsed.notifSchedule || !parsed.notifSchedule.defaultWeek) {
        parsed.notifSchedule = {
          defaultWeek: createDefaultWeekSchedule(),
          weekOverrides: {}
        };
      } else {
        parsed.notifSchedule.defaultWeek = cloneWeekSchedule(parsed.notifSchedule.defaultWeek);
        if (!parsed.notifSchedule.weekOverrides || typeof parsed.notifSchedule.weekOverrides !== 'object') {
          parsed.notifSchedule.weekOverrides = {};
        } else {
          Object.keys(parsed.notifSchedule.weekOverrides).forEach(k => {
            parsed.notifSchedule.weekOverrides[k] = cloneWeekSchedule(parsed.notifSchedule.weekOverrides[k]);
          });
        }
      }
      if (!parsed.ui || typeof parsed.ui !== 'object') {
        parsed.ui = { biomesViewMode: 'grid', themeMode: 'system', betaMode: true, lang: 'es' };
      }
      if (!['grid', 'list', 'large'].includes(parsed.ui.biomesViewMode)) {
        parsed.ui.biomesViewMode = 'grid';
      }
      if (!['system', 'light', 'dark'].includes(parsed.ui.themeMode)) {
        parsed.ui.themeMode = 'system';
      }
      if (typeof parsed.ui.betaMode !== 'boolean') {
        parsed.ui.betaMode = true;
      }
      if (parsed.ui.lang !== 'en' && parsed.ui.lang !== 'es') {
        parsed.ui.lang = (window.SplashI18n && window.SplashI18n.getLang()) || 'es';
      }
      if (!parsed.ui.betaMode && parsed.lumi) {
        parsed.lumi.betaPointsOverride = null;
      }
      syncStateCatalogLanguage(parsed);
      if (parsed.beRealPing && !parsed.beRealPing.category) {
        parsed.beRealPing.category = 'RAPIDO';
      }
      if (!parsed.user || typeof parsed.user !== 'object') {
        parsed.user = { name: '' };
      }
      if (typeof parsed.user.name !== 'string') {
        parsed.user.name = '';
      }
      return parsed;
    } catch (_) {
      return createInitialState();
    }
  }

  function saveState() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch (_) {}
  }

  function getActiveBiomeId() {
    return (state && state.lumi && state.lumi.biome) || 'valle';
  }

  function isGoalCompatibleWithBiome(goal, biomeId) {
    return !goal.biomaExclusivo || goal.biomaExclusivo === biomeId;
  }

  function matchesCurrentHour(goal, hour) {
    if (!goal.franjaHoraria) return true;
    if (goal.franjaHoraria === 'manana') return hour >= 6 && hour <= 13;
    if (goal.franjaHoraria === 'dia') return hour >= 7 && hour <= 19;
    if (goal.franjaHoraria === 'noche') return hour >= 18 || hour <= 5;
    return true;
  }

  function pickWeightedGoals(category, count, excludeIds = []) {
    const activeBiome = getActiveBiomeId();
    const hour = new Date().getHours();
    const basePool = state.catalog.filter(
      g => g.categoria === category &&
        !g.estaOculto &&
        !excludeIds.includes(g.id) &&
        isGoalCompatibleWithBiome(g, activeBiome)
    );
    if (basePool.length === 0) return [];

    const timeMatchedPool = basePool.filter(g => matchesCurrentHour(g, hour));
    const pool = timeMatchedPool.length >= count ? timeMatchedPool : basePool;

    const weighted = [];
    pool.forEach(g => {
      let weight = g.esFavorito ? 3 : 1;
      if (g.esSecretoBioma && g.biomaExclusivo === activeBiome) {
        weight += 3;
      }
      if (g.franjaHoraria && matchesCurrentHour(g, hour)) {
        weight += 2;
      }
      for (let i = 0; i < weight; i++) weighted.push(g);
    });

    const chosen = [];
    const usedIds = new Set();
    let safety = 0;
    while (chosen.length < count && usedIds.size < pool.length && safety < 240) {
      safety++;
      const pick = weighted[Math.floor(Math.random() * weighted.length)];
      if (!usedIds.has(pick.id)) {
        usedIds.add(pick.id);
        chosen.push(pick.id);
      }
    }
    return chosen;
  }

  function refreshBiomeSuggestionsOnSwitch() {
    const doneR = getPeriodCompletedGoals('RAPIDO').map(g => g.id);
    const doneM = getPeriodCompletedGoals('MEDIO').map(g => g.id);
    state.suggestions.rapidos = pickWeightedGoals('RAPIDO', 2, doneR);
    state.suggestions.medios = pickWeightedGoals('MEDIO', 2, doneM);
    ensureBiomeSecretInSuggestions();
  }

  function ensureBiomeSecretInSuggestions() {
    const activeBiome = getActiveBiomeId();
    const secrets = state.catalog.filter(g => g.esSecretoBioma && g.biomaExclusivo === activeBiome && !g.estaOculto);

    const rSecret = secrets.find(g => g.categoria === 'RAPIDO' && !isGoalDoneToday(g.id));
    if (rSecret && !state.suggestions.rapidos.includes(rSecret.id)) {
      state.suggestions.rapidos = [rSecret.id, ...(state.suggestions.rapidos || []).filter(id => id !== rSecret.id)].slice(0, 2);
    }

    const mSecret = secrets.find(g => g.categoria === 'MEDIO' && !isGoalDoneToday(g.id));
    if (mSecret && !state.suggestions.medios.includes(mSecret.id)) {
      state.suggestions.medios = [mSecret.id, ...(state.suggestions.medios || []).filter(id => id !== mSecret.id)].slice(0, 2);
    }

    const lSecret = secrets.find(g => g.categoria === 'LARGO' && !isGoalDoneThisWeek(g.id));
    if (lSecret && !state.weekly.challengeIds.includes(lSecret.id)) {
      state.weekly.challengeIds = [lSecret.id, ...(state.weekly.challengeIds || []).filter(id => id !== lSecret.id)].slice(0, 2);
    }
  }

  function ensureWeeklyAndDaily() {
    const wk = getBroteWeekKey();
    if (state.weekly.weekKey !== wk || state.weekly.challengeIds.length !== 2) {
      if (state.weekly.weekKey !== wk) {
        state.weekly.weekKey = wk;
        state.weekly.challengeIds = pickWeightedGoals('LARGO', 2);
        state.weekly.rerollsLeft = 1;
      } else if (state.weekly.challengeIds.length > 2) {
        state.weekly.challengeIds = state.weekly.challengeIds.slice(0, 2);
      } else if (state.weekly.challengeIds.length < 2) {
        const extra = pickWeightedGoals('LARGO', 2 - state.weekly.challengeIds.length, state.weekly.challengeIds);
        state.weekly.challengeIds = state.weekly.challengeIds.concat(extra);
      }
    }
    if (!state.suggestions.rapidos.length) {
      state.suggestions.rapidos = pickWeightedGoals('RAPIDO', 2);
    }
    if (!state.suggestions.medios.length) {
      state.suggestions.medios = pickWeightedGoals('MEDIO', 2);
    }
    ensureBiomeSecretInSuggestions();
    saveState();
  }

  function getCumulativeStreak() {
    return Object.values(state.logsByDate).filter(d => d.completed && d.completed.length > 0).length;
  }

  function getConsecutiveStreakDays() {
    const today = new Date();
    const todayKey = getTodayKey(today);
    const hasToday = Boolean(state.logsByDate[todayKey] && state.logsByDate[todayKey].completed && state.logsByDate[todayKey].completed.length);
    let offset = hasToday ? 0 : 1;
    let streak = 0;
    for (let i = 0; i < 60; i++) {
      const d = new Date(today);
      d.setDate(today.getDate() - (offset + i));
      const k = getTodayKey(d);
      const day = state.logsByDate[k];
      if (day && Array.isArray(day.completed) && day.completed.length > 0) {
        streak++;
      } else {
        break;
      }
    }
    return streak;
  }

  function getDaysSinceLastActivity() {
    const todayIdx = dayIndexFromDateKey(getTodayKey());
    let latestIdx = null;
    Object.keys(state.logsByDate || {}).forEach(k => {
      const day = state.logsByDate[k];
      if (day && Array.isArray(day.completed) && day.completed.length > 0) {
        const idx = dayIndexFromDateKey(k);
        if (idx <= todayIdx && (latestIdx === null || idx > latestIdx)) {
          latestIdx = idx;
        }
      }
    });
    if (latestIdx === null) return 0;
    return Math.max(0, todayIdx - latestIdx);
  }

  function getLiveWeatherMode() {
    const todayLog = state.logsByDate[getTodayKey()];
    const todayCount = todayLog && Array.isArray(todayLog.completed) ? todayLog.completed.length : 0;
    if (todayCount >= 2 || getConsecutiveStreakDays() >= 3) {
      return 'radiant';
    }
    if (getDaysSinceLastActivity() >= 2) {
      return 'mist';
    }
    return 'clear';
  }

  function getWeatherLabel(weatherMode, timePhase) {
    if (weatherMode === 'radiant') return tx('Radiante', 'Radiant');
    if (weatherMode === 'mist') return tx('Neblina suave', 'Soft Mist');
    if (timePhase === 'night') return tx('Noche', 'Night');
    if (timePhase === 'sunset') return tx('Atardecer', 'Sunset');
    if (timePhase === 'dawn') return tx('Amanecer', 'Dawn');
    return tx('En calma', 'Calm');
  }

  function getLumiLevel() {
    return 1 + Math.floor((state.lumi.totalLongs || 0) / 3);
  }

  function getBiomePoints(biomeId) {
    const id = biomeId || (state.lumi && state.lumi.biome) || 'valle';
    const maxPts = window.BroteMascot ? window.BroteMascot.MAX_POINTS : 80;
    const raw = (state.lumi && state.lumi.biomePoints && state.lumi.biomePoints[id]) || 0;
    return Math.max(0, Math.min(maxPts, raw));
  }

  function isBiomeCompleted(biomeId) {
    const maxPts = window.BroteMascot ? window.BroteMascot.MAX_POINTS : 80;
    return getBiomePoints(biomeId) >= maxPts;
  }

  function getCurrentProgressBiomeId() {
    return getCurrentProgressBiomeIdFromPoints(state && state.lumi && state.lumi.biomePoints);
  }

  function isBiomeUnlocked(biomeId) {
    return isBiomeCompleted(biomeId) || biomeId === getCurrentProgressBiomeId();
  }

  function getUnlockedBiomesCount() {
    return getBiomeOrderIds().filter(id => isBiomeUnlocked(id)).length;
  }

  function getRealBiomePoints(biomeId) {
    return getBiomePoints(biomeId || (state.lumi && state.lumi.biome) || 'valle');
  }

  function getBiomeQuotaStatus() {
    const todayKey = getTodayKey();
    const todayIdx = dayIndexFromDateKey(todayKey);
    const curWk = getBroteWeekKey();
    let rapidoToday = 0;
    let lastMedioDayIdx = -9999;
    let largoThisWeek = 0;

    Object.keys(state.logsByDate).sort().forEach(dateKey => {
      const day = state.logsByDate[dateKey];
      if (!day || !Array.isArray(day.completed)) return;
      const dIdx = dayIndexFromDateKey(dateKey);
      day.completed.forEach(c => {
        if (c.categoria === 'RAPIDO' && dateKey === todayKey) {
          rapidoToday++;
        } else if (c.categoria === 'MEDIO') {
          if (dIdx - lastMedioDayIdx >= CAP_MEDIO_DAYS) {
            lastMedioDayIdx = dIdx;
          }
        } else if (c.categoria === 'LARGO' && getBroteWeekKey(parseLogEntryDate(dateKey, c)) === curWk) {
          largoThisWeek++;
        }
      });
    });

    return {
      rapidoToday: Math.min(CAP_RAPIDO_DAILY, rapidoToday),
      rapidoCap: CAP_RAPIDO_DAILY,
      medioAvailableToday: (todayIdx - lastMedioDayIdx) >= CAP_MEDIO_DAYS,
      largoThisWeek: Math.min(CAP_LARGO_WEEKLY, largoThisWeek),
      largoCap: CAP_LARGO_WEEKLY
    };
  }

  function getEffectiveBiomePoints() {
    return state.lumi.betaPointsOverride !== null && typeof state.lumi.betaPointsOverride === 'number'
      ? state.lumi.betaPointsOverride
      : getRealBiomePoints();
  }

  function getEffectiveBiomeStage() {
    return window.BroteMascot.getLevelFromPoints(getEffectiveBiomePoints());
  }

  function isGoalDoneToday(goalId) {
    const today = getTodayKey();
    const day = state.logsByDate[today];
    if (!day || !day.completed) return false;
    return day.completed.some(item => item.id === goalId);
  }

  function isGoalDoneThisWeek(goalId) {
    const wk = getBroteWeekKey();
    return Object.entries(state.logsByDate).some(([dateKey, day]) => {
      if (!day.completed) return false;
      return day.completed.some(c => c.id === goalId && getBroteWeekKey(parseLogEntryDate(dateKey, c)) === wk);
    });
  }

  function completeGoal(goal, options = {}) {
    const today = getTodayKey();
    const q = getBiomeQuotaStatus();
    const rawGain =
      goal.categoria === 'RAPIDO' ? (q.rapidoToday < q.rapidoCap ? BIOME_PTS.RAPIDO : 0) :
      goal.categoria === 'MEDIO' ? (q.medioAvailableToday ? BIOME_PTS.MEDIO : 0) :
      goal.categoria === 'LARGO' ? (q.largoThisWeek < q.largoCap ? BIOME_PTS.LARGO : 0) : 0;

    if (!state.logsByDate[today]) {
      state.logsByDate[today] = { completed: [], photo: null };
    }

    state.logsByDate[today].completed.push({
      id: goal.id,
      titulo: goal.titulo,
      categoria: goal.categoria,
      icono: goal.icono,
      biome: getActiveBiomeId(),
      timestamp: new Date().toISOString()
    });

    if (!state.lumi.biomePoints || typeof state.lumi.biomePoints !== 'object') {
      state.lumi.biomePoints = {};
    }

    const maxPts = window.BroteMascot ? window.BroteMascot.MAX_POINTS : 80;
    const activeId = state.lumi.biome || 'valle';
    const targetId = getCurrentProgressBiomeId();

    const prevTargetPts = getBiomePoints(targetId);
    const newTargetPts = Math.min(maxPts, prevTargetPts + rawGain);
    state.lumi.biomePoints[targetId] = newTargetPts;
    const ptsGain = Math.max(0, newTargetPts - prevTargetPts);
    const justMaxedTarget = prevTargetPts < maxPts && newTargetPts >= maxPts;
    let unlockedNextId = null;

    if (justMaxedTarget) {
      const nextId = getCurrentProgressBiomeId();
      if (nextId && nextId !== targetId) {
        state.lumi.biome = nextId;
        unlockedNextId = nextId;
      }
    }

    state.lumi.betaPointsOverride = null;
    if (goal.categoria && typeof showExtraInBlock[goal.categoria] === 'boolean') {
      showExtraInBlock[goal.categoria] = false;
    }

    if (goal.categoria === 'RAPIDO') {
      state.lumi.lastAwakeDate = today;
      window.BroteMascot.spawnParticles(22, ['#E5A03D', '#F9E5C4', '#68A87E']);
    } else if (goal.categoria === 'MEDIO') {
      state.lumi.lastAwakeDate = today;
      state.lumi.gardenItems = Math.min(7, (state.lumi.gardenItems || 0) + 1);
      window.BroteMascot.spawnParticles(28, ['#4E909D', '#68A87E', '#E5A03D']);
    } else if (goal.categoria === 'LARGO') {
      state.lumi.lastAwakeDate = today;
      state.lumi.totalLongs = (state.lumi.totalLongs || 0) + 1;
      window.BroteMascot.spawnParticles(36, ['#D16C4C', '#E5A03D', '#68A87E']);
    }

    if (justMaxedTarget) {
      window.BroteMascot.spawnParticles(45, ['#FFD573', '#1F7A4C', '#E08600']);
    }

    if (navigator.vibrate) {
      navigator.vibrate([18, 30, 22]);
    }
    window.BroteAudio.playBiomeChime(activeId);

    saveState();
    renderAll(true);

    if (!options.silentToast) {
      const showPhotoBtn = goal.categoria === 'RAPIDO' || options.allowPhoto;
      let msg;
      if (justMaxedTarget && unlockedNextId) {
        const nb = window.BroteMascot.getBiome(unlockedNextId);
        msg = tx(
          `¡Bioma completado (Nv. 8)! Pasas a ${nb.name} y se despejan las nubes.`,
          `Biome completed (Lvl. 8)! Moving to ${nb.name} — clouds cleared.`
        );
      } else if (justMaxedTarget) {
        msg = tx(
          '¡Bioma completado al nivel máximo (Nv. 8)! Ya has desbloqueado todos.',
          'Biome completed at max level (Lvl. 8)! All biomes unlocked.'
        );
      } else if (ptsGain > 0 && targetId !== activeId) {
        const tb = window.BroteMascot.getBiome(targetId);
        msg = tx(
          `Hecho (+${ptsGain} pts para ${tb.shortName}, tu bioma en progreso).`,
          `Done (+${ptsGain} pts for ${tb.shortName}, your active progression biome).`
        );
      } else if (ptsGain > 0) {
        msg = tx(
          `Hecho (+${ptsGain} al bioma). Ya puedes bloquear el móvil y volver a tu día.`,
          `Done (+${ptsGain} to biome). You can lock your phone and get back to your day.`
        );
      } else {
        msg = tx(
          'Hecho · Guardado en tu Muro (límite del bioma cubierto en este periodo).',
          'Done · Saved to your Wall (biome point cap reached for this period).'
        );
      }
      showToast('check_circle', msg, showPhotoBtn);
    }
  }

  function renderUserProfile() {
    const name = ((state.user && state.user.name) || '').trim();
    const greetingEl = document.getElementById('settings-user-greeting');
    const avatarEl = document.getElementById('settings-user-avatar');
    const inputEl = document.getElementById('user-name-input');
    const muroSubEl = document.getElementById('muro-subtitle');

    if (greetingEl) {
      greetingEl.textContent = name ? tx(`Hola, ${name}`, `Hi, ${name}`) : tx('Tu perfil', 'Your profile');
    }
    if (avatarEl) {
      if (name) {
        avatarEl.textContent = name[0].toUpperCase();
      } else {
        avatarEl.innerHTML = iconHTML('person');
      }
    }
    if (inputEl && document.activeElement !== inputEl) {
      inputEl.value = name;
    }
    if (muroSubEl) {
      muroSubEl.textContent = name
        ? tx(
            `Tu calendario privado, ${name}. Sin comparaciones, solo tus momentos.`,
            `Your private calendar, ${name}. No comparisons, just your moments.`
          )
        : tx(
            'Tu calendario privado de vida fuera de la pantalla. Sin comparaciones, solo tus momentos.',
            'Your private calendar of life off-screen. No comparisons, just your moments.'
          );
    }
  }

  function isBetaModeActive() {
    return !state.ui || state.ui.betaMode !== false;
  }

  function applyBetaMode() {
    const isBeta = isBetaModeActive();
    document.body.classList.toggle('is-beta-off', !isBeta);
    if (!isBeta && state.lumi) {
      state.lumi.betaPointsOverride = null;
    }
    const betaBtn = document.getElementById('btn-toggle-beta');
    if (betaBtn) {
      betaBtn.classList.toggle('is-on', isBeta);
      betaBtn.setAttribute('aria-checked', isBeta ? 'true' : 'false');
    }
    const betaDesc = document.getElementById('beta-mode-desc');
    if (betaDesc) {
      betaDesc.textContent = isBeta
        ? tx('Activo · Prueba niveles del bioma y simulador', 'Active · Preview biome stages & simulator tools')
        : tx('Desactivado · Solo progresión real', 'Off · Real progression only');
    }
  }

  const systemThemeMQ = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;

  function applyThemeMode() {
    const mode = (state.ui && state.ui.themeMode) || 'system';
    const isDark = mode === 'dark' || (mode === 'system' && Boolean(systemThemeMQ && systemThemeMQ.matches));
    const resolved = isDark ? 'dark' : 'light';
    document.documentElement.dataset.theme = resolved;
    document.documentElement.style.colorScheme = resolved;
    const themeMeta = document.querySelector('meta[name="theme-color"]');
    if (themeMeta) {
      themeMeta.setAttribute('content', resolved === 'dark' ? '#000000' : '#FAF8F3');
    }
    try {
      if (window.SplashNative && typeof window.SplashNative.setSystemBarsTheme === 'function') {
        window.SplashNative.setSystemBarsTheme(resolved === 'dark', resolved === 'dark' ? '#000000' : '#FAF8F3');
      }
    } catch (_) {}
    document.querySelectorAll('#theme-mode-tabs .theme-mode-btn').forEach(btn => {
      const isMatch = btn.dataset.themeMode === mode;
      btn.classList.toggle('active', isMatch);
      btn.setAttribute('aria-checked', isMatch ? 'true' : 'false');
    });
  }

  let catalogDirty = true;

  function renderAll(celebrate = false) {
    applyThemeMode();
    applyBetaMode();
    renderHeader();
    renderUserProfile();
    renderMascot(celebrate);
    renderBeRealCard();
    renderTimeBlocks();
    renderCalendar();
    renderMuroSocialViews();
    const catalogTab = document.getElementById('tab-catalogo');
    if (catalogTab && catalogTab.classList.contains('active')) {
      renderCatalog();
    } else {
      catalogDirty = true;
    }
    const biomesTab = document.getElementById('tab-biomas');
    if (biomesTab && biomesTab.classList.contains('active')) {
      renderBiomesStack();
    }
    if (widgetStudio.open) {
      renderWidgetStudio();
    }
    syncAndroidWidget();
    syncAndroidNotificationSchedule();
  }

  function sendNativeNotification(title, body) {
    try {
      if (window.SplashNative && typeof window.SplashNative.sendNotification === 'function') {
        window.SplashNative.sendNotification(String(title || 'Splash'), String(body || ''));
        return;
      }
      if ('Notification' in window && Notification.permission === 'granted') {
        new Notification(String(title || 'Splash'), { body: String(body || '') });
      }
    } catch (_) {}
  }

  let widgetBiomePngCacheKey = '';
  let widgetBiomePngBase64 = '';
  let widgetOffscreenCanvas = null;

  function buildWidgetBiomePngBase64(activeId, pts, resolvedPhase, weatherMode) {
    const key = `${activeId}|${pts}|${resolvedPhase}|${weatherMode}`;
    if (key === widgetBiomePngCacheKey && widgetBiomePngBase64) {
      return widgetBiomePngBase64;
    }
    try {
      if (!window.BroteMascot || typeof window.BroteMascot.renderToCanvas !== 'function') return '';
      if (!widgetOffscreenCanvas) {
        widgetOffscreenCanvas = document.createElement('canvas');
      }
      const off = widgetOffscreenCanvas;
      off.width = 260;
      off.height = 220;
      window.BroteMascot.renderToCanvas(off, {
        biome: activeId,
        points: pts,
        isAwake: resolvedPhase !== 'night',
        timePhase: resolvedPhase,
        weatherMode,
        yaw: Math.PI / 4,
        pitch: 0.60,
        zoom: 1.18,
        maxDpr: 1,
        width: 260,
        height: 220
      });
      const dataUrl = off.toDataURL('image/png');
      const idx = dataUrl.indexOf('base64,');
      widgetBiomePngBase64 = idx !== -1 ? dataUrl.slice(idx + 7) : '';
      widgetBiomePngCacheKey = key;
      return widgetBiomePngBase64;
    } catch (_) {
      return '';
    }
  }

  function syncAndroidWidget() {
    try {
      if (!window.SplashNative) return;
      const isEnNow = Boolean(window.SplashI18n && window.SplashI18n.isEn());
      const activeId = getActiveBiomeId();
      const bMeta = window.BroteMascot ? window.BroteMascot.getBiome(activeId) : { name: 'Valle Alpino', shortName: 'Valle' };
      const pts = getEffectiveBiomePoints();
      const lvl = window.BroteMascot ? window.BroteMascot.getLevelFromPoints(pts) : 1;
      const streak = getCumulativeStreak();
      const weatherMode = getLiveWeatherMode();
      const resolvedPhase = widgetStudio.timeMode === 'auto'
        ? (window.BroteMascot ? window.BroteMascot.getTimeOfDayPhase() : 'day')
        : widgetStudio.timeMode;
      const widgetIsDark =
        widgetStudio.themeMode === 'dark' ||
        (widgetStudio.themeMode === 'auto' && resolvedPhase === 'night');

      const rId = (state.suggestions && state.suggestions.rapidos && state.suggestions.rapidos[0]) ||
        (state.dailySuggestions && state.dailySuggestions.RAPIDO && state.dailySuggestions.RAPIDO[0]);
      const mId = (state.suggestions && state.suggestions.medios && state.suggestions.medios[0]);
      const g1 = state.catalog.find(g => g.id === rId) || state.catalog.find(g => g.categoria === 'RAPIDO') || state.catalog[0];
      const g2 = state.catalog.find(g => g.id === mId) || state.catalog.find(g => g.categoria === 'MEDIO') || state.catalog[1];
      const goal2Title = g1 ? getShortTitle(g1) : tx('Respirar aire fresco', 'Breathe fresh air');
      const goal15Title = g2 ? getShortTitle(g2) : tx('Caminar sin móvil', 'Phone-free walk');

      if (typeof window.SplashNative.updateWidgetRich === 'function') {
        const pngB64 = buildWidgetBiomePngBase64(activeId, pts, resolvedPhase, weatherMode);
        const payload = {
          biomeName: bMeta.name || 'Valle Alpino',
          biomeShort: bMeta.shortName || bMeta.name || 'Valle',
          levelLabel: `${tx('Nv.', 'Lvl.')} ${lvl}`,
          streakLabel: `${streak} ${streak === 1 ? tx('día', 'day') : tx('días', 'days')}`,
          goal2Title,
          goal15Title,
          weatherLabel: typeof getWeatherLabel === 'function' ? getWeatherLabel(weatherMode, resolvedPhase) : 'Radiante',
          isDark: widgetIsDark,
          themeMode: widgetStudio.themeMode || 'auto',
          isEn: isEnNow,
          biomePngBase64: pngB64
        };
        window.SplashNative.updateWidgetRich(JSON.stringify(payload));
      } else if (typeof window.SplashNative.updateWidget === 'function') {
        window.SplashNative.updateWidget(
          bMeta.name || 'Splash',
          `${tx('Nv.', 'Lvl.')} ${lvl}`,
          `${streak} ${streak === 1 ? tx('día', 'day') : tx('días', 'days')}`,
          goal2Title
        );
      }
    } catch (_) {}
  }

  function syncAndroidNotificationSchedule() {
    try {
      if (!window.SplashNative || typeof window.SplashNative.syncNotificationSchedule !== 'function') return;
      const isEnNow = Boolean(window.SplashI18n && window.SplashI18n.isEn());
      const activeId = getActiveBiomeId();
      const notifMeta = BIOME_NOTIFS[activeId] || BIOME_NOTIFS.valle;
      const sched = getEffectiveScheduleForWeek(getBroteWeekKey());

      function buildWeightedPool(category, limit) {
        const pool = [];
        state.catalog.forEach(g => {
          if (g.categoria !== category || g.estaOculto) return;
          if (g.biomaExclusivo && !isBiomeUnlocked(g.biomaExclusivo)) return;
          let w = 1;
          if (g.esFavorito) w += 2;
          if (g.biomaExclusivo === activeId) w += 1;
          for (let i = 0; i < w && pool.length < limit; i++) {
            pool.push(g.titulo);
          }
        });
        return pool;
      }

      const payload = {
        schedule: sched,
        quickPool: buildWeightedPool('RAPIDO', 32),
        medioPool: buildWeightedPool('MEDIO', 20),
        quickBadge: notifMeta.quickBadge,
        medioBadge: notifMeta.medioBadge,
        lang: isEnNow ? 'en' : 'es'
      };
      window.SplashNative.syncNotificationSchedule(JSON.stringify(payload));
    } catch (_) {}
  }

  function renderHeader() {
    const streak = getCumulativeStreak();
    const unlocked = getUnlockedBiomesCount();
    const total = getBiomeOrderIds().length;
    document.getElementById('streak-days').textContent = `${streak} ${streak === 1 ? tx('día', 'day') : tx('días', 'days')}`;
    document.getElementById('lumi-level-pill').textContent = `${unlocked}/${total}`;
    const levelPill = document.getElementById('header-level');
    if (levelPill) {
      levelPill.setAttribute('title', tx(`Biomas desbloqueados: ${unlocked} de ${total}`, `Unlocked biomes: ${unlocked} of ${total}`));
    }
  }

  function setOledSystemBars(enabled) {
    try {
      if (enabled) {
        const themeMeta = document.querySelector('meta[name="theme-color"]');
        if (themeMeta) themeMeta.setAttribute('content', '#000000');
        if (window.SplashNative && typeof window.SplashNative.setSystemBarsTheme === 'function') {
          window.SplashNative.setSystemBarsTheme(true, '#000000');
        }
      } else {
        applyThemeMode();
      }
    } catch (_) {}
  }

  let lastDioramaRenderKey = '';

  function renderMascot(celebrate = false, forceRedraw = false) {
    const points = getEffectiveBiomePoints();
    const stage = window.BroteMascot.getLevelFromPoints(points);
    const sub = window.BroteMascot.getSubProgress(points);
    const isAwake = state.lumi.lastAwakeDate === getTodayKey();
    const xpPct = points >= window.BroteMascot.MAX_POINTS ? 100 : Math.max(8, Math.round((sub / 10) * 100));
    const biomeId = state.lumi.biome || 'valle';
    const stageEl = document.getElementById('mascot-stage');
    if (stageEl) stageEl.dataset.biome = biomeId;
    const biome = window.BroteMascot.getBiome(biomeId);
    const weatherMode = getLiveWeatherMode();
    const timePhase = window.BroteMascot.getTimeOfDayPhase();
    const theme = document.documentElement.dataset.theme || 'light';

    const dioramaKey = `${biomeId}|${points}|${isAwake ? 1 : 0}|${weatherMode}|${timePhase}|${theme}`;
    if (forceRedraw || celebrate || dioramaKey !== lastDioramaRenderKey) {
      lastDioramaRenderKey = dioramaKey;
      window.BroteMascot.renderDiorama({
        biome: biomeId,
        points,
        isAwake,
        weatherMode,
        timePhase
      });
    }

    const wrap = document.getElementById('mascot-diorama-wrap');
    if (celebrate && wrap) {
      wrap.classList.remove('is-celebrating');
      void wrap.offsetWidth;
      wrap.classList.add('is-celebrating');
      setTimeout(() => {
        wrap.classList.remove('is-celebrating');
      }, 600);
    }

    document.getElementById('mascot-stage-name').textContent = biome.name;
    const isOverride = isBetaModeActive() && state.lumi.betaPointsOverride !== null && typeof state.lumi.betaPointsOverride === 'number';
    const progressPart = points >= window.BroteMascot.MAX_POINTS ? tx('Máx', 'Max') : `${sub}/10`;
    const weatherLabel = getWeatherLabel(weatherMode, timePhase);
    document.getElementById('mascot-mood-tag').textContent =
      `${tx('Nv.', 'Lvl.')} ${stage} · ${progressPart} · ${weatherLabel}${isOverride ? ' · Beta' : ''}`;
    document.getElementById('mascot-xp-fill').style.width = `${xpPct}%`;
  }

  function renderBeRealCard() {
    const card = document.getElementById('bereal-active-card');
    const badge = document.getElementById('bell-badge');
    if (!state.beRealPing.active || Date.now() > state.beRealPing.expiresAt) {
      state.beRealPing.active = false;
      card.classList.add('hidden');
      badge.classList.add('hidden');
      return;
    }

    const isMedio = state.beRealPing.category === 'MEDIO';
    const goal = state.catalog.find(g => g.id === state.beRealPing.goalId) || state.catalog[0];
    const activeBiome = getActiveBiomeId();
    const notifMeta = BIOME_NOTIFS[activeBiome] || BIOME_NOTIFS.valle;
    document.getElementById('bereal-prompt-text').textContent = goal.titulo;
    card.classList.toggle('is-medio', isMedio);
    document.getElementById('bereal-badge-icon').textContent = isMedio ? 'schedule' : 'bolt';
    document.getElementById('bereal-badge-text').textContent = isMedio
      ? `${notifMeta.medioBadge} · 4h`
      : `${notifMeta.quickBadge} · 20m`;
    card.classList.remove('hidden');
    badge.classList.remove('hidden');
    updateBeRealCountdown();
  }

  function updateBeRealCountdown() {
    if (!state.beRealPing.active) return;
    const diff = Math.max(0, Math.floor((state.beRealPing.expiresAt - Date.now()) / 1000));
    const h = Math.floor(diff / 3600);
    const m = String(Math.floor((diff % 3600) / 60)).padStart(2, '0');
    const s = String(diff % 60).padStart(2, '0');
    document.getElementById('bereal-countdown').textContent = h > 0 ? `${h}:${m}:${s}` : `${m}:${s}`;
    if (diff === 0) {
      state.beRealPing.active = false;
      saveState();
      renderBeRealCard();
    }
  }

  function getShortTitle(goal) {
    return goal.tituloCorto || goal.titulo;
  }

  function getPeriodCompletedGoals(category) {
    const seen = new Map();
    if (category === 'LARGO') {
      const wk = getBroteWeekKey();
      Object.entries(state.logsByDate).forEach(([dateKey, day]) => {
        if (!day || !Array.isArray(day.completed)) return;
        day.completed.forEach(c => {
          if (c.categoria === 'LARGO' && getBroteWeekKey(parseLogEntryDate(dateKey, c)) === wk) {
            const g = state.catalog.find(item => item.id === c.id) || {
              id: c.id,
              titulo: c.titulo,
              tituloCorto: c.titulo,
              subcategoria: tx('Semanal', 'Weekly'),
              categoria: 'LARGO'
            };
            seen.set(c.id, g);
          }
        });
      });
    } else {
      const todayDay = state.logsByDate[getTodayKey()];
      if (todayDay && Array.isArray(todayDay.completed)) {
        todayDay.completed.forEach(c => {
          if (c.categoria === category) {
            const g = state.catalog.find(item => item.id === c.id) || {
              id: c.id,
              titulo: c.titulo,
              tituloCorto: c.titulo,
              subcategoria: category === 'MEDIO' ? tx('Enfoque', 'Focus') : tx('Rápido', 'Quick'),
              categoria: category
            };
            seen.set(c.id, g);
          }
        });
      }
    }
    return Array.from(seen.values());
  }

  function renderTimeBlocks() {
    const listEl = document.getElementById('active-home-list');
    const captionEl = document.getElementById('active-block-caption');
    const refreshBtn = document.getElementById('btn-block-refresh');
    const rerollBtn = document.getElementById('btn-weekly-reroll');
    const drawerWrap = document.getElementById('completed-drawer-wrap');
    const toggleCompletedBtn = document.getElementById('btn-toggle-completed');
    const completedLabelEl = document.getElementById('completed-toggle-label');
    const completedListEl = document.getElementById('completed-home-list');

    const q = getBiomeQuotaStatus();
    listEl.innerHTML = '';
    listEl.classList.remove('has-expanded', 'single-card');
    listEl.classList.add('is-bento');

    document.querySelectorAll('.time-seg').forEach(seg => {
      const blk = seg.dataset.block;
      const doneBlk =
        (blk === 'RAPIDO' && q.rapidoToday >= q.rapidoCap) ||
        (blk === 'MEDIO' && !q.medioAvailableToday) ||
        (blk === 'LARGO' && q.largoThisWeek >= q.largoCap);
      seg.classList.toggle('is-block-done', doneBlk);
    });

    const isWeekly = activeHomeBlock === 'LARGO';
    const isQuotaDone =
      (activeHomeBlock === 'RAPIDO' && q.rapidoToday >= q.rapidoCap) ||
      (activeHomeBlock === 'MEDIO' && !q.medioAvailableToday) ||
      (activeHomeBlock === 'LARGO' && q.largoThisWeek >= q.largoCap);

    let rawIds = [];
    let actionFn = () => tx('Completar', 'Complete');

    if (activeHomeBlock === 'RAPIDO') {
      captionEl.textContent = q.rapidoToday < q.rapidoCap
        ? tx(`2 min · Bioma hoy: ${q.rapidoToday}/${q.rapidoCap} (+1 pt)`, `2 min · Biome today: ${q.rapidoToday}/${q.rapidoCap} (+1 pt)`)
        : tx('2 min · Objetivo diario al día', '2 min · Daily goal complete');
      refreshBtn.classList.remove('hidden');
      rerollBtn.classList.add('hidden');
      rawIds = state.suggestions.rapidos;
      actionFn = () => tx('Completar', 'Complete');
    } else if (activeHomeBlock === 'MEDIO') {
      captionEl.textContent = q.medioAvailableToday
        ? tx('15 min · Disponible (+4 pts · cada 2 días)', '15 min · Available (+4 pts · every 2 days)')
        : tx('15 min · Bloque cubierto (48h)', '15 min · Block covered (48h)');
      refreshBtn.classList.remove('hidden');
      rerollBtn.classList.add('hidden');
      rawIds = state.suggestions.medios;
      actionFn = (g) => (g.tipoVerificacion === 'SOCIAL' ? tx('Mensaje', 'Message') : '15 min');
    } else {
      captionEl.textContent = tx(
        `2 semanales · Lunes 04:00 · Bioma: ${q.largoThisWeek}/${q.largoCap} (+8 pts)`,
        `2 weekly · Mon 04:00 · Biome: ${q.largoThisWeek}/${q.largoCap} (+8 pts)`
      );
      refreshBtn.classList.add('hidden');
      rerollBtn.classList.remove('hidden');
      rawIds = state.weekly.challengeIds.slice(0, 2);
      actionFn = () => tx('Completar', 'Complete');
    }

    const doneFn = (id) => (isWeekly ? isGoalDoneThisWeek(id) : isGoalDoneToday(id));
    const pendingGoals = rawIds
      .map(id => state.catalog.find(item => item.id === id))
      .filter(g => g && !doneFn(g.id));
    const completedGoals = getPeriodCompletedGoals(activeHomeBlock);

    if ((isQuotaDone && !showExtraInBlock[activeHomeBlock]) || pendingGoals.length === 0) {
      const doneTitle =
        activeHomeBlock === 'RAPIDO' ? tx('Objetivos Flash Completados', 'Quick Goals Completed') :
        activeHomeBlock === 'MEDIO' ? tx('Sesión media completada', 'Medium Session Completed') :
        tx('Retos semanales completados', 'Weekly Challenges Completed');
      const doneSub =
        activeHomeBlock === 'RAPIDO' ? tx('Los Objetivos extras no mejoran el Bioma.', 'Extra quick goals do not upgrade the biome.') :
        activeHomeBlock === 'MEDIO' ? tx('Cupo medio cubierto (48h). Puedes hacer otro extra solo para tu Muro Real.', 'Medium quota met (48h). You can do an extra one for your Real Wall.') :
        tx('Has completado tus retos semanales. El lunes a las 04:00 llegan nuevos.', 'You finished your weekly challenges. Fresh ones arrive Monday at 04:00.');

      const doneCard = document.createElement('div');
      doneCard.className = 'block-done-card';
      doneCard.innerHTML = `
        <div class="block-done-icon">${iconHTML('check_circle', 'icon-sm')}</div>
        <div class="block-done-title"><strong>${doneTitle}</strong></div>
        <p class="block-done-sub">${doneSub}</p>
        <button type="button" class="block-extra-btn">${tx('Hacer uno extra', 'Do an extra one')}</button>
      `;
      doneCard.querySelector('.block-extra-btn').addEventListener('click', () => {
        showExtraInBlock[activeHomeBlock] = true;
        const doneIds = completedGoals.map(g => g.id);
        if (activeHomeBlock === 'RAPIDO') {
          const fresh = pickWeightedGoals('RAPIDO', 2, doneIds);
          state.suggestions.rapidos = fresh.length ? fresh : pickWeightedGoals('RAPIDO', 2);
        } else if (activeHomeBlock === 'MEDIO') {
          const fresh = pickWeightedGoals('MEDIO', 2, doneIds);
          state.suggestions.medios = fresh.length ? fresh : pickWeightedGoals('MEDIO', 2);
        } else {
          const fresh = pickWeightedGoals('LARGO', 2, doneIds);
          if (fresh.length) state.weekly.challengeIds = fresh;
        }
        saveState();
        renderTimeBlocks();
      });
      listEl.appendChild(doneCard);
    } else {
      listEl.classList.toggle('single-card', pendingGoals.length === 1);
      pendingGoals.forEach(g => {
        listEl.appendChild(createGoalCard(g, false, actionFn(g)));
      });
    }

    if (drawerWrap && completedListEl) {
      if (completedGoals.length > 0) {
        drawerWrap.classList.remove('hidden');
        completedLabelEl.textContent = `${completedGoals.length} ${completedGoals.length === 1 ? tx('completado', 'completed') : tx('completados', 'completed')}`;
        toggleCompletedBtn.classList.toggle('is-open', showCompletedDrawer);
        completedListEl.classList.toggle('hidden', !showCompletedDrawer);
        completedListEl.innerHTML = '';
        if (showCompletedDrawer) {
          completedGoals.forEach(g => {
            completedListEl.appendChild(createGoalCard(g, true, actionFn(g)));
          });
        }
      } else {
        drawerWrap.classList.add('hidden');
        completedListEl.innerHTML = '';
      }
    }

    document.getElementById('reroll-count').textContent = state.weekly.rerollsLeft;
    rerollBtn.disabled = state.weekly.rerollsLeft <= 0;
  }

  function createGoalCard(goal, isDone, actionLabel) {
    const catKey = (goal.categoria || 'RAPIDO').toLowerCase();
    const catClass = `cat-${catKey}`;
    const catLabel = goal.categoria === 'MEDIO' ? tx('Medio', 'Medium') : goal.categoria === 'LARGO' ? tx('Largo', 'Long') : tx('Rápido', 'Quick');
    const catIcon =
      goal.categoria === 'MEDIO' ? 'schedule' :
      goal.categoria === 'LARGO' ? 'local_fire_department' : 'bolt';
    const isBiomeSecret = Boolean(goal.esSecretoBioma && goal.biomaExclusivo);
    const secretClass = isBiomeSecret ? 'is-biome-secret' : '';
    const card = document.createElement('div');
    card.className = `goal-card ${catClass} ${secretClass} ${isDone ? 'is-done' : ''}`;
    card.setAttribute('role', 'button');
    card.setAttribute('tabindex', '0');

    if (isBiomeSecret) {
      const biomeTagMeta = window.BroteMascot.getBiome(goal.biomaExclusivo);
      card.dataset.biome = goal.biomaExclusivo;
      card.innerHTML = `
        <md-ripple></md-ripple>
        <div class="biome-card-strata" aria-hidden="true"><span></span><span></span><span></span></div>
        <div class="goal-body">
          <div class="goal-top-row">
            <span class="biome-card-origin">${biomeTagMeta.shortName}</span>
            <div class="biome-card-rightchips">
              <span class="biome-card-cat-chip ${catClass}"><span class="biome-cat-dot"></span>${catLabel}</span>
              <span class="material-symbols-rounded goal-expand-hint">expand_more</span>
            </div>
          </div>
          <span class="goal-short-title">${getShortTitle(goal)}</span>
          <span class="goal-full-desc">${goal.titulo}</span>
          ${goal.esFavorito ? `<span class="goal-meta"><span class="goal-fav-tag">${tx('Favorito', 'Favorite')}</span></span>` : ''}
        </div>
        <button type="button" class="goal-action-pill">
          ${isDone ? `${iconHTML('check', 'icon-xs')} ${tx('Listo', 'Done')}` : actionLabel}
        </button>
      `;
    } else {
      card.innerHTML = `
        <md-ripple></md-ripple>
        <span class="material-symbols-rounded goal-bg-glyph" aria-hidden="true">${catIcon}</span>
        <div class="goal-body">
          <div class="goal-top-row">
            <span class="goal-subcat-tag">${goal.subcategoria}</span>
            <span class="material-symbols-rounded goal-expand-hint">expand_more</span>
          </div>
          <span class="goal-short-title">${getShortTitle(goal)}</span>
          <span class="goal-full-desc">${goal.titulo}</span>
          ${goal.esFavorito ? `<span class="goal-meta"><span class="goal-fav-tag">${tx('Favorito', 'Favorite')}</span></span>` : ''}
        </div>
        <button type="button" class="goal-action-pill">
          ${isDone ? `${iconHTML('check', 'icon-xs')} ${tx('Listo', 'Done')}` : actionLabel}
        </button>
      `;
    }

    const actionBtn = card.querySelector('.goal-action-pill');
    actionBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      handleGoalTap(goal);
    });

    function toggleExpandCard() {
      const parentList = card.parentElement;
      const willExpand = !card.classList.contains('is-expanded');
      if (parentList) {
        parentList.querySelectorAll('.goal-card.is-expanded').forEach(el => {
          el.classList.remove('is-expanded');
        });
      }
      if (willExpand) {
        card.classList.add('is-expanded');
      }
      if (parentList) {
        parentList.classList.toggle('has-expanded', Boolean(parentList.querySelector('.goal-card.is-expanded')));
      }
    }

    card.addEventListener('click', toggleExpandCard);
    card.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        toggleExpandCard();
      }
    });

    return card;
  }

  function handleGoalTap(goal) {
    if (goal.categoria === 'RAPIDO') {
      completeGoal(goal, { allowPhoto: true });
    } else if (goal.categoria === 'MEDIO') {
      openTimerModal(goal);
    } else {
      openAnalogModal(goal);
    }
  }

  // --- MODAL TEMPORIZADOR (MEDIOS & SOCIAL) ---
  function renderTimerSoundControls() {
    const wrap = document.getElementById('ambient-controls');
    if (!wrap) return;
    const activeBiome = getActiveBiomeId();
    const unlockedIds = getBiomeOrderIds().filter(id => isBiomeUnlocked(id));
    const orderedIds = [activeBiome, ...unlockedIds.filter(id => id !== activeBiome)].slice(0, 4);
    const currentMode = window.BroteAudio.getMode();

    wrap.innerHTML = `<span class="ambient-label">${tx('Sonido del bioma', 'Biome sound')}</span>`;
    orderedIds.forEach(id => {
      const meta = window.BroteAudio.getBiomeSoundMeta(id);
      const isActive = currentMode === `biome:${id}`;
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = `chip-btn ${isActive ? 'active' : ''}`;
      btn.innerHTML = `${iconHTML(meta.icon, 'icon-xs')}<span>${meta.label}</span>`;
      btn.addEventListener('click', () => {
        if (window.BroteAudio.getMode() === `biome:${id}`) {
          window.BroteAudio.stopAmbient();
        } else {
          window.BroteAudio.startBiomeAmbient(id);
        }
        renderTimerSoundControls();
      });
      wrap.appendChild(btn);
    });

    const whiteBtn = document.createElement('button');
    whiteBtn.type = 'button';
    whiteBtn.className = `chip-btn ${currentMode === 'white' ? 'active' : ''}`;
    whiteBtn.innerHTML = `${iconHTML('water', 'icon-xs')}<span>${tx('Ruido cálido', 'Warm noise')}</span>`;
    whiteBtn.addEventListener('click', () => {
      if (window.BroteAudio.getMode() === 'white') {
        window.BroteAudio.stopAmbient();
      } else {
        window.BroteAudio.startAmbient('white');
      }
      renderTimerSoundControls();
    });
    wrap.appendChild(whiteBtn);
  }

  function openTimerModal(goal) {
    activeTimer.goal = goal;
    activeTimer.totalSeconds = 15 * 60;
    activeTimer.remainingSeconds = 15 * 60;
    activeTimer.running = false;
    activeTimer.leftForSocial = false;
    clearInterval(activeTimer.intervalId);

    document.getElementById('timer-title').textContent = goal.titulo;
    const socialBox = document.getElementById('social-deeplink-box');
    if (goal.tipoVerificacion === 'SOCIAL') {
      socialBox.classList.remove('hidden');
      document.getElementById('timer-cat-badge').textContent = tx('Objetivo Social', 'Social Goal');
    } else {
      socialBox.classList.add('hidden');
      document.getElementById('timer-cat-badge').textContent = tx('Objetivo Medio', 'Medium Goal');
    }

    renderTimerSoundControls();
    updateTimerUI();
    document.getElementById('btn-timer-toggle').textContent = tx('Iniciar', 'Start');
    document.getElementById('modal-timer').classList.remove('hidden');
  }

  function updateTimerUI() {
    const m = String(Math.floor(activeTimer.remainingSeconds / 60)).padStart(2, '0');
    const s = String(activeTimer.remainingSeconds % 60).padStart(2, '0');
    document.getElementById('timer-display').textContent = `${m}:${s}`;

    const circumference = 490;
    const ratio = activeTimer.totalSeconds > 0
      ? activeTimer.remainingSeconds / activeTimer.totalSeconds
      : 0;
    const offset = circumference * (1 - ratio);
    document.getElementById('timer-ring-progress').style.strokeDashoffset = offset;
  }

  function closeTimerModal() {
    clearInterval(activeTimer.intervalId);
    activeTimer.running = false;
    window.BroteAudio.stopAmbient();
    document.getElementById('modal-timer').classList.add('hidden');
  }

  // --- MODAL PUENTE ANALÓGICO (LARGOS - OLED) ---
  function updateAnalogSoundBtn() {
    const btn = document.getElementById('btn-analog-sound');
    const labelEl = document.getElementById('analog-sound-label');
    const iconEl = document.getElementById('analog-sound-icon');
    if (!btn || !labelEl || !iconEl) return;
    const activeId = getActiveBiomeId();
    const soundMeta = window.BroteAudio.getBiomeSoundMeta(activeId);
    const isPlaying = window.BroteAudio.getMode() === `biome:${activeId}`;
    btn.classList.toggle('active', isPlaying);
    iconEl.textContent = isPlaying ? 'graphic_eq' : 'volume_up';
    labelEl.textContent = isPlaying
      ? `${soundMeta.label} · ${tx('Activo', 'Playing')}`
      : `${tx('Sonido', 'Sound')}: ${soundMeta.label}`;
  }

  function openAnalogModal(goal) {
    activeAnalog.countdownMode = false;
    activeAnalog.running = false;
    activeAnalog.remainingSeconds = 0;
    activeAnalog.goal = goal;
    activeAnalog.elapsedSeconds = 0;
    activeAnalog.pausedByLeave = false;
    clearInterval(activeAnalog.intervalId);

    const toggleBtn = document.getElementById('btn-analog-toggle');
    const completeBtn = document.getElementById('btn-analog-complete');
    if (toggleBtn) toggleBtn.classList.add('hidden');
    if (completeBtn) {
      completeBtn.classList.remove('btn-dark-ghost');
      completeBtn.textContent = tx('Ya terminé este reto', 'I finished this challenge');
    }

    document.getElementById('analog-icon').textContent = goal.icono || 'auto_stories';
    document.getElementById('analog-goal-title').textContent = goal.titulo;
    document.getElementById('analog-status').textContent = tx(
      'Modo Puente Analógico activo. Disfruta el mundo real; si sales a redes sociales, pausaremos el contador suavemente.',
      'Analog Bridge mode active. Enjoy the real world; if you switch to social apps, we gently pause the timer.'
    );
    updateAnalogTimerUI();
    updateAnalogSoundBtn();

    activeAnalog.intervalId = setInterval(() => {
      if (!activeAnalog.pausedByLeave) {
        activeAnalog.elapsedSeconds++;
        updateAnalogTimerUI();
      }
    }, 1000);

    document.getElementById('modal-analog').classList.remove('hidden');
    setOledSystemBars(true);
  }

  function openWidgetOledTimer(minutes) {
    const mins = [2, 15, 30].includes(Number(minutes)) ? Number(minutes) : 15;
    ['modal-simulator', 'modal-widgets', 'modal-biome-3d', 'modal-timer'].forEach(id => {
      const el = document.getElementById(id);
      if (el) el.classList.add('hidden');
    });
    if (viewer3D.open) {
      viewer3D.open = false;
      if (viewer3D.rafId) {
        cancelAnimationFrame(viewer3D.rafId);
        viewer3D.rafId = null;
      }
    }
    clearInterval(activeAnalog.intervalId);
    activeAnalog.countdownMode = true;
    activeAnalog.goal = null;
    activeAnalog.remainingSeconds = mins * 60;
    activeAnalog.elapsedSeconds = 0;
    activeAnalog.running = false;
    activeAnalog.pausedByLeave = false;

    const toggleBtn = document.getElementById('btn-analog-toggle');
    const completeBtn = document.getElementById('btn-analog-complete');
    if (toggleBtn) {
      toggleBtn.classList.remove('hidden');
      toggleBtn.textContent = tx('Activar temporizador', 'Start timer');
    }
    if (completeBtn) {
      completeBtn.classList.add('btn-dark-ghost');
      completeBtn.textContent = tx('Cerrar', 'Close');
    }

    document.getElementById('analog-icon').textContent = 'timer';
    document.getElementById('analog-goal-title').textContent = tx(
      `Temporizador anti-pantalla · ${mins} min`,
      `Screen-free timer · ${mins} min`
    );
    document.getElementById('analog-status').textContent = tx(
      'Pulsa activar y deja el móvil boca abajo para desconectar.',
      'Tap start and place your phone face down to disconnect.'
    );
    updateAnalogTimerUI();
    updateAnalogSoundBtn();
    document.getElementById('modal-analog').classList.remove('hidden');
    setOledSystemBars(true);
  }

  function updateAnalogTimerUI() {
    const secs = activeAnalog.countdownMode ? activeAnalog.remainingSeconds : activeAnalog.elapsedSeconds;
    const m = String(Math.floor(secs / 60)).padStart(2, '0');
    const s = String(secs % 60).padStart(2, '0');
    document.getElementById('analog-timer').textContent = `${m}:${s}`;
  }

  function closeAnalogModal() {
    clearInterval(activeAnalog.intervalId);
    activeAnalog.goal = null;
    activeAnalog.countdownMode = false;
    activeAnalog.running = false;
    window.BroteAudio.stopAmbient();
    updateAnalogSoundBtn();
    document.getElementById('modal-analog').classList.add('hidden');
    setOledSystemBars(false);
  }

  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'hidden') {
      if (activeAnalog.goal || (activeAnalog.countdownMode && activeAnalog.running)) {
        activeAnalog.pausedByLeave = true;
      }
    } else if (document.visibilityState === 'visible') {
      if ((activeAnalog.goal || activeAnalog.countdownMode) && activeAnalog.pausedByLeave) {
        activeAnalog.pausedByLeave = false;
        document.getElementById('analog-status').textContent = tx(
          'Pausado suavemente mientras estuviste fuera. Retoma cuando quieras, cero culpa.',
          'Gently paused while you stepped away. Pick up anytime, zero guilt.'
        );
      }
      if (activeTimer.goal && activeTimer.leftForSocial) {
        activeTimer.leftForSocial = false;
        const g = activeTimer.goal;
        closeTimerModal();
        completeGoal(g);
      }
    }
  });

  // --- MODAL CÁMARA RÁPIDA (BEREAL) ---
  async function openCameraModal() {
    capturedDataUrl = null;
    const video = document.getElementById('camera-video');
    const img = document.getElementById('camera-captured-img');
    const fallback = document.getElementById('camera-fallback');
    const btnSnap = document.getElementById('btn-take-snap');
    const btnSave = document.getElementById('btn-save-snap');

    video.classList.add('hidden');
    img.classList.add('hidden');
    btnSnap.classList.add('hidden');
    btnSave.classList.add('hidden');
    fallback.classList.remove('hidden');

    document.getElementById('modal-camera').classList.remove('hidden');

    if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
      try {
        cameraStream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: 'environment' },
          audio: false
        });
        video.srcObject = cameraStream;
        video.classList.remove('hidden');
        fallback.classList.add('hidden');
        btnSnap.classList.remove('hidden');
      } catch (_) {}
    }
  }

  function closeCameraModal() {
    if (cameraStream) {
      cameraStream.getTracks().forEach(t => t.stop());
      cameraStream = null;
    }
    document.getElementById('modal-camera').classList.add('hidden');
  }

  function compressImageToDataUrl(sourceEl, width, height) {
    const maxW = 640;
    const scale = Math.min(1, maxW / width);
    const canvas = document.getElementById('camera-canvas');
    canvas.width = Math.round(width * scale);
    canvas.height = Math.round(height * scale);
    const ctx = canvas.getContext('2d');
    ctx.drawImage(sourceEl, 0, 0, canvas.width, canvas.height);
    return canvas.toDataURL('image/jpeg', 0.72);
  }

  function savePhotoToToday(dataUrl) {
    const targetKey = pendingPhotoDateKey || getTodayKey();
    pendingPhotoDateKey = null;
    if (!state.logsByDate[targetKey]) {
      state.logsByDate[targetKey] = { completed: [], photo: null };
    }
    state.logsByDate[targetKey].photo = dataUrl;
    saveState();
    renderCalendar();
    renderMuroSocialViews();
    const dayModal = document.getElementById('modal-day');
    if (dayModal && !dayModal.classList.contains('hidden')) {
      openDayModal(targetKey, state.logsByDate[targetKey]);
    }
    showToast('photo_camera', tx('Foto guardada en tu Muro Real privado.', 'Photo saved to your private Real Wall.'));
  }

  function hasUserCompletedToday() {
    const todayLog = state.logsByDate[getTodayKey()];
    return Boolean(todayLog && Array.isArray(todayLog.completed) && todayLog.completed.length > 0);
  }

  function getMuroRealPhotos() {
    const entries = Object.entries(state.logsByDate || {})
      .filter(([, log]) => log && log.photo)
      .sort(([a], [b]) => b.localeCompare(a));
    return entries.slice(0, 6).map(([dateKey, log]) => {
      const d = new Date(`${dateKey}T12:00:00`);
      const label = Number.isNaN(d.getTime())
        ? dateKey
        : d.toLocaleDateString(getLocale(), { day: 'numeric', month: 'short' });
      const firstGoal = Array.isArray(log.completed) && log.completed.length
        ? log.completed[0].titulo
        : tx('Momento en el Muro Real', 'Real Wall Moment');
      return {
        src: log.photo,
        dateKey,
        label,
        goalTitle: firstGoal
      };
    });
  }

  function escapeHtmlText(str) {
    return String(str || '').replace(/[&<>"']/g, ch => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;'
    }[ch]));
  }

  let pendingMapRefreshOnViewerClose = false;
  let isMapDiving = false;

  function navigateToBiomeFromMap(biomeId) {
    if (!biomeId || isMapDiving) return;
    const unlocked = isBiomeUnlocked(biomeId);
    if (!unlocked) {
      const bMeta = window.BroteMascot.getBiome(biomeId);
      const progMeta = window.BroteMascot.getBiome(getCurrentProgressBiomeId());
      showToast('lock', tx(
        `${bMeta.name} aún está bloqueado. Completa ${progMeta.name} al Nv. 8 para desbloquearlo.`,
        `${bMeta.name} is still locked. Complete ${progMeta.name} (Lvl. 8) to unlock it.`
      ));
      return;
    }

    isMapDiving = true;
    const board = document.querySelector('#muro-panel-archipielago .world-map-board');
    const zone = window.BroteWorldMap && typeof window.BroteWorldMap.getZone === 'function'
      ? window.BroteWorldMap.getZone(biomeId)
      : null;

    function launchBiomeViewer() {
      state.lumi.biome = biomeId;
      state.lumi.betaPointsOverride = null;
      refreshBiomeSuggestionsOnSwitch();
      saveState();
      pendingMapRefreshOnViewerClose = true;

      openBiome3DViewer(biomeId, { fromMapZoom: true });
      setTimeout(() => {
        if (board) board.classList.remove('is-zooming-in');
        isMapDiving = false;
      }, 560);
    }

    if (board) {
      if (zone) {
        board.style.transformOrigin = `${zone.cx}px ${zone.cy}px`;
      }
      board.classList.remove('is-zooming-in');
      void board.offsetWidth;
      board.classList.add('is-zooming-in');
      setTimeout(launchBiomeViewer, 220);
    } else {
      launchBiomeViewer();
    }
  }

  function renderMuroSocialViews() {
    const badge = document.getElementById('circle-count-badge');
    if (badge && window.BroteSocial) {
      badge.textContent = String(window.BroteSocial.getFriends().length);
    }
    if (activeMuroView === 'circulo') {
      renderCirclePanel();
    } else if (activeMuroView === 'archipielago') {
      renderArchipelagoPanel();
    }
  }

  function switchMuroView(viewName) {
    activeMuroView = ['calendario', 'circulo', 'archipielago'].includes(viewName) ? viewName : 'calendario';
    document.querySelectorAll('#muro-subtabs .muro-subtab-btn').forEach(btn => {
      const match = btn.dataset.muroView === activeMuroView;
      btn.classList.toggle('active', match);
      btn.setAttribute('aria-selected', match ? 'true' : 'false');
    });
    const pCal = document.getElementById('muro-panel-calendario');
    const pCir = document.getElementById('muro-panel-circulo');
    const pArc = document.getElementById('muro-panel-archipielago');
    if (pCal) pCal.classList.toggle('hidden', activeMuroView !== 'calendario');
    if (pCir) pCir.classList.toggle('hidden', activeMuroView !== 'circulo');
    if (pArc) pArc.classList.toggle('hidden', activeMuroView !== 'archipielago');
    renderMuroSocialViews();
  }

  function renderCirclePanel() {
    const container = document.getElementById('muro-panel-circulo');
    if (!container || !window.BroteSocial) return;

    const circles = window.BroteSocial.getCircles();
    const activeCircle = window.BroteSocial.getActiveCircle();
    const friends = window.BroteSocial.getFriends(activeCircle.id);
    const unlockedByUser = hasUserCompletedToday();
    const activeBiome = getActiveBiomeId();
    const userBiomeMeta = window.BroteMascot.getBiome(activeBiome);
    const userStage = getEffectiveBiomeStage();
    const todayLog = state.logsByDate[getTodayKey()];
    const userLastGoal = todayLog && todayLog.completed && todayLog.completed.length
      ? todayLog.completed[todayLog.completed.length - 1].titulo
      : tx('Completa 1 objetivo hoy para desbloquear el círculo', 'Complete 1 goal today to unlock your circle');
    const userPhoto = (todayLog && todayLog.photo) || window.BroteSocial.getSceneSnapshot('dawn_horizon');

    const circleTabsHtml = circles.map(c => `
      <button type="button" class="circle-group-pill ${c.id === activeCircle.id ? 'active' : ''}" data-circle-select="${escapeHtmlText(c.id)}">
        <span>${escapeHtmlText(c.name)}</span>
        <span class="circle-group-count">${c.friends.length}</span>
      </button>
    `).join('') + (circles.length < window.BroteSocial.MAX_CIRCLES
      ? `<button type="button" class="circle-group-pill circle-group-add" id="btn-toggle-new-circle">+ ${tx('Nuevo', 'New')}</button>`
      : '');

    const newCircleFormHtml = circleShowNewForm
      ? `
        <form class="circle-new-form" id="circle-new-form">
          <input type="text" id="circle-new-name-input" maxlength="18" placeholder="${tx('Nombre del círculo (ej. Compañeros)...', 'Circle name (e.g. Coworkers)...')}" aria-label="${tx('Nombre del nuevo círculo', 'New circle name')}" />
          <button type="submit">${tx('Crear', 'Create')}</button>
        </form>
      `
      : '';

    const bannerHtml = !unlockedByUser
      ? `
        <div class="circle-lock-banner">
          <div class="circle-lock-copy">
            <strong>${tx(`Haz 1 objetivo hoy para ver a ${escapeHtmlText(activeCircle.name)}`, `Complete 1 goal today to see ${escapeHtmlText(activeCircle.name)}`)}</strong>
            <small>${tx('Modo BeReal anti-scroll: sus fotos están difuminadas hasta tu brote de hoy.', 'BeReal-style anti-scroll: friends\' photos stay blurred until your daily Splash.')}</small>
          </div>
          <button type="button" class="circle-go-btn" id="btn-circle-go-hoy">${tx('Ir a Hoy', 'Go to Today')}</button>
        </div>
      `
      : '';

    const reactions = [
      { key: 'eco', icon: 'eco', label: tx('Calma', 'Calm') },
      { key: 'favorite', icon: 'favorite', label: tx('Bien', 'Nice') },
      { key: 'local_fire_department', icon: 'local_fire_department', label: tx('Racha', 'Streak') },
      { key: 'celebration', icon: 'celebration', label: tx('Genial', 'Great') }
    ];

    const userName = ((state.user && state.user.name) || '').trim() || tx('Tú', 'You');
    const lvlPrefix = tx('Nv.', 'Lvl.');
    const selfCardHtml = unlockedByUser
      ? `
        <div class="circle-card">
          <div class="circle-head">
            <div class="circle-user">
              <div class="circle-avatar" style="background:#1F7A4C">${escapeHtmlText(userName[0].toUpperCase())}</div>
              <div class="circle-user-meta">
                <strong>${escapeHtmlText(userName)} (${tx('Tu día', 'Your day')})</strong>
                <small>${tx('Hoy', 'Today')} · ${lvlPrefix} ${userStage}</small>
              </div>
            </div>
            <span class="circle-biome-pill">${userBiomeMeta.shortName}</span>
          </div>
          <div class="circle-photo-box">
            <img class="circle-photo-img" src="${userPhoto}" alt="${tx('Tu momento de hoy', 'Your moment today')}" />
          </div>
          <div class="circle-goal-line">${escapeHtmlText(userLastGoal)}</div>
        </div>
      `
      : '';

    const friendsHtml = friends.length
      ? friends.map(f => {
          const bMeta = window.BroteMascot.getBiome(f.biome);
          const isBlurred = !unlockedByUser;
          const snap = window.BroteSocial.getSceneSnapshot(f.sceneType);
          const reactsHtml = reactions.map(r => `
            <button type="button" class="circle-react-btn ${f.userReaction === r.key ? 'active' : ''}" data-react-friend="${escapeHtmlText(f.id)}" data-react-key="${r.key}">
              ${iconHTML(r.icon, 'icon-xs')}
              <span>${r.label}</span>
            </button>
          `).join('');

          return `
            <div class="circle-card">
              <div class="circle-head">
                <div class="circle-user">
                  <div class="circle-avatar" style="background:${f.color}">${escapeHtmlText(f.initial)}</div>
                  <div class="circle-user-meta">
                    <strong>${escapeHtmlText(f.name)}</strong>
                    <small>${escapeHtmlText(f.timeAgo)} · ${lvlPrefix} ${f.level}</small>
                  </div>
                </div>
                <span class="circle-biome-pill">${bMeta.shortName}</span>
              </div>
              <div class="circle-photo-box">
                <img class="circle-photo-img ${isBlurred ? 'is-blurred' : ''}" src="${snap}" alt="${tx(`Momento de ${escapeHtmlText(f.name)}`, `${escapeHtmlText(f.name)}'s moment`)}" />
                ${isBlurred ? `
                  <div class="circle-lock-veil">
                    <span class="circle-lock-pill">${iconHTML('lock', 'icon-xs')} ${tx('Completa 1 reto hoy para revelar', 'Complete 1 goal today to reveal')}</span>
                  </div>
                ` : ''}
              </div>
              <div class="circle-goal-line">${isBlurred ? tx('Objetivo oculto hasta completar el tuyo', 'Goal hidden until you finish yours') : escapeHtmlText(f.todayGoal)}</div>
              ${!isBlurred ? `<div class="circle-reactions-row">${reactsHtml}</div>` : ''}
            </div>
          `;
        }).join('')
      : `
        <div class="circle-card" style="text-align:center;color:var(--md-sys-color-on-surface-variant);font-size:0.82rem;">
          ${tx(
            `Todavía no hay personas en <strong>${escapeHtmlText(activeCircle.name)}</strong>. Añade un contacto abajo con tu código.`,
            `No one in <strong>${escapeHtmlText(activeCircle.name)}</strong> yet. Add a contact below with your invite code.`
          )}
        </div>
      `;

    const inviteCode = window.BroteSocial.getInviteCode(activeCircle.id);
    const canAdd = friends.length < window.BroteSocial.MAX_FRIENDS;

    container.innerHTML = `
      <div class="circle-group-bar" role="tablist" aria-label="${tx('Tus círculos', 'Your circles')}">${circleTabsHtml}</div>
      ${newCircleFormHtml}
      <div class="circle-shared-note">
        ${iconHTML('public', 'icon-xs')}
        <span>${tx(
          `Progreso global compartido: ${userBiomeMeta.shortName} Nv. ${userStage} en todos tus círculos`,
          `Shared global progress: ${userBiomeMeta.shortName} Lvl. ${userStage} across all your circles`
        )}</span>
      </div>
      ${bannerHtml}
      <div class="circle-feed">
        ${selfCardHtml}
        ${friendsHtml}
      </div>
      <div class="circle-invite-card">
        <div class="circle-invite-head">
          <div>
            <strong>${escapeHtmlText(activeCircle.name)} (${friends.length}/${window.BroteSocial.MAX_FRIENDS})</strong>
            <small style="display:block;font-size:0.72rem;color:var(--md-sys-color-on-surface-variant);">${tx('Solo personas de este círculo · Cero rankings', 'People in this circle only · Zero leaderboards')}</small>
          </div>
          <button type="button" class="circle-code-pill" id="btn-copy-circle-code" title="${tx('Copiar código del círculo', 'Copy circle invite code')}" style="border:none;cursor:pointer;">${escapeHtmlText(inviteCode)}</button>
        </div>
        ${canAdd ? `
          <form class="circle-invite-form" id="circle-add-form">
            <input type="text" id="circle-friend-input" maxlength="18" placeholder="${tx('Añadir a', 'Add to')} ${escapeHtmlText(activeCircle.name)}..." aria-label="${tx('Nombre del amigo', 'Friend\'s name')}" />
            <button type="submit">${tx('Invitar', 'Invite')}</button>
          </form>
        ` : `<small style="color:var(--md-sys-color-on-surface-variant);font-weight:700;">${tx('Círculo completo (8/8 personas).', 'Circle full (8/8 people).')}</small>`}
      </div>
    `;

    container.querySelectorAll('[data-circle-select]').forEach(btn => {
      btn.addEventListener('click', () => {
        window.BroteSocial.setActiveCircle(btn.dataset.circleSelect);
        selectedArchFriendId = null;
        circleShowNewForm = false;
        renderMuroSocialViews();
      });
    });

    const toggleNewBtn = container.querySelector('#btn-toggle-new-circle');
    if (toggleNewBtn) {
      toggleNewBtn.addEventListener('click', () => {
        circleShowNewForm = !circleShowNewForm;
        renderCirclePanel();
        if (circleShowNewForm) {
          const inp = container.querySelector('#circle-new-name-input');
          if (inp) inp.focus();
        }
      });
    }

    const newCircleForm = container.querySelector('#circle-new-form');
    if (newCircleForm) {
      newCircleForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const inp = container.querySelector('#circle-new-name-input');
        const res = window.BroteSocial.createCircle(inp ? inp.value : '');
        if (res.ok) {
          circleShowNewForm = false;
          selectedArchFriendId = null;
          renderMuroSocialViews();
          showToast('group_add', tx(`Círculo "${res.circle.name}" creado.`, `Circle "${res.circle.name}" created.`));
        } else if (res.error) {
          showToast('info', res.error);
        }
      });
    }

    const goBtn = container.querySelector('#btn-circle-go-hoy');
    if (goBtn) {
      goBtn.addEventListener('click', () => {
        const hoyNav = document.querySelector('.ft-nav-btn[data-tab="tab-hoy"]');
        if (hoyNav) hoyNav.click();
      });
    }

    const copyBtn = container.querySelector('#btn-copy-circle-code');
    if (copyBtn) {
      copyBtn.addEventListener('click', () => {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          navigator.clipboard.writeText(inviteCode).catch(() => {});
        }
        showToast('share', tx(`Código de ${activeCircle.name}: ${inviteCode}`, `Invite code for ${activeCircle.name}: ${inviteCode}`));
      });
    }

    container.querySelectorAll('.circle-react-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        window.BroteSocial.toggleReaction(btn.dataset.reactFriend, btn.dataset.reactKey);
        renderCirclePanel();
      });
    });

    const addForm = container.querySelector('#circle-add-form');
    if (addForm) {
      addForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const inp = container.querySelector('#circle-friend-input');
        const biomes = getBiomeOrderIds();
        const randBiome = biomes[Math.floor(Math.random() * biomes.length)];
        const res = window.BroteSocial.addFriend(inp ? inp.value : '', randBiome, activeCircle.id);
        if (res.ok) {
          renderMuroSocialViews();
          showToast('group_add', tx(`${res.friend.name} añadido a ${activeCircle.name}.`, `${res.friend.name} added to ${activeCircle.name}.`));
        } else if (res.error) {
          showToast('info', res.error);
        }
      });
    }
  }

  function renderArchipelagoPanel() {
    const container = document.getElementById('muro-panel-archipielago');
    if (!container || !window.BroteSocial || !window.BroteWorldMap) return;

    const existingVp = container.querySelector('.world-map-viewport');
    if (existingVp) {
      worldMapScroll = { left: existingVp.scrollLeft, top: existingVp.scrollTop };
    }

    const circles = window.BroteSocial.getCircles();
    if (mapCircleFilter !== 'ALL' && !circles.some(c => c.id === mapCircleFilter)) {
      mapCircleFilter = 'ALL';
    }
    const friends = window.BroteSocial.getFriends(mapCircleFilter);
    const unlockedByUser = hasUserCompletedToday();
    const activeBiomeId = getActiveBiomeId();
    const userPoints = getEffectiveBiomePoints();
    const userLevel = window.BroteMascot.getLevelFromPoints(userPoints);
    const order = getBiomeOrderIds();

    if ((!selectedArchFriendId || !friends.some(f => f.id === selectedArchFriendId)) && friends.length) {
      selectedArchFriendId = friends[0].id;
    }
    const selectedFriend = friends.find(f => f.id === selectedArchFriendId) || null;

    const unlockedMap = {};
    order.forEach(bId => {
      unlockedMap[bId] = isBiomeUnlocked(bId);
    });

    const isDark = document.documentElement.dataset.theme === 'dark';

    const styleBarHtml = `
      <div class="world-map-style-bar" role="tablist" aria-label="${tx('Estilo de mapa continental', 'Continental map style')}">
        <button type="button" class="world-map-style-pill ${mapDesignStyle === 'zootropolis' ? 'active' : ''}" data-map-style="zootropolis">Prueba 1</button>
        <button type="button" class="world-map-style-pill ${mapDesignStyle === 'fortnite' ? 'active' : ''}" data-map-style="fortnite">Prueba 2</button>
        <button type="button" class="world-map-style-pill ${mapDesignStyle === 'atlas' ? 'active' : ''}" data-map-style="atlas">Prueba 3</button>
        <button type="button" class="world-map-style-pill ${mapDesignStyle === 'topo' ? 'active' : ''}" data-map-style="topo">Prueba 4</button>
        <button type="button" class="world-map-color-pill ${mapShowAllColor ? 'active' : ''}" id="btn-map-color-toggle">
          ${mapShowAllColor ? tx('Todo a color', 'All in color') : tx('Bloqueados gris', 'Locked grey')}
        </button>
      </div>
    `;

    const filterPillsHtml = [
      `<button type="button" class="world-map-filter-pill ${mapCircleFilter === 'ALL' ? 'active' : ''}" data-map-filter="ALL">${tx('Todos', 'All')} (${window.BroteSocial.getFriends('ALL').length})</button>`,
      ...circles.map(c => `<button type="button" class="world-map-filter-pill ${mapCircleFilter === c.id ? 'active' : ''}" data-map-filter="${escapeHtmlText(c.id)}">${escapeHtmlText(c.name)} (${c.friends.length})</button>`)
    ].join('');

    const selMeta = selectedFriend ? window.BroteMascot.getBiome(selectedFriend.biome) : null;
    const detailWeather = selectedFriend
      ? (selectedFriend.weather === 'radiant' ? tx('Radiante', 'Radiant') : selectedFriend.weather === 'mist' ? tx('Neblina suave', 'Soft Mist') : tx('En calma', 'Calm'))
      : '';
    const detailSub = selectedFriend
      ? (unlockedByUser
          ? `${selectedFriend.circleName ? `${selectedFriend.circleName} · ` : ''}${selectedFriend.todayGoal} (${detailWeather})`
          : `${selectedFriend.circleName ? `${selectedFriend.circleName} · ` : ''}${tx('Clima', 'Weather')}: ${detailWeather} · ${tx('Haz 1 reto hoy para ver su objetivo', 'Do 1 goal today to reveal')}`)
      : '';

    container.innerHTML = `
      ${styleBarHtml}
      <div class="world-map-filter-bar" role="tablist" aria-label="${tx('Filtrar amigos en el mapa', 'Filter friends on map')}">${filterPillsHtml}</div>
      <div class="world-map-viewport" id="world-map-viewport" data-map-style="${escapeHtmlText(mapDesignStyle)}">
        <div class="world-map-hint-bar">${iconHTML('explore', 'icon-xs')}<span>${tx('Arrastra el continente 3D · Toca un bioma para entrar', 'Drag the 3D continent · Tap a biome to enter')}</span></div>
        <div class="world-map-board">
          <canvas class="world-map-canvas" id="world-map-canvas" width="${window.BroteWorldMap.MAP_WIDTH}" height="${window.BroteWorldMap.MAP_HEIGHT}" role="img" aria-label="${tx('Mapa 3D continental de los 10 biomas de Splash', 'Splash 3D continental map of all 10 biomes')}"></canvas>
          <div class="world-map-markers-layer" id="world-map-markers-layer"></div>
        </div>
      </div>
      ${selectedFriend && selMeta ? `
        <div class="arch-detail-sheet">
          <div class="arch-detail-meta">
            <strong>${escapeHtmlText(selectedFriend.name)} ${tx('en', 'in')} ${selMeta.shortName} (${tx('Nv.', 'Lvl.')} ${selectedFriend.level})</strong>
            <small>${escapeHtmlText(detailSub)}</small>
          </div>
          <div class="arch-detail-actions">
            <button type="button" class="world-map-open-btn" id="btn-map-visit-biome">${tx('Ir allí', 'Visit')}</button>
            <button type="button" class="arch-cheer-btn ${selectedFriend.cheeredToday ? 'is-sent' : ''}" id="btn-arch-cheer">
              ${selectedFriend.cheeredToday ? tx('Enviado', 'Sent') : tx('Ánimo', 'Cheer')}
            </button>
          </div>
        </div>
      ` : ''}
    `;

    const mapCanvas = container.querySelector('#world-map-canvas');
    window.BroteWorldMap.renderMapInto(mapCanvas, {
      style: mapDesignStyle,
      showAllColor: mapShowAllColor,
      unlockedMap,
      activeBiomeId,
      selectedBiomeId: selectedFriend ? selectedFriend.biome : null,
      isDark
    });

    const markersLayer = container.querySelector('#world-map-markers-layer');
    if (markersLayer) {
      markersLayer.innerHTML = window.BroteWorldMap.ZONES.map(z => {
        const isUserHere = z.id === activeBiomeId;
        const friendsHere = friends.filter(f => f.biome === z.id);
        if (!isUserHere && !friendsHere.length) return '';

        const userBadgeHtml = isUserHere
          ? `<span class="world-map-you-pill">${tx('Tú', 'You')} · ${tx('Nv.', 'Lvl.')} ${userLevel}</span>`
          : '';

        const friendChipsHtml = friendsHere.map(f => {
          const isSel = selectedFriend && selectedFriend.id === f.id;
          return `
            <button type="button" class="world-map-friend-chip ${isSel ? 'is-selected' : ''}" data-map-friend="${escapeHtmlText(f.id)}">
              <span class="world-map-friend-dot" style="background:${f.color}">${escapeHtmlText(f.initial)}</span>
              <span>${escapeHtmlText(f.name)}</span>
            </button>
          `;
        }).join('');

        return `
          <div class="world-map-marker-anchor" style="left:${z.cx}px;top:${z.cy - 26}px;">
            ${userBadgeHtml}
            ${friendsHere.length ? `<div class="world-map-friends-row">${friendChipsHtml}</div>` : ''}
          </div>
        `;
      }).join('');
    }

    container.querySelectorAll('[data-map-style]').forEach(btn => {
      btn.addEventListener('click', () => {
        mapDesignStyle = btn.dataset.mapStyle || 'zootropolis';
        const vp = container.querySelector('#world-map-viewport');
        if (vp) worldMapScroll = { left: vp.scrollLeft, top: vp.scrollTop };
        renderArchipelagoPanel();
      });
    });

    const colorToggleBtn = container.querySelector('#btn-map-color-toggle');
    if (colorToggleBtn) {
      colorToggleBtn.addEventListener('click', () => {
        mapShowAllColor = !mapShowAllColor;
        const vp = container.querySelector('#world-map-viewport');
        if (vp) worldMapScroll = { left: vp.scrollLeft, top: vp.scrollTop };
        renderArchipelagoPanel();
      });
    }

    container.querySelectorAll('[data-map-filter]').forEach(btn => {
      btn.addEventListener('click', () => {
        mapCircleFilter = btn.dataset.mapFilter || 'ALL';
        const vp = container.querySelector('#world-map-viewport');
        if (vp) worldMapScroll = { left: vp.scrollLeft, top: vp.scrollTop };
        renderArchipelagoPanel();
      });
    });

    container.querySelectorAll('[data-map-friend]').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const vp = container.querySelector('#world-map-viewport');
        if (vp) worldMapScroll = { left: vp.scrollLeft, top: vp.scrollTop };
        selectedArchFriendId = btn.dataset.mapFriend;
        renderArchipelagoPanel();
      });
    });

    if (mapCanvas) {
      mapCanvas.addEventListener('mousemove', (e) => {
        const rect = mapCanvas.getBoundingClientRect();
        const bx = ((e.clientX - rect.left) / rect.width) * window.BroteWorldMap.MAP_WIDTH;
        const by = ((e.clientY - rect.top) / rect.height) * window.BroteWorldMap.MAP_HEIGHT;
        const hitBiome = window.BroteWorldMap.hitTestBiome(bx, by);
        mapCanvas.style.cursor = hitBiome ? 'pointer' : '';
      });
      mapCanvas.addEventListener('click', (e) => {
        if (mapCanvas.dataset.mapSuppressClick === '1') {
          mapCanvas.dataset.mapSuppressClick = '';
          return;
        }
        const rect = mapCanvas.getBoundingClientRect();
        const bx = ((e.clientX - rect.left) / rect.width) * window.BroteWorldMap.MAP_WIDTH;
        const by = ((e.clientY - rect.top) / rect.height) * window.BroteWorldMap.MAP_HEIGHT;
        const hitBiome = window.BroteWorldMap.hitTestBiome(bx, by);
        if (hitBiome) {
          navigateToBiomeFromMap(hitBiome);
        }
      });
    }

    const visitBtn = container.querySelector('#btn-map-visit-biome');
    if (visitBtn && selectedFriend) {
      visitBtn.addEventListener('click', () => {
        navigateToBiomeFromMap(selectedFriend.biome);
      });
    }

    const cheerBtn = container.querySelector('#btn-arch-cheer');
    if (cheerBtn && selectedFriend) {
      cheerBtn.addEventListener('click', () => {
        const vp = container.querySelector('#world-map-viewport');
        if (vp) worldMapScroll = { left: vp.scrollLeft, top: vp.scrollTop };
        window.BroteSocial.cheerFriend(selectedFriend.id);
        renderArchipelagoPanel();
        showToast('eco', tx(
          `Has enviado ánimo silencioso a ${selectedFriend.name}.`,
          `Sent a quiet cheer to ${selectedFriend.name}.`
        ));
      });
    }

    const viewport = container.querySelector('#world-map-viewport');
    if (viewport) {
      let dragStart = null;
      let draggingMoved = false;
      viewport.addEventListener('mousedown', (e) => {
        dragStart = { x: e.clientX, y: e.clientY, left: viewport.scrollLeft, top: viewport.scrollTop };
        draggingMoved = false;
      });
      viewport.addEventListener('mousemove', (e) => {
        if (!dragStart) return;
        const dx = e.clientX - dragStart.x;
        const dy = e.clientY - dragStart.y;
        if (Math.hypot(dx, dy) > 5) draggingMoved = true;
        if (draggingMoved) {
          viewport.scrollLeft = dragStart.left - dx;
          viewport.scrollTop = dragStart.top - dy;
        }
      });
      viewport.addEventListener('mouseup', () => {
        if (draggingMoved && mapCanvas) {
          mapCanvas.dataset.mapSuppressClick = '1';
          setTimeout(() => {
            if (mapCanvas) mapCanvas.dataset.mapSuppressClick = '';
          }, 80);
        }
        dragStart = null;
        draggingMoved = false;
      });
      viewport.addEventListener('mouseleave', () => {
        dragStart = null;
      });
    }

    requestAnimationFrame(() => {
      if (viewport) {
        if (worldMapScroll) {
          viewport.scrollLeft = worldMapScroll.left;
          viewport.scrollTop = worldMapScroll.top;
        } else {
          const z = window.BroteWorldMap.getZone(activeBiomeId);
          viewport.scrollLeft = Math.max(0, z.cx - viewport.clientWidth / 2);
          viewport.scrollTop = Math.max(0, z.cy - viewport.clientHeight / 2);
        }
      }
    });
  }

  // --- PESTAÑA 2: MURO REAL (CALENDARIO) ---
  function renderCalendar() {
    const year = currentCalendarDate.getFullYear();
    const month = currentCalendarDate.getMonth();

    const monthName = currentCalendarDate.toLocaleDateString(getLocale(), {
      month: 'long',
      year: 'numeric'
    });
    document.getElementById('cal-month-label').textContent = monthName;

    const grid = document.getElementById('calendar-grid');
    grid.innerHTML = '';

    const firstDay = new Date(year, month, 1);
    const startOffset = (firstDay.getDay() + 6) % 7;
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const todayKey = getTodayKey();

    for (let i = 0; i < startOffset; i++) {
      const empty = document.createElement('div');
      empty.className = 'cal-day is-empty';
      grid.appendChild(empty);
    }

    let countRapido = 0;
    let countMedio = 0;
    let countLargo = 0;

    for (let day = 1; day <= daysInMonth; day++) {
      const dateKey = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      const log = state.logsByDate[dateKey];
      const cell = document.createElement('button');
      cell.className = 'cal-day';
      if (dateKey === todayKey) cell.classList.add('is-today');

      const hasCompleted = log && log.completed && log.completed.length > 0;
      const hasPhoto = log && log.photo;

      if (hasCompleted || hasPhoto) {
        cell.classList.add('has-activity');
      }

      if (hasCompleted) {
        log.completed.forEach(c => {
          if (c.categoria === 'RAPIDO') countRapido++;
          if (c.categoria === 'MEDIO') countMedio++;
          if (c.categoria === 'LARGO') countLargo++;
        });
      }

      if (hasPhoto) {
        cell.innerHTML = `
          <img class="cal-day-photo" src="${log.photo}" alt="${tx('Día', 'Day')} ${day}" />
          <span class="cal-day-num-overlay">${day}</span>
        `;
      } else if (hasCompleted) {
        cell.innerHTML = `<span>${day}</span><span class="cal-day-dot"></span>`;
      } else {
        cell.innerHTML = `<span>${day}</span>`;
      }

      if (hasCompleted || hasPhoto) {
        cell.addEventListener('click', () => openDayModal(dateKey, log));
      }

      grid.appendChild(cell);
    }

    const maxCount = Math.max(countRapido, countMedio, countLargo, 1);
    document.getElementById('balance-bars').innerHTML = `
      <div class="balance-row">
        <div class="balance-label-row"><span>${tx('Rápidos', 'Quick')}</span><span>${countRapido}</span></div>
        <div class="balance-track"><div class="balance-fill fill-honey" style="width:${(countRapido / maxCount) * 100}%"></div></div>
      </div>
      <div class="balance-row">
        <div class="balance-label-row"><span>${tx('Medios', 'Medium')}</span><span>${countMedio}</span></div>
        <div class="balance-track"><div class="balance-fill fill-teal" style="width:${(countMedio / maxCount) * 100}%"></div></div>
      </div>
      <div class="balance-row">
        <div class="balance-label-row"><span>${tx('Largos', 'Long')}</span><span>${countLargo}</span></div>
        <div class="balance-track"><div class="balance-fill fill-ember" style="width:${(countLargo / maxCount) * 100}%"></div></div>
      </div>
    `;
  }

  function openDayModal(dateKey, log) {
    const d = new Date(dateKey + 'T12:00:00');
    document.getElementById('day-modal-date').textContent = d.toLocaleDateString(getLocale(), {
      weekday: 'long',
      day: 'numeric',
      month: 'long'
    });

    const photoWrap = document.getElementById('day-modal-photo-wrap');
    if (log && log.photo) {
      document.getElementById('day-modal-photo').src = log.photo;
      photoWrap.classList.remove('hidden');
    } else {
      photoWrap.classList.add('hidden');
    }

    const addPhotoBtn = document.getElementById('btn-day-add-photo');
    if (addPhotoBtn) {
      addPhotoBtn.dataset.dateKey = dateKey;
      addPhotoBtn.classList.toggle('hidden', Boolean(log && log.photo));
    }

    const logsEl = document.getElementById('day-modal-logs');
    logsEl.innerHTML = '';
    ((log && log.completed) || []).forEach(c => {
      const item = document.createElement('div');
      item.className = 'day-log-item';
      item.innerHTML = `<span class="day-log-dot"></span><span>${c.titulo}</span>`;
      logsEl.appendChild(item);
    });

    document.getElementById('modal-day').classList.remove('hidden');
  }

  // --- PESTAÑA 3: CATÁLOGO ---
  function renderCatalog() {
    catalogDirty = false;
    const listEl = document.getElementById('catalog-list');
    if (!listEl) return;
    listEl.innerHTML = '';

    const q = catalogQuery.trim().toLowerCase();
    const filtered = state.catalog.filter(g => {
      if (catalogFilter === 'SECRETO') {
        if (!g.esSecretoBioma) return false;
      } else {
        if (g.biomaExclusivo && !isBiomeUnlocked(g.biomaExclusivo)) return false;
        const matchCat = catalogFilter === 'ALL' || g.categoria === catalogFilter;
        if (!matchCat) return false;
      }
      const matchText =
        !q ||
        g.titulo.toLowerCase().includes(q) ||
        (g.tituloCorto && g.tituloCorto.toLowerCase().includes(q)) ||
        g.subcategoria.toLowerCase().includes(q);
      return matchText;
    });

    filtered.sort((a, b) => {
      const aLocked = a.biomaExclusivo && !isBiomeUnlocked(a.biomaExclusivo) ? 1 : 0;
      const bLocked = b.biomaExclusivo && !isBiomeUnlocked(b.biomaExclusivo) ? 1 : 0;
      if (catalogFilter === 'ALL') {
        const rankOf = (g) => {
          if (g.esSecretoBioma || g.biomaExclusivo) return 3;
          if (g.categoria === 'RAPIDO') return 0;
          if (g.categoria === 'MEDIO') return 1;
          return 2;
        };
        const rDiff = rankOf(a) - rankOf(b);
        if (rDiff !== 0) return rDiff;
      }
      if (aLocked !== bLocked) return aLocked - bLocked;
      return 0;
    });

    const frag = document.createDocumentFragment();
    filtered.forEach(g => {
      const isLockedSecret = Boolean(g.biomaExclusivo && !isBiomeUnlocked(g.biomaExclusivo));
      const row = document.createElement('div');
      const catKey = (g.categoria || 'RAPIDO').toLowerCase();
      const catClass = `cat-${catKey}`;
      const catLabel = g.categoria === 'RAPIDO' ? tx('Rápido', 'Quick') : g.categoria === 'MEDIO' ? tx('Medio', 'Medium') : tx('Largo', 'Long');
      const catIcon =
        g.categoria === 'MEDIO' ? 'schedule' :
        g.categoria === 'LARGO' ? 'local_fire_department' : 'bolt';

      if (isLockedSecret) {
        const bMeta = window.BroteMascot.getBiome(g.biomaExclusivo);
        row.className = `catalog-item is-biome-secret-locked ${catClass}`;
        row.dataset.biome = g.biomaExclusivo;
        row.innerHTML = `
          <div class="biome-card-strata" aria-hidden="true"><span></span><span></span><span></span></div>
          <div class="biome-card-header">
            <span class="biome-card-origin">${tx('Bioma', 'Biome')} · ${bMeta.shortName}</span>
            <span class="biome-card-cat-chip ${catClass}"><span class="biome-cat-dot"></span>${catLabel}</span>
          </div>
          <div class="goal-body">
            <span class="goal-title">${tx(`Misión especial de ${bMeta.shortName}`, `Special ${bMeta.shortName} Mission`)}</span>
            <span class="goal-meta">
              <span class="goal-secret-pill">${tx('Por descubrir', 'Undiscovered')}</span>
            </span>
            <span class="catalog-desc">${tx(
              `Se desbloquea automáticamente al llegar al bioma ${bMeta.name}.`,
              `Unlocks automatically when you reach the ${bMeta.name} biome.`
            )}</span>
          </div>
        `;
        frag.appendChild(row);
        return;
      }

      const isBiomeSpecial = Boolean(g.esSecretoBioma && g.biomaExclusivo);
      if (isBiomeSpecial) {
        const bMeta = window.BroteMascot.getBiome(g.biomaExclusivo);
        row.className = `catalog-item is-biome-secret ${catClass} ${g.estaOculto ? 'is-hidden-goal' : ''}`;
        row.dataset.biome = g.biomaExclusivo;
        row.innerHTML = `
          <div class="biome-card-strata" aria-hidden="true"><span></span><span></span><span></span></div>
          <div class="biome-card-header">
            <span class="biome-card-origin">${tx('Bioma', 'Biome')} · ${bMeta.shortName}</span>
            <span class="biome-card-cat-chip ${catClass}"><span class="biome-cat-dot"></span>${catLabel}</span>
          </div>
          <div class="biome-card-row">
            <div class="goal-body">
              <span class="goal-title">${getShortTitle(g)}</span>
              <span class="catalog-desc">${g.titulo}</span>
            </div>
            <div class="catalog-actions">
              <button class="cat-toggle-btn ${g.esFavorito ? 'is-fav' : ''}" title="${tx('Favorito (más probabilidad)', 'Favorite (higher chance)')}">
                ${iconHTML('star', 'icon-sm')}
              </button>
              <button class="cat-toggle-btn" title="${g.estaOculto ? tx('Mostrar', 'Show') : tx('Ocultar', 'Hide')}">
                ${iconHTML(g.estaOculto ? 'visibility_off' : 'visibility', 'icon-sm')}
              </button>
            </div>
          </div>
        `;
      } else {
        row.className = `catalog-item ${catClass} ${g.estaOculto ? 'is-hidden-goal' : ''}`;
        row.innerHTML = `
          <span class="material-symbols-rounded catalog-bg-glyph" aria-hidden="true">${catIcon}</span>
          <div class="goal-body">
            <span class="goal-title">${getShortTitle(g)}</span>
            <span class="goal-meta">
              <span class="catalog-cat-badge">${catLabel}</span>
              <span>${g.subcategoria}</span>
            </span>
            <span class="catalog-desc">${g.titulo}</span>
          </div>
          <div class="catalog-actions">
            <button class="cat-toggle-btn ${g.esFavorito ? 'is-fav' : ''}" title="${tx('Favorito (más probabilidad)', 'Favorite (higher chance)')}">
              ${iconHTML('star', 'icon-sm')}
            </button>
            <button class="cat-toggle-btn" title="${g.estaOculto ? tx('Mostrar', 'Show') : tx('Ocultar', 'Hide')}">
              ${iconHTML(g.estaOculto ? 'visibility_off' : 'visibility', 'icon-sm')}
            </button>
          </div>
        `;
      }

      const [favBtn, hideBtn] = row.querySelectorAll('.cat-toggle-btn');
      favBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        g.esFavorito = !g.esFavorito;
        saveState();
        renderCatalog();
      });
      hideBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        g.estaOculto = !g.estaOculto;
        saveState();
        renderCatalog();
      });
      row.addEventListener('click', () => {
        row.classList.toggle('is-expanded');
      });

      frag.appendChild(row);
    });
    listEl.appendChild(frag);
  }

  function showToast(iconName, text, withPhotoBtn = false) {
    const toast = document.getElementById('cozy-toast');
    document.getElementById('toast-icon').textContent = iconName;
    document.getElementById('toast-text').textContent = text;
    const photoBtn = document.getElementById('toast-photo-btn');
    if (withPhotoBtn) {
      photoBtn.classList.remove('hidden');
    } else {
      photoBtn.classList.add('hidden');
    }
    toast.classList.remove('hidden');
    clearTimeout(toastTimeout);
    toastTimeout = setTimeout(() => toast.classList.add('hidden'), 4800);
  }

  // --- HORARIO PERSONALIZABLE DE NOTIFICACIONES RÁPIDAS ---
  function getWeekStartDate(offsetWeeks = 0) {
    const now = new Date();
    const day = now.getDay();
    const diffToMon = (day === 0 ? -6 : 1 - day) + offsetWeeks * 7;
    const monday = new Date(now.getFullYear(), now.getMonth(), now.getDate() + diffToMon);
    return monday;
  }

  function getScheduleWeekKey(offsetWeeks = 0) {
    return getBroteWeekKey(getWeekStartDate(offsetWeeks));
  }

  function formatWeekRange(mondayDate) {
    const months = tx(
      ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'],
      ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
    );
    const sunday = new Date(mondayDate.getFullYear(), mondayDate.getMonth(), mondayDate.getDate() + 6);
    if (mondayDate.getMonth() === sunday.getMonth()) {
      return tx(
        `${mondayDate.getDate()}–${sunday.getDate()} ${months[sunday.getMonth()]}`,
        `${months[sunday.getMonth()]} ${mondayDate.getDate()}–${sunday.getDate()}`
      );
    }
    return tx(
      `${mondayDate.getDate()} ${months[mondayDate.getMonth()]}–${sunday.getDate()} ${months[sunday.getMonth()]}`,
      `${months[mondayDate.getMonth()]} ${mondayDate.getDate()}–${months[sunday.getMonth()]} ${sunday.getDate()}`
    );
  }

  function formatHoursSummary(hours) {
    const clean = normalizeHoursList(hours);
    if (!clean.length) return tx('Ninguna hora', 'No hours selected');
    const spans = [];
    let start = clean[0];
    let prev = clean[0];
    for (let i = 1; i < clean.length; i++) {
      if (clean[i] === prev + 1) {
        prev = clean[i];
      } else {
        spans.push(`${start}–${prev + 1}h`);
        start = clean[i];
        prev = clean[i];
      }
    }
    spans.push(`${start}–${prev + 1}h`);
    return spans.join(' · ');
  }

  function getEffectiveScheduleForWeek(weekKey) {
    const sched = state.notifSchedule || { defaultWeek: createDefaultWeekSchedule(), weekOverrides: {} };
    return (sched.weekOverrides && sched.weekOverrides[weekKey]) || sched.defaultWeek;
  }

  function getEditableWeekSchedule() {
    if (schedMode === 'default') {
      return state.notifSchedule.defaultWeek;
    }
    const wk = getScheduleWeekKey(schedWeekOffset);
    if (!state.notifSchedule.weekOverrides[wk]) {
      state.notifSchedule.weekOverrides[wk] = cloneWeekSchedule(state.notifSchedule.defaultWeek);
    }
    return state.notifSchedule.weekOverrides[wk];
  }

  function getNotificationStatusNow() {
    const now = new Date();
    const curHour = now.getHours();
    const dayKey = getTodayDayKey();
    const sched = getEffectiveScheduleForWeek(getBroteWeekKey(now));
    const dayCfg = sched[dayKey];
    if (!dayCfg || !dayCfg.enabled || !Array.isArray(dayCfg.hours) || !dayCfg.hours.length) {
      return { allowed: false, reason: tx('Silenciadas hoy según tu horario', 'Muted today per your schedule') };
    }
    const allowed = dayCfg.hours.includes(curHour);
    const summary = formatHoursSummary(dayCfg.hours);
    return allowed
      ? { allowed: true, reason: tx(`Activas ahora (${curHour}:00–${curHour + 1}:00)`, `Active now (${curHour}:00–${curHour + 1}:00)`) }
      : { allowed: false, reason: tx(`Silenciadas a esta hora (hoy: ${summary})`, `Muted this hour (today: ${summary})`) };
  }

  function renderNotifSchedule() {
    const wrap = document.getElementById('notif-schedule-visual');
    if (!wrap) return;

    const liveEl = document.getElementById('sched-live-status');
    const statusNow = getNotificationStatusNow();
    if (liveEl) {
      liveEl.className = `sched-live-badge ${statusNow.allowed ? 'is-on' : 'is-muted'}`;
      liveEl.innerHTML = `${iconHTML(statusNow.allowed ? 'notifications_active' : 'notifications_off', 'icon-xs')}<span>${statusNow.reason}</span>`;
    }

    document.querySelectorAll('.sched-mode-btn').forEach(btn => {
      const isMatch = btn.dataset.schedMode === schedMode;
      btn.classList.toggle('active', isMatch);
      btn.setAttribute('aria-selected', isMatch ? 'true' : 'false');
    });

    const weekBar = document.getElementById('sched-week-bar');
    const wkKey = getScheduleWeekKey(schedWeekOffset);
    const hasCustom = Boolean(state.notifSchedule.weekOverrides && state.notifSchedule.weekOverrides[wkKey]);

    if (weekBar) {
      weekBar.classList.toggle('hidden', schedMode !== 'week');
      if (schedMode === 'week') {
        const range = formatWeekRange(getWeekStartDate(schedWeekOffset));
        const prefix = schedWeekOffset === 0
          ? tx('Esta semana', 'This week')
          : schedWeekOffset === 1
            ? tx('Próx. semana', 'Next week')
            : `${tx('Semana', 'Week')} ${wkKey.split('-')[1]}`;
        document.getElementById('sched-week-label').textContent = `${prefix} · ${range}`;
        const tagEl = document.getElementById('sched-week-tag');
        tagEl.textContent = hasCustom ? tx('Personalizada', 'Custom') : tx('Usa predeterminada', 'Uses default');
        tagEl.classList.toggle('is-custom', hasCustom);
        document.getElementById('sched-week-reset').classList.toggle('hidden', !hasCustom);
      }
    }

    const activeSched = schedMode === 'default'
      ? state.notifSchedule.defaultWeek
      : getEffectiveScheduleForWeek(wkKey);
    const todayKey = getTodayDayKey();
    if (!SCHED_DAYS.some(d => d.key === schedSelectedDay)) schedSelectedDay = todayKey;

    const stripHtml = SCHED_DAYS.map(d => {
      const cfg = activeSched[d.key] || { enabled: false, hours: [] };
      const isSel = schedSelectedDay === d.key;
      const dotsHtml = SCHED_BANDS.map(band => {
        const hasAny = cfg.enabled && band.hours.some(h => cfg.hours.includes(h));
        return `<span class="dot ${hasAny ? 'on' : ''}"></span>`;
      }).join('');
      return `
        <button type="button" class="sched-day-pill ${isSel ? 'is-selected' : ''} ${cfg.enabled ? 'is-enabled' : ''} ${d.key === todayKey ? 'is-today' : ''}" data-select-day="${d.key}" aria-label="${d.name}">
          <span class="sched-day-letter">${d.label}</span>
          <span class="sched-day-dots">${dotsHtml}</span>
        </button>
      `;
    }).join('');

    const dayMeta = SCHED_DAYS.find(d => d.key === schedSelectedDay) || SCHED_DAYS[0];
    const selCfg = activeSched[dayMeta.key] || { enabled: false, hours: [] };
    const summaryText = selCfg.enabled ? formatHoursSummary(selCfg.hours) : tx('Sin notificaciones este día', 'No pings this day');

    const bandsHtml = SCHED_BANDS.map(band => {
      const pills = band.hours.map(h => {
        const isOn = selCfg.enabled && selCfg.hours.includes(h);
        return `<button type="button" class="sched-hour-pill ${isOn ? 'is-on' : ''}" data-hour="${h}" aria-pressed="${isOn}">${h}h</button>`;
      }).join('');
      return `
        <div class="sched-band-row">
          <button type="button" class="sched-band-btn" data-toggle-band="${band.key}" title="${tx('Alternar', 'Toggle')} ${band.label}">${band.label}</button>
          <div class="sched-hours-row">${pills}</div>
        </div>
      `;
    }).join('');

    wrap.innerHTML = `
      <div class="sched-days-strip">${stripHtml}</div>
      <div class="sched-editor-card">
        <div class="sched-editor-head">
          <div>
            <div class="sched-editor-day">${dayMeta.name}</div>
            <div class="sched-editor-summary">${summaryText}</div>
          </div>
          <button type="button" class="sched-switch ${selCfg.enabled ? 'is-on' : ''}" data-toggle-day="${dayMeta.key}" aria-label="${tx('Activar', 'Enable')} ${dayMeta.name}" aria-pressed="${selCfg.enabled}">
            <span class="sched-switch-thumb"></span>
          </button>
        </div>
        <div class="sched-bands">${bandsHtml}</div>
        <div class="sched-editor-foot">
          <span class="sched-editor-summary">${tx('Toca o desliza sobre las horas', 'Tap or drag across hours')}</span>
          <button type="button" class="sched-mini-btn" data-copy-weekdays="${dayMeta.key}">${tx('Aplicar a L–V', 'Apply Mon–Fri')}</button>
        </div>
      </div>
    `;
    syncAndroidNotificationSchedule();
  }

  // --- PESTAÑA 4: GALERÍA DE BIOMAS APILADOS ---
  function getDisplayPointsForBiome(biomeId) {
    const activeId = (state.lumi && state.lumi.biome) || 'valle';
    if (biomeId === activeId && state.lumi.betaPointsOverride !== null && typeof state.lumi.betaPointsOverride === 'number') {
      return state.lumi.betaPointsOverride;
    }
    return getBiomePoints(biomeId);
  }

  const biomeThumbCache = new Map();
  let biomeStackRenderToken = 0;

  function renderBiomesStackCanvases() {
    const token = ++biomeStackRenderToken;
    const isAwake = state.lumi.lastAwakeDate === getTodayKey();
    const weatherMode = getLiveWeatherMode();
    const theme = document.documentElement.dataset.theme || 'light';
    const canvases = Array.from(document.querySelectorAll('#biomes-stack-list .biome-stack-canvas'));
    const pending = [];

    canvases.forEach(canvas => {
      const id = canvas.dataset.biomeCanvas;
      const done = isBiomeCompleted(id);
      const unlocked = isBiomeUnlocked(id);
      const pts = unlocked ? getDisplayPointsForBiome(id) : 30;
      const parent = canvas.parentElement;
      const w = Math.round((parent && parent.clientWidth > 10 ? parent.clientWidth : 180));
      const h = Math.round((parent && parent.clientHeight > 10 ? parent.clientHeight : 140));
      const cachePrefix = `${id}|${pts}|${unlocked ? 1 : 0}|${done || (unlocked && isAwake) ? 1 : 0}|${unlocked ? weatherMode : 'clear'}|${theme}|`;
      const cacheKey = `${cachePrefix}${w}x${h}`;

      let cached = biomeThumbCache.get(cacheKey);
      let isExact = Boolean(cached);
      if (!cached) {
        for (const [k, val] of biomeThumbCache.entries()) {
          if (k.startsWith(cachePrefix)) {
            cached = val;
            break;
          }
        }
      }
      if (cached) {
        canvas.width = isExact ? cached.width : Math.max(1, Math.round(w * 1.15));
        canvas.height = isExact ? cached.height : Math.max(1, Math.round(h * 1.15));
        const ctx = canvas.getContext('2d');
        if (ctx) {
          ctx.clearRect(0, 0, canvas.width, canvas.height);
          ctx.drawImage(cached, 0, 0, canvas.width, canvas.height);
        }
      }
      if (!isExact) {
        pending.push({
          canvas,
          cacheKey,
          opts: {
            biome: id,
            points: pts,
            isAwake: done || (unlocked && isAwake),
            lockedClouds: !unlocked,
            weatherMode: unlocked ? weatherMode : 'clear',
            yaw: Math.PI / 4,
            pitch: 0.60,
            zoom: 1.05,
            maxDpr: 1.15
          }
        });
      }
    });

    if (pending.length === 0) return;

    let idx = 0;
    function renderBatch() {
      if (token !== biomeStackRenderToken) return;
      const batchEnd = Math.min(idx + 2, pending.length);
      for (; idx < batchEnd; idx++) {
        const item = pending[idx];
        if (!item.canvas.isConnected) continue;
        window.BroteMascot.renderToCanvas(item.canvas, item.opts);
        if (item.canvas.width > 0 && item.canvas.height > 0) {
          if (biomeThumbCache.size >= 24) {
            const firstKey = biomeThumbCache.keys().next().value;
            biomeThumbCache.delete(firstKey);
          }
          const off = document.createElement('canvas');
          off.width = item.canvas.width;
          off.height = item.canvas.height;
          const offCtx = off.getContext('2d');
          if (offCtx) {
            offCtx.drawImage(item.canvas, 0, 0);
            biomeThumbCache.set(item.cacheKey, off);
          }
        }
      }
      if (idx < pending.length) {
        requestAnimationFrame(renderBatch);
      }
    }

    requestAnimationFrame(renderBatch);
  }

  function updateBiomesViewSlider(viewMode) {
    const slider = document.getElementById('biomes-view-slider');
    if (!slider) return;
    const idx = viewMode === 'list' ? 1 : viewMode === 'large' ? 2 : 0;
    slider.style.transform = `translate3d(calc(${idx * 100}% + ${idx * 6}px), 0, 0)`;
  }

  function renderBiomesStack() {
    const listEl = document.getElementById('biomes-stack-list');
    if (!listEl) return;

    const viewMode = (state.ui && state.ui.biomesViewMode) || 'grid';
    listEl.classList.remove('view-grid', 'view-list', 'view-large');
    listEl.classList.add(`view-${viewMode}`);
    document.querySelectorAll('#biomes-view-switcher .biomes-view-btn').forEach(btn => {
      const active = btn.dataset.view === viewMode;
      btn.classList.toggle('active', active);
      btn.setAttribute('aria-selected', active ? 'true' : 'false');
    });
    updateBiomesViewSlider(viewMode);

    const activeId = state.lumi.biome || 'valle';
    const maxPts = window.BroteMascot ? window.BroteMascot.MAX_POINTS : 80;
    const unlockedCount = window.BroteMascot.BIOMES.filter(b => isBiomeUnlocked(b.id)).length;
    const summaryEl = document.getElementById('biomes-level-summary');
    if (summaryEl) {
      summaryEl.textContent = `${unlockedCount}/${window.BroteMascot.BIOMES.length} ${tx('desbloqueados', 'unlocked')}`;
    }

    const progressBiome = window.BroteMascot.getBiome(getCurrentProgressBiomeId());

    listEl.innerHTML = '';
    const stackFrag = document.createDocumentFragment();
    window.BroteMascot.BIOMES.forEach((b, idx) => {
      const isActive = b.id === activeId;
      const bPts = getDisplayPointsForBiome(b.id);
      const bLevel = window.BroteMascot.getLevelFromPoints(bPts);
      const done = isBiomeCompleted(b.id);
      const unlocked = isBiomeUnlocked(b.id);

      const card = document.createElement('div');
      card.className = `biome-stack-card ${isActive ? 'is-active' : ''} ${done ? 'is-completed' : unlocked ? 'is-improving' : 'is-uncompleted'}`;
      card.dataset.biome = b.id;
      card.setAttribute('role', 'button');
      card.setAttribute('tabindex', '0');

      const dotsHtml = b.palette.map(c => `<span class="biome-palette-dot" style="background:${c}"></span>`).join('');
      const orderText = viewMode === 'grid' ? `#${idx + 1}` : `${tx('Bioma', 'Biome')} ${idx + 1}`;
      const titleText = !unlocked ? tx('Por descubrir', 'Undiscovered') : (viewMode === 'grid' ? b.shortName : b.name);
      const subText = !unlocked
        ? tx(`Completa ${progressBiome.shortName} para despejar las nubes`, `Complete ${progressBiome.shortName} to clear the clouds`)
        : b.subtitle;
      const statusBadge = done
        ? `<span class="biome-stack-badge is-done-biome">${tx('Nv. 8 · Máx', 'Lvl. 8 · Max')}</span>`
        : unlocked
          ? `<span class="biome-stack-badge is-improving-badge">${tx('Nv.', 'Lvl.')} ${bLevel}/8</span>`
          : `<span class="biome-stack-badge is-progress">${tx('Bloqueado', 'Locked')}</span>`;
      const footerHint = isActive
        ? `${tx('Nv.', 'Lvl.')} ${bLevel} · ${tx('En inicio', 'On Home')}`
        : unlocked
          ? `${bPts}/${maxPts} pts · ${tx('Equipar', 'Equip')}`
          : `${tx('Completa', 'Complete')} ${progressBiome.shortName}`;

      card.innerHTML = `
        <div class="biome-stack-top">
          <div class="biome-stack-meta">
            <span class="biome-stack-order">${orderText}</span>
            <h3 class="biome-stack-title">${titleText}</h3>
            <p class="biome-stack-sub">${subText}</p>
          </div>
          <div class="biome-stack-actions">
            ${statusBadge}
            <button type="button" class="biome-stack-3d-btn" aria-label="${tx(
              `Abrir ${unlocked ? b.name : 'bioma por descubrir'} en 3D`,
              `Open ${unlocked ? b.name : 'undiscovered biome'} in 3D`
            )}">
              ${iconHTML('3d_rotation', 'icon-xs')}
              <span>3D</span>
            </button>
          </div>
        </div>
        <div class="biome-stack-canvas-wrap">
          <canvas class="biome-stack-canvas" data-biome-canvas="${b.id}" aria-label="${tx(
            `Vista 3D de ${unlocked ? b.name : 'bioma bloqueado'}`,
            `3D view of ${unlocked ? b.name : 'locked biome'}`
          )}"></canvas>
          ${!unlocked ? '<div class="biome-lock-overlay" aria-hidden="true"><span class="biome-lock-qmark">?</span></div>' : ''}
        </div>
        <div class="biome-stack-footer">
          <div class="biome-palette-dots">${dotsHtml}${unlocked ? `<span class="biome-missions-chip">${tx('2 especiales', '2 special')}</span>` : ''}</div>
          <span class="biome-stack-equip-hint">${footerHint}</span>
        </div>
      `;

      function equipBiome() {
        if (!unlocked) {
          showToast('lock', tx(
            `Completa ${progressBiome.name} al Nv. 8 para despejar las nubes de este bioma.`,
            `Complete ${progressBiome.name} (Lvl. 8) to clear the clouds on this biome.`
          ));
          return;
        }
        state.lumi.biome = b.id;
        state.lumi.betaPointsOverride = null;
        refreshBiomeSuggestionsOnSwitch();
        saveState();
        renderAll(false);
        renderBiomesStack();
        showToast('landscape', `${tx('Bioma activo', 'Active biome')}: ${b.name}`);
      }

      card.addEventListener('click', equipBiome);
      card.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          equipBiome();
        }
      });

      card.querySelector('.biome-stack-3d-btn').addEventListener('click', (e) => {
        e.stopPropagation();
        openBiome3DViewer(b.id);
      });

      stackFrag.appendChild(card);
    });
    listEl.appendChild(stackFrag);

    const tabBiomas = document.getElementById('tab-biomas');
    if (tabBiomas && tabBiomas.classList.contains('active')) {
      requestAnimationFrame(renderBiomesStackCanvases);
    }
  }

  // --- VISOR 3D INTERACTIVO DE BIOMAS ---
  function clamp(val, min, max) {
    return Math.max(min, Math.min(max, val));
  }

  function getViewerPoints() {
    const activeId = (state.lumi && state.lumi.biome) || 'valle';
    const viewId = viewer3D.biome || activeId;
    if (viewId === activeId) return getEffectiveBiomePoints();
    if (state.lumi.betaPointsOverride !== null && typeof state.lumi.betaPointsOverride === 'number') {
      return state.lumi.betaPointsOverride;
    }
    return getBiomePoints(viewId);
  }

  function updateViewerUI() {
    const biomeId = viewer3D.biome || (state.lumi && state.lumi.biome) || 'valle';
    const unlocked = isBiomeUnlocked(biomeId);
    const points = getViewerPoints();
    const stage = window.BroteMascot.getLevelFromPoints(points);
    const sub = window.BroteMascot.getSubProgress(points);
    const biome = window.BroteMascot.getBiome(biomeId);
    const stageInfo = window.BroteMascot.getStageInfo(stage);
    const isOverride = isBetaModeActive() && state.lumi.betaPointsOverride !== null && typeof state.lumi.betaPointsOverride === 'number';

    document.getElementById('modal-biome-3d').dataset.biome = biomeId;
    syncViewerBiomePillLabels();
    document.getElementById('viewer-biome-title').textContent = (unlocked || isOverride) ? biome.name : tx('Bioma por descubrir', 'Undiscovered Biome');
    document.getElementById('viewer-stage-label').textContent = (unlocked || isOverride)
      ? `${stageInfo.title} (${sub}/10 pts) · ${isOverride ? 'Beta' : 'Real'}`
      : isBetaModeActive()
        ? tx('Cubierto por nubes · Prueba Beta para previsualizar', 'Covered by clouds · Use Beta controls below to preview')
        : tx('Cubierto por nubes · Completa el bioma actual para desbloquear', 'Covered by clouds · Complete current biome to unlock');

    document.querySelectorAll('#viewer-biome-pills .viewer-pill').forEach(btn => {
      const isMatch = btn.dataset.biome === biomeId;
      btn.classList.toggle('active', isMatch);
      if (isMatch && typeof btn.scrollIntoView === 'function') {
        btn.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });
      }
    });

    const activeStageKey = isOverride ? String(stage) : 'real';
    document.querySelectorAll('#viewer-stage-pills .viewer-stage-pill').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.stage === activeStageKey);
    });

    const sunBtn = document.getElementById('viewer-sun-toggle');
    const sunIcon = document.getElementById('viewer-sun-icon');
    if (sunBtn && sunIcon) {
      sunBtn.classList.toggle('active', viewer3D.lightMode !== 'auto');
      sunIcon.textContent =
        viewer3D.lightMode === 'day' ? 'wb_sunny' :
        viewer3D.lightMode === 'sunset' ? 'wb_twilight' :
        viewer3D.lightMode === 'night' ? 'dark_mode' : 'brightness_auto';
    }

    const soundBtn = document.getElementById('viewer-sound-toggle');
    const soundIcon = document.getElementById('viewer-sound-icon');
    if (soundBtn && soundIcon) {
      soundBtn.classList.toggle('active', viewer3D.soundOn);
      soundIcon.textContent = viewer3D.soundOn ? 'volume_up' : 'volume_off';
    }

    const enterBtn = document.getElementById('btn-enter-biome');
    const enterLabel = document.getElementById('btn-enter-biome-label');
    if (enterBtn) {
      const canEnter = biomeId === 'costa' || biomeId === 'valle';
      enterBtn.classList.toggle('hidden', !canEnter);
      if (enterLabel) {
        enterLabel.textContent = biomeId === 'valle'
          ? tx('Entrar en Valle', 'Enter Valley')
          : tx('Entrar en Costa', 'Enter Coast');
      }
    }
  }

  function renderViewerFrame(nowMs, drawOnly = false) {
    if (!viewer3D.open) return;

    const now = typeof nowMs === 'number' ? nowMs : performance.now();
    const dt = viewer3D.prevFrameMs ? Math.min(0.05, (now - viewer3D.prevFrameMs) / 1000) : 0.0083;
    viewer3D.prevFrameMs = now;

    const isFlyingIn = (viewer3D.targetYaw !== null) || (viewer3D.targetPitch !== null);
    const isMoving =
      isFlyingIn ||
      viewer3D.pointers.size > 0 ||
      viewer3D.velYaw !== 0 ||
      viewer3D.velPitch !== 0 ||
      Math.abs(viewer3D.targetZoom - viewer3D.zoom) > 0.002;

    if (!drawOnly && !isMoving && viewer3D.lastDrawMs && (now - viewer3D.lastDrawMs) < 48) {
      viewer3D.rafId = requestAnimationFrame(renderViewerFrame);
      return;
    }
    viewer3D.lastDrawMs = now;

    const flyK = 1 - Math.exp(-9.5 * dt);
    if (!drawOnly && viewer3D.pointers.size === 0) {
      if (viewer3D.targetYaw !== null) {
        viewer3D.yaw += (viewer3D.targetYaw - viewer3D.yaw) * flyK;
        if (Math.abs(viewer3D.targetYaw - viewer3D.yaw) < 0.002) {
          viewer3D.yaw = viewer3D.targetYaw;
          viewer3D.targetYaw = null;
        }
      } else {
        viewer3D.yaw += viewer3D.velYaw * (dt * 60);
      }

      if (viewer3D.targetPitch !== null) {
        viewer3D.pitch += (viewer3D.targetPitch - viewer3D.pitch) * flyK;
        if (Math.abs(viewer3D.targetPitch - viewer3D.pitch) < 0.002) {
          viewer3D.pitch = viewer3D.targetPitch;
          viewer3D.targetPitch = null;
        }
      } else {
        viewer3D.pitch = clamp(viewer3D.pitch + viewer3D.velPitch * (dt * 60), 0.30, 1.28);
      }

      const damp = Math.pow(0.91, dt * 60);
      viewer3D.velYaw *= damp;
      viewer3D.velPitch *= damp;
      if (Math.abs(viewer3D.velYaw) < 0.0002) viewer3D.velYaw = 0;
      if (Math.abs(viewer3D.velPitch) < 0.0002) viewer3D.velPitch = 0;
    }

    if (!drawOnly) {
      viewer3D.zoom += (viewer3D.targetZoom - viewer3D.zoom) * flyK;
    }

    const viewBiome = viewer3D.biome || (state.lumi && state.lumi.biome) || 'valle';
    const isOverride = state.lumi.betaPointsOverride !== null && typeof state.lumi.betaPointsOverride === 'number';
    const lockedClouds = !isBiomeUnlocked(viewBiome) && !isOverride;
    const resolvedPhase = viewer3D.lightMode === 'auto'
      ? window.BroteMascot.getTimeOfDayPhase()
      : viewer3D.lightMode;

    const canvas = document.getElementById('viewer-3d-canvas');
    window.BroteMascot.renderToCanvas(canvas, {
      biome: viewBiome,
      points: getViewerPoints(),
      isAwake: resolvedPhase !== 'night',
      lockedClouds,
      timePhase: viewer3D.lightMode,
      weatherMode: lockedClouds ? 'clear' : getLiveWeatherMode(),
      yaw: viewer3D.yaw,
      pitch: viewer3D.pitch,
      zoom: viewer3D.zoom,
      time: now / 1000,
      maxDpr: 1.25
    });

    if (!drawOnly) {
      viewer3D.rafId = requestAnimationFrame(renderViewerFrame);
    }
  }

  function openBiome3DViewer(previewBiomeId, opts = {}) {
    viewer3D.open = true;
    viewer3D.biome = typeof previewBiomeId === 'string' && previewBiomeId
      ? previewBiomeId
      : ((state.lumi && state.lumi.biome) || 'valle');
    viewer3D.lightMode = 'auto';
    viewer3D.soundOn = false;
    viewer3D.velYaw = 0;
    viewer3D.velPitch = 0;
    viewer3D.prevFrameMs = 0;
    if (opts && opts.fromMapZoom) {
      viewer3D.zoom = 0.56;
      viewer3D.targetZoom = 1.05;
      viewer3D.pitch = 1.18;
      viewer3D.targetPitch = 0.60;
      viewer3D.yaw = Math.PI / 4 - 0.55;
      viewer3D.targetYaw = Math.PI / 4;
    } else {
      viewer3D.targetYaw = null;
      viewer3D.targetPitch = null;
    }
    viewer3D.pointers.clear();
    viewer3D.lastPinchDist = null;

    document.getElementById('viewer-hint').classList.remove('is-hidden');
    const modalEl = document.getElementById('modal-biome-3d');
    if (opts && opts.fromMapZoom && modalEl) {
      modalEl.classList.add('is-pre-enter');
    }
    modalEl.classList.remove('hidden');
    updateViewerUI();

    if (viewer3D.rafId) cancelAnimationFrame(viewer3D.rafId);
    renderViewerFrame(performance.now(), true);
    if (opts && opts.fromMapZoom && modalEl) {
      void modalEl.offsetWidth;
      modalEl.classList.remove('is-pre-enter');
    }
    viewer3D.rafId = requestAnimationFrame(renderViewerFrame);
  }

  function closeBiome3DViewer() {
    viewer3D.open = false;
    if (window.BroteExploreCosta && typeof window.BroteExploreCosta.close === 'function') {
      window.BroteExploreCosta.close();
    }
    if (viewer3D.soundOn) {
      window.BroteAudio.stopAmbient();
      viewer3D.soundOn = false;
    }
    if (viewer3D.rafId) {
      cancelAnimationFrame(viewer3D.rafId);
      viewer3D.rafId = null;
    }
    viewer3D.pointers.clear();
    if (viewer3D.biome !== ((state.lumi && state.lumi.biome) || 'valle')) {
      state.lumi.betaPointsOverride = null;
      saveState();
    }
    document.getElementById('modal-biome-3d').classList.add('hidden');
    if (pendingMapRefreshOnViewerClose) {
      pendingMapRefreshOnViewerClose = false;
      renderAll(false);
    } else {
      renderMascot(false);
      const biomesTab = document.getElementById('tab-biomas');
      if (biomesTab && biomesTab.classList.contains('active')) {
        renderBiomesStack();
      }
    }
  }

  // --- STUDIO DE WIDGETS PIXEL (MATERIAL 3 EXPRESSIVE) ---
  function openWidgetsModal() {
    widgetStudio.open = true;
    document.getElementById('modal-widgets').classList.remove('hidden');
    renderWidgetStudio();
  }

  function renderWidgetStudio() {
    const stage = document.getElementById('pixel-widget-stage');
    if (!stage) return;

    document.querySelectorAll('#widget-type-tabs .widget-tab-btn').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.wtype === widgetStudio.type);
    });
    document.querySelectorAll('#widget-time-tabs .widget-time-pill').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.wtime === widgetStudio.timeMode);
    });
    document.querySelectorAll('#widget-theme-tabs .widget-time-pill').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.wtheme === widgetStudio.themeMode);
    });

    const activeBiome = getActiveBiomeId();
    const biomeMeta = window.BroteMascot.getBiome(activeBiome);
    const pts = getEffectiveBiomePoints();
    const lvl = window.BroteMascot.getLevelFromPoints(pts);
    const weatherMode = getLiveWeatherMode();
    const resolvedPhase = widgetStudio.timeMode === 'auto'
      ? window.BroteMascot.getTimeOfDayPhase()
      : widgetStudio.timeMode;
    stage.dataset.wtime = resolvedPhase;

    const widgetIsDark =
      widgetStudio.themeMode === 'dark' ||
      (widgetStudio.themeMode === 'auto' && resolvedPhase === 'night');
    const darkCls = widgetIsDark ? 'is-dark' : '';

    const now = new Date();
    const hh = String(now.getHours()).padStart(2, '0');
    const mm = String(now.getMinutes()).padStart(2, '0');
    const clockBar = `<div class="pixel-bar-status"><span>Splash</span><span>${hh}:${mm}</span></div>`;

    const rId = (state.suggestions.rapidos || [])[0];
    const mId = (state.suggestions.medios || [])[0];
    const g1 = state.catalog.find(g => g.id === rId) || state.catalog.find(g => g.categoria === 'RAPIDO');
    const g2 = state.catalog.find(g => g.id === mId) || state.catalog.find(g => g.categoria === 'MEDIO');

    if (widgetStudio.type === '4x2') {
      stage.innerHTML = `
        ${clockBar}
        <div class="m3e-widget-4x2 ${darkCls}">
          <div class="m3e-w-diorama-box">
            <div class="m3e-w-canvas-wrap">
              <canvas class="m3e-w-canvas" id="widget-biome-canvas"></canvas>
            </div>
            <span class="m3e-w-pill">${biomeMeta.shortName}</span>
          </div>
          <div class="m3e-w-goals-col">
            <button type="button" class="m3e-goal-tile" data-wgoal-id="${g1 ? g1.id : ''}">
              <div>
                <strong>${g1 ? getShortTitle(g1) : tx('Desconecta', 'Eye Rest')}</strong>
                <small>2 min · ${tx('Rápido', 'Quick')}</small>
              </div>
            </button>
            <button type="button" class="m3e-goal-tile is-medio" data-wgoal-id="${g2 ? g2.id : ''}">
              <div>
                <strong>${g2 ? getShortTitle(g2) : tx('Modo pausa', 'Pause Mode')}</strong>
                <small>15 min · ${tx('Enfoque', 'Focus')}</small>
              </div>
            </button>
          </div>
        </div>
      `;
    } else if (widgetStudio.type === '2x2') {
      stage.innerHTML = `
        <div class="pixel-bar-status"><span>Splash · Widgets</span></div>
        <div class="m3e-widget-2x2 ${darkCls}">
          <div class="m3e-2x2-canvas-wrap">
            <canvas class="m3e-w-canvas" id="widget-biome-canvas"></canvas>
          </div>
          <div class="m3e-2x2-title">${biomeMeta.name}</div>
        </div>
      `;
    } else {
      stage.innerHTML = `
        ${clockBar}
        <div class="m3e-widget-lock ${darkCls}">
          <div class="m3e-lock-clock">${hh}:${mm}</div>
          <div class="m3e-lock-glance">
            ${iconHTML('landscape', 'icon-xs')}
            <span>${biomeMeta.shortName} ${tx('Nv.', 'Lvl.')}${lvl} · ${getWeatherLabel(weatherMode, resolvedPhase)}</span>
          </div>
          <div class="m3e-lock-actions">
            <button type="button" class="m3e-lock-pill-btn">2 min</button>
            <button type="button" class="m3e-lock-pill-btn">15 min</button>
            <button type="button" class="m3e-lock-pill-btn">30 min</button>
          </div>
        </div>
      `;
    }

    const wCanvas = document.getElementById('widget-biome-canvas');
    if (wCanvas) {
      requestAnimationFrame(() => {
        window.BroteMascot.renderToCanvas(wCanvas, {
          biome: activeBiome,
          points: pts,
          isAwake: resolvedPhase !== 'night',
          timePhase: resolvedPhase,
          weatherMode,
          yaw: Math.PI / 4,
          pitch: 0.60,
          zoom: 1.05
        });
      });
    }
    syncAndroidWidget();
  }

  function bindEvents() {
    const TAB_ORDER = ['tab-hoy', 'tab-muro', 'tab-catalogo', 'tab-biomas'];
    let currentTabId = 'tab-hoy';

    function updateNavSlider(targetId) {
      const slider = document.getElementById('ft-active-slider');
      const toolbar = slider && slider.parentElement;
      if (!slider || !toolbar) return;
      const activeBtn = toolbar.querySelector(`.ft-item[data-tab="${targetId}"]`);
      if (!activeBtn) {
        slider.style.opacity = '0';
        return;
      }
      slider.style.opacity = '1';
      slider.style.width = `${activeBtn.offsetWidth}px`;
      slider.style.transform = `translate3d(${activeBtn.offsetLeft}px, 0, 0)`;
    }

    requestAnimationFrame(() => updateNavSlider(currentTabId));
    window.addEventListener('resize', () => updateNavSlider(currentTabId));

    function openTab(targetId) {
      if (targetId === currentTabId) return;
      const prevIdx = TAB_ORDER.indexOf(currentTabId);
      const nextIdx = TAB_ORDER.indexOf(targetId);
      const dirClass = nextIdx > prevIdx ? 'slide-from-right' : nextIdx < prevIdx ? 'slide-from-left' : '';
      currentTabId = targetId;

      if (targetId === 'tab-catalogo' && catalogDirty) {
        renderCatalog();
      } else if (targetId === 'tab-hoy') {
        renderMascot(false);
      }

      document.querySelectorAll('.ft-nav-btn').forEach(b => {
        b.classList.toggle('active', b.dataset.tab === targetId);
      });
      document.querySelectorAll('.tab-view').forEach(v => {
        v.classList.remove('slide-from-right', 'slide-from-left');
        const isTarget = v.id === targetId;
        v.classList.toggle('active', isTarget);
        if (isTarget && dirClass) {
          void v.offsetWidth;
          v.classList.add(dirClass);
        }
      });
      requestAnimationFrame(() => updateNavSlider(targetId));
      setTimeout(() => updateNavSlider(targetId), 300);

      const mainEl = document.getElementById('main-content');
      if (mainEl) mainEl.scrollTop = 0;
      if (targetId === 'tab-biomas') {
        const stackList = document.getElementById('biomes-stack-list');
        if (stackList && stackList.children.length > 0) {
          setTimeout(() => renderBiomesStackCanvases(), 110);
        } else {
          setTimeout(() => renderBiomesStack(), 90);
        }
      } else {
        biomeStackRenderToken++;
        if (targetId === 'tab-muro') {
          requestAnimationFrame(() => renderMuroSocialViews());
        }
      }
    }

    const muroSubtabs = document.getElementById('muro-subtabs');
    if (muroSubtabs) {
      muroSubtabs.addEventListener('click', (e) => {
        const btn = e.target.closest('.muro-subtab-btn');
        if (!btn) return;
        switchMuroView(btn.dataset.muroView);
      });
    }

    const dayAddPhotoBtn = document.getElementById('btn-day-add-photo');
    if (dayAddPhotoBtn) {
      dayAddPhotoBtn.addEventListener('click', () => {
        pendingPhotoDateKey = dayAddPhotoBtn.dataset.dateKey || getTodayKey();
        openCameraModal();
      });
    }

    // Barra flotante estilo Google Photos
    document.querySelectorAll('.ft-nav-btn').forEach(btn => {
      btn.addEventListener('click', () => openTab(btn.dataset.tab));
    });

    const headerLevelPill = document.getElementById('header-level');
    if (headerLevelPill) {
      headerLevelPill.addEventListener('click', () => openTab('tab-biomas'));
      headerLevelPill.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          openTab('tab-biomas');
        }
      });
    }

    const viewSwitcher = document.getElementById('biomes-view-switcher');
    if (viewSwitcher) {
      viewSwitcher.addEventListener('click', (e) => {
        const btn = e.target.closest('.biomes-view-btn');
        if (!btn) return;
        const nextView = btn.dataset.view || 'grid';
        const curView = (state.ui && state.ui.biomesViewMode) || 'grid';
        if (nextView === curView) return;
        if (!state.ui) state.ui = { biomesViewMode: 'grid', themeMode: 'system' };
        state.ui.biomesViewMode = nextView;
        saveState();

        document.querySelectorAll('#biomes-view-switcher .biomes-view-btn').forEach(b => {
          const active = b.dataset.view === nextView;
          b.classList.toggle('active', active);
          b.setAttribute('aria-selected', active ? 'true' : 'false');
        });
        updateBiomesViewSlider(nextView);

        const listEl = document.getElementById('biomes-stack-list');
        if (listEl) {
          listEl.classList.add('is-switching-view');
          setTimeout(() => {
            renderBiomesStack();
            requestAnimationFrame(() => {
              requestAnimationFrame(() => {
                listEl.classList.remove('is-switching-view');
              });
            });
          }, 150);
        } else {
          renderBiomesStack();
        }
      });
    }

    document.querySelectorAll('[data-close]').forEach(btn => {
      btn.addEventListener('click', () => {
        const id = btn.dataset.close;
        if (id === 'modal-timer') closeTimerModal();
        else if (id === 'modal-analog') closeAnalogModal();
        else if (id === 'modal-camera') {
          pendingPhotoDateKey = null;
          closeCameraModal();
        } else if (id === 'modal-widgets') {
          widgetStudio.open = false;
          document.getElementById(id).classList.add('hidden');
        } else {
          document.getElementById(id).classList.add('hidden');
        }
      });
    });

    // Selector de bioma 3D (solo entre los desbloqueados)
    function switchBiome(delta) {
      const unlockedList = getBiomeOrderIds().filter(id => isBiomeUnlocked(id));
      const current = state.lumi.biome || 'valle';
      if (unlockedList.length <= 1) {
        const currMeta = window.BroteMascot.getBiome(current);
        showToast('lock', tx(
          `Completa ${currMeta.shortName} al Nv. 8 para desbloquear el siguiente bioma.`,
          `Complete ${currMeta.shortName} (Lvl. 8) to unlock the next biome.`
        ));
        return;
      }
      const idx = unlockedList.indexOf(current);
      const start = idx === -1 ? 0 : idx;
      const nextId = unlockedList[(start + delta + unlockedList.length) % unlockedList.length];
      state.lumi.biome = nextId;
      state.lumi.betaPointsOverride = null;
      refreshBiomeSuggestionsOnSwitch();
      saveState();
      renderAll(true);
      const biome = window.BroteMascot.getBiome(nextId);
      window.BroteMascot.spawnParticles(14, biome.palette);
    }

    document.getElementById('btn-biome-prev').addEventListener('click', () => switchBiome(-1));
    document.getElementById('btn-biome-next').addEventListener('click', () => switchBiome(1));

    // Entrar al visor 3D interactivo
    document.getElementById('btn-open-biome-3d').addEventListener('click', (e) => {
      e.stopPropagation();
      openBiome3DViewer();
    });

    const dioramaWrap = document.getElementById('mascot-diorama-wrap');
    dioramaWrap.addEventListener('click', () => openBiome3DViewer());
    dioramaWrap.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        openBiome3DViewer();
      }
    });

    document.getElementById('btn-close-biome-3d').addEventListener('click', closeBiome3DViewer);

    const canvas3d = document.getElementById('viewer-3d-canvas');
    canvas3d.addEventListener('pointerdown', (e) => {
      canvas3d.setPointerCapture(e.pointerId);
      viewer3D.pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
      viewer3D.velYaw = 0;
      viewer3D.velPitch = 0;
      viewer3D.targetYaw = null;
      viewer3D.targetPitch = null;
      if (viewer3D.pointers.size === 2) {
        const pts = Array.from(viewer3D.pointers.values());
        viewer3D.lastPinchDist = Math.hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y);
      }
    });

    canvas3d.addEventListener('pointermove', (e) => {
      const prev = viewer3D.pointers.get(e.pointerId);
      if (!prev) return;
      viewer3D.pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });

      if (viewer3D.pointers.size === 1) {
        const dx = e.clientX - prev.x;
        const dy = e.clientY - prev.y;
        viewer3D.yaw += dx * 0.011;
        viewer3D.pitch = clamp(viewer3D.pitch + dy * 0.008, 0.30, 1.28);
        viewer3D.velYaw = dx * 0.007;
        viewer3D.velPitch = dy * 0.005;
        document.getElementById('viewer-hint').classList.add('is-hidden');
      } else if (viewer3D.pointers.size === 2) {
        const pts = Array.from(viewer3D.pointers.values());
        const dist = Math.hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y);
        if (viewer3D.lastPinchDist && dist > 10) {
          viewer3D.targetZoom = clamp(viewer3D.targetZoom * (dist / viewer3D.lastPinchDist), 0.75, 2.6);
        }
        viewer3D.lastPinchDist = dist;
        document.getElementById('viewer-hint').classList.add('is-hidden');
      }
    });

    function handlePointerEnd(e) {
      viewer3D.pointers.delete(e.pointerId);
      if (viewer3D.pointers.size < 2) {
        viewer3D.lastPinchDist = null;
      }
    }
    canvas3d.addEventListener('pointerup', handlePointerEnd);
    canvas3d.addEventListener('pointercancel', handlePointerEnd);

    canvas3d.addEventListener('wheel', (e) => {
      e.preventDefault();
      viewer3D.targetZoom = clamp(viewer3D.targetZoom - e.deltaY * 0.0014, 0.75, 2.6);
      document.getElementById('viewer-hint').classList.add('is-hidden');
    }, { passive: false });

    document.getElementById('viewer-zoom-in').addEventListener('click', () => {
      viewer3D.targetZoom = clamp(viewer3D.targetZoom + 0.25, 0.75, 2.6);
    });
    document.getElementById('viewer-zoom-out').addEventListener('click', () => {
      viewer3D.targetZoom = clamp(viewer3D.targetZoom - 0.25, 0.75, 2.6);
    });
    document.getElementById('viewer-reset').addEventListener('click', () => {
      viewer3D.yaw = Math.PI / 4;
      viewer3D.pitch = 0.60;
      viewer3D.velYaw = 0;
      viewer3D.velPitch = 0;
      viewer3D.targetZoom = 1.05;
    });
    document.getElementById('viewer-sun-toggle').addEventListener('click', () => {
      const order = ['auto', 'day', 'sunset', 'night'];
      const curIdx = order.indexOf(viewer3D.lightMode);
      viewer3D.lightMode = order[(curIdx + 1) % order.length];
      updateViewerUI();
    });

    const viewerSoundBtn = document.getElementById('viewer-sound-toggle');
    if (viewerSoundBtn) {
      viewerSoundBtn.addEventListener('click', () => {
        const viewBiome = viewer3D.biome || getActiveBiomeId();
        if (viewer3D.soundOn) {
          window.BroteAudio.stopAmbient();
          viewer3D.soundOn = false;
        } else {
          window.BroteAudio.startBiomeAmbient(viewBiome);
          viewer3D.soundOn = true;
        }
        updateViewerUI();
      });
    }

    const enterBiomeBtn = document.getElementById('btn-enter-biome');
    if (enterBiomeBtn) {
      enterBiomeBtn.addEventListener('click', () => {
        const currentBiome = viewer3D.biome || getActiveBiomeId();
        if (currentBiome !== 'costa' && currentBiome !== 'valle') return;
        if (window.BroteExploreCosta && typeof window.BroteExploreCosta.open === 'function') {
          window.BroteExploreCosta.open({
            biome: currentBiome,
            lightMode: viewer3D.lightMode,
            soundOn: viewer3D.soundOn,
            points: getViewerPoints(),
            weatherMode: getLiveWeatherMode(),
            photos: getMuroRealPhotos()
          });
        }
      });
    }

    const closeExploreBtn = document.getElementById('btn-close-explore-costa');
    if (closeExploreBtn) {
      closeExploreBtn.addEventListener('click', () => {
        if (window.BroteExploreCosta && typeof window.BroteExploreCosta.close === 'function') {
          window.BroteExploreCosta.close();
        }
        viewer3D.soundOn = false;
        updateViewerUI();
      });
    }

    const exploreWeatherBtn = document.getElementById('explore-weather-btn');
    if (exploreWeatherBtn) {
      exploreWeatherBtn.addEventListener('click', () => {
        if (window.BroteExploreCosta && typeof window.BroteExploreCosta.cycleWeather === 'function') {
          window.BroteExploreCosta.cycleWeather();
        }
      });
    }

    const exploreLightBtn = document.getElementById('explore-light-btn');
    if (exploreLightBtn) {
      exploreLightBtn.addEventListener('click', () => {
        if (window.BroteExploreCosta && typeof window.BroteExploreCosta.cycleLight === 'function') {
          window.BroteExploreCosta.cycleLight();
        }
      });
    }

    const exploreSoundBtn = document.getElementById('explore-sound-btn');
    if (exploreSoundBtn) {
      exploreSoundBtn.addEventListener('click', () => {
        if (window.BroteExploreCosta && typeof window.BroteExploreCosta.toggleSound === 'function') {
          window.BroteExploreCosta.toggleSound();
        }
      });
    }

    const explorePhotoClose = document.getElementById('explore-photo-close');
    const explorePhotoOverlay = document.getElementById('explore-photo-overlay');
    if (explorePhotoClose) {
      explorePhotoClose.addEventListener('click', () => {
        if (window.BroteExploreCosta && typeof window.BroteExploreCosta.closePhoto === 'function') {
          window.BroteExploreCosta.closePhoto();
        }
      });
    }
    if (explorePhotoOverlay) {
      explorePhotoOverlay.addEventListener('click', (e) => {
        if (e.target === explorePhotoOverlay && window.BroteExploreCosta && typeof window.BroteExploreCosta.closePhoto === 'function') {
          window.BroteExploreCosta.closePhoto();
        }
      });
    }

    const exploreSpotsDock = document.getElementById('explore-spots-dock');
    if (exploreSpotsDock) {
      exploreSpotsDock.addEventListener('click', (e) => {
        const btn = e.target.closest('.explore-spot-pill');
        if (!btn) return;
        if (window.BroteExploreCosta && typeof window.BroteExploreCosta.goToSpot === 'function') {
          window.BroteExploreCosta.goToSpot(btn.dataset.spot);
        }
      });
    }

    const exploreWalkPad = document.getElementById('explore-walk-pad');
    if (exploreWalkPad) {
      exploreWalkPad.querySelectorAll('[data-move]').forEach(btn => {
        const dir = btn.dataset.move;
        const startMove = (e) => {
          e.preventDefault();
          if (window.BroteExploreCosta && typeof window.BroteExploreCosta.setMove === 'function') {
            window.BroteExploreCosta.setMove(dir, true);
          }
        };
        const stopMove = () => {
          if (window.BroteExploreCosta && typeof window.BroteExploreCosta.setMove === 'function') {
            window.BroteExploreCosta.setMove(dir, false);
          }
        };
        btn.addEventListener('pointerdown', startMove);
        btn.addEventListener('pointerup', stopMove);
        btn.addEventListener('pointercancel', stopMove);
        btn.addEventListener('pointerleave', stopMove);
      });
    }

    document.getElementById('viewer-biome-pills').addEventListener('click', (e) => {
      const btn = e.target.closest('.viewer-pill');
      if (!btn) return;
      viewer3D.biome = btn.dataset.biome || 'valle';
      state.lumi.betaPointsOverride = null;
      if (viewer3D.soundOn) {
        window.BroteAudio.startBiomeAmbient(viewer3D.biome);
      }
      saveState();
      updateViewerUI();
    });

    document.getElementById('viewer-stage-pills').addEventListener('click', (e) => {
      const btn = e.target.closest('.viewer-stage-pill');
      if (!btn) return;
      state.lumi.betaPointsOverride = btn.dataset.stage === 'real'
        ? null
        : window.BroteMascot.getPointsForStagePreset(Number(btn.dataset.stage));
      saveState();
      updateViewerUI();
      if ((viewer3D.biome || 'valle') === (state.lumi.biome || 'valle')) {
        renderMascot(false);
      }
    });

    document.getElementById('viewer-sim-pills').addEventListener('click', (e) => {
      const btn = e.target.closest('.viewer-sim-btn');
      if (!btn) return;
      const add = Number(btn.dataset.add) || 1;
      const currentPts = getViewerPoints();
      state.lumi.betaPointsOverride = Math.min(window.BroteMascot.MAX_POINTS, currentPts + add);
      saveState();
      updateViewerUI();
      if ((viewer3D.biome || 'valle') === (state.lumi.biome || 'valle')) {
        renderMascot(true);
      }
    });

    // Selector de tiempo en Hoy (cero scroll)
    document.querySelectorAll('.time-seg').forEach(seg => {
      seg.addEventListener('click', () => {
        document.querySelectorAll('.time-seg').forEach(s => {
          s.classList.remove('active');
          s.setAttribute('aria-selected', 'false');
        });
        seg.classList.add('active');
        seg.setAttribute('aria-selected', 'true');
        activeHomeBlock = seg.dataset.block;
        showCompletedDrawer = false;
        renderTimeBlocks();
      });
    });

    const toggleCompletedBtn = document.getElementById('btn-toggle-completed');
    if (toggleCompletedBtn) {
      toggleCompletedBtn.addEventListener('click', () => {
        showCompletedDrawer = !showCompletedDrawer;
        renderTimeBlocks();
      });
    }

    document.getElementById('btn-block-refresh').addEventListener('click', () => {
      if (activeHomeBlock === 'RAPIDO') {
        const exclude = state.suggestions.rapidos.concat(getPeriodCompletedGoals('RAPIDO').map(g => g.id));
        state.suggestions.rapidos = pickWeightedGoals('RAPIDO', 2, exclude);
        if (!state.suggestions.rapidos.length) state.suggestions.rapidos = pickWeightedGoals('RAPIDO', 2);
      } else if (activeHomeBlock === 'MEDIO') {
        const exclude = state.suggestions.medios.concat(getPeriodCompletedGoals('MEDIO').map(g => g.id));
        state.suggestions.medios = pickWeightedGoals('MEDIO', 2, exclude);
        if (!state.suggestions.medios.length) state.suggestions.medios = pickWeightedGoals('MEDIO', 2);
      }
      saveState();
      renderTimeBlocks();
    });

    document.getElementById('btn-weekly-reroll').addEventListener('click', () => {
      if (state.weekly.rerollsLeft <= 0) return;
      state.weekly.challengeIds = pickWeightedGoals('LARGO', 2, state.weekly.challengeIds);
      state.weekly.rerollsLeft--;
      saveState();
      renderTimeBlocks();
      showToast('casino', tx('Nuevos retos asignados para esta semana.', 'Fresh weekly challenges assigned.'));
    });

    const dismissBeRealBtn = document.getElementById('btn-bereal-dismiss');
    if (dismissBeRealBtn) {
      dismissBeRealBtn.addEventListener('click', () => {
        state.beRealPing.active = false;
        saveState();
        renderBeRealCard();
      });
    }

    document.getElementById('btn-bereal-photo').addEventListener('click', () => {
      const goal = state.catalog.find(g => g.id === state.beRealPing.goalId) || state.catalog[0];
      state.beRealPing.active = false;
      saveState();
      renderBeRealCard();
      if (goal) {
        completeGoal(goal, { allowPhoto: false });
      }
      openCameraModal();
    });
    document.getElementById('btn-bereal-done').addEventListener('click', () => {
      const goal = state.catalog.find(g => g.id === state.beRealPing.goalId) || state.catalog[0];
      state.beRealPing.active = false;
      saveState();
      completeGoal(goal, { allowPhoto: true });
    });

    const enableNotifsBtn = document.getElementById('btn-enable-notifs');
    if (enableNotifsBtn) {
      enableNotifsBtn.addEventListener('click', () => {
        if (window.SplashNative && typeof window.SplashNative.requestNotificationPermission === 'function') {
          const granted = window.SplashNative.hasNotificationPermission && window.SplashNative.hasNotificationPermission();
          if (granted) {
            window.SplashNative.sendNotification(
              'Splash',
              tx('Las notificaciones están activas en tu móvil.', 'Notifications are active on your phone.')
            );
            showToast('notifications_active', tx('Notificaciones ya permitidas. Te enviamos una prueba.', 'Notifications already enabled. Test sent.'));
          } else {
            window.SplashNative.requestNotificationPermission();
            syncAndroidNotificationSchedule();
            showToast('notifications', tx('Acepta el permiso de notificaciones en Android.', 'Allow the Android notification permission prompt.'));
          }
          return;
        }
        if ('Notification' in window) {
          Notification.requestPermission().then((perm) => {
            showToast(
              perm === 'granted' ? 'notifications_active' : 'notifications_off',
              perm === 'granted'
                ? tx('Notificaciones activadas.', 'Notifications enabled.')
                : tx('Permiso de notificaciones denegado.', 'Notification permission denied.')
            );
          }).catch(() => {});
        } else {
          showToast('info', tx('Disponible en la app de Android.', 'Available in the Android app.'));
        }
      });
    }

    const pinWidgetBtn = document.getElementById('btn-pin-android-widget');
    if (pinWidgetBtn) {
      pinWidgetBtn.addEventListener('click', () => {
        syncAndroidWidget();
        if (window.SplashNative && typeof window.SplashNative.pinWidget === 'function') {
          const ok = window.SplashNative.pinWidget(widgetStudio.type || '4x2');
          showToast(
            'widgets',
            ok
              ? tx('Confirma en Android para añadir el widget de Splash a tu inicio.', 'Confirm in Android to add the Splash widget to your home screen.')
              : tx('Mantén pulsada tu pantalla de inicio > Widgets > Splash.', 'Long-press your home screen > Widgets > Splash.')
          );
        } else {
          showToast('widgets', tx('En Android: mantén pulsada la pantalla de inicio > Widgets > Splash.', 'On Android: long-press home screen > Widgets > Splash.'));
        }
      });
    }

    document.getElementById('toast-photo-btn').addEventListener('click', () => {
      document.getElementById('cozy-toast').classList.add('hidden');
      openCameraModal();
    });

    document.getElementById('timer-minus').addEventListener('click', () => {
      activeTimer.totalSeconds = Math.max(5 * 60, activeTimer.totalSeconds - 5 * 60);
      activeTimer.remainingSeconds = activeTimer.totalSeconds;
      updateTimerUI();
    });

    document.getElementById('timer-plus').addEventListener('click', () => {
      activeTimer.totalSeconds = Math.min(30 * 60, activeTimer.totalSeconds + 5 * 60);
      activeTimer.remainingSeconds = activeTimer.totalSeconds;
      updateTimerUI();
    });

    document.getElementById('btn-timer-toggle').addEventListener('click', () => {
      const btn = document.getElementById('btn-timer-toggle');
      if (activeTimer.running) {
        clearInterval(activeTimer.intervalId);
        activeTimer.running = false;
        btn.textContent = tx('Reanudar', 'Resume');
      } else {
        activeTimer.running = true;
        btn.textContent = tx('Pausar', 'Pause');
        activeTimer.intervalId = setInterval(() => {
          if (activeTimer.remainingSeconds > 0) {
            activeTimer.remainingSeconds--;
            updateTimerUI();
          } else {
            const g = activeTimer.goal;
            closeTimerModal();
            completeGoal(g);
          }
        }, 1000);
      }
    });

    document.getElementById('btn-timer-complete').addEventListener('click', () => {
      const g = activeTimer.goal;
      closeTimerModal();
      completeGoal(g);
    });

    document.querySelectorAll('.social-link-btn').forEach(link => {
      link.addEventListener('click', () => {
        activeTimer.leftForSocial = true;
      });
    });

    const analogSoundBtn = document.getElementById('btn-analog-sound');
    if (analogSoundBtn) {
      analogSoundBtn.addEventListener('click', () => {
        const activeId = getActiveBiomeId();
        if (window.BroteAudio.getMode() === `biome:${activeId}`) {
          window.BroteAudio.stopAmbient();
        } else {
          window.BroteAudio.startBiomeAmbient(activeId);
        }
        updateAnalogSoundBtn();
      });
    }

    const analogToggleBtn = document.getElementById('btn-analog-toggle');
    if (analogToggleBtn) {
      analogToggleBtn.addEventListener('click', () => {
        if (!activeAnalog.countdownMode) return;
        if (activeAnalog.running) {
          clearInterval(activeAnalog.intervalId);
          activeAnalog.running = false;
          analogToggleBtn.textContent = tx('Reanudar', 'Resume');
          document.getElementById('analog-status').textContent = tx(
            'Temporizador en pausa. Retoma cuando quieras.',
            'Timer paused. Resume anytime.'
          );
        } else {
          if (activeAnalog.remainingSeconds <= 0) return;
          activeAnalog.running = true;
          analogToggleBtn.textContent = tx('Pausar', 'Pause');
          document.getElementById('analog-status').textContent = tx(
            'Temporizador activo. Deja el móvil boca abajo y desconecta.',
            'Timer active. Place your phone face down and unplug.'
          );
          clearInterval(activeAnalog.intervalId);
          activeAnalog.intervalId = setInterval(() => {
            if (activeAnalog.pausedByLeave || !activeAnalog.running) return;
            if (activeAnalog.remainingSeconds > 0) {
              activeAnalog.remainingSeconds--;
              updateAnalogTimerUI();
            }
            if (activeAnalog.remainingSeconds <= 0) {
              clearInterval(activeAnalog.intervalId);
              activeAnalog.running = false;
              analogToggleBtn.textContent = tx('Completado', 'Completed');
              document.getElementById('analog-status').textContent = tx(
                '¡Tiempo completado! Puedes volver cuando quieras.',
                'Time completed! Come back whenever you are ready.'
              );
              window.BroteAudio.playBiomeChime(getActiveBiomeId());
              showToast('timer', tx('Temporizador completado.', 'Timer completed.'));
            }
          }, 1000);
        }
      });
    }

    document.getElementById('btn-analog-complete').addEventListener('click', () => {
      if (activeAnalog.countdownMode) {
        closeAnalogModal();
        return;
      }
      const g = activeAnalog.goal;
      closeAnalogModal();
      if (g) completeGoal(g);
    });

    document.getElementById('btn-take-snap').addEventListener('click', () => {
      const video = document.getElementById('camera-video');
      capturedDataUrl = compressImageToDataUrl(video, video.videoWidth || 640, video.videoHeight || 480);
      const img = document.getElementById('camera-captured-img');
      img.src = capturedDataUrl;
      video.classList.add('hidden');
      img.classList.remove('hidden');
      document.getElementById('btn-take-snap').classList.add('hidden');
      document.getElementById('btn-save-snap').classList.remove('hidden');
    });

    document.getElementById('camera-file-input').addEventListener('change', (e) => {
      const file = e.target.files[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = (ev) => {
        const tempImg = new Image();
        tempImg.onload = () => {
          capturedDataUrl = compressImageToDataUrl(tempImg, tempImg.width, tempImg.height);
          const img = document.getElementById('camera-captured-img');
          img.src = capturedDataUrl;
          document.getElementById('camera-fallback').classList.add('hidden');
          img.classList.remove('hidden');
          document.getElementById('btn-save-snap').classList.remove('hidden');
        };
        tempImg.src = ev.target.result;
      };
      reader.readAsDataURL(file);
    });

    document.getElementById('btn-save-snap').addEventListener('click', () => {
      if (capturedDataUrl) {
        savePhotoToToday(capturedDataUrl);
      }
      closeCameraModal();
    });

    document.getElementById('cal-prev').addEventListener('click', () => {
      currentCalendarDate.setMonth(currentCalendarDate.getMonth() - 1);
      renderCalendar();
    });
    document.getElementById('cal-next').addEventListener('click', () => {
      currentCalendarDate.setMonth(currentCalendarDate.getMonth() + 1);
      renderCalendar();
    });

    document.getElementById('catalog-search').addEventListener('input', (e) => {
      catalogQuery = e.target.value;
      renderCatalog();
    });

    document.getElementById('catalog-filters').addEventListener('click', (e) => {
      const pill = e.target.closest('.pill');
      if (!pill) return;
      document.querySelectorAll('#catalog-filters .pill').forEach(p => p.classList.remove('active'));
      pill.classList.add('active');
      catalogFilter = pill.dataset.filter;
      renderCatalog();
    });

    document.getElementById('btn-open-create').addEventListener('click', () => {
      document.getElementById('modal-create').classList.remove('hidden');
    });

    document.getElementById('create-goal-form').addEventListener('submit', (e) => {
      e.preventDefault();
      const titulo = document.getElementById('new-goal-title').value.trim();
      const categoria = document.getElementById('new-goal-cat').value;
      const icono = 'eco';
      const subcategoria = document.getElementById('new-goal-subcat').value.trim() || 'Personal';

      const tipoVerificacion =
        categoria === 'RAPIDO' ? 'TOQUE' :
        categoria === 'MEDIO' ? 'TEMPORIZADOR' : 'PUENTE_ANALOGICO';

      const newGoal = {
        id: Date.now(),
        tituloCorto: titulo.length > 26 ? `${titulo.slice(0, 24)}…` : titulo,
        titulo,
        categoria,
        subcategoria,
        icono,
        esFavorito: true,
        estaOculto: false,
        tipoVerificacion
      };
      state.catalog.unshift(newGoal);
      saveState();
      renderCatalog();
      e.target.reset();
      document.getElementById('modal-create').classList.add('hidden');
      showToast(icono, tx('Objetivo añadido a tu catálogo.', 'Goal added to your catalog.'));
    });

    document.getElementById('btn-simulator').addEventListener('click', () => {
      renderUserProfile();
      applyThemeMode();
      applyBetaMode();
      if (window.SplashI18n) window.SplashI18n.applyDom();
      renderNotifSchedule();
      document.getElementById('modal-simulator').classList.remove('hidden');
    });

    const langTabs = document.getElementById('lang-mode-tabs');
    if (langTabs) {
      langTabs.addEventListener('click', (e) => {
        const btn = e.target.closest('.lang-mode-btn');
        if (!btn) return;
        const nextLang = btn.dataset.langMode === 'en' ? 'en' : 'es';
        const curLang = (window.SplashI18n && window.SplashI18n.getLang()) || ((state.ui && state.ui.lang) || 'es');
        if (nextLang === curLang) return;

        if (!state.ui || typeof state.ui !== 'object') {
          state.ui = { biomesViewMode: 'grid', themeMode: 'system', betaMode: true, lang: nextLang };
        } else {
          state.ui.lang = nextLang;
        }

        const overlay = document.getElementById('m3e-lang-overlay');
        const label = document.getElementById('m3e-lang-label');
        if (label) {
          label.textContent = nextLang === 'en' ? 'Switching to English...' : 'Cambiando a Español...';
        }
        if (overlay) {
          overlay.classList.remove('hidden');
          void overlay.offsetWidth;
          overlay.classList.add('is-visible');
        }

        setTimeout(() => {
          syncAllModulesLanguage(nextLang);
          saveState();
          renderAll(false);
          renderNotifSchedule();
          if (viewer3D.open) updateViewerUI();
          requestAnimationFrame(() => updateNavSlider(currentTabId));

          setTimeout(() => {
            if (overlay) {
              overlay.classList.remove('is-visible');
              setTimeout(() => overlay.classList.add('hidden'), 240);
            }
            showToast('translate', nextLang === 'en' ? 'Language switched to English.' : 'Idioma cambiado a Español.');
          }, 340);
        }, 220);
      });
    }

    const toggleBetaBtn = document.getElementById('btn-toggle-beta');
    if (toggleBetaBtn) {
      toggleBetaBtn.addEventListener('click', () => {
        if (!state.ui || typeof state.ui !== 'object') {
          state.ui = { biomesViewMode: 'grid', themeMode: 'system', betaMode: true, lang: 'es' };
        }
        const nextBeta = !(state.ui.betaMode !== false);
        state.ui.betaMode = nextBeta;
        if (!nextBeta && state.lumi) {
          state.lumi.betaPointsOverride = null;
          const lockPreview = document.getElementById('lockscreen-preview');
          if (lockPreview) lockPreview.classList.add('hidden');
        }
        saveState();
        renderAll(false);
        if (viewer3D.open) updateViewerUI();
        showToast('tune', nextBeta ? tx('Modo Beta activado.', 'Beta Mode enabled.') : tx('Modo Beta desactivado.', 'Beta Mode disabled.'));
      });
    }

    const themeTabs = document.getElementById('theme-mode-tabs');
    if (themeTabs) {
      themeTabs.addEventListener('click', (e) => {
        const btn = e.target.closest('.theme-mode-btn');
        if (!btn) return;
        const mode = btn.dataset.themeMode;
        if (!['system', 'light', 'dark'].includes(mode)) return;
        if (!state.ui || typeof state.ui !== 'object') {
          state.ui = { biomesViewMode: 'grid', themeMode: mode };
        } else {
          state.ui.themeMode = mode;
        }
        saveState();
        applyThemeMode();
      });
    }

    if (systemThemeMQ) {
      const onSystemThemeChange = () => {
        if (!state.ui || state.ui.themeMode === 'system') {
          applyThemeMode();
        }
      };
      if (typeof systemThemeMQ.addEventListener === 'function') {
        systemThemeMQ.addEventListener('change', onSystemThemeChange);
      } else if (typeof systemThemeMQ.addListener === 'function') {
        systemThemeMQ.addListener(onSystemThemeChange);
      }
    }

    function saveUserProfileName(showFeedback = false) {
      const inp = document.getElementById('user-name-input');
      if (!inp) return;
      if (!state.user || typeof state.user !== 'object') state.user = { name: '' };
      const cleaned = inp.value.trim().slice(0, 28);
      if (state.user.name === cleaned && !showFeedback) return;
      state.user.name = cleaned;
      saveState();
      renderUserProfile();
      if (showFeedback) {
        showToast('check_circle', cleaned ? tx(`Nombre guardado: ${cleaned}`, `Name saved: ${cleaned}`) : tx('Perfil actualizado.', 'Profile updated.'));
      }
    }

    const saveNameBtn = document.getElementById('btn-save-user-name');
    const nameInput = document.getElementById('user-name-input');
    if (saveNameBtn) {
      saveNameBtn.addEventListener('click', () => saveUserProfileName(true));
    }
    if (nameInput) {
      nameInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          saveUserProfileName(true);
          nameInput.blur();
        }
      });
      nameInput.addEventListener('blur', () => saveUserProfileName(false));
    }

    document.querySelectorAll('.sched-mode-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        schedMode = btn.dataset.schedMode || 'default';
        renderNotifSchedule();
      });
    });

    document.getElementById('sched-week-prev').addEventListener('click', () => {
      schedWeekOffset = Math.max(-4, schedWeekOffset - 1);
      renderNotifSchedule();
    });

    document.getElementById('sched-week-next').addEventListener('click', () => {
      schedWeekOffset = Math.min(12, schedWeekOffset + 1);
      renderNotifSchedule();
    });

    document.getElementById('sched-week-reset').addEventListener('click', () => {
      const wk = getScheduleWeekKey(schedWeekOffset);
      if (state.notifSchedule.weekOverrides && state.notifSchedule.weekOverrides[wk]) {
        delete state.notifSchedule.weekOverrides[wk];
        saveState();
        renderNotifSchedule();
        showToast('event', tx('Esta semana vuelve a usar tu horario predeterminado.', 'This week uses your default schedule again.'));
      }
    });

    const schedVisual = document.getElementById('notif-schedule-visual');
    function updateHourPillDOM(pill, isOn) {
      pill.classList.toggle('is-on', isOn);
      pill.setAttribute('aria-pressed', isOn ? 'true' : 'false');
    }

    schedVisual.addEventListener('pointerdown', (e) => {
      const hourPill = e.target.closest('.sched-hour-pill');
      if (!hourPill) return;
      e.preventDefault();
      const targetSched = getEditableWeekSchedule();
      const cfg = targetSched[schedSelectedDay];
      const h = Number(hourPill.dataset.hour);
      const currentlyOn = cfg.enabled && cfg.hours.includes(h);
      schedDragTargetState = !currentlyOn;
      if (schedDragTargetState) {
        cfg.enabled = true;
        cfg.hours = normalizeHoursList(cfg.hours.concat([h]));
      } else {
        cfg.hours = cfg.hours.filter(x => x !== h);
      }
      updateHourPillDOM(hourPill, schedDragTargetState);
    });

    schedVisual.addEventListener('pointermove', (e) => {
      if (schedDragTargetState === null) return;
      const el = document.elementFromPoint(e.clientX, e.clientY);
      const hourPill = el && el.closest && el.closest('.sched-hour-pill');
      if (!hourPill) return;
      const targetSched = getEditableWeekSchedule();
      const cfg = targetSched[schedSelectedDay];
      const h = Number(hourPill.dataset.hour);
      if (schedDragTargetState) {
        if (!cfg.hours.includes(h) || !cfg.enabled) {
          cfg.enabled = true;
          cfg.hours = normalizeHoursList(cfg.hours.concat([h]));
          updateHourPillDOM(hourPill, true);
        }
      } else if (cfg.hours.includes(h)) {
        cfg.hours = cfg.hours.filter(x => x !== h);
        updateHourPillDOM(hourPill, false);
      }
    });

    function finishHourDrag() {
      if (schedDragTargetState === null) return;
      schedDragTargetState = null;
      saveState();
      renderNotifSchedule();
    }
    window.addEventListener('pointerup', finishHourDrag);
    window.addEventListener('pointercancel', finishHourDrag);

    schedVisual.addEventListener('click', (e) => {
      const selBtn = e.target.closest('[data-select-day]');
      if (selBtn) {
        schedSelectedDay = selBtn.dataset.selectDay;
        renderNotifSchedule();
        return;
      }

      const toggleBtn = e.target.closest('[data-toggle-day]');
      if (toggleBtn) {
        const day = toggleBtn.dataset.toggleDay;
        const targetSched = getEditableWeekSchedule();
        targetSched[day].enabled = !targetSched[day].enabled;
        saveState();
        renderNotifSchedule();
        return;
      }

      const bandBtn = e.target.closest('[data-toggle-band]');
      if (bandBtn) {
        const band = SCHED_BANDS.find(b => b.key === bandBtn.dataset.toggleBand);
        if (!band) return;
        const targetSched = getEditableWeekSchedule();
        const cfg = targetSched[schedSelectedDay];
        const allOn = cfg.enabled && band.hours.every(h => cfg.hours.includes(h));
        if (allOn) {
          cfg.hours = cfg.hours.filter(h => !band.hours.includes(h));
        } else {
          cfg.enabled = true;
          cfg.hours = normalizeHoursList(cfg.hours.concat(band.hours));
        }
        saveState();
        renderNotifSchedule();
        return;
      }

      const copyBtn = e.target.closest('[data-copy-weekdays]');
      if (copyBtn) {
        const srcDay = copyBtn.dataset.copyWeekdays;
        const targetSched = getEditableWeekSchedule();
        const src = targetSched[srcDay];
        ['mon', 'tue', 'wed', 'thu', 'fri'].forEach(k => {
          targetSched[k] = { enabled: src.enabled, hours: src.hours.slice() };
        });
        saveState();
        renderNotifSchedule();
        showToast('event', tx('Horario aplicado de lunes a viernes.', 'Schedule applied Monday through Friday.'));
      }
    });

    document.getElementById('sched-presets').addEventListener('click', (e) => {
      const chip = e.target.closest('.sched-preset-chip');
      if (!chip) return;
      const preset = chip.dataset.preset;
      const targetSched = getEditableWeekSchedule();
      const def = createDefaultWeekSchedule();
      const workHours = [9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20];
      SCHED_DAYS.forEach(d => {
        const isWeekday = d.key !== 'sat' && d.key !== 'sun';
        if (preset === 'default') {
          targetSched[d.key] = { enabled: def[d.key].enabled, hours: def[d.key].hours.slice() };
        } else if (preset === 'weekdays') {
          targetSched[d.key] = { enabled: isWeekday, hours: workHours.slice() };
        } else if (preset === 'all') {
          targetSched[d.key] = { enabled: true, hours: workHours.slice() };
        } else if (preset === 'mute') {
          targetSched[d.key] = { enabled: false, hours: targetSched[d.key].hours.slice() };
        }
      });
      saveState();
      renderNotifSchedule();
    });

    document.getElementById('sim-bereal').addEventListener('click', () => {
      const statusNow = getNotificationStatusNow();
      const [id] = pickWeightedGoals('RAPIDO', 1);
      const activeBiome = getActiveBiomeId();
      const notifMeta = BIOME_NOTIFS[activeBiome] || BIOME_NOTIFS.valle;
      state.beRealPing = {
        active: true,
        category: 'RAPIDO',
        goalId: id || 5,
        expiresAt: Date.now() + QUICK_PING_MS
      };
      saveState();
      renderBeRealCard();
      document.getElementById('modal-simulator').classList.add('hidden');
      if (statusNow.allowed) {
        window.BroteAudio.playBiomeChime(activeBiome);
        const pingGoal = state.catalog.find(g => g.id === state.beRealPing.goalId);
        sendNativeNotification(
          `${notifMeta.quickBadge} · 20m`,
          pingGoal ? pingGoal.titulo : tx('Tienes 20 min para completar tu reto rápido.', 'You have 20 min to complete your quick goal.')
        );
      }
      showToast(
        statusNow.allowed ? 'bolt' : 'notifications_off',
        statusNow.allowed
          ? tx(`${notifMeta.quickBadge} · Tienes 20 min para completarlo.`, `${notifMeta.quickBadge} · You have 20 min to complete it.`)
          : tx('Simulado en Beta (silenciado a esta hora según tu horario).', 'Simulated in Beta (muted this hour per your schedule).')
      );
    });

    document.getElementById('sim-medio-ping').addEventListener('click', () => {
      const [id] = pickWeightedGoals('MEDIO', 1);
      const activeBiome = getActiveBiomeId();
      const notifMeta = BIOME_NOTIFS[activeBiome] || BIOME_NOTIFS.valle;
      state.beRealPing = {
        active: true,
        category: 'MEDIO',
        goalId: id || 17,
        expiresAt: Date.now() + MEDIO_PING_MS
      };
      saveState();
      renderBeRealCard();
      document.getElementById('modal-simulator').classList.add('hidden');
      window.BroteAudio.playBiomeChime(activeBiome);
      const medioGoal = state.catalog.find(g => g.id === state.beRealPing.goalId);
      sendNativeNotification(
        `${notifMeta.medioBadge} · 4h`,
        medioGoal ? medioGoal.titulo : tx('Ventana de 4 horas para tu reto medio.', '4-hour window for your medium goal.')
      );
      showToast('schedule', `${notifMeta.medioBadge} · ${tx('Ventana de 4 horas.', '4-hour window.')}`);
    });

    document.getElementById('sim-max-biome').addEventListener('click', () => {
      if (!state.lumi.biomePoints || typeof state.lumi.biomePoints !== 'object') {
        state.lumi.biomePoints = {};
      }
      const targetId = getCurrentProgressBiomeId();
      const maxPts = window.BroteMascot ? window.BroteMascot.MAX_POINTS : 80;
      state.lumi.biomePoints[targetId] = maxPts;
      state.lumi.betaPointsOverride = null;
      state.lumi.lastAwakeDate = getTodayKey();
      const nextId = getCurrentProgressBiomeId();
      const completedMeta = window.BroteMascot.getBiome(targetId);
      let toastMsg;
      if (nextId && nextId !== targetId) {
        state.lumi.biome = nextId;
        refreshBiomeSuggestionsOnSwitch();
        const nextMeta = window.BroteMascot.getBiome(nextId);
        toastMsg = tx(
          `${completedMeta.shortName} completado (Nv. 8). Pasas a ${nextMeta.name} y se despejan las nubes.`,
          `${completedMeta.shortName} completed (Lvl. 8). Moving to ${nextMeta.name} — clouds cleared.`
        );
      } else {
        state.lumi.biome = targetId;
        toastMsg = tx(
          '¡Todos los biomas completados al máximo (Nv. 8)!',
          'All biomes completed at max level (Lvl. 8)!'
        );
      }
      saveState();
      renderAll(true);
      document.getElementById('modal-simulator').classList.add('hidden');
      window.BroteMascot.spawnParticles(45, ['#FFD573', '#1F7A4C', '#E08600']);
      showToast('workspace_premium', toastMsg);
    });

    document.getElementById('sim-antiscroll').addEventListener('click', () => {
      const preview = document.getElementById('lockscreen-preview');
      const pEl = preview && preview.querySelector('p');
      if (pEl) {
        const notifMeta = BIOME_NOTIFS[getActiveBiomeId()] || BIOME_NOTIFS.valle;
        pEl.textContent = notifMeta.rescueCopy;
      }
      preview.classList.toggle('hidden');
      if (!preview.classList.contains('hidden')) {
        const notifMeta = BIOME_NOTIFS[getActiveBiomeId()] || BIOME_NOTIFS.valle;
        sendNativeNotification(tx('Splash · Rescate Anti-Scroll', 'Splash · Anti-Scroll Rescue'), notifMeta.rescueCopy);
      }
    });

    document.querySelectorAll('.lock-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const cat = btn.dataset.rescue;
        document.getElementById('modal-simulator').classList.add('hidden');
        const [id] = pickWeightedGoals(cat, 1);
        const goal = state.catalog.find(g => g.id === id);
        if (goal) handleGoalTap(goal);
      });
    });

    document.getElementById('sim-thursday').addEventListener('click', () => {
      document.getElementById('modal-simulator').classList.add('hidden');
      showToast('event', tx(
        'Jueves sereno: ¿te apetece elegir un reto largo para el finde?',
        'Calm Thursday: want to pick a weekend long challenge?'
      ));
    });

    const resetBtn = document.getElementById('sim-reset-progress');
    if (resetBtn) {
      let resetArmedTimer = null;
      resetBtn.addEventListener('click', () => {
        if (resetBtn.dataset.armed !== '1') {
          resetBtn.dataset.armed = '1';
          const titleEl = resetBtn.querySelector('strong');
          const subEl = resetBtn.querySelector('small');
          if (titleEl) titleEl.textContent = tx('¿Seguro? Toca otra vez para reiniciar', 'Sure? Tap again to reset');
          if (subEl) subEl.textContent = tx('Volverá a 0 puntos, 0 días y sin nombre', 'Resets to 0 pts, 0 days & clears name');
          clearTimeout(resetArmedTimer);
          resetArmedTimer = setTimeout(() => {
            delete resetBtn.dataset.armed;
            if (window.SplashI18n) window.SplashI18n.applyDom();
          }, 3500);
          return;
        }
        clearTimeout(resetArmedTimer);
        delete resetBtn.dataset.armed;
        const keepLang = (state.ui && state.ui.lang) || (window.SplashI18n && window.SplashI18n.getLang()) || 'es';
        const keepTheme = (state.ui && state.ui.themeMode) || 'system';
        try { localStorage.removeItem(STORAGE_KEY); } catch (_) {}
        state = createInitialState();
        state.ui.lang = keepLang;
        state.ui.themeMode = keepTheme;
        if (window.BroteSocial && typeof window.BroteSocial.resetSocial === 'function') {
          window.BroteSocial.resetSocial(keepLang);
        }
        syncAllModulesLanguage(keepLang);
        ensureWeeklyAndDaily();
        saveState();
        renderAll(false);
        if (viewer3D.open) updateViewerUI();
        document.getElementById('modal-simulator').classList.add('hidden');
        showToast('restart_alt', tx('Progreso reiniciado desde 0.', 'Progress reset from 0.'));
      });
    }

    const openWidgetsBtn = document.getElementById('btn-open-widgets');
    if (openWidgetsBtn) {
      openWidgetsBtn.addEventListener('click', () => {
        document.getElementById('modal-simulator').classList.add('hidden');
        openWidgetsModal();
      });
    }

    const feedbackBtn = document.getElementById('btn-send-feedback');
    if (feedbackBtn) {
      feedbackBtn.addEventListener('click', () => {
        if (FEEDBACK_FORM_URL && /^https?:\/\//i.test(FEEDBACK_FORM_URL)) {
          window.open(FEEDBACK_FORM_URL, '_blank', 'noopener');
        } else {
          showToast('rate_review', tx(
            'Configura FEEDBACK_FORM_URL en js/app.js con el enlace de tu Google Form.',
            'Set FEEDBACK_FORM_URL in js/app.js with your Google Form link.'
          ));
        }
      });
    }

    const widgetTypeTabs = document.getElementById('widget-type-tabs');
    if (widgetTypeTabs) {
      widgetTypeTabs.addEventListener('click', (e) => {
        const btn = e.target.closest('.widget-tab-btn');
        if (!btn) return;
        widgetStudio.type = btn.dataset.wtype || '4x2';
        renderWidgetStudio();
      });
    }

    const widgetTimeTabs = document.getElementById('widget-time-tabs');
    if (widgetTimeTabs) {
      widgetTimeTabs.addEventListener('click', (e) => {
        const btn = e.target.closest('.widget-time-pill');
        if (!btn) return;
        widgetStudio.timeMode = btn.dataset.wtime || 'auto';
        renderWidgetStudio();
      });
    }

    const widgetThemeTabs = document.getElementById('widget-theme-tabs');
    if (widgetThemeTabs) {
      widgetThemeTabs.addEventListener('click', (e) => {
        const btn = e.target.closest('.widget-time-pill');
        if (!btn) return;
        widgetStudio.themeMode = btn.dataset.wtheme || 'auto';
        renderWidgetStudio();
      });
    }
  }

  function updatePixelPreviewScale() {
    const scale = window.innerWidth >= 480
      ? Math.min(1, (window.innerWidth - 28) / 436, (window.innerHeight - 28) / 939)
      : 1;
    document.documentElement.style.setProperty('--pixel-scale', scale.toFixed(4));
  }

  function init() {
    updatePixelPreviewScale();
    const initLang = (state.ui && (state.ui.lang === 'en' || state.ui.lang === 'es'))
      ? state.ui.lang
      : ((window.SplashI18n && window.SplashI18n.getLang()) || 'es');
    syncAllModulesLanguage(initLang);
    ensureWeeklyAndDaily();
    window.BroteMascot.initDiorama(document.getElementById('mascot-diorama'));
    window.BroteMascot.initParticles(document.getElementById('mascot-particles'));
    window.addEventListener('resize', () => {
      updatePixelPreviewScale();
      window.BroteMascot.resizeCanvas();
      renderMascot(false, true);
      const biomesTab = document.getElementById('tab-biomas');
      if (biomesTab && biomesTab.classList.contains('active')) {
        renderBiomesStackCanvases();
      }
    });
    bindEvents();
    renderAll(false);
    setTimeout(() => {
      if (catalogDirty) renderCatalog();
    }, 350);

    if (beRealIntervalId) clearInterval(beRealIntervalId);
    beRealIntervalId = setInterval(updateBeRealCountdown, 1000);

    window.addEventListener('splash-widget-timer', (e) => {
      const m = e && e.detail ? Number(e.detail.minutes) : 0;
      if (m > 0) openWidgetOledTimer(m);
    });
    try {
      if (window.SplashNative && typeof window.SplashNative.consumePendingTimerMinutes === 'function') {
        const pendingMin = Number(window.SplashNative.consumePendingTimerMinutes());
        if (pendingMin > 0) {
          openWidgetOledTimer(pendingMin);
        }
      }
    } catch (_) {}
  }

  document.addEventListener('DOMContentLoaded', init);
})();
