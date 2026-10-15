package sn.naat.model;

import sn.naat.model.Enums.StatutTransaction;
import sn.naat.model.Enums.TypeTransaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Transaction "1" --> "1" ModePaiement : payé via */
public class Transaction {

    private String id;
    private String tontineId;
    private String membreId;
    private String distributionId; // renseigné si type = VERSEMENT
    private String modePaiementId;
    private TypeTransaction type;
    private BigDecimal montant;
    private LocalDateTime date = LocalDateTime.now();
    private StatutTransaction statut = StatutTransaction.VALIDEE;
    private String libelle; // explication affichée dans l'historique (ex. « Retard — cycle n°3 »)

    public Transaction() { }

    public Transaction(String id, String tontineId, String membreId,
                        TypeTransaction type, BigDecimal montant, String modePaiementId) {
        this.id = id;
        this.tontineId = tontineId;
        this.membreId = membreId;
        this.type = type;
        this.montant = montant;
        this.modePaiementId = modePaiementId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTontineId() { return tontineId; }
    public void setTontineId(String tontineId) { this.tontineId = tontineId; }

    public String getMembreId() { return membreId; }
    public void setMembreId(String membreId) { this.membreId = membreId; }

    public String getDistributionId() { return distributionId; }
    public void setDistributionId(String distributionId) { this.distributionId = distributionId; }

    public String getModePaiementId() { return modePaiementId; }
    public void setModePaiementId(String modePaiementId) { this.modePaiementId = modePaiementId; }

    public TypeTransaction getType() { return type; }
    public void setType(TypeTransaction type) { this.type = type; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public StatutTransaction getStatut() { return statut; }
    public void setStatut(StatutTransaction statut) { this.statut = statut; }
}
