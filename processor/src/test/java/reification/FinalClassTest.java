package reification;

import org.junit.Test;

import javax.tools.JavaFileObject;

import static reification.TestFunctions.*;

public class FinalClassTest {
	
	@Test
	public void singleReferenceTypeParameter() {
		JavaFileObject X = inputSource("X", "final class X<@Reify(String.class) T> {}");
		assertAboutProcessedSourceThat(X)
				.failsToCompile()
				.withErrorCount(1)
				.withErrorContaining(Message.NOT_YET_IMPLEMENTED);
	}
}
