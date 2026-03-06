# Guia completa de Passives (armes)

Aquesta guia explica **com crear passives** i **com configurar-les** a les armes amb el pipeline per fases.

---

## 1. Què és una passiva?

Una **passiva** és un comportament associat a una **arma** que pot executar-se en diferents **fases** del torn de combat (pipeline).

Exemples:
- Robatori de vida després d’un impacte real
- Bonus de dany quan l’enemic està per sota d’un llindar
- Dany verdader extra
- Aplicar un **efecte** (com verí) quan connectes un cop
- Aplicar un **efecte beneficiós** a l’atacant (com crític garantit al següent atac)

Les passives **no viuen al personatge**, viuen a la seva **arma** i s’executen quan l’arma participa en un atac.

---

## 2. Responsabilitat d’una passiva

Una passiva hauria de fer una de dues coses:

### A) Proc puntual
Executa una acció immediata i sense estat persistent.

Exemples:
- robar vida a `AFTER_HIT`
- sumar dany pla a `MODIFY_DAMAGE`
- fer dany verdader extra a `AFTER_HIT`

### B) Aplicar un efecte
Quan el comportament necessita **duració, càrregues, stacks o reutilització**, la passiva hauria de **crear/aplicar un `Effect`** i prou.

Exemples:
- enverinar l’enemic
- donar un escut temporal
- garantir crític al pròxim cop
- bloquejar crítics durant 2 torns

> Regla d’or: si la lògica necessita **estat**, normalment no hauria de quedar dins la passiva.

---

## 3. Fases del pipeline

Les fases que has definit al `HitContext.Phase` són la base per decidir **quan** s’aplica cada passiva:

- `START_TURN`  
  Inici del torn. Útil per armes molt especials o sincronització amb efectes.

- `BEFORE_ATTACK`  
  Abans d’aplicar modificadors al dany. Ideal per checks i per escriure `meta`.

- `ROLL_CRIT`  
  Si el pipeline ho suporta, és la fase ideal per forçar, prohibir o alterar el crític.

- `MODIFY_DAMAGE`  
  Per **modificar el dany** abans de defensar (flat/multiplicador).

- `BEFORE_DEFENSE`  
  Abans de resoldre `DEFEND/DODGE`.

- `AFTER_DEFENSE`  
  Després de defensar/esquivar: ja tens el `Result` del defensor i el dany real.

- `AFTER_HIT`  
  Només quan hi ha **dany real**. Recomanat per “on-hit”.

- `END_TURN`  
  Lògica per torn. Normalment més pròpia d’efectes que de passives.

> Regla pràctica: **si la passiva depèn de “connectar”**, posa-la a `AFTER_HIT` i comprova `ctx.damageDealt() > 0`.

---

## 4. Estructura d’una passiva

Una passiva implementa `WeaponPassive` i sobrescriu només les fases que necessita.

### Patró recomanat
- retornar `null` o `""` si no vols imprimir res
- retornar un missatge curt si vols log
- si necessita estat persistent, aplicar un `Effect`

### Exemple “plantilla”

```java
public static WeaponPassive example() {
    return new WeaponPassive() {

        @Override
        public String modifyDamage(Weapon weapon, HitContext ctx, Random rng) {
            return null;
        }

        @Override
        public String afterHit(Weapon weapon, HitContext ctx, Random rng) {
            return null;
        }
    };
}
```

---

## 5. Com modifiquen el dany

Les passives **no han d’aplicar dany directament** tret de casos concrets de post-hit.  
En `MODIFY_DAMAGE` i `BEFORE_DEFENSE` és millor fer:

- `ctx.addFlatDamage(x)` → suma dany
- `ctx.multiplyDamage(m)` → multiplica dany

I el dany que entrarà a `DEFEND/DODGE` serà `ctx.damageToResolve()`.

Això manté el sistema net i predictible.

---

## 6. Exemples complets de passives puntuals

### 6.1 Robatori de vida (`AFTER_HIT`)

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

### 6.2 Executor (`MODIFY_DAMAGE`)

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

### 6.3 Dany verdader (`AFTER_HIT`)

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

## 7. Exemples de passives que apliquen efectes

### 7.1 Verí on-hit

Aquesta passiva **no fa el dany del verí**.  
Només aplica l’efecte.

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

### 7.2 Donar escut a l’atacant

```java
public static WeaponPassive grantShieldOnHit(int charges, double reductionPct) {
    return new WeaponPassive() {
        @Override
        public String afterHit(Weapon weapon, HitContext ctx, Random rng) {
            if (ctx.damageDealt() <= 0) return null;

            ctx.attacker().addEffect(new ShieldEffect(charges, reductionPct));

            return String.format("%s obté un escut temporal.",
                    ctx.attacker().getName());
        }
    };
}
```

### 7.3 Garantir crític al pròxim atac

```java
public static WeaponPassive prepareNextCrit(double chance) {
    return new WeaponPassive() {
        @Override
        public String afterHit(Weapon weapon, HitContext ctx, Random rng) {
            if (rng.nextDouble() > chance) return null;

            ctx.attacker().addEffect(new GuaranteedCritEffect(1));

            return String.format("%s prepara un crític per al pròxim cop.",
                    ctx.attacker().getName());
        }
    };
}
```

---

## 8. Quan la passiva ha de comprovar si l’efecte ja existeix?

Hi ha dues estratègies vàlides.

### Estratègia A: deixar-ho al contenidor
La passiva sempre fa:

```java
ctx.defender().addEffect(new PoisonEffect(turns, dmg));
```

I `Character.addEffect(...)` resol:
- ignorar
- refrescar
- apilar
- reemplaçar

Aquesta és la més neta si `StackingRule` està ben implementat.

### Estratègia B: comprovar abans
La passiva fa una condició explícita:

```java
if (!ctx.defender().hasEffect(PoisonEffect.KEY)) {
    ctx.defender().addEffect(new PoisonEffect(turns, dmg));
}
```

És útil quan la regla exacta és:
- “només si no el té”
- “aplica un efecte únic”
- “no vull refrescar ni apilar”

### Recomanació
- si el comportament és genèric, usa `StackingRule`
- si la condició forma part del disseny de la passiva, fes el check explícit

---

## 9. On es configuren les passives? (`Arsenal` / creació d’armes)

Les passives es posen a la llista de passives de l’arma quan la crees.

### Exemple conceptual

```java
VAMPIRIC_DAGGERS(
    "...",
    Skills::nothing,
    Passives.lifeSteal(0.10),
    Passives.poisonOnHit(0.25, 3, 5)
);
```

---

## 10. Ordre d’execució i bones pràctiques

### 10.1 Ordre dins la mateixa fase
Per defecte, `Weapon.triggerPhase(...)` executa en l’ordre de la llista de passives.

### 10.2 Evita side-effects abans del dany
- si una passiva ha de “canviar el dany”, fes-ho a `MODIFY_DAMAGE`
- si una passiva ha de “reaccionar a un hit real”, fes-ho a `AFTER_HIT`
- si una passiva ha de donar estat, aplica un `Effect`

### 10.3 Condicions típiques
- “Només si defensa”: `ctx.defenderAction() == Action.DEFEND`
- “Només si és crític”: `ctx.getMeta("CRIT", Boolean.class, false)`
- “Només si el dany base era alt”: `ctx.getMeta("RAW_DAMAGE", Double.class, 0.0)`

---

## 11. Checklist ràpid per crear una passiva nova

1) Decideix **fase**.
2) Decideix si és:
   - proc puntual
   - aplicació d’un efecte
3) Decideix **condicions**.
4) Decideix **acció**:
   - `addFlatDamage / multiplyDamage`
   - cura / dany extra
   - `character.addEffect(...)`
5) Retorna missatge (o `null`) per log.

---

## 12. Errors típics

- Posar dins la passiva lògica que hauria de viure dins un `Effect`
- Aplicar un efecte i després també duplicar la seva lògica manualment
- No controlar `ctx.damageDealt()`
- Fer massa lògica persistent en `AFTER_HIT`
- Oblidar que una passiva és **trigger**, però un efecte és **estat**

---

## 13. Regla d’or

**La passiva detecta i aplica.  
L’efecte persisteix i resol.**

Exemple:
- passiva: “si connecta, aplica `PoisonEffect`”
- efecte: “fa dany a `END_TURN` i expira als 3 torns”