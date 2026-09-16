#!/usr/bin/env python3
"""Generate FireMind launcher icons and TV banner (original vector-style art)."""
from PIL import Image, ImageDraw, ImageFont
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")
BG = (11, 14, 20)        # #0B0E14
BRAND = (255, 107, 53)   # #FF6B35
FOCUS = (255, 209, 102)  # #FFD166
TEXT = (245, 247, 250)   # #F5F7FA


def find_font(size):
    candidates = [
        "C:/Windows/Fonts/arialbd.ttf",
        "C:/Windows/Fonts/arial.ttf",
        "C:/Windows/Fonts/segoeui.ttf",
    ]
    for path in candidates:
        if os.path.exists(path):
            try:
                return ImageFont.truetype(path, size)
            except OSError:
                continue
    return ImageFont.load_default()


def draw_brain_flame(d: ImageDraw.ImageDraw, cx: float, cy: float, r: float):
    """Simple original mark: a rounded brain outline with a flame spark."""
    d.ellipse([cx - r, cy - r * 0.85, cx + r * 0.35, cy + r * 0.85],
              outline=BRAND, width=max(2, int(r * 0.14)))
    d.ellipse([cx - r * 0.35, cy - r * 0.85, cx + r, cy + r * 0.85],
              outline=BRAND, width=max(2, int(r * 0.14)))
    d.line([cx - r * 0.05, cy - r * 0.8, cx - r * 0.05, cy + r * 0.8],
           fill=BRAND, width=max(2, int(r * 0.10)))
    # flame spark
    fx, fy, fr = cx + r * 0.95, cy - r * 0.95, r * 0.45
    d.polygon([(fx, fy - fr), (fx + fr * 0.8, fy + fr * 0.5),
               (fx - fr * 0.8, fy + fr * 0.5)], fill=FOCUS)


def make_icon(size):
    img = Image.new("RGB", (size, size), BG)
    d = ImageDraw.Draw(img)
    pad = size * 0.22
    icon_r = size * 0.18
    draw_brain_flame(d, size * 0.5 - icon_r * 0.2, size * 0.40, icon_r)
    # wordmark "FM"
    font = find_font(int(size * 0.30))
    bbox = d.textbbox((0, 0), "FM", font=font)
    w, h = bbox[2] - bbox[0], bbox[3] - bbox[1]
    d.text((size / 2 - w / 2 - bbox[0], size * 0.72 - h / 2 - bbox[1]),
           "FM", font=font, fill=TEXT)
    return img


def make_banner():
    w, h = 320, 180
    img = Image.new("RGB", (w, h), BG)
    d = ImageDraw.Draw(img)
    draw_brain_flame(d, w * 0.24, h * 0.42, w * 0.085)
    font = find_font(int(h * 0.17))
    d.text((w * 0.42, h * 0.30), "FIREMIND", font=font, fill=TEXT)
    small = find_font(int(h * 0.085))
    d.text((w * 0.42, h * 0.55), "AI viewing companion", font=small, fill=BRAND)
    return img


def main():
    densities = {
        "mipmap-mdpi": 48, "mipmap-hdpi": 72, "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144, "mipmap-xxxhdpi": 192,
    }
    for folder, size in densities.items():
        out = os.path.join(ROOT, folder)
        os.makedirs(out, exist_ok=True)
        make_icon(size).save(os.path.join(out, "ic_launcher.png"))
        print(f"wrote {folder}/ic_launcher.png ({size}x{size})")

    banner_dir = os.path.join(ROOT, "drawable")
    os.makedirs(banner_dir, exist_ok=True)
    make_banner().save(os.path.join(banner_dir, "banner.png"))
    print("wrote drawable/banner.png (320x180)")


if __name__ == "__main__":
    main()
