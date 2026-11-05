"""Renders the store images and the Android 7 launcher icons from SVG.

Run from the repo root:  python docs/play-store/src/render.py
Uses headless Chrome (or Edge) for exact-size PNGs with transparency.
The brand mark is the prototype's <symbol id="brand">, unchanged.
"""
import os, pathlib, subprocess, sys, tempfile

ROOT = pathlib.Path(__file__).resolve().parents[3]
OUT = ROOT / "docs" / "play-store"
RES = ROOT / "app" / "src" / "main" / "res"
FONT = (ROOT / "app/src/main/res/font/inter_semibold.ttf").as_uri()
FONT_R = (ROOT / "app/src/main/res/font/inter_regular.ttf").as_uri()

BROWSERS = [r"C:\Program Files\Google\Chrome\Application\chrome.exe",
            r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"]
BROWSER = next(b for b in BROWSERS if os.path.exists(b))

MARK = """<g>
  <path d="M50 8.5a41.5 41.5 0 0 0 0 83" fill="none" stroke="#9EC6E8" stroke-width="4.4" stroke-linecap="round"/>
  <path d="M50 8.5a41.5 41.5 0 0 1 0 83" fill="none" stroke="#C9A14A" stroke-width="4.4" stroke-linecap="round"/>
  <circle cx="50" cy="50" r="35" fill="none" stroke="#16305B" stroke-width="5"/>
  <circle cx="50" cy="50" r="31" fill="#E9A84F"/>
  <path d="M28 42a24 24 0 0 1 20-17" fill="none" stroke="#F6D3A0" stroke-width="4.5" stroke-linecap="round" opacity=".85"/>
  <ellipse cx="34.5" cy="55.5" rx="5" ry="3.4" fill="#EE8F86" opacity=".85"/>
  <ellipse cx="65.5" cy="55.5" rx="5" ry="3.4" fill="#EE8F86" opacity=".85"/>
  <path d="M34 47.5q6-7.5 12 0" fill="none" stroke="#16305B" stroke-width="3.4" stroke-linecap="round"/>
  <path d="M54 47.5q6-7.5 12 0" fill="none" stroke="#16305B" stroke-width="3.4" stroke-linecap="round"/>
  <path d="M39 57q11 11 22 0" fill="none" stroke="#16305B" stroke-width="3.4" stroke-linecap="round"/>
  <path d="M55 63.5q4.5 1 5.5 5.5-4.5 1.5-6.5-2z" fill="#EE8F86" stroke="#16305B" stroke-width="1.6" stroke-linejoin="round"/>
</g>"""

def icon_svg(shape: str) -> str:
    """shape: square (Play, full bleed), rounded (legacy launcher), round (legacy round)."""
    bg = {"square": '<rect width="100" height="100" fill="#F2F5F9"/>',
          "rounded": '<rect x="4" y="4" width="92" height="92" rx="18" fill="#F2F5F9"/>',
          "round": '<circle cx="50" cy="50" r="46" fill="#F2F5F9"/>'}[shape]
    scale = 0.72 if shape == "square" else 0.70
    off = 50 - 50 * scale
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100%" height="100%">'
            f'{bg}<g transform="translate({off} {off}) scale({scale})">{MARK}</g></svg>')

FEATURE = f"""<style>
@font-face {{ font-family: Inter; font-weight: 600; src: url('{FONT}'); }}
@font-face {{ font-family: Inter; font-weight: 400; src: url('{FONT_R}'); }}
body {{ margin: 0; width: 1024px; height: 500px; background: #0D1F3D; font-family: Inter;
       display: flex; align-items: center; gap: 56px; padding: 0 88px; box-sizing: border-box; }}
.mark {{ width: 260px; height: 260px; flex: none; background: #F2F5F9; border-radius: 56px;
        display: flex; align-items: center; justify-content: center; }}
.mark svg {{ width: 210px; height: 210px; }}
h1 {{ color: #EEF2F7; font-weight: 600; font-size: 72px; letter-spacing: -0.03em; margin: 0 0 14px; }}
p {{ color: #EEF2F7; font-size: 32px; font-weight: 600; letter-spacing: -0.01em; margin: 0 0 22px; }}
small {{ color: #95A2B4; font-size: 22px; font-weight: 400; }}
</style>
<div class="mark"><svg viewBox="0 0 100 100">{MARK}</svg></div>
<div><h1>Ri-magwinya</h1><p>Order ahead. Skip the queue.</p>
<small>The Rosebank College tuckshop, Bloemfontein</small></div>"""

def shoot(html: str, w: int, h: int, out: pathlib.Path):
    with tempfile.TemporaryDirectory() as d:
        page = pathlib.Path(d) / "p.html"
        page.write_text("<!doctype html><meta charset=utf-8><style>html,body{margin:0;overflow:hidden}</style>" + html,
                        encoding="utf-8")
        out.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run([BROWSER, "--headless=new", "--disable-gpu", "--hide-scrollbars",
                        "--default-background-color=00000000", "--force-device-scale-factor=1",
                        f"--window-size={w},{h}", f"--screenshot={out}", page.as_uri()],
                       check=True, capture_output=True, timeout=60)
    print("wrote", out.relative_to(ROOT), f"{w}x{h}")

def box(svg: str, n: int) -> str:
    return f'<div style="width:{n}px;height:{n}px">{svg}</div>'

shoot(box(icon_svg("square"), 512), 512, 512, OUT / "icon-512.png")
shoot(FEATURE, 1024, 500, OUT / "feature-graphic-1024x500.png")
for folder, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
    shoot(box(icon_svg("rounded"), px), px, px, RES / f"mipmap-{folder}" / "ic_launcher.png")
    shoot(box(icon_svg("round"), px), px, px, RES / f"mipmap-{folder}" / "ic_launcher_round.png")
