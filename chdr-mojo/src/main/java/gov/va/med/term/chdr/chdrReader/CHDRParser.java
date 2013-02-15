package gov.va.med.term.chdr.chdrReader;

import gov.va.oia.terminology.converters.sharedUtils.ConsoleUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import au.com.bytecode.opencsv.CSVReader;

public class CHDRParser
{
	public static Boolean doingCSV = null; 

	public static ArrayList<CHDR> readData(File file) throws Exception
	{
		if (file.getName().toLowerCase().endsWith(".csv"))
		{
			if (doingCSV == null)
			{
				doingCSV = true;
			}
			else if (!doingCSV)
			{
				throw new RuntimeException("Mixing and matching CSV and XLS is not supported"); 
			}
			ArrayList<String[]> lines = new ArrayList<String[]>();
			int directionPos = file.getName().indexOf("-");
			String directionInfo = file.getName().substring(directionPos + 1, file.getName().indexOf(".csv"));

			CHDR.DIRECTION direction;
			if (directionInfo.toLowerCase().equals("incoming"))
			{
				direction = CHDR.DIRECTION.Incoming;
			}
			else if (directionInfo.toLowerCase().equals("outgoing"))
			{
				direction = CHDR.DIRECTION.Outgoing;
			}
			else
			{
				throw new RuntimeException("Unexpeted direction info in file name " + file.getName());
			}

			CSVReader reader = new CSVReader(new FileReader(file));
			String[] line = reader.readNext();
			while (line != null)
			{
				lines.add(line);
				line = reader.readNext();
			}
			reader.close();

			return processLines(lines, direction);
		}
		else if (file.getName().endsWith(".xls"))
		{
			if (doingCSV == null)
			{
				doingCSV = false;
			}
			else if (doingCSV)
			{
				throw new RuntimeException("Mixing and matching CSV and XLS is not supported"); 
			}
			HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(file));

			boolean foundIncoming = false;
			boolean foundOutgoing = false;

			ArrayList<CHDR> results = new ArrayList<CHDR>();
			for (int i = 0; i < wb.getNumberOfSheets(); i++)
			{
				ArrayList<String[]> sheetLines = new ArrayList<String[]>();
				Sheet sheet = wb.getSheetAt(i);
				CHDR.DIRECTION direction;
				if (sheet.getSheetName().toLowerCase().indexOf("incoming") >= 0)
				{
					if (foundIncoming)
					{
						throw new RuntimeException("Found two sheets for incoming!");
					}
					direction = CHDR.DIRECTION.Incoming;
					foundIncoming = true;
				}
				else if (sheet.getSheetName().toLowerCase().indexOf("outgoing") >= 0)
				{
					if (foundOutgoing)
					{
						throw new RuntimeException("Found two sheets for outgoing!");
					}
					direction = CHDR.DIRECTION.Outgoing;
					foundOutgoing = true;
				}
				else
				{
					ConsoleUtil.println("Ignoring sheet '" + sheet.getSheetName() + "' within " + file.getName());
					continue;
				}
				

				short colStart = sheet.getRow(0).getFirstCellNum();
				short colEnd = sheet.getRow(0).getLastCellNum();
				Iterator<Row> rowIter = sheet.rowIterator();
				while (rowIter.hasNext())
				{
					Row r = rowIter.next();
					
					ArrayList<String> lineItems = new ArrayList<>();
					for (short cellIndex = colStart; cellIndex < colEnd; cellIndex++)
					{
						Cell cell = r.getCell(cellIndex);
						if (cell == null)
						{
							lineItems.add("");
						}
						else
						{
							if (cell.getCellType() == Cell.CELL_TYPE_STRING)
							{
								lineItems.add(cell.getStringCellValue());
							}
							else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC)
							{
								lineItems.add(new Double(cell.getNumericCellValue()).longValue() + "");  //strip off the .0 that I get from double...
							}
							else if (cell.getCellType() == Cell.CELL_TYPE_BLANK)
							{
								lineItems.add("");
							}
							else
							{
								throw new RuntimeException("Unexpected cell type");
							}
						}
					}
					boolean rowHasContent = false;
					for (String s : lineItems)
					{
						if (s.length() > 0)
						{
							rowHasContent = true;
							break;
						}
					}
					
					if (rowHasContent)
					{
						sheetLines.add(lineItems.toArray(new String[lineItems.size()]));
					}
				}
				results.addAll(processLines(sheetLines, direction));
			}
			
			if (!foundIncoming || !foundOutgoing)
			{
				throw new RuntimeException("Didn't find both incoming and outgoing sheets in the spreadsheet for " + file.getName());
			}
			
			return results;
		}
		else
		{
			throw new RuntimeException("Unexpected format");
		}

	}

	private static ArrayList<CHDR> processLines(ArrayList<String[]> lines, CHDR.DIRECTION direction)
	{
		int medCodePos = -1;
		int medTextPos = -1;
		int vuidPos = -1;
		int vuidTextPos = -1;

		String[] header = lines.remove(0);

		int pos = 0;
		for (String s : header)
		{
			if (s == null)
			{
				pos++;
				continue;
			}
			s = s.trim().toLowerCase();
			if (s.equals("mediationcode"))
			{
				medCodePos = pos++;
			}
			else if (s.equals("mediationtext"))
			{
				medTextPos = pos++;
			}
			else if (s.equals("vuid"))
			{
				vuidPos = pos++;
			}
			else if (s.equals("vuidtext"))
			{
				vuidTextPos = pos++;
			}
			else
			{
				pos++;
				if (s.trim().length() > 0)
				{
					ConsoleUtil.printErrorln("Unknown field: '" + s + "'");
				}
			}
		}

		int headerSize = pos;

		ArrayList<CHDR> data = new ArrayList<CHDR>();

		for (String[] lineBits : lines)
		{
			if (data.size() % 100 == 0)
			{
				ConsoleUtil.showProgress();
			}
			if (lineBits.length != headerSize)
			{
				ConsoleUtil.printErrorln("Wrong number of items on line!");
			}
			
			data.add(new CHDR((vuidPos == -1 || lineBits[vuidPos].length() == 0 ? null : lineBits[vuidPos]),
					(vuidTextPos == -1 ? null : lineBits[vuidTextPos]),
					(medCodePos == -1 ? null : lineBits[medCodePos]), (medTextPos == -1 ? null : lineBits[medTextPos]),
					direction));


			if (lineBits.length > 4)
			{
				// Don't think these matter, but check.
				for (int i = 4; i < lineBits.length; i++)
				{
					if (lineBits[i].trim().length() > 0)
					{
						ConsoleUtil.printErrorln("Skipped data on line: '" + lineBits[i] + "'");
					}
				}
			}

		}
		return data;
	}
}
