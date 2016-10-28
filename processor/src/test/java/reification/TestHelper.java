package reification;

import com.google.testing.compile.CompileTester;
import com.google.testing.compile.JavaFileObjects;

import javax.tools.JavaFileObject;
import java.util.Objects;

import static reification.TestFunctions.assertAboutSourceThat;

public class TestHelper {
	
	private final CompileTester.CleanCompilationClause assertion;
	
	public TestHelper(CompileTester.CleanCompilationClause assertion) {
		this.assertion = Objects.requireNonNull(assertion);
	}
	
	public static TestHelper assertCompiles(String fullyQualifiedName, String source) {
		JavaFileObject fileObject = JavaFileObjects.forSourceString(fullyQualifiedName, source);
		return assertCompiles(fileObject);
	}
	
	public static TestHelper assertCompiles(JavaFileObject fileObject) {
		CompileTester.CleanCompilationClause assertion = assertAboutSourceThat(fileObject)
				.compilesWithoutWarnings();
		return new TestHelper(assertion);
	}
	
	public void andGeneratesSources(JavaFileObject first, JavaFileObject... rest) {
		assertion.and().generatesSources(first, rest);
	}
}
