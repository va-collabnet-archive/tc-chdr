package gov.va.med.term.chdr.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Attributes;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.Property;

public class PT_Attributes extends BPT_Attributes
{
	public enum Attributes
	{
		//CHDR defines relationships on descriptions - we move them up to concepts, and annotate the relationships with the desc it came from. 
		CHDR_REL_SOURCE("CHDR Relation Source");

		private Property property;

		private Attributes(String niceName)
		{
			// Don't know the owner yet - will be autofilled when we add this to the parent, below.
			property = new Property(null, niceName);
		}

		public Property getProperty()
		{
			return property;
		}
	}

	public PT_Attributes(String uuidRoot)
	{
		super(uuidRoot);
		for (Attributes attr : Attributes.values())
		{
			addProperty(attr.getProperty());
		}
	}
}
