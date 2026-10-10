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
- Le build se fait depuis Android Studio, ou avec `gradlew.bat` en utilisant le JDK 21 d'Android Studio
  (`$env:JAVA_HOME="$env:USERPROFILE\.jdks\jbr-21.0.11"`). Gradle 8.11.1 refuse le JDK 25.
- Le téléphone de test (Galaxy A55) et un émulateur sont accessibles via `adb`.
- Les captures du README sont dans `docs/screenshots/` (prises sur l'émulateur).
- Fins de ligne : `.gitattributes` force LF dans le dépôt.

## Organisation du code

```
app/src/main/java/dev/mathieuburnat/piratefocus/
├── MainActivity.kt          # point d'entrée, démarre/arrête le gardien selon la phase
├── guard/
│   ├── BlacklistStore.kt    # liste noire (défaut : Instagram, TikTok, Reddit)
│   ├── GuardPermissions.kt  # accès aux données d'utilisation + affichage par-dessus
│   ├── FocusGuardService.kt # service au premier plan qui surveille l'appli ouverte
│   └── CaughtActivity.kt    # le capitaine surgit sur une appli interdite
├── journal/                 # journal secret (7 taps sur le menu) : sport contre boissons
│   ├── Journal.kt           # compteurs et verdict du capitaine (logique pure, testée)
│   ├── JournalQuotes.kt     # insultes, réactions par boisson, interventions tous les 5 verres
│   ├── JournalViewModel.kt  # en mémoire seulement : remis à zéro à chaque lancement
│   └── Crew.kt              # [dev] équipage imaginaire, scores jour/semaine/mois
├── focus/
│   ├── FocusTimer.kt        # logique pure du minuteur (testée unitairement)
│   ├── FocusViewModel.kt    # état de l'écran, boucle de décompte
│   └── PirateQuotes.kt      # répliques du capitaine selon la phase
└── ui/
    ├── PirateApp.kt         # navigation : menu, focus, paramètres
    ├── MenuScreen.kt        # menu de démarrage (journal grisé, déverrouillé après 7 taps)
    ├── JournalScreen.kt     # BISCOTOS VS TAVERNE (toucher un camp ouvre ses activités, commentées par une petite tête du capitaine), capitaine pompette si ça boit trop
    ├── CrewScreen.kt        # « Et comment se portent les autres matelots ?! » (classement [dev])
    ├── SettingsScreen.kt    # autorisations du gardien + liste noire
    ├── FocusScreen.kt       # écran principal (durées 5/10/30 + ":" pour une durée sur mesure)
    ├── MinutesWheel.kt      # roue de défilement pour choisir les minutes
    ├── PixelPirate.kt       # le capitaine + palette et drawSprite() partagés
    ├── PixelParrot.kt       # Coco le perroquet (pop-up au 8e verre)
    ├── VersionFooter.kt     # version discrète en bas des pages (0.1.<nb de commits>)
    ├── PixelShip.kt         # le navire, centré, qui tangue pendant que les vagues défilent
    └── theme/Theme.kt       # couleurs "mer de nuit" + typo monospace partout
```

## Conventions

- Tout le texte affiché utilise `FontFamily.Monospace` (via le thème, ne pas le contourner).
- Le pixel art est défini sous forme de grilles de caractères dans `PixelPirate.kt`
  (un caractère = une couleur de la palette). Ajouter de nouveaux sprites de la même façon.
- La logique du minuteur reste dans `FocusTimer.kt`, sans dépendance Android, pour rester testable.
- Les nouvelles répliques vont dans `PirateQuotes.kt` : courtes, drôles, en français pirate.
