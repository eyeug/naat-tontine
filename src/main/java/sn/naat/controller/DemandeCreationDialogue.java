package sn.naat.controller;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import sn.naat.model.Enums;
import sn.naat.service.DemandeCreationRegles;
import sn.naat.service.MembreService;
import sn.naat.service.MontantCotisationRegles;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Formulaire « Demander la création d'une tontine », partagé par tous les écrans qui le proposent
 * (accueil pour un compte sans rôle, espace Gérant). Il n'y a qu'un seul formulaire :
 * les trois écrans se comportent donc exactement de la même façon.
 *
 * Ce n'est qu'une DEMANDE : la tontine n'est créée que si l'administrateur la valide.
 * Les erreurs de saisie s'affichent DANS la boîte, qui reste ouverte : on corrige sans tout retaper.
 */
final class DemandeCreationDialogue {

    private static final MembreService membreService = new MembreService();

    private DemandeCreationDialogue() { }

    /** On tolère « 5 000 » et « 12,5 » : espaces ignorés, virgule décimale acceptée. */
    private static BigDecimal lireMontant(TextField champ) {
        return new BigDecimal(champ.getText().replace(" ", "").replace(',', '.'));
    }

    static void afficher(String demandeurId) {
        // Vérifié AVANT le formulaire : inutile de le faire remplir pour refuser derrière
        // (une seule demande en attente par utilisateur).
        try {
            membreService.verifierPeutDemanderCreation(demandeurId);
        } catch (IllegalStateException e) {
            Dialogues.erreur(e.getMessage());
            return;
        } catch (SQLException e) {
            Dialogues.erreur("Erreur : " + e.getMessage());
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Créer une tontine");
        dialog.setHeaderText("Votre demande sera examinée par un administrateur");
        ButtonType envoyer = new ButtonType("Envoyer la demande", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(envoyer, ButtonType.CANCEL);

        TextField champNom = new TextField();
        champNom.setPromptText("Nom de la tontine");
        ChoiceBox<String> champType = new ChoiceBox<>(FXCollections.observableArrayList(
                "Fonds commun", "Tour de rôle", "Tirage au sort"));
        champType.getSelectionModel().selectFirst();
        TextField champMontant = new TextField();
        champMontant.setPromptText("Montant fixe de cotisation (FCFA)");
        TextField champMin = new TextField();
        champMin.setPromptText("Minimum (FCFA)");
        TextField champMax = new TextField();
        champMax.setPromptText("Maximum (FCFA)");
        HBox fourchette = new HBox(8, champMin, new Label("à"), champMax);
        fourchette.setAlignment(Pos.CENTER_LEFT);
        Label libelleCotisation = new Label("Cotisation :");
        Label noteFourchette = new Label("Fonds commun : chaque membre cotise le montant de son choix, entre ce minimum et ce "
                + "maximum. Cette fourchette fait partie de votre projet : l'administrateur la valide, "
                + "puis elle ne change plus.");
        noteFourchette.setWrapText(true);
        noteFourchette.setMaxWidth(380);
        noteFourchette.setStyle("-fx-text-fill: #68716c; -fx-font-size: 12px;");
        ChoiceBox<String> champFrequence = new ChoiceBox<>(FXCollections.observableArrayList(
                "Hebdomadaire", "Mensuelle"));
        champFrequence.getSelectionModel().selectFirst();
        TextField champNbMembres = new TextField();
        champNbMembres.setPromptText("Nombre de membres");

        GridPane grille = new GridPane();
        grille.setHgap(10);
        grille.setVgap(10);
        grille.addRow(0, new Label("Nom :"), champNom);
        grille.addRow(1, new Label("Type :"), champType);
        grille.addRow(2, libelleCotisation, champMontant);
        grille.add(fourchette, 1, 2);
        grille.addRow(3, new Label("Fréquence :"), champFrequence);
        grille.addRow(4, new Label("Membres :"), champNbMembres);

        // Fonds commun : fourchette min–max ; Tour de rôle / Tirage au sort : montant fixe identique pour tous.
        Runnable adapterCotisation = () -> {
            boolean fonds = "Fonds commun".equals(champType.getValue());
            champMontant.setVisible(!fonds);
            champMontant.setManaged(!fonds);
            fourchette.setVisible(fonds);
            fourchette.setManaged(fonds);
            noteFourchette.setVisible(fonds);
            noteFourchette.setManaged(fonds);
            libelleCotisation.setText(fonds ? "Fourchette :" : "Cotisation :");
        };
        champType.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> adapterCotisation.run());
        adapterCotisation.run();

        Label erreur = new Label();
        erreur.setWrapText(true);
        erreur.setMaxWidth(380);
        erreur.setStyle("-fx-text-fill: #c0392b; -fx-font-size: 12px;");
        dialog.getDialogPane().setContent(new VBox(12, grille, noteFourchette, erreur));

        // Validation et envoi AVANT la fermeture : en cas d'erreur on consomme l'événement,
        // la boîte reste ouverte et le message s'affiche dedans.
        Button boutonEnvoyer = (Button) dialog.getDialogPane().lookupButton(envoyer);
        boutonEnvoyer.addEventFilter(ActionEvent.ACTION, evenement -> {
            erreur.setText("");
            try {
                // On tolère « 5 000 » et « 12,5 » : espaces ignorés, virgule décimale acceptée.
                boolean fonds = "Fonds commun".equals(champType.getValue());
                BigDecimal montantMin = null;
                BigDecimal montantMax = null;
                BigDecimal montant;
                if (fonds) {
                    montantMin = lireMontant(champMin);
                    montantMax = lireMontant(champMax);
                    String refusFourchette = MontantCotisationRegles.refusFourchette(montantMin, montantMax);
                    if (refusFourchette != null) {
                        erreur.setText(refusFourchette);
                        evenement.consume();
                        return;
                    }
                    montant = montantMin;
                } else {
                    montant = lireMontant(champMontant);
                }
                int nbMembres = Integer.parseInt(champNbMembres.getText().trim());

                String refus = DemandeCreationRegles.refusFormulaire(champNom.getText(), montant, nbMembres);
                if (refus != null) {
                    erreur.setText(refus);
                    evenement.consume();
                    return;
                }

                Enums.TypeTontine type = switch (champType.getValue()) {
                    case "Fonds commun" -> Enums.TypeTontine.FONDS_COMMUN;
                    case "Tour de rôle" -> Enums.TypeTontine.TOUR_DE_ROLE;
                    default -> Enums.TypeTontine.TIRAGE_AU_SORT;
                };
                Enums.Frequence frequence = champFrequence.getValue().equals("Hebdomadaire")
                        ? Enums.Frequence.HEBDOMADAIRE : Enums.Frequence.MENSUELLE;

                membreService.soumettreDemandeCreation(demandeurId, champNom.getText().trim(),
                        type, montant, montantMin, montantMax, frequence, nbMembres);
            } catch (NumberFormatException e) {
                erreur.setText("Les montants et le nombre de membres doivent être des nombres.");
                evenement.consume();
            } catch (IllegalArgumentException e) {
                erreur.setText(e.getMessage()); // fourchette refusée par le service
                evenement.consume();
            } catch (IllegalStateException e) {
                erreur.setText(e.getMessage()); // ex. une autre demande a été soumise entre-temps
                evenement.consume();
            } catch (SQLException e) {
                erreur.setText("Erreur : " + e.getMessage());
                evenement.consume();
            }
        });

        var resultat = dialog.showAndWait();
        if (resultat.isPresent() && resultat.get() == envoyer) {
            Dialogues.info("Demande de création envoyée à l'administrateur.");
        }
    }
}
