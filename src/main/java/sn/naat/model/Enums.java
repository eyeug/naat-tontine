package sn.naat.model;

/**
 * Toutes les énumérations du modèle métier NAAT, regroupées ici
 * pour éviter d'avoir un fichier par valeur — elles sont toutes
 * de simples listes fermées de statuts/types, comme dans le
 * diagramme de classes.
 */
public final class Enums {

    private Enums() { }

    public enum StatutCompte { ACTIF, SUSPENDU }

    public enum Frequence { HEBDOMADAIRE, MENSUELLE }

    public enum StatutCycle { EN_COURS, CLOTURE }

    public enum RoleAdhesion { MEMBRE, GERANT }

    public enum StatutAdhesion { ACTIF, INACTIF }

    public enum TypeTransaction { COTISATION, VERSEMENT, REMBOURSEMENT_PRET, PENALITE }

    public enum StatutTransaction { EN_ATTENTE, VALIDEE, ECHOUEE }

    public enum StatutDemande { EN_ATTENTE, VALIDEE, REFUSEE }

    public enum StatutPret { EN_ATTENTE, VALIDE, REFUSE, REMBOURSE }

    public enum Valideur { GERANT, ADMINISTRATEUR }

    public enum StatutPaiement { INITIE, CONFIRME, ECHOUE }

    public enum TypeTontine { FONDS_COMMUN, TOUR_DE_ROLE, TIRAGE_AU_SORT }

    /**
     * Les « espaces » de l'application : un écran par rôle. Un compte peut en avoir plusieurs
     * (un gérant est aussi membre ; un administrateur peut l'être aussi). Du plus élevé au moins élevé.
     */
    public enum Espace {
        ADMINISTRATEUR("Espace administrateur"),
        GERANT("Espace gérant"),
        MEMBRE("Espace membre");

        private final String libelle;

        Espace(String libelle) { this.libelle = libelle; }

        public String getLibelle() { return libelle; }

        @Override
        public String toString() { return libelle; }
    }

    /** Statut d'une tontine, affiché sur l'accueil. Calculé d'après ses cycles (voir StatutTontineRegles). */
    public enum StatutTontine {
        EN_PREPARATION("En préparation"),
        EN_COURS("En cours"),
        CLOTUREE("Clôturée");

        private final String libelle;

        StatutTontine(String libelle) { this.libelle = libelle; }

        public String getLibelle() { return libelle; }
    }
}
