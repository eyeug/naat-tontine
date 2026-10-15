package sn.naat.service;

import sn.naat.dao.*;
import sn.naat.model.*;
import sn.naat.model.Enums.*;
import sn.naat.util.Utils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Opérations portées par la classe Membre du diagramme :
 * cotiser(), demanderPret(), demanderTourAnticipe(), consulterHistorique().
 * Les paramètres restent typés sur le bon sous-type de Tontine,
 * comme dans le diagramme (vérifié ici par instanceof, l'équivalent
 * Java du typage statique du diagramme).
 */
public class MembreService {

    private final AdhesionDAO adhesionDAO = new AdhesionDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final ModePaiementDAO modePaiementDAO = new ModePaiementDAO();
    private final PretDAO pretDAO = new PretDAO();
    private final TontineDAO tontineDAO = new TontineDAO();
    private final DemandeAdhesionDAO demandeAdhesionDAO = new DemandeAdhesionDAO();
    private final DemandeCreationDAO demandeCreationDAO = new DemandeCreationDAO();
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final CycleDAO cycleDAO = new CycleDAO();
    private final DiversDAO diversDAO = new DiversDAO();
    private final RoleService roleService = new RoleService();
    private final StatutTontineService statutTontineService = new StatutTontineService();
    private final DemandeTourAnticipeDAO demandeTourAnticipeDAO = new DemandeTourAnticipeDAO();
    private final SerieService serieService = new SerieService();

    /** +cotiser(tontine, montant, mode): Transaction */
    public Transaction cotiser(Tontine tontine, String membreId, BigDecimal montant, ModePaiement mode) throws SQLException {
        Optional<Adhesion> opt = adhesionDAO.trouver(tontine.getId(), membreId);
        if (opt.isEmpty()) throw new IllegalStateException("Ce membre n'a pas d'adhésion active sur cette tontine.");
        Adhesion adhesion = opt.get();
        // Une adhésion désactivée par le gérant ne cotise plus (le membre peut toujours rembourser un prêt).
        String refusInactive = CotisationRegles.refusAdhesionInactive(adhesion.getStatut() == StatutAdhesion.ACTIF, true);
        if (refusInactive != null) {
            throw new IllegalStateException(refusInactive);
        }

        // Montant fixe (Tour de rôle / Tirage au sort), cycle actif, une seule
        // cotisation par cycle : règles partagées avec la saisie en espèces du gérant.
        CotisationRegles.verifier(tontine, membreId, montant);

        // 1. Initier le paiement (polymorphisme : Wave / OrangeMoney / Espèces)
        mode.initierPaiement(montant);
        modePaiementDAO.enregistrer(mode);

        // 2. Enregistrer la transaction
        Transaction t = new Transaction(Utils.nouvelId(), tontine.getId(), membreId,
                TypeTransaction.COTISATION, montant, mode.getId());
        transactionDAO.creer(t);

        // 3. Mettre à jour le solde de l'adhésion (Adhesion.enregistrerCotisation)
        adhesionDAO.enregistrerCotisation(adhesion.getId(), montant);

        diversDAO.creerNotification(new Notification(Utils.nouvelId(), membreId,
                "Cotisation de " + montant + " FCFA enregistrée sur " + tontine.getNom() +
                        " via " + mode.getLibelle() + ".", "cotisation"));

        return t;
    }

    /** +demanderPret(tontine: TontineFondsCommun, montant, duree, motif): Pret */
    public Pret demanderPret(TontineFondsCommun tontine, String membreId, BigDecimal montant,
                              int dureeJours, String motif) throws SQLException {
        if (dureeJours > tontine.getDureeMaxPret()) {
            throw new IllegalArgumentException("Durée demandée supérieure à la durée maximale autorisée ("
                    + tontine.getDureeMaxPret() + " jours).");
        }

        // Un membre ne peut avoir qu'un seul prêt actif à la fois sur cette
        // tontine (pas de nouvelle demande tant qu'un prêt précédent n'est
        // pas remboursé, ni tant qu'une demande est encore en attente).
        boolean aUnPretEnCours = pretDAO.listerParMembre(membreId).stream()
                .filter(p -> p.getTontineId().equals(tontine.getId()))
                .anyMatch(p -> p.getStatut() == StatutPret.EN_ATTENTE || p.getStatut() == StatutPret.VALIDE);
        if (aUnPretEnCours) {
            throw new IllegalStateException("Vous avez déjà un prêt en attente ou non remboursé sur cette tontine. "
                    + "Remboursez-le avant d'en demander un nouveau.");
        }

        // Plafond : le prêt ne peut pas dépasser le triple du solde déjà cotisé.
        Adhesion adhesion = adhesionDAO.trouver(tontine.getId(), membreId)
                .orElseThrow(() -> new IllegalStateException("Aucune adhésion active sur cette tontine."));
        String refusInactive = CotisationRegles.refusAdhesionInactive(adhesion.getStatut() == StatutAdhesion.ACTIF, true);
        if (refusInactive != null) {
            throw new IllegalStateException(refusInactive);
        }
        BigDecimal plafond = adhesion.getSolde().multiply(new BigDecimal("3"));
        if (montant.compareTo(plafond) > 0) {
            throw new IllegalArgumentException("Montant demandé (" + montant + " FCFA) supérieur au plafond "
                    + "autorisé (3 × votre solde = " + plafond + " FCFA). Cotisez davantage pour emprunter plus.");
        }

        Pret p = new Pret(Utils.nouvelId(), tontine.getId(), membreId, montant, dureeJours, motif);
        pretDAO.creer(p);
        return p;
    }

    /**
     * Remboursement d'un prêt (capital + intérêt au taux de la tontine).
     * Alimente le calcul de la répartition des intérêts à la clôture du
     * cycle (TontineFondsCommun.repartirInterets()).
     */
    public Transaction rembourserPret(Pret pret, TontineFondsCommun tontine, ModePaiement mode) throws SQLException {
        if (pret.getStatut() != StatutPret.VALIDE) {
            throw new IllegalStateException("Seul un prêt validé peut être remboursé.");
        }
        BigDecimal interet = pret.getMontant()
                .multiply(tontine.getTauxInteret())
                .divide(new BigDecimal("100"));
        BigDecimal montantTotal = pret.getMontant().add(interet);

        mode.initierPaiement(montantTotal);
        modePaiementDAO.enregistrer(mode);

        Transaction t = new Transaction(Utils.nouvelId(), tontine.getId(), pret.getMembreId(),
                TypeTransaction.REMBOURSEMENT_PRET, montantTotal, mode.getId());
        transactionDAO.creer(t);

        pretDAO.rembourser(pret.getId(), montantTotal);

        diversDAO.creerNotification(new Notification(Utils.nouvelId(), pret.getMembreId(),
                "Remboursement de " + montantTotal + " FCFA (dont " + interet + " FCFA d'intérêt) enregistré sur "
                        + tontine.getNom() + ".", "remboursement"));

        return t;
    }

    /** +demanderTourAnticipe(tontine: TontineTourDeRole, motif): void */
    public void demanderTourAnticipe(TontineTourDeRole tontine, String membreId, String motif) throws SQLException {
        if (motif == null || motif.isBlank()) {
            throw new IllegalArgumentException("Précisez le motif de votre demande.");
        }
        verifierPeutDemanderTourAnticipe(tontine, membreId);
        demandeTourAnticipeDAO.creer(new DemandeTourAnticipe(Utils.nouvelId(), tontine.getId(), membreId, motif.trim()));
        diversDAO.creerNotification(new Notification(Utils.nouvelId(), tontine.getGerantId(),
                nomUtilisateur(membreId) + " demande un tour anticipé sur « " + tontine.getNom() + " » : "
                        + motif.trim(), "tour_anticipe"));
    }

    /**
     * Vérifie qu'un membre peut demander un tour anticipé sur cette tontine. Appelée par l'écran AVANT d'ouvrir
     * le formulaire (inutile de le faire remplir pour refuser derrière), et par demanderTourAnticipe().
     *
     * @throws IllegalStateException avec le motif du refus
     */
    public void verifierPeutDemanderTourAnticipe(Tontine tontine, String membreId) throws SQLException {
        boolean tourDeRole = tontine instanceof TontineTourDeRole;
        StatutTontine statut = statutTontineService.statutDe(tontine);
        boolean adhesionActive = adhesionDAO.trouver(tontine.getId(), membreId)
                .map(a -> a.getStatut() == StatutAdhesion.ACTIF).orElse(false);

        java.util.List<String> ordre = tourDeRole ? ((TontineTourDeRole) tontine).getOrdreTour() : java.util.List.of();
        int indexCourant = tourDeRole ? ((TontineTourDeRole) tontine).getTourActuelIndex() : 0;
        int position = ordre.indexOf(membreId);

        java.util.Set<String> servis = transactionDAO.membresAyantRecuVersement(tontine.getId(),
                serieService.serieCourante(tontine.getId()));
        java.util.Set<String> membresActifs = adhesionDAO.listerParTontine(tontine.getId()).stream()
                .filter(a -> a.getStatut() == StatutAdhesion.ACTIF)
                .map(Adhesion::getMembreId)
                .collect(java.util.stream.Collectors.toSet());
        // « Prochain bénéficiaire » : même calcul que « Avancer le tour » (saute les servis et les désactivés).
        java.util.Set<String> ecartes = DistributionRegles.servisOuDesactives(ordre, servis, membresActifs);
        boolean estProchain = position >= 0
                && DistributionRegles.prochainNonServi(ordre, indexCourant, ecartes) == position;

        String refus = TourAnticipeRegles.refusDemande(tourDeRole, statut, adhesionActive, position >= 0,
                servis.contains(membreId), position >= 0 && position == indexCourant, estProchain,
                demandeTourAnticipeDAO.existeEnAttente(tontine.getId(), membreId));
        if (refus != null) {
            throw new IllegalStateException(refus);
        }
    }

    /**
     * +consulterHistorique(tontine): List — transactions du membre sur UNE
     * tontine précise. Chaque tontine a son propre historique : on ne mélange
     * pas les cotisations, versements et remboursements de tontines différentes.
     */
    public List<Transaction> consulterHistorique(Tontine tontine, String membreId) throws SQLException {
        return transactionDAO.listerParTontineEtMembre(tontine.getId(), membreId);
    }

    /**
     * État de la cotisation du membre pour le cycle en cours de cette tontine : « déjà cotisé » ou
     * « à faire avant la clôture (sinon pénalité) ». Même critère que l'application de la pénalité
     * à la clôture : aucune cotisation depuis le début du cycle.
     */
    public EtatCotisationRegles.EtatCotisation etatCotisation(Tontine tontine, String membreId) throws SQLException {
        boolean adhesionActive = adhesionDAO.trouver(tontine.getId(), membreId)
                .map(a -> a.getStatut() == StatutAdhesion.ACTIF)
                .orElse(false);
        Optional<Cycle> cycle = cycleDAO.trouverCycleEnCours(tontine.getId());
        boolean dejaCotise = cycle.isPresent() && transactionDAO.aDejaCotiseDepuis(
                tontine.getId(), membreId, cycle.get().getDateDebut().atStartOfDay());
        return EtatCotisationRegles.calculer(cycle.isPresent(), cycle.map(Cycle::getNumero).orElse(0),
                adhesionActive, dejaCotise, tontine.getPenaliteRetard());
    }

    /**
     * Date de fin prévue d'une série complète (Tour de rôle / Tirage au
     * sort) : nombre de membres × durée d'un cycle selon la fréquence.
     * Retourne null pour un fonds commun, qui n'a pas de fin naturelle.
     */
    public java.time.LocalDate calculerDateFinPrevue(Tontine tontine) {
        java.time.LocalDate depart = tontine.getDateCreation() != null
                ? tontine.getDateCreation().toLocalDate() : java.time.LocalDate.now();
        return sn.naat.util.DureeTontine.dateFinPrevue(
                depart, TypeTontine.valueOf(tontine.getTypeCode()), tontine.getNbMembresMax(), tontine.getFrequence());
    }

    /** Consulte le solde courant d'un membre sur une tontine (Adhesion.solde). */
    public Adhesion consulterSolde(String tontineId, String membreId) throws SQLException {
        return adhesionDAO.trouver(tontineId, membreId)
                .orElseThrow(() -> new IllegalStateException("Aucune adhésion active sur cette tontine."));
    }

    /**
     * Consulte l'historique des distributions d'une tontine — commun aux
     * trois types (une Distribution est produite à chaque clôture de Cycle,
     * quel que soit le type de tontine).
     */
    public List<Distribution> consulterDistributions(String tontineId) throws SQLException {
        return cycleDAO.listerDistributionsParTontine(tontineId);
    }

    /** Nom d'affichage d'un utilisateur (bénéficiaire d'une distribution, par exemple). */
    public String nomUtilisateur(String utilisateurId) throws SQLException {
        return utilisateurDAO.trouverParId(utilisateurId).map(Utilisateur::getNom).orElse("(compte introuvable)");
    }

    /** Spécifique à TontineTourDeRole : qui est le bénéficiaire actuel et sa position dans l'ordre. */
    public String consulterBeneficiaireActuel(TontineTourDeRole tontine) throws SQLException {
        List<String> ordre = tontine.getOrdreTour();
        if (ordre.isEmpty()) return "Aucun ordre de tour défini.";
        int index = tontine.getTourActuelIndex();
        if (index < 0 || index >= ordre.size()) return "Tour non encore démarré.";
        return nomUtilisateur(ordre.get(index)) + " (tour " + (index + 1) + "/" + ordre.size() + ")";
    }

    /**
     * Spécifique à TontineTourDeRole : ordre complet du tour, visible par
     * tous les membres du groupe (pas seulement le bénéficiaire actuel) —
     * qui a déjà reçu, qui reçoit maintenant, qui attend encore.
     */
    public List<String> consulterOrdreTourComplet(TontineTourDeRole tontine) throws SQLException {
        List<String> ordre = tontine.getOrdreTour();
        List<String> lignes = new java.util.ArrayList<>();
        if (ordre.isEmpty()) {
            lignes.add("Aucun ordre de tour défini pour l'instant.");
            return lignes;
        }
        int indexActuel = tontine.getTourActuelIndex();
        // Qui a réellement reçu sa distribution dans cette série, et qui est désactivé : la position seule ne
        // suffit plus (« Avancer le tour » saute les membres désactivés et ceux déjà servis).
        java.util.Set<String> servis = transactionDAO.membresAyantRecuVersement(tontine.getId(),
                serieService.serieCourante(tontine.getId()));
        java.util.Set<String> membresActifs = adhesionDAO.listerParTontine(tontine.getId()).stream()
                .filter(a -> a.getStatut() == StatutAdhesion.ACTIF)
                .map(Adhesion::getMembreId)
                .collect(java.util.stream.Collectors.toSet());
        for (int i = 0; i < ordre.size(); i++) {
            String membreId = ordre.get(i);
            lignes.add(DistributionRegles.libelleOrdreDuTour(i + 1, nomUtilisateur(membreId),
                    servis.contains(membreId), i == indexActuel, membresActifs.contains(membreId)));
        }
        return lignes;
    }

    /** Spécifique à TontineTirageAuSort : historique des tirages effectués. */
    public List<Tirage> consulterTirages(String tontineId) throws SQLException {
        return diversDAO.listerParTontine(tontineId);
    }

    public List<Tontine> mesTontines(String membreId) throws SQLException {
        return tontineDAO.listerParMembre(membreId);
    }

    /**
     * Statut de chaque tontine (en préparation / en cours / clôturée), pour l'accueil.
     * Calculé d'après les cycles et les membres actifs : voir StatutTontineRegles.
     * Retourne une table id de tontine → statut, chargée en une fois pour que
     * l'écran n'interroge pas la base à chaque affichage de cellule.
     */
    public java.util.Map<String, StatutTontine> statutsDesTontines(List<Tontine> tontines) throws SQLException {
        return statutTontineService.statutsDe(tontines);
    }

    /** Tontines qu'un membre peut rejoindre — toutes sauf celles où il est déjà adhérent actif. */
    public List<Tontine> tontinesDisponiblesPourRejoindre(String membreId) throws SQLException {
        List<Tontine> dejaMembre = tontineDAO.listerParMembre(membreId);
        return tontineDAO.listerToutes().stream()
                .filter(t -> dejaMembre.stream().noneMatch(m -> m.getId().equals(t.getId())))
                .toList();
    }

    /** Tontines que ce membre n'a pas encore rejointes, pour le choix dans "Rejoindre une tontine". */
    public List<Tontine> tontinesDisponibles(String membreId) throws SQLException {
        List<String> idsDejaMembre = tontineDAO.listerParMembre(membreId).stream()
                .map(Tontine::getId)
                .toList();
        return tontineDAO.listerToutes().stream()
                .filter(t -> !idsDejaMembre.contains(t.getId()))
                .toList();
    }

    public List<Pret> mesPrets(String membreId) throws SQLException {
        return pretDAO.listerParMembre(membreId);
    }

    public List<Notification> mesNotifications(String utilisateurId) throws SQLException {
        return diversDAO.listerParUtilisateur(utilisateurId);
    }

    /**
     * Vérifie qu'une demande de création peut être soumise : le demandeur est un gérant ou n'est
     * membre d'aucune tontine, et il n'a qu'une seule demande en attente. Appelée par l'écran AVANT d'ouvrir le formulaire (inutile de le faire remplir
     * pour refuser derrière), et par soumettreDemandeCreation() par sécurité.
     *
     * @throws IllegalStateException avec le motif du refus
     */
    public void verifierPeutDemanderCreation(String demandeurId) throws SQLException {
        String refusRole = DemandeCreationRegles.refusRole(
                roleService.estMembreActif(demandeurId), utilisateurDAO.estGerant(demandeurId),
                utilisateurDAO.estAdministrateur(demandeurId));
        if (refusRole != null) {
            throw new IllegalStateException(refusRole);
        }
        String refus = DemandeCreationRegles.refusSoumission(demandeCreationDAO.compterEnAttente(demandeurId));
        if (refus != null) {
            throw new IllegalStateException(refus);
        }
    }

    /** +soumettreDemandeCreation(nom, type, montant, freq, nbMembres): DemandeCreation */
    public DemandeCreation soumettreDemandeCreation(String demandeurId, String nomSouhaite,
                                                     TypeTontine type, BigDecimal montant,
                                                     Frequence frequence, int nbMembres) throws SQLException {
        return soumettreDemandeCreation(demandeurId, nomSouhaite, type, montant, null, null, frequence, nbMembres);
    }

    /**
     * Variante avec fourchette : pour un Fonds commun, {@code montantMin} et {@code montantMax} sont obligatoires
     * (le montant « de référence » enregistré est alors le minimum) ; ils sont ignorés pour les autres types.
     */
    public DemandeCreation soumettreDemandeCreation(String demandeurId, String nomSouhaite,
                                                     TypeTontine type, BigDecimal montant,
                                                     BigDecimal montantMin, BigDecimal montantMax,
                                                     Frequence frequence, int nbMembres) throws SQLException {
        verifierPeutDemanderCreation(demandeurId); // par sécurité : l'écran l'a déjà vérifié avant le formulaire
        if (type == TypeTontine.FONDS_COMMUN) {
            String refus = MontantCotisationRegles.refusFourchette(montantMin, montantMax);
            if (refus != null) {
                throw new IllegalArgumentException(refus);
            }
            montant = montantMin;
        } else {
            montantMin = null;
            montantMax = null;
        }
        DemandeCreation d = new DemandeCreation(Utils.nouvelId(), demandeurId, nomSouhaite,
                type, montant, frequence, nbMembres);
        d.setMontantMin(montantMin);
        d.setMontantMax(montantMax);
        demandeCreationDAO.creer(d);
        return d;
    }

    /**
     * +soumettreDemandeAdhesion(tontine, motDePasse, contactRef, cguAcceptees): DemandeAdhesion
     * Méthode d'un Utilisateur déjà existant (connecté) : nom/tel/email sont
     * lus sur son compte, motDePasse confirme son identité avant l'envoi.
     */
    public DemandeAdhesion soumettreDemandeAdhesion(String utilisateurId, String tontineId,
                                                     String motDePasse, String contactRef,
                                                     boolean cguAcceptees) throws SQLException {
        if (!cguAcceptees) {
            throw new IllegalArgumentException("Vous devez accepter les termes et conditions pour continuer.");
        }
        // Séparation des rôles : un administrateur ne devient pas membre d'une tontine.
        String refusAdmin = AdministrationRegles.refusAdhesionAdministrateur(utilisateurDAO.estAdministrateur(utilisateurId));
        if (refusAdmin != null) {
            throw new IllegalStateException(refusAdmin);
        }
        Tontine tontineVisee = tontineDAO.trouverParId(tontineId)
                .orElseThrow(() -> new IllegalStateException("Tontine introuvable."));
        // Une tontine terminée ne se rejoint pas : ses membres ne sont plus membres (leur adhésion prend fin
        // à la validation de la série suivante). Le dire clairement, plutôt qu'un trompeur « déjà membre ».
        String refusTerminee = CapaciteRegles.refusTontineTerminee(statutTontineService.statutDe(tontineVisee));
        if (refusTerminee != null) {
            throw new IllegalStateException(refusTerminee);
        }
        Optional<Adhesion> adhesionExistante = adhesionDAO.trouver(tontineId, utilisateurId);
        String refusDoublon = CapaciteRegles.refusAdhesionExistante(adhesionExistante.isPresent(),
                adhesionExistante.map(a -> a.getStatut() == StatutAdhesion.ACTIF).orElse(false));
        if (refusDoublon != null) {
            throw new IllegalStateException(refusDoublon);
        }
        String refus = CapaciteRegles.refusAdhesion(
                adhesionDAO.compterMembresActifs(tontineId), tontineVisee.getNbMembresMax());
        if (refus != null) {
            throw new IllegalStateException(refus);
        }
        Utilisateur u = utilisateurDAO.trouverParId(utilisateurId)
                .orElseThrow(() -> new IllegalStateException("Compte introuvable."));
        if (!Utils.verifierMotDePasse(motDePasse, u.getMotDePasseHash())) {
            throw new IllegalArgumentException("Mot de passe incorrect.");
        }

        DemandeAdhesion d = new DemandeAdhesion(Utils.nouvelId(), tontineId, u.getNom(), u.getTel(), u.getEmail());
        d.setCguAcceptees(true);
        d.setContactRef(contactRef);
        demandeAdhesionDAO.creer(d);
        return d;
    }
}
