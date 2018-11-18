package org.opensourcebim;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class VoxelServerClient implements AutoCloseable {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final Logger LOGGER = LoggerFactory.getLogger(VoxelServerClient.class);
	private final CloseableHttpClient httpClient;
	private final String baseAddress;
	private final RequestConfig requestConfig;

	public VoxelServerClient(String baseAddress) {
		this.baseAddress = baseAddress;
		httpClient = HttpClients.createDefault();
		
		RequestConfig.Builder requestConfigBuilder = RequestConfig.copy(RequestConfig.DEFAULT);
		requestConfig = requestConfigBuilder.setConnectTimeout(4000)
	        .setSocketTimeout(120000) // 2 minutes
	        .setConnectionRequestTimeout(4000)
	        .build();
	}
	
	public double getSurfaceArea(InputStream ifcInputStream) throws Exception {
		HttpPost post = new HttpPost(baseAddress + "/surface_area");
		post.setConfig(requestConfig);
		
		HttpEntity data = MultipartEntityBuilder.create()
            .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
            .addBinaryBody("ifc", ifcInputStream, ContentType.DEFAULT_BINARY, "ifc")
            .build();
		
		post.setEntity(data);
		
		try (CloseableHttpResponse httpResponse = httpClient.execute(post)) {
			StatusLine statusLine = httpResponse.getStatusLine();
			if (statusLine.getStatusCode() == 200) {
				ObjectNode response = OBJECT_MAPPER.readValue(httpResponse.getEntity().getContent(), ObjectNode.class);
				return response.get("area").asDouble();
			} else {
				throw new Exception("Wrong status code: " + statusLine.toString());
			}
		} catch (ClientProtocolException e) {
			throw new Exception(e);
		} catch (IOException e) {
			throw new Exception(e);
		}
	}
	
	public void close() {
		try {
			httpClient.close();
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}
}