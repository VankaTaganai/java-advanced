package info.kgeorgiy.java.advanced.implementor;

import info.kgeorgiy.java.advanced.base.BaseTester;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
public class OldTester extends BaseTester {
    public static void main(final String... args) {
        new Tester()
                .add("interface", OldInterfaceImplementorTest.class)
                .add("class", OldClassImplementorTest.class)
                .add("advanced", OldAdvancedImplementorTest.class)
                .run(args);
    }
}
