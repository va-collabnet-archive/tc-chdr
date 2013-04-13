package gov.va.med.term.chdr.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Relations;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.Property;

public class PT_Relations extends BPT_Relations
{
	public enum MediationMapping
	{
		INCOMING("Incoming to VA"), 
		OUTGOING("Outgoing from VA");

		private Property property;

		private MediationMapping(String niceName)
		{
			// Don't know the owner yet - will be autofilled when we add this to the parent, below.
			property = new Property(null, niceName);
		}

		public Property getProperty()
		{
			return property;
		}
	}

	public PT_Relations()
	{
		super("CHDR");
		for (MediationMapping mm : MediationMapping.values())
		{
			addProperty(mm.getProperty());
		}
	}
}
