package gov.va.med.term.chdr.analysis;

import gov.va.med.term.chdr.chdrReader.CHDR;
import gov.va.med.term.chdr.chdrReader.CHDRDataHolder;
import gov.va.med.term.chdr.chdrReader.Concept;
import gov.va.med.term.chdr.chdrReader.ConceptType;
import gov.va.med.term.chdr.chdrReader.VHATConcept;
import gov.va.oia.terminology.converters.sharedUtils.ConsoleUtil;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.etypes.EConcept;
import org.ihtsdo.tk.dto.concept.component.description.TkDescription;
import org.ihtsdo.tk.dto.concept.component.identifier.TkIdentifier;
import org.ihtsdo.tk.dto.concept.component.refex.TkRefexAbstractMember;
import org.ihtsdo.tk.dto.concept.component.refex.type_string.TkRefsetStrMember;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * Goal which converts CHDR data into the workbench jbin format
 * 
 * @goal analyze-CHDR-data
 * 
 * @phase process-sources
 */
public class CHDRAnalysisMojo extends AbstractMojo
{
	private final String luceneField = "d";

	/**
	 * Where to put the output file.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */
	private File outputDirectory;

	/**
	 * Location of CHDR source data files. Expected to be a directory.
	 * 
	 * @parameter
	 * @required
	 */
	private File inputFile;

	/**
	 * Location of vhat jbin data file. Expected to be a directory.
	 * 
	 * @parameter
	 * @required
	 */
	private File vhatInputFile;

	/**
	 * Locations of external terminology input files. Each is expected to be a directory.
	 * 
	 * @parameter
	 * @required
	 */
	private File[] externalInputFiles;

	private HashMap<String, DescriptionInfo> vhatDescriptions_ = new HashMap<>();
	private CHDRDataHolder cdh_;
	DoubleMetaphone dm_ = new DoubleMetaphone();
	WhitespaceAnalyzer wa = new WhitespaceAnalyzer(Version.LUCENE_46);

	//This turned out to be incredibly slow, and didn't offer any better results than the simpler Levenshtein algorithm
	//	DamerauLevenshteinAlgorithm dla_ = new DamerauLevenshteinAlgorithm(1, 1, 1, 1);
	
	ConcurrentHashMap<String, String> dlaCache = new ConcurrentHashMap<>();
	
	private enum Terminology {RxNORM, SCT, NDFRT, UNKNOWN};

	public CHDRAnalysisMojo()
	{
		dm_.setMaxCodeLen(20);
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		try
		{
			outputDirectory = new File(outputDirectory, "analysis");
			if (!outputDirectory.exists())
			{
				outputDirectory.mkdirs();
			}

			ConsoleUtil.println("Reading CHDR data");

			cdh_ = new CHDRDataHolder(inputFile);

			ConsoleUtil.println("Reading VHAT concepts file");

			// Read in the VHAT data
			DataInputStream in = new DataInputStream(new FileInputStream(vhatInputFile.listFiles(new FilenameFilter()
			{
				@Override
				public boolean accept(File dir, String name)
				{
					if (name.endsWith(".jbin"))
					{
						return true;
					}
					return false;
				}
			})[0]));

			int vhatConceptCount = 0;

			while (in.available() > 0)
			{
				EConcept temp = new EConcept(in);
				vhatConceptCount++;

				String conceptId = null;
				if (temp.getConceptAttributes() != null && temp.getConceptAttributes().getAdditionalIdComponents() != null)
					for (TkIdentifier conceptIds : temp.getConceptAttributes().getAdditionalIdComponents())
					{
						if (conceptId != null)
						{
							throw new Exception("Unexpected - multiple IDs on a VHAT Concept");
						}
						conceptId = conceptIds.getDenotation().toString();
					}
				if (conceptId == null)
				{
					// Use the UUID, but this probably isn't a concept we care about (metadata concept, most likely)
					conceptId = temp.getPrimordialUuid().toString();
				}

				if (temp.getDescriptions() != null)
				{
					for (TkDescription d : temp.getDescriptions())
					{
						String descriptionId = null;
						if (d.getAdditionalIdComponents() != null)
						{
							for (TkIdentifier descIds : d.getAdditionalIdComponents())
							{
								if (descriptionId != null)
								{
									throw new Exception("Unexpected - multiple IDs on a VHAT description");
								}
								descriptionId = descIds.getDenotation().toString();
							}
						}
						if (descriptionId == null)
						{
							descriptionId = d.getPrimordialComponentUuid().toString();
						}

						DescriptionInfo di = new DescriptionInfo(conceptId, descriptionId, d.getText());
						vhatDescriptions_.put(descriptionId, di);
					}
				}
			}
			in.close();

			ConsoleUtil.println("Read " + vhatConceptCount + " concepts from the VHAT jbin file with a total of " + vhatDescriptions_.size() + " descriptions");

			for (File f : externalInputFiles)
			{
				process(f);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new MojoExecutionException("oops", e);
		}
	}

	private void process(File externalInputFile) throws Exception
	{
		ConsoleUtil.println("Reading " + externalInputFile.getName());
		// lowercase desc text to concept id to descriptioninfo
		HashMap<String, HashMap<String, DescriptionInfo>> externalDescriptionsByText = new HashMap<>();
		// lowercase desc text double-metaphone value to concept id to descriptioninfo
		HashMap<String, ArrayList<DescriptionInfo>> externalDescriptionsByTextSoundsLike = new HashMap<>();
		// concept id to description info
		HashMap<String, ArrayList<DescriptionInfo>> externalDescriptionsByConceptId = new HashMap<>();
		
		dlaCache.clear();
		
		Directory luceneIndex = new RAMDirectory();
		@SuppressWarnings("resource")
		IndexWriter luceneIndexWriter = new IndexWriter(luceneIndex, new IndexWriterConfig(Version.LUCENE_46, wa));

		DataInputStream in = new DataInputStream(new FileInputStream(externalInputFile.listFiles(new FilenameFilter()
		{
			@Override
			public boolean accept(File dir, String name)
			{
				if (name.endsWith(".jbin"))
				{
					return true;
				}
				return false;
			}
		})[0]));
		
		int conceptCounter = 0;
		Terminology termType = Terminology.UNKNOWN;
		
		if (externalInputFile.getName().toLowerCase().equals("ndf-rt"))
		{
			termType = Terminology.NDFRT;
		}
		else if (externalInputFile.getName().toLowerCase().equals("sct"))
		{
			termType = Terminology.SCT;
		}
		else if (externalInputFile.getName().toLowerCase().equals("rxnorm"))
		{
			termType = Terminology.RxNORM;
		}
		else
		{
			ConsoleUtil.printErrorln("Processing an unexpected target terminology - code may need updates");
		}

		while (in.available() > 0)
		{
			if (externalDescriptionsByText.size() % 1000 == 0)
			{
				ConsoleUtil.showProgress();
			}

			EConcept temp = new EConcept(in);
			conceptCounter++;

			String conceptId = null;
			// In the case of ndf-rt, pull out the CUI to use as the code.
			if (termType == Terminology.NDFRT)
			{
				if (temp.getConceptAttributes() != null && temp.getConceptAttributes().getAnnotations() != null)
				{
					for (TkRefexAbstractMember<?> annotation : temp.getConceptAttributes().getAnnotations())
					{
						// dd7722cd-ebe8-5554-aee3-4b792feb8d98 is the UUID for 'UMLS CUI' in NDF-RT. could put code here that shows how to do that
						// from the
						// NDF-RT loader... but not bothering at the moment
						if (annotation instanceof TkRefsetStrMember && annotation.getRefexUuid().equals(UUID.fromString("dd7722cd-ebe8-5554-aee3-4b792feb8d98")))
						{
							conceptId = ((TkRefsetStrMember) annotation).getString1();
							break;
						}
					}
				}
			}

			if (conceptId == null && temp.getConceptAttributes() != null && temp.getConceptAttributes().getAdditionalIdComponents() != null)
			{
				for (TkIdentifier conceptIds : temp.getConceptAttributes().getAdditionalIdComponents())
				{
					conceptId = conceptIds.getDenotation().toString();
					break;
					// - perhaps, need to select which one to use in some cases?
				}
			}
			if (conceptId == null)
			{
				conceptId = temp.getPrimordialUuid().toString();
			}

			if (temp.getDescriptions() != null)
			{
				for (TkDescription d : temp.getDescriptions())
				{
					String descriptionId = null;
					if (d.getAdditionalIdComponents() != null)
					{
						for (TkIdentifier descIds : d.getAdditionalIdComponents())
						{
							if (descriptionId != null)
							{
								throw new Exception("Unexpected - multiple IDs on a VHAT description");
							}
							descriptionId = descIds.getDenotation().toString();
						}
					}
					if (descriptionId == null)
					{
						descriptionId = d.getPrimordialComponentUuid().toString();
					}
					HashMap<String, DescriptionInfo> diMap = externalDescriptionsByText.get(d.getText().toLowerCase());
					if (diMap == null)
					{
						diMap = new HashMap<>();
						externalDescriptionsByText.put(d.getText().toLowerCase(), diMap);
					}
					// This may overwrite an existing one - but I don't care - the conceptId and text are identical - doesn't matter if they have a
					// different
					// descriptionID for whatever reason.
					DescriptionInfo di = new DescriptionInfo(conceptId, descriptionId, d.getText());
					diMap.put(conceptId, di);

					ArrayList<DescriptionInfo> list = externalDescriptionsByConceptId.get(conceptId);
					if (list == null)
					{
						list = new ArrayList<DescriptionInfo>();
						externalDescriptionsByConceptId.put(conceptId, list);
					}
					list.add(di);

					String soundsLikeCode = doubleMetaphone(di.getDescription());
					list = externalDescriptionsByTextSoundsLike.get(soundsLikeCode);
					if (list == null)
					{
						list = new ArrayList<DescriptionInfo>();
						externalDescriptionsByTextSoundsLike.put(soundsLikeCode, list);
					}
					list.add(di);
					
					//Index this in to lucene as well
					
					Document luceneDoc = new Document();
					luceneDoc.add(new TextField(luceneField, di.getDescription().toLowerCase(), Field.Store.YES));
					luceneIndexWriter.addDocument(luceneDoc);
				}
			}
		}
		luceneIndexWriter.close();
		in.close();
		ConsoleUtil.println("Read " + externalDescriptionsByText.size() + " unique descriptions from external terminology " + externalInputFile.getName() 
				+ " (" + conceptCounter + " concepts)");

		DirectoryReader dr = DirectoryReader.open(luceneIndex);
		IndexSearcher searcher = new IndexSearcher(dr);
		
		File termFolder = new File(outputDirectory, externalInputFile.getName());
		termFolder.mkdirs();

		for (ConceptType ct : ConceptType.values())
		{
			ExecutorService es = Executors.newFixedThreadPool(12);
			if (ct == ConceptType.VHAT)
			{
				continue;
			}
			File outputFolder = new File(termFolder, ct.name());
			outputFolder.mkdir();

			File incomingOutputFile = new File(outputFolder, externalInputFile.getName() + "-" + ct.name() + "-" + "incoming.tsv");
			CSVWriter incomingOutputFileWriter = new CSVWriter(new FileWriter(incomingOutputFile), '\t');
			incomingOutputFileWriter.writeNext(getOutputHeader());
			Stats incomingStats = new Stats();
			List<String[]> incomingResult = Collections.synchronizedList(new ArrayList<String[]>());
			List<String[]> outgoingResult = Collections.synchronizedList(new ArrayList<String[]>());

			File outgoingOutputFile = new File(outputFolder, externalInputFile.getName() + "-" + ct.name() + "-" + "outgoing.tsv");
			CSVWriter outgoingOutputFileWriter = new CSVWriter(new FileWriter(outgoingOutputFile), '\t');
			outgoingOutputFileWriter.writeNext(getOutputHeader());
			Stats outgoingStats = new Stats();

			ConsoleUtil.println("Processing " + externalInputFile.getName() + "-" + ct.name());

			for (VHATConcept vc : cdh_.getVhatConcepts().values())
			{
				if (vc.getConceptSourceFiles().keySet().contains(ct))
				{
					ThreadedCruncher tc = new ThreadedCruncher(incomingResult, outgoingResult, incomingStats, outgoingStats, ct, vc, externalDescriptionsByText,
							externalDescriptionsByConceptId, externalDescriptionsByTextSoundsLike, searcher, termType);
					es.submit(tc);
				}
			}
			
			es.shutdown();
			es.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			
			Collections.sort(incomingResult, new ResultComparator());
			Collections.sort(outgoingResult, new ResultComparator());
			
			for (String[] s : incomingResult)
			{
				incomingOutputFileWriter.writeNext(s);
			}
			for (String[] s : outgoingResult)
			{
				outgoingOutputFileWriter.writeNext(s);
			}
			
			incomingOutputFileWriter.close();
			outgoingOutputFileWriter.close();

			ConsoleUtil.println(incomingOutputFile.getName().substring(0, incomingOutputFile.getName().length() - 4) + " Stats:");
			ConsoleUtil.println(incomingStats.toString());

			Files.write(new File(incomingOutputFile.getParentFile(), incomingOutputFile.getName().substring(0, incomingOutputFile.getName().length() - 4) + "-stats.txt")
					.toPath(), incomingStats.toString().getBytes(), StandardOpenOption.CREATE);

			ConsoleUtil.println(outgoingOutputFile.getName().substring(0, outgoingOutputFile.getName().length() - 4) + " Stats:");
			ConsoleUtil.println(outgoingStats.toString());
			Files.write(new File(outgoingOutputFile.getParentFile(), outgoingOutputFile.getName().substring(0, outgoingOutputFile.getName().length() - 4) + "-stats.txt")
					.toPath(), outgoingStats.toString().getBytes(), StandardOpenOption.CREATE);
		}

	}

	class ThreadedCruncher implements Runnable
	{
		List<String[]> incomingResult_;
		List<String[]> outgoingResult_;

		Stats incomingStats_;
		Stats outgoingStats_;

		VHATConcept chdrVhatConcept_;
		ConceptType ct_;
		Terminology termType_;
		HashMap<String, HashMap<String, DescriptionInfo>> externalDescriptionsByDesc_;
		HashMap<String, ArrayList<DescriptionInfo>> externalDescriptionsByConceptId_;
		HashMap<String, ArrayList<DescriptionInfo>> externalDescriptionsByTextSoundsLike_;
		IndexSearcher luceneIndex_;

		protected ThreadedCruncher(List<String[]> incomingResult, List<String[]> outgoingResult, Stats incomingStats, Stats outgoingStats,
				ConceptType currentConceptType, VHATConcept chdrVhatConcept, HashMap<String, HashMap<String, DescriptionInfo>> externalDescriptionsByDesc,
				HashMap<String, ArrayList<DescriptionInfo>> externalDescriptionsByConceptId,
				HashMap<String, ArrayList<DescriptionInfo>> externalDescriptionsByTextSoundsLike,
				IndexSearcher luceneIndex, 
				Terminology termType)
		{
			this.incomingResult_ = incomingResult;
			this.outgoingResult_ = outgoingResult;
			this.incomingStats_ = incomingStats;
			this.outgoingStats_ = outgoingStats;
			this.ct_ = currentConceptType;
			this.chdrVhatConcept_ = chdrVhatConcept;
			this.externalDescriptionsByDesc_ = externalDescriptionsByDesc;
			this.externalDescriptionsByConceptId_ = externalDescriptionsByConceptId;
			this.externalDescriptionsByTextSoundsLike_ = externalDescriptionsByTextSoundsLike;
			this.luceneIndex_ = luceneIndex;
			this.termType_ = termType;
		}

		@Override
		public void run()
		{
			try
			{
				if ((incomingResult_.size() + outgoingResult_.size()) % 100 == 0)
				{
					ConsoleUtil.showProgress();
				}
				int added = 0;
				for (Concept rel : chdrVhatConcept_.getIncomingRels())
				{
					if (rel.getType().equals(ct_))
					{
						incomingResult_.add(crunch(rel, incomingStats_));
						added++;
						incomingStats_.incChdrItemCount();
					}
				}
				if (added == 0 && chdrVhatConcept_.getConceptSourceFiles().get(ct_).contains(CHDR.DIRECTION.Incoming))
				{
					incomingResult_.add(crunch(null, incomingStats_));
					incomingStats_.incChdrItemCount();
				}

				added = 0;
				for (Concept rel : chdrVhatConcept_.getOutgoingRels())
				{
					if (rel.getType().equals(ct_))
					{
						outgoingResult_.add(crunch(rel, outgoingStats_));
						added++;
						outgoingStats_.incChdrItemCount();
					}
				}
				if (added == 0 && chdrVhatConcept_.getConceptSourceFiles().get(ct_).contains(CHDR.DIRECTION.Outgoing))
				{
					outgoingResult_.add(crunch(null, outgoingStats_));
					outgoingStats_.incChdrItemCount();
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

		private String[] crunch(Concept chdrLinkedConcept, Stats stats) throws Exception
		{
			ArrayList<String> errors = new ArrayList<>();
			ArrayList<String> otherNotes = new ArrayList<>();
			String vhatCHDRDescription = chdrVhatConcept_.getDescription();
			String vhatDescriptionId = chdrVhatConcept_.getId();
			String currentMappingId = (chdrLinkedConcept == null ? "" : chdrLinkedConcept.getId());
			String currentMappingDescription;
			try
			{
				currentMappingDescription = (chdrLinkedConcept == null ? "" : chdrLinkedConcept.getDescription());
			}
			catch (Exception e)
			{
				Iterator<String> it = chdrLinkedConcept.getDescriptions().iterator();
				currentMappingDescription = it.next();
				while (it.hasNext())
				{
					errors.add("Mediation code '" + currentMappingId + "'  has inconsistent text - also has '" + it.next() + "'");
					stats.incChdrMediationTextInconsistency();
				}
			}

			DescriptionInfo vhatDescInfo = vhatDescriptions_.get(vhatDescriptionId);
			String vhatDescription;
			String vhatConceptId;

			if (vhatDescInfo == null)
			{
				errors.add("The ID '" + vhatDescriptionId + "' from the CHDR data file does not exist in VHAT");
				stats.incInvalidVHATIdentifier();
				vhatDescription = "";
				vhatConceptId = "";
			}
			else
			{
				vhatDescription = vhatDescInfo.getDescription();
				vhatConceptId = vhatDescInfo.getConceptId();

				if (!vhatDescription.equals(vhatCHDRDescription))
				{
					errors.add("The description in CHDR for VHAT Description ID '" + vhatDescriptionId + "' does not equal the actual value in VHAT");
					stats.incChdrVhatTextInconsistency();
				}
			}

			String matchText = null;
			double matchAccuracy = -1d;
			String matchConceptId = null;

			if (currentMappingDescription.length() > 0)
			{
				// Already mapped
				boolean superValidate =  ((ct_ == ConceptType.REACTANT && termType_ == Terminology.NDFRT) 
						|| (ct_ == ConceptType.REACTION && termType_ == Terminology.SCT)
						|| (ct_ == ConceptType.DRUG && termType_ == Terminology.RxNORM));
				
				//First - validate the Mediation text of the mapped concept
				HashMap<String, DescriptionInfo> result = externalDescriptionsByDesc_.get(currentMappingDescription.toLowerCase());
				if (result == null)
				{
					//Mediation text is invalid for some reason...
					if (superValidate)
					{
						// no exact match - lookup by ID - 
						ArrayList<DescriptionInfo> result2 = externalDescriptionsByConceptId_.get(currentMappingId);
						if (result2 != null && result2.size() > 0)
						{
							String temp = "";
							for (DescriptionInfo di : result2)
							{
								temp += "'" + di.getDescription() + "', ";
							}
							temp = temp.substring(0, temp.length() - 2);
	
							errors.add("CHDR currently has a mapping to mediation code '" + currentMappingId + "' with the text '" + currentMappingDescription + "' but "
									+ "that is not the correct text for the concept.  Concept '" + currentMappingId + "' has descriptions of: " + temp);
							stats.incChdrMediationTextInconsistency();
						}
						else
						{
							stats.incInvalidMediationIdentifier();
							errors.add("The mapping code '" + currentMappingId + "' was not found");
						}
					}
					else
					{
						//This isn't the right terminology to validate the mediation mappings - do nothing
					}
				}
				else
				{
					// 1 or more exact match.
					// See if the conceptID is the same
					{
						DescriptionInfo di = result.get(currentMappingId);
						if (di != null)
						{
							matchConceptId = di.getConceptId();
							matchText = di.getDescription();
							matchAccuracy = matchText.equalsIgnoreCase(vhatDescription) ? 1d : 0.9d;
						}
					}
					for (DescriptionInfo di : result.values())
					{
						if (matchConceptId == null)
						{
							matchConceptId = di.getConceptId();
							matchText = di.getDescription();
							matchAccuracy = matchText.equalsIgnoreCase(vhatDescription) ? 1d : .9d;
						}
						else
						{
							if (!matchConceptId.equals(di.getConceptId()))
							{
								otherNotes.add("The concept '" + di.getConceptId() + "' also matches");
							}
						}
					}
					
					if (matchAccuracy == 1d)
					{
						stats.incExistingExactMatch();
					}
					else
					{
						//So, many times, they listed the FSN in CHDR, but that wasn't the best matching string within the concept.
						//If there is a better matching string, list that instead.
						ArrayList<DescriptionInfo> desc = externalDescriptionsByConceptId_.get(matchConceptId);
						if (desc != null)
						{
							for (DescriptionInfo di : desc)
							{
								if (di.getDescription().toLowerCase().equals(vhatDescription.toLowerCase()))
								{
									matchText = di.getDescription();
									matchAccuracy = .95d;
									break;
								}
							}
						}
						
						if (matchAccuracy == 0.95d)
						{
							stats.incExistingExactMatchToOtherDescription();
						}
						else
						{
							stats.incExistingNonExactMatch();
						}
					}
					
					if (matchConceptId.equals(currentMappingId))
					{
						stats.incVerifiedExactMatch();
					}
					else
					{
						if (superValidate)
						{
							errors.add("The text value matches, but the conceptID does not match.");
							stats.incMisMatchedConceptId();
						}
						else
						{
							otherNotes.add("The text value matches, but the conceptID does not match (this isn't from the same mediation terminology).");
						}
					}
				}
			}
			else
			{
				// No mapping currently exists
				HashMap<String, DescriptionInfo> result = externalDescriptionsByDesc_.get(vhatDescription.toLowerCase());

				if (result == null)
				{
					// no exact match - try a sounds-like match
					ArrayList<DescriptionInfo> soundsLikeResult = externalDescriptionsByTextSoundsLike_.get(doubleMetaphone(vhatDescription));
					if (soundsLikeResult != null)
					{
						for (DescriptionInfo di : soundsLikeResult)
						{
							if (matchConceptId == null)
							{
								stats.incNewSoundsLikeMatch();
								matchConceptId = di.getConceptId();
								matchText = di.getDescription();
								matchAccuracy = .8d;
							}
							else
							{
								if (!matchConceptId.equals(di.getConceptId()))
								{
									otherNotes.add("The concept '" + di.getConceptId() + "' also has a sounds-like match");
								}
							}
						}
					}
					
					if (matchConceptId == null)
					{
						// Still no match - try a lucene search
						String matchedDescription = luceneSearch(vhatDescription, luceneIndex_);
						if (matchedDescription != null)
						{
							int searchTokens = tokenize(vhatDescription).size();
							int matchTokens = tokenize(matchedDescription).size();
							int diff = matchTokens - searchTokens; 
							if (diff < 3)
							{
								HashMap<String, DescriptionInfo> luceneResult = externalDescriptionsByDesc_.get(matchedDescription);
								stats.incNewLuceneMatch();
								for (DescriptionInfo di : luceneResult.values())
								{
									if (matchConceptId == null)
									{
										matchConceptId = di.getConceptId();
										matchText = di.getDescription();
										matchAccuracy = (diff == 0  ? .75d : (diff == 1 ? .7d : .65d));
									}
									else
									{
										if (!matchConceptId.equals(di.getConceptId()))
										{
											otherNotes.add("The concept '" + di.getConceptId() + "' also matches");
										}
									}
								}
							}
						}
					}

					if (matchConceptId == null)
					{
						// Still no match - find the closest match based on a levenshtein algorithm
						double smallestRatio = Double.MAX_VALUE;
						String bestString = null;

						String cacheValue = dlaCache.get(vhatDescription.toLowerCase());
						if (cacheValue != null)
						{
							smallestRatio = levenshtein(cacheValue, vhatDescription);
							bestString = cacheValue;
						}
						else
						{
							for (String s : externalDescriptionsByDesc_.keySet())
							{
								double differenceRatio = levenshtein(s, vhatDescription);
								if (differenceRatio < smallestRatio)
								{
									smallestRatio = differenceRatio;
									bestString = s;
								}
							}
							dlaCache.put(vhatDescription.toLowerCase(), bestString);
						}
						if (smallestRatio < 0.35)
						{
							HashMap<String, DescriptionInfo> fuzzyResult = externalDescriptionsByDesc_.get(bestString);
							stats.incNewFuzzyMatches();
							for (DescriptionInfo di : fuzzyResult.values())
							{
								if (matchConceptId == null)
								{
									matchConceptId = di.getConceptId();
									matchText = di.getDescription();
									matchAccuracy = 0.64d - smallestRatio;
								}
								else
								{
									if (!matchConceptId.equals(di.getConceptId()))
									{
										otherNotes.add("The concept '" + di.getConceptId() + "' also matches");
									}
								}
							}
						}
					}
				}
				else
				{
					stats.incNewExactMatch();
					// 1 or more exact match
					for (DescriptionInfo di : result.values())
					{
						if (matchConceptId == null)
						{
							matchConceptId = di.getConceptId();
							matchText = di.getDescription();
							matchAccuracy = 1d;
						}
						else
						{
							if (!matchConceptId.equals(di.getConceptId()))
							{
								otherNotes.add("The concept '" + di.getConceptId() + "' also matches");
							}
						}
					}
				}
			}

			String detectedErrors = "";
			for (String s : errors)
			{
				detectedErrors += s + "; ";
			}
			if (detectedErrors.length() > 0)
			{
				detectedErrors = detectedErrors.substring(0, detectedErrors.length() - 2);
			}

			stats.incErrorCount(errors.size());

			String notes = "";
			for (String s : otherNotes)
			{
				notes += s + "; ";
			}
			if (notes.length() > 0)
			{
				notes = notes.substring(0, notes.length() - 2);
			}

			stats.incOtherNotesCount(otherNotes.size());
			
			if (matchConceptId == null)
			{
				stats.incNoMatch();
			}
			
			MatchType matchType;
			
			if (matchAccuracy == 1d)
			{
				if (currentMappingId == null || currentMappingId.length() == 0)
				{
					matchType = MatchType.NEW_EXACT_MATCH;
				}
				else
				{
					matchType = MatchType.EXACT_MATCH;
				}
			}
			else if (matchAccuracy == .95d)
			{
				matchType = MatchType.EXACT_MATCH_OTHER_DESC;
			}
			else if (matchAccuracy == .9d)
			{
				matchType = MatchType.EXISTING_NO_MATCH;
			}
			else if (matchAccuracy == .8d)
			{
				matchType = MatchType.NEW_SOUNDS_LIKE;
			}
			else if (matchAccuracy == .75d)
			{
				matchType = MatchType.NEW_LUCENE;
			}
			else if (matchAccuracy == .70d)
			{
				matchType = MatchType.NEW_LUCENE_1;
			}
			else if (matchAccuracy == .65d)
			{
				matchType = MatchType.NEW_LUCENE_2;
			}
			else if (matchAccuracy < .65d)
			{
				matchType = MatchType.SIMILAR;
			}
			else if (matchAccuracy < 0d)
			{
				matchType = MatchType.NO_MATCH;
			}
			else
			{
				throw new RuntimeException("Oops - currently set to " + matchAccuracy);
			}

			return new String[] { vhatDescriptionId, vhatDescription, vhatCHDRDescription, vhatConceptId, currentMappingId, currentMappingDescription, matchText,
					matchAccuracy + "", matchType.getDescription(), matchConceptId, detectedErrors, notes };
		}
	}
	
	private String luceneSearch(String searchString, IndexSearcher indexSearcher) throws IOException
	{
		//Lucene scores things based on relevance... where it thinks a hit in a longer document might be 
		//more relevant than a hit in a shorter document.  Since the query I'm passing in requires 
		//matches on each term - the best result for us would be the shortest result.
		//So, grab the top 25 (arbitrary) results from lucene, and then pick the shortest one.
		Query q = buildQuery(searchString);
		TopScoreDocCollector collector = TopScoreDocCollector.create(25, true);
		indexSearcher.search(q, collector);
		String bestResult = null;
		ScoreDoc[] result = collector.topDocs().scoreDocs;
		for (int i = 0; i < result.length; i++)
		{
			String temp = indexSearcher.doc(result[i].doc).get(luceneField);
			if (bestResult == null)
			{
				bestResult = temp;
			}
			else if (temp.length() < bestResult.length())
			{
				bestResult = temp;
			}
		}
		return bestResult;
	}
	
	private List<String> tokenize(String searchString) throws IOException
	{
		StringReader textReader = new StringReader(searchString.toLowerCase());
		TokenStream tokenStream = wa.tokenStream(luceneField, textReader);
		tokenStream.reset();
		List<String> terms = new ArrayList<>();
		CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
		
		while (tokenStream.incrementToken())
		{
			terms.add(charTermAttribute.toString());
		}
		textReader.close();
		tokenStream.close();
		return terms;
	}
	
	private Query buildQuery(String searchString) throws IOException
	{
		List<String> terms = tokenize(searchString);
		BooleanQuery bq = new BooleanQuery();
		for (String s : terms)
		{
			bq.add(new TermQuery(new Term(luceneField, s)), Occur.MUST);
		}
		
		return bq;
	}

	public String doubleMetaphone(String input)
	{
		StringBuilder sb = new StringBuilder();

		for (String s : input.split("\\s+"))
		{
			sb.append(dm_.doubleMetaphone(s));
			sb.append(" ");
		}
		sb.setLength(sb.length() - 1);
		return sb.toString();
	}
	
	public double levenshtein(String a, String b)
	{
		if (Math.max(a.length(), b.length()) - Math.min(a.length(), b.length()) > 20)
		{
			//If they are more than 20 characters apart, just skip the calculation, and return a big number...
			return 0.9;
		}
		
		return ((double)StringUtils.getLevenshteinDistance(a.toLowerCase(), b.toLowerCase()))
				/ (Math.max(a.length(), b.length()));
		//This was too slow - shows no improvement
//		return ((double) dla_.execute(a.toLowerCase(), b.toLowerCase()))
//			/ (Math.max(a.length(), b.length()));
	}

	public static String[] getOutputHeader()
	{
		return new String[] { "VHAT Description ID", "VHAT Description", "VHAT Description in CHDR", "VHAT Concept ID", "Current Mapping ID",
				"Current Mapping Description", "Match Text", "Match Accuracy", "Match Type", "Match Concept ID", "Detected Errors", "Other Notes" };
	}

	public static void main(String[] args) throws MojoExecutionException, MojoFailureException
	{
		CHDRAnalysisMojo i = new CHDRAnalysisMojo();
		i.outputDirectory = new File("../chdr-econcept/target");
		i.inputFile = new File("../chdr-econcept/target/generated-resources/data");
		i.vhatInputFile = new File("../chdr-econcept/target/generated-resources/data/VHAT");
		i.externalInputFiles = new File[] { new File("../chdr-econcept/target/generated-resources/data/NDF-RT"),
				new File("../chdr-econcept/target/generated-resources/data/RxNorm"), new File("../chdr-econcept/target/generated-resources/data/SCT") };

		i.execute();

	}
}
