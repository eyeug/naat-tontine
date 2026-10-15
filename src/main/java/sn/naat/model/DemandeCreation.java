package sn.naat.model;

import sn.naat.model.Enums.Frequence;
import sn.naat.model.Enums.StatutDemande;
import sn.naat.model.Enums.TypeTontine;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Utilisateur "1" --> "0..*" DemandeCreation ; validée par l'Administrateur. */
public class DemandeCreation {

    private String id;
    private String demandeurId;
    private String nomSouhaite;
    private TypeTontine typeSouhaite;
    private BigDecimal montant;
    private BigDecimal montantMin; // Fonds commun uniquement
    private BigDecimal montantMax;
    private Frequence frequence;
    private int nbMembres;
    private LocalDateTime dateEnvoi = LocalDateTime.now();
    private StatutDemande statut = StatutDemande.EN_ATTENTE;

    public DemandeCreation() { }

    public DemandeCreation(String id, String demandeurId, String nomSouhaite,
                            TypeTontine typeSouhaite, BigDecimal montant,
                            Frequence frequence, int nbMembres) {
        this.id = id;
        this.demandeurId = demandeurId;
        this.nomSouhaite = nomSouhaite;
        this.typeSouhaite = typeSouhaite;
        this.montant = montant;
        this.frequence = frequence;
        this.nbMembres = nbMembres;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDemandeurId() { return demandeurId; }
    public void setDemandeurId(String demandeurId) { this.demandeurId = demandeurId; }

    public String getNomSouhaite() { return nomSouhaite; }
    public void setNomSouhaite(String nomSouhaite) { this.nomSouhaite = nomSouhaite; }

    public TypeTontine getTypeSouhaite() { return typeSouhaite; }
    public void setTypeSouhaite(TypeTontine typeSouhaite) { this.typeSouhaite = typeSouhaite; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public BigDecimal getMontantMin() { return montantMin; }
    public void setMontantMin(BigDecimal montantMin) { this.montantMin = montantMin; }

    public BigDecimal getMontantMax() { return montantMax; }
    public void setMontantMax(BigDecimal montantMax) { this.montantMax = montantMax; }

    public Frequence getFrequence() { return frequence; }
    public void setFrequence(Frequence frequence) { this.frequence = frequence; }

    public int getNbMembres() { return nbMembres; }
    public void setNbMembres(int nbMembres) { this.nbMembres = nbMembres; }

    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    public StatutDemande getStatut() { return statut; }
    public void setStatut(StatutDemande statut) { this.statut = statut; }
}
