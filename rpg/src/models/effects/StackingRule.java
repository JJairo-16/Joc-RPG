package models.effects;

/**
 * Regles d'apilament quan s'aplica un efecte que ja existeix al contenidor.
 */
public enum StackingRule {
    /** No fer res si ja existeix un efecte amb la mateixa key. */
    IGNORE,
    /** Substituir l'efecte existent per un de nou. */
    REPLACE,
    /** Refrescar duració/cooldown/càrregues segons la lògica de merge. */
    REFRESH,
    /** Apilar stacks/càrregues (fins als màxims) segons la lògica de merge. */
    STACK
}