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

        this.maxHealth = constitution * 50.0;
        this.maxMana = intelligence * 30.0;

        health = maxHealth;
        mana = maxMana;
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

    /**
     * Regenera vida i mana segons constitució i intel·ligència, sense superar els màxims.
     */
    public void reg() {
        double hp = constitution * 3.0;
        double ma = intelligence * 2.0;

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
    public boolean consume(double price) {
        if (price > mana) {
            return false;
        }

        mana -= price;
        return true;
    }

    /**
     * Afegeix una quantitat proporcional al màxim i limita el resultat entre min i max.
     */
    private double affectClamp(double act, double per, double max, double min) {
        return Math.clamp(act + max * per, min, max);
    }
}