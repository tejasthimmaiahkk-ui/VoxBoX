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
  AlignmentType, BorderStyle, Document, Footer, Header, HeadingLevel, LevelFormat, NumberFormat,
  Packer, PageBreak, PageNumber, Paragraph, ShadingType, Table, TableCell, TableRow,
  TableOfContents, TextRun, WidthType,
} = require("docx");

const A = require("./content");
const B = require("./content2");
const C = require("./content3");

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

function render(blocks) {
  const out = [];
  blocks.forEach((b) => {
    if (b.h1) out.push(heading(b.h1, 1));
    else if (b.h2) out.push(heading(b.h2, 2));
    else if (b.h3) out.push(heading(b.h3, 3));
    else if (b.p) out.push(para(b.p));
    else if (b.b) out.push(...bullets(b.b));
    else if (b.n) out.push(...numbered(b.n));
    else if (b.table) out.push(...table(b.table));
    else if (b.code) out.push(...codeBlock(b.code, b.caption));
    else if (b.note) out.push(note(b.note));
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

// ---------------------------------------------------------------- document

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

const body = [
  ...titlePage(),
  ...signaturePages(),
  ...abstractPage(),
  ...tocPage(),
  ...render(A.CH_INTRO),
  ...render(A.CH_PROBLEM),
  ...render(A.CH_LITERATURE),
  ...render(A.CH_REQUIREMENTS),
  ...render(A.CH_SCOPE),
  ...render(B.CH_DESIGN),
  ...render(B.CH_ALGORITHMS),
  ...render(B.CH_IMPLEMENTATION),
  ...render(B.CH_UI),
  ...render(B.CH_BACKEND),
  ...render(B.CH_SECURITY),
  ...render(C.CH_TESTING),
  ...render(C.CH_CHALLENGES),
  ...render(C.CH_RESULTS),
  ...render(C.CH_LIMITATIONS),
  ...render(C.CH_FUTURE),
  ...render(C.CH_CONCLUSION),
  ...render(C.CH_REFERENCES),

  heading("Appendix A — Android Application Source", 1),
  para("The complete Kotlin source of the Android application, reproduced from the repository at " +
    "the time this report was generated. Files are listed alphabetically by path. Line numbers are " +
    "included so that the discussion in Chapters 7 and 13 can be followed against the code."),
  ...sourceListing("A.1 Application Source Files", androidMain, path.join(REPO, "VoxBox/app/src/main/java/me/thimmaiah")),

  heading("Appendix B — Backend Service Source", 1),
  para("The proxy service and its contract test suite. The service is dependency-free Node.js: " +
    "there is no web framework, and every route, validator and schema is visible in the single " +
    "file below."),
  ...sourceListing("B.1 Server and Tests", serverFiles, path.join(REPO, "server")),

  heading("Appendix C — Test Suites", 1),
  para("Unit and instrumentation tests. Every algorithm described in Chapter 7 has a corresponding " +
    "suite here, and several tests encode a specific field failure so that it cannot recur."),
  ...sourceListing("C.1 Android Test Source", androidTest, path.join(REPO, "VoxBox/app/src")),

  heading("Appendix D — Build and Deployment Configuration", 1),
  para("Build scripts, the dependency catalogue, the Android manifest and the deployment blueprint."),
  ...sourceListing("D.1 Configuration Files", buildFiles, REPO),
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
