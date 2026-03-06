# Guia del pipeline de combat

Aquesta guia explica **l’ordre real de resolució d’un atac** dins del sistema de combat i com s’hi connecten les **passives**, els **effects** i el **HitContext**.

És la guia principal per entendre **què passa primer, què passa després i en quin moment has d’enganxar cada comportament**.

---

## 1. Objectiu d’aquesta guia

Quan una persona nova entra al projecte, la pregunta més important no és:

> “què és una passiva?”

sinó:

> “en quin moment exacte passa cada cosa?”

Aquesta guia respon això.

---

## 2. Visió general

Quan un personatge ataca, el sistema no aplica el dany immediatament.  
Primer construeix un **context del cop** i després passa per un **pipeline per fases**.

Ordre conceptual:

```text
AttackResult base
↓
crear HitContext
↓
START_TURN
↓
BEFORE_ATTACK
↓
ROLL_CRIT
↓
MODIFY_DAMAGE
↓
BEFORE_DEFENSE
↓
resoldre defensa / esquiva / dany
↓
AFTER_DEFENSE
↓
AFTER_HIT
↓
END_TURN
```

> `END_TURN` idealment s’hauria d’executar **una vegada per round**, no una vegada per cada atac individual.

---

## 3. Flux complet d’un atac

### 3.1 L’atacant genera un `AttackResult`
L’arma o skill produeix un resultat base:

- dany base
- missatge
- objectiu (`ENEMY` o `SELF`)

Exemple conceptual:

```java
AttackResult attackResult = attacker.attack();
```

Aquest resultat encara **no és necessàriament el dany final real**.

---

### 3.2 Es determina l’objectiu real
Segons `AttackResult.target()`:
- si és `ENEMY`, l’objectiu és el defensor
- si és `SELF`, l’objectiu és l’atacant

En atacs `SELF`, el pipeline habitual d’impacte contra enemic pot no aplicar-se igual.

---

### 3.3 Es crea el `HitContext`
El `HitContext` és l’objecte central del cop.

Conté:
- atacant
- defensor
- arma
- RNG
- acció de l’atacant
- acció del defensor
- dany base mutable
- configuració de crític
- modificadors de dany
- meta-dades
- esdeveniments del cop
- resultat defensiu
- dany real final

Exemple conceptual:

```java
HitContext ctx = new HitContext(attacker, defender, weapon, rng, attackerAction, defenderAction);
ctx.setAttackResult(attackResult);
```

---

## 4. Fases del pipeline

---

### 4.1 `START_TURN`

#### Què representa
L’inici del torn de qui està actuant.

#### Ús recomanat
- activacions que només han de passar quan el personatge entra en el seu torn
- efectes que preparen el cop
- sincronitzacions simples

#### No és ideal per
- aplicar dany on-hit
- aplicar verí
- reaccions a esquiva o bloqueig
- ticks de duració globals del round

#### Exemple
- “el pròxim atac d’aquest torn guanya +10 dany”
- “consumeix una càrrega d’un buff temporal a l’inici del torn”

---

### 4.2 `BEFORE_ATTACK`

#### Què representa
Fase primerenca abans de resoldre crític i abans de modificar fortament el dany.

#### Ús recomanat
- checks previs
- escriure meta-dades a `ctx`
- preparar informació contextual
- cancel·lacions suaus o condicionals

#### Exemples
- marcar que aquest atac és “projectile”
- marcar que aquest atac ve d’una skill concreta
- activar una condició si el defensor està per sota d’un llindar

Exemple conceptual:

```java
ctx.putMeta("ATTACK_TAG", "PROJECTILE");
```

---

### 4.3 `ROLL_CRIT`

#### Què representa
Fase dedicada a la resolució del crític.

#### Ús recomanat
- forçar crític
- prohibir crític
- pujar o baixar probabilitat de crític
- canviar multiplicador del crític
- preparar condicions “si aquest cop surt crític”

#### Exemples
- “el pròxim atac serà crític”
- “aquest objectiu no pot rebre crítics”
- “si tens benedicció, +25% crit chance”

Exemple conceptual:

```java
ctx.forceCritical();
```

o

```java
ctx.forbidCritical();
```

> Si una mecànica necessita memòria o duració, és millor que això ho faci un **Effect**, no una passiva puntual.

---

### 4.4 `MODIFY_DAMAGE`

#### Què representa
Fase principal per modificar el dany abans de defensar.

#### Ús recomanat
- sumar dany pla
- aplicar multiplicadors
- buffs ofensius
- debuffs ofensius
- executors
- bonificacions condicionals

#### Exemples
- “+15 de dany”
- “x1.20 si l’enemic està baix de vida”
- “x0.80 si l’atacant està debilitat”

Exemples típics:

```java
ctx.addFlatDamage(15);
```

```java
ctx.multiplyDamage(1.20);
```

---

### 4.5 `BEFORE_DEFENSE`

#### Què representa
Moment immediat anterior a resoldre `DEFEND`, `DODGE` o el dany directe.

#### Ús recomanat
- escuts
- mitigació defensiva
- reduccions situacionals
- efectes que reaccionen a “estic a punt de rebre un cop”

#### Exemples
- escut per càrregues
- reducció del 30% del primer cop rebut
- barrera màgica que baixa dany màgic

Exemple conceptual:

```java
ctx.multiplyDamage(0.70);
```

> Aquesta fase acostuma a ser millor per a **effects defensius** que viuen al defensor.

---

### 4.6 Resolució de defensa i aplicació del dany

Després de les fases anteriors, el sistema calcula:

```java
double damageToResolve = ctx.damageToResolve();
```

I resol la defensa:

- `DODGE`
- `DEFEND`
- rebre dany directament

Exemple conceptual:

```java
Result defenderResult = resolveAttack(damageToResolve, defender, defenderAction);
ctx.setDefenderResult(defenderResult);
ctx.setDamageDealt(defenderResult.recivied());
```

Aquest és el moment en què apareix el **dany real final**.

---

### 4.7 `AFTER_DEFENSE`

#### Què representa
Fase posterior a la resolució defensiva.

#### Què ja tens disponible aquí
- `ctx.defenderResult()`
- `ctx.damageDealt()`
- el resultat de si s’ha esquivat, defensat o rebut dany

#### Ús recomanat
- reaccions a defensa
- reaccions a esquiva
- detectar si el cop ha estat parcialment mitigat
- escriure meta-dades finals

#### Exemples
- “si l’enemic esquiva, guanyo velocitat”
- “si defensa, li aplico fatiga”
- “si el cop entra parcialment, activa una runa”

---

### 4.8 `AFTER_HIT`

#### Què representa
Fase posterior a un impacte real.

#### Condició típica
Només hauria d’executar-se si:

```java
ctx.damageDealt() > 0
```

#### Ús recomanat
- life steal
- dany extra on-hit
- aplicar verí
- aplicar cremades
- aplicar buffs al connectar
- reaccions a crític ja resolt
- reaccions a kill

#### És la millor fase per
- passives “on-hit”
- passives que apliquen effects

Exemple conceptual:

```java
ctx.defender().addEffect(new PoisonEffect(3, 5));
```

---

### 4.9 `END_TURN`

#### Què representa
Final del torn o final del round, segons la teva arquitectura.

#### Recomanació forta
Per efectes persistents, el millor és que `END_TURN` s’executi **una vegada per round per personatge**, no una vegada per atac.

#### Ús recomanat
- verí
- cremades
- regeneracions per torn
- reducció de duració
- tick de cooldown

#### Exemples
- “rep 5 de dany de verí”
- “baixa en 1 la duració de la fúria”
- “recupera 3 mana per torn”

---

## 5. Esdeveniments derivats del cop

A més de les fases, el sistema pot registrar **events** dins el `HitContext`.

Exemples:
- `ON_CRIT`
- `ON_HIT`
- `ON_DAMAGE_DEALT`
- `ON_DAMAGE_TAKEN`
- `ON_DODGE`
- `ON_DEFEND`
- `ON_KILL`

Això serveix perquè passives o effects reaccionin sense haver d’endevinar-ho manualment.

Exemple conceptual:

```java
if (ctx.hasEvent(HitContext.Event.ON_CRIT)) {
    // reacció a crític
}
```

---

## 6. Ordre recomanat dins `CombatSystem`

Per un atac normal:

```text
1. Crear AttackResult
2. Determinar target real
3. Crear HitContext
4. START_TURN de l’atacant
5. BEFORE_ATTACK (atacant, defensor, arma)
6. ROLL_CRIT (atacant, defensor, arma)
7. Resoldre crític
8. MODIFY_DAMAGE (atacant, defensor, arma)
9. BEFORE_DEFENSE (atacant, defensor, arma)
10. Resoldre defensa i dany real
11. Registrar events
12. AFTER_DEFENSE (atacant, defensor, arma)
13. AFTER_HIT si hi ha dany real
14. END_TURN al final del round
```

---

## 7. Com decidir on posar una mecànica

Pregunta’t això:

### “Aquesta mecànica modifica el cop abans d’entrar?”
- usa `MODIFY_DAMAGE`

### “Aquesta mecànica altera crítics?”
- usa `ROLL_CRIT`

### “Aquesta mecànica reacciona a rebre el cop?”
- usa `BEFORE_DEFENSE` o `AFTER_DEFENSE`

### “Aquesta mecànica només passa si connecta?”
- usa `AFTER_HIT`

### “Aquesta mecànica dura torns?”
- fes-la com a `Effect`, normalment amb `END_TURN`

---

## 8. Errors habituals d’arquitectura

### Error 1
Aplicar verí a `MODIFY_DAMAGE`

### Problema
Encara no saps si el cop ha connectat de veritat.

### Correcte
Aplicar-lo a `AFTER_HIT`.

---

### Error 2
Fer ticks de verí dins `playPlayerTurn(...)`

### Problema
Si hi ha dos atacs per round, pots tickejar dues vegades.

### Correcte
Fer-ho a `END_TURN` una vegada per round.

---

### Error 3
Posar dins la passiva lògica que dura diversos torns

### Problema
Converteixes la passiva en un pseudo-effect.

### Correcte
La passiva aplica l’effect; l’effect fa la resta.

---

## 9. Exemple mental complet

### Mecànica
“Espasa corrupta:
- si connecta, 30% d’aplicar verí
- si el cop és crític, el verí fa més mal
- si mata, l’atacant guanya fúria”

### Solució recomanada
- passiva en `AFTER_HIT`
- comprova `ctx.damageDealt() > 0`
- consulta `ctx.hasEvent(ON_CRIT)`
- aplica `PoisonEffect(...)` amb més o menys potència
- si `ctx.hasEvent(ON_KILL)`, aplica `RageEffect(...)` a l’atacant

La passiva només decideix **què aplicar**.  
Els effects decideixen **què passa després**.

---

## 10. Regla final

**El pipeline marca el moment.  
La passiva o l’effect hi enganxen la lògica correcta.**