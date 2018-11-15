package org.opensourcebim;

import org.bimserver.bimbots.BimBotDefaultErrorCode;
import org.bimserver.bimbots.BimBotErrorCode;
import org.bimserver.bimbots.BimBotsException;

public class BimBotConfigurationException extends BimBotsException {

	private static final long serialVersionUID = -7197287994378303924L;

	public BimBotConfigurationException(String message, BimBotErrorCode errorCode) {
		super(message, errorCode);
	}

	public BimBotConfigurationException(String message) {
		super(message, BimBotDefaultErrorCode.INVALID_CONFIGURATION);
	}
}
