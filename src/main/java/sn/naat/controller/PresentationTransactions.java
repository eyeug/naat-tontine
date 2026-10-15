package sn.naat.controller;

import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import sn.naat.model.Transaction;
import sn.naat.service.HistoriqueRegles;

/** Présentation commune des tableaux de transactions (espace Membre et historique vu par le gérant). */
final class PresentationTransactions {

    private static final String STYLE_RETARD = "-fx-background-color: #fdecea; -fx-text-background-color: #8c2a1f;";

    private PresentationTransactions() { }

    /** Surligne en rouge les lignes de retard, pour les repérer d'un coup d'œil. */
    static void surlignerRetards(TableView<Transaction> table) {
        table.setRowFactory(tv -> new TableRow<Transaction>() {
            @Override
            protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(!empty && item != null && HistoriqueRegles.estRetard(item) ? STYLE_RETARD : "");
            }
        });
    }
}
