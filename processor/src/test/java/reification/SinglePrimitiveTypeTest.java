package reification;

import org.junit.Test;

import javax.tools.JavaFileObject;

import static reification.TestFunctions.*;

public class SinglePrimitiveTypeTest {
	
	@Test
	public void classWithSingleInt() {
		JavaFileObject X = inputSource("X", "class X<@Reify(int.class) T> {}");
		assertAboutSourceThat(X)
				.failsToCompile()
				.withErrorCount(1)
				.withErrorContaining("not yet implemented");
	}
}
