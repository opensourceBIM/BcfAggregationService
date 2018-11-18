package org.opensourcebim;

import org.bimserver.bimbots.BimBotErrorCode;

public enum BcfAggregationErrorCodes implements BimBotErrorCode {
	NO_OR_TOO_MANY_IFCPROJECTS(101), NO_IFC_VALIDATOR_IN_SETTINGS(102), NO_SETTINGS(103);
	
	private int code;

	BcfAggregationErrorCodes(int code) {
		this.code = code;
	}
	
	@Override
	public int getErrorCode() {
		return code;
	}
}