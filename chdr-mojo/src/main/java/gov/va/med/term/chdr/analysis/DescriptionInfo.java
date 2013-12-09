package gov.va.med.term.chdr.analysis;

public class DescriptionInfo implements Comparable<DescriptionInfo>
{
	private String conceptId_, descriptionId_, description_;

	protected DescriptionInfo(String conceptId, String descriptionId, String description)
	{
		super();
		this.conceptId_ = conceptId;
		this.descriptionId_ = descriptionId;
		this.description_ = description;
	}

	public String getConceptId()
	{
		return conceptId_;
	}

	public void setConceptId(String conceptId)
	{
		this.conceptId_ = conceptId;
	}

	public String getDescriptionId()
	{
		return descriptionId_;
	}

	public void setDescriptionId(String descriptionId)
	{
		this.descriptionId_ = descriptionId;
	}

	public String getDescription()
	{
		return description_;
	}

	public void setDescription(String description)
	{
		this.description_ = description;
	}

	@Override
	public int compareTo(DescriptionInfo o)
	{
		int i = this.getConceptId().compareTo(o.getConceptId());
		if (i == 0)
		{
			i = this.getDescription().compareTo(o.getDescription());
		}
		return i;
	}
}
