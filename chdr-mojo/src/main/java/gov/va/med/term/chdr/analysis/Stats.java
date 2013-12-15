package gov.va.med.term.chdr.analysis;

import java.util.concurrent.atomic.AtomicInteger;

public class Stats
{
	AtomicInteger chdrVhatTextInconsistency, chdrMediationTextInconsistency, newExactMatches, newFuzzyMatches, invalidVHATIdentifer, errors, otherNotes, chdrItemCount,
			newSoundsLikeMatch, newLuceneMatches, verifiedExactMatch, noMatch, invalidMediationIdentifier, existingExactMatch, existingExactMatchToOtherDescription,
			existingNonExactMatch, misMatchedConceptId, startingRowsWithMatches, startingRowsWithNoMatches;

	protected Stats()
	{
		this.chdrVhatTextInconsistency = new AtomicInteger();
		this.chdrMediationTextInconsistency = new AtomicInteger();
		this.newExactMatches = new AtomicInteger();
		this.newFuzzyMatches = new AtomicInteger();
		this.invalidVHATIdentifer = new AtomicInteger();
		this.errors = new AtomicInteger();
		this.otherNotes = new AtomicInteger();
		this.chdrItemCount = new AtomicInteger();
		this.newSoundsLikeMatch = new AtomicInteger();
		this.newLuceneMatches = new AtomicInteger();
		this.verifiedExactMatch = new AtomicInteger();
		this.noMatch = new AtomicInteger();
		this.invalidMediationIdentifier = new AtomicInteger();
		this.existingExactMatch = new AtomicInteger();
		this.existingNonExactMatch = new AtomicInteger();
		this.existingExactMatchToOtherDescription = new AtomicInteger();
		this.misMatchedConceptId = new AtomicInteger();
		this.startingRowsWithMatches = new AtomicInteger();
		this.startingRowsWithNoMatches = new AtomicInteger();
	}

	public void incChdrVhatTextInconsistency()
	{
		chdrVhatTextInconsistency.incrementAndGet();
	}

	public void incChdrMediationTextInconsistency()
	{
		chdrMediationTextInconsistency.incrementAndGet();
	}

	public void incNewExactMatch()
	{
		newExactMatches.incrementAndGet();
	}

	public void incNewSoundsLikeMatch()
	{
		newSoundsLikeMatch.incrementAndGet();
	}

	public void incNewFuzzyMatches()
	{
		newFuzzyMatches.incrementAndGet();
	}

	public void incInvalidVHATIdentifier()
	{
		invalidVHATIdentifer.incrementAndGet();
	}

	public void incErrorCount(int count)
	{
		errors.addAndGet(count);
	}

	public void incOtherNotesCount(int count)
	{
		otherNotes.addAndGet(count);
	}

	public void incChdrItemCount()
	{
		chdrItemCount.incrementAndGet();
	}
	
	public void incNewLuceneMatch()
	{
		newLuceneMatches.incrementAndGet();
	}
	
	public void incVerifiedExactMatch()
	{
		verifiedExactMatch.incrementAndGet();
	}
	
	public void incNoMatch()
	{
		noMatch.incrementAndGet();
	}
	
	public void incInvalidMediationIdentifier()
	{
		invalidMediationIdentifier.incrementAndGet();
	}
	
	public void incExistingExactMatch()
	{
		existingExactMatch.incrementAndGet();
	}
	
	public void incExistingNonExactMatch()
	{
		existingNonExactMatch.incrementAndGet();
	}
	
	public void incExistingExactMatchToOtherDescription()
	{
		existingExactMatchToOtherDescription.incrementAndGet();
	}
	
	public void incMisMatchedConceptId()
	{
		misMatchedConceptId.incrementAndGet();
	}
	
	public void incStartingRowsWithMatches()
	{
		startingRowsWithMatches.incrementAndGet();
	}
	
	public void incStartingRowsWithoutMatches()
	{
		startingRowsWithNoMatches.incrementAndGet();
	}

	public String toString()
	{
		String eol = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();
		sb.append("Processed " + chdrItemCount + " CHDR rows" + eol);
		sb.append("Identified " + startingRowsWithMatches + " existing CHDR rows with matches" + eol);
		sb.append("Identified " + startingRowsWithNoMatches + " CHDR rows without matches" + eol);
		sb.append("Verified mediation code and mediation text matches: " + verifiedExactMatch + eol);
		sb.append(eol);
		sb.append("*** Existing Mappings ***" + eol);
		sb.append("Existing exact Matches: " + existingExactMatch + eol);
		sb.append("Existing exact match to unlisted description: " + existingExactMatchToOtherDescription + eol);
		sb.append("Existing non-exact matches: " + existingNonExactMatch + eol);
		sb.append(eol);
		sb.append("*** Proposed Matches ***" + eol);
		sb.append("Proposed exact matches: " + newExactMatches + eol);
		sb.append("Proposed \"Sounds-Like\" matches: " + newSoundsLikeMatch + eol);
		sb.append("Proposed Lucene matches: " + newLuceneMatches + eol);
		sb.append("Proposed \"Fuzzy\" matches: " + newFuzzyMatches + eol);
		sb.append(eol);
		sb.append("*** Non-Matching ***" + eol);
		sb.append("No matches exist nor are any matches proposed: " + noMatch + eol);
		sb.append(eol);
		sb.append("*** Errors in Data ***" + eol);
		sb.append("CHDR VHAT description inconsistent: " + chdrVhatTextInconsistency + eol);
		sb.append("Incoming and outgoing mediation text inconsistency and/or mediation text incorrect for concept: " + chdrMediationTextInconsistency + eol);
		sb.append("VHAT ID listed by CHDR not found in VHAT: " + invalidVHATIdentifer + eol);
		sb.append("Mediation ID listed by CHDR not found in terminology: " + invalidMediationIdentifier + eol);
		sb.append("Mediation text listed by CHDR not found in the specified terminology concept: " + misMatchedConceptId + eol);
		sb.append("Total CHDR Rows with errors in data: " + errors + eol);
		sb.append(eol);
		sb.append("*** Notes on Existing Matches ***" + eol);
		sb.append("Other note count: " + otherNotes + eol);
		return sb.toString();
	}

}
