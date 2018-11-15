package org.opensourcebim;

import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.opensourcebim.bcf.BcfFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ClashDetectionAsBcfCaller extends BimBotCaller<BcfFile> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClashDetectionAsBcfCaller.class);
	
	public ClashDetectionAsBcfCaller(String baseUrl, String token, String serviceIdentifier, BimBotsInput bimBotsInput) {
		super(baseUrl, token, serviceIdentifier, bimBotsInput);
	}
	
	public ClashDetectionAsBcfCaller(ObjectNode jsonConfig, BimBotsInput bimBotsInput) throws BimBotConfigurationException {
		super(jsonConfig, bimBotsInput);
	}
	
	@Override
	public BcfFile processOutput(BimBotsOutput bimBotsOutput) {
		try {
			return BcfFile.read(bimBotsOutput.getData());
		} catch (Exception e) {
			LOGGER.error("", e);
			return null;
		}
	}
}
