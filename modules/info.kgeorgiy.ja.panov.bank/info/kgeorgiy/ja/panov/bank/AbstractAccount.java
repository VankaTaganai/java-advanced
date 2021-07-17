package info.kgeorgiy.ja.panov.bank;

public abstract class AbstractAccount implements Account {
    private final String id;
    private int amount;

    public AbstractAccount(final String id) {
        this(id, 0);
    }

    protected AbstractAccount(final String id, final int amount) {
        this.id = id;
        this.amount = amount;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public synchronized void setAmount(final int amount) {
        this.amount = amount;
    }

    @Override
    public synchronized void addAmount(final int amount) {
        this.amount += amount;
    }
}
