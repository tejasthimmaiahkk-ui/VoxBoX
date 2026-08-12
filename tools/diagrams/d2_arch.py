"""Architecture, data-flow, component, deployment and trust-boundary diagrams."""

import sys, os
sys.path.insert(0, os.path.dirname(__file__))
from dsl import Canvas, P

OUT = "D:/CollegeProject/docs/diagrams"


def title(c, n, t, sub):
    c.text(40, 28, t, size=15, bold=True)
    c.text(40, 51, sub, size=9.6, color=P.muted, max_w=c.w - 80)


# --------------------------------------------------------------- context DFD
def context_dfd():
    c = Canvas(1240, 720)
    title(c, "3.1", "Context diagram (Data-Flow Diagram, Level 0)",
          "The whole system as one process, showing only what crosses its boundary. "
          "Two human actors and two external services touch VoxBox; nothing else does.")

    c.node(500, 300, 240, 140, "VoxBox", ["0"], fill=P.blue, stroke=P.blue_s,
           radius=70, title_size=17)

    stu = c.node(90, 130, 190, 76, "Student", ["external entity"], fill=P.grey,
                 stroke=P.grey_s)
    lec = c.node(90, 480, 190, 76, "Lecturer / board",
                 ["external entity (passive)"], fill=P.grey, stroke=P.grey_s)
    prov = c.node(960, 130, 200, 76, "AI provider", ["OpenRouter"],
                  fill=P.amber, stroke=P.amber_s)
    shr = c.node(960, 480, 200, 76, "Sharing target",
                 ["mail, chat, storage"], fill=P.grey, stroke=P.grey_s)

    L = (110, 118, 132)
    c.arrow([(280, 152), (400, 152), (400, 330), (500, 330)], color=L,
            label="syllabus, settings,\nstart / stop, edits", label_size=8.6)
    c.arrow([(500, 380), (400, 380), (400, 186), (280, 186)], color=L,
            label="live note, flags,\nstorage state", label_size=8.6)
    c.arrow([(280, 505), (400, 505), (400, 410), (500, 410)], color=L,
            label="speech, board writing", label_size=8.6)

    c.arrow([(740, 330), (860, 330), (860, 152), (960, 152)], color=L,
            label="audio chunk, board crop,\nbounded note context", label_size=8.6)
    c.arrow([(960, 186), (860, 186), (860, 380), (740, 380)], color=L,
            label="transcript, extraction,\nMarkdown delta", label_size=8.6)
    c.arrow([(740, 410), (860, 410), (860, 505), (960, 505)], color=L,
            label="exported note + assets", label_size=8.6)

    c.band(60, 600, 1120, 84, "Boundary note")
    c.text(76, 626, "The lecturer is drawn as an external entity but never interacts with the "
                    "application: their speech and writing enter through the microphone and camera of the\n"
                    "student's own phone. No datum leaves the phone except to the project's own proxy, "
                    "and the credential for the AI provider is held only by that proxy.",
           size=9.4, color=(76, 84, 98))
    c.save(os.path.join(OUT, "fig-context-dfd.png"))


# --------------------------------------------------------------- level 1 DFD
def dfd_l1():
    c = Canvas(1560, 940)
    title(c, "3.2", "Data-Flow Diagram, Level 1",
          "The seven processes inside VoxBox and the four data stores they share. "
          "Numbering follows the context diagram; open-ended rectangles are data stores.")

    L = (110, 118, 132)

    def proc(x, y, n, name, lines, fill=P.blue, stroke=P.blue_s, w=200, h=92):
        c.rect(x, y, w, h, fill=fill, stroke=stroke, radius=12, width=1.3)
        c.line([(x, y + 22), (x + w, y + 22)], color=stroke, width=1.1)
        c.text(x + 10, y + 6, n, size=9.4, bold=True, mono=True, color=stroke)
        c.text(x + w / 2, y + 26 + (h - 26) / 2, name + "\n" + "\n".join(lines),
               size=10.2, bold=False, anchor="cm")
        return (x, y, w, h)

    def store(x, y, n, name, w=250):
        h = 46
        c.rect(x, y, w, h, fill=P.grey, stroke=P.grey_s, width=1.2)
        c.line([(x + 34, y), (x + 34, y + h)], color=P.grey_s, width=1.2)
        c.text(x + 17, y + h / 2, n, size=9.4, bold=True, mono=True,
               color=P.grey_s, anchor="cm")
        c.text(x + 34 + (w - 34) / 2, y + h / 2, name, size=10, anchor="cm")
        return (x, y, w, h)

    c.node(40, 130, 150, 66, "Student", [], fill=P.white, stroke=P.grey_s)
    c.node(40, 470, 150, 66, "Microphone", [], fill=P.white, stroke=P.grey_s)
    c.node(40, 600, 150, 66, "Camera", [], fill=P.white, stroke=P.grey_s)
    c.node(1370, 130, 150, 66, "AI proxy", [], fill=P.amber, stroke=P.amber_s)
    c.node(1370, 790, 150, 66, "Share sheet", [], fill=P.white, stroke=P.grey_s)

    p1 = proc(250, 120, "1.0", "Configure", ["session"], P.violet, P.violet_s)
    p2 = proc(250, 450, "2.0", "Capture", ["audio, 20 s"], P.teal, P.teal_s)
    p3 = proc(250, 590, "3.0", "Filter", ["board frames"], P.red, P.red_s)
    p4 = proc(580, 300, "4.0", "Understand", ["speech + board"], P.blue, P.blue_s, w=220)
    p5 = proc(900, 300, "5.0", "Compose", ["note delta"], P.green, P.green_s, w=220)
    p6 = proc(900, 560, "6.0", "Verify", ["and flag"], P.amber, P.amber_s, w=220)
    p7 = proc(900, 780, "7.0", "Read and", ["export"], P.violet, P.violet_s, w=220)

    d1 = store(560, 130, "D1", "syllabi", 260)
    d2 = store(560, 700, "D2", "transcript_segments", 260)
    d3 = store(560, 790, "D3", "visual_evidence", 260)
    d4 = store(1230, 430, "D4", "notes / note_blocks", 280)

    c.arrow([(190, 150), (250, 150)], color=L, label="settings", label_size=8.4)
    c.arrow([(450, 153), (560, 153)], color=L, label="parsed", label_size=8.4)
    c.arrow([(690, 176), (690, 300)], color=L, label="topics", label_size=8.4)
    c.arrow([(190, 490), (250, 490)], color=L)
    c.arrow([(190, 625), (250, 625)], color=L)
    c.arrow([(450, 490), (515, 490), (515, 380), (580, 380)], color=L,
            label="PCM chunk\n+ 2 s overlap", label_size=8.4)
    c.arrow([(450, 630), (515, 630), (515, 410), (580, 410)], color=L,
            label="accepted\nframe", label_size=8.4)
    c.arrow([(360, 542), (360, 700), (560, 700)], color=L, label="raw text", label_size=8.4)
    c.arrow([(360, 682), (360, 812), (560, 812)], color=L, label="crop", label_size=8.4)
    c.arrow([(800, 346), (900, 346)], color=L, label="evidence", label_size=8.4)
    c.arrow([(1120, 330), (1370, 330), (1370, 196)], color=L,
            label="bounded context + hash", label_size=8.4)
    c.arrow([(1370, 130), (1230, 130), (1230, 300), (1120, 300)], color=L,
            label="delta", label_size=8.4, label_side=-14)
    c.arrow([(1120, 380), (1300, 380), (1300, 430)], color=L,
            label="append if revision matches", label_size=8.4)
    c.arrow([(1230, 470), (1010, 470), (1010, 560)], color=L, label="on stop", label_size=8.4)
    c.arrow([(1010, 652), (1010, 780)], color=L, label="review flags", label_size=8.4)
    c.arrow([(1230, 476), (1160, 476), (1160, 840), (1120, 840)], color=L,
            label="blocks + assets", label_size=8.4)
    c.arrow([(1120, 812), (1370, 812)], color=L, label="ZIP", label_size=8.4)
    c.arrow([(820, 723), (900, 723), (900, 640)], color=L, label="compare", label_size=8.4)
    c.save(os.path.join(OUT, "fig-dfd-l1.png"))


# ------------------------------------------------------------- architecture
def architecture():
    c = Canvas(1420, 1330)
    title(c, "5.1", "Layered architecture",
          "Four layers on the phone, one hosted process, one third party. An arrow crosses a layer "
          "boundary in one direction only; no layer reaches past the one beneath it.")

    layers = [
        ("Presentation  -  Jetpack Compose, Material 3", 100, 200, P.violet, P.violet_s,
         [("HomeScreen", "note list, resume"), ("CaptureSetupScreen", "mode, syllabus"),
          ("LiveSessionScreen", "timer, tuning, preview"), ("NoteReaderScreen", "Markdown, flags"),
          ("LibraryScreen", "folders, rename"), ("SettingsScreen", "six sub-pages")]),
        ("Coordination  -  ViewModels and session controller", 330, 200, P.blue, P.blue_s,
         [("CaptureSessionController", "owns the running session"),
          ("NoteViewModel", "reader and editor state"),
          ("LibraryViewModel", "folders and filing"),
          ("SettingsViewModel", "DataStore projection")]),
        ("Domain  -  the project's own algorithms", 560, 200, P.green, P.green_s,
         [("FrameChangeDetector", "32x32 luminance, settle test"),
          ("SpeechPaceTracker", "words/min to interval + threshold"),
          ("TranscriptOverlap", "word-level seam repair"),
          ("SpeakerAggregator", "per-chunk dominant voice"),
          ("NoteDeltaAssembler", "append-only, hash bound"),
          ("ReviewFlagParser", "evidence vs claim")]),
        ("Data  -  Room, DataStore, files", 790, 200, P.teal, P.teal_s,
         [("VoxBoxDatabase", "10 tables, 5 migrations"),
          ("NoteRepository", "transactional appends"),
          ("SettingsRepository", "DataStore preferences"),
          ("AssetStore", "scoped private files")]),
    ]
    for name, y, h, fill, stroke, items in layers:
        c.rect(60, y, 1300, h, fill=(252, 253, 255), stroke=stroke, radius=8, width=1.4)
        c.text(76, y + 12, name, size=11.5, bold=True, color=stroke)
        cols = 3 if len(items) > 4 else 2
        cw = (1300 - 32 - (cols - 1) * 16) / cols
        for i, (t, s) in enumerate(items):
            col, row = i % cols, i // cols
            c.node(76 + col * (cw + 16), y + 40 + row * 74, cw, 62, t, [s],
                   fill=fill, stroke=stroke, radius=5, title_size=10.4, body_size=8.8)

    L = (120, 128, 142)
    for y1, y2 in ((300, 330), (530, 560), (760, 790)):
        c.arrow([(710, y1), (710, y2)], color=L, width=1.4)

    c.rect(60, 1030, 1300, 58, fill=(248, 250, 253), stroke=(206, 212, 222), radius=6)
    c.text(710, 1059, "HTTPS  -  bearer token in the client, provider key never in the APK",
           size=10.6, bold=True, anchor="cm", color=(70, 78, 94))
    c.arrow([(710, 990), (710, 1030)], color=L, width=1.4)
    c.arrow([(710, 1088), (710, 1120)], color=L, width=1.4)

    c.rect(60, 1120, 640, 145, fill=P.amber, stroke=P.amber_s, radius=8, width=1.4)
    c.text(78, 1132, "Hosted proxy  -  Node 22, no dependencies", size=11.5, bold=True,
           color=P.amber_s)
    for i, (t, s) in enumerate([("Bearer auth", "constant-time compare"),
                                ("Rate + budget", "per minute, per day"),
                                ("Schema enforcement", "strict json_schema"),
                                ("Grounding guard", "delta vs evidence")]):
        c.node(78 + i * 152, 1162, 144, 86, t, [s], fill=(255, 255, 255),
               stroke=P.amber_s, radius=5, title_size=9.6, body_size=8.4)

    c.rect(740, 1120, 620, 145, fill=P.grey, stroke=P.grey_s, radius=8, width=1.4)
    c.text(758, 1132, "Third party  -  model provider", size=11.5, bold=True, color=P.grey_s)
    for i, (t, s) in enumerate([("Transcription", "audio to text + timing"),
                                ("Vision extraction", "board text, equations"),
                                ("Note composition", "Markdown delta")]):
        c.node(758 + i * 200, 1162, 190, 86, t, [s], fill=(255, 255, 255),
               stroke=P.grey_s, radius=5, title_size=9.6, body_size=8.4)

    c.arrow([(700, 1192), (740, 1192)], color=L, width=1.4,
            label="OpenAI-compatible\nchat/completions", label_size=8.6)
    c.save(os.path.join(OUT, "fig-architecture.png"))


# ---------------------------------------------------------------- component
def component():
    c = Canvas(1400, 900)
    title(c, "5.2", "UML component diagram",
          "Provided interfaces are drawn as lollipops, required interfaces as sockets. "
          "Arrows point from the user of an interface to its provider.")

    def comp(x, y, w, h, name, stereo, lines, fill, stroke):
        c.rect(x, y, w, h, fill=fill, stroke=stroke, radius=4, width=1.3)
        c.text(x + w / 2, y + 12, "<<component>>", size=8.2, color=P.muted, anchor="ct")
        c.text(x + w / 2, y + 26, name, size=11.2, bold=True, anchor="ct")
        c.rect(x + w - 26, y + 8, 18, 13, fill=fill, stroke=stroke, width=1.1)
        c.rect(x + w - 30, y + 10, 8, 4, fill=fill, stroke=stroke, width=1.0)
        c.rect(x + w - 30, y + 16, 8, 4, fill=fill, stroke=stroke, width=1.0)
        yy = y + 48
        for ln in lines:
            c.text(x + 12, yy, "- " + ln, size=8.8, color=(84, 92, 106))
            yy += 15
        if stereo:
            c.text(x + w / 2, y + h - 18, stereo, size=8.4, mono=True,
                   color=stroke, anchor="ct")
        return (x, y, w, h)

    ui = comp(60, 120, 250, 150, "ui", "16 screens",
              ["navigation graph", "theme and primitives", "Markdown renderer"],
              P.violet, P.violet_s)
    cap = comp(60, 340, 250, 150, "capture", "audio + camera",
               ["PcmAudioChunkRecorder", "FrameChangeDetector", "SpeechPaceTracker"],
               P.teal, P.teal_s)
    notes = comp(430, 120, 250, 150, "notes", "Room + repository",
                 ["VoxBoxDatabase", "NoteRepository", "5 migrations"],
                 P.blue, P.blue_s)
    ai = comp(430, 340, 250, 150, "ai", "client",
              ["AiClient", "delta assembler", "retry and backoff"],
              P.green, P.green_s)
    dbg = comp(430, 590, 250, 130, "debug", "temporary",
               ["ring buffer, 500 lines", "export to share sheet"],
               P.red, P.red_s)
    prox = comp(830, 230, 250, 170, "proxy", "Node 22",
                ["auth and rate limit", "schema enforcement", "grounding guard",
                 "budget accounting"], P.amber, P.amber_s)
    prov = comp(830, 500, 250, 130, "provider", "third party",
                ["transcription", "vision", "composition"], P.grey, P.grey_s)

    L = (110, 118, 132)

    def lolli(x, y, name, d="r"):
        dx = 30 if d == "r" else -30
        c.line([(x, y), (x + dx, y)], color=L, width=1.2)
        c.ellipse(x + dx - 6, y - 6, 12, 12, fill=P.white, stroke=L, width=1.3)
        c.text(x + dx + (12 if d == "r" else -12), y - 16, name, size=8.4,
               color=P.muted, anchor=("l" if d == "r" else "r") + "t")

    lolli(680, 160, "NoteRepository")
    lolli(680, 400, "AiClient")
    lolli(1080, 300, "POST /v1/notes/*")

    c.arrow([(310, 170), (430, 170)], color=L, dash=(5, 4), hollow=True,
            label="<<use>>", label_size=8.2)
    c.arrow([(310, 220), (380, 220), (380, 400), (430, 400)], color=L, dash=(5, 4),
            hollow=True, label="<<use>>", label_size=8.2)
    c.arrow([(310, 410), (430, 410)], color=L, dash=(5, 4), hollow=True,
            label="<<use>>", label_size=8.2)
    c.arrow([(680, 440), (760, 440), (760, 200), (680, 200)], color=L, dash=(5, 4),
            hollow=True, label="persists", label_size=8.2)
    c.arrow([(680, 370), (830, 370)], color=L, dash=(5, 4), hollow=True,
            label="HTTPS + bearer", label_size=8.2)
    c.arrow([(955, 400), (955, 500)], color=L, dash=(5, 4), hollow=True,
            label="server key", label_size=8.2)
    c.arrow([(310, 460), (390, 460), (390, 640), (430, 640)], color=L, dash=(5, 4),
            hollow=True)
    c.arrow([(680, 640), (760, 640), (760, 420), (830, 420)], color=L, dash=(5, 4),
            hollow=True, head=False)

    c.band(60, 780, 1280, 90, "Removal boundary")
    c.text(78, 808, "The debug component is the only one marked temporary. It has no inbound "
                    "dependency from any other component, is compiled out of release builds, and its\n"
                    "removal checklist lives in docs/TEMPORARY_DEBUG_LOG.md. It was added to diagnose "
                    "the settle-gate defect described in Chapter 9 and is scheduled for deletion.",
           size=9.4, color=(76, 84, 98))
    c.save(os.path.join(OUT, "fig-component.png"))


# --------------------------------------------------------------- deployment
def deployment():
    c = Canvas(1360, 780)
    title(c, "5.3", "UML deployment diagram",
          "Three nodes, two network hops. The provider credential exists on exactly one of them.")

    def dnode(x, y, w, h, name, stereo, fill, stroke):
        d = 22
        c.poly([(x, y + d), (x + d, y), (x + w, y), (x + w - d, y + d)],
               fill=(min(fill[0] + 6, 255), min(fill[1] + 6, 255), min(fill[2] + 6, 255)),
               stroke=stroke, width=1.2)
        c.poly([(x + w, y), (x + w, y + h - d), (x + w - d, y + h), (x + w - d, y + d)],
               fill=fill, stroke=stroke, width=1.2)
        c.rect(x, y + d, w - d, h - d, fill=P.white, stroke=stroke, width=1.3)
        c.text(x + (w - d) / 2, y + d + 10, "<<" + stereo + ">>", size=8.4,
               color=P.muted, anchor="ct")
        c.text(x + (w - d) / 2, y + d + 24, name, size=12, bold=True, anchor="ct")
        return (x, y + d, w - d, h - d)

    dnode(60, 110, 400, 470, "Android phone", "device", P.blue, P.blue_s)
    for i, (t, s) in enumerate([
            ("VoxBox.apk", "minSdk 26, target 36, Kotlin 2.2"),
            ("voxbox.db", "SQLite via Room, schema v5"),
            ("settings.preferences_pb", "DataStore"),
            ("files/assets/<noteId>/", "board crops, retained WAV")]):
        c.node(80, 190 + i * 90, 360, 74, t, [s], fill=P.blue, stroke=P.blue_s,
               radius=5, title_size=10.4, body_size=8.8)

    dnode(560, 110, 380, 470, "Render web service", "container", P.amber, P.amber_s)
    for i, (t, s) in enumerate([
            ("server.mjs", "Node 22, zero dependencies"),
            ("Docker image", "node:22-alpine, non-root"),
            ("Environment", "OPENROUTER_API_KEY, VOXBOX_CLIENT_TOKEN"),
            ("In-memory counters", "rate window, daily budget")]):
        c.node(580, 190 + i * 90, 340, 74, t, [s], fill=P.amber, stroke=P.amber_s,
               radius=5, title_size=10.4, body_size=8.8)

    dnode(1030, 190, 300, 300, "OpenRouter", "external", P.grey, P.grey_s)
    for i, (t, s) in enumerate([("Transcription model", "audio in, text out"),
                                ("Vision model", "image in, structure out"),
                                ("Text model", "context in, delta out")]):
        c.node(1050, 270 + i * 68, 260, 56, t, [s], fill=P.grey, stroke=P.grey_s,
               radius=5, title_size=9.8, body_size=8.4)

    L = (100, 108, 122)
    c.arrow([(440, 300), (560, 300)], color=L, width=1.5, back=True,
            label="HTTPS / TLS 1.3\nBearer <client token>", label_size=8.8)
    c.arrow([(920, 340), (1030, 340)], color=L, width=1.5, back=True,
            label="HTTPS\nBearer <provider key>", label_size=8.8)

    c.rect(60, 620, 1270, 120, fill=(252, 246, 246), stroke=P.red_s, radius=6, width=1.2)
    c.text(78, 634, "Credential placement", size=11, bold=True, color=P.red_s)
    c.text(78, 658, "The provider key exists only in the Render environment. It is never compiled "
                    "into the APK, never sent to the phone, and never written to the repository.\n"
                    "The phone holds a client token instead, which authorises use of the proxy and "
                    "nothing else: it cannot be replayed against the provider, its blast radius is bounded\n"
                    "by the proxy's rate limit and daily budget, and it can be rotated by changing one "
                    "environment variable without rebuilding the application.",
           size=9.4, color=(88, 66, 66))
    c.save(os.path.join(OUT, "fig-deployment.png"))


# ------------------------------------------------------------ trust boundary
def security():
    c = Canvas(1360, 820)
    title(c, "10.1", "Trust boundaries and threat surface",
          "Each dashed boundary is a place where data changes owner. The table beneath names the "
          "threat considered at each one and the control that answers it.")

    c.band(60, 100, 380, 400, "Boundary A  -  the phone")
    c.node(84, 140, 330, 66, "Microphone and camera", ["runtime permission, revocable"],
           fill=P.blue, stroke=P.blue_s, radius=5, title_size=10.4, body_size=8.6)
    c.node(84, 222, 330, 66, "Private app storage", ["no external write, no MediaStore"],
           fill=P.blue, stroke=P.blue_s, radius=5, title_size=10.4, body_size=8.6)
    c.node(84, 304, 330, 66, "Client token in APK", ["extractable, accepted risk"],
           fill=P.red, stroke=P.red_s, radius=5, title_size=10.4, body_size=8.6)
    c.node(84, 386, 330, 66, "Debug log (temporary)", ["prefix only, 60 chars, no upload"],
           fill=P.amber, stroke=P.amber_s, radius=5, title_size=10.4, body_size=8.6)

    c.band(500, 100, 380, 400, "Boundary B  -  the proxy")
    c.node(524, 140, 330, 66, "Bearer check", ["constant-time comparison"],
           fill=P.green, stroke=P.green_s, radius=5, title_size=10.4, body_size=8.6)
    c.node(524, 222, 330, 66, "Rate limit and budget", ["per-minute window, daily cap"],
           fill=P.green, stroke=P.green_s, radius=5, title_size=10.4, body_size=8.6)
    c.node(524, 304, 330, 66, "Schema enforcement", ["strict json_schema, no free text"],
           fill=P.green, stroke=P.green_s, radius=5, title_size=10.4, body_size=8.6)
    c.node(524, 386, 330, 66, "Provider key", ["environment only, rotatable"],
           fill=P.green, stroke=P.green_s, radius=5, title_size=10.4, body_size=8.6)

    c.band(940, 100, 380, 400, "Boundary C  -  the provider")
    c.node(964, 140, 330, 66, "Model inference", ["third-party retention policy applies"],
           fill=P.grey, stroke=P.grey_s, radius=5, title_size=10.4, body_size=8.6)
    c.node(964, 222, 330, 66, "No account linkage", ["proxy sends no student identity"],
           fill=P.grey, stroke=P.grey_s, radius=5, title_size=10.4, body_size=8.6)
    c.node(964, 304, 330, 148, "Residual risk",
           ["Lecture audio is processed by a third party.",
            "This is disclosed, not mitigated: the honest",
            "statement is that the recording leaves the",
            "device, and consent is the student's to give."],
           fill=P.red, stroke=P.red_s, radius=5, title_size=10.4, body_size=8.6)

    L = (110, 118, 132)
    c.arrow([(440, 300), (500, 300)], color=L, width=1.4)
    c.arrow([(880, 300), (940, 300)], color=L, width=1.4)

    rows = [
        ("A", "APK is decompiled and the client token lifted",
         "Token is scoped to the proxy alone; rate limit and daily budget bound the loss; rotation is one variable"),
        ("A", "Another application reads captured audio or crops",
         "All files live in app-private storage; nothing is written to shared media"),
        ("A", "Diagnostic log leaks lecture content",
         "Speech is stored as a length and a 60-character prefix, never in full; the log is local and compiled out of release"),
        ("B", "Unauthenticated caller drains the budget",
         "Bearer token required before any provider call is made; failures are counted, not proxied"),
        ("B", "Model returns prose where structure is required",
         "Response is rejected unless it validates against the declared JSON schema"),
        ("B", "Model fabricates content not present in the evidence",
         "Grounding guard scores the delta against the evidence and discards it below the floor"),
        ("C", "Provider retains or trains on lecture audio",
         "Not mitigated. Disclosed in Chapter 10 and in the consent screen; the deployment holds no student identifier"),
    ]
    y = 540
    c.rect(60, y, 1260, 26, fill=(238, 241, 246), stroke=(206, 212, 222), width=1.1)
    for cx, t in ((76, "At"), (130, "Threat considered"), (640, "Control in place")):
        c.text(cx, y + 7, t, size=9.6, bold=True)
    y += 26
    for b, th, ct in rows:
        c.rect(60, y, 1260, 32, fill=P.white, stroke=(214, 220, 230), width=1.0)
        c.text(76, y + 10, b, size=9.4, bold=True, mono=True, color=P.blue_s)
        c.text(130, y + 10, th, size=9.2)
        c.text(640, y + 10, ct, size=9.2, color=(76, 84, 98))
        y += 32
    c.save(os.path.join(OUT, "fig-security.png"))


if __name__ == "__main__":
    context_dfd(); dfd_l1(); architecture(); component(); deployment(); security()
