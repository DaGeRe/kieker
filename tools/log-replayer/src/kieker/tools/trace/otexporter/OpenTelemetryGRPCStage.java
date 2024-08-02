package kieker.tools.trace.otexporter;

import java.util.HashMap;
import java.util.Map;
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
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import teetime.framework.AbstractConsumerStage;

public class OpenTelemetryGRPCStage extends AbstractConsumerStage<ExecutionTrace> {

	private static boolean initialized = false;
	private int lastEss;
	private final Stack<Span> lastSpan = new Stack<Span>();

	public OpenTelemetryGRPCStage() {
		synchronized (OpenTelemetryGRPCStage.class) {
			if (!initialized) {
				createTracerProvider("kieker-data");
				initialized = true;
			}
		}
	}

	private SdkTracerProvider createTracerProvider(final String serviceName) {
		final Resource resource = Resource.getDefault().merge(Resource
				.create(Attributes.builder().put(AttributeKey.stringKey("service.name"), serviceName).build()));

		final SpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
				// .setEndpoint("http://localhost:55678/")
				.build();
		final SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().setResource(resource)
				.addSpanProcessor(BatchSpanProcessor
						.builder(spanExporter)
						.build())
				.build();

		OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider).buildAndRegisterGlobal();
		return sdkTracerProvider;
	}

	private int i = 0;
	private int serviceIndex = 0;
	private final Map<String, String> serviceIndexMap = new HashMap<>();

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
				final String serviceName = execution.getAllocationComponent().getExecutionContainer().getName();
				span.setAttribute("service.name", serviceName);
				String serviceInstanceId = serviceIndexMap.get(serviceName);
				if (serviceInstanceId == null) {
					serviceInstanceId = Integer.toString(serviceIndex++);
					serviceIndexMap.put(serviceName, serviceInstanceId);
				}
				span.setAttribute("service.instance.id", serviceInstanceId);
				span.setAttribute("code.namespace", fullClassname);
				span.setAttribute("code.function", execution.getOperation().getSignature().getName());
				span.setAttribute("telemetry.sdk.language", "java");
				span.setAttribute("explorviz.token.id", "mytokenvalue");
				span.setAttribute("explorviz.token.secret", "mytokensecret");
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
