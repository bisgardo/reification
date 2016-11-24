package reification;

import com.squareup.javapoet.*;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Types;
import java.util.*;
import java.util.stream.Collectors;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.DEFAULT;
import static javax.tools.Diagnostic.Kind.ERROR;

public class SourceGenerator {
	private final Types types;
	private final Messager messager;
	
	private final TypeElement superTypeElement;
	
	public SourceGenerator(Types types, Messager messager, TypeElement superTypeElement) {
		this.types = Objects.requireNonNull(types, "types");
		this.messager = Objects.requireNonNull(messager, "messager");
		this.superTypeElement = Objects.requireNonNull(superTypeElement, "superTypeElement");
	}
	
	// TODO Refactor method.
	public TypeSpec.Builder generateSource(LinkedHashMap<String, DeclaredType> reifiedTypeArguments, String generatedTypeName, TypeElement typeElement) {
		if (reifiedTypeArguments.size() > 1) {
			messager.printMessage(
					ERROR,
					String.format(
							"'@Reify'-annotation in type '%s' with multiple type variables %s is %s",
							typeElement,
							reifiedTypeArguments,
							Message.NOT_YET_IMPLEMENTED
					),
					typeElement
			);
			return null;
		}
		
		List<ExecutableElement> abstractMethods = new ArrayList<>();
		ElementFunctions.recursivelyAddAbstractMethods(superTypeElement, abstractMethods, types);
		
		class MethodDescription {
			ExecutableElement methodElement;
			DeclaredType instantiatedType;
		}
		
		List<MethodDescription> newInstanceMethods = new ArrayList<>();
		List<MethodDescription> classMethods = new ArrayList<>();
		
		boolean unimplementedAbstractMethods = false;
		for (ExecutableElement abstractMethod : abstractMethods) {
			String name = abstractMethod.getSimpleName().toString();
			
			DeclaredType newInstanceMethodType = newInstanceMethodType(name, reifiedTypeArguments);
			if (newInstanceMethodType != null) {
				MethodDescription methodDescription = new MethodDescription();
				methodDescription.methodElement = abstractMethod;
				methodDescription.instantiatedType = newInstanceMethodType;
				newInstanceMethods.add(methodDescription);
				continue;
			}
			
			DeclaredType classMethodType = classMethodType(name, reifiedTypeArguments);
			if (classMethodType != null) {
				MethodDescription methodDescription = new MethodDescription();
				methodDescription.methodElement = abstractMethod;
				methodDescription.instantiatedType = classMethodType;
				classMethods.add(methodDescription);
				continue;
			}
			
			unimplementedAbstractMethods = true;
		}
		
		// Build reified type.
		ParameterizedTypeName reifiedSuperType = reifiedSuperType(reifiedTypeArguments);
		
		ElementKind kind = superTypeElement.getKind();
		boolean interfaceType = false;
		if (kind.isInterface()) {
			interfaceType = true;
		} else if (!kind.isClass()) {
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
		
		TypeSpec.Builder typeBuilder;
		if (interfaceType) {
			typeBuilder = TypeSpec.interfaceBuilder(generatedTypeName).addSuperinterface(reifiedSuperType);
		} else {
			typeBuilder = TypeSpec.classBuilder(generatedTypeName).superclass(reifiedSuperType);
			if (unimplementedAbstractMethods) {
				typeBuilder.addModifiers(ABSTRACT);
			}
		}
		
		for (MethodDescription newInstanceMethod : newInstanceMethods) {
			ExecutableElement methodElement = newInstanceMethod.methodElement;
			DeclaredType instantiatedType = newInstanceMethod.instantiatedType;
			
			// TODO Validate that method parameters match a constructor.
			List<? extends VariableElement> methodParameters = methodElement.getParameters();
			
			// TODO Only declare exceptions that are declared thrown by the called constructor.
			MethodSpec.Builder builder = MethodSpec.overriding(methodElement);
			
			// Replace method return type with the instantiated type.
			// TODO Check that method return type is indeed (compatible with) the type parameter?
			builder.returns(ClassName.get(instantiatedType));
			
			if (interfaceType) {
				builder.addModifiers(DEFAULT);
			}
			
			String constructorArguments = methodParameters
					.stream()
					.map(ve -> ve.getSimpleName().toString())
					.collect(Collectors.joining(", "));
			builder.addCode(CodeBlock.of("return new $T($L);", instantiatedType, constructorArguments));
			
			typeBuilder.addMethod(builder.build());
		}
		
		for (MethodDescription classMethod : classMethods) {
			ExecutableElement methodElement = classMethod.methodElement;
			DeclaredType instantiatedType = classMethod.instantiatedType;
			
			// TODO Validate that method parameters match a constructor.
			if (!methodElement.getParameters().isEmpty()) {
				messager.printMessage(
						ERROR,
						String.format(
								"Auto-implemented abstract method '%s' must not have any parameters",
								methodElement
						),
						methodElement
				);
				return null;
			}
			
			// TODO Only declare exceptions that are declared thrown by the called constructor.
			MethodSpec.Builder builder = MethodSpec.overriding(methodElement);
			
			// Replace method return type with the instantiated type; `Class<T>`.
			ParameterizedTypeName returnType = ParameterizedTypeName.get(
					ClassName.get(Class.class),
					ClassName.get(instantiatedType)
			);
			// TODO Check that method return type is indeed (compatible with) `returnType`?
			builder.returns(returnType);
			
			if (interfaceType) {
				builder.addModifiers(DEFAULT);
			}
			
			builder.addCode(CodeBlock.of("return $T.class;", instantiatedType));
			
			typeBuilder.addMethod(builder.build());
		}
		
		return typeBuilder;
	}
	
	private ParameterizedTypeName reifiedSuperType(LinkedHashMap<String, DeclaredType> reifiedTypeArguments) {
		return ParameterizedTypeName.get(
				ClassName.get(superTypeElement),
				reifiedTypeArguments.values().stream().map(ClassName::get).toArray(TypeName[]::new)
		);
	}
	
	private DeclaredType newInstanceMethodType(String name, Map<String, DeclaredType> typeArguments) {
		// TODO Should be based on annotation instead of naming convention?
		return methodType("new", name, typeArguments);
	}
	
	private DeclaredType classMethodType(String name, Map<String, DeclaredType> typeArguments) {
		// TODO Should be based on annotation instead of naming convention?
		return methodType("class", name, typeArguments);
	}
	
	private DeclaredType methodType(String prefix, String name, Map<String, DeclaredType> typeArguments) {
		if (!name.startsWith(prefix)) {
			return null;
		}
		
		String typeName = name.substring(prefix.length());
		return typeArguments.get(typeName);
	}
}
