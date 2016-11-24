package reification;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import com.sun.tools.javac.code.Symbol.ClassSymbol;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static javax.lang.model.element.Modifier.*;
import static javax.lang.model.type.TypeKind.DECLARED;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;

public class ReificationProcessor extends AbstractProcessor {
	
	private Elements elementUtils;
	private Filer filer;
	private Messager messager;
	
	private SourceGenerator sourceGenerator;
	
	@Override
	public synchronized void init(ProcessingEnvironment environment) {
		super.init(environment);
		
		Types typeUtils = environment.getTypeUtils();
		
		elementUtils = environment.getElementUtils();
		filer = environment.getFiler();
		messager = environment.getMessager();
		
		sourceGenerator = new SourceGenerator(typeUtils, messager);
		
		messager.printMessage(NOTE, "Initializing '@reification.Reify'-annotation processor");
	}
	
	// TODO Refactor into sensible abstractions.
	@Override
	public boolean process(Set<? extends TypeElement> annotationElements, RoundEnvironment environment) {
		// Method gets called again with generated files.
		if (annotationElements.isEmpty()) {
			return false;
		}
		
		for (TypeElement annotationElement : annotationElements) {
			for (Element rootElement : environment.getRootElements()) {
				ElementKind kind = rootElement.getKind();
				if (!kind.isInterface() && !kind.isClass()) {
					messager.printMessage(
							ERROR,
							String.format(
									"'@Reify'-annotation in non-type '%s' of kind '%s' is not supported",
									rootElement,
									kind
							),
							rootElement
					);
					continue;
				}
				
				ClassSymbol typeElement = (ClassSymbol) rootElement;
				
				Set<? extends Element> annotatedElements = environment.getElementsAnnotatedWith(annotationElement);
				for (Element annotatedElement : annotatedElements) {
					Element enclosingElement = annotatedElement.getEnclosingElement();
					
					if (typeElement.equals(enclosingElement)) {
						process(typeElement);
					} else if (ElementFunctions.enclosingElements(enclosingElement).contains(typeElement)) {
						if (enclosingElement.getModifiers().contains(STATIC)) {
							messager.printMessage(
									ERROR,
									String.format(
											"'@Reify'-annotation in static inner class '%s' is not yet implemented",
											enclosingElement
									),
									enclosingElement
							);
							break;
						} else {
							messager.printMessage(
									ERROR,
									String.format(
											"'@Reify'-annotation in non-static inner class '%s' is not supported",
											enclosingElement
									),
									enclosingElement
							);
							break;
						}
					}
				}
				
			}
		}
		
		return true;
	}
	
	private void process(ClassSymbol typeElement) {
		if (typeElement.getModifiers().contains(FINAL)) {
			// TODO Support reification of final classes: Implement in two iterations:
			//      1. Require the class to have no methods (abstract methods would not be a problem, but since the
			//         class is final, it cannot have any). The solution is then to just extend the type's superclass
			//         instead of the type itself and implement type's interfaces.
			//      2. Read public field `sourcefile` of `typeElement`, use some framework to parse the methods in
			//         question, reify them, and then toss them back into the generated class.
			//      If at some point one wants to reify an un-modifiable type, this will provide a rather clean way of
			//      doing it: Extend the class with a final one). Also, see implementation comment on primitive types
			//      below.
			messager.printMessage(
					ERROR,
					String.format("'@Reify'-annotation in final class '%s' not yet implemented", typeElement),
					typeElement
			);
			return;
		}
		
		PackageElement packageElement = elementUtils.getPackageOf(typeElement);
		String generatedPackageName = "";
		if (!packageElement.isUnnamed()) {
			generatedPackageName = packageElement.toString();
		}
		
		// TODO For each combination of of annotated type parameters, generate an extending class with the type parameter.
		
		// Current simplified solution only supports single, non-primitive type parameter.
		List<? extends TypeParameterElement> typeParameters = typeElement.getTypeParameters();
		if (typeParameters.isEmpty()) {
			// Since the processor should only be activated for classes with annotated type parameters, it shouldn't be
			// possible that an activated class has no type parameters.
			messager.printMessage(ERROR, "Class without type parameters not expected", typeElement);
			return;
		}
		if (typeParameters.size() > 1) {
			messager.printMessage(
					ERROR,
					String.format(
							"'@Reify'-annotation in class '%s' with multiple type variables %s is not yet implemented",
							typeElement,
							typeParameters
					),
					typeElement
			);
			return;
		}
		
		TypeParameterElement typeParameterElement = typeParameters.get(0);
		
		Reify[] annotations = typeParameterElement.getAnnotationsByType(Reify.class);
		if (annotations.length == 0) {
			return;
		}
		if (annotations.length > 1) {
			messager.printMessage(
					ERROR,
					"Repeated '@Reify'-annotations is not yet implemented",
					typeParameterElement
			);
			return;
		}
		
		Reify annotation = annotations[0];
		
		// `Class`-object can't be accessed on compile-time. See
		// 'https://blog.retep.org/2009/02/13/getting-class-values-from-annotations-in-an-annotationprocessor/'.
		DeclaredType typeArgumentMirror;
		try {
			annotation.value();
			throw new IllegalStateException("Expected 'MirroredTypeException'");
		} catch (MirroredTypeException e) {
			TypeMirror typeMirror = e.getTypeMirror();
			TypeKind typeMirrorKind = typeMirror.getKind();
			if (typeMirrorKind.isPrimitive()) {
				// TODO Support reification of primitive types. Like with final classes (see TODO above), the
				//      generated cannot extend "type" (except in useless cases when the parameter isn't used - we
				//      don't care about such cases). The generated type's superclass should be the "closest"
				//      superclass of "type" that doesn't define any type parameter that are being reified into
				//      primitive types. Also, all interfaces not containing these type parameters should be
				//      implemented as well.
				//      In short, consider this to be a complex expansion of the requirement to support final classes.
				messager.printMessage(
						ERROR,
						String.format(
								"'@Reify'-annotation with primitive type '%s' on type parameter is not yet implemented",
								typeMirror
						),
						typeParameterElement
				);
				return;
			}
			if (typeMirrorKind != DECLARED) {
				messager.printMessage(
						ERROR,
						String.format(
								"Expected type mirror '%s' of kind '%s' to be of kind 'DECLARED'",
								typeMirrorKind,
								typeMirror
						),
						typeParameterElement
				);
				return;
			}
			typeArgumentMirror = (DeclaredType) typeMirror;
		}
		
		String typeName = typeElement.getSimpleName().toString();
		String typeArgumentName = typeArgumentMirror.asElement().getSimpleName().toString();
		String generatedClassName = typeName + '$' + typeArgumentName;
		
		TypeSpec.Builder javaFileBuilder = sourceGenerator.generateSource(
				typeElement,
				typeArgumentMirror,
				generatedClassName,
				typeElement
		);
		if (javaFileBuilder == null) {
			// Error has been reported to `messager` from within `sourceGenerator`.
			return;
		}
		
		// Build compilation unit.
		TypeSpec typeSpec = javaFileBuilder
				.addModifiers(PUBLIC)
				.addOriginatingElement(typeElement)
				.build();
		
		JavaFile javaFile = JavaFile.builder(generatedPackageName, typeSpec)
				.skipJavaLangImports(true)
				.build();
		
		
		// Write source file.
		try {
			javaFile.writeTo(filer);
		} catch (IOException e) {
			messager.printMessage(ERROR, e.getMessage());
		}
	}
	
	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Collections.singleton(Reify.class.getCanonicalName());
	}
	
	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latest();
	}
}
