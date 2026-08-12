"""Entity-relationship diagram for the VoxBox Room schema."""

import sys, os
sys.path.insert(0, os.path.dirname(__file__))
from dsl import Canvas, P

ROW = 16.5
HEAD = 27


def entity(c, x, y, w, name, attrs, accent=P.blue_s, tint=P.blue):
    h = HEAD + ROW * len(attrs) + 9
    c.rect(x, y, w, h, fill=(255, 255, 255), stroke=accent, radius=4, width=1.4)
    c.rect(x, y, w, HEAD, fill=tint, stroke=None, width=0)
    c.line([(x, y + HEAD), (x + w, y + HEAD)], color=accent, width=1.2)
    c.rect(x, y, w, h, fill=None, stroke=accent, radius=4, width=1.4)
    c.text(x + w / 2, y + HEAD / 2, name, size=11, bold=True, mono=True,
           color=accent, anchor="cm")
    yy = y + HEAD + 5
    for a in attrs:
        key = a.startswith("*") or a.startswith("#")
        label = a.lstrip("*#")
        mark = ""
        if a.startswith("*"):
            mark = "PK"
        elif a.startswith("#"):
            mark = "FK"
        c.text(x + 9, yy, label, size=8.6, mono=True, bold=key,
               color=P.ink if key else (86, 94, 108))
        if mark:
            c.text(x + w - 9, yy, mark, size=7.4, bold=True,
                   color=accent if mark == "PK" else P.muted, anchor="rt")
        yy += ROW
    return (x, y, w, h)


def foot(c, x, y, d, color=P.line):
    """Crow's foot at (x,y) opening in direction d = l/r/t/b."""
    s = 7
    if d in ("l", "r"):
        sx = -s if d == "r" else s
        for dy in (-5, 0, 5):
            c.line([(x, y), (x + sx, y + dy)], color=color, width=1.1)
    else:
        sy = -s if d == "b" else s
        for dx in (-5, 0, 5):
            c.line([(x, y), (x + dx, y + sy)], color=color, width=1.1)


def one(c, x, y, d, color=P.line):
    if d in ("l", "r"):
        c.line([(x, y - 5), (x, y + 5)], color=color, width=1.4)
    else:
        c.line([(x - 5, y), (x + 5, y)], color=color, width=1.4)


def build(out):
    c = Canvas(1780, 1000)
    c.text(40, 30, "Entity-Relationship diagram - VoxBox on-device database (Room, schema version 5)",
           size=15, bold=True)
    c.text(40, 52, "Crow's foot notation.  A bar marks the 'one' end, a fork marks the 'many' end.  "
                   "PK = primary key, FK = foreign key.  All ten tables live in a single SQLite file inside "
                   "the application's private storage.", size=9.6, color=P.muted)

    y0 = 92
    folders = entity(c, 50, y0 + 120, 250, "folders",
                     ["*id", "#parentId", "name", "createdAt", "updatedAt"],
                     P.violet_s, P.violet)
    syllabi = entity(c, 50, y0 + 500, 250, "syllabi",
                     ["*id", "title", "localRelativePath", "extractedText",
                      "sha256  (unique)", "importedAt", "updatedAt"],
                     P.teal_s, P.teal)
    nloc = entity(c, 380, y0 + 40, 260, "note_locations",
                  ["*#noteId", "#folderId"], P.violet_s, P.violet)
    notes = entity(c, 380, y0 + 250, 260, "notes",
                   ["*id", "title", "createdAt", "updatedAt"], P.blue_s, P.blue)
    blocks = entity(c, 720, y0 + 40, 300, "note_blocks",
                    ["*id", "#noteId", "position", "type", "content",
                     "chartValue", "accentColor", "label"], P.blue_s, P.blue)
    sess = entity(c, 720, y0 + 330, 300, "capture_sessions",
                  ["*id", "#noteId", "#syllabusId", "#generatedBlockId",
                   "mode", "notePolicy", "status", "revision", "lastPatchId",
                   "frameIntervalMs", "changeThreshold", "primarySpeakerId",
                   "startedAt", "stoppedAt", "updatedAt"], P.green_s, P.green)
    prov = entity(c, 1110, y0 + 40, 290, "block_provenance",
                  ["*#blockId", "#sessionId", "sourceKind", "sourceId",
                   "startMs", "endMs", "confidence", "reviewState"],
                  P.amber_s, P.amber)
    tseg = entity(c, 1450, y0 + 330, 290, "transcript_segments",
                  ["*id", "#sessionId", "position", "speakerId", "startMs",
                   "endMs", "text", "isFinal", "confidence", "createdAt"],
                  P.amber_s, P.amber)
    vis = entity(c, 1110, y0 + 620, 290, "visual_evidence",
                 ["*id", "#sessionId", "capturedAt", "fingerprint", "deltaScore",
                  "processingState", "temporaryPath", "error", "processedAt"],
                 P.red_s, P.red)
    asset = entity(c, 720, y0 + 700, 300, "note_assets",
                   ["*id", "#noteId", "#evidenceId", "kind",
                    "localRelativePath  (unique)", "caption", "createdAt"],
                   P.red_s, P.red)

    L = (96, 104, 118)

    def rel(pts, a_end, b_end, label, lx=None, ly=None, dash=None):
        c.line(pts, color=L, width=1.15, dash=dash)
        (x1, y1) = pts[0]
        (x2, y2) = pts[-1]
        (a_kind, a_dir) = a_end
        (b_kind, b_dir) = b_end
        (one if a_kind == "1" else foot)(c, x1, y1, a_dir, L)
        (one if b_kind == "1" else foot)(c, x2, y2, b_dir, L)
        if label:
            mx = lx if lx is not None else (x1 + x2) / 2
            my = ly if ly is not None else (y1 + y2) / 2
            c.tag(mx, my, label, size=8.4)

    fx, fy, fw, fh = folders
    # folders self-reference (nesting)
    c.line([(fx + fw - 40, fy), (fx + fw - 40, fy - 34), (fx + 34, fy - 34),
            (fx + 34, fy)], color=L, width=1.15)
    one(c, fx + fw - 40, fy, "t", L)
    foot(c, fx + 34, fy, "t", L)
    c.tag(fx + fw / 2, fy - 34, "contains  (nesting)", size=8.4)

    # folders 1 --- N note_locations
    rel([(fx + fw, fy + 30), (355, fy + 30), (355, y0 + 40 + 40), (380, y0 + 40 + 40)],
        ("1", "r"), ("N", "l"), "filed in", lx=340, ly=y0 + 118)

    nx, ny, nw, nh = notes
    # notes 1 --- 1 note_locations
    rel([(nx + 60, ny), (nx + 60, y0 + 40 + nloc[3])],
        ("1", "b"), ("1", "t"), "0..1", lx=nx + 60, ly=y0 + 190)
    # notes 1 --- N note_blocks
    rel([(nx + nw, ny + 30), (680, ny + 30), (680, y0 + 120), (720, y0 + 120)],
        ("1", "r"), ("N", "l"), "consists of", lx=676, ly=ny + 8)
    # notes 1 --- N capture_sessions
    rel([(nx + nw, ny + 66), (700, ny + 66), (700, y0 + 420), (720, y0 + 420)],
        ("1", "r"), ("N", "l"), "recorded by", lx=690, ly=y0 + 300)
    # notes 1 --- N note_assets
    rel([(nx + nw, ny + 88), (690, ny + 88), (690, y0 + 760), (720, y0 + 760)],
        ("1", "r"), ("N", "l"), "owns", lx=690, ly=y0 + 620)

    sx, sy, sw, sh = syllabi
    # syllabi 1 --- N capture_sessions
    rel([(sx + sw, sy + 40), (660, sy + 40), (660, y0 + 470), (720, y0 + 470)],
        ("1", "r"), ("N", "l"), "guides", lx=628, ly=y0 + 470)

    bx, by, bw, bh = blocks
    # note_blocks 1 --- 1 capture_sessions (generatedBlockId, unique)
    rel([(bx + 150, by + bh), (bx + 150, y0 + 330)],
        ("1", "b"), ("1", "t"), "living block", lx=bx + 150, ly=y0 + 260)
    # note_blocks 1 --- 1 block_provenance
    rel([(bx + bw, by + 40), (1110, by + 40)],
        ("1", "r"), ("1", "l"), "traced by", lx=1065, ly=by + 24)

    ex, ey, ew, eh = sess
    # capture_sessions 1 --- N block_provenance
    rel([(ex + ew, ey + 30), (1060, ey + 30), (1060, y0 + 210), (1110, y0 + 210)],
        ("1", "r"), ("N", "l"), "attributes", lx=1058, ly=y0 + 300)
    # capture_sessions 1 --- N transcript_segments
    rel([(ex + ew, ey + 120), (1450, ey + 120)],
        ("1", "r"), ("N", "l"), "produces", lx=1230, ly=ey + 104)
    # capture_sessions 1 --- N visual_evidence
    rel([(ex + ew, ey + 220), (1075, ey + 220), (1075, y0 + 700), (1110, y0 + 700)],
        ("1", "r"), ("N", "l"), "captures", lx=1078, ly=y0 + 640)

    vx, vy, vw, vh = vis
    ax, ay, aw, ah = asset
    # visual_evidence 1 --- 0..1 note_assets
    rel([(vx, vy + 60), (ax + aw, vy + 60)],
        ("1", "l"), ("1", "r"), "promoted to", lx=1065, ly=vy + 44)

    # legend
    lx, ly = 50, y0 + 700
    c.rect(lx, ly, 600, 190, fill=(250, 251, 253), stroke=(212, 218, 228), radius=5)
    c.text(lx + 14, ly + 12, "Reading the schema", size=11, bold=True)
    txt = [
        "notes and note_blocks are the durable artefact; every other table exists to justify them.",
        "capture_sessions is the transactional heart: revision guards concurrent appends and",
        "generatedBlockId points at the single living block the AI is allowed to extend.",
        "transcript_segments and visual_evidence are evidence of record and are never rewritten.",
        "block_provenance answers 'where did this sentence come from?' for any block on screen.",
        "note_locations is a separate table rather than a column on notes so that a note not yet",
        "filed has no row at all, which keeps the unfiled case free of a sentinel folder id.",
    ]
    yy = ly + 38
    for t in txt:
        c.text(lx + 14, yy, t, size=9.2, color=(76, 84, 98))
        yy += 20
    c.save(os.path.join(out, "fig-er-diagram.png"))


if __name__ == "__main__":
    build(sys.argv[1] if len(sys.argv) > 1 else "D:/CollegeProject/docs/diagrams")
