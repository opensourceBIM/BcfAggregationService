package org.opensourcebim;

import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ClashDetectionCaller extends BimBotCaller<ArrayNode> {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final Logger LOGGER = LoggerFactory.getLogger(ClashDetectionCaller.class);
	
	public ClashDetectionCaller(String baseUrl, String token, String serviceIdentifier, BimBotsInput bimBotsInput) {
		super(baseUrl, token, serviceIdentifier, bimBotsInput);
	}
	
	public ClashDetectionCaller(ObjectNode jsonConfig, BimBotsInput bimBotsInput) throws BimBotCallerException, BimBotConfigurationException {
		super(jsonConfig, bimBotsInput);
	}
	
	@Override
	public ArrayNode processOutput(BimBotsOutput bimBotsOutput) {
		try {
			return OBJECT_MAPPER.readValue(bimBotsOutput.getData(), ArrayNode.class);
		} catch (Exception e) {
			LOGGER.error("", e);
			return null;
		}
	}
}
