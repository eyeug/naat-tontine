package sn.naat.model;

import sn.naat.model.Enums.StatutDemande;
import java.time.LocalDateTime;

/**
 * Membre "1" --> "0..*" DemandeTourAnticipe ; accordée ou refusée par le Gerant de la tontine.
 * Dans une tontine Tour de rôle, un membre demande à recevoir sa distribution plus tôt que prévu.
 * Accordée, la demande le place juste après le bénéficiaire actuel dans l'ordre du tour.
 */
public class DemandeTourAnticipe {

    private String id;
    private String tontineId;
    private String membreId;
    private String motif;
    private LocalDateTime dateEnvoi = LocalDateTime.now();
    private StatutDemande statut = StatutDemande.EN_ATTENTE;
    private LocalDateTime dateDecision;

    public DemandeTourAnticipe() { }

    public DemandeTourAnticipe(String id, String tontineId, String membreId, String motif) {
        this.id = id;
        this.tontineId = tontineId;
        this.membreId = membreId;
        this.motif = motif;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTontineId() { return tontineId; }
    public void setTontineId(String tontineId) { this.tontineId = tontineId; }

    public String getMembreId() { return membreId; }
    public void setMembreId(String membreId) { this.membreId = membreId; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    public StatutDemande getStatut() { return statut; }
    public void setStatut(StatutDemande statut) { this.statut = statut; }

    public LocalDateTime getDateDecision() { return dateDecision; }
    public void setDateDecision(LocalDateTime dateDecision) { this.dateDecision = dateDecision; }
}
