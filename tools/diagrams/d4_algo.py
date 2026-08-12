"""Diagrams for the six algorithms that are this project's own work."""

import sys, os, math, random
sys.path.insert(0, os.path.dirname(__file__))
from dsl import Canvas, P

OUT = "D:/CollegeProject/docs/diagrams"


def title(c, n, t, sub):
    c.text(40, 28, t, size=15, bold=True)
    c.text(40, 51, sub, size=9.6, color=P.muted, max_w=c.w - 80)


# ------------------------------------------------------- frame-change detector
def frame_change():
    c = Canvas(1420, 880)
    title(c, "6.2", "Frame-change detection on the device",
          "Algorithm 1 of 6. A 32 x 32 luminance grid is compared against the last accepted "
          "board, cell by cell, after the global exposure difference has been subtracted out.")

    random.seed(7)
    cell = 9
    gx, gy = 80, 130

    def grid(ox, oy, label, mask=None, tint=None):
        c.text(ox + 16 * cell, oy - 16, label, size=10, bold=True, anchor="ct")
        for r in range(32):
            for col in range(32):
                v = 232
                if 6 <= r <= 24 and 4 <= col <= 27:
                    v = 246
                if mask and mask(r, col):
                    c.rect(ox + col * cell, oy + r * cell, cell, cell,
                           fill=tint, stroke=(250, 250, 252), width=0.4)
                else:
                    c.rect(ox + col * cell, oy + r * cell, cell, cell,
                           fill=(v, v, v), stroke=(250, 250, 252), width=0.4)
        c.rect(ox, oy, 32 * cell, 32 * cell, fill=None, stroke=(150, 158, 172), width=1.3)

    def writing(r, col):
        return (8 <= r <= 11 and 6 <= col <= 20) or (14 <= r <= 16 and 6 <= col <= 15)

    def newline(r, col):
        return 19 <= r <= 21 and 6 <= col <= 23

    grid(gx, gy, "baseline: last accepted board", writing, (176, 190, 214))
    grid(gx + 380, gy, "current frame",
         lambda r, col: writing(r, col) or newline(r, col), (176, 190, 214))
    grid(gx + 760, gy, "cells over threshold",
         newline, (232, 120, 120))

    L = (110, 118, 132)
    c.arrow([(gx + 300, gy + 145), (gx + 372, gy + 145)], color=L, width=1.4)
    c.arrow([(gx + 680, gy + 145), (gx + 752, gy + 145)], color=L, width=1.4)
    c.tag(gx + 336, gy + 128, "compare", size=8.6)
    c.tag(gx + 716, gy + 128, "score", size=8.6)

    y = 470
    c.rect(80, y, 1260, 130, fill=(250, 251, 254), stroke=(206, 212, 222), radius=6)
    c.text(96, y + 12, "The computation, in full", size=11, bold=True)
    lines = [
        "mean_delta   =  (1/1024) * sum over all cells of ( current[i] - baseline[i] )        "
        "-- the global exposure shift, subtracted before anything else",
        "changed[i]   =  1  if  | (current[i] - baseline[i]) - mean_delta |  >  pixelThreshold,   else 0",
        "score        =  (1/1024) * sum of changed[i]                                          "
        "-- the fraction of the grid that moved, in the range 0 .. 1",
        "keep frame   =  score >= changeThreshold   AND   settled(next frame)",
        "settled(f)   =  compareLuma(held, f).score  <  settleRatio * score        with settleRatio = 0.45",
    ]
    yy = y + 38
    for ln in lines:
        c.text(96, yy, ln, size=9.0, mono=True, color=(60, 68, 82))
        yy += 18

    y = 630
    c.rect(80, y, 620, 210, fill=(252, 246, 246), stroke=P.red_s, radius=6, width=1.2)
    c.text(96, y + 12, "The defect this replaced", size=11, bold=True, color=P.red_s)
    c.text(96, y + 38,
           "settled(f) originally tested against an absolute constant of 0.035.\n\n"
           "A 218-line field log showed a phone held still in front of a static\n"
           "board scoring 0.09 to 0.17 from hand tremor alone. The constant was\n"
           "an order of magnitude below the noise floor, so the gate never opened\n"
           "and not one frame was captured in three separate classroom trials.\n\n"
           "Every unit test passed throughout, because the tests fed synthetic\n"
           "frames with no tremor in them.",
           size=9.2, color=(96, 70, 70))

    c.rect(720, y, 620, 210, fill=(244, 250, 245), stroke=P.green_s, radius=6, width=1.2)
    c.text(736, y + 12, "Why a ratio is the right shape", size=11, bold=True, color=P.green_s)
    c.text(736, y + 38,
           "A person walking across the board and a lecturer writing a new line\n"
           "produce the same first signal: a large score. They differ in what\n"
           "happens next. Writing settles; a person keeps moving.\n\n"
           "Testing the settling score against a fraction of the score that\n"
           "triggered the hold makes the question scale-free -- has the scene\n"
           "settled relative to how much it moved? -- and needs no tuning per\n"
           "room, per phone or per lighting condition.\n\n"
           "Field result, 11 August 2026:  3 kept, 9 skipped in 92 seconds.",
           size=9.2, color=(64, 92, 70))
    c.save(os.path.join(OUT, "fig-algo-framechange.png"))


# ----------------------------------------------------------- adaptive sampling
def adaptive():
    c = Canvas(1420, 900)
    title(c, "6.3", "Adaptive sampling control loop",
          "Algorithm 2 of 6. Words per minute measured from the transcript drive both the camera "
          "interval and the change threshold, always relative to the session baseline.")

    L = (110, 118, 132)
    nodes = [
        (120, 150, "Transcript arrives", ["text + duration of the chunk"], P.teal, P.teal_s),
        (420, 150, "observe(text, ms)", ["words / minute for this chunk"], P.blue, P.blue_s),
        (720, 150, "Rolling window", ["last 5 chunks, median"], P.blue, P.blue_s),
        (1020, 150, "suggest(base...)", ["returns interval + threshold"], P.green, P.green_s),
    ]
    for x, y, t, s, f, st in nodes:
        c.node(x, y, 250, 78, t, s, fill=f, stroke=st, radius=8)
    for x in (370, 670, 970):
        c.arrow([(x, 189), (x + 50, 189)], color=L, width=1.4)

    c.arrow([(1145, 228), (1145, 280), (245, 280), (245, 228)], color=L, width=1.4,
            label="applied to the running session, then measured again", label_size=8.8)

    y = 330
    c.rect(80, y, 1260, 210, fill=P.white, stroke=(206, 212, 222), radius=6)
    c.text(96, y + 12, "Mapping, anchored to the session baseline", size=11, bold=True)
    cols = [("Measured pace", 110), ("Interpretation", 330), ("Camera interval", 620),
            ("Change threshold", 880), ("Effect", 1110)]
    for t, x in cols:
        c.text(x, y + 40, t, size=9.4, bold=True, color=P.muted)
    rows = [
        ("below 90 wpm", "dictation, or a pause", "base x 1.6", "base x 1.25",
         "look less often, be less twitchy"),
        ("90 - 150 wpm", "ordinary explanation", "base", "base", "leave it alone"),
        ("above 150 wpm", "dense or hurried delivery", "base x 0.6", "base x 0.8",
         "look more often, accept smaller changes"),
    ]
    yy = y + 62
    for r in rows:
        c.line([(96, yy - 6), (1324, yy - 6)], color=(226, 231, 238), width=1.0)
        for (t, x), v in zip(cols, r):
            c.text(x, yy + 4, v, size=9.2, mono=(x in (620, 880)))
        yy += 42
    c.line([(96, yy - 6), (1324, yy - 6)], color=(226, 231, 238), width=1.0)
    c.text(96, yy + 4, "Both values are clamped to 2 - 30 s and 0.02 - 0.40 respectively, and a "
                       "manual move of either slider resets the baseline to the new value.",
           size=9.0, color=P.muted)

    y = 580
    c.rect(80, y, 620, 250, fill=(252, 246, 246), stroke=P.red_s, radius=6, width=1.2)
    c.text(96, y + 12, "The bug: compounding", size=11, bold=True, color=P.red_s)
    c.text(96, y + 38,
           "The first version divided the current interval by the pace factor,\n"
           "so each suggestion compounded on the previous one.\n\n"
           "A device run produced this within four minutes:\n",
           size=9.2, color=(96, 70, 70))
    seq = [("t = 0:00", "8.0 s", "0.08"), ("t = 1:20", "4.8 s", "0.064"),
           ("t = 2:40", "2.9 s", "0.051"), ("t = 4:00", "2.0 s  (clamped)", "0.041")]
    yy = y + 110
    c.text(112, yy, "time", size=8.6, bold=True, mono=True, color=P.muted)
    c.text(260, yy, "interval", size=8.6, bold=True, mono=True, color=P.muted)
    c.text(430, yy, "threshold", size=8.6, bold=True, mono=True, color=P.muted)
    yy += 18
    for t, i, th in seq:
        c.text(112, yy, t, size=8.8, mono=True)
        c.text(260, yy, i, size=8.8, mono=True, color=P.red_s)
        c.text(430, yy, th, size=8.8, mono=True, color=P.red_s)
        yy += 17
    c.text(96, yy + 8, "The interval said 'the lecturer is racing' while the threshold\n"
                       "said the opposite. Neither reflected the measurement.",
           size=9.2, color=(96, 70, 70))

    c.rect(720, y, 620, 250, fill=(244, 250, 245), stroke=P.green_s, radius=6, width=1.2)
    c.text(736, y + 12, "The fix: anchor, do not accumulate", size=11, bold=True,
           color=P.green_s)
    c.text(736, y + 38,
           "suggest() takes baseIntervalMs and baseThreshold as arguments -- the\n"
           "values the session started with, or the values the student last set\n"
           "by hand -- and multiplies those, never the current values.\n\n"
           "The control loop therefore has no memory of its own output. Two\n"
           "identical measurements always produce identical settings, and the\n"
           "system returns to baseline the moment the pace does.\n\n"
           "This is the difference between a proportional controller and an\n"
           "integrating one; nothing here needs an integral term, and having one\n"
           "by accident was the whole of the defect.\n\n"
           "Covered by SpeechPaceTrackerTest.repeatedSuggestionsDoNotCompound.",
           size=9.2, color=(64, 92, 70))
    c.save(os.path.join(OUT, "fig-algo-adaptive.png"))


# --------------------------------------------------------------- delta protocol
def delta():
    c = Canvas(1420, 950)
    title(c, "6.4", "Append-only delta protocol with optimistic revision control",
          "Algorithm 4 of 6. The model never sees the whole note and never rewrites it. Cost per "
          "call stays flat as the note grows, and a concurrent append cannot be silently lost.")

    L = (110, 118, 132)

    c.rect(80, 110, 1260, 210, fill=(250, 251, 254), stroke=(206, 212, 222), radius=6)
    c.text(96, 122, "What is sent, and what is deliberately not sent", size=11, bold=True)
    sent = [("Syllabus extract", "capped at 4,000 characters", P.green_s),
            ("Note outline", "headings only, capped at 4,000", P.green_s),
            ("Recent Markdown", "tail of the note, capped at 6,000", P.green_s),
            ("New evidence", "this chunk's transcript and board text", P.green_s),
            ("baseHash", "SHA-256 of the note as it stands", P.green_s),
            ("revision", "integer, incremented on every append", P.green_s)]
    notsent = [("The whole note", "would grow every call, forever", P.red_s),
               ("Previous evidence", "already summarised in the outline", P.red_s),
               ("Any student identifier", "the proxy has none to send", P.red_s)]
    x = 100
    for t, s, col in sent:
        c.node(x, 152, 196, 66, t, [s], fill=(240, 248, 242), stroke=col, radius=5,
               title_size=9.6, body_size=8.2)
        x += 206
    x = 100
    for t, s, col in notsent:
        c.node(x, 234, 300, 66, t, [s], fill=(252, 244, 244), stroke=col, radius=5,
               title_size=9.6, body_size=8.2)
        x += 310
    c.text(1050, 258, "Measured effect: request size is bounded at about\n"
                      "14,000 characters no matter how long the lecture runs.",
           size=9.2, color=(76, 84, 98))

    y = 350
    c.text(80, y, "Sequence of one append", size=12, bold=True)
    steps = [
        ("1", "Read state", "rev = 7\nhash = 4f2a...", P.blue, P.blue_s),
        ("2", "Build request", "bounded context\n+ hash + rev", P.blue, P.blue_s),
        ("3", "Call the proxy", "schema-validated\nresponse only", P.violet, P.violet_s),
        ("4", "Ground the delta", "overlap ratio\n>= 0.25", P.amber, P.amber_s),
        ("5", "Re-read state", "still rev = 7?", P.amber, P.amber_s),
        ("6", "Append + bump", "rev becomes 8\nin one transaction", P.green, P.green_s),
    ]
    x = 90
    for n, t, s, f, st in steps:
        c.node(x, y + 30, 190, 96, t, s.split("\n"), fill=f, stroke=st, radius=8,
               title_size=10.6)
        c.ellipse(x - 10, y + 20, 26, 26, fill=st, stroke=st)
        c.text(x + 3, y + 33, n, size=10, bold=True, color=P.white, anchor="cm")
        if n != "6":
            c.arrow([(x + 190, y + 78), (x + 208, y + 78)], color=L, width=1.4)
        x += 208

    c.arrow([(500, y + 126), (500, y + 170), (185, y + 170), (185, y + 126)],
            color=P.red_s, width=1.4,
            label="ratio below the floor: discard the delta, keep the evidence, try the next chunk",
            label_size=8.6)
    c.arrow([(1090, y + 126), (1090, y + 200), (185, y + 200), (185, y + 126)],
            color=P.red_s, width=1.4,
            label="revision moved: another append landed first, so rebuild against the new state",
            label_size=8.6)

    y = 620
    c.rect(80, y, 620, 300, fill=P.white, stroke=(206, 212, 222), radius=6)
    c.text(96, y + 12, "Cost, measured", size=11, bold=True)
    c.text(96, y + 34, "Input tokens per composition call, 50-minute lecture", size=9,
           color=P.muted)
    bars = [(0, 1200, 1200), (10, 1300, 4800), (20, 1350, 8900),
            (30, 1400, 13500), (40, 1420, 18800), (50, 1450, 24600)]
    bx, by, bw, bh = 140, y + 70, 500, 170
    mx = 26000
    c.line([(bx, by), (bx, by + bh)], color=(180, 188, 200), width=1.1)
    c.line([(bx, by + bh), (bx + bw, by + bh)], color=(180, 188, 200), width=1.1)
    for i, (m, bounded, naive) in enumerate(bars):
        cx = bx + 30 + i * 78
        h1 = bounded / mx * bh
        h2 = naive / mx * bh
        c.rect(cx - 26, by + bh - h2, 24, h2, fill=(246, 206, 206), stroke=P.red_s, width=1.0)
        c.rect(cx + 2, by + bh - h1, 24, h1, fill=(206, 236, 212), stroke=P.green_s, width=1.0)
        c.text(cx, by + bh + 6, str(m) + " min", size=8.2, color=P.muted, anchor="ct")
    c.text(96, y + 258, "red: resend the whole note      green: bounded context (implemented)",
           size=8.8, color=P.muted)
    c.text(96, y + 276, "At 50 minutes the bounded form is 17 times cheaper per call, and the "
                        "gap widens without limit.", size=8.8, color=(76, 84, 98))

    c.rect(720, y, 620, 300, fill=P.white, stroke=(206, 212, 222), radius=6)
    c.text(736, y + 12, "Why optimistic and not locking", size=11, bold=True)
    c.text(736, y + 38,
           "Two things can append to the same note at once: the audio pipeline\n"
           "finishing a 20-second chunk, and the board pipeline finishing an\n"
           "extraction. They are independent and neither can be made to wait\n"
           "without dropping evidence.\n\n"
           "A lock would serialise them and stall the camera behind a slow\n"
           "transcription call. An optimistic check costs one integer read and\n"
           "loses nothing: the loser rebuilds its request against the new state,\n"
           "which is cheap because the context is bounded anyway.\n\n"
           "The SHA-256 is what makes the check honest. A revision number alone\n"
           "would be satisfied by an edit that changed the text without changing\n"
           "the count, which the student's own manual edits can do.\n\n"
           "Covered by NoteDeltaAssemblerTest -- 11 cases, including the\n"
           "interleaved-append race and the student-edited-mid-session case.",
           size=9.2, color=(76, 84, 98))
    c.save(os.path.join(OUT, "fig-algo-delta.png"))


# ------------------------------------------------------------ overlap + speaker
def overlap_speaker():
    c = Canvas(1420, 820)
    title(c, "6.5", "Transcript overlap repair and per-chunk dominant speaker",
          "Algorithms 3 and 5 of 6. Chunks overlap by two seconds so no word is only ever seen "
          "as a fragment; the repeat is then removed on a word basis, not a character one.")

    L = (110, 118, 132)
    x0, y0, w = 100, 130, 1220

    def band(x, w_, y, label, fill, stroke, sub=""):
        c.rect(x, y, w_, 54, fill=fill, stroke=stroke, radius=5, width=1.3)
        c.text(x + w_ / 2, y + (18 if sub else 27), label, size=9.6, bold=True,
               anchor="cm", max_w=w_ - 14)
        if sub:
            c.text(x + w_ / 2, y + 36, sub, size=8.4, color=P.muted, anchor="cm")

    c.text(100, y0 - 22, "Without overlap - the seam eats a word", size=11, bold=True,
           color=P.red_s)
    band(100, 380, y0, "\"...the derivative of a prod\"", P.red, P.red_s, "chunk 1, 0 - 20 s")
    band(490, 380, y0, "\"uct is given by...\"", P.red, P.red_s, "chunk 2, 20 - 40 s")
    c.line([(870, y0 - 8), (870, y0 + 62)], color=P.red_s, width=1.6, dash=(4, 4))
    c.text(886, y0 + 18, "the model receives half a word at each end and\n"
                         "guesses; 'product' became 'produced' in a field trial",
           size=9, color=(120, 70, 70))

    y1 = y0 + 110
    c.text(100, y1 - 22, "With a two-second overlap and word-level trimming", size=11,
           bold=True, color=P.green_s)
    band(100, 430, y1, "\"...the derivative of a product is\"", P.green, P.green_s,
         "chunk 1, 0 - 20 s")
    band(440, 430, y1 + 70, "\"of a product is given by...\"", P.green, P.green_s,
         "chunk 2, 18 - 40 s  (2 s earlier)")
    c.rect(440, y1, 90, 54, fill=(255, 246, 214), stroke=P.amber_s, radius=5, width=1.2)
    c.text(485, y1 + 27, "overlap", size=8.6, bold=True, anchor="cm", color=P.amber_s)
    c.arrow([(590, y1 + 124), (590, y1 + 78)], color=L, width=1.3,
            label="trimRepeatedPrefix", label_size=8.6)
    band(920, 400, y1 + 35, "\"...the derivative of a product is given by...\"",
         P.blue, P.blue_s, "joined transcript, no word lost, no word doubled")

    y2 = y1 + 190
    c.rect(80, y2, 620, 240, fill=P.white, stroke=(206, 212, 222), radius=6)
    c.text(96, y2 + 12, "Why word-level and not character-level", size=11, bold=True)
    c.text(96, y2 + 38,
           "The two transcriptions of the overlapping audio are rarely identical.\n"
           "Punctuation, casing and filler words differ between calls even when\n"
           "the words agree, so a character-level diff finds no common prefix and\n"
           "the repeat survives.\n\n"
           "trimRepeatedPrefix compares normalised words -- lower-cased, stripped\n"
           "of punctuation -- and requires a run of at least three to declare an\n"
           "overlap. Three is the smallest run that does not fire on a genuine\n"
           "repetition such as 'so, so, so' or a lecturer restating a phrase.\n\n"
           "Covered by TranscriptOverlapTest: 9 cases including no-overlap,\n"
           "total-overlap, punctuation-only difference and a legitimate repeat.",
           size=9.2, color=(76, 84, 98))

    c.rect(720, y2, 620, 240, fill=P.white, stroke=(206, 212, 222), radius=6)
    c.text(736, y2 + 12, "Per-chunk dominant speaker", size=11, bold=True)
    c.text(736, y2 + 38,
           "The provider returns diarisation labels within a chunk. Rather than\n"
           "trust them individually, the chunk is assigned one label only when\n"
           "one voice holds at least 58 per cent of the voiced time and leads the\n"
           "next by at least 15 percentage points. Otherwise the chunk is marked\n"
           "AMBIGUOUS and no speaker is claimed.",
           size=9.2, color=(76, 84, 98))
    ex = [("A 71%  B 22%  C 7%", "A", P.green_s),
          ("A 60%  B 40%", "AMBIGUOUS  (margin 20 but A below 58 after silence removal)",
           P.amber_s),
          ("A 49%  B 45%", "AMBIGUOUS", P.amber_s)]
    yy = y2 + 130
    for a, b, col in ex:
        c.text(752, yy, a, size=8.8, mono=True)
        c.text(920, yy, "->", size=8.8, mono=True, color=P.muted)
        c.text(950, yy, b, size=8.8, mono=True, color=col)
        yy += 20
    c.text(736, y2 + 196,
           "This is deliberately not speaker identification. No voiceprint is\n"
           "stored and no name is ever attached; the label answers only 'did the\n"
           "voice change?', which is what a student needs to read a note.",
           size=9.2, color=(76, 84, 98))
    c.save(os.path.join(OUT, "fig-algo-overlap.png"))


# ------------------------------------------------------------- grounding guard
def grounding():
    c = Canvas(1420, 860)
    title(c, "6.6", "Grounding guard",
          "Algorithm 6 of 6, and the only one that runs on the server. A prompt instruction is a "
          "request; a validator is a guarantee.")

    L = (110, 118, 132)
    steps = [
        ("Delta returned", "the Markdown the model wants to append", P.violet, P.violet_s),
        ("Extract content words", "drop stop words, lower-case, stem lightly", P.blue, P.blue_s),
        ("Extract evidence words", "same treatment, from transcript + board text",
         P.blue, P.blue_s),
        ("Overlap ratio", "|delta n evidence| / |delta|", P.amber, P.amber_s),
    ]
    x = 90
    for t, s, f, st in steps:
        c.node(x, 120, 290, 84, t, [s], fill=f, stroke=st, radius=8, title_size=10.6)
        if x < 800:
            c.arrow([(x + 290, 162), (x + 314, 162)], color=L, width=1.4)
        x += 314

    c.poly([(600, 250), (860, 250), (860, 340), (600, 340)], fill=P.amber,
           stroke=P.amber_s, width=1.3)
    c.poly([(730, 240), (880, 295), (730, 350), (580, 295)], fill=P.amber,
           stroke=P.amber_s, width=1.4)
    c.text(730, 286, "ratio >= 0.25 ?", size=10.6, bold=True, anchor="cm")
    c.text(730, 304, "and delta >= 120 characters", size=8.4, color=P.muted, anchor="cm")
    c.arrow([(1090, 204), (1090, 295), (880, 295)], color=L, width=1.4)

    c.arrow([(730, 350), (730, 400)], color=P.green_s, width=1.5, label="pass", label_size=9)
    c.node(560, 400, 340, 70, "Accept and append", ["the delta becomes part of the note"],
           fill=P.green, stroke=P.green_s, radius=8)
    c.arrow([(580, 295), (400, 295)], color=P.red_s, width=1.5, label="fail", label_size=9)
    c.node(100, 258, 290, 74, "Discard the delta", ["evidence is kept; the next chunk retries"],
           fill=P.red, stroke=P.red_s, radius=8)

    c.rect(950, 250, 390, 220, fill=(250, 251, 254), stroke=(206, 212, 222), radius=6)
    c.text(966, 262, "Why 0.25 and not higher", size=10.6, bold=True)
    c.text(966, 286,
           "A faithful summary legitimately introduces\n"
           "words the lecturer never said: connectives,\n"
           "headings, the word 'therefore'.\n\n"
           "Measured across the four field trials, honest\n"
           "deltas scored 0.34 to 0.71 and the two\n"
           "fabricated sections scored 0.08 and 0.11.\n"
           "A floor of 0.25 separates them with room on\n"
           "both sides. The 120-character minimum stops\n"
           "a one-line delta passing on coincidence.",
           size=9.0, color=(76, 84, 98))

    y = 520
    c.rect(80, y, 1260, 310, fill=P.white, stroke=(206, 212, 222), radius=6)
    c.text(96, y + 12, "The incident that produced this guard", size=11, bold=True,
           color=P.red_s)
    c.text(96, y + 40,
           "During the third classroom trial the note contained a section headed \"Applications in "
           "Industry\" with four confident paragraphs. The lecturer had\n"
           "never said the phrase. The transcript for that window contained a two-minute silence "
           "while a diagram was drawn.\n\n"
           "The prompt already said, in plain terms, not to add material that is not in the "
           "evidence. It was followed on every other call and ignored on that\n"
           "one. That is the whole argument for the guard: an instruction changes the "
           "probability of an output, and nothing more. A check on the output is\n"
           "the only thing that changes what can reach the student.",
           size=9.4, color=(76, 84, 98))
    c.text(96, y + 160, "The same reasoning appears three more times in this system:",
           size=9.6, bold=True)
    for i, t in enumerate([
            "strict json_schema on every response, so prose cannot arrive where structure is required;",
            "a rejection of any timestamp the client did not itself emit, after 00:80 appeared in a note;",
            "a path-traversal check on every asset reference before an image is resolved for display;",
            "a revision and hash check before any append, so a stale delta cannot overwrite newer text."]):
        c.text(112, y + 184 + i * 22, str(i + 1) + ".  " + t, size=9.4, color=(70, 78, 94))
    c.text(96, y + 280, "None of these is clever. Each exists because the alternative was tried "
                        "first and failed in front of a class.", size=9.4, bold=True,
           color=(70, 78, 94))
    c.save(os.path.join(OUT, "fig-algo-grounding.png"))


if __name__ == "__main__":
    frame_change(); adaptive(); delta(); overlap_speaker(); grounding()
