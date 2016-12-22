# Reification

Annotation processor for reifying/instantiating type parameters of classes and interfaces at compile time by generating
predictably named classes where the instantiated type parameter has been baked in.

The processor reacts on `@reification.Reify`-annotations, which may only be applied to type variables (and thus only
works with Java 8+). The annotation has a attribute `value()`, through which the programmer supplies the `Class`-object
of the type that the annotated type variable should be reified with. When the processor encounters a type with annotated
type parameters, it will generate new types for each combination of reified type parameters as described in the next
section.

The generated types are always `public` and contained in their very own source file.

The processor supports auto-implementation of the followingly named [TODO and/or suitably annotated] abstract methods,
if they are present:

*   `T newT(? arg_1, ..., ? arg_n)`: Create a new instance of the reified type `T` by passing the (arbitrarily typed)
    parameters `arg_1` through `arg_n` to a suitable constructor of `T`.
*   `T classT()`: Return the `Class`-object for the reified type `T`.

[TODO In order to limit possible surprises and inconsistencies (one may rename supertypes' type variables), only methods
defined directly within the reifying class are auto-implemented. Note that this is not a limitation, as one can always
redefine inherited abstract methods.]

## Limitations

Reification is not supported for non-static inner classes, although it might be possible to add it for non-anonymous,
non-local classes at some later time.

The current implementation is also quite incomplete, and the following features haven't been implemented yet:

*   Reification of type parameters on static inner classes.
*   Reification of type parameters on final classes.
*   Reification of type that defines more than one type parameter.
*   Reification of primitive types.

All these constraints are intended to be lifted later on and other features may be added as well.

Also, for now, checking that type parameters are instantiated by types within required bounds only is left as an
exercise to the compiler. The same thing goes with ensuring that method declarations (in particular, return types and
`throws`-declarations) of auto-implemented methods are consistent with the ones that they override.

## Examples

By listing only examples that will work on the currently implemented features, the noble objective of being both
exhaustive and brief can be easily achieved. The only feature listed above that actually works is reification of a
single, non-primitive type variable of a top-level class or interface:

The annotation processor deals with the class

    public class X<@Reify(String.class) T> {
        // ...
    }

by generating the class

    public class X$String extends X<String> {
    }

If `X` contains abstract methods, then `X$String` will be abstract. If `X` were an interface, then so would `X$String`
be.

The generated class will reside in the same package as the input class.

[TODO Example of auto-instantiated method]

## Future ideas

Other than lifting the limitations described and otherwise hinted above, the following list of ideas might be
investigated:

*   [TODO Get some ideas!]
