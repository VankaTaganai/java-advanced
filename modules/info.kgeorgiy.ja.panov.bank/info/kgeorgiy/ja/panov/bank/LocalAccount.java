package info.kgeorgiy.ja.panov.bank;

public class LocalAccount extends AbstractAccount {
    public LocalAccount(final String id) {
        super(id);
    }

    public LocalAccount(final String id, final int amount) {
        super(id, amount);
    }
}
