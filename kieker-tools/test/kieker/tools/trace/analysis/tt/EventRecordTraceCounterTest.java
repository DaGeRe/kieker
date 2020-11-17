/***************************************************************************
 * Copyright 2020 Kieker Project (http://kieker-monitoring.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package kieker.tools.trace.analysis.tt;

import org.junit.Assert;
import org.junit.Test;

import kieker.analysis.stage.flow.TraceEventRecords;
import kieker.analysis.trace.EventRecordTraceCounter;

import kieker.test.tools.util.tt.bookstore.BookstoreEventRecordFactory;

import teetime.framework.test.StageTester;

/**
 *
 * @author Reiner Jung
 *
 * @since 1.15
 */
public class EventRecordTraceCounterTest {

	public EventRecordTraceCounterTest() {
		// TODO Auto-generated constructor stub
	}

	@Test
	public void testCountingEvents() {
		final TraceEventRecords traceEventRecords = BookstoreEventRecordFactory.validSyncTraceBeforeAfterEvents(1, 1, "sessionId", "testhost");

		final EventRecordTraceCounter filter = new EventRecordTraceCounter(true);

		StageTester.test(filter).and().send(traceEventRecords).to(filter.getValidEventRecordTraceInputPort()).start();

		Assert.assertEquals("Expected a complete trace", filter.getSuccessCount(), 1);
	}

}
