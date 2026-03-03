package models.characters;

import java.util.List;

/**
 * Enumeració de races disponibles i el seu bonus d'estadística principal.
 */
public enum Breed {

    ORC(
            Stat.STRENGTH,
            "Orc",
            "Guerrers ferotges i resistents, amb una força descomunal que els converteix en combatents temibles cos a cos."
    ),
    ELF(
            Stat.DEXTERITY,
            "Elf",
            "Àgils i precisos, experts en armes a distància i moviments ràpids. La seva destresa supera la de qualsevol altra raça."
    ),
    DWARF(
            Stat.CONSTITUTION,
            "Nan",
            "Robustos i tenaços, amb una constitució excepcional que els permet resistir cops devastadors."
    ),
    GNOME(
            Stat.INTELLIGENCE,
            "Gnom",
            "Ments brillants i curioses, especialistes en màgia i coneixement arcà."
    ),
    HUMAN(
            Stat.WISDOM,
            "Humà",
            "Versàtils i adaptables, destaquen per la seva saviesa i capacitat d'aprendre de qualsevol situació."
    ),
    TIEFLING(
            Stat.CHARISMA,
            "Tiflíng",
            "D'origen infernal, carismàtics i enigmàtics, amb una presència que intimida o sedueix amb facilitat."
    ),
    HALFLING(
            Stat.LUCK,
            "Halfling",
            "Petits però sorprenentment afortunats, la sort sovint juga al seu favor en els moments més crítics."
    );

    private static final double STAT_BONUS_BY_BREED = 1.15;

    private static final Breed[] BREEDS = Breed.values();

    private static final List<String> NAMES_LIST = List.of(BREEDS).stream()
            .map(b -> b.getName() + ": " + b.getDescription())
            .toList();

    private final Stat bonusStat;
    private final String name;
    private final String description;

    Breed(Stat bonusStat, String name, String description) {
        this.bonusStat = bonusStat;
        this.name = name;
        this.description = description;
    }

    /** Estadística que rep el bonus racial. */
    public Stat bonusStat() {
        return bonusStat;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Aplica el multiplicador racial si l'estadística coincideix amb el bonus.
     */
    public static int effectiveStat(int base, Stat stat, Breed breed) {
        if (breed.bonusStat() != stat) return base;
        return (int) Math.round(base * STAT_BONUS_BY_BREED);
    }

    /** Llista amb nom i descripció de cada raça. */
    public static List<String> getNamesList() {
        return NAMES_LIST;
    }

    /**
     * Retorna la raça segons l'índex.
     *
     * @throws IllegalArgumentException si l'índex està fora de rang
     */
    public static Breed getByIdx(int idx) {
        if (idx < 0 || idx > BREEDS.length)
            throw new IllegalArgumentException("L'index està fora del rang.");

        return BREEDS[idx];
    }
}