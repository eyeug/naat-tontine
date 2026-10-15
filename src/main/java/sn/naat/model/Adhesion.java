package sn.naat.model;

import sn.naat.model.Enums.RoleAdhesion;
import sn.naat.model.Enums.StatutAdhesion;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Membre "1" --> "0..*" Adhesion ; Tontine "1" --> "0..*" Adhesion */
public class Adhesion {

    private String id;
    private String tontineId;
    private String membreId;
    private RoleAdhesion role = RoleAdhesion.MEMBRE;
    private LocalDateTime dateAdhesion = LocalDateTime.now();
    private BigDecimal solde = BigDecimal.ZERO;
    private BigDecimal totalCotise = BigDecimal.ZERO;
    private int cotisationsFaites = 0;
    private StatutAdhesion statut = StatutAdhesion.ACTIF;

    public Adhesion() { }

    public Adhesion(String id, String tontineId, String membreId, RoleAdhesion role) {
        this.id = id;
        this.tontineId = tontineId;
        this.membreId = membreId;
        this.role = role;
    }

    /** -mettreAJourSolde(montant): void — appelée en interne par enregistrerCotisation(). */
    private void mettreAJourSolde(BigDecimal montant) {
        this.solde = this.solde.add(montant);
        this.totalCotise = this.totalCotise.add(montant);
        this.cotisationsFaites++;
    }

    /** +enregistrerCotisation(montant, mode): Transaction */
    public void enregistrerCotisation(BigDecimal montant) {
        mettreAJourSolde(montant);
    }

    /** +desactiver(): void */
    public void desactiver() {
        this.statut = StatutAdhesion.INACTIF;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTontineId() { return tontineId; }
    public void setTontineId(String tontineId) { this.tontineId = tontineId; }

    public String getMembreId() { return membreId; }
    public void setMembreId(String membreId) { this.membreId = membreId; }

    public RoleAdhesion getRole() { return role; }
    public void setRole(RoleAdhesion role) { this.role = role; }

    public LocalDateTime getDateAdhesion() { return dateAdhesion; }
    public void setDateAdhesion(LocalDateTime dateAdhesion) { this.dateAdhesion = dateAdhesion; }

    public BigDecimal getSolde() { return solde; }
    public void setSolde(BigDecimal solde) { this.solde = solde; }

    public BigDecimal getTotalCotise() { return totalCotise; }
    public void setTotalCotise(BigDecimal totalCotise) { this.totalCotise = totalCotise; }

    public int getCotisationsFaites() { return cotisationsFaites; }
    public void setCotisationsFaites(int cotisationsFaites) { this.cotisationsFaites = cotisationsFaites; }

    public StatutAdhesion getStatut() { return statut; }
    public void setStatut(StatutAdhesion statut) { this.statut = statut; }
}
