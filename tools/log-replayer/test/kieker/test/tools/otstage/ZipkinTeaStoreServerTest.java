package kieker.test.tools.otstage;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import kieker.tools.log.replayer.ReplayerMain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ZipkinTeaStoreServerTest {

	private static final String kiekerDataPath = "test-resources/teastore";

	private Process process;

	@BeforeEach
	public void startZipkinServer() throws IOException, InterruptedException {
		process = ZipkinServerUtil.startZipkin();
	}

	@Test
	public void test() throws IOException, InterruptedException {
		final File[] kiekerDataFiles = Objects.requireNonNull(new java.io.File(kiekerDataPath).listFiles());

		for (final File kiekerDataFile : kiekerDataFiles) {
			if (kiekerDataFile.isDirectory()) {
				final ReplayerMain main = new ReplayerMain();
				main.run("Replayer", "replayer", new String[] { "--no-delay", "-i", kiekerDataFile.getAbsolutePath() });
			}
		}

		// Check Zipkin API for spans
		final boolean spansCreated = ZipkinServerUtil.checkZipkinSpanValidity();
		Assertions.assertTrue(spansCreated, "Spans should be created in Zipkin");
	}

	@AfterEach
	public void stopZipkinServer() {
		process.destroyForcibly();
	}
}
