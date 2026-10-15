package sn.naat.service;

/**
 * Règles de validation du profil utilisateur. Chaque méthode retourne null si la valeur est
 * acceptable, sinon le message expliquant le refus.
 *
 * Fonctions pures (aucun accès à la base) : les données sont passées en paramètres.
 */
public final class ProfilRegles {

    public static final int LONGUEUR_MIN_MOT_DE_PASSE = 6;

    private ProfilRegles() { }

    /** Un numéro de téléphone sénégalais saisi sans indicatif : 9 chiffres (ex. 771234567). */
    public static final int LONGUEUR_TELEPHONE = 9;

    /** 30 caractères au plus avant « @ » (limite de Gmail) + « @gmail.com ». */
    public static final int LONGUEUR_MAX_EMAIL = 40;

    private static final String DOMAINE_EMAIL = "gmail.com";

    /**
     * Le téléphone sert d'identifiant de connexion : obligatoire, exactement {@link #LONGUEUR_TELEPHONE} chiffres,
     * sans espace, tiret ni « + ». Le nombre de caractères est limité aussi à la saisie (voir saisieTelephoneAutorisee).
     */
    public static String refusTelephone(String tel) {
        if (tel == null || tel.isBlank()) {
            return "Le téléphone est obligatoire.";
        }
        String t = tel.trim();
        if (!t.matches("[0-9]+")) {
            return "Le téléphone ne doit contenir que des chiffres (sans espace, tiret ni +).";
        }
        if (t.length() != LONGUEUR_TELEPHONE) {
            return "Le téléphone doit contenir exactement " + LONGUEUR_TELEPHONE + " chiffres (ex. 771234567) : "
                    + "vous en avez saisi " + t.length() + ".";
        }
        return null;
    }

    /**
     * L'email est facultatif ; s'il est renseigné, il doit être une adresse Gmail : un seul « @ », le domaine
     * gmail.com, et avant « @ » entre 6 et 30 caractères (lettres, chiffres et points, sans point au début,
     * à la fin ni doublé), comme pour une vraie adresse Gmail.
     */
    public static String refusEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String e = email.trim();
        if (e.length() > LONGUEUR_MAX_EMAIL) {
            return "L'adresse email ne doit pas dépasser " + LONGUEUR_MAX_EMAIL + " caractères.";
        }
        int arobase = e.indexOf('@');
        if (arobase < 0) {
            return "L'adresse email doit contenir « @ » (ex. prenom.nom@gmail.com).";
        }
        if (arobase != e.lastIndexOf('@')) {
            return "L'adresse email ne doit contenir qu'un seul « @ ».";
        }
        String locale = e.substring(0, arobase);
        String domaine = e.substring(arobase + 1);
        if (!domaine.equalsIgnoreCase(DOMAINE_EMAIL)) {
            return "Seules les adresses Gmail sont acceptées (…@" + DOMAINE_EMAIL + ").";
        }
        if (locale.length() < 6 || locale.length() > 30) {
            return "Avant « @ », l'adresse doit contenir entre 6 et 30 caractères.";
        }
        if (!locale.matches("[A-Za-z0-9.]+") || locale.startsWith(".") || locale.endsWith(".") || locale.contains("..")) {
            return "Avant « @ » : lettres, chiffres et points uniquement, sans point au début, à la fin ni doublé.";
        }
        return null;
    }

    /** Adresse enregistrée : sans espaces autour et en minuscules (Gmail ne distingue pas les majuscules). */
    public static String normaliserEmail(String email) {
        return (email == null || email.isBlank()) ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Ce texte peut-il être tapé dans un champ téléphone ? Chiffres seulement, 9 au plus : la saisie est limitée,
     * on ne peut pas dépasser le nombre de caractères autorisé. (Une saisie incomplète reste permise : on tape
     * chiffre par chiffre ; c'est refusTelephone qui exige les 9 chiffres à la validation.)
     */
    public static boolean saisieTelephoneAutorisee(String texte) {
        return texte != null && texte.matches("[0-9]{0," + LONGUEUR_TELEPHONE + "}");
    }

    /** Ce texte peut-il être tapé dans un champ email ? Caractères d'une adresse Gmail, 40 au plus. */
    public static boolean saisieEmailAutorisee(String texte) {
        return texte != null && texte.length() <= LONGUEUR_MAX_EMAIL && texte.matches("[A-Za-z0-9.@]*");
    }

    /**
     * @param ancien mot de passe actuel, ou null s'il n'y en a pas à comparer (ex. mot de passe provisoire)
     */
    public static String refusNouveauMotDePasse(String nouveau, String ancien) {
        if (nouveau == null || nouveau.length() < LONGUEUR_MIN_MOT_DE_PASSE) {
            return "Le nouveau mot de passe doit contenir au moins " + LONGUEUR_MIN_MOT_DE_PASSE + " caractères.";
        }
        if (nouveau.isBlank()) {
            return "Le nouveau mot de passe ne peut pas être composé uniquement d'espaces.";
        }
        if (ancien != null && nouveau.equals(ancien)) {
            return "Le nouveau mot de passe doit être différent de l'actuel.";
        }
        return null;
    }
}
