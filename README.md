# Reification

Annotation processor for reifying/instantiating type parameters of
classes and interfaces at compile time by generating predictably named
classes where the instantiated type parameter has been baked in.

The processor reacts on `@reification.Reify`-annotations, which may only
be applied to type variables (and thus only works with Java 8). The
annotation has a attribute `value()`, through which the programmer
supplies the `Class`-object of the type that the annotated type variable
should be reified with. When the processor encounters a type with
annotated type parameters, it will generate new types for each
combination of reified type parameters as described in the next section.

The processor furthermore [doesn't yet] supports auto-implementation of
the followingly named [or suitably annotated] abstract methods, if they
are present:

* `T newT(? arg_1, ..., ? arg_n)`: Create new instance of
  reified type `T` by passing the (arbitrarily typed) parameters `arg_1`
  through `arg_n` to a suitable constructor of `T`.

* `T classT()`: Return `Class`-object for reified type `T`.

Reification is not supported for non-static inner classes, although it
might be possible to add it for non-anonymous, non-local classes at some
later time.

The current implementation is also quite incomplete, and the following
features haven't been implemented yet:

* Reification of type parameters on static inner classes.

* Reification of type parameters on final classes.

* Reification of type that defines more than one type parameter.

* Reification of primitive types.

* Auto-implementation of methods.

All these constraints are intended to be lifted later on and other
features may be added as well.

## Examples

By deciding to list only examples that will work on the currently
implemented features, the list can be both exhaustive and short: The
only feature listed above that actually works is reification of a single,
non-primitive type variable of a top-level class or interface:

The annotation processor deals with the class

    public class X<@Reify(String.class) T> {
        // ...
    }

by generating the class

    public class X$String extends X<String> {
    }

If `X` contains abstract methods, then `X$String` will be abstract. If
`X` were an interface, then so would `X$String` be.
