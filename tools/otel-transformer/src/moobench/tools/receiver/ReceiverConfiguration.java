package moobench.tools.receiver;

import teetime.framework.Configuration;
import kieker.analysis.generic.CountingStage;
import kieker.analysis.generic.source.rewriter.NoneTraceMetadataRewriter;
import kieker.analysis.generic.source.tcp.MultipleConnectionTcpSourceStage;

public class ReceiverConfiguration extends Configuration {

	public ReceiverConfiguration(final int inputPort, final int bufferSize) {
		MultipleConnectionTcpSourceStage source = new MultipleConnectionTcpSourceStage(inputPort, bufferSize, new NoneTraceMetadataRewriter());
		CountingStage counting = new CountingStage(false, 10000);
		
		connectPorts(source.getOutputPort(), counting.getInputPort());
	}
}
