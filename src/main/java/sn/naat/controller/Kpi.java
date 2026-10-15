package sn.naat.controller;

import javafx.scene.control.Label;

/** Mise à jour des cartes d'indicateurs (valeur, note, couleur selon l'état) des tableaux de bord. */
final class Kpi {

    enum Etat { NORMAL, ALERTE, ATTENTE }

    private Kpi() { }

    static void definir(Label valeur, Label note, String texte, String texteNote, Etat etat) {
        valeur.setText(texte);
        note.setText(texteNote == null ? "" : texteNote);
        valeur.getStyleClass().removeAll("kpi-valeur-alerte", "kpi-valeur-attente");
        if (etat == Etat.ALERTE) valeur.getStyleClass().add("kpi-valeur-alerte");
        if (etat == Etat.ATTENTE) valeur.getStyleClass().add("kpi-valeur-attente");
    }
}
