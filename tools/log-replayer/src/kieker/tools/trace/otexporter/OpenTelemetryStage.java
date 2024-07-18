package kieker.tools.trace.otexporter;

import java.time.Instant;
import java.util.Stack;

import kieker.common.record.IMonitoringRecord;
import kieker.common.record.controlflow.OperationExecutionRecord;
import kieker.common.record.misc.KiekerMetadataRecord;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import teetime.framework.AbstractConsumerStage;

public class OpenTelemetryStage extends AbstractConsumerStage<IMonitoringRecord> {

	private static volatile boolean initialized = false;

	public OpenTelemetryStage() {
		synchronized (OpenTelemetryStage.class) {
			if (!initialized) {
				createTracerProvider("kieker-data");
				initialized = true;
			}
		}
	}

	private SdkTracerProvider createTracerProvider(final String serviceName) {
		final Resource resource = Resource.getDefault().merge(Resource
				.create(Attributes.builder().put(AttributeKey.stringKey("service.name"), serviceName).build()));

		final SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().setResource(resource)
				.addSpanProcessor(BatchSpanProcessor
						.builder(ZipkinSpanExporter.builder().setEndpoint("http://localhost:9411/api/v2/spans").build())
						.build())
				.build();

		OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider).buildAndRegisterGlobal();
		return sdkTracerProvider;
	}

	private int lastEss;
	private final Stack<Span> lastSpan = new Stack<Span>();

	@Override
	protected void execute(final IMonitoringRecord record) throws Exception {
		// System.out.println("Reading span: " + record);
		if (record instanceof OperationExecutionRecord) {
			final OperationExecutionRecord oer = (OperationExecutionRecord) record;

			final Tracer tracer = GlobalOpenTelemetry.getTracer(oer.getHostname());

			final Instant startTime = Instant.ofEpochMilli(oer.getTin());

			// Start a new span for the operation
			final String operationSignature = oer.getOperationSignature();
			final SpanBuilder spanBuilder = tracer.spanBuilder(operationSignature).setStartTimestamp(startTime);
			if (lastSpan != null && oer.getEss() > 0) {
				spanBuilder.setParent(Context.current().with(lastSpan.peek()));
			}

			final Span span = spanBuilder.startSpan();

			try (Scope scope = span.makeCurrent()) {
				span.setAttribute("customAttribute", "5");

			} finally {
				final Instant endTime = Instant.ofEpochMilli(oer.getTout());
				span.end(endTime);
			}

			// System.out.println("Ess: " + oer.getEss() + " " + lastEss);

			if (oer.getEss() >= lastEss) {
				lastEss++;
				lastSpan.add(span);
			} else if (oer.getEss() == lastEss) {
				lastSpan.pop();
				lastSpan.add(span);
			} else {
				lastEss--;
				lastSpan.pop();
				lastSpan.add(span);
			}

		} else if (record instanceof KiekerMetadataRecord) {
			System.out.println("Ignore metadata: " + record);
		}
	}
}
