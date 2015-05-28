package gov.va.med.term.chdr.mojo;

import gov.va.med.term.chdr.chdrReader.CHDRDataHolder;
import gov.va.med.term.chdr.chdrReader.Concept;
import gov.va.med.term.chdr.chdrReader.ConceptType;
import gov.va.med.term.chdr.chdrReader.VHATConcept;
import gov.va.med.term.chdr.propertyTypes.PT_Annotations;
import gov.va.med.term.chdr.propertyTypes.PT_ContentVersion;
import gov.va.med.term.chdr.propertyTypes.PT_ContentVersion.ContentVersion;
import gov.va.med.term.chdr.propertyTypes.PT_IDs;
import gov.va.med.term.chdr.propertyTypes.PT_Refsets;
import gov.va.med.term.chdr.propertyTypes.PT_Relations;
import gov.va.oia.terminology.converters.sharedUtils.ConsoleUtil;
import gov.va.oia.terminology.converters.sharedUtils.EConceptUtility;
import gov.va.oia.terminology.converters.sharedUtils.EConceptUtility.DescriptionType;
import gov.va.oia.terminology.converters.sharedUtils.stats.ConverterUUID;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.dwfa.cement.ArchitectonicAuxiliary;
import org.dwfa.util.id.Type5UuidFactory;
import org.ihtsdo.etypes.EConcept;
import org.ihtsdo.tk.dto.concept.component.description.TkDescription;
import org.ihtsdo.tk.dto.concept.component.identifier.TkIdentifier;
import org.ihtsdo.tk.dto.concept.component.refex.TkRefexAbstractMember;
import org.ihtsdo.tk.dto.concept.component.refex.type_string.TkRefsetStrMember;
import org.ihtsdo.tk.dto.concept.component.relationship.TkRelationship;

/**
 * Goal which converts CHDR data into the workbench jbin format
 * 
 * @goal convert-CHDR-data
 * 
 * @phase process-sources
 */
public class CHDRImportMojo extends AbstractMojo
{
	private static final String chdrNamespaceBaseSeed = "gov.va.med.term.vhat.chdr";
	private static final String vhatNamespaceBaseSeed = "gov.va.med.term.vhat";	
	private UUID vhatNamespaceUUID = ConverterUUID.createNamespaceUUIDFromString(null, vhatNamespaceBaseSeed);
	private UUID vhatIDUUIDAuthority;
	
	private PT_ContentVersion contentVersion;
	private PT_IDs ids;
	private PT_Relations rels;
	private PT_Annotations attributes;
	private PT_Refsets refsets;
	private EConceptUtility eConceptUtil_;
	private DataOutputStream dos;

	private HashMap<String, UUIDTypeRef> vhatVuidConceptToUUID = new HashMap<String, UUIDTypeRef>();
	
	private int dupeRelCount = 0;
	private int dupeRelCountWithAnnotation = 0;
	
	private HashMap<String, String> sctCodesMissingMapping = new HashMap<>();
	private HashMap<String, String> rxnCodesMissingMapping = new HashMap<>();
	//They provide CUI codes, but mostly link to NDF-RT concepts via CUI codes.  So go directly to NDF-RT, since we don't have NDF-RT from UMLS at the moment.
	//private HashMap<String, String> umlsCodesMissingMapping = new HashMap<>();
	private HashMap<String, String> ndfRtCodesMissingMapping = new HashMap<>();
	
	private HashMap<String, UUID> chdrIDToUUID = new HashMap<>();

	/**
	 * Where to put the output file.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */

	private File outputDirectory;

	/**
	 * Location of source data file. Expected to be a directory.
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
	 * Location of sct jbin data file. Expected to be a directory.
	 * 
	 * @parameter
	 * @required
	 */
	private File sctInputFile;
	
	/**
	 * Location of RxNorm jbin data file. Expected to be a directory.
	 * 
	 * @parameter
	 * @required
	 */
	private File rxnInputFile;
	
//	/**
//	 * Location of UMLS jbin data file. Expected to be a directory.
//	 * 
//	 * @parameter
//	 * @required
//	 */
//	private File umlsInputFile;
	
	/**
	 * Location of NDF-RT jbin data file. Expected to be a directory.
	 * 
	 * @parameter
	 * @required
	 */
	private File ndfRtInputFile;


	/**
	 * Loader version number Use parent because project.version pulls in the version of the data file, which I don't want.
	 * 
	 * @parameter expression="${project.parent.version}"
	 * @required
	 */
	private String loaderVersion;

	/**
	 * Content version number
	 * 
	 * @parameter expression="${project.version}"
	 * @required
	 */
	private String releaseVersion;
	
	@Override
	public void execute() throws MojoExecutionException
	{
		try
		{
			if (!outputDirectory.exists())
			{
				outputDirectory.mkdirs();
			}
			
			File touch = new File(outputDirectory, "CHDREConcepts.jbin");
			dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(touch)));
			eConceptUtil_ = new EConceptUtility(chdrNamespaceBaseSeed, "CHDR Path", dos, System.currentTimeMillis());
			contentVersion = new PT_ContentVersion();
			ids = new PT_IDs();
			rels = new PT_Relations();
			attributes = new PT_Annotations();
			refsets = new PT_Refsets();
			
			//This is how the UUID for the VHAT ID type is created.
			vhatIDUUIDAuthority = ConverterUUID.createNamespaceUUIDFromString(vhatNamespaceUUID, ids.getPropertyTypeDescription() + ":VUID");

			CHDRDataHolder cdh = new CHDRDataHolder(inputFile);

			ConsoleUtil.println("Reading VHAT concepts file");

			// Read in the VHAT data
			ArrayList<EConcept> vhatConcepts = new ArrayList<>();
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

			while (in.available() > 0)
			{
				vhatConcepts.add(new EConcept(in));
			}
			in.close();

			ConsoleUtil.println("Read " + vhatConcepts.size() + " concepts from the VHAT jbin file");

			UUID vhatRootUUID = null;

			for (EConcept c : vhatConcepts)
			{
				if (c.getDescriptions() == null 
						&& (c.getPrimordialUuid().equals(eConceptUtil_.pathOriginRefSetUUID_) || c.getPrimordialUuid().equals(eConceptUtil_.pathRefSetUUID_)))
				{
					//ignore these two - they don't have descriptions, but that is ok...
					continue;
				}
				String vuid = null;
				if (c.getConceptAttributes() != null && c.getConceptAttributes().getAdditionalIdComponents() != null)
				{
					for (TkIdentifier id : c.getConceptAttributes().getAdditionalIdComponents())
					{
						if (id.getAuthorityUuid().equals(vhatIDUUIDAuthority))
						{
							vuid = id.getDenotation().toString();
							break;
						}
					}
				}

				String preferredDesc = null;
				for (TkDescription desc : c.getDescriptions())
				{
					if (desc.getTypeUuid().equals(eConceptUtil_.fullySpecifiedNameUuid_))
					{
						preferredDesc = desc.getText();
						if (preferredDesc.equals("VHAT"))
						{
							vhatRootUUID = c.getPrimordialUuid();
						}
						break;
					}
				}
				if (preferredDesc == null)
				{
					throw new RuntimeException("Missing preferred description in VHAT jbin file - something is wrong");
				}

				if (vuid != null)
				{
					vhatVuidConceptToUUID.put(vuid, new UUIDTypeRef(c.getPrimordialUuid(), WBType.Concept, preferredDesc));
				}

				// Also need to go through all of the descriptions, and map those.
				for (TkDescription d : c.getDescriptions())
				{
					if (d != null && d.getAdditionalIdComponents() != null)
					{
						String descVuid = null;
						UUID descUUID = null;
						for (TkIdentifier id : d.getAdditionalIdComponents())
						{
							if (id.getAuthorityUuid().equals(vhatIDUUIDAuthority))
							{
								descVuid = id.getDenotation().toString();
								descUUID = d.getPrimordialComponentUuid();
								break;
							}
						}
						if (descVuid != null)
						{
							vhatVuidConceptToUUID.put(descVuid, new UUIDTypeRef(descUUID, WBType.Description, d.getText(), c.getPrimordialUuid()));
						}
					}
				}
			}

			if (vhatRootUUID == null)
			{
				throw new RuntimeException("Missing root VHAT concept in jbin file - something is wrong");
			}

			vhatConcepts = null;

			ConsoleUtil.println("Indexed UUIDs and descriptions from VHAT file");
			ConsoleUtil.println("Indexing SCT Concepts");
			
			// Read in the SCT data
			HashMap<String, UUID> sctConcepts = new HashMap<>();
			UUID sctIDType = UUID.fromString("0418a591-f75b-39ad-be2c-3ab849326da9");  //"SNOMED integer id"
			in = new DataInputStream(new FileInputStream(sctInputFile.listFiles(new FilenameFilter()
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

			while (in.available() > 0)
			{
				if (sctConcepts.size() % 1000 == 0)
				{
					ConsoleUtil.showProgress();
				}
				EConcept concept = new EConcept(in);
				
				if (concept.getConceptAttributes() != null && concept.getConceptAttributes().getAdditionalIdComponents() != null)
				{
					for (TkIdentifier id : concept.getConceptAttributes().getAdditionalIdComponents())
					{
						if (sctIDType.equals(id.getAuthorityUuid()))
						{
							//Store these by SCTID, because there is no reliable way to generate a UUID from a SCTID.
							sctConcepts.put(id.getDenotation().toString(), concept.getPrimordialUuid());
							break;
						}
					}
				}
			}
			in.close();
			ConsoleUtil.println("Indexed UUIDs from SCT file - read " + sctConcepts.size() + " concepts");
			
			ConsoleUtil.println("Indexing RxNorm Concepts");
			
			// Read in the RxNorm data
			HashSet<UUID> rxNormConcepts = new HashSet<>();
			in = new DataInputStream(new FileInputStream(rxnInputFile.listFiles(new FilenameFilter()
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

			while (in.available() > 0)
			{
				if (rxNormConcepts.size() % 1000 == 0)
				{
					ConsoleUtil.showProgress();
				}
				rxNormConcepts.add(new EConcept(in).getPrimordialUuid());
			}
			in.close();
			ConsoleUtil.println("Indexed UUIDs from RxNorm file - read " + rxNormConcepts.size() + " concepts");
			
//			ConsoleUtil.println("Indexing UMLS Concepts");
//			
//			// Read in the UMLS data
//			HashSet<UUID> umlsConcepts = new HashSet<>();
//			in = new DataInputStream(new FileInputStream(umlsInputFile.listFiles(new FilenameFilter()
//			{
//				@Override
//				public boolean accept(File dir, String name)
//				{
//					if (name.endsWith(".jbin"))
//					{
//						return true;
//					}
//					return false;
//				}
//			})[0]));
//
//			while (in.available() > 0)
//			{
//				if (sctConcepts.size() % 1000 == 0)
//				{
//					ConsoleUtil.showProgress();
//				}
//				//I really only need the CUI based concepts, not the AUI based concepts, so I could dig in an check, but I have enough RAM not to care at the moment...
//				umlsConcepts.add(new EConcept(in).getPrimordialUuid());
//			}
//			in.close();
//			ConsoleUtil.println("Indexed UUIDs from UMLS file - read " + umlsConcepts.size() + " concepts");
			
			ConsoleUtil.println("Indexing NDF-RT Concepts");
			
			// Read in the NDF-RT data
			HashMap<String, UUID> ndfRtConcepts = new HashMap<>();
			in = new DataInputStream(new FileInputStream(ndfRtInputFile.listFiles(new FilenameFilter()
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

			while (in.available() > 0)
			{
				if (ndfRtConcepts.size() % 1000 == 0)
				{
					ConsoleUtil.showProgress();
				}
				
				EConcept tempConcept = new EConcept(in);
				//Dig through, to find the 'UMLS CUI' string extension'
				String cuiCode = null;
				
				if (tempConcept.getConceptAttributes() != null && tempConcept.getConceptAttributes().getAnnotations() != null)
				{
					for (TkRefexAbstractMember<?> annotation : tempConcept.getConceptAttributes().getAnnotations())
					{
						//dd7722cd-ebe8-5554-aee3-4b792feb8d98 is the UUID for 'UMLS CUI' in NDF-RT.  could put code here that shows how to do that from the 
						//NDF-RT loader... but not bothering at the moment
						if (annotation instanceof TkRefsetStrMember && annotation.getRefexUuid().equals(UUID.fromString("dd7722cd-ebe8-5554-aee3-4b792feb8d98")))
						{
							cuiCode = ((TkRefsetStrMember)annotation).getString1();
							break;
						}
					}
				}
				
				if (cuiCode != null)
				{
					ndfRtConcepts.put(cuiCode, tempConcept.getPrimordialUuid());
				}
			}
			in.close();
			ConsoleUtil.println("Indexed UUIDs from NDF-RT file - read " + ndfRtConcepts.size() + " concepts");

			EConcept metaDataRoot = eConceptUtil_.createConcept("CHDR Metadata", ArchitectonicAuxiliary.Concept.ARCHITECTONIC_ROOT_CONCEPT.getPrimoridalUid());
			metaDataRoot.writeExternal(dos);

			eConceptUtil_.loadMetaDataItems(contentVersion, metaDataRoot.getPrimordialUuid(), dos);
			eConceptUtil_.loadMetaDataItems(ids, metaDataRoot.getPrimordialUuid(), dos);
			eConceptUtil_.loadMetaDataItems(rels, metaDataRoot.getPrimordialUuid(), dos);
			eConceptUtil_.loadMetaDataItems(attributes, metaDataRoot.getPrimordialUuid(), dos);
			eConceptUtil_.loadMetaDataItems(refsets, metaDataRoot.getPrimordialUuid(), dos);
			
			HashSet<String> missingIds = new HashSet<String>();

			EConcept chdr = refsets.getRefsetIdentityParent();  //"CHDR Refsets" concept
			eConceptUtil_.addStringAnnotation(chdr, cdh.getVersion(), ContentVersion.VERSION.getProperty().getUUID(), false);
			eConceptUtil_.addStringAnnotation(chdr, releaseVersion, contentVersion.RELEASE.getUUID(), false);
			eConceptUtil_.addStringAnnotation(chdr, loaderVersion, contentVersion.LOADER_VERSION.getUUID(), false);
			eConceptUtil_.addDescription(chdr, "Clinical Health Data Repository", DescriptionType.SYNONYM, true, null, null, false);
			// Also hang it under vhat root
			eConceptUtil_.addRelationship(chdr, vhatRootUUID);
			
			ConsoleUtil.println("Metadata load stats");
			for (String line : eConceptUtil_.getLoadStats().getSummary())
			{
				ConsoleUtil.println(line);
			}
			eConceptUtil_.clearLoadStats();

			// Strip out any concepts with no mapping
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputDirectory, "VUIDs in CHDR with no mapping.tsv")));
			bw.write("VUID\tCHDR Description" + System.getProperty("line.separator"));
			int noMapping = 0;
			Iterator<VHATConcept> conceptIter = cdh.getVhatConcepts().values().iterator();
			while (conceptIter.hasNext())
			{
				VHATConcept c = conceptIter.next();
				if (c.hasNoRels())
				{
					noMapping++;
					bw.write(c.getId() + "\t" + Arrays.deepToString(c.getDescriptions().toArray(new String[0])) + System.getProperty("line.separator"));
					conceptIter.remove();
				}
			}

			bw.close();
			ConsoleUtil.println("CHDR concepts with no mapping: " + noMapping + " - logged to 'VUIDs in CHDR with no mapping.tsv' and ignored");

			// Create the CHDR refset (all VUIDs mentioned in the data files)
			EConcept chdrAllConcepts = refsets.getConcept(PT_Refsets.Refsets.ALL.getProperty());

			for (VHATConcept c : cdh.getVhatConcepts().values())
			{
				UUIDTypeRef member = vhatVuidConceptToUUID.get(c.getId());
				UUID memberUUID;
				if (member == null)
				{
					missingIds.add(c.getId());
					memberUUID = makeUUID(c);
				}
				else
				{
					memberUUID = member.getUUID();
				}
				eConceptUtil_.addRefsetMember(chdrAllConcepts, memberUUID, null, makeRefsetUUID("CHDR", c.getId()), true, null);
			}

			ConsoleUtil.println(missingIds.size() + " VUIDs exist in CHDR that do not exist in VHAT");

			// Create the drug products refset
			EConcept drugProducts = refsets.getConcept(PT_Refsets.Refsets.DRUG_PRODUCTS.getProperty());
			for (Concept concept : cdh.getDrugProducts().values())
			{
				UUID memberUUID;
				UUID rxNormCode = Type5UuidFactory.get(Type5UuidFactory.get(null, "gov.va.med.term.RRF.RXN.RxNorm"), "CUI:" + concept.getId());
				if (rxNormConcepts.contains(rxNormCode))
				{
					memberUUID = rxNormCode;
				}
				else
				{
					//only create if necessary
					memberUUID = createRefsetMember(concept, PT_IDs.ID.DRUG_MEDIATION_CODE.getProperty().getUUID());
					rxnCodesMissingMapping.put(concept.getId(), (concept.getDescriptions().size() > 0 ? concept.getDescriptions().iterator().next() : ""));
				}
				eConceptUtil_.addRefsetMember(drugProducts, memberUUID, null, makeRefsetUUID("Drug Products", concept.getId()), true, null);
				chdrIDToUUID.put(concept.getId(), memberUUID);
			}

			// and the Reactants refset
			EConcept reactants = refsets.getConcept(PT_Refsets.Refsets.REACTANTS.getProperty());
			for (Concept concept : cdh.getReactants().values())
			{
				UUID memberUUID;
				//UUID umlsCode = Type5UuidFactory.get(Type5UuidFactory.get(null, "gov.va.med.term.RRF.MR.UMLS"), "CUI:" + concept.getId());
				if (ndfRtConcepts.containsKey(concept.getId()))
				{
					memberUUID = ndfRtConcepts.get(concept.getId());
				}
				else
				{
					memberUUID = createRefsetMember(concept, PT_IDs.ID.REACTANT_MEDIATION_CODE.getProperty().getUUID());
					//umlsCodesMissingMapping.put(concept.getId(), (concept.getDescriptions().size() > 0 ? concept.getDescriptions().iterator().next() : ""));
					ndfRtCodesMissingMapping.put(concept.getId(), (concept.getDescriptions().size() > 0 ? concept.getDescriptions().iterator().next() : ""));
				}
				eConceptUtil_.addRefsetMember(reactants, memberUUID, null, makeRefsetUUID("Reactants", concept.getId()), true, null);
				chdrIDToUUID.put(concept.getId(), memberUUID);
			}

			// and the Reactions refset - these are SCT codes 
			EConcept reactions = refsets.getConcept(PT_Refsets.Refsets.REACTIONS.getProperty());
			for (Concept concept : cdh.getReactions().values())
			{
				UUID memberUUID = null;
				if (sctConcepts.containsKey(concept.getId()))
				{
					memberUUID = sctConcepts.get(concept.getId());
				}
				else
				{
					//Only create if necessary
					memberUUID = createRefsetMember(concept, PT_IDs.ID.REACTION_MEDIATION_CODE.getProperty().getUUID());
					sctCodesMissingMapping.put(concept.getId(), (concept.getDescriptions().size() > 0 ? concept.getDescriptions().iterator().next() : ""));
				}
				eConceptUtil_.addRefsetMember(reactions, memberUUID, null, makeRefsetUUID("Reactions", concept.getId()), true, null);
				chdrIDToUUID.put(concept.getId(), memberUUID);
			}

			// Hang this under VHAT as well
			EConcept chdrPendingRoot = eConceptUtil_.createConcept("CHDR Pending", vhatRootUUID);
			chdrPendingRoot.writeExternal(dos);

			// Create the "missing" concepts, link them to pending
			for (String id : missingIds)
			{
				createVHATConcept(cdh.getVhatConcepts().get(id), chdrPendingRoot.getPrimordialUuid());
			}

			int conceptMatch = 0;
			int descMatch = 0;
			
			HashMap<UUID, EConcept> conceptsToStore = new HashMap<>();

			// Finally, create the relations between the VUID concepts and the targets
			for (VHATConcept c : cdh.getVhatConcepts().values())
			{
				// These should all exist now
				UUIDTypeRef member = vhatVuidConceptToUUID.get(c.getId());

				// This concept should already exist in the DB - these rels will (hopefully) just be merged onto it
				EConcept eConcept = conceptsToStore.get(WBType.Concept == member.getType() ? member.getUUID() : member.getParentConceptUUID());
				if (eConcept == null)
				{
					eConcept = eConceptUtil_.createConcept((WBType.Concept == member.getType() ? member.getUUID() : member.getParentConceptUUID()),
						(Long) null, null);
					conceptsToStore.put(eConcept.getPrimordialUuid(), eConcept);
				}

				if (WBType.Concept == member.getType())
				{
					conceptMatch++;
					for (Concept relConcept : c.getIncomingRels())
					{
						UUID target = chdrIDToUUID.get(relConcept.getId());
						if (target == null)
						{
							throw new RuntimeException("oops");
						}
						eConceptUtil_.addRelationship(eConcept, target, PT_Relations.MediationMapping.INCOMING.getProperty().getUUID(), null);
					}
					for (Concept relConcept : c.getOutgoingRels())
					{
						UUID target = chdrIDToUUID.get(relConcept.getId());
						if (target == null)
						{
							throw new RuntimeException("oops");
						}
						eConceptUtil_.addRelationship(eConcept, target, PT_Relations.MediationMapping.OUTGOING.getProperty().getUUID(), null);
					}
				}
				else if (WBType.Description == member.getType())
				{
					descMatch++;
					for (Concept relConcept : c.getIncomingRels())
					{
						UUID target = chdrIDToUUID.get(relConcept.getId());
						if (target == null)
						{
							throw new RuntimeException("oops");
						}
						//I tried to map these to the UUID of the description that had the Rel, but the WB doesn't allow refsets to descriptions, only concept.
						//So now, I just stick the description and the VUID in as a string annotation on the rel.
						createOrUpdateRel(eConcept, target, PT_Relations.MediationMapping.INCOMING.getProperty().getUUID(), member.getDescription() + " - " + c.getId());
					}
					for (Concept relConcept : c.getOutgoingRels())
					{
						UUID target = chdrIDToUUID.get(relConcept.getId());
						if (target == null)
						{
							throw new RuntimeException("oops");
						}
						createOrUpdateRel(eConcept, target, PT_Relations.MediationMapping.OUTGOING.getProperty().getUUID(), member.getDescription() + " - " + c.getId());
					}
				}
				else
				{
					throw new RuntimeException("oops");
				}
			}
			
			for (EConcept eConcept : conceptsToStore.values())
			{
				eConcept.writeExternal(dos);
			}
			
			//And write out the refset concepts
			eConceptUtil_.storeRefsetConcepts(refsets, dos);

			ConsoleUtil.println("Mappings to descriptions (moved up to concepts): " + descMatch);
			ConsoleUtil.println("Mappings to concepts: " + conceptMatch);
			
			ConsoleUtil.println("Duplicate Relationships Merged: " + dupeRelCount + " and " + dupeRelCountWithAnnotation + " were complete duplicates.");

			dos.flush();
			dos.close();

			for (String line : eConceptUtil_.getLoadStats().getSummary())
			{
				ConsoleUtil.println(line);
			}

			// this could be removed from final release. Just added to help debug editor problems.
			ConsoleUtil.println("Dumping UUID Debug File");
			ConverterUUID.dump(outputDirectory, "chdrUuid");

			ConsoleUtil.println("Writing 'Missing Concept' file");
			bw = new BufferedWriter(new FileWriter(new File(outputDirectory, "VUIDs in CHDR not in VHAT.tsv")));
			bw.write("VUID\tCHDR Description" + System.getProperty("line.separator"));
			for (String id : missingIds)
			{
				Concept c = cdh.getVhatConcepts().get(id);
				bw.write(c.getId() + "\t" + Arrays.deepToString(c.getDescriptions().toArray(new String[] {})) + System.getProperty("line.separator"));
			}
			bw.close();

			ConsoleUtil.println("Writing 'Mismatched descriptions' file");
			bw = new BufferedWriter(new FileWriter(new File(outputDirectory, "Mismatched Descriptions.tsv")));
			bw.write("VUID\tCHDR Description\tVHAT Description" + System.getProperty("line.separator"));
			for (Concept c : cdh.getVhatConcepts().values())
			{
				if (c.getDescriptions().size() > 1)
				{
					System.err.println("Dan goof - " + Arrays.deepToString(c.getDescriptions().toArray(new String[] {})));
				}
				else
				{
					String desc = c.getDescriptions().iterator().next();
					if (!missingIds.contains(c.getId()))
					{
						String vhatDesc = vhatVuidConceptToUUID.get(c.getId()).getDescription();
						if (!desc.equals(vhatDesc))
						{
							bw.write(c.getId() + "\t" + desc + "\t" + vhatDesc + System.getProperty("line.separator"));
						}
					}
				}
			}
			bw.close();
			
			if (sctCodesMissingMapping.size() > 0)
			{
				ConsoleUtil.println("Writing 'Missing SCT Mappings' file - " + sctCodesMissingMapping.size());
				bw = new BufferedWriter(new FileWriter(new File(outputDirectory, "Missing SCT Mappings.tsv")));
				bw.write("SCT Code from CHDR\tDescription from CHDR" + System.getProperty("line.separator"));
				for (Entry<String, String> s : sctCodesMissingMapping.entrySet())
				{
					bw.write(s.getKey() + "\t" + s.getValue() + System.getProperty("line.separator"));
				}
				bw.close();
			}
			
			if (rxnCodesMissingMapping.size() > 0)
			{
				ConsoleUtil.println("Writing 'Missing RxNorm Mappings' file - " + rxnCodesMissingMapping.size());
				bw = new BufferedWriter(new FileWriter(new File(outputDirectory, "Missing RxNorm Mappings.tsv")));
				bw.write("RxNorm Code from CHDR\tDescription from CHDR" + System.getProperty("line.separator"));
				for (Entry<String, String> s : rxnCodesMissingMapping.entrySet())
				{
					bw.write(s.getKey() + "\t" + s.getValue() + System.getProperty("line.separator"));
				}
				bw.close();
			}
			
//			if (umlsCodesMissingMapping.size() > 0)
//			{
//				ConsoleUtil.println("Writing 'Missing UMLS Mappings' file - " + umlsCodesMissingMapping.size());
//				bw = new BufferedWriter(new FileWriter(new File(outputDirectory, "Missing UMLS Mappings.tsv")));
//				bw.write("UMLS Code from CHDR\tDescription from CHDR" + System.getProperty("line.separator"));
//				for (Entry<String, String> s : umlsCodesMissingMapping.entrySet())
//				{
//					bw.write(s.getKey() + "\t" + s.getValue() + System.getProperty("line.separator"));
//				}
//				bw.close();
//			}
			
			if (ndfRtCodesMissingMapping.size() > 0)
			{
				ConsoleUtil.println("Writing 'Missing NDF-RT Mappings' file - " + ndfRtCodesMissingMapping.size());
				bw = new BufferedWriter(new FileWriter(new File(outputDirectory, "Missing NDF-RT Mappings.tsv")));
				bw.write("NDF-RT CUI Code from CHDR\tDescription from CHDR" + System.getProperty("line.separator"));
				for (Entry<String, String> s : ndfRtCodesMissingMapping.entrySet())
				{
					bw.write(s.getKey() + "\t" + s.getValue() + System.getProperty("line.separator"));
				}
				bw.close();
			}
			
			ConsoleUtil.writeOutputToFile(new File(outputDirectory, "ConsoleOutput.txt").toPath());
		}
		catch (Exception ex)
		{
			throw new MojoExecutionException(ex.getLocalizedMessage(), ex);
		}
	}
	
	private void createOrUpdateRel(EConcept eConcept, UUID target, UUID relType, String annotationValue)
	{
		//Need to search through the relationships on the concept as we have it now - and if the desired rel doesn't exist - create it.
		//If it does, see if it is annotated for this description.  If not, add the annotation.
		TkRelationship rel = null;
		boolean annotationFound = false;
		if (eConcept.getRelationships() != null)
		{
			for (TkRelationship currentRel : eConcept.getRelationships())
			{
				if (currentRel.getRelationshipTargetUuid().equals(target) && currentRel.getTypeUuid().equals(relType))
				{
					dupeRelCount++;
					rel = currentRel;
					for (TkRefexAbstractMember<?> annotation : rel.getAnnotations())
					{
						if (annotation instanceof TkRefsetStrMember)
						{
							TkRefsetStrMember annot = (TkRefsetStrMember)annotation;
							if (annot.getString1().equals(annotationValue) && annot.getRefexUuid().equals(PT_Annotations.Attributes.CHDR_REL_SOURCE.getProperty().getUUID()))
							{
								dupeRelCountWithAnnotation++;
								annotationFound = true;
								break;
							}
						}
					}
					
					break;
				}
			}
		}
		if (rel == null)
		{
			rel = eConceptUtil_.addRelationship(eConcept, target, relType, null); 
		}
		
		if (!annotationFound)
		{
			eConceptUtil_.addStringAnnotation(rel, annotationValue, PT_Annotations.Attributes.CHDR_REL_SOURCE.getProperty().getUUID(), false);
		}
	}

	private UUID createRefsetMember(Concept concept, UUID idTypeUUID) throws IOException
	{
		String fsn = null;
		ArrayList<String> synonyms = new ArrayList<String>();
		for (String s : concept.getDescriptions())
		{
			if (fsn == null)
			{
				fsn = s;
			}
			else
			{
				synonyms.add(s);
			}
		}

		if (fsn == null)
		{
			fsn = concept.getId();
		}

		EConcept eConcept = eConceptUtil_.createConcept(makeUUID(concept), fsn);

		eConceptUtil_.addAdditionalIds(eConcept, concept.getId(), idTypeUUID, false);

		for (String s : synonyms)
		{
			eConceptUtil_.addDescription(eConcept, s, DescriptionType.SYNONYM, false, null, null, false);
		}
		eConcept.writeExternal(dos);
		return eConcept.getPrimordialUuid();
	}

	private void createVHATConcept(Concept concept, UUID parentUUID) throws IOException
	{
		String fsn = null;
		ArrayList<String> synonyms = new ArrayList<String>();
		for (String s : concept.getDescriptions())
		{
			if (fsn == null)
			{
				fsn = s;
			}
			else
			{
				synonyms.add(s);
			}
		}

		if (fsn == null)
		{
			fsn = concept.getId();
		}

		EConcept eConcept = eConceptUtil_.createConcept(makeUUID(concept), fsn);

		eConceptUtil_.addAdditionalIds(eConcept, concept.getId(), PT_IDs.ID.VUID.getProperty().getUUID(), false);

		for (String s : synonyms)
		{
			eConceptUtil_.addDescription(eConcept, s, DescriptionType.SYNONYM, false, null, null, false);
		}
		eConceptUtil_.addRelationship(eConcept, parentUUID);
		vhatVuidConceptToUUID.put(concept.getId(), new UUIDTypeRef(eConcept.getPrimordialUuid(), WBType.Concept, fsn));

		eConcept.writeExternal(dos);
	}

	private UUID makeRefsetUUID(String refsetType, String value)
	{
		return ConverterUUID.createNamespaceUUIDFromString("CHDR-Refset:" + refsetType + ":" + value);
	}

	private UUID makeUUID(Concept c)
	{
		// The code bit makes it line up with VHAT generation for sanity
		if (ConceptType.VHAT == c.getType())
		{
			return ConverterUUID.createNamespaceUUIDFromString(vhatNamespaceUUID, "code:" + c.getId(), true);
		}
		else
		{
			return ConverterUUID.createNamespaceUUIDFromString(c.getType().name() + ":" + c.getId(), true);
		}
	}

	enum WBType
	{
		Concept, Description
	};
	private class UUIDTypeRef
	{
		private UUID uuid, parentConceptUUID;
		private WBType wbType;
		private String description;

		public UUIDTypeRef(UUID uuid, WBType wbType, String description)
		{
			this.uuid = uuid;
			this.wbType = wbType;
			this.description = description;
		}

		public UUIDTypeRef(UUID uuid, WBType wbType, String description, UUID parentConceptUUID)
		{
			this.uuid = uuid;
			this.wbType = wbType;
			this.description = description;
			this.parentConceptUUID = parentConceptUUID;
		}

		public UUID getUUID()
		{
			return this.uuid;
		}

		public WBType getType()
		{
			return this.wbType;
		}

		public String getDescription()
		{
			return description;
		}

		public UUID getParentConceptUUID()
		{
			return parentConceptUUID;
		}
	}

	public static void main(String[] args) throws Exception
	{
		CHDRImportMojo i = new CHDRImportMojo();
		i.outputDirectory = new File("../chdr-econcept/target");
		// i.inputFile = new File("../chdr-econcept/CHDR Data/");
		i.inputFile = new File("../chdr-econcept/target/generated-resources/data");
		i.vhatInputFile = new File("../chdr-econcept/target/generated-resources/data/VHAT");
		i.execute();
	}
}