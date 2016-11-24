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
import static reification.ElementFunctions.resolveConstructor;

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
		
		// TODO Replace this temporary hack with polymorphism.
		Object NEW = new Object();
		Object CLASS = new Object();
		
		class MethodDescription {
			Object type;
			ExecutableElement methodElement;
			DeclaredType instantiatedType;
		}
		
		// Having methods in single list ensures that generated methods appear in the same order as they were defined.
		List<MethodDescription> methodDescriptions = new ArrayList<>();
		
		boolean unimplementedAbstractMethods = false;
		for (ExecutableElement abstractMethod : abstractMethods) {
			String name = abstractMethod.getSimpleName().toString();
			
			DeclaredType newInstanceMethodType = newInstanceMethodType(name, reifiedTypeArguments);
			if (newInstanceMethodType != null) {
				MethodDescription methodDescription = new MethodDescription();
				methodDescription.type = NEW;
				methodDescription.methodElement = abstractMethod;
				methodDescription.instantiatedType = newInstanceMethodType;
				methodDescriptions.add(methodDescription);
				continue;
			}
			
			DeclaredType classMethodType = classMethodType(name, reifiedTypeArguments);
			if (classMethodType != null) {
				MethodDescription methodDescription = new MethodDescription();
				methodDescription.type = CLASS;
				methodDescription.methodElement = abstractMethod;
				methodDescription.instantiatedType = classMethodType;
				methodDescriptions.add(methodDescription);
				continue;
			}
			
			unimplementedAbstractMethods = true;
		}
		
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
		
		for (MethodDescription methodDescription : methodDescriptions) {
			ExecutableElement methodElement = methodDescription.methodElement;
			DeclaredType instantiatedType = methodDescription.instantiatedType;
			
			Object type = methodDescription.type;
			MethodSpec method = null;
			if (type == NEW) {
				method = createNewInstanceMethod(interfaceType, methodElement, instantiatedType);
			}
			if (type == CLASS) {
				method = createClassMethod(interfaceType, methodElement, instantiatedType);
			}
			
			if (method == null) {
				return null;
			}
			
			typeBuilder.addMethod(method);
		}
		
		return typeBuilder;
	}
	
	private MethodSpec createNewInstanceMethod(boolean interfaceType, ExecutableElement methodElement, DeclaredType instantiatedType) {
		// TODO Validate that method parameters match a constructor.
		List<? extends VariableElement> parameters = methodElement.getParameters();
		
		// Resolve constructor from parameters.
		ExecutableElement constructor = resolveConstructor(instantiatedType, parameters, types);
		if (constructor == null) {
			messager.printMessage(
					ERROR,
					String.format(
							"Could not resolve constructor of type '%s' with parameters %s",
							instantiatedType,
							parameters
					)
			);
			return null;
		}
		
		// Must copy features manually due to poor builder design (cannot remove).
		MethodSpec.Builder overridingBuilder = MethodSpec.overriding(methodElement);
		MethodSpec overridingMethod = overridingBuilder.build();
		
		// Create new builder with a copy of name, modifiers and parameters, but without type parameters and
		// declaring only exceptions from the constructor.
		MethodSpec.Builder builder = MethodSpec.methodBuilder(overridingMethod.name);
		builder.addAnnotation(Override.class);
		builder.addModifiers(overridingMethod.modifiers);
		builder.addParameters(overridingMethod.parameters);
		
		if (interfaceType) {
			builder.addModifiers(DEFAULT);
		}
		
		// Replace method return type with the instantiated type.
		// TODO Check that method return type is indeed (compatible with) the type parameter?
		builder.returns(ClassName.get(instantiatedType));
		
		// Declare thrown exceptions.
		constructor.getThrownTypes().forEach(e -> builder.addException(ClassName.get(e)));
		
		// Add method body.
		String constructorArguments = parameters
				.stream()
				.map(ve -> ve.getSimpleName().toString())
				.collect(Collectors.joining(", "));
		builder.addCode(CodeBlock.of("return new $T($L);", instantiatedType, constructorArguments));
		
		return builder.build();
	}
	
	private MethodSpec createClassMethod(boolean interfaceType, ExecutableElement methodElement, DeclaredType instantiatedType) {
		// TODO Should allow (and just ignore) parameters?
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
		
		// Must copy features manually due to poor builder design (cannot remove).
		MethodSpec.Builder overridingBuilder = MethodSpec.overriding(methodElement);
		MethodSpec overridingMethod = overridingBuilder.build();
		
		// Create new builder with a copy of name, modifiers and parameters, but without type parameters and
		// declared exceptions.
		MethodSpec.Builder builder = MethodSpec.methodBuilder(overridingMethod.name);
		builder.addAnnotation(Override.class);
		builder.addModifiers(overridingMethod.modifiers);
		builder.addParameters(overridingMethod.parameters);
		
		if (interfaceType) {
			builder.addModifiers(DEFAULT);
		}
		
		// Set return type to `Class<T>`.
		ParameterizedTypeName returnType = ParameterizedTypeName.get(
				ClassName.get(Class.class),
				ClassName.get(instantiatedType)
		);
		// TODO Check that method return type is indeed (compatible with) `returnType`?
		builder.returns(returnType);
		
		// Add method body.
		builder.addCode(CodeBlock.of("return $T.class;", instantiatedType));
		
		return builder.build();
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
