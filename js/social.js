/**
 * Multi-Circle Inner Groups & Shared World Map for Splash
 * - Independent circles (Friends, Family, Partner...) with switcher
 * - Global biome & progression shared across all circles
 * - BeReal-style mechanic: complete 1 goal today to reveal friends' photos & goals
 */
window.BroteSocial = (function () {
  const SOCIAL_STORAGE_KEY = 'brote_social_v2';
  const MAX_FRIENDS = 8;
  const MAX_CIRCLES = 5;

  const PRESET_SCENES = [
    'coast_sunset',
    'window_coffee',
    'tea_book',
    'green_trail',
    'sketchbook',
    'dawn_horizon'
  ];

  function createInviteCode() {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    let code = 'SPLASH-';
    for (let i = 0; i < 4; i++) {
      code += chars[Math.floor(Math.random() * chars.length)];
    }
    return code;
  }

  const DEFAULT_CIRCLES = [
    {
      id: 'c_amigos',
      name: 'Friends',
      inviteCode: 'SPLASH-A7M2',
      friends: [
        {
          id: 'f_lucia',
          name: 'Alex',
          initial: 'A',
          color: '#D97757',
          biome: 'costa',
          level: 5,
          weather: 'radiant',
          todayGoal: 'Watch the sunset phone-free',
          timeAgo: '22m ago',
          sceneType: 'coast_sunset',
          hasPostedToday: true,
          userReaction: null,
          cheeredToday: false
        },
        {
          id: 'f_marcos',
          name: 'Sam',
          initial: 'S',
          color: '#1F7A4C',
          biome: 'valle',
          level: 4,
          weather: 'clear',
          todayGoal: '40-min forest trail walk',
          timeAgo: '1h ago',
          sceneType: 'green_trail',
          hasPostedToday: true,
          userReaction: null,
          cheeredToday: false
        },
        {
          id: 'f_clara',
          name: 'Leo',
          initial: 'L',
          color: '#B55B7E',
          biome: 'kioto',
          level: 6,
          weather: 'radiant',
          todayGoal: 'Silent tea before reading',
          timeAgo: '2h ago',
          sceneType: 'tea_book',
          hasPostedToday: true,
          userReaction: null,
          cheeredToday: false
        },
        {
          id: 'f_hugo',
          name: 'Maya',
          initial: 'M',
          color: '#2E6943',
          biome: 'jungla',
          level: 3,
          weather: 'mist',
          todayGoal: '15-min hand sketching',
          timeAgo: 'Yesterday',
          sceneType: 'sketchbook',
          hasPostedToday: false,
          userReaction: null,
          cheeredToday: false
        }
      ]
    },
    {
      id: 'c_familia',
      name: 'Family',
      inviteCode: 'SPLASH-F4K9',
      friends: [
        {
          id: 'f_sofia',
          name: 'Noa',
          initial: 'N',
          color: '#C7522A',
          biome: 'otono',
          level: 4,
          weather: 'clear',
          todayGoal: 'Face-to-face coffee, phones away',
          timeAgo: '3h ago',
          sceneType: 'window_coffee',
          hasPostedToday: true,
          userReaction: null,
          cheeredToday: false
        },
        {
          id: 'f_dani',
          name: 'Kai',
          initial: 'K',
          color: '#0E8599',
          biome: 'arrozales',
          level: 2,
          weather: 'clear',
          todayGoal: 'Airbender & deep breathing',
          timeAgo: '4h ago',
          sceneType: 'dawn_horizon',
          hasPostedToday: true,
          userReaction: null,
          cheeredToday: false
        }
      ]
    },
    {
      id: 'c_pareja',
      name: 'Partner',
      inviteCode: 'SPLASH-P8X3',
      friends: [
        {
          id: 'f_elena',
          name: 'Nora',
          initial: 'N',
          color: '#C96D4B',
          biome: 'toscana',
          level: 5,
          weather: 'radiant',
          todayGoal: 'Phones-off table talk',
          timeAgo: '35m ago',
          sceneType: 'tea_book',
          hasPostedToday: true,
          userReaction: null,
          cheeredToday: false
        }
      ]
    }
  ];

  const snapshotCache = {};

  function tx(esVal, enVal) {
    return window.SplashI18n ? window.SplashI18n.pick(esVal, enVal) : esVal;
  }

  const SOCIAL_I18N = {
    circles: {
      c_amigos: { es: 'Amigos', en: 'Friends' },
      c_familia: { es: 'Familia', en: 'Family' },
      c_pareja: { es: 'Pareja', en: 'Partner' }
    },
    friends: {
      f_lucia:  { es: ['Alex', 'Ver el atardecer sin móvil', 'Hace 22 min'], en: ['Alex', 'Watch the sunset phone-free', '22m ago'] },
      f_marcos: { es: ['Sam', 'Caminata larga de 40 min', 'Hace 1 h'], en: ['Sam', '40-min forest trail walk', '1h ago'] },
      f_clara:  { es: ['Leo', 'Té en silencio antes de leer', 'Hace 2 h'], en: ['Leo', 'Silent tea before reading', '2h ago'] },
      f_hugo:   { es: ['Maya', 'Dibujar a mano 15 min', 'Ayer'], en: ['Maya', '15-min hand sketching', 'Yesterday'] },
      f_sofia:  { es: ['Noa', 'Café cara a cara sin móviles', 'Hace 3 h'], en: ['Noa', 'Face-to-face coffee, phones away', '3h ago'] },
      f_dani:   { es: ['Kai', 'Airbender y respira hondo', 'Hace 4 h'], en: ['Kai', 'Airbender & deep breathing', '4h ago'] },
      f_elena:  { es: ['Nora', 'Sobremesa sin móviles', 'Hace 35 min'], en: ['Nora', 'Phones-off table talk', '35m ago'] }
    }
  };

  function migrateSocialStrings(parsed, lang) {
    const useEn = lang ? lang === 'en' : !!(window.SplashI18n && window.SplashI18n.isEn());
    (parsed.circles || []).forEach(c => {
      const cPair = SOCIAL_I18N.circles[c.id];
      if (cPair && [cPair.es, cPair.en].includes(c.name)) {
        c.name = useEn ? cPair.en : cPair.es;
      }
      if (typeof c.inviteCode === 'string' && c.inviteCode.startsWith('BROTE-')) {
        c.inviteCode = c.inviteCode.replace(/^BROTE-/, 'SPLASH-');
      }
      (c.friends || []).forEach(f => {
        const fSpec = SOCIAL_I18N.friends[f.id];
        if (fSpec) {
          const pack = useEn ? fSpec.en : fSpec.es;
          f.name = pack[0];
          f.initial = pack[0][0].toUpperCase();
          f.todayGoal = pack[1];
          f.timeAgo = pack[2];
        } else {
          if (f.todayGoal === 'Respira hondo · Desconexión breve' || f.todayGoal === 'Deep Breath · Quick Screen Break') {
            f.todayGoal = useEn ? 'Deep Breath · Quick Screen Break' : 'Respira hondo · Desconexión breve';
          }
          if (f.timeAgo === 'Recién unido' || f.timeAgo === 'Just joined') {
            f.timeAgo = useEn ? 'Just joined' : 'Recién unido';
          }
        }
      });
    });
  }

  function loadSocialState() {
    try {
      const raw = localStorage.getItem(SOCIAL_STORAGE_KEY);
      if (!raw) {
        const init = {
          activeCircleId: 'c_amigos',
          circles: JSON.parse(JSON.stringify(DEFAULT_CIRCLES))
        };
        migrateSocialStrings(init);
        localStorage.setItem(SOCIAL_STORAGE_KEY, JSON.stringify(init));
        return init;
      }
      const parsed = JSON.parse(raw);
      if (!Array.isArray(parsed.circles) || !parsed.circles.length) {
        parsed.circles = JSON.parse(JSON.stringify(DEFAULT_CIRCLES));
      }
      migrateSocialStrings(parsed);
      if (!parsed.activeCircleId || !parsed.circles.some(c => c.id === parsed.activeCircleId)) {
        parsed.activeCircleId = parsed.circles[0].id;
      }
      localStorage.setItem(SOCIAL_STORAGE_KEY, JSON.stringify(parsed));
      return parsed;
    } catch (_) {
      const fallback = {
        activeCircleId: 'c_amigos',
        circles: JSON.parse(JSON.stringify(DEFAULT_CIRCLES))
      };
      migrateSocialStrings(fallback);
      return fallback;
    }
  }

  let socialState = loadSocialState();

  function saveSocialState() {
    try {
      localStorage.setItem(SOCIAL_STORAGE_KEY, JSON.stringify(socialState));
    } catch (_) {}
  }

  function getCircles() {
    return socialState.circles || [];
  }

  function getActiveCircleId() {
    return socialState.activeCircleId || (socialState.circles[0] && socialState.circles[0].id) || 'c_amigos';
  }

  function getCircleById(circleId) {
    const id = circleId || getActiveCircleId();
    return socialState.circles.find(c => c.id === id) || socialState.circles[0];
  }

  function getActiveCircle() {
    return getCircleById(getActiveCircleId());
  }

  function setActiveCircle(circleId) {
    const found = socialState.circles.find(c => c.id === circleId);
    if (!found) return false;
    socialState.activeCircleId = found.id;
    saveSocialState();
    return true;
  }

  function applyLanguage(lang) {
    migrateSocialStrings(socialState, lang);
    saveSocialState();
  }

  function createCircle(nameRaw) {
    const clean = String(nameRaw || '').trim().slice(0, 18);
    if (!clean) return { ok: false, error: tx('Escribe un nombre para el círculo.', 'Enter a name for the circle.') };
    if (socialState.circles.length >= MAX_CIRCLES) {
      return { ok: false, error: tx(`Puedes tener hasta ${MAX_CIRCLES} círculos a la vez.`, `You can have up to ${MAX_CIRCLES} circles at once.`) };
    }
    const circle = {
      id: `c_${Date.now()}`,
      name: clean,
      inviteCode: createInviteCode(),
      friends: []
    };
    socialState.circles.push(circle);
    socialState.activeCircleId = circle.id;
    saveSocialState();
    return { ok: true, circle };
  }

  function getFriends(circleId) {
    if (circleId === 'ALL') {
      const seen = new Map();
      socialState.circles.forEach(c => {
        (c.friends || []).forEach(f => {
          if (!seen.has(f.id)) {
            seen.set(f.id, { ...f, circleName: c.name, circleId: c.id });
          }
        });
      });
      return Array.from(seen.values());
    }
    const c = getCircleById(circleId);
    return (c && c.friends) || [];
  }

  function getInviteCode(circleId) {
    const c = getCircleById(circleId);
    return (c && c.inviteCode) || 'SPLASH-A7M2';
  }

  function addFriend(nameRaw, biomeId, circleId) {
    const circle = getCircleById(circleId);
    if (!circle) return { ok: false, error: tx('Círculo no encontrado.', 'Circle not found.') };
    const cleanName = String(nameRaw || '').trim().slice(0, 18);
    if (!cleanName) return { ok: false, error: tx('Escribe un nombre o alias.', 'Enter a name or nickname.') };
    if (circle.friends.length >= MAX_FRIENDS) {
      return { ok: false, error: tx(`Cada círculo tiene un máximo de ${MAX_FRIENDS} personas.`, `Each circle supports up to ${MAX_FRIENDS} people.`) };
    }
    const palette = ['#1F7A4C', '#D97757', '#0E8599', '#B55B7E', '#C7522A', '#E08600'];
    const idx = circle.friends.length;
    const friend = {
      id: `f_${Date.now()}`,
      name: cleanName,
      initial: cleanName[0].toUpperCase(),
      color: palette[idx % palette.length],
      biome: biomeId || 'valle',
      level: 1 + (idx % 5),
      weather: idx % 2 === 0 ? 'radiant' : 'clear',
      todayGoal: tx('Respira hondo · Desconexión breve', 'Deep Breath · Quick Screen Break'),
      timeAgo: tx('Recién unido', 'Just joined'),
      sceneType: PRESET_SCENES[idx % PRESET_SCENES.length],
      hasPostedToday: true,
      userReaction: null,
      cheeredToday: false
    };
    circle.friends.push(friend);
    saveSocialState();
    return { ok: true, friend };
  }

  function removeFriend(friendId, circleId) {
    const circle = getCircleById(circleId);
    if (!circle) return false;
    const prevLen = circle.friends.length;
    circle.friends = circle.friends.filter(f => f.id !== friendId);
    if (circle.friends.length !== prevLen) {
      saveSocialState();
      return true;
    }
    return false;
  }

  function findFriendEverywhere(friendId) {
    const matches = [];
    socialState.circles.forEach(c => {
      (c.friends || []).forEach(f => {
        if (f.id === friendId) matches.push(f);
      });
    });
    return matches;
  }

  function toggleReaction(friendId, reactionKey) {
    const matches = findFriendEverywhere(friendId);
    if (!matches.length) return null;
    const nextVal = matches[0].userReaction === reactionKey ? null : reactionKey;
    matches.forEach(f => { f.userReaction = nextVal; });
    saveSocialState();
    return nextVal;
  }

  function cheerFriend(friendId) {
    const matches = findFriendEverywhere(friendId);
    if (!matches.length) return false;
    matches.forEach(f => { f.cheeredToday = true; });
    saveSocialState();
    return true;
  }

  function getSceneSnapshot(sceneType) {
    const key = sceneType || 'coast_sunset';
    if (snapshotCache[key]) return snapshotCache[key];

    const c = document.createElement('canvas');
    c.width = 360;
    c.height = 220;
    const ctx = c.getContext('2d');
    if (!ctx) return '';

    if (key === 'coast_sunset') {
      const sky = ctx.createLinearGradient(0, 0, 0, 220);
      sky.addColorStop(0, '#F5A372');
      sky.addColorStop(0.55, '#FAD69B');
      sky.addColorStop(1, '#5889A0');
      ctx.fillStyle = sky;
      ctx.fillRect(0, 0, 360, 220);
      ctx.fillStyle = '#FFF3C4';
      ctx.beginPath();
      ctx.arc(210, 108, 28, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = '#3F6F87';
      ctx.fillRect(0, 138, 360, 82);
      ctx.fillStyle = 'rgba(255, 240, 190, 0.36)';
      ctx.fillRect(184, 146, 52, 6);
      ctx.fillRect(192, 160, 38, 4);
      ctx.fillStyle = '#2B363A';
      ctx.beginPath();
      ctx.moveTo(0, 150);
      ctx.lineTo(118, 124);
      ctx.lineTo(148, 150);
      ctx.closePath();
      ctx.fill();
    } else if (key === 'green_trail') {
      const bg = ctx.createLinearGradient(0, 0, 0, 220);
      bg.addColorStop(0, '#D7ECD9');
      bg.addColorStop(0.6, '#A6D1AA');
      bg.addColorStop(1, '#58875B');
      ctx.fillStyle = bg;
      ctx.fillRect(0, 0, 360, 220);
      ctx.fillStyle = '#3D6941';
      [60, 110, 255, 310].forEach((x, idx) => {
        const h = 58 + (idx % 2) * 22;
        ctx.beginPath();
        ctx.moveTo(x, 155 - h);
        ctx.lineTo(x - 28, 155);
        ctx.lineTo(x + 28, 155);
        ctx.closePath();
        ctx.fill();
      });
      ctx.fillStyle = '#E5D3B0';
      ctx.beginPath();
      ctx.moveTo(156, 152);
      ctx.lineTo(204, 152);
      ctx.lineTo(235, 220);
      ctx.lineTo(125, 220);
      ctx.closePath();
      ctx.fill();
    } else if (key === 'tea_book') {
      const bg = ctx.createLinearGradient(0, 0, 360, 220);
      bg.addColorStop(0, '#F5E7DC');
      bg.addColorStop(1, '#D8B9A6');
      ctx.fillStyle = bg;
      ctx.fillRect(0, 0, 360, 220);
      ctx.fillStyle = '#8C6247';
      ctx.fillRect(0, 155, 360, 65);
      ctx.fillStyle = '#FFFDF9';
      ctx.fillRect(72, 126, 112, 52);
      ctx.strokeStyle = '#DDD1C3';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.moveTo(128, 126);
      ctx.lineTo(128, 178);
      ctx.stroke();
      ctx.fillStyle = '#FFFFFF';
      ctx.beginPath();
      ctx.roundRect(226, 120, 44, 44, 10);
      ctx.fill();
      ctx.strokeStyle = 'rgba(255,255,255,0.65)';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.moveTo(242, 106);
      ctx.quadraticCurveTo(234, 92, 246, 78);
      ctx.stroke();
    } else if (key === 'sketchbook') {
      ctx.fillStyle = '#EFECE4';
      ctx.fillRect(0, 0, 360, 220);
      ctx.fillStyle = '#FFFFFF';
      ctx.fillRect(66, 26, 228, 168);
      ctx.strokeStyle = '#4B5E4E';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(178, 110, 42, 0, Math.PI * 2);
      ctx.moveTo(132, 160);
      ctx.lineTo(226, 160);
      ctx.stroke();
      ctx.fillStyle = '#D98C45';
      ctx.fillRect(245, 75, 10, 92);
    } else if (key === 'window_coffee') {
      const bg = ctx.createLinearGradient(0, 0, 0, 220);
      bg.addColorStop(0, '#F6C28B');
      bg.addColorStop(0.55, '#E48966');
      bg.addColorStop(1, '#4C3B34');
      ctx.fillStyle = bg;
      ctx.fillRect(0, 0, 360, 220);
      ctx.fillStyle = '#382B25';
      ctx.fillRect(0, 0, 48, 220);
      ctx.fillRect(312, 0, 48, 220);
      ctx.fillRect(0, 162, 360, 58);
      ctx.fillStyle = '#FAF5EC';
      ctx.fillRect(148, 130, 46, 36);
    } else {
      const bg = ctx.createLinearGradient(0, 0, 0, 220);
      bg.addColorStop(0, '#96BFE2');
      bg.addColorStop(0.65, '#FCE2BA');
      bg.addColorStop(1, '#588B68');
      ctx.fillStyle = bg;
      ctx.fillRect(0, 0, 360, 220);
      ctx.fillStyle = '#FFFFFF';
      ctx.globalAlpha = 0.55;
      ctx.beginPath();
      ctx.arc(100, 66, 22, 0, Math.PI * 2);
      ctx.arc(122, 66, 26, 0, Math.PI * 2);
      ctx.arc(146, 68, 20, 0, Math.PI * 2);
      ctx.fill();
      ctx.globalAlpha = 1;
      ctx.fillStyle = '#477656';
      ctx.fillRect(0, 152, 360, 68);
    }

    const url = c.toDataURL('image/png');
    snapshotCache[key] = url;
    return url;
  }

  function resetSocial(lang) {
    socialState = {
      activeCircleId: 'c_amigos',
      circles: JSON.parse(JSON.stringify(DEFAULT_CIRCLES))
    };
    migrateSocialStrings(socialState, lang);
    saveSocialState();
  }

  return {
    MAX_FRIENDS,
    MAX_CIRCLES,
    applyLanguage,
    resetSocial,
    getCircles,
    getActiveCircle,
    getActiveCircleId,
    setActiveCircle,
    createCircle,
    getFriends,
    getInviteCode,
    addFriend,
    removeFriend,
    toggleReaction,
    cheerFriend,
    getSceneSnapshot
  };
})();
