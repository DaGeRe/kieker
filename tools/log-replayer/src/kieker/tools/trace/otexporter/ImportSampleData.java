package kieker.tools.trace.otexporter;

import java.io.File;

import kieker.tools.log.replayer.ReplayerMain;

public class ImportSampleData {
	public static void main(final String[] args) {
		final File[] kiekerDataFiles = new File("test-resources/teastore").listFiles();

		for (final File kiekerDataFile : kiekerDataFiles) {
			if (kiekerDataFile.isDirectory()) {
				final ReplayerMain main = new ReplayerMain();
				main.run("Replayer", "replayer", new String[] { "--no-delay", "-i", kiekerDataFile.getAbsolutePath() });
			}
		}
	}
}
