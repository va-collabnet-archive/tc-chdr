package gov.va.med.term.chdr.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_IDs;

/**
 * Just a class to allow generation of the same exact UUIDs that VHAT used to generate UUIDs of the concepts.
 * 
 * @author darmbrust
 * 
 */
public class PT_VHAT_ID extends BPT_IDs
{
	public PT_VHAT_ID(String uuidRoot)
	{
		super(uuidRoot);
		addProperty("VUID");
	}
}
