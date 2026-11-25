# Splash — Offline Productivity & 3D Biomes

**Splash** es una aplicación minimalista anti-pantalla pensada bajo una regla sencilla: **entrar, elegir o completar un reto fuera de la pantalla y volver a tu día en menos de 10 segundos**.

Disponible en **Español** e **English** desde *Ajustes / Settings*.

---

## Qué incluye

- **Regla < 10 segundos**: bloques de **2 min**, **15 min** (temporizador con audio generativo) y **Semanal** (Modo Puente Analógico para dejar el móvil boca abajo).
- **10 Biomas 3D interactivos**: cada reto real hace crecer tu bioma (Valle Alpino, Jungla Lluviosa, Costa Mediterránea, Kioto, Oasis, Bali, Otoño, Fiordo, Atolón y Toscana) a lo largo de 8 etapas y clima dinámico.
- **Explorador 3D en primera persona** (Valle Alpino y Costa Mediterránea): camina por el bioma, escucha audio espacial 3D y contempla las fotos de tu calendario colgadas dentro del refugio o la casa.
- **Muro Real privado**: calendario personal de momentos sin pantalla y recuerdos fotográficos locales en tu dispositivo (cero filtros, cero algoritmos).
- **Círculos y Mapa compartido (estilo BeReal)**: círculos pequeños y privados sin rankings ni likes públicos.
- **Modo Beta activable/desactivable**: prueba niveles, avisos y reinicia progreso desde cero en un toque.

---

## Descarga e instalación (Android)

1. Ve a **[Releases](../../releases)** y descarga el último archivo `Splash-*.apk`.
2. Ábrelo en tu Android y acepta instalar desde orígenes permitidos.
3. *(Opcional)* Usa **Obtainium** apuntando a este repositorio para recibir actualizaciones automáticas cada vez que salga una versión.

---

## Compilar APK localmente

Requisitos: Node.js 18+ y Java 21 + Android SDK (incluido con Android Studio).

```bash
npm install
npm run cap:sync
cd android
./gradlew assembleDebug
```

El APK generado aparecerá en `android/app/build/outputs/apk/debug/app-debug.apk`.
