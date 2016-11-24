package reification;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.ABSTRACT;

public class ElementFunctions {
	
	public static List<Element> enclosingElements(Element typeElement) {
		ArrayList<Element> enclosingElements = new ArrayList<>();
		while (typeElement != null) {
			enclosingElements.add(typeElement);
			typeElement = typeElement.getEnclosingElement();
		}
		return enclosingElements;
	}
	
	public static void recursivelyAddAbstractMethods(TypeElement typeElement, List<ExecutableElement> abstractMethods, Types types) {
		List<? extends TypeMirror> interfaceTypes = typeElement.getInterfaces();
		TypeMirror superclassType = typeElement.getSuperclass();
		
		// Order of recursion prevents non-abstract methods from being "re-registered" as abstract ones (by removing
		// them from the result list). I.e., processing order reflects the "override" order of elements.
		for (TypeMirror interfaceType : interfaceTypes) {
			Element interfaceElement = types.asElement(interfaceType);
			ElementKind kind = interfaceElement.getKind();
			if (!kind.isInterface()) {
				throw new IllegalStateException(
						String.format("Expected type '%s' of kind '%s' to have kind 'INTERFACE'", interfaceElement, kind)
				);
			}
			TypeElement interfaceTypeElement = (TypeElement) interfaceElement;
			recursivelyAddAbstractMethods(interfaceTypeElement, abstractMethods, types);
		}
		Element superclassElement = types.asElement(superclassType);
		if (superclassElement != null) {
			ElementKind kind = superclassElement.getKind();
			if (!kind.isClass()) {
				throw new IllegalStateException(
						String.format("Expected type '%s' of kind '%s' to have kind 'CLASS'", superclassElement, kind)
				);
			}
			TypeElement superClassTypeElement = (TypeElement) superclassElement;
			recursivelyAddAbstractMethods(superClassTypeElement, abstractMethods, types);
		}
		
		List<? extends Element> memberElements = typeElement.getEnclosedElements();
		for (Element memberElement : memberElements) {
			if (memberElement.getKind() != METHOD) {
				continue;
			}
			
			ExecutableElement methodElement = (ExecutableElement) memberElement;
			Set<Modifier> modifiers = methodElement.getModifiers();
			if (modifiers.contains(ABSTRACT)) {
				abstractMethods.add(methodElement);
			} else {
				abstractMethods.remove(methodElement);
			}
		}
	}
}
