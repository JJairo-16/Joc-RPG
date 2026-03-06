# Patró recomanat: passives que apliquen efectes

Aquesta guia resumeix el patró correcte per combinar **passives** i **effects** sense barrejar responsabilitats.

---

## 1. Resum curt

Quan una arma té una passiva que “posa un estat”, el patró recomanat és:

1) la **passiva** detecta el moment correcte
2) la **passiva** aplica l’efecte al personatge corresponent
3) l’**efecte** s’encarrega de tota la seva lògica a partir d’aquí

---

## 2. Qui fa què?

### La passiva
Responsabilitats:
- decidir **quan** es dispara
- mirar condicions de combat
- aplicar l’efecte a atacant o defensor
- opcionalment evitar duplicitats si la regla ho demana

Exemples:
- “si hi ha hit real, aplica verí”
- “si és crític, dona escut”
- “si mata, aplica fúria a l’atacant”

### L’efecte
Responsabilitats:
- guardar estat
- actuar en les fases corresponents
- consumir càrregues / ticks / duració
- expirar

Exemples:
- “el verí fa 5 de dany durant 3 torns”
- “l’escut redueix un 30% durant 2 càrregues”
- “el buff garanteix crític al pròxim atac”

---

## 3. Quan fer-ho amb una passiva simple i quan amb un efecte?

### Fes-ho com a passiva simple si:
- és una acció instantània
- no necessita estat
- no s’ha de recordar després

Exemples:
- robar vida
- fer dany extra immediat
- sumar +20% dany si l’objectiu està per sota del 30%

### Fes-ho com a efecte si:
- dura més d’un moment
- usa torns, càrregues o stacks
- s’ha de poder refrescar o apilar
- altres sistemes han de poder consultar-lo

Exemples:
- verí
- escut
- fúria
- pròxim atac crític
- bloqueig de crítics

---

## 4. Patró base

```java
@Override
public String afterHit(Weapon weapon, HitContext ctx, Random rng) {
    if (ctx.damageDealt() <= 0) return null;

    ctx.defender().addEffect(new PoisonEffect(3, 5));
    return "aplica verí";
}
```

Aquí la passiva:
- comprova que hi ha impacte real
- aplica l’efecte

I prou.

---

## 5. Patró amb comprovació prèvia

```java
@Override
public String afterHit(Weapon weapon, HitContext ctx, Random rng) {
    if (ctx.damageDealt() <= 0) return null;

    if (!ctx.defender().hasEffect(PoisonEffect.KEY)) {
        ctx.defender().addEffect(new PoisonEffect(3, 5));
        return "aplica verí";
    }

    return null;
}
```

Aquest patró és bo quan la regla de disseny és explícitament:
- “només si no el té”

---

## 6. Patró amb `StackingRule`

Si l’efecte ja sap apilar-se o refrescar-se, la passiva no necessita gaire lògica:

```java
@Override
public String afterHit(Weapon weapon, HitContext ctx, Random rng) {
    if (ctx.damageDealt() <= 0) return null;

    ctx.defender().addEffect(new PoisonEffect(3, 5));
    return "el verí s’intensifica";
}
```

I el comportament real el decideix:
- `IGNORE`
- `REFRESH`
- `STACK`
- `REPLACE`

---

## 7. Exemple mental complet

### Cas: daga tòxica
Vols que:
- en impactar, hi hagi 25% d’aplicar verí
- el verí duri 3 torns
- si ja hi és, refresqui duració

### Solució correcta
- passiva:
  - va a `AFTER_HIT`
  - comprova hit real
  - tira la probabilitat
  - fa `addEffect(new PoisonEffect(...))`
- efecte:
  - `stackingRule() = REFRESH`
  - `endTurn(...)` aplica dany
  - `isExpired()` quan s’acaben els torns

---

## 8. Error habitual

### Error
La passiva:
- marca un flag
- guarda un comptador
- fa part del dany
- refresca duració
- controla expiració

### Problema
Això converteix la passiva en un pseudo-efecte i trenca la separació de responsabilitats.

### Solució
Treure tot això a un `Effect`.

---

## 9. Regles pràctiques

- si necessita **memòria**, probablement és un `Effect`
- si només necessita **instant**, probablement és una passiva
- la passiva no hauria de gestionar duració manualment
- la passiva no hauria de consumir càrregues d’un estat que podria ser un `Effect`
- si el comportament es podria reutilitzar en altres armes o skills, millor com a `Effect`

---

## 10. Regla final

**Passiva = trigger / aplicació**  
**Effect = estat / execució / expiració**