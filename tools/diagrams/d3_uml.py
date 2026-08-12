"""UML behavioural and structural diagrams."""

import sys, os
sys.path.insert(0, os.path.dirname(__file__))
from dsl import Canvas, P

OUT = "D:/CollegeProject/docs/diagrams"


def title(c, n, t, sub):
    c.text(40, 28, t, size=15, bold=True)
    c.text(40, 51, sub, size=9.6, color=P.muted, max_w=c.w - 80)


def actor(c, x, y, name, sub=""):
    L = P.line
    c.ellipse(x - 11, y, 22, 22, fill=P.white, stroke=L, width=1.4)
    c.line([(x, y + 22), (x, y + 54)], color=L, width=1.4)
    c.line([(x - 18, y + 32), (x + 18, y + 32)], color=L, width=1.4)
    c.line([(x, y + 54), (x - 15, y + 78)], color=L, width=1.4)
    c.line([(x, y + 54), (x + 15, y + 78)], color=L, width=1.4)
    c.text(x, y + 86, name, size=10.4, bold=True, anchor="ct")
    if sub:
        c.text(x, y + 102, sub, size=8.6, color=P.muted, anchor="ct")


# ------------------------------------------------------------------ use case
def use_case():
    c = Canvas(1440, 880)
    title(c, "3.3", "UML use-case diagram",
          "Nineteen use cases in four packages. <<include>> is unconditional; <<extend>> happens "
          "only when its condition holds.")

    actor(c, 110, 300, "Student", "primary actor")
    actor(c, 110, 600, "Lecturer", "passive source")
    actor(c, 1340, 260, "AI proxy", "supporting actor")
    actor(c, 1340, 560, "Android OS", "share, permissions")

    c.rect(240, 100, 1000, 630, fill=(252, 253, 255), stroke=(200, 208, 220),
           radius=8, width=1.3)
    c.text(760, 116, "VoxBox", size=12.5, bold=True, anchor="ct")

    ucs = {}

    def uc(x, y, w, name, sub="", fill=P.blue, stroke=P.blue_s, key=None):
        h = 60
        c.ellipse(x, y, w, h, fill=fill, stroke=stroke, width=1.3)
        c.text(x + w / 2, y + (h / 2 if not sub else h / 2 - 7), name, size=9.8,
               bold=True, anchor="cm", max_w=w - 22)
        if sub:
            c.text(x + w / 2, y + h / 2 + 10, sub, size=8.2, color=P.muted, anchor="cm")
        ucs[key or name] = (x, y, w, h)
        return (x, y, w, h)

    c.text(280, 148, "Preparation", size=10, bold=True, color=P.violet_s)
    uc(280, 168, 210, "Import syllabus", "Markdown or text", P.violet, P.violet_s)
    uc(280, 240, 210, "Choose capture mode", "audio or board", P.violet, P.violet_s)
    uc(280, 312, 210, "Set note policy", "concise or detailed", P.violet, P.violet_s)

    c.text(280, 400, "Capture", size=10, bold=True, color=P.teal_s)
    uc(280, 420, 210, "Run a live session", "", P.teal, P.teal_s, key="run")
    uc(280, 492, 210, "Capture a frame now", "manual override", P.teal, P.teal_s, key="manual")
    uc(280, 564, 210, "Tune while running", "interval, sensitivity", P.teal, P.teal_s, key="tune")
    uc(280, 636, 210, "Recover retained audio", "after a failure", P.teal, P.teal_s, key="recover")

    c.text(560, 148, "Automatic behaviour", size=10, bold=True, color=P.green_s)
    uc(560, 168, 230, "Transcribe speech", "", P.green, P.green_s, key="trans")
    uc(560, 240, 230, "Detect board change", "on device", P.green, P.green_s, key="detect")
    uc(560, 312, 230, "Extract board content", "text, equations", P.green, P.green_s, key="extract")
    uc(560, 384, 230, "Append note delta", "revision guarded", P.green, P.green_s, key="append")
    uc(560, 456, 230, "Adapt sampling rate", "from speech pace", P.green, P.green_s, key="adapt")
    uc(560, 528, 230, "Verify against evidence", "on session end", P.green, P.green_s, key="verify")

    c.text(860, 148, "After the lecture", size=10, bold=True, color=P.amber_s)
    uc(860, 168, 220, "Read the note", "rendered Markdown", P.amber, P.amber_s, key="read")
    uc(860, 240, 220, "Resolve a review flag", "", P.amber, P.amber_s, key="flag")
    uc(860, 312, 220, "Organise into folders", "nested", P.amber, P.amber_s, key="folder")
    uc(860, 384, 220, "Rename or delete", "confirmed", P.amber, P.amber_s, key="rename")
    uc(860, 456, 220, "Export and share", "ZIP with assets", P.amber, P.amber_s, key="export")
    uc(860, 528, 220, "Inspect diagnostics", "temporary", P.red, P.red_s, key="diag")

    L = (120, 128, 142)
    for y in (198, 270, 342, 450, 522, 594, 666):
        c.line([(160, 340), (280, y)], color=L, width=1.0)
    c.line([(160, 640), (280, 450)], color=L, width=1.0)
    for k, y in (("read", 198), ("flag", 270), ("folder", 342), ("rename", 414),
                 ("export", 486), ("diag", 558)):
        c.line([(160, 340), (860, y)], color=(200, 206, 216), width=0.9)

    def dep(a, b, kind="include", sa="r", sb="l"):
        (ax, ay, aw, ah) = ucs[a]
        (bx, by, bw, bh) = ucs[b]
        p1 = (ax + aw, ay + ah / 2) if sa == "r" else (ax + aw / 2, ay + ah)
        p2 = (bx, by + bh / 2) if sb == "l" else (bx + bw / 2, by)
        c.arrow([p1, p2], color=L, dash=(5, 4), hollow=True, width=1.1,
                label="<<" + kind + ">>", label_size=8.0)

    dep("run", "trans")
    dep("run", "detect")
    dep("detect", "extract", "include")
    dep("extract", "append", "include")
    dep("trans", "append", "include")
    dep("run", "adapt", "include")
    dep("run", "verify", "extend")
    dep("verify", "flag", "include")

    # The three manual overrides extend "Run a live session". Routed down the left
    # margin without labels so they do not sit on top of the ellipse text; the
    # legend beneath names them.
    for k in ("manual", "tune", "recover"):
        bx, by, bw, bh = ucs[k]
        c.arrow([(bx, by + bh / 2), (258, by + bh / 2), (258, 450), (280, 450)],
                color=L, dash=(5, 4), hollow=True, width=1.1)
    # Export extends Read the note, routed down the right margin.
    ex, ey, ew, eh = ucs["export"]
    rx, ry, rw, rh = ucs["read"]
    c.arrow([(ex + ew, ey + eh / 2), (1105, ey + eh / 2), (1105, ry + rh / 2),
             (rx + rw, ry + rh / 2)], color=L, dash=(5, 4), hollow=True, width=1.1)

    c.line([(1080, 260), (1290, 300)], color=L, width=1.0)
    c.line([(1080, 486), (1290, 590)], color=L, width=1.0)
    c.line([(790, 200), (1290, 292)], color=(200, 206, 216), width=0.9)
    c.line([(790, 340), (1290, 300)], color=(200, 206, 216), width=0.9)

    c.band(240, 754, 1000, 96, "Relationships not drawn as arrows, to keep the ellipses legible")
    c.text(258, 782,
           "<<extend>>   Capture a frame now,  Tune while running  and  Recover retained audio  "
           "all extend  Run a live session  (dashed, left margin).\n"
           "<<extend>>   Export and share  extends  Read the note  (dashed, right margin).\n"
           "The Lecturer actor never operates the application: the association records that their "
           "speech and board writing are the inputs a live session consumes.",
           size=9.2, color=(76, 84, 98))
    c.save(os.path.join(OUT, "fig-usecase.png"))


# ---------------------------------------------------------------------- class
def class_diagram():
    c = Canvas(1560, 1020)
    title(c, "5.4", "UML class diagram - domain layer",
          "Only the classes that carry the project's own logic are shown; Compose screens and "
          "generated Room data-access code are omitted. Filled diamonds mark composition.")

    def cls(x, y, w, name, stereo, attrs, ops, fill=P.blue, stroke=P.blue_s):
        hh = 26 + (13 if stereo else 0)
        ah = len(attrs) * 15 + 8
        oh = len(ops) * 15 + 8
        h = hh + ah + oh
        c.rect(x, y, w, h, fill=P.white, stroke=stroke, radius=3, width=1.4)
        c.rect(x, y, w, hh, fill=fill, stroke=None, width=0)
        c.rect(x, y, w, h, fill=None, stroke=stroke, radius=3, width=1.4)
        if stereo:
            c.text(x + w / 2, y + 5, "<<" + stereo + ">>", size=7.8, color=P.muted,
                   anchor="ct")
        c.text(x + w / 2, y + (18 if stereo else 7), name, size=10.6, bold=True,
               anchor="ct")
        c.line([(x, y + hh), (x + w, y + hh)], color=stroke, width=1.1)
        yy = y + hh + 4
        for a in attrs:
            c.text(x + 8, yy, a, size=8.3, mono=True, color=(84, 92, 106))
            yy += 15
        c.line([(x, y + hh + ah), (x + w, y + hh + ah)], color=stroke, width=1.1)
        yy = y + hh + ah + 4
        for o in ops:
            c.text(x + 8, yy, o, size=8.3, mono=True, color=(46, 54, 68))
            yy += 15
        return (x, y, w, h)

    ctl = cls(560, 90, 340, "CaptureSessionController", "singleton",
              ["- session: CaptureSessionEntity", "- revision: Long",
               "- queue: ArrayDeque<Chunk>"],
              ["+ start(mode, policy): SessionId", "+ captureNow()",
               "+ retune(interval, threshold)", "+ stop(): NoteId"],
              P.green, P.green_s)

    fcd = cls(60, 330, 300, "FrameChangeDetector", "",
              ["- baseline: IntArray(1024)", "- settleRatio = 0.45",
               "- exposureComp: Boolean"],
              ["+ shouldCapture(f): Decision", "+ forceNextCapture()",
               "- compareLuma(a, b): Score"], P.red, P.red_s)

    spt = cls(60, 560, 300, "SpeechPaceTracker", "",
              ["- baseIntervalMs: Long", "- baseThreshold: Double",
               "- window: RingBuffer<Int>"],
              ["+ observe(text, ms): Int", "+ suggest(...): Suggestion?"],
              P.red, P.red_s)

    rec = cls(60, 90, 300, "PcmAudioChunkRecorder", "",
              ["- sampleRate = 16000", "- chunkMs = 20000", "- overlapMs = 2000"],
              ["+ start(): Flow<Chunk>", "+ stop()"], P.teal, P.teal_s)

    ovl = cls(60, 790, 300, "TranscriptOverlap", "object",
              ["- MIN_WORDS = 3"],
              ["+ trimRepeatedPrefix(a, b)", "+ dropOverlappingSegments(...)"],
              P.teal, P.teal_s)

    agg = cls(420, 790, 280, "SpeakerAggregator", "object",
              ["- DOMINANT = 0.58", "- MARGIN = 0.15"],
              ["+ dominant(segs): SpeakerId?"], P.teal, P.teal_s)

    asm = cls(560, 400, 340, "NoteDeltaAssembler", "",
              ["- maxContextChars: Int", "- baseHash: String"],
              ["+ buildRequest(evidence): Request", "+ apply(delta, rev): Result",
               "- contentHash(md): String"], P.green, P.green_s)

    cli = cls(980, 90, 300, "AiClient", "interface",
              ["- baseUrl: String", "- token: String"],
              ["+ transcribe(chunk): Transcript", "+ extractBoard(jpeg): Board",
               "+ compose(request): Delta", "+ verify(note): Findings"],
              P.violet, P.violet_s)

    repo = cls(980, 400, 300, "NoteRepository", "",
               ["- db: VoxBoxDatabase"],
               ["+ appendBlock(...): Long", "+ addAsset(...)", "+ rename(id, title)",
                "+ delete(id)", "+ move(id, folderId)"], P.blue, P.blue_s)

    rfp = cls(980, 700, 300, "ReviewFlagParser", "object",
              ["- FLAG = Regex(...)"],
              ["+ parse(md): List<ReviewFlag>", "+ strip(md): String"],
              P.amber, P.amber_s)

    db = cls(1330, 400, 200, "VoxBoxDatabase", "RoomDatabase",
             ["+ SCHEMA = 5"], ["+ noteDao()", "+ sessionDao()", "+ assetDao()"],
             P.blue, P.blue_s)

    L = (120, 128, 142)

    def comp(p1, p2, label, mult=""):
        c.line([p1, p2], color=L, width=1.15)
        x, y = p1
        d = 8
        if abs(p2[0] - x) > abs(p2[1] - y):
            s = 1 if p2[0] > x else -1
            c.poly([(x, y), (x + s * d, y - 5), (x + s * 2 * d, y), (x + s * d, y + 5)],
                   fill=L, stroke=L, width=1.0)
        else:
            s = 1 if p2[1] > y else -1
            c.poly([(x, y), (x - 5, y + s * d), (x, y + s * 2 * d), (x + 5, y + s * d)],
                   fill=L, stroke=L, width=1.0)
        mx, my = (p1[0] + p2[0]) / 2, (p1[1] + p2[1]) / 2
        c.tag(mx, my, label, size=8.2)
        if mult:
            c.text(p2[0] - 6, p2[1] - 14, mult, size=8, color=P.muted, anchor="rt")

    comp((560, 150), (360, 150), "records")
    comp((560, 200), (360, 400), "filters frames", "1")
    comp((560, 240), (360, 620), "adapts from", "1")
    comp((730, 214), (730, 400), "composes")
    comp((900, 130), (980, 130), "calls")
    comp((900, 470), (980, 470), "persists via")
    c.arrow([(1280, 470), (1330, 470)], color=L, dash=(5, 4), hollow=True, width=1.1)
    c.arrow([(210, 232), (210, 330)], color=L, dash=(5, 4), hollow=True, width=1.1,
            label="<<use>>", label_size=8)
    c.arrow([(210, 700), (210, 790)], color=L, dash=(5, 4), hollow=True, width=1.1)
    c.arrow([(360, 850), (420, 850)], color=L, dash=(5, 4), hollow=True, width=1.1)
    c.arrow([(1130, 214), (1130, 700)], color=L, dash=(5, 4), hollow=True, width=1.1,
            label="findings parsed by", label_size=8)

    c.save(os.path.join(OUT, "fig-class.png"))


# ------------------------------------------------------------------- sequence
def sequence():
    c = Canvas(1560, 1080)
    title(c, "5.5", "UML sequence diagram - one cycle of a live board session",
          "The cycle repeats about every eight seconds. Solid arrows are calls, dashed arrows are "
          "returns, and the shaded bars are activations.")

    lanes = [("Student", 110, P.grey, P.grey_s),
             ("LiveSession\nScreen", 300, P.violet, P.violet_s),
             ("CaptureSession\nController", 510, P.green, P.green_s),
             ("FrameChange\nDetector", 700, P.red, P.red_s),
             ("NoteDelta\nAssembler", 890, P.green, P.green_s),
             ("AiClient", 1080, P.violet, P.violet_s),
             ("Proxy +\nprovider", 1250, P.amber, P.amber_s),
             ("Note\nRepository", 1440, P.blue, P.blue_s)]
    top = 100
    bottom = 960
    for name, x, fill, stroke in lanes:
        c.rect(x - 78, top, 156, 46, fill=fill, stroke=stroke, radius=4, width=1.3)
        c.text(x, top + 23, name, size=9.8, bold=True, anchor="cm")
        c.line([(x, top + 46), (x, bottom)], color=(190, 197, 210), width=1.1,
               dash=(6, 5))

    L = (100, 108, 122)

    def act(x, y, h):
        c.rect(x - 6, y, 12, h, fill=(232, 236, 244), stroke=(150, 158, 172), width=1.0)

    def msg(x1, x2, y, label, dash=None, note=None):
        c.arrow([(x1, y), (x2, y)], color=L, width=1.15, dash=dash, hollow=bool(dash))
        mid = (x1 + x2) / 2
        c.text(mid, y - 15, label, size=8.8, anchor="cm", color=(56, 64, 78))
        if note:
            c.text(mid, y + 6, note, size=8.0, anchor="ct", color=P.muted)

    act(300, 150, 780); act(510, 190, 720); act(700, 250, 60)
    act(890, 470, 300); act(1080, 520, 230); act(1250, 560, 150); act(1440, 800, 90)

    msg(110, 300, 175, "tap Start")
    msg(300, 510, 205, "start(mode = BOARD, policy = DETAILED)")
    c.tag(510, 232, "session row created, revision = 0", size=8.2)

    msg(510, 700, 268, "shouldCapture(frame)")
    msg(700, 510, 300, "Decision(keep, score = 0.31)", dash=(5, 4))
    c.tag(605, 322, "settle test: score >= 0.45 x previous?", size=8.0)

    c.rect(430, 350, 700, 96, fill=(250, 251, 254), stroke=(196, 204, 216),
           radius=4, width=1.1)
    c.poly([(430, 350), (516, 350), (516, 366), (430, 366)], fill=(232, 236, 244),
           stroke=(196, 204, 216), width=1.1)
    c.text(438, 353, "loop", size=8.6, bold=True, color=P.muted)
    c.text(780, 380, "every 20 s of audio, with a 2 s overlap carried into the next chunk",
           size=8.8, anchor="cm")
    c.text(780, 400, "transcribe -> trim repeated prefix -> dominant speaker -> store segment",
           size=8.6, anchor="cm", color=P.muted)
    c.text(780, 422, "adapt: observe(words, ms) -> suggest(baseInterval, baseThreshold)",
           size=8.6, anchor="cm", color=P.muted)

    msg(510, 890, 490, "buildRequest(transcript, board, syllabus)")
    c.tag(700, 512, "context capped: 4k syllabus, 4k outline, 6k recent", size=8.0)
    msg(890, 1080, 540, "compose(request + baseHash + revision)")
    msg(1080, 1250, 580, "POST /v1/notes/compose")
    c.tag(1165, 602, "bearer, rate limit, budget", size=8.0)
    msg(1250, 1080, 640, "delta (json_schema validated)", dash=(5, 4))
    c.tag(1165, 662, "grounding guard: ratio >= 0.25", size=8.0)
    msg(1080, 890, 690, "Delta(markdown, assets, flags)", dash=(5, 4))

    c.rect(830, 720, 700, 66, fill=(250, 251, 254), stroke=(196, 204, 216),
           radius=4, width=1.1)
    c.poly([(830, 720), (900, 720), (900, 736), (830, 736)], fill=(232, 236, 244),
           stroke=(196, 204, 216), width=1.1)
    c.text(838, 723, "alt", size=8.6, bold=True, color=P.muted)
    c.text(1180, 742, "[revision unchanged]  apply    /    [revision moved]  discard and rebuild",
           size=8.8, anchor="cm")

    msg(890, 1440, 815, "appendBlock(delta, revision)")
    msg(1440, 890, 858, "revision + 1", dash=(5, 4))
    msg(890, 510, 890, "applied", dash=(5, 4))
    msg(510, 300, 915, "state: rev n, kept / skipped", dash=(5, 4))
    msg(300, 110, 940, "living note updates on screen", dash=(5, 4))

    c.save(os.path.join(OUT, "fig-sequence-live.png"))


# ------------------------------------------------------------------- activity
def activity():
    c = Canvas(1280, 1180)
    title(c, "6.1", "UML activity diagram - the board frame decision",
          "This is the on-device filter that decides whether a camera frame is worth an AI call. "
          "Roughly 450 frames an hour reach it and about 40 survive.")

    L = (110, 118, 132)

    def act(x, y, w, h, t, fill=P.blue, stroke=P.blue_s, sub=None):
        c.rect(x, y, w, h, fill=fill, stroke=stroke, radius=14, width=1.3)
        c.text(x + w / 2, y + h / 2 - (7 if sub else 0), t, size=10.4, bold=True,
               anchor="cm", max_w=w - 20)
        if sub:
            c.text(x + w / 2, y + h / 2 + 9, sub, size=8.4, color=P.muted, anchor="cm")
        return (x, y, w, h)

    def dec(x, y, w, h, t, sub=None):
        c.poly([(x + w / 2, y), (x + w, y + h / 2), (x + w / 2, y + h), (x, y + h / 2)],
               fill=P.amber, stroke=P.amber_s, width=1.3)
        c.text(x + w / 2, y + h / 2 - (7 if sub else 0), t, size=9.6, bold=True,
               anchor="cm", max_w=w - 40)
        if sub:
            c.text(x + w / 2, y + h / 2 + 9, sub, size=8.2, color=P.muted, anchor="cm")
        return (x, y, w, h)

    c.ellipse(455, 100, 26, 26, fill=(40, 46, 58), stroke=(40, 46, 58))
    c.arrow([(468, 126), (468, 160)], color=L)

    act(348, 160, 240, 54, "Grab preview frame", P.grey, P.grey_s)
    c.arrow([(468, 214), (468, 250)], color=L)
    act(348, 250, 240, 54, "Downsample to 32 x 32 luminance", P.blue, P.blue_s)
    c.arrow([(468, 304), (468, 340)], color=L)
    act(348, 340, 240, 54, "Compensate global exposure shift", P.blue, P.blue_s)
    c.arrow([(468, 394), (468, 430)], color=L)
    act(348, 430, 240, 54, "Score changed cells against baseline", P.blue, P.blue_s)
    c.arrow([(468, 484), (468, 520)], color=L)

    dec(338, 520, 260, 92, "score >= threshold?")
    c.arrow([(598, 566), (860, 566)], color=L, label="no", label_size=9)
    act(860, 538, 220, 56, "Discard", P.grey, P.grey_s, sub="no call is spent")
    c.arrow([(468, 612), (468, 650)], color=L, label="yes", label_size=9)

    act(338, 650, 260, 54, "Hold frame as 'awaiting settle'", P.violet, P.violet_s)
    c.arrow([(468, 704), (468, 740)], color=L)
    dec(318, 740, 300, 100, "next frame still moving?",
        "settling >= 0.45 x score")
    c.arrow([(618, 790), (860, 790)], color=L, label="yes", label_size=9)
    act(860, 762, 220, 56, "Replace held frame", P.violet, P.violet_s,
        sub="a person is crossing the board")
    c.arrow([(970, 818), (970, 900), (760, 900)], color=L)
    c.arrow([(760, 900), (468, 900), (468, 880)], color=L, head=False)
    c.arrow([(468, 840), (468, 880)], color=L, label="no", label_size=9)

    act(338, 880, 260, 54, "Compute perceptual fingerprint", P.green, P.green_s)
    c.arrow([(468, 934), (468, 970)], color=L)
    dec(338, 970, 260, 92, "seen this board before?")
    c.arrow([(598, 1016), (860, 1016)], color=L, label="yes", label_size=9)
    act(860, 988, 220, 56, "Drop as duplicate", P.grey, P.grey_s)
    c.arrow([(468, 1062), (468, 1096)], color=L, label="no", label_size=9)
    act(298, 1096, 340, 54, "Upload crop for extraction", P.red, P.red_s)

    c.ellipse(1170, 543, 26, 26, fill=P.white, stroke=(40, 46, 58), width=1.6)
    c.ellipse(1175, 548, 16, 16, fill=(40, 46, 58), stroke=(40, 46, 58))
    c.arrow([(1080, 566), (1170, 566)], color=L)
    c.arrow([(1080, 1016), (1130, 1016), (1130, 600), (1183, 600), (1183, 569)],
            color=L, head=True)

    c.band(60, 160, 250, 620, "Why the settle test is relative")
    c.text(76, 192,
           "The first implementation compared the\nsettling score against a fixed 0.035.\n\n"
           "A field log showed a static board on a\nhandheld phone scoring 0.09 to 0.17 from\n"
           "breathing alone, so the gate never opened\nand not one frame was ever captured.\n\n"
           "Comparing against a fraction of the score\nthat triggered the hold makes the test\n"
           "scale-free: it asks whether the scene has\nsettled relative to how much it moved,\n"
           "not whether it is absolutely still.\n\n"
           "Confirmed in the field on 11 August 2026:\n3 frames kept, 9 skipped in 92 seconds.",
           size=8.8, color=(76, 84, 98))
    c.save(os.path.join(OUT, "fig-activity-frame.png"))


# --------------------------------------------------------------- state charts
def statecharts():
    c = Canvas(1440, 700)
    title(c, "5.6", "UML state machine - capture session lifecycle",
          "The status column of capture_sessions takes exactly these values. Every transition is "
          "written to the database before it is shown on screen.")

    L = (110, 118, 132)

    def st(x, y, w, h, name, lines, fill=P.blue, stroke=P.blue_s):
        c.rect(x, y, w, h, fill=P.white, stroke=stroke, radius=10, width=1.5)
        c.rect(x, y, w, 28, fill=fill, stroke=None, width=0)
        c.rect(x, y, w, h, fill=None, stroke=stroke, radius=10, width=1.5)
        c.text(x + w / 2, y + 8, name, size=10.6, bold=True, anchor="ct")
        yy = y + 36
        for ln in lines:
            c.text(x + 10, yy, ln, size=8.4, color=(84, 92, 106))
            yy += 14
        return (x, y, w, h)

    c.ellipse(70, 300, 24, 24, fill=(40, 46, 58), stroke=(40, 46, 58))
    st(140, 250, 190, 124, "PREPARING",
       ["entry / create note row", "entry / create living block",
        "do / request permissions", "revision = 0"], P.violet, P.violet_s)
    st(400, 250, 190, 124, "RUNNING",
       ["do / record audio", "do / sample camera", "do / adapt sampling",
        "do / append deltas"], P.green, P.green_s)
    st(400, 460, 190, 110, "DEGRADED",
       ["entry / retain WAV", "do / warn the student", "do / keep recording"],
       P.amber, P.amber_s)
    st(660, 250, 190, 124, "FINALISING",
       ["entry / flush queue", "do / verification pass", "do / attach findings",
        "exit / stop camera"], P.blue, P.blue_s)
    st(920, 250, 190, 96, "COMPLETE",
       ["entry / stoppedAt = now", "note becomes readable", "assets retained"],
       P.teal, P.teal_s)
    st(920, 460, 190, 96, "ABANDONED",
       ["entry / retain evidence", "note kept, marked partial"], P.red, P.red_s)

    c.arrow([(94, 312), (140, 312)], color=L)
    c.arrow([(330, 312), (400, 312)], color=L, label="permissions granted", label_size=8.4)
    c.arrow([(495, 374), (495, 460)], color=L, label="network or quota failure", label_size=8.4)
    c.arrow([(530, 460), (560, 420), (560, 374)], color=L, label="recovered", label_size=8.4)
    c.arrow([(590, 312), (660, 312)], color=L, label="stop pressed", label_size=8.4)
    c.arrow([(850, 300), (920, 300)], color=L, label="all work flushed", label_size=8.4)
    c.arrow([(590, 515), (920, 515)], color=L, label="process killed / storage exhausted",
            label_size=8.4)
    c.arrow([(755, 374), (755, 420), (1015, 420), (1015, 460)], color=L,
            label="verification cannot run", label_size=8.4)
    c.ellipse(1200, 285, 26, 26, fill=P.white, stroke=(40, 46, 58), width=1.6)
    c.ellipse(1205, 290, 16, 16, fill=(40, 46, 58), stroke=(40, 46, 58))
    c.arrow([(1110, 298), (1200, 298)], color=L)
    c.arrow([(1110, 508), (1160, 508), (1160, 320), (1213, 320), (1213, 311)], color=L)

    c.text(140, 610, "Self-transition:  RUNNING -- [tuning changed] / persist frameIntervalMs, "
                     "changeThreshold --> RUNNING", size=9.4, color=(76, 84, 98))
    c.text(140, 630, "Self-transition:  RUNNING -- [manual capture] / forceNextCapture() --> RUNNING",
           size=9.4, color=(76, 84, 98))
    c.text(140, 650, "Guard on every append:  revision in the database must equal the revision the "
                     "delta was built against, otherwise the delta is discarded and rebuilt.",
           size=9.4, color=(76, 84, 98))
    c.save(os.path.join(OUT, "fig-state-session.png"))

    # ---- note lifecycle
    c = Canvas(1440, 620)
    title(c, "5.7", "Note lifecycle",
          "What happens to a note from the moment the student presses record to the moment it "
          "leaves the phone. Nothing in this path overwrites captured evidence.")

    stages = [
        ("Empty", "note row and one\nliving block exist", P.grey, P.grey_s),
        ("Living", "block is extended on\nevery accepted delta", P.green, P.green_s),
        ("Verified", "findings attached as\nreview flags", P.blue, P.blue_s),
        ("Curated", "student resolves flags,\nedits, renames, files", P.violet, P.violet_s),
        ("Exported", "Markdown + assets\nleave as a ZIP", P.teal, P.teal_s),
    ]
    x = 90
    for i, (n, s, f, st_) in enumerate(stages):
        c.node(x, 190, 210, 110, n, s.split("\n"), fill=f, stroke=st_, radius=10,
               title_size=13)
        if i < len(stages) - 1:
            c.arrow([(x + 210, 245), (x + 260, 245)], color=(110, 118, 132), width=1.4)
        x += 260

    labels = ["press record", "accepted delta\n(0 .. n times)", "press stop",
              "student action", "share sheet"]
    x = 300
    for lb in labels[:-1]:
        c.tag(x + 25, 320, lb, size=8.6)
        x += 260
    c.tag(1215, 320, labels[-1], size=8.6)

    c.band(90, 380, 1260, 180, "Invariants held at every stage")
    inv = [
        "1.  transcript_segments and visual_evidence rows are written before any AI call and are never updated afterwards.",
        "2.  The AI may only append to the single block named by capture_sessions.generatedBlockId; it cannot address any other block.",
        "3.  Every append carries the SHA-256 of the content it was built against; a mismatch discards the delta rather than merging it.",
        "4.  A review flag shows both readings - what the evidence says and what the model wrote - and neither available action rewrites the evidence.",
        "5.  Deleting a note cascades to its blocks, sessions, segments, evidence and assets in one transaction, so no orphan survives.",
    ]
    yy = 412
    for t in inv:
        c.text(108, yy, t, size=9.4, color=(70, 78, 94))
        yy += 26
    c.save(os.path.join(OUT, "fig-note-lifecycle.png"))


if __name__ == "__main__":
    use_case(); class_diagram(); sequence(); activity(); statecharts()
