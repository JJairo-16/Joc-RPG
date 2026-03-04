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

## 3. Com es creen “efectes fills” (sense herència)

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

## 4. El contenidor d’efectes a `Character` (implementació necessària)

Perquè els efectes funcionin, `Character` ha de tenir:

- `List<Effect> effects`
- `addEffect(Effect incoming)` aplicant `StackingRule`
- `triggerEffects(HitContext ctx, Phase phase, Random rng)`
- neteja d’expirats

### 4.1 `addEffect(...)` (Java 8+ compatible, sense yield)

Comportament recomanat:
- si ja existeix la mateixa `key()`:
  - `IGNORE` → no fer res
  - `REPLACE` → substituir
  - `REFRESH/STACK` → `existing.mergeFrom(incoming)`
- si no existeix → afegir

Després, ordenar per `priority()` descendent perquè sigui consistent.

---

## 5. Quan s’executen els efectes?

Recomanació clara (simple i potent):

- **Efectes ofensius** (buffs del dany): `MODIFY_DAMAGE` sobre l’**atacant**
- **Efectes defensius** (escuts, reduccions): `BEFORE_DEFENSE` sobre el **defensor**
- **Ticks per torn** (DoT com verí): `END_TURN` (idealment 1 cop per round)

> IMPORTANT: `END_TURN` s’hauria d’executar **una vegada per round**, no per cada atac.

---

## 6. Exemple complet: Verí (Poison)

### 6.1 Disseny
- Duració: 3 torns
- Dany: 5 per torn (o escalable)
- Apilat: opcional

### 6.2 Implementació típica
- `endTurn(...)`: aplica dany i després `tickDuration()`
- `isExpired()`: quan `remainingTurns <= 0`

**Notes de modelatge:**
- El verí ha d’aplicar-se al **personatge que el porta**.
- El contenidor ha d’executar el verí quan correspongui (fase `END_TURN`).

---

## 7. Exemple complet: Escut amb càrregues

### 7.1 Disseny
- 3 càrregues
- Redueix dany entrant un 30%
- Consumeix 1 càrrega quan hi ha dany a resoldre

### 7.2 Fase
- `beforeDefense(...)` és el lloc ideal.

Aquest escut no necessita duració, només càrregues.

---

## 8. Exemple complet: Fúria (buff de dany per duració)

### 8.1 Disseny
- 2 torns
- x1.20 dany en `MODIFY_DAMAGE`

### 8.2 Fase
- `modifyDamage(...)`

A `endTurn(...)` tiqueja duració i expira quan `remainingTurns == 0`.

---

## 9. Com fa una passiva per aplicar un efecte?

El patró és:

- passiva en `AFTER_HIT`
- condició: `ctx.damageDealt() > 0`
- aplicar l’efecte al defensor: `ctx.defender().addEffect(new PoisonEffect(...))`

**Per què AFTER_HIT?**  
Perquè així no enverines si l’enemic esquiva o bloqueja a 0.

---

## 10. Integració a `CombatSystem` (ordre recomanat)

Per a un atac (atacant → defensor):

1) Crear `HitContext`
2) `attacker.triggerEffects(MODIFY_DAMAGE, ...)`
3) `weapon.triggerPhase(MODIFY_DAMAGE, ...)`
4) `defender.triggerEffects(BEFORE_DEFENSE, ...)`
5) `weapon.triggerPhase(BEFORE_DEFENSE, ...)`
6) Resoldre defensa (`DEFEND/DODGE`) i set `damageDealt`
7) `weapon.triggerPhase(AFTER_HIT, ...)` (aplica efectes com verí)
8) (Opcional) `attacker/defender.triggerEffects(AFTER_HIT, ...)`

Al final del round (`play(...)`):
- `player1.triggerEffects(END_TURN, ...)`
- `player2.triggerEffects(END_TURN, ...)`

> Això fa que els DoT (verí) s’apliquin una vegada per round.

---

## 11. Bones pràctiques

- Usa `priority()` per tenir ordre determinista.
- Limita max stacks/càrregues per evitar escalats infinits.
- No facis `END_TURN` dins `playPlayerTurn(...)` si hi ha dos atacs per round.
- Decideix una convenció de “qui és el propietari” de l’efecte i executa’l sempre des d’aquell personatge.

---

## 12. Checklist ràpid per crear un efecte nou

1) Decideix si és:
   - per duració (turns)
   - per càrregues
   - per stacks
2) Defineix `key()` i `stackingRule()`.
3) Implementa la fase correcta:
   - DoT: `END_TURN`
   - Escut: `BEFORE_DEFENSE`
   - Buff dany: `MODIFY_DAMAGE`
4) Implementa `isExpired()`.
5) (Opcional) `mergeFrom(...)` per stacking/refresh.
6) Afegeix-lo via `character.addEffect(...)` des d’una passiva o skill.

---

Amb això tens un sistema d’efectes modular, extensible i coherent amb el pipeline de passives.
