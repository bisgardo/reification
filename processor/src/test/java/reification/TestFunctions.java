package reification;

import com.google.testing.compile.CompileTester;
import com.google.testing.compile.JavaFileObjects;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

public class TestFunctions {
	
	public static CompileTester assertAboutSourceThat(JavaFileObject fileObject) {
		// Check that file compiles without warnings when annotation processing is not performed.
		assertAbout(javaSource()).that(fileObject).compilesWithoutWarnings();
		
		return assertAbout(javaSource()).that(fileObject).processedWith(new ReificationProcessor());
	}
	
	public static void assertCompilesAndGenerates(JavaFileObject input, JavaFileObject firstGenerated, JavaFileObject... remainingGenerated) {
		TestHelper.assertCompiles(input).andGeneratesSources(firstGenerated, remainingGenerated);
	}
	
	public static JavaFileObject inputSource(String fullyQualifiedName, String source) {
		return JavaFileObjects.forSourceString(fullyQualifiedName, source.replace("@Reify", "@reification.Reify"));
	}
	
	public static JavaFileObject generatedSource(String fullyQualifiedName, String source) {
		return JavaFileObjects.forSourceString("/SOURCE_OUTPUT/" + fullyQualifiedName, source);
	}
}
