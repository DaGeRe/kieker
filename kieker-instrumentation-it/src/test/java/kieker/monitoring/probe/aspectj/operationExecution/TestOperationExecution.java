package kieker.monitoring.probe.aspectj.operationExecution;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.example.Instrumentable;

public class TestOperationExecution {

	@Before
	public void clearFolder() {
		Util.clearFolder();
	}

	@Test
	public void testRegularExecution() throws IOException {
		Instrumentable instrumentable = new Instrumentable();
		instrumentable.callee();
		
		List<String> lines = Util.getLatestLogRecord();
		String firstSignature = lines.get(1).split(";")[2];
		Assert.assertEquals("public net.example.Instrumentable.<init>()", firstSignature);
		String secondSignature = lines.get(2).split(";")[2];
		Assert.assertEquals("public void net.example.Instrumentable.callee2(java.lang.String)", secondSignature);
		String thirdSignature = lines.get(3).split(";")[2];
		Assert.assertEquals("public void net.example.Instrumentable.callee()", thirdSignature);
	}

}
