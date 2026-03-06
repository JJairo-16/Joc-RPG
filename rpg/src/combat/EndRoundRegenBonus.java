package combat;

public final class EndRoundRegenBonus {
    private double bonusHealthPct;
    private double bonusManaPct;

    public void add(double hpPct, double manaPct) {
        bonusHealthPct += hpPct;
        bonusManaPct += manaPct;
    }

    public double bonusHealthPct() {
        return bonusHealthPct;
    }

    public double bonusManaPct() {
        return bonusManaPct;
    }
}