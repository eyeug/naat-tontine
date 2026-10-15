package sn.naat.model;

import sn.naat.model.Enums.StatutDemande;
import java.time.LocalDateTime;

/**
 * Utilisateur "1" --> "0..*" DemandeReinitialisation ; traitée par l'Administrateur.
 * Un utilisateur qui a oublié son mot de passe en fait la demande depuis l'écran de connexion ;
 * l'administrateur vérifie son identité puis lui remet un mot de passe provisoire.
 * VALIDEE = mot de passe provisoire généré ; REFUSEE = identité non vérifiée.
 */
public class DemandeReinitialisation {

    private String id;
    private String utilisateurId;
    private LocalDateTime dateEnvoi = LocalDateTime.now();
    private StatutDemande statut = StatutDemande.EN_ATTENTE;
    private LocalDateTime dateTraitement;

    public DemandeReinitialisation() { }

    public DemandeReinitialisation(String id, String utilisateurId) {
        this.id = id;
        this.utilisateurId = utilisateurId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(String utilisateurId) { this.utilisateurId = utilisateurId; }

    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    public StatutDemande getStatut() { return statut; }
    public void setStatut(StatutDemande statut) { this.statut = statut; }

    public LocalDateTime getDateTraitement() { return dateTraitement; }
    public void setDateTraitement(LocalDateTime dateTraitement) { this.dateTraitement = dateTraitement; }
}
