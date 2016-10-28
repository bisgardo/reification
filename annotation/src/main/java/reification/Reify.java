package reification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE_PARAMETER})
public @interface Reify {
	
	Class<?> value();
	
//	/**
//	 * TODO ...
//	 * @return Name of the generated class
//	 */
//	String name() default "";
	
	// TODO Figure out if it will be possible to copy non-abstract (reified) methods. 
	
//	/**
//	 * Allows one to set supertype manually. Defaults to `java.lang.Object` if `value()` is primitive or the class
//	 * containing the annotation otherwise.
//	 * @return The supertype of the generated class.
//	 */
//	Class<?> superType() default void.class;
}
