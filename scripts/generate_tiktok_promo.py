"""
Splash TikTok Promo Video Generator (1080x1920, 30s, 30fps, H.264 + AAC)
Renders kinetic typography, sleek Android phone mockups displaying real Splash screens,
isometric 3D voxel biomes, 3D flashcard flip, AMOLED timer, world map & circular memories,
synced to an original 120 BPM chill-electronic beat and Splash's completion chime.
"""

import math
import os
import subprocess
import sys
import wave
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

W, H = 1080, 1920
FPS = 30
DURATION_S = 30.0
TOTAL_FRAMES = int(FPS * DURATION_S)

ROOT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT_DIR = os.path.join(ROOT_DIR, "promo")
OUT_MP4 = os.path.join(OUT_DIR, "Splash-TikTok-Promo-30s.mp4")
ICON_PATH = os.path.join(
    ROOT_DIR,
    "android-native", "app", "src", "main", "res", "mipmap-xxxhdpi", "ic_launcher.png"
)
FFMPEG_BIN = "ffmpeg"

# Color palette (Material 3 Expressive 1.5 - Splash)
CREAM = (246, 244, 236)
CREAM_CARD = (255, 253, 248)
INK = (26, 34, 26)
INK_MUTED = (95, 107, 94)
EMERALD = (44, 122, 63)
EMERALD_BRIGHT = (106, 204, 108)
EMERALD_LIGHT = (212, 238, 210)
GOLD_BG1 = (246, 228, 178)
GOLD_BG2 = (234, 202, 126)
GOLD_INK = (72, 48, 8)
GOLD_ACCENT = (176, 118, 12)
QUICK_BG = (224, 244, 216)
QUICK_INK = (24, 62, 24)
AMOLED = (6, 8, 10)
PURE_BLACK = (0, 0, 0)
OCEAN_TOP = (74, 168, 202)
OCEAN_BOT = (40, 124, 158)
RED_ALERT = (224, 72, 64)


def ease_out_cubic(t):
    t = max(0.0, min(1.0, t))
    return 1.0 - (1.0 - t) ** 3


def ease_in_out(t):
    t = max(0.0, min(1.0, t))
    return 3 * t * t - 2 * t * t * t


def ease_out_back(t, s=1.70158):
    t = max(0.0, min(1.0, t)) - 1.0
    return 1.0 + (t * t * ((s + 1) * t + s))


def lerp(a, b, t):
    return a + (b - a) * t


def lerp_col(c1, c2, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(c1[i] + (c2[i] - c1[i]) * t)) for i in range(3))


def find_font(candidates, size):
    for p in candidates:
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size)
            except Exception:
                pass
    return ImageFont.load_default()


FONT_BLACK = [
    r"C:\Windows\Fonts\seguibl.ttf",
    r"C:\Windows\Fonts\ariblk.ttf",
    r"C:\Windows\Fonts\segoeuib.ttf",
    r"C:\Windows\Fonts\arialbd.ttf",
]
FONT_BOLD = [
    r"C:\Windows\Fonts\segoeuib.ttf",
    r"C:\Windows\Fonts\bahnschrift.ttf",
    r"C:\Windows\Fonts\arialbd.ttf",
]
FONT_REG = [
    r"C:\Windows\Fonts\segoeui.ttf",
    r"C:\Windows\Fonts\arial.ttf",
]

FONTS = {}


def get_font(kind, size):
    key = (kind, size)
    if key not in FONTS:
        cands = FONT_BLACK if kind == "black" else FONT_BOLD if kind == "bold" else FONT_REG
        FONTS[key] = find_font(cands, size)
    return FONTS[key]


def draw_centered_text(draw, cx, y, text, font, fill):
    bbox = draw.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    draw.text((cx - tw / 2.0, y), text, font=font, fill=fill)


def draw_pill(draw, cx, cy, text, font, bg_col, fg_col, pad_x=28, pad_y=12, outline=None, width=2):
    bbox = draw.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    rw = tw + pad_x * 2
    rh = th + pad_y * 2
    rect = [cx - rw / 2, cy - rh / 2, cx + rw / 2, cy + rh / 2]
    draw.rounded_rectangle(rect, radius=int(rh / 2), fill=bg_col, outline=outline, width=width if outline else 0)
    draw.text((cx - tw / 2, cy - th / 2 - bbox[1] / 2), text, font=font, fill=fg_col)


def load_rounded_icon(size_px):
    if os.path.exists(ICON_PATH):
        im = Image.open(ICON_PATH).convert("RGBA").resize((size_px, size_px), Image.Resampling.LANCZOS)
    else:
        im = Image.new("RGBA", (size_px, size_px), EMERALD + (255,))
    mask = Image.new("L", (size_px, size_px), 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle([0, 0, size_px - 1, size_px - 1], radius=int(size_px * 0.24), fill=255)
    out = Image.new("RGBA", (size_px, size_px), (0, 0, 0, 0))
    out.paste(im, (0, 0), mask)
    return out


def make_gradient_bg(top_col, bot_col):
    arr = np.zeros((H, W, 3), dtype=np.uint8)
    t = np.linspace(0.0, 1.0, H)[:, None, None]
    for c in range(3):
        arr[:, :, c] = np.clip(top_col[c] * (1.0 - t[:, :, 0]) + bot_col[c] * t[:, :, 0], 0, 255).astype(np.uint8)
    return Image.fromarray(arr, mode="RGB")


def shade_rgb(col, factor):
    return tuple(max(0, min(255, int(col[i] * factor))) for i in range(3))


def draw_iso_voxel_block(draw, cx, cy, half_w, half_h, h_px, top_col):
    # Diamond top
    pts_top = [(cx, cy - half_h), (cx + half_w, cy), (cx, cy + half_h), (cx - half_w, cy)]
    pts_left = [(cx - half_w, cy), (cx, cy + half_h), (cx, cy + half_h + h_px), (cx - half_w, cy + h_px)]
    pts_right = [(cx, cy + half_h), (cx + half_w, cy), (cx + half_w, cy + h_px), (cx, cy + half_h + h_px)]
    draw.polygon(pts_left, fill=shade_rgb(top_col, 0.78))
    draw.polygon(pts_right, fill=shade_rgb(top_col, 0.62))
    draw.polygon(pts_top, fill=top_col)


def render_diorama_card_art(w, h, biome="valle", rot_phase=0.0):
    img = Image.new("RGBA", (w, h), (240, 239, 229, 255))
    d = ImageDraw.Draw(img)
    if biome == "valle":
        bg = (242, 239, 224)
        base_cols = [(116, 180, 106), (86, 155, 88), (72, 123, 80)]
        water_col = (100, 198, 223)
        snow_col = (244, 248, 252)
    elif biome == "kioto":
        bg = (248, 235, 240)
        base_cols = [(126, 178, 124), (242, 184, 204), (108, 154, 112)]
        water_col = (109, 174, 182)
        snow_col = (248, 206, 220)
    else:
        bg = (232, 244, 238)
        base_cols = [(64, 138, 86), (42, 105, 65), (78, 150, 96)]
        water_col = (94, 210, 230)
        snow_col = (186, 246, 255)

    d.rectangle([0, 0, w, h], fill=bg)
    # Soft rainbow halo
    d.arc([w * 0.14, h * 0.08, w * 0.86, h * 0.86], 195, 345, fill=(224, 234, 214), width=5)

    ox = w * 0.50
    oy = h * 0.30 + math.sin(rot_phase) * 4.0
    hw, hh = w * 0.044, h * 0.032

    # 8x8 isometric mini-island back-to-front
    for sum_g in range(15):
        for gx in range(8):
            gy = sum_g - gx
            if gy < 0 or gy >= 8:
                continue
            sx = ox + (gx - gy) * hw
            sy = oy + (gx + gy) * hh
            is_river = (gx - gy) in (0, 1) and biome != "kioto" or (biome == "kioto" and gy == 4)
            is_peak = (gx <= 2 and gy <= 2)
            if is_river:
                col = water_col
                hz = 8
            elif is_peak:
                col = snow_col
                hz = 28 + (3 - gx) * 5
            else:
                col = base_cols[(gx + gy * 2) % len(base_cols)]
                hz = 16 + ((gx * 3 + gy) % 3) * 4
            draw_iso_voxel_block(d, sx, sy - hz * 0.45, hw * 0.98, hh * 0.98, hz, col)
            # Small cabin / tree
            if biome == "valle" and gx == 5 and gy == 2:
                draw_iso_voxel_block(d, sx, sy - hz * 0.45 - 12, hw * 0.55, hh * 0.55, 10, (245, 236, 214))
                draw_iso_voxel_block(d, sx, sy - hz * 0.45 - 20, hw * 0.62, hh * 0.62, 6, (198, 98, 72))
            elif biome == "kioto" and (gx, gy) in ((2, 5), (5, 2), (6, 5)):
                draw_iso_voxel_block(d, sx, sy - hz * 0.45 - 12, hw * 0.55, hh * 0.55, 11, (245, 174, 198))
            elif biome == "jungla" and (gx, gy) in ((2, 4), (4, 2), (5, 5)):
                draw_iso_voxel_block(d, sx, sy - hz * 0.45 - 14, hw * 0.56, hh * 0.56, 14, (36, 104, 62))
    return img


def draw_phone_frame(canvas, px, py, pw, ph, tilt_deg=0.0):
    shadow = Image.new("RGBA", (pw + 80, ph + 80), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    sd.rounded_rectangle([40, 46, pw + 40, ph + 46], radius=70, fill=(10, 20, 14, 95))
    shadow = shadow.filter(ImageFilter.GaussianBlur(22))
    if abs(tilt_deg) > 0.1:
        shadow = shadow.rotate(tilt_deg, resample=Image.Resampling.BICUBIC, expand=True)
    canvas.paste(shadow, (int(px - (shadow.width - pw) / 2), int(py - (shadow.height - ph) / 2)), shadow)


def composite_phone_screen(canvas, screen_img, px, py, pw, ph, tilt_deg=0.0):
    draw_phone_frame(canvas, px, py, pw, ph, tilt_deg)
    phone = Image.new("RGBA", (pw, ph), (0, 0, 0, 0))
    pd = ImageDraw.Draw(phone)
    # Bezel
    pd.rounded_rectangle([0, 0, pw - 1, ph - 1], radius=64, fill=(24, 28, 25, 255), outline=(78, 86, 80, 255), width=4)
    inset = 16
    sw, sh = pw - inset * 2, ph - inset * 2
    fitted = screen_img.resize((sw, sh), Image.Resampling.LANCZOS)
    mask = Image.new("L", (sw, sh), 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle([0, 0, sw - 1, sh - 1], radius=50, fill=255)
    phone.paste(fitted, (inset, inset), mask)
    # Top punch camera
    pd.ellipse([pw / 2 - 11, inset + 14, pw / 2 + 11, inset + 36], fill=(12, 14, 14, 255))
    if abs(tilt_deg) > 0.1:
        phone = phone.rotate(tilt_deg, resample=Image.Resampling.BICUBIC, expand=True)
        canvas.paste(phone, (int(px - (phone.width - pw) / 2), int(py - (phone.height - ph) / 2)), phone)
    else:
        canvas.paste(phone, (int(px), int(py)), phone)


def render_hoy_screen(sw, sh, icon_small, diorama_valle, flip_t, tap_t, plus4_t):
    scr = Image.new("RGBA", (sw, sh), CREAM + (255,))
    d = ImageDraw.Draw(scr)
    # Header
    scr.paste(icon_small, (32, 48), icon_small)
    d.text((92, 52), "Splash", font=get_font("bold", 30), fill=INK)
    draw_pill(d, sw - 178, 70, "7 días", get_font("bold", 17), (252, 232, 206), (138, 80, 12), pad_x=16, pad_y=8)
    draw_pill(d, sw - 78, 70, "24/80", get_font("bold", 17), EMERALD_LIGHT, QUICK_INK, pad_x=15, pad_y=8)

    # Biome Card
    bx0, by0, bx1, by1 = 26, 120, sw - 26, 515
    d.rounded_rectangle([bx0, by0, bx1, by1], radius=34, fill=CREAM_CARD, outline=(228, 224, 212), width=2)
    dw, dh = bx1 - bx0 - 24, 265
    d_art = diorama_valle.resize((dw, dh), Image.Resampling.LANCZOS)
    d_mask = Image.new("L", (dw, dh), 0)
    ImageDraw.Draw(d_mask).rounded_rectangle([0, 0, dw - 1, dh - 1], radius=24, fill=255)
    scr.paste(d_art, (bx0 + 12, by0 + 12), d_mask)
    draw_pill(d, bx1 - 66, by0 + 42, "3D", get_font("bold", 17), (255, 255, 255), INK, pad_x=16, pad_y=7)
    d.text((bx0 + 24, by0 + 296), "Valle Alpino", font=get_font("bold", 25), fill=INK)
    draw_pill(d, bx1 - 110, by0 + 308, "Nv. 3 · Radiante", get_font("bold", 16), EMERALD_LIGHT, QUICK_INK, pad_x=14, pad_y=6)
    # Progress bar
    d.rounded_rectangle([bx0 + 24, by0 + 350, bx1 - 24, by0 + 362], radius=6, fill=(226, 228, 220))
    d.rounded_rectangle([bx0 + 24, by0 + 350, bx0 + 24 + int((bx1 - bx0 - 48) * 0.48), by0 + 362], radius=6, fill=EMERALD)

    # Segmented Time Buckets
    py = 540
    bw = (sw - 68) / 3
    draw_pill(d, 26 + bw * 0.5, py + 24, "2 min", get_font("bold", 20), (234, 232, 224), INK_MUTED, pad_x=36, pad_y=12)
    draw_pill(d, 34 + bw * 1.5, py + 24, "15 min", get_font("bold", 20), (242, 198, 78), GOLD_INK, pad_x=36, pad_y=12)
    draw_pill(d, 42 + bw * 2.5, py + 24, "Semanal", get_font("bold", 20), (234, 232, 224), INK_MUTED, pad_x=30, pad_y=12)

    d.text((28, 608), "15 min · Bioma hoy: 1/2 (+4 pts)", font=get_font("bold", 19), fill=INK_MUTED)

    # Two Mission Cards
    cy0, cy1 = 650, 968
    cw = (sw - 68) / 2

    # Card 1 (Biome - simulated 3D horizontal flip around center)
    c1x0, c1x1 = 26, 26 + cw
    ang = flip_t * math.pi
    width_scale = max(0.06, abs(math.cos(ang)))
    cx_mid = (c1x0 + c1x1) / 2
    half_c = (cw * 0.5) * width_scale
    show_back = flip_t > 0.5
    d.rounded_rectangle([cx_mid - half_c, cy0, cx_mid + half_c, cy1], radius=28, fill=GOLD_BG1, outline=(212, 162, 52), width=3)
    if width_scale > 0.38:
        tx0 = cx_mid - half_c + 18
        if not show_back:
            draw_pill(d, tx0 + 52 * width_scale, cy0 + 34, "Valle", get_font("bold", 16), (255, 246, 218), GOLD_ACCENT, pad_x=14, pad_y=6)
            d.text((tx0, cy0 + 74), "Caminar por", font=get_font("bold", 22), fill=GOLD_INK)
            d.text((tx0, cy0 + 104), "zona verde", font=get_font("bold", 22), fill=GOLD_INK)
        else:
            draw_pill(d, tx0 + 52 * width_scale, cy0 + 34, "Valle", get_font("bold", 16), (255, 246, 218), GOLD_ACCENT, pad_x=14, pad_y=6)
            d.text((tx0, cy0 + 72), "Sal 15 min sin", font=get_font("reg", 19), fill=GOLD_INK)
            d.text((tx0, cy0 + 98), "mirar pantallas", font=get_font("reg", 19), fill=GOLD_INK)
            d.text((tx0, cy0 + 124), "y respira fuera.", font=get_font("reg", 19), fill=GOLD_INK)
        btn_scale = 1.0 - 0.08 * math.sin(tap_t * math.pi)
        b_pad = 16 + (1.0 - btn_scale) * 24
        d.rounded_rectangle([cx_mid - half_c + b_pad, cy1 - 74, cx_mid + half_c - b_pad, cy1 - 20], radius=26, fill=GOLD_ACCENT)
        if width_scale > 0.55:
            draw_centered_text(d, cx_mid, cy1 - 60, "Completar", get_font("bold", 20), (255, 255, 255))

    # Tap ring & floating +4 pts
    if 0.05 < tap_t < 0.95:
        r = 18 + tap_t * 48
        d.ellipse([cx_mid - r, cy1 - 47 - r, cx_mid + r, cy1 - 47 + r], outline=(255, 255, 255, 200), width=4)
    if plus4_t > 0.01:
        fy = cy1 - 92 - ease_out_cubic(plus4_t) * 85
        draw_pill(d, cx_mid, fy, "+4 pts bioma", get_font("black", 22), EMERALD, (255, 255, 255), pad_x=20, pad_y=10)

    # Card 2 (General)
    c2x0, c2x1 = 26 + cw + 16, sw - 26
    d.rounded_rectangle([c2x0, cy0, c2x1, cy1], radius=28, fill=QUICK_BG, outline=(168, 214, 162), width=2)
    draw_pill(d, c2x0 + 72, cy0 + 34, "Desconexión", get_font("bold", 16), (255, 255, 255), QUICK_INK, pad_x=14, pad_y=6)
    d.text((c2x0 + 18, cy0 + 74), "Café offline", font=get_font("bold", 22), fill=QUICK_INK)
    d.text((c2x0 + 18, cy0 + 112), "Disfruta tu bebida", font=get_font("reg", 17), fill=(46, 86, 46))
    d.text((c2x0 + 18, cy0 + 136), "sin mirar el móvil.", font=get_font("reg", 17), fill=(46, 86, 46))
    d.rounded_rectangle([c2x0 + 16, cy1 - 74, c2x1 - 16, cy1 - 20], radius=26, fill=EMERALD)
    draw_centered_text(d, (c2x0 + c2x1) / 2, cy1 - 60, "Completar", get_font("bold", 20), (255, 255, 255))

    # Bottom floating toolbar
    ny = sh - 104
    d.rounded_rectangle([30, ny, sw - 118, ny + 72], radius=36, fill=(255, 255, 255), outline=(218, 218, 210), width=2)
    draw_pill(d, 96, ny + 36, "Hoy", get_font("bold", 17), EMERALD_LIGHT, QUICK_INK, pad_x=22, pad_y=9)
    draw_centered_text(d, 204, ny + 25, "Muro", get_font("bold", 17), INK_MUTED)
    draw_centered_text(d, 314, ny + 25, "Catálogo", get_font("bold", 17), INK_MUTED)
    d.ellipse([sw - 104, ny + 3, sw - 38, ny + 69], fill=(255, 255, 255), outline=(218, 218, 210), width=2)
    draw_centered_text(d, sw - 71, ny + 22, "3D", get_font("bold", 18), EMERALD)
    return scr


def render_timer_screen(sw, sh, rem_sec, total_sec, finished):
    scr = Image.new("RGBA", (sw, sh), PURE_BLACK + (255,))
    d = ImageDraw.Draw(scr)
    draw_pill(d, sw / 2, 130, "Modo OLED · Sin Pantalla", get_font("bold", 20), (35, 71, 23), (226, 244, 200), pad_x=22, pad_y=10)
    draw_centered_text(d, sw / 2, 186, "Sal 15 min sin mirar pantallas", font=get_font("bold", 28), fill=(255, 255, 255))

    # Ring
    cx, cy, r = sw / 2, 505, 178
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=(30, 35, 42), width=16)
    frac = max(0.0, min(1.0, rem_sec / max(1.0, total_sec)))
    ring_col = (102, 227, 156) if finished else (155, 224, 107)
    if finished:
        d.ellipse([cx - r - 12, cy - r - 12, cx + r + 12, cy + r + 12], outline=(102, 227, 156, 90), width=8)
    end_ang = -90 + 360 * frac
    if frac > 0.01:
        d.arc([cx - r, cy - r, cx + r, cy + r], -90, end_ang, fill=ring_col, width=16)

    mm = int(rem_sec) // 60
    ss = int(rem_sec) % 60
    draw_centered_text(d, cx, cy - 48, f"{mm:02d}:{ss:02d}", get_font("black", 76), (255, 255, 255))
    status = "Tiempo completado" if finished else "En curso · apaga la pantalla"
    draw_centered_text(d, cx, cy + 44, status, get_font("bold", 19), (167, 174, 169))

    # Buttons
    btn_y = 755
    if not finished:
        d.rounded_rectangle([54, btn_y, sw - 54, btn_y + 74], radius=37, fill=(29, 36, 44))
        draw_centered_text(d, sw / 2, btn_y + 22, "Pausar temporizador", get_font("bold", 22), (255, 255, 255))
    else:
        draw_pill(d, sw / 2, btn_y + 34, "Sonido suave completado", get_font("bold", 20), (30, 54, 22), (226, 244, 200), pad_x=26, pad_y=12)

    d.rounded_rectangle([54, btn_y + 98, sw - 54, btn_y + 174], radius=38, fill=(155, 224, 107) if finished else (35, 50, 36))
    draw_centered_text(
        d,
        sw / 2,
        btn_y + 120,
        "Completar objetivo",
        get_font("bold", 24),
        (20, 48, 8) if finished else (226, 244, 200),
    )
    return scr


def render_map_screen(sw, sh, progress_t):
    scr = Image.new("RGBA", (sw, sh), CREAM + (255,))
    d = ImageDraw.Draw(scr)
    d.text((30, 56), "Muro Real", font=get_font("black", 34), fill=INK)
    bw = (sw - 84) / 4
    for i, lbl in enumerate(["Muro", "Recuerdos", "Círculo", "Mapa"]):
        sel = i == 3
        draw_pill(
            d,
            30 + bw * (i + 0.5) + i * 8,
            128,
            lbl,
            get_font("bold", 16),
            EMERALD_LIGHT if sel else (238, 236, 228),
            QUICK_INK if sel else INK_MUTED,
            pad_x=16,
            pad_y=10,
        )

    # Ocean Map Canvas
    mx0, my0, mx1, my1 = 26, 178, sw - 26, 860
    d.rounded_rectangle([mx0, my0, mx1, my1], radius=34, fill=OCEAN_TOP)
    # Continent organic island
    mw, mh = mx1 - mx0, my1 - my0
    zoom = 1.0 + 0.25 * math.sin(progress_t * math.pi)
    icx, icy = mx0 + mw * 0.50, my0 + mh * 0.53
    rx, ry = mw * 0.42 * zoom, mh * 0.36 * zoom

    # White shoreline halo & plinth
    d.ellipse([icx - rx * 1.08, icy - ry * 1.08, icx + rx * 1.08, icy + ry * 1.08], fill=(120, 208, 230))
    d.ellipse([icx - rx, icy - ry + 18, icx + rx, icy + ry + 18], fill=(90, 110, 118))
    d.ellipse([icx - rx, icy - ry, icx + rx, icy + ry], fill=(144, 198, 126), outline=(255, 255, 255), width=3)

    zones = [
        (-0.46, -0.36, (108, 174, 102), "VALLE ALPINO"),
        (-0.02, -0.44, (192, 214, 230), "FIORDO"),
        (0.42, -0.32, (232, 184, 108), "OASIS"),
        (-0.48, 0.10, (54, 126, 78), "JUNGLA"),
        (0.02, -0.02, (242, 186, 204), "KIOTO"),
        (0.45, 0.14, (222, 128, 82), "OTOÑO"),
        (-0.24, 0.44, (234, 208, 158), "COSTA"),
        (0.28, 0.44, (104, 206, 202), "ATOLÓN"),
    ]
    for fx, fy, col, name in zones:
        zx, zy = icx + fx * rx, icy + fy * ry
        r_blob = 56 * zoom
        d.ellipse([zx - r_blob * 1.15, zy - r_blob * 0.72, zx + r_blob * 1.15, zy + r_blob * 0.72], fill=col)

    # River across center
    d.line([(icx - 10, icy - ry * 0.85), (icx + 14, icy - 20), (icx - 8, icy + ry * 0.85)], fill=(86, 192, 216), width=18)

    for fx, fy, _, name in zones:
        zx, zy = icx + fx * rx, icy + fy * ry
        draw_pill(d, zx, zy + 18, name, get_font("bold", 12), (255, 253, 248), INK, pad_x=10, pad_y=4)

    draw_pill(d, icx - 0.46 * rx, icy - 0.36 * ry - 26, "Tú · Nv. 3", get_font("bold", 14), EMERALD, (255, 255, 255), pad_x=12, pad_y=5)
    draw_pill(d, (mx0 + mx1) / 2 - 42, my0 + 36, "Pellizca · Toca un bioma", get_font("bold", 14), (255, 255, 255), INK, pad_x=16, pad_y=8)

    # Bottom info card
    d.rounded_rectangle([26, 882, sw - 26, 982], radius=24, fill=CREAM_CARD)
    d.text((48, 902), "10 biomas 3D por descubrir", font=get_font("bold", 22), fill=INK)
    d.text((48, 938), "Cada objetivo cumplido añade árboles, ríos y luz.", font=get_font("reg", 17), fill=INK_MUTED)
    return scr


def render_memories_screen(sw, sh, pop_t):
    scr = Image.new("RGBA", (sw, sh), CREAM + (255,))
    d = ImageDraw.Draw(scr)
    d.text((30, 56), "Muro Real", font=get_font("black", 34), fill=INK)
    bw = (sw - 84) / 4
    for i, lbl in enumerate(["Muro", "Recuerdos", "Círculo", "Mapa"]):
        sel = i == 1
        draw_pill(
            d,
            30 + bw * (i + 0.5) + i * 8,
            128,
            lbl,
            get_font("bold", 16),
            EMERALD_LIGHT if sel else (238, 236, 228),
            QUICK_INK if sel else INK_MUTED,
            pad_x=16,
            pad_y=10,
        )

    d.rounded_rectangle([26, 176, sw - 26, 294], radius=24, fill=CREAM_CARD)
    d.text((46, 196), "Fotos recuerdos", font=get_font("bold", 24), fill=INK)
    d.text((46, 232), "Momentos reales lejos de la pantalla", font=get_font("reg", 17), fill=INK_MUTED)

    samples = [
        ("Amanecer alpino", (86, 155, 88), (154, 213, 245), (255, 224, 140)),
        ("Atardecer sin móvil", (224, 133, 91), (246, 158, 123), (75, 166, 189)),
        ("Paseo cerezos", (224, 138, 162), (255, 229, 238), (208, 79, 67)),
        ("Cascada escondida", (62, 140, 91), (154, 224, 206), (93, 199, 219)),
        ("Merienda dorada", (209, 164, 71), (254, 229, 178), (156, 180, 96)),
        ("Noche estrellas", (76, 199, 196), (14, 35, 58), (255, 244, 194)),
    ]
    for idx, (title, ring, bg1, bg2) in enumerate(samples):
        col = idx % 2
        row = idx // 2
        cx = 152 if col == 0 else sw - 152
        cy = 374 + row * 194
        st = ease_out_back(max(0.0, min(1.0, (pop_t - idx * 0.08) * 1.9)))
        rr = int(74 * max(0.05, st))
        if rr > 8:
            d.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=bg1, outline=ring, width=6)
            d.chord([cx - rr + 6, cy - rr + 6, cx + rr - 6, cy + rr - 6], 20, 160, fill=bg2)
            d.ellipse([cx + int(rr * 0.14), cy - int(rr * 0.42), cx + int(rr * 0.46), cy - int(rr * 0.10)], fill=bg2)
        if st > 0.6:
            draw_centered_text(d, cx, cy + 82, title, get_font("bold", 15), INK)
    return scr


def synthesize_audio(wav_path):
    sr = 44100
    n_samples = int(sr * DURATION_S)
    t = np.arange(n_samples, dtype=np.float64) / sr
    mix = np.zeros(n_samples, dtype=np.float64)

    # 120 BPM => beat = 0.5s, 8th = 0.25s, bar = 2.0s
    beat_len = 0.5
    bar_len = 2.0

    # Soft Kick on every beat (muted gently in first 1.8s intro)
    kick_env_mask = np.where(t < 1.8, 0.35, 1.0)
    beat_phase = np.mod(t, beat_len)
    kick_gate = np.exp(-beat_phase * 22.0) * (beat_phase < 0.22)
    kick_freq = 54.0 + 75.0 * np.exp(-beat_phase * 38.0)
    kick = np.sin(2.0 * np.pi * kick_freq * beat_phase) * kick_gate * 0.30 * kick_env_mask
    mix += kick

    # Sidechain pump from kick
    sidechain = 1.0 - 0.38 * np.exp(-beat_phase * 14.0)

    # Warm chord progression every 2.0s: Fmaj7 -> G6 -> Em7 -> Am7
    chords = [
        [174.61, 220.00, 261.63, 329.63],  # Fmaj7
        [196.00, 246.94, 293.66, 329.63],  # G6
        [164.81, 196.00, 246.94, 293.66],  # Em7
        [220.00, 261.63, 329.63, 392.00],  # Am7
    ]
    bass_roots = [87.31, 98.00, 82.41, 110.00]
    bar_idx = (t / bar_len).astype(int) % len(chords)
    bar_phase = np.mod(t, bar_len)

    pad = np.zeros(n_samples, dtype=np.float64)
    bass = np.zeros(n_samples, dtype=np.float64)
    for b_i, chord_freqs in enumerate(chords):
        mask = (bar_idx == b_i)
        if not np.any(mask):
            continue
        tm = t[mask]
        chord_sig = sum(np.sin(2.0 * np.pi * f * tm) for f in chord_freqs) * 0.24
        chord_sig += sum(np.sin(2.0 * np.pi * (f * 2.0) * tm) for f in chord_freqs[:2]) * 0.05
        pad[mask] = chord_sig
        bf = bass_roots[b_i]
        bass[mask] = (np.sin(2.0 * np.pi * bf * tm) + 0.25 * np.sin(4.0 * np.pi * bf * tm)) * 0.24

    pad_env = np.minimum(1.0, bar_phase / 0.06) * np.exp(-bar_phase * 0.45)
    mix += pad * pad_env * sidechain * np.where(t < 1.8, 0.45, 0.85)
    mix += bass * np.exp(-np.mod(t, 0.5) * 3.5) * sidechain * np.where(t < 1.8, 0.25, 0.80)

    # Crispy hi-hats on off-beats after 1.8s + snare claps on beats 2 & 4
    rng = np.random.default_rng(42)
    noise = rng.standard_normal(n_samples)
    hat_phase = np.mod(t, 0.25)
    hat_hit = ((np.floor(t / 0.25).astype(int) % 2) == 1) & (t >= 1.8)
    mix += np.where(hat_hit, noise * np.exp(-hat_phase * 75.0) * 0.055, 0.0)

    clap_phase = np.mod(t - 0.5, 1.0)
    clap_hit = (t >= 2.0) & (clap_phase >= 0.0) & (clap_phase < 0.14)
    mix += np.where(clap_hit, noise * np.exp(-clap_phase * 36.0) * 0.095, 0.0)

    # Riser before 1.8s drop
    drop_mask = (t >= 1.2) & (t < 1.8)
    mix[drop_mask] += noise[drop_mask] * ((t[drop_mask] - 1.2) / 0.6) ** 2 * 0.08

    # Soft scene transition whooshes
    for wt in [1.8, 3.5, 8.5, 13.5, 19.5, 24.5]:
        wm = (t >= wt - 0.14) & (t < wt + 0.14)
        dt = (t[wm] - (wt - 0.14)) / 0.28
        w_env = np.sin(dt * np.pi)
        mix[wm] += (noise[wm] * 0.04 + np.sin(2 * np.pi * (240 + 480 * dt) * t[wm]) * 0.04) * w_env

    # Tap pop at 6.8s
    pop_m = (t >= 6.8) & (t < 6.92)
    pt = t[pop_m] - 6.8
    mix[pop_m] += np.sin(2.0 * np.pi * (520.0 + 380.0 * np.exp(-pt * 30.0)) * pt) * np.exp(-pt * 45.0) * 0.24

    # Splash signature C5->G5 gentle completion chime at 12.15s and 25.1s
    for ct in [12.15, 25.10]:
        cm1 = (t >= ct) & (t < ct + 0.9)
        t1 = t[cm1] - ct
        env1 = np.minimum(1.0, t1 / 0.02) * np.exp(-t1 * 4.5)
        mix[cm1] += (np.sin(2 * np.pi * 523.25 * t1) * 0.78 + np.sin(2 * np.pi * 1046.5 * t1) * 0.22) * env1 * 0.30

        cm2 = (t >= ct + 0.20) & (t < ct + 1.15)
        t2 = t[cm2] - (ct + 0.20)
        env2 = np.minimum(1.0, t2 / 0.025) * np.exp(-t2 * 3.2)
        mix[cm2] += (np.sin(2 * np.pi * 783.99 * t2) * 0.80 + np.sin(2 * np.pi * 1567.98 * t2) * 0.18) * env2 * 0.32

    # Master fade in/out + soft limiter
    mix *= np.minimum(1.0, t / 0.08) * np.minimum(1.0, (DURATION_S - t) / 0.6)
    mix = np.tanh(mix * 1.15) * 0.88

    stereo = np.stack([mix, mix * 0.98], axis=1)
    pcm = (np.clip(stereo, -1.0, 1.0) * 32767.0).astype(np.int16)
    with wave.open(wav_path, "wb") as wf:
        wf.setnchannels(2)
        wf.setsampwidth(2)
        wf.setframerate(sr)
        wf.writeframes(pcm.tobytes())


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    tmp_wav = os.path.join(OUT_DIR, "_promo_audio.wav")
    print("Synthesizing beat-synced audio...")
    synthesize_audio(tmp_wav)

    print("Caching base assets...")
    bg_cream = make_gradient_bg((248, 246, 238), (229, 240, 224))
    bg_dark = make_gradient_bg((10, 14, 12), (4, 6, 5))
    bg_finale = make_gradient_bg((236, 246, 232), (247, 242, 224))

    icon_hero = load_rounded_icon(240)
    icon_small = load_rounded_icon(46)
    diorama_valle = render_diorama_card_art(470, 265, biome="valle", rot_phase=0.0)
    diorama_kioto = render_diorama_card_art(320, 210, biome="kioto", rot_phase=0.4)
    diorama_jungla = render_diorama_card_art(320, 210, biome="jungla", rot_phase=0.8)

    pw, ph = 560, 1080
    px_center = (W - pw) / 2.0
    py_center = 490.0

    cmd = [
        FFMPEG_BIN, "-y",
        "-f", "rawvideo",
        "-vcodec", "rawvideo",
        "-s", f"{W}x{H}",
        "-pix_fmt", "rgb24",
        "-r", str(FPS),
        "-i", "-",
        "-i", tmp_wav,
        "-c:v", "libx264",
        "-preset", "medium",
        "-crf", "19",
        "-pix_fmt", "yuv420p",
        "-c:a", "aac",
        "-b:a", "192k",
        "-shortest",
        "-movflags", "+faststart",
        OUT_MP4,
    ]
    proc = subprocess.Popen(cmd, stdin=subprocess.PIPE)

    try:
        for f_idx in range(TOTAL_FRAMES):
            t = f_idx / float(FPS)

            # SCENE 1: 0.0 - 3.5s Hook + Reveal
            if t < 3.5:
                if t < 1.8:
                    canvas = bg_dark.copy().convert("RGBA")
                    d = ImageDraw.Draw(canvas)
                    lt = t / 1.8
                    sc = ease_out_back(min(1.0, lt * 3.0))
                    # Animated scroll-time counter
                    mins = int(12 + ease_in_out(lt) * 155)
                    hh, mm = mins // 60, mins % 60
                    draw_pill(d, W / 2, 470, f"{hh:02d}:{mm:02d} HOY EN REDES", get_font("black", 36), (48, 22, 22), (255, 128, 120), pad_x=34, pad_y=16, outline=RED_ALERT, width=3)
                    draw_centered_text(d, W / 2, 620, "¿OTRA VEZ 2 HORAS", get_font("black", int(68 * sc)), (255, 255, 255))
                    draw_centered_text(d, W / 2, 710, "SCROLLEANDO?", get_font("black", int(74 * sc)), EMERALD_BRIGHT)
                    draw_centered_text(d, W / 2, 860, "Y otro día sin hacer nada por ti...", get_font("bold", 34), (168, 178, 170))
                else:
                    lt = (t - 1.8) / 1.7
                    canvas = bg_cream.copy().convert("RGBA")
                    d = ImageDraw.Draw(canvas)
                    glow_r = 160 + ease_out_cubic(min(1.0, lt * 1.4)) * 220
                    d.ellipse([W / 2 - glow_r, 640 - glow_r, W / 2 + glow_r, 640 + glow_r], fill=(214, 240, 210))
                    bsc = ease_out_back(min(1.0, lt * 2.2))
                    isize = max(40, int(240 * bsc))
                    ic = icon_hero.resize((isize, isize), Image.Resampling.LANCZOS)
                    canvas.paste(ic, (int(W / 2 - isize / 2), int(640 - isize / 2)), ic)
                    draw_centered_text(d, W / 2, 830, "CONOCE SPLASH", get_font("black", 82), INK)
                    draw_pill(d, W / 2, 975, "Vive fuera de la pantalla", get_font("bold", 36), EMERALD, (255, 255, 255), pad_x=40, pad_y=18)

            # SCENE 2: 3.5 - 8.5s Hoy Screen + 3D Flashcard Flip
            elif t < 8.5:
                lt = (t - 3.5) / 5.0
                canvas = bg_cream.copy().convert("RGBA")
                d = ImageDraw.Draw(canvas)
                draw_pill(d, W / 2, 200, "PASO 1 · EN MENOS DE 10 SEGUNDOS", get_font("bold", 24), EMERALD_LIGHT, QUICK_INK, pad_x=24, pad_y=10)
                draw_centered_text(d, W / 2, 246, "ELIGE UN RETO REAL", get_font("black", 66), INK)
                draw_centered_text(d, W / 2, 332, "Micro-misiones de 2 o 15 min sin pantalla", get_font("bold", 30), INK_MUTED)

                flip_t = ease_in_out(max(0.0, min(1.0, (t - 4.8) / 1.1)))
                tap_t = max(0.0, min(1.0, (t - 6.7) / 0.6))
                plus4_t = max(0.0, min(1.0, (t - 6.95) / 1.1))
                scr = render_hoy_screen(pw - 32, ph - 32, icon_small, diorama_valle, flip_t, tap_t, plus4_t)
                slide_y = (1.0 - ease_out_cubic(min(1.0, lt * 4.5))) * 140.0
                tilt = math.sin(lt * math.pi) * -1.8
                composite_phone_screen(canvas, scr, px_center, py_center + slide_y, pw, ph, tilt_deg=tilt)
                if t > 5.0:
                    draw_pill(d, W / 2, 1626, "Toca y hazlo en la vida real", get_font("bold", 28), INK, (255, 255, 255), pad_x=32, pad_y=14)

            # SCENE 3: 8.5 - 13.5s AMOLED Focus Timer
            elif t < 13.5:
                lt = (t - 8.5) / 5.0
                canvas = bg_dark.copy().convert("RGBA")
                d = ImageDraw.Draw(canvas)
                draw_pill(d, W / 2, 200, "PASO 2 · TEMPORIZADOR ANTI-TRAMPAS", get_font("bold", 23), (35, 71, 23), (226, 244, 200), pad_x=24, pad_y=10)
                draw_centered_text(d, W / 2, 246, "APAGA LA PANTALLA", get_font("black", 66), (255, 255, 255))
                draw_centered_text(d, W / 2, 332, "El tiempo sigue contando aunque bloquees el móvil", get_font("bold", 28), (168, 182, 170))

                rem = max(0.0, 12.0 * (1.0 - min(1.0, (t - 8.8) / 3.35)))
                finished = t >= 12.15
                scr = render_timer_screen(pw - 32, ph - 32, rem, 12.0, finished)
                composite_phone_screen(canvas, scr, px_center, py_center, pw, ph, tilt_deg=math.sin(lt * math.pi * 2) * 1.2)

                # Floating callout chips
                if lt > 0.12:
                    draw_pill(d, 265, 1615, "Apagas pantalla: sigue", get_font("bold", 24), (24, 58, 28), (220, 248, 210), pad_x=24, pad_y=12)
                if lt > 0.25:
                    draw_pill(d, 745, 1615, "Si sales: se pausa", get_font("bold", 24), (58, 34, 22), (255, 214, 168), pad_x=24, pad_y=12)

            # SCENE 4: 13.5 - 19.5s 10 Living 3D Biomes + World Map
            elif t < 19.5:
                lt = (t - 13.5) / 6.0
                canvas = bg_cream.copy().convert("RGBA")
                d = ImageDraw.Draw(canvas)
                draw_pill(d, W / 2, 200, "PASO 3 · PROGRESO VISUAL REAL", get_font("bold", 24), EMERALD_LIGHT, QUICK_INK, pad_x=24, pad_y=10)
                draw_centered_text(d, W / 2, 246, "DA VIDA A 10 MUNDOS 3D", get_font("black", 60), INK)
                draw_centered_text(d, W / 2, 332, "Árboles, ríos, templos y fauna con tus hábitos", get_font("bold", 29), INK_MUTED)

                scr = render_map_screen(pw - 32, ph - 32, lt)
                composite_phone_screen(canvas, scr, px_center - 45, py_center, pw, ph, tilt_deg=-1.5)

                # Pop-out 3D biome preview cards on right side (kept inside x <= 930 safe zone)
                if lt > 0.14:
                    p1 = ease_out_back(min(1.0, (lt - 0.14) * 3.0))
                    kw, kh = 310, 236
                    k_card = Image.new("RGBA", (kw, kh), CREAM_CARD + (255,))
                    kd = ImageDraw.Draw(k_card)
                    kd.rounded_rectangle([0, 0, kw - 1, kh - 1], radius=26, fill=CREAM_CARD, outline=(224, 138, 162), width=3)
                    k_card.paste(diorama_kioto.resize((kw - 16, 174), Image.Resampling.LANCZOS), (8, 8))
                    kd.text((18, 192), "Kioto · Nv. 6", font=get_font("bold", 22), fill=INK)
                    canvas.paste(k_card, (int(620 + (1.0 - p1) * 140), 700), k_card)

                if lt > 0.34:
                    p2 = ease_out_back(min(1.0, (lt - 0.34) * 3.0))
                    jw, jh = 310, 236
                    j_card = Image.new("RGBA", (jw, jh), CREAM_CARD + (255,))
                    jd = ImageDraw.Draw(j_card)
                    jd.rounded_rectangle([0, 0, jw - 1, jh - 1], radius=26, fill=CREAM_CARD, outline=EMERALD, width=3)
                    j_card.paste(diorama_jungla.resize((jw - 16, 174), Image.Resampling.LANCZOS), (8, 8))
                    jd.text((18, 192), "Jungla · Cascada 3D", font=get_font("bold", 21), fill=INK)
                    canvas.paste(j_card, (int(620 + (1.0 - p2) * 140), 980), j_card)

                draw_pill(d, W / 2, 1626, "Valle · Kioto · Costa · Jungla · Oasis y más", get_font("bold", 26), INK, (255, 255, 255), pad_x=28, pad_y=12)

            # SCENE 5: 19.5 - 24.5s Circular Photo Memories & Home Widget
            elif t < 24.5:
                lt = (t - 19.5) / 5.0
                canvas = bg_cream.copy().convert("RGBA")
                d = ImageDraw.Draw(canvas)
                draw_pill(d, W / 2, 200, "TUS MOMENTOS OFF-SCREEN", get_font("bold", 24), EMERALD_LIGHT, QUICK_INK, pad_x=24, pad_y=10)
                draw_centered_text(d, W / 2, 246, "MURO DE RECUERDOS", get_font("black", 64), INK)
                draw_centered_text(d, W / 2, 332, "Guarda tus fotos reales · Sin likes ni postureo", get_font("bold", 29), INK_MUTED)

                scr = render_memories_screen(pw - 32, ph - 32, min(1.0, lt * 1.4))
                composite_phone_screen(canvas, scr, px_center - 40, py_center, pw, ph, tilt_deg=1.4)

                # Floating Home Screen Widget Card
                if lt > 0.25:
                    wp = ease_out_back(min(1.0, (lt - 0.25) * 3.0))
                    wx, wy, ww, wh = int(530 + (1.0 - wp) * 120), 1370, 390, 210
                    d.rounded_rectangle([wx, wy, wx + ww, wy + wh], radius=32, fill=(30, 36, 31), outline=(110, 178, 114), width=3)
                    d.text((wx + 24, wy + 22), "Widget Inicio · Splash", font=get_font("bold", 22), fill=(255, 255, 255))
                    d.text((wx + 24, wy + 54), "Valle Nv. 3 · En calma", font=get_font("reg", 18), fill=(176, 198, 176))
                    for ci, cl in enumerate(["2 min", "15 min", "30 min"]):
                        cxw = wx + 76 + ci * 118
                        draw_pill(d, cxw, wy + 144, cl, get_font("bold", 19), (58, 86, 60), (226, 246, 224), pad_x=18, pad_y=12)

            # SCENE 6: 24.5 - 30.0s Viral CTA Finale
            else:
                lt = (t - 24.5) / 5.5
                canvas = bg_finale.copy().convert("RGBA")
                d = ImageDraw.Draw(canvas)
                pulse = 1.0 + 0.04 * math.sin(lt * 10.0)
                glow_r = int(220 * pulse)
                d.ellipse([W / 2 - glow_r, 560 - glow_r, W / 2 + glow_r, 560 + glow_r], fill=(208, 238, 204))
                ic_sc = ease_out_back(min(1.0, lt * 2.5))
                isize = max(48, int(248 * ic_sc))
                ic = icon_hero.resize((isize, isize), Image.Resampling.LANCZOS)
                canvas.paste(ic, (int(W / 2 - isize / 2), int(560 - isize / 2)), ic)

                draw_centered_text(d, W / 2, 745, "MENOS SCROLL.", get_font("black", 82), INK)
                draw_centered_text(d, W / 2, 845, "MÁS VIDA REAL.", get_font("black", 82), EMERALD)

                badges = [
                    ("100% Offline", (222, 244, 218), QUICK_INK),
                    ("Sin anuncios", (252, 236, 194), GOLD_INK),
                    ("10 Biomas 3D", (214, 236, 248), (18, 66, 94)),
                ]
                for bi, (blbl, bbg, bfg) in enumerate(badges):
                    if lt > 0.08 + bi * 0.08:
                        draw_pill(d, 215 + bi * 320, 1020, blbl, get_font("bold", 26), bbg, bfg, pad_x=24, pad_y=14)

                draw_centered_text(d, W / 2, 1140, "Prueba la Beta de SPLASH en Android", get_font("bold", 38), INK)
                draw_pill(d, W / 2, 1285, "Comenta 'SPLASH' para el enlace", get_font("black", 34), INK, (255, 255, 255), pad_x=44, pad_y=22)

            frame_rgb = canvas.convert("RGB")
            proc.stdin.write(frame_rgb.tobytes())
    finally:
        proc.stdin.close()
        rc = proc.wait()
        if os.path.exists(tmp_wav):
            try:
                os.remove(tmp_wav)
            except Exception:
                pass
        if rc != 0:
            raise RuntimeError(f"ffmpeg failed with exit code {rc}")

    print(f"Generated {OUT_MP4}")


if __name__ == "__main__":
    main()
