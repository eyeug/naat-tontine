package sn.naat.controller;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Boîtes de dialogue communes à tous les écrans. */
final class Dialogues {

    private Dialogues() { }

    /**
     * Affiche une erreur EN ENTIER. Une Alert classique coupe son texte (« Table ... ») dès qu'il
     * est un peu long, ce qui rend les messages techniques illisibles. Ici le message est dans une
     * zone de texte : retour à la ligne automatique, texte sélectionnable et copiable.
     * Si l'erreur ressemble à une base de données pas à jour, un conseil est ajouté.
     */
    static void erreur(String message) {
        String texte = message == null ? "Erreur inconnue." : message;
        if (ressembleABasePasAJour(texte)) {
            texte += "\n\nLa base de données n'est probablement pas à jour par rapport à cette version de "
                    + "l'application. Exécutez le script naat_correctif_consolide.sql sur la base « naat » "
                    + "(il est fourni dans src/main/resources/sn/naat/db/ du projet), puis relancez l'application.";
        }

        TextArea zone = new TextArea(texte);
        zone.setEditable(false);
        zone.setWrapText(true);
        zone.setPrefColumnCount(48);
        zone.setPrefRowCount(6);

        Alert alerte = new Alert(AlertType.ERROR);
        alerte.getDialogPane().setContent(zone);
        alerte.setResizable(true);
        alerte.showAndWait();
    }

    /**
     * Affiche une valeur sensible (ex. un mot de passe provisoire) dans un champ en grands caractères,
     * sélectionnable et copiable, avec une note. Elle n'est montrée qu'ici : à la fermeture, elle disparaît.
     */
    static void afficherSecret(String titre, String entete, String libelle, String valeur, String note) {
        TextField champ = new TextField(valeur);
        champ.setEditable(false);
        champ.setStyle("-fx-font-size: 20px; -fx-font-family: monospace;");
        Label labelNote = new Label(note);
        labelNote.setWrapText(true);
        labelNote.setMaxWidth(420);

        Alert alerte = new Alert(AlertType.INFORMATION);
        alerte.setTitle(titre);
        alerte.setHeaderText(entete);
        alerte.getDialogPane().setContent(new VBox(10, new Label(libelle + " :"), champ, labelNote));
        alerte.showAndWait();
    }

    static void info(String message) {
        new Alert(AlertType.INFORMATION, message).showAndWait();
    }

    /** Messages MySQL typiques d'un schéma en retard : table ou colonne absente. */
    private static boolean ressembleABasePasAJour(String message) {
        String m = message.toLowerCase();
        return m.contains("doesn't exist") || m.contains("unknown column");
    }
}
