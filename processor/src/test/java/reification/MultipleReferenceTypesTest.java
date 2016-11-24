package reification;

import org.junit.Test;

import javax.tools.JavaFileObject;

import static reification.TestFunctions.*;

public class MultipleReferenceTypesTest {
	
	@Test
	public void classWithTwoReifiedParameters() {
		// TODO Make this compile.
		JavaFileObject X = inputSource("X", "class X<@Reify(String.class) T1, @Reify(String.class) T2> {}");
		assertAboutProcessedSourceThat(X)
				.failsToCompile()
				.withErrorCount(1)
				.withErrorContaining(Message.NOT_YET_IMPLEMENTED);
	}
	
	@Test
	public void classWithTwoParametersFirstReified() {
		// TODO Make this compile.
		JavaFileObject X = inputSource("X", "class X<@Reify(String.class) T1, T2> {}");
		assertAboutProcessedSourceThat(X)
				.failsToCompile()
				.withErrorCount(1)
				.withErrorContaining(Message.NOT_YET_IMPLEMENTED);
	}
	
	@Test
	public void classWithTwoParametersSecondReified() {
		// TODO Make this compile.
		JavaFileObject X = inputSource("X", "class X<T1, @Reify(String.class) T2> {}");
		assertAboutProcessedSourceThat(X)
				.failsToCompile()
				.withErrorCount(1)
				.withErrorContaining(Message.NOT_YET_IMPLEMENTED);
	}
}
