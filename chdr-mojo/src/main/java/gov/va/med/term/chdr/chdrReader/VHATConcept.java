package gov.va.med.term.chdr.chdrReader;

import java.util.Collection;
import java.util.HashSet;

public class VHATConcept extends Concept
{
	HashSet<Concept> incomingRels;
	HashSet<Concept> outgoingRels;

	public VHATConcept(String vuid, String description)
	{
		super(vuid, description, ConceptType.VHAT);
		incomingRels = new HashSet<Concept>();
		outgoingRels = new HashSet<Concept>();
	}

	/**
	 * Only adds if different from already entered rel
	 */
	public void addIncomingRel(Concept c)
	{
		if (c == null)
		{
			return;
		}
		incomingRels.add(c);
	}

	/**
	 * Only adds if different from already entered rel
	 */
	public void addOutgoingRel(Concept c)
	{
		if (c == null)
		{
			return;
		}
		outgoingRels.add(c);
	}

	public boolean hasNoRels()
	{
		if (incomingRels.size() == 0 && outgoingRels.size() == 0)
		{
			return true;
		}
		return false;
	}

	public Collection<Concept> getIncomingRels()
	{
		return incomingRels;
	}

	public Collection<Concept> getOutgoingRels()
	{
		return outgoingRels;
	}
}
