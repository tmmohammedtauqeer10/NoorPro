"""Render the Play Store assets to exact pixel sizes with headless Chrome.

Serve this folder first (python -m http.server 8778 --directory playstore),
then run:  python export.py [icon|feature|shots|all]
"""

import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image

CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
BASE_URL = "http://localhost:8778/export.html"
OUT_DIR = Path(__file__).parent / "out"
RAW_DIR = Path(__file__).parent / "raw"

# name -> (url query, width, height)
TARGETS = {
    "icon-512": ("target=icon", 512, 512),
    "feature-graphic-1024x500": ("target=feature", 1024, 500),
}
for i in range(1, 9):
    TARGETS[f"screenshot-{i}"] = (f"target=shot&n={i}", 1080, 1920)


def render(name: str, query: str, width: int, height: int) -> Path:
    out = OUT_DIR / f"{name}.png"
    profile = tempfile.mkdtemp(prefix="noor-chrome-")
    try:
        subprocess.run(
            [
                CHROME,
                "--headless=new",
                "--disable-gpu",
                "--hide-scrollbars",
                "--force-device-scale-factor=1",
                "--default-background-color=00000000",
                f"--user-data-dir={profile}",
                f"--window-size={width},{height}",
                # Give webfonts and the screenshot <img> time to decode.
                "--virtual-time-budget=8000",
                f"--screenshot={out}",
                f"{BASE_URL}?{query}",
            ],
            check=True,
            capture_output=True,
            timeout=120,
        )
    finally:
        shutil.rmtree(profile, ignore_errors=True)

    # Chrome occasionally rounds the viewport; force the exact store dimensions.
    with Image.open(out) as img:
        img = img.convert("RGB")
        if img.size != (width, height):
            img = img.resize((width, height), Image.LANCZOS)
        img.save(out, "PNG", optimize=True)

    return out


def main() -> None:
    which = sys.argv[1] if len(sys.argv) > 1 else "all"
    OUT_DIR.mkdir(exist_ok=True)

    if which == "icon":
        names = ["icon-512"]
    elif which == "feature":
        names = ["feature-graphic-1024x500"]
    elif which == "shots":
        names = [f"screenshot-{i}" for i in range(1, 9)]
    else:
        names = list(TARGETS)

    for name in names:
        query, w, h = TARGETS[name]
        if name.startswith("screenshot-"):
            n = name.split("-")[1]
            if not (RAW_DIR / f"shot-{n}.png").exists():
                print(f"skip {name}: raw/shot-{n}.png missing")
                continue
        path = render(name, query, w, h)
        with Image.open(path) as img:
            print(f"{name}: {img.size[0]}x{img.size[1]} -> {path}")


if __name__ == "__main__":
    main()
