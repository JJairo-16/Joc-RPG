package models.characters;

public class Statistics {
    private final int strength;
    private final int dexterity;
    private final int constitution; // vida
    private final int intelligence;
    private final int wisdom;
    private final int charisma;
    private final int luck;

    private final double maxHealth;
    private final double maxMana;

    private double health;
    private double mana;

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

    public void reg(double qtt) {
        health = affectClamp(health, qtt, maxHealth, 0);
        mana = affectClamp(mana, qtt, maxMana, 0);
    }

    public void damage(double dmg) {
        health = Math.max(0, health - dmg);
    }

    public boolean consume(double price) {
        if (price > mana) {
            return false;
        }

        mana -= price;
        return true;
    }

    private double affectClamp(double act, double per, double max, double min) {
        return Math.clamp(act + max * per, min, max);
    }
}
