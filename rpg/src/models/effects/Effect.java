package models.effects;

import java.util.Random;

import models.weapons.passives.HitContext;

/**
 * Efecte genèric per fases (com les passives), però amb estat propi.
 *
 * <p>
 * Aquest és el “pare efecte” sense herència: una interfície amb mètodes default.
 * Els efectes reals es creen implementant aquesta interfície (classe, anònim, etc.)
 * i gestionant el seu {@link EffectState}.
 * </p>
 */
public interface Effect {

    /**
     * Identificador estable per agrupar/actualitzar efectes iguals dins un contenidor.
     *
     * <p>Ex: "SHIELD_CHARGES", "RAGE", "THORNS".</p>
     */
    String key();

    /**
     * Prioritat d'execució (més alt = s'executa abans).
     *
     * <p>Útil quan tens molts efectes i vols ordre consistent.</p>
     */
    default int priority() { return 0; }

    /**
     * Regla d'apilament quan s'aplica un efecte amb la mateixa {@link #key()}.
     */
    default StackingRule stackingRule() { return StackingRule.REPLACE; }

    /**
     * Màxim de càrregues (si l'efecte en fa servir).
     */
    default int maxCharges() { return 0; }

    /**
     * Màxim de stacks (si l'efecte en fa servir).
     */
    default int maxStacks() { return 1; }

    /**
     * Estat mutable de l'efecte.
     *
     * <p>
     * Nota: el contenidor d'efectes pot guardar aquesta instància i modificar-la.
     * </p>
     */
    EffectState state();

    /**
     * Indica si l'efecte està “actiu” per executar lògica.
     *
     * <p>
     * Per defecte, si està en cooldown no actua.
     * </p>
     */
    default boolean isActive() {
        return !state().onCooldown();
    }

    /**
     * Indica si l'efecte ha expirat i s'hauria d'eliminar del contenidor.
     *
     * <p>
     * Per defecte:
     * </p>
     * <ul>
     *   <li>Si maxCharges() > 0 → expira quan charges == 0</li>
     *   <li>Si remainingTurns > 0 → expira quan remainingTurns == 0 (quan es tiqueja)</li>
     * </ul>
     *
     * <p>
     * Un efecte pot sobreescriure això si vol una política diferent.
     * </p>
     */
    default boolean isExpired() {
        EffectState st = state();

        if (maxCharges() > 0) {
            return st.charges() <= 0;
        }

        // Si no usa càrregues, pot usar duració; si remainingTurns és 0,
        // això pot significar "sense duració" o "acabat". El contenidor ho
        // gestionarà; aquí fem una regla segura:
        return false;
    }

    /**
     * Merge quan s'aplica un efecte amb la mateixa key (apilar/refrescar/...) .
     *
     * <p>
     * El contenidor cridarà aquest mètode sobre l'efecte existent amb l'efecte nou.
     * </p>
     */
    default void mergeFrom(Effect incoming) {
        // Política per defecte (simple): REPLACE no fa merge (el contenidor substituirà).
        // STACK/REFRESH podrien sobreescriure aquest mètode.
    }

    // ─────────────────────────────
    // Pipeline per fases
    // ─────────────────────────────

    default EffectResult onPhase(HitContext ctx, HitContext.Phase phase, Random rng) {
        return switch (phase) {
            case BEFORE_ATTACK -> beforeAttack(ctx, rng);
            case MODIFY_DAMAGE -> modifyDamage(ctx, rng);
            case BEFORE_DEFENSE -> beforeDefense(ctx, rng);
            case AFTER_DEFENSE -> afterDefense(ctx, rng);
            case AFTER_HIT -> afterHit(ctx, rng);
            case END_TURN -> endTurn(ctx, rng);
        };
    }

    /** Abans de calcular/modificar el dany (ideal per checks i meta). */
    default EffectResult beforeAttack(HitContext ctx, Random rng) { return EffectResult.none(); }

    /** Per modificar el dany abans de defensar (ctx.addFlatDamage / ctx.multiplyDamage). */
    default EffectResult modifyDamage(HitContext ctx, Random rng) { return EffectResult.none(); }

    /** Abans d'aplicar DEFEND/DODGE (ideal per escuts / mitigació). */
    default EffectResult beforeDefense(HitContext ctx, Random rng) { return EffectResult.none(); }

    /** Després de DEFEND/DODGE (ja tens ctx.defenderResult + damageDealt). */
    default EffectResult afterDefense(HitContext ctx, Random rng) { return EffectResult.none(); }

    /** Després d'un impacte real (normalment ctx.damageDealt() > 0). */
    default EffectResult afterHit(HitContext ctx, Random rng) { return EffectResult.none(); }

    /**
     * Final del torn (ideal per tiquejar duració/cooldown).
     *
     * <p>
     * Per defecte: baixa cooldown i duració si existeixen.
     * </p>
     */
    default EffectResult endTurn(HitContext ctx, Random rng) {
        state().tickCooldown();
        state().tickDuration();
        return EffectResult.none();
    }
}