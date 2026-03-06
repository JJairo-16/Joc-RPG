package models.effects;

/**
 * Estat mutable d'un efecte.
 *
 * <p>
 * No defineix què fa l'efecte; només guarda la seva “vida útil”:
 * duració, càrregues, apilaments i cooldown.
 * </p>
 */
public final class EffectState {

    private int charges;        // càrregues disponibles (<=0 = expirat si en fa servir)
    private int stacks;         // apilaments (>=1 habitualment)
    private int remainingTurns; // torns restants (<=0 = expirat si l'efecte té duració)
    private int cooldownTurns;  // torns de cooldown (0 = llest)

    public EffectState(int charges, int stacks, int remainingTurns, int cooldownTurns) {
        this.charges = Math.max(0, charges);
        this.stacks = Math.max(0, stacks);
        this.remainingTurns = Math.max(0, remainingTurns);
        this.cooldownTurns = Math.max(0, cooldownTurns);
    }

    public static EffectState ofCharges(int charges) {
        return new EffectState(charges, 1, 0, 0);
    }

    public static EffectState ofDuration(int turns) {
        return new EffectState(0, 1, turns, 0);
    }

    public static EffectState ofStacks(int stacks) {
        return new EffectState(0, stacks, 0, 0);
    }

    // ── Getters ──────────────────────────────────────────────────

    public int charges() {
        return charges;
    }

    public int stacks() {
        return stacks;
    }

    public int remainingTurns() {
        return remainingTurns;
    }

    public int cooldownTurns() {
        return cooldownTurns;
    }

    // ── Helpers de modificació ───────────────────────────────────

    public boolean hasCharges() {
        return charges > 0;
    }

    public boolean onCooldown() {
        return cooldownTurns > 0;
    }

    public void addCharges(int amount, int maxCharges) {
        if (amount <= 0) {
            return;
        }
        charges = Math.min(maxCharges, charges + amount);
    }

    public boolean consumeCharge() {
        if (charges <= 0) {
            return false;
        }
        charges--;
        return true;
    }

    public void addStacks(int amount, int maxStacks) {
        if (amount <= 0) {
            return;
        }
        stacks = Math.min(maxStacks, stacks + amount);
    }

    public void setStacks(int value) {
        stacks = Math.max(0, value);
    }

    public void refreshDuration(int turns) {
        remainingTurns = Math.max(remainingTurns, Math.max(0, turns));
    }

    public void setDuration(int turns) {
        remainingTurns = Math.max(0, turns);
    }

    public void tickDuration() {
        if (remainingTurns > 0) {
            remainingTurns--;
        }
    }

    public void setCooldown(int turns) {
        cooldownTurns = Math.max(0, turns);
    }

    public void tickCooldown() {
        if (cooldownTurns > 0) {
            cooldownTurns--;
        }
    }

    /**
     * L'efecte es considera expirat si:
     * <ul>
     * <li>té duració i remainingTurns == 0</li>
     * <li>o té càrregues i charges == 0</li>
     * </ul>
     *
     * <p>
     * Com que no sabem si un efecte “usa” duració o càrregues, aquesta funció
     * només és un helper. El contenidor decidirà la política final.
     * </p>
     */
    public boolean isDepletedByTurns() {
        return remainingTurns == 0;
    }

    public boolean isDepletedByCharges() {
        return charges == 0;
    }
}