package sn.naat.service;

import sn.naat.model.Adhesion;
import sn.naat.model.Enums.StatutAdhesion;
import sn.naat.model.Enums.StatutTransaction;
import sn.naat.model.Enums.TypeTransaction;
import sn.naat.model.Transaction;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/** Calculs des indicateurs affichés en haut des tableaux de bord. Fonctions pures (aucun accès à la base). */
public final class IndicateursRegles {

    private IndicateursRegles() { }

    /** Somme des cotisations validées (les pénalités, versements et remboursements ne comptent pas). */
    public static BigDecimal totalCotise(List<Transaction> transactions) {
        BigDecimal total = BigDecimal.ZERO;
        for (Transaction t : transactions) {
            if (t.getType() == TypeTransaction.COTISATION && t.getStatut() == StatutTransaction.VALIDEE) {
                total = total.add(t.getMontant());
            }
        }
        return total;
    }

    public static long membresActifs(List<Adhesion> adhesions) {
        return adhesions.stream().filter(a -> a.getStatut() == StatutAdhesion.ACTIF).count();
    }

    /** « 12 500 FCFA » : séparateur de milliers (espace), sans décimales inutiles. */
    public static String montant(BigDecimal montant) {
        DecimalFormatSymbols symboles = new DecimalFormatSymbols(Locale.FRANCE);
        symboles.setGroupingSeparator(' ');
        DecimalFormat format = new DecimalFormat("#,##0.##", symboles);
        return format.format(montant) + " FCFA";
    }
}
