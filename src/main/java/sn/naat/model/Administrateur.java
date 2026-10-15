package sn.naat.model;

/**
 * Utilisateur &lt;|-- Administrateur (hérite directement
 * d'Utilisateur, pas de Membre).
 */
public class Administrateur extends Utilisateur {

    public Administrateur() {
        super();
    }

    public Administrateur(Utilisateur u) {
        super(u.getId(), u.getNom(), u.getTel(), u.getEmail());
        setStatutCompte(u.getStatutCompte());
        setDateInscription(u.getDateInscription());
        setMotDePasseHash(u.getMotDePasseHash());
        setDoitChangerMotDePasse(u.isDoitChangerMotDePasse());
    }
}
