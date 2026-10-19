# Lab 1 — Environnement, projet PokéQuiz et premier lancement

**Auteurs :** Ahmed Ennouri & Mohamed Koussay Fattoum

## Environnement utilisé

| Élément | Valeur |
|---|---|
| OS | Windows 11 Pro (x86_64) |
| JDK Gradle | JetBrains Runtime 21.0.8 (JBR fourni avec Android Studio) |
| SDK Android | `%LOCALAPPDATA%\Android\Sdk` — Platform 36, Build-Tools 36.1.0, Platform-Tools, Emulator |
| Émulateur | AVD `Medium_Phone`, image API 36 x86_64 (WHPX) |
| Git | 2.51.2 |

Les vérifications iOS / Xcode ne s'appliquent pas sous Windows : le module `iosApp` est présent dans le dépôt
mais n'a pas pu être lancé.

> **Remarque sur les versions.** Les dernières versions de Compose Multiplatform (1.12.x) tirent
> `androidx.compose.ui:ui-android:1.12.1`, qui impose `compileSdk 37`. Le TP demandant la plateforme 36,
> le projet utilise Compose Multiplatform **1.10.3** (+ Material 3 `1.10.0-alpha05`), dernière ligne compatible
> avec l'API 36.

---

## Étape 5 — Exploration de l'arborescence

### 1. Modules Gradle

Le projet contient **3 modules Gradle** : `:shared`, `:androidApp` et `:desktopApp`.
Ils sont déclarés dans **`settings.gradle.kts`** :

```kotlin
include(":shared")
include(":androidApp")
include(":desktopApp")
```

`iosApp` **ne fait pas partie** des modules Gradle : c'est un projet **Xcode**
(`iosApp/iosApp.xcodeproj`). Il n'a pas de `build.gradle.kts`. Il consomme le framework natif `Shared`
produit par Gradle : la build phase « Compile Kotlin Framework » du `project.pbxproj` appelle
`./gradlew :shared:embedAndSignAppleFrameworkForXcode`. C'est Xcode qui pilote Gradle, et non l'inverse.

### 2. Versions

Fichier **`gradle/libs.versions.toml`** (version catalog) et **`gradle/wrapper/gradle-wrapper.properties`** :

| Outil | Version | Source |
|---|---|---|
| Kotlin | 2.4.21 | `libs.versions.toml` → `kotlin = "2.4.21"` |
| Compose Multiplatform | 1.10.3 | `libs.versions.toml` → `composeMultiplatform = "1.10.3"` |
| AGP | 9.4.1 | `libs.versions.toml` → `agp = "9.4.1"` |
| Gradle | 9.8.1 | `gradle-wrapper.properties` → `distributionUrl=…gradle-9.8.1-bin.zip` |

(`./gradlew --version` affiche « Kotlin 2.4.10 » : c'est le Kotlin **embarqué par Gradle** pour les scripts
`.kts`, pas celui qui compile le projet.)

### 3. Plugins

**`shared/build.gradle.kts`** :
- `org.jetbrains.kotlin.multiplatform` : le module est un module KMP (plusieurs cibles) ;
- `com.android.kotlin.multiplatform.library` : nouveau plugin Android pour bibliothèque KMP (bloc `kotlin { android { … } }`) ;
- `org.jetbrains.compose` : Compose Multiplatform (ressources, dépendances, outils) ;
- `org.jetbrains.kotlin.plugin.compose` : plugin du compilateur Compose (transforme les `@Composable`).

**`androidApp/build.gradle.kts`** :
- `com.android.application` ;
- `org.jetbrains.compose` ;
- `org.jetbrains.kotlin.plugin.compose`.

`androidApp` n'applique pas `org.jetbrains.kotlin.android` parce qu'**AGP 9 gère Kotlin nativement**
(« built-in Kotlin ») : le plugin `com.android.application` compile lui-même les sources Kotlin.
Ajouter `kotlin.android` est inutile, et AGP 9 le signale même comme une erreur.

### 4. `minSdk`

- Bibliothèque partagée : `shared/build.gradle.kts`, dans `kotlin { android { minSdk = … } }` ;
- Application : `androidApp/build.gradle.kts`, dans `android { defaultConfig { minSdk = … } }`.

Dans les deux cas, la valeur n'est **pas en dur** : elle vient du catalog,
`libs.versions.android.minSdk.get().toInt()`, avec `android-minSdk = "24"` dans `gradle/libs.versions.toml`.
Pareil pour `compileSdk` (36) et `targetSdk` (36).

### 5. Dossiers de `shared/src/`

```
shared/src/
├── androidMain/
├── commonMain/      (+ composeResources/)
├── commonTest/
├── iosMain/
└── jvmMain/
```

Il n'y a pas de `iosArm64Main` parce que Kotlin applique le **default hierarchy template** : il crée
automatiquement des source sets intermédiaires (`nativeMain` → `appleMain` → `iosMain`) partagés par
`iosArm64()` et `iosSimulatorArm64()`. Le code iOS est le même pour l'appareil et pour le simulateur. Il va donc
dans `iosMain`. Un dossier propre à une cible (`iosArm64Main`) n'est utile que pour du code spécifique à une
architecture. Comme il est facultatif, le wizard ne le crée pas.

### 6. `expect fun getPlatform()`

Déclaré dans **`shared/src/commonMain/kotlin/fr/m2/pokequiz/Platform.kt`**.

Il y a **3 `actual`** :
- `shared/src/androidMain/kotlin/fr/m2/pokequiz/Platform.android.kt` → `AndroidPlatform` ;
- `shared/src/jvmMain/kotlin/fr/m2/pokequiz/Platform.jvm.kt` → `JVMPlatform` ;
- `shared/src/iosMain/kotlin/fr/m2/pokequiz/Platform.ios.kt` → `IOSPlatform` (un seul `actual` pour les 2 cibles iOS, grâce à `iosMain`).

### 7. `applicationId` et `mainClass`

- `applicationId = "fr.m2.pokequiz"` (fichier `androidApp/build.gradle.kts`, `defaultConfig`).
- `mainClass = "fr.m2.pokequiz.MainKt"` (fichier `desktopApp/build.gradle.kts`, `compose.desktop.application`).

Le suffixe `Kt` vient de la JVM : elle n'accepte pas de fonction hors d'une classe. Kotlin compile donc les
fonctions *top-level* d'un fichier `main.kt` dans une classe générée nommée d'après le fichier,
**`MainKt`**. C'est elle qui contient la méthode statique `main` lancée par la JVM.

### 8. `App()`

Définie dans **`shared/src/commonMain/kotlin/fr/m2/pokequiz/App.kt`**.

Elle est appelée depuis **4 endroits** (une fois par plateforme, plus la preview Android) :
1. `androidApp/src/main/kotlin/fr/m2/pokequiz/MainActivity.kt` → `setContent { App() }` ;
2. `androidApp/src/main/kotlin/fr/m2/pokequiz/MainActivity.kt` → `AppAndroidPreview()` (`@Preview`) ;
3. `desktopApp/src/main/kotlin/fr/m2/pokequiz/main.kt` → `Window(…) { App() }` ;
4. `shared/src/iosMain/kotlin/fr/m2/pokequiz/MainViewController.kt` → `ComposeUIViewController { App() }`
   (lui-même appelé depuis Swift dans `iosApp/iosApp/ContentView.swift`).

### 9. `.gitignore`

Il ignore : `*.iml`, `.kotlin/`, `.gradle/`, `**/build/`, `xcuserdata/`, `local.properties`, `.idea/`,
`.DS_Store`, `captures`, `.externalNativeBuild`, `.cxx`, le contenu de `*.xcodeproj/` sauf
`project.pbxproj` et les données partagées, `node_modules/`.

`local.properties` doit être ignoré parce qu'il est **propre à chaque machine** : il contient le chemin
absolu du SDK (`sdk.dir=C\:\\Users\\ahmed\\AppData\\Local\\Android\\Sdk`), qui n'a aucun sens chez un autre
développeur ou sur la CI. On y met aussi souvent des **secrets** (clés d'API, mots de passe du keystore de
signature) qui ne doivent jamais arriver dans un dépôt.

### 10. `java.io.File` dans `commonMain`

En ajoutant `val f = java.io.File("x")` dans `App()`, l'IDE souligne `java` en rouge, et la compilation de
`:shared:compileCommonMainKotlinMetadata` échoue :

```
e: …/shared/src/commonMain/kotlin/fr/m2/pokequiz/App.kt:25:13 Unresolved reference 'java'.
```

`commonMain` est compilé pour **toutes** les cibles, y compris iOS (Kotlin/Native), où il n'y a ni JVM ni
bibliothèque Java. Seules la stdlib Kotlin commune et les bibliothèques multiplateformes y sont
accessibles. `java.io.File` n'existe que sur JVM/Android : sa place est dans `jvmMain` ou `androidMain`, ou
derrière un `expect`/`actual`. (Ligne supprimée ensuite.)

---

## Étape 6 — Lancement

- **Desktop** (`./gradlew :desktopApp:run`) : la fenêtre « PokeQuiz » s'ouvre avec le bouton « Click me! ».
  Après le clic, le texte affiche la version de Java : « Compose: Hello, Java 21.0.8! ».
- **Android** (émulateur `Medium_Phone`, API 36) : l'app s'installe avec `./gradlew :androidApp:installDebug`
  et affiche « Hello, Android 36! » après le clic.
- **iOS** : impossible sous Windows (Xcode requis).

## Étape 7 — Compose Hot Reload

App lancée avec `./gradlew :desktopApp:hotRun` (équivalent de la run configuration « with Compose Hot Reload »).

- Pendant que l'app tourne, j'ai changé le texte du bouton (« Clique-moi ! ») et ajouté
  `.background(Color(0xFFFFCB05))` au `Modifier` de la `Column`. Après sauvegarde et reload, le fond devient
  jaune et le texte change **sans redémarrer l'application**. La valeur du compteur affichée **est conservée**
  pendant le reload (compteur à 2 avant et après une modification du libellé).
- Le titre affiche « Java 25.0.4.1 » : le Hot Reload lance l'app sur une **JetBrains Runtime** spéciale
  (téléchargée par le plugin), qui sait redéfinir des classes à chaud, et non sur le JDK 21 de Gradle.
- **Renommer `main`** : le reload n'arrive pas à appliquer le changement, et la console affiche
  `java.lang.NoSuchMethodError: … ComposableSingletons$MainKt.lambda…`. Le point d'entrée déjà en cours
  d'exécution renvoie vers du code qui n'existe plus. Il faut **redémarrer** l'application.
- **Ajouter une dépendance Gradle** : un fichier de build modifié n'est pas rechargeable. Le classpath de
  la JVM est fixé au lancement, donc il faut aussi un **redémarrage complet**.

## Étape 8 — Écran d'accueil

- `Greeting.greet()` renvoie maintenant `"Bonjour depuis ${platform.name} !"`.
- `JVMPlatform.name` vaut `"Desktop (${os.name}) · Java ${java.version}"`.
- `App()` affiche, centrés : le titre « Bienvenue dans PokéQuiz » (`headlineMedium`), le message de
  plateforme, et un `Button` Material 3 « Compteur : N » (`remember { mutableIntStateOf(0) }`).

Résultats :
- Desktop : « Bonjour depuis Desktop (Windows 11) · Java 21.0.8 ! ». Le compteur passe bien à 3 après 3 clics.
- Android : « Bonjour depuis Android 36 ! ». Le compteur passe bien à 3 après 3 clics.

**Rotation de l'émulateur** : le compteur **n'est pas conservé**. « Compteur : 3 » repasse à
« Compteur : 0 » après la rotation. Une rotation est un *changement de configuration* : Android détruit puis
recrée l'`Activity`, donc toute la composition est recréée. `remember` garde l'état seulement tant que la
composition existe. Pour le conserver, il faudrait `rememberSaveable` (sauvegarde dans le `Bundle` de
l'Activity) ou un `ViewModel` (qui survit aux changements de configuration).

---

## Questions de réflexion

### 1. Pourquoi développer l'UI sur Desktop même pour une app mobile ?

- **Itérations beaucoup plus rapides** : pas d'APK à packager, pas d'installation sur un émulateur. Un run
  desktop est une simple JVM.
- **Compose Hot Reload** n'existe que pour la cible desktop (JVM) : on voit chaque modification en direct
  sans perdre l'état.
- Pas besoin d'émulateur (gourmand en RAM/CPU) pendant la majeure partie du développement de l'UI.
- L'UI est dans `commonMain` : ce qui marche sur desktop marche quasiment à l'identique sur Android et
  iOS. On vérifie ensuite sur les vraies plateformes (insets, rotation, densité, comportements propres à
  chaque OS).

### 2. Hot Reload vs redémarrage

- **Hot Reload** : le code modifié est recompilé, puis les classes sont **remplacées dans la JVM en cours
  d'exécution**. Compose relance ensuite la recomposition. Le processus, les fenêtres et l'**état**
  (`remember`, variables) sont conservés.
- **Redémarrage** : le processus est tué puis relancé. Tout l'état est perdu et l'app repart de zéro.

À l'étape 7, un redémarrage complet était nécessaire pour : **renommer `main`** (le point d'entrée en cours
ne peut pas être remplacé, `NoSuchMethodError`) et **ajouter une dépendance Gradle** (changement du
classpath et du script de build, impossible à recharger à chaud). Les changements de texte, de couleur ou
de `Modifier` passaient en Hot Reload.

### 3. Ajouter `androidTarget { }` (tutoriel de 2023) dans `shared/build.gradle.kts`

La configuration Gradle échoue immédiatement (testé) :

```
* What went wrong:
Failed to create 'androidTarget()'
Enabled `androidTarget()` target is not compatible with 'com.android.kotlin.multiplatform.library' plugin.
Solution: Please migrate your project from using 'androidTarget()' to 'android()' DSL provided by
'com.android.kotlin.multiplatform.library' plugin
```

`androidTarget()` appartient à l'ancien modèle (plugin `com.android.library` + cible Android déclarée par
le plugin Kotlin). Depuis AGP 9, une bibliothèque KMP utilise `com.android.kotlin.multiplatform.library`
et la cible se déclare avec `kotlin { android { … } }`. Les deux approches sont incompatibles. Morale : il
faut se méfier des tutoriels datés dans un écosystème qui évolue vite.

### 4. Pourquoi « Bonjour depuis … » diffère selon la plateforme ?

C'est le mécanisme **`expect`/`actual`** :
- `commonMain` déclare `expect fun getPlatform(): Platform`, un contrat sans implémentation ;
- chaque source set de plateforme fournit son `actual fun getPlatform()` : `AndroidPlatform`
  (`Build.VERSION.SDK_INT`), `JVMPlatform` (`System.getProperty("os.name")`, `java.version`),
  `IOSPlatform` (`UIDevice.currentDevice`).
- À la compilation de chaque cible, le compilateur associe l'`expect` à l'`actual` de cette cible.
  `Greeting` est écrit une seule fois en code commun, mais appelle une implémentation différente selon le
  binaire produit.

### 5. DEX, bytecode JVM ou binaire natif : lequel est le plus facile à analyser ?

Le plus facile est le **bytecode JVM** (desktop), suivi de très près par le **DEX** (Android) :
- le bytecode JVM garde les noms de classes, méthodes et champs, les signatures, les chaînes, et souvent
  les numéros de ligne. Des décompilateurs (CFR, Fernflower, JD-GUI) redonnent un code Java/Kotlin presque
  lisible. Un `.jar` desktop est en plus non obfusqué par défaut ;
- le DEX se décompile tout aussi bien avec **jadx** / apktool. Sans R8/ProGuard (ici
  `isMinifyEnabled = false`), tous les noms d'origine restent visibles ;
- le **binaire natif iOS** (code machine ARM64 produit par Kotlin/Native + LLVM) demande un
  désassembleur ou décompilateur (Ghidra, IDA, Hopper). Le résultat est du pseudo-C bien moins lisible.
  C'est plus difficile, mais **pas impossible** : les chaînes en clair et certains symboles restent
  visibles.

C'est important pour `commonMain` : **le même code est livré sur les trois plateformes**. Un secret (clé
d'API, algorithme « maison », URL interne, logique de licence) placé dans `commonMain` est aussi protégé
que sur la plateforme **la plus faible**, ici le jar desktop, décompilable en quelques secondes.
Conclusion : ne jamais mettre de secret côté client. Les garder côté serveur, et activer l'obfuscation
(R8 sur Android, ProGuard pour les distributions desktop) en sachant qu'elle ne fait que ralentir un
attaquant.

### 6. Tâches Gradle liées à l'exécution

`.\gradlew.bat tasks --all | findstr /i run` (extrait) :

```
desktopApp:run
desktopApp:runDistributable
desktopApp:runRelease
desktopApp:runReleaseDistributable
shared:jvmRun
Compose Hot Reload: Run tasks
desktopApp:hotRun        - Compose Application Run (Hot Reload enabled) | --mainClass=...
desktopApp:hotRunAsync   - Compose Application Run (Hot Reload enabled) | --mainClass=... [Async]
desktopApp:hotDev        - Compose Application Dev Run (Hot Reload enabled) | --className=... --funName=...
desktopApp:hotDevAsync
desktopApp:reload        - Hot Reloads code for all running applications
shared:hotRunJvm / shared:hotRunJvmAsync / shared:hotDevJvm / shared:hotDevJvmAsync / shared:reload
desktopApp:runHot        - Deprecated: Use 'hotRun' instead
shared:jvmRunHot         - Deprecated: Use 'hotRunJvm' instead
desktopApp:hotMcpServer  - Start MCP server for AI agent integration with the running Compose application
```

Tâches de **Hot Reload** : `hotRun` (lancer l'app en mode Hot Reload, option `--auto` pour recharger dès
qu'un fichier change), `hotRunAsync`, `hotDev` (lancer directement un composable donné), `reload`
(déclencher un rechargement des apps en cours), plus leurs équivalents `*Jvm` dans `:shared`.
`runHot` et `jvmRunHot` sont les anciens noms, dépréciés.

### 7. Sorties de compilation

Après `./gradlew :shared:jvmJar` puis `:shared:compileKotlinIosSimulatorArm64` (la compilation en klib
fonctionne même sous Windows ; seule l'édition de liens du framework iOS demande un Mac) :

- **`.class`** :
  - `shared/build/classes/kotlin/jvm/main/fr/m2/pokequiz/` → `AppKt.class`, `Greeting.class`,
    `JVMPlatform.class`, `Platform_jvmKt.class`, `ComposableSingletons$AppKt.class`… ;
  - empaquetés dans `shared/build/libs/shared-jvm.jar` ;
  - (même chose pour Android dans `shared/build/classes/kotlin/android/main/`, avec `AndroidPlatform.class`).
- **klib** :
  - `shared/build/classes/kotlin/iosSimulatorArm64/main/klib/shared/default/` (dossiers `ir/`, `linkdata/`,
    `manifest`…) : la bibliothèque Kotlin/Native du module pour le simulateur iOS ;
  - `shared/build/classes/kotlin/metadata/commonMain/` : les métadonnées compilées de `commonMain` ;
  - `shared/build/kotlinTransformedMetadataLibraries/commonMain/*.klib` : les métadonnées des dépendances
    (compose runtime, lifecycle…) utilisées pour analyser `commonMain`.

Un **klib** (Kotlin Library) est le format de bibliothèque de Kotlin pour les cibles non-JVM (Native, JS,
Wasm) et pour les métadonnées communes. Il contient la **représentation intermédiaire (IR) sérialisée** du
code Kotlin, pas du code machine. Le compilateur la transforme ensuite en binaire natif (LLVM) au moment de
l'édition de liens (`linkDebugFrameworkIosSimulatorArm64` → `Shared.framework`).

### 8. Dépendances runtime desktop (`jvmRuntimeClasspath`)

`./gradlew :shared:dependencies --configuration jvmRuntimeClasspath` fait apparaître :

```
+--- org.jetbrains.skiko:skiko:0.9.37.4
|    \--- org.jetbrains.skiko:skiko-awt:0.9.37.4
|         +--- org.jetbrains.runtime:jbr-api:1.5.0
```

**Skiko** (« Skia for Kotlin ») est le moteur de rendu de Compose Multiplatform hors Android. Il enveloppe
la bibliothèque graphique **Skia** (celle de Chrome et de Flutter). Sur desktop, Compose ne s'appuie pas sur
les composants Swing/AWT natifs : il dessine lui-même chaque pixel via Skia, dans une surface AWT
(`skiko-awt`). Sur Android, cette bibliothèque n'est pas nécessaire puisque le système fournit déjà son
propre pipeline de rendu (Skia/HWUI).

`skiko-awt` ne contient que l'API Java. Le code natif (DLL Skia) arrive par
`compose.desktop.currentOs` dans `desktopApp`, comme le montre
`./gradlew :desktopApp:dependencies --configuration runtimeClasspath` :
`org.jetbrains.skiko:skiko-awt-runtime-windows-x64:0.9.37.4`.

### 9. Preflight checks

L'outil *Preflight checks* du plugin Kotlin Multiplatform vérifie l'OS, l'IDE, le JDK, le SDK Android et,
sur Mac, Xcode, les simulateurs et CocoaPods. Il permet de relancer les vérifications et d'afficher le
détail de chaque contrôle avec la correction à appliquer. Sous Windows, les vérifications Xcode / iOS ne
s'appliquent pas (ignorées). La partie « désactiver/réactiver une vérification iOS » n'a pas pu être faite
faute de Mac.

### 10. iOS (Mac)

Non réalisé : pas de Mac disponible (Windows). Le projet `iosApp` est néanmoins présent et configuré
(`iosApp/iosApp.xcodeproj`, `ContentView.swift` qui appelle `MainViewControllerKt.MainViewController()`).
