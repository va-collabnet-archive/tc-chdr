package gov.va.med.term.chdr.chdrReader;

import gov.va.oia.terminology.converters.sharedUtils.ConsoleUtil;
import java.io.File;
import java.util.HashMap;

public class CHDRDataHolder
{
	private HashMap<String, VHATConcept> vhatConcepts = new HashMap<>();
	private HashMap<String, Concept> drugProducts = new HashMap<>();
	private HashMap<String, Concept> reactants = new HashMap<>();
	private HashMap<String, Concept> reactions = new HashMap<>();
	private String version;

	public CHDRDataHolder(File folderContainingDataFiles) throws Exception
	{
		/*
		 * Expected File Naming Patterns:
		 * Drug Products Release 61-Outgoing.csv
		 * Drug Products Release 61-Incoming.csv
		 * Reactants Release 61-Outgoing.csv
		 * Reactants Release 61-Incoming.csv
		 * Reactions Release 61-Outgoing.csv
		 * Reactions Release 61-Incoming.csv
		 * 
		 * or
		 * 
		 * [stuff] Drug Products Release 61.xls
		 * [stuff] Reactants Release 61.xls
		 * [stuff] Reactions Release 61.xls
		 */

		for (File f : folderContainingDataFiles.listFiles())
		{
			if (f.isFile() && (f.getName().toLowerCase().endsWith(".csv") || f.getName().toLowerCase().endsWith(".xls")))
			{
				ConsoleUtil.println("Processing '" + f.getName() + "'");
				int releasePos = f.getName().indexOf("Release");
				int directionPos = f.getName().indexOf("-");

				String name = f.getName().substring(0, releasePos).trim();
				String releaseInfo = f.getName().substring(releasePos, (directionPos > 0 ? directionPos : f.getName().indexOf('.'))).trim();

				if (version == null)
				{
					version = releaseInfo;
				}
				else
				{
					if (!version.equalsIgnoreCase(releaseInfo))
					{
						throw new Exception("Release info varies!");
					}
				}

				HashMap<String, Concept> map;
				ConceptType type;
				if (name.toLowerCase().endsWith("drug products"))
				{
					map = drugProducts;
					type = ConceptType.DRUG;
				}
				else if (name.toLowerCase().endsWith("reactants"))
				{
					map = reactants;
					type = ConceptType.REACTANT;
				}
				else if (name.toLowerCase().endsWith("reactions"))
				{
					map = reactions;
					type = ConceptType.REACTION;
				}
				else
				{
					throw new Exception("Unexpected file name");
				}

				for (CHDR chdr : CHDRParser.readData(f))
				{
					VHATConcept vhat = vhatConcepts.get(chdr.VUID);
					if (vhat == null)
					{
						vhat = new VHATConcept(chdr.VUID, chdr.VUIDText, type, chdr.direction);
						vhatConcepts.put(chdr.VUID, vhat);
					}
					else
					{
						vhat.addDescription(chdr.VUIDText, type, chdr.direction);
					}

					if (chdr.MediationCode != null && chdr.MediationCode.trim().length() > 0)
					{
						Concept other = map.get(chdr.MediationCode);
						if (other == null)
						{
							other = new Concept(chdr.MediationCode, chdr.MediationText, type);
							map.put(other.getId(), other);
						}
						else
						{
							other.addDescription(chdr.MediationText);
						}

						// Tie the two together
						if (chdr.direction == CHDR.DIRECTION.Incoming)
						{
							vhat.addIncomingRel(other);
						}
						else if (chdr.direction == CHDR.DIRECTION.Outgoing)
						{
							vhat.addOutgoingRel(other);
						}
						else
						{
							throw new Exception("Unexpected Direction info!");
						}
					}
					else
					{
						if (chdr.MediationText != null && chdr.MediationText.trim().length() > 0)
						{
							ConsoleUtil.printErrorln("text but no code?");
						}
					}
				}
			}
		}
		ConsoleUtil.println("Read in " + vhatConcepts.size() + " VHAT concepts");
		ConsoleUtil.println("Read in " + drugProducts.size() + " Drug Product concepts");
		ConsoleUtil.println("Read in " + reactants.size() + " Reactant concepts");
		ConsoleUtil.println("Read in " + reactions.size() + " Reaction concepts");
	}

	public HashMap<String, VHATConcept> getVhatConcepts()
	{
		return vhatConcepts;
	}

	public HashMap<String, Concept> getDrugProducts()
	{
		return drugProducts;
	}

	public HashMap<String, Concept> getReactants()
	{
		return reactants;
	}

	public HashMap<String, Concept> getReactions()
	{
		return reactions;
	}

	public String getVersion()
	{
		return version;
	}

	public static void main(String[] args) throws Exception
	{
		new CHDRDataHolder(new File("/mnt/d/Documents/Desktop/CHDR Data/"));
	}
}
