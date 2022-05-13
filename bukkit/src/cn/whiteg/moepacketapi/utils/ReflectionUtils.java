package cn.whiteg.moepacketapi.utils;

import com.google.gson.internal.reflect.ReflectionAccessor;
import org.bukkit.Bukkit;

import javax.naming.RefAddr;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An utility class that simplifies reflection in Bukkit plugins.
 *
 * @author Kristian
 */
public final class ReflectionUtils {
    // Deduce the net.minecraft.server.v* package
    private static final String OBC_PREFIX = Bukkit.getServer().getClass().getPackage().getName();
    private static final String VERSION = OBC_PREFIX.replace("org.bukkit.craftbukkit","").replace(".","");
    // Variable replacement
    private static final Pattern MATCH_VARIABLE = Pattern.compile("\\{([^\\}]+)\\}");

    private ReflectionUtils() {
        // Seal class
    }

    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param target    - the target type.
     * @param name      - the name of the field, or NULL to ignore.
     * @param fieldType - a compatible field type.
     * @return The field accessor.
     */
    public static <T> FieldAccessor<T> getField(Class<?> target,String name,Class<T> fieldType) {
        return getField(target,name,fieldType,0);
    }

    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param className - lookup name of the class, see {@link #getClass(String)}.
     * @param name      - the name of the field, or NULL to ignore.
     * @param fieldType - a compatible field type.
     * @return The field accessor.
     */
    public static <T> FieldAccessor<T> getField(String className,String name,Class<T> fieldType) {
        return getField(getClass(className),name,fieldType,0);
    }

    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param target    - the target type.
     * @param fieldType - a compatible field type.
     * @param index     - the number of compatible fields to skip.
     * @return The field accessor.
     */
    public static <T> FieldAccessor<T> getField(Class<?> target,Class<T> fieldType,int index) {
        return getField(target,null,fieldType,index);
    }

    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param className - lookup name of the class, see {@link #getClass(String)}.
     * @param fieldType - a compatible field type.
     * @param index     - the number of compatible fields to skip.
     * @return The field accessor.
     */
    public static <T> FieldAccessor<T> getField(String className,Class<T> fieldType,int index) {
        return getField(getClass(className),fieldType,index);
    }

    // Common method
    private static <T> FieldAccessor<T> getField(Class<?> target,String name,Class<T> fieldType,int index) {
        for (final Field field : target.getDeclaredFields()) {
            if ((name == null || field.getName().equals(name)) && fieldType.isAssignableFrom(field.getType()) && index-- <= 0){
                field.setAccessible(true);
                return new FieldAccessor<>(field);
            }
        }

        // Search in parent classes
        if (target.getSuperclass() != null)
            return getField(target.getSuperclass(),name,fieldType,index);

        throw new IllegalArgumentException("Cannot find field with type " + fieldType);
    }

    /**
     * Search for the first publicly and privately defined method of the given name and parameter count.
     *
     * @param className  - lookup name of the class, see {@link #getClass(String)}.
     * @param methodName - the method name, or NULL to skip.
     * @param params     - the expected parameters.
     * @return An object that invokes this specific method.
     * @throws IllegalStateException If we cannot find this method.
     */
    public static MethodInvoker getMethod(String className,String methodName,Class<?>... params) {
        return getTypedMethod(getClass(className),methodName,null,params);
    }

    /**
     * Search for the first publicly and privately defined method of the given name and parameter count.
     *
     * @param clazz      - a class to start with.
     * @param methodName - the method name, or NULL to skip.
     * @param params     - the expected parameters.
     * @return An object that invokes this specific method.
     * @throws IllegalStateException If we cannot find this method.
     */
    public static MethodInvoker getMethod(Class<?> clazz,String methodName,Class<?>... params) {
        return getTypedMethod(clazz,methodName,null,params);
    }

    /**
     * Search for the first publicly and privately defined method of the given name and parameter count.
     *
     * @param clazz      - a class to start with.
     * @param methodName - the method name, or NULL to skip.
     * @param returnType - the expected return type, or NULL to ignore.
     * @param params     - the expected parameters.
     * @return An object that invokes this specific method.
     * @throws IllegalStateException If we cannot find this method.
     */
    public static MethodInvoker getTypedMethod(Class<?> clazz,String methodName,Class<?> returnType,Class<?>... params) {
        for (final Method method : clazz.getDeclaredMethods()) {
            if ((methodName == null || method.getName().equals(methodName))
                    && (returnType == null || method.getReturnType().equals(returnType))
                    && Arrays.equals(method.getParameterTypes(),params)){
                method.setAccessible(true);

                return new MethodInvoker() {

                    @Override
                    public Object invoke(Object target,Object... arguments) {
                        try{
                            return method.invoke(target,arguments);
                        }catch (Exception e){
                            throw new RuntimeException("Cannot invoke method " + method,e);
                        }
                    }

                };
            }
        }

        // Search in every superclass
        if (clazz.getSuperclass() != null)
            return getMethod(clazz.getSuperclass(),methodName,params);

        throw new IllegalStateException(String.format("Unable to find method %s (%s).",methodName,Arrays.asList(params)));
    }

    /**
     * Search for the first publically and privately defined constructor of the given name and parameter count.
     *
     * @param className - lookup name of the class, see {@link #getClass(String)}.
     * @param params    - the expected parameters.
     * @return An object that invokes this constructor.
     * @throws IllegalStateException If we cannot find this method.
     */
    public static ConstructorInvoker getConstructor(String className,Class<?>... params) {
        return getConstructor(getClass(className),params);
    }

    /**
     * Search for the first publically and privately defined constructor of the given name and parameter count.
     *
     * @param clazz  - a class to start with.
     * @param params - the expected parameters.
     * @return An object that invokes this constructor.
     * @throws IllegalStateException If we cannot find this method.
     */
    public static ConstructorInvoker getConstructor(Class<?> clazz,Class<?>... params) {
        for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (Arrays.equals(constructor.getParameterTypes(),params)){
                constructor.setAccessible(true);

                return new ConstructorInvoker() {

                    @Override
                    public Object invoke(Object... arguments) {
                        try{
                            return constructor.newInstance(arguments);
                        }catch (Exception e){
                            throw new RuntimeException("Cannot invoke constructor " + constructor,e);
                        }
                    }

                };
            }
        }

        throw new IllegalStateException(String.format("Unable to find constructor for %s (%s).",clazz,Arrays.asList(params)));
    }

    /**
     * Retrieve a class from its full name, without knowing its type on compile time.
     * <p>
     * This is useful when looking up fields by a NMS or OBC type.
     * <p>
     *
     * @param lookupName - the class name with variables.
     * @return The class.
     * @see {@link #getClass()} for more information.
     */
    public static Class<Object> getUntypedClass(String lookupName) {
        @SuppressWarnings({"rawtypes","unchecked"})
        Class<Object> clazz = (Class) getClass(lookupName);
        return clazz;
    }

    /**
     * Retrieve a class from its full name.
     * <p>
     * Strings enclosed with curly brackets - such as {TEXT} - will be replaced according to the following table:
     * <p>
     * <table border="1">
     * <tr>
     * <th>Variable</th>
     * <th>Content</th>
     * </tr>
     * <tr>
     * <td>{nms}</td>
     * <td>Actual package name of net.minecraft.server.VERSION</td>
     * </tr>
     * <tr>
     * <td>{obc}</td>
     * <td>Actual pacakge name of org.bukkit.craftbukkit.VERSION</td>
     * </tr>
     * <tr>
     * <td>{version}</td>
     * <td>The current Minecraft package VERSION, if any.</td>
     * </tr>
     * </table>
     *
     * @param lookupName - the class name with variables.
     * @return The looked up class.
     * @throws IllegalArgumentException If a variable or class could not be found.
     */
    public static Class<?> getClass(String lookupName) {
        return getCanonicalClass(expandVariables(lookupName));
    }

    /**
     * Retrieve a class in the org.bukkit.craftbukkit.VERSION.* package.
     *
     * @param name - the name of the class, excluding the package.
     * @throws IllegalArgumentException If the class doesn't exist.
     */
    public static Class<?> getCraftBukkitClass(String name) {
        return getCanonicalClass(OBC_PREFIX + "." + name);
    }

    /**
     * Retrieve a class by its canonical name.
     *
     * @param canonicalName - the canonical name.
     * @return The class.
     */
    private static Class<?> getCanonicalClass(String canonicalName) {
        try{
            return Class.forName(canonicalName);
        }catch (ClassNotFoundException e){
            throw new IllegalArgumentException("Cannot find " + canonicalName,e);
        }
    }

    /**
     * Expand variables such as "{nms}" and "{obc}" to their corresponding packages.
     *
     * @param name - the full name of the class.
     * @return The expanded string.
     */
    private static String expandVariables(String name) {
        StringBuffer output = new StringBuffer();
        Matcher matcher = MATCH_VARIABLE.matcher(name);

        while (matcher.find()) {
            String variable = matcher.group(1);
            String replacement = "";

            // Expand all detected variables
            if ("obc".equalsIgnoreCase(variable))
                replacement = OBC_PREFIX;
            else if ("version".equalsIgnoreCase(variable))
                replacement = VERSION;
            else
                throw new IllegalArgumentException("Unknown variable: " + variable);

            // Assume the expanded variables are all packages, and append a dot
            if (replacement.length() > 0 && matcher.end() < name.length() && name.charAt(matcher.end()) != '.')
                replacement += ".";
            matcher.appendReplacement(output,Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(output);
        return output.toString();
    }

    //根据类型获取Field
    public static <T> FieldAccessor<T> getFieldFormType(Class<?> clazz,Class<T> type) {
        for (Field declaredField : clazz.getDeclaredFields()) {
            if (declaredField.getType().equals(type)) return makeFieldAccessor(declaredField);
        }
        throw new RuntimeException(new NoSuchFieldException(type.getName()));
    }

    public static Field getFieldFormType(Class<?> clazz,String type) {
        for (Field declaredField : clazz.getDeclaredFields()) {
            if (declaredField.getAnnotatedType().getType().getTypeName().equals(type))
                return declaredField;
        }
        throw new RuntimeException(new NoSuchFieldException(type));
    }

    //从数组结构中查找Field
    public static Field[] getFieldFormStructure(Class<?> clazz,Class<?>... types) throws NoSuchFieldException {
        var fields = clazz.getDeclaredFields();
        Field[] result = new Field[types.length];
        int index = 0;
        for (Field f : fields) {
            if (f.getType() == types[index]){
                result[index] = f;
                index++;
                if (index >= types.length){
                    return result;
                }
            } else {
                index = 0;
            }
        }
        throw new NoSuchFieldException(Arrays.toString(types));
    }

    public static <T> FieldAccessor<T> makeFieldAccessor(Field field) {
        return new FieldAccessor<>(field);
    }

    /**
     * An interface for invoking a specific constructor.
     */
    public interface ConstructorInvoker {
        /**
         * Invoke a constructor for a specific class.
         *
         * @param arguments - the arguments to pass to the constructor.
         * @return The constructed object.
         */
        public Object invoke(Object... arguments);
    }

    /**
     * An interface for invoking a specific method.
     */
    public interface MethodInvoker {
        /**
         * Invoke a method on a specific target object.
         *
         * @param target    - the target object, or NULL for a static method.
         * @param arguments - the arguments to pass to the method.
         * @return The return value, or NULL if is void.
         */
        public Object invoke(Object target,Object... arguments);
    }
}
