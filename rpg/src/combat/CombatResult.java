package combat;

/**
 * Representa el resultat global d'un round o estat de combat.
 *
 * @param finished indica si el combat ha finalitzat
 * @param winner identificador del guanyador (segons la convenció utilitzada)
 *
 * <p>Aquest record encapsula de manera immutable la informació mínima
 * necessària per determinar l'estat actual del combat.</p>
 */
public record CombatResult(boolean finished, int winner) {

}