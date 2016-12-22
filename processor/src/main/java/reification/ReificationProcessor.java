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
import java.util.*;

import static javax.lang.model.element.Modifier.*;
import static javax.lang.model.type.TypeKind.DECLARED;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;

public class ReificationProcessor extends AbstractProcessor {
	private Types types;
	private Elements elements;
	private Filer filer;
	private Messager messager;
	
	@Override
	public synchronized void init(ProcessingEnvironment environment) {
		super.init(environment);
		
		types = environment.getTypeUtils();
		elements = environment.getElementUtils();
		filer = environment.getFiler();
		messager = environment.getMessager();
		
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
									"'@Reify'-annotation in non-type '%s' of kind '%s' is %s",
									rootElement,
									kind,
									Message.NOT_SUPPORTED
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
											"'@Reify'-annotation in static inner class '%s' is %s",
											enclosingElement,
											Message.NOT_YET_IMPLEMENTED
									),
									enclosingElement
							);
							break;
						} else {
							messager.printMessage(
									ERROR,
									String.format(
											"'@Reify'-annotation in non-static inner class '%s' is %s",
											enclosingElement,
											Message.NOT_SUPPORTED
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
	
	// TODO Consider only reporting errors from this class and rely on exceptions elsewhere.
	
	private void process(ClassSymbol typeElement) {
		TypeGenerator typeGenerator = new TypeGenerator(types, messager, typeElement);
		
		if (typeElement.getModifiers().contains(FINAL)) {
			// TODO Support reification of final classes: Implement in two iterations:
			//      1. Require the class to have no methods (abstract methods would not be a problem, but since the
			//         class is final, it cannot have any). The solution is then to just extend the type's superclass
			//         instead of the type itself and to implement the type's interfaces.
			//      2. Read public field `sourcefile` of `typeElement`, use some framework to parse the methods in
			//         question, reify them, and then toss them back into the generated class.
			//      
			//      If at some point one wants to reify an un-modifiable type, this will provide a rather clean way of
			//      doing it: Extend the class with a final one and reify that one. Also, see implementation comment on
			//      primitive types below.
			messager.printMessage(
					ERROR,
					String.format("'@Reify'-annotation in final class '%s' is %s", typeElement, Message.NOT_YET_IMPLEMENTED),
					typeElement
			);
			return;
		}
		
		PackageElement packageElement = elements.getPackageOf(typeElement);
		String generatedPackageName = "";
		if (!packageElement.isUnnamed()) {
			generatedPackageName = packageElement.toString();
		}
		
		// TODO For each combination of of annotated type parameters, generate an extending class with the type
		//      parameters instantiated.
		
		// Current simplified solution only supports single, non-primitive type parameter.
		List<? extends TypeParameterElement> typeParameters = typeElement.getTypeParameters();
		
		LinkedHashMap<String, DeclaredType> reifiedTypeArguments = reifiedTypeArguments(typeParameters);
		if (reifiedTypeArguments == null) {
			// Error has been reported to `messager` from within `reifiedTypeArguments`.
			return;
		}
		
		String typeName = typeElement.getSimpleName().toString();
		String generatedTypeName = generatedTypeName(typeName, reifiedTypeArguments);
		
		TypeSpec.Builder javaFileBuilder = typeGenerator.generateType(reifiedTypeArguments, generatedTypeName);
		if (javaFileBuilder == null) {
			// Error has been reported to `messager` from within `typeGenerator`.
			return;
		}
		
		// Build compilation unit.
		TypeSpec typeSpec = javaFileBuilder
				.addModifiers(PUBLIC)
				.addOriginatingElement(typeElement)
				.build();
		
		// TODO Add comment describing the origin of the file and a threat about changing the source manually.
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
	
	private static String generatedTypeName(String typeName, LinkedHashMap<String, DeclaredType> reifiedTypeArguments) {
		StringBuilder stringBuilder = new StringBuilder(typeName);
		
		reifiedTypeArguments
				.values()
				.stream()
				.filter(Objects::nonNull)
				.map(d -> d.asElement().getSimpleName().toString())
				.forEach(n -> stringBuilder.append('$').append(n));
		
		return stringBuilder.toString();
	}
	
	private LinkedHashMap<String, DeclaredType> reifiedTypeArguments(List<? extends TypeParameterElement> typeParameters) {
		if (typeParameters.isEmpty()) {
			// The processor should only be activated for types with annotated type parameters.
			throw new IllegalStateException("Type without type parameters not expected");
		}
		
		LinkedHashMap<String, DeclaredType> reifiedTypeArguments = new LinkedHashMap<>();
		for (TypeParameterElement typeParameter : typeParameters) {
			Reify[] annotations = typeParameter.getAnnotationsByType(Reify.class);
			String typeParameterName = typeParameter.getSimpleName().toString();
			if (annotations.length == 0) {
				// Non-annotated type parameter.
				reifiedTypeArguments.put(typeParameterName, null);
				continue;
			}
			
			if (annotations.length > 1) {
				messager.printMessage(
						ERROR,
						"Repeated '@Reify'-annotations is " + Message.NOT_YET_IMPLEMENTED,
						typeParameter
				);
				return null;
			}
			
			Reify annotation = annotations[0];
			DeclaredType typeArgument = getDeclaredType(annotation, typeParameter);
			if (typeArgument == null) {
				// Error has been reported to `messager` from within `getDeclaredType`.
				return null;
			}
			
			reifiedTypeArguments.put(typeParameterName, typeArgument);
		}
		return reifiedTypeArguments;
	}
	
	private DeclaredType getDeclaredType(Reify annotation, TypeParameterElement typeParameterElement) {
		// `Class`-object can't be accessed on compile-time. See
		// 'https://blog.retep.org/2009/02/13/getting-class-values-from-annotations-in-an-annotationprocessor/'.
		try {
			annotation.value();
			throw new IllegalStateException("Expected 'MirroredTypeException'");
		} catch (MirroredTypeException e) {
			TypeMirror typeMirror = e.getTypeMirror();
			TypeKind typeMirrorKind = typeMirror.getKind();
			if (typeMirrorKind.isPrimitive()) {
				// TODO Support reification of primitive types. Like with final classes (see TODO above), the
				//      generated cannot extend "type" (except in useless cases when the parameter isn't used - we don't
				//      care about such cases). The generated type's superclass should be the "closest" superclass of
				//      "type" that doesn't define any type parameter that are being reified into primitive types. Also,
				//      all interfaces not containing these type parameters should be implemented as well. In short,
				//      consider this to be a complex expansion of the requirement to support final classes.
				messager.printMessage(
						ERROR,
						String.format(
								"'@Reify'-annotation with primitive type '%s' on type parameter is %s",
								typeMirror,
								Message.NOT_YET_IMPLEMENTED
						),
						typeParameterElement
				);
				return null;
			}
			if (typeMirrorKind != DECLARED) {
				throw new IllegalStateException(
						String.format(
								"Expected type mirror '%s' of kind '%s' to be of kind 'DECLARED'",
								typeMirrorKind,
								typeMirror
						)
				);
			}
			return (DeclaredType) typeMirror;
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
