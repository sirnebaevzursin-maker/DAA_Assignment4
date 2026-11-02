"""Build the four-page PDF and matching Markdown report from the submitted CSV."""
import csv
from collections import defaultdict
from html import escape
from pathlib import Path
from statistics import median

ROOT = Path(__file__).resolve().parents[1]
groups = defaultdict(list)
with (ROOT / "results/results.csv").open(newline="", encoding="utf-8") as stream:
    for row in csv.DictReader(stream):
        groups[(row["algorithm"], row["input"], int(row["n"]))].append(row)


def row(algorithm, input_type, n):
    return groups[(algorithm, input_type, n)][0]


def time(algorithm, input_type, n):
    return median(float(r["time_ms"]) for r in groups[(algorithm, input_type, n)])


def count(algorithm, input_type, n, column):
    return int(row(algorithm, input_type, n)[column])


recursive_calls = count("recursive", "disjoint", 12, "recursive_calls")
table_bytes = 4 * count("table", "disjoint", 1024, "allocated_int_slots")
rows_bytes = 4 * count("two_rows", "disjoint", 1024, "allocated_int_slots")
naive_comparisons = count("naive", "late_mismatch", 65536, "total_comparisons")
kmp_comparisons = count("kmp", "late_mismatch", 65536, "total_comparisons")
card = (ROOT / "EXPERIMENT_DESIGN.md").read_text(encoding="utf-8")

ed_prediction = (
    f"Prediction vs result (Edit Distance). The predicted repeated-work explosion occurred: at n=12, "
    f"disjoint recursion entered {recursive_calls:,} calls, versus 169 evaluated cells in either DP version. "
    f"Random recursion entered {count('recursive', 'random', 12, 'recursive_calls'):,} calls because equal "
    "characters sometimes take only the diagonal branch. DP evaluates the same (n+1)^2 cells for both "
    "input types; on the large sizes, doubling n approaches four times as many cells. At n=1024, measured "
    f"integer payload was {table_bytes:,} bytes for the table and {rows_bytes:,} bytes for two rows, "
    f"a {table_bytes / rows_bytes:.1f}x reduction, matching the quadratic/linear storage prediction."
)
ed_scope = (
    "Measurement scope. A recursive call and a DP cell are both state evaluations, but they do not "
    "have identical instruction costs. Integer bytes are measured allocated slots multiplied by four; "
    "array headers, row references, input strings, recursion stack and JVM heap overhead are excluded. "
    "No edit-plan changes were made after the pre-run card."
)
search_prediction = (
    "Prediction vs result (string search). With m=n/64, each doubling of n made late-mismatch Naive "
    "work almost four times larger, while KMP work including LPS almost doubled. At n=65,536 and "
    f"m=1,024, the measured counts were {naive_comparisons:,} versus {kmp_comparisons:,}, a "
    f"{naive_comparisons / kmp_comparisons:.1f}x gap. Random and early-mismatch counts grew roughly "
    "linearly as predicted. Early-mismatch Naive made fewer comparisons than KMP including LPS, "
    "yet its largest-case median was slower; comparison counts omit branch behavior and execution costs, "
    "so this timing difference is an observation, not proof of a particular hardware cause."
)
time_paragraph = (
    "Measured time. Times are normalized milliseconds per invocation, with three untimed warm-ups "
    "and five independent timed batches of at least 20 ms per case; figures use medians and min-max "
    "ranges. KMP includes LPS construction, and all algorithms include counters, allocation, result "
    "consumption and small harness checks. At n=1,024 on random ED, table/two-row medians were "
    f"{time('table', 'random', 1024):.3f}/{time('two_rows', 'random', 1024):.3f} ms; on the largest "
    f"late-mismatch search they were {time('naive', 'late_mismatch', 65536):.3f} ms for Naive and "
    f"{time('kmp', 'late_mismatch', 65536):.3f} ms for KMP. JIT compilation, caching, garbage collection "
    "and desktop background work can change these values, especially tiny cases; counters provide "
    "the stronger evidence about growth. These are instrumented educational measurements, not a JMH benchmark."
)
answers = [
    ("Which input made string search slowest?",
     "The largest late_mismatch input made Naive slowest among the submitted search cases. "
     "The text is all a, while the pattern contains a long run of a followed by b, so each legal alignment "
     f"fails only after m comparisons and produces no match. Naive performed {naive_comparisons:,} "
     "comparisons, exactly matching (n-m+1)*m when checked after measurement. KMP retains prefix "
     "information and never moves its text index backwards, avoiding those repeated prefix scans."),
    ("How large an n could recursive Edit Distance handle?",
     f"The largest tested recursive size was n=12, with a disjoint median of {time('recursive', 'disjoint', 12):.2f} ms "
     f"and {recursive_calls:,} calls per invocation. The measured call count grew by "
     f"{recursive_calls / count('recursive', 'disjoint', 10, 'recursive_calls'):.1f}x from n=10 to n=12. "
     "I stopped at the predeclared cap because repeated subproblems rapidly exhaust the experiment's "
     "time budget, not because a stack overflow or timeout was observed. This is a practical tested "
     "boundary for the chosen worst case, not a universal maximum; equal strings follow only one branch."),
    ("What do two rows gain and lose?",
     "Two rows preserve the exact distance and O(n*m) time while reducing extra integer storage "
     f"from O(n*m) to O(m), saving {table_bytes / rows_bytes:.1f}x payload at n=m=1,024 here. "
     "They overwrite previous rows, so a simple traceback cannot directly reconstruct the edit sequence. "
     "A full table is easier to inspect and supports ordinary traceback, while reconstruction with "
     "reduced storage requires extra computation or a different reconstruction technique. Two rows "
     "were faster at the largest random size in this run, but that is not a universal speed guarantee."),
    ("Which search would you choose for the city platform?",
     "I would choose KMP for exact pattern search in long operator logs because its O(n+m) work "
     "remains bounded on repetitive or adversarial input. Its LPS array costs O(m) extra storage, "
     "and retaining the longest border after a match preserves overlapping results. Naive remains a "
     "reasonable small-input baseline, but its worst case scales as O(n*m). For residents' misspelled "
     "street names, I would use the two-row Edit Distance routine to rank candidates; exact KMP "
     "matching by itself does not correct spelling.")
]
captions = [
    "Figure 1. Disjoint recursion revisits hundreds of millions of states while both DP variants evaluate only 169 cells at n=12.",
    f"Figure 2. At n=1,024, two rows use {rows_bytes:,} bytes of integer payload versus {table_bytes:,} bytes for the full table.",
    "Figure 3. Growing the pattern with n makes late-mismatch Naive work quadratic, while KMP including LPS remains linear.",
    "Figure 4. Median timings broadly follow the work curves, but visible ranges and execution overhead limit precise speed claims."
]
validation = (
    "Validation and reproducibility. All 244 JUnit 5 tests passed with no failures, errors or skipped "
    "tests: reference values, empty/long patterns, overlaps, 100 random ED pairs, 100 random search "
    "texts, brute-force LPS checks and counter resets. The primary CSV has 430 timed samples across "
    "86 cases; summary.csv contains all 86 medians. A second complete execution is retained in "
    "reproducibility.csv, and its outputs, input SHA-256 values and per-call counters agree in all "
    "430 rows; timing and batch lengths are deliberately excluded from equality checks."
)
environment = (
    "Environment: Windows 11 amd64, Oracle Java HotSpot 25.0.1, 8 logical processors, "
    "-Xms256m -Xmx1g; compiled for Java 17. CPU model was not available through the sandbox query "
    "and is not inferred. Seed: 20261103; all times were collected serially on the same machine."
)


def markdown():
    # Keep the original pre-run card verbatim at the very beginning.
    text = card + "\n\n# Assignment 4 - Results and conclusions\n\nZhursin Sirnebayev | SE-2509\n\n"
    text += "The companion REPORT.pdf is the fixed-layout, four-page submission version.\n\n"
    text += "## Edit Distance\n\n"
    for index, file in ((0, "01_edit_work.png"), (1, "02_edit_memory.png")):
        text += f"![{captions[index]}](results/plots/{file})\n\n{captions[index]}\n\n"
    text += ed_prediction + "\n\n" + ed_scope + "\n\n## String search and timing\n\n"
    for index, file in ((2, "03_search_work.png"), (3, "04_median_time.png")):
        text += f"![{captions[index]}](results/plots/{file})\n\n{captions[index]}\n\n"
    text += search_prediction + "\n\n" + time_paragraph + "\n\n## Short answers\n\n"
    for question, answer in answers:
        text += f"### {question}\n\n{answer}\n\n"
    text += validation + "\n\n" + environment + "\n"
    (ROOT / "REPORT.md").write_text(text, encoding="utf-8")


def pdf():
    from reportlab.pdfgen import canvas
    from reportlab.lib.pagesizes import A4
    from reportlab.lib import colors
    from reportlab.lib.styles import ParagraphStyle
    from reportlab.platypus import Paragraph, Table, TableStyle
    from reportlab.lib.utils import ImageReader
    page_w, page_h = A4
    margin, content_w = 42, page_w - 84
    doc = canvas.Canvas(str(ROOT / "REPORT.pdf"), pagesize=A4)
    doc.setTitle("Assignment 4 - Dynamic Programming and String Algorithms")
    doc.setAuthor("Zhursin Sirnebayev, SE-2509")
    body = ParagraphStyle("Body", fontName="Helvetica", fontSize=9.2, leading=12.2)
    caption = ParagraphStyle("Caption", parent=body, fontSize=8.5, leading=10.6, textColor=colors.HexColor("#475569"))
    small = ParagraphStyle("Small", parent=body, fontSize=8.25, leading=10.1)
    card_style = ParagraphStyle("Card", parent=body, fontSize=8.55, leading=10.6)
    y = page_h - margin

    def paragraph(text, style=body, gap=8):
        nonlocal y
        item = Paragraph(escape(text), style)
        _, height = item.wrap(content_w, page_h)
        if y - height < 48:
            raise ValueError(f"Report page overflow near: {text[:80]}")
        item.drawOn(doc, margin, y - height)
        y -= height + gap

    def heading(text, size=17):
        nonlocal y
        doc.setFillColor(colors.HexColor("#0f172a"))
        doc.setFont("Helvetica-Bold", size)
        doc.drawString(margin, y - size, text)
        y -= size + 14

    def start(title, page):
        nonlocal y
        y = page_h - margin
        heading(title)
        if page == 1:
            paragraph("Design and Analysis of Algorithms | Assignment 4", small, 3)
            paragraph("Zhursin Sirnebayev | SE-2509 | 3 November 2026", small, 10)

    def finish(page):
        doc.setStrokeColor(colors.HexColor("#cbd5e1"))
        doc.line(margin, 36, page_w - margin, 36)
        doc.setFont("Helvetica", 8)
        doc.setFillColor(colors.HexColor("#64748b"))
        doc.drawString(margin, 23, "Zhursin Sirnebayev | SE-2509 | DAA Assignment 4")
        doc.drawRightString(page_w - margin, 23, f"{page} / 4")
        doc.showPage()

    def figure(filename, text):
        nonlocal y
        image = ImageReader(str(ROOT / "results/plots" / filename))
        width, height = image.getSize()
        height = content_w * height / width
        doc.drawImage(image, margin, y - height, width=content_w, height=height, mask="auto")
        y -= height + 4
        paragraph(text, caption, 10)

    start("Experiment Design Card", 1)
    table_rows = [[Paragraph("<b>Field</b>", card_style), Paragraph("<b>Plan written before the first run</b>", card_style)]]
    for line in card.splitlines():
        if line.startswith("| ") and not line.startswith("| Field") and not line.startswith("| ---"):
            values = [p.strip() for p in line.strip("|").split("|")]
            table_rows.append([Paragraph(escape(value), card_style) for value in values])
    table = Table(table_rows, colWidths=[91, content_w - 91], hAlign="LEFT")
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#e2e8f0")),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#f8fafc")]),
        ("BOX", (0, 0), (-1, -1), .6, colors.HexColor("#cbd5e1")),
        ("INNERGRID", (0, 0), (-1, -1), .35, colors.HexColor("#cbd5e1")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 6), ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5)
    ]))
    _, height = table.wrap(content_w, page_h)
    if y - height < 75: raise ValueError("Design card does not fit page 1")
    table.drawOn(doc, margin, y - height)
    y -= height + 9
    paragraph("Frozen in the first Git commit before execution; no changes to the plan were needed. "
              "The complete original card is preserved in EXPERIMENT_DESIGN.md.", small)
    finish(1)

    start("1. Dynamic programming", 2)
    figure("01_edit_work.png", captions[0])
    figure("02_edit_memory.png", captions[1])
    paragraph(ed_prediction)
    paragraph(ed_scope, small)
    finish(2)

    start("2. String search and measured time", 3)
    figure("03_search_work.png", captions[2])
    figure("04_median_time.png", captions[3])
    paragraph(search_prediction)
    paragraph(time_paragraph)
    finish(3)

    start("3. Short answers and validation", 4)
    for question, answer in answers:
        heading(question, 11)
        paragraph(answer, body, 12)
    paragraph(validation, body, 10)
    paragraph(environment, small)
    finish(4)
    doc.save()


if __name__ == "__main__":
    markdown()
    pdf()
    print("Created REPORT.md and the four-page REPORT.pdf from results/results.csv.")
