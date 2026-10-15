package sn.naat.service;

import sn.naat.dao.*;
import sn.naat.model.*;
import sn.naat.model.Enums.*;
import sn.naat.util.Utils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Opérations portées par la classe Administrateur du diagramme. */
public class AdministrateurService {

    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final DemandeCreationDAO demandeCreationDAO = new DemandeCreationDAO();
    private final TontineDAO tontineDAO = new TontineDAO();
    private final AdhesionDAO adhesionDAO = new AdhesionDAO();
    private final PretDAO pretDAO = new PretDAO();
    private final DiversDAO diversDAO = new DiversDAO();
    private final DemandeNouvelleSerieDAO demandeNouvelleSerieDAO = new DemandeNouvelleSerieDAO();
    private final DemandeReinitialisationDAO demandeReinitialisationDAO = new DemandeReinitialisationDAO();
    private final DemandeTourAnticipeDAO demandeTourAnticipeDAO = new DemandeTourAnticipeDAO();
    private final StatutTontineService statutTontineService = new StatutTontineService();
    private final SerieService serieService = new SerieService();
    private final RoleService roleService = new RoleService();

    /** Ligne affichée dans l'onglet « Mots de passe oubliés » : la demande + le nom et le téléphone du demandeur. */
    public record DemandeReinitialisationAffichee(DemandeReinitialisation demande, String nom, String tel) { }

    public List<DemandeReinitialisationAffichee> demandesReinitialisationEnAttente() throws SQLException {
        List<DemandeReinitialisationAffichee> lignes = new java.util.ArrayList<>();
        for (DemandeReinitialisation d : demandeReinitialisationDAO.listerEnAttente()) {
            Optional<Utilisateur> u = utilisateurDAO.trouverParId(d.getUtilisateurId());
            lignes.add(new DemandeReinitialisationAffichee(d,
                    u.map(Utilisateur::getNom).orElse("(compte introuvable)"),
                    u.map(Utilisateur::getTel).orElse("")));
        }
        return lignes;
    }

    /**
     * +reinitialiserMotDePasse(demande): String — à n'utiliser qu'APRÈS avoir vérifié l'identité du
     * demandeur (par exemple par un appel). Génère un mot de passe provisoire, affiché une seule fois
     * à l'administrateur, que l'utilisateur devra changer dès sa prochaine connexion.
     * L'opération est inscrite au journal d'audit.
     */
    public String genererMotDePasseProvisoire(DemandeReinitialisation demande, String adminId) throws SQLException {
        DemandeReinitialisation courante = demandeReinitialisationDAO.trouverParId(demande.getId())
                .orElseThrow(() -> new IllegalStateException("Demande introuvable."));
        if (courante.getStatut() != StatutDemande.EN_ATTENTE) {
            throw new IllegalStateException("Cette demande a déjà été traitée.");
        }
        Utilisateur u = utilisateurDAO.trouverParId(courante.getUtilisateurId())
                .orElseThrow(() -> new IllegalStateException("Le compte de cette demande n'existe plus."));

        // On marque d'abord la demande : si deux administrateurs agissent en même temps, un seul passe,
        // et le second ne peut pas écraser le mot de passe que le premier vient de communiquer.
        if (demandeReinitialisationDAO.changerStatut(courante.getId(), StatutDemande.VALIDEE) == 0) {
            throw new IllegalStateException("Cette demande a déjà été traitée.");
        }
        String provisoire = Utils.genererMotDePasseProvisoire();
        utilisateurDAO.definirMotDePasse(u.getId(), Utils.hasher(provisoire), true);
        diversDAO.enregistrer(JournalAudit.enregistrer(Utils.nouvelId(), adminId,
                "Mot de passe provisoire généré pour " + u.getNom() + " (" + u.getTel() + ")"));
        return provisoire;
    }

    /** Refus (identité non vérifiée) : le mot de passe de l'utilisateur n'est pas modifié. */
    public void refuserReinitialisation(DemandeReinitialisation demande, String adminId) throws SQLException {
        if (demandeReinitialisationDAO.changerStatut(demande.getId(), StatutDemande.REFUSEE) == 0) {
            throw new IllegalStateException("Cette demande a déjà été traitée.");
        }
        Optional<Utilisateur> u = utilisateurDAO.trouverParId(demande.getUtilisateurId());
        diversDAO.enregistrer(JournalAudit.enregistrer(Utils.nouvelId(), adminId,
                "Demande de réinitialisation refusée pour " + u.map(Utilisateur::getNom).orElse("(compte introuvable)")));
    }

    /** Ligne affichée dans l'onglet « Nouvelles séries » : la demande + les noms à afficher. */
    public record DemandeNouvelleSerieAffichee(DemandeNouvelleSerie demande, String nomTontine,
                                               String nomGerant, int numeroSerieDemandee) { }

    /** Demandes de nouvelle série en attente, avec les noms déjà résolus (pas d'accès base à l'affichage). */
    public List<DemandeNouvelleSerieAffichee> demandesNouvelleSerieEnAttente() throws SQLException {
        List<DemandeNouvelleSerieAffichee> lignes = new java.util.ArrayList<>();
        for (DemandeNouvelleSerie d : demandeNouvelleSerieDAO.listerEnAttente()) {
            lignes.add(new DemandeNouvelleSerieAffichee(d, nomTontine(d.getTontineId()),
                    nomUtilisateur(d.getGerantId()), serieService.serieCourante(d.getTontineId()) + 1));
        }
        return lignes;
    }

    /**
     * +validerDemandeNouvelleSerie(demande, statut: String): void
     * Valider : la tontine démarre une nouvelle série (mêmes règles). Les adhésions de la série terminée
     * prennent fin, sauf celle du gérant : chaque ancien membre décide lui-même de rester en redemandant à
     * rejoindre, le gérant valide, et le premier cycle ne démarre qu'une fois le nombre de membres demandé
     * atteint. Le tour de rôle repart de zéro et la tontine repasse « En préparation ».
     */
    public void validerNouvelleSerie(DemandeNouvelleSerie demande, String statut) throws SQLException {
        DemandeNouvelleSerie courante = demandeNouvelleSerieDAO.trouverParId(demande.getId())
                .orElseThrow(() -> new IllegalStateException("Demande introuvable."));
        if (courante.getStatut() != StatutDemande.EN_ATTENTE) {
            throw new IllegalStateException("Cette demande a déjà été traitée.");
        }
        Tontine tontine = tontineDAO.trouverParId(courante.getTontineId())
                .orElseThrow(() -> new IllegalStateException("Tontine introuvable."));
        boolean acceptee = "validee".equals(statut);

        // La série doit toujours être terminée au moment de la validation (un membre a pu
        // rejoindre la tontine depuis la demande, ce qui rouvrirait la série en cours).
        if (acceptee && statutTontineService.statutDe(tontine) != StatutTontine.CLOTUREE) {
            throw new IllegalStateException("La série en cours de « " + tontine.getNom() + " » n'est plus terminée "
                    + "(un membre a peut-être rejoint la tontine depuis la demande) : validation impossible.");
        }
        // Les adhésions vont prendre fin : on ne le fait pas tant que des prêts sont à rembourser (les anciens
        // membres ne verraient plus la tontine pour les rembourser).
        if (acceptee) {
            String refusPrets = SerieRegles.refusPretsNonSoldes(pretDAO.compterNonSoldes(tontine.getId()));
            if (refusPrets != null) {
                throw new IllegalStateException(refusPrets);
            }
        }

        // Le changement de statut ne touche que les demandes encore en attente : une seconde
        // validation simultanée de la même demande ne peut donc pas faire avancer la série deux fois.
        int modifiees = demandeNouvelleSerieDAO.changerStatut(courante.getId(),
                acceptee ? StatutDemande.VALIDEE : StatutDemande.REFUSEE);
        if (modifiees == 0) {
            throw new IllegalStateException("Cette demande a déjà été traitée.");
        }

        if (!acceptee) {
            diversDAO.creerNotification(new Notification(Utils.nouvelId(), courante.getGerantId(),
                    "Votre demande de nouvelle série pour « " + tontine.getNom() + " » a été refusée.",
                    "nouvelle_serie"));
            return;
        }

        // ---- La série précédente est terminée : les adhésions prennent fin (sauf celle du gérant) ----
        // Chacun décide lui-même de rester : il redemande à rejoindre, le gérant valide. On retient d'abord
        // qui était membre, pour pouvoir les prévenir une fois les adhésions supprimées.
        String gerantId = tontine.getGerantId();
        List<String> anciensMembres = adhesionDAO.listerParTontine(tontine.getId()).stream()
                .filter(a -> a.getStatut() == StatutAdhesion.ACTIF && !a.getMembreId().equals(gerantId))
                .map(Adhesion::getMembreId)
                .toList();
        adhesionDAO.reinitialiserPourNouvelleSerie(tontine.getId(), gerantId);
        // Les demandes de tour anticipé en attente concernaient l'ancienne série : elles n'ont plus d'objet.
        demandeTourAnticipeDAO.refuserToutesEnAttente(tontine.getId());

        // Le tour de rôle repart de zéro : seuls ceux qui rejoindront la nouvelle série y seront ajoutés.
        if (tontine instanceof TontineTourDeRole tourDeRole) {
            tontineDAO.mettreAJourTourDeRole(tourDeRole.getId(),
                    SerieRegles.ordreDuTourNouvelleSerie(tourDeRole.getOrdreTour(), gerantId), 0);
        }

        int numeroSerie = serieService.serieCourante(tontine.getId());
        for (String membreId : anciensMembres) {
            diversDAO.creerNotification(new Notification(Utils.nouvelId(), membreId,
                    "La série n°" + (numeroSerie - 1) + " de « " + tontine.getNom() + " » est terminée et votre "
                            + "adhésion a pris fin. Une nouvelle série (n°" + numeroSerie + ") s'ouvre : si vous "
                            + "souhaitez y participer, demandez à rejoindre la tontine.", "nouvelle_serie"));
        }
        diversDAO.creerNotification(new Notification(Utils.nouvelId(), gerantId,
                "La série n°" + numeroSerie + " de « " + tontine.getNom() + " » a été validée. Les adhésions de "
                        + "la série précédente ont pris fin : chaque ancien membre doit redemander à rejoindre. "
                        + "Le premier cycle pourra démarrer quand le nombre de membres demandé ("
                        + tontine.getNbMembresMax() + ") sera atteint.", "nouvelle_serie"));
    }

    /**
     * Un administrateur ne peut pas devenir gérant (séparation des rôles). Le seul chemin vers le rôle de
     * gérant est la validation d'une demande de création de tontine, qui appelle cette vérification.
     */
    public void verifierPeutPromouvoirGerant(String utilisateurId) throws SQLException {
        String refus = AdministrationRegles.refusPromotionGerant(utilisateurDAO.estAdministrateur(utilisateurId));
        if (refus != null) {
            throw new IllegalStateException(refus);
        }
    }

    /**
     * Vérifie qu'un compte peut être promu administrateur : pas déjà administrateur, et limite du nombre
     * d'administrateurs non atteinte. Appelée par l'écran AVANT la confirmation (inutile de demander
     * « Confirmer ? » pour refuser derrière), et par promouvoirAdministrateur() par sécurité.
     *
     * @throws IllegalStateException avec le motif du refus
     */
    public void verifierPeutPromouvoirAdministrateur(String utilisateurId) throws SQLException {
        // Séparation des rôles : on ne promeut pas administrateur un gérant ni un membre actif.
        boolean gerantOuMembreActif = utilisateurDAO.estGerant(utilisateurId) || roleService.estMembreActif(utilisateurId);
        String refus = AdministrationRegles.refusPromotion(
                utilisateurDAO.estAdministrateur(utilisateurId), gerantOuMembreActif, utilisateurDAO.compterAdministrateurs());
        if (refus != null) {
            throw new IllegalStateException(refus);
        }
    }

    /** Promeut un utilisateur en Administrateur — réservé aux administrateurs existants. */
    public void promouvoirAdministrateur(String utilisateurId) throws SQLException {
        verifierPeutPromouvoirAdministrateur(utilisateurId); // par sécurité : l'écran l'a déjà vérifié
        utilisateurDAO.promouvoirAdministrateur(utilisateurId);
        diversDAO.creerNotification(new Notification(Utils.nouvelId(), utilisateurId,
                "Vous avez été promu administrateur.", "role"));
    }

    /**
     * Vérifie qu'on peut retirer le rôle d'administrateur à ce compte. Appelée par l'écran AVANT la
     * confirmation, et par retirerRoleAdministrateur() par sécurité.
     *
     * @param adminId l'administrateur qui agit
     * @throws IllegalStateException avec le motif du refus
     */
    public void verifierPeutRetirerRoleAdministrateur(String utilisateurId, String adminId) throws SQLException {
        boolean estAdministrateur = utilisateurDAO.estAdministrateur(utilisateurId);
        boolean estActif = utilisateurDAO.trouverParId(utilisateurId).map(Utilisateur::estActif).orElse(false);
        String refus = AdministrationRegles.refusRetraitRole(estAdministrateur, estActif,
                utilisateurDAO.compterAdministrateursActifs(), utilisateurId.equals(adminId));
        if (refus != null) {
            throw new IllegalStateException(refus);
        }
    }

    /**
     * +retirerRoleAdministrateur(utilisateur): void — retire le rôle d'administrateur, ce qui libère une place
     * sous la limite. Le compte garde ses autres rôles. Inscrit au journal d'audit, et le compte est prévenu.
     */
    public void retirerRoleAdministrateur(String utilisateurId, String adminId) throws SQLException {
        verifierPeutRetirerRoleAdministrateur(utilisateurId, adminId); // par sécurité : l'écran l'a déjà vérifié
        Optional<Utilisateur> cible = utilisateurDAO.trouverParId(utilisateurId);
        utilisateurDAO.retirerAdministrateur(utilisateurId);
        diversDAO.creerNotification(new Notification(Utils.nouvelId(), utilisateurId,
                "Votre rôle d'administrateur a été retiré.", "role"));
        diversDAO.enregistrer(JournalAudit.enregistrer(Utils.nouvelId(), adminId,
                "Rôle d'administrateur retiré à " + cible.map(Utilisateur::getNom).orElse("(compte introuvable)")));
    }

    /** +suspendreCompte(utilisateur): void */
    public void suspendreCompte(String utilisateurId) throws SQLException {
        // Jamais le dernier administrateur actif : l'application deviendrait impossible à administrer.
        boolean cibleEstAdministrateurActif = utilisateurDAO.estAdministrateur(utilisateurId)
                && utilisateurDAO.trouverParId(utilisateurId).map(Utilisateur::estActif).orElse(false);
        String refus = AdministrationRegles.refusSuspension(cibleEstAdministrateurActif,
                utilisateurDAO.compterAdministrateursActifs());
        if (refus != null) {
            throw new IllegalStateException(refus);
        }
        utilisateurDAO.suspendreCompte(utilisateurId);
        diversDAO.creerNotification(new Notification(Utils.nouvelId(), utilisateurId,
                "Votre compte a été suspendu par un administrateur.", "compte"));
    }

    /** +reactiverCompte(utilisateur): void */
    public void reactiverCompte(String utilisateurId) throws SQLException {
        utilisateurDAO.reactiverCompte(utilisateurId);
        diversDAO.creerNotification(new Notification(Utils.nouvelId(), utilisateurId,
                "Votre compte a été réactivé.", "compte"));
    }

    /** +validerDemandeCreation(demande, statut: String): Tontine */
    public Optional<Tontine> validerDemandeCreation(DemandeCreation demande, String statut) throws SQLException {
        boolean acceptee = "validee".equals(statut);
        if (acceptee) {
            // Valider fait du demandeur un gérant : impossible si c'est un administrateur (vérifié AVANT de
            // changer le statut, la demande peut encore être refusée).
            verifierPeutPromouvoirGerant(demande.getDemandeurId());
        }
        demandeCreationDAO.changerStatut(demande.getId(), acceptee ? StatutDemande.VALIDEE : StatutDemande.REFUSEE);
        if (!acceptee) return Optional.empty();

        utilisateurDAO.promouvoirGerant(demande.getDemandeurId());

        Tontine t = switch (demande.getTypeSouhaite()) {
            case FONDS_COMMUN -> new TontineFondsCommun(Utils.nouvelId(), demande.getNomSouhaite(),
                    demande.getFrequence(), demande.getMontant(), demande.getNbMembres(),
                    demande.getDemandeurId(), new BigDecimal("5.0"), 30);
            case TOUR_DE_ROLE -> new TontineTourDeRole(Utils.nouvelId(), demande.getNomSouhaite(),
                    demande.getFrequence(), demande.getMontant(), demande.getNbMembres(), demande.getDemandeurId());
            case TIRAGE_AU_SORT -> new TontineTirageAuSort(Utils.nouvelId(), demande.getNomSouhaite(),
                    demande.getFrequence(), demande.getMontant(), demande.getNbMembres(), demande.getDemandeurId());
        };
        if (demande.getTypeSouhaite() == sn.naat.model.Enums.TypeTontine.FONDS_COMMUN) {
            t.setMontantMin(demande.getMontantMin());
            t.setMontantMax(demande.getMontantMax());
        }
        t.setDureeLabel(sn.naat.util.DureeTontine.genererDureeLabel(
                demande.getTypeSouhaite(), demande.getNbMembres(), demande.getFrequence()));
        tontineDAO.creer(t);

        Adhesion adhesionGerant = new Adhesion(Utils.nouvelId(), t.getId(), demande.getDemandeurId(), RoleAdhesion.GERANT);
        adhesionDAO.creer(adhesionGerant);

        diversDAO.creerNotification(new Notification(Utils.nouvelId(), demande.getDemandeurId(),
                "Votre tontine \"" + t.getNom() + "\" a été créée. Vous en êtes le gérant.", "creation_tontine"));

        return Optional.of(t);
    }

    /** Prêts en attente demandés par le gérant de leur propre tontine (bloqués côté Gerant.validerPret par la règle anti-conflit). */
    public List<Pret> pretsDeGerantsEnAttente() throws SQLException {
        List<Pret> resultat = new java.util.ArrayList<>();
        for (Pret p : pretDAO.listerTousEnAttente()) {
            tontineDAO.trouverParId(p.getTontineId())
                    .filter(t -> t.getGerantId().equals(p.getMembreId()))
                    .ifPresent(t -> resultat.add(p));
        }
        return resultat;
    }

    /** +validerPretGerant(pret, tontine: TontineFondsCommun, statut: String): void */
    public void validerPretGerant(Pret pret, TontineFondsCommun tontine, String statut) throws SQLException {
        boolean approuve = "valide".equals(statut);
        pretDAO.changerStatut(pret.getId(), approuve ? StatutPret.VALIDE : StatutPret.REFUSE, Valideur.ADMINISTRATEUR);
        diversDAO.creerNotification(new Notification(Utils.nouvelId(), pret.getMembreId(),
                "Votre demande de prêt (examinée par l'administrateur) a été "
                        + (approuve ? "validée" : "refusée") + ".", "pret"));
    }

    /** +consulterJournalAudit(): List */
    public List<JournalAudit> consulterJournalAudit() throws SQLException {
        return diversDAO.listerTout();
    }

    public List<DemandeCreation> demandesCreationEnAttente() throws SQLException {
        return demandeCreationDAO.listerEnAttente();
    }

    /** Prêts demandés par un gérant sur sa propre tontine — que lui-même ne peut pas valider. */
    public List<Pret> pretsGerantsEnAttente() throws SQLException {
        return pretDAO.listerPretsGerantsEnAttente();
    }

    public String nomTontine(String tontineId) throws SQLException {
        return tontineDAO.trouverParId(tontineId).map(Tontine::getNom).orElse("(tontine introuvable)");
    }

    public String nomUtilisateur(String utilisateurId) throws SQLException {
        return utilisateurDAO.trouverParId(utilisateurId).map(Utilisateur::getNom).orElse("(compte introuvable)");
    }

    /** Récupère la tontine d'un prêt, typée TontineFondsCommun (seul type autorisant les prêts). */
    public Optional<TontineFondsCommun> tontineFondsCommunDuPret(Pret pret) throws SQLException {
        return tontineDAO.trouverParId(pret.getTontineId())
                .filter(t -> t instanceof TontineFondsCommun)
                .map(t -> (TontineFondsCommun) t);
    }

    public List<Utilisateur> tousLesUtilisateurs() throws SQLException {
        return utilisateurDAO.listerTous();
    }

    /** Libellé du rôle le plus élevé de cet utilisateur, pour affichage. */
    public String roleDe(String utilisateurId) throws SQLException {
        if (utilisateurDAO.estAdministrateur(utilisateurId)) return "Administrateur";
        if (utilisateurDAO.estGerant(utilisateurId)) return "Gérant";
        if (roleService.estMembreActif(utilisateurId)) return "Membre";
        return "Simple utilisateur";
    }

    public List<Tontine> toutesLesTontines() throws SQLException {
        return tontineDAO.listerToutes();
    }
}
