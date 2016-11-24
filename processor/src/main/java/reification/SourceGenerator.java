package reification;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.WARNING;

public class SourceGenerator {
	private static final String NEW_INSTANCE_METHOD_NAME = "newInstance";
	private static final String CLASS_DESCRIPTOR_METHOD_NAME = "classDescriptor";
	
	private final Types types;
	private final Messager messager;
	
	public SourceGenerator(Types types, Messager messager) {
		this.types = types;
		this.messager = messager;
	}
	
	public TypeSpec.Builder generateSource(
			TypeElement superTypeElement,
			DeclaredType typeArgument,
			String generatedClassName,
			TypeElement classElement
	) {
		List<ExecutableElement> abstractMethods = new ArrayList<>();
		ElementFunctions.recursivelyAddAbstractMethods(superTypeElement, abstractMethods, types);
		
		boolean unimplementedAbstractMethods = false;
		for (ExecutableElement abstractMethod : abstractMethods) {
			String name = abstractMethod.getSimpleName().toString();
			switch (name) {
				case NEW_INSTANCE_METHOD_NAME:
					messager.printMessage(
							WARNING,
							String.format(
									"Auto-implementation of abstract method '%s' is not yet implemented",
									NEW_INSTANCE_METHOD_NAME
							),
							classElement
					);
					unimplementedAbstractMethods = true;
					continue;
				case CLASS_DESCRIPTOR_METHOD_NAME:
					messager.printMessage(
							WARNING,
							String.format(
									"Auto-implementation of abstract method '%s' is not yet implemented",
									CLASS_DESCRIPTOR_METHOD_NAME
							),
							classElement
					);
					unimplementedAbstractMethods = true;
					continue;
			}
			unimplementedAbstractMethods = true;
		}
		
		// Build reified type.
		ParameterizedTypeName reifiedSuperType = ParameterizedTypeName.get(
				ClassName.get(superTypeElement),
				ClassName.get(typeArgument)
		);
		
		ElementKind kind = superTypeElement.getKind();
		TypeSpec.Builder typeBuilder;
		if (kind.isClass()) {
			typeBuilder = TypeSpec.classBuilder(generatedClassName).superclass(reifiedSuperType);
			if (unimplementedAbstractMethods) {
				typeBuilder.addModifiers(ABSTRACT);
			}
		} else if (kind.isInterface()) {
			typeBuilder = TypeSpec.interfaceBuilder(generatedClassName).addSuperinterface(reifiedSuperType);
		} else {
			messager.printMessage(
					ERROR,
					String.format(
							"Expected type '%s' of kind '%s' to be either a class or an interface",
							superTypeElement,
							kind
					),
					superTypeElement
			);
			return null;
		}
		
		// TODO Add auto-implemented abstract methods.
		
		return typeBuilder;
	}
}
