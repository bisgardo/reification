package reification;

import org.junit.Test;

import javax.tools.JavaFileObject;
import java.util.Arrays;

import static reification.TestFunctions.*;
import static reification.TestHelper.*;

public class PackageTest {
	
	@Test
	public void sameOutputPackage() {
		JavaFileObject X = inputSource("x.X", "package x; interface X<@Reify(String.class) T> {}");
		JavaFileObject X$String = generatedSource("x.X$String", "package x; public interface X$String extends X<String> {}");
		
		assertCompilesAndGenerates(X, X$String);
	}
	
	@Test
	public void differentSourcePackages() {
		JavaFileObject X = inputSource("x.X", "package x; interface X<@Reify(y.Y.class) T> {}");
		JavaFileObject Y = inputSource("y.Y", "package y; public interface Y {}");
		JavaFileObject X$Y = generatedSource("x.X$Y", "package x; import y.Y; public interface X$Y extends X<Y> {}");
		
		assertCompilesAndGenerates(Arrays.asList(X, Y), X$Y);
	}
	
	@Test
	public void samePackageUnqualified() {
		JavaFileObject X = inputSource("javax.swing.X", "package javax.swing; interface X<@Reify(JFrame.class) T> {}");
		JavaFileObject X$JFrame = generatedSource("javax.swing.X$JFrame", "package javax.swing; public interface X$JFrame extends X<JFrame> {}");
		
		assertCompilesAndGenerates(X, X$JFrame);
	}
	
	@Test
	public void samePackageQualified() {
		JavaFileObject X = inputSource("javax.swing.X", "package javax.swing; interface X<@Reify(javax.swing.JFrame.class) T> {}");
		JavaFileObject X$JFrame = generatedSource("javax.swing.X$JFrame", "package javax.swing; public interface X$JFrame extends X<JFrame> {}");
		
		assertCompilesAndGenerates(X, X$JFrame);
	}
	
	@Test
	public void differentPackageQualified() {
		JavaFileObject X = inputSource("X", "interface X<@Reify(javax.swing.JFrame.class) T> {}");
		JavaFileObject X$JFrame = generatedSource("X$JFrame", "import javax.swing.JFrame; public interface X$JFrame extends X<JFrame> {}");
		
		assertCompilesAndGenerates(X, X$JFrame);
	}
	
	@Test
	public void differentPackageImported() {
		JavaFileObject X = inputSource("X", "import javax.swing.JFrame; interface X<@Reify(javax.swing.JFrame.class) T> {}");
		JavaFileObject X$JFrame = generatedSource("X$JFrame", "import javax.swing.JFrame; public interface X$JFrame extends X<JFrame> {}");
		
		assertCompilesAndGenerates(X, X$JFrame);
	}
}
