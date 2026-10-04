# CLAUDE.md

## Le projet

**Pirate Can Focus Too** est une application Android de concentration (minuteur façon Pomodoro)
déguisée en aventure de pirate. Le ton est **fun et drôle** : un capitaine pirate en **pixel art**
accompagne l'utilisateur, avec une interface entièrement en **police monospace** (esprit terminal / rétro).

- Une session de focus = une traversée en mer. La finir rapporte des doublons (pièces d'or).
- Une pause = une escale au port.
- Abandonner une session = le navire coule (gentiment, avec une réplique moqueuse).
- Les textes de l'app sont en **français**, avec un vocabulaire de pirate exagéré.

## Stack

- Kotlin, Jetpack Compose (Material 3), une seule Activity.
- Gradle Kotlin DSL + catalogue de versions (`gradle/libs.versions.toml`).
- `minSdk 26`, `targetSdk`/`compileSdk 35`, JVM 17.
- Pas de dépendance réseau ni de backend : tout est local.

## Environnement du développeur

- Le projet est ouvert dans **Android Studio sous Windows** (`C:\0_projects\pirate-can-focus-too`).
- `git` et `gh` sont installés dans **WSL (Ubuntu)**, pas dans PowerShell :
  lancer les commandes git via `wsl -e bash -lc "cd /mnt/c/0_projects/pirate-can-focus-too && git ..."`.
- Le build se fait depuis Android Studio (le wrapper Gradle est régénéré par Android Studio au premier sync si `gradlew` manque).
- Fins de ligne : `.gitattributes` force LF dans le dépôt.

## Organisation du code

```
app/src/main/java/dev/mathieuburnat/piratefocus/
├── MainActivity.kt          # point d'entrée, garde l'écran allumé pendant le focus
├── focus/
│   ├── FocusTimer.kt        # logique pure du minuteur (testée unitairement)
│   ├── FocusViewModel.kt    # état de l'écran, boucle de décompte
│   └── PirateQuotes.kt      # répliques du capitaine selon la phase
└── ui/
    ├── FocusScreen.kt       # écran principal (durées 5/10/30 + ":" pour une durée sur mesure)
    ├── MinutesWheel.kt      # roue de défilement pour choisir les minutes
    ├── PixelPirate.kt       # le capitaine + palette et drawSprite() partagés
    ├── PixelShip.kt         # le navire, centré, qui tangue pendant que les vagues défilent
    └── theme/Theme.kt       # couleurs "mer de nuit" + typo monospace partout
```

## Conventions

- Tout le texte affiché utilise `FontFamily.Monospace` (via le thème, ne pas le contourner).
- Le pixel art est défini sous forme de grilles de caractères dans `PixelPirate.kt`
  (un caractère = une couleur de la palette). Ajouter de nouveaux sprites de la même façon.
- La logique du minuteur reste dans `FocusTimer.kt`, sans dépendance Android, pour rester testable.
- Les nouvelles répliques vont dans `PirateQuotes.kt` : courtes, drôles, en français pirate.
