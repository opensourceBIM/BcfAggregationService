package org.opensourcebim;

import java.io.ByteArrayInputStream;

import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.opensourcebim.bcf.BcfException;
import org.opensourcebim.bcf.BcfFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class IfcValidatorCaller extends BimBotCaller<BcfFile> {

	private static final Logger LOGGER = LoggerFactory.getLogger(IfcValidatorCaller.class);

	public IfcValidatorCaller(String baseUrl, String token, String serviceIdentifier, BimBotsInput bimBotsInput) {
		super(baseUrl, token, serviceIdentifier, bimBotsInput);
	}
	
	public IfcValidatorCaller(ObjectNode settings, BimBotsInput bimBotsInput) throws BimBotCallerException {
		super(settings, bimBotsInput);
	}

	public BcfFile processOutput(BimBotsOutput bimBotsOutput) {
		try {
			return BcfFile.read(new ByteArrayInputStream(bimBotsOutput.getData()));
		} catch (BcfException e) {
			LOGGER.error("", e);
			return null;
		}
	}
}
