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
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import teetime.framework.AbstractConsumerStage;

public class OpenTelemetryStage extends AbstractConsumerStage<IMonitoringRecord> {

	private final Tracer tracer;

	private static volatile boolean initialized = false;
	private static final Object lock = new Object();

	// private final Tracer tracer;

	public OpenTelemetryStage() {
		// Check if OpenTelemetry has already been initialized
		if (!initialized) {
			// Ensure thread-safety during initialization
			synchronized (lock) {
				// Double-check to avoid race conditions
				if (!initialized) {
					initializeOpenTelemetry();
					initialized = true;
				}
			}
		}
		;

		// Get a tracer instance for instrumentation
		this.tracer = GlobalOpenTelemetry.getTracer("kieker-instrumentation");

	}

	private void initializeOpenTelemetry() {
		// Create a tracer provider and register it globally
		final SdkTracerProvider tracerProvider = createTracerProvider();

		OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).buildAndRegisterGlobal();
	}

	private OtlpHttpSpanExporter createSpanExporter() {
		// Create an OTLP HTTP Span Exporter
		return OtlpHttpSpanExporter.builder().setEndpoint("http://localhost:55681/v1/traces") // 55681

				.build();
	}

	private SdkTracerProvider createTracerProvider() {

		// Define resource information, such as service name
		final Resource resource = Resource.getDefault().merge(Resource
				.create(Attributes.builder().put(AttributeKey.stringKey("service.name"), "kieker-data").build()));

		// Create a tracer provider with a BatchSpanProcessor for exporting spans to
		// Zipkin
		return SdkTracerProvider.builder().setResource(resource)
				.addSpanProcessor(BatchSpanProcessor
						.builder(ZipkinSpanExporter.builder().setEndpoint("http://localhost:9411/api/v2/spans").build())
						.build())
				.build();
	}

	private int lastEss;
	private final Stack<Span> lastSpan = new Stack<Span>();

	@Override
	protected void execute(final IMonitoringRecord record) throws Exception {
		// System.out.println("Reading span: " + record);
		if (record instanceof OperationExecutionRecord) {
			final OperationExecutionRecord oer = (OperationExecutionRecord) record;
			// System.out.println("OER: " + oer);

			final Instant startTime = Instant.ofEpochMilli(oer.getTin());

			// Start a new span for the operation
			final SpanBuilder spanBuilder = tracer.spanBuilder(oer.getOperationSignature()).setStartTimestamp(startTime);
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
