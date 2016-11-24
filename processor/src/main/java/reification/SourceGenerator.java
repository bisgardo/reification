package reification;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.WARNING;

public class SourceGenerator {
	private final Types types;
	private final Messager messager;
	
	private final TypeElement superTypeElement;
	
	public SourceGenerator(Types types, Messager messager, TypeElement superTypeElement) {
		this.types = types;
		this.messager = messager;
		this.superTypeElement = superTypeElement;
	}
	
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
		TypeSpec.Builder typeBuilder;
		if (kind.isClass()) {
			typeBuilder = TypeSpec.classBuilder(generatedTypeName).superclass(reifiedSuperType);
			if (unimplementedAbstractMethods) {
				typeBuilder.addModifiers(ABSTRACT);
			}
		} else if (kind.isInterface()) {
			typeBuilder = TypeSpec.interfaceBuilder(generatedTypeName).addSuperinterface(reifiedSuperType);
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
		
		for (MethodDescription newInstanceMethod : newInstanceMethods) {
			messager.printMessage(
					WARNING,
					String.format(
							"Auto-implementation of abstract method '%s' is %s",
							newInstanceMethod.methodElement.getSimpleName(),
							Message.NOT_YET_IMPLEMENTED
					),
					typeElement
			);
		}
		
		for (MethodDescription classMethod : classMethods) {
			messager.printMessage(
					WARNING,
					String.format(
							"Auto-implementation of abstract method '%s' is %s",
							classMethod.methodElement.getSimpleName(),
							Message.NOT_YET_IMPLEMENTED
					),
					typeElement
			);
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
