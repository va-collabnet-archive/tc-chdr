package gov.va.med.term.chdr.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_MemberRefsets;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.Property;

public class PT_Refsets extends BPT_MemberRefsets
{
	public enum Refsets
	{
		ALL("All CHDR Concepts"), 
		DRUG_PRODUCTS("Drug Products"),
		REACTANTS("Reactants"),
		REACTIONS("Reactions");

		private Property property;

		private Refsets(String niceName)
		{
			// Don't know the owner yet - will be autofilled when we add this to the parent, below.
			property = new Property(null, niceName);
		}

		public Property getProperty()
		{
			return property;
		}
	}

	public PT_Refsets()
	{
		super("CHDR");
		for (Refsets mm : Refsets.values())
		{
			addProperty(mm.getProperty());
		}
	}
}
