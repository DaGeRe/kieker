/**
 * 
 */
package moobench.tools.receiver;

import teetime.framework.Execution;

/**
 * @author Reiner Jung
 *
 */
public class RecordReceiverMain {

	private RecordReceiverMain() {}

	public static void main(final String[] args) {
		ReceiverConfiguration config = new ReceiverConfiguration(Integer.parseInt(args[0]), 8192);
		Execution<ReceiverConfiguration> execution = new Execution<ReceiverConfiguration>(config);
		execution.executeBlocking();
	}
}
