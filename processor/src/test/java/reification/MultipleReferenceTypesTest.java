package reification;

import org.junit.Test;

import javax.tools.JavaFileObject;

import static reification.TestFunctions.*;

public class MultipleReferenceTypesTest {
	
	@Test
	public void classWithTwoReifiedParameters() {
		// TODO Make this compile.
		JavaFileObject X = inputSource("X", "class X<@Reify(String.class) T1, @Reify(String.class) T2> {}");
		assertAboutSourceThat(X)
				.failsToCompile()
				.withErrorCount(1)
				.withErrorContaining("not yet implemented");
	}
	
	@Test
	public void classWithTwoParametersFirstReified() {
		// TODO Make this compile.
		JavaFileObject X = inputSource("X", "class X<@Reify(String.class) T1, T2> {}");
		assertAboutSourceThat(X)
				.failsToCompile()
				.withErrorCount(1)
				.withErrorContaining("not yet implemented");
	}
	
	@Test
	public void classWithTwoParametersSecondReified() {
		// TODO Make this compile.
		JavaFileObject X = inputSource("X", "class X<T1, @Reify(String.class) T2> {}");
		assertAboutSourceThat(X)
				.failsToCompile()
				.withErrorCount(1)
				.withErrorContaining("not yet implemented");
	}
}
