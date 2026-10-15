package sn.naat.service;

import sn.naat.dao.*;
import sn.naat.model.*;
import sn.naat.model.Enums.*;
import sn.naat.util.Utils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Opérations portées par la classe Gerant du diagramme.
 * Chaque opération liée à un sous-type de Tontine (avancerTour,
 * lancerTirage) prend ce sous-type en paramètre, exactement comme
 * dans le diagramme de classes.
 */
public class GerantService {

    private final DemandeAdhesionDAO demandeAdhesionDAO = new DemandeAdhesionDAO();
    private final AdhesionDAO adhesionDAO = new AdhesionDAO();
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final ModePaiementDAO modePaiementDAO = new ModePaiementDAO();
    private final PretDAO pretDAO = new PretDAO();
    private final TontineDAO tontineDAO = new TontineDAO();
    private final CycleDAO cycleDAO = new CycleDAO();
    private final DiversDAO diversDAO = new DiversDAO();
    private final StatutTontineService statutTontineService = new StatutTontineService();
    private final SerieService serieService = new SerieService();
    private final DemandeTourAnticipeDAO demandeTourAnticipeDAO = new DemandeTourAnticipeDAO();
    private final DemandeNouvelleSerieDAO demandeNouvelleSerieDAO = new DemandeNouvelleSerieDAO();

    /** +validerDemandeAdhesion(demande, statut: String): Adhesion */
    public Optional<Adhesion> validerDemandeAdhesion(DemandeAdhesion demande, String statut) throws SQLException {
        boolean acceptee = "validee".equals(statut);
        if (acceptee) {
            // Le nombre de membres demandé à la création est un plafond : une fois atteint, plus
            // d'adhésion. Vérifié AVANT de changer le statut de la demande (le gérant peut encore la refuser).
            Tontine tontineDemandee = tontineDAO.trouverParId(demande.getTontineId())
                    .orElseThrow(() -> new IllegalStateException("Tontine introuvable."));
            String refus = CapaciteRegles.refusAdhesion(
                    adhesionDAO.compterMembresActifs(tontineDemandee.getId()), tontineDemandee.getNbMembresMax());
            if (refus != null) {
                throw new IllegalStateException(refus);
            }
            // Séparation des rôles : valider ferait un membre du demandeur, ce qu'un administrateur ne peut pas être.
            Optional<Utilisateur> demandeur = utilisateurDAO.trouverParTelephone(demande.getTel());
            boolean demandeurEstAdministrateur = demandeur.isPresent()
                    && utilisateurDAO.estAdministrateur(demandeur.get().getId());
            String refusAdmin = AdministrationRegles.refusAdhesionAdministrateur(demandeurEstAdministrateur);
            if (refusAdmin != null) {
                throw new IllegalStateException(refusAdmin);
            }
        }
        demandeAdhesionDAO.changerStatut(demande.getId(), acceptee ? StatutDemande.VALIDEE : StatutDemande.REFUSEE);
        if (!acceptee) return Optional.empty();

        // Le demandeur devient Utilisateur (s'il n'existe pas déjà) puis Membre
        Optional<Utilisateur> existant = utilisateurDAO.trouverParTelephone(demande.getTel());
        Utilisateur u = existant.orElseGet(() -> {
            Utilisateur nouveau = new Utilisateur(Utils.nouvelId(), demande.getNom(), demande.getTel(), demande.getEmail());
            nouveau.setMotDePasseHash(Utils.hasher("naat1234")); // repli défensif : normalement le demandeur a déjà un compte
            try {
                utilisateurDAO.creerUtilisateur(nouveau);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return nouveau;
        });
        utilisateurDAO.promouvoirMembre(u.getId());

        Adhesion adhesion = new Adhesion(Utils.nouvelId(), demande.getTontineId(), u.getId(), RoleAdhesion.MEMBRE);
        adhesionDAO.creer(adhesion);

        // Si la tontine est de type Tour de rôle, le nouveau membre rejoint
        // l'ordre du tour (sinon Gerant.avancerTour() n'aurait jamais personne
        // à désigner).
        tontineDAO.trouverParId(demande.getTontineId()).ifPresent(t -> {
            if (t instanceof TontineTourDeRole tourDeRole) {
                tourDeRole.getOrdreTour().add(u.getId());
                try {
                    tontineDAO.mettreAJourTourDeRole(tourDeRole.getId(), tourDeRole.getOrdreTour(), tourDeRole.getTourActuelIndex());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        diversDAO.creerNotification(new Notification(Utils.nouvelId(), u.getId(),
                "Votre demande d'adhésion a été validée. Bienvenue dans la tontine !", "adhesion"));

        return Optional.of(adhesion);
    }

    /**
     * +reactiverMembre(adhesion): void — annule la désactivation d'un membre. Refusé si la tontine est terminée
     * ou déjà complète. Le membre retrouve sa place dans la rotation du tour de rôle, et passera à son tour.
     */
    public void reactiverMembre(Adhesion adhesion) throws SQLException {
        Tontine tontine = tontineDAO.trouverParId(adhesion.getTontineId())
                .orElseThrow(() -> new IllegalStateException("Tontine introuvable."));
        // On relit l'adhésion en base : celle de l'écran a pu changer entre-temps.
        Adhesion courante = adhesionDAO.trouver(adhesion.getTontineId(), adhesion.getMembreId())
                .orElseThrow(() -> new IllegalStateException("Ce membre n'a plus d'adhésion sur cette tontine."));
        if (courante.getStatut() == StatutAdhesion.ACTIF) {
            throw new IllegalStateException("Ce membre est déjà actif.");
        }
        String refus = CapaciteRegles.refusReactivation(statutTontineService.statutDe(tontine),
                adhesionDAO.compterMembresActifs(tontine.getId()), tontine.getNbMembresMax());
        if (refus != null) {
            throw new IllegalStateException(refus);
        }
        if (adhesionDAO.reactiver(courante.getId()) == 0) {
            throw new IllegalStateException("Ce membre n'est plus désactivé.");
        }
        diversDAO.creerNotification(new Notification(Utils.nouvelId(), courante.getMembreId(),
                "Votre adhésion à « " + tontine.getNom() + " » a été réactivée par le gérant.", "adhesion"));
    }

    /** +desactiverMembre(adhesion): void */
    public void desactiverMembre(String adhesionId) throws SQLException {
        adhesionDAO.desactiver(adhesionId);
    }

    /** +saisirCotisationEspeces(adhesion, montant): Transaction */
    public Transaction saisirCotisationEspeces(Adhesion adhesion, BigDecimal montant) throws SQLException {
        // Mêmes règles que pour la cotisation en ligne : montant fixe pour
        // Tour de rôle / Tirage au sort, cycle actif, une cotisation par cycle.
        Tontine tontine = tontineDAO.trouverParId(adhesion.getTontineId())
                .orElseThrow(() -> new IllegalStateException("Tontine introuvable."));
        // Le gérant a pu désactiver ce membre depuis l'affichage de la liste : on relit l'adhésion en base.
        Adhesion courante = adhesionDAO.trouver(adhesion.getTontineId(), adhesion.getMembreId())
                .orElseThrow(() -> new IllegalStateException("Ce membre n'a plus d'adhésion sur cette tontine."));
        String refusInactive = CotisationRegles.refusAdhesionInactive(courante.getStatut() == StatutAdhesion.ACTIF, false);
        if (refusInactive != null) {
            throw new IllegalStateException(refusInactive);
        }
        CotisationRegles.verifier(tontine, adhesion.getMembreId(), montant);

        ModePaiement especes = new ModePaiement.Especes();
        especes.initierPaiement(montant);
        modePaiementDAO.enregistrer(especes);

        Transaction t = new Transaction(Utils.nouvelId(), adhesion.getTontineId(), adhesion.getMembreId(),
                TypeTransaction.COTISATION, montant, especes.getId());
        transactionDAO.creer(t);
        adhesionDAO.enregistrerCotisation(adhesion.getId(), montant);
        return t;
    }

    /** +validerPret(pret, tontine: TontineFondsCommun, statut: String): void — anti-conflit d'intérêt. */
    public void validerPret(Pret pret, TontineFondsCommun tontine, String gerantId, String statut) throws SQLException {
        if (pret.getMembreId().equals(gerantId)) {
            throw new IllegalStateException("Un gérant ne peut pas valider son propre prêt : "
                    + "cette demande doit être transmise à l'administrateur.");
        }
        boolean approuve = "valide".equals(statut);
        pretDAO.changerStatut(pret.getId(), approuve ? StatutPret.VALIDE : StatutPret.REFUSE, Valideur.GERANT);
        diversDAO.creerNotification(new Notification(Utils.nouvelId(), pret.getMembreId(),
                "Votre demande de prêt a été " + (approuve ? "validée" : "refusée") + ".", "pret"));
    }

    /** +definirParametres(tontine, montant, penalite, nbMax): void */
    public void definirParametres(String tontineId, BigDecimal montant, BigDecimal penalite, int nbMax) throws SQLException {
        tontineDAO.definirParametres(tontineId, montant, penalite, nbMax);
    }

    /**
     * Vérifie que le cycle peut être clôturé. Pour une tontine Tirage au sort,
     * le cycle n°N exige que le tirage n°N ait été effectué : sans tirage il
     * n'y a pas de bénéficiaire à qui verser la cagnotte.
     * Appelée par le contrôleur avant la confirmation, et par cloturerCycle()
     * par sécurité.
     *
     * @throws IllegalStateException si un prérequis manque
     */
    public void verifierPrerequisCloture(Cycle cycle) throws SQLException {
        Tontine tontine = tontineDAO.trouverParId(cycle.getTontineId()).orElse(null);
        if (tontine instanceof TontineTirageAuSort) {
            int nbTirages = diversDAO.listerParTontine(cycle.getTontineId()).size();
            if (nbTirages < cycle.getNumero()) {
                throw new IllegalStateException("Lancez d'abord le tirage au sort pour le cycle n°"
                        + cycle.getNumero() + " avant de le clôturer : sans tirage, il n'y a pas de bénéficiaire.");
            }
        }

        // Un membre ne peut être payé qu'UNE SEULE FOIS par série. Vérifié ici, avant toute
        // action (pénalités, clôture, versement), pour ne rien laisser à moitié fait.
        String beneficiaire = beneficiaireDuCycle(tontine, cycle);
        if (DistributionRegles.dejaServi(beneficiaire, transactionDAO.membresAyantRecuVersement(cycle.getTontineId(), cycle.getSerie()))) {
            String conseil = tontine instanceof TontineTourDeRole
                    ? " Avancez le tour pour désigner le prochain bénéficiaire, puis clôturez le cycle."
                    : "";
            throw new IllegalStateException(nomDuMembre(beneficiaire) + " a déjà reçu sa distribution : "
                    + "un membre ne peut être payé qu'une seule fois par série." + conseil);
        }

        // Un membre désactivé par le gérant ne reçoit pas de distribution (il a pu être désactivé
        // après avoir été désigné). Sans adhésion trouvée, on ne bloque pas : comportement inchangé.
        if (beneficiaire != null) {
            boolean actif = adhesionDAO.trouver(cycle.getTontineId(), beneficiaire)
                    .map(a -> a.getStatut() == StatutAdhesion.ACTIF)
                    .orElse(true);
            String refusDesactive = DistributionRegles.refusBeneficiaireDesactive(
                    actif, tontine instanceof TontineTourDeRole, nomDuMembre(beneficiaire));
            if (refusDesactive != null) {
                throw new IllegalStateException(refusDesactive);
            }
        }
    }

    /**
     * Bénéficiaire unique du cycle : le membre désigné par le tour (Tour de rôle) ou
     * le gagnant du tirage n°N pour le cycle n°N (Tirage au sort). Retourne null pour
     * un Fonds commun (plusieurs bénéficiaires) ou si personne n'est encore désigné.
     */
    private String beneficiaireDuCycle(Tontine tontine, Cycle cycle) throws SQLException {
        if (tontine instanceof TontineTourDeRole tourDeRole) {
            List<String> ordre = tourDeRole.getOrdreTour();
            int index = tourDeRole.getTourActuelIndex();
            return (!ordre.isEmpty() && index >= 0 && index < ordre.size()) ? ordre.get(index) : null;
        }
        if (tontine instanceof TontineTirageAuSort) {
            // Les tirages sont triés par semaine croissante : le cycle n°N correspond au N-ième tirage.
            List<Tirage> tirages = diversDAO.listerParTontine(cycle.getTontineId());
            int rang = cycle.getNumero() - 1;
            return (rang >= 0 && rang < tirages.size()) ? tirages.get(rang).getBeneficiaireId() : null;
        }
        return null;
    }

    /** Membres actifs n'ayant pas cotisé depuis le début du cycle donné (base de l'application des pénalités). */
    public List<Adhesion> listerMembresEnDefaut(Cycle cycle) throws SQLException {
        List<Adhesion> enDefaut = new java.util.ArrayList<>();
        LocalDateTime debutCycle = cycle.getDateDebut().atStartOfDay();
        for (Adhesion adhesion : adhesionDAO.listerParTontine(cycle.getTontineId())) {
            if (adhesion.getStatut() != StatutAdhesion.ACTIF) continue;
            if (!transactionDAO.aDejaCotiseDepuis(cycle.getTontineId(), adhesion.getMembreId(), debutCycle)) {
                enDefaut.add(adhesion);
            }
        }
        return enDefaut;
    }

    /** +cloturerCycle(cycle): Distribution — le montant est calculé à partir des cotisations reçues depuis le début du cycle. */
    public Distribution cloturerCycle(Cycle cycle, BigDecimal montantTotal) throws SQLException {
        // Application des pénalités de retard : tout membre actif qui n'a pas
        // cotisé pendant ce cycle se voit déduire Tontine.penaliteRetard de
        // son solde, conformément à la règle "une pénalité s'applique
        // automatiquement après la date d'échéance".
        Tontine tontine = tontineDAO.trouverParId(cycle.getTontineId()).orElse(null);

        // Vérification AVANT toute action (pénalités, clôture, versement),
        // sinon le cycle serait clôturé sans bénéficiaire.
        verifierPrerequisCloture(cycle);

        if (tontine != null && tontine.getPenaliteRetard().signum() > 0) {
            LocalDateTime debutCycle = cycle.getDateDebut().atStartOfDay();
            for (Adhesion adhesion : adhesionDAO.listerParTontine(cycle.getTontineId())) {
                if (adhesion.getStatut() != StatutAdhesion.ACTIF) continue;
                boolean aCotise = transactionDAO.aDejaCotiseDepuis(cycle.getTontineId(), adhesion.getMembreId(), debutCycle);
                if (!aCotise) {
                    // Le retard est tracé dans l'historique du membre (c'est ce que le
                    // gérant voit en cliquant sur lui). On l'écrit AVANT de déduire le
                    // solde : si l'écriture échoue, rien n'a encore été déduit.
                    Transaction retard = new Transaction(Utils.nouvelId(), cycle.getTontineId(),
                            adhesion.getMembreId(), TypeTransaction.PENALITE, tontine.getPenaliteRetard(), null);
                    retard.setLibelle("Retard — cycle n°" + cycle.getNumero()
                            + " : aucune cotisation reçue avant la clôture");
                    transactionDAO.creer(retard);

                    adhesionDAO.appliquerPenalite(adhesion.getId(), tontine.getPenaliteRetard());
                    diversDAO.creerNotification(new Notification(Utils.nouvelId(), adhesion.getMembreId(),
                            "Vous n'avez pas cotisé pour le cycle n°" + cycle.getNumero() + " : une pénalité de "
                                    + tontine.getPenaliteRetard() + " FCFA a été déduite de votre solde.", "penalite"));
                }
            }
        }

        cycleDAO.cloturer(cycle.getId());
        Distribution d = new Distribution(Utils.nouvelId(), cycle.getId(), montantTotal);
        LocalDateTime debutCycle = cycle.getDateDebut().atStartOfDay();

        // Détermine le(s) bénéficiaire(s) selon le type de tontine, et
        // rattache la distribution pour que tous les membres puissent voir
        // qui a reçu quoi (pas seulement l'état "actuel").
        // Même fonction que pour la vérification faite en début de méthode : ce qui est
        // contrôlé est exactement ce qui est payé.
        d.setBeneficiaireId(beneficiaireDuCycle(tontine, cycle));
        // Pour TontineFondsCommun, beneficiaireId reste null : la distribution
        // profite à plusieurs membres à la fois (partage des intérêts), pas à un seul.

        cycleDAO.creerDistribution(d);

        // +verserBeneficiaire(beneficiaire, mode): Transaction — le versement
        // effectif, qui manquait jusqu'ici : la distribution était calculée
        // et enregistrée, mais aucun argent ne "sortait" réellement vers un
        // membre concret.
        if (tontine instanceof TontineFondsCommun) {
            // Fonds commun : répartition proportionnelle à la cotisation de
            // chacun pendant ce cycle (partage des intérêts collectés).
            List<Adhesion> actifs = adhesionDAO.listerParTontine(cycle.getTontineId()).stream()
                    .filter(a -> a.getStatut() == StatutAdhesion.ACTIF)
                    .toList();
            java.util.Map<String, BigDecimal> parMembre = new java.util.HashMap<>();
            BigDecimal totalCotiseCycle = BigDecimal.ZERO;
            for (Adhesion a : actifs) {
                BigDecimal montantMembre = transactionDAO.sommeCotisationsMembreDepuis(
                        cycle.getTontineId(), a.getMembreId(), debutCycle);
                parMembre.put(a.getMembreId(), montantMembre);
                totalCotiseCycle = totalCotiseCycle.add(montantMembre);
            }
            if (totalCotiseCycle.signum() > 0) {
                for (Adhesion a : actifs) {
                    BigDecimal part = parMembre.get(a.getMembreId());
                    if (part == null || part.signum() <= 0) continue;
                    BigDecimal montantVerse = montantTotal.multiply(part)
                            .divide(totalCotiseCycle, 2, java.math.RoundingMode.HALF_UP);
                    if (montantVerse.signum() > 0) {
                        verserMembre(cycle.getTontineId(), a.getMembreId(), montantVerse, d.getId());
                    }
                }
            }
        } else if (d.getBeneficiaireId() != null) {
            // Tour de rôle / Tirage au sort : un seul bénéficiaire reçoit tout.
            verserMembre(cycle.getTontineId(), d.getBeneficiaireId(), montantTotal, d.getId());
        }

        return d;
    }

    /**
     * Verse effectivement un montant à un membre suite à une clôture de
     * cycle : crée le ModePaiement, la Transaction de type VERSEMENT, et
     * notifie le membre. Correspond à Distribution.verserBeneficiaire().
     */
    private void verserMembre(String tontineId, String membreId, BigDecimal montant, String distributionId) throws SQLException {
        ModePaiement mode = new ModePaiement.Especes();
        mode.initierPaiement(montant);
        modePaiementDAO.enregistrer(mode);

        Transaction t = new Transaction(Utils.nouvelId(), tontineId, membreId,
                TypeTransaction.VERSEMENT, montant, mode.getId());
        t.setDistributionId(distributionId);
        transactionDAO.creer(t);

        diversDAO.creerNotification(new Notification(Utils.nouvelId(), membreId,
                "Vous avez reçu un versement de " + montant + " FCFA suite à la clôture d'un cycle.", "versement"));
    }

    /** Calcule le montant total cotisé depuis le début du cycle (base du montant à distribuer). */
    public BigDecimal calculerMontantCycle(Cycle cycle) throws SQLException {
        Tontine tontine = tontineDAO.trouverParId(cycle.getTontineId()).orElse(null);
        LocalDateTime debutCycle = cycle.getDateDebut().atStartOfDay();
        // Le cycle n'est pas encore clôturé au moment de ce calcul (c'est un
        // aperçu avant clôture) : la borne de fin est donc "maintenant", pas
        // cycle.getDateFin() qui est encore null à ce stade.
        LocalDateTime finCycle = LocalDateTime.now();

        if (tontine instanceof TontineFondsCommun) {
            // Fonds commun : le capital cotisé reste dans la caisse pour les
            // prêts futurs — seuls les INTÉRÊTS perçus sur les prêts
            // remboursés pendant ce cycle sont partagés entre les membres.
            return pretDAO.sommeInteretsRembourses(cycle.getTontineId(), debutCycle, finCycle);
        }
        // Tour de rôle / Tirage au sort : toute la cagnotte cotisée part au
        // seul bénéficiaire du cycle.
        return transactionDAO.sommeCotisationsDepuis(cycle.getTontineId(), debutCycle, finCycle);
    }

    /** Date de fin prévue d'une série complète (Tour de rôle / Tirage au sort) ; null pour Fonds commun. */
    public LocalDate calculerDateFinPrevue(Tontine tontine) {
        LocalDate depart = tontine.getDateCreation() != null ? tontine.getDateCreation().toLocalDate() : LocalDate.now();
        return sn.naat.util.DureeTontine.dateFinPrevue(
                depart, TypeTontine.valueOf(tontine.getTypeCode()), tontine.getNbMembresMax(), tontine.getFrequence());
    }

    public List<Cycle> cyclesDeLaTontine(String tontineId) throws SQLException {
        return cycleDAO.listerParTontine(tontineId);
    }

    public List<Notification> mesNotifications(String gerantId) throws SQLException {
        return diversDAO.listerParUtilisateur(gerantId);
    }

    /** +avancerTour(tontine: TontineTourDeRole): Utilisateur — retourne l'id du nouveau bénéficiaire. */
    public String avancerTour(TontineTourDeRole tontine) throws SQLException {
        // Une fois toute la rotation servie, avancer le tour reviendrait au premier
        // membre (déjà payé) : la série est terminée, il n'y a plus de tour à avancer.
        java.util.Set<String> servis = transactionDAO.membresAyantRecuVersement(tontine.getId(),
                serieService.serieCourante(tontine.getId()));
        java.util.Set<String> membresActifs = adhesionDAO.listerParTontine(tontine.getId()).stream()
                .filter(a -> a.getStatut() == StatutAdhesion.ACTIF)
                .map(Adhesion::getMembreId)
                .collect(java.util.stream.Collectors.toSet());
        // Sont écartés du tour : les membres déjà servis ET les membres désactivés (ils ne sont jamais désignés).
        java.util.Set<String> ecartes = DistributionRegles.servisOuDesactives(tontine.getOrdreTour(), servis, membresActifs);
        if (DistributionRegles.rotationTerminee(tontine.getOrdreTour(), ecartes)) {
            throw new IllegalStateException("Tous les membres actifs de la rotation ont déjà reçu leur distribution : "
                    + "la série est terminée, il n'y a plus de tour à avancer.");
        }
        // On saute les membres déjà payés et les membres désactivés : le tour ne s'arrête que sur un membre
        // actif qui n'a pas encore reçu sa distribution.
        int prochain = DistributionRegles.prochainNonServi(tontine.getOrdreTour(), tontine.getTourActuelIndex(), ecartes);
        if (prochain < 0) return null; // aucun ordre de tour défini
        tontine.setTourActuelIndex(prochain);
        String beneficiaireId = tontine.getOrdreTour().get(prochain);
        tontineDAO.mettreAJourTourDeRole(tontine.getId(), tontine.getOrdreTour(), prochain);
        if (beneficiaireId != null) {
            diversDAO.creerNotification(new Notification(Utils.nouvelId(), beneficiaireId,
                    "C'est votre tour de recevoir le versement sur " + tontine.getNom() + " !", "tour"));
        }
        return beneficiaireId;
    }

    /** +lancerTirage(tontine: TontineTirageAuSort): Tirage */
    public Tirage lancerTirage(TontineTirageAuSort tontine, List<String> membresEligibles, int semaine) throws SQLException {
        // Un seul tirage par cycle : il faut un cycle actif, et son tirage ne
        // doit pas déjà avoir eu lieu (le cycle n°N correspond au tirage n°N).
        Cycle cycleActif = cycleDAO.trouverCycleEnCours(tontine.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun cycle actif : démarrez un cycle avant de lancer le tirage."));
        int nbTirages = diversDAO.listerParTontine(tontine.getId()).size();
        if (nbTirages >= cycleActif.getNumero()) {
            throw new IllegalStateException("Le tirage du cycle n°" + cycleActif.getNumero()
                    + " a déjà été effectué. Clôturez ce cycle avant de lancer le suivant.");
        }

        String gagnantId = tontine.lancerTirage(membresEligibles);
        if (gagnantId == null) throw new IllegalStateException("Aucun membre éligible pour le tirage.");
        Tirage tirage = new Tirage(Utils.nouvelId(), tontine.getId(), semaine, gagnantId);
        tirage.setSerie(cycleActif.getSerie());
        diversDAO.creerTirage(tirage);
        diversDAO.creerNotification(new Notification(Utils.nouvelId(), gagnantId,
                "Félicitations, vous avez été tiré(e) au sort sur " + tontine.getNom() + " !", "tirage"));
        return tirage;
    }

    public List<DemandeAdhesion> demandesEnAttente(String tontineId) throws SQLException {
        return demandeAdhesionDAO.listerEnAttenteParTontine(tontineId);
    }

    public List<Pret> pretsEnAttente(String tontineId) throws SQLException {
        return pretDAO.listerEnAttenteParTontine(tontineId);
    }

    public List<Adhesion> membresDeLaTontine(String tontineId) throws SQLException {
        return adhesionDAO.listerParTontine(tontineId);
    }

    /** Nom d'affichage d'un membre, pour la colonne "Nom" du tableau du gérant. */
    public String nomDuMembre(String membreId) throws SQLException {
        return utilisateurDAO.trouverParId(membreId).map(Utilisateur::getNom).orElse("(compte introuvable)");
    }

    /** Historique des transactions d'un membre, limité à cette tontine. */
    public List<Transaction> consulterHistoriqueMembre(String tontineId, String membreId) throws SQLException {
        return transactionDAO.listerParTontineEtMembre(tontineId, membreId);
    }

    public List<Tontine> mesTontinesGerees(String gerantId) throws SQLException {
        return tontineDAO.listerParGerant(gerantId);
    }

    /**
     * Vérifie qu'une demande de nouvelle série est possible. Appelée par l'écran avant la
     * confirmation (inutile de demander « Confirmer ? » pour refuser derrière), et par
     * demanderNouvelleSerie() par sécurité.
     *
     * @throws IllegalStateException avec le motif du refus
     */
    public void verifierPeutDemanderNouvelleSerie(Tontine tontine, String gerantId) throws SQLException {
        if (!tontine.getGerantId().equals(gerantId)) {
            throw new IllegalStateException("Seul le gérant de cette tontine peut demander une nouvelle série.");
        }
        String refus = SerieRegles.refusDemande(statutTontineService.statutDe(tontine),
                demandeNouvelleSerieDAO.existeEnAttente(tontine.getId()));
        if (refus != null) {
            throw new IllegalStateException(refus);
        }
        // Les adhésions de la série terminée prendront fin : les prêts doivent être soldés avant.
        String refusPrets = SerieRegles.refusPretsNonSoldes(pretDAO.compterNonSoldes(tontine.getId()));
        if (refusPrets != null) {
            throw new IllegalStateException(refusPrets);
        }
    }

    public List<DemandeTourAnticipe> demandesTourAnticipeEnAttente(String tontineId) throws SQLException {
        return demandeTourAnticipeDAO.listerEnAttente(tontineId);
    }

    /**
     * +traiterDemandeTourAnticipe(demande, statut): void — accorder place le membre juste après le bénéficiaire
     * actuel dans l'ordre du tour : il sera le prochain à recevoir, quand le cycle en cours sera clôturé. Le
     * bénéficiaire actuel n'est pas déplacé. Les mêmes contrôles que pour la demande sont refaits : la situation
     * a pu changer (membre désactivé, déjà servi, tontine terminée).
     */
    public void traiterDemandeTourAnticipe(DemandeTourAnticipe demande, String statut) throws SQLException {
        DemandeTourAnticipe courante = demandeTourAnticipeDAO.trouverParId(demande.getId())
                .orElseThrow(() -> new IllegalStateException("Demande introuvable."));
        if (courante.getStatut() != StatutDemande.EN_ATTENTE) {
            throw new IllegalStateException("Cette demande a déjà été traitée.");
        }
        Tontine tontine = tontineDAO.trouverParId(courante.getTontineId())
                .orElseThrow(() -> new IllegalStateException("Tontine introuvable."));
        boolean accordee = "validee".equals(statut);

        TontineTourDeRole tourDeRole = null;
        if (accordee) {
            if (!(tontine instanceof TontineTourDeRole t)) {
                throw new IllegalStateException("Le tour anticipé n'existe que pour les tontines de type Tour de rôle.");
            }
            tourDeRole = t;
            String membreId = courante.getMembreId();
            java.util.List<String> ordre = tourDeRole.getOrdreTour();
            int position = ordre.indexOf(membreId);
            java.util.Set<String> servis = transactionDAO.membresAyantRecuVersement(tontine.getId(),
                    serieService.serieCourante(tontine.getId()));
            boolean actif = adhesionDAO.trouver(tontine.getId(), membreId)
                    .map(a -> a.getStatut() == StatutAdhesion.ACTIF).orElse(false);
            String refus = TourAnticipeRegles.refusDemande(true, statutTontineService.statutDe(tontine), actif,
                    position >= 0, servis.contains(membreId),
                    position >= 0 && position == tourDeRole.getTourActuelIndex(), false, false);
            if (refus != null) {
                throw new IllegalStateException(refus);
            }
        }

        // Le changement de statut ne touche que les demandes encore en attente : une seconde validation de la
        // même demande ne peut donc pas réordonner le tour deux fois.
        if (demandeTourAnticipeDAO.changerStatut(courante.getId(),
                accordee ? StatutDemande.VALIDEE : StatutDemande.REFUSEE) == 0) {
            throw new IllegalStateException("Cette demande a déjà été traitée.");
        }

        if (accordee) {
            TourAnticipeRegles.Rotation rotation = TourAnticipeRegles.apresTourAnticipe(
                    tourDeRole.getOrdreTour(), tourDeRole.getTourActuelIndex(), courante.getMembreId());
            tontineDAO.mettreAJourTourDeRole(tourDeRole.getId(), rotation.ordre(), rotation.indexCourant());
            diversDAO.creerNotification(new Notification(Utils.nouvelId(), courante.getMembreId(),
                    "Votre demande de tour anticipé sur « " + tontine.getNom() + " » a été accordée : vous serez "
                            + "le prochain bénéficiaire, après la clôture du cycle en cours.", "tour_anticipe"));
        } else {
            diversDAO.creerNotification(new Notification(Utils.nouvelId(), courante.getMembreId(),
                    "Votre demande de tour anticipé sur « " + tontine.getNom() + " » a été refusée.", "tour_anticipe"));
        }
    }

    /** Résumé de la série en cours : statut et progression (membres inscrits, ou membres servis). */
    public String resumeSerie(Tontine tontine) throws SQLException {
        return statutTontineService.resume(tontine);
    }

    /** Numéro de la série en cours de cette tontine. */
    public int serieCourante(String tontineId) throws SQLException {
        return serieService.serieCourante(tontineId);
    }

    /** Statut (en préparation / en cours / clôturée) de la série en cours. */
    public StatutTontine statutDe(Tontine tontine) throws SQLException {
        return statutTontineService.statutDe(tontine);
    }

    /**
     * +demanderNouvelleSerie(tontine): DemandeNouvelleSerie — quand la tontine est clôturée, le gérant
     * demande à l'administrateur de démarrer une nouvelle série (mêmes règles ; les adhésions prennent fin et chacun redemande à rejoindre).
     */
    public DemandeNouvelleSerie demanderNouvelleSerie(Tontine tontine, String gerantId) throws SQLException {
        verifierPeutDemanderNouvelleSerie(tontine, gerantId);

        DemandeNouvelleSerie demande = new DemandeNouvelleSerie(Utils.nouvelId(), tontine.getId(), gerantId);
        demandeNouvelleSerieDAO.creer(demande);

        String nomGerant = nomDuMembre(gerantId);
        for (String adminId : utilisateurDAO.listerAdministrateursIds()) {
            diversDAO.creerNotification(new Notification(Utils.nouvelId(), adminId,
                    nomGerant + " demande une nouvelle série pour la tontine « " + tontine.getNom()
                            + " » : votre validation est requise.", "nouvelle_serie"));
        }
        return demande;
    }

    public int prochainNumeroSemaine(String tontineId) throws SQLException {
        return diversDAO.listerParTontine(tontineId).size() + 1;
    }

    /**
     * Membres éligibles au prochain tirage : tous les membres actifs qui
     * n'ont pas encore gagné pendant la série en cours. Si tout le monde a
     * déjà gagné une fois, une nouvelle série démarre et tout le monde
     * redevient éligible — ce qui garantit que le nombre de cycles avant
     * qu'un même membre puisse regagner correspond au nombre de membres.
     */
    public List<String> membresEligiblesPourTirage(String tontineId) throws SQLException {
        List<String> tousLesMembres = adhesionDAO.listerParTontine(tontineId).stream()
                .filter(a -> a.getStatut() == StatutAdhesion.ACTIF)
                .map(Adhesion::getMembreId)
                .toList();

        // Seuls comptent les tirages et les versements de la série COURANTE : après une
        // nouvelle série validée par l'administrateur, tout le monde redevient éligible.
        int serie = serieService.serieCourante(tontineId);
        List<String> dejaGagnants = SerieRegles.tiragesDeLaSerie(diversDAO.listerParTontine(tontineId), serie).stream()
                .map(Tirage::getBeneficiaireId)
                .toList();

        List<String> eligibles = DistributionRegles.eligiblesAuTirage(
                tousLesMembres, dejaGagnants, transactionDAO.membresAyantRecuVersement(tontineId, serie));

        // Plus personne à tirer : la série est terminée. On ne remet SURTOUT PAS tout
        // le monde dans le tirage, sinon un même membre pourrait être payé deux fois :
        // pour rejouer, il faut une nouvelle série, validée par l'administrateur.
        if (eligibles.isEmpty()) {
            throw new IllegalStateException("Il n'y a plus de membre à tirer au sort : tous ont déjà gagné "
                    + "dans cette série. Un membre ne peut être payé qu'une seule fois par série. "
                    + "Pour continuer, demandez une nouvelle série à l'administrateur.");
        }
        return eligibles;
    }

    public Cycle demarrerNouveauCycle(String tontineId, int numero) throws SQLException {
        // Un seul cycle actif à la fois par tontine : sinon, le calcul du
        // montant d'un cycle (qui part de sa date de début, sans borne de
        // fin tant qu'il n'est pas clôturé) finirait par englober aussi les
        // cotisations du cycle suivant.
        if (cycleDAO.trouverCycleEnCours(tontineId).isPresent()) {
            throw new IllegalStateException("Un cycle est déjà en cours sur cette tontine. "
                    + "Clôturez-le avant d'en démarrer un nouveau.");
        }
        // Sur une tontine clôturée (tous les membres ont reçu leur distribution), un nouveau
        // cycle ne pourrait jamais être payé : les membres cotiseraient pour rien.
        Tontine tontine = tontineDAO.trouverParId(tontineId)
                .orElseThrow(() -> new IllegalStateException("Tontine introuvable."));
        if (statutTontineService.statutDe(tontine) == StatutTontine.CLOTUREE) {
            throw new IllegalStateException("Cette tontine est clôturée : tous ses membres ont déjà reçu leur "
                    + "distribution. Demandez une nouvelle série à l'administrateur pour continuer.");
        }
        // Le premier cycle d'une série ne démarre qu'une fois le nombre de membres demandé atteint :
        // tant que ce n'est pas le cas, la tontine reste « En préparation ».
        int serie = serieService.serieCourante(tontineId);
        boolean premierCycleDeLaSerie = SerieRegles.cyclesDeLaSerie(cycleDAO.listerParTontine(tontineId), serie).isEmpty();
        if (premierCycleDeLaSerie) {
            String refus = CapaciteRegles.refusPremierCycle(
                    adhesionDAO.compterMembresActifs(tontineId), tontine.getNbMembresMax());
            if (refus != null) {
                throw new IllegalStateException(refus);
            }
        }
        Cycle c = new Cycle(Utils.nouvelId(), tontineId, numero, LocalDate.now());
        c.setSerie(serie);
        c.demarrer();
        cycleDAO.creer(c);
        return c;
    }
}
