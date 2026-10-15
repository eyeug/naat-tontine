package sn.naat.model;

import sn.naat.model.Enums.StatutDemande;
import java.time.LocalDateTime;

/** Tontine "1" --> "0..*" DemandeAdhesion ; validée par le Gérant. */
public class DemandeAdhesion {

    private String id;
    private String tontineId;
    private String nom;
    private String tel;
    private String email;
    private String motDePasseHash;
    private String pieceIdentiteHash;
    private String certificatResidenceHash;
    private boolean cguAcceptees;
    private LocalDateTime dateEnvoi = LocalDateTime.now();
    private StatutDemande statut = StatutDemande.EN_ATTENTE;
    private String contactRef;

    public DemandeAdhesion() { }

    public DemandeAdhesion(String id, String tontineId, String nom, String tel, String email) {
        this.id = id;
        this.tontineId = tontineId;
        this.nom = nom;
        this.tel = tel;
        this.email = email;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTontineId() { return tontineId; }
    public void setTontineId(String tontineId) { this.tontineId = tontineId; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getTel() { return tel; }
    public void setTel(String tel) { this.tel = tel; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasseHash() { return motDePasseHash; }
    public void setMotDePasseHash(String motDePasseHash) { this.motDePasseHash = motDePasseHash; }

    public String getPieceIdentiteHash() { return pieceIdentiteHash; }
    public void setPieceIdentiteHash(String pieceIdentiteHash) { this.pieceIdentiteHash = pieceIdentiteHash; }

    public String getCertificatResidenceHash() { return certificatResidenceHash; }
    public void setCertificatResidenceHash(String certificatResidenceHash) { this.certificatResidenceHash = certificatResidenceHash; }

    public boolean isCguAcceptees() { return cguAcceptees; }
    public void setCguAcceptees(boolean cguAcceptees) { this.cguAcceptees = cguAcceptees; }

    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    public StatutDemande getStatut() { return statut; }
    public void setStatut(StatutDemande statut) { this.statut = statut; }

    public String getContactRef() { return contactRef; }
    public void setContactRef(String contactRef) { this.contactRef = contactRef; }
}
