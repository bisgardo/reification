# Reification

Annotation processor for reifying/instantiating type parameters of classes
and interfaces at compile time by generating classes where the
instantiated type parameter has been baked in.
 
The processor furthermore [doesn't yet] support auto-implementation of
the following abstract methods, if they are present:

* `newInstance(T object, ? arg_1, ..., ? arg_n)`: Create new instance of
  generic type `T` by passing the (arbitrarily typed) parameters `arg_1`
  through `arg_n`.

* `classDescriptor(T object)`: Return `Class`-object for generic type `T`.

Reification is not supported for non-static inner classes, although it
might be possible to add it for non-anonymous, non-local classes at some
later time.

The current implementation is also quite incomplete, and the following
features haven't been implemented yet:

* Reification of type parameters on static inner classes

* Reification of type parameters on final classes

* Reification when there is more than one type parameter on a class

* Reification of primitive types

* Auto-implementation of methods.

All these constraints are intended to be lifted later on and other
features may be added as well.
