package sn.naat.controller;

import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import sn.naat.MainApp;
import sn.naat.model.Enums.Espace;
import sn.naat.service.EspacesRegles;
import sn.naat.util.SessionContext;

import java.util.List;

/**
 * Navigation entre les « espaces » (un écran par rôle) d'un compte qui en a plusieurs.
 * À la connexion on arrive toujours sur l'espace le plus élevé ; ce bouton permet d'aller dans les autres,
 * par exemple un gérant qui veut cotiser ou demander un prêt (actions de l'espace membre).
 */
final class NavigationEspaces {

    private NavigationEspaces() { }

    /** À appeler à l'ouverture d'un espace : le bouton n'apparaît que si le compte a au moins deux espaces. */
    static void configurer(Button boutonChangerEspace) {
        boolean visible = EspacesRegles.boutonVisible(SessionContext.getEspaces());
        boutonChangerEspace.setVisible(visible);
        boutonChangerEspace.setManaged(visible);
    }

    /**
     * Quitte l'espace courant. S'il n'y a qu'un seul autre espace, on y va directement (un clic) ;
     * sinon on demande lequel.
     */
    static void changerEspace(Espace courant) {
        List<Espace> autres = EspacesRegles.autres(SessionContext.getEspaces(), courant);
        if (autres.isEmpty()) {
            return;
        }
        Espace choisi;
        if (autres.size() == 1) {
            choisi = autres.get(0);
        } else {
            ChoiceDialog<Espace> dialogue = new ChoiceDialog<>(autres.get(0), autres);
            dialogue.setTitle("Changer d'espace");
            dialogue.setHeaderText("Dans quel espace voulez-vous aller ?");
            var resultat = dialogue.showAndWait();
            if (resultat.isEmpty()) {
                return;
            }
            choisi = resultat.get();
        }
        MainApp.naviguerVers(cheminDe(choisi));
    }

    private static String cheminDe(Espace espace) {
        return switch (espace) {
            case ADMINISTRATEUR -> "/sn/naat/view/admin_dashboard.fxml";
            case GERANT -> "/sn/naat/view/gerant_dashboard.fxml";
            case MEMBRE -> "/sn/naat/view/membre_dashboard.fxml";
        };
    }
}
