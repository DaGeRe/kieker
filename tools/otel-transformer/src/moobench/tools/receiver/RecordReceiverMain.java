/**
 * 
 */
package moobench.tools.receiver;

import kieker.monitoring.core.configuration.ConfigurationFactory;
import teetime.framework.Execution;

/**
 * @author Reiner Jung
 *
 */
public class RecordReceiverMain {

	private RecordReceiverMain() {}

	public static void main(final String[] args) {
		final kieker.common.configuration.Configuration configuration;
		if (args.length > 1) {
			configuration = ConfigurationFactory.createConfigurationFromFile(args[1]);
		} else {
			configuration = ConfigurationFactory.createDefaultConfiguration();
		}
		
		OpenTelemetryExportConfiguration teeTimeConfig = new OpenTelemetryExportConfiguration(Integer.parseInt(args[0]), 8192, configuration);
		
		Execution<OpenTelemetryExportConfiguration> execution = new Execution<OpenTelemetryExportConfiguration>(teeTimeConfig);
		execution.executeBlocking();
	}
}
