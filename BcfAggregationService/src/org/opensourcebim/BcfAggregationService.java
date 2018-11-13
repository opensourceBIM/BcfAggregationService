package org.opensourcebim;

import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.bimserver.bimbots.BimBotContext;
import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.models.ifc2x3tc1.IfcProject;
import org.bimserver.models.store.IfcHeader;
import org.bimserver.models.store.ObjectDefinition;
import org.bimserver.models.store.ParameterDefinition;
import org.bimserver.models.store.PrimitiveDefinition;
import org.bimserver.models.store.PrimitiveEnum;
import org.bimserver.models.store.StoreFactory;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.SchemaName;
import org.bimserver.plugins.services.BimBotAbstractService;
import org.opensourcebim.bcf.BcfException;
import org.opensourcebim.bcf.BcfFile;
import org.opensourcebim.bcf.TopicFolder;
import org.opensourcebim.bcf.markup.Header;
import org.opensourcebim.bcf.markup.Markup;
import org.opensourcebim.bcf.markup.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class BcfAggregationService extends BimBotAbstractService {
	private static final Logger LOGGER = LoggerFactory.getLogger(BcfAggregationService.class);
	
	private static DatatypeFactory DATE_FACTORY;

	static {
		try {
			DATE_FACTORY = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			LOGGER.error("", e);
		}
	}

	@Override
	public ObjectDefinition getSettingsDefinition() {
		ObjectDefinition settings = StoreFactory.eINSTANCE.createObjectDefinition();
		
		PrimitiveDefinition stringType = StoreFactory.eINSTANCE.createPrimitiveDefinition();
		stringType.setType(PrimitiveEnum.STRING);

		ParameterDefinition settingsParameter = StoreFactory.eINSTANCE.createParameterDefinition();
		settingsParameter.setName("settingsJson");
		settingsParameter.setIdentifier("settingsJson");
		settingsParameter.setDescription("JSON configuration");
		settingsParameter.setType(stringType);

		settings.getParameters().add(settingsParameter);
		return settings;
	}
	
	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, BimBotContext bimBotContext, PluginConfiguration pluginConfiguration) throws BimBotsException {
		try {
			ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(3, 3, 1, TimeUnit.DAYS, new ArrayBlockingQueue<>(10));

			List<IfcProject> projects = input.getIfcModel().getAll(IfcProject.class);
			IfcProject project = null;
			if (projects.size() == 1) {
				project = projects.get(0);
			} else {
				throw new BimBotsException("No, or too many IfcProject entities found");
			}
			
			ObjectNode jsonSettings = new ObjectMapper().readValue(pluginConfiguration.getString("settingsJson"), ObjectNode.class);
			if (!jsonSettings.has("ifcvalidator")) {
				throw new BimBotsException("No \"ifcvalidator\" in settings");
			}
			ObjectNode ifcValidatorSettings = (ObjectNode) jsonSettings.get("ifcvalidator");
			Future<BcfFile> ifcValidationResults;
			try {
				ifcValidationResults = threadPoolExecutor.submit(new IfcValidatorCaller(ifcValidatorSettings, input));
			} catch (BimBotCallerException e) {
				throw new BimBotsException(e);
			}

			Future<Double> voxelResults = null;
			if (jsonSettings.has("voxelservice")) {
				ObjectNode voxelSettings = (ObjectNode)jsonSettings.get("voxelservice");
				voxelResults = threadPoolExecutor.submit(new CallVoxelServer(voxelSettings.get("url").asText(), input.getData()));
			}
			
			Future<BcfFile> clashDetectionResults = null;
			if (jsonSettings.has("clashdetection")) {
				try {
					clashDetectionResults = threadPoolExecutor.submit(new ClashDetectionAsBcfCaller((ObjectNode) jsonSettings.get("clashdetection"), input));
				} catch (BimBotCallerException e) {
					throw new BimBotsException(e);
				}
			}

			threadPoolExecutor.shutdown();
			threadPoolExecutor.awaitTermination(1, TimeUnit.HOURS);
			
			BcfFile bcfFile = ifcValidationResults.get();
			if (bcfFile == null) {
				LOGGER.warn("Something went wrong during IfcValidation, stopping");
				return null;
			}
			
			if (voxelResults != null) {
				Double area = voxelResults.get();
				if (area != null) {
					TopicFolder topicFolder = bcfFile.createTopicFolder();
					Topic topic = topicFolder.createTopic();
					topic.setCreationAuthor("Unknown");
					topic.setCreationDate(DATE_FACTORY.newXMLGregorianCalendar(new GregorianCalendar()));
					topic.setTitle("Total outer area");
					topic.setTopicType("TOTAL_AREA");
					topic.setDescription("The total outer area calculated by the voxel BIMbot is: " + area);
					
					Markup markup = topicFolder.getMarkup();
					Header header = new Header();
					markup.setHeader(header);
					List<Header.File> files = header.getFile();
					
					Header.File file = new Header.File();
					file.setIfcSpatialStructureElement(project.getGlobalId());
					file.setIfcProject((String) project.eGet(project.eClass().getEStructuralFeature("GlobalId")));
					file.setIsExternal(true);
					
					String filename = null;
					XMLGregorianCalendar fileDate = null;
					IfcHeader ifcHeader = input.getIfcModel().getModelMetaData().getIfcHeader();
					if (ifcHeader != null) {
						if (ifcHeader.getTimeStamp() != null) {
							fileDate = dateToXMLGregorianCalendar(ifcHeader.getTimeStamp(), TimeZone.getDefault());
						}
						filename = ifcHeader.getFilename();
					}

					file.setFilename(filename);
					file.setReference(filename);
					file.setDate(fileDate);
					files.add(file);
				} else {
					LOGGER.warn("Something went wrong during area calculation");
				}
			} else {
				LOGGER.info("Not running Voxel calculation, no configuration found");
			}
			
			if (clashDetectionResults != null) {
				BcfFile clashesBcf = clashDetectionResults.get();
				if (clashesBcf != null) {
					bcfFile.mergeInto(clashesBcf);
				} else {
					LOGGER.warn("Something went wrong during ClashDetection");
				}
			} else {
				LOGGER.info("Not running ClashDetection, no configuration found");
			}
			
			LOGGER.info("Storing results");
			BimBotsOutput bimBotsOutput = new BimBotsOutput(SchemaName.BCF_ZIP_2_0, bcfFile.toBytes());
			bimBotsOutput.setTitle("Bcf Aggregation Result");
			bimBotsOutput.setContentType("application/zip");
			return bimBotsOutput;
		} catch (BcfException e) {
			LOGGER.error("", e);
		} catch (InterruptedException e) {
			LOGGER.error("", e);
		} catch (ExecutionException e) {
			LOGGER.error("", e);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
		
		return null;
	}

	public static XMLGregorianCalendar dateToXMLGregorianCalendar(Date date, TimeZone zone) {
		XMLGregorianCalendar xmlGregorianCalendar = null;
		GregorianCalendar gregorianCalendar = new GregorianCalendar();
		gregorianCalendar.setTime(date);
		gregorianCalendar.setTimeZone(zone);
		try {
			DatatypeFactory dataTypeFactory = DatatypeFactory.newInstance();
			xmlGregorianCalendar = dataTypeFactory.newXMLGregorianCalendar(gregorianCalendar);
		} catch (Exception e) {
			System.out.println("Exception in conversion of Date to XMLGregorianCalendar" + e);
		}

		return xmlGregorianCalendar;
	}
	
	@Override
	public String getOutputSchema() {
		return "BCF_ZIP_2_0";
	}
	
	public boolean needsRawInput() {
		return true;
	}
}