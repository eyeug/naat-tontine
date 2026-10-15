package sn.naat.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import sn.naat.MainApp;
import sn.naat.model.*;
import sn.naat.service.AdministrateurService;
import sn.naat.service.MontantCotisationRegles;
import sn.naat.service.NomsRegles;
import sn.naat.util.SessionContext;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class AdminDashboardController {

    @FXML private Label labelBienvenue;
    @FXML private Button boutonChangerEspace;

    @FXML private TableView<DemandeCreation> tableDemandesCreation;
    @FXML private TableColumn<DemandeCreation, String> colDcNom;
    @FXML private TableColumn<DemandeCreation, String> colDcType;
    @FXML private TableColumn<DemandeCreation, String> colDcMontant;
    @FXML private TableColumn<DemandeCreation, String> colDcMembres;

    @FXML private TableView<AdministrateurService.DemandeReinitialisationAffichee> tableMotsDePasse;
    @FXML private TableColumn<AdministrateurService.DemandeReinitialisationAffichee, String> colMpNom;
    @FXML private TableColumn<AdministrateurService.DemandeReinitialisationAffichee, String> colMpTel;
    @FXML private TableColumn<AdministrateurService.DemandeReinitialisationAffichee, String> colMpDate;

    @FXML private TableView<AdministrateurService.DemandeNouvelleSerieAffichee> tableNouvellesSeries;
    @FXML private TableColumn<AdministrateurService.DemandeNouvelleSerieAffichee, String> colNsTontine;
    @FXML private TableColumn<AdministrateurService.DemandeNouvelleSerieAffichee, String> colNsGerant;
    @FXML private TableColumn<AdministrateurService.DemandeNouvelleSerieAffichee, String> colNsSerie;
    @FXML private TableColumn<AdministrateurService.DemandeNouvelleSerieAffichee, String> colNsDate;

    @FXML private TableView<Utilisateur> tableUtilisateurs;
    @FXML private TableColumn<Utilisateur, String> colUNom;
    @FXML private TableColumn<Utilisateur, String> colUTel;
    @FXML private TableColumn<Utilisateur, String> colUStatut;
    @FXML private TableColumn<Utilisateur, String> colURole;

    @FXML private TableView<Tontine> tableTontines;
    @FXML private TableColumn<Tontine, String> colTNom;
    @FXML private TableColumn<Tontine, String> colTType;
    @FXML private TableColumn<Tontine, String> colTGerant;

    @FXML private TableView<Pret> tablePretsGerants;
    @FXML private TableColumn<Pret, String> colPgTontine;
    @FXML private TableColumn<Pret, String> colPgGerant;
    @FXML private TableColumn<Pret, String> colPgMontant;
    @FXML private TableColumn<Pret, String> colPgDuree;
    @FXML private TableColumn<Pret, String> colPgMotif;

    @FXML private TableView<JournalAudit> tableAudit;
    @FXML private TableColumn<JournalAudit, String> colAuditDate;
    @FXML private TableColumn<JournalAudit, String> colAuditActeur;
    @FXML private TableColumn<JournalAudit, String> colAuditAction;


    @FXML private javafx.scene.control.Label kpiATraiter;
    @FXML private javafx.scene.control.Label kpiATraiterNote;
    @FXML private javafx.scene.control.Label kpiComptes;
    @FXML private javafx.scene.control.Label kpiComptesNote;
    @FXML private javafx.scene.control.Label kpiTontines;
    @FXML private javafx.scene.control.Label kpiTontinesNote;
    @FXML private javafx.scene.control.Label kpiAdmins;
    @FXML private javafx.scene.control.Label kpiAdminsNote;

    private final AdministrateurService adminService = new AdministrateurService();

    /** Rôle affiché de chaque compte (id → rôle), calculé une fois par rafraîchissement. */
    private java.util.Map<String, String> roles = new java.util.HashMap<>();

    /** Nom de chaque compte (id → nom), pour afficher des noms et non des identifiants dans les tableaux. */
    private java.util.Map<String, String> nomsUtilisateurs = new java.util.HashMap<>();
    private final DateTimeFormatter formatDate = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        Utilisateur u = SessionContext.getUtilisateurConnecte();
        labelBienvenue.setText("Bienvenue, " + u.getNom());
        NavigationEspaces.configurer(boutonChangerEspace);

        colDcNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNomSouhaite()));
        colDcType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTypeSouhaite().name()));
        colDcMontant.setCellValueFactory(d -> new SimpleStringProperty(MontantCotisationRegles.libelle(
                d.getValue().getMontantMin(), d.getValue().getMontantMax(), d.getValue().getMontant())));
        colDcMembres.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getNbMembres())));

        colMpNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nom()));
        colMpTel.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().tel()));
        colMpDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().demande().getDateEnvoi().format(formatDate)));

        colNsTontine.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nomTontine()));
        colNsGerant.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nomGerant()));
        colNsSerie.setCellValueFactory(d -> new SimpleStringProperty("Série n°" + d.getValue().numeroSerieDemandee()));
        colNsDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().demande().getDateEnvoi().format(formatDate)));

        colUNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNom()));
        colUTel.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTel()));
        colUStatut.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatutCompte().name()));
        // Le rôle est calculé une fois par rafraîchissement (il demande plusieurs requêtes pour savoir si un
        // compte est « membre ») : la colonne se contente de le lire, sans interroger la base à chaque cellule.
        colURole.setCellValueFactory(d -> new SimpleStringProperty(
                roles.getOrDefault(d.getValue().getId(), "?")));

        colTNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNom()));
        colTType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTypeLibelle()));
        colTGerant.setCellValueFactory(d -> new SimpleStringProperty(
                NomsRegles.nomAffiche(nomsUtilisateurs, d.getValue().getGerantId(), "—")));

        colPgTontine.setCellValueFactory(d -> {
            try { return new SimpleStringProperty(adminService.nomTontine(d.getValue().getTontineId())); }
            catch (SQLException e) { return new SimpleStringProperty("?"); }
        });
        colPgGerant.setCellValueFactory(d -> {
            try { return new SimpleStringProperty(adminService.nomUtilisateur(d.getValue().getMembreId())); }
            catch (SQLException e) { return new SimpleStringProperty("?"); }
        });
        colPgMontant.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMontant() + " FCFA"));
        colPgDuree.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getDureeJours())));
        colPgMotif.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMotif()));

        colAuditDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTs().format(formatDate)));
        // Une action sans acteur (faite automatiquement) s'affiche « Système » et non « null ».
        colAuditActeur.setCellValueFactory(d -> new SimpleStringProperty(
                NomsRegles.nomAffiche(nomsUtilisateurs, d.getValue().getActeurId(), "Système")));
        colAuditAction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAction()));

        rafraichir();
    }

    private void rafraichir() {
        try {
            tableDemandesCreation.setItems(FXCollections.observableArrayList(adminService.demandesCreationEnAttente()));
            tableNouvellesSeries.setItems(FXCollections.observableArrayList(adminService.demandesNouvelleSerieEnAttente()));
            tableMotsDePasse.setItems(FXCollections.observableArrayList(adminService.demandesReinitialisationEnAttente()));
            tablePretsGerants.setItems(FXCollections.observableArrayList(adminService.pretsGerantsEnAttente()));
            java.util.List<Utilisateur> utilisateurs = adminService.tousLesUtilisateurs();
            java.util.Map<String, String> nouveauxRoles = new java.util.HashMap<>();
            for (Utilisateur u : utilisateurs) {
                nouveauxRoles.put(u.getId(), adminService.roleDe(u.getId()));
            }
            roles = nouveauxRoles;
            java.util.Map<String, String> nouveauxNoms = new java.util.HashMap<>();
            for (Utilisateur u : utilisateurs) {
                nouveauxNoms.put(u.getId(), u.getNom());
            }
            nomsUtilisateurs = nouveauxNoms;
            tableUtilisateurs.setItems(FXCollections.observableArrayList(utilisateurs));
            tableTontines.setItems(FXCollections.observableArrayList(adminService.toutesLesTontines()));
            tableAudit.setItems(FXCollections.observableArrayList(adminService.consulterJournalAudit()));
            mettreAJourIndicateurs(utilisateurs);
        } catch (SQLException e) {
            afficherErreur("Erreur de chargement : " + e.getMessage());
        }
    }

    private void mettreAJourIndicateurs(java.util.List<Utilisateur> utilisateurs) {
        int aTraiter = tableDemandesCreation.getItems().size() + tableNouvellesSeries.getItems().size()
                + tableMotsDePasse.getItems().size() + tablePretsGerants.getItems().size();
        Kpi.definir(kpiATraiter, kpiATraiterNote, String.valueOf(aTraiter),
                aTraiter == 0 ? "Rien en attente" : "demande(s) en attente de décision",
                aTraiter == 0 ? Kpi.Etat.NORMAL : Kpi.Etat.ATTENTE);
        long suspendus = utilisateurs.stream()
                .filter(u -> u.getStatutCompte() == sn.naat.model.Enums.StatutCompte.SUSPENDU).count();
        Kpi.definir(kpiComptes, kpiComptesNote, String.valueOf(utilisateurs.size() - suspendus),
                suspendus == 0 ? "Aucun compte suspendu" : suspendus + " compte(s) suspendu(s)",
                Kpi.Etat.NORMAL);
        int nbTontines = tableTontines.getItems().size();
        Kpi.definir(kpiTontines, kpiTontinesNote, String.valueOf(nbTontines), "tontine(s) enregistrée(s)", Kpi.Etat.NORMAL);
        long admins = roles.values().stream().filter("Administrateur"::equals).count();
        Kpi.definir(kpiAdmins, kpiAdminsNote, admins + " / 2", "maximum autorisé : 2",
                Kpi.Etat.NORMAL);
    }

    @FXML
    private void validerCreation() {
        traiterDemandeCreation(true);
    }

    @FXML
    private void refuserCreation() {
        traiterDemandeCreation(false);
    }

    private void traiterDemandeCreation(boolean accepte) {
        DemandeCreation d = tableDemandesCreation.getSelectionModel().getSelectedItem();
        if (d == null) { afficherErreur("Sélectionnez une demande."); return; }
        try {
            adminService.validerDemandeCreation(d, accepte ? "validee" : "refusee");
            afficherInfo(accepte ? "Tontine créée et gérant promu." : "Demande refusée.");
            rafraichir();
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void validerPretGerant() {
        traiterPretGerant(true);
    }

    @FXML
    private void refuserPretGerant() {
        traiterPretGerant(false);
    }

    /**
     * Un gérant ne peut pas valider son propre prêt (règle anti-conflit) : c'est l'administrateur
     * qui le fait ici. Le prêt n'existe que sur une tontine Fonds commun, dont on récupère le type exact.
     */
    private void traiterPretGerant(boolean approuve) {
        Pret pret = tablePretsGerants.getSelectionModel().getSelectedItem();
        if (pret == null) { afficherErreur("Sélectionnez un prêt."); return; }
        try {
            TontineFondsCommun tontine = adminService.tontineFondsCommunDuPret(pret)
                    .orElseThrow(() -> new IllegalStateException("Ce prêt n'est pas rattaché à une tontine Fonds commun."));
            adminService.validerPretGerant(pret, tontine, approuve ? "valide" : "refuse");
            afficherInfo(approuve ? "Prêt validé." : "Prêt refusé.");
            rafraichir();
        } catch (IllegalStateException e) {
            afficherErreur(e.getMessage());
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void genererMotDePasseProvisoire() {
        AdministrateurService.DemandeReinitialisationAffichee ligne = tableMotsDePasse.getSelectionModel().getSelectedItem();
        if (ligne == null) { afficherErreur("Sélectionnez une demande."); return; }

        Alert confirmation = new Alert(AlertType.CONFIRMATION,
                "Avez-vous vérifié l'identité de " + ligne.nom() + " (téléphone : " + ligne.tel() + ") ?\n\n"
                        + "Un mot de passe provisoire va être généré. Il ne sera affiché qu'une seule fois : "
                        + "communiquez-le à l'utilisateur, qui devra le changer dès sa prochaine connexion.");
        confirmation.setHeaderText("Réinitialiser le mot de passe");
        var reponse = confirmation.showAndWait();
        if (reponse.isEmpty() || reponse.get() != ButtonType.OK) return;

        try {
            String provisoire = adminService.genererMotDePasseProvisoire(
                    ligne.demande(), SessionContext.getUtilisateurConnecte().getId());
            Dialogues.afficherSecret("Mot de passe provisoire",
                    "Mot de passe provisoire de " + ligne.nom(),
                    "Mot de passe provisoire", provisoire,
                    "Notez-le maintenant : il ne sera plus affiché. L'utilisateur devra choisir un nouveau "
                            + "mot de passe à sa prochaine connexion.");
            rafraichir();
        } catch (IllegalStateException e) {
            afficherErreur(e.getMessage());
            rafraichir();
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void refuserReinitialisation() {
        AdministrateurService.DemandeReinitialisationAffichee ligne = tableMotsDePasse.getSelectionModel().getSelectedItem();
        if (ligne == null) { afficherErreur("Sélectionnez une demande."); return; }
        try {
            adminService.refuserReinitialisation(ligne.demande(), SessionContext.getUtilisateurConnecte().getId());
            afficherInfo("Demande refusée : le mot de passe de " + ligne.nom() + " n'a pas été modifié.");
            rafraichir();
        } catch (IllegalStateException e) {
            afficherErreur(e.getMessage());
            rafraichir();
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void validerNouvelleSerie() {
        traiterNouvelleSerie(true);
    }

    @FXML
    private void refuserNouvelleSerie() {
        traiterNouvelleSerie(false);
    }

    private void traiterNouvelleSerie(boolean accepte) {
        AdministrateurService.DemandeNouvelleSerieAffichee ligne = tableNouvellesSeries.getSelectionModel().getSelectedItem();
        if (ligne == null) { afficherErreur("Sélectionnez une demande."); return; }
        try {
            adminService.validerNouvelleSerie(ligne.demande(), accepte ? "validee" : "refusee");
            afficherInfo(accepte
                    ? "Série n°" + ligne.numeroSerieDemandee() + " validée pour « " + ligne.nomTontine() + " »."
                    : "Demande refusée.");
            rafraichir();
        } catch (IllegalStateException e) {
            afficherErreur(e.getMessage());
            rafraichir();
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void suspendreCompte() {
        Utilisateur u = tableUtilisateurs.getSelectionModel().getSelectedItem();
        if (u == null) { afficherErreur("Sélectionnez un utilisateur."); return; }
        try {
            adminService.suspendreCompte(u.getId());
            afficherInfo("Compte suspendu.");
            rafraichir();
        } catch (IllegalStateException e) {
            afficherErreur(e.getMessage()); // ex. dernier administrateur actif
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void reactiverCompte() {
        Utilisateur u = tableUtilisateurs.getSelectionModel().getSelectedItem();
        if (u == null) { afficherErreur("Sélectionnez un utilisateur."); return; }
        try {
            adminService.reactiverCompte(u.getId());
            afficherInfo("Compte réactivé.");
            rafraichir();
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void promouvoirAdministrateur() {
        Utilisateur u = tableUtilisateurs.getSelectionModel().getSelectedItem();
        if (u == null) { afficherErreur("Sélectionnez un utilisateur."); return; }

        // Vérifié AVANT la confirmation : inutile de demander « Confirmer ? » pour refuser derrière.
        try {
            adminService.verifierPeutPromouvoirAdministrateur(u.getId());
        } catch (IllegalStateException e) {
            afficherErreur(e.getMessage());
            return;
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
            return;
        }

        Alert confirmation = new Alert(AlertType.CONFIRMATION,
                "Promouvoir " + u.getNom() + " en administrateur ? Cette personne aura alors les mêmes "
                        + "droits que vous (suspension de comptes, validation de créations de tontine, etc.).");
        var reponse = confirmation.showAndWait();
        if (reponse.isEmpty() || reponse.get() != ButtonType.OK) return;

        try {
            adminService.promouvoirAdministrateur(u.getId());
            afficherInfo(u.getNom() + " est désormais Administrateur.");
            rafraichir();
        } catch (IllegalStateException e) {
            afficherErreur(e.getMessage());
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void retirerRoleAdministrateur() {
        Utilisateur u = tableUtilisateurs.getSelectionModel().getSelectedItem();
        if (u == null) { afficherErreur("Sélectionnez un utilisateur."); return; }
        String adminId = SessionContext.getUtilisateurConnecte().getId();

        // Vérifié AVANT la confirmation : inutile de demander « Confirmer ? » pour refuser derrière.
        try {
            adminService.verifierPeutRetirerRoleAdministrateur(u.getId(), adminId);
        } catch (IllegalStateException e) {
            afficherErreur(e.getMessage());
            return;
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
            return;
        }

        Alert confirmation = new Alert(AlertType.CONFIRMATION,
                "Retirer le rôle d'administrateur à " + u.getNom() + " ?\n\nSon compte et ses autres rôles "
                        + "(membre, gérant) ne sont pas touchés. Une place d'administrateur se libère.");
        confirmation.setHeaderText("Retirer le rôle d'administrateur");
        var reponse = confirmation.showAndWait();
        if (reponse.isEmpty() || reponse.get() != ButtonType.OK) return;

        try {
            adminService.retirerRoleAdministrateur(u.getId(), adminId);
            afficherInfo(u.getNom() + " n'est plus administrateur.");
            rafraichir();
        } catch (IllegalStateException e) {
            afficherErreur(e.getMessage());
        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void changerEspace() {
        NavigationEspaces.changerEspace(sn.naat.model.Enums.Espace.ADMINISTRATEUR);
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
