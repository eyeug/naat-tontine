package sn.naat.model;

import sn.naat.model.Enums.Frequence;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Classe de base des trois types de tontine.
 * Tontine &lt;|-- TontineFondsCommun
 * Tontine &lt;|-- TontineTourDeRole
 * Tontine &lt;|-- TontineTirageAuSort
 */
public abstract class Tontine {

    private String id;
    private String nom;
    private String description;
    private Frequence frequence;
    private String dureeLabel;
    private BigDecimal montantCotisation;
    private BigDecimal montantMin; // Fonds commun : borne basse de la cotisation (null = pas de fourchette)
    private BigDecimal montantMax; // Fonds commun : borne haute
    private BigDecimal penaliteRetard = BigDecimal.ZERO;
    private int nbMembresMax;
    private String gerantId;
    private LocalDateTime dateCreation;

    protected Tontine() { }

    protected Tontine(String id, String nom, Frequence frequence,
                       BigDecimal montantCotisation, int nbMembresMax, String gerantId) {
        this.id = id;
        this.nom = nom;
        this.frequence = frequence;
        this.montantCotisation = montantCotisation;
        this.nbMembresMax = nbMembresMax;
        this.gerantId = gerantId;
    }

    /** Nom du type pour affichage / persistance (discriminant côté BD). */
    public abstract String getTypeCode();

    /** Libellé lisible du type, pour l'interface. */
    public abstract String getTypeLibelle();

    // --- Getters / Setters communs ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Frequence getFrequence() { return frequence; }
    public void setFrequence(Frequence frequence) { this.frequence = frequence; }

    public String getDureeLabel() { return dureeLabel; }
    public void setDureeLabel(String dureeLabel) { this.dureeLabel = dureeLabel; }

    public BigDecimal getMontantCotisation() { return montantCotisation; }
    public void setMontantCotisation(BigDecimal montantCotisation) { this.montantCotisation = montantCotisation; }

    public BigDecimal getMontantMin() { return montantMin; }
    public void setMontantMin(BigDecimal montantMin) { this.montantMin = montantMin; }

    public BigDecimal getMontantMax() { return montantMax; }
    public void setMontantMax(BigDecimal montantMax) { this.montantMax = montantMax; }

    public BigDecimal getPenaliteRetard() { return penaliteRetard; }
    public void setPenaliteRetard(BigDecimal penaliteRetard) { this.penaliteRetard = penaliteRetard; }

    public int getNbMembresMax() { return nbMembresMax; }
    public void setNbMembresMax(int nbMembresMax) { this.nbMembresMax = nbMembresMax; }

    public String getGerantId() { return gerantId; }
    public void setGerantId(String gerantId) { this.gerantId = gerantId; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    @Override
    public String toString() {
        return nom + " (" + getTypeLibelle() + ")";
    }
}
