package gov.va.med.term.chdr.analysis;

import java.util.concurrent.atomic.AtomicInteger;

public class Stats
{
	AtomicInteger chdrVhatTextInconsistency, chdrMediationTextInconsistency, newExactMatches, newFuzzyMatches, invalidVHATIdentifer, errors, otherNotes, chdrItemCount,
			newSoundsLikeMatch, newLuceneMatches, verifiedExactMatch, noMatch, invalidMediationIdentifier, existingExactMatch, existingExactMatchToOtherDescription,
			existingNonExactMatch, misMatchedConceptId;

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

	public String toString()
	{
		String eol = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();
		sb.append("Processed " + chdrItemCount + " CHDR rows" + eol);
		sb.append("CHDR VHAT Description Inconsistent: " + chdrVhatTextInconsistency + eol);
		sb.append("Incoming and Outgoing Mediation Text Inconsistency and/or Mediation Text Incorrect for Concept: " + chdrMediationTextInconsistency + eol);
		sb.append("VHAT Identifier Missing in VHAT: " + invalidVHATIdentifer + eol);
		sb.append("Mediation ID Missing in Terminology: " + invalidMediationIdentifier + eol);
		sb.append("Mediation Text doesn't exist in Mediation Concept: " + misMatchedConceptId + eol);
		sb.append("Total Error count: " + errors + eol);
		sb.append("Other Note Count: " + otherNotes + eol);
		sb.append("Verified Mediation Code and Text Matches: " + verifiedExactMatch + eol);
		sb.append("Existing Exact Matches: " + existingExactMatch + eol);
		sb.append("Existing Exact Match to unlisted description: " + existingExactMatchToOtherDescription + eol);
		sb.append("Existing Non-Exact Matches: " + existingNonExactMatch + eol);
		sb.append("Proposed Exact Matches: " + newExactMatches + eol);
		sb.append("Proposed Sounds-Like Matches: " + newSoundsLikeMatch + eol);
		sb.append("Proposed Lucene Matches: " + newLuceneMatches + eol);
		sb.append("Proposed Fuzzy Matches: " + newFuzzyMatches + eol);
		sb.append("No Match: " + noMatch + eol);

		return sb.toString();
	}

}
