package kieker.tools.trace.otexporter;

import java.util.Stack;
import java.util.concurrent.TimeUnit;

import kieker.common.util.signature.ClassOperationSignaturePair;
import kieker.model.system.model.Execution;
import kieker.model.system.model.ExecutionTrace;

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

public class OpenTelemetryZipkinStage extends AbstractConsumerStage<ExecutionTrace> {

	private static boolean initialized = false;
	private int lastEss;
	private final Stack<Span> lastSpan = new Stack<Span>();

	public OpenTelemetryZipkinStage() {
		synchronized (OpenTelemetryZipkinStage.class) {
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

	int i = 0;

	@Override
	protected void execute(final ExecutionTrace trace) throws Exception {
		for (final Execution execution : trace.getTraceAsSortedExecutionSet()) {
			final String fullClassname = execution.getOperation().getComponentType().getFullQualifiedName().intern();

			final String operationSignature = ClassOperationSignaturePair.createOperationSignatureString(fullClassname, execution.getOperation().getSignature());

			final Tracer tracer = GlobalOpenTelemetry.getTracer("kieker-import");

			final SpanBuilder spanBuilder1 = tracer.spanBuilder(operationSignature);
			final SpanBuilder spanBuilder = spanBuilder1.setStartTimestamp(execution.getTin(), TimeUnit.NANOSECONDS);
			if (lastSpan != null && execution.getEss() > 0) {

				System.out.println("Parent: " + execution.getEss() + " " + execution.getEoi());

				spanBuilder.setParent(Context.current().with(lastSpan.peek()));
			} else {
				System.out.println("Root span");
			}

			System.out.println(spanBuilder.getClass());

			final Span span = spanBuilder.startSpan();

			try (Scope scope = span.makeCurrent()) {
				span.setAttribute("service.name", execution.getAllocationComponent().getIdentifier());
			} finally {
				span.end(execution.getTout(), TimeUnit.NANOSECONDS);
			}

			System.out.println("Spans added: " + ++i);

			if (execution.getEss() >= lastEss) {
				lastEss++;
				lastSpan.add(span);
			} else if (execution.getEss() == lastEss) {
				lastSpan.pop();
				lastSpan.add(span);
			} else {
				lastEss--;
				lastSpan.pop();
				lastSpan.add(span);
			}
		}
	}
}
