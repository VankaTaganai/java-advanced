module info.kgeorgiy.ja.panov.bank {
    requires java.rmi;
    requires junit;

    exports info.kgeorgiy.ja.panov.bank;

    opens info.kgeorgiy.ja.panov.bank to junit;
}