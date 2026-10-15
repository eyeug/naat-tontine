package sn.naat.model;

import sn.naat.model.Enums.StatutCycle;
import java.time.LocalDate;

/** Tontine "1" --> "1..*" Cycle */
public class Cycle {

    private String id;
    private String tontineId;
    private int numero;
    private int serie = 1; // série de la tontine à laquelle appartient ce cycle
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private StatutCycle statut = StatutCycle.EN_COURS;

    public Cycle() { }

    public Cycle(String id, String tontineId, int numero, LocalDate dateDebut) {
        this.id = id;
        this.tontineId = tontineId;
        this.numero = numero;
        this.dateDebut = dateDebut;
    }

    /** +demarrer(): void */
    public void demarrer() {
        this.statut = StatutCycle.EN_COURS;
        this.dateDebut = LocalDate.now();
    }

    /**
     * +cloturer(): Distribution — la création effective de la
     * Distribution (calcul du montant) est déléguée au service,
     * qui a accès aux transactions et au type de tontine.
     */
    public void cloturer() {
        this.statut = StatutCycle.CLOTURE;
        this.dateFin = LocalDate.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTontineId() { return tontineId; }
    public void setTontineId(String tontineId) { this.tontineId = tontineId; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public int getSerie() { return serie; }
    public void setSerie(int serie) { this.serie = serie; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public StatutCycle getStatut() { return statut; }
    public void setStatut(StatutCycle statut) { this.statut = statut; }
}
