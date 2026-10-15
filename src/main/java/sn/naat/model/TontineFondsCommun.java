package sn.naat.model;

import sn.naat.model.Enums.Frequence;
import sn.naat.model.Enums.TypeTontine;
import java.math.BigDecimal;

/**
 * TontineFondsCommun (type 1) — seule à autoriser les prêts.
 */
public class TontineFondsCommun extends Tontine {

    private BigDecimal tauxInteret;
    private int dureeMaxPret; // en jours

    public TontineFondsCommun() { super(); }

    public TontineFondsCommun(String id, String nom, Frequence frequence,
                               BigDecimal montantCotisation, int nbMembresMax, String gerantId,
                               BigDecimal tauxInteret, int dureeMaxPret) {
        super(id, nom, frequence, montantCotisation, nbMembresMax, gerantId);
        this.tauxInteret = tauxInteret;
        this.dureeMaxPret = dureeMaxPret;
    }

    /** -repartirInterets(): void — appelée en interne à la clôture d'un cycle. */
    private void repartirInterets() {
        // Logique de répartition des intérêts entre les membres,
        // déclenchée par Cycle.cloturer() -> Distribution.
    }

    @Override
    public String getTypeCode() { return TypeTontine.FONDS_COMMUN.name(); }

    @Override
    public String getTypeLibelle() { return "Fonds commun"; }

    public BigDecimal getTauxInteret() { return tauxInteret; }
    public void setTauxInteret(BigDecimal tauxInteret) { this.tauxInteret = tauxInteret; }

    public int getDureeMaxPret() { return dureeMaxPret; }
    public void setDureeMaxPret(int dureeMaxPret) { this.dureeMaxPret = dureeMaxPret; }
}
