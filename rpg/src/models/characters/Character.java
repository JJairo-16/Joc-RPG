package models.characters;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import models.effects.Effect;
import models.weapons.AttackResult;
import models.weapons.Weapon;
import models.weapons.WeaponType;

import models.effects.EffectResult;
import models.effects.StackingRule;
import models.weapons.passives.HitContext;

/**
 * Representa un personatge del joc amb estadístiques, raça i arma equipable.
 * Inclou validacions bàsiques a la construcció i accions de combat.
 */
public class Character {

    private static final int TOTAL_POINTS = 140;
    private static final int MIN_STAT = 10;
    private static final int MIN_CONSTITUTION = MIN_STAT + 2; // mínim específic per a la vida

    private final String name;
    private final int age;
    private final Breed breed;

    private final Statistics stats;
    private Weapon weapon;

    private final Random rng = new Random();
    private final List<Effect> effects = new ArrayList<>();

    /**
     * Crea un personatge i valida:
     * nom, edat, longitud d'stats (7), mínims i suma total (140).
     *
     * @param name  nom del personatge
     * @param age   edat del personatge
     * @param stats estadístiques en ordre: força, destresa, constitució,
     *              intel·ligència, saviesa, carisma, sort
     * @param breed raça usada per calcular les estadístiques efectives
     * @throws IllegalArgumentException si alguna validació falla
     */
    public Character(String name, int age, int[] stats, Breed breed) {
        validateName(name);
        validateAge(age);
        validateStats(stats);

        int[] effectiveStats = applyBreed(stats, breed);

        this.name = name;
        this.age = age;
        this.breed = breed;
        this.stats = new Statistics(effectiveStats);
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public Statistics geStatistics() {
        return stats;
    }

    public Weapon getWeapon() {
        return weapon;
    }

    /**
     * Prova d'equipar una arma si compleix els requisits de
     * {@link Weapon#canEquip(Statistics)}.
     *
     * @param w arma a equipar
     * @return {@code true} si s'ha equipat; {@code false} si no compleix requisits
     */
    public boolean setWeapon(Weapon w) {
        if (!w.canEquip(stats))
            return false;

        weapon = w;
        return true;
    }

    /**
     * Ataca amb l'arma equipada o, si no n'hi ha, amb dany bàsic físic.
     */
    public AttackResult attack() {
        if (weapon == null) {
            return new AttackResult(
                    WeaponType.PHYSICAL.getBasicDamage(5, stats),
                    "ataca amb les mans desnudes.");
        }

        return weapon.attack(stats, rng);
    }

    /**
     * Defensa reduint el dany rebut a la meitat.
     *
     * @param attack dany entrant
     * @return resultat amb dany rebut i missatge
     */
    public Result defend(double attack) {
        if (attack <= 0) {
            return new Result(0, name + " ha bloquejat... sense raó aparent.");
        }

        double recivied = attack / 2.0;
        stats.damage(recivied);
        return new Result(recivied, name + " ha bloquejat l'atac.");
    }

    /**
     * Intenta esquivar en funció de destresa i sort. Si falla, rep el dany sencer.
     *
     * @param attack dany entrant
     * @return resultat amb dany rebut i missatge
     */
    public Result dodge(double attack) {
        if (attack <= 0) {
            return new Result(0, name + " ha esquivat... l'aire.");
        }

        double dodgeProb = (stats.getDexterity() - 10) * 3.33;
        dodgeProb += stats.getLuck() * 0.002;

        double multier = (rng.nextDouble() < dodgeProb ? 0 : 1);
        double recivied = attack * multier;
        stats.damage(recivied);

        if (recivied <= 0) {
            return new Result(0, name + " ha esquivat l'atac.");
        }

        return new Result(recivied, name + " ha rebut l'atac de ple.");
    }

    /**
     * Aplica dany directe (sense bloqueig ni esquiva).
     */
    public Result getDamage(double attack) {
        stats.damage(attack);
        return new Result(attack, name + " ha rebut l'atac de ple.");
    }

    public boolean isAlive() {
        return stats.getHealth() > 0;
    }

    public void regen() {
        stats.reg();
    }

    // -------------------------
    // Helpers de validació
    // -------------------------

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nom no pot ser nul ni buit");
        }
    }

    private static void validateAge(int age) {
        if (age <= 0) {
            throw new IllegalArgumentException("L'edat ha de ser major que 0");
        }
    }

    private static void validateStats(int[] stats) {
        if (stats == null) {
            throw new IllegalArgumentException("L'array d'estadístiques no pot ser nul");
        }

        if (stats.length != 7) {
            throw new IllegalArgumentException("Les estadístiques han de contenir exactament 7 valors");
        }

        int sum = 0;

        for (int stat : stats) {
            if (stat < MIN_STAT) {
                throw new IllegalArgumentException("Cada estadística ha de ser com a mínim " + MIN_STAT);
            }
            sum += stat;
        }

        if (stats[2] < MIN_CONSTITUTION) {
            throw new IllegalArgumentException(
                    "La constitució (vida) ha de ser com a mínim " + MIN_CONSTITUTION);
        }

        if (sum != TOTAL_POINTS) {
            throw new IllegalArgumentException(
                    "La suma total de punts ha de ser exactament " + TOTAL_POINTS +
                            ". Suma actual: " + sum);
        }
    }

    private static int[] applyBreed(int[] stats, Breed breed) {
        Stat[] statValues = Stat.values();
        int[] effectiveStats = stats.clone();

        for (int i = 0; i < stats.length; i++) {
            effectiveStats[i] = Breed.effectiveStat(stats[i], statValues[i], breed);
        }

        return effectiveStats;
    }

    public Breed getBreed() {
        return breed;
    }

    public Random rng() {
        return rng;
    }

    /**
     * Aplica un efecte al personatge segons la seva {@link StackingRule}.
     *
     * <p>
     * Si ja existeix un efecte amb la mateixa {@link Effect#key()}, aplica la
     * regla:
     * </p>
     * <ul>
     * <li>IGNORE: no fa res</li>
     * <li>REPLACE: substitueix l'antic</li>
     * <li>REFRESH / STACK: crida {@link Effect#mergeFrom(Effect)} sobre
     * l'existent</li>
     * </ul>
     */
    public void addEffect(Effect incoming) {
        if (incoming == null)
            return;

        if (effects.isEmpty()) {
            effects.add(incoming);
            return;
        }

        for (int i = 0; i < effects.size(); i++) {
            Effect existing = effects.get(i);

            if (!existing.key().equals(incoming.key()))
                continue;

            StackingRule rule = existing.stackingRule();

            switch (rule) {
                case IGNORE:
                    return;

                case REPLACE:
                    effects.set(i, incoming);
                    return;

                case REFRESH, STACK:
                    existing.mergeFrom(incoming);
                    return;
            }
        }

        // No existia: afegim
        effects.add(incoming);

        // Ordre consistent per prioritat
        effects.sort(Comparator.comparingInt(Effect::priority).reversed());
    }

    /**
     * Executa els efectes del personatge en una fase del combat.
     *
     * <p>
     * Retorna missatges per log i elimina els efectes expirats.
     * </p>
     */
    public List<String> triggerEffects(HitContext ctx, HitContext.Phase phase, Random rng) {
        if (effects.isEmpty())
            return List.of();

        List<String> messages = new ArrayList<>();
        triggerEffects(ctx, phase, rng, messages);

        return messages;
    }

    /**
     * Executa els efectes del personatge en una fase del combat.
     *
     * <p>
     * Retorna missatges per log i elimina els efectes expirats.
     * </p>
     */
    public void triggerEffects(HitContext ctx, HitContext.Phase phase, Random rng, List<String> out) {
        if (effects.isEmpty())
            return;

        for (Effect e : effects) {
            if (!e.isActive())
                continue;

            EffectResult r = e.onPhase(ctx, phase, rng);
            if (r != null && r.message() != null && !r.message().isBlank()) {
                out.add(r.message());
            }
        }

        // Neteja d'expirats (després d'executar)
        cleanupExpiredEffects();
    }

    /**
     * Elimina efectes expirats.
     */
    private void cleanupExpiredEffects() {
        if (effects.isEmpty())
            return;

        effects.removeIf(Effect::isExpired);
    }

    /**
     * (Opcional) Exposa una vista dels efectes, per UI/debug.
     */
    public List<Effect> getEffects() {
        return List.copyOf(effects);
    }
}