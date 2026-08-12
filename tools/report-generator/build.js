/**
 * Builds the VoxBox project report as a .docx.
 *
 * Narrative lives in content*.js. This file is layout only. The source-code appendices are read
 * from the repository at build time, so re-running this after a code change regenerates a report
 * whose listings match the tree.
 */
const fs = require("fs");
const path = require("path");
const {
  AlignmentType, BorderStyle, Document, Footer, Header, HeadingLevel, ImageRun, LevelFormat,
  NumberFormat, Packer, PageBreak, PageNumber, Paragraph, ShadingType, Table, TableCell, TableRow,
  TableOfContents, TextRun, WidthType,
} = require("docx");

const A = require("./content");
const B = require("./content2");
const C = require("./content3");
const D = require("./content4");
const E = require("./content5");
const F = require("./content6");
const G = require("./content7");
const H = require("./content8");
const I = require("./content9");

const REPO = "D:/CollegeProject";
const OUT = path.join(REPO, "outputs", "VoxBox_Project_Report.docx");

const SERIF = "Cambria";
const SANS = "Calibri";
const MONO = "Consolas";
const INK = "1A1A1A";
const MUTED = "5A6270";
const RULE = "C8CDD6";
const ACCENT = "1E3A8A";

// ---------------------------------------------------------------- block renderers

function para(text, opts = {}) {
  return new Paragraph({
    spacing: { after: opts.after ?? 140, line: opts.line ?? 300 },
    alignment: opts.align ?? AlignmentType.JUSTIFIED,
    indent: opts.indent,
    border: opts.border,
    children: [
      new TextRun({
        text,
        font: opts.font ?? SANS,
        size: opts.size ?? 21,
        color: opts.color ?? INK,
        bold: opts.bold,
        italics: opts.italics,
      }),
    ],
  });
}

function heading(text, level) {
  const sizes = { 1: 32, 2: 26, 3: 23 };
  return new Paragraph({
    heading: level === 1 ? HeadingLevel.HEADING_1 : level === 2 ? HeadingLevel.HEADING_2 : HeadingLevel.HEADING_3,
    spacing: { before: level === 1 ? 0 : 280, after: level === 1 ? 240 : 140 },
    pageBreakBefore: level === 1,
    children: [new TextRun({ text, font: SERIF, size: sizes[level], bold: true, color: level === 1 ? ACCENT : INK })],
  });
}

function bullets(items) {
  return items.map((t) => new Paragraph({
    numbering: { reference: "vb-bullets", level: 0 },
    spacing: { after: 90, line: 290 },
    children: [new TextRun({ text: t, font: SANS, size: 21, color: INK })],
  }));
}

function numbered(items) {
  return items.map((t) => new Paragraph({
    numbering: { reference: "vb-numbers", level: 0 },
    spacing: { after: 90, line: 290 },
    children: [new TextRun({ text: t, font: SANS, size: 21, color: INK })],
  }));
}

function cell(text, { header = false, width, mono = false } = {}) {
  return new TableCell({
    width: { size: width, type: WidthType.DXA },
    shading: header ? { type: ShadingType.CLEAR, fill: "EDF1F7" } : undefined,
    margins: { top: 90, bottom: 90, left: 110, right: 110 },
    children: [new Paragraph({
      spacing: { after: 0, line: 260 },
      children: [new TextRun({
        text,
        font: mono ? MONO : SANS,
        size: mono ? 16 : 19,
        bold: header,
        color: INK,
      })],
    })],
  });
}

function table(spec) {
  const total = spec.widths.reduce((a, b) => a + b, 0);
  const rows = [
    new TableRow({
      tableHeader: true,
      children: spec.head.map((h, i) => cell(h, { header: true, width: spec.widths[i] })),
    }),
    ...spec.rows.map((r) => new TableRow({
      children: r.map((c, i) => cell(String(c), { width: spec.widths[i] })),
    })),
  ];
  return [
    new Table({ columnWidths: spec.widths, width: { size: total, type: WidthType.DXA }, rows }),
    new Paragraph({ spacing: { after: 200 }, children: [] }),
  ];
}

function codeBlock(text, caption) {
  const out = text.split("\n").map((line) => new Paragraph({
    spacing: { after: 0, line: 200 },
    indent: { left: 220 },
    children: [new TextRun({ text: line.length ? line : " ", font: MONO, size: 16, color: "232629" })],
  }));
  if (caption) {
    out.push(new Paragraph({
      spacing: { before: 90, after: 220 },
      alignment: AlignmentType.CENTER,
      children: [new TextRun({ text: caption, font: SANS, size: 17, italics: true, color: MUTED })],
    }));
  } else {
    out.push(new Paragraph({ spacing: { after: 200 }, children: [] }));
  }
  return out;
}

function note(text) {
  return new Paragraph({
    spacing: { before: 120, after: 180, line: 290 },
    indent: { left: 400, right: 300 },
    border: { left: { style: BorderStyle.SINGLE, size: 12, color: ACCENT, space: 12 } },
    children: [new TextRun({ text, font: SANS, size: 20, italics: true, color: MUTED })],
  });
}

// ---------------------------------------------------------------- figures
//
// The text column is 9406 twips wide (A4 less the margins), which is 6.53 inches, which is
// 627 pixels at the 96 dpi docx-js assumes. Figures are scaled to fit that and no wider.

const TEXT_WIDTH_PX = 620;
const FIGURES = [];   // { num, caption } in document order, for the List of Figures
const TABLES = [];    // { num, caption } in document order, for the List of Tables

/** Reads width and height straight out of a PNG IHDR chunk. */
function pngSize(file) {
  const b = fs.readFileSync(file);
  if (b.length < 24 || b.readUInt32BE(0) !== 0x89504e47) throw new Error("not a PNG: " + file);
  return { w: b.readUInt32BE(16), h: b.readUInt32BE(20), data: b };
}

function figure(num, file, caption, { maxW = TEXT_WIDTH_PX, maxH = 780 } = {}) {
  const abs = path.isAbsolute(file) ? file : path.join(REPO, file);
  const { w, h, data } = pngSize(abs);
  let scale = Math.min(maxW / w, maxH / h, 1);
  const width = Math.round(w * scale);
  const height = Math.round(h * scale);
  FIGURES.push({ num, caption });
  return [
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { before: 200, after: 90 },
      children: [new ImageRun({ type: "png", data, transformation: { width, height } })],
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 260 },
      children: [
        new TextRun({ text: "Figure " + num + "  ", font: SANS, size: 18, bold: true, color: ACCENT }),
        new TextRun({ text: caption, font: SANS, size: 18, italics: true, color: MUTED }),
      ],
    }),
  ];
}

/** A row of phone screenshots, side by side, each with its own caption. */
function shots(items, { perRow = 2, width = 236 } = {}) {
  const out = [];
  for (let i = 0; i < items.length; i += perRow) {
    const row = items.slice(i, i + perRow);
    const cells = row.map((it) => {
      const abs = path.join(REPO, "docs/screenshots", it.file);
      const { w, h, data } = pngSize(abs);
      const height = Math.round(h * (width / w));
      FIGURES.push({ num: it.num, caption: it.caption });
      return new TableCell({
        width: { size: Math.floor(9406 / perRow), type: WidthType.DXA },
        margins: { top: 120, bottom: 120, left: 120, right: 120 },
        borders: {
          top: { style: BorderStyle.NONE }, bottom: { style: BorderStyle.NONE },
          left: { style: BorderStyle.NONE }, right: { style: BorderStyle.NONE },
        },
        children: [
          new Paragraph({
            alignment: AlignmentType.CENTER,
            spacing: { after: 80 },
            children: [new ImageRun({ type: "png", data, transformation: { width, height } })],
          }),
          new Paragraph({
            alignment: AlignmentType.CENTER,
            spacing: { after: 0 },
            children: [
              new TextRun({ text: "Figure " + it.num + "  ", font: SANS, size: 17, bold: true, color: ACCENT }),
              new TextRun({ text: it.caption, font: SANS, size: 17, italics: true, color: MUTED }),
            ],
          }),
        ],
      });
    });
    while (cells.length < perRow) {
      cells.push(new TableCell({
        width: { size: Math.floor(9406 / perRow), type: WidthType.DXA },
        borders: {
          top: { style: BorderStyle.NONE }, bottom: { style: BorderStyle.NONE },
          left: { style: BorderStyle.NONE }, right: { style: BorderStyle.NONE },
        },
        children: [new Paragraph({ children: [] })],
      }));
    }
    out.push(new Table({
      columnWidths: new Array(perRow).fill(Math.floor(9406 / perRow)),
      width: { size: 9406, type: WidthType.DXA },
      rows: [new TableRow({ children: cells })],
    }));
    out.push(new Paragraph({ spacing: { after: 200 }, children: [] }));
  }
  return out;
}

function tableCaption(num, caption) {
  TABLES.push({ num, caption });
  return new Paragraph({
    spacing: { before: 40, after: 160 },
    alignment: AlignmentType.CENTER,
    children: [
      new TextRun({ text: "Table " + num + "  ", font: SANS, size: 18, bold: true, color: ACCENT }),
      new TextRun({ text: caption, font: SANS, size: 18, italics: true, color: MUTED }),
    ],
  });
}

function render(blocks) {
  const out = [];
  blocks.forEach((b) => {
    if (b.h1) out.push(heading(b.h1, 1));
    else if (b.h2) out.push(heading(b.h2, 2));
    else if (b.h3) out.push(heading(b.h3, 3));
    else if (b.p) out.push(para(b.p));
    else if (b.b) out.push(...bullets(b.b));
    else if (b.n) out.push(...numbered(b.n));
    else if (b.table) {
      out.push(...table(b.table));
      if (b.tnum) out.push(tableCaption(b.tnum, b.caption));
    } else if (b.code) out.push(...codeBlock(b.code, b.caption));
    else if (b.note) out.push(note(b.note));
    else if (b.fig) out.push(...figure(b.num, b.fig, b.caption, b.opts));
    else if (b.shots) out.push(...shots(b.shots, b.opts));
    else if (b.pagebreak) out.push(new Paragraph({ children: [new PageBreak()] }));
  });
  return out;
}

// ---------------------------------------------------------------- front matter

function titlePage() {
  const gap = (n) => new Paragraph({ spacing: { after: n }, children: [] });
  return [
    gap(1400),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 120 },
      children: [new TextRun({ text: "STRUCTURED VOICE TO NOTES USING AI", font: SERIF, size: 52, bold: true, color: ACCENT })],
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 900 },
      children: [new TextRun({ text: "VoxBox", font: SERIF, size: 36, color: MUTED })],
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 260 },
      children: [new TextRun({ text: "A PROJECT REPORT", font: SANS, size: 24, bold: true, color: INK })],
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 900 },
      children: [new TextRun({
        text: "Submitted in partial fulfilment of the requirements for internal assessment",
        font: SANS, size: 20, color: MUTED,
      })],
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 120 },
      children: [new TextRun({ text: "Submitted by", font: SANS, size: 20, color: MUTED })],
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 700 },
      children: [new TextRun({ text: "Tejas", font: SANS, size: 26, bold: true, color: INK })],
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 120 },
      children: [new TextRun({ text: "Guide", font: SANS, size: 20, color: MUTED })],
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 700 },
      children: [new TextRun({ text: "_______________________________", font: SANS, size: 22, color: MUTED })],
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 100 },
      children: [new TextRun({ text: "Department of Computer Science and Engineering", font: SANS, size: 21, color: INK })],
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      children: [new TextRun({ text: "Academic Year 2026", font: SANS, size: 21, color: MUTED })],
    }),
    new Paragraph({ children: [new PageBreak()] }),
  ];
}

function signaturePages() {
  const line = (label) => [
    new Paragraph({ spacing: { before: 700, after: 60 }, children: [new TextRun({ text: "_______________________________", font: SANS, size: 22, color: MUTED })] }),
    new Paragraph({ spacing: { after: 200 }, children: [new TextRun({ text: label, font: SANS, size: 20, color: INK })] }),
  ];
  return [
    heading("Certificate", 1),
    para("This is to certify that the project entitled \"Structured Voice to Notes using AI\", " +
      "developed under the application name VoxBox, is a bona fide record of work carried out by " +
      "Tejas in partial fulfilment of the requirements for internal assessment during the academic " +
      "year 2026."),
    para("The work presented in this report has been carried out by the candidate and has not been " +
      "submitted elsewhere."),
    ...line("Guide"),
    ...line("Head of Department"),
    ...line("External Examiner"),

    heading("Declaration", 1),
    para("I declare that this project report is my own work. The application, the backend service " +
      "and the test suites described here were designed and implemented by me. Where third-party " +
      "libraries, platform APIs or hosted AI models have been used, they are identified in the " +
      "requirements chapter and in the references."),
    para("Measurements reported in this document were taken from the running system. Where a " +
      "quantity has not been measured, that is stated explicitly rather than estimated, and the " +
      "report identifies which claims remain unverified."),
    ...line("Tejas"),

    heading("Acknowledgement", 1),
    para("I thank my project guide for continued direction throughout the development of this " +
      "work, and the Department of Computer Science and Engineering for providing the environment " +
      "in which it was carried out."),
    para("I also acknowledge that several of the most instructive parts of this project came from " +
      "testing it in real lectures and finding that it did not behave as intended. Those failures, " +
      "and their corrections, are recorded in Chapter 13."),
  ];
}

function abstractPage() {
  return [
    heading("Abstract", 1),
    para("A lecture is delivered once. The spoken explanation exists only while it is being " +
      "spoken, and the board that carries the formulas is erased to make room for the next one. A " +
      "student has a single opportunity to convert both into something revisable, and must do so " +
      "while also trying to understand the material."),
    para("This project presents VoxBox, an Android application that converts a live lecture into a " +
      "structured Markdown note while the class is happening. It captures continuous audio in " +
      "overlapping twenty-second chunks and, in board mode, samples the board through the device " +
      "camera. An on-device change filter reduces approximately 450 camera frames per lecture-hour " +
      "to approximately 40 distinct board states before any network call is made, which is the " +
      "principal reason the system is affordable to run."),
    para("The design principle throughout is evidence preservation. The raw transcript and board " +
      "text are written to local storage before any AI refinement is attempted, and no model " +
      "output overwrites them. Where the model disagrees with the captured evidence the " +
      "disagreement is presented to the student as a review flag showing both readings, and " +
      "neither available action rewrites the captured line. A server-side grounding check discards " +
      "generated text whose vocabulary does not match the evidence it claims to summarise, after a " +
      "trial session established that a language model given thin evidence will produce fluent and " +
      "entirely fabricated content."),
    para("The system comprises 13,337 lines of Kotlin across 74 files, a dependency-free Node.js " +
      "proxy that holds the AI provider credential so that it never resides on the device, and " +
      "2,574 lines of test code. It is covered by 134 Android unit tests, 43 backend contract " +
      "tests and 3 instrumentation tests, and has been exercised in four field trials against real " +
      "lectures."),
    para("Six algorithms are the project's own work rather than calls to a model: on-device " +
      "frame-change detection with exposure compensation and a relative settle test; adaptive " +
      "sampling driven by measured speech pace; per-chunk dominant-speaker selection that reports " +
      "ambiguity rather than guessing; an append-only note protocol with content hashing and " +
      "optimistic revision control that keeps cost flat as the note grows; transcript overlap " +
      "de-duplication that stopped speech disappearing at chunk boundaries; and a grounding guard " +
      "against fabricated content. The AI provider was migrated once during development and none " +
      "of this logic changed."),
    para("Accuracy is not yet measured against a labelled corpus and no accuracy figure is claimed. " +
      "Constructing that corpus and scorer is the first item of future work, and the report states " +
      "explicitly which of its claims remain unverified."),
  ];
}

function tocPage() {
  return [
    heading("Table of Contents", 1),
    para("Right-click anywhere in the table below and choose \"Update Field\" to regenerate page " +
      "numbers after editing this document in Microsoft Word.",
      { size: 19, italics: true, color: MUTED }),
    // A real TOC field, not literal text. Word populates it on open or on "Update Field";
    // the build then repaginates and saves it so the delivered file already carries page numbers.
    new TableOfContents("Contents", {
      hyperlink: true,
      headingStyleRange: "1-3",
    }),
  ];
}

// ---------------------------------------------------------------- source appendices

function listFiles(dir, ext) {
  const out = [];
  (function walk(d) {
    for (const entry of fs.readdirSync(d, { withFileTypes: true })) {
      const full = path.join(d, entry.name);
      if (entry.isDirectory()) walk(full);
      else if (entry.name.endsWith(ext)) out.push(full);
    }
  })(dir);
  return out.sort();
}

function sourceListing(title, files, base) {
  const out = [heading(title, 2)];
  files.forEach((file) => {
    const rel = path.relative(base, file).replace(/\\/g, "/");
    const text = fs.readFileSync(file, "utf8").replace(/\r\n/g, "\n");
    const lines = text.split("\n");
    out.push(new Paragraph({
      spacing: { before: 260, after: 60 },
      keepNext: true,
      children: [new TextRun({ text: rel, font: MONO, size: 19, bold: true, color: ACCENT })],
    }));
    out.push(new Paragraph({
      spacing: { after: 100 },
      keepNext: true,
      border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: RULE, space: 2 } },
      children: [new TextRun({ text: `${lines.length} lines`, font: SANS, size: 16, color: MUTED })],
    }));
    lines.forEach((line, index) => {
      out.push(new Paragraph({
        spacing: { after: 0, line: 190 },
        children: [
          new TextRun({ text: String(index + 1).padStart(4, " ") + "  ", font: MONO, size: 13, color: "9AA3B0" }),
          new TextRun({ text: line.length ? line : " ", font: MONO, size: 14, color: "232629" }),
        ],
      }));
    });
  });
  return out;
}

// ---------------------------------------------------------------- generated registers
//
// These two appendices are extracted from the repository at build time rather than written by
// hand, so they cannot drift out of step with the tree the way a hand-maintained list would.

/** Every @Test in the suite, grouped by file, with its declared name. */
function testRegister() {
  const roots = [
    path.join(REPO, "VoxBox/app/src/test/java"),
    path.join(REPO, "VoxBox/app/src/androidTest/java"),
  ];
  const files = roots.flatMap((r) => (fs.existsSync(r) ? listFiles(r, ".kt") : []));
  const out = [];
  let total = 0;
  files.sort((a, b) => path.basename(a).localeCompare(path.basename(b))).forEach((file) => {
    const text = fs.readFileSync(file, "utf8").replace(/\r\n/g, "\n");
    const names = [];
    const lines = text.split("\n");
    for (let i = 0; i < lines.length; i++) {
      if (!/^\s*@Test\b/.test(lines[i])) continue;
      for (let j = i + 1; j < Math.min(i + 5, lines.length); j++) {
        const m = lines[j].match(/fun\s+`?([^`(]+)`?\s*\(/);
        if (m) { names.push(m[1].trim()); break; }
      }
    }
    if (!names.length) return;
    total += names.length;
    const suite = path.basename(file, ".kt");
    out.push(new Paragraph({
      spacing: { before: 240, after: 100 },
      keepNext: true,
      border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: RULE, space: 2 } },
      children: [
        new TextRun({ text: suite, font: MONO, size: 19, bold: true, color: ACCENT }),
        new TextRun({ text: "   " + names.length + " tests", font: SANS, size: 16, color: MUTED }),
      ],
    }));
    out.push(...table({
      head: ["#", "Test"],
      widths: [700, 8706],
      rows: names.map((n, i) => [String(i + 1), n.replace(/([a-z])([A-Z])/g, "$1 $2")]),
    }));
  });
  return { blocks: out, total, files: files.length };
}

/** Every Kotlin source file in the application, with its size. */
function fileInventory() {
  const base = path.join(REPO, "VoxBox/app/src/main/java/me/thimmaiah/voxbox");
  const files = listFiles(base, ".kt");
  const byPackage = new Map();
  let totalLines = 0;
  files.forEach((f) => {
    const rel = path.relative(base, f).replace(/\\/g, "/");
    const pkg = rel.includes("/") ? rel.slice(0, rel.lastIndexOf("/")) : "(root)";
    const lines = fs.readFileSync(f, "utf8").replace(/\r\n/g, "\n").split("\n").length;
    totalLines += lines;
    if (!byPackage.has(pkg)) byPackage.set(pkg, []);
    byPackage.get(pkg).push([path.basename(f), String(lines)]);
  });
  const out = [];
  [...byPackage.keys()].sort().forEach((pkg) => {
    const rows = byPackage.get(pkg);
    const sub = rows.reduce((a, r) => a + Number(r[1]), 0);
    out.push(new Paragraph({
      spacing: { before: 240, after: 100 },
      keepNext: true,
      border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: RULE, space: 2 } },
      children: [
        new TextRun({ text: pkg + "/", font: MONO, size: 19, bold: true, color: ACCENT }),
        new TextRun({ text: "   " + rows.length + " files, " + sub + " lines", font: SANS, size: 16, color: MUTED }),
      ],
    }));
    out.push(...table({
      head: ["File", "Lines"],
      widths: [7806, 1600],
      rows: rows.sort((a, b) => a[0].localeCompare(b[0])),
    }));
  });
  return { blocks: out, totalLines, count: files.length };
}

// ---------------------------------------------------------------- document

const TESTS = testRegister();
const INVENTORY = fileInventory();

const androidMain = listFiles(path.join(REPO, "VoxBox/app/src/main/java"), ".kt");
const androidTest = [
  ...listFiles(path.join(REPO, "VoxBox/app/src/test/java"), ".kt"),
  ...listFiles(path.join(REPO, "VoxBox/app/src/androidTest/java"), ".kt"),
];
const serverFiles = [
  path.join(REPO, "server/server.mjs"),
  path.join(REPO, "server/test/server.test.mjs"),
];
const buildFiles = [
  path.join(REPO, "VoxBox/app/build.gradle.kts"),
  path.join(REPO, "VoxBox/gradle/libs.versions.toml"),
  path.join(REPO, "VoxBox/app/src/main/AndroidManifest.xml"),
  path.join(REPO, "render.yaml"),
  path.join(REPO, "Dockerfile"),
].filter((f) => fs.existsSync(f));

// Chapters are rendered first so that the figure and table registries are populated before the
// List of Figures and List of Tables are generated; the front matter is prepended afterwards.
const main = [
  ...render([...A.CH_INTRO, ...H.INTRO_EXTRA]),
  ...render([...A.CH_PROBLEM, ...G.PROBLEM_EXTRA]),
  ...render([...A.CH_LITERATURE, ...H.LIT_EXTRA]),
  ...render([...A.CH_REQUIREMENTS, ...G.REQ_EXTRA]),
  ...render([...A.CH_SCOPE, ...I.SCOPE_EXTRA]),
  ...render([...B.CH_DESIGN, ...G.DESIGN_EXTRA]),
  ...render([...B.CH_ALGORITHMS, ...G.ALGO_EXTRA]),
  ...render([...B.CH_IMPLEMENTATION, ...H.IMPL_EXTRA]),
  ...render([...B.CH_UI, ...G.UI_EXTRA]),
  ...render([...B.CH_BACKEND, ...H.BACKEND_EXTRA]),
  ...render([...B.CH_SECURITY, ...G.SEC_EXTRA]),
  ...render([...C.CH_TESTING, ...G.TEST_EXTRA]),
  ...render(C.CH_CHALLENGES),
  ...render([...C.CH_RESULTS, ...H.RESULTS_EXTRA]),
  ...render(C.CH_LIMITATIONS),
  ...render([...C.CH_FUTURE, ...G.FUTURE_EXTRA]),
  ...render([...C.CH_CONCLUSION, ...I.CONCLUSION_EXTRA]),
  ...render(C.CH_REFERENCES),

  ...render(D.APP_SCHEMA),
  ...render(D.APP_API),
  ...render(D.APP_TESTS),
  ...render(D.APP_TRIALS),
  ...render(D.APP_MANUAL),
  ...render(D.APP_GLOSSARY),
  ...render(E.APP_TRACE),
  ...render(E.APP_DECISIONS),
  ...render(E.APP_WALKTHROUGH),
  ...render(E.APP_SCREENS),
  ...render(F.APP_DDL),
  ...render(F.APP_PROMPTS),
  ...render(F.APP_LOG),
  ...render(F.APP_WALKED),

  ...render(I.APP_GALLERY),

  heading("Appendix T — Complete Test Register", 1),
  para("Every automated test in the suite, listed by its declared name, extracted from the " +
    "repository at the time this report was generated. The names are the tests' own: a test named " +
    "personMovingThroughFrameIsNeverAccepted states its intent, and reading this appendix straight " +
    "through is a reasonable way to understand what the system believes about itself."),
  para(TESTS.total + " tests across " + TESTS.files + " files. Their source is reproduced in " +
    "Appendix C, and the suites are discussed by concern in Appendix G and in Chapter 12."),
  ...TESTS.blocks,

  heading("Appendix U — Source File Inventory", 1),
  para("Every Kotlin file in the application, grouped by package, with its length. " +
    INVENTORY.count + " files, " + INVENTORY.totalLines + " lines. The complete source of each is " +
    "reproduced in Appendix A; this inventory exists so that a reader can locate a file by " +
    "responsibility without paging through the listing."),
  ...INVENTORY.blocks,

  heading("Appendix V — Android Application Source", 1),
  para("The complete Kotlin source of the Android application, reproduced from the repository at " +
    "the time this report was generated. Files are listed alphabetically by path. Line numbers are " +
    "included so that the discussion in Chapters 7 and 13 can be followed against the code."),
  ...sourceListing("V.1 Application Source Files", androidMain, path.join(REPO, "VoxBox/app/src/main/java/me/thimmaiah")),

  heading("Appendix W — Backend Service Source", 1),
  para("The proxy service and its contract test suite. The service is dependency-free Node.js: " +
    "there is no web framework, and every route, validator and schema is visible in the single " +
    "file below."),
  ...sourceListing("W.1 Server and Tests", serverFiles, path.join(REPO, "server")),

  heading("Appendix X — Test Suites", 1),
  para("Unit and instrumentation tests. Every algorithm described in Chapter 7 has a corresponding " +
    "suite here, and several tests encode a specific field failure so that it cannot recur."),
  ...sourceListing("X.1 Android Test Source", androidTest, path.join(REPO, "VoxBox/app/src")),

  heading("Appendix Y — Build and Deployment Configuration", 1),
  para("Build scripts, the dependency catalogue, the Android manifest and the deployment blueprint."),
  ...sourceListing("Y.1 Configuration Files", buildFiles, REPO),
];

// ---------------------------------------------------------------- lists of figures and tables

function indexList(entries, label) {
  if (!entries.length) return [];
  return entries.map((e) => new Paragraph({
    spacing: { after: 70, line: 260 },
    tabStops: [{ type: "right", position: 9200, leader: "dot" }],
    children: [
      new TextRun({ text: label + " " + e.num, font: SANS, size: 19, bold: true, color: ACCENT }),
      new TextRun({ text: "\t" + e.caption, font: SANS, size: 19, color: INK }),
    ],
  }));
}

function listOfFigures() {
  return [
    heading("List of Figures", 1),
    para("Twenty-one diagrams and " + FIGURES.filter((f) => /^\d+\.\d+[a-z]$/.test(String(f.num))).length +
      " annotated screenshots taken from the application running on a physical device. Every " +
      "diagram in this report was generated from the system it describes rather than drawn by " +
      "hand, so a change in the schema or the architecture is reflected the next time the report " +
      "is built.",
      { size: 19, italics: true, color: MUTED }),
    ...indexList(FIGURES, "Figure"),
    heading("List of Tables", 1),
    ...indexList(TABLES, "Table"),
  ];
}

const body = [
  ...titlePage(),
  ...signaturePages(),
  ...abstractPage(),
  ...tocPage(),
  ...listOfFigures(),
  ...main,
];

const doc = new Document({
  creator: "Tejas",
  title: "Structured Voice to Notes using AI — VoxBox",
  description: "Project report",
  numbering: {
    config: [
      {
        reference: "vb-bullets",
        levels: [{
          level: 0, format: LevelFormat.BULLET, text: "\u2022", alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: 520, hanging: 260 } } },
        }],
      },
      {
        reference: "vb-numbers",
        levels: [{
          level: 0, format: LevelFormat.DECIMAL, text: "%1.", alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: 520, hanging: 260 } } },
        }],
      },
    ],
  },
  styles: {
    default: {
      document: { run: { font: SANS, size: 21, color: INK } },
    },
  },
  sections: [{
    properties: {
      page: {
        size: { width: 11906, height: 16838 },
        margin: { top: 1200, right: 1100, bottom: 1200, left: 1400 },
      },
    },
    headers: {
      default: new Header({
        children: [new Paragraph({
          alignment: AlignmentType.RIGHT,
          spacing: { after: 60 },
          border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: RULE, space: 6 } },
          children: [new TextRun({
            text: "Structured Voice to Notes using AI — VoxBox",
            font: SANS, size: 16, color: MUTED,
          })],
        })],
      }),
    },
    footers: {
      default: new Footer({
        children: [new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [new TextRun({ children: ["Page ", PageNumber.CURRENT], font: SANS, size: 16, color: MUTED })],
        })],
      }),
    },
    children: body,
  }],
});

Packer.toBuffer(doc).then((buffer) => {
  fs.mkdirSync(path.dirname(OUT), { recursive: true });
  fs.writeFileSync(OUT, buffer);
  const kb = (buffer.length / 1024).toFixed(0);
  console.log(`written: ${OUT} (${kb} KB)`);
  console.log(`paragraphs: ${body.length}`);
  console.log(`android source files: ${androidMain.length}`);
  console.log(`test files: ${androidTest.length}`);
});
