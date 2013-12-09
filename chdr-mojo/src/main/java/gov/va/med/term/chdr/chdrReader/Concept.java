package gov.va.med.term.chdr.chdrReader;

import java.util.Collection;
import java.util.HashSet;


public class Concept
{
	String id;
	ConceptType type;
	HashSet<String> descriptions;
	
	/**
	 * Only adds description if description is not empty
	 */
	public Concept(String id, String description, ConceptType type)
	{
		this.id = id;
		descriptions = new HashSet<String>();
		if (description != null && description.trim().length() > 0)
		{
			descriptions.add(description.trim());
		}
		this.type = type;
	}
	
	/**
	 * Only adds if different from already entered descriptions
	 */
	public void addDescription(String description)
	{
		if (description == null)
		{
			return;
		}
		String d = description.trim();
		if (d.length() > 0)
		{
			descriptions.add(d);
		}
	}
	
	public String getId()
	{
		return id;
	}
	
	public Collection<String> getDescriptions()
	{
		return descriptions;
	}
	
	/**
	 * Throws an error if there is more than one description
	 * @throws Exception 
	 */
	public String getDescription() throws Exception
	{
		if (descriptions.size() == 0)
		{
			return null;
		}
		else if (descriptions.size() == 1)
		{
			return descriptions.iterator().next();
		}
		else
		{
			throw new Exception("More than 1 description on the concept");
		}
	}
	
	public ConceptType getType()
	{
		return type;
	}
}
