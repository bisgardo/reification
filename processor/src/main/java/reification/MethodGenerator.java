package reification;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static javax.tools.Diagnostic.Kind.ERROR;
import static reification.ElementFunctions.resolveConstructor;

public interface MethodGenerator {
	
	MethodSpec.Builder generateMethod();
	
	class Class implements MethodGenerator {
		private final Messager messager;
		private final ExecutableElement methodElement;
		private final DeclaredType instantiatedType;
		
		public Class(Messager messager, ExecutableElement methodElement, DeclaredType instantiatedType) {
			this.messager = Objects.requireNonNull(messager, "messager");
			this.methodElement = Objects.requireNonNull(methodElement, "methodElement");
			this.instantiatedType = Objects.requireNonNull(instantiatedType, "instantiatedType");
		}
		
		@Override
		public MethodSpec.Builder generateMethod() {
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
			MethodSpec.Builder builder = MethodSpec.methodBuilder(overridingMethod.name)
					.addAnnotation(Override.class)
					.addModifiers(overridingMethod.modifiers)
					.addParameters(overridingMethod.parameters);
			
			// Set return type to `Class<T>`.
			ParameterizedTypeName returnType = ParameterizedTypeName.get(
					ClassName.get(java.lang.Class.class),
					ClassName.get(instantiatedType)
			);
			// TODO Check that method return type is indeed (compatible with) `returnType`?
			builder.returns(returnType);
			
			// Add method body.
			builder.addStatement("return $T.class", instantiatedType);
			
			return builder;
		}
	}
	
	class NewInstance implements MethodGenerator {
		private final Types types;
		private final Messager messager;
		private final ExecutableElement methodElement;
		private final DeclaredType instantiatedType;
		
		public NewInstance(Types types, Messager messager, ExecutableElement methodElement, DeclaredType instantiatedType) {
			this.types = Objects.requireNonNull(types, "types");
			this.messager = Objects.requireNonNull(messager, "messager");
			this.methodElement = Objects.requireNonNull(methodElement, "methodElement");
			this.instantiatedType = Objects.requireNonNull(instantiatedType, "instantiatedType");
		}
		
		@Override
		public MethodSpec.Builder generateMethod() {
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
			MethodSpec overridingMethod = MethodSpec.overriding(methodElement).build();
			
			// Create new builder with a copy of name, modifiers and parameters, but without type parameters and
			// declaring only exceptions from the constructor.
			MethodSpec.Builder builder = MethodSpec.methodBuilder(overridingMethod.name)
					.addAnnotation(Override.class)
					.addModifiers(overridingMethod.modifiers)
					.addParameters(overridingMethod.parameters);
			
			// Replace method return type with the instantiated type.
			// TODO Check that method return type is indeed (compatible with) the type parameter?
			builder.returns(ClassName.get(instantiatedType));
			
			// Declare thrown exceptions.
			// TODO Check consistency with throws-declaration of overriden method? And remove unchecked exceptions?
			constructor.getThrownTypes().forEach(e -> builder.addException(ClassName.get(e)));
			
			// Add method body.
			String constructorArguments = parameters
					.stream()
					.map(ve -> ve.getSimpleName().toString())
					.collect(Collectors.joining(", "));
			builder.addStatement("return new $T($L)", instantiatedType, constructorArguments);
			
			return builder;
		}
	}
}
