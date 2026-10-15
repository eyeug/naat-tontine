package sn.naat.model;

/**
 * Membre extends Utilisateur — un compte ayant au moins une
 * adhésion validée. Les opérations de cotisation / prêt sont
 * portées ici plutôt que dans les services pour rester fidèle
 * au diagramme de classes, mais délèguent la persistance aux
 * services applicatifs (cette classe reste un objet métier,
 * pas un DAO).
 */
public class Membre extends Utilisateur {

    public Membre() {
        super();
    }

    public Membre(Utilisateur u) {
        super(u.getId(), u.getNom(), u.getTel(), u.getEmail());
        setEmail(u.getEmail());
        setStatutCompte(u.getStatutCompte());
        setDateInscription(u.getDateInscription());
        setMotDePasseHash(u.getMotDePasseHash());
        setDoitChangerMotDePasse(u.isDoitChangerMotDePasse());
    }
}
