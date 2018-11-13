package org.opensourcebim;

import java.util.concurrent.Callable;

import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.plugins.services.BimBotClient;
import org.bimserver.plugins.services.BimBotExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class BimBotCaller<V> implements Callable<V> {
	private static final Logger LOGGER = LoggerFactory.getLogger(BimBotCaller.class);

	private String baseUrl;
	private String token;
	private String serviceIdentifier;
	private BimBotsInput bimBotsInput;

	public BimBotCaller(String baseUrl, String token, String serviceIdentifier, BimBotsInput bimBotsInput) {
		this.baseUrl = baseUrl;
		this.token = token;
		this.serviceIdentifier = serviceIdentifier;
		this.bimBotsInput = bimBotsInput;
	}

	public BimBotCaller(ObjectNode jsonConfig, BimBotsInput bimBotsInput) throws BimBotCallerException {
		this.bimBotsInput = bimBotsInput;
		if (!jsonConfig.has("baseUrl")) {
			throw new BimBotCallerException("No \"baseUrl\" in config");
		}
		if (!jsonConfig.has("token")) {
			throw new BimBotCallerException("No \"token\" in config");
		}
		if (!jsonConfig.has("serviceIdentifier")) {
			throw new BimBotCallerException("No \"serviceIdentifier\" in config");
		}
		this.baseUrl = jsonConfig.get("baseUrl").asText();
		this.token = jsonConfig.get("token").asText();
		this.serviceIdentifier = jsonConfig.get("serviceIdentifier").asText();
	}

	@Override
	public V call() throws Exception {
		LOGGER.info("Calling IfcValidator");
		try (BimBotClient bimBotCaller = new BimBotClient(baseUrl, token)) {
			BimBotsOutput bimBotsOutput = bimBotCaller.call(serviceIdentifier, bimBotsInput);
			return processOutput(bimBotsOutput);
		} catch (BimBotExecutionException e) {
			LOGGER.error("", e);
		}
		return null;
	}

	public abstract V processOutput(BimBotsOutput bimBotsOutput);
}
