package combat;

/**
 * Enum que defineix els possibles estats finals d'un combat.
 *
 * <p>S'utilitza per indicar si el combat continua o quin jugador
 * ha resultat vencedor.</p>
 */
public enum Winner {
    NONE,
    PLAYER1,
    PLAYER2,
    TIE
}