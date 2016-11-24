package reification;

import org.junit.Test;

import javax.tools.JavaFileObject;

import static reification.TestFunctions.*;
import static reification.TestHelper.*;

public class SingleReferenceTypeTest {
	
	@Test
	public void nonAbstractClass() {
		JavaFileObject X = inputSource("X", "class X<@Reify(String.class) T> {}");
		JavaFileObject X$String = generatedSource("X$String", "public class X$String extends X<String> {}");
		
		assertCompilesAndGenerates(X, X$String);
	}
	
	@Test
	public void abstractClassWithoutAbstractMethods() {
		JavaFileObject X = inputSource("X", "abstract class X<@Reify(String.class) T> {}");
		JavaFileObject X$String = generatedSource("X$String", "public class X$String extends X<String> {}");
		
		assertCompilesAndGenerates(X, X$String);
	}
	
	@Test
	public void abstractClassWithUnknownAbstractMethods() {
		JavaFileObject X = inputSource("X", "abstract class X<@Reify(String.class) T> { abstract int f(); }");
		JavaFileObject X$String = generatedSource("X$String", "public abstract class X$String extends X<String> {}");
		
		assertCompilesAndGenerates(X, X$String);
	}
	
	@Test
	public void emptyInterface() {
		JavaFileObject X = inputSource("X", "interface X<@Reify(String.class) T> {}");
		JavaFileObject X$String = generatedSource("X$String", "public interface X$String extends X<String> {}");
		
		assertCompilesAndGenerates(X, X$String);
	}
	
	@Test
	public void nonEmptyInterface() {
		JavaFileObject X = inputSource("X", "interface X<@Reify(String.class) T> { int f(); }");
		JavaFileObject X$String = generatedSource("X$String", "public interface X$String extends X<String> {}");
		
		assertCompilesAndGenerates(X, X$String);
	}
}
