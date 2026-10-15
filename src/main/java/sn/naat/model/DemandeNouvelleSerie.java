package sn.naat.model;

import sn.naat.model.Enums.StatutDemande;
import java.time.LocalDateTime;

/**
 * Gerant "1" --> "0..*" DemandeNouvelleSerie ; validée ou refusée par l'Administrateur.
 * Quand une tontine est clôturée (tous les membres ont reçu leur distribution), son gérant
 * peut demander une nouvelle série ; elle ne démarre qu'avec l'accord de l'administrateur.
 */
public class DemandeNouvelleSerie {

    private String id;
    private String tontineId;
    private String gerantId;
    private LocalDateTime dateEnvoi = LocalDateTime.now();
    private StatutDemande statut = StatutDemande.EN_ATTENTE;
    private LocalDateTime dateDecision;

    public DemandeNouvelleSerie() { }

    public DemandeNouvelleSerie(String id, String tontineId, String gerantId) {
        this.id = id;
        this.tontineId = tontineId;
        this.gerantId = gerantId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTontineId() { return tontineId; }
    public void setTontineId(String tontineId) { this.tontineId = tontineId; }

    public String getGerantId() { return gerantId; }
    public void setGerantId(String gerantId) { this.gerantId = gerantId; }

    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    public StatutDemande getStatut() { return statut; }
    public void setStatut(StatutDemande statut) { this.statut = statut; }

    public LocalDateTime getDateDecision() { return dateDecision; }
    public void setDateDecision(LocalDateTime dateDecision) { this.dateDecision = dateDecision; }
}
