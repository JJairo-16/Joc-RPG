# Guia completa d’Efectes (buffs/debuffs) per fases

Aquesta guia explica com construir un sistema d’**efectes persistents** (verí, escuts, fúria…) amb el mateix concepte de **fases** que les passives, però vivint al **personatge**.

---

## 1. Què és un “Effect”?

Un **Effect** és un objecte que:
- viu en un personatge (atacant o defensor)
- té estat (duració, càrregues, stacks, cooldown…)
- s’executa en una o més fases del torn

Exemples:
- **Verí**: fa dany a `END_TURN` durant X torns
- **Escut**: redueix dany a `BEFORE_DEFENSE` consumint càrregues
- **Fúria**: augmenta dany a `MODIFY_DAMAGE` durant X torns
- **Benedicció crítica**: força o millora el crític durant un nombre limitat d’atacs o torns

> Idea clau: un efecte **no és un proc puntual**, sinó una peça de comportament amb **estat propi**.

---

## 2. Peces del sistema

### 2.1 `Effect`
Interfície principal. Defineix:
- `key()` identificador (per apilar/refresh)
- `priority()` (ordre)
- `stackingRule()` (com s’aplica si ja existeix)
- `state()` (estat mutable)
- callbacks per fase (`modifyDamage`, `beforeDefense`, `endTurn`, etc.)

### 2.2 `EffectState`
Estat mutable:
- `charges` (càrregues)
- `stacks` (apilaments)
- `remainingTurns` (duració)
- `cooldownTurns` (cooldown)

### 2.3 `EffectResult`
Resultat de l’execució d’una fase:
- missatge opcional
- flags (si s’ha consumit càrrega / si s’ha canviat estat)

### 2.4 `StackingRule`
Com es comporta quan reapliques un efecte amb la mateixa `key()`:
- `IGNORE`
- `REPLACE`
- `REFRESH`
- `STACK`

---

## 3. Responsabilitat d’un efecte

Un efecte hauria de ser responsable de:
- mantenir el seu **estat**
- decidir **quan actua**
- aplicar la seva **lògica persistent o reactiva**
- saber **quan expira**

Un efecte **no** hauria de dependre que la passiva li torni a fer la lògica cada cop.

### Patró correcte
- la passiva diu: “aplico `PoisonEffect`”
- `PoisonEffect` diu: “faig dany a `END_TURN` durant 3 torns”

### Patró incorrecte
- la passiva diu: “marca enverinat”
- una altra part del codi diu: “si està enverinat, potser faig dany”
- una tercera part diu: “li baixo torns”

Això dispersa la responsabilitat i fa el sistema difícil de mantenir.

---

## 4. Com es creen “efectes fills”

Un “fill” és una classe normal que fa:

```java
public class PoisonEffect implements Effect { ... }
```

**Checklist mínim:**
1) `key()`
2) `state()`
3) sobrescriure una o més fases
4) `isExpired()` (quan s’ha d’eliminar)

Opcional:
- `priority()`
- `stackingRule()`
- `mergeFrom(Effect incoming)`

---

## 5. El contenidor d’efectes a `Character` (implementació necessària)

Perquè els efectes funcionin, `Character` ha de tenir:

- `List<Effect> effects`
- `addEffect(Effect incoming)` aplicant `StackingRule`
- `hasEffect(String key)` si vols facilitar checks ràpids
- `getEffect(String key)` si vols inspecció o lògica avançada
- `triggerEffects(HitContext ctx, Phase phase, Random rng)`
- neteja d’expirats

### 5.1 `addEffect(...)` (Java 8+ compatible, sense yield)

Comportament recomanat:
- si ja existeix la mateixa `key()`:
  - `IGNORE` → no fer res
  - `REPLACE` → substituir
  - `REFRESH/STACK` → `existing.mergeFrom(incoming)`
- si no existeix → afegir

Després, ordenar per `priority()` descendent perquè sigui consistent.

### 5.2 `hasEffect(...)`

És molt útil per passives que fan:
- “aplica l’efecte si no el té”
- “si ja el té, no reapliquis”
- “si ja el té, reforça’l amb `STACK` o `REFRESH`”

Exemple conceptual:

```java
if (!ctx.defender().hasEffect(PoisonEffect.KEY)) {
    ctx.defender().addEffect(new PoisonEffect(3, 5));
}
```

> Tot i això, si el teu sistema de `StackingRule` ja està ben fet, moltes vegades la passiva pot simplement fer `addEffect(...)` i deixar que el contenidor decideixi si ignora, refresca, apila o substitueix.

---

## 6. Quan s’executen els efectes?

Recomanació clara (simple i potent):

- **Efectes ofensius** (buffs del dany): `MODIFY_DAMAGE` sobre l’**atacant**
- **Efectes defensius** (escuts, reduccions): `BEFORE_DEFENSE` sobre el **defensor**
- **Ticks per torn** (DoT com verí): `END_TURN` (idealment 1 cop per round)
- **Reaccions post-impacte**: `AFTER_HIT`
- **Manipulació de crítics**: si el pipeline ho permet, `ROLL_CRIT`

> IMPORTANT: `END_TURN` s’hauria d’executar **una vegada per round**, no per cada atac.

---

## 7. Exemple complet: Verí (Poison)

### 7.1 Disseny
- Duració: 3 torns
- Dany: 5 per torn
- L’efecte viu al defensor

### 7.2 Implementació típica
- `endTurn(...)`: aplica dany i després `tickDuration()`
- `isExpired()`: quan `remainingTurns <= 0`

### 7.3 Exemple simplificat

```java
public final class PoisonEffect implements Effect {
    public static final String KEY = "POISON";

    private final EffectState state;
    private final double damagePerTurn;

    public PoisonEffect(int turns, double damagePerTurn) {
        this.state = EffectState.ofDuration(turns);
        this.damagePerTurn = damagePerTurn;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public EffectState state() {
        return state;
    }

    @Override
    public EffectResult endTurn(HitContext ctx, Random rng) {
        ctx.attacker().getDamage(damagePerTurn);
        state.tickDuration();
        return EffectResult.msg("-el verí fa mal.");
    }

    @Override
    public boolean isExpired() {
        return state.remainingTurns() <= 0;
    }
}
```

**Nota de modelatge:** el context concret dependrà de com invoquis `triggerEffects(...)`. El més important és que el verí s’executi sobre el **propietari de l’efecte**.

---

## 8. Exemple complet: Escut amb càrregues

### 8.1 Disseny
- 3 càrregues
- Redueix dany entrant un 30%
- Consumeix 1 càrrega quan hi ha dany a resoldre

### 8.2 Fase
- `beforeDefense(...)` és el lloc ideal.

### 8.3 Exemple simplificat

```java
public final class ShieldEffect implements Effect {
    public static final String KEY = "SHIELD";

    private final EffectState state;
    private final double reductionPct;

    public ShieldEffect(int charges, double reductionPct) {
        this.state = EffectState.ofCharges(charges);
        this.reductionPct = reductionPct;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public EffectState state() {
        return state;
    }

    @Override
    public EffectResult beforeDefense(HitContext ctx, Random rng) {
        if (!state.hasCharges()) return EffectResult.none();
        if (ctx.damageToResolve() <= 0) return EffectResult.none();

        ctx.multiplyDamage(1.0 - reductionPct);
        state.consumeCharge();

        return EffectResult.consumed("l’escut absorbeix part del cop.");
    }

    @Override
    public boolean isExpired() {
        return state.charges() <= 0;
    }
}
```

---

## 9. Exemple complet: Fúria (buff de dany per duració)

### 9.1 Disseny
- 2 torns
- x1.20 dany en `MODIFY_DAMAGE`

### 9.2 Fase
- `modifyDamage(...)`

### 9.3 Exemple simplificat

```java
public final class RageEffect implements Effect {
    public static final String KEY = "RAGE";

    private final EffectState state;
    private final double multiplier;

    public RageEffect(int turns, double multiplier) {
        this.state = EffectState.ofDuration(turns);
        this.multiplier = multiplier;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public EffectState state() {
        return state;
    }

    @Override
    public EffectResult modifyDamage(HitContext ctx, Random rng) {
        ctx.multiplyDamage(multiplier);
        return EffectResult.msg("la fúria augmenta el dany.");
    }

    @Override
    public EffectResult endTurn(HitContext ctx, Random rng) {
        state.tickDuration();
        return EffectResult.none();
    }

    @Override
    public boolean isExpired() {
        return state.remainingTurns() <= 0;
    }
}
```

---

## 10. Exemple complet: Efecte de crític garantit

### 10.1 Disseny
- dura 1 atac o 1 torn
- força crític
- després expira o consumeix càrrega

### 10.2 Modelatge
Aquest tipus d’efecte és ideal quan vols que una passiva o skill no modifiqui el crític directament, sinó que **apliqui un estat** que després el pipeline consumirà.

### 10.3 Exemple conceptual

```java
public final class GuaranteedCritEffect implements Effect {
    public static final String KEY = "GUARANTEED_CRIT";

    private final EffectState state;

    public GuaranteedCritEffect(int charges) {
        this.state = EffectState.ofCharges(charges);
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public EffectState state() {
        return state;
    }

    @Override
    public EffectResult rollCrit(HitContext ctx, Random rng) {
        if (!state.hasCharges()) return EffectResult.none();

        ctx.forceCritical();
        state.consumeCharge();

        return EffectResult.consumed("un poder misteriós força el crític.");
    }

    @Override
    public boolean isExpired() {
        return state.charges() <= 0;
    }
}
```

---

## 11. Com fa una passiva per aplicar un efecte?

El patró recomanat és:

- passiva en `AFTER_HIT`
- condició: `ctx.damageDealt() > 0`
- aplicar l’efecte al defensor o a l’atacant
- deixar que l’efecte faci tota la seva feina després

Exemple:

```java
ctx.defender().addEffect(new PoisonEffect(3, 5));
```

**Per què `AFTER_HIT`?**  
Perquè així no apliques l’efecte si l’enemic esquiva o bloqueja a 0.

---

## 12. Integració a `CombatSystem` (ordre recomanat)

Per a un atac (atacant → defensor):

1) Crear `HitContext`
2) `attacker.triggerEffects(START_TURN, ...)`
3) `attacker/defender/weapon` en `BEFORE_ATTACK`
4) `attacker/defender/weapon` en `ROLL_CRIT` (si existeix)
5) resoldre crític
6) `attacker/defender/weapon` en `MODIFY_DAMAGE`
7) `attacker/defender/weapon` en `BEFORE_DEFENSE`
8) resoldre defensa (`DEFEND/DODGE`) i set `damageDealt`
9) `attacker/defender/weapon` en `AFTER_DEFENSE`
10) si hi ha hit real: `AFTER_HIT`

Al final del round (`play(...)`):
- `player1.triggerEffects(END_TURN, ...)`
- `player2.triggerEffects(END_TURN, ...)`

> Això fa que els DoT s’apliquin una vegada per round.

---

## 13. Bones pràctiques

- Usa `priority()` per tenir ordre determinista.
- Limita max stacks/càrregues per evitar escalats infinits.
- No facis `END_TURN` dins `playPlayerTurn(...)` si hi ha dos atacs per round.
- Decideix una convenció clara de “propietari de l’efecte”.
- Si un comportament necessita **estat**, normalment hauria de ser un `Effect`.
- Si un comportament és només un **proc puntual sense estat**, normalment pot ser una passiva simple.

---

## 14. Checklist ràpid per crear un efecte nou

1) Decideix si és:
   - per duració
   - per càrregues
   - per stacks
2) Defineix `key()` i `stackingRule()`.
3) Implementa la fase correcta:
   - DoT: `END_TURN`
   - Escut: `BEFORE_DEFENSE`
   - Buff dany: `MODIFY_DAMAGE`
   - Crític garantit o prohibit: `ROLL_CRIT`
4) Implementa `isExpired()`.
5) (Opcional) `mergeFrom(...)` per stacking/refresh.
6) Aplica’l via `character.addEffect(...)` des d’una passiva o skill.

---

## 15. Regla d’or

**La passiva aplica l’efecte.  
L’efecte implementa la lògica.**

Si una passiva necessita preguntar “té ja aquest efecte?”, està bé.  
Però la passiva no hauria de duplicar la lògica interna de l’efecte.