package models.weapons;

import static models.weapons.WeaponType.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static models.weapons.passives.Passives.*;
import models.weapons.passives.WeaponPassive;

/**
 * Catàleg d'armes disponibles al joc.
 *
 * <p>
 * Cada constant defineix una plantilla immutable amb totes les
 * característiques necessàries per crear una {@link Weapon}.
 * </p>
 */
public enum Arsenal {

    EXPLOSIVE_CROSSBOW(
            "Ballesta explosiva",
            "Una ballesta inestable que converteix cada tret en una aposta.",
            80, 0.12, 1.6,
            RANGE,
            Skills::explosiveShot),

    VAMPIRICS_DAGGERS(
            "Dagues vampíriques",
            "Dues fulles àgils que colpegen dues vegades i et retornen vida amb cada impacte.",
            42, 0.16, 1.35,
            PHYSICAL,
            Skills::nothing,
            lifeSteal(0.10)),

    ARCANE_DISRUPTION_STAFF(
            "Bastó de la disrupció arcana",
            "Un bastó inestable que canalitza una explosió d'energia pura. Pot devastar... o dissipar-se sense efecte.",
            120, 0.08, 1.5,
            MAGICAL,
            Skills::arcaneDisruption,
            200),

    SCIMITAR(
            "Cimitarra del desert",
            "Una fulla corba i elegant dissenyada per a cops ràpids i precisos. Els seus talls són fluids i letals en mans hàbils.",
            80, 0.20, 1.4,
            PHYSICAL,
            Skills::nothing),

    BALLISTA(
            "Ballista de la sort",
            "Una arma capritxosa que pot disparar entre un i quatre projectils. "
                    + "Cada tret consecutiu és més difícil d'encertar... "
                    + "però la sort pot desafiar el destí.",
            44, 0.15, 1.4,
            RANGE,
            Skills::luckyBallista),
    GRIMORIE(
            "Grimori del codi",
            "Un grimori viu que et mostra un codi de 5 dígits (1-4). "
                    + "Com més ràpid el recites, més dany infligeixes; si t'entrebanques, el grimori et penalitza.",
            110, 0.12, 1.35,
            MAGICAL,
            Skills::grimoriCipher,
            135),
    BLOODPIERCER_THROWING_DAGGER(
            "Daga Perforasang",
            "Una daga lleugera dissenyada per travessar defenses. "
                    + "La seva fulla estreta troba els punts febles amb facilitat.",
            28, 0.16, 1.45,
            RANGE,
            Skills::perforatingThrow,
            trueHarm(0.003)),
    EXECUTIONERS_EDGE(
            "Sentència Final",
            "Una espasa concebuda per culminar el combat. "
                    + "Quan l'enemic es troba al llindar de la derrota, "
                    + "la seva fulla colpeja amb una determinació implacable.",
            76, 0.18, 1.4,
            PHYSICAL,
            Skills::nothing,
            executor(0.30, 0.25)),
    CHRONO_WEAVER_STAFF(
            "Bastó Tejedor del Tiempo",
            "Un bastó antic que distorsiona el flux temporal. "
                    + "Els seus atacs poden repetir-se en un eco del passat.",
            88, 0.14, 1.60,
            MAGICAL,
            Skills::chronoWeave,
            95);

    /*
     * ─────────────────────────────────────────────────────────────
     * Dades internes de configuració de cada arma
     * ─────────────────────────────────────────────────────────────
     */
    private final String name;
    private final String description;

    private final int baseDamage;
    private final double criticalProb;
    private final double criticalDamage;

    private final WeaponType type;
    private final Attack attack;
    private final double manaPrice;

    private final List<WeaponPassive> passives;

    private Arsenal(
            String name,
            String description,
            int baseDamage,
            double criticalProb,
            double criticalDamage,
            WeaponType type,
            Attack attack,
            WeaponPassive... passives) {
        this(name, description, baseDamage, criticalProb, criticalDamage, type, attack, 0, passives);
    }

    private Arsenal(
            String name,
            String description,
            int baseDamage,
            double criticalProb,
            double criticalDamage,
            WeaponType type,
            Attack attack,
            double price,
            WeaponPassive... passives) {
        this.name = name;
        this.description = description;

        this.baseDamage = baseDamage;
        this.criticalProb = criticalProb;
        this.criticalDamage = criticalDamage;

        this.type = type;
        this.attack = attack;
        this.manaPrice = price;

        this.passives = (passives == null || passives.length == 0)
                ? List.of()
                : List.of(passives); // immutable
    }

    /**
     * Crea una nova instància de {@link Weapon} a partir de la configuració
     * definida en aquesta entrada de l'Arsenal.
     */
    public Weapon create() {
        return new Weapon(
                name, description,
                baseDamage, criticalProb, criticalDamage,
                type, attack, manaPrice,
                passives, this);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public WeaponType getType() {
        return type;
    }

    public int getBaseDamage() {
        return baseDamage;
    }

    public double getCriticalProb() {
        return criticalProb;
    }

    public double getCriticalDamage() {
        return criticalDamage;
    }

    public double getManaPrice() {
        return manaPrice;
    }

    /**
     * Retorna els requisits mínims per equipar l'arma segons el seu
     * {@link WeaponType}.
     *
     * <p>
     * S'utilitza per mostrar informació al menú (no valida res).
     * </p>
     */
    public String getMinStatsInfo() {
        return "Mínims: " + type.toString();
    }

    /*
     * ─────────────────────────────────────────────────────────────
     * Utilitats estàtiques per mostrar i obtenir armes
     * ─────────────────────────────────────────────────────────────
     */

    /**
     * Cache interna amb totes les entrades de l'enum, ordenades per tipus i nom.
     * Aquesta és la font de veritat per al menú.
     */
    private static final Arsenal[] weaponsSorted = Arrays.stream(Arsenal.values())
            .sorted(Comparator
                    .comparing((Arsenal w) -> w.type.getName())
                    .thenComparing(Arsenal::getName))
            .toArray(Arsenal[]::new);

    /**
     * @return llista d'armes ordenades (per tipus i nom)
     */
    public static List<Arsenal> getSortedWeapons() {
        return List.of(weaponsSorted);
    }

    /**
     * Llista preparada per mostrar al menú de selecció d'armes (ordenada).
     */
    private static final List<String> namesList = Arrays.stream(weaponsSorted)
            .map(Arsenal::formatForMenu)
            .toList();

    private static String formatForMenu(Arsenal w) {
        String stats = String.format(
                "Tipus: %s | Dany: %d | Crit: %.0f%% | Mult: x%.2f%s",
                w.type.getName(),
                w.baseDamage,
                w.criticalProb * 100.0,
                w.criticalDamage,
                (w.manaPrice > 0) ? String.format(" | Mana: %.0f", w.manaPrice) : "");

        return w.getName() + " - " + w.getDescription() + " (" + stats + ")";
    }

    /**
     * @return llista de descripcions de totes les armes disponibles per al menú
     *         (ordenada)
     */
    public static List<String> getNamesList() {
        return namesList;
    }

    /**
     * Retorna una arma nova segons el seu índex dins l'Arsenal ORDENAT.
     *
     * @param idx índex dins el catàleg ordenat
     * @return instància nova de {@link Weapon}
     * @throws IllegalArgumentException si l'índex està fora de rang
     */
    public static Weapon getWeaponByIdx(int idx) {
        if (idx < 0 || idx >= weaponsSorted.length) {
            throw new IllegalArgumentException("L'index està fora del rang.");
        }
        return weaponsSorted[idx].create();
    }
}