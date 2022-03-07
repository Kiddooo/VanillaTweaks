/*
 * GNU General Public License v3
 *
 * VanillaTweaks, a performant replacement for the VanillaTweaks datapacks.
 *
 * Copyright (C) 2021 Machine_Maker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package me.machinemaker.vanillatweaks.utils;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;
import org.bukkit.Bukkit;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReflectionUtils {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static MethodBuilder method(Class<?> ownerClass, Class<?> returnType, Class<?>...parameterTypes) {
        return new MethodBuilder(ownerClass, MethodType.methodType(returnType, parameterTypes));
    }

    public static final class MethodBuilder {

        private final Class<?> ownerClass;
        private final MethodType type;
        private final Set<String> names = new LinkedHashSet<>();

        private MethodBuilder(Class<?> ownerClass, MethodType type) {
            this.ownerClass = ownerClass;
            this.type = type;
        }

        public MethodBuilder named(String... names) {
            this.names.addAll(List.of(names));
            return this;
        }

        public MethodInvoker build() {
            MethodHandle handle = null;
            for (String name : this.names) {
                try {
                    handle = LOOKUP.findVirtual(this.ownerClass, name, this.type);
                } catch (NoSuchMethodException | IllegalAccessException ignored) {}
            }
            if (handle == null) {
                throw new NoSuchElementException("Couldn't find a method named " + this.names + " on " + this.ownerClass.getSimpleName() + " with types " + this.type);
            }

            MethodHandle finalHandle = handle;
            return (target, arguments) -> {
                try {
                    final Object[] args = new Object[arguments.length + 1];
                    args[0] = target;
                    if (arguments.length > 0) {
                        System.arraycopy(arguments, 0, args, 1, args.length);
                    }
                    return finalHandle.invokeWithArguments(args);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            };
        }
    }

    // Deduce the net.minecraft.server.v* package
    private static final String OBC_PREFIX = Bukkit.getServer().getClass().getPackage().getName();
    private static final String NMS_PREFIX = "net.minecraft";
    private static final String VERSION = OBC_PREFIX.replace("org.bukkit.craftbukkit", "").replace(".", "");
    // Variable replacement
    private static final Pattern MATCH_VARIABLE = Pattern.compile("\\{([^\\}]+)\\}");

    private ReflectionUtils() { }

    /**
     * Expand variables such as "{nms}" and "{obc}" to their corresponding packages.
     *
     * @param name the full name of the class
     * @return the expanded string
     */
    private static String expandVariables(String name) {
        StringBuffer output = new StringBuffer();
        Matcher matcher = MATCH_VARIABLE.matcher(name);

        while (matcher.find()) {
            String variable = matcher.group(1);
            String replacement;

            // Expand all detected variables
            if ("nms".equalsIgnoreCase(variable))
                replacement = NMS_PREFIX;
            else if ("obc".equalsIgnoreCase(variable))
                replacement = OBC_PREFIX;
            else if ("version".equalsIgnoreCase(variable))
                replacement = VERSION;
            else
                throw new IllegalArgumentException("Unknown variable: " + variable);

            // Assume the expanded variables are all packages, and append a dot
            if (replacement.length() > 0 && matcher.end() < name.length() && name.charAt(matcher.end()) != '.')
                replacement += ".";
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    /**
     * Retrieve a class by its canonical name.
     *
     * @param canonicalName the canonical name
     * @return the class
     */
    private static Class<?> getCanonicalClass(String canonicalName) {
        try {
            return Class.forName(canonicalName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot find " + canonicalName, e);
        }
    }

    /**
     * Retrieve a class from its full name.
     *
     * @param lookupName the class name with variables
     * @return the looked up class
     * @throws IllegalArgumentException If a variable or class could not be found
     */
    public static Class<?> getClass(String lookupName) {
        return getCanonicalClass(expandVariables(lookupName));
    }

    /**
     * Search for the first publicly and privately defined constructor of the given name and parameter count.
     *
     * @param className lookup name of the class, see {@link #getClass(String)}
     * @param params    the expected parameters
     * @return an object that invokes this constructor
     * @throws IllegalStateException If we cannot find this method
     */
    public static ConstructorInvoker getConstructor(String className, Class<?>... params) {
        return getConstructor(getClass(className), params);
    }

    /**
     * Search for the first publicly and privately defined constructor of the given name and parameter count.
     *
     * @param clazz  a class to start with
     * @param params the expected parameters
     * @return an object that invokes this constructor
     * @throws IllegalStateException If we cannot find this method
     */
    public static ConstructorInvoker getConstructor(Class<?> clazz, Class<?>... params) {
        for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (Arrays.equals(constructor.getParameterTypes(), params)) {

                constructor.setAccessible(true);
                return new ConstructorInvoker() {
                    @Override
                    public Object invoke(Object... arguments) {
                        try {
                            return constructor.newInstance(arguments);
                        } catch (Exception e) {
                            throw new RuntimeException("Cannot invoke constructor " + constructor, e);
                        }
                    }
                };
            }
        }
        throw new IllegalStateException(String.format(
                "Unable to find constructor for %s (%s).", clazz, Arrays.asList(params)));
    }

    /**
     * Retrieve a class in the org.bukkit.craftbukkit.VERSION.* package.
     *
     * @param name the name of the class, excluding the package
     * @throws IllegalArgumentException If the class doesn't exist
     */
    public static Class<?> getCraftBukkitClass(String name) {
        return getCanonicalClass(OBC_PREFIX + "." + name);
    }

    /**
     * Retrieve a class in the org.bukkit.craftbukkit.VERSION.* package.
     *
     * @param names the names of classes to check for
     * @throws IllegalArgumentException If the class doesn't exist
     */
    public static Class<?> findCraftBukkitClass(String ...names) {
        for (String name : names) {
            try {
                return getCraftBukkitClass(name);
            } catch (IllegalArgumentException ignored) {
            }
        }
        throw new IllegalArgumentException("None of " + Arrays.toString(names) + " could be matched to a class");
    }

    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param target    the target type
     * @param name      the name of the field, or NULL to ignore
     * @param fieldType a compatible field type
     * @return the field accessor
     */
    public static <T> FieldAccessor<T> getField(Class<?> target, String name, Class<T> fieldType) {
        return getField(target, name, TypeToken.get(fieldType), 0);
    }

    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param target    the target type
     * @param name      the name of the field, or NULL to ignore
     * @param fieldType a compatible field type
     * @return the field accessor
     */
    public static <T> FieldAccessor<T> getField(Class<?> target, String name, TypeToken<T> fieldType) {
        return getField(target, name, fieldType, 0);
    }

    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param className lookup name of the class, see {@link #getClass(String)}
     * @param name      the name of the field, or NULL to ignore
     * @param fieldType a compatible field type
     * @return the field accessor
     */
    public static <T> FieldAccessor<T> getField(String className, String name, Class<T> fieldType) {
        return getField(getClass(className), name, TypeToken.get(fieldType), 0);
    }

    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param className lookup name of the class, see {@link #getClass(String)}
     * @param name      the name of the field, or NULL to ignore
     * @param fieldType a compatible field type
     * @return the field accessor
     */
    public static <T> FieldAccessor<T> getField(String className, String name, TypeToken<T> fieldType) {
        return getField(getClass(className), name, fieldType, 0);
    }

    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param target    the target type
     * @param fieldType a compatible field type
     * @param index     the number of compatible fields to skip
     * @return the field accessor
     */
    public static <T> FieldAccessor<T> getField(Class<?> target, Class<T> fieldType, int index) {
        return getField(target, null, TypeToken.get(fieldType), index);
    }

    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param className lookup name of the class, see {@link #getClass(String)}
     * @param fieldType a compatible field type
     * @param index     the number of compatible fields to skip
     * @return the field accessor
     */
    public static <T> FieldAccessor<T> getField(String className, Class<T> fieldType, int index) {
        return getField(getClass(className), fieldType, index);
    }

    // Common method
    private static <T> FieldAccessor<T> getField(Class<?> target, String name, TypeToken<T> fieldType, int index) {
        for (final Field field : target.getDeclaredFields()) {
            if ((name == null || field.getName().equals(name)) && GenericTypeReflector.erase(fieldType.getType()).isAssignableFrom(field.getType()) && index-- <= 0) {
                field.setAccessible(true);

                // A function for retrieving a specific field value
                return new FieldAccessor<T>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public T get(Object target) {
                        try {
                            return (T) field.get(target);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("Cannot access reflection.", e);
                        }
                    }

                    @Override
                    public void set(Object target, Object value) {
                        try {
                            field.set(target, value);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("Cannot access reflection.", e);
                        }
                    }

                    @Override
                    public boolean hasField(Object target) {
                        // target instanceof DeclaringClass
                        return field.getDeclaringClass().isAssignableFrom(target.getClass());
                    }
                };
            }
        }

        // Search in parent classes
        if (target.getSuperclass() != null)
            return getField(target.getSuperclass(), name, fieldType, index);
        throw new IllegalArgumentException("Cannot find field with type " + fieldType.getType());
    }

    /**
     * Search for the first publicly and privately defined method of the given name and parameter count.
     *
     * @param className  lookup name of the class, see {@link #getClass(String)}
     * @param methodName the method name, or NULL to skip
     * @param params     the expected parameters
     * @return an object that invokes this specific method
     * @throws IllegalStateException If we cannot find this method
     */
    public static MethodInvoker getMethod(String className, String methodName, Class<?>... params) {
        return getTypedMethod(getClass(className), methodName, null, params);
    }

    /**
     * Search for the first publicly and privately defined method of the given name and parameter count.
     *
     * @param clazz      a class to start with
     * @param methodName the method name, or NULL to skip
     * @param params     the expected parameters
     * @return an object that invokes this specific method
     * @throws IllegalStateException If we cannot find this method
     */
    public static MethodInvoker getMethod(Class<?> clazz, String methodName, Class<?>... params) {
        return getTypedMethod(clazz, methodName, null, params);
    }

    /**
     * Search for the first publicly and privately defined method of the given name and parameter count.
     *
     * @param clazz      a class to start with
     * @param methodNames the method names to check for
     * @param params     the expected parameters
     * @return an object that invokes this specific method
     * @throws IllegalStateException If we cannot find this method
     */
    public static MethodInvoker findMethod(Class<?> clazz, Collection<String> methodNames, Class<?>... params) {
        MethodInvoker invoker = null;
        for (String name : methodNames) {
            try {
                invoker = getMethod(clazz, name, params);
            } catch (IllegalStateException ignored) { }
            if (invoker != null) {
                break;
            }
        }
        if (invoker == null) {
            throw new IllegalStateException(String.format("Unable to find method (one of: %s) (%s) in %s.", String.join(", ", methodNames), Arrays.asList(params), clazz.getCanonicalName()));
        }
        return invoker;
    }

    /**
     * Search for the first publicly and privately defined method of the given name and parameter count.
     *
     * @param clazz  target class
     * @param method the method name
     * @return the method found
     */
    public static Method getMethodSimply(Class<?> clazz, String method) {
        for (Method m : clazz.getMethods()) if (m.getName().equals(method)) return m;
        return null;
    }

    /**
     * Retrieve a class in the net.minecraft.server.VERSION.* package.
     *
     * @param name the name of the class
     * @throws IllegalArgumentException If the class doesn't exist
     */
    public static Class<?> getMinecraftClass(String name) {
        return getCanonicalClass(NMS_PREFIX + "." + name);
    }

    /**
     * Retrieve a class in the net.minecraft.server.VERSION.* package.
     *
     * @param names the names of the class
     * @throws IllegalArgumentException If the none of the names match a class
     */
    public static Class<?> findMinecraftClass(String...names) {
        for (String name : names) {
            try {
                return getMinecraftClass(name);
            } catch (IllegalArgumentException ignored) {
            }
        }
        throw new IllegalArgumentException("None of " + Arrays.toString(names) + " could be matched to a minecraft class");
    }

    /**
     * Search for the first publicly and privately defined method of the given name and parameter count.
     *
     * @param clazz      a class to start with
     * @param methodName the method name, or NULL to skip
     * @param returnType the expected return type, or NULL to ignore
     * @param params     the expected parameters
     * @return an object that invokes this specific method
     * @throws IllegalStateException If we cannot find this method
     */
    public static MethodInvoker getTypedMethod(Class<?> clazz, String methodName, Class<?> returnType, Class<?>... params) {
        for (final Method method : clazz.getDeclaredMethods()) {
            if ((methodName == null || method.getName().equals(methodName)) &&
                    (returnType == null) || method.getReturnType().equals(returnType) &&
                    Arrays.equals(method.getParameterTypes(), params)) {

                method.setAccessible(true);
                return new MethodInvoker() {
                    @Override
                    public Object invoke(Object target, Object... arguments) {
                        try {
                            return method.invoke(target, arguments);
                        } catch (Exception e) {
                            throw new RuntimeException("Cannot invoke method " + method, e);
                        }
                    }
                };
            }
        }
        // Search in every superclass
        if (clazz.getSuperclass() != null)
            return getMethod(clazz.getSuperclass(), methodName, params);
        throw new IllegalStateException(String.format(
                "Unable to find method %s (%s) in %s.", methodName, Arrays.asList(params), clazz.getCanonicalName()));
    }

    /**
     * Retrieve a class from its full name, without knowing its type on compile time.
     * <p>
     * This is useful when looking up fields by a NMS or OBC type.
     *
     * @param lookupName the class name with variables
     * @return the class
     * @see #getClass() for more information
     */
    public static Class<Object> getUntypedClass(String lookupName) {
        @SuppressWarnings({"rawtypes", "unchecked"})
        Class<Object> clazz = (Class<Object>) (Class) getClass(lookupName);
        return clazz;
    }

    public static <T> T newInstance(Class<T> type) {
        try {
            var ctor = type.getDeclaredConstructor();
            ctor.trySetAccessible();
            return ctor.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * An interface for invoking a specific constructor.
     */
    public interface ConstructorInvoker {
        /**
         * Invoke a constructor for a specific class.
         *
         * @param arguments the arguments to pass to the constructor.
         * @return the constructed object.
         */
        Object invoke(Object... arguments);
    }

    /**
     * An interface for invoking a specific method.
     */
    public interface MethodInvoker {
        /**
         * Invoke a method on a specific target object.
         *
         * @param target    the target object, or NULL for a static method.
         * @param arguments the arguments to pass to the method.
         * @return the return value, or NULL if is void.
         */
        Object invoke(Object target, Object... arguments);
    }

    /**
     * An interface for retrieving the field content.
     *
     * @param <T> field type
     */
    public interface FieldAccessor<T> {
        /**
         * Retrieve the content of a field.
         *
         * @param target the target object, or NULL for a static field
         * @return the value of the field
         */
        T get(Object target);

        /**
         * Set the content of a field.
         *
         * @param target the target object, or NULL for a static field
         * @param value  the new value of the field
         */
        void set(Object target, Object value);

        /**
         * Determine if the given object has this field.
         *
         * @param target the object to test
         * @return TRUE if it does, FALSE otherwise
         */
        boolean hasField(Object target);
    }

}
