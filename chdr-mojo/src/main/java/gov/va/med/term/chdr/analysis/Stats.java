package gov.va.med.term.chdr.analysis;

import java.util.concurrent.atomic.AtomicInteger;

public class Stats
{
	AtomicInteger chdrVhatTextInconsistency, chdrMediationTextInconsistency, newExactMatches, newFuzzyMatches, invalidVHATIdentifer, errors, otherNotes, chdrItemCount,
			newSoundsLikeMatch, newLuceneMatches, verifiedExactMatch, noMatch, invalidMediationIdentifier, existingExactMatch, existingExactMatchToOtherDescription,
			existingNonExactMatch;

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

	public String toString()
	{
		String eol = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();
		sb.append("Processed " + chdrItemCount + " CHDR rows" + eol);
		sb.append("Inconsistencies between CHDR and VHAT descriptions: " + chdrVhatTextInconsistency + eol);
		sb.append("Inconsistencies between CHDR and Mediation Terminology descriptions: " + chdrMediationTextInconsistency + eol);
		sb.append("Invalid VHAT Identifier linked by CHDR: " + invalidVHATIdentifer + eol);
		sb.append("Invalid Mediation Identifier linked by CHDR: " + invalidMediationIdentifier + eol);
		sb.append("Total Error count: " + errors + eol);
		sb.append("Other Note Count: " + otherNotes + eol);
		sb.append("Verified Mediation Code and Text Matches: " + verifiedExactMatch + eol);
		sb.append("Existing Exact Matches: " + existingExactMatch + eol);
		sb.append("Existing Exact Match to unlisted description: " + existingExactMatchToOtherDescription + eol);
		sb.append("Existing Non-Exact Matches: " + existingNonExactMatch + eol);
		sb.append("New Exact Matches: " + newExactMatches + eol);
		sb.append("New Sounds-Like Matches: " + newSoundsLikeMatch + eol);
		sb.append("New Lucene Matches: " + newLuceneMatches + eol);
		sb.append("New Fuzzy Matches: " + newFuzzyMatches + eol);
		sb.append("No Match: " + noMatch + eol);

		return sb.toString();
	}

}
