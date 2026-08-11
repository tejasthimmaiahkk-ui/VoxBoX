# Report generator

Rebuilds `outputs/VoxBox_Project_Report.docx`.

The narrative lives in `content.js`, `content2.js` and `content3.js`; `build.js` is layout only.
The source-code appendices are read from the repository at build time, so re-running this after a
code change produces a report whose listings match the tree.

```bash
npm install docx
node --max-old-space-size=4096 build.js
```

Then open the document in Word once and choose **Update Field** on the table of contents, or run:

```powershell
$w = New-Object -ComObject Word.Application; $w.Visible = $false
$d = $w.Documents.Open("D:\CollegeProject\outputs\VoxBox_Project_Report.docx", $false, $false)
$d.TablesOfContents.Item(1).Update(); $d.Repaginate(); $d.Save(); $d.Close($false); $w.Quit()
```

Editing the report: change the content files and regenerate. Editing the `.docx` by hand works too,
but is lost on the next rebuild.
