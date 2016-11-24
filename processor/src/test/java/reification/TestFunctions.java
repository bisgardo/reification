package reification;

import com.google.testing.compile.CompileTester;
import com.google.testing.compile.JavaFileObjects;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

public class TestFunctions {
	
	public static CompileTester assertAboutProcessedSourceThat(JavaFileObject fileObject) {
		// Check that file compiles without warnings when annotation processing is not performed.
		assertAbout(javaSource()).that(fileObject).compilesWithoutWarnings();
		
		return assertAbout(javaSource()).that(fileObject).processedWith(new ReificationProcessor());
	}
	
	public static CompileTester assertAboutProcessedSourcesThat(Iterable<JavaFileObject> fileObjects) {
		// Check that file compiles without warnings when annotation processing is not performed.
		assertAbout(javaSources()).that(fileObjects).compilesWithoutWarnings();
		
		return assertAbout(javaSources()).that(fileObjects).processedWith(new ReificationProcessor());
	}
	
	public static JavaFileObject inputSource(String fullyQualifiedName, String source) {
		return JavaFileObjects.forSourceString(fullyQualifiedName, source.replace("@Reify", "@reification.Reify"));
	}
	
	public static JavaFileObject generatedSource(String fullyQualifiedName, String source) {
		return JavaFileObjects.forSourceString("/SOURCE_OUTPUT/" + fullyQualifiedName, source);
	}
	
	public static String lines(String... remainingSourceLines) {
		StringBuilder stringBuilder = new StringBuilder();
		for (String sourceLine : remainingSourceLines) {
			if (stringBuilder.length() > 0) {
				stringBuilder.append('\n');
			}
			stringBuilder.append(sourceLine);
		}
		
		return stringBuilder.toString();
	}
}
