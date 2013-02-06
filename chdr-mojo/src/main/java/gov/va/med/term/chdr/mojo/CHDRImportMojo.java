package gov.va.med.term.chdr.mojo;

import gov.va.med.term.chdr.chdrReader.CHDRDataHolder;
import gov.va.med.term.chdr.chdrReader.Concept;
import gov.va.med.term.chdr.chdrReader.ConceptType;
import gov.va.med.term.chdr.chdrReader.VHATConcept;
import gov.va.med.term.chdr.propertyTypes.PT_ContentVersion;
import gov.va.med.term.chdr.propertyTypes.PT_ContentVersion.ContentVersion;
import gov.va.med.term.chdr.propertyTypes.PT_IDs;
import gov.va.med.term.chdr.propertyTypes.PT_Refsets;
import gov.va.med.term.chdr.propertyTypes.PT_Relations;
import gov.va.med.term.chdr.propertyTypes.PT_VHAT_ID;
import gov.va.oia.terminology.converters.sharedUtils.ConsoleUtil;
import gov.va.oia.terminology.converters.sharedUtils.EConceptUtility;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_ContentVersion.BaseContentVersion;
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
import java.util.UUID;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.dwfa.cement.ArchitectonicAuxiliary;
import org.ihtsdo.etypes.EConcept;
import org.ihtsdo.tk.dto.concept.component.description.TkDescription;
import org.ihtsdo.tk.dto.concept.component.identifier.TkIdentifier;

/**
 * Goal which converts CHDR data into the workbench jbin format
 * 
 * @goal convert-CHDR-data
 * 
 * @phase process-sources
 */
public class CHDRImportMojo extends AbstractMojo
{
    private String uuidRoot_ = "gov.va.med.term.vhat.chdr:";
    private String uuidRootVhat_ = "gov.va.med.term.vhat:";
	private PT_ContentVersion contentVersion = new PT_ContentVersion(uuidRoot_);
	private PT_IDs ids = new PT_IDs(uuidRoot_);
	private PT_VHAT_ID vhatId = new PT_VHAT_ID(uuidRootVhat_);
	private PT_Relations rels = new PT_Relations(uuidRoot_);
	private PT_Refsets relRefsets = new PT_Refsets(uuidRoot_);
	private EConceptUtility eConceptUtil_;
	private DataOutputStream dos;
	
	private HashMap<String, UUIDTypeRef> vhatVuidConceptToUUID = new HashMap<String, UUIDTypeRef>();

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
	 * Loader version number
	 * Use parent because project.version pulls in the version of the data file, which I don't want.
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

	public void execute() throws MojoExecutionException
	{
		File f = outputDirectory;

		try
		{
			if (!f.exists())
			{
				f.mkdirs();
			}

			CHDRDataHolder cdh = new CHDRDataHolder(inputFile);

			if (cdh.getNoId() > 0)
			{
				ConsoleUtil.println("CHDR data contains " + cdh.getNoId() + " VUIDs with no associated mapping");
			}
			
			ConsoleUtil.println("Reading VHAT concepts file");
			
			//Read in the VHAT data
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
			
			ConsoleUtil.println("Read " + vhatConcepts.size() + " concepts from the VHAT jbin file");
			
			eConceptUtil_ = new EConceptUtility(uuidRoot_);
			
			UUID vhatRootUUID = null;
			
			for (EConcept c : vhatConcepts)
			{
			    String vuid = null;
			    if (c.getConceptAttributes() != null && c.getConceptAttributes().getAdditionalIdComponents() != null)
			    {
    			    for (TkIdentifier id : c.getConceptAttributes().getAdditionalIdComponents())
    			    {
    			        if (id.getAuthorityUuid().equals(vhatId.getProperty("VUID").getUUID()))
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
			        vhatVuidConceptToUUID.put(vuid,  new UUIDTypeRef(c.getPrimordialUuid(), WBType.Concept, preferredDesc));
			    }
			    
			    //Also need to go through all of the descriptions, and map those.
			    for (TkDescription d : c.getDescriptions())
			    {
			        if (d != null && d.getAdditionalIdComponents() != null)
			        {
			            String descVuid = null;
			            UUID descUUID = null;
			            for (TkIdentifier id : d.getAdditionalIdComponents())
	                    {
	                        if (id.getAuthorityUuid().equals(vhatId.getProperty("VUID").getUUID()))
	                        {
	                            descVuid = id.getDenotation().toString();
	                            descUUID = d.getPrimordialComponentUuid();
	                            break;
	                        }
	                    }
			            if (descVuid != null)
		                {
			                vhatVuidConceptToUUID.put(descVuid, new UUIDTypeRef(descUUID, WBType.Description, d.getText(),d.getTypeUuid(), c.getPrimordialUuid()));
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
			
			File touch = new File(f, "CHDREConcepts.jbin");
			dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(touch)));

			EConcept metaDataRoot = eConceptUtil_.createConcept("CHDR Metadata", ArchitectonicAuxiliary.Concept.ARCHITECTONIC_ROOT_CONCEPT.getPrimoridalUid());
			metaDataRoot.writeExternal(dos);
			
			EConcept vaRefsets = eConceptUtil_.createVARefsetRootConcept();
			vaRefsets.writeExternal(dos);
			
			eConceptUtil_.loadMetaDataItems(contentVersion, metaDataRoot.getPrimordialUuid(), dos);
			eConceptUtil_.loadMetaDataItems(ids, metaDataRoot.getPrimordialUuid(), dos);
			eConceptUtil_.loadMetaDataItems(rels, metaDataRoot.getPrimordialUuid(), dos);
			eConceptUtil_.loadMetaDataItems(relRefsets, metaDataRoot.getPrimordialUuid(), dos);
			
			HashSet<String> missingIds = new HashSet<String>();
			
			EConcept chdr = eConceptUtil_.createConcept("CHDR Refsets", vhatRootUUID);
			eConceptUtil_.addStringAnnotation(chdr, cdh.getVersion(), ContentVersion.VERSION.getProperty().getUUID(), false);
			eConceptUtil_.addStringAnnotation(chdr, releaseVersion, BaseContentVersion.RELEASE.getProperty().getUUID(), false);
			eConceptUtil_.addStringAnnotation(chdr, loaderVersion, BaseContentVersion.LOADER_VERSION.getProperty().getUUID(), false);
			//Also hang it under refsets
			eConceptUtil_.addRelationship(chdr, vaRefsets.getPrimordialUuid(), null, null);
			chdr.writeExternal(dos);
			
			//Create the CHDR refset (all VUIDs mentioned in the data files)
			
			EConcept chdrAllConcepts = eConceptUtil_.createConcept("All CHDR Concepts", chdr.getPrimordialUuid());
			
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
			    eConceptUtil_.addRefsetMember(chdrAllConcepts, memberUUID, makeRefsetUUID("CHDR", c.getId()), true, null);
			}
			
			ConsoleUtil.println(missingIds.size() + " VUIDs exist in CHDR that do not exist in VHAT");
			
			chdrAllConcepts.writeExternal(dos);
			
			//Create the drug products refset
			EConcept drugProducts = eConceptUtil_.createConcept("Drug Products", chdr.getPrimordialUuid());
			for (Concept concept : cdh.getDrugProducts().values())
			{
			    UUID memberUUID = createRefsetMember(concept, PT_IDs.ID.DRUG_MEDIATION_CODE.getProperty().getUUID());
			    eConceptUtil_.addRefsetMember(drugProducts, memberUUID, makeRefsetUUID("Drug Products", concept.getId()), true, null);
			}
			drugProducts.writeExternal(dos);
			
			//and the Reactants refset
            EConcept reactants = eConceptUtil_.createConcept("Reactants", chdr.getPrimordialUuid());
            for (Concept concept : cdh.getReactants().values())
            {
                UUID memberUUID = createRefsetMember(concept, PT_IDs.ID.REACTANT_MEDIATION_CODE.getProperty().getUUID());
                eConceptUtil_.addRefsetMember(reactants, memberUUID, makeRefsetUUID("Reactants", concept.getId()), true, null);
            }
            reactants.writeExternal(dos);
            
            //and the Reactions refset
            EConcept reactions = eConceptUtil_.createConcept("Reactions", chdr.getPrimordialUuid());
            for (Concept concept : cdh.getReactions().values())
            {
                UUID memberUUID = createRefsetMember(concept, PT_IDs.ID.REACTION_MEDIATION_CODE.getProperty().getUUID());
                eConceptUtil_.addRefsetMember(reactions, memberUUID, makeRefsetUUID("Reactions", concept.getId()), true, null);
            }
            reactions.writeExternal(dos);
            
            //Hang this under VHAT as well
            EConcept chdrPendingRoot = eConceptUtil_.createConcept("CHDR Pending", vhatRootUUID);
            chdrPendingRoot.writeExternal(dos);

            //Create the "missing" concepts, link them to pending
            for (String id : missingIds)
            {
                createVHATConcept(cdh.getVhatConcepts().get(id), chdrPendingRoot.getPrimordialUuid());
            }
            
            int conceptMatch = 0;
            int descMatch = 0;
            
            //Finally, create the relations between the VUID concepts and the targets
            for (VHATConcept c : cdh.getVhatConcepts().values())
            {
                //These should all exist now
                UUIDTypeRef member = vhatVuidConceptToUUID.get(c.getId());
                
                //This concept should already exist in the DB - these rels will (hopefully) just be merged onto it
                EConcept eConcept = eConceptUtil_.createConcept((WBType.Concept == member.getType() ? member.getUUID() : member.getParentConceptUUID()), 
                        (Long)null, null);
                if (WBType.Concept == member.getType())
                {
                    conceptMatch++;
                    for (Concept relConcept : c.getIncomingRels())
                    {
                        eConceptUtil_.addRelationship(eConcept, makeUUID(relConcept), PT_Relations.MediationMapping.INCOMING.getProperty().getUUID(), null);
                    }
                    for (Concept relConcept : c.getOutgoingRels())
                    {
                        eConceptUtil_.addRelationship(eConcept, makeUUID(relConcept), PT_Relations.MediationMapping.OUTGOING.getProperty().getUUID(), null);
                    }
                }
                else if (WBType.Description == member.getType())
                {
                    descMatch++;
                    ConverterUUID.addMapping("Merge to existing concept", member.getDescriptionType());
                    TkDescription description = eConceptUtil_.addDescription(eConcept, member.getUUID(), member.getDescription(), member.getDescriptionType(), false);
                    //This description already exists in the WB, just need to add stuff to it.
                    for (Concept relConcept : c.getIncomingRels())
                    {
                        eConceptUtil_.addUuidAnnotation(description, makeUUID(relConcept), PT_Refsets.Refsets.INCOMING.getProperty().getUUID());
                    }
                    for (Concept relConcept : c.getOutgoingRels())
                    {
                        eConceptUtil_.addUuidAnnotation(description, makeUUID(relConcept), PT_Refsets.Refsets.OUTGOING.getProperty().getUUID());
                    }
                }
                else
                {
                    throw new RuntimeException("oops");
                }
                
                eConcept.writeExternal(dos);
            }
            
            ConsoleUtil.println("Mappings to descriptions: " + descMatch);
            ConsoleUtil.println("Mappings to concepts: " + conceptMatch);
			
			dos.flush();
			dos.close();

			for (String line : eConceptUtil_.getLoadStats().getSummary())
			{
				ConsoleUtil.println(line);
			}

			// this could be removed from final release. Just added to help debug editor problems.
			ConsoleUtil.println("Dumping UUID Debug File");
			ConverterUUID.dump(new File(outputDirectory, "chdrUuidDebugMap.txt"));
			
			ConsoleUtil.println("Writing 'Missing Concept' file");
			BufferedWriter br = new BufferedWriter(new FileWriter(new File(outputDirectory, "VUIDs in CHDR not in VHAT.tsv")));
			br.write("VUID\tCHDR Description" + System.getProperty("line.separator"));
	        for (String id : missingIds)
	        {
	            Concept c = cdh.getVhatConcepts().get(id);
	            br.write(c.getId() + "\t" + Arrays.deepToString(c.getDescriptions().toArray(new String[]{})) + System.getProperty("line.separator"));
	        }
	        br.close();
	        
	        ConsoleUtil.println("Writing 'Mismatched descriptions' file");
            br = new BufferedWriter(new FileWriter(new File(outputDirectory, "Mismatched Descriptions.tsv")));
            br.write("VUID\tCHDR Description\tVHAT Description" + System.getProperty("line.separator"));
            for (Concept c : cdh.getVhatConcepts().values())
            {
                if (c.getDescriptions().size() > 1)
                {
                    System.err.println("Dan goof - " + Arrays.deepToString(c.getDescriptions().toArray(new String[]{})));
                }
                else
                {
                    String desc = c.getDescriptions().iterator().next();
                    if (!missingIds.contains(c.getId()))
                    {
                        String vhatDesc = vhatVuidConceptToUUID.get(c.getId()).getDescription(); 
                        if (!desc.equals(vhatDesc))
                        {
                            br.write(c.getId() + "\t" + desc + "\t" + vhatDesc + System.getProperty("line.separator"));
                        }
                    }
                }
            }
            br.close();
		}
		catch (Exception ex)
		{
			throw new MojoExecutionException(ex.getLocalizedMessage(), ex);
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
			eConceptUtil_.addSynonym(eConcept, s, false, null);
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
            eConceptUtil_.addSynonym(eConcept, s, false, null);
        }
        eConceptUtil_.addRelationship(eConcept, parentUUID, null, null);
        vhatVuidConceptToUUID.put(concept.getId(), new UUIDTypeRef(eConcept.getPrimordialUuid(), WBType.Concept, fsn));
        
        eConcept.writeExternal(dos);
    }
	
	private UUID makeRefsetUUID(String refsetType, String value)
	{
	    return ConverterUUID.nameUUIDFromBytes((uuidRoot_ + "CHDR-Refset:" + refsetType + ":" + value).getBytes());
	}
	
	private UUID makeUUID(Concept c)
	{
	    //The code bit makes it line up with VHAT generation for sanity
	    if (ConceptType.VHAT == c.getType())
	    {
	        return ConverterUUID.nameUUIDFromBytes((uuidRootVhat_ + "code:" + c.getId()).getBytes());
	    }
	    else
	    {
	        return ConverterUUID.nameUUIDFromBytes((uuidRoot_ + c.getType().name() + ":" + c.getId()).getBytes());
	    }
	}
	
	enum WBType {Concept, Description};
	private class UUIDTypeRef
	{
	    private UUID uuid, parentConceptUUID, descriptionType;
	    private WBType wbType;
	    private String description;
	    
	    public UUIDTypeRef(UUID uuid, WBType wbType, String description)
	    {
	        this.uuid = uuid;
	        this.wbType = wbType;
	        this.description = description;
	    }
	    
	    public UUIDTypeRef(UUID uuid, WBType wbType, String description, UUID descriptionType, UUID parentConceptUUID)
        {
            this.uuid = uuid;
            this.wbType = wbType;
            this.description = description;
            this.descriptionType = descriptionType;
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
	    
	    public UUID getDescriptionType()
	    {
	        return descriptionType;
	    }
	}
	
    public static void main(String[] args) throws MojoExecutionException
    {
        CHDRImportMojo i = new CHDRImportMojo();
        i.outputDirectory = new File("../chdr-data/target");
        i.inputFile = new File("/mnt/d/Work/Apelon/Workspaces/Loaders/chdr/chdr-data/CHDR Data/");
        i.vhatInputFile = new File("../chdr-data/target/generated-resources/data/VHAT");
        i.execute();
    }
}