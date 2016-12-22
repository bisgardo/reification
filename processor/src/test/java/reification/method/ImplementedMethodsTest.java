package reification.method;

import org.junit.Test;

import javax.tools.JavaFileObject;

import java.util.Arrays;

import static reification.TestFunctions.*;
import static reification.TestHelper.assertCompilesAndGenerates;

public class ImplementedMethodsTest {
	
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
	public void nonClassMethodOnClass() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"abstract class X<@Reify(String.class) T> {",
						"    abstract Class<T> classR();           ",
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
	public void classMethodWithTypeParameter() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"abstract class X<@Reify(String.class) T> {",
						"    abstract <R> Class<R> classT();       ",
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
	public void classMethodWithShadowingTypeParameter() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"abstract class X<@Reify(String.class) T> {",
						"    abstract <T> Class<T> classT();       ",
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
	public void classMethodWithShadowingTypeParameterWithinBounds() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"abstract class X<@Reify(String.class) T> {        ",
						"    abstract <T extends String> Class<T> classT();",
						"}                                                 "
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
	public void classMethodWithShadowingTypeParameterOutsideBounds() {
		// Not sure why this is valid Java, but apparently it is.
		JavaFileObject X = inputSource(
				"X",
				lines(
						"abstract class X<@Reify(String.class) T> {         ",
						"    abstract <T extends Integer> Class<T> classT();",
						"}                                                  "
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
	public void compatibleClassMethodOnInterface() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"interface X<@Reify(String.class) T> {",
						"    Object classT();                 ",
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
						"    Integer classT();                ",
						"}                                    "
				)
		);
		assertAboutProcessedSourceThat(X)
				.failsToCompile();
	}
	
	@Test
	public void nonMatchingGenericClassMethodOnInterface() {
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
						"    Class<? super T> classT();       ",
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
	public void classMethodOfInterfaceType() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"interface X<@Reify(CharSequence.class) T> {",
						"    Class<T> classT();                     ",
						"}                                          "
				)
		);
		JavaFileObject X$String = generatedSource(
				"X$CharSequence",
				lines(
						"public interface X$CharSequence extends X<CharSequence> {",
						"    @Override                                            ",
						"    default Class<CharSequence> classT() {               ",
						"        return CharSequence.class;                       ",
						"    }                                                    ",
						"}                                                        "
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
						"    abstract T newT(char[] value, int offset, int count);",
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
	
	@Test
	public void newInstanceMethodWithoutExceptions() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"interface X<@Reify(String.class) T> {",
						"    T newT() throws Exception;       ",
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
	
	@Test
	public void newInstanceMethodWithoutMatchingConstructor() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"interface X<@Reify(String.class) T> {",
						"    T newT(Object o);                ",
						"}                                    "
				)
		);
		assertAboutProcessedSourceThat(X).failsToCompile().withErrorContaining("Could not resolve constructor");
	}
	
	@Test
	public void newInstanceMethodOfInterfaceType() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"interface X<@Reify(CharSequence.class) T> {",
						"    T newT();                              ",
						"}                                          "
				)
		);
		assertAboutProcessedSourceThat(X).failsToCompile().withErrorContaining("Could not resolve constructor");
	}
	
	@Test
	public void newInstanceMethodWithExceptions() {
		JavaFileObject U = inputSource(
				"U",
				lines(
						"public class U {                                                              ",
						"    U() throws java.io.IOException, RuntimeException, java.sql.SQLException {}",
						"}                                                                             "
				)
		);
		JavaFileObject X = inputSource(
				"X",
				lines(
						"interface X<@Reify(U.class) T> {",
						"    T newT() throws Exception;  ",
						"}                               "
				)
		);
		JavaFileObject X$String = generatedSource(
				"X$U",
				lines(
						"import java.io.IOException;                                              ",
						"import java.sql.SQLException;                                            ",
						"public interface X$U extends X<U> {                                      ",
						"    @Override                                                            ",
						"    default U newT() throws IOException, RuntimeException, SQLException {",
						"        return new U();                                                  ",
						"    }                                                                    ",
						"}                                                                        "
				)
		);
		
		assertCompilesAndGenerates(Arrays.asList(U, X), X$String);
	}
	
	@Test
	public void classAndMultipleNewInstanceMethods() {
		JavaFileObject X = inputSource(
				"X",
				lines(
						"interface X<@Reify(String.class) T> {",
						"    Class<T> classT();               ",
						"    T newT();                        ",
						"    T newT(String s);                ",
						"    T newT(char[] v, int o, int c);  ",
						"}                                    "
				)
		);
		JavaFileObject X$String = generatedSource(
				"X$String",
				lines(
						"public interface X$String extends X<String> {    ",
						"    @Override                                    ",
						"    default Class<String> classT() {             ",
						"        return String.class;                     ",
						"    }                                            ",
						"                                                 ",
						"    @Override                                    ",
						"    default String newT() {                      ",
						"        return new String();                     ",
						"    }                                            ",
						"                                                 ",
						"    @Override                                    ",
						"    default String newT(String s) {              ",
						"        return new String(s);                    ",
						"    }                                            ",
						"                                                 ",
						"    @Override                                    ",
						"    default String newT(char[] v, int o, int c) {",
						"        return new String(v, o, c);              ",
						"    }                                            ",
						"}                                                "
				)
		);
		
		assertCompilesAndGenerates(X, X$String);
	}
	
	// TODO Add test with indirectly inherited abstract methods, incl.
	//      - collisions (might not be our problem, though)
	//      - overloading (for 'newInstance' only).
	//      - both interface and superclass
	//      - abstract but also implemented.
	//      - class remains abstract if there are extra abstract methods.
	
}
