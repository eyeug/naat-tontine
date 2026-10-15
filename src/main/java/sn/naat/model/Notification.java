package sn.naat.model;

import java.time.LocalDateTime;

/** Utilisateur "1" --> "0..*" Notification : reçoit */
public class Notification {

    private String id;
    private String utilisateurId;
    private String message;
    private LocalDateTime dateEnvoi = LocalDateTime.now();
    private String type;
    private String refId; // référence optionnelle (ex. id du membre demandeur d'un tour anticipé)
    private boolean lue = false;

    public Notification() { }

    public Notification(String id, String utilisateurId, String message, String type) {
        this.id = id;
        this.utilisateurId = utilisateurId;
        this.message = message;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(String utilisateurId) { this.utilisateurId = utilisateurId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRefId() { return refId; }
    public void setRefId(String refId) { this.refId = refId; }

    public boolean isLue() { return lue; }
    public void setLue(boolean lue) { this.lue = lue; }
}
