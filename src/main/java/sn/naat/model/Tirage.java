package sn.naat.model;

import java.time.LocalDate;

/** TontineTirageAuSort "1" --> "0..*" Tirage */
public class Tirage {

    private String id;
    private String tontineId;
    private int semaine;
    private int serie = 1; // série de la tontine à laquelle appartient ce tirage
    private LocalDate date = LocalDate.now();
    private String beneficiaireId;

    public Tirage() { }

    public Tirage(String id, String tontineId, int semaine, String beneficiaireId) {
        this.id = id;
        this.tontineId = tontineId;
        this.semaine = semaine;
        this.beneficiaireId = beneficiaireId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTontineId() { return tontineId; }
    public void setTontineId(String tontineId) { this.tontineId = tontineId; }

    public int getSemaine() { return semaine; }
    public void setSemaine(int semaine) { this.semaine = semaine; }

    public int getSerie() { return serie; }
    public void setSerie(int serie) { this.serie = serie; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getBeneficiaireId() { return beneficiaireId; }
    public void setBeneficiaireId(String beneficiaireId) { this.beneficiaireId = beneficiaireId; }
}
