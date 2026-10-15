package sn.naat.model;

import java.time.LocalDateTime;

public class JournalAudit {

    private String id;
    private LocalDateTime ts = LocalDateTime.now();
    private String acteurId;
    private String action;

    public JournalAudit() { }

    public JournalAudit(String id, String acteurId, String action) {
        this.id = id;
        this.acteurId = acteurId;
        this.action = action;
    }

    /** -formaterHorodatage(): String */
    private String formaterHorodatage() {
        return ts.toString();
    }

    /** +enregistrer(acteur, action): void — utilisé côté service pour créer une entrée. */
    public static JournalAudit enregistrer(String id, String acteurId, String action) {
        return new JournalAudit(id, acteurId, action);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDateTime getTs() { return ts; }
    public void setTs(LocalDateTime ts) { this.ts = ts; }

    public String getActeurId() { return acteurId; }
    public void setActeurId(String acteurId) { this.acteurId = acteurId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
