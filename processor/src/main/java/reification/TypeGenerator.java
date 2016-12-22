package reification;

import com.squareup.javapoet.*;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Types;
import java.util.*;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.DEFAULT;
import static javax.tools.Diagnostic.Kind.ERROR;

public class TypeGenerator {
	private final Types types;
	private final Messager messager;
	
	private final TypeElement superTypeElement;
	
	public TypeGenerator(Types types, Messager messager, TypeElement superTypeElement) {
		this.types = Objects.requireNonNull(types, "types");
		this.messager = Objects.requireNonNull(messager, "messager");
		this.superTypeElement = Objects.requireNonNull(superTypeElement, "superTypeElement");
	}
	
	public TypeSpec.Builder generateType(LinkedHashMap<String, DeclaredType> reifiedTypeArguments, String generatedTypeName) {
		if (reifiedTypeArguments.size() > 1) {
			messager.printMessage(
					ERROR,
					String.format(
							"'@Reify'-annotation in type '%s' with multiple type variables %s is %s",
							superTypeElement,
							reifiedTypeArguments,
							Message.NOT_YET_IMPLEMENTED
					),
					superTypeElement
			);
			return null;
		}
		
		List<ExecutableElement> abstractMethods = new ArrayList<>();
		ElementFunctions.recursivelyAddAbstractMethods(superTypeElement, abstractMethods, types);
		
		// Having methods in single list ensures that generated methods appear in the same order as they were defined.
		List<MethodGenerator> methodGenerators = new ArrayList<>();
		
		boolean unimplementedAbstractMethods = false;
		for (ExecutableElement abstractMethod : abstractMethods) {
			String name = abstractMethod.getSimpleName().toString();
			
			DeclaredType newInstanceMethodType = newInstanceMethodType(name, reifiedTypeArguments);
			if (newInstanceMethodType != null) {
				methodGenerators.add(new MethodGenerator.NewInstance(types, messager, abstractMethod, newInstanceMethodType));
				continue;
			}
			
			DeclaredType classMethodType = classMethodType(name, reifiedTypeArguments);
			if (classMethodType != null) {
				methodGenerators.add(new MethodGenerator.Class(messager, abstractMethod, classMethodType));
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
		
		for (MethodGenerator methodGenerator : methodGenerators) {
			MethodSpec.Builder builder = methodGenerator.generateMethod();
			if (builder == null) {
				return null;
			}
			
			if (interfaceType) {
				builder.addModifiers(DEFAULT);
			}
			
			MethodSpec method = builder.build();
			typeBuilder.addMethod(method);
		}
		
		return typeBuilder;
	}
	
	private ParameterizedTypeName reifiedSuperType(LinkedHashMap<String, DeclaredType> reifiedTypeArguments) {
		return ParameterizedTypeName.get(
				ClassName.get(superTypeElement),
				reifiedTypeArguments.values().stream().map(ClassName::get).toArray(TypeName[]::new)
		);
	}
	
	// TODO Naming convention is fragile: For instance, an abstract method may be inherited from an interface where the
	//      type parameter may have another name. Require the abstract method to be defined in the reifying class
	//      instead (one can always redefine an abstract method). And possibly require it to be annotated?
	
	private DeclaredType newInstanceMethodType(String name, Map<String, DeclaredType> typeArguments) {
		return methodType("new", name, typeArguments);
	}
	
	private DeclaredType classMethodType(String name, Map<String, DeclaredType> typeArguments) {
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
