package kieker.test.tools.otstage;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import kieker.tools.log.replayer.ReplayerMain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ZipkinMoobenchServerTest {
	private Process process;

	@BeforeEach
	public void startZipkinServer() throws IOException, InterruptedException {
		process = ZipkinServerUtil.startZipkin();
	}

	@Test
	public void test16Operations() throws IOException, InterruptedException {
		final File[] kiekerDataFiles = Objects.requireNonNull(new java.io.File("test-resources/moobench").listFiles());

		for (final File kiekerDataFile : kiekerDataFiles) {
			if (kiekerDataFile.isDirectory()) {
				final ReplayerMain main = new ReplayerMain();
				main.run("Replayer", "replayer", new String[] { "--no-delay", "-i", kiekerDataFile.getAbsolutePath() });
			}
		}

		Thread.sleep(10000);

		final JsonNode rootNode = ZipkinServerUtil.readRootNode();

		final boolean spansCreated = ZipkinServerUtil.checkTreeValidity(rootNode);
		Assertions.assertTrue(spansCreated, "Spans should be created in Zipkin");

		final int spans = ZipkinServerUtil.getSpanCount(rootNode);
		Assertions.assertEquals(16, spans);
	}

	@Test
	public void test101Operations() throws IOException, InterruptedException {
		final File[] kiekerDataFiles = Objects.requireNonNull(new java.io.File("test-resources/moobench-2").listFiles());

		for (final File kiekerDataFile : kiekerDataFiles) {
			if (kiekerDataFile.isDirectory()) {
				final ReplayerMain main = new ReplayerMain();
				main.run("Replayer", "replayer", new String[] { "--no-delay", "-i", kiekerDataFile.getAbsolutePath() });
			}
		}

		Thread.sleep(10000);

		// Check Zipkin API for spans
		final JsonNode rootNode = ZipkinServerUtil.readRootNode();
		final boolean spansCreated = ZipkinServerUtil.checkTreeValidity(rootNode);
		Assertions.assertTrue(spansCreated, "Spans should be created in Zipkin");

		final int spans = ZipkinServerUtil.getSpanCount(rootNode);
		Assertions.assertEquals(101, spans);
	}

	@AfterEach
	public void stopZipkinServer() {
		process.destroyForcibly();
	}
}
