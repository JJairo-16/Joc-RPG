package models.effects.templates;

import java.util.Random;

import models.effects.Effect;
import models.effects.EffectResult;
import models.effects.EffectState;
import models.weapons.passives.HitContext;

public abstract class ConstantDamageEffect implements Effect {
    protected static String key = "ConstantDamageEffect";

    protected final EffectState state;
    protected double damagePerTurn;

    protected ConstantDamageEffect(int turns, double damagePerTurn) {
        this.state = EffectState.ofDuration(turns);
        this.damagePerTurn = damagePerTurn;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public EffectState state() {
        return state;
    }

    @Override
    public EffectResult endTurn(HitContext ctx, Random rng) {
        if (isExpired()) {
            return EffectResult.none();
        }

        state.tickDuration();

        return EffectResult.none();
    }

    @Override
    public boolean isExpired() {
        return state.remainingTurns() <= 0;
    }
}
