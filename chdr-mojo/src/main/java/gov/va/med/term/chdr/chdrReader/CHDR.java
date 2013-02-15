package gov.va.med.term.chdr.chdrReader;

import gov.va.oia.terminology.converters.sharedUtils.ConsoleUtil;

public class CHDR
{
	enum DIRECTION
	{
		Incoming, Outgoing
	};

	String MediationCode, VUIDText, MediationText;
	String VUID;
	DIRECTION direction;

	public CHDR(String VUID, String VUIDText, String MediationCode, String MediationText, DIRECTION direction)
	{
		this.MediationCode = MediationCode;
		this.MediationText = MediationText;
		this.VUID = VUID;
		this.VUIDText = VUIDText;
		this.direction = direction;

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
