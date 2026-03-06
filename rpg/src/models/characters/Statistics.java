package models.characters;

/**
 * Emmagatzema les estadístiques base i els valors dinàmics (vida i mana).
 */
public class Statistics {

    // Estadístiques base
    private final int strength;
    private final int dexterity;
    private final int constitution; // vida
    private final int intelligence;
    private final int wisdom;
    private final int charisma;
    private final int luck;

    // Límits màxims
    private final double maxHealth;
    private final double maxMana;

    // Valors actuals
    private double health;
    private double mana;

    private static final int MAX_CONSTITUTION_FULL_EFFECT = 20;
    private static final double CONSTITUTION_VALUE = 50.0;

    private static final double HEALTH_SOFTCAP_FACTOR = 0.08;
    private static final double REGEN_SOFTCAP_FACTOR = 0.10;

    /**
     * Construeix les estadístiques a partir d'un array de 7 valors en ordre fix.
     *
     * @param stats força, destresa, constitució, intel·ligència, saviesa, carisma, sort
     */
    public Statistics(int[] stats) {
        this.strength = stats[0];
        this.dexterity = stats[1];
        this.constitution = stats[2];
        this.intelligence = stats[3];
        this.wisdom = stats[4];
        this.charisma = stats[5];
        this.luck = stats[6];

        this.maxHealth = calculateMaxHealth(constitution);
        this.maxMana = intelligence * 30.0;

        this.health = maxHealth;
        this.mana = maxMana;
    }

    public int getStrength() {
        return strength;
    }

    public int getDexterity() {
        return dexterity;
    }

    public int getConstitution() {
        return constitution;
    }

    public int getIntelligence() {
        return intelligence;
    }

    public int getWisdom() {
        return wisdom;
    }

    public int getCharisma() {
        return charisma;
    }

    public int getLuck() {
        return luck;
    }

    public double getHealth() {
        return health;
    }

    public double getMana() {
        return mana;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public double getMaxMana() {
        return maxMana;
    }

    /**
     * Regenera vida i mana segons constitució i intel·ligència, sense superar els màxims.
     */
    public void reg() {
        double hp = calculateHealthRegen(constitution);

        double ma = intelligence * 0.9;

        health = affectClamp(health, hp, maxHealth, 0);
        mana = affectClamp(mana, ma, maxMana, 0);
    }

    /**
     * Aplica dany a la vida (mai baixa de 0).
     *
     * @param dmg dany rebut
     */
    public void damage(double dmg) {
        health = Math.max(0, health - dmg);
    }

    /**
     * Consumeix mana si n'hi ha prou.
     *
     * @param price cost de mana
     * @return {@code true} si s'ha pogut pagar; {@code false} si no hi ha mana suficient
     */
    public boolean consumeMana(double price) {
        if (price > mana) {
            return false;
        }

        mana -= price;
        return true;
    }

    /**
     * Cura vida fins al màxim i retorna la curació real aplicada.
     *
     * @param amount quantitat de curació sol·licitada
     * @return quantitat real curada
     */
    public double heal(double amount) {
        if (amount <= 0) {
            return 0;
        }

        double before = health;
        health = Math.min(maxHealth, health + amount);
        return health - before;
    }

    /**
     * Restaura mana fins al màxim i retorna la quantitat real restaurada.
     *
     * @param amount quantitat de restauració sol·licitada
     * @return quantitat real restaurada
     */
    public double restoreMana(double amount) {
        if (amount <= 0) {
            return 0;
        }

        double before = mana;
        mana = Math.min(maxMana, mana + amount);
        return mana - before;
    }

    /**
     * Calcula la vida màxima amb soft cap suau a partir de 20 de constitució.
     */
    private double calculateMaxHealth(int con) {
        double effectiveCon = softenStat(con, MAX_CONSTITUTION_FULL_EFFECT, HEALTH_SOFTCAP_FACTOR);
        return effectiveCon * CONSTITUTION_VALUE;
    }

    /**
     * Calcula la regeneració de vida amb un soft cap una mica més fort que la vida màxima.
     */
    private double calculateHealthRegen(int con) {
        double effectiveCon = softenStat(con, MAX_CONSTITUTION_FULL_EFFECT, REGEN_SOFTCAP_FACTOR);
        return effectiveCon * 2.35;
    }

    /**
     * Fins al llindar, l'estadística té efecte complet.
     * A partir d'aquí, cada punt extra aporta una mica menys que l'anterior.
     */
    private double softenStat(int stat, int threshold, double factor) {
        if (stat <= threshold) {
            return stat;
        }

        double extra = stat - (double) threshold;
        return threshold + (extra / (1.0 + extra * factor));
    }

    /**
     * Aplica un increment i limita el resultat dins del rang indicat.
     */
    private double affectClamp(double act, double amount, double max, double min) {
        return Math.clamp(act + amount, min, max);
    }
}