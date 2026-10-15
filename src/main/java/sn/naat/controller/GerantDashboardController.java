package sn.naat.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import sn.naat.MainApp;
import sn.naat.model.*;
import sn.naat.service.CotisationRegles;
import sn.naat.service.GerantService;
import sn.naat.service.MontantCotisationRegles;
import sn.naat.service.HistoriqueRegles;
import sn.naat.service.IndicateursRegles;
import sn.naat.util.SessionContext;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GerantDashboardController {

    @FXML private Label labelBienvenue;
    @FXML private Button boutonChangerEspace;
    @FXML private ListView<Tontine> listeTontines;
    @FXML private Label labelDureeTontine;
    @FXML private ComboBox<Cycle> comboCycles;
    @FXML private Button boutonNouveauCycle;
    @FXML private Button boutonAvancerTour;
    @FXML private Button boutonLancerTirage;
    @FXML private Button boutonCloturerCycle;
    @FXML private Button boutonNouvelleSerie;
    @FXML private ListView<String> listeNotifications;

    @FXML private TableView<DemandeAdhesion> tableDemandesAdhesion;
    @FXML private TableColumn<DemandeAdhesion, String> colDaNom;
    @FXML private TableColumn<DemandeAdhesion, String> colDaTel;
    @FXML private TableColumn<DemandeAdhesion, String> colDaDate;

    @FXML private TableView<Pret> tablePrets;
    @FXML private TableColumn<Pret, String> colPretMembre;
    @FXML private TableColumn<Pret, String> colPretMontant;
    @FXML private TableColumn<Pret, String> colPretDuree;
    @FXML private TableColumn<Pret, String> colPretMotif;

    @FXML private TableView<DemandeTourAnticipe> tableToursAnticipes;
    @FXML private TableColumn<DemandeTourAnticipe, String> colTaMembre;
    @FXML private TableColumn<DemandeTourAnticipe, String> colTaMotif;
    @FXML private TableColumn<DemandeTourAnticipe, String> colTaDate;

    @FXML private TableView<Adhesion> tableMembres;
    @FXML private TableColumn<Adhesion, Void> colMembreActions;
    /** Vrai si la tontine sélectionnée a un cycle en cours (condition pour proposer « Saisir cotisation »). */
    private boolean cycleEnCours = false;
    @FXML private TableColumn<Adhesion, String> colMembreNom;
    @FXML private TableColumn<Adhesion, String> colMembreSolde;
    @FXML private TableColumn<Adhesion, String> colMembreStatut;

    @FXML private javafx.scene.control.Label kpiMembres;
    @FXML private javafx.scene.control.Label kpiMembresNote;
    @FXML private javafx.scene.control.Label kpiDemandes;
    @FXML private javafx.scene.control.Label kpiDemandesNote;
    @FXML private javafx.scene.control.Label kpiCycle;
    @FXML private javafx.scene.control.Label kpiCycleNote;
    @FXML private javafx.scene.control.Label kpiCotisation;
    @FXML private javafx.scene.control.Label kpiCotisationNote;

    private final GerantService gerantService = new GerantService();
    private final DateTimeFormatter formatDate = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        Utilisateur u = SessionContext.getUtilisateurConnecte();
        labelBienvenue.setText("Bienvenue, " + u.getNom());
        NavigationEspaces.configurer(boutonChangerEspace);

        colDaNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNom()));
        colDaTel.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTel()));
        colDaDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDateEnvoi().format(formatDate)));

        colPretMembre.setCellValueFactory(d -> {
            try {
                return new SimpleStringProperty(gerantService.nomDuMembre(d.getValue().getMembreId()));
            } catch (SQLException e) {
                return new SimpleStringProperty("?");
            }
        });
        colPretMontant.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMontant() + " FCFA"));
        colPretDuree.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getDureeJours())));
        colPretMotif.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMotif()));

        colTaMembre.setCellValueFactory(d -> {
            try {
                return new SimpleStringProperty(gerantService.nomDuMembre(d.getValue().getMembreId()));
            } catch (SQLException e) {
                return new SimpleStringProperty("?");
            }
        });
        colTaMotif.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMotif()));
        colTaDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDateEnvoi().format(formatDate)));

        colMembreNom.setCellValueFactory(d -> {
            try {
                return new SimpleStringProperty(gerantService.nomDuMembre(d.getValue().getMembreId()));
            } catch (SQLException e) {
                return new SimpleStringProperty("?");
            }
        });
        colMembreSolde.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSolde() + " FCFA"));
        colMembreStatut.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatut().name()));

        // Statut : pastille verte (actif) ou grise (inactif).
        colMembreStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) { setGraphic(null); setText(null); return; }
                boolean actif = "ACTIF".equals(statut);
                Label pastille = new Label(actif ? "Actif" : "Inactif");
                pastille.getStyleClass().addAll("badge", actif ? "badge-succes" : "badge-neutre");
                setGraphic(pastille);
                setText(null);
            }
        });
        // Actions : boutons au niveau de chaque membre (seule l'action possible est proposée).
        colMembreActions.setCellFactory(col -> new TableCell<Adhesion, Void>() {
            private final Button boutonCotisation = new Button("Saisir cotisation");
            private final Button boutonHistorique = new Button("Historique");
            private final Button boutonStatut = new Button();
            private final HBox boite = new HBox(8, boutonCotisation, boutonHistorique, boutonStatut);
            private final Tooltip aide = new Tooltip();
            {
                boite.setAlignment(Pos.CENTER_LEFT);
                for (Button b : List.of(boutonCotisation, boutonHistorique, boutonStatut)) b.getStyleClass().add("bouton-compact");
                boutonCotisation.getStyleClass().add("bouton-principal");
                boutonCotisation.setOnAction(e -> saisirCotisationEspeces(getTableRow().getItem()));
                boutonHistorique.setOnAction(e -> voirHistoriqueMembre(getTableRow().getItem()));
                boutonStatut.setOnAction(e -> {
                    Adhesion a = getTableRow().getItem();
                    if (a.getStatut() == sn.naat.model.Enums.StatutAdhesion.ACTIF) desactiverMembre(a);
                    else reactiverMembre(a);
                });
                Tooltip.install(boite, aide);
            }
            @Override protected void updateItem(Void rien, boolean empty) {
                super.updateItem(rien, empty);
                Adhesion a = (empty || getTableRow() == null) ? null : getTableRow().getItem();
                if (a == null) { setGraphic(null); return; }
                boolean actif = a.getStatut() == sn.naat.model.Enums.StatutAdhesion.ACTIF;
                boutonStatut.setText(actif ? "Désactiver" : "Réactiver");
                boutonStatut.getStyleClass().remove("bouton-danger");
                if (actif) boutonStatut.getStyleClass().add("bouton-danger");
                boolean peutCotiser = actif && cycleEnCours;
                boutonCotisation.setDisable(!peutCotiser);
                aide.setText(peutCotiser ? "Enregistrer une cotisation reçue en espèces"
                        : !actif ? "Membre désactivé : aucune cotisation possible"
                                 : "Aucun cycle en cours : démarrez un cycle pour permettre les cotisations");
                setGraphic(boite);
            }
        });

        tableMembres.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Adhesion> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    voirHistoriqueMembre(row.getItem());
                }
            });
            return row;
        });

        listeTontines.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> rafraichirOnglets());

        rafraichirTontines();
    }

    private void rafraichirTontines() {
        Utilisateur u = SessionContext.getUtilisateurConnecte();
        try {
            List<Tontine> tontines = gerantService.mesTontinesGerees(u.getId());
            listeTontines.setItems(FXCollections.observableArrayList(tontines));
            if (!tontines.isEmpty()) listeTontines.getSelectionModel().selectFirst();
        } catch (SQLException e) {
            afficherErreur("Erreur de chargement : " + e.getMessage());
        }
    }

    private void rafraichirOnglets() {
        Tontine t = listeTontines.getSelectionModel().getSelectedItem();
        if (t == null) return;

        java.time.LocalDate dateFin = gerantService.calculerDateFinPrevue(t);
        labelDureeTontine.setText(t instanceof TontineFondsCommun
                ? "Durée : " + t.getDureeLabel() + " — fin prévue le " + dateFin
                : "Durée : " + t.getDureeLabel() + " (" + t.getNbMembresMax() + " cycles) — fin prévue le " + dateFin);

        try {
            // Série en cours, statut et progression (membres inscrits ou membres servis) : le gérant voit
            // pourquoi la tontine n'est pas encore démarrée ou clôturée, et quand demander une nouvelle série.
            labelDureeTontine.setText(labelDureeTontine.getText() + "\n" + gerantService.resumeSerie(t));

            tableDemandesAdhesion.setItems(FXCollections.observableArrayList(gerantService.demandesEnAttente(t.getId())));
            tableMembres.setItems(FXCollections.observableArrayList(gerantService.membresDeLaTontine(t.getId())));

            if (t instanceof TontineFondsCommun) {
                tablePrets.setItems(FXCollections.observableArrayList(gerantService.pretsEnAttente(t.getId())));
            } else {
                tablePrets.setItems(FXCollections.observableArrayList());
            }

            // Les demandes de tour anticipé n'existent que pour une tontine Tour de rôle.
            if (t instanceof TontineTourDeRole) {
                tableToursAnticipes.setItems(FXCollections.observableArrayList(
                        gerantService.demandesTourAnticipeEnAttente(t.getId())));
            } else {
                tableToursAnticipes.setItems(FXCollections.observableArrayList());
            }

            boutonAvancerTour.setDisable(!(t instanceof TontineTourDeRole));
            boutonLancerTirage.setDisable(!(t instanceof TontineTirageAuSort));

            List<Cycle> cycles = gerantService.cyclesDeLaTontine(t.getId());
            comboCycles.setItems(FXCollections.observableArrayList(cycles));
            comboCycles.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(Cycle c) {
                    return c == null ? "" : "Cycle n°" + c.getNumero() + " (" + c.getStatut() + ")";
                }
                @Override public Cycle fromString(String s) { return null; }
            });
            cycles.stream().filter(c -> c.getStatut() == sn.naat.model.Enums.StatutCycle.EN_COURS)
                    .findFirst().ifPresent(comboCycles.getSelectionModel()::select);

            cycleEnCours = cycles.stream().anyMatch(c -> c.getStatut() == sn.naat.model.Enums.StatutCycle.EN_COURS);
            tableMembres.refresh();
            mettreAJourIndicateurs(t, cycles);

            List<Notification> notifications = gerantService.mesNotifications(SessionContext.getUtilisateurConnecte().getId());
            listeNotifications.setItems(FXCollections.observableArrayList(
                    notifications.stream()
                            .map(n -> "[" + n.getDateEnvoi().format(formatDate) + "] " + n.getMessage())
                            .toList()));
        } catch (SQLException e) {
            afficherErreur("Erreur de chargement : " + e.getMessage());
        }
    }

    private void mettreAJourIndicateurs(Tontine t, List<Cycle> cycles) {
        long actifs = IndicateursRegles.membresActifs(tableMembres.getItems());
        Kpi.definir(kpiMembres, kpiMembresNote, actifs + " / " + t.getNbMembresMax(),
                "membres actifs / prévus", Kpi.Etat.NORMAL);

        int adhesions = tableDemandesAdhesion.getItems().size();
        int prets = tablePrets.getItems().size();
        int tours = tableToursAnticipes.getItems().size();
        int total = adhesions + prets + tours;
        Kpi.definir(kpiDemandes, kpiDemandesNote, String.valueOf(total),
                total == 0 ? "Rien en attente"
                        : adhesions + " adhésion(s), " + prets + " prêt(s), " + tours + " tour(s) anticipé(s)",
                total == 0 ? Kpi.Etat.NORMAL : Kpi.Etat.ATTENTE);

        var cycleEnCours = cycles.stream()
                .filter(c -> c.getStatut() == sn.naat.model.Enums.StatutCycle.EN_COURS).findFirst();
        if (cycleEnCours.isPresent()) {
            Cycle c = cycleEnCours.get();
            Kpi.definir(kpiCycle, kpiCycleNote, "n° " + c.getNumero(), "démarré le " + c.getDateDebut(), Kpi.Etat.NORMAL);
        } else {
            Kpi.definir(kpiCycle, kpiCycleNote, "Aucun", "démarrez un cycle pour permettre les cotisations", Kpi.Etat.ATTENTE);
        }

        Kpi.definir(kpiCotisation, kpiCotisationNote,
                MontantCotisationRegles.libelle(t.getMontantMin(), t.getMontantMax(), t.getMontantCotisation()),
                t instanceof TontineFondsCommun ? "fourchette (montant libre)" : "montant fixe pour tous",
                Kpi.Etat.NORMAL);
    }

    @FXML
    private void validerAdhesion() {
        traiterDemandeAdhesion(true);
    }

    @FXML
    private void refuserAdhesion() {
        traiterDemandeAdhesion(false);
    }

    private void traiterDemandeAdhesion(boolean accepte) {
        DemandeAdhesion demande = tableDemandesAdhesion.getSelectionModel().getSelectedItem();
        if (demande == null) { afficherErreur("Sélectionnez une demande."); return; }
        try {
            gerantService.validerDemandeAdhesion(demande, accepte ? "validee" : "refusee");
            afficherInfo(accepte ? "Adhésion validée." : "Demande refusée.");
            rafraichirOnglets();
        } catch (IllegalStateException e) {
            afficherErreur(e.getMessage()); // ex. tontine déjà complète
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void accorderTourAnticipe() {
        traiterTourAnticipe(true);
    }

    @FXML
    private void refuserTourAnticipe() {
        traiterTourAnticipe(false);
    }

    private void traiterTourAnticipe(boolean accorde) {
        DemandeTourAnticipe demande = tableToursAnticipes.getSelectionModel().getSelectedItem();
        if (demande == null) { afficherErreur("Sélectionnez une demande."); return; }
        try {
            gerantService.traiterDemandeTourAnticipe(demande, accorde ? "validee" : "refusee");
            afficherInfo(accorde
                    ? "Tour anticipé accordé : le membre sera le prochain bénéficiaire."
                    : "Demande refusée.");
            rafraichirOnglets();
        } catch (IllegalStateException e) {
            afficherErreur(e.getMessage()); // ex. membre désactivé ou déjà servi depuis la demande
            rafraichirOnglets();
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void validerPret() {
        traiterPret(true);
    }

    @FXML
    private void refuserPret() {
        traiterPret(false);
    }

    private void traiterPret(boolean approuve) {
        Pret pret = tablePrets.getSelectionModel().getSelectedItem();
        Tontine t = listeTontines.getSelectionModel().getSelectedItem();
        if (pret == null || !(t instanceof TontineFondsCommun fc)) {
            afficherErreur("Sélectionnez un prêt sur une tontine de type Fonds commun.");
            return;
        }
        try {
            gerantService.validerPret(pret, fc, SessionContext.getUtilisateurConnecte().getId(), approuve ? "valide" : "refuse");
            afficherInfo(approuve ? "Prêt validé." : "Prêt refusé.");
            rafraichirOnglets();
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        } catch (IllegalStateException e) {
            afficherErreur(e.getMessage()); // règle anti-conflit gérant/emprunteur
        }
    }

    private void voirHistoriqueMembre(Adhesion adhesion) {
        if (adhesion == null) return;

        try {
            String nom = gerantService.nomDuMembre(adhesion.getMembreId());
            List<Transaction> transactions = gerantService.consulterHistoriqueMembre(
                    adhesion.getTontineId(), adhesion.getMembreId());

            // Un retard est une ligne de type PENALITE : elle est écrite à la clôture du
            // cycle pour chaque membre qui n'avait pas cotisé (cf. GerantService.cloturerCycle).
            TableView<Transaction> table = new TableView<>();
            TableColumn<Transaction, String> colDate = new TableColumn<>("Date");
            colDate.setPrefWidth(130);
            colDate.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
            TableColumn<Transaction, String> colType = new TableColumn<>("Type");
            colType.setPrefWidth(130);
            colType.setCellValueFactory(d -> new SimpleStringProperty(HistoriqueRegles.libelleType(d.getValue())));
            TableColumn<Transaction, String> colMontant = new TableColumn<>("Montant");
            colMontant.setPrefWidth(100);
            colMontant.setCellValueFactory(d -> new SimpleStringProperty(HistoriqueRegles.libelleMontant(d.getValue())));
            TableColumn<Transaction, String> colDetail = new TableColumn<>("Détail");
            colDetail.setPrefWidth(280);
            colDetail.setCellValueFactory(d -> new SimpleStringProperty(HistoriqueRegles.libelleDetail(d.getValue())));
            TableColumn<Transaction, String> colStatut = new TableColumn<>("Statut");
            colStatut.setPrefWidth(80);
            colStatut.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatut().name()));
            table.getColumns().addAll(colDate, colType, colMontant, colDetail, colStatut);

            // Les lignes de retard sont surlignées en rouge pour les repérer d'un coup d'œil.
            PresentationTransactions.surlignerRetards(table);

            table.setItems(FXCollections.observableArrayList(transactions));
            table.setPrefSize(740, 340);
            table.setPlaceholder(new Label("Aucune transaction pour ce membre sur cette tontine."));

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Historique de " + nom);
            dialog.setHeaderText("Solde actuel : " + adhesion.getSolde() + " FCFA — "
                    + adhesion.getCotisationsFaites() + " cotisation(s) au total\n"
                    + HistoriqueRegles.resumeRetards(transactions));
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().setContent(table);
            dialog.showAndWait();
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    private void saisirCotisationEspeces(Adhesion adhesion) {
        if (adhesion == null) return;

        Tontine tontine = listeTontines.getSelectionModel().getSelectedItem();
        if (tontine == null) { afficherErreur("Sélectionnez une tontine."); return; }

        try {
            BigDecimal montant;
            if (CotisationRegles.montantImpose(tontine)) {
                // Tour de rôle / Tirage au sort : montant fixe identique pour tous,
                // affiché en lecture seule (pas de champ de saisie).
                montant = tontine.getMontantCotisation();
                String nomMembre = gerantService.nomDuMembre(adhesion.getMembreId());
                boolean valide = FormulaireLectureSeule.confirmer(
                        "Cotisation en espèces",
                        "Enregistrer la réception d'une cotisation en espèces",
                        "Le montant est fixé par la tontine (identique pour tous les membres) "
                                + "et ne peut pas être modifié.",
                        "Membre", nomMembre,
                        "Tontine", tontine.getNom(),
                        "Montant (FCFA)", montant.toPlainString());
                if (!valide) return;
            } else {
                // Fonds commun : montant libre.
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Cotisation en espèces");
                dialog.setHeaderText("Montant reçu en espèces\nMontant autorisé : "
                        + MontantCotisationRegles.libelle(tontine.getMontantMin(), tontine.getMontantMax(),
                                tontine.getMontantCotisation()));
                dialog.setContentText("Montant (FCFA) :");
                var resultat = dialog.showAndWait();
                if (resultat.isEmpty()) return;
                montant = new BigDecimal(resultat.get());
            }

            gerantService.saisirCotisationEspeces(adhesion, montant);
            afficherInfo("Cotisation espèces enregistrée.");
            rafraichirOnglets();
        } catch (NumberFormatException e) {
            afficherErreur("Montant invalide.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            afficherErreur(e.getMessage());
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    private String nomMembreAffiche(Adhesion adhesion) {
        try {
            return gerantService.nomDuMembre(adhesion.getMembreId());
        } catch (SQLException e) {
            return "ce membre";
        }
    }

    private void desactiverMembre(Adhesion adhesion) {
        if (adhesion == null) return;
        Alert confirmation = new Alert(AlertType.CONFIRMATION,
                "Désactiver « " + nomMembreAffiche(adhesion) + " » ? Il ne pourra plus cotiser, demander de prêt "
                        + "ni de tour anticipé (il pourra toujours rembourser un prêt en cours).",
                ButtonType.OK, ButtonType.CANCEL);
        confirmation.setHeaderText("Désactiver ce membre");
        if (confirmation.showAndWait().filter(b -> b == ButtonType.OK).isEmpty()) return;
        try {
            gerantService.desactiverMembre(adhesion.getId());
            afficherInfo("Membre désactivé.");
            rafraichirOnglets();
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    private void reactiverMembre(Adhesion adhesion) {
        if (adhesion == null) return;
        try {
            gerantService.reactiverMembre(adhesion);
            afficherInfo("Membre réactivé.");
            rafraichirOnglets();
        } catch (IllegalStateException e) {
            afficherErreur(e.getMessage()); // ex. déjà actif, tontine terminée ou complète
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void avancerTour() {
        Tontine t = listeTontines.getSelectionModel().getSelectedItem();
        if (!(t instanceof TontineTourDeRole tr)) { afficherErreur("Cette action n'existe que pour Tour de rôle."); return; }
        try {
            String beneficiaireId = gerantService.avancerTour(tr);
            afficherInfo(beneficiaireId == null
                    ? "Aucun ordre de tour défini pour cette tontine."
                    : "Nouveau bénéficiaire du tour : " + gerantService.nomDuMembre(beneficiaireId));
        } catch (IllegalStateException e) {
            afficherErreur(e.getMessage());
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void lancerTirage() {
        Tontine t = listeTontines.getSelectionModel().getSelectedItem();
        if (!(t instanceof TontineTirageAuSort tirageAuSort)) {
            afficherErreur("Cette action n'existe que pour Tirage au sort.");
            return;
        }
        try {
            List<String> eligibles = gerantService.membresEligiblesPourTirage(t.getId());
            // Le numéro de semaine devrait normalement venir du cycle en cours ;
            // ici on prend le nombre de tirages déjà effectués + 1.
            int prochaineSemaine = gerantService.prochainNumeroSemaine(t.getId());
            Tirage tirage = gerantService.lancerTirage(tirageAuSort, eligibles, prochaineSemaine);
            afficherInfo("Gagnant du tirage : " + gerantService.nomDuMembre(tirage.getBeneficiaireId()));
        } catch (IllegalStateException e) {
            afficherErreur(e.getMessage());
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void demanderNouvelleSerie() {
        Tontine t = listeTontines.getSelectionModel().getSelectedItem();
        if (t == null) { afficherErreur("Sélectionnez une tontine."); return; }
        String gerantId = SessionContext.getUtilisateurConnecte().getId();
        try {
            // Vérifié AVANT la confirmation : inutile de demander « Confirmer ? » pour refuser derrière.
            gerantService.verifierPeutDemanderNouvelleSerie(t, gerantId);

            Alert confirmation = new Alert(AlertType.CONFIRMATION,
                    "Demander à l'administrateur de démarrer une nouvelle série sur « " + t.getNom() + " » ?\n\n"
                            + "Les adhésions actuelles prendront fin : chaque membre décide lui-même de rester et "
                            + "devra redemander à rejoindre. La tontine redémarrera quand le nombre de membres demandé "
                            + "sera de nouveau atteint. La série ne démarre qu'après la validation de l'administrateur.");
            confirmation.setHeaderText("Nouvelle série");
            var reponse = confirmation.showAndWait();
            if (reponse.isEmpty() || reponse.get() != ButtonType.OK) return;

            gerantService.demanderNouvelleSerie(t, gerantId);
            afficherInfo("Demande envoyée à l'administrateur. Vous serez notifié de sa décision.");
        } catch (IllegalStateException e) {
            afficherErreur(e.getMessage());
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void demarrerNouveauCycle() {
        Tontine t = listeTontines.getSelectionModel().getSelectedItem();
        if (t == null) { afficherErreur("Sélectionnez une tontine."); return; }
        try {
            int prochainNumero = gerantService.cyclesDeLaTontine(t.getId()).size() + 1;
            gerantService.demarrerNouveauCycle(t.getId(), prochainNumero);
            afficherInfo("Nouveau cycle n°" + prochainNumero + " démarré.");
            rafraichirOnglets();
        } catch (SQLException | IllegalStateException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void cloturerCycle() {
        Cycle cycle = comboCycles.getSelectionModel().getSelectedItem();
        if (cycle == null) { afficherErreur("Sélectionnez un cycle."); return; }
        if (cycle.getStatut() != sn.naat.model.Enums.StatutCycle.EN_COURS) {
            afficherErreur("Ce cycle est déjà clôturé.");
            return;
        }
        try {
            // Prérequis vérifiés AVANT la confirmation : inutile de demander
            // « Confirmer ? » pour refuser derrière (ex. tirage non effectué).
            gerantService.verifierPrerequisCloture(cycle);

            BigDecimal montant = gerantService.calculerMontantCycle(cycle);
            List<Adhesion> enDefaut = gerantService.listerMembresEnDefaut(cycle);

            String messageConfirmation = "Clôturer le cycle n°" + cycle.getNumero()
                    + " et distribuer " + montant + " FCFA ?";
            if (!enDefaut.isEmpty()) {
                messageConfirmation += "\n\n⚠ " + enDefaut.size() + " membre(s) n'ont pas cotisé et recevront "
                        + "automatiquement une pénalité de retard déduite de leur solde.";
            }

            Alert confirmation = new Alert(AlertType.CONFIRMATION, messageConfirmation);
            var reponse = confirmation.showAndWait();
            if (reponse.isEmpty() || reponse.get() != ButtonType.OK) return;

            gerantService.cloturerCycle(cycle, montant);
            afficherInfo("Cycle clôturé, distribution de " + montant + " FCFA enregistrée"
                    + (enDefaut.isEmpty() ? "." : ", " + enDefaut.size() + " pénalité(s) appliquée(s)."));
            rafraichirOnglets();
        } catch (IllegalStateException e) {
            afficherErreur(e.getMessage());
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void demanderNouvelleTontine() {
        DemandeCreationDialogue.afficher(SessionContext.getUtilisateurConnecte().getId());
    }

    @FXML
    private void changerEspace() {
        NavigationEspaces.changerEspace(sn.naat.model.Enums.Espace.GERANT);
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
