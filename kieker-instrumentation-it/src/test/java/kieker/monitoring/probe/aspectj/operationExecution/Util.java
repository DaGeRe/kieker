package kieker.monitoring.probe.aspectj.operationExecution;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

public enum Util {
	;
	public static File monitoringFolder = new File("monitoring-logs");

	public static void clearFolder() {
		try {
			FileUtils.deleteDirectory(monitoringFolder);
		} catch (IOException e) {
			e.printStackTrace();
		}
		monitoringFolder.mkdir();
	}

	public static List<String> getLatestLogRecord() throws IOException {
		File dataFolder = monitoringFolder.listFiles()[0];
		File dataFile = dataFolder.listFiles((FilenameFilter) new WildcardFileFilter("*.dat"))[0];
		List<String> lines = FileUtils.readLines(dataFile, "utf-8");
		return lines;
	}
}
