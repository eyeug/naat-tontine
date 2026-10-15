package sn.naat.service;

import sn.naat.dao.DemandeReinitialisationDAO;
import sn.naat.dao.DiversDAO;
import sn.naat.dao.UtilisateurDAO;
import sn.naat.model.*;
import sn.naat.model.Enums.Espace;
import sn.naat.util.Utils;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * +seConnecter(login, motDePasse): boolean — détermine aussi le
 * rôle effectif (Membre / Gérant / Administrateur) après connexion,
 * puisque la hiérarchie du diagramme est résolue depuis les tables
 * membre / gerant / administrateur.
 */
public class AuthService {

    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final DemandeReinitialisationDAO demandeReinitialisationDAO = new DemandeReinitialisationDAO();
    private final DiversDAO diversDAO = new DiversDAO();
    private final RoleService roleService = new RoleService();

    /** Résultat de connexion : l'utilisateur concret le plus spécialisé possible. */
    public Optional<Utilisateur> seConnecter(String tel, String motDePasse) throws SQLException {
        Optional<Utilisateur> opt = utilisateurDAO.trouverParTelephone(tel);
        if (opt.isEmpty()) return Optional.empty();

        Utilisateur u = opt.get();
        if (!Utils.verifierMotDePasse(motDePasse, u.getMotDePasseHash())) return Optional.empty();
        if (!u.estActif()) return Optional.empty();

        if (utilisateurDAO.estAdministrateur(u.getId())) {
            return Optional.of(new Administrateur(u));
        }
        if (utilisateurDAO.estGerant(u.getId())) {
            return Optional.of(new Gerant(u));
        }
        // Membre = adhérent actif d'une tontine non terminée. Sinon : simple utilisateur (arrive sur l'accueil).
        if (roleService.estMembreActif(u.getId())) {
            return Optional.of(new Membre(u));
        }
        return Optional.of(u);
    }

    /**
     * +mettreAJourProfil(tel, email, ancienMotDePasse, nouveauMotDePasse) — action sur soi-même.
     * Le nom n'est pas modifiable. Pour changer de mot de passe, il faut fournir l'actuel : sans cela,
     * quiconque trouve une session ouverte pourrait prendre le compte. Un nouveau mot de passe vide
     * signifie « ne pas le changer ».
     *
     * Tout est validé AVANT la moindre écriture, puis enregistré en une seule requête :
     * un refus ne laisse jamais le profil modifié à moitié.
     *
     * @throws IllegalArgumentException si une valeur est invalide (téléphone, email, mot de passe)
     * @throws IllegalStateException    si le téléphone appartient déjà à un autre compte
     */
    public void mettreAJourProfil(String utilisateurId, String tel, String email,
                                  String ancienMotDePasse, String nouveauMotDePasse) throws SQLException {
        Utilisateur u = utilisateurDAO.trouverParId(utilisateurId)
                .orElseThrow(() -> new IllegalStateException("Compte introuvable."));
        String telNormalise = tel == null ? "" : tel.trim();
        String emailNormalise = ProfilRegles.normaliserEmail(email);

        // Le téléphone n'est contrôlé que s'il change : un ancien compte ne doit pas être bloqué
        // pour un numéro qu'il n'a pas touché.
        if (!telNormalise.equals(u.getTel())) {
            String refus = ProfilRegles.refusTelephone(telNormalise);
            if (refus != null) throw new IllegalArgumentException(refus);
            Optional<Utilisateur> autre = utilisateurDAO.trouverParTelephone(telNormalise);
            if (autre.isPresent() && !autre.get().getId().equals(u.getId())) {
                throw new IllegalStateException("Ce numéro de téléphone est déjà utilisé par un autre compte.");
            }
        }
        // Comme le téléphone, l'email n'est contrôlé que s'il change : un ancien compte dont l'adresse n'est pas
        // une adresse Gmail ne doit pas être bloqué pour modifier autre chose (son téléphone, son mot de passe).
        boolean emailModifie = !java.util.Objects.equals(emailNormalise, ProfilRegles.normaliserEmail(u.getEmail()));
        if (emailModifie) {
            String refusEmail = ProfilRegles.refusEmail(emailNormalise);
            if (refusEmail != null) throw new IllegalArgumentException(refusEmail);
        }

        String nouveauHash = null;
        boolean veutChangerMotDePasse = nouveauMotDePasse != null && !nouveauMotDePasse.isEmpty();
        boolean ancienSaisi = ancienMotDePasse != null && !ancienMotDePasse.isEmpty();
        if (veutChangerMotDePasse || ancienSaisi) {
            if (!veutChangerMotDePasse) {
                throw new IllegalArgumentException("Saisissez le nouveau mot de passe, "
                        + "ou laissez les trois champs de mot de passe vides.");
            }
            if (!ancienSaisi || !Utils.verifierMotDePasse(ancienMotDePasse, u.getMotDePasseHash())) {
                throw new IllegalArgumentException("Le mot de passe actuel est incorrect.");
            }
            String refusMotDePasse = ProfilRegles.refusNouveauMotDePasse(nouveauMotDePasse, ancienMotDePasse);
            if (refusMotDePasse != null) throw new IllegalArgumentException(refusMotDePasse);
            nouveauHash = Utils.hasher(nouveauMotDePasse);
        }

        try {
            utilisateurDAO.mettreAJourProfil(u.getId(), telNormalise, emailNormalise, nouveauHash);
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            // Un autre compte a pris ce numéro entre la vérification et l'écriture.
            throw new IllegalStateException("Ce numéro de téléphone est déjà utilisé par un autre compte.");
        }
    }

    /**
     * Changement OBLIGATOIRE après une réinitialisation : l'utilisateur vient de se connecter avec le
     * mot de passe provisoire remis par l'administrateur, et doit en choisir un qu'il est seul à connaître.
     */
    public void changerMotDePasseObligatoire(String utilisateurId, String nouveauMotDePasse) throws SQLException {
        Utilisateur u = utilisateurDAO.trouverParId(utilisateurId)
                .orElseThrow(() -> new IllegalStateException("Compte introuvable."));
        String refus = ProfilRegles.refusNouveauMotDePasse(nouveauMotDePasse, null);
        if (refus != null) throw new IllegalArgumentException(refus);
        if (Utils.verifierMotDePasse(nouveauMotDePasse, u.getMotDePasseHash())) {
            throw new IllegalArgumentException("Choisissez un mot de passe différent du mot de passe provisoire.");
        }
        utilisateurDAO.definirMotDePasse(u.getId(), Utils.hasher(nouveauMotDePasse), false);
    }

    /**
     * +demanderReinitialisationMotDePasse(tel) — depuis l'écran de connexion, sans être connecté.
     * Ne retourne rien et ne signale pas si le numéro existe : la réponse affichée est la même dans
     * tous les cas, pour qu'on ne puisse pas deviner quels numéros ont un compte.
     */
    public void demanderReinitialisationMotDePasse(String tel) throws SQLException {
        Optional<Utilisateur> opt = utilisateurDAO.trouverParTelephone(tel == null ? "" : tel.trim());
        if (opt.isEmpty()) return;
        Utilisateur u = opt.get();
        if (demandeReinitialisationDAO.existeEnAttente(u.getId())) return; // une demande est déjà en attente

        demandeReinitialisationDAO.creer(new sn.naat.model.DemandeReinitialisation(Utils.nouvelId(), u.getId()));
        for (String adminId : utilisateurDAO.listerAdministrateursIds()) {
            diversDAO.creerNotification(new Notification(Utils.nouvelId(), adminId,
                    u.getNom() + " (" + u.getTel() + ") a demandé la réinitialisation de son mot de passe : "
                            + "vérifiez son identité, puis générez un mot de passe provisoire.", "mot_de_passe"));
        }
    }

    /**
     * Espaces auxquels ce compte a accès, du plus élevé au moins élevé. À la connexion on arrive sur le
     * premier ; s'il y en a plusieurs, l'écran propose « Changer d'espace ».
     */
    public List<Espace> espacesDisponibles(String utilisateurId) throws SQLException {
        return EspacesRegles.espaces(utilisateurDAO.estAdministrateur(utilisateurId),
                utilisateurDAO.estGerant(utilisateurId), roleService.estMembreActif(utilisateurId));
    }

    public Utilisateur inscrire(String nom, String tel, String email, String motDePasseClair) throws SQLException {
        // Mêmes règles que « Mon profil » (et plus aucune valeur libre à l'inscription) : téléphone de 9 chiffres,
        // email Gmail facultatif. L'écran limite déjà la saisie ; ceci protège aussi tout autre appelant.
        String telNormalise = tel == null ? "" : tel.trim();
        String refusTel = ProfilRegles.refusTelephone(telNormalise);
        if (refusTel != null) throw new IllegalArgumentException(refusTel);
        String refusMail = ProfilRegles.refusEmail(email);
        if (refusMail != null) throw new IllegalArgumentException(refusMail);

        Utilisateur u = new Utilisateur(Utils.nouvelId(), nom, telNormalise, ProfilRegles.normaliserEmail(email));
        u.setMotDePasseHash(Utils.hasher(motDePasseClair));
        return utilisateurDAO.creerUtilisateur(u);
    }
}
