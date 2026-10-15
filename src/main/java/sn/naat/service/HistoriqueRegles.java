package sn.naat.service;

import sn.naat.model.Enums.TypeTransaction;
import sn.naat.model.Transaction;

import java.math.BigDecimal;
import java.util.List;

/**
 * Présentation de l'historique des transactions, commune à l'espace Membre et à l'espace Gérant, pour que
 * les deux affichent les retards de la même façon. Un retard est une ligne de type PENALITE, écrite à la
 * clôture du cycle pour chaque membre qui n'avait pas cotisé : la pénalité est déduite de son solde.
 *
 * Fonctions pures (aucun accès à la base) : les données sont passées en paramètres.
 */
public final class HistoriqueRegles {

    private HistoriqueRegles() { }

    public static boolean estRetard(Transaction t) {
        return t.getType() == TypeTransaction.PENALITE;
    }

    /** Type affiché : « RETARD (pénalité) » plutôt que le nom technique PENALITE. */
    public static String libelleType(Transaction t) {
        return estRetard(t) ? "RETARD (pénalité)" : t.getType().name();
    }

    /** Montant affiché : précédé de « − » pour un retard, car c'est une somme déduite et non un paiement. */
    public static String libelleMontant(Transaction t) {
        return (estRetard(t) ? "− " : "") + t.getMontant() + " FCFA";
    }

    /** Explication de la ligne (ex. « Retard — cycle n°3 : aucune cotisation reçue avant la clôture »). */
    public static String libelleDetail(Transaction t) {
        return t.getLibelle() == null ? "" : t.getLibelle();
    }

    public static long nombreRetards(List<Transaction> transactions) {
        return transactions.stream().filter(HistoriqueRegles::estRetard).count();
    }

    public static BigDecimal totalPenalites(List<Transaction> transactions) {
        return transactions.stream()
                .filter(HistoriqueRegles::estRetard)
                .map(Transaction::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Ligne de synthèse : « Aucun retard. » ou « ⚠ 2 retard(s) — pénalités déduites : 1000 FCFA ». */
    public static String resumeRetards(List<Transaction> transactions) {
        long nb = nombreRetards(transactions);
        return nb == 0
                ? "Aucun retard."
                : "⚠ " + nb + " retard(s) — pénalités déduites : " + totalPenalites(transactions) + " FCFA";
    }
}
