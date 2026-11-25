/**
 * Motor de audio generativo suave para Brote (Web Audio API) + grabaciones libres sin copyright
 * - Costa: Wikimedia Commons File:Water_on_Rocks.ogg (Dsw4, CC BY 3.0) -> assets/audio/costa-waves.ogg
 * - Bali: Wikimedia Commons File:Welling_rivulet_in_the_woods.ogg (stephan / PDsounds #100, Public Domain) -> assets/audio/bali-water.ogg
 * - Acordes suaves diferenciados por bioma al completar objetivos
 */
window.BroteAudio = (function () {
  let ctx = null;
  let activeNodes = [];
  let activeMasterGain = null;
  let accentIntervalId = null;
  let musicIntervalId = null;
  let activeFileSession = null;
  let currentMode = null; // 'biome:<id>' | 'white' | null
  let currentBiomeId = null;
  let spatialBus = null;
  let spatialZone = null;

  const BIOME_SOUNDSCAPES = {
    valle: {
      id: 'valle',
      label: 'Alpine breeze',
      icon: 'air',
      noiseType: 'pink',
      filterType: 'bandpass',
      baseFreq: 540,
      q: 0.75,
      lfoRate: 0.14,
      lfoDepth: 190,
      gain: 0.26,
      accentType: 'birds',
      accentMs: 3200,
      chimeNotes: [523.25, 659.25, 783.99]
    },
    jungla: {
      id: 'jungla',
      label: 'Rain',
      icon: 'rainy',
      noiseType: 'pink',
      filterType: 'lowpass',
      baseFreq: 1750,
      q: 0.55,
      lfoRate: 0.11,
      lfoDepth: 320,
      gain: 0.36,
      secondaryLayer: {
        noiseType: 'white_soft',
        filterType: 'highpass',
        baseFreq: 2100,
        q: 0.5,
        lfoRate: 0.19,
        lfoDepth: 260,
        gain: 0.10
      },
      accentType: 'rain_drops',
      accentMs: 1650,
      chimeNotes: [493.88, 659.25, 830.61]
    },
    costa: {
      id: 'costa',
      label: 'Waves on rocks',
      icon: 'waves',
      audioFile: 'assets/audio/costa-waves.ogg',
      audioVolume: 0.66,
      crossfadeSec: 2.2,
      tideOverlay: true,
      noiseType: 'brown',
      filterType: 'lowpass',
      baseFreq: 280,
      q: 0.65,
      lfoRate: 0.14,
      lfoDepth: 150,
      gain: 0.24,
      gainWaveLfo: { rate: 0.14, depth: 0.10 },
      chimeNotes: [587.33, 739.99, 880.0]
    },
    kioto: {
      id: 'kioto',
      label: 'Kyoto village',
      icon: 'temple_buddhist',
      noiseType: 'soft',
      filterType: 'lowpass',
      baseFreq: 620,
      q: 0.6,
      lfoRate: 0.10,
      lfoDepth: 80,
      gain: 0.035,
      musicType: 'kioto_town',
      stepMs: 365,
      chimeNotes: [587.33, 659.25, 880.0]
    },
    desierto: {
      id: 'desierto',
      label: 'Egypt oasis',
      icon: 'music_note',
      noiseType: 'soft',
      filterType: 'lowpass',
      baseFreq: 480,
      q: 0.6,
      lfoRate: 0.08,
      lfoDepth: 60,
      gain: 0.025,
      musicType: 'egypt_oasis',
      stepMs: 385,
      chimeNotes: [440.0, 466.16, 554.37]
    },
    arrozales: {
      id: 'arrozales',
      label: 'Bali stream',
      icon: 'water_drop',
      audioFile: 'assets/audio/bali-water.ogg',
      audioVolume: 0.62,
      crossfadeSec: 1.6,
      noiseType: 'pink',
      filterType: 'lowpass',
      baseFreq: 900,
      q: 0.6,
      lfoRate: 0.1,
      lfoDepth: 50,
      gain: 0,
      accentType: 'bali_drops',
      accentMs: 520,
      chimeNotes: [523.25, 659.25, 783.99]
    },
    otono: {
      id: 'otono',
      label: 'Autumn breeze',
      icon: 'eco',
      noiseType: 'pink',
      filterType: 'bandpass',
      baseFreq: 1320,
      q: 0.68,
      lfoRate: 0.24,
      lfoDepth: 340,
      gain: 0.24,
      accentType: 'leaves',
      accentMs: 3000,
      chimeNotes: [440.0, 523.25, 698.46]
    },
    fiordo: {
      id: 'fiordo',
      label: 'Crisp wind',
      icon: 'ac_unit',
      noiseType: 'pink',
      filterType: 'bandpass',
      baseFreq: 950,
      q: 1.4,
      lfoRate: 0.14,
      lfoDepth: 210,
      gain: 0.16,
      secondaryLayer: {
        noiseType: 'pink',
        filterType: 'bandpass',
        baseFreq: 1520,
        q: 4.4,
        lfoRate: 0.13,
        lfoDepth: 420,
        gain: 0.22
      },
      accentType: 'snow_whistle',
      accentMs: 2800,
      chimeNotes: [587.33, 783.99, 1046.5]
    },
    atolon: {
      id: 'atolon',
      label: 'Beach chill',
      icon: 'sailing',
      noiseType: 'brown',
      filterType: 'lowpass',
      baseFreq: 360,
      q: 0.6,
      lfoRate: 0.085,
      lfoDepth: 110,
      gain: 0.08,
      musicType: 'beach_chill',
      stepMs: 355,
      chimeNotes: [523.25, 659.25, 987.77]
    },
    toscana: {
      id: 'toscana',
      label: 'Tuscan hills',
      icon: 'wb_sunny',
      noiseType: 'soft',
      filterType: 'lowpass',
      baseFreq: 820,
      q: 0.7,
      lfoRate: 0.11,
      lfoDepth: 165,
      gain: 0.24,
      secondaryLayer: {
        noiseType: 'pink',
        filterType: 'bandpass',
        baseFreq: 3400,
        q: 3.2,
        lfoRate: 0.32,
        lfoDepth: 220,
        gain: 0.025
      },
      accentType: 'tuscan_birds',
      accentMs: 2200,
      chimeNotes: [493.88, 622.25, 739.99]
    }
  };

  const SOUNDSCAPE_LABELS = {
    valle: { es: 'Brisa alpina', en: 'Alpine breeze' },
    jungla: { es: 'Lluvia', en: 'Rain' },
    costa: { es: 'Olas en rocas y marea', en: 'Waves on rocks' },
    kioto: { es: 'Pueblo de Kioto', en: 'Kyoto village' },
    desierto: { es: 'Oasis de Egipto', en: 'Egypt oasis' },
    arrozales: { es: 'Agua suave de Bali', en: 'Bali stream' },
    otono: { es: 'Brisa y hojas', en: 'Autumn breeze' },
    fiordo: { es: 'Viento agudo', en: 'Crisp wind' },
    atolon: { es: 'Música playera', en: 'Beach chill' },
    toscana: { es: 'Campo toscano', en: 'Tuscan hills' }
  };

  function applyLanguage(lang) {
    const useEn = lang ? lang === 'en' : !!(window.SplashI18n && window.SplashI18n.isEn());
    Object.keys(BIOME_SOUNDSCAPES).forEach(k => {
      const pair = SOUNDSCAPE_LABELS[k];
      if (pair) BIOME_SOUNDSCAPES[k].label = useEn ? pair.en : pair.es;
    });
  }

  function getContext() {
    if (!ctx) {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (AudioCtx) {
        ctx = new AudioCtx();
      }
    }
    if (ctx && ctx.state === 'suspended') {
      ctx.resume().catch(() => {});
    }
    return ctx;
  }

  function getBiomeSoundMeta(biomeId) {
    return BIOME_SOUNDSCAPES[biomeId] || BIOME_SOUNDSCAPES.valle;
  }

  function playBiomeChime(biomeId) {
    try {
      const c = getContext();
      if (!c) return;
      const now = c.currentTime;
      const meta = getBiomeSoundMeta(biomeId);
      const freqs = meta.chimeNotes || [523.25, 659.25, 783.99];

      freqs.forEach((f, idx) => {
        const osc = c.createOscillator();
        const gain = c.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(f, now + idx * 0.055);

        gain.gain.setValueAtTime(0.001, now + idx * 0.055);
        gain.gain.exponentialRampToValueAtTime(0.08, now + idx * 0.055 + 0.02);
        gain.gain.exponentialRampToValueAtTime(0.0001, now + idx * 0.055 + 0.48);

        osc.connect(gain);
        gain.connect(c.destination);
        osc.start(now + idx * 0.055);
        osc.stop(now + idx * 0.055 + 0.52);
      });
    } catch (_) {}
  }

  function playCozyChime(biomeId) {
    playBiomeChime(biomeId || 'valle');
  }

  function createNoiseBuffer(c, type) {
    const bufferSize = c.sampleRate * 3;
    const buffer = c.createBuffer(1, bufferSize, c.sampleRate);
    const output = buffer.getChannelData(0);

    if (type === 'rain' || type === 'pink') {
      let b0 = 0, b1 = 0, b2 = 0, b3 = 0, b4 = 0, b5 = 0, b6 = 0;
      for (let i = 0; i < bufferSize; i++) {
        const white = Math.random() * 2 - 1;
        b0 = 0.99886 * b0 + white * 0.0555179;
        b1 = 0.99332 * b1 + white * 0.0750759;
        b2 = 0.96900 * b2 + white * 0.1538520;
        b3 = 0.86650 * b3 + white * 0.3104856;
        b4 = 0.55000 * b4 + white * 0.5329522;
        b5 = -0.7616 * b5 - white * 0.0168980;
        output[i] = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362) * 0.04;
        b6 = white * 0.115926;
      }
    } else if (type === 'white_soft') {
      for (let i = 0; i < bufferSize; i++) {
        output[i] = (Math.random() * 2 - 1) * 0.075;
      }
    } else if (type === 'soft') {
      let lastOut = 0.0;
      for (let i = 0; i < bufferSize; i++) {
        const white = Math.random() * 2 - 1;
        lastOut = (lastOut + 0.035 * white) / 1.035;
        output[i] = lastOut * 0.22 + white * 0.010;
      }
    } else {
      let lastOut = 0.0;
      for (let i = 0; i < bufferSize; i++) {
        const white = Math.random() * 2 - 1;
        output[i] = (lastOut + 0.02 * white) / 1.02;
        lastOut = output[i];
        output[i] *= 0.28;
      }
    }
    return buffer;
  }

  // --- SINTETIZADORES MELÓDICOS ORIGINALES ---
  function playKotoPluck(c, dest, freq, vol = 0.05, dur = 1.05) {
    const now = c.currentTime;
    const osc = c.createOscillator();
    const harm = c.createOscillator();
    const filter = c.createBiquadFilter();
    const gain = c.createGain();
    osc.type = 'triangle';
    osc.frequency.setValueAtTime(freq, now);
    harm.type = 'sine';
    harm.frequency.setValueAtTime(freq * 2, now);
    filter.type = 'lowpass';
    filter.frequency.setValueAtTime(2400, now);
    filter.frequency.exponentialRampToValueAtTime(520, now + dur * 0.65);

    gain.gain.setValueAtTime(0.0005, now);
    gain.gain.exponentialRampToValueAtTime(vol, now + 0.012);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + dur);

    const hGain = c.createGain();
    hGain.gain.value = 0.22;
    harm.connect(hGain);
    hGain.connect(filter);
    osc.connect(filter);
    filter.connect(gain);
    gain.connect(dest);

    osc.start(now);
    harm.start(now);
    osc.stop(now + dur + 0.04);
    harm.stop(now + dur + 0.04);
  }

  function playWindFlute(c, dest, freq, vol = 0.042, dur = 0.72) {
    const now = c.currentTime;
    const osc = c.createOscillator();
    const over = c.createOscillator();
    const vib = c.createOscillator();
    const vibGain = c.createGain();
    const filter = c.createBiquadFilter();
    const gain = c.createGain();

    osc.type = 'sine';
    osc.frequency.setValueAtTime(freq * 0.985, now);
    osc.frequency.linearRampToValueAtTime(freq, now + 0.07);
    over.type = 'triangle';
    over.frequency.setValueAtTime(freq * 2, now);

    vib.frequency.value = 4.6;
    vibGain.gain.value = 3.2;
    vib.connect(vibGain);
    vibGain.connect(osc.frequency);

    filter.type = 'lowpass';
    filter.frequency.value = 1850;

    gain.gain.setValueAtTime(0.0004, now);
    gain.gain.linearRampToValueAtTime(vol, now + 0.11);
    gain.gain.setValueAtTime(vol * 0.85, now + dur * 0.62);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + dur);

    const oGain = c.createGain();
    oGain.gain.value = 0.12;
    over.connect(oGain);
    oGain.connect(filter);
    osc.connect(filter);
    filter.connect(gain);
    gain.connect(dest);

    osc.start(now);
    over.start(now);
    vib.start(now);
    osc.stop(now + dur + 0.05);
    over.stop(now + dur + 0.05);
    vib.stop(now + dur + 0.05);
  }

  function playOudPluck(c, dest, freq, vol = 0.048, dur = 0.85) {
    const now = c.currentTime;
    const osc = c.createOscillator();
    const filter = c.createBiquadFilter();
    const gain = c.createGain();
    osc.type = 'sawtooth';
    osc.frequency.setValueAtTime(freq, now);
    filter.type = 'lowpass';
    filter.frequency.setValueAtTime(1150, now);
    filter.frequency.exponentialRampToValueAtTime(280, now + dur * 0.7);
    gain.gain.setValueAtTime(0.0005, now);
    gain.gain.exponentialRampToValueAtTime(vol, now + 0.015);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + dur);

    osc.connect(filter);
    filter.connect(gain);
    gain.connect(dest);
    osc.start(now);
    osc.stop(now + dur + 0.04);
  }

  function playSoftDrum(c, dest, freq = 88, vol = 0.036) {
    const now = c.currentTime;
    const osc = c.createOscillator();
    const gain = c.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(freq * 1.4, now);
    osc.frequency.exponentialRampToValueAtTime(freq, now + 0.09);
    gain.gain.setValueAtTime(0.001, now);
    gain.gain.exponentialRampToValueAtTime(vol, now + 0.012);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.28);
    osc.connect(gain);
    gain.connect(dest);
    osc.start(now);
    osc.stop(now + 0.3);
  }

  function playBeachPluck(c, dest, freq, vol = 0.04, dur = 0.78) {
    const now = c.currentTime;
    const osc = c.createOscillator();
    const gain = c.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(freq, now);
    gain.gain.setValueAtTime(0.0005, now);
    gain.gain.exponentialRampToValueAtTime(vol, now + 0.016);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + dur);
    osc.connect(gain);
    gain.connect(dest);
    osc.start(now);
    osc.stop(now + dur + 0.04);
  }

  function startMusicSequencer(c, dest, musicType, stepMs) {
    let step = 0;

    // 1. Pueblo de Kioto (tema original estilo JRPG clásico cálido en escala Yo pentatónica D)
    const kiotoKoto = [293.66, 0, 440.0, 587.33, 392.0, 0, 440.0, 659.25, 293.66, 0, 440.0, 587.33, 329.63, 440.0, 587.33, 0];
    const kiotoFlute = [587.33, 0, 659.25, 783.99, 880.0, 0, 783.99, 659.25, 587.33, 0, 493.88, 587.33, 440.0, 0, 392.0, 0];

    // 2. Oasis de Egipto (tema original Hijaz suave en D con oud + ney + bendir)
    const egyptOud = [146.83, 0, 220.0, 293.66, 146.83, 277.18, 220.0, 0, 155.56, 0, 220.0, 311.13, 146.83, 0, 293.66, 220.0];
    const egyptNey = [587.33, 0, 554.37, 466.16, 440.0, 0, 466.16, 440.0, 392.0, 0, 466.16, 554.37, 587.33, 0, 0, 0];

    // 3. Música playera del Atolón (progresión Cmaj7 - Am7 - Fmaj7 - G6)
    const beachBass = [261.63, 0, 329.63, 392.0, 220.0, 0, 329.63, 392.0, 174.61, 0, 261.63, 329.63, 196.0, 0, 293.66, 392.0];
    const beachLead = [659.25, 0, 783.99, 987.77, 880.0, 0, 783.99, 659.25, 523.25, 0, 659.25, 783.99, 587.33, 0, 659.25, 0];

    function runStep() {
      if (!c || c.state !== 'running') return;
      const i = step % 16;
      try {
        if (musicType === 'kioto_town') {
          if (kiotoKoto[i]) playKotoPluck(c, dest, kiotoKoto[i], 0.044, 1.0);
          if (kiotoFlute[i]) playWindFlute(c, dest, kiotoFlute[i], 0.036, 0.78);
          if (i === 0 || i === 8) playSoftDrum(c, dest, 92, 0.026);
        } else if (musicType === 'egypt_oasis') {
          if (egyptOud[i]) playOudPluck(c, dest, egyptOud[i], 0.042, 0.85);
          if (egyptNey[i]) playWindFlute(c, dest, egyptNey[i], 0.034, 0.82);
          if (i === 0 || i === 6 || i === 12) playSoftDrum(c, dest, 82, 0.030);
        } else if (musicType === 'beach_chill') {
          if (beachBass[i]) playBeachPluck(c, dest, beachBass[i], 0.038, 0.88);
          if (beachLead[i]) playKotoPluck(c, dest, beachLead[i], 0.026, 0.72);
        }
      } catch (_) {}
      step++;
    }

    runStep();
    musicIntervalId = setInterval(runStep, stepMs || 360);
  }

  function scheduleBiomeAccent(c, accentType) {
    if (!c || c.state !== 'running') return;
    try {
      const now = c.currentTime;

      if (accentType === 'bali_drops') {
        // Gotas suaves cayendo sobre agua tranquila (sin viento)
        const drops = Math.random() < 0.35 ? 2 : 1;
        for (let k = 0; k < drops; k++) {
          const t0 = now + k * 0.14 + Math.random() * 0.08;
          const f = 540 + Math.random() * 820;
          const osc = c.createOscillator();
          const gain = c.createGain();
          osc.type = 'sine';
          osc.frequency.setValueAtTime(f, t0);
          osc.frequency.exponentialRampToValueAtTime(f * 1.42, t0 + 0.048);
          gain.gain.setValueAtTime(0.0004, t0);
          gain.gain.exponentialRampToValueAtTime(0.022 + Math.random() * 0.010, t0 + 0.007);
          gain.gain.exponentialRampToValueAtTime(0.0001, t0 + 0.14);
          osc.connect(gain);
          gain.connect(getSpatialDest(c));
          osc.start(t0);
          osc.stop(t0 + 0.16);
        }
      } else if (accentType === 'rain_drops' || accentType === 'waterfall_drops') {
        const drops = accentType === 'waterfall_drops' ? 3 + Math.floor(Math.random() * 3) : 2 + Math.floor(Math.random() * 2);
        for (let k = 0; k < drops; k++) {
          const t0 = now + Math.random() * 0.85;
          const f = accentType === 'waterfall_drops' ? 460 + Math.random() * 760 : 780 + Math.random() * 980;
          const osc = c.createOscillator();
          const gain = c.createGain();
          osc.type = 'sine';
          osc.frequency.setValueAtTime(f, t0);
          osc.frequency.exponentialRampToValueAtTime(f * 1.35, t0 + 0.055);
          gain.gain.setValueAtTime(0.0004, t0);
          gain.gain.exponentialRampToValueAtTime(0.015, t0 + 0.009);
          gain.gain.exponentialRampToValueAtTime(0.0001, t0 + 0.09);
          osc.connect(gain);
          gain.connect(getSpatialDest(c));
          osc.start(t0);
          osc.stop(t0 + 0.11);
        }
      } else if (accentType === 'tuscan_birds' || accentType === 'birds') {
        const base = accentType === 'tuscan_birds' ? 1820 + Math.random() * 640 : 1560 + Math.random() * 440;
        const chirps = accentType === 'tuscan_birds' ? 3 : 2;
        for (let k = 0; k < chirps; k++) {
          const osc = c.createOscillator();
          const gain = c.createGain();
          const t0 = now + k * 0.125;
          const mult = k % 2 === 0 ? 1.24 : 0.92;
          osc.type = 'sine';
          osc.frequency.setValueAtTime(base, t0);
          osc.frequency.exponentialRampToValueAtTime(base * mult, t0 + 0.075);
          gain.gain.setValueAtTime(0.0004, t0);
          gain.gain.exponentialRampToValueAtTime(0.016, t0 + 0.018);
          gain.gain.exponentialRampToValueAtTime(0.0001, t0 + 0.105);
          osc.connect(gain);
          gain.connect(getSpatialDest(c));
          osc.start(t0);
          osc.stop(t0 + 0.12);
        }
      } else if (accentType === 'snow_whistle') {
        // Ráfaga aguda y limpia de viento de alta montaña
        const osc = c.createOscillator();
        const gain = c.createGain();
        const f0 = 1120 + Math.random() * 420;
        osc.type = 'sine';
        osc.frequency.setValueAtTime(f0, now);
        osc.frequency.exponentialRampToValueAtTime(f0 * 1.28, now + 0.75);
        osc.frequency.exponentialRampToValueAtTime(f0 * 0.92, now + 1.65);
        gain.gain.setValueAtTime(0.0002, now);
        gain.gain.exponentialRampToValueAtTime(0.009, now + 0.55);
        gain.gain.exponentialRampToValueAtTime(0.0001, now + 1.70);
        osc.connect(gain);
        gain.connect(getSpatialDest(c));
        osc.start(now);
        osc.stop(now + 1.75);
      }
    } catch (_) {}
  }

  function ensureSpatialBus(c) {
    if (!c) return null;
    if (spatialBus && spatialBus.ctx === c) return spatialBus;
    const filter = c.createBiquadFilter();
    filter.type = 'lowpass';
    filter.frequency.value = 18000;
    filter.Q.value = 0.7;

    let panner;
    if (typeof c.createStereoPanner === 'function') {
      panner = c.createStereoPanner();
      panner.pan.value = 0;
    } else {
      panner = c.createGain();
      panner.gain.value = 1.0;
    }

    const gain = c.createGain();
    gain.gain.value = 1.0;

    filter.connect(panner);
    panner.connect(gain);
    gain.connect(c.destination);

    spatialBus = { ctx: c, filter, panner, gain };
    return spatialBus;
  }

  function getSpatialDest(c) {
    const bus = ensureSpatialBus(c);
    return bus ? bus.filter : c.destination;
  }

  function buildNoiseLayer(c, layerCfg, targetGainNode, nodesBag) {
    const source = c.createBufferSource();
    source.buffer = createNoiseBuffer(c, layerCfg.noiseType);
    source.loop = true;

    const filter = c.createBiquadFilter();
    filter.type = layerCfg.filterType || 'lowpass';
    filter.frequency.value = layerCfg.baseFreq;
    if (layerCfg.q) filter.Q.value = layerCfg.q;

    const lfo = c.createOscillator();
    const lfoGain = c.createGain();
    lfo.type = 'sine';
    lfo.frequency.value = layerCfg.lfoRate || 0.12;
    lfoGain.gain.value = layerCfg.lfoDepth || 150;
    lfo.connect(lfoGain);
    lfoGain.connect(filter.frequency);
    lfo.start();

    const layerGain = c.createGain();
    layerGain.gain.value = layerCfg.gain;

    source.connect(filter);
    filter.connect(layerGain);
    layerGain.connect(targetGainNode);
    source.start();

    nodesBag.push(source, lfo);
    return layerGain;
  }

  function stopAudioFileLoop() {
    const s = activeFileSession;
    activeFileSession = null;
    if (!s) return;
    s.cancelled = true;
    if (s.pollId) {
      clearInterval(s.pollId);
      s.pollId = null;
    }
    s.fadeIntervals.forEach(id => clearInterval(id));
    s.fadeIntervals = [];

    s.decks.forEach(deck => {
      try {
        const startVol = deck.volume || 0;
        if (startVol <= 0.01 || deck.paused) {
          deck.pause();
          deck.removeAttribute('src');
          deck.load();
          return;
        }
        let step = 0;
        const steps = 6;
        const iv = setInterval(() => {
          step++;
          const ratio = Math.max(0, 1 - step / steps);
          try {
            deck.volume = startVol * ratio;
          } catch (_) {}
          if (step >= steps) {
            clearInterval(iv);
            try {
              deck.pause();
              deck.removeAttribute('src');
              deck.load();
            } catch (_) {}
          }
        }, 42);
      } catch (_) {}
    });
  }

  function startAudioFileLoop(src, targetVolume, crossfadeSec, onError) {
    stopAudioFileLoop();
    const deckA = new Audio(src);
    const deckB = new Audio(src);
    [deckA, deckB].forEach(d => {
      d.preload = 'auto';
      d.loop = true;
      d.volume = 0;
      d._baseVol = 0;
    });

    const session = {
      cancelled: false,
      decks: [deckA, deckB],
      activeIdx: 0,
      crossfading: false,
      startedPlayback: false,
      targetVolume: Math.max(0.05, Math.min(1, targetVolume || 0.68)),
      spatialVolumeMul: 1.0,
      routedViaWebAudio: false,
      mediaSources: [],
      pollId: null,
      fadeIntervals: []
    };
    activeFileSession = session;

    try {
      const c = getContext();
      const canRoute = c && typeof location !== 'undefined' && location.protocol !== 'file:';
      if (canRoute && typeof c.createMediaElementSource === 'function') {
        const destFilter = getSpatialDest(c);
        const sA = c.createMediaElementSource(deckA);
        const sB = c.createMediaElementSource(deckB);
        sA.connect(destFilter);
        sB.connect(destFilter);
        session.mediaSources.push(sA, sB);
        session.routedViaWebAudio = true;
      }
    } catch (_) {
      session.routedViaWebAudio = false;
    }

    function fadeDeck(deck, fromVol, toVol, durMs, doneCb) {
      const steps = Math.max(4, Math.round(durMs / 45));
      let step = 0;
      deck._baseVol = fromVol;
      try {
        deck.volume = Math.max(0, Math.min(1, fromVol * session.spatialVolumeMul));
      } catch (_) {}
      const iv = setInterval(() => {
        if (session.cancelled) {
          clearInterval(iv);
          return;
        }
        step++;
        const t = Math.min(1, step / steps);
        const v = fromVol + (toVol - fromVol) * t;
        deck._baseVol = v;
        try {
          deck.volume = Math.max(0, Math.min(1, v * session.spatialVolumeMul));
        } catch (_) {}
        if (step >= steps) {
          clearInterval(iv);
          if (doneCb) doneCb();
        }
      }, 45);
      session.fadeIntervals.push(iv);
    }

    function handleFail() {
      if (session.cancelled || session.startedPlayback) return;
      stopAudioFileLoop();
      if (typeof onError === 'function') onError();
    }

    deckA.onerror = handleFail;

    try {
      const p = deckA.play();
      if (p && typeof p.then === 'function') {
        p.then(() => {
          if (session.cancelled) return;
          session.startedPlayback = true;
          fadeDeck(deckA, 0, session.targetVolume, 700);
        }).catch(handleFail);
      } else {
        session.startedPlayback = true;
        fadeDeck(deckA, 0, session.targetVolume, 700);
      }
    } catch (_) {
      handleFail();
      return;
    }

    const cf = Math.max(0.8, crossfadeSec || 1.8);
    session.pollId = setInterval(() => {
      if (session.cancelled || session.crossfading) return;
      const curDeck = session.decks[session.activeIdx];
      const dur = curDeck.duration;
      if (!isFinite(dur) || dur <= cf * 2.2) return;
      if (curDeck.currentTime >= dur - cf) {
        session.crossfading = true;
        const nextIdx = 1 - session.activeIdx;
        const nextDeck = session.decks[nextIdx];
        try {
          nextDeck.currentTime = 0;
          nextDeck.volume = 0;
          const np = nextDeck.play();
          if (np && typeof np.catch === 'function') np.catch(() => {});
        } catch (_) {}
        const fadeMs = Math.round(cf * 900);
        fadeDeck(curDeck, curDeck.volume, 0, fadeMs, () => {
          try { curDeck.pause(); } catch (_) {}
        });
        fadeDeck(nextDeck, 0, session.targetVolume, fadeMs, () => {
          if (session.cancelled) return;
          session.activeIdx = nextIdx;
          session.crossfading = false;
        });
      }
    }, 150);
  }

  function startProceduralBiomeAmbient(meta) {
    stopAmbient();
    const c = getContext();
    if (!c) return false;

    const masterGain = c.createGain();
    masterGain.gain.setValueAtTime(0.001, c.currentTime);
    masterGain.gain.linearRampToValueAtTime(1.0, c.currentTime + 0.65);
    masterGain.connect(getSpatialDest(c));

    const bag = [];
    if (meta.gain > 0) {
      const primaryGain = buildNoiseLayer(c, meta, masterGain, bag);

      if (meta.gainWaveLfo) {
        const waveLfo = c.createOscillator();
        const waveDepth = c.createGain();
        waveLfo.type = 'sine';
        waveLfo.frequency.value = meta.gainWaveLfo.rate;
        waveDepth.gain.value = meta.gainWaveLfo.depth;
        waveLfo.connect(waveDepth);
        waveDepth.connect(primaryGain.gain);
        waveLfo.start();
        bag.push(waveLfo);
      }
    }

    if (meta.secondaryLayer) {
      buildNoiseLayer(c, meta.secondaryLayer, masterGain, bag);
    }

    activeNodes = bag;
    activeMasterGain = masterGain;
    currentBiomeId = meta.id;
    currentMode = `biome:${meta.id}`;

    if (meta.musicType) {
      startMusicSequencer(c, masterGain, meta.musicType, meta.stepMs);
    }

    if (meta.accentType) {
      if (meta.accentType === 'bali_drops') {
        setTimeout(() => {
          if (currentMode === `biome:${meta.id}`) {
            scheduleBiomeAccent(c, meta.accentType);
          }
        }, 280);
      }
      accentIntervalId = setInterval(() => {
        if (Math.random() < 0.85) {
          scheduleBiomeAccent(c, meta.accentType);
        }
      }, meta.accentMs || 2800);
    }

    return true;
  }

  function startCostaTideOverlay() {
    const c = getContext();
    if (!c) return;
    const masterGain = c.createGain();
    masterGain.gain.setValueAtTime(0.001, c.currentTime);
    masterGain.gain.linearRampToValueAtTime(1.0, c.currentTime + 0.9);
    masterGain.connect(getSpatialDest(c));

    const bag = [];
    // Marea lenta y profunda bajo las olas en rocas
    const tideGain = buildNoiseLayer(c, {
      noiseType: 'brown',
      filterType: 'lowpass',
      baseFreq: 320,
      q: 0.6,
      lfoRate: 0.09,
      lfoDepth: 110,
      gain: 0.14
    }, masterGain, bag);

    const tideLfo = c.createOscillator();
    const tideDepth = c.createGain();
    tideLfo.type = 'sine';
    tideLfo.frequency.value = 0.09;
    tideDepth.gain.value = 0.05;
    tideLfo.connect(tideDepth);
    tideDepth.connect(tideGain.gain);
    tideLfo.start();
    bag.push(tideLfo);

    // Brisa costera suave (sin ráfagas secas)
    buildNoiseLayer(c, {
      noiseType: 'pink',
      filterType: 'bandpass',
      baseFreq: 540,
      q: 0.85,
      lfoRate: 0.11,
      lfoDepth: 95,
      gain: 0.065
    }, masterGain, bag);

    activeNodes = bag;
    activeMasterGain = masterGain;
  }

  function startBiomeAmbient(biomeId) {
    stopAmbient();
    const meta = getBiomeSoundMeta(biomeId);
    if (meta.audioFile) {
      currentBiomeId = meta.id;
      currentMode = `biome:${meta.id}`;
      startAudioFileLoop(
        meta.audioFile,
        meta.audioVolume,
        meta.crossfadeSec,
        () => startProceduralBiomeAmbient(meta)
      );
      if (meta.tideOverlay) {
        startCostaTideOverlay();
      }
      return true;
    }
    return startProceduralBiomeAmbient(meta);
  }

  function startAmbient(mode) {
    if (mode === 'rain') {
      return startBiomeAmbient('jungla');
    }

    stopAmbient();
    const c = getContext();
    if (!c) return false;

    const masterGain = c.createGain();
    masterGain.gain.setValueAtTime(0.001, c.currentTime);
    masterGain.gain.linearRampToValueAtTime(1.0, c.currentTime + 0.6);
    masterGain.connect(c.destination);

    const bag = [];
    buildNoiseLayer(c, {
      noiseType: 'brown',
      filterType: 'lowpass',
      baseFreq: 520,
      q: 0.6,
      lfoRate: 0.08,
      lfoDepth: 80,
      gain: 0.42
    }, masterGain, bag);

    activeNodes = bag;
    activeMasterGain = masterGain;
    currentBiomeId = null;
    currentMode = mode;
    return true;
  }

  function resetSpatial3D() {
    if (spatialZone) {
      if (spatialZone.crackleTimer) {
        clearInterval(spatialZone.crackleTimer);
        spatialZone.crackleTimer = null;
      }
      const stopNodes = spatialZone.nodes ? spatialZone.nodes.slice() : [];
      spatialZone = null;
      stopNodes.forEach(n => {
        try { n.stop(); } catch (_) {}
        try { n.disconnect(); } catch (_) {}
      });
    }
    if (spatialBus && spatialBus.ctx) {
      const now = spatialBus.ctx.currentTime;
      try {
        spatialBus.filter.frequency.setTargetAtTime(18000, now, 0.08);
        spatialBus.gain.gain.setTargetAtTime(1.0, now, 0.08);
        if (spatialBus.panner && spatialBus.panner.pan) {
          spatialBus.panner.pan.setTargetAtTime(0, now, 0.08);
        }
      } catch (_) {}
    }
    if (activeFileSession && !activeFileSession.cancelled) {
      activeFileSession.spatialVolumeMul = 1.0;
      activeFileSession.decks.forEach(deck => {
        if (!deck.paused && typeof deck._baseVol === 'number') {
          try { deck.volume = Math.max(0, Math.min(1, deck._baseVol)); } catch (_) {}
        }
      });
    }
  }

  function ensureSpatialZones(c, biomeId) {
    if (!c) return null;
    if (spatialZone && spatialZone.ctx === c && spatialZone.biome === biomeId) {
      return spatialZone;
    }
    if (spatialZone) {
      if (spatialZone.crackleTimer) clearInterval(spatialZone.crackleTimer);
      (spatialZone.nodes || []).forEach(n => {
        try { n.stop(); } catch (_) {}
        try { n.disconnect(); } catch (_) {}
      });
      spatialZone = null;
    }

    const bag = [];
    const zone = {
      ctx: c,
      biome: biomeId,
      nodes: bag,
      leafGain: null,
      leafPanner: null,
      indoorGain: null,
      streamGain: null,
      streamPanner: null,
      fireGain: null,
      pineGain: null,
      crackleTimer: null,
      fireLevel: 0
    };

    try {
      if (biomeId === 'costa') {
        const leafOut = c.createGain();
        leafOut.gain.value = 0;
        const leafPan = typeof c.createStereoPanner === 'function' ? c.createStereoPanner() : c.createGain();
        leafOut.connect(leafPan);
        leafPan.connect(c.destination);
        buildNoiseLayer(c, {
          noiseType: 'pink',
          filterType: 'bandpass',
          baseFreq: 1220,
          q: 1.35,
          lfoRate: 0.22,
          lfoDepth: 240,
          gain: 0.34
        }, leafOut, bag);
        buildNoiseLayer(c, {
          noiseType: 'white_soft',
          filterType: 'highpass',
          baseFreq: 2150,
          q: 0.5,
          lfoRate: 0.30,
          lfoDepth: 280,
          gain: 0.14
        }, leafOut, bag);
        zone.leafGain = leafOut;
        zone.leafPanner = leafPan;

        const indoorOut = c.createGain();
        indoorOut.gain.value = 0;
        indoorOut.connect(c.destination);
        buildNoiseLayer(c, {
          noiseType: 'brown',
          filterType: 'lowpass',
          baseFreq: 210,
          q: 0.6,
          lfoRate: 0.08,
          lfoDepth: 35,
          gain: 0.24
        }, indoorOut, bag);
        zone.indoorGain = indoorOut;
      } else if (biomeId === 'valle') {
        const streamOut = c.createGain();
        streamOut.gain.value = 0;
        const streamPan = typeof c.createStereoPanner === 'function' ? c.createStereoPanner() : c.createGain();
        streamOut.connect(streamPan);
        streamPan.connect(c.destination);
        buildNoiseLayer(c, {
          noiseType: 'pink',
          filterType: 'bandpass',
          baseFreq: 780,
          q: 0.85,
          lfoRate: 0.32,
          lfoDepth: 165,
          gain: 0.42
        }, streamOut, bag);
        buildNoiseLayer(c, {
          noiseType: 'white_soft',
          filterType: 'bandpass',
          baseFreq: 2280,
          q: 1.05,
          lfoRate: 0.42,
          lfoDepth: 240,
          gain: 0.18
        }, streamOut, bag);
        zone.streamGain = streamOut;
        zone.streamPanner = streamPan;

        const fireOut = c.createGain();
        fireOut.gain.value = 0;
        fireOut.connect(c.destination);
        buildNoiseLayer(c, {
          noiseType: 'brown',
          filterType: 'lowpass',
          baseFreq: 270,
          q: 0.7,
          lfoRate: 2.1,
          lfoDepth: 85,
          gain: 0.36
        }, fireOut, bag);
        zone.fireGain = fireOut;

        const pineOut = c.createGain();
        pineOut.gain.value = 0;
        pineOut.connect(c.destination);
        buildNoiseLayer(c, {
          noiseType: 'pink',
          filterType: 'bandpass',
          baseFreq: 960,
          q: 1.25,
          lfoRate: 0.19,
          lfoDepth: 210,
          gain: 0.28
        }, pineOut, bag);
        zone.pineGain = pineOut;

        zone.crackleTimer = setInterval(() => {
          if (!spatialZone || spatialZone !== zone || zone.fireLevel < 0.15) return;
          if (!c || c.state !== 'running' || Math.random() > 0.65) return;
          try {
            const t0 = c.currentTime;
            const osc = c.createOscillator();
            const flt = c.createBiquadFilter();
            const g = c.createGain();
            osc.type = 'triangle';
            osc.frequency.setValueAtTime(1600 + Math.random() * 1400, t0);
            flt.type = 'bandpass';
            flt.frequency.value = 2400;
            flt.Q.value = 1.8;
            const amp = (0.007 + Math.random() * 0.008) * zone.fireLevel;
            g.gain.setValueAtTime(0.0002, t0);
            g.gain.exponentialRampToValueAtTime(amp, t0 + 0.005);
            g.gain.exponentialRampToValueAtTime(0.0001, t0 + 0.038);
            osc.connect(flt);
            flt.connect(g);
            g.connect(c.destination);
            osc.start(t0);
            osc.stop(t0 + 0.045);
          } catch (_) {}
        }, 920);
      }
      spatialZone = zone;
    } catch (_) {}
    return spatialZone;
  }

  function clamp01(v) {
    return Math.max(0, Math.min(1, Number(v) || 0));
  }

  function updateSpatial3D(opts = {}) {
    if (!currentMode && !activeFileSession) return;
    const c = getContext();
    if (!c) return;
    const bus = ensureSpatialBus(c);
    if (!bus) return;

    const biome = opts.biome === 'valle' ? 'valle' : 'costa';
    const pan = Math.max(-0.85, Math.min(0.85, Number(opts.pan) || 0));
    const waterProx = clamp01(opts.waterProx !== undefined ? opts.waterProx : 0.5);
    const indoorFactor = clamp01(opts.indoorFactor || 0);
    const treeProx = clamp01(opts.treeProx || 0);
    const fireProx = clamp01(opts.fireProx || 0);

    const now = c.currentTime;
    try {
      const cutoff = 18000 - indoorFactor * (18000 - 650);
      bus.filter.frequency.setTargetAtTime(cutoff, now, 0.14);
      if (bus.panner && bus.panner.pan) {
        bus.panner.pan.setTargetAtTime(pan * (1 - indoorFactor * 0.65), now, 0.12);
      }
      const baseGain = biome === 'costa'
        ? (0.78 + waterProx * 0.44) * (1 - indoorFactor * 0.45)
        : (0.95 - indoorFactor * 0.52);
      bus.gain.gain.setTargetAtTime(Math.max(0.32, Math.min(1.22, baseGain)), now, 0.14);
    } catch (_) {}

    if (activeFileSession && !activeFileSession.cancelled) {
      if (activeFileSession.routedViaWebAudio) {
        activeFileSession.spatialVolumeMul = 1.0;
      } else {
        const volMul = Math.max(0.25, Math.min(1.18, (0.52 + waterProx * 0.58) * (1 - indoorFactor * 0.50)));
        activeFileSession.spatialVolumeMul = volMul;
        activeFileSession.decks.forEach(deck => {
          if (!deck.paused && typeof deck._baseVol === 'number') {
            try {
              deck.volume = Math.max(0, Math.min(1, deck._baseVol * volMul));
            } catch (_) {}
          }
        });
      }
    }

    const z = ensureSpatialZones(c, biome);
    if (!z) return;
    try {
      if (biome === 'costa') {
        if (z.leafGain) {
          z.leafGain.gain.setTargetAtTime(treeProx * (1 - indoorFactor * 0.85) * 0.16, now, 0.16);
        }
        if (z.leafPanner && z.leafPanner.pan) {
          z.leafPanner.pan.setTargetAtTime(Math.max(-0.75, Math.min(0.75, -pan * 0.65)), now, 0.15);
        }
        if (z.indoorGain) {
          z.indoorGain.gain.setTargetAtTime(indoorFactor * 0.12, now, 0.18);
        }
      } else if (biome === 'valle') {
        if (z.streamGain) {
          z.streamGain.gain.setTargetAtTime(waterProx * (1 - indoorFactor * 0.72) * 0.22, now, 0.15);
        }
        if (z.streamPanner && z.streamPanner.pan) {
          z.streamPanner.pan.setTargetAtTime(pan, now, 0.14);
        }
        if (z.fireGain) {
          z.fireGain.gain.setTargetAtTime(fireProx * 0.18, now, 0.16);
          z.fireLevel = fireProx;
        }
        if (z.pineGain) {
          z.pineGain.gain.setTargetAtTime(treeProx * (1 - indoorFactor * 0.8) * 0.14, now, 0.16);
        }
      }
    } catch (_) {}
  }

  function stopAmbient() {
    resetSpatial3D();
    stopAudioFileLoop();
    if (accentIntervalId) {
      clearInterval(accentIntervalId);
      accentIntervalId = null;
    }
    if (musicIntervalId) {
      clearInterval(musicIntervalId);
      musicIntervalId = null;
    }
    if (activeNodes.length && ctx) {
      const nodesToStop = activeNodes.slice();
      const oldMaster = activeMasterGain;
      try {
        if (oldMaster && oldMaster.gain) {
          oldMaster.gain.linearRampToValueAtTime(0.0001, ctx.currentTime + 0.25);
        }
        setTimeout(() => {
          nodesToStop.forEach(n => {
            try { n.stop(); } catch (_) {}
          });
        }, 280);
      } catch (_) {}
    }
    activeNodes = [];
    activeMasterGain = null;
    currentBiomeId = null;
    currentMode = null;
  }

  function getMode() {
    return currentMode;
  }

  function getCurrentBiomeId() {
    return currentBiomeId;
  }

  applyLanguage();

  return {
    BIOME_SOUNDSCAPES,
    applyLanguage,
    getBiomeSoundMeta,
    playCozyChime,
    playBiomeChime,
    startAmbient,
    startBiomeAmbient,
    stopAmbient,
    updateSpatial3D,
    resetSpatial3D,
    getMode,
    getCurrentBiomeId
  };
})();
