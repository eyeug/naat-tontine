package sn.naat.controller;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import sn.naat.service.ProfilRegles;

/**
 * Limite la saisie dans les champs téléphone et email : on ne PEUT pas taper plus de caractères que permis,
 * ni des caractères interdits (les règles sont dans ProfilRegles, testées sans interface). Le contrôle final
 * à la validation reste fait par ProfilRegles.refusTelephone / refusEmail.
 */
final class ChampsSaisie {

    private ChampsSaisie() { }

    /** Chiffres seulement, 9 au plus. */
    static void limiterTelephone(TextField champ) {
        champ.setTextFormatter(new TextFormatter<String>(
                changement -> ProfilRegles.saisieTelephoneAutorisee(changement.getControlNewText()) ? changement : null));
    }

    /** Caractères d'une adresse Gmail (lettres, chiffres, points, @), 40 au plus. */
    static void limiterEmail(TextField champ) {
        champ.setTextFormatter(new TextFormatter<String>(
                changement -> ProfilRegles.saisieEmailAutorisee(changement.getControlNewText()) ? changement : null));
    }
}
