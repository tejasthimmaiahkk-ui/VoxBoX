"""Project-management and quality diagrams: navigation map, timeline, testing, cost."""

import sys, os
sys.path.insert(0, os.path.dirname(__file__))
from dsl import Canvas, P

OUT = "D:/CollegeProject/docs/diagrams"


def title(c, n, t, sub):
    c.text(40, 28, t, size=15, bold=True)
    c.text(40, 51, sub, size=9.6, color=P.muted, max_w=c.w - 80)


# ------------------------------------------------------------- navigation map
def navigation():
    c = Canvas(1420, 900)
    title(c, "7.1", "Screen map and navigation graph",
          "Sixteen destinations. Solid arrows are forward navigation; the dashed arrow is the "
          "one place the back stack is cleared, so a finished session cannot be re-entered.")

    L = (110, 118, 132)

    def scr(x, y, t, s, fill=P.blue, stroke=P.blue_s, w=210, h=68):
        c.node(x, y, w, h, t, [s], fill=fill, stroke=stroke, radius=8, title_size=10.6,
               body_size=8.6)
        return (x, y, w, h)

    scr(80, 120, "Onboarding", "first launch only", P.grey, P.grey_s)
    c.arrow([(185, 188), (185, 230)], color=L, label="accepted", label_size=8.4)
    home = scr(80, 230, "Home", "resume, recent notes", P.violet, P.violet_s)

    scr(380, 120, "Capture setup", "mode, syllabus, policy", P.teal, P.teal_s)
    scr(380, 240, "Live session", "timer, preview, tuning", P.teal, P.teal_s)
    scr(380, 360, "Library", "folders, search, filing", P.blue, P.blue_s)
    scr(380, 480, "Note reader", "Markdown, flags, assets", P.blue, P.blue_s)
    scr(380, 600, "Note editor", "manual correction", P.blue, P.blue_s)
    scr(380, 720, "Settings", "six sub-pages", P.amber, P.amber_s)

    subs = ["Capture defaults", "Connection", "Storage", "Appearance",
            "Privacy and data", "Diagnostics (temporary)"]
    for i, s in enumerate(subs):
        fill, stroke = ((P.red, P.red_s) if "Diagnostics" in s else (P.amber, P.amber_s))
        scr(700 + (i % 2) * 240, 600 + (i // 2) * 90, s, "", fill, stroke, w=220, h=60)
        c.line([(590, 754), (700 + (i % 2) * 240, 630 + (i // 2) * 90)],
               color=(206, 212, 222), width=1.0)

    scr(700, 120, "Syllabus picker", "document import", P.teal, P.teal_s, w=220)
    scr(700, 240, "Session summary", "kept, skipped, cost", P.teal, P.teal_s, w=220)
    scr(700, 360, "Folder browser", "nested, breadcrumb", P.blue, P.blue_s, w=220)
    scr(700, 480, "Asset lightbox", "pinch and slider zoom", P.blue, P.blue_s, w=220)
    scr(1030, 480, "Share sheet", "system, ZIP + Markdown", P.grey, P.grey_s, w=220)

    for y in (154, 274, 394, 514, 634, 754):
        c.arrow([(290, 264), (380, y)], color=L, width=1.1)
    c.arrow([(590, 154), (700, 154)], color=L, width=1.1)
    c.arrow([(590, 274), (700, 274)], color=L, width=1.1)
    c.arrow([(590, 394), (700, 394)], color=L, width=1.1)
    c.arrow([(590, 514), (700, 514)], color=L, width=1.1)
    c.arrow([(920, 514), (1030, 514)], color=L, width=1.1)
    c.arrow([(485, 188), (485, 240)], color=L, width=1.1, label="start", label_size=8.4)
    c.arrow([(485, 428), (485, 480)], color=L, width=1.1, label="open", label_size=8.4)
    c.arrow([(485, 548), (485, 600)], color=L, width=1.1, label="edit", label_size=8.4)
    c.arrow([(590, 300), (640, 300), (640, 520), (590, 520)], color=L, width=1.3,
            dash=(6, 4), label="finish: back stack cleared", label_size=8.4)

    c.band(80, 800, 1260, 80, "Two navigation rules that came out of field use")
    c.text(98, 828,
           "1.  Pressing back during a live session does not leave the session. It collapses the "
           "preview instead, because the first field trial lost 11 minutes of a lecture to an "
           "accidental back gesture.\n"
           "2.  Finishing a session pops every capture destination and lands on the note reader. "
           "A finished session is not a place the student can return to, so it is not left on the stack.",
           size=9.2, color=(76, 84, 98))
    c.save(os.path.join(OUT, "fig-navigation.png"))


# -------------------------------------------------------------------- timeline
def timeline():
    c = Canvas(1540, 900)
    title(c, "11.1", "Project timeline",
          "Actual dates, not a plan. Field trials are marked because every one of them changed "
          "the design; the milestones between them are consequences.")

    weeks = ["Wk " + str(i + 1) for i in range(16)]
    x0, y0, cw = 300, 130, 70
    c.rect(x0, y0, cw * 16, 26, fill=(238, 241, 246), stroke=(206, 212, 222), width=1.1)
    c.rect(x0 + cw * 12, y0, cw * 4, 26, fill=(246, 247, 250), stroke=(206, 212, 222),
           width=1.1)
    for i, w in enumerate(weeks):
        c.text(x0 + i * cw + cw / 2, y0 + 7, w, size=9, bold=True, anchor="ct",
               color=(190, 196, 208) if i >= 12 else P.muted)
        if i:
            c.line([(x0 + i * cw, y0), (x0 + i * cw, 700)], color=(232, 236, 242), width=1.0)

    tasks = [
        ("Requirements and scope", 0, 1.5, P.violet, P.violet_s, "done"),
        ("Room schema and migrations", 0.5, 2, P.blue, P.blue_s, "done"),
        ("Audio capture pipeline", 1.5, 2, P.teal, P.teal_s, "done"),
        ("Camera and frame filter", 2, 2.5, P.teal, P.teal_s, "done"),
        ("Proxy: auth, limits, schema", 3, 2, P.amber, P.amber_s, "done"),
        ("Append-only delta protocol", 4, 2.5, P.green, P.green_s, "done"),
        ("Field trial 1  -  markdown, chunk loss", 5.4, 0.3, P.red, P.red_s, "trial"),
        ("Markdown renderer and export", 5.7, 1.8, P.blue, P.blue_s, "done"),
        ("Folders, rename, delete", 6.2, 1.3, P.blue, P.blue_s, "done"),
        ("UI redesign, 16 destinations", 7, 2, P.violet, P.violet_s, "done"),
        ("Field trial 2  -  nothing captured", 8.4, 0.3, P.red, P.red_s, "trial"),
        ("Temporary diagnostic log", 8.6, 0.8, P.red, P.red_s, "done"),
        ("Field trial 3  -  218-line log", 9.2, 0.3, P.red, P.red_s, "trial"),
        ("Settle gate, adaptive sampling", 9.4, 1.4, P.green, P.green_s, "done"),
        ("Grounding guard", 9.8, 1, P.amber, P.amber_s, "done"),
        ("Field trial 4  -  3 kept, 9 skipped", 10.7, 0.3, P.red, P.red_s, "trial"),
        ("Report, diagrams, deck", 10.4, 1.6, P.grey, P.grey_s, "done"),
        ("Labelled corpus and scorer", 12, 1.5, P.grey, P.grey_s, "future"),
        ("Diagram structure recognition", 12, 2, P.grey, P.grey_s, "future"),
        ("Offline queueing", 12.5, 1.5, P.grey, P.grey_s, "future"),
    ]
    y = y0 + 38
    for name, start, dur, fill, stroke, kind in tasks:
        c.text(288, y + 4, name, size=9.2, anchor="rt",
               color=P.red_s if kind == "trial" else
               (P.muted if kind == "future" else P.ink),
               bold=(kind == "trial"))
        bx = x0 + start * cw
        bw = max(dur * cw, 10)
        if kind == "future":
            c.rect(bx, y, bw, 18, fill=(248, 249, 251), stroke=stroke, radius=4, width=1.1)
            c.line([(bx, y), (bx + bw, y), (bx + bw, y + 18), (bx, y + 18), (bx, y)],
                   color=stroke, width=1.1, dash=(4, 3))
        elif kind == "trial":
            c.poly([(bx + bw / 2, y - 2), (bx + bw / 2 + 12, y + 9),
                    (bx + bw / 2, y + 20), (bx + bw / 2 - 12, y + 9)],
                   fill=P.red_s, stroke=P.red_s)
        else:
            c.rect(bx, y, bw, 18, fill=fill, stroke=stroke, radius=4, width=1.1)
        y += 28

    c.line([(x0 + 12 * cw, y0), (x0 + 12 * cw, 700)], color=P.red_s, width=1.6,
           dash=(6, 4))
    c.text(x0 + 12 * cw + 8, y0 + 34, "today\n11 Aug 2026", size=9, bold=True,
           color=P.red_s)

    c.band(80, 740, 1380, 130, "What the trials cost, and what they bought")
    rows = [
        ("Trial 1", "Notes showed raw ## and $$ markers; speech vanished at every chunk seam; "
                    "export did nothing.", "Markdown renderer, 2 s overlap, FileProvider share."),
        ("Trial 2", "A whole lecture produced zero board captures. Every unit test still passed.",
         "The temporary diagnostic log, added specifically to see inside a running session."),
        ("Trial 3", "218 log lines showed the settle gate rejecting every frame, and the pace "
                    "tracker compounding.", "Relative settle ratio; baseline-anchored suggestions."),
        ("Trial 4", "3 frames kept, 9 skipped in 92 seconds - the first accepted captures in "
                    "the project's history.", "Confirms the fix; unblocks the placement work in Chapter 12."),
    ]
    yy = 770
    for a, b, d in rows:
        c.text(98, yy, a, size=9.2, bold=True, color=P.red_s)
        c.text(160, yy, b, size=9.2, color=(76, 84, 98))
        c.text(830, yy, d, size=9.2, color=(70, 78, 94))
        yy += 24
    c.save(os.path.join(OUT, "fig-timeline.png"))


# -------------------------------------------------------------------- testing
def testing():
    c = Canvas(1420, 860)
    title(c, "8.1", "Test strategy",
          "180 automated tests. The shape is deliberate: the algorithms carry the risk, so they "
          "carry the tests, and the UI is verified on a real device rather than mocked.")

    layers = [
        (3, "Device / instrumentation", "3 tests", "navigation graph reachable, "
         "permissions honoured, release build reaches the proxy", P.red, P.red_s),
        (43, "Backend  (node:test)", "43 tests", "auth, rate limit, budget, schema "
         "rejection, grounding ratio, error mapping", P.amber, P.amber_s),
        (134, "Android unit  (JUnit4 + Compose)", "134 tests", "the six algorithms, Room "
         "migrations, Markdown parser, flag parser, repositories", P.green, P.green_s),
    ]
    cx, top = 420, 130
    widths = [260, 520, 800]
    y = top
    for (n, name, count, detail, fill, stroke), w in zip(layers, widths):
        h = 110
        c.poly([(cx - w / 2, y + h), (cx + w / 2, y + h),
                (cx + w / 2 - 70, y), (cx - w / 2 + 70, y)],
               fill=fill, stroke=stroke, width=1.4)
        c.text(cx, y + 34, name, size=11, bold=True, anchor="cm")
        c.text(cx, y + 54, count, size=10, mono=True, color=stroke, anchor="cm")
        c.text(cx + w / 2 + 30, y + 44, detail, size=9.2, color=(76, 84, 98), max_w=460)
        y += h + 12

    y = 500
    c.rect(80, y, 620, 330, fill=P.white, stroke=(206, 212, 222), radius=6)
    c.text(96, y + 12, "What the tests did not catch", size=11, bold=True, color=P.red_s)
    misses = [
        ("Settle gate never opened", "Tests fed synthetic frames with no hand tremor. "
         "The gate was two orders of magnitude tighter than reality."),
        ("Rename and delete did nothing", "The dialogs compiled and 109 tests passed; the "
         "render block sat outside the composable, so the state changed and nothing drew."),
        ("Six settings pages crashed", "A negative Modifier.padding compiles and throws at "
         "runtime. No unit test instantiates those screens."),
        ("Empty delta rejected on both sides", "The prompt asked for an empty delta when there "
         "was nothing to add; client and server both treated it as a failure."),
    ]
    yy = y + 40
    for t, d in misses:
        c.text(96, yy, "-  " + t, size=9.4, bold=True)
        c.text(108, yy + 16, d, size=9.0, color=(76, 84, 98), max_w=580)
        yy += 70

    c.rect(720, y, 620, 330, fill=P.white, stroke=(206, 212, 222), radius=6)
    c.text(736, y + 12, "What was added because of them", size=11, bold=True,
           color=P.green_s)
    adds = [
        ("Field-derived fixtures", "FrameChangeDetectorTest now replays luminance scores "
         "taken from the real 218-line log, tremor included."),
        ("A device smoke suite", "Three instrumentation tests walk the navigation graph, so a "
         "screen that cannot be reached fails the build."),
        ("Property-style seam tests", "TranscriptOverlapTest generates seams at every word "
         "boundary of a fixed paragraph rather than testing one hand-picked seam."),
        ("An honest status register", "docs/PROJECT_STATUS.md section 3 lists what compiles "
         "and is tested but has never been seen working in the field. It is nine items long."),
    ]
    yy = y + 40
    for t, d in adds:
        c.text(736, yy, "-  " + t, size=9.4, bold=True)
        c.text(748, yy + 16, d, size=9.0, color=(76, 84, 98), max_w=580)
        yy += 70
    c.save(os.path.join(OUT, "fig-testing.png"))


# ----------------------------------------------------------------------- cost
def cost():
    c = Canvas(1420, 820)
    title(c, "9.1", "Where a lecture's cost actually goes",
          "Measured on the deployed proxy across four trials. The finding that changed the "
          "design: the bill is input, not output.")

    c.rect(80, 120, 640, 340, fill=P.white, stroke=(206, 212, 222), radius=6)
    c.text(96, 132, "Per 50-minute lecture, by call type", size=11, bold=True)
    data = [("Transcription", 150, 62, P.teal_s, "150 chunks of 20 s"),
            ("Board extraction", 40, 18, P.red_s, "40 accepted frames"),
            ("Note composition", 150, 17, P.green_s, "one per chunk"),
            ("Verification", 1, 3, P.amber_s, "once, at the end")]
    bx, by, bw, bh = 250, 170, 400, 250
    total = sum(d[2] for d in data)
    yy = by
    for name, n, share, col, sub in data:
        w = share / 70 * bw
        c.rect(bx, yy, w, 34, fill=col, stroke=col, radius=3)
        c.text(bx - 12, yy + 9, name, size=9.4, bold=True, anchor="rt")
        c.text(bx - 12, yy + 23, sub, size=8.2, color=P.muted, anchor="rt")
        c.text(bx + w + 10, yy + 12, str(share) + "% of spend", size=9.2, mono=True,
               color=col)
        yy += 58
    c.text(96, 424, "Output tokens are a small minority of every one of these. The dominant "
                    "term is what we send.", size=9.2, color=(76, 84, 98))

    c.rect(760, 120, 580, 340, fill=(250, 251, 254), stroke=(206, 212, 222), radius=6)
    c.text(776, 132, "The measurement that redirected the work", size=11, bold=True)
    c.text(776, 158,
           "The first cost-reduction attempt targeted verbosity: the prompt was\n"
           "shortened, the note policy defaulted to concise, and the model was\n"
           "asked for fewer words. The bill moved by about four per cent.\n\n"
           "Logging the request sizes showed why. A composition call at minute\n"
           "forty was sending roughly 24,000 characters of context and receiving\n"
           "about 600 back. Trimming the 600 could not matter.\n\n"
           "Bounding the context - 4,000 characters of syllabus, 4,000 of note\n"
           "outline, 6,000 of recent Markdown - held the request flat at about\n"
           "14,000 characters regardless of lecture length, and cut the measured\n"
           "cost of a 50-minute lecture by roughly two thirds.\n\n"
           "The standing lesson, recorded in docs/PROJECT_STATUS.md: measure the\n"
           "request before blaming the response.",
           size=9.2, color=(76, 84, 98))

    c.rect(80, 500, 1260, 300, fill=P.white, stroke=(206, 212, 222), radius=6)
    c.text(96, 512, "Cost controls in place, and what each one bounds", size=11, bold=True)
    ctrls = [
        ("On-device frame filter", "About 450 candidate frames an hour become about 40 uploads. "
         "This is the single largest lever, and it costs nothing to run.",
         "~90% of board calls avoided"),
        ("Bounded delta context", "Request size is capped and does not grow with the note.",
         "flat cost per call"),
        ("Duplicate rejection", "A perceptual fingerprint drops a re-photographed board that "
         "has not changed.", "avoids repeat extraction"),
        ("Per-minute rate limit", "The proxy refuses bursts before any provider call is made.",
         "bounds a runaway client"),
        ("Daily budget", "A hard stop on the proxy, counted per day.",
         "bounds a leaked token"),
        ("Empty-delta acceptance", "When a chunk adds nothing, the model is allowed to say so "
         "and the call ends cheaply instead of being retried as a failure.",
         "avoids retry storms"),
    ]
    yy = 544
    for a, b, d in ctrls:
        c.text(98, yy, a, size=9.4, bold=True)
        c.text(330, yy, b, size=9.2, color=(76, 84, 98), max_w=700)
        c.text(1324, yy, d, size=9.0, mono=True, color=P.green_s, anchor="rt")
        yy += 42
    c.save(os.path.join(OUT, "fig-cost.png"))


if __name__ == "__main__":
    navigation(); timeline(); testing(); cost()
