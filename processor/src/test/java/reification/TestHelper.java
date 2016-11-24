package reification;

import com.google.testing.compile.CompileTester;
import com.google.testing.compile.JavaFileObjects;

import javax.tools.JavaFileObject;
import java.util.Objects;

import static reification.TestFunctions.*;

public class TestHelper {
	
	private final CompileTester.CleanCompilationClause assertion;
	
	public TestHelper(CompileTester.CleanCompilationClause assertion) {
		this.assertion = Objects.requireNonNull(assertion);
	}
	
	public void andGeneratesSources(JavaFileObject first, JavaFileObject... rest) {
		assertion.and().generatesSources(first, rest);
	}
	
	/* SINGLE FILE */
	
	public static TestHelper assertCompiles(String fullyQualifiedName, String source) {
		JavaFileObject fileObject = JavaFileObjects.forSourceString(fullyQualifiedName, source);
		return assertCompiles(fileObject);
	}
	
	public static TestHelper assertCompiles(JavaFileObject fileObject) {
		CompileTester.CleanCompilationClause assertion = assertAboutProcessedSourceThat(fileObject)
				.compilesWithoutWarnings();
		return new TestHelper(assertion);
	}
	
	public static void assertCompilesAndGenerates(JavaFileObject input, JavaFileObject firstGenerated, JavaFileObject... remainingGenerated) {
		assertCompiles(input).andGeneratesSources(firstGenerated, remainingGenerated);
	}
	
	/* MULTIPLE FILES */
	
	public static TestHelper assertCompiles(Iterable<JavaFileObject> fileObjects) {
		CompileTester.CleanCompilationClause assertion = assertAboutProcessedSourcesThat(fileObjects)
				.compilesWithoutWarnings();
		return new TestHelper(assertion);
	}
	
	public static void assertCompilesAndGenerates(Iterable<JavaFileObject> inputs, JavaFileObject firstGenerated, JavaFileObject... remainingGenerated) {
		assertCompiles(inputs).andGeneratesSources(firstGenerated, remainingGenerated);
	}
}
