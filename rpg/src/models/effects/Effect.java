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
 *
 * <p>
 * Els efectes poden reaccionar tant a les fases del pipeline com als esdeveniments
 * registrats dins el {@link HitContext}. Per exemple, en una fase tardana poden
 * consultar {@code ctx.hasEvent(HitContext.Event.ON_CRIT)} per saber si el cop
 * ha resultat crític.
 * </p>
 */
public interface Effect {

    /**
     * Identificador estable per agrupar/actualitzar efectes iguals dins un contenidor.
     *
     * <p>
     * Exemples: {@code "SHIELD_CHARGES"}, {@code "RAGE"}, {@code "THORNS"}.
     * </p>
     *
     * @return clau estable de l'efecte
     */
    String key();

    /**
     * Prioritat d'execució (més alt = s'executa abans).
     *
     * @return prioritat de l'efecte
     */
    default int priority() {
        return 0;
    }

    /**
     * Regla d'apilament quan s'aplica un efecte amb la mateixa {@link #key()}.
     *
     * @return regla d'apilament
     */
    default StackingRule stackingRule() {
        return StackingRule.REPLACE;
    }

    /**
     * Màxim de càrregues (si l'efecte en fa servir).
     *
     * @return màxim de càrregues
     */
    default int maxCharges() {
        return 0;
    }

    /**
     * Màxim de stacks (si l'efecte en fa servir).
     *
     * @return màxim d'apilaments
     */
    default int maxStacks() {
        return 1;
    }

    /**
     * Estat mutable de l'efecte.
     *
     * @return estat intern de l'efecte
     */
    EffectState state();

    /**
     * Indica si l'efecte està actiu.
     *
     * <p>
     * Per defecte, si està en cooldown no actua.
     * </p>
     *
     * @return {@code true} si l'efecte pot actuar
     */
    default boolean isActive() {
        return !state().onCooldown();
    }

    /**
     * Indica si l'efecte ha expirat i s'hauria d'eliminar del contenidor.
     *
     * @return {@code true} si es considera expirat
     */
    default boolean isExpired() {
        EffectState st = state();

        if (maxCharges() > 0) {
            return st.charges() <= 0;
        }

        return false;
    }

    /**
     * Merge quan s'aplica un efecte amb la mateixa key.
     *
     * @param incoming efecte entrant
     */
    default void mergeFrom(Effect incoming) {
        // Política per defecte:
        // REPLACE no fa merge i el contenidor substituirà l'efecte existent.
    }

    // ─────────────────────────────
    // Pipeline per fases
    // ─────────────────────────────

    /**
     * Punt d'entrada principal per executar la lògica de l'efecte en una fase.
     *
     * @param ctx context del cop
     * @param phase fase actual
     * @param rng generador aleatori
     * @return resultat de l'execució
     */
    default EffectResult onPhase(HitContext ctx, HitContext.Phase phase, Random rng) {
        return switch (phase) {
            case START_TURN -> startTurn(ctx, rng);
            case BEFORE_ATTACK -> beforeAttack(ctx, rng);
            case ROLL_CRIT -> rollCrit(ctx, rng);
            case MODIFY_DAMAGE -> modifyDamage(ctx, rng);
            case BEFORE_DEFENSE -> beforeDefense(ctx, rng);
            case AFTER_DEFENSE -> afterDefense(ctx, rng);
            case AFTER_HIT -> afterHit(ctx, rng);
            case END_TURN -> endTurn(ctx, rng);
        };
    }

    /**
     * Lògica a l'inici del torn.
     *
     * @param ctx context del cop
     * @param rng generador aleatori
     * @return resultat de l'efecte
     */
    default EffectResult startTurn(HitContext ctx, Random rng) {
        return EffectResult.none();
    }

    /**
     * Lògica abans de l'atac.
     *
     * @param ctx context del cop
     * @param rng generador aleatori
     * @return resultat de l'efecte
     */
    default EffectResult beforeAttack(HitContext ctx, Random rng) {
        return EffectResult.none();
    }

    /**
     * Lògica durant la resolució del crític.
     *
     * <p>
     * Aquesta fase és ideal per fer coses com:
     * </p>
     * <ul>
     * <li>forçar 100% crític</li>
     * <li>prohibir crític</li>
     * <li>augmentar o reduir la probabilitat de crític</li>
     * <li>canviar el multiplicador del crític</li>
     * </ul>
     *
     * @param ctx context del cop
     * @param rng generador aleatori
     * @return resultat de l'efecte
     */
    default EffectResult rollCrit(HitContext ctx, Random rng) {
        return EffectResult.none();
    }

    /**
     * Lògica per modificar el dany abans de defensar.
     *
     * @param ctx context del cop
     * @param rng generador aleatori
     * @return resultat de l'efecte
     */
    default EffectResult modifyDamage(HitContext ctx, Random rng) {
        return EffectResult.none();
    }

    /**
     * Lògica abans d'aplicar DEFEND/DODGE.
     *
     * @param ctx context del cop
     * @param rng generador aleatori
     * @return resultat de l'efecte
     */
    default EffectResult beforeDefense(HitContext ctx, Random rng) {
        return EffectResult.none();
    }

    /**
     * Lògica després de DEFEND/DODGE.
     *
     * @param ctx context del cop
     * @param rng generador aleatori
     * @return resultat de l'efecte
     */
    default EffectResult afterDefense(HitContext ctx, Random rng) {
        return EffectResult.none();
    }

    /**
     * Lògica després d'un impacte real.
     *
     * @param ctx context del cop
     * @param rng generador aleatori
     * @return resultat de l'efecte
     */
    default EffectResult afterHit(HitContext ctx, Random rng) {
        return EffectResult.none();
    }

    /**
     * Final del torn.
     *
     * <p>
     * Per defecte: baixa cooldown i duració si existeixen.
     * </p>
     *
     * @param ctx context del cop
     * @param rng generador aleatori
     * @return resultat de l'efecte
     */
    default EffectResult endTurn(HitContext ctx, Random rng) {
        state().tickCooldown();
        state().tickDuration();
        return EffectResult.none();
    }
}