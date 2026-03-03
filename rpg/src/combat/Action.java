package combat;

/**
 * Representa les accions bàsiques que un personatge pot realitzar durant un torn de combat.
 *
 * <p>Cada valor indica el comportament general que s'aplicarà en la resolució
 * del torn dins del {@link CombatSystem}.</p>
 */
public enum Action {
    ATTACK,
    DEFEND,
    DODGE
}