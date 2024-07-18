package kieker.test.tools.otstage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipkinServerUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(ZipkinServerUtil.class);

	private static final String jarPath = "test-resources/zipkin-3.4.0.jar";
	private static final String ZIPKIN_URL = "http://localhost:9411/";

	public static Process startZipkin() throws IOException, InterruptedException {
		LOGGER.info("Starting Zipkin");

		final String command = String.format("java -jar %s", jarPath);
		LOGGER.info("Command: " + command);
		final Process process = Runtime.getRuntime().exec(command);

		waitForZipkinStartup(process);

		if (!process.isAlive()) {
			throw new RuntimeException("Zipkin did not start up correctly");
		}
		return process;
	}

	private static void waitForZipkinStartup(final Process process) throws IOException, InterruptedException {
		// capture and print the process output

		final StringBuffer standardOut = new StringBuffer();
		final StreamGobbler stdoutStreamThread = new StreamGobbler(process.getInputStream(), standardOut);
		final StreamGobbler stderrStreamThread = new StreamGobbler(process.getErrorStream(), standardOut);

		stdoutStreamThread.start();
		stderrStreamThread.start();

		for (int i = 0; i < 10 && process.isAlive(); i++) {
			if (standardOut.toString().contains("Serving HTTP at")) {
				if (checkZipkinHealth()) {
					LOGGER.info("Startup finished");
					return;
				} else {
					LOGGER.warn("Zipkin server is not healthy. Retrying...");
				}
			}
			Thread.sleep(5000);
		}
	}

	private static boolean checkZipkinHealth() throws IOException {
		// Zipkin health check to ensure the server is ready
		final URL url = new URL(ZIPKIN_URL + "health");
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");

		final int responseCode = connection.getResponseCode();
		return responseCode == HttpURLConnection.HTTP_OK;
	}

	public static boolean checkZipkinSpanValidity() throws IOException, InterruptedException {
		Thread.sleep(5000);

		// Zipkin API to check if traces were created
		final URL url = new URL(ZIPKIN_URL + "api/v2/traces");
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");

		// Use the actual response code from the server
		final int responseCode = connection.getResponseCode();
		LOGGER.info("Zipkin server response code: " + responseCode);
		if (responseCode != HttpURLConnection.HTTP_OK) {
			LOGGER.error("Received HTTP error code from Zipkin server: " + responseCode);
			return false;
		}

		final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		final StringBuilder response = new StringBuilder();
		String inputLine;
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		// LOGGER.info("Zipkin traces response: " + response);

		final ObjectMapper objectMapper = new ObjectMapper();
		try {
			final JsonNode rootNode = objectMapper.readTree(response.toString());

			objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
			objectMapper.writeValue(new File("test.json"), rootNode);

			if (!rootNode.isArray() || rootNode.size() == 0) {
				Assert.fail("No traces found in Zipkin.");
			}

			// Validate each trace and span according to expected structure and content
			for (final JsonNode trace : rootNode) {
				if (!trace.isArray() || trace.size() == 0) {
					LOGGER.error("A trace with no spans was found.");
					return false;
				}
				for (final JsonNode span : trace) {
					if (!isValidSpan(span)) {
						return false;
					}
				}
			}
		} catch (final IOException e) {
			LOGGER.error("IOException during Zipkin span check", e);
			return false;
		} catch (final Exception e) {
			LOGGER.error("Unexpected exception during Zipkin span check", e);
			return false;
		}
		return true;
	}

	private static boolean isValidSpan(final JsonNode span) {
		// Check for a non-empty name
		if (!span.has("name") || span.get("name").asText().isEmpty()) {
			LOGGER.error("Span name is empty.");
			Assert.fail("A span with an empty or missing name was found.");
			return false;
		}

		// Verify timestamp format
		final String timestamp = span.get("timestamp").asText();
		if (!timestamp.matches("\\d+")) {
			LOGGER.error("Invalid or missing timestamp for span.");
			Assert.fail("Invalid or missing timestamp for span.");
			return false;
		}

		// Check for expected span relationships
		if (span.has("parentId") && span.get("parentId").asText().isEmpty()) {
			LOGGER.error("Span has an empty parentId, indicating a broken parent-child relationship.");
			Assert.fail("Span has an empty parentId, indicating a broken parent-child relationship.");
			return false;
		}

		// Validate span name structure and content
		final String spanName = span.get("name").asText().trim();
		final boolean containsParentheses = spanName.contains("(") && spanName.contains(")");
		final boolean isConstructor = spanName.contains("<init>");
		if (!containsParentheses && !isConstructor) {
			LOGGER.error("Span name does not contain a valid method or constructor signature: " + spanName);
			Assert.fail("Span name does not contain a valid method or constructor signature.");
			return false;
		}

		// checks for method signature
		if (!isConstructor) {
			// Check for space before opening parenthesis for methods
			final int parenIndex = spanName.indexOf('(');
			if (parenIndex <= 0 || spanName.substring(0, parenIndex).endsWith(" ")) {
				LOGGER.error("Invalid method signature: " + spanName);
				Assert.fail("Invalid method signature.");
				return false;
			}
		}

		// Validate 'ipv4' and 'serviceName' in 'localEndpoint'
		final JsonNode localEndpoint = span.get("localEndpoint");
		if (localEndpoint == null || !localEndpoint.has("ipv4") || localEndpoint.get("ipv4").asText().isEmpty() ||
				!localEndpoint.has("serviceName") || localEndpoint.get("serviceName").asText().isEmpty()) {
			LOGGER.error("Missing or incorrect 'ipv4' or 'serviceName' in 'localEndpoint'.");
			return false;
		}

		// Validate 'traceId'
		if (!span.has("traceId") || span.get("traceId").asText().isEmpty()) {
			LOGGER.error("Missing 'traceId'.");
			return false;
		}

		// If all validations pass
		// LOGGER.info("Span name and related properties are valid.");
		return true;
	}
}
