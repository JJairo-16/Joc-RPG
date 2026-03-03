package models.weapons;

/**
 * Defineix el tipus d'objectiu al qual es dirigeix un atac.
 *
 * <p>S'utilitza dins {@link AttackResult} per indicar si el dany
 * s'ha d'aplicar a l'enemic o al mateix atacant.</p>
 */
public enum Target {
    SELF,
    ENEMY
}