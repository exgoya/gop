package service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

public class Rest {
	final static String USER_AGENT = "gop/1.0";

	public void sendGET(String requestURL) throws IOException {
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpGet httpGet = new HttpGet(requestURL);
			httpGet.addHeader("User-Agent", USER_AGENT);
			try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
				System.out.println("GET Response Status:: "
						+ httpResponse.getCode());

				try {
					String response = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
					// print result
					System.out.println(response);
				} catch (ParseException e) {
					throw new IOException("Failed to parse response body", e);
				}
			}
		}
	}

	public void sendPOST(String requestURL, String jsonMessage) {

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpPost httpPost = new HttpPost(requestURL);
			httpPost.addHeader("User-Agent", USER_AGENT);

			StringEntity requestEntity = new StringEntity(jsonMessage, ContentType.TEXT_PLAIN);
			httpPost.setEntity(requestEntity);

		// List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		// urlParameters.add(new BasicNameValuePair("userName", "Pankaj Kumar"));

		// HttpEntity postParams = new UrlEncodedFormEntity(urlParameters);
		// httpPost.setEntity(postParams);

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {

				System.out.println("POST Response Status:: " +
						httpResponse.getCode());

				// Consume response content to release the connection.
				EntityUtils.consume(httpResponse.getEntity());

			// print result
			// System.out.println(response.toString());
			}
		} catch (IOException e) {
			System.out.println("post err : " + requestURL);
		}
	}
}
