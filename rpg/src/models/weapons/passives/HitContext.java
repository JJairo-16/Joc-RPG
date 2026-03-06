package models.weapons.passives;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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
 * Aquest context centralitza tota la informació i estat temporal d'un impacte:
 * participants, resultat base de l'atac, configuració de crític, modificadors
 * de dany, resultat defensiu i metadades flexibles.
 * </p>
 *
 * <p>
 * L'objectiu és desacoblar la lògica del combat de les passives i efectes,
 * permetent que aquests modifiquin el resultat del cop sense haver de tocar
 * el flux principal del sistema.
 * </p>
 */
public final class HitContext {

    /**
     * Fases on poden actuar les passives i efectes.
     */
    public enum Phase {
        START_TURN,
        BEFORE_ATTACK,
        ROLL_CRIT,
        MODIFY_DAMAGE,
        BEFORE_DEFENSE,
        AFTER_DEFENSE,
        AFTER_HIT,
        END_TURN
    }

    /**
     * Esdeveniments derivats del combat.
     *
     * <p>
     * A diferència de les fases, aquests no representen una posició fixa del
     * pipeline, sinó fets que han passat durant la resolució del cop.
     * </p>
     */
    public enum Event {
        ON_CRIT,
        ON_HIT,
        ON_DAMAGE_DEALT,
        ON_DAMAGE_TAKEN,
        ON_DODGE,
        ON_DEFEND,
        ON_KILL
    }

    private final Character attacker;
    private final Character defender;
    private final Weapon weapon;
    private final Random rng;

    private final Action attackerAction;
    private final Action defenderAction;

    // Resultat base de l'atac (habilitat/arma)
    private AttackResult attackResult;

    // Dany base reconstruït abans de crític/modificadors
    private double baseDamage;

    // Modificadors de dany
    private final List<Double> flatDamageModifiers = new ArrayList<>();
    private final List<Double> damageMultipliers = new ArrayList<>();

    // Configuració de crític
    private double criticalChance;
    private double criticalMultiplier = 1.0;
    private boolean criticalForced;
    private boolean criticalForbidden;
    private boolean criticalResolved;
    private boolean critical;

    // Resultat real després de DEFEND/DODGE i aplicació
    private Result defenderResult;
    private double damageDealt;

    // Metadades flexibles
    private final Map<String, Object> meta = new HashMap<>();

    // Esdeveniments registrats durant la resolució del cop
    private final EnumSet<Event> events = EnumSet.noneOf(Event.class);

    /**
     * Crea un nou context per a la resolució d'un atac.
     *
     * @param attacker atacant
     * @param defender defensor
     * @param weapon arma emprada
     * @param rng font d'aleatorietat
     * @param attackerAction acció triada per l'atacant
     * @param defenderAction acció triada pel defensor
     */
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

    /**
     * @return personatge atacant
     */
    public Character attacker() {
        return attacker;
    }

    /**
     * @return personatge defensor
     */
    public Character defender() {
        return defender;
    }

    /**
     * @return arma emprada en el cop, o {@code null} si no n'hi ha
     */
    public Weapon weapon() {
        return weapon;
    }

    /**
     * @return generador aleatori associat al cop
     */
    public Random rng() {
        return rng;
    }

    /**
     * @return acció triada per l'atacant
     */
    public Action attackerAction() {
        return attackerAction;
    }

    /**
     * @return acció triada pel defensor
     */
    public Action defenderAction() {
        return defenderAction;
    }

    // ── AttackResult base ────────────────────────────────────────

    /**
     * @return resultat base de l'atac generat per l'arma/habilitat
     */
    public AttackResult attackResult() {
        return attackResult;
    }

    /**
     * Desa el resultat base de l'atac.
     *
     * <p>
     * Aquest mètode no hauria de fixar per si sol el dany final a resoldre; per a
     * això es recomana cridar també {@link #setBaseDamage(double)} amb el dany
     * reconstruït previ al crític i als modificadors.
     * </p>
     *
     * @param attackResult resultat base de l'atac
     */
    public void setAttackResult(AttackResult attackResult) {
        this.attackResult = attackResult;
    }

    // ── Dany base / modificadors ─────────────────────────────────

    /**
     * Defineix el dany base del cop abans de crític i abans dels modificadors.
     *
     * @param baseDamage dany base
     */
    public void setBaseDamage(double baseDamage) {
        this.baseDamage = Math.max(0, baseDamage);
    }

    /**
     * @return dany base abans de crític i modificadors
     */
    public double baseDamage() {
        return baseDamage;
    }

    /**
     * Afegeix un modificador pla de dany.
     *
     * @param amount quantitat a sumar
     */
    public void addFlatDamage(double amount) {
        if (amount != 0) {
            flatDamageModifiers.add(amount);
        }
    }

    /**
     * Afegeix un multiplicador de dany.
     *
     * <p>
     * Exemples:
     * </p>
     * <ul>
     * <li>{@code 1.10} = +10%</li>
     * <li>{@code 0.80} = -20%</li>
     * </ul>
     *
     * @param mult multiplicador a aplicar
     */
    public void multiplyDamage(double mult) {
        if (mult > 0) {
            damageMultipliers.add(mult);
        }
    }

    /**
     * @return suma de tots els modificadors plans
     */
    public double flatDamageBonus() {
        double total = 0;
        for (double v : flatDamageModifiers) {
            total += v;
        }
        return total;
    }

    /**
     * @return producte de tots els multiplicadors de dany
     */
    public double damageMultiplier() {
        double total = 1.0;
        for (double v : damageMultipliers) {
            total *= v;
        }
        return total;
    }

    /**
     * Calcula el dany final que s'ha de resoldre contra la defensa.
     *
     * @return dany final no negatiu
     */
    public double damageToResolve() {
        double d = (baseDamage + flatDamageBonus()) * damageMultiplier();
        return Math.max(0, round2(d));
    }

    // ── Configuració de crític ───────────────────────────────────

    /**
     * Configura la probabilitat base de crític.
     *
     * @param chance probabilitat entre 0 i 1
     */
    public void setCriticalChance(double chance) {
        this.criticalChance = Math.clamp(chance, 0.0, 1.0);
    }

    /**
     * @return probabilitat actual de crític
     */
    public double criticalChance() {
        return criticalChance;
    }

    /**
     * Configura el multiplicador de crític.
     *
     * @param multiplier multiplicador de crític
     */
    public void setCriticalMultiplier(double multiplier) {
        this.criticalMultiplier = Math.max(1.0, multiplier);
    }

    /**
     * @return multiplicador actual de crític
     */
    public double criticalMultiplier() {
        return criticalMultiplier;
    }

    /**
     * Força que aquest atac sigui crític.
     *
     * <p>
     * Té prioritat sobre la probabilitat normal.
     * </p>
     */
    public void forceCritical() {
        this.criticalForced = true;
        this.criticalForbidden = false;
    }

    /**
     * Prohibeix que aquest atac sigui crític.
     *
     * <p>
     * Té prioritat sobre la probabilitat normal.
     * </p>
     */
    public void forbidCritical() {
        this.criticalForbidden = true;
        this.criticalForced = false;
    }

    /**
     * @return {@code true} si el crític s'ha forçat explícitament
     */
    public boolean isCriticalForced() {
        return criticalForced;
    }

    /**
     * @return {@code true} si el crític s'ha prohibit explícitament
     */
    public boolean isCriticalForbidden() {
        return criticalForbidden;
    }

    /**
     * Indica si el crític ja s'ha resolt per aquest cop.
     *
     * @return {@code true} si el crític ja està resolt
     */
    public boolean isCriticalResolved() {
        return criticalResolved;
    }

    /**
     * @return {@code true} si el cop resolt ha estat crític
     */
    public boolean wasCritical() {
        return critical;
    }

    /**
     * Resol el crític del cop i aplica el multiplicador sobre el dany base.
     *
     * <p>
     * Aquest mètode només s'hauria d'executar una vegada per atac.
     * </p>
     *
     * @return {@code true} si el cop ha quedat com a crític
     */
    public boolean resolveCritical() {
        if (criticalResolved) {
            return critical;
        }

        if (criticalForced) {
            critical = true;
        } else if (criticalForbidden) {
            critical = false;
        } else {
            critical = rng.nextDouble() < criticalChance;
        }

        if (critical) {
            baseDamage = round2(baseDamage * criticalMultiplier);
            registerEvent(Event.ON_CRIT);
        }

        criticalResolved = true;
        putMeta("CRIT", critical);

        return critical;
    }

    // ── Resultat real després de defensar ────────────────────────

    /**
     * @return resultat de la defensa aplicada
     */
    public Result defenderResult() {
        return defenderResult;
    }

    /**
     * Desa el resultat de la defensa aplicada.
     *
     * @param defenderResult resultat defensiu
     */
    public void setDefenderResult(Result defenderResult) {
        this.defenderResult = defenderResult;
    }

    /**
     * @return dany real infligit després de la defensa
     */
    public double damageDealt() {
        return damageDealt;
    }

    /**
     * Desa el dany real infligit després de la defensa.
     *
     * @param damageDealt dany final aplicat
     */
    public void setDamageDealt(double damageDealt) {
        this.damageDealt = Math.max(0, damageDealt);
    }

    // ── Helpers de target ────────────────────────────────────────

    /**
     * Retorna l'objectiu indicat per l'atac base.
     *
     * <p>
     * Si no hi ha {@code attackResult}, assumeix {@link Target#ENEMY}.
     * </p>
     *
     * @return objectiu base de l'atac
     */
    public Target target() {
        return (attackResult == null) ? Target.ENEMY : attackResult.target();
    }

    // ── Metadades ────────────────────────────────────────────────

    /**
     * Desa una metadada flexible.
     *
     * <p>
     * Exemples:
     * </p>
     * <ul>
     * <li>{@code "CRIT" -> true}</li>
     * <li>{@code "HITS" -> 2}</li>
     * <li>{@code "SKILL" -> "chronoWeave"}</li>
     * </ul>
     *
     * @param key clau
     * @param value valor
     */
    public void putMeta(String key, Object value) {
        if (key != null) {
            meta.put(key, value);
        }
    }

    /**
     * Obté una metadada.
     *
     * @param key clau
     * @return valor associat o {@code null}
     */
    public Object getMeta(String key) {
        return meta.get(key);
    }

    /**
     * Obté una metadada tipada o un valor per defecte.
     *
     * @param <T> tipus esperat
     * @param key clau
     * @param type classe del tipus esperat
     * @param def valor per defecte
     * @return valor convertit si és compatible; altrament {@code def}
     */
    public <T> T getMeta(String key, Class<T> type, T def) {
        Object v = meta.get(key);
        if (type.isInstance(v)) {
            return type.cast(v);
        }
        return def;
    }

    // ── Esdeveniments ────────────────────────────────────────────

    /**
     * Registra un esdeveniment dins el context del cop.
     *
     * @param event esdeveniment a marcar
     */
    public void registerEvent(Event event) {
        if (event != null) {
            events.add(event);
        }
    }

    /**
     * Indica si un esdeveniment s'ha produït durant la resolució del cop.
     *
     * @param event esdeveniment a consultar
     * @return {@code true} si està registrat
     */
    public boolean hasEvent(Event event) {
        return event != null && events.contains(event);
    }

    /**
     * Retorna una còpia immutable dels esdeveniments registrats.
     *
     * @return conjunt d'esdeveniments actuals
     */
    public Set<Event> events() {
        return events.isEmpty()
                ? EnumSet.noneOf(Event.class)
                : EnumSet.copyOf(events);
    }

    // ── Helpers interns ──────────────────────────────────────────

    /**
     * Arrodoneix a 2 decimals.
     *
     * @param n valor
     * @return valor arrodonit
     */
    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }
}