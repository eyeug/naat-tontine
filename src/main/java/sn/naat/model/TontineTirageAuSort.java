package sn.naat.model;

import sn.naat.model.Enums.Frequence;
import sn.naat.model.Enums.TypeTontine;
import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

/**
 * TontineTirageAuSort (type 3) — chaque cycle, un gagnant est
 * tiré au sort parmi les membres qui n'ont pas encore gagné.
 */
public class TontineTirageAuSort extends Tontine {

    private static final Random RANDOM = new Random();

    public TontineTirageAuSort() { super(); }

    public TontineTirageAuSort(String id, String nom, Frequence frequence,
                                BigDecimal montantCotisation, int nbMembresMax, String gerantId) {
        super(id, nom, frequence, montantCotisation, nbMembresMax, gerantId);
    }

    /**
     * -selectionnerGagnantAleatoire(): Utilisateur — appelée en
     * interne par lancerTirage(), jamais exposée directement.
     */
    private String selectionnerGagnantAleatoire(List<String> eligibles) {
        if (eligibles == null || eligibles.isEmpty()) return null;
        return eligibles.get(RANDOM.nextInt(eligibles.size()));
    }

    /** Tire au sort un gagnant parmi les membres éligibles fournis par le service. */
    public String lancerTirage(List<String> membresEligibles) {
        return selectionnerGagnantAleatoire(membresEligibles);
    }

    @Override
    public String getTypeCode() { return TypeTontine.TIRAGE_AU_SORT.name(); }

    @Override
    public String getTypeLibelle() { return "Tirage au sort"; }
}
