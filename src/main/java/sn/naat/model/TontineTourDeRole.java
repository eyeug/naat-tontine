package sn.naat.model;

import sn.naat.model.Enums.Frequence;
import sn.naat.model.Enums.TypeTontine;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * TontineTourDeRole (type 2) — chaque cycle, un membre différent
 * de l'ordre de tour reçoit le versement.
 */
public class TontineTourDeRole extends Tontine {

    private List<String> ordreTour = new ArrayList<>(); // ids des membres, dans l'ordre
    private int tourActuelIndex = 0;

    public TontineTourDeRole() { super(); }

    public TontineTourDeRole(String id, String nom, Frequence frequence,
                              BigDecimal montantCotisation, int nbMembresMax, String gerantId) {
        super(id, nom, frequence, montantCotisation, nbMembresMax, gerantId);
    }

    /** -designerProchainBeneficiaire(): Utilisateur — appelée en interne par avancerTour(). */
    private String designerProchainBeneficiaire() {
        if (ordreTour.isEmpty()) return null;
        int prochain = (tourActuelIndex + 1) % ordreTour.size();
        return ordreTour.get(prochain);
    }

    /** Avance le tour et retourne l'id du nouveau bénéficiaire désigné. */
    public String avancerEtDesignerBeneficiaire() {
        String beneficiaire = designerProchainBeneficiaire();
        if (beneficiaire != null) {
            tourActuelIndex = ordreTour.indexOf(beneficiaire);
        }
        return beneficiaire;
    }

    @Override
    public String getTypeCode() { return TypeTontine.TOUR_DE_ROLE.name(); }

    @Override
    public String getTypeLibelle() { return "Tour de rôle"; }

    public List<String> getOrdreTour() { return ordreTour; }
    public void setOrdreTour(List<String> ordreTour) { this.ordreTour = ordreTour; }

    public int getTourActuelIndex() { return tourActuelIndex; }
    public void setTourActuelIndex(int tourActuelIndex) { this.tourActuelIndex = tourActuelIndex; }
}
