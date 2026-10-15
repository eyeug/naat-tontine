package sn.naat.model;

import sn.naat.model.Enums.StatutPret;
import sn.naat.model.Enums.Valideur;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** TontineFondsCommun "1" --> "0..*" Pret ; Membre "1" --> "0..*" Pret */
public class Pret {

    private String id;
    private String tontineId;
    private String membreId;
    private BigDecimal montant;
    private int dureeJours;
    private String motif;
    private LocalDateTime dateDemande = LocalDateTime.now();
    private StatutPret statut = StatutPret.EN_ATTENTE;
    private Valideur valideur;
    private BigDecimal montantRembourse;   // capital + intérêt, renseigné au remboursement
    private LocalDateTime dateRemboursement;

    public Pret() { }

    public Pret(String id, String tontineId, String membreId,
                BigDecimal montant, int dureeJours, String motif) {
        this.id = id;
        this.tontineId = tontineId;
        this.membreId = membreId;
        this.montant = montant;
        this.dureeJours = dureeJours;
        this.motif = motif;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTontineId() { return tontineId; }
    public void setTontineId(String tontineId) { this.tontineId = tontineId; }

    public String getMembreId() { return membreId; }
    public void setMembreId(String membreId) { this.membreId = membreId; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public int getDureeJours() { return dureeJours; }
    public void setDureeJours(int dureeJours) { this.dureeJours = dureeJours; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public LocalDateTime getDateDemande() { return dateDemande; }
    public void setDateDemande(LocalDateTime dateDemande) { this.dateDemande = dateDemande; }

    public StatutPret getStatut() { return statut; }
    public void setStatut(StatutPret statut) { this.statut = statut; }

    public Valideur getValideur() { return valideur; }
    public void setValideur(Valideur valideur) { this.valideur = valideur; }

    public BigDecimal getMontantRembourse() { return montantRembourse; }
    public void setMontantRembourse(BigDecimal montantRembourse) { this.montantRembourse = montantRembourse; }

    public LocalDateTime getDateRemboursement() { return dateRemboursement; }
    public void setDateRemboursement(LocalDateTime dateRemboursement) { this.dateRemboursement = dateRemboursement; }

    @Override
    public String toString() {
        return montant + " FCFA — " + motif + " (demandé le " + dateDemande.toLocalDate() + ")";
    }
}
