package sn.naat.controller;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Boîte de dialogue « formulaire » dont tous les champs sont affichés en
 * lecture seule : grisés, non modifiables, non sélectionnables au clavier.
 * Sert à présenter une valeur imposée (ex. le montant fixe d'une cotisation)
 * sous la forme d'un champ qu'on voit mais qu'on ne peut pas changer.
 */
final class FormulaireLectureSeule {

    private static final String STYLE_CHAMP_FIXE =
            "-fx-background-color: #eceeec;"
          + "-fx-border-color: #d3d8d4;"
          + "-fx-border-radius: 6;"
          + "-fx-background-radius: 6;"
          + "-fx-text-fill: #3a403c;"
          + "-fx-opacity: 1;";

    private FormulaireLectureSeule() { }

    /**
     * Affiche le formulaire et attend la réponse de l'utilisateur.
     *
     * @param titre  titre de la fenêtre
     * @param entete phrase d'introduction sous le titre
     * @param note   remarque affichée sous les champs (peut être null)
     * @param lignes paires libellé / valeur, par exemple :
     *               "Membre", "Moussa Seck", "Montant (FCFA)", "5000.00"
     * @return true si l'utilisateur a validé (OK), false s'il a annulé
     */
    static boolean confirmer(String titre, String entete, String note, String... lignes) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(titre);
        dialog.setHeaderText(entete);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grille = new GridPane();
        grille.setHgap(12);
        grille.setVgap(12);
        for (int i = 0; i + 1 < lignes.length; i += 2) {
            TextField champ = new TextField(lignes[i + 1]);
            champ.setEditable(false);
            champ.setFocusTraversable(false);
            champ.setPrefColumnCount(18);
            champ.setStyle(STYLE_CHAMP_FIXE);
            grille.addRow(i / 2, new Label(lignes[i] + " :"), champ);
        }

        VBox contenu = new VBox(14, grille);
        if (note != null && !note.isBlank()) {
            Label labelNote = new Label(note);
            labelNote.setWrapText(true);
            labelNote.setMaxWidth(420);
            labelNote.setStyle("-fx-text-fill: #68716c; -fx-font-size: 12px;");
            contenu.getChildren().add(labelNote);
        }
        dialog.getDialogPane().setContent(contenu);

        var resultat = dialog.showAndWait();
        return resultat.isPresent() && resultat.get() == ButtonType.OK;
    }
}
