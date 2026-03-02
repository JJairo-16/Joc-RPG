package models.characters;

import java.util.Random;

import models.weapons.AttackResult;
import models.weapons.Weapon;

/**
 * Representa un personatge del joc amb un conjunt de característiques fixes.
 * 
 * Regles:
 * - El nom no pot ser nul ni buit.
 * - L'edat ha de ser major que 0.
 * - L'array d'estadístiques ha de contenir exactament 7 valors.
 * - Cap estadística pot ser inferior al mínim establert.
 * - La constitució (vida) té un mínim específic.
 * - La suma total dels punts ha de ser exactament 140.
 */
public class Character {

    private final String name;
    private final int age;

    private final Statistics stats;
    private Weapon weapon;

    private static final int TOTAL_POINTS = 140;
    private static final int MIN_STAT = 5;
    private static final int MIN_CONSTITUTION = 10; // mínim específic per a la vida

    private final Random rng = new Random();

    /**
     * Constructor del personatge amb validacions.
     *
     * @param name  Nom del personatge
     * @param age   Edat del personatge
     * @param stats Array amb les 7 estadístiques en ordre:
     *              força, destresa, constitució, intel·ligència,
     *              saviesa, carisma i sort.
     *
     * @throws IllegalArgumentException si alguna validació no es compleix
     */
    public Character(String name, int age, int[] stats, Breed breed) {

        // Validació del nom
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nom no pot ser nul ni buit");
        }

        // Validació de l'edat
        if (age <= 0) {
            throw new IllegalArgumentException("L'edat ha de ser major que 0");
        }

        // Validació de l'array
        if (stats == null) {
            throw new IllegalArgumentException("L'array d'estadístiques no pot ser nul");
        }

        if (stats.length != 7) {
            throw new IllegalArgumentException("Les estadístiques han de contenir exactament 7 valors");
        }

        int suma = 0;

        for (int stat : stats) {
            if (stat < MIN_STAT) {
                throw new IllegalArgumentException(
                        "Cada estadística ha de ser com a mínim " + MIN_STAT);
            }
            suma += stat;
        }

        // Validació específica de la constitució
        if (stats[2] < MIN_CONSTITUTION) {
            throw new IllegalArgumentException(
                    "La constitució (vida) ha de ser com a mínim " + MIN_CONSTITUTION);
        }

        // Validació de la suma total
        if (suma != TOTAL_POINTS) {
            throw new IllegalArgumentException(
                    "La suma total de punts ha de ser exactament " + TOTAL_POINTS +
                            ". Suma actual: " + suma);
        }

        Stat[] statValues = Stat.values();
        for (int i = 0; i < stats.length; i++) {
            stats[i] = Breed.effectiveStat(stats[i], statValues[i], breed);
        }

        // Assignació final (només si tot és correcte)
        this.name = name;
        this.age = age;

        this.stats = new Statistics(stats);
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public AttackResult attack() {
        return weapon.attack(stats, rng);
    }

    public Result defend(double attack) {
        if (attack <= 0) {
            return new Result(0, name + " ha bloquejat... sense raó aparent.");
        }

        double recivied = attack / 2.0;
        stats.damage(recivied);
        return new Result(recivied, name + " ha bloquejat l'atac.");
    }

    public Result dodge(double attack) {
        if (attack <= 0) {
            return new Result(0, name + " ha esquivat... l'aire.");
        }

        double dodgeProb = (stats.getDexterity() - 5) * 3.33;
        dodgeProb += stats.getLuck() * 0.002;

        double multier = (rng.nextDouble() < dodgeProb ? 0 : 1);
        double recivied = attack * multier;
        stats.damage(recivied);

        if (recivied <= 0) {
            return new Result(0, name + " ha esquivat l'atac.");
        }

        return new Result(recivied, name + " ha rebut l'atac de ple.");
    }
}