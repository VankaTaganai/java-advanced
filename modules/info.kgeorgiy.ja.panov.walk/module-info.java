module info.kgeorgiy.ja.panov.walk {
    requires transitive info.kgeorgiy.java.advanced.walk;
    requires transitive info.kgeorgiy.java.advanced.base;

    exports info.kgeorgiy.ja.panov.walk;

    opens info.kgeorgiy.ja.panov.walk to junit;
}