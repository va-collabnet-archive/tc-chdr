package gov.va.med.term.chdr.chdrReader;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class VHATConcept extends Concept
{
	HashSet<Concept> incomingRels;
	HashSet<Concept> outgoingRels;
	HashMap<ConceptType, HashSet<CHDR.DIRECTION>> chdrSourceFiles;

	public VHATConcept(String vuid, String description, ConceptType sourceFile, CHDR.DIRECTION direction)
	{
		super(vuid, description, ConceptType.VHAT);
		incomingRels = new HashSet<Concept>();
		outgoingRels = new HashSet<Concept>();
		chdrSourceFiles = new HashMap<>();
		HashSet<CHDR.DIRECTION> temp = new HashSet<>();
		temp.add(direction);
		chdrSourceFiles.put(sourceFile, temp);
	}

	public void addDescription(String description, ConceptType sourceFile, CHDR.DIRECTION direction)
	{
		super.addDescription(description);
		HashSet<CHDR.DIRECTION> temp = chdrSourceFiles.get(sourceFile);
		if (temp == null)
		{
			temp = new HashSet<CHDR.DIRECTION>();
			chdrSourceFiles.put(sourceFile, temp);
		}
		temp.add(direction);
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
	
	public HashMap<ConceptType, HashSet<CHDR.DIRECTION>> getConceptSourceFiles()
	{
		return chdrSourceFiles;
	}
}
