/**
 *
 */
package org.theseed.join;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFName;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.io.KeyedFileMap;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * This saves a current copy of the output in an excel spreadsheet as a table.
 *
 * @author Bruce Parrello
 *
 */
public class ExcelSaveSpec extends SaveSpec {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(ExcelSaveSpec.class);
    /** number format precision */
    private int precision;
    /** integer data type pattern */
    protected static final Pattern INTEGER_PATTERN = Pattern.compile("\\s*[\\-+]?\\d+");
    /** double data type pattern */
    protected static final Pattern DOUBLE_PATTERN = KeyedFileMap.DOUBLE_PATTERN;
    /** color to use for header */
    private static final short HEAD_COLOR = IndexedColors.INDIGO.getIndex();
    /** color to use for data */
    private static final XSSFColor BAND_COLOR = new XSSFColor(new byte[] { (byte) 0xDD, (byte) 0xEE, (byte) 0xFF }, null);

    // CONTROLS

    /** sheet name to use */
    @FXML
    private TextField txtSheetName;

    /** number format precision slider */
    @FXML
    private Slider slidePrecision;

    /** label to display number format precision */
    @FXML
    private Label lblPrecision;


    /**
     * This is called when the precision slider changes.  We use it to update
     * the label.
     *
     * @param intValue	new value of slider
     */
    private void updatePrecision(int intValue) {
        this.precision = intValue;
        this.lblPrecision.setText(Integer.toString(intValue));
    }

    @Override
    public boolean isValid() {
        return (this.getOutFile() != null && ! this.txtSheetName.getText().isBlank());
    }

    @Override
    public void apply(KeyedFileMap keyedMap) throws IOException {
        // Get the list of column headers.
        List<String> headers = keyedMap.getHeaders();
        // Get the number of records.
        int rows = keyedMap.size();
        // Now we build the workbook.
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Create a new sheet to hold our output.
            String sheetName = this.txtSheetName.getText();
            XSSFSheet newSheet = (XSSFSheet) workbook.createSheet(sheetName);
            // Create the special formatting styles.
            DataFormat format = workbook.createDataFormat();
            short intFmt = format.getFormat("##0");
            short dblFmt = format.getFormat("##0." + StringUtils.repeat("0", this.precision));
            XSSFCellStyle headStyle = workbook.createCellStyle();
            headStyle.setFillForegroundColor(HEAD_COLOR);
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headStyle.setBorderBottom(BorderStyle.MEDIUM);
            Font font = workbook.createFont();
            font.setColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
            headStyle.setFont(font);
            XSSFCellStyle numStyle = workbook.createCellStyle();
            numStyle.setDataFormat(dblFmt);
            numStyle.setAlignment(HorizontalAlignment.RIGHT);
            this.shadeCell(numStyle);
            XSSFCellStyle intStyle = workbook.createCellStyle();
            intStyle.setDataFormat(intFmt);
            intStyle.setAlignment(HorizontalAlignment.RIGHT);
            this.shadeCell(intStyle);
            XSSFCellStyle flagStyle = workbook.createCellStyle();
            flagStyle.setAlignment(HorizontalAlignment.CENTER);
            this.shadeCell(flagStyle);
            XSSFCellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setAlignment(HorizontalAlignment.LEFT);
            this.shadeCell(cellStyle);
            // Create the header row.
            XSSFRow row = newSheet.createRow(0);
            for (int c = 0; c < headers.size(); c++) {
                XSSFCell cell = row.createCell(c, CellType.STRING);
                cell.setCellValue(headers.get(c));
                cell.setCellStyle(headStyle);
            }
            // Next, we fill in the cell values.  We also track how many numbers are in each
            // column.
            int[] intCounts = new int[headers.size()];
            int[] numCounts = new int[headers.size()];
            int[] stringCounts = new int[headers.size()];
            int[] flagCounts = new int[headers.size()];
            int r = 1;
            for (List<String> record : keyedMap.getRecords()) {
                row = newSheet.createRow(r);
                int c = 0;
                for (String datum : record) {
                    XSSFCell cell;
                    // Here we determine the cell type.  The key (c == 0) is always a string.
                    if (datum.isBlank())
                        cell = row.createCell(c, CellType.BLANK);
                    else if (c >= 1 && DOUBLE_PATTERN.matcher(datum).matches()) {
                        cell = row.createCell(c, CellType.NUMERIC);
                        cell.setCellValue(Double.parseDouble(datum));
                        // Count the cell type.  If the column is all integers we use a different
                        // format.
                        if (INTEGER_PATTERN.matcher(datum).matches())
                            intCounts[c]++;
                        numCounts[c]++;
                    } else {
                        cell = row.createCell(c, CellType.STRING);
                        cell.setCellValue(datum);
                        // Count the cell type.  If the column is all single-character we use a
                        // different format.
                        if (datum.length() <= 1)
                            flagCounts[c]++;
                        stringCounts[c]++;
                    }
                    c++;
                }
                r++;
            }
            // Fix up the column formatting.  We auto-size the columns and set the format.
            for (int c = 0; c < headers.size(); c++) {
                // Here we figure out the column format.  The basic formats are integer,
                // floating, flag, and text.
                if (stringCounts[c] == 0) {
                    // Here we are all numbers.  We use the integer style if we are all integers.
                    if (intCounts[c] >= numCounts[c])
                        this.formatColumn(newSheet, intStyle, c, rows);
                    else
                        this.formatColumn(newSheet, numStyle, c, rows);
                } else {
                    // Here there are strings.  We use the flag style if we are all flags.
                    if (flagCounts[c] >= stringCounts[c])
                        this.formatColumn(newSheet, flagStyle, c, rows);
                    else
                        this.formatColumn(newSheet, cellStyle, c, rows);
                }
                // With the column formatted, we can auto-size it.  We add 512 to the width to
                // provide a extra character space.
                newSheet.autoSizeColumn(c);
                int oldWidth = newSheet.getColumnWidth(c);
                newSheet.setColumnWidth(c, oldWidth + 512);
            }
            // Set filtering in the top row and name the range.
            CellRangeAddress tableRange = new CellRangeAddress(0, rows, 0, headers.size() - 1);
            newSheet.setAutoFilter(tableRange);
            XSSFName tableName = workbook.createName();
            tableName.setNameName(sheetName);
            String tableRangeAddress = tableRange.formatAsString(sheetName, true);
            tableName.setRefersToFormula(tableRangeAddress);
            // Now write the table out.
            try (FileOutputStream saveStream = new FileOutputStream(this.getOutFile())) {
                workbook.write(saveStream);
            }
            this.checkForOpen();
        }
    }

    /**
     * Set the fill color and border for this cell style.
     *
     * @param style		cell style to modify
     */
    private void shadeCell(XSSFCellStyle style) {
        style.setBorderBottom(BorderStyle.DOTTED);
        style.setBorderTop(BorderStyle.DOTTED);
        style.setBorderLeft(BorderStyle.DOTTED);
        style.setBorderRight(BorderStyle.DOTTED);
        style.setFillForegroundColor(BAND_COLOR);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }

    /**
     * Apply a format to all the cells in a table column.
     *
     * @param sheet		sheet containing the table
     * @param format	formatting style to apply
     * @param c			column index
     * @param rows		number of data rows
     */
    private void formatColumn(XSSFSheet sheet, CellStyle format, int c, int rows) {
        // We don't format the header row, since we want the labels to remain left-aligned
        // and away from the little filter arrows.
        for (int r = 1; r <= rows; r++) {
            XSSFCell cell = sheet.getRow(r).getCell(c);
            cell.setCellStyle(format);
        }
    }

    @Override
    protected String getFileLabel() {
        return "Spreadsheet";
    }

    @Override
    protected ExtensionFilter getFilter() {
        return JoinDialog.EXCEL_FILTER;
    }

    @Override
    protected void initControls() {
        // Default a sheet name.
        this.txtSheetName.setText(this.getParent().getName());
        // Get the initial number format precision.
        this.precision = (int) this.slidePrecision.getValue();
        // Set up a listener for the number format slider.
        this.slidePrecision.valueProperty().addListener((observable, oldVal, newVal) -> {
            this.updatePrecision(newVal.intValue());
        });
    }

}
