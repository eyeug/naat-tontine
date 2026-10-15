# NAAT — Tontine sénégalaise modernisée (JavaFX)

Application de bureau JavaFX pour la gestion de tontines (fonds
commun, tour de rôle, tirage au sort), avec paiement simulé via
Wave, Orange Money ou espèces. Implémentation fidèle au diagramme
de classes et au schéma de base de données élaborés au fil de la
conversation.

## Architecture du projet

```
src/main/java/sn/naat/
  model/        Classes métier (Utilisateur/Membre/Gerant/Administrateur,
                Tontine + 3 sous-types, ModePaiement + 3 sous-types,
                Adhesion, Transaction, Pret, Cycle, Distribution, etc.)
  dao/          Accès JDBC à MySQL (un DAO par entité / groupe d'entités)
  service/      Logique métier (AuthService, MembreService, GerantService,
                AdministrateurService) — c'est ici que vivent les règles
                du diagramme (typage par sous-classe, anti-conflit
                gérant/prêt, etc.)
  controller/   Contrôleurs JavaFX (un par écran)
  util/         Connexion BD, session, utilitaires (hash mot de passe, ids)
  MainApp.java  Point d'entrée de l'application

src/main/resources/sn/naat/
  view/         Fichiers FXML des écrans
  css/          Feuille de style
  db/           naat_schema.sql (script de création de la base + compte admin)
```

## 1. Prérequis

- **JDK 17** ou supérieur — [adoptium.net](https://adoptium.net)
- **Maven 3.9+** — [maven.apache.org](https://maven.apache.org/download.cgi)
- **MySQL 8+** — via XAMPP, WampServer, ou MySQL Community Server

Vérifiez les installations :
```bash
java -version
mvn -version
mysql --version
```

## 2. Créer la base de données

Démarrez MySQL, puis exécutez le script fourni (`src/main/resources/sn/naat/db/naat_schema.sql`) :

```bash
mysql -u root -p -e "CREATE DATABASE naat;"
mysql -u root -p naat < src/main/resources/sn/naat/db/naat_schema.sql
```

Ou via phpMyAdmin : créez une base nommée `naat`, puis onglet
**Importer** → sélectionnez `naat_schema.sql`.

Le script crée automatiquement un **compte administrateur** de test :
- **Téléphone :** `770000000`
- **Mot de passe :** `admin123`

### Mettre à jour une base existante

Si votre base `naat` a été créée avec une version plus ancienne du projet, **exécutez aussi** le script
`src/main/resources/sn/naat/db/naat_correctif_consolide.sql` après chaque mise à jour du projet. Il ajoute
uniquement ce qui manque (colonnes, tables) et peut être relancé sans risque : ce qui existe déjà est ignoré.
Sans lui, l'application affiche des erreurs du type « Table '…' doesn't exist » ou « Unknown column ».

```bash
mysql -u root -p naat < src/main/resources/sn/naat/db/naat_correctif_consolide.sql
```

## 3. Configurer la connexion (si besoin)

Par défaut, l'application se connecte à `jdbc:mysql://localhost:3306/naat`
avec l'utilisateur `root` et un mot de passe vide (valeurs par défaut
de XAMPP/WAMP). Pour changer ces valeurs, définissez des variables
d'environnement avant de lancer l'application :

```bash
# Windows (PowerShell)
$env:NAAT_DB_URL="jdbc:mysql://localhost:3306/naat"
$env:NAAT_DB_USER="root"
$env:NAAT_DB_PASSWORD="votre_mot_de_passe"

# macOS / Linux
export NAAT_DB_URL="jdbc:mysql://localhost:3306/naat"
export NAAT_DB_USER="root"
export NAAT_DB_PASSWORD="votre_mot_de_passe"
```

## 4. Compiler et lancer l'application

Depuis le dossier du projet (celui qui contient `pom.xml`) :

```bash
mvn clean javafx:run
```

Maven télécharge automatiquement JavaFX et le connecteur MySQL au
premier lancement (connexion internet nécessaire une seule fois).

## 5. Premiers pas dans l'application

1. Connectez-vous avec le compte administrateur (`770000000` / `admin123`),
   ou cliquez sur **« Pas encore de compte — S'inscrire »** depuis l'écran
   de connexion pour créer votre propre compte.
2. Une fois inscrit, connectez-vous, puis cliquez sur **« Créer une
   tontine »** pour soumettre une demande (nom, type, cotisation,
   fréquence, nombre de membres max).
3. Reconnectez-vous avec le compte administrateur pour **valider la
   demande** dans l'onglet « Demandes de création de tontine » — le
   demandeur est alors automatiquement promu gérant.
4. Reconnectez-vous avec ce compte : vous êtes maintenant sur le
   tableau de bord Gérant. Démarrez un premier cycle, validez les
   demandes d'adhésion reçues, saisissez des cotisations espèces,
   avancez le tour ou lancez un tirage selon le type de tontine.
5. Connectez-vous avec un compte membre pour cotiser (Wave, Orange
   Money ou espèces), demander un prêt (si Fonds commun), consulter
   l'historique et les notifications reçues.
6. Depuis le tableau de bord Gérant, sélectionnez un cycle dans la
   liste déroulante et cliquez sur **« Clôturer le cycle sélectionné »**
   pour déclencher la distribution (le montant est calculé automatiquement
   à partir des cotisations reçues depuis le début du cycle).

Tous les écrans du diagramme de classes sont désormais reliés à des
vues fonctionnelles : inscription, création de tontine, validation
en cascade (adhésion → gérant, création → administrateur), gestion
de cycle, et affichage des notifications sur les tableaux de bord
Membre et Gérant.

