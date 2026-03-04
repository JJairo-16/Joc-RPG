package combat;

import java.util.Random;

import models.characters.Character;

/**
 * Política de prioritat de torn.
 *
 * <p>
 * Decideix quin jugador actua primer en un round segons:
 * </p>
 * <ul>
 *   <li>Accions triades (ATTACK/DEFEND/DODGE)</li>
 *   <li>Estadístiques / aleatorietat (si cal)</li>
 * </ul>
 *
 * <p>
 * Retorna {@code true} si el jugador 1 ha d'actuar abans que el jugador 2.
 * </p>
 */
public interface TurnPriorityPolicy {

    /**
     * Decideix si el jugador 1 va primer aquest round.
     *
     * @param p1  jugador 1
     * @param a1  acció del jugador 1
     * @param p2  jugador 2
     * @param a2  acció del jugador 2
     * @param rng font d'aleatorietat comuna del combat
     * @return {@code true} si p1 va primer; {@code false} si p2 va primer
     */
    boolean player1First(Character p1, Action a1, Character p2, Action a2, Random rng);
}