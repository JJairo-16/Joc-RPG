# Combat RPG - [Jairo Linares](https://github.com/JJairo-16)

![Java Version](https://img.shields.io/badge/Java-21%2B-blue)
![License](https://img.shields.io/badge/License-MIT-green)

---

## ▌Què és?

**Combat RPG** és un joc de rol per torns (1vs1) on dos jugadors creen els seus personatges i s'enfronten en combat estratègic.

Cada jugador personalitza el seu personatge repartint punts d'estadístiques, escollint raça i equipant armes amb habilitats úniques.

---

## ▌Sistema de Personatges

Cada personatge té:

- Nom  
- Edat  
- Vida  
- Manà  
- Raça  
- Arma equipada  
- 7 estadístiques principals  

### ▌Estadístiques (7)

Els jugadors disposen de **140 punts** per repartir entre:

| Estadística      | Funció principal |
|------------------|-----------------|
| Força            | Dany físic i combat cos a cos |
| Destresa         | Esquiva, precisió i combat a distancia |
| Constitució      | Vida i resistència |
| Intel·ligència   | Dany màgic i manà |
| Saviesa          | Control i percepció |
| Carisma          | Influència i efectes socials |
| Sort             | Probabilitat de crítics i efectes especials |

---

## ▌Races (7)

Hi ha 7 races disponibles.  
Cada raça ofereix una **bonificació específica** a una estadística concreta, fomentant diferents estils de joc.

Exemple orientatiu:

- Humà → Bonificació a Carisma  
- Elf → Bonificació a Destresa  
- Orc → Bonificació a Força  
- Nan → Bonificació a Constitució  
- etc.

---

## ▌Armes

Hi ha **3 tipus d’armes** diferents.

Característiques:

- Rang de dany propi  
- Poden ser físiques, màgiques o de rang  
- Cada arma té una habilitat única  

Les habilitats poden incloure:

- "Ruleta rusa"
- Multiples atacs per torn
- Major benefici per major risc

---

## ▌Sistema de Combat

El combat és per torns:

1. Canviar arma (opcional)  
2. Escollir acció:
   - Atacar (i utilitzar l'habilitat)
   - Defensar-se  
   - Esquivar
3. Aplicació de regeneració automàtica  

La partida finalitza quan un personatge arriba a 0 de vida.

---

## ▌Mecàniques Destacades

- Sistema d’esquiva basat en Destresa  
- Sistema de crítics influenciat per la Sort  
- Regeneració de vida i manà per torn  
- Diferenciació entre armes físiques i màgiques  
- Escalat de dany segons estadístiques  

---

## ▌Execució

Compilar i executar la classe principal:

```java
App.java
```

---

## ▌Llicència

Aquest projecte està sota la llicència [MIT](LICENSE).

---

## ▌Autor

Jairo Linares  
GitHub: https://github.com/JJairo-16