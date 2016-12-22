package reification.method;

import org.junit.Test;

import javax.tools.JavaFileObject;

import static reification.TestFunctions.generatedSource;
import static reification.TestFunctions.inputSource;
import static reification.TestFunctions.lines;
import static reification.TestHelper.assertCompilesAndGenerates;

public class NonImplementedMethodsTest {
	
	@Test
	public void classWithUnknownAbstractMethod() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"abstract class X<@Reify(String.class) T> {",
						"    abstract int f();                     ",
						"}                                         "
				)
		);
		JavaFileObject X$String = generatedSource(
				"X$String",
				lines(
						"public abstract class X$String extends X<String> {",
						"}                                                 "
				)
		);
		
		assertCompilesAndGenerates(X, X$String);
	}
	
	
	@Test
	public void nonAbstractClassMethod() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"class X<@Reify(String.class) T> {   ",
						"    String classT() { return null; }",
						"}                                   "
				)
		);
		JavaFileObject X$String = generatedSource(
				"X$String",
				lines(
						"public class X$String extends X<String> {",
						"}                                        "
				)
		);
		
		assertCompilesAndGenerates(X, X$String);
	}
	
	@Test
	public void nonAbstractClassMethodGeneric() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"class X<@Reify(String.class) T extends CharSequence> {",
						"    Class<? super T> classT() {                       ",
						"        return CharSequence.class;                    ",
						"    }                                                 ",
						"}                                                     "
				)
		);
		JavaFileObject X$String = generatedSource(
				"X$String",
				lines(
						"public class X$String extends X<String> {",
						"}                                        "
				)
		);
		
		assertCompilesAndGenerates(X, X$String);
	}
	
	// TODO Test that inherited abstract methods are not auto-implemented (and make the code work that way).
}
