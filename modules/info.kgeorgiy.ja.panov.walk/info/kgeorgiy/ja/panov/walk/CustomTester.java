package info.kgeorgiy.ja.panov.walk;

import info.kgeorgiy.java.advanced.base.BaseTester;
import info.kgeorgiy.java.advanced.walk.OldRecursiveWalkTest;
import info.kgeorgiy.java.advanced.walk.OldWalkTest;

public class CustomTester extends BaseTester {
    public static void main(String[] args) {
        new CustomTester()
                .add("OldWalk", OldWalkTest.class)
                .add("OldRecursive", OldRecursiveWalkTest.class)
                .run(args);
    }
}
