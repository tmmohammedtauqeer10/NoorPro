from __future__ import annotations

import math
import subprocess
from pathlib import Path

import imageio_ffmpeg
from PIL import Image, ImageDraw, ImageFilter, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "promo"
OUT_DIR.mkdir(exist_ok=True)
VIDEO_PATH = OUT_DIR / "noor_pro_feature_ad.mp4"
THUMB_PATH = OUT_DIR / "noor_pro_feature_ad_thumbnail.jpg"
LOGO_PATH = Path(r"C:\Users\tmmoh\Downloads\logo.jpeg")

W, H = 1080, 1920
FPS = 20
DURATION = 24
TOTAL_FRAMES = FPS * DURATION

NAVY = (4, 13, 30)
NAVY_2 = (10, 30, 58)
GOLD = (212, 175, 55)
GOLD_2 = (255, 211, 92)
CREAM = (255, 250, 238)
MUTED = (177, 189, 196)
EMERALD = (9, 104, 85)
WHITE = (255, 255, 255)


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    candidates = [
        r"C:\Windows\Fonts\segoeuib.ttf" if bold else r"C:\Windows\Fonts\segoeui.ttf",
        r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\arial.ttf",
    ]
    for candidate in candidates:
        if Path(candidate).exists():
            return ImageFont.truetype(candidate, size=size)
    return ImageFont.load_default()


F_TITLE = font(86, True)
F_SUB = font(42, False)
F_SMALL = font(30, False)
F_MED = font(42, True)
F_CARD = font(36, True)
F_AR = font(58, True)


def ease(x: float) -> float:
    x = max(0.0, min(1.0, x))
    return 1 - pow(1 - x, 3)


def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


def draw_center_text(draw: ImageDraw.ImageDraw, text: str, y: int, fnt, fill, spacing: int = 8) -> int:
    lines = text.split("\n")
    cur = y
    for line in lines:
        box = draw.textbbox((0, 0), line, font=fnt)
        draw.text(((W - (box[2] - box[0])) / 2, cur), line, font=fnt, fill=fill)
        cur += (box[3] - box[1]) + spacing
    return cur


def rounded_rectangle(draw: ImageDraw.ImageDraw, xy, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)


def gradient_background(t: float) -> Image.Image:
    img = Image.new("RGB", (W, H), NAVY)
    base_draw = ImageDraw.Draw(img)
    for y in range(H):
        yy = y / H
        r = int(lerp(NAVY[0], NAVY_2[0], yy))
        g = int(lerp(NAVY[1], NAVY_2[1], yy))
        b = int(lerp(NAVY[2], NAVY_2[2], yy))
        base_draw.line((0, y, W, y), fill=(r, g, b))

    overlay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(overlay, "RGBA")
    shimmer_x = int(W * (0.15 + 0.7 * t))
    d.ellipse((shimmer_x - 520, 20, shimmer_x + 520, 980), fill=(36, 96, 155, 58))
    d.ellipse((-300, 890, 680, 2050), fill=(212, 175, 55, 24))
    d.ellipse((420, 760, 1380, 2050), fill=(9, 104, 85, 34))
    overlay = overlay.filter(ImageFilter.GaussianBlur(54))
    return Image.alpha_composite(img.convert("RGBA"), overlay).convert("RGB")


def draw_stars(draw: ImageDraw.ImageDraw, frame: int):
    for i in range(62):
        x = (i * 173 + 91) % W
        y = 80 + ((i * 241 + 37) % 820)
        a = 0.25 + 0.75 * (0.5 + 0.5 * math.sin(frame * 0.04 + i))
        c = tuple(int(v * a) for v in GOLD_2)
        r = 1 + (i % 3)
        draw.ellipse((x - r, y - r, x + r, y + r), fill=c)


def draw_mosque_silhouette(draw: ImageDraw.ImageDraw, y_base: int, alpha: float = 0.5):
    c = tuple(int(v * alpha) for v in (18, 48, 83))
    draw.rectangle((0, y_base, W, H), fill=(2, 9, 20))
    for cx, radius, height in [(270, 85, 250), (540, 140, 340), (810, 85, 250)]:
        draw.rectangle((cx - radius, y_base - height + radius, cx + radius, y_base), fill=c)
        draw.pieslice((cx - radius, y_base - height, cx + radius, y_base - height + radius * 2), 180, 360, fill=c)
    for x in [115, 965]:
        draw.rectangle((x - 28, y_base - 410, x + 28, y_base), fill=c)
        draw.pieslice((x - 38, y_base - 480, x + 38, y_base - 404), 180, 360, fill=c)
        draw.rectangle((x - 8, y_base - 535, x + 8, y_base - 468), fill=c)
        draw.ellipse((x - 16, y_base - 552, x + 16, y_base - 520), fill=c)


def logo_image(size: int) -> Image.Image:
    if LOGO_PATH.exists():
        logo = Image.open(LOGO_PATH).convert("RGB")
        logo = logo.resize((size, size), Image.Resampling.LANCZOS)
        mask = Image.new("L", (size, size), 0)
        ImageDraw.Draw(mask).rounded_rectangle((0, 0, size, size), radius=size // 5, fill=255)
        out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        out.paste(logo, (0, 0), mask)
        return out
    out = Image.new("RGBA", (size, size), (6, 18, 38, 255))
    d = ImageDraw.Draw(out)
    d.rounded_rectangle((0, 0, size - 1, size - 1), radius=size // 5, outline=GOLD, width=5)
    d.text((size * 0.25, size * 0.32), "N", font=font(size // 2, True), fill=GOLD)
    return out


def draw_phone(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, title: str, body: str, progress: float):
    rounded_rectangle(draw, (x, y, x + w, y + h), 58, (8, 20, 40), GOLD, 3)
    rounded_rectangle(draw, (x + 26, y + 34, x + w - 26, y + h - 34), 42, (4, 12, 28), (51, 63, 83), 2)
    draw.text((x + 60, y + 82), title, font=F_MED, fill=CREAM)
    draw.text((x + 60, y + 148), body, font=F_SMALL, fill=MUTED)
    rounded_rectangle(draw, (x + 60, y + 230, x + w - 60, y + 500), 42, (13, 42, 65), (82, 96, 112), 2)
    draw.text((x + 98, y + 270), "Next Prayer", font=F_SMALL, fill=GOLD_2)
    draw.text((x + 98, y + 320), "Asr", font=F_TITLE, fill=WHITE)
    draw.text((x + 98, y + 430), "15:42", font=F_MED, fill=CREAM)
    rounded_rectangle(draw, (x + 60, y + 540, x + w - 60, y + 610), 35, (255, 255, 255), None)
    rounded_rectangle(draw, (x + 60, y + 540, x + 60 + int((w - 120) * progress), y + 610), 35, GOLD, None)
    draw.text((x + 98, y + 650), "Quran • Reels • AI • Qibla", font=F_SMALL, fill=MUTED)


def draw_feature_card(draw: ImageDraw.ImageDraw, x: int, y: int, title: str, body: str, icon: str, delay_t: float):
    slide = int(lerp(70, 0, ease(delay_t)))
    y += slide
    rounded_rectangle(draw, (x, y, x + 840, y + 170), 34, (255, 255, 255, 18), (212, 175, 55), 2)
    draw.ellipse((x + 36, y + 38, x + 118, y + 120), fill=GOLD)
    draw.text((x + 60, y + 50), icon, font=font(44, True), fill=NAVY)
    draw.text((x + 150, y + 34), title, font=F_CARD, fill=CREAM)
    draw.text((x + 150, y + 92), body, font=F_SMALL, fill=MUTED)


def scene(frame: int) -> Image.Image:
    t = frame / FPS
    img = gradient_background((frame % TOTAL_FRAMES) / TOTAL_FRAMES)
    draw = ImageDraw.Draw(img, "RGBA")
    draw_stars(draw, frame)
    draw_mosque_silhouette(draw, 1700, 0.72)

    if t < 4:
        p = ease(t / 4)
        logo = logo_image(int(260 + 35 * math.sin(p * math.pi)))
        img.paste(logo, ((W - logo.width) // 2, int(240 - 30 * (1 - p))), logo)
        draw_center_text(draw, "Noor Pro", 560, F_TITLE, CREAM)
        draw_center_text(draw, "All-in-one Islamic companion", 675, F_SUB, MUTED)
        rounded_rectangle(draw, (170, 820, 910, 910), 45, EMERALD, None)
        draw_center_text(draw, "Prayer • Quran • Ummah • AI", 843, F_MED, WHITE)
        draw.text((260, 1550), "Built for daily deen, beautifully.", font=F_SMALL, fill=GOLD_2)

    elif t < 9:
        local = ease((t - 4) / 5)
        draw.text((100, 170), "Everything for your day", font=F_MED, fill=GOLD_2)
        draw.text((100, 230), "From prayer to reflection", font=F_TITLE, fill=CREAM)
        draw_phone(draw, 210, int(390 - 35 * (1 - local)), 660, 1020, "Today", "Makkah • Adhan on", local)

    elif t < 15:
        local = (t - 9) / 6
        draw.text((100, 160), "Powerful features", font=F_MED, fill=GOLD_2)
        draw.text((100, 220), "Made simple", font=F_TITLE, fill=CREAM)
        cards = [
            ("Prayer Times", "Adhan, qibla, hijri calendar", "1"),
            ("Quran & Audio", "Read, recite, save, continue", "2"),
            ("Ummah Reels", "Islamic posts and videos", "3"),
            ("Ask Imam Noor", "AI guidance with respectful care", "4"),
        ]
        for i, (title, body, icon) in enumerate(cards):
            draw_feature_card(draw, 120, 430 + i * 215, title, body, icon, local * 1.6 - i * 0.18)

    elif t < 20:
        local = ease((t - 15) / 5)
        draw.text((100, 160), "A premium experience", font=F_MED, fill=GOLD_2)
        draw.text((100, 220), "For the Ummah", font=F_TITLE, fill=CREAM)
        draw_center_text(draw, "السلام عليكم", 450, F_AR, GOLD_2)
        panels = [
            ("Nearby Masjid", "Find mosques and halal food"),
            ("Library", "Islamic books and learning"),
            ("Deen Points", "Track habits and progress"),
        ]
        for i, (title, body) in enumerate(panels):
            y = int(720 + i * 210 - 30 * (1 - local))
            rounded_rectangle(draw, (130, y, 950, y + 150), 34, (255, 255, 255, 20), (255, 255, 255, 44), 2)
            draw.text((180, y + 30), title, font=F_CARD, fill=CREAM)
            draw.text((180, y + 88), body, font=F_SMALL, fill=MUTED)

    else:
        local = ease((t - 20) / 4)
        logo = logo_image(250)
        img.paste(logo, ((W - 250) // 2, 220), logo)
        draw_center_text(draw, "Noor Pro", 520, F_TITLE, CREAM)
        draw_center_text(draw, "Your faith. Your community.\nOne beautiful app.", 660, F_SUB, MUTED)
        rounded_rectangle(draw, (150, 930, 930, 1035), 52, GOLD, None)
        draw_center_text(draw, "Download / Open Noor Pro", 960, F_MED, NAVY)
        draw.text((265, 1510), "Prayer • Quran • Reels • AI • Library", font=F_SMALL, fill=GOLD_2)
        draw.rectangle((0, 0, W, H), fill=(0, 0, 0, int(20 * (1 - local))))

    # soft vignette
    overlay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    for i in range(16):
        alpha = int(9 + i * 3)
        od.rounded_rectangle((i * 14, i * 24, W - i * 14, H - i * 24), radius=90, outline=(0, 0, 0, alpha), width=16)
    img = Image.alpha_composite(img.convert("RGBA"), overlay).convert("RGB")
    return img


def render() -> None:
    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    cmd = [
        ffmpeg,
        "-y",
        "-loglevel",
        "error",
        "-f",
        "rawvideo",
        "-vcodec",
        "rawvideo",
        "-pix_fmt",
        "rgb24",
        "-s",
        f"{W}x{H}",
        "-r",
        str(FPS),
        "-i",
        "-",
        "-an",
        "-c:v",
        "libx264",
        "-preset",
        "veryfast",
        "-crf",
        "20",
        "-pix_fmt",
        "yuv420p",
        "-movflags",
        "+faststart",
        str(VIDEO_PATH),
    ]
    proc = subprocess.Popen(cmd, stdin=subprocess.PIPE)
    try:
        for frame in range(TOTAL_FRAMES):
            img = scene(frame)
            if frame == 60:
                img.save(THUMB_PATH, quality=92)
            proc.stdin.write(img.tobytes())
    finally:
        if proc.stdin:
            proc.stdin.close()
        code = proc.wait()
        if code != 0:
            raise RuntimeError(f"ffmpeg failed with exit code {code}")


if __name__ == "__main__":
    render()
    print(VIDEO_PATH)
    print(THUMB_PATH)
