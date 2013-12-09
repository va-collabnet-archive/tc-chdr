package gov.va.med.term.chdr.analysis;

public enum MatchType
{
	EXACT_MATCH("Existing Exact Match"),
	NEW_EXACT_MATCH("Proposed Exact Match"), 
	EXACT_MATCH_OTHER_DESC("Existing Different Description"),
	EXISTING_NO_MATCH("Existing Non-Exact Match"),
	NEW_SOUNDS_LIKE("Proposed Sounds-Like Match"),
	NEW_LUCENE("Proposed Different Word Order"),
	NEW_LUCENE_1("Proposed Different Word Order +1"),
	NEW_LUCENE_2("Proposed Different Word Order +2"),
	SIMILAR("Proposed Similarity Match"),
	NO_MATCH("No Match");
	
	
	private String description_;
	
	private MatchType(String description)
	{
		description_ = description;
	}
	
	public String getDescription()
	{
		return description_;
	}
	
}
