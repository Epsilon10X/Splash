/**
 * Splash — Sistema bilingüe ES / EN (sin traducción automática: textos originales en español)
 */
window.SplashI18n = (function () {
  const STORAGE_KEY = 'splash_lang_v1';

  const STRINGS = {
    doc_title: ['Splash — Productividad Anti-Pantalla', 'Splash — Offline Productivity'],
    header_streak_title: ['Racha acumulativa (nunca se reinicia)', 'Cumulative streak (never resets)'],
    header_level_title: ['Biomas desbloqueados', 'Unlocked biomes'],
    settings_aria: ['Ajustes', 'Settings'],
    tab_hoy_aria: ['Hoy', 'Today'],
    open_3d_aria: ['Explorar bioma en 3D', 'Explore biome in 3D'],
    open_3d_title: ['Entrar al bioma en 3D', 'Open 3D biome viewer'],
    diorama_wrap_aria: ['Entrar al bioma en 3D', 'Open 3D biome viewer'],
    diorama_canvas_aria: ['Maqueta 3D de bioma', '3D Biome Diorama'],
    biome_prev_aria: ['Bioma anterior', 'Previous biome'],
    biome_next_aria: ['Siguiente bioma', 'Next biome'],
    bereal_photo_btn: ['Foto', 'Photo'],
    bereal_done_btn: ['Hecho', 'Done'],
    time_switcher_aria: ['Tiempo disponible', 'Available time'],
    time_weekly: ['Semanal', 'Weekly'],
    refresh_aria: ['Nuevas sugerencias', 'New suggestions'],
    reroll_title: ['Cambiar retos de la semana', 'Swap weekly challenges'],
    tab_muro_aria: ['Muro Real', 'Real Wall'],
    muro_title: ['Muro Real', 'Real Wall'],
    muro_subtabs_aria: ['Vistas de Muro', 'Wall views'],
    muro_view_cal: ['Mi Muro', 'My Wall'],
    muro_view_circle: ['Círculo', 'Circle'],
    muro_view_map: ['Mapa', 'Map'],
    cal_prev_aria: ['Mes anterior', 'Previous month'],
    cal_next_aria: ['Mes siguiente', 'Next month'],
    wd_1: ['L', 'M'],
    wd_2: ['M', 'T'],
    wd_3: ['X', 'W'],
    wd_4: ['J', 'T'],
    wd_5: ['V', 'F'],
    wd_6: ['S', 'S'],
    wd_7: ['D', 'S'],
    balance_title: ['Equilibrio este mes', 'Balance this month'],
    tab_catalogo_aria: ['Catálogo', 'Catalog'],
    catalog_title: ['Catálogo', 'Catalog'],
    btn_create_custom: ['Crear propio', 'Create custom'],
    catalog_search_ph: ['Buscar entre los 67+ objetivos...', 'Search across 67+ goals...'],
    filter_all: ['Todos', 'All'],
    filter_quick: ['Rápidos', 'Quick'],
    filter_medium: ['Medios', 'Medium'],
    filter_long: ['Largos', 'Long'],
    filter_biome: ['Del bioma', 'Biome'],
    tab_biomas_aria: ['Biomas', 'Biomes'],
    biomes_title: ['Tus Biomas', 'Your Biomes'],
    biomes_sub: ['10 mundos apilados en orden. Toca uno para equiparlo o ábrelo en 3D.', '10 worlds stacked in order. Tap one to equip it or open it in 3D.'],
    biomes_view_aria: ['Vista de biomas', 'Biomes layout'],
    biomes_view_list: ['Apilado', 'Stack'],
    biomes_view_large: ['Grande', 'Large'],
    nav_aria: ['Navegación principal', 'Main navigation'],
    nav_hoy: ['Hoy', 'Today'],
    nav_muro: ['Muro', 'Wall'],
    nav_catalogo: ['Catálogo', 'Catalog'],
    nav_biomas_aria: ['Ver todos los biomas', 'View all biomes'],
    nav_biomas_title: ['Tus biomas apilados', 'Your stacked biomes'],
    toast_default: ['Hecho. Ya puedes bloquear el móvil.', 'Done. You can lock your phone now.'],
    toast_photo: ['Foto', 'Photo'],
    close_aria: ['Cerrar', 'Close'],
    exit_aria: ['Salir', 'Exit'],
    social_hint: ['Abre tu app de mensajería para escribir con calma. Al volver aquí, validaremos el objetivo.', 'Open your messaging app to write calmly. When you return here, we will mark the goal complete.'],
    ambient_label: ['Sonido suave', 'Soft sound'],
    btn_timer_complete: ['Completar', 'Complete'],
    analog_heading: ['Deja el móvil boca abajo', 'Place your phone face down'],
    analog_sound_label: ['Sonido del bioma', 'Biome sound'],
    btn_analog_complete: ['Ya terminé este reto', 'I finished this challenge'],
    cam_badge: ['Muro Real', 'Real Wall'],
    cam_title: ['Guarda un recuerdo rápido', 'Save a quick memory'],
    cam_sub: ['Solo se guarda en tu dispositivo. Cero redes, cero filtros.', 'Stored only on your device. No feeds, no filters.'],
    cam_img_alt: ['Foto capturada', 'Captured photo'],
    cam_fallback_p: ['Puedes usar la cámara o elegir una foto rápida de tu galería', 'Use your camera or pick a quick photo from your gallery'],
    cam_pick_btn: ['Elegir o sacar foto', 'Take or choose photo'],
    cam_capture_btn: ['Capturar', 'Capture'],
    cam_save_btn: ['Guardar en Muro', 'Save to Wall'],
    create_title: ['Crear objetivo propio', 'Create custom goal'],
    create_q: ['¿Qué quieres hacer fuera de la pantalla?', 'What do you want to do off-screen?'],
    create_ph: ['Ej. Regar las suculentas del balcón', 'e.g. Water the balcony succulents'],
    create_dur: ['Tiempo', 'Duration'],
    opt_quick: ['Rápido (<2 min)', 'Quick (<2 min)'],
    opt_medium: ['Medio (10-20 min)', 'Medium (10-20 min)'],
    opt_long: ['Largo (30-60+ min)', 'Long (30-60+ min)'],
    create_area: ['Área', 'Area'],
    create_save: ['Guardar en Catálogo', 'Save to Catalog'],
    settings_badge: ['Ajustes', 'Settings'],
    settings_profile_sub: ['Personaliza tu nombre en Splash', 'Personalize your name in Splash'],
    settings_name_ph: ['Escribe tu nombre...', 'Enter your name...'],
    settings_name_aria: ['Nombre de usuario', 'User name'],
    settings_save: ['Guardar', 'Save'],
    settings_lang_title: ['Idioma', 'Language'],
    settings_lang_aria: ['Idioma de la aplicación', 'App language'],
    settings_appearance: ['Apariencia', 'Appearance'],
    settings_theme_aria: ['Tema de la aplicación', 'App theme'],
    theme_sys: ['Sistema', 'System'],
    theme_light: ['Claro', 'Light'],
    theme_dark: ['Oscuro', 'Dark'],
    settings_notif: ['Notificaciones rápidas', 'Quick notifications'],
    settings_notif_sub: ['Toca un día y marca las horas en las que quieres recibir avisos rápidos (20 min):', 'Tap a day and mark the hours when you want gentle 20-minute pings:'],
    sched_active_label: ['Activas en este horario', 'Active for this hour'],
    sched_scope_aria: ['Ámbito de horario', 'Schedule scope'],
    sched_default: ['Predeterminada', 'Default'],
    sched_week: ['Otra semana', 'Specific week'],
    sched_prev_aria: ['Semana anterior', 'Previous week'],
    sched_next_aria: ['Semana siguiente', 'Next week'],
    sched_reset: ['Restaurar', 'Reset'],
    sched_visual_aria: ['Selector visual de horas', 'Visual hour selector'],
    preset_default: ['L–V sin lun. tarde', 'Mon–Fri no Mon PM'],
    preset_weekdays: ['L–V 9–21h', 'Mon–Fri 9a–9p'],
    preset_all: ['Todos 9–21h', 'Daily 9a–9p'],
    preset_mute: ['Silenciar', 'Mute'],
    widgets_title: ['Widgets', 'Widgets'],
    widgets_sub: ['Vista interactiva de inicio y pantalla de bloqueo', 'Interactive home & lockscreen widget preview'],
    feedback_title: ['Enviar feedback', 'Send feedback'],
    feedback_sub: ['Beta · Cuéntanos qué funciones añadirías o qué mejorarías', 'Beta · Share ideas for biomes, features or bug reports'],
    beta_title: ['Modo Beta', 'Beta Mode'],
    beta_toggle_aria: ['Activar o desactivar Modo Beta', 'Toggle Beta Mode'],
    beta_divider: ['Pruebas rápidas Beta', 'Beta Quick Tests'],
    sim_bereal_t: ['Aviso Rápido (20 min)', 'Quick Ping (20 min)'],
    sim_bereal_s: ['Activa un objetivo de 2 min con ventana de 20 min', 'Triggers a 2-min goal with a 20-min window'],
    sim_medio_t: ['Aviso Medio (4 horas)', 'Medium Ping (4 hours)'],
    sim_medio_s: ['Activa un objetivo de 15 min con ventana de 4 h', 'Triggers a 15-min goal with a 4-hour window'],
    sim_max_t: ['Completar bioma actual (Nv. 8 · Beta)', 'Complete current biome (Lvl. 8 · Beta)'],
    sim_max_s: ['Completa tu bioma activo (80/80) y desbloquea el siguiente', 'Maxes your active biome (80/80) and unlocks the next'],
    sim_anti_t: ['Notificación Rescate Anti-Scroll', 'Anti-Scroll Rescue Notification'],
    sim_anti_s: ['Vista previa de pantalla bloqueada con 3 botones', 'Lockscreen preview with 3 quick exit actions'],
    sim_thu_t: ['Recordatorio Amable (Jueves)', 'Gentle Thursday Reminder'],
    sim_thu_s: ['Aviso suave para planificar tus retos largos del finde', 'Soft prompt to pick your weekend long goals'],
    sim_reset_t: ['Reiniciar progreso (desde 0)', 'Reset progress (from 0)'],
    sim_reset_s: ['Borra racha, biomas, fotos y objetivos completados', 'Clears streak, biomes, photos & completed goals'],
    lock_head: ['Splash · Rescate Anti-Scroll', 'Splash · Anti-Scroll Rescue'],
    lock_now: ['ahora', 'now'],
    lock_p: ['¿Llevas un rato haciendo scroll? Sal de la pantalla en menos de 10 segundos:', 'Been scrolling for a while? Step off-screen in under 10 seconds:'],
    lock_weekly: ['Reto Semanal', 'Weekly'],
    day_img_alt: ['Foto del día en Splash', 'Daily photo in Splash'],
    day_add_photo: ['Añadir foto de este recuerdo', 'Add photo for this memory'],
    viewer_aria: ['Visor 3D de bioma', '3D Biome Viewer'],
    viewer_close_aria: ['Cerrar visor 3D', 'Close 3D viewer'],
    viewer_hint: ['Arrastra para rotar · Pellizca o rueda para ampliar', 'Drag to rotate · Pinch or scroll to zoom'],
    viewer_zin_aria: ['Ampliar', 'Zoom in'],
    viewer_zout_aria: ['Alejar', 'Zoom out'],
    viewer_reset_aria: ['Centrar cámara', 'Center camera'],
    viewer_sun_aria: ['Cambiar hora del día', 'Switch time of day'],
    viewer_sun_title: ['Luz (Auto / Día / Atardecer / Noche)', 'Lighting (Auto / Day / Sunset / Night)'],
    viewer_sound_aria: ['Sonido del bioma', 'Biome sound'],
    viewer_sound_title: ['Sonido ambiental del bioma', 'Ambient biome sound'],
    viewer_biomes_aria: ['Biomas', 'Biomes'],
    viewer_stage_caption: ['Mejora', 'Level'],
    viewer_stage_aria: ['Progresión del bioma', 'Biome progression'],
    viewer_sim_aria: ['Simular cumplimiento de misiones', 'Simulate completing goals'],
    sim_add_1: ['+1 Rápido', '+1 Quick'],
    sim_add_4: ['+4 Medio', '+4 Medium'],
    sim_add_8: ['+8 Largo', '+8 Long'],
    viewer_beta_note: ['Vista libre Beta · sin cumplir objetivos', 'Beta free view · without completing goals'],
    w_modal_aria: ['Widgets', 'Widgets'],
    w_modal_badge: ['Splash', 'Splash'],
    w_modal_title: ['Widgets', 'Widgets'],
    w_modal_sub: ['Vista previa en tiempo real. Elige el estilo y añádelo a tu pantalla de inicio.', 'Live visual preview. Pick a style and add it to your home screen.'],
    w_type_aria: ['Formato de widget', 'Widget layout'],
    w_type_4x2: ['4×2 Retos', '4×2 Goals'],
    w_type_2x2: ['2×2 Bioma', '2×2 Biome'],
    w_type_lock: ['Bloqueo', 'Lockscreen'],
    w_time_aria: ['Luz del día en widget', 'Daylight in widget'],
    w_time_dawn: ['Amanecer', 'Dawn'],
    w_time_day: ['Día', 'Day'],
    w_time_sunset: ['Tarde', 'Sunset'],
    w_time_night: ['Noche', 'Night'],
    w_theme_aria: ['Tema del widget', 'Widget theme'],
    w_theme_auto: ['Tema auto (noche)', 'Auto theme (night)'],
    w_theme_light: ['Siempre claro', 'Always light'],
    w_theme_dark: ['Siempre oscuro', 'Always dark'],
    w_note: ['Vista previa: añade el widget a tu pantalla de inicio de Android con el botón superior.', 'Preview only: add the widget to your Android home screen with the button above.'],
    exp_aria: ['Explorar bioma en primera persona', 'First-person 3D biome explorer'],
    exp_back_aria: ['Volver al visor 3D', 'Back to 3D viewer'],
    exp_weather_aria: ['Cambiar clima en vista 3D', 'Cycle weather in 3D view'],
    exp_weather_title: ['Clima vivo', 'Live weather'],
    exp_light_aria: ['Cambiar hora del día', 'Switch time of day'],
    exp_sound_aria: ['Sonido posicional 3D', '3D spatial sound'],
    exp_pad_aria: ['Controles para caminar por el bioma', 'Walking controls'],
    exp_fwd_aria: ['Avanzar', 'Move forward'],
    exp_left_aria: ['Paso a la izquierda', 'Step left'],
    exp_back_move_aria: ['Retroceder', 'Move backward'],
    exp_right_aria: ['Paso a la derecha', 'Step right'],
    exp_spots_aria: ['Puntos del bioma', 'Biome spots'],
    exp_photo_aria: ['Recuerdo del Muro Real', 'Real Wall Memory'],
    exp_photo_close_aria: ['Cerrar cuadro', 'Close frame'],
    exp_photo_alt: ['Recuerdo del Muro Real', 'Real Wall Memory'],
    exp_photo_date_default: ['Muro Real', 'Real Wall'],
    exp_photo_title_default: ['Tu recuerdo', 'Your memory'],
    bereal_dismiss_aria: ['Descartar aviso', 'Dismiss alert'],
    notif_perm_title: ['Permiso de notificaciones', 'Notification permission'],
    notif_perm_sub: ['Activa avisos nativos en Android para recordatorios rápidos', 'Enable native Android alerts for quick reminders'],
    w_pin_btn: ['Añadir widget a la pantalla de inicio', 'Add widget to home screen']
  };

  function normalizeLang(lang) {
    return lang === 'en' ? 'en' : 'es';
  }

  let currentLang = 'es';
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === 'en' || saved === 'es') {
      currentLang = saved;
    }
  } catch (_) {}

  function getLang() {
    return currentLang;
  }

  function isEn() {
    return currentLang === 'en';
  }

  function pick(esVal, enVal) {
    return currentLang === 'en' ? enVal : esVal;
  }

  function getStr(key) {
    const pair = STRINGS[key];
    if (!pair) return '';
    return currentLang === 'en' ? pair[1] : pair[0];
  }

  function applyDom(lang) {
    if (lang) {
      currentLang = normalizeLang(lang);
    }
    const idx = currentLang === 'en' ? 1 : 0;
    document.documentElement.lang = currentLang;
    document.title = STRINGS.doc_title[idx];

    document.querySelectorAll('[data-i18n]').forEach(el => {
      const pair = STRINGS[el.getAttribute('data-i18n')];
      if (pair) el.textContent = pair[idx];
    });
    document.querySelectorAll('[data-i18n-aria]').forEach(el => {
      const pair = STRINGS[el.getAttribute('data-i18n-aria')];
      if (pair) el.setAttribute('aria-label', pair[idx]);
    });
    document.querySelectorAll('[data-i18n-title]').forEach(el => {
      const pair = STRINGS[el.getAttribute('data-i18n-title')];
      if (pair) el.setAttribute('title', pair[idx]);
    });
    document.querySelectorAll('[data-i18n-ph]').forEach(el => {
      const pair = STRINGS[el.getAttribute('data-i18n-ph')];
      if (pair) el.setAttribute('placeholder', pair[idx]);
    });
    document.querySelectorAll('[data-i18n-alt]').forEach(el => {
      const pair = STRINGS[el.getAttribute('data-i18n-alt')];
      if (pair) el.setAttribute('alt', pair[idx]);
    });

    document.querySelectorAll('.lang-mode-btn').forEach(btn => {
      const active = btn.dataset.langMode === currentLang;
      btn.classList.toggle('active', active);
      btn.setAttribute('aria-checked', active ? 'true' : 'false');
    });
  }

  function setLang(lang) {
    currentLang = normalizeLang(lang);
    try {
      localStorage.setItem(STORAGE_KEY, currentLang);
    } catch (_) {}
    applyDom(currentLang);
    return currentLang;
  }

  return {
    STORAGE_KEY,
    getLang,
    isEn,
    pick,
    t: getStr,
    setLang,
    applyDom
  };
})();
