package models.weapons;

import static models.weapons.WeaponType.*;

import java.util.List;

/**
 * Catàleg d'armes disponibles al joc.
 *
 * <p>Cada constant defineix una plantilla immutable amb totes les
 * característiques necessàries per crear una {@link Weapon}:</p>
 *
 * <ul>
 *   <li>Nom i descripció</li>
 *   <li>Dany base i configuració de crítics</li>
 *   <li>Tipus d'arma ({@link WeaponType})</li>
 *   <li>Comportament d'atac ({@link Attack})</li>
 *   <li>Cost de mana (si aplica)</li>
 * </ul>
 *
 * <p>El mètode {@link #create()} genera una instància nova de {@link Weapon}
 * basada en la configuració definida a l'enum.</p>
 */
public enum Arsenal {

    /**
     * Ballesta amb tret explosiu d'alt risc.
     *
     * <p>Pot causar gran dany a distància, però segons la lògica de l'habilitat
     * associada pot tenir efectes secundaris imprevisibles.</p>
     */
    EXPLOSIVE_CROSSBOW(
            "Ballesta explosiva",
            "Una ballesta inestable que converteix cada tret en una aposta: pot esclatar amb força devastadora contra l’enemic… o girar-se contra tu mateix.",
            120, 0.15, 1.6,
            RANGE,
            Skills::explosiveShot);

    /*
     * ─────────────────────────────────────────────────────────────
     * Dades internes de configuració de cada arma
     * ─────────────────────────────────────────────────────────────
     *
     * Aquest conjunt de camps defineix completament el comportament
     * de l'arma quan es crea una instància de Weapon.
     */
    private final String name;
    private final String description;

    private final int baseDamage;
    private final double criticalProb;
    private final double criticalDamage;

    private final WeaponType type;
    private final Attack attack;
    private final double manaPrice;

    /**
     * Constructor base per armes sense cost de mana.
     */
    private Arsenal(String name, String description, int baseDamage, double criticalProb, double criticalDamage,
            WeaponType type, Attack attack) {
        this.name = name;
        this.description = description;
        this.baseDamage = baseDamage;
        this.criticalProb = criticalProb;
        this.criticalDamage = criticalDamage;
        this.type = type;
        this.attack = attack;
        this.manaPrice = 0;
    }

    /**
     * Constructor complet per armes amb cost de mana.
     */
    private Arsenal(String name, String description, int baseDamage, double criticalProb, double criticalDamage,
            WeaponType type, Attack attack, double price) {
        this.name = name;
        this.description = description;
        this.baseDamage = baseDamage;
        this.criticalProb = criticalProb;
        this.criticalDamage = criticalDamage;
        this.type = type;
        this.attack = attack;
        this.manaPrice = price;
    }

    /**
     * Crea una nova instància de {@link Weapon} a partir de la configuració
     * definida en aquesta entrada de l'Arsenal.
     *
     * @return arma nova amb les propietats configurades
     */
    public Weapon create() {
        return new Weapon(name, baseDamage, criticalProb, criticalDamage, type, attack, manaPrice);
    }

    /**
     * @return nom visible de l'arma
     */
    public String getName() {
        return name;
    }

    /**
     * @return descripció completa de l'arma
     */
    public String getDescription() {
        return description;
    }

    /*
     * ─────────────────────────────────────────────────────────────
     * Utilitats estàtiques per mostrar i obtenir armes
     * ─────────────────────────────────────────────────────────────
     */

    /**
     * Llista preparada per mostrar al menú de selecció d'armes.
     * Inclou nom i descripció concatenats.
     */
    private static List<String> namesList = List.of(Arsenal.values()).stream()
            .map(w -> w.getName() + ": " + w.getDescription())
            .toList();

    /**
     * @return llista de noms descriptius de totes les armes disponibles
     */
    public static List<String> getNamesList() {
        return namesList;
    }

    /**
     * Cache interna amb totes les entrades de l'enum.
     */
    private static Arsenal[] weapons = Arsenal.values();

    /**
     * Retorna una arma nova segons el seu índex dins l'Arsenal.
     *
     * @param idx índex dins l'enum
     * @return instància nova de {@link Weapon}
     * @throws IllegalArgumentException si l'índex està fora de rang
     */
    public static Weapon getWeaponByIdx(int idx) {
        if (idx < 0 || idx > weapons.length)
            throw new IllegalArgumentException("L'index està fora del rang.");

        return weapons[idx].create();
    }
}