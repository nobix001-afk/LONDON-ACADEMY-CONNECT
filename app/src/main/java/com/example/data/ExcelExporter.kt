package com.example.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Pure-Kotlin OOXML (.xlsx) Excel workbook generator and secure FileProvider share helper.
 * Produces standard .xlsx files compatible with Microsoft Excel, Google Sheets, and WPS Office
 * with zero third-party POI bloat and zero external storage permissions.
 */
object ExcelExporter {

    const val EXCEL_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    data class ExcelSheet(
        val name: String,
        val headers: List<String>,
        val rows: List<List<Any?>>,
        val titleBanner: String? = null
    )

    fun writeWorkbookToFile(
        context: Context,
        fileNamePrefix: String,
        sheets: List<ExcelSheet>
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        // Clean old temporary export files (> 24 hours) so cache stays tidy
        val cutoff = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
        exportDir.listFiles()?.forEach { old ->
            if (old.isFile && old.lastModified() < cutoff) {
                old.delete()
            }
        }

        val safeName = fileNamePrefix
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_')
            .ifBlank { "LondonAcademy_Export" }
            .removeSuffix(".xlsx")

        val outFile = File(exportDir, "$safeName.xlsx")
        FileOutputStream(outFile).use { fos ->
            writeWorkbook(fos, sheets)
        }
        return outFile
    }

    fun writeWorkbookToBytes(sheets: List<ExcelSheet>): ByteArray {
        val baos = ByteArrayOutputStream()
        writeWorkbook(baos, sheets)
        return baos.toByteArray()
    }

    fun writeWorkbook(outputStream: OutputStream, sheets: List<ExcelSheet>) {
        val normalizedSheets = sanitizeSheets(sheets.ifEmpty {
            listOf(ExcelSheet(name = "Report", headers = listOf("Status"), rows = listOf(listOf("No records"))))
        })

        ZipOutputStream(outputStream, StandardCharsets.UTF_8).use { zip ->
            writeZipEntry(zip, "[Content_Types].xml", buildContentTypesXml(normalizedSheets.size))
            writeZipEntry(zip, "_rels/.rels", buildRootRelsXml())
            writeZipEntry(zip, "xl/workbook.xml", buildWorkbookXml(normalizedSheets))
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml(normalizedSheets.size))
            writeZipEntry(zip, "xl/styles.xml", buildStylesXml())

            normalizedSheets.forEachIndexed { index, sheet ->
                writeZipEntry(
                    zip,
                    "xl/worksheets/sheet${index + 1}.xml",
                    buildWorksheetXml(sheet)
                )
            }
        }
    }

    fun shareExcelFile(
        context: Context,
        file: File,
        chooserTitle: String
    ) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = EXCEL_MIME_TYPE
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension.replace('_', ' '))
                clipData = ClipData.newRawUri(file.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to open share sheet for ${file.name}: ${e.localizedMessage ?: "Unknown error"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }

    private fun sanitizeSheets(sheets: List<ExcelSheet>): List<ExcelSheet> {
        val usedNames = mutableSetOf<String>()
        return sheets.mapIndexed { idx, sheet ->
            val cleaned = sheet.name
                .replace(Regex("[\\[\\]:*?/\\\\]"), " ")
                .trim()
                .ifBlank { "Sheet${idx + 1}" }
                .take(31)
            var candidate = cleaned
            var suffix = 2
            while (!usedNames.add(candidate.lowercase())) {
                val tag = " $suffix"
                candidate = cleaned.take(31 - tag.length) + tag
                suffix++
            }
            sheet.copy(name = candidate)
        }
    }

    private fun buildContentTypesXml(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        for (i in 1..sheetCount) {
            append("""<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        append("</Types>")
    }

    private fun buildRootRelsXml(): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        append("""<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""")
        append("</Relationships>")
    }

    private fun buildWorkbookXml(sheets: List<ExcelSheet>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        append("<sheets>")
        sheets.forEachIndexed { idx, sheet ->
            append("""<sheet name="${escapeXml(sheet.name)}" sheetId="${idx + 1}" r:id="rId${idx + 1}"/>""")
        }
        append("</sheets>")
        append("</workbook>")
    }

    private fun buildWorkbookRelsXml(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        for (i in 1..sheetCount) {
            append("""<Relationship Id="rId$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$i.xml"/>""")
        }
        append("""<Relationship Id="rId${sheetCount + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
        append("</Relationships>")
    }

    /**
     * Style indices:
     * 0 = Normal data text (11pt dark slate, thin border, left/center aligned)
     * 1 = Column header (11pt bold white, Navy900 fill, thin border, center aligned)
     * 2 = Title banner (13pt bold Navy900, soft indigo fill, no border)
     * 3 = Numeric value (11pt dark slate, thin border, right aligned)
     */
    private fun buildStylesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <fonts count="3">
            <font><sz val="11"/><color rgb="FF1E293B"/><name val="Calibri"/></font>
            <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
            <font><b/><sz val="13"/><color rgb="FF0F172A"/><name val="Calibri"/></font>
          </fonts>
          <fills count="4">
            <fill><patternFill patternType="none"/></fill>
            <fill><patternFill patternType="gray125"/></fill>
            <fill><patternFill patternType="solid"><fgColor rgb="FF1E3A8A"/></patternFill></fill>
            <fill><patternFill patternType="solid"><fgColor rgb="FFE0E7FF"/></patternFill></fill>
          </fills>
          <borders count="2">
            <border><left/><right/><top/><bottom/><diagonal/></border>
            <border>
              <left style="thin"><color rgb="FFCBD5E1"/></left>
              <right style="thin"><color rgb="FFCBD5E1"/></right>
              <top style="thin"><color rgb="FFCBD5E1"/></top>
              <bottom style="thin"><color rgb="FFCBD5E1"/></bottom>
              <diagonal/>
            </border>
          </borders>
          <cellStyleXfs count="1">
            <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
          </cellStyleXfs>
          <cellXfs count="4">
            <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>
            <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
            <xf numFmtId="0" fontId="2" fillId="3" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1"><alignment horizontal="left" vertical="center"/></xf>
            <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
          </cellXfs>
          <cellStyles count="1">
            <cellStyle name="Normal" xfId="0" builtinId="0"/>
          </cellStyles>
        </styleSheet>
    """.trimIndent()

    private fun buildWorksheetXml(sheet: ExcelSheet): String = buildString {
        val colCount = maxOf(
            sheet.headers.size,
            sheet.rows.maxOfOrNull { it.size } ?: 0,
            1
        )
        val colWidths = IntArray(colCount) { 14 }
        sheet.headers.forEachIndexed { colIdx, header ->
            colWidths[colIdx] = max(colWidths[colIdx], min(40, header.length + 5))
        }
        sheet.rows.forEach { row ->
            row.forEachIndexed { colIdx, cell ->
                if (colIdx < colCount && cell != null) {
                    val len = cell.toString().length + 4
                    colWidths[colIdx] = max(colWidths[colIdx], min(42, len))
                }
            }
        }

        val headerRowNum = if (sheet.titleBanner.isNullOrBlank()) 1 else 2

        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        if (sheet.headers.isNotEmpty()) {
            append("""<sheetViews><sheetView workbookViewId="0"><pane ySplit="$headerRowNum" topLeftCell="A${headerRowNum + 1}" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>""")
        }
        append("<cols>")
        for (c in 0 until colCount) {
            append("""<col min="${c + 1}" max="${c + 1}" width="${colWidths[c]}" customWidth="1"/>""")
        }
        append("</cols>")
        append("<sheetData>")

        var rowIdx = 1
        if (!sheet.titleBanner.isNullOrBlank()) {
            append("""<row r="$rowIdx" ht="24" customHeight="1">""")
            appendCellXml(rowIdx, 0, sheet.titleBanner, styleIndex = 2)
            append("</row>")
            rowIdx++
        }

        if (sheet.headers.isNotEmpty()) {
            append("""<row r="$rowIdx" ht="22" customHeight="1">""")
            sheet.headers.forEachIndexed { colIdx, header ->
                appendCellXml(rowIdx, colIdx, header, styleIndex = 1)
            }
            append("</row>")
            rowIdx++
        }

        sheet.rows.forEach { rowValues ->
            append("""<row r="$rowIdx">""")
            for (colIdx in 0 until colCount) {
                val value = rowValues.getOrNull(colIdx)
                val isNum = isNumericValue(value)
                appendCellXml(rowIdx, colIdx, value, styleIndex = if (isNum) 3 else 0)
            }
            append("</row>")
            rowIdx++
        }

        append("</sheetData>")
        append("</worksheet>")
    }

    private fun isNumericValue(value: Any?): Boolean = when (value) {
        is Int, is Long -> true
        is Double -> value.isFinite()
        is Float -> value.isFinite()
        else -> false
    }

    private fun StringBuilder.appendCellXml(
        rowNumber: Int,
        colIndex: Int,
        value: Any?,
        styleIndex: Int
    ) {
        val ref = "${columnLetters(colIndex)}$rowNumber"
        when (value) {
            null -> append("""<c r="$ref" s="$styleIndex"/>""")
            is Int, is Long -> append("""<c r="$ref" s="$styleIndex" t="n"><v>$value</v></c>""")
            is Double -> {
                if (value.isFinite()) {
                    val formatted = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
                    append("""<c r="$ref" s="$styleIndex" t="n"><v>$formatted</v></c>""")
                } else {
                    append("""<c r="$ref" s="$styleIndex" t="inlineStr"><is><t>${escapeXml(value.toString())}</t></is></c>""")
                }
            }
            is Float -> {
                if (value.isFinite()) {
                    val formatted = if (value % 1.0f == 0.0f) value.toLong().toString() else value.toString()
                    append("""<c r="$ref" s="$styleIndex" t="n"><v>$formatted</v></c>""")
                } else {
                    append("""<c r="$ref" s="$styleIndex" t="inlineStr"><is><t>${escapeXml(value.toString())}</t></is></c>""")
                }
            }
            else -> {
                val text = escapeXml(value.toString())
                append("""<c r="$ref" s="$styleIndex" t="inlineStr"><is><t xml:space="preserve">$text</t></is></c>""")
            }
        }
    }

    private fun columnLetters(index: Int): String {
        var n = index
        val sb = StringBuilder()
        while (n >= 0) {
            sb.append(('A' + (n % 26)))
            n = (n / 26) - 1
        }
        return sb.reverse().toString()
    }

    private fun escapeXml(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> {
                    val code = ch.code
                    // Filter out XML 1.0 illegal control characters while preserving tab, LF, CR
                    if (code == 0x9 || code == 0xA || code == 0xD || code >= 0x20) {
                        sb.append(ch)
                    }
                }
            }
        }
        return sb.toString()
    }
}
