package reification;

import org.junit.Test;

import javax.tools.JavaFileObject;

import static reification.TestFunctions.assertAboutSourceThat;
import static reification.TestFunctions.inputSource;

public class FinalClassTest {
	
	@Test
	public void singleReferenceTypeParameter() {
		JavaFileObject X = inputSource("X", "final class X<@Reify(String.class) T> {}");
		assertAboutSourceThat(X)
				.failsToCompile()
				.withErrorCount(1)
				.withErrorContaining("not yet implemented");
	}
}
