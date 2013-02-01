package gov.va.med.term.chdr.chdrReader;

import gov.va.oia.terminology.converters.sharedUtils.ConsoleUtil;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import au.com.bytecode.opencsv.CSVReader;

public class CHDRParser
{

	public static ArrayList<CHDR> readData(File file) throws Exception
	{
		CSVReader reader = new CSVReader(new FileReader(file));
		
		String[] lineBits = reader.readNext();
		
		int medCodePos = -1;
		int medTextPos = -1;
		int vuidPos = -1;
		int vuidTextPos = -1;
		
		
		int pos = 0;
		for (String s : lineBits)
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
		
		String[] bits = reader.readNext();
		while (bits != null)
		{
			if (data.size() % 100 == 0)
			{
				ConsoleUtil.showProgress();
			}
			if (bits.length != headerSize)
			{
				ConsoleUtil.printErrorln("Wrong number of items on line!");
			}
			
			data.add(new CHDR((vuidPos == -1 || bits[vuidPos].length() == 0 ? null : Long.parseLong(bits[vuidPos])), 
					(vuidTextPos == -1 ? null : bits[vuidTextPos]), 
					(medCodePos == -1 ? null : bits[medCodePos]), 
					(medTextPos == -1 ? null : bits[medTextPos])));
			
			if (bits.length > 4)
			{
				//Don't think these matter, but check.
				for (int i = 4; i < bits.length; i++)
				{
					if (bits[i].trim().length() > 0)
					{
						ConsoleUtil.printErrorln("Skipped data on line: '" + bits[i] + "'");
					}
				}
			}
			
			bits = reader.readNext();
		}
		reader.close();
		return data;
	}
}
