# Splash — Productividad real para salir del piloto automático

**Splash** es una app para Android pensada para romper la inercia de la rueda automática del día a día, recuperar el enfoque y convertir minutos muertos en acciones reales de productividad, claridad mental, movimiento y entorno (además de ayudarte a frenar el scroll sin sentido).

Regla base: **entrar, elegir o completar un reto real y volver a tu vida en menos de 10 segundos**.

Disponible en **Español** e **English** desde *Ajustes / Settings*.

---

## Qué incluye (v2.0.17 nativa · Material 3 Expressive)

- **Acciones por tiempo y bloques reales**: retos **Rápidos (2 min)**, **Medios (15 min)** con temporizador anti-trampas (si sales de la app se pausa, si apagas la pantalla sigue contando) y retos **Semanales** de mayor impacto.
- **10 Biomas 3D interactivos**: cada objetivo completado evoluciona tu mundo (Valle Alpino, Jungla Lluviosa, Costa Mediterránea, Kioto, Oasis, Bali, Otoño, Fiordo, Atolón y Toscana) a lo largo de 8 etapas sobre el mapa del continente.
- **Muro Real y galería privada de recuerdos**: calendario personal de progreso y fotos recuerdo guardadas en tu móvil (puedes exportarlas a la galería del teléfono cuando quieras; cero feeds públicos, cero algoritmos).
- **3 Widgets nativos en tu pantalla de inicio**: Retos (`4×2`), Bioma (`2×2`) y Temporizador rápido (`4×2`), con tema automático claro/oscuro.
- **Actualizaciones automáticas dentro de la app**: desde la `v2.0.17`, Splash comprueba nuevas versiones publicadas en GitHub Releases y te deja actualizar en 1 toque.
- **100 % offline y privada**: tus hábitos, rachas y fotos se quedan en tu dispositivo.

---

## Descarga e instalación (Android)

1. Entra en la última versión en **[GitHub Releases](https://github.com/Epsilon10X/Splash/releases/latest)** y descarga el archivo `Splash-*.apk`.
2. Ábrelo en tu Android y permite la instalación desde esta fuente.
3. A partir de `v2.0.17`, la propia app te avisará automáticamente cuando salga una nueva actualización.

---

## Compilar APK localmente

Requisitos: Java 21 y Android SDK (incluido con Android Studio).

```bash
cd android-native
./gradlew assembleDebug
./gradlew assembleRelease
```

Los APKs generados aparecen en `android-native/app/build/outputs/apk/`.
