#!/usr/bin/env python3
"""
Generates every PNG for the Angel of Death Wings mod with Pillow.

Usage:  python3 generate_textures.py <path/to/src/main/resources/assets/angelofdeathwings>

Outputs
  textures/entity/angel_wings.png                (128x64, 3D wing model)
  textures/item/angel_of_death_wings.png         (16x16 inventory icon)
  textures/models/armor/angel_wings_layer_1.png  (64x32, fully transparent)
  textures/models/armor/angel_wings_layer_2.png  (64x32, fully transparent)

Palette (NO red anywhere):
  white    #F5F7FA   silver   #DCE1E8   black  #0E0E12   charcoal #1C1D24

The left wing occupies u 0..63 of the entity texture, the right wing the same
layout shifted by +64 in u, painted with its own random feather sequence.
The UV table printed at the end is what AngelOfDeathWingsModel hard-codes.
"""
import math
import os
import random
import sys

from PIL import Image

PALETTE = {
    "white": (0xF5, 0xF7, 0xFA),
    "silver": (0xDC, 0xE1, 0xE8),
    "black": (0x0E, 0x0E, 0x12),
    "charcoal": (0x1C, 0x1D, 0x24),
}
LIGHT = ("white", "silver")
DARK = ("black", "charcoal")

# name, [(width, height) of each box that makes up the feather, top to bottom]
FEATHERS = [
    ("cov_a", [(7, 8)]),
    ("cov_b", [(5, 4)]),
    ("sec_a", [(3, 12)]),
    ("sec_b", [(3, 14)]),
    ("sec_c", [(3, 11)]),
    ("sec_d", [(2, 8)]),
    ("sec2_a", [(3, 9)]),
    ("sec2_b", [(3, 11)]),
    ("sec2_c", [(3, 8)]),
    ("blade_0", [(3, 11), (2, 6), (1, 4)]),
    ("blade_1", [(3, 13), (2, 7), (1, 4)]),
    ("blade_2", [(3, 12), (2, 7), (1, 4)]),
    ("blade_3", [(3, 10), (2, 6), (1, 3)]),
    ("blade_4", [(3, 8), (2, 5), (1, 3)]),
]
DEPTH = 1  # every feather box is 1 pixel thick
REGION_W = 64
TEX_W, TEX_H = 128, 64


def shade(rgb, delta):
    r, g, b = (max(0, min(255, c + delta)) for c in rgb)
    return (min(r, b), g, b)  # keep it cool-toned: red never exceeds blue


def pack_layout():
    """Deterministic shelf packer, tallest boxes first. Returns {(feather, box): (u, v)}."""
    items = []
    for fi, (_, boxes) in enumerate(FEATHERS):
        for bi, (w, h) in enumerate(boxes):
            items.append((fi, bi, w, h))
    items.sort(key=lambda t: (-t[3], t[0], t[1]))
    x = y = row_h = 0
    layout = {}
    for fi, bi, w, h in items:
        bw, bh = 2 * (w + DEPTH), h + DEPTH
        if x + bw > REGION_W:
            x, y, row_h = 0, y + row_h, 0
        layout[(fi, bi)] = (x, y)
        x += bw
        row_h = max(row_h, bh)
    assert y + row_h <= TEX_H, "layout does not fit"
    return layout


def pick_sequence(rng, n):
    """Random light/dark family order: irregular clusters, no long runs, no strict alternation."""
    while True:
        fam = [rng.random() < 0.5 for _ in range(n)]  # True = dark
        runs_ok = all(not (fam[i] == fam[i + 1] == fam[i + 2] == fam[i + 3])
                      for i in range(n - 3))
        # chained != on booleans means strict alternation (W,B,W,B)
        alt_ok = all(not (fam[i] != fam[i + 1] != fam[i + 2] != fam[i + 3])
                     for i in range(n - 3))
        dark = sum(fam)
        if runs_ok and alt_ok and 0.4 * n <= dark <= 0.6 * n:
            return [rng.choice(DARK if d else LIGHT) for d in fam]


def feather_pixels(rng, color_name, w, h, is_tip):
    base = PALETTE[color_name]
    dark = color_name in DARK
    shaft = (0x44, 0x47, 0x55) if dark else (0xA4, 0xAC, 0xBB)
    barb_a = shade(base, 9 if dark else -13)
    barb_b = shade(base, -3 if dark else 5)
    edge = shade(base, -6 if dark else -26)
    cx = (w - 1) // 2
    px = [[None] * w for _ in range(h)]
    for y in range(h):
        for x in range(w):
            c = base
            m = (y + abs(x - cx)) % 4
            if m == 0:
                c = barb_a
            elif m == 2:
                c = barb_b
            if w >= 3 and (x == 0 or x == w - 1):
                c = edge
            if x == cx:
                c = shaft
            if rng.random() < 0.04 and x != cx:
                c = barb_a if rng.random() < 0.5 else barb_b
            px[y][x] = c + (255,)
    if is_tip:
        for x in range(w):
            if w == 1:
                if rng.random() < 0.5:
                    px[h - 1][x] = (0, 0, 0, 0)
            else:
                if x in (0, w - 1) or rng.random() < 0.55:
                    px[h - 1][x] = (0, 0, 0, 0)
                if h > 2 and rng.random() < 0.25:
                    px[h - 2][x] = (0, 0, 0, 0)
        if w >= 3:
            for x in (0, w - 1):
                if h > 3 and rng.random() < 0.5:
                    px[h - 2][x] = (0, 0, 0, 0)
    return px, shade(base, -10 if dark else -30)


def paint_box(img, u, v, w, h, px, side_color):
    d = DEPTH
    put = img.putpixel
    front = (u + d, v + d)
    back = (u + 2 * d + w, v + d)
    for y in range(h):
        for x in range(w):
            put((front[0] + x, front[1] + y), px[y][x])
            put((back[0] + x, back[1] + y), px[y][w - 1 - x])  # back face is U-mirrored
    solid = side_color + (255,)
    for x in range(w):
        for k in range(d):
            put((u + d + x, v + k), solid)          # top
            put((u + d + w + x, v + k), solid)      # bottom
    for y in range(h):
        west_alpha = 255 if px[y][0][3] else 0
        east_alpha = 255 if px[y][w - 1][3] else 0
        for k in range(d):
            put((u + k, v + d + y), side_color + (west_alpha,))
            put((u + d + w + k, v + d + y), side_color + (east_alpha,))


def build_entity_texture(out_dir):
    layout = pack_layout()
    img = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
    seqs = {}
    for side, seed, shift in (("left", 1307, 0), ("right", 8821, REGION_W)):
        rng = random.Random(seed)
        seq = pick_sequence(rng, len(FEATHERS))
        seqs[side] = seq
        for fi, (name, boxes) in enumerate(FEATHERS):
            color = seq[fi]
            for bi, (w, h) in enumerate(boxes):
                is_tip = bi == len(boxes) - 1
                px, side_color = feather_pixels(rng, color, w, h, is_tip)
                u, v = layout[(fi, bi)]
                paint_box(img, u + shift, v, w, h, px, side_color)
    path = os.path.join(out_dir, "textures", "entity", "angel_wings.png")
    img.save(path)
    return img, layout, seqs


def plot(px, x, y, color):
    if 0 <= x < 16 and 0 <= y < 16:
        px[x, y] = color + (255,)


def build_item_icon(out_dir):
    rng = random.Random(4242)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    # (angle from +x in degrees, length) for one wing, lowest feather first
    feathers = [(-22, 6), (4, 8), (30, 8), (56, 7), (82, 6)]
    seq = pick_sequence(rng, 10)
    idx = 0
    for mirrored in (False, True):
        base_x, base_y = 8.4, 9.6
        for ang, length in feathers:
            color_name = seq[idx]
            idx += 1
            base = PALETTE[color_name]
            dark = color_name in DARK
            dx, dy = math.cos(math.radians(ang)), -math.sin(math.radians(ang))
            for i in range(int(length * 2) + 1):
                t = i / 2.0
                x, y = base_x + dx * t, base_y + dy * t
                if mirrored:
                    x = 15 - x
                main = (round(x), round(y))
                shaft = shade(base, 22 if dark else -34)
                plot(px, main[0], main[1], shaft if 1 <= t < length - 0.5 and t % 2 == 1 else base)
                if t < length * 0.4:
                    sx = x + (-dy) * 1.0 * (-1 if mirrored else 1)
                    sy = y + dx * 1.0
                    plot(px, round(sx), round(sy), shade(base, 6 if dark else -12))
    for x, y in ((7, 9), (8, 9), (7, 10), (8, 10), (7, 11), (8, 11)):
        px[x, y] = PALETTE["charcoal"] + (255,)
    # 1px outline for definition against any slot background
    src = img.copy().load()
    for y in range(16):
        for x in range(16):
            if src[x, y][3] == 0:
                for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                    if 0 <= nx < 16 and 0 <= ny < 16 and src[nx, ny][3]:
                        lum = sum(src[nx, ny][:3]) / 3
                        px[x, y] = ((0x5E, 0x67, 0x79, 255) if lum > 128 else (0x4A, 0x4D, 0x5E, 255))
                        break
    path = os.path.join(out_dir, "textures", "item", "angel_of_death_wings.png")
    img.save(path)
    return img


def build_armor_layers(out_dir):
    for n in (1, 2):
        Image.new("RGBA", (64, 32), (0, 0, 0, 0)).save(
            os.path.join(out_dir, "textures", "models", "armor", f"angel_wings_layer_{n}.png"))


def assert_no_red(img, label):
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a and (r > b or r > g + 8):
                raise SystemExit(f"{label}: reddish pixel {(r, g, b)} at {(x, y)}")


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    for sub in ("textures/entity", "textures/item", "textures/models/armor"):
        os.makedirs(os.path.join(out, sub), exist_ok=True)
    entity, layout, seqs = build_entity_texture(out)
    icon = build_item_icon(out)
    build_armor_layers(out)
    assert_no_red(entity, "entity")
    assert_no_red(icon, "icon")
    for side, seq in seqs.items():
        print(f"{side} wing feather colours: {', '.join(c[0].upper() for c in seq)}")
    print("UV table (left wing; right wing = u + 64):")
    for fi, (name, boxes) in enumerate(FEATHERS):
        for bi, (w, h) in enumerate(boxes):
            u, v = layout[(fi, bi)]
            print(f"  {name}[{bi}] w={w} h={h} -> texOffs({u}, {v})")


if __name__ == "__main__":
    main()
