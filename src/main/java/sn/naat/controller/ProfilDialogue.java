package sn.naat.controller;

import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import sn.naat.model.Utilisateur;
import sn.naat.service.AuthService;
import sn.naat.service.ProfilRegles;
import sn.naat.util.SessionContext;

import java.sql.SQLException;

/**
 * Boîtes de dialogue du profil : « Mon profil » (utilisateur connecté) et le changement de mot de
 * passe OBLIGATOIRE qui suit une réinitialisation par l'administrateur.
 *
 * Les erreurs de saisie s'affichent DANS la boîte, qui reste ouverte : l'utilisateur corrige sans
 * tout retaper.
 */
final class ProfilDialogue {

    private static final AuthService authService = new AuthService();
    private static final String STYLE_ERREUR = "-fx-text-fill: #c0392b; -fx-font-size: 12px;";

    private ProfilDialogue() { }

    /** « Mon profil » : téléphone, email et, facultativement, nouveau mot de passe. Le nom n'est pas modifiable. */
    static void afficher() {
        Utilisateur u = SessionContext.getUtilisateurConnecte();
        if (u == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Mon profil");
        dialog.setHeaderText("Vos informations personnelles");
        ButtonType enregistrer = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(enregistrer, ButtonType.CANCEL);

        TextField champNom = new TextField(u.getNom());
        champNom.setDisable(true); // le nom n'est pas modifiable
        TextField champTel = new TextField(u.getTel());
        TextField champEmail = new TextField(u.getEmail() == null ? "" : u.getEmail());
        champEmail.setPromptText("exemple@gmail.com (facultatif)");
        champTel.setPromptText("9 chiffres, ex. 771234567");
        ChampsSaisie.limiterTelephone(champTel);
        ChampsSaisie.limiterEmail(champEmail);
        Label statut = new Label(u.estActif() ? "Actif" : "Suspendu");

        PasswordField champAncien = new PasswordField();
        champAncien.setPromptText("Mot de passe actuel");
        PasswordField champNouveau = new PasswordField();
        champNouveau.setPromptText("Nouveau mot de passe");
        PasswordField champConfirmation = new PasswordField();
        champConfirmation.setPromptText("Confirmer le nouveau mot de passe");

        GridPane grille = new GridPane();
        grille.setHgap(12);
        grille.setVgap(10);
        grille.addRow(0, new Label("Nom :"), champNom);
        grille.addRow(1, new Label("Téléphone :"), champTel);
        grille.addRow(2, new Label("Email :"), champEmail);
        grille.addRow(3, new Label("Statut du compte :"), statut);
        grille.addRow(4, new Label("Mot de passe actuel :"), champAncien);
        grille.addRow(5, new Label("Nouveau mot de passe :"), champNouveau);
        grille.addRow(6, new Label("Confirmation :"), champConfirmation);

        Label note = new Label("Pour changer de mot de passe, remplissez les trois champs du bas. "
                + "Laissez-les vides pour le conserver. Le téléphone est votre identifiant de connexion.");
        note.setWrapText(true);
        note.setMaxWidth(420);
        note.setStyle("-fx-text-fill: #68716c; -fx-font-size: 12px;");
        Label erreur = new Label();
        erreur.setWrapText(true);
        erreur.setMaxWidth(420);
        erreur.setStyle(STYLE_ERREUR);

        dialog.getDialogPane().setContent(new VBox(12, grille, note, erreur));

        // La validation se fait AVANT la fermeture : en cas d'erreur on consomme l'événement,
        // la boîte reste ouverte et le message s'affiche dedans.
        Button boutonEnregistrer = (Button) dialog.getDialogPane().lookupButton(enregistrer);
        boutonEnregistrer.addEventFilter(ActionEvent.ACTION, evenement -> {
            erreur.setText("");
            if (!champNouveau.getText().equals(champConfirmation.getText())) {
                erreur.setText("Les deux nouveaux mots de passe ne correspondent pas.");
                evenement.consume();
                return;
            }
            try {
                authService.mettreAJourProfil(u.getId(), champTel.getText(), champEmail.getText(),
                        champAncien.getText(), champNouveau.getText());
                // Garde la session à jour (opération mettreAJourProfil du diagramme).
                String email = ProfilRegles.normaliserEmail(champEmail.getText());
                u.mettreAJourProfil(champTel.getText().trim(), email);
            } catch (IllegalArgumentException | IllegalStateException e) {
                erreur.setText(e.getMessage());
                evenement.consume();
            } catch (SQLException e) {
                erreur.setText("Erreur : " + e.getMessage());
                evenement.consume();
            }
        });

        var resultat = dialog.showAndWait();
        if (resultat.isPresent() && resultat.get() == enregistrer) {
            new Alert(AlertType.INFORMATION, "Votre profil a été mis à jour.").showAndWait();
        }
    }

    /**
     * Changement de mot de passe obligatoire, après connexion avec un mot de passe provisoire.
     * @return vrai si le nouveau mot de passe a été enregistré ; faux si l'utilisateur a annulé
     */
    static boolean changementObligatoire(String utilisateurId) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nouveau mot de passe");
        dialog.setHeaderText("Votre mot de passe a été réinitialisé par l'administrateur.\n"
                + "Choisissez maintenant un mot de passe que vous êtes seul(e) à connaître.");
        ButtonType valider = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(valider, ButtonType.CANCEL);

        PasswordField champNouveau = new PasswordField();
        champNouveau.setPromptText("Nouveau mot de passe");
        PasswordField champConfirmation = new PasswordField();
        champConfirmation.setPromptText("Confirmer le nouveau mot de passe");
        Label erreur = new Label();
        erreur.setWrapText(true);
        erreur.setMaxWidth(380);
        erreur.setStyle(STYLE_ERREUR);

        GridPane grille = new GridPane();
        grille.setHgap(12);
        grille.setVgap(10);
        grille.addRow(0, new Label("Nouveau mot de passe :"), champNouveau);
        grille.addRow(1, new Label("Confirmation :"), champConfirmation);
        dialog.getDialogPane().setContent(new VBox(12, grille, erreur));

        Button boutonValider = (Button) dialog.getDialogPane().lookupButton(valider);
        boutonValider.addEventFilter(ActionEvent.ACTION, evenement -> {
            erreur.setText("");
            if (!champNouveau.getText().equals(champConfirmation.getText())) {
                erreur.setText("Les deux mots de passe ne correspondent pas.");
                evenement.consume();
                return;
            }
            try {
                authService.changerMotDePasseObligatoire(utilisateurId, champNouveau.getText());
            } catch (IllegalArgumentException | IllegalStateException e) {
                erreur.setText(e.getMessage());
                evenement.consume();
            } catch (SQLException e) {
                erreur.setText("Erreur : " + e.getMessage());
                evenement.consume();
            }
        });

        var resultat = dialog.showAndWait();
        return resultat.isPresent() && resultat.get() == valider;
    }
}
