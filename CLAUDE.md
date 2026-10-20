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
- Sans compte, tout est local (journal et doublons sauvegardés sur le téléphone). Avec un compte gratuit,
  journal et coffre sont synchronisés avec le Worker Cloudflare de `backend/` (issue #8).
  Compte gratuit : email (simple identifiant privé, jamais vérifié, aucun mail envoyé) + mot de passe + code secret
  (montré une seule fois, il remet un mot de passe oublié). Les routes email/Google du Worker restent, inutilisées.

## Environnement du développeur

- Le projet est ouvert dans **Android Studio sous Windows** (`C:\0_projets\pirate-can-focus-too`).
- `git` et `gh` sont installés dans **WSL (Ubuntu)**, pas dans PowerShell :
  lancer les commandes git via `wsl -e bash -lc "cd /mnt/c/0_projets/pirate-can-focus-too && git ..."`.
- Le backend (`node`, `npm`, `npx wrangler`) se lance aussi dans **WSL** :
  `wsl -e bash -lc "cd /mnt/c/0_projets/pirate-can-focus-too/backend && npm ..."`. Si Node vient de nvm, un shell
  non interactif ne le charge pas toujours : utiliser `bash -lic`, ou `source ~/.nvm/nvm.sh` avant la commande.
- Le build se fait depuis Android Studio, ou avec `gradlew.bat` en utilisant le JDK 21 d'Android Studio
  (`$env:JAVA_HOME="$env:USERPROFILE\.jdks\jbr-21.0.11"`). Gradle 8.11.1 refuse le JDK 25.
- Le téléphone de test (Galaxy A55) et un émulateur sont accessibles via `adb`.
- Les captures du README sont dans `docs/screenshots/` (prises sur l'émulateur).
- Fins de ligne : `.gitattributes` force LF dans le dépôt.

## Organisation du code

```
app/src/main/java/dev/mathieuburnat/piratefocus/
├── MainActivity.kt          # point d'entrée, démarre/arrête le gardien selon la phase, synchro avec le Worker
├── account/
│   ├── Account.kt           # le compte tel que le téléphone le connaît (anonyme ou compte gratuit)
│   ├── AccountStore.kt      # compte rangé dans les SharedPreferences (pseudo, identifiant, jeton)
│   ├── AccountViewModel.kt  # bienvenue, créer un compte / se connecter / mot de passe oublié, code secret, synchro
│   ├── AccountQuotes.kt     # textes du capitaine sur les comptes (avertissement sans compte...)
│   ├── PasswordStrength.kt  # solidité d'un mot de passe, indicative (logique pure, testée)
│   ├── PirateApi.kt         # client de l'API backend/ (HttpURLConnection + org.json)
│   └── PirateNames.kt       # pseudos de pirate tirés au sort (logique pure, testée)
├── settings/
│   ├── FactoryReset.kt      # factory reset : efface tout ce que le téléphone garde (compte local, journal, coffre...)
│   └── DevModeStore.kt      # mode dev, et son option « fake data » : équipage inventé au lieu des vrais pirates publics
├── guard/
│   ├── BlacklistStore.kt    # liste noire (défaut : Instagram, TikTok, Reddit)
│   ├── GuardPermissions.kt  # accès aux données d'utilisation + affichage par-dessus
│   ├── GuardNudge.kt        # quand le capitaine réclame ses autorisations (1re fois, puis 1 fois sur 3 ; logique pure, testée)
│   ├── FocusGuardService.kt # service au premier plan qui surveille l'appli ouverte
│   └── CaughtActivity.kt    # le capitaine surgit sur une appli interdite
├── journal/                 # journal du capitaine : sport contre boissons
│   ├── Journal.kt           # compteurs et verdict du capitaine (logique pure, testée)
│   ├── JournalQuotes.kt     # insultes, réactions par boisson, interventions tous les 5 verres
│   ├── JournalViewModel.kt  # compteurs du jour, journal de bord chargé depuis un LogBook
│   ├── LogBook.kt           # où ranger le journal (interface ; Forgetful pour les tests)
│   ├── JournalStore.kt      # LogBook sur le téléphone : fichier journal.tsv (écriture atomique)
│   ├── JournalCodec.kt      # journal <-> texte, une ligne par entrée (logique pure, testée)
│   ├── JournalSync.kt       # fusion du journal du téléphone et du Worker (logique pure, testée)
│   └── Crew.kt              # scores jour/semaine/mois, équipage imaginaire de « fake data »
├── focus/
│   ├── FocusTimer.kt        # logique pure du minuteur (testée unitairement)
│   ├── FocusViewModel.kt    # état de l'écran, boucle de décompte
│   ├── ChestStore.kt        # le coffre : doublons et traversées gardés (SharedPreferences)
│   └── PirateQuotes.kt      # répliques du capitaine selon la phase
└── ui/
    ├── PirateApp.kt         # navigation : menu, focus, paramètres
    ├── MenuScreen.kt        # menu de démarrage (focus, journal, paramètres, fiche de matelot), tag « [ Compte local ] » sans compte
    ├── JournalScreen.kt     # BISCOTOS VS TAVERNE (toucher un camp ouvre ses activités, commentées par une petite tête du capitaine), capitaine pompette si ça boit trop
    ├── CrewScreen.kt        # « Et comment se portent les autres matelots ?! » (vrais pirates publics, ou inventés avec « fake data »)
    ├── SettingsScreen.kt    # paramètres : applications bloquées (et tout débloquer), déconnexion, mode dev caché (5 taps sur le titre)
    ├── ReallySureDialog.kt  # ChoiceDialog (choix numérotés : conseillé, destructeur en rouge, annulation discrète) et « T'es vraiment sûr ? » : CONFIRMER après 5 s (factory reset, liste noire vidée, compte supprimé)
    ├── DevModeScreen.kt     # page du mode dev : activer/désactiver (l'éteindre recache le bouton) ; allumé : « fake data », factory reset, et « Hi daad » après 2 taps sur le titre
    ├── GuardNudgeDialog.kt  # pop-up du capitaine souriant : « SUIVRE LE CAPITAINE » vers les autorisations
    ├── BlockedAppsScreen.kt # autorisations du gardien + liste noire
    ├── WelcomeScreen.kt     # premier lancement : matelot anonyme ou compte gratuit
    ├── AccountScreen.kt     # « Ma fiche de matelot » : Identité, Sécurité, Visibilité, puis Zone dangereuse (déconnexion, suppression)
    ├── AccountForms.kt      # compte gratuit (créer / j'ai déjà un compte / mot de passe oublié), code secret montré une fois, dé
    ├── FocusScreen.kt       # écran principal (durées 5/10/30 + ":" pour une durée sur mesure)
    ├── MinutesWheel.kt      # roue de défilement pour choisir les minutes
    ├── PixelPirate.kt       # le capitaine + palette et drawSprite() partagés
    ├── PixelParrot.kt       # Coco le perroquet (pop-up au 8e verre)
    ├── PixelEye.kt          # œil / cache-œil des champs de mot de passe (montrer / cacher)
    ├── PixelDice.kt         # le dé des pseudos de pirate (faces 1 à 6)
    ├── Sparkles.kt          # gerbe de paillettes en pixel art (SparkleBurst)
    ├── VersionFooter.kt     # version discrète en bas des pages (0.1.<nb de commits>)
    ├── PixelShip.kt         # le navire, centré, qui tangue pendant que les vagues défilent
    └── theme/Theme.kt       # couleurs "mer de nuit" + typo monospace partout

backend/                     # API Cloudflare (Worker + D1) pour les comptes et la synchro, voir backend/README.md
```

## Conventions

- Tout le texte affiché utilise `FontFamily.Monospace` (via le thème, ne pas le contourner).
- Le pixel art est défini sous forme de grilles de caractères dans `PixelPirate.kt`
  (un caractère = une couleur de la palette). Ajouter de nouveaux sprites de la même façon.
- La logique du minuteur reste dans `FocusTimer.kt`, sans dépendance Android, pour rester testable.
- Les nouvelles répliques vont dans `PirateQuotes.kt` : courtes, drôles, en français pirate.
- Base de données : chaque changement de schéma est une **nouvelle migration** numérotée dans `backend/migrations/`
  (`0003_xxx.sql`, ...). Ne jamais modifier une migration déjà appliquée.
