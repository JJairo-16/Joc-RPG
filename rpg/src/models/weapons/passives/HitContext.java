package models.weapons.passives;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import combat.Action;
import models.characters.Character;
import models.characters.Result;
import models.weapons.AttackResult;
import models.weapons.Target;
import models.weapons.Weapon;

/**
 * Context mutable d'una resolució d'atac.
 *
 * <p>
 * Substitueix l'antic {@code HitContext} immutable per donar suport a:
 * </p>
 * <ul>
 *   <li>Fases d'execució (abans/després) per a passius.</li>
 *   <li>Modificació del dany (multiplicadors/flat) abans d'aplicar-lo.</li>
 *   <li>Metadades (tags) per donar “context” a habilitats i passives.</li>
 * </ul>
 */
public final class HitContext {

    /** Fases on poden actuar les passives. */
    public enum Phase {
        BEFORE_ATTACK,
        MODIFY_DAMAGE,
        BEFORE_DEFENSE,
        AFTER_DEFENSE,
        AFTER_HIT,
        END_TURN
    }

    private final Character attacker;
    private final Character defender;
    private final Weapon weapon;
    private final Random rng;

    private final Action attackerAction;
    private final Action defenderAction;

    // Resultat d'atac “base” (habilitat/arma)
    private AttackResult attackResult;

    // Dany mutable que es pot modificar per passius (abans de defensar)
    private double baseDamage;
    private double bonusFlatDamage;
    private double bonusMultiplier = 1.0;

    // Resultat real després de DEFEND/DODGE i aplicació
    private Result defenderResult;
    private double damageDealt;

    // Metadades flexibles
    private final Map<String, Object> meta = new HashMap<>();

    public HitContext(
            Character attacker,
            Character defender,
            Weapon weapon,
            Random rng,
            Action attackerAction,
            Action defenderAction) {
        this.attacker = attacker;
        this.defender = defender;
        this.weapon = weapon;
        this.rng = rng;
        this.attackerAction = attackerAction;
        this.defenderAction = defenderAction;
    }

    // ── Participants / info bàsica ───────────────────────────────

    public Character attacker() { return attacker; }
    public Character defender() { return defender; }
    public Weapon weapon() { return weapon; }
    public Random rng() { return rng; }

    public Action attackerAction() { return attackerAction; }
    public Action defenderAction() { return defenderAction; }

    // ── AttackResult base ────────────────────────────────────────

    public AttackResult attackResult() { return attackResult; }

    public void setAttackResult(AttackResult attackResult) {
        this.attackResult = attackResult;
        if (attackResult != null) {
            this.baseDamage = attackResult.damage();
        } else {
            this.baseDamage = 0;
        }
    }

    // ── Dany mutable (abans de defensar) ──────────────────────────

    /** Dany base abans de modificadors (normalment el dany de l'habilitat). */
    public double baseDamage() { return baseDamage; }

    /** Increment pla de dany (suma). */
    public void addFlatDamage(double amount) {
        if (amount > 0) bonusFlatDamage += amount;
    }

    /** Multiplicador acumulatiu (p.ex. 1.10 = +10%). */
    public void multiplyDamage(double mult) {
        if (mult > 0) bonusMultiplier *= mult;
    }

    /** Dany final que s'ha d'aplicar a la defensa (abans de DEFEND/DODGE). */
    public double damageToResolve() {
        double d = (baseDamage + bonusFlatDamage) * bonusMultiplier;
        return Math.max(0, d);
    }

    // ── Resultat real després de defensar ─────────────────────────

    public Result defenderResult() { return defenderResult; }
    public void setDefenderResult(Result defenderResult) { this.defenderResult = defenderResult; }

    /** Dany real aplicat (després de defensar). */
    public double damageDealt() { return damageDealt; }
    public void setDamageDealt(double damageDealt) { this.damageDealt = damageDealt; }

    // ── Helpers de target ────────────────────────────────────────

    /**
     * Retorna l'objectiu indicat per l'atac base.
     *
     * <p>Si no hi ha {@code attackResult}, assumeix {@link Target#ENEMY}.</p>
     */
    public Target target() {
        return (attackResult == null) ? Target.ENEMY : attackResult.target();
    }

    // ── Metadades ────────────────────────────────────────────────

    /**
     * Desa una metadada (p.ex. "CRIT"->true, "HITS"->2, "SKILL"->"chronoWeave").
     */
    public void putMeta(String key, Object value) {
        if (key != null) meta.put(key, value);
    }

    /** Obté una metadada o {@code null} si no existeix. */
    public Object getMeta(String key) {
        return meta.get(key);
    }

    /**
     * Obté una metadada amb tipus, o un valor per defecte si no és compatible.
     */
    public <T> T getMeta(String key, Class<T> type, T def) {
        Object v = meta.get(key);
        if (type.isInstance(v)) return type.cast(v);
        return def;
    }
}