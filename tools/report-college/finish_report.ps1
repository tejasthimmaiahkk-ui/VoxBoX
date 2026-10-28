# Assembles the final report in Word:
#   1. the college's starting pages (cover, title page, certificate) with the project title filled in,
#   2. the generated body (declaration onward),
# then updates the contents, figure and table lists and saves .docx and .pdf.
param(
    [string]$StartingPages = "$PSScriptRoot\starting_pages.docx",
    [string]$Body = "$PSScriptRoot\build\body.docx",
    [string]$Out = "$PSScriptRoot\..\..\VoxBox_Project_Report.docx"
)
$ErrorActionPreference = "Stop"
$Out = [System.IO.Path]::GetFullPath($Out)
$Pdf = [System.IO.Path]::ChangeExtension($Out, ".pdf")
$title = "VOXBOX: AN INTELLIGENT MULTIMODAL SYSTEM FOR CONTINUOUS LECTURE NOTE GENERATION USING VOICE AND VISUAL CONTENT"

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
try {
    $doc = $word.Documents.Open($StartingPages, $false, $true)
    function Replace-All($find, $replace) {
        $range = $doc.Content
        $f = $range.Find
        $f.ClearFormatting(); $f.Replacement.ClearFormatting()
        [void]$f.Execute($find, $true, $false, $false, $false, $false, $true, 1, $false, $replace, 2)
    }
    Replace-All ([string][char]0x201C + "PROJECT TITLE" + [char]0x201D) $title
    Replace-All "PROJECT TITLE" $title
    Replace-All ([string][char]0x201C + "Project Title" + [char]0x201D) ([string][char]0x201C + $title + [char]0x201D)
    Replace-All "Mr./Ms. Student Name" "[Student Name]"
    Replace-All "Your Reg No" "[Registration Number]"
    Replace-All "[Student Name]" "[Student Name]"

    # The full title is three lines longer than the template's placeholder, which pushed the
    # certificate's last examiner line onto a page of its own. Shrink only the empty spacer lines
    # that follow the certificate heading.
    $inCertificate = $false
    foreach ($p in $doc.Paragraphs) {
        $text = $p.Range.Text.Trim()
        if ($text -eq "CERTIFICATE") { $inCertificate = $true; continue }
        if ($inCertificate -and $text -eq "") { $p.Range.Font.Size = 6; $p.SpaceAfter = 0; $p.SpaceBefore = 0 }
    }

    # Body goes after a section break so it can carry its own margins and page numbers.
    $end = $doc.Content
    $end.Collapse(0)
    $end.InsertBreak(2)   # wdSectionBreakNextPage
    $end = $doc.Content
    $end.Collapse(0)
    $end.InsertFile($Body)

    # Starting pages: no page numbers. Everything after: the body's own footers (roman, then arabic).
    $first = $doc.Sections.Item(1)
    for ($i = 2; $i -le $doc.Sections.Count; $i++) {
        $s = $doc.Sections.Item($i)
        $s.Footers.Item(1).LinkToPrevious = $false
        $s.PageSetup.LeftMargin = 42.52    # 1.5 cm
        $s.PageSetup.RightMargin = 28.35   # 1 cm
        $s.PageSetup.TopMargin = 28.35
        $s.PageSetup.BottomMargin = 28.35
        $s.PageSetup.PaperSize = 7         # A4
    }
    $first.Footers.Item(1).Range.Text = ""

    $s2 = $doc.Sections.Item(2).Footers.Item(1).PageNumbers
    $s2.NumberStyle = 2; $s2.RestartNumberingAtSection = $true; $s2.StartingNumber = 1   # lower roman
    $s3 = $doc.Sections.Item(3).Footers.Item(1).PageNumbers
    $s3.NumberStyle = 0; $s3.RestartNumberingAtSection = $true; $s3.StartingNumber = 1   # arabic

    [void]$doc.Fields.Update()
    foreach ($toc in $doc.TablesOfContents) { [void]$toc.Update() }
    [void]$doc.Fields.Update()
    foreach ($toc in $doc.TablesOfContents) { [void]$toc.Update() }

    $doc.SaveAs2($Out, 16)
    $doc.ExportAsFixedFormat($Pdf, 17)
    $pages = $doc.ComputeStatistics(2)
    "saved $Out"
    "pages $pages"
    $doc.Close($false)
}
finally {
    $word.Quit()
}

