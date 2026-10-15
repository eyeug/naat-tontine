package sn.naat.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import sn.naat.MainApp;
import sn.naat.model.*;
import sn.naat.model.Enums;
import sn.naat.service.ActionsRegles;
import sn.naat.service.CotisationRegles;
import sn.naat.service.IndicateursRegles;
import sn.naat.service.MontantCotisationRegles;
import sn.naat.service.EtatCotisationRegles;
import sn.naat.service.HistoriqueRegles;
import sn.naat.service.MembreService;
import sn.naat.util.SessionContext;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MembreDashboardController {

    @FXML private Label labelBienvenue;
    @FXML private Button boutonChangerEspace;
    @FXML private ListView<Tontine> listeTontines;
    @FXML private Label labelHistorique;
    @FXML private TableView<Transaction> tableTransactions;
    @FXML private TableColumn<Transaction, String> colDate;
    @FXML private TableColumn<Transaction, String> colType;
    @FXML private TableColumn<Transaction, String> colMontant;
    @FXML private TableColumn<Transaction, String> colDetail;
    @FXML private TableColumn<Transaction, String> colStatut;
    @FXML private Label labelRetards;
    @FXML private Label labelCycle;
    @FXML private Button boutonCotiser;
    @FXML private Button boutonDemanderPret;
    @FXML private Button boutonRembourserPret;
    @FXML private Button boutonTourAnticipe;
    @FXML private Label labelAideBoutons;
    @FXML private Button boutonRejoindre;
    @FXML private ListView<String> listeNotifications;

    @FXML private javafx.scene.control.Label kpiTotal;
    @FXML private javafx.scene.control.Label kpiTotalNote;
    @FXML private javafx.scene.control.Label kpiEtat;
    @FXML private javafx.scene.control.Label kpiEtatNote;
    @FXML private javafx.scene.control.Label kpiRetards;
    @FXML private javafx.scene.control.Label kpiRetardsNote;
    @FXML private javafx.scene.control.Label kpiNotifs;
    @FXML private javafx.scene.control.Label kpiNotifsNote;

    private final MembreService membreService = new MembreService();
    private final DateTimeFormatter formatDate = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        Utilisateur u = SessionContext.getUtilisateurConnecte();
        labelBienvenue.setText("Bienvenue, " + u.getNom());
        NavigationEspaces.configurer(boutonChangerEspace);

        colDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDate().format(formatDate)));
        // Un retard (pénalité) s'affiche clairement : type « RETARD (pénalité) », montant précédé de « − »,
        // explication du cycle concerné, ligne surlignée en rouge. Même présentation que chez le gérant.
        colType.setCellValueFactory(d -> new SimpleStringProperty(HistoriqueRegles.libelleType(d.getValue())));
        colMontant.setCellValueFactory(d -> new SimpleStringProperty(HistoriqueRegles.libelleMontant(d.getValue())));
        colDetail.setCellValueFactory(d -> new SimpleStringProperty(HistoriqueRegles.libelleDetail(d.getValue())));
        PresentationTransactions.surlignerRetards(tableTransactions);
        colStatut.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatut().name()));

        tableTransactions.setPlaceholder(new Label("Aucune transaction sur cette tontine."));

        // Chaque tontine a ses propres transactions : l'historique affiché suit
        // la tontine sélectionnée dans la liste.
        listeTontines.getSelectionModel().selectedItemProperty()
                .addListener((obs, ancienne, nouvelle) -> rafraichirHistorique());

        rafraichir();
    }

    /** Recharge l'historique des transactions de la tontine actuellement sélectionnée. */
    private void rafraichirHistorique() {
        Tontine selection = listeTontines.getSelectionModel().getSelectedItem();
        if (selection == null) {
            labelHistorique.setText("Historique des transactions");
            labelRetards.setText("");
            labelCycle.setText("");
            Kpi.definir(kpiTotal, kpiTotalNote, "—", "sélectionnez une tontine", Kpi.Etat.NORMAL);
            Kpi.definir(kpiEtat, kpiEtatNote, "—", "", Kpi.Etat.NORMAL);
            Kpi.definir(kpiRetards, kpiRetardsNote, "—", "", Kpi.Etat.NORMAL);
            tableTransactions.setItems(FXCollections.observableArrayList());
            masquerActionsSelonType();
            return;
        }
        Utilisateur u = SessionContext.getUtilisateurConnecte();
        try {
            labelHistorique.setText("Historique des transactions — " + selection.getNom());
            List<Transaction> transactions = membreService.consulterHistorique(selection, u.getId());
            tableTransactions.setItems(FXCollections.observableArrayList(transactions));

            // Où j'en suis pour le cycle en cours : « déjà cotisé » ou « à faire » (en rouge, avant que la pénalité tombe).
            EtatCotisationRegles.EtatCotisation etat = membreService.etatCotisation(selection, u.getId());
            labelCycle.setText(etat.message());
            labelCycle.setStyle(etat.aFaire()
                    ? "-fx-text-fill: #c0392b; -fx-font-weight: bold;"
                    : "-fx-text-fill: #0b7a4f; -fx-font-weight: bold;");

            Kpi.definir(kpiTotal, kpiTotalNote, IndicateursRegles.montant(IndicateursRegles.totalCotise(transactions)),
                    "cotisé sur " + selection.getNom(), Kpi.Etat.NORMAL);
            Kpi.definir(kpiEtat, kpiEtatNote, etat.aFaire() ? "À faire" : "À jour",
                    etat.aFaire() ? "cotisation attendue ce cycle" : "cotisation du cycle enregistrée",
                    etat.aFaire() ? Kpi.Etat.ALERTE : Kpi.Etat.NORMAL);
            long retards = HistoriqueRegles.nombreRetards(transactions);
            Kpi.definir(kpiRetards, kpiRetardsNote, String.valueOf(retards),
                    retards == 0 ? "Aucun retard" : "pénalité(s) de retard",
                    retards == 0 ? Kpi.Etat.NORMAL : Kpi.Etat.ALERTE);

            // Synthèse des retards de CETTE tontine : en rouge s'il y en a, discrète sinon.
            labelRetards.setText(HistoriqueRegles.resumeRetards(transactions));
            labelRetards.setStyle(HistoriqueRegles.nombreRetards(transactions) > 0
                    ? "-fx-text-fill: #c0392b; -fx-font-weight: bold;"
                    : "-fx-text-fill: #68716c;");

            // Seules les actions qui s'appliquent à CETTE tontine sont proposées.
            actualiserActionsSelonTontine(selection, u.getId());
        } catch (SQLException e) {
            afficherErreur("Erreur lors du chargement des transactions : " + e.getMessage());
        }
    }

    /** Aucune tontine sélectionnée : aucune action dépendant du type de tontine n'est proposée. */
    private void masquerActionsSelonType() {
        montrer(boutonDemanderPret, false);
        montrer(boutonRembourserPret, false);
        montrer(boutonTourAnticipe, false);
        labelAideBoutons.setText("");
    }

    /**
     * N'affiche que les actions qui s'appliquent au TYPE de la tontine sélectionnée (prêts : Fonds commun ;
     * tour anticipé : Tour de rôle). Pour le tour anticipé, qui s'applique mais n'est pas toujours possible,
     * le bouton reste visible mais grisé, et la raison est écrite à côté : le membre comprend pourquoi, au lieu
     * de découvrir le refus en cliquant. (Une infobulle ne s'afficherait pas : un bouton grisé ne reçoit pas la souris.)
     */
    private void actualiserActionsSelonTontine(Tontine selection, String membreId) throws SQLException {
        ActionsRegles.Actions actions = ActionsRegles.pour(selection);
        montrer(boutonDemanderPret, actions.demanderPret());
        montrer(boutonRembourserPret, actions.rembourserPret());
        montrer(boutonTourAnticipe, actions.tourAnticipe());

        labelAideBoutons.setText("");
        if (actions.tourAnticipe()) {
            try {
                membreService.verifierPeutDemanderTourAnticipe(selection, membreId);
                boutonTourAnticipe.setDisable(false);
            } catch (IllegalStateException e) {
                boutonTourAnticipe.setDisable(true);
                labelAideBoutons.setText("Tour anticipé indisponible : " + e.getMessage());
            }
        }
    }

    private static void montrer(Button bouton, boolean visible) {
        bouton.setVisible(visible);
        bouton.setManaged(visible);
    }

    private void rafraichir() {
        Utilisateur u = SessionContext.getUtilisateurConnecte();
        try {
            // Recharger la liste efface la sélection : on retient la tontine choisie
            // pour la resélectionner, sinon l'historique se viderait après chaque action.
            Tontine selectionnee = listeTontines.getSelectionModel().getSelectedItem();
            String idSelectionne = selectionnee != null ? selectionnee.getId() : null;

            List<Tontine> tontines = membreService.mesTontines(u.getId());
            listeTontines.setItems(FXCollections.observableArrayList(tontines));

            Tontine aSelectionner = tontines.stream()
                    .filter(t -> t.getId().equals(idSelectionne))
                    .findFirst()
                    .orElse(tontines.isEmpty() ? null : tontines.get(0));
            if (aSelectionner != null) {
                listeTontines.getSelectionModel().select(aSelectionner);
            }
            rafraichirHistorique();

            List<Notification> notifications = membreService.mesNotifications(u.getId());
            listeNotifications.setItems(FXCollections.observableArrayList(
                    notifications.stream()
                            .map(n -> "[" + n.getDateEnvoi().format(formatDate) + "] " + n.getMessage())
                            .toList()));
            Kpi.definir(kpiNotifs, kpiNotifsNote, String.valueOf(notifications.size()),
                    notifications.isEmpty() ? "Aucune notification" : "message(s) reçu(s)", Kpi.Etat.NORMAL);
        } catch (SQLException e) {
            afficherErreur("Erreur lors du chargement des données : " + e.getMessage());
        }
    }

    @FXML
    private void ouvrirCotisation() {
        Tontine selection = listeTontines.getSelectionModel().getSelectedItem();
        if (selection == null) {
            afficherErreur("Sélectionnez d'abord une tontine dans la liste.");
            return;
        }

        BigDecimal montant;
        if (CotisationRegles.montantImpose(selection)) {
            // Tour de rôle / Tirage au sort : montant fixe identique pour tous,
            // affiché en lecture seule (pas de champ de saisie).
            montant = selection.getMontantCotisation();
            boolean valide = FormulaireLectureSeule.confirmer(
                    "Cotiser",
                    "Cotisation sur " + selection.getNom(),
                    "Le montant est fixé par la tontine (identique pour tous les membres) "
                            + "et ne peut pas être modifié.",
                    "Tontine", selection.getNom(),
                    "Montant (FCFA)", montant.toPlainString());
            if (!valide) return;
        } else {
            // Fonds commun : montant libre, dans la fourchette fixée par le gérant.
            TextInputDialog dialogMontant = new TextInputDialog(selection.getMontantCotisation().toString());
            dialogMontant.setTitle("Cotiser");
            dialogMontant.setHeaderText("Cotisation sur " + selection.getNom() + "\nMontant autorisé : "
                    + MontantCotisationRegles.libelle(selection.getMontantMin(), selection.getMontantMax(),
                            selection.getMontantCotisation()));
            dialogMontant.setContentText("Montant (FCFA) :");
            var resultatMontant = dialogMontant.showAndWait();
            if (resultatMontant.isEmpty()) return;

            try {
                montant = new BigDecimal(resultatMontant.get());
            } catch (NumberFormatException e) {
                afficherErreur("Montant invalide.");
                return;
            }
        }

        // Le membre cotise lui-même uniquement via un moyen de paiement en
        // ligne (Wave, Orange Money) : les espèces exigent une remise physique
        // et ne peuvent être saisies que par le gérant (saisirCotisationEspeces).
        ChoiceDialog<String> dialogMode = new ChoiceDialog<>("Wave", List.of("Wave", "Orange Money"));
        dialogMode.setTitle("Mode de paiement");
        dialogMode.setHeaderText("Comment souhaitez-vous cotiser en ligne ?");
        var resultatMode = dialogMode.showAndWait();
        if (resultatMode.isEmpty()) return;

        ModePaiement mode = "Wave".equals(resultatMode.get())
                ? new ModePaiement.Wave(SessionContext.getUtilisateurConnecte().getTel())
                : new ModePaiement.OrangeMoney(SessionContext.getUtilisateurConnecte().getTel());

        try {
            membreService.cotiser(selection, SessionContext.getUtilisateurConnecte().getId(), montant, mode);
            afficherInfo("Cotisation enregistrée via " + mode.getLibelle() + " (réf. " + mode.getReference() + ").");
            rafraichir();
        } catch (SQLException | IllegalStateException | IllegalArgumentException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void ouvrirDemandePret() {
        Tontine selection = listeTontines.getSelectionModel().getSelectedItem();
        if (selection == null) {
            afficherErreur("Sélectionnez d'abord une tontine dans la liste « Mes tontines ».");
            return;
        }
        if (!(selection instanceof TontineFondsCommun fondsCommun)) {
            afficherErreur("Le prêt n'est disponible que pour une tontine de type Fonds commun.");
            return;
        }

        TextInputDialog dialogMontant = new TextInputDialog();
        dialogMontant.setTitle("Demander un prêt");
        dialogMontant.setHeaderText("Prêt sur " + fondsCommun.getNom()
                + " (durée max : " + fondsCommun.getDureeMaxPret() + " jours)");
        dialogMontant.setContentText("Montant souhaité (FCFA) :");
        var resultat = dialogMontant.showAndWait();
        if (resultat.isEmpty()) return;

        try {
            BigDecimal montant = new BigDecimal(resultat.get());
            membreService.demanderPret(fondsCommun, SessionContext.getUtilisateurConnecte().getId(),
                    montant, fondsCommun.getDureeMaxPret(), "Demande via l'application");
            afficherInfo("Demande de prêt envoyée, en attente de validation.");
        } catch (NumberFormatException e) {
            afficherErreur("Montant invalide.");
        } catch (SQLException | IllegalArgumentException | IllegalStateException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void ouvrirDemandeTourAnticipe() {
        Tontine selection = listeTontines.getSelectionModel().getSelectedItem();
        if (selection == null) {
            afficherErreur("Sélectionnez d'abord une tontine dans la liste « Mes tontines ».");
            return;
        }
        Utilisateur u = SessionContext.getUtilisateurConnecte();

        // Vérifié AVANT le formulaire : inutile de demander un motif pour refuser derrière.
        try {
            membreService.verifierPeutDemanderTourAnticipe(selection, u.getId());
        } catch (IllegalStateException e) {
            afficherErreur(e.getMessage());
            return;
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
            return;
        }

        TextInputDialog dialogue = new TextInputDialog();
        dialogue.setTitle("Tour anticipé");
        dialogue.setHeaderText("Demander à recevoir plus tôt dans « " + selection.getNom() + " »\n"
                + "Si le gérant accorde, vous serez le prochain bénéficiaire.");
        dialogue.setContentText("Motif :");
        var resultat = dialogue.showAndWait();
        if (resultat.isEmpty()) return;

        try {
            membreService.demanderTourAnticipe((TontineTourDeRole) selection, u.getId(), resultat.get());
            afficherInfo("Demande envoyée au gérant. Vous serez prévenu de sa décision.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            afficherErreur(e.getMessage());
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void ouvrirRemboursementPret() {
        Tontine selection = listeTontines.getSelectionModel().getSelectedItem();
        if (selection == null) {
            afficherErreur("Sélectionnez d'abord une tontine dans la liste « Mes tontines ».");
            return;
        }
        if (!(selection instanceof TontineFondsCommun fondsCommun)) {
            afficherErreur("Le remboursement n'existe que pour une tontine de type Fonds commun.");
            return;
        }
        Utilisateur u = SessionContext.getUtilisateurConnecte();

        try {
            List<Pret> pretsValides = membreService.mesPrets(u.getId()).stream()
                    .filter(p -> p.getTontineId().equals(fondsCommun.getId()))
                    .filter(p -> p.getStatut() == Enums.StatutPret.VALIDE)
                    .toList();
            if (pretsValides.isEmpty()) {
                afficherInfo("Vous n'avez aucun prêt validé à rembourser sur cette tontine.");
                return;
            }

            ChoiceDialog<Pret> dialogPret = new ChoiceDialog<>(pretsValides.get(0), pretsValides);
            dialogPret.setTitle("Rembourser un prêt");
            dialogPret.setHeaderText("Sélectionnez le prêt à rembourser");
            var resultatPret = dialogPret.showAndWait();
            if (resultatPret.isEmpty()) return;
            Pret pret = resultatPret.get();

            BigDecimal interet = pret.getMontant().multiply(fondsCommun.getTauxInteret()).divide(new BigDecimal("100"));
            BigDecimal total = pret.getMontant().add(interet);

            Alert confirmation = new Alert(AlertType.CONFIRMATION,
                    "Rembourser " + total + " FCFA (capital " + pret.getMontant()
                            + " + intérêt " + interet + " FCFA au taux de " + fondsCommun.getTauxInteret() + "%) ?");
            var reponse = confirmation.showAndWait();
            if (reponse.isEmpty() || reponse.get() != ButtonType.OK) return;

            ChoiceDialog<String> dialogMode = new ChoiceDialog<>("Wave", List.of("Wave", "Orange Money"));
            dialogMode.setTitle("Mode de paiement");
            dialogMode.setHeaderText("Comment souhaitez-vous rembourser ?");
            var resultatMode = dialogMode.showAndWait();
            if (resultatMode.isEmpty()) return;

            ModePaiement mode = "Wave".equals(resultatMode.get())
                    ? new ModePaiement.Wave(u.getTel())
                    : new ModePaiement.OrangeMoney(u.getTel());

            membreService.rembourserPret(pret, fondsCommun, mode);
            afficherInfo("Remboursement de " + total + " FCFA enregistré.");
            rafraichir();
        } catch (SQLException | IllegalStateException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void ouvrirDemandeAdhesion() {
        Utilisateur u = SessionContext.getUtilisateurConnecte();

        List<Tontine> disponibles;
        try {
            disponibles = membreService.tontinesDisponiblesPourRejoindre(u.getId());
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
            return;
        }
        if (disponibles.isEmpty()) {
            afficherInfo("Aucune autre tontine disponible pour l'instant — vous êtes déjà membre de toutes celles qui existent.");
            return;
        }

        // Liste déroulante par NOM de tontine (Tontine.toString() affiche "nom (type)"),
        // plus compréhensible qu'un identifiant technique à saisir à la main.
        ChoiceDialog<Tontine> dialogTontine = new ChoiceDialog<>(disponibles.get(0), disponibles);
        dialogTontine.setTitle("Rejoindre une tontine");
        dialogTontine.setHeaderText("Choisissez la tontine à rejoindre");
        var resultatTontine = dialogTontine.showAndWait();
        if (resultatTontine.isEmpty()) return;
        Tontine tontineChoisie = resultatTontine.get();

        TextInputDialog dialogPass = new TextInputDialog();
        dialogPass.setTitle("Confirmation");
        dialogPass.setHeaderText("Confirmez votre mot de passe pour envoyer la demande sur « " + tontineChoisie.getNom() + " »");
        dialogPass.setContentText("Mot de passe :");
        var resultatPass = dialogPass.showAndWait();
        if (resultatPass.isEmpty()) return;

        try {
            membreService.soumettreDemandeAdhesion(u.getId(), tontineChoisie.getId(), resultatPass.get(), null, true);
            afficherInfo("Demande d'adhésion envoyée au gérant de « " + tontineChoisie.getNom() + " ».");
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            afficherErreur(e.getMessage());
        }
    }

    @FXML
    private void ouvrirSoldeDistribution() {
        Tontine selection = listeTontines.getSelectionModel().getSelectedItem();
        if (selection == null) {
            afficherErreur("Sélectionnez d'abord une tontine dans la liste.");
            return;
        }
        Utilisateur u = SessionContext.getUtilisateurConnecte();

        VBox contenu = new VBox(10);
        contenu.setPrefWidth(420);

        try {
            Adhesion adhesion = membreService.consulterSolde(selection.getId(), u.getId());
            contenu.getChildren().add(new Label("Solde actuel : " + adhesion.getSolde() + " FCFA"));
            contenu.getChildren().add(new Label("Total cotisé : " + adhesion.getTotalCotise()
                    + " FCFA (" + adhesion.getCotisationsFaites() + " cotisations)"));

            java.time.LocalDate dateFin = membreService.calculerDateFinPrevue(selection);
            String libelleDuree = selection instanceof TontineFondsCommun
                    ? "Durée : " + selection.getDureeLabel() + " — fin prévue le " + dateFin
                    : "Durée de la série : " + selection.getDureeLabel()
                        + " (" + selection.getNbMembresMax() + " cycles, " + selection.getFrequence()
                        + ") — fin prévue le " + dateFin;
            contenu.getChildren().add(new Label(libelleDuree));

            Separator sep1 = new Separator();
            contenu.getChildren().add(sep1);

            if (selection instanceof TontineTourDeRole tourDeRole) {
                contenu.getChildren().add(new Label("Type : Tour de rôle"));
                contenu.getChildren().add(new Label("Bénéficiaire actuel : "
                        + membreService.consulterBeneficiaireActuel(tourDeRole)));
                contenu.getChildren().add(new Label("Ordre complet du tour (visible par tout le groupe) :"));
                for (String ligne : membreService.consulterOrdreTourComplet(tourDeRole)) {
                    contenu.getChildren().add(new Label("  " + ligne));
                }
            } else if (selection instanceof TontineTirageAuSort) {
                contenu.getChildren().add(new Label("Type : Tirage au sort"));
                List<Tirage> tirages = membreService.consulterTirages(selection.getId());
                if (tirages.isEmpty()) {
                    contenu.getChildren().add(new Label("Aucun tirage effectué pour l'instant."));
                } else {
                    contenu.getChildren().add(new Label("Historique des tirages :"));
                    for (Tirage t : tirages) {
                        String qui = t.getBeneficiaireId().equals(u.getId()) ? " (c'était vous ! 🎉)" : "";
                        contenu.getChildren().add(new Label("  Semaine " + t.getSemaine() + " — "
                                + t.getDate() + " — bénéficiaire : " + membreService.nomUtilisateur(t.getBeneficiaireId()) + qui));
                    }
                }
            } else if (selection instanceof TontineFondsCommun fondsCommun) {
                contenu.getChildren().add(new Label("Type : Fonds commun"));
                contenu.getChildren().add(new Label("Taux d'intérêt : " + fondsCommun.getTauxInteret() + " %"));
            }

            Separator sep2 = new Separator();
            contenu.getChildren().add(sep2);

            List<Distribution> distributions = membreService.consulterDistributions(selection.getId());
            if (distributions.isEmpty()) {
                contenu.getChildren().add(new Label("Aucune distribution effectuée pour l'instant sur cette tontine."));
            } else {
                contenu.getChildren().add(new Label("Historique des distributions (tous les membres) :"));
                for (Distribution d : distributions) {
                    String ligne = "  " + d.getDate() + " — " + d.getMontantTotal() + " FCFA";
                    if (d.getBeneficiaireId() != null) {
                        String nomBeneficiaire = membreService.nomUtilisateur(d.getBeneficiaireId());
                        String qui = d.getBeneficiaireId().equals(u.getId()) ? " (c'était vous !)" : "";
                        ligne += " — bénéficiaire : " + nomBeneficiaire + qui;
                    }
                    contenu.getChildren().add(new Label(ligne));
                }
            }
        } catch (SQLException | IllegalStateException e) {
            afficherErreur("Erreur : " + e.getMessage());
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Solde & distribution — " + selection.getNom());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setContent(new ScrollPane(contenu));
        dialog.showAndWait();
    }

    @FXML
    private void changerEspace() {
        NavigationEspaces.changerEspace(sn.naat.model.Enums.Espace.MEMBRE);
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

    private void afficherErreur(String message) {
        Dialogues.erreur(message);
    }

    private void afficherInfo(String message) {
        new Alert(AlertType.INFORMATION, message).showAndWait();
    }
}
