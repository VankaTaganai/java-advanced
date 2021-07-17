package info.kgeorgiy.ja.panov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.CodeSource;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * The Implementor generates source files for given classes.
 */
public class Implementor implements JarImpler {
    /**
     * A whitespace constant.
     */
    private static final String SPACE = " ";

    /**
     * An empty string constant.
     */
    private static final String EMPTY_STRING = "";

    /**
     * A line separator constant.
     */
    private static final String NEW_LINE = System.lineSeparator();

    /**
     * Classname suffix {@code Impl} for generated class.
     */
    private static final String CLASSNAME_SUFF = "Impl";

    /**
     * Classname suffix with type for generated source file.
     */
    private static final String FILENAME_END = CLASSNAME_SUFF + ".java";

    /**
     * Classname suffix with type for generated class file.
     */
    private static final String CLASSNAME_END = CLASSNAME_SUFF + ".class";

    /**
     * A package constant.
     */
    private static final String PACKAGE = "package ";

    /**
     * A semicolon with line separator constant.
     */
    private static final String LINE_END = ";" + NEW_LINE;

    /**
     * A {@code public} modifier constant.
     */
    private static final String PUBLIC = "public";

    /**
     * A keyword {@code class} constant.
     */
    private static final String CLASS = "class";

    /**
     * A keyword {@code implements} constant.
     */
    private static final String IMPLEMENTS = "implements";

    /**
     * A keyword {@code extends} constant.
     */
    private static final String EXTENDS = "extends";

    /**
     * A left curly brace with line separator constant.
     */
    private static final String BLOCK_BEGIN = "{" + NEW_LINE;

    /**
     * A right curly brace with line separator parenthesis.
     */
    private static final String BLOCK_END = "}" + NEW_LINE;

    /**
     * A left parenthesis constant.
     */
    private static final String LEFT_BRACKET = "(";

    /**
     * A right parenthesis constant.
     */
    private static final String RIGHT_BRACKET = ")";

    /**
     * A delimiter constant equals comma with whitespace.
     */
    private static final String DELIMITER = "," + SPACE;

    /**
     * A keyword {@code super} constant.
     */
    private static final String SUPER = "super";

    /**
     * A keyword {@code return} constant.
     */
    private static final String RETURN = "return";

    /**
     * A keyword {@code null} constant.
     */
    private static final String NULL = "null";

    /**
     * A default boolean value constant.
     */
    private static final String DEFAULT_BOOLEAN_VALUE = "true";

    /**
     * A default numeric value constant.
     */
    private static final String DEFAULT_NUMERIC_VALUE = "0";

    /**
     * A keyword {@code throws} constant.
     */
    private static final String THROWS = "throws";

    /**
     * Default Implementor constructor.
     */
    public Implementor() {}

    /**
     * Generate package for source java file of token.
     * @param token implementing class or interface.
     * @return package {@code String}
     */
    private String generatePackage(final Class<?> token) {
        final String packageName = token.getPackageName();
        return packageName.isEmpty() ? "" : PACKAGE + packageName + LINE_END;
    }

    /**
     * Generate classname for implementing class or interface.
     * @param token implementing class or interface.
     * @return generated classname
     */
    private String generateName(final Class<?> token) {
        return token.getSimpleName() + CLASSNAME_SUFF;
    }

    /**
     * Generate first line of implementing class.
     * This line contains modifiers, class name and throwable exception
     * @param token implementing class or interface.
     * @return First line of implementing class.
     */
    private String generateClassDeclaration(final Class<?> token) {
        return String.join(
                SPACE,
                PUBLIC,
                CLASS,
                generateName(token),
                token.isInterface() ? IMPLEMENTS : EXTENDS,
                // :NOTE: canonical name might return a name with $ in it
                token.getCanonicalName());
    }

    /**
     * Generate string consisting arguments for given executable.
     * @param executable implementing {@code Executable}.
     * @param withTypes true if result should contains arguments type, false otherwise.
     * @return {@code String} with arguments of executable.
     */
    private static String generateArguments(final Executable executable, final boolean withTypes) {
        return Arrays.stream(executable.getParameters())
                .map(parameter -> {
                    String type = (withTypes ? parameter.getType().getCanonicalName() + SPACE : "");
                    return type + parameter.getName();
                })
                .collect(Collectors.joining(DELIMITER));
    }

    /**
     * Generate constructors body.
     * @param constructor implementing constructor
     * @return code of constructor
     */
    private static String generateConstructorBody(final Constructor<?> constructor) {
        return String.join("", SUPER, LEFT_BRACKET, generateArguments(constructor, false), RIGHT_BRACKET, LINE_END);
    }

    /**
     * Generate method body.
     * @param method implementing method
     * @return code of method
     */
    private static String generateMethodBody(final Method method) {
        final Class<?> returnType = method.getReturnType();
        final String returnValue;
        if (!returnType.isPrimitive()) {
            returnValue = NULL;
        } else if (returnType.equals(boolean.class)) {
            returnValue = DEFAULT_BOOLEAN_VALUE;
        } else if (returnType.equals(void.class)) {
            returnValue = EMPTY_STRING;
        } else {
            returnValue = DEFAULT_NUMERIC_VALUE;
        }
        return String.join(SPACE, RETURN, returnValue, LINE_END);
    }

    /**
     * Generate throwable exceptions of given executable.
     * @param executable implementing {@code Executable}.
     * @return {@code String} with throwable exceptions.
     */
    private String generateExceptions(final Executable executable) {
        final Class<?>[] exceptions = executable.getExceptionTypes();
        if (exceptions.length == 0) {
            return EMPTY_STRING;
        } else {
            return THROWS + SPACE + Arrays.stream(exceptions).map(Class::getCanonicalName).collect(Collectors.joining(DELIMITER));
        }
    }

    /**
     * Generate signature of implementing executable.
     * @param executable implementing {@code Executable}.
     * @param executableName name of executable.
     * @param returnType return type of executable.
     * @return {@code String} with signature of executable.
     */
    private String generateSignature(final Executable executable, final String executableName, final String returnType) {
        final String modifiers = Modifier.toString(executable.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT);
        final String arguments = generateArguments(executable, true);
        final String exceptions = generateExceptions(executable);
        return String.join(SPACE, modifiers, returnType, executableName, LEFT_BRACKET, arguments, RIGHT_BRACKET, exceptions);
    }

    /**
     * Generate source code of implementing executables.
     * @param executables {@code List} of executables components of class or interface.
     * @param signatureBuilder signature generator for executable.
     * @param bodyGenerator generator of body source code.
     * @param predicate for filtering executables.
     * @param <T> type of executable
     * @return {@code String} with source code of implementing executable.
     */
    private <T> String generateExecutable(final List<T> executables,
                                          final Function<T, String> signatureBuilder,
                                          final Function<T, String> bodyGenerator,
                                          final Predicate<T> predicate) {
        return executables.stream().filter(predicate).map(executable -> {
            String signature = signatureBuilder.apply(executable);
            String body = bodyGenerator.apply(executable);
            return String.join(EMPTY_STRING, signature, BLOCK_BEGIN, body, BLOCK_END);
        }).collect(Collectors.joining(NEW_LINE));
    }

    /**
     * Generate constructors for implementing token.
     * @param token implementing class or interface.
     * @return {@code String} source code of implementing constructors.
     * @throws ImplerException if token is class and it doesn't contains at least one public constructor.
     */
    private String generateConstructors(final Class<?> token) throws ImplerException {
        final List<Constructor<?>> constructors =
                Arrays.stream(token.getDeclaredConstructors())
                        .filter(constructor -> !Modifier.isPrivate(constructor.getModifiers()))
                        .collect(Collectors.toList());

        if (constructors.size() < 1 && !token.isInterface()) {
            throw new ImplerException("Implemented type should contains at least one public constructor");
        }

        return generateExecutable(constructors,
                constructor -> generateSignature(constructor, generateName(token), EMPTY_STRING),
                Implementor::generateConstructorBody,
                constructor -> true);
    }

    /**
     * {@link Method} wrapper for distinct methods with same signature.
     */
    static class ImplementedMethod {
        /**
         * Wrapped method.
         */
        private final Method method;

        /**
         * Constructor for creating {@code ImplementedMethod}
         * @param method for wrap.
         */
        public ImplementedMethod(Method method) {
            this.method = method;
        }

        /**
         * Return wrapped method.
         * @return wrapped {@link Method}.
         */
        public Method getMethod() {
            return method;
        }

        /**
         * Create {@code List} of {@code ImplementedMethod} by given methods
         * @param methods array of methods
         * @return {@code List} of {@code ImplementedMethod} by given methods
         */
        public static List<ImplementedMethod> createMethods(Method[] methods) {
            return Arrays.stream(methods).map(ImplementedMethod::new).collect(Collectors.toList());
        }

        /**
         * Generate hashcode that equals for method with equals signature.
         * @return hashcode of method.
         */
        @Override
        public int hashCode() {
            return Objects.hash(method.getName(), method.getReturnType(), Arrays.hashCode(method.getParameterTypes()));
        }

        /**
         * Check that given object has same type and method signature.
         * @param obj comparable object.
         * @return true if objects are equals, false otherwise.
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ImplementedMethod other = (ImplementedMethod) obj;
            return method.getName().equals(other.getMethod().getName()) &&
                    method.getReturnType().equals(other.getMethod().getReturnType()) &&
                    Arrays.equals(method.getParameterTypes(), other.getMethod().getParameterTypes());
        }
    }

    /**
     * Generate methods for implementing token.
     * @param token implementing class or interface.
     * @return {@code String} source code of implementing methods.
     */
    private String generateMethods(final Class<?> token) {
        Class<?> iterToken = token;
        Set<ImplementedMethod> methods = new HashSet<>(ImplementedMethod.createMethods(iterToken.getMethods()));
        while (iterToken != null) {
            methods.addAll(ImplementedMethod.createMethods(iterToken.getDeclaredMethods()));
            iterToken = iterToken.getSuperclass();
        }

        return generateExecutable(methods.stream().map(ImplementedMethod::getMethod).collect(Collectors.toList()),
                method -> generateSignature(method, method.getName(), method.getReturnType().getCanonicalName()),
                Implementor::generateMethodBody,
                method -> Modifier.isAbstract(method.getModifiers()));
    }

    /**
     * Validate input for implementation.
     * @param token implementing class or interface.
     * @param root root directory.
     * @throws ImplerException if an invalid arguments given.
     */
    private void validateInput(final Class<?> token, final Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Arguments should be not null");
        }
        if (token.isPrimitive()) {
            throw new ImplerException("Can not implement a primitive type");
        }
        if (Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("Can not implement final type");
        }
        if (token == Enum.class || token.isEnum()) { // :NOTE: does not catch actual enums
            throw new ImplerException("Can not implement Enum");
        }
        if (Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Can not implement private type");
        }
        if (token.isArray()) {
            throw new ImplerException("Can not implement array type");
        }
    }

    /**
     * Create directory by given path.
     * @param path file location.
     * @throws ImplerException if creation error occurred.
     */
    private void makeDir(final Path path) throws ImplerException {
        final Path parent = path.toAbsolutePath().getParent();

        if (Objects.nonNull(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new ImplerException("Can not create file directory", e);
            }
        }
    }

    /**
     * Resolving path.
     * @param root root directory.
     * @param token implementing class or interface.
     * @param fileType {@code String} file type
     * @return resolved path by given arguments.
     */
    private Path resolvePath(Path root, Class<?> token, String fileType) {
        return root.toAbsolutePath()
                .resolve(token.getPackageName().replace(".", File.separator))
                .resolve(token.getSimpleName() + fileType);
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        validateInput(token, root);

        Path path = resolvePath(root, token, FILENAME_END);

        makeDir(path);

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(toUnicode(String.join(
                    "",
                    generatePackage(token),
                    generateClassDeclaration(token),
                    BLOCK_BEGIN,
                    generateConstructors(token),
                    generateMethods(token),
                    BLOCK_END
            )));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Compiles source file of implementing {@code token} and locate that to {@code path}.
     * @param token implementing class or interface.
     * @param path destination of compiled filed
     * @throws ImplerException if can not find java compiler or compile exit code is not a zero
     */
    private void compileFiles(Class<?> token, Path path) throws ImplerException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Can not find java compiler");
        }

        final String[] args;
        final CodeSource codeSource = token.getProtectionDomain().getCodeSource();
        final String filePath = resolvePath(path, token, FILENAME_END).toString();
        if (codeSource == null) {
            args = new String[]{ "--patch-module", token.getModule().getName() + "=" + path.toString() ,filePath };
        } else {
            String classpath = path + File.pathSeparator + getClassPath(codeSource);
            args = new String[]{ filePath, "-cp", classpath };
        }

        int compileResult = compiler.run(null, null, null, args);
        if (compileResult != 0) {
            throw new ImplerException("Compile exit code is not a zero");
        }
    }

    /**
     * Returns class path.
     * @param codeSource implementing class or interface {@link CodeSource}.
     * @return class path.
     * @throws ImplerException if URI syntax error occurred.
     */
    private static String getClassPath(final CodeSource codeSource) throws ImplerException {
        try {
            return Path.of(codeSource.getLocation().toURI()).toString();
        } catch (final URISyntaxException e) {
            throw new ImplerException("URI syntax exception=[" + e + "]");
        }
    }

    /**
     * Generate Jar ({@code .jar}) file.
     * @param token implementing class or interface.
     * @param classDir compiled class location.
     * @param outputFile JAR destination.
     * @throws ImplerException if Jar file output error occurred.
     */
    private void generateJar(final Class<?> token, final Path classDir, final Path outputFile) throws ImplerException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try(JarOutputStream stream = new JarOutputStream(Files.newOutputStream(outputFile), manifest)) {
            String entry = token.getPackageName().replace('.', '/') + "/" + token.getSimpleName() + CLASSNAME_END;
            stream.putNextEntry(new ZipEntry(entry));
            Files.copy(resolvePath(classDir, token, CLASSNAME_END), stream);
        } catch (IOException e) {
            throw new ImplerException("Jar file output error occurred");
        }
    }

    /**
     * File visitor that removes all visited files and directories.
     */
    private static final FileVisitor<Path> DELETE_FILE_VISITOR = new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };

    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        validateInput(token, jarFile);
        makeDir(jarFile);

        Path tmp;
        try {
            tmp = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "tmp");
        } catch (IOException e) {
            throw new ImplerException("Can not create temporary directory", e);
        }

        try {
            implement(token, tmp);
            compileFiles(token, tmp);
            generateJar(token, tmp, jarFile);
        } finally {
            try {
                Files.walkFileTree(tmp, DELETE_FILE_VISITOR);
            } catch (IOException e) {
                System.err.println("Error occurred while deleting temporary files");
            }
        }
    }

    /**
     * Unicode encoded string to string letters.
     * @param str encoded {@code String}.
     * @return decoded {@code String}
     */
    private String toUnicode(String str) {
        StringBuilder result = new StringBuilder();
        for (char ch : str.toCharArray()) {
            result.append(ch < 128 ? ch : String.format("\\u%04x", (int) ch));
        }
        return result.toString();
    }

    /**
     * Validates arguments and invoke implementor.
     * @param args arguments for implementor.
     */
    public static void main(String[] args) {
        if (args == null || args.length < 2 || args.length > 3) {
            System.err.println("Wrong arguments");
            return;
        }

        if (args.length == 3 && !args[0].equals("-jar")) {
            System.err.println("[-jar] expected as first argument");
        }

        Implementor implementor = new Implementor();

        try {
            if (args.length == 2) {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            } else {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Wrong class name=[" + e.getMessage() + "]");
        } catch (ImplerException e) {
            System.err.println(e.getMessage());
        }
    }
}