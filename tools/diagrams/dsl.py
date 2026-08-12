"""Minimal vector-style diagram toolkit built on Pillow.

No Graphviz or matplotlib is available in this environment, so the report's
diagrams are drawn directly. Everything is drawn at ``SS`` times the requested
size and downsampled with LANCZOS at save time, which is what gives the strokes
and text their antialiased edges -- Pillow's own drawing primitives have none.
"""

from PIL import Image, ImageDraw, ImageFont
import math
import os

SS = 3  # supersampling factor

FONT_DIR = os.path.join(os.environ.get("WINDIR", r"C:\Windows"), "Fonts")


def _font(name, size):
    path = os.path.join(FONT_DIR, name)
    return ImageFont.truetype(path, int(size * SS))


class Palette:
    ink = (28, 32, 42)
    muted = (110, 118, 132)
    line = (72, 80, 96)
    white = (255, 255, 255)
    page = (255, 255, 255)

    # Fills chosen to stay legible when the report is printed in greyscale.
    blue = (222, 232, 248)
    blue_s = (52, 96, 176)
    green = (223, 240, 226)
    green_s = (46, 122, 64)
    amber = (252, 238, 214)
    amber_s = (168, 108, 20)
    red = (250, 226, 226)
    red_s = (168, 52, 52)
    grey = (238, 240, 244)
    grey_s = (120, 128, 142)
    violet = (233, 228, 248)
    violet_s = (98, 78, 176)
    teal = (219, 240, 240)
    teal_s = (30, 118, 122)


P = Palette


class Canvas:
    def __init__(self, w, h, bg=P.page):
        self.w, self.h = w, h
        self.img = Image.new("RGB", (int(w * SS), int(h * SS)), bg)
        self.d = ImageDraw.Draw(self.img)
        self._f = {}

    def font(self, size=11, bold=False, mono=False):
        key = (size, bold, mono)
        if key not in self._f:
            if mono:
                name = "consolab.ttf" if bold else "consola.ttf"
            else:
                name = "segoeuib.ttf" if bold else "segoeui.ttf"
            self._f[key] = _font(name, size)
        return self._f[key]

    # ---------- primitives ----------

    def _s(self, *vals):
        return [v * SS for v in vals]

    def text(self, x, y, s, size=11, bold=False, mono=False, color=P.ink,
             anchor="lt", spacing=1.35, max_w=None):
        f = self.font(size, bold, mono)
        lines = s.split("\n") if max_w is None else self.wrap(s, size, bold, mono, max_w)
        lh = size * spacing
        total = lh * len(lines)
        va = anchor[1] if len(anchor) > 1 else "t"
        if va == "m":
            y -= total / 2 - (lh - size) / 2
        elif va == "b":
            y -= total
        ha = anchor[0]
        for i, ln in enumerate(lines):
            yy = (y + i * lh) * SS
            self.d.text((x * SS, yy), ln, font=f, fill=color,
                        anchor={"l": "la", "c": "ma", "r": "ra"}[ha])
        return total

    def wrap(self, s, size, bold, mono, max_w):
        f = self.font(size, bold, mono)
        out = []
        for para in s.split("\n"):
            words, cur = para.split(" "), ""
            for wd in words:
                trial = (cur + " " + wd).strip()
                if self.d.textlength(trial, font=f) / SS <= max_w or not cur:
                    cur = trial
                else:
                    out.append(cur)
                    cur = wd
            out.append(cur)
        return out

    def measure(self, s, size=11, bold=False, mono=False):
        return self.d.textlength(s, font=self.font(size, bold, mono)) / SS

    def rect(self, x, y, w, h, fill=None, stroke=P.line, width=1.2, radius=0):
        xy = self._s(x, y, x + w, y + h)
        if radius:
            self.d.rounded_rectangle(xy, radius=radius * SS, fill=fill,
                                     outline=stroke, width=max(1, int(width * SS)))
        else:
            self.d.rectangle(xy, fill=fill, outline=stroke,
                             width=max(1, int(width * SS)))

    def ellipse(self, x, y, w, h, fill=None, stroke=P.line, width=1.2):
        self.d.ellipse(self._s(x, y, x + w, y + h), fill=fill, outline=stroke,
                       width=max(1, int(width * SS)))

    def poly(self, pts, fill=None, stroke=P.line, width=1.2):
        self.d.polygon([(px * SS, py * SS) for px, py in pts], fill=fill,
                       outline=stroke, width=max(1, int(width * SS)))

    def line(self, pts, color=P.line, width=1.2, dash=None):
        if dash is None:
            self.d.line([(px * SS, py * SS) for px, py in pts], fill=color,
                        width=max(1, int(width * SS)), joint="curve")
            return
        on, off = dash
        for i in range(len(pts) - 1):
            (x1, y1), (x2, y2) = pts[i], pts[i + 1]
            seg = math.hypot(x2 - x1, y2 - y1)
            if seg == 0:
                continue
            ux, uy = (x2 - x1) / seg, (y2 - y1) / seg
            t = 0.0
            while t < seg:
                t2 = min(t + on, seg)
                self.d.line([((x1 + ux * t) * SS, (y1 + uy * t) * SS),
                             ((x1 + ux * t2) * SS, (y1 + uy * t2) * SS)],
                            fill=color, width=max(1, int(width * SS)))
                t = t2 + off

    def head(self, x, y, ang, size=7, color=P.line, hollow=False):
        a = math.radians(ang)
        tip = (x, y)
        l = (x - size * math.cos(a - 0.42), y - size * math.sin(a - 0.42))
        r = (x - size * math.cos(a + 0.42), y - size * math.sin(a + 0.42))
        self.poly([tip, l, r], fill=(P.white if hollow else color), stroke=color, width=1.1)

    def arrow(self, pts, color=P.line, width=1.2, dash=None, head=True,
              hollow=False, label=None, label_size=9, label_side=0, label_bg=True,
              back=False):
        self.line(pts, color=color, width=width, dash=dash)
        (x1, y1), (x2, y2) = pts[-2], pts[-1]
        if head:
            self.head(x2, y2, math.degrees(math.atan2(y2 - y1, x2 - x1)),
                      color=color, hollow=hollow)
        if back:
            (bx1, by1), (bx2, by2) = pts[1], pts[0]
            self.head(bx2, by2, math.degrees(math.atan2(by2 - by1, bx2 - bx1)),
                      color=color, hollow=hollow)
        if label:
            mi = len(pts) // 2
            if len(pts) % 2 == 0:
                mx = (pts[mi - 1][0] + pts[mi][0]) / 2
                my = (pts[mi - 1][1] + pts[mi][1]) / 2
            else:
                mx, my = pts[mi]
            self.tag(mx, my + label_side, label, size=label_size, bg=label_bg)

    def tag(self, x, y, s, size=9, color=P.muted, bg=True, pad=2.5):
        lines = s.split("\n")
        w = max(self.measure(l, size) for l in lines)
        h = size * 1.3 * len(lines)
        if bg:
            self.rect(x - w / 2 - pad, y - h / 2 - pad + 1, w + 2 * pad, h + 2 * pad,
                      fill=P.white, stroke=None, width=0)
        self.text(x, y, s, size=size, color=color, anchor="cm")

    # ---------- composites ----------

    def node(self, x, y, w, h, title, lines=None, fill=P.blue, stroke=P.blue_s,
             radius=5, title_size=11.5, body_size=9.3, title_color=P.ink,
             align="c", sub=None, width=1.3):
        self.rect(x, y, w, h, fill=fill, stroke=stroke, radius=radius, width=width)
        lines = lines or []
        tw = self.wrap(title, title_size, True, False, w - 12)
        th = len(tw) * title_size * 1.28
        bh = 0
        wrapped = []
        for ln in lines:
            ww = self.wrap(ln, body_size, False, False, w - 14)
            wrapped += ww
            bh += len(ww) * body_size * 1.35
        if sub:
            bh += body_size * 1.5
        top = y + (h - th - bh) / 2
        cx = x + w / 2 if align == "c" else x + 8
        anch = "c" if align == "c" else "l"
        for i, ln in enumerate(tw):
            self.text(cx, top + i * title_size * 1.28, ln, size=title_size,
                      bold=True, color=title_color, anchor=anch + "t")
        yy = top + th + (2 if wrapped else 0)
        for ln in wrapped:
            self.text(cx, yy, ln, size=body_size, color=P.muted, anchor=anch + "t")
            yy += body_size * 1.35
        if sub:
            self.text(cx, yy + 2, sub, size=body_size, mono=True, color=stroke,
                      anchor=anch + "t")
        return (x, y, w, h)

    def band(self, x, y, w, h, label, fill=(250, 251, 253), stroke=(206, 212, 222),
             label_color=P.muted, dash=(6, 4)):
        self.rect(x, y, w, h, fill=fill, stroke=None, width=0)
        self.line([(x, y), (x + w, y), (x + w, y + h), (x, y + h), (x, y)],
                  color=stroke, width=1.1, dash=dash)
        self.text(x + 9, y + 7, label, size=9.5, bold=True, color=label_color)

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        out = self.img.resize((int(self.w), int(self.h)), Image.LANCZOS)
        out.save(path, "PNG")
        print("  " + os.path.basename(path) + "  " + str(int(self.w)) + "x" + str(int(self.h)))


def anchor_edge(box, side):
    x, y, w, h = box
    return {
        "t": (x + w / 2, y), "b": (x + w / 2, y + h),
        "l": (x, y + h / 2), "r": (x + w, y + h / 2),
        "tl": (x, y), "tr": (x + w, y), "bl": (x, y + h), "br": (x + w, y + h),
        "c": (x + w / 2, y + h / 2),
    }[side]
