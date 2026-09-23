Add-Type -AssemblyName System.IO.Compression.FileSystem
$docxFile = Resolve-Path "RR_Jaggery_Traders_Product_Requirements_and_Agile_Sprint_Plan_Updated_OCI_Runnable_Checkpoints.docx"
$zip = [System.IO.Compression.ZipFile]::OpenRead($docxFile)
$entry = $zip.GetEntry("word/document.xml")
$stream = $entry.Open()
$reader = New-Object System.IO.StreamReader($stream)
$xml = $reader.ReadToEnd()
$reader.Close()
$stream.Close()
$zip.Dispose()

[xml]$doc = $xml
$ns = New-Object System.Xml.XmlNamespaceManager($doc.NameTable)
$ns.AddNamespace("w", "http://schemas.openxmlformats.org/wordprocessingml/2006/main")

# Iterate over body elements (paragraphs, tables) to preserve structure
$output = @()

$bodyNodes = $doc.SelectNodes("//w:body/*", $ns)
foreach ($node in $bodyNodes) {
    if ($node.LocalName -eq "p") {
        $texts = $node.SelectNodes(".//w:t", $ns)
        if ($texts -and $texts.Count -gt 0) {
            $pText = ($texts | ForEach-Object { $_.InnerText }) -join ""
            if ($pText.Trim()) {
                $output += $pText
            }
        }
    } elseif ($node.LocalName -eq "tbl") {
        $output += "`n--- TABLE START ---"
        $rows = $node.SelectNodes(".//w:tr", $ns)
        foreach ($row in $rows) {
            $cells = $row.SelectNodes(".//w:tc", $ns)
            $cellTexts = @()
            foreach ($cell in $cells) {
                $cTexts = $cell.SelectNodes(".//w:t", $ns)
                $cText = ($cTexts | ForEach-Object { $_.InnerText }) -join " "
                $cellTexts += $cText.Trim()
            }
            $output += ("| " + ($cellTexts -join " | ") + " |")
        }
        $output += "--- TABLE END ---`n"
    }
}

$output | Out-File -FilePath "extracted_doc.md" -Encoding utf8
Write-Output "Successfully extracted $($output.Count) elements to extracted_doc.md"
