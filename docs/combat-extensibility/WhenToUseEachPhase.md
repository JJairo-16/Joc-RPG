# Quan utilitzar cada fase

Aquesta guia és una referència ràpida per decidir **en quina fase del pipeline** has de posar una passiva o la lògica d’un effect.

Si dubtes entre dues fases, aquesta és la guia que has de mirar primer.

---

## 1. Taula ràpida

| Objectiu | Fase recomanada | Comentari |
|---|---|---|
| Preparar dades del cop | `BEFORE_ATTACK` | ideal per `meta`, tags i checks |
| Forçar / prohibir crític | `ROLL_CRIT` | és la fase natural del crític |
| Canviar probabilitat o multiplicador de crític | `ROLL_CRIT` | millor aquí que dispersat |
| Sumar dany pla | `MODIFY_DAMAGE` | abans de defensar |
| Aplicar multiplicador de dany | `MODIFY_DAMAGE` | bonus o penalització ofensiva |
| Mitigar dany entrant | `BEFORE_DEFENSE` | escuts i barreres |
| Reaccionar a esquiva o defensa | `AFTER_DEFENSE` | aquí ja coneixes el resultat |
| Robar vida | `AFTER_HIT` | només si hi ha dany real |
| Aplicar verí / burn / bleed | `AFTER_HIT` | evita aplicar-lo si no connecta |
| Aplicar un buff a l’atacant després d’impactar | `AFTER_HIT` | patró molt habitual |
| Reaccionar a un crític ja resolt | `AFTER_HIT` o `AFTER_DEFENSE` | depèn de si vols hit real o no |
| Tick de verí / burn | `END_TURN` | idealment 1 cop per round |
| Baixar duració o cooldown | `END_TURN` | millor al tancament del round |
| Activació a l’inici del torn | `START_TURN` | útil per preparació |

---

## 2. Explicació fase per fase

---

### `START_TURN`

### Usa-la quan:
- el comportament passa a l’inici del torn del personatge
- vols preparar un estat abans del cop
- vols consumir una càrrega “en començar el torn”

### Bons casos
- “al començar el torn, guanya +10 dany”
- “al començar el torn, neteja una marca”
- “al començar el torn, activa una runa”

### No és la millor per
- on-hit
- verí
- reaccions a defensa
- dany que depèn de si el cop entra

---

### `BEFORE_ATTACK`

### Usa-la quan:
- necessites preparar informació
- vols llegir condicions del combat abans d’altres modificadors
- vols afegir tags o meta-dades

### Bons casos
- marcar tipus d’atac
- detectar si l’objectiu està baix de vida
- preparar condicions per altres sistemes

### Exemple conceptual

```java
ctx.putMeta("ATTACK_TAG", "BLEED");
```

---

### `ROLL_CRIT`

### Usa-la quan:
- la mecànica toca el crític directament

### Bons casos
- crític garantit
- crític prohibit
- +crit chance
- +crit multiplier
- efecte que diu “el pròxim atac serà crític”

### Exemples conceptuals

```java
ctx.forceCritical();
```

```java
ctx.forbidCritical();
```

```java
ctx.setCriticalChance(ctx.criticalChance() + 0.15);
```

### No és la millor per
- aplicar verí
- life steal
- escuts
- dany on-hit

---

### `MODIFY_DAMAGE`

### Usa-la quan:
- la mecànica canvia el dany ofensiu abans de la defensa

### Bons casos
- +20 dany
- x1.25 si enemic debilitat
- x0.80 si atacant està exhaust
- “execute” sota 30% de vida

### Exemples conceptuals

```java
ctx.addFlatDamage(20);
```

```java
ctx.multiplyDamage(1.25);
```

### No és la millor per
- coses que requereixen saber si el cop ha connectat
- efectes persistents de diversos torns
- escuts del defensor

---

### `BEFORE_DEFENSE`

### Usa-la quan:
- la mecànica és defensiva i ha d’actuar abans d’aplicar el dany final

### Bons casos
- escuts
- barreres
- reducció percentual
- reducció del primer cop rebut

### Exemple conceptual

```java
ctx.multiplyDamage(0.70);
```

### És especialment bona per
- `Effect` que viu al defensor

---

### `AFTER_DEFENSE`

### Usa-la quan:
- necessites saber com ha acabat la defensa
- vols reaccionar a esquiva o bloqueig
- vols inspeccionar `defenderResult`

### Bons casos
- “si esquiva, guanya evasió”
- “si defensa, perd una càrrega”
- “si rep el cop parcialment, activa un contraefecte”

### No és la millor per
- on-hit pur, si només vols casos de dany real

---

### `AFTER_HIT`

### Usa-la quan:
- el comportament només ha de passar si hi ha **dany real**
- estàs implementant un “on-hit”
- vols aplicar un `Effect`

### Bons casos
- life steal
- aplicar verí
- aplicar burn
- fer dany addicional real
- donar buff a l’atacant
- reaccionar a `ON_CRIT`
- reaccionar a `ON_KILL`

### Patró típic

```java
if (ctx.damageDealt() <= 0) return null;

ctx.defender().addEffect(new PoisonEffect(3, 5));
return "aplica verí";
```

### Aquesta és la millor fase per
- passives que “connecten”
- passives que apliquen effects

---

### `END_TURN`

### Usa-la quan:
- el comportament passa al final del round o del torn global
- vols tickejar duració, cooldown o DoT

### Bons casos
- verí
- cremades
- regen per torn
- baixar duració
- baixar cooldown
- destruir escuts caducats

### Exemples
- “rep 5 de verí”
- “la fúria perd 1 torn”
- “l’escut es dissipa”

### Recomanació important
Executa-la una vegada per round, no una vegada per atac.

---

## 3. Regles de decisió ràpida

### Si modifica el crític
→ `ROLL_CRIT`

### Si modifica dany ofensiu
→ `MODIFY_DAMAGE`

### Si protegeix el defensor
→ `BEFORE_DEFENSE`

### Si reacciona al resultat de defensar
→ `AFTER_DEFENSE`

### Si només ha de passar si connecta
→ `AFTER_HIT`

### Si dura torns i fa ticks
→ `END_TURN`

### Si prepara coses abans de tot
→ `START_TURN` o `BEFORE_ATTACK`

---

## 4. Dubtes típics

### “Aplicar verí va a `MODIFY_DAMAGE` o `AFTER_HIT`?”
→ `AFTER_HIT`

Perquè vols assegurar-te que el cop ha connectat.

---

### “Un escut temporal va a `MODIFY_DAMAGE` o `BEFORE_DEFENSE`?”
→ `BEFORE_DEFENSE`

Perquè és una reacció defensiva.

---

### “Un buff que garanteix crític al pròxim cop va a `AFTER_HIT` o `ROLL_CRIT`?”
Depèn del paper:
- la **passiva** que el concedeix pot anar a `AFTER_HIT`
- l’**effect** que força el crític actuarà a `ROLL_CRIT`

---

### “Una passiva que dona fúria quan mata, on va?”
Normalment a `AFTER_HIT`, consultant `ON_KILL`.

Exemple conceptual:

```java
if (ctx.hasEvent(HitContext.Event.ON_KILL)) {
    ctx.attacker().addEffect(new RageEffect(2, 1.20));
}
```

---

## 5. Errors comuns per fase

### Posar on-hit a `MODIFY_DAMAGE`
Error perquè encara no saps si hi ha hit real.

### Posar DoT a `AFTER_HIT`
Error si vols que duri torns; això hauria de ser un `Effect`.

### Posar escuts a `AFTER_HIT`
Error si han de protegir abans de rebre el dany.

### Posar lògica persistent en una passiva
Error perquè això pertany a un `Effect`.

---

## 6. Regla final

**La fase correcta és aquella on ja tens la informació necessària,  
però encara ets a temps d’afectar el comportament que vols modificar.**