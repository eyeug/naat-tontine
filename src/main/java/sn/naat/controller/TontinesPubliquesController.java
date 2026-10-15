package sn.naat.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import sn.naat.MainApp;
import sn.naat.dao.AdhesionDAO;
import sn.naat.dao.TontineDAO;
import sn.naat.model.Enums;
import sn.naat.model.Tontine;
import sn.naat.model.Utilisateur;
import sn.naat.service.MembreService;
import sn.naat.service.MontantCotisationRegles;
import sn.naat.util.SessionContext;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Écran accessible sans authentification (Utilisateur "visiteur") :
 * consultation des tontines existantes et demande d'adhésion, qui
 * correspond à Utilisateur.soumettreDemandeAdhesion() dans le
 * diagramme de classes — accessible avant même d'avoir un compte.
 */
public class TontinesPubliquesController {

    @FXML private TableView<Tontine> tableTontines;
    @FXML private TableColumn<Tontine, String> colNom;
    @FXML private TableColumn<Tontine, Enums.StatutTontine> colStatut;
    @FXML private TableColumn<Tontine, String> colType;
    @FXML private TableColumn<Tontine, String> colCotisation;
    @FXML private TableColumn<Tontine, String> colFrequence;
    @FXML private TableColumn<Tontine, String> colMembres;
    @FXML private TableColumn<Tontine, String> colDescription;
    @FXML private Button boutonRejoindre;
    @FXML private Button boutonCreerTontine;
    @FXML private Label labelSousTitre;
    @FXML private Button boutonConnexion;
    @FXML private Button boutonInscription;
    @FXML private Button boutonDeconnexion;
    @FXML private Button boutonProfil;

    private final TontineDAO tontineDAO = new TontineDAO();
    private final AdhesionDAO adhesionDAO = new AdhesionDAO();
    private final MembreService membreService = new MembreService();

    /** Statut de chaque tontine (id → statut), calculé une fois par rafraîchissement. */
    private Map<String, Enums.StatutTontine> statuts = new HashMap<>();

    /** Nombre de membres actifs de chaque tontine (id → effectif), calculé une fois par rafraîchissement. */
    private Map<String, Integer> effectifs = new HashMap<>();

    /** Rafraîchit la page toute seule : les statuts évoluent au fil des cycles. */
    private Timeline minuteur;

    @FXML
    private void initialize() {
        Utilisateur connecte = SessionContext.getUtilisateurConnecte();
        if (connecte != null) {
            labelSousTitre.setText("Bienvenue, " + connecte.getNom()
                    + " — votre compte n'est actuellement membre d'aucune tontine en cours.");
            boutonConnexion.setVisible(false);
            boutonConnexion.setManaged(false);
            boutonInscription.setVisible(false);
            boutonInscription.setManaged(false);
            boutonDeconnexion.setVisible(true);
            boutonDeconnexion.setManaged(true);
            boutonProfil.setVisible(true);
            boutonProfil.setManaged(true);
            boutonCreerTontine.setVisible(true);
            boutonCreerTontine.setManaged(true);
        } else {
            boutonCreerTontine.setVisible(false);
            boutonCreerTontine.setManaged(false);
        }

        colNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNom()));

        // Statut de la tontine (en préparation / en cours / clôturée) sous forme de pastille colorée.
        colStatut.setCellValueFactory(d -> new SimpleObjectProperty<>(statuts.get(d.getValue().getId())));
        colStatut.setCellFactory(col -> new TableCell<Tontine, Enums.StatutTontine>() {
            @Override
            protected void updateItem(Enums.StatutTontine statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label pastille = new Label(statut.getLibelle());
                pastille.setStyle(stylePastille(statut));
                setGraphic(pastille);
                setText(null);
            }
        });
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTypeLibelle()));
        colCotisation.setCellValueFactory(d -> new SimpleStringProperty(MontantCotisationRegles.libelle(
                d.getValue().getMontantMin(), d.getValue().getMontantMax(), d.getValue().getMontantCotisation())));
        colFrequence.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFrequence().name()));
        colDescription.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDescription() == null ? "" : d.getValue().getDescription()));
        // Membres actifs / nombre demandé. Tant que ce nombre n'est pas atteint, la tontine reste « En préparation ».
        colMembres.setCellValueFactory(d -> new SimpleStringProperty(
                effectifs.getOrDefault(d.getValue().getId(), 0) + " / " + d.getValue().getNbMembresMax()));

        rafraichir(false);

        // Le statut change au fil des cycles (en préparation → en cours → clôturée) : la page se met à
        // jour toute seule, sans qu'il faille la rouvrir.
        minuteur = new Timeline(new KeyFrame(Duration.seconds(15), evenement -> {
            // Écran quitté : on arrête le rafraîchissement pour ne pas interroger la base pour rien.
            if (tableTontines.getScene() == null || tableTontines.getScene().getWindow() == null) {
                minuteur.stop();
                return;
            }
            rafraichir(true);
        }));
        minuteur.setCycleCount(Animation.INDEFINITE);
        minuteur.play();
    }

    /**
     * Recharge les tontines, leurs effectifs et leurs statuts.
     * @param silencieux vrai pour le rafraîchissement automatique : une base momentanément
     *                   indisponible n'ouvre alors pas une boîte de dialogue toutes les 15 secondes.
     */
    private void rafraichir(boolean silencieux) {
        try {
            // Le rafraîchissement automatique ne doit pas faire perdre sa sélection à l'utilisateur.
            Tontine selectionnee = tableTontines.getSelectionModel().getSelectedItem();
            String idSelectionne = selectionnee != null ? selectionnee.getId() : null;

            List<Tontine> tontines = tontineDAO.listerToutes();
            Map<String, Integer> nouveauxEffectifs = new HashMap<>();
            for (Tontine tontine : tontines) {
                nouveauxEffectifs.put(tontine.getId(), adhesionDAO.compterMembresActifs(tontine.getId()));
            }
            effectifs = nouveauxEffectifs;
            statuts = membreService.statutsDesTontines(tontines);
            tableTontines.setItems(FXCollections.observableArrayList(tontines));

            if (idSelectionne != null) {
                tontines.stream()
                        .filter(t -> t.getId().equals(idSelectionne))
                        .findFirst()
                        .ifPresent(t -> tableTontines.getSelectionModel().select(t));
            }
        } catch (SQLException e) {
            if (!silencieux) {
                afficherErreur("Erreur de chargement des tontines : " + e.getMessage());
            }
        }
    }

    @FXML
    private void demanderAdhesion() {
        Utilisateur connecte = SessionContext.getUtilisateurConnecte();
        if (connecte == null) {
            Alert alerte = new Alert(AlertType.INFORMATION,
                    "Vous devez être connecté(e) pour demander à rejoindre une tontine. "
                            + "Créez un compte ou connectez-vous, c'est gratuit et rapide.");
            alerte.setHeaderText("Connexion requise");
            alerte.getButtonTypes().setAll(new ButtonType("Se connecter"), new ButtonType("Créer un compte"),
                    ButtonType.CANCEL);
            var choix = alerte.showAndWait();
            if (choix.isPresent() && choix.get().getText().equals("Se connecter")) {
                MainApp.naviguerVers("/sn/naat/view/login.fxml");
            } else if (choix.isPresent() && choix.get().getText().equals("Créer un compte")) {
                MainApp.naviguerVers("/sn/naat/view/register.fxml");
            }
            return;
        }

        Tontine selection = tableTontines.getSelectionModel().getSelectedItem();
        if (selection == null) {
            afficherErreur("Sélectionnez d'abord une tontine dans la liste.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Rejoindre " + selection.getNom());
        dialog.setHeaderText("Confirmez votre identité pour envoyer la demande");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        PasswordField champMotDePasse = new PasswordField();
        champMotDePasse.setPromptText("Votre mot de passe");
        TextField champContactRef = new TextField();
        champContactRef.setPromptText("Nom, téléphone, relation");
        CheckBox caseCgu = new CheckBox("J'accepte les termes et conditions du contrat.");

        GridPane grille = new GridPane();
        grille.setHgap(10);
        grille.setVgap(10);
        grille.addRow(0, new Label("Mot de passe :"), champMotDePasse);
        grille.addRow(1, new Label("Contact de référence :"), champContactRef);
        grille.add(caseCgu, 1, 2);
        dialog.getDialogPane().setContent(grille);

        var resultat = dialog.showAndWait();
        if (resultat.isEmpty() || resultat.get() != ButtonType.OK) return;

        if (!caseCgu.isSelected()) {
            afficherErreur("Vous devez accepter les termes et conditions.");
            return;
        }

        try {
            membreService.soumettreDemandeAdhesion(connecte.getId(), selection.getId(),
                    champMotDePasse.getText(), champContactRef.getText().isBlank() ? null : champContactRef.getText(), true);
            afficherInfo("Votre demande a été envoyée au gérant de " + selection.getNom()
                    + ". Vous serez contacté(e) une fois validée.");
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            afficherErreur(e.getMessage());
        }
    }

    @FXML
    private void creerTontine() {
        Utilisateur connecte = SessionContext.getUtilisateurConnecte();
        if (connecte == null) return; // le bouton n'est visible que si connecté
        DemandeCreationDialogue.afficher(connecte.getId());
    }

    @FXML
    private void retourConnexion() {
        MainApp.naviguerVers("/sn/naat/view/login.fxml");
    }

    @FXML
    private void allerInscription() {
        MainApp.naviguerVers("/sn/naat/view/register.fxml");
    }

    @FXML
    private void ouvrirProfil() {
        ProfilDialogue.afficher();
    }

    @FXML
    private void seDeconnecter() {
        SessionContext.deconnecter();
        MainApp.naviguerVers("/sn/naat/view/login.fxml");
    }

    private static String stylePastille(Enums.StatutTontine statut) {
        String couleurs = switch (statut) {
            case EN_PREPARATION -> "-fx-background-color: #fdf3dc; -fx-text-fill: #8a5a00;";
            case EN_COURS -> "-fx-background-color: #e5f3ec; -fx-text-fill: #085c3b;";
            case CLOTUREE -> "-fx-background-color: #eceeec; -fx-text-fill: #555d58;";
        };
        return couleurs + " -fx-background-radius: 10; -fx-padding: 2 10 2 10;"
                + " -fx-font-weight: bold; -fx-font-size: 12px;";
    }

    private void afficherErreur(String message) {
        Dialogues.erreur(message);
    }

    private void afficherInfo(String message) {
        new Alert(AlertType.INFORMATION, message).showAndWait();
    }
}
