package creator;

import java.util.List;

import models.characters.Breed;
import models.characters.Character;
import utils.input.Getters;
import utils.input.Menu;
import utils.ui.Prettier;
import utils.rng.StatsBudget;
import utils.rng.StatsBudget.Result;

/**
 * Gestor de creació de personatges.
 *
 * <p>Aquesta classe centralitza la lògica per crear una instància de {@link Character}
 * demanant dades per consola. Permet dues vies de generació:</p>
 * <ul>
 *   <li><b>Automàtica</b>: reparteix els punts d'estadístiques i assigna una raça mitjançant {@link StatsBudget}.</li>
 *   <li><b>Manual</b>: l'usuari tria la raça i reparteix exactament {@value #TOTAL_POINTS} punts entre 7 estadístiques.</li>
 * </ul>
 *
 * <p><b>Nota:</b> Aquesta classe és utilitària (no instanciable) i només exposa mètodes estàtics.</p>
 */
public class CharacterCreator {

    /**
     * Constructor privat per evitar instàncies.
     */
    private CharacterCreator() {}

    /**
     * Utilitat d'entrada per llegir valors des de consola amb validació.
     */
    private static final Getters getter = new Getters();

    /** Longitud mínima del nom del personatge. */
    private static final int MIN_NAME_LEN = 3;

    /** Longitud màxima del nom del personatge. */
    private static final int MAX_NAME_LEN = 20;

    /** Edat mínima permesa del personatge. */
    private static final int MIN_AGE = 12;

    /**
     * Edat màxima permesa del personatge.
     * <p>En aquest cas s'accepta qualsevol enter positiu fins al límit d'Integer.</p>
     */
    private static final int MAX_AGE = Integer.MAX_VALUE;

    /**
     * Pressupost total de punts per repartir entre les 7 estadístiques.
     * <p>En mode manual, la suma ha de ser exactament aquest valor.</p>
     */
    private static final int TOTAL_POINTS = 140;

    /**
     * Restricció mínima genèrica per a cada estadística.
     * <p><b>Important:</b> Ha de coincidir amb les restriccions definides al teu {@code Character}.</p>
     */
    private static final int MIN_STAT = 5;

    /**
     * Restricció mínima específica per a Constitució (vida).
     * <p>Es força un mínim superior al de la resta d'estadístiques.</p>
     */
    private static final int MIN_CONSTITUTION = 10;

    /**
     * Llista de noms de races disponibles, obtinguda des de {@link Breed}.
     * <p>Es fa servir per mostrar el menú a l'usuari en la generació manual.</p>
     */
    private static final List<String> breeds = Breed.getNamesList();

    /**
     * Crea un nou personatge preguntant a l'usuari el nom i l'edat, i permetent
     * decidir si la generació d'estadístiques i raça serà automàtica o manual.
     *
     * @return una nova instància de {@link Character} amb nom, edat, estadístiques i raça.
     */
    public static Character createNewCharacter() {
        String name = getter.getString(
                "Si us plau, introdueixi el nom del seu personatge: ",
                "El nom",
                MIN_NAME_LEN,
                MAX_NAME_LEN
        );

        int age = getter.getInteger(
                "Si us plau, introdueixi l'edat del seu personatge: ",
                "L'edat",
                MIN_AGE,
                MAX_AGE
        );

        boolean autoGenerate = getter.getBoolean("Vol generar el personatge automàticament? [S/N] ", true, "S", "N");

        Generation gen = autoGenerate ? autoGenerate() : manualGenerate();

        return new Character(name, age, gen.stats(), gen.breed());
    }

    /**
     * Registre intern que encapsula el resultat d'una generació:
     * el paquet d'estadístiques (ordre fix) i la raça.
     *
     * <p>L'ordre de {@code stats} és:
     * Força, Destresa, Constitució, Intel·ligència, Saviesa, Carisma, Sort.</p>
     *
     * @param stats array d'enters amb les 7 estadístiques
     * @param breed raça seleccionada o generada
     */
    private record Generation(int[] stats, Breed breed) {}

    /**
     * Generació automàtica del personatge.
     *
     * <p>Fa servir {@link StatsBudget#generate(int)} per repartir {@value #TOTAL_POINTS}
     * i recuperar:</p>
     * <ul>
     *   <li>les estadístiques base</li>
     *   <li>la raça associada al resultat</li>
     * </ul>
     *
     * @return un {@link Generation} amb estadístiques i raça generades automàticament
     */
    private static Generation autoGenerate() {
        Result res = StatsBudget.generate(TOTAL_POINTS);
        return new Generation(res.baseStats(), res.breed());
    }

    /**
     * Generació manual del personatge.
     *
     * <p>Flux:</p>
     * <ol>
     *   <li>Tria de raça mitjançant un menú.</li>
     *   <li>Repartiment manual de punts en 7 estadístiques, amb restriccions mínimes.</li>
     *   <li>Validació estricta: la suma ha de ser exactament {@value #TOTAL_POINTS}.</li>
     * </ol>
     *
     * <p>Si la suma no és correcta, es mostra un avís i es torna a demanar tot el repartiment.</p>
     *
     * @return un {@link Generation} amb estadístiques i raça escollides manualment
     */
    private static Generation manualGenerate() {
        // 1) Raça
        int breedIdx = Menu.getOption(breeds, "Tria una raça:") - 1;
        Breed breed = Breed.values()[breedIdx];

        // 2) Stats manuals (forcem suma TOTAL_POINTS)
        while (true) {
            System.out.println("\nReparteix " + TOTAL_POINTS + " punts en 7 estadístiques.");
            System.out.println("Mínim per estadística: " + MIN_STAT + " | Constitució mínim: " + MIN_CONSTITUTION);

            int strength = getter.getInteger("Força: ", "La força", MIN_STAT, TOTAL_POINTS);
            int dexterity = getter.getInteger("Destresa: ", "La destresa", MIN_STAT, TOTAL_POINTS);
            int constitution = getter.getInteger("Constitució (vida): ", "La constitució", MIN_CONSTITUTION, TOTAL_POINTS);
            int intelligence = getter.getInteger("Intel·ligència: ", "L'intel·ligència", MIN_STAT, TOTAL_POINTS);
            int wisdom = getter.getInteger("Saviesa: ", "La saviesa", MIN_STAT, TOTAL_POINTS);
            int charisma = getter.getInteger("Carisma: ", "El carisma", MIN_STAT, TOTAL_POINTS);
            int luck = getter.getInteger("Sort: ", "La sort", MIN_STAT, TOTAL_POINTS);

            int sum = strength + dexterity + constitution + intelligence + wisdom + charisma + luck;

            if (sum != TOTAL_POINTS) {
                Prettier.warn("La suma ha de ser exactament " + TOTAL_POINTS + ". Suma actual: " + sum);
                Menu.pause();
                continue;
            }

            int[] stats = new int[] {
                    strength, dexterity, constitution,
                    intelligence, wisdom, charisma, luck
            };

            return new Generation(stats, breed);
        }
    }
}