# Jarful — un gestionnaire de tâches « boucle de jeu » pour les cerveaux TDAH

[![CI](https://github.com/kamome-run/jarful/actions/workflows/ci.yml/badge.svg)](https://github.com/kamome-run/jarful/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[日本語](README.md) · [English](README.en.md) · **Français** · [العربية](README.ar.md) · [Русский](README.ru.md) · [Español](README.es.md) · [Deutsch](README.de.md) · [Tiếng Việt](README.vi.md) · [Polski](README.pl.md) · [Українська](README.uk.md) · [Bahasa Indonesia](README.id.md) · [繁體中文（台灣）](README.zh-TW.md)

« Je peux jouer des heures, mais je repousse le travail et les corvées. » Jarful transforme la méthode
**post-it × bocal transparent × imprimante thermique** décrite par l'entrepreneur TDAH Laurie Hérault
([article original](https://www.laurieherault.com/articles/a-thermal-receipt-printer-cured-my-procrastination))
en une application pour Android (y compris les Chromebooks et les portables ChromeOS/Android de Google) et Windows 11.

> **Jarful** = « un bocal plein ». Froissez chaque ticket terminé et regardez le bocal se remplir.

---

## Sommaire

1. [Principe](#1-principe)
2. [Ce qu'il vous faut](#2-ce-quil-vous-faut)
3. [Plateformes prises en charge](#3-plateformes-prises-en-charge)
4. [Installation](#4-installation)
5. [Premier lancement et déroulé d'une journée](#5-premier-lancement-et-déroulé-dune-journée)
6. [Configuration de l'imprimante (détaillée)](#6-configuration-de-limprimante-détaillée)
7. [Synchronisation Android ⇄ Windows (détaillée)](#7-synchronisation-android--windows-détaillée)
8. [Raccourcis clavier](#8-raccourcis-clavier)
9. [Gestes tactiles](#9-gestes-tactiles)
10. [Données et sauvegardes](#10-données-et-sauvegardes)
11. [Langues](#11-langues)
12. [Dépannage](#12-dépannage)
13. [Compilation depuis les sources](#13-compilation-depuis-les-sources)
14. [Licence et avertissement](#14-licence-et-avertissement)

---

## 1. Principe

| La méthode | Dans Jarful |
|-----------|-------------|
| Découper les tâches en **micro-tâches de 2 à 5 minutes** pour répéter la boucle souvent | Tâches hiérarchiques en **colonnes côte à côte** ; `Tab` ajoute une sous-tâche instantanément |
| Un post-it = une tâche ; une fois faite, **on le froisse dans un bocal transparent** | Terminer un « ticket du jour » déclenche une **animation de froissement + bruit de papier + vibration** et fait tomber une boulette dans le bocal |
| Commencer la journée par des habitudes faciles ; **préparer le lendemain la veille** | Les **routines** par jour de la semaine génèrent les tickets du lendemain après l'heure de préparation (21 h par défaut) |
| Dès qu'on procrastine, écrire **les 3 à 5 prochaines tâches** et commencer | `Ctrl+K` **Se recentrer** : une tâche par ligne → tickets immédiats → la première démarre |
| Les tâches indivisibles sont **découpées par le temps** (« 10 minutes seulement ») | Les tickets avec timebox font un compte à rebours ; à la fin : « terminer / +5 min / découper » |
| Un arriéré (des milliers d'e-mails) devient « **tout le nouveau + N anciens, chaque jour** » | **Routines à quota** (compteur +1, terminé à l'objectif) |
| Une **imprimante thermique** supprime la friction | Impression en Bluetooth Classic / Bluetooth LE / COM / TCP avec **ESC/POS, TSPL ou CPCL**, un ticket par reçu |

## Captures d'écran

Windows 11 utilise le **Fluent Design System** (WinUI 3) ; Android utilise **Material 3**. Les couleurs de la marque sont communes.

| Windows 11 (Fluent) | Windows 11 sombre | Windows 11, chinois traditionnel |
|---|---|---|
| ![windows](docs/screenshots/windows-fluent.png) | ![windows dark](docs/screenshots/windows-fluent-dark.png) | ![windows zh-TW](docs/screenshots/windows-fluent-zh-TW.png) |

| Android (Material 3) | Android sombre | Android, arabe (RTL) |
|---|---|---|
| ![phone](docs/screenshots/android-material-phone.png) | ![phone dark](docs/screenshots/android-material-phone-dark.png) | ![phone ar](docs/screenshots/android-material-phone-ar.png) |

| Ticket imprimé (japonais) | Arabe | Chinois traditionnel | Vietnamien |
|---|---|---|---|
| ![ticket](docs/screenshots/ticket-raster.png) | ![ticket ar](docs/screenshots/ticket-raster-ar.png) | ![ticket zh](docs/screenshots/ticket-raster-zh-TW.png) | ![ticket vi](docs/screenshots/ticket-raster-vi.png) |

## 2. Ce qu'il vous faut

**Indispensable**
- Un appareil Android (téléphone / tablette / Chromebook / portable ChromeOS ou Android de Google) ou un PC Windows 11.

**Recommandé (pour reproduire toute la méthode)**
- **Imprimante thermique** : n'importe quel modèle ESC/POS de 58 mm ou 80 mm (Bluetooth Classic, Bluetooth LE, ou réseau filaire / Wi-Fi). Pour des étiquettes, une imprimante compatible TSPL ou CPCL.
- **Rouleaux de papier thermique** de la bonne largeur. Vous manipulez beaucoup les tickets : préférez un papier **sans bisphénol (sans BPA/BPS)**.
- **Un tableau blanc et des aimants** : les tickets imprimés sont **fixés au tableau blanc avec des aimants**, pour avoir le travail du jour sous les yeux. Décoller un ticket terminé et le froisser, c'est la récompense. Prévoyez 20 à 30 petits aimants (10–15 mm).
- **Un bocal transparent** pour les tickets froissés. Le bocal de l'application suffit, mais un vrai bocal renforce l'effet.

## 3. Plateformes prises en charge

| Plateforme | Téléchargement | Remarques |
|---|---|---|
| Android 8.0 et plus | `androidApp-debug.apk` / `androidApp-release-unsigned.apk` | Téléphones et tablettes ; Material 3, couleurs dynamiques sur Android 12+ |
| Chromebook (ChromeOS) / portables ChromeOS ou Android de Google | idem | S'installe sans écran tactile ; clavier, souris et tactile pris en charge |
| Windows 11 (x64) | `Jarful-*.msi` / `Jarful-*.exe` | Interface Fluent Design ; imprimantes Bluetooth via un port COM virtuel |

Les téléchargements sont sur la page [Releases](https://github.com/kamome-run/jarful/releases).

## 4. Installation

### 4.1 Android (téléphone / tablette)

1. Ouvrez [Releases](https://github.com/kamome-run/jarful/releases) dans le navigateur de l'appareil et téléchargez le dernier `androidApp-debug.apk`.
2. Touchez l'APK depuis la notification ou l'application Fichiers.
3. Si Android signale une application de source inconnue, touchez **Paramètres → Autoriser cette source** puis revenez (autorisation accordée une fois au navigateur / à Fichiers).
4. Touchez **Installer**, puis **Ouvrir**.
5. Au premier lancement, choisissez **Ajouter les exemples** pour obtenir une routine matinale modifiable.

> `release-unsigned.apk` s'adresse aux développeurs qui signent et distribuent l'application eux-mêmes. Utilisez normalement `debug.apk`.

### 4.2 Chromebook / portables ChromeOS ou Android de Google

ChromeOS permet d'installer un APK hors Google Play de deux façons.

**Option A : environnement de développement Linux + adb (recommandé)**
1. **Paramètres → Avancé → Développeurs → Environnement de développement Linux → Activer** (quelques minutes la première fois).
2. Sur le même écran, activez **Développer des applications Android → Débogage ADB**, puis redémarrez.
3. Dans le terminal Linux, installez adb et connectez-vous à l'appareil :
   ```bash
   sudo apt update && sudo apt install -y adb
   adb connect 100.115.92.2:5555      # acceptez la demande affichée à l'écran
   adb install ~/Downloads/androidApp-debug.apk
   ```
4. **Jarful** apparaît dans le lanceur. La fenêtre est redimensionnable ; à partir de 840 dp de large, l'application passe en trois volets.

**Option B : distribution par le Play Store géré** (appareils d'école / d'entreprise) : un administrateur peut publier l'APK comme application privée.

Appairez d'abord l'imprimante Bluetooth dans **Paramètres ChromeOS → Bluetooth**, puis choisissez-la dans l'application (Classic et LE fonctionnent).

### 4.3 Windows 11

1. Téléchargez `Jarful-<version>.msi` depuis [Releases](https://github.com/kamome-run/jarful/releases).
2. Double-cliquez sur l'installateur. Si l'écran bleu **SmartScreen** apparaît, cliquez sur **Informations complémentaires → Exécuter quand même** (l'installateur n'est pas signé ; le code source est public dans ce dépôt).
3. Confirmez l'emplacement et cliquez sur **Install**. L'installation est par utilisateur, sans droits administrateur.
4. Lancez **Jarful** depuis le menu Démarrer.
5. Les données sont dans `%APPDATA%\Jarful\jarful-data.json` (voir « Emplacement » dans les réglages).

Désinstallation : **Paramètres → Applications → Applications installées → Jarful**. Le fichier de données est conservé ; supprimez-le à la main si nécessaire.

## 5. Premier lancement et déroulé d'une journée

1. **Réglez vos routines** (onglet Routines) : listez de haut en bas des habitudes matinales faciles (préparer le café, ouvrir la fenêtre…). Activez les jours de la semaine par routine ; pour les habitudes comptées comme « traiter 10 e-mails », saisissez le nombre comme **quota**.
2. **Préparé la veille** : ouvrir l'application après l'heure « Préparer demain à » (21 h par défaut) génère les tickets de routine du lendemain. Le matin, les tickets du jour sont créés s'ils manquent.
3. **Découpez** (onglet Colonnes) : créez une grande tâche (« Nettoyer la maison ») dans la colonne de gauche, sélectionnez-la et appuyez sur `Tab` (ou « Ajouter une sous-tâche ») pour ajouter « Cuisine », « Salle de bain »… dans la colonne suivante, puis découpez jusqu'à des morceaux de **2 à 5 minutes** (« Laver la vaisselle »). Une tâche ouverte depuis plus de 3 jours affiche un rappel « découpez davantage ».
4. **Créez les tickets du jour** : sélectionnez une tâche et appuyez sur `T` ; pour toute une colonne, `Maj+T` (ou le menu de la colonne). Ils apparaissent sous forme de cartes façon reçu dans l'onglet Aujourd'hui.
5. **Imprimez et affichez** (facultatif) : `Ctrl+P` imprime tous les tickets du jour ; détachez-les et **fixez-les au tableau blanc avec des aimants**.
6. **Faites → terminez** : **Démarrer** affiche le temps écoulé (compte à rebours si timebox). Appuyez sur **Terminé** (ou glissez la carte vers la droite) : elle se froisse dans le bocal avec son et vibration. Décollez le ticket papier, froissez-le et jetez-le dans le vrai bocal.
7. **Quand vous vous surprenez à procrastiner** : `Ctrl+K` (⚡ Se recentrer), écrivez les 3 à 5 prochaines tâches, une par ligne, et appuyez sur **Commencer**. Elles deviennent des tickets et la première démarre.
8. **Statistiques** : boucles par jour (90 jours), série et réussite des routines.

## 6. Configuration de l'imprimante (détaillée)

Onglet Réglages → **Imprimante thermique**.

### 6.1 Choisir le mode de connexion

| Connexion | Systèmes | Imprimantes concernées |
|---|---|---|
| **Bluetooth Classic (SPP)** | Android / Chromebook | Imprimantes bimode, Bluetooth 2.1–5.x (demandent en général un PIN à l'appairage) |
| **Bluetooth LE (GATT)** | Android / Chromebook | Imprimantes de poche LE uniquement, Bluetooth 4.0–5.x (vendues « pour appli ») |
| **Serial / COM** | Windows 11 | Imprimantes Bluetooth Classic via port COM virtuel ; adaptateurs USB-série |
| **TCP/IP** | Android / Windows | Imprimantes de reçus réseau filaire / Wi-Fi (port 9100) |

Vous ne savez pas quel Bluetooth vous avez ? Si les réglages Bluetooth du système peuvent **appairer** l'imprimante (PIN ou confirmation), c'est du Classic ; si l'appairage échoue et que la notice dit « connectez-vous depuis l'application », c'est probablement du LE. Essayez les deux et gardez celui dont l'**impression test** réussit.

### 6.2 Choisir le protocole d'impression

| Protocole | Usage |
|---|---|
| **ESC/POS raster (par défaut)** | La plupart des imprimantes de reçus 58/80 mm. Le ticket est envoyé comme image : **toutes les langues s'impriment correctement quelles que soient les polices internes** |
| ESC/POS image bitmap | Anciens modèles sans raster `GS v 0` |
| ESC/POS texte | Impression avec la police interne. Choisissez le jeu de caractères (UTF-8 / Shift_JIS / Big5 / GB18030 / Windows-125x / CP8xx…) correspondant à l'imprimante |
| TSPL | Imprimantes d'étiquettes (hauteur et espacement en mm) |
| CPCL | Imprimantes d'étiquettes CPCL |
| Cat printer | Imprimantes de poche 57 mm qui ne parlent pas ESC/POS (famille GB01 / GT01 / MX06, livrées avec les applis « iPrint » / « Fun Print »). Connexion en BLE |
| Cat printer MXW01 | Génération plus récente (famille MXW01 / X5h ; le diagnostic liste AE01–AE04). BLE uniquement |

Largeur de papier : **58 mm (384 points)** ou **80 mm (576 points)**. Sur les imprimantes de poche sans massicot, laissez **Cut désactivé** ; 3 à 5 lignes d'avance papier conviennent.

### 6.3 Android / Chromebook — Bluetooth Classic

1. Allumez l'imprimante ; maintenez son bouton Bluetooth si elle exige un mode appairage.
2. **Paramètres de l'appareil → Bluetooth → Associer un nouvel appareil**, choisissez l'imprimante et saisissez le PIN de la notice (`0000` ou `1234` sont fréquents).
3. Jarful → Réglages → Imprimante thermique → **Bluetooth Classic (SPP)**.
4. Choisissez l'imprimante dans la liste des appareils appairés (🔄 actualise). Sur Android 12+, accordez l'autorisation **Appareils à proximité**.
5. Appuyez sur **Impression test** : « Jarful / Test print OK » doit sortir en quelques secondes.
6. Si l'impression s'arrête à mi-chemin, augmentez les lignes d'avance ou imprimez ticket par ticket (⋮ → Imprimer).

### 6.4 Android / Chromebook — Bluetooth LE

1. Activez le **Bluetooth** (la plupart des imprimantes LE ne nécessitent pas d'appairage). Sur Android 11 et antérieur, activez aussi la **localisation** (nécessaire au balayage BLE).
2. Jarful → Réglages → Imprimante thermique → **Bluetooth LE (GATT)**.
3. Appuyez sur 🔄 : balayage d'environ 4 secondes, les imprimantes nommées apparaissent. Sélectionnez la vôtre.
4. **Impression test**. La première connexion peut prendre 5 à 10 secondes.
5. Rien ne sort ? Redémarrez l'imprimante, fermez complètement l'application du fabricant (une imprimante LE n'accepte qu'une connexion à la fois) ou désactivez/réactivez le Bluetooth.

### 6.5 Windows 11 — Bluetooth (port COM virtuel)

1. **Paramètres → Bluetooth et appareils → Ajouter un appareil → Bluetooth**, appairez l'imprimante (PIN de la notice).
2. **Paramètres → Bluetooth et appareils → Appareils**, tout en bas, ouvrez **Autres paramètres d'appareils et d'imprimantes**.
3. Clic droit sur l'imprimante → **Propriétés → Services**, cochez **Port série (SPP)** puis **OK**.
4. Dans la même fenêtre, **Autres options Bluetooth → Ports COM** : notez le `COMx` **Sortant** de l'imprimante. S'il n'existe pas : **Ajouter → Sortant → choisir l'imprimante → SPP**.
5. Jarful → Réglages → Imprimante thermique → **Serial / COM** → choisissez `COMx` → **Impression test**.
6. « PORT_OPEN_FAILED » signifie qu'un autre programme occupe le port (utilitaire du fabricant) ou que l'imprimante est éteinte : fermez-le et redémarrez l'imprimante.

> La version Windows ne peut pas dialoguer avec les imprimantes LE uniquement. Imprimez depuis un appareil Android ou utilisez une imprimante TCP.

### 6.6 Imprimantes réseau (TCP/IP)

1. Branchez l'imprimante au réseau et imprimez sa **page d'autotest** (en général : bouton d'avance maintenu à l'allumage) pour lire son adresse IP.
2. Jarful → Réglages → **TCP/IP** → saisissez l'IP ; port `9100` (par défaut).
3. **Impression test**. Réservez l'IP dans votre box (réservation DHCP) pour qu'elle ne change pas.

### 6.7 Routine « imprimer et afficher »

- Le matin : onglet Aujourd'hui → **Imprimer toute la journée** (`Ctrl+P`) → détachez → **fixez au tableau blanc avec des aimants**, de haut en bas.
- Dans la journée : après chaque ticket, décollez-le, froissez-le et jetez-le **dans le bocal transparent**. Appuyer sur Terminé dans l'application ajoute aussi une boulette au bocal virtuel.
- Le soir : les tickets du lendemain se préparent seuls ; le matin, il n'y a plus qu'à imprimer.

## 7. Synchronisation Android ⇄ Windows (détaillée)

Ni cloud ni compte. **Les appareils d'un même Wi-Fi se synchronisent directement** (code PIN, port 47831 par défaut).

### 7.1 Hôte (un PC Windows est recommandé)

1. Réglages → **Synchronisation entre appareils** → activez **Faire de cet appareil l'hôte**.
2. Notez les **adresses de cet appareil** (ex. `192.168.1.20`) et le **PIN à 6 chiffres**.
3. Si le pare-feu Windows demande si Jarful peut accéder au réseau, autorisez-le sur les **réseaux privés**.
4. Tant que l'application est ouverte, elle accepte la synchronisation des autres appareils (« ● en écoute »).

### 7.2 Client (Android et autres)

1. Réglages → **Synchronisation entre appareils** → sous **Se connecter à**, saisissez l'**adresse IP de l'hôte** et le **PIN**.
2. Appuyez sur **Synchroniser maintenant**. « Synchronisé » confirme ; le bouton 🔄 de la barre supérieure fait de même.
3. Laissez la **synchronisation automatique** activée : au lancement et toutes les 5 minutes.

### 7.3 Fonctionnement et précautions

- Synchronisés : **tâches, tickets, routines**. Les réglages propres à l'appareil (imprimante…) ne le sont pas.
- Si les deux appareils modifient le même élément, **la modification la plus récente l'emporte**. Les suppressions se propagent aussi (une modification ultérieure ressuscite l'élément).
- Le trafic est du HTTP en clair dans le réseau local. Utilisez-le sur des réseaux de confiance et désactivez l'hôte sur un Wi-Fi public.
- Trois appareils ou plus fonctionnent tant qu'ils se connectent tous au même hôte.

## 8. Raccourcis clavier

| Touche | Action |
|-----|--------|
| `N` / `Entrée` | Nouvelle tâche dans cette colonne |
| `Tab` / `Maj+Entrée` | Ajouter une sous-tâche (découper) |
| `↑ ↓` | Se déplacer dans la colonne |
| `← →` | Changer de colonne |
| `Espace` | Terminé / non terminé |
| `T` / `Maj+T` | Ticket du jour / toute la colonne → aujourd'hui |
| `P` / `Maj+P` / `Ctrl+P` | Imprimer la tâche / la colonne / toute la journée |
| `Ctrl+K` | Se recentrer |
| `F2` | Renommer |
| `Suppr` | Supprimer (`Ctrl+Z` annule) |
| `Alt+↑ ↓` | Réordonner |
| `Ctrl+Z` | Annuler |
| `Ctrl+1–5` | Changer d'onglet |
| `Échap` | Annuler |

## 9. Gestes tactiles

| Geste | Action |
|---|---|
| **Glisser un ticket vers la droite** | Terminer (au-delà de 40 % de la largeur) |
| **Toucher** une tâche | Sélectionner (sur téléphone : ouvrir ses sous-tâches) |
| **Appui long** sur une tâche | Menu (sous-tâche / aujourd'hui / imprimer / renommer / déplacer / supprimer) |
| **Double toucher** | Renommer |
| **←** en haut à gauche | Revenir à la colonne parente |

Fonctionne sur les écrans tactiles des Chromebooks et portables Google ainsi que sur tablette, en parallèle de la souris et du clavier.

## 10. Données et sauvegardes

- Emplacement : Android `filesDir/jarful-data.json` (privé à l'application) ; Windows `%APPDATA%\Jarful\jarful-data.json`.
- **Sauvegarde** : Réglages → Données → **Exporter en JSON (copier)** copie tout dans le presse-papiers ; collez-le dans une note.
- **Restauration** : collez dans **Importer du JSON**. Les données existantes sont remplacées (`Ctrl+Z` annule une fois).
- Rien n'est envoyé hors de l'appareil automatiquement ; le seul correspondant est l'hôte de synchronisation que vous configurez.

## 11. Langues

日本語 / English / Français / العربية / Русский / Español / Deutsch / Tiếng Việt / Polski / Українська / Bahasa Indonesia / 繁體中文（台灣）.
L'application suit la langue du système par défaut ; changez-la dans Réglages → Langue. L'arabe passe toute l'interface de droite à gauche.
L'impression raster fonctionne dans toutes les langues (police adaptée choisie ligne par ligne ; l'arabe est ligaturé et aligné à droite).

## 12. Dépannage

| Symptôme | Solution |
|---|---|
| L'APK ne s'installe pas | Autorisez les sources inconnues ; vérifiez Android 8.0+ |
| Appareil Bluetooth absent de la liste | Appairez d'abord dans le système (Classic). En LE, relancez 🔄 et activez la localisation (Android 11 et antérieur) |
| L'impression test expire | Vérifiez l'alimentation, la distance et les autres applications connectées ; fermez l'appli du fabricant en LE |
| Texte illisible (mode texte) | Adaptez le jeu de caractères à la police interne ou passez en **ESC/POS raster** |
| Impression pâle | Le côté brillant du papier thermique doit faire face à la tête d'impression |
| Pas de port COM sous Windows | Ajoutez un port **Sortant** comme en 6.5 ; refaites l'appairage |
| Synchro : « hôte injoignable » | Même Wi-Fi ? Application hôte ouverte ? Pare-feu autorisé ? |
| Synchro : « PIN incorrect » | Ressaisissez le PIN affiché sur l'écran des réglages de l'hôte |
| S'appaire mais ne se connecte jamais (l'appli du fabricant échoue aussi) | Certaines **imprimantes LE ne se connectent plus une fois appairées dans le système**. Dissociez-la dans les réglages Bluetooth → redémarrez l'imprimante → choisissez **Bluetooth LE** dans Jarful → 🔄 → Impression test. Fermez complètement l'appli du fabricant. En cas d'échec, collez le rapport **Diagnostiquer la connexion** dans un ticket |
| Connexion impossible sur OPPO / Xiaomi / Huawei | Accordez « Appareils à proximité » et « Localisation », activez la localisation, exemptez Jarful de l'optimisation de batterie. Un échec en Classic bascule automatiquement en LE et le mode qui fonctionne est mémorisé |
| Tickets de routine absents | Vérifiez les jours et l'interrupteur « Activée » ; utilisez « Régénérer aujourd'hui » |

## 13. Compilation depuis les sources

```bash
# tests unitaires (domaine, encodage d'impression, synchro, captures)
./gradlew :shared:desktopTest :desktopApp:test
# APK Android (debug)
./gradlew :androidApp:assembleDebug
# installateur Windows (à lancer sous Windows)
./gradlew :desktopApp:packageMsi
# lancer l'application de bureau
./gradlew :desktopApp:run
```

Prérequis : JDK 17 et le SDK Android (API 35). Voir [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) ;
la spécification est dans [docs/SPEC.md](docs/SPEC.md) et les notes sur les articles dans [docs/SOURCES.md](docs/SOURCES.md) (en japonais).

## 14. Licence et avertissement

Licence MIT. Jarful est une **implémentation indépendante et non officielle** inspirée d'un article public.
Le projet n'est ni affilié à ni approuvé par Laurie Hérault, son application Colonnes ou la rédaction de Nazology, et ne contient
ni leurs textes, ni leurs images, ni leurs logiciels. Aucune affiliation ni garantie avec un produit d'imprimante.
