# Guia completa de Passives (armes)

Aquesta guia explica **com crear passives** i **com configurar-les** a les armes amb el teu pipeline per fases.

---

## 1. Què és una passiva?

Una **passiva** és un comportament associat a una **arma** que pot executar-se en diferents **fases** del torn de combat (pipeline).

Exemples:
- Robatori de vida després d’un impacte real
- Bonus de dany quan l’enemic està per sota d’un llindar
- Dany verdader extra
- Aplicar un **efecte** (com verí) quan connectes un cop

Les passives **no viuen al personatge**, viuen a la seva **arma** i s’executen quan l’arma participa en un atac.

---

## 2. Fases del pipeline

Les fases que has definit al `HitContext.Phase` són la base per decidir **quan** s’aplica cada passiva:

- `BEFORE_ATTACK`  
  Abans d’aplicar modificadors al dany. Ideal per checks i per escriure `meta`.

- `MODIFY_DAMAGE`  
  Per **modificar el dany** abans de defensar (flat/multiplicador).

- `BEFORE_DEFENSE`  
  Abans de resoldre `DEFEND/DODGE`. Útil per passives que volen reaccionar a l’acció defensiva rival.

- `AFTER_DEFENSE`  
  Després de defensar/esquivar: ja tens el `Result` del defensor i el dany real.

- `AFTER_HIT`  
  Només quan hi ha **dany real** (recomanat per “on-hit”: roba vida, aplica verí, etc.).

- `END_TURN`  
  Lògica “per torn”. Si no tens sistema d’estats complet, ho pots usar per ticks simples d’arma.

> Regla pràctica: **si la passiva depèn de “connectar”**, posa-la a `AFTER_HIT` i comprova `ctx.damageDealt() > 0`.

---

## 3. Estructura d’una passiva

Una passiva implementa `WeaponPassive` i sobrescriu només les fases que necessita.

### Patró recomanat
- Retornar `null` o `""` si no vols imprimir res
- Retornar un missatge curt si vols log

### Exemple “plantilla”

```java
public static WeaponPassive example() {
    return new WeaponPassive() {

        @Override
        public String modifyDamage(Weapon weapon, HitContext ctx, Random rng) {
            // ctx.addFlatDamage(...);
            // ctx.multiplyDamage(...);
            return null;
        }

        @Override
        public String afterHit(Weapon weapon, HitContext ctx, Random rng) {
            // ctx.damageDealt() > 0 assegura impacte real
            return null;
        }
    };
}
```

---

## 4. Com modifiquen el dany (el punt important)

Les passives **no han d’aplicar dany directament** (excepte casos molt específics).  
El teu disseny ja ho facilita: en `MODIFY_DAMAGE` i `BEFORE_DEFENSE` pots fer:

- `ctx.addFlatDamage(x)` → suma dany
- `ctx.multiplyDamage(m)` → multiplica dany
- i el dany que entrarà a `DEFEND/DODGE` és `ctx.damageToResolve()`

Això manté el sistema net i predictible.

---

## 5. Exemples complets de passives

### 5.1 Robatori de vida (AFTER_HIT)

**Quan aplica:** després d’un hit real.  
**Com:** cura l’atacant un percentatge del dany real.

```java
public static WeaponPassive lifeSteal(double pct) {
    return new WeaponPassive() {
        @Override
        public String afterHit(Weapon weapon, HitContext ctx, Random rng) {
            if (ctx.damageDealt() <= 0) return null;

            double healAmount = ctx.damageDealt() * pct;
            double realHealed = ctx.attacker().geStatistics().heal(healAmount);

            if (realHealed <= 0) return null;

            return String.format("%s roba %.1f HP", ctx.attacker().getName(), realHealed);
        }
    };
}
```

> Nota: si l’atacant està a vida completa, `heal(...)` clampa i curarà 0.

---

### 5.2 Executor (MODIFY_DAMAGE)

**Quan aplica:** abans de defensar, perquè sigui part del dany base.  
**Com:** si l’enemic està sota un llindar de vida, augmenta el dany.

```java
public static WeaponPassive executor(double thresholdLife, double damageBonus) {
    return new WeaponPassive() {
        @Override
        public String modifyDamage(Weapon weapon, HitContext ctx, Random rng) {
            double ratio = ctx.defender().geStatistics().getHealth()
                         / ctx.defender().geStatistics().getMaxHealth();

            if (ratio > thresholdLife) return null;

            ctx.multiplyDamage(1.0 + damageBonus);

            return String.format("%s prepara una execució (+%.0f%% dany)",
                    ctx.attacker().getName(), damageBonus * 100.0);
        }
    };
}
```

---

### 5.3 Dany verdader (AFTER_HIT)

**Quan aplica:** després d’un hit real.  
**Com:** aplica dany extra basat en la vida màxima rival.

```java
public static WeaponPassive trueHarm(double pct) {
    return new WeaponPassive() {
        @Override
        public String afterHit(Weapon weapon, HitContext ctx, Random rng) {
            if (ctx.damageDealt() <= 0) return null;

            double maxHp = ctx.defender().geStatistics().getMaxHealth();
            double extra = maxHp * pct;

            if (extra <= 0) return null;

            ctx.defender().geStatistics().damage(extra);

            return String.format("%s connecta un dany verdader del %.2f%%",
                    ctx.attacker().getName(), pct * 100.0);
        }
    };
}
```

---

### 5.4 Aplicar un efecte (verí) en impactar (AFTER_HIT)

Això requereix que `Character` tingui:
- `addEffect(Effect e)`
- `triggerEffects(...)` (perquè el verí tiquegi a `END_TURN`)

**Passiva:**

```java
public static WeaponPassive poisonOnHit(double chance, int turns, double dmgPerTurn) {
    return new WeaponPassive() {
        @Override
        public String afterHit(Weapon weapon, HitContext ctx, Random rng) {
            if (ctx.damageDealt() <= 0) return null;
            if (rng.nextDouble() > chance) return null;

            ctx.defender().addEffect(new PoisonEffect(turns, dmgPerTurn));

            return String.format("%s enverina a %s.",
                    ctx.attacker().getName(),
                    ctx.defender().getName());
        }
    };
}
```

> La passiva **només aplica l’efecte**. L’efecte fa el dany quan toca (p. ex. `END_TURN`).

---

## 6. On es configuren les passives? (Arsenal / creació d’armes)

Les passives es posen a la llista de passives de l’arma quan la crees.

### Exemple conceptual (Arsenal)

```java
VAMPIRIC_DAGGERS(
    "...",
    Skills::nothing,
    Passives.lifeSteal(0.10),
    Passives.poisonOnHit(0.25, 3, 5)
);
```

Això fa que, cada vegada que aquella arma ataca, el combat executi les passives per fase.

---

## 7. Ordre d’execució i bones pràctiques

### 7.1 Ordre dins la mateixa fase
Per defecte, `Weapon.triggerPhase(...)` executa en l’ordre de la llista de passives.  
Si necessites ordre estable, mantén el catàleg amb una convenció (ex: primer mitigacions, després procs).

### 7.2 Evita side-effects abans del dany
- Si una passiva ha de “canviar el dany”, fes-ho a `MODIFY_DAMAGE` o `BEFORE_DEFENSE`.
- Si una passiva ha de “reaccionar a un hit real”, fes-ho a `AFTER_HIT`.

### 7.3 Condicions típiques
- “Només si defensa”: `ctx.defenderAction() == Action.DEFEND`
- “Només si és crític”: `ctx.getMeta("CRIT", Boolean.class, false)`
- “Només si el dany base era alt”: `ctx.getMeta("RAW_DAMAGE", Double.class, 0.0)`

---

## 8. Checklist ràpid per crear una passiva nova

1) Decideix **fase** (quan aplica).
2) Decideix **condicions** (ex: `damageDealt > 0`, acció defensiva, llindar de vida).
3) Decideix **efecte**:
   - modificar dany: `addFlatDamage / multiplyDamage`
   - cura / dany extra: normalment a `AFTER_HIT`
   - aplicar un Effect: `ctx.defender().addEffect(...)`
4) Retorna missatge (o `null`) per log.

---

## 9. Errors típics

- Aplicar cura/veneno a `MODIFY_DAMAGE`: crea comportaments estranys (millor `AFTER_HIT` o `END_TURN`).
- No controlar `ctx.damageDealt()`: procs que s’apliquen encara que el cop s’hagi esquivat.
- Fer massa lògica en una sola fase: millor dividir en pre i post.

---

## 10. Exemple complet: arma amb dos procs

- `executor(...)` (pre-dany)
- `lifeSteal(...)` (post-hit)

Això crea una arma que **acaba combats** quan el rival està baix i **sustenta** l’atacant si connecta cops.
