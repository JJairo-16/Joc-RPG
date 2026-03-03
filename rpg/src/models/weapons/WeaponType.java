package models.weapons;

import models.characters.Statistics;

/**
 * Tipus d'arma amb requisits mínims d'stats per poder-se equipar.
 * <p>
 * Els valors de cada enum representen el mínim requerit per cada stat.
 * </p>
 */
public enum WeaponType {
    PHYSICAL(0, 0, 0, 0, 0, 0, 0, "Arma física"),
    MAGICAL(0, 0, 0, 15, 0, 0, 0, "Arma màgica"),
    RANGE(7, 15, 0, 0, 0, 0, 0, "Arma de rang");

    private final String name;

    private final int strength;
    private final int dexterity;
    private final int constitution; // vida
    private final int intelligence;
    private final int wisdom;
    private final int charisma;
    private final int luck;

    private WeaponType(int str, int dex, int con, int intel, int wis, int cha, int luck, String name) {
        this.strength = str;
        this.dexterity = dex;
        this.constitution = con;
        this.intelligence = intel;
        this.wisdom = wis;
        this.charisma = cha;
        this.luck = luck;

        this.name = name;
    }

    /**
     * Comprova si unes stats compleixen els mínims per equipar aquest tipus d'arma.
     *
     * @param stats estadístiques del personatge
     * @return {@code true} si compleix tots els mínims; {@code false} en cas contrari
     * @throws IllegalArgumentException si {@code stats} és {@code null}
     */
    public boolean canEquip(Statistics stats) {
        if (stats == null) {
            throw new IllegalArgumentException("Les estadístiques no poden ser null");
        }

        return stats.getStrength() >= strength
                && stats.getDexterity() >= dexterity
                && stats.getConstitution() >= constitution
                && stats.getIntelligence() >= intelligence
                && stats.getWisdom() >= wisdom
                && stats.getCharisma() >= charisma
                && stats.getLuck() >= luck;
    }

    public double getBasicDamage(int inputBase, Statistics stats) {
        int base = inputBase;
        
        switch (this) {
          case PHYSICAL -> base += stats.getStrength();
          case MAGICAL -> base += stats.getIntelligence();
          default -> base += stats.getDexterity();
        }

        return round2(base + stats.getDexterity() * 0.2);
    }

    private double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }

    public String getName() {
        return name;
    }
}