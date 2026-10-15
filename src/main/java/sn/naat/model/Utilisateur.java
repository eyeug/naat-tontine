package sn.naat.model;

import sn.naat.model.Enums.StatutCompte;
import java.time.LocalDateTime;

/**
 * Classe de base de la hiérarchie des comptes.
 * Utilisateur &lt;|-- Membre &lt;|-- Gerant
 * Utilisateur &lt;|-- Administrateur
 */
public class Utilisateur {

    private String id;
    private String nom;
    private String initials;
    private String tel;
    private String email;
    private String motDePasseHash;
    private StatutCompte statutCompte = StatutCompte.ACTIF;
    private LocalDateTime dateInscription;
    private boolean doitChangerMotDePasse; // vrai tant que l'utilisateur utilise un mot de passe provisoire

    public Utilisateur() { }

    public Utilisateur(String id, String nom, String tel, String email) {
        this.id = id;
        this.nom = nom;
        this.tel = tel;
        this.email = email;
        this.initials = genererInitiales(nom);
    }

    private String genererInitiales(String nomComplet) {
        if (nomComplet == null || nomComplet.isBlank()) return "??";
        String[] parts = nomComplet.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0)));
            if (sb.length() >= 2) break;
        }
        return sb.toString();
    }

    /** +mettreAJourProfil(tel, email): void — action sur soi-même, accessible à tout compte. */
    public void mettreAJourProfil(String tel, String email) {
        this.tel = tel;
        this.email = email;
    }

    public boolean estActif() {
        return statutCompte == StatutCompte.ACTIF;
    }

    // --- Getters / Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; this.initials = genererInitiales(nom); }

    public String getInitials() { return initials; }
    public void setInitials(String initials) { this.initials = initials; }

    public String getTel() { return tel; }
    public void setTel(String tel) { this.tel = tel; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasseHash() { return motDePasseHash; }
    public void setMotDePasseHash(String motDePasseHash) { this.motDePasseHash = motDePasseHash; }

    public StatutCompte getStatutCompte() { return statutCompte; }
    public void setStatutCompte(StatutCompte statutCompte) { this.statutCompte = statutCompte; }

    public boolean isDoitChangerMotDePasse() { return doitChangerMotDePasse; }
    public void setDoitChangerMotDePasse(boolean doitChangerMotDePasse) { this.doitChangerMotDePasse = doitChangerMotDePasse; }

    public LocalDateTime getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDateTime dateInscription) { this.dateInscription = dateInscription; }

    @Override
    public String toString() {
        return nom + " (" + tel + ")";
    }
}
