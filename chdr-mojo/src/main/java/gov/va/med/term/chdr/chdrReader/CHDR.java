package gov.va.med.term.chdr.chdrReader;

import gov.va.oia.terminology.converters.sharedUtils.ConsoleUtil;

public class CHDR
{
	String MediationCode, VUIDText, MediationText;
	Long VUID;
	
	public CHDR(Long VUID, String VUIDText, String MediationCode, String MediationText)
	{
		this.MediationCode = MediationCode;
		this.MediationText = MediationText;
		this.VUID = VUID;
		this.VUIDText = VUIDText;
		
		if (this.VUID == null)
		{
			ConsoleUtil.printErrorln("No VUID");
		}
		if (this.VUIDText == null || this.VUIDText.trim().length() == 0)
		{
			ConsoleUtil.printErrorln("No VUID Text");
		}
	}
}
