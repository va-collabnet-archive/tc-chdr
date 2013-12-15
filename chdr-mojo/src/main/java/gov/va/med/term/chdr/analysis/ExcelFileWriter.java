package gov.va.med.term.chdr.analysis;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

public class ExcelFileWriter
{
	HSSFWorkbook workbook = new HSSFWorkbook();
	HSSFSheet sheet = workbook.createSheet("Result");
	int rowNum = 0;

	public ExcelFileWriter(String[] headerColumns)
	{
		Row row = sheet.createRow(rowNum++);
		int cellNum = 0;
		for (String s : headerColumns)
		{
			Cell c = row.createCell(cellNum);
			c.setCellValue(s);
			HSSFCellStyle cellStyle = workbook.createCellStyle();
			HSSFFont font = workbook.createFont();
			font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
			cellStyle.setFont(font);
			c.setCellStyle(cellStyle);
			sheet.autoSizeColumn(cellNum++);
		}
	}

	public void addLine(String[] rowData)
	{
		Row row = sheet.createRow(rowNum++);
		int cellNum = 0;
		for (String s : rowData)
		{
			Cell c = row.createCell(cellNum++);
			if (s != null && s.length() > 0)
			{
				try
				{
					double d = Double.parseDouble(s);
					c.setCellValue(d);
				}
				catch (NumberFormatException e)
				{
					c.setCellValue(s);
				}
			}
		}
	}

	public void writeFile(File path) throws IOException
	{
		FileOutputStream out = new FileOutputStream(path);
		workbook.write(out);
		out.close();
	}
}
