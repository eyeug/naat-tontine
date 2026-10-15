package sn.naat.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Cycle "1" --> "0..1" Distribution */
public class Distribution {

    private String id;
    private String cycleId;
    private LocalDateTime date = LocalDateTime.now();
    private BigDecimal montantTotal;
    private String beneficiaireId; // renseigné pour Tour de rôle / Tirage au sort ; null pour Fonds commun

    public Distribution() { }

    public Distribution(String id, String cycleId, BigDecimal montantTotal) {
        this.id = id;
        this.cycleId = cycleId;
        this.montantTotal = montantTotal;
    }

    /**
     * -calculerMontant(): double — logique de calcul interne,
     * différente selon le type de tontine (part d'intérêts pour
     * un fonds commun, montant fixe pour tour de rôle / tirage).
     * Déléguée au service qui connaît le sous-type de Tontine.
     */
    private BigDecimal calculerMontant(BigDecimal base) {
        return base;
    }

    /**
     * +verserBeneficiaire(beneficiaire, mode): Transaction — le
     * versement effectif est réalisé par le service (qui a accès
     * au DAO Transaction), cette méthode reste un point d'entrée
     * métier symbolique sur l'objet Distribution.
     */

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCycleId() { return cycleId; }
    public void setCycleId(String cycleId) { this.cycleId = cycleId; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public BigDecimal getMontantTotal() { return montantTotal; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }

    public String getBeneficiaireId() { return beneficiaireId; }
    public void setBeneficiaireId(String beneficiaireId) { this.beneficiaireId = beneficiaireId; }
}
