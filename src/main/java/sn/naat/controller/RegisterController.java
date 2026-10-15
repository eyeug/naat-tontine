package sn.naat.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import sn.naat.MainApp;
import sn.naat.service.AuthService;

import java.sql.SQLException;

public class RegisterController {

    @FXML private TextField champNom;
    @FXML private TextField champTel;
    @FXML private TextField champEmail;
    @FXML private PasswordField champMotDePasse;
    @FXML private PasswordField champConfirmation;
    @FXML private Button boutonInscription;
    @FXML private Label labelErreur;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        // On ne peut pas taper plus de 9 chiffres, ni de lettres, dans le téléphone ; l'email est limité à 40 caractères.
        ChampsSaisie.limiterTelephone(champTel);
        ChampsSaisie.limiterEmail(champEmail);
    }

    @FXML
    private void sInscrire() {
        labelErreur.setText("");

        String nom = champNom.getText();
        String tel = champTel.getText();
        String email = champEmail.getText();
        String motDePasse = champMotDePasse.getText();
        String confirmation = champConfirmation.getText();

        if (nom == null || nom.isBlank() || tel == null || tel.isBlank()
                || motDePasse == null || motDePasse.isBlank()) {
            labelErreur.setText("Le nom, le téléphone et le mot de passe sont obligatoires.");
            return;
        }
        if (!motDePasse.equals(confirmation)) {
            labelErreur.setText("Les deux mots de passe ne correspondent pas.");
            return;
        }
        if (motDePasse.length() < 6) {
            labelErreur.setText("Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }

        try {
            authService.inscrire(nom.trim(), tel.trim(), email == null || email.isBlank() ? null : email.trim(), motDePasse);
            MainApp.naviguerVers("/sn/naat/view/login.fxml");
        } catch (IllegalArgumentException e) {
            labelErreur.setText(e.getMessage()); // téléphone ou email invalide (règles de ProfilRegles)
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("uq_utilisateur_tel")) {
                labelErreur.setText("Ce numéro de téléphone est déjà utilisé.");
            } else {
                labelErreur.setText("Erreur lors de l'inscription : " + e.getMessage());
            }
        }
    }

    @FXML
    private void retourConnexion() {
        MainApp.naviguerVers("/sn/naat/view/login.fxml");
    }
}
