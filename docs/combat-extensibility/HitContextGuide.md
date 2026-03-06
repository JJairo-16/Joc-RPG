# Guia completa de `HitContext`

Aquesta guia explica què és `HitContext`, quina informació guarda i com s’ha d’utilitzar per crear passives i effects de manera segura i flexible.

`HitContext` és el **centre del sistema de resolució d’un cop**.

---

## 1. Què és `HitContext`?

`HitContext` és un objecte mutable que representa **un cop concret** durant la seva resolució.

Conté:
- qui ataca
- qui defensa
- amb quina arma
- quines accions han triat
- quin és el dany base
- com es modifica aquest dany
- si hi ha crític
- què ha passat després de defensar
- meta-dades addicionals
- events registrats durant el cop

És, en essència, la “fitxa viva” del cop.

---

## 2. Per què existeix?

Sense `HitContext`, cada passiva o effect hauria de:
- recalcular dades pel seu compte
- consultar massa coses externes
- duplicar lògica
- fer side-effects difícils de seguir

Amb `HitContext`, totes les peces treballen sobre el mateix objecte.

Això permet:
- pipeline clar
- extensibilitat
- reaccions desacoblades
- menys codi trencadís

---

## 3. Quina informació acostuma a tenir?

---

### 3.1 Participants

- `ctx.attacker()`
- `ctx.defender()`
- `ctx.weapon()`
- `ctx.rng()`

Serveix per accedir als protagonistes del cop.

Exemple:

```java
Character attacker = ctx.attacker();
Character defender = ctx.defender();
```

---

### 3.2 Accions triades

- `ctx.attackerAction()`
- `ctx.defenderAction()`

Serveix per saber si el defensor està:
- atacant
- defensant
- esquivant

Exemple:

```java
if (ctx.defenderAction() == Action.DEFEND) {
    // lògica específica
}
```

---

### 3.3 Resultat base de l’atac

- `ctx.attackResult()`

Guarda el resultat inicial produït per arma o skill.

Pot incloure:
- dany inicial
- missatge
- target

---

### 3.4 Dany base mutable

- `ctx.setBaseDamage(double)`
- `ctx.baseDamage()`

Això és el **dany abans de modificadors finals**.

Important:
- és el punt de partida per al càlcul
- no és necessàriament el dany final real

---

### 3.5 Modificadors de dany

- `ctx.addFlatDamage(double)`
- `ctx.multiplyDamage(double)`
- `ctx.damageToResolve()`

Aquestes són les eines principals per canviar el cop.

#### `addFlatDamage`
Afegeix una quantitat plana.

Exemple:

```java
ctx.addFlatDamage(12);
```

#### `multiplyDamage`
Aplica un multiplicador.

Exemple:

```java
ctx.multiplyDamage(1.25);
```

#### `damageToResolve`
Calcula el resultat actual del dany.

Exemple:

```java
double finalDamage = ctx.damageToResolve();
```

---

## 4. Sistema de crítics

Si el teu `HitContext` ja suporta crítics de forma flexible, les operacions típiques són:

- `ctx.setCriticalChance(...)`
- `ctx.criticalChance()`
- `ctx.setCriticalMultiplier(...)`
- `ctx.criticalMultiplier()`
- `ctx.forceCritical()`
- `ctx.forbidCritical()`
- `ctx.resolveCritical()`
- `ctx.wasCritical()`

---

### 4.1 Quan s’han d’usar?
Normalment a la fase `ROLL_CRIT`.

### 4.2 Exemples típics

#### Forçar crític
```java
ctx.forceCritical();
```

#### Prohibir crític
```java
ctx.forbidCritical();
```

#### Pujar la probabilitat
```java
ctx.setCriticalChance(ctx.criticalChance() + 0.20);
```

#### Canviar multiplicador
```java
ctx.setCriticalMultiplier(2.0);
```

> Si el comportament dura més d’un cop, és millor modelar-lo com a `Effect`.

---

## 5. Resultat defensiu i dany real

Després de resoldre la defensa, `HitContext` acostuma a guardar:

- `ctx.defenderResult()`
- `ctx.damageDealt()`

Això és molt important.

### `damageToResolve()`
És el dany que **s’intenta aplicar** abans de la defensa.

### `damageDealt()`
És el dany que **realment ha entrat** després de defensa/esquiva.

Exemple:

```java
if (ctx.damageDealt() > 0) {
    // hi ha hit real
}
```

Aquesta diferència és clau per no aplicar on-hit quan no toca.

---

## 6. Meta-dades (`meta`)

El `HitContext` acostuma a tenir un mapa flexible de meta-dades.

Operacions típiques:
- `ctx.putMeta(key, value)`
- `ctx.getMeta(key)`
- `ctx.getMeta(key, Class<T>, defaultValue)`

Això serveix per passar informació especial sense rigiditzar l’API.

---

### 6.1 Exemples útils de meta-dades

- `"CRIT"` → `Boolean`
- `"RAW_DAMAGE"` → `Double`
- `"SKILL"` → `String`
- `"ATTACK_TAG"` → `String`
- `"HITS"` → `Integer`

Exemple:

```java
ctx.putMeta("SKILL", "chronoWeave");
```

I després:

```java
String skill = ctx.getMeta("SKILL", String.class, "");
if ("chronoWeave".equals(skill)) {
    // lògica específica
}
```

---

### 6.2 Quan usar meta i quan no?

### Usa `meta` per:
- informació auxiliar
- tags
- dades temporals específiques d’una mecànica
- experiments o extensió lleugera

### No l’usis per:
- coses centrals que haurien de ser camps de primer nivell
- substituir estructures clares del sistema
- guardar estat persistent entre torns

Per estat persistent, millor un `Effect`.

---

## 7. Events del cop

A més de meta-dades, `HitContext` pot registrar events.

Operacions típiques:
- `ctx.registerEvent(...)`
- `ctx.hasEvent(...)`
- `ctx.events()`

Exemples d’events:
- `ON_CRIT`
- `ON_HIT`
- `ON_DAMAGE_DEALT`
- `ON_DAMAGE_TAKEN`
- `ON_DODGE`
- `ON_DEFEND`
- `ON_KILL`

---

### 7.1 Per què són útils?

Sense events, moltes passives haurien de re-deduïr coses manualment.

Amb events pots fer:

```java
if (ctx.hasEvent(HitContext.Event.ON_KILL)) {
    ctx.attacker().addEffect(new RageEffect(2, 1.20));
}
```

Això és molt més net.

---

## 8. Què ha de modificar una passiva i què no?

### Pot modificar
- dany base o final via API del context
- flags de crític
- meta-dades
- aplicació d’efectes
- reaccions condicionals

### No hauria de modificar directament
- duracions persistents que pertanyen a un `Effect`
- estructures internes alienes al sistema
- dades “globals” del combat sense control

---

## 9. Patrons d’ús típics

---

### 9.1 Bonus de dany condicional

```java
if (ctx.defender().geStatistics().getHealth() < 50) {
    ctx.multiplyDamage(1.25);
}
```

---

### 9.2 Aplicar efecte només si hi ha hit real

```java
if (ctx.damageDealt() > 0) {
    ctx.defender().addEffect(new PoisonEffect(3, 5));
}
```

---

### 9.3 Reaccionar a crític

```java
if (ctx.hasEvent(HitContext.Event.ON_CRIT)) {
    ctx.addFlatDamage(10);
}
```

O bé, si el teu sistema ho modela així:

```java
if (ctx.getMeta("CRIT", Boolean.class, false)) {
    ctx.addFlatDamage(10);
}
```

---

### 9.4 Marcar un tag d’atac

```java
ctx.putMeta("ATTACK_TAG", "FIRE");
```

Després una altra peça podria fer:

```java
if ("FIRE".equals(ctx.getMeta("ATTACK_TAG", String.class, ""))) {
    // lògica de foc
}
```

---

## 10. Diferència entre `meta` i `events`

### `meta`
Serveix per guardar **dades**.

Exemple:
- tipus d’atac
- nom de skill
- nombre de hits

### `events`
Serveix per marcar **fets que han passat**.

Exemple:
- ha estat crític
- ha matat
- s’ha esquivat

Regla pràctica:
- si és una **dada**, usa `meta`
- si és un **fet**, usa `events`

---

## 11. Errors habituals

### Error 1
Usar `damageToResolve()` per decidir on-hit

### Problema
Encara no saps si ha entrat el dany real.

### Correcte
Usa `damageDealt()`.

---

### Error 2
Guardar estat de diversos torns dins `meta`

### Problema
`HitContext` és per a **un cop concret**, no per persistència llarga.

### Correcte
Usa un `Effect`.

---

### Error 3
Fer massa coses directes sobre `Character` sense passar pel model del context

### Problema
Perds traçabilitat del pipeline.

### Correcte
Usa el context sempre que sigui possible i deixa els side-effects persistents als `Effect`.

---

## 12. Checklist ràpid

Quan creïs una passiva o effect, pregunta’t:

1) necessito saber qui ataca o defensa?
2) necessito modificar el dany?
3) necessito saber si hi ha hit real?
4) necessito saber si ha estat crític?
5) necessito guardar una dada temporal?
6) necessito reaccionar a un event?
7) necessito persistència entre torns?

Si la resposta a 7 és sí, probablement necessites un `Effect`.

---

## 13. Regla final

**`HitContext` és el lloc on es resol el cop.  
Els `Effect` són el lloc on viu l’estat persistent.**