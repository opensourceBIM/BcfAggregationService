package org.opensourcebim;

import java.io.ByteArrayInputStream;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallVoxelServer implements Callable<Double> {

	private static final Logger LOGGER = LoggerFactory.getLogger(CallVoxelServer.class);
	private String address;
	private byte[] inputData;
	
	public CallVoxelServer(String address, byte[] inputData) {
		this.address = address;
		this.inputData = inputData;
	}
	
	@Override
	public Double call() throws Exception {
		LOGGER.info("Calling voxel server");
		try (VoxelServerClient voxelServerClient = new VoxelServerClient(address)) {
			return voxelServerClient.getSurfaceArea(new ByteArrayInputStream(inputData));
		} catch (Exception e) {
			LOGGER.error("", e);
			return null;
		}
	}
}
