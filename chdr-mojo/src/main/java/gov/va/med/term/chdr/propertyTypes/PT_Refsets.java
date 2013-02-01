package gov.va.med.term.chdr.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.Property;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.PropertyType;

public class PT_Refsets extends PropertyType
{
	public enum Refsets
	{
		INCOMING("Incoming to VA"),
		OUTGOING("Outgoing from VA");

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

	public PT_Refsets(String uuidRoot)
	{
		super("RefSets", uuidRoot);
		for (Refsets r : Refsets.values())
		{
		    addProperty(r.getProperty());
		}
	}
}
