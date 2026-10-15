package sn.naat.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import sn.naat.MainApp;
import sn.naat.model.Administrateur;
import sn.naat.model.Gerant;
import sn.naat.model.Membre;
import sn.naat.model.Utilisateur;
import sn.naat.service.AuthService;
import sn.naat.util.SessionContext;

import java.sql.SQLException;
import java.util.Optional;

public class LoginController {

    @FXML private TextField champTel;
    @FXML private PasswordField champMotDePasse;
    @FXML private Button boutonConnexion;
    @FXML private Label labelErreur;

    private final AuthService authService = new AuthService();

    @FXML
    private void seConnecter() {
        labelErreur.setText("");
        String tel = champTel.getText();
        String motDePasse = champMotDePasse.getText();

        if (tel == null || tel.isBlank() || motDePasse == null || motDePasse.isBlank()) {
            labelErreur.setText("Veuillez renseigner le téléphone et le mot de passe.");
            return;
        }

        try {
            Optional<Utilisateur> opt = authService.seConnecter(tel.trim(), motDePasse);
            if (opt.isEmpty()) {
                labelErreur.setText("Identifiants incorrects ou compte suspendu.");
                return;
            }

            Utilisateur u = opt.get();

            // Mot de passe provisoire remis par l'administrateur : on exige un nouveau mot de passe
            // avant d'aller plus loin. Si l'utilisateur annule, il n'est pas connecté.
            if (u.isDoitChangerMotDePasse() && !ProfilDialogue.changementObligatoire(u.getId())) {
                labelErreur.setText("Vous devez choisir un nouveau mot de passe pour continuer.");
                return;
            }
            SessionContext.connecter(u);
            SessionContext.setEspaces(authService.espacesDisponibles(u.getId())); // pour « Changer d'espace »

            if (u instanceof Administrateur) {
                MainApp.naviguerVers("/sn/naat/view/admin_dashboard.fxml");
            } else if (u instanceof Gerant) {
                MainApp.naviguerVers("/sn/naat/view/gerant_dashboard.fxml");
            } else if (u instanceof Membre) {
                MainApp.naviguerVers("/sn/naat/view/membre_dashboard.fxml");
            } else {
                // Compte sans tontine en cours (nouveau, ou dont toutes les tontines sont terminées) :
                // on l'envoie sur la liste des tontines pour qu'il en rejoigne une ou en crée une.
                MainApp.naviguerVers("/sn/naat/view/tontines_publiques.fxml");
            }
        } catch (SQLException e) {
            labelErreur.setText("Erreur de connexion à la base de données : " + e.getMessage());
        }
    }

    @FXML
    private void motDePasseOublie() {
        TextInputDialog dialog = new TextInputDialog(champTel.getText());
        dialog.setTitle("Mot de passe oublié");
        dialog.setHeaderText("Saisissez le numéro de téléphone de votre compte");
        dialog.setContentText("Téléphone :");
        var resultat = dialog.showAndWait();
        if (resultat.isEmpty() || resultat.get().isBlank()) return;

        try {
            authService.demanderReinitialisationMotDePasse(resultat.get());
            // Même réponse que le numéro existe ou non : on ne révèle pas quels numéros ont un compte.
            new Alert(AlertType.INFORMATION, "Si ce numéro correspond à un compte, une demande a été transmise "
                    + "à l'administrateur.\n\nIl vous contactera pour vérifier votre identité, puis vous remettra "
                    + "un mot de passe provisoire que vous devrez changer à votre prochaine connexion.").showAndWait();
        } catch (SQLException e) {
            labelErreur.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void allerInscription() {
        MainApp.naviguerVers("/sn/naat/view/register.fxml");
    }

    @FXML
    private void voirTontinesPubliques() {
        MainApp.naviguerVers("/sn/naat/view/tontines_publiques.fxml");
    }
}
