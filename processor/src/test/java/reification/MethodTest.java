package reification;

import org.junit.Test;

import javax.tools.JavaFileObject;

import static reification.TestFunctions.*;
import static reification.TestHelper.assertCompilesAndGenerates;

public class MethodTest {
	
	/* CLASS METHOD */
	
	@Test
	public void classMethodOnClass() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"abstract class X<@Reify(String.class) T> {",
						"    abstract Class<T> classT();           ",
						"}                                         "
				)
		);
		JavaFileObject X$String = generatedSource(
				"X$String",
				lines(
						"public class X$String extends X<String> {",
						"    @Override                            ",
						"    Class<String> classT() {             ",
						"        return String.class;             ",
						"    }                                    ",
						"}                                        "
				)
		);
		
		assertCompilesAndGenerates(X, X$String);
	}
	
	@Test
	public void classMethodOnClassWithParameter() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"abstract class X<@Reify(String.class) T> {  ",
						"    abstract Class<T> classT(Object object);",
						"}                                           "
				)
		);
		
		assertAboutProcessedSourceThat(X)
				.failsToCompile()
				.withErrorCount(1)
				.withErrorContaining("must not have any parameters");
	}
	
	@Test
	public void classMethodOnInterface() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"interface X<@Reify(String.class) T> {",
						"    Class<T> classT();               ",
						"}                                    "
				)
		);
		JavaFileObject X$String = generatedSource(
				"X$String",
				lines(
						"public interface X$String extends X<String> {",
						"    @Override                                ",
						"    default Class<String> classT() {         ",
						"        return String.class;                 ",
						"    }                                        ",
						"}                                            "
				)
		);
		
		assertCompilesAndGenerates(X, X$String);
	}
	
	@Test
	public void matchingClassMethodOnInterface() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"interface X<@Reify(String.class) T> {",
						"    Class<String> classT();          ",
						"}                                    "
				)
		);
		JavaFileObject X$String = generatedSource(
				"X$String",
				lines(
						"public interface X$String extends X<String> {",
						"    @Override                                ",
						"    default Class<String> classT() {         ",
						"        return String.class;                 ",
						"    }                                        ",
						"}                                            "
				)
		);
		
		assertCompilesAndGenerates(X, X$String);
	}
	
	@Test
	public void nonMatchingClassMethodOnInterface() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"interface X<@Reify(String.class) T> {",
						"    Class<Object> classT();          ",
						"}                                    "
				)
		);
		assertAboutProcessedSourceThat(X)
				.failsToCompile();
	}
	
	@Test
	public void unboundedWildcardClassMethodOnInterface() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"interface X<@Reify(String.class) T> {",
						"    Class<?> classT();               ",
						"}                                    "
				)
		);
		JavaFileObject X$String = generatedSource(
				"X$String",
				lines(
						"public interface X$String extends X<String> {",
						"    @Override                                ",
						"    default Class<String> classT() {         ",
						"        return String.class;                 ",
						"    }                                        ",
						"}                                            "
				)
		);
		
		assertCompilesAndGenerates(X, X$String);
	}
	
	@Test
	public void wildcardExtendsClassMethodOnInterface() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"interface X<@Reify(String.class) T> {",
						"    Class<? extends T> classT();     ",
						"}                                    "
				)
		);
		JavaFileObject X$String = generatedSource(
				"X$String",
				lines(
						"public interface X$String extends X<String> {",
						"    @Override                                ",
						"    default Class<String> classT() {         ",
						"        return String.class;                 ",
						"    }                                        ",
						"}                                            "
				)
		);
		
		assertCompilesAndGenerates(X, X$String);
	}
	
	@Test
	public void wildcardSuperClassMethodOnInterface() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"interface X<@Reify(String.class) T> {",
						"    Class<? super T> classT();     ",
						"}                                    "
				)
		);
		JavaFileObject X$String = generatedSource(
				"X$String",
				lines(
						"public interface X$String extends X<String> {",
						"    @Override                                ",
						"    default Class<String> classT() {         ",
						"        return String.class;                 ",
						"    }                                        ",
						"}                                            "
				)
		);
		
		assertCompilesAndGenerates(X, X$String);
	}
	
	/* NEW INSTANCE METHOD */
	
	@Test
	public void newInstanceMethodOnClassWithNoParameterConstructor() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"abstract class X<@Reify(String.class) T> {",
						"    abstract T newT();                    ",
						"}                                         "
				)
		);
		JavaFileObject X$String = generatedSource(
				"X$String",
				lines(
						"public class X$String extends X<String> {",
						"    @Override                            ",
						"    String newT() {                      ",
						"        return new String();             ",
						"    }                                    ",
						"}                                        "
				)
		);
		
		assertCompilesAndGenerates(X, X$String);
	}
	
	@Test
	public void newInstanceMethodOnClassWithSingleParameterConstructor() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"abstract class X<@Reify(String.class) T> {",
						"    abstract T newT(String string);       ",
						"}                                         "
				)
		);
		JavaFileObject X$String = generatedSource(
				"X$String",
				lines(
						"public class X$String extends X<String> { ",
						"    @Override                             ",
						"    String newT(String string) {          ",
						"        return new String(string);        ",
						"    }                                     ",
						"}                                         "
				)
		);
		
		assertCompilesAndGenerates(X, X$String);
	}
	
	@Test
	public void newInstanceMethodOnClassWithMultipleParametersConstructor() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"abstract class X<@Reify(String.class) T> {               ",
						"    abstract T newT(char value[], int offset, int count);",
						"}                                                        "
				)
		);
		JavaFileObject X$String = generatedSource(
				"X$String",
				lines(
						"public class X$String extends X<String> {             ",
						"    @Override                                         ",
						"    String newT(char[] value, int offset, int count) {",
						"        return new String(value, offset, count);      ",
						"    }                                                 ",
						"}                                                     "
				)
		);
		
		assertCompilesAndGenerates(X, X$String);
	}
	
	@Test
	public void newInstanceMethodOnInterface() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"interface X<@Reify(String.class) T> {",
						"    T newT();                        ",
						"}                                    "
				)
		);
		JavaFileObject X$String = generatedSource(
				"X$String",
				lines(
						"public interface X$String extends X<String> {",
						"    @Override                                ",
						"    default String newT() {                  ",
						"        return new String();                 ",
						"    }                                        ",
						"}                                            "
				)
		);
		
		assertCompilesAndGenerates(X, X$String);
	}
	
	/* NON-ABSTRACT */
	
	@Test
	public void nonAbstractClassMethodOnClassSimple() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"class X<@Reify(String.class) T> {     ",
						"    Class<T> classT() { return null; }",
						"}                                     "
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
	public void nonAbstractClassMethodOnClassComplex() {
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
	
	// TODO Add test with indirectly inherited abstract methods, incl.
	//      - collisions (might not be our problem, though)
	//      - overloading (for 'newInstance' only).
	//      - both interface and superclass)
	//      - matching name but not abstract
	//      - abstract but also implemented.
	
	/* BOTH METHODS */
}
