package reification;

import org.junit.Test;

import javax.tools.JavaFileObject;

import static reification.TestFunctions.assertAboutSourceThat;
import static reification.TestFunctions.inputSource;
import static reification.TestHelper.assertCompiles;

public class InnerClassTest {
	
	@Test
	public void reifiedClassWithNonStaticInnerClass() {
		JavaFileObject X = inputSource("X", "class X<@Reify(String.class) T1> { class Y<T2> {} }");
		assertCompiles(X);
	}
	
	@Test
	public void reifiedClassWithStaticInnerClass() {
		JavaFileObject X = inputSource("X", "class X<@Reify(String.class) T1> { static class Y<T2> {} }");
		assertCompiles(X);
	}
	
	@Test
	public void reifiedClassWithNonStaticReifiedInnerClass() {
		JavaFileObject X = inputSource("X", "class X<@Reify(String.class) T1> { class Y<@Reify(String.class) T1> {} }");
		assertAboutSourceThat(X)
				.failsToCompile()
				.withErrorCount(1)
				.withErrorContaining("not supported");
	}
	
	@Test
	public void nonReifiedClassWithNonStaticReifiedInnerClass() {
		JavaFileObject X = inputSource("X", "class X { class Y<@Reify(String.class) T1> {} }");
		assertAboutSourceThat(X)
				.failsToCompile()
				.withErrorCount(1)
				.withErrorContaining("not supported");
	}
	
	@Test
	public void reifiedClassWithStaticReifiedInnerClass() {
		JavaFileObject X = inputSource("X", "class X<@Reify(String.class) T1> { static class Y<@Reify(String.class) T1> {} }");
		assertAboutSourceThat(X)
				.failsToCompile()
				.withErrorCount(1)
				.withErrorContaining("not yet implemented");
	}
	
	@Test
	public void nonReifiedClassWithStaticReifiedInnerClass() {
		JavaFileObject X = inputSource("X", "class X { static class Y<@Reify(String.class) T1> {} }");
		assertAboutSourceThat(X)
				.failsToCompile()
				.withErrorCount(1)
				.withErrorContaining("not yet implemented");
	}
}
