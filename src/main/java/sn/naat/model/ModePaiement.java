package sn.naat.model;

import sn.naat.model.Enums.StatutPaiement;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Classe abstraite de base de la hiérarchie des moyens de paiement.
 * ModePaiement &lt;|-- Wave, OrangeMoney, Especes
 */
public abstract class ModePaiement {

    private String id;
    protected String reference;
    protected StatutPaiement statut = StatutPaiement.INITIE;

    protected ModePaiement(String id) {
        this.id = id;
    }

    /**
     * +initierPaiement(montant): String — point d'entrée public,
     * commun à tous les moyens de paiement. Délègue à l'appel
     * spécifique du sous-type (polymorphisme).
     */
    public final String initierPaiement(BigDecimal montant) {
        this.reference = executerPaiement(montant);
        this.statut = StatutPaiement.CONFIRME;
        return this.reference;
    }

    /** Chaque sous-classe fournit sa propre façon de traiter le paiement. */
    protected abstract String executerPaiement(BigDecimal montant);

    /** #verifierStatut(): String — accessible seulement aux sous-classes. */
    protected String verifierStatut() {
        return statut.name();
    }

    public abstract String getLibelle();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public StatutPaiement getStatut() { return statut; }
    public void setStatut(StatutPaiement statut) { this.statut = statut; }

    // ------------------------------------------------------------
    // Sous-classes
    // ------------------------------------------------------------

    /** ModePaiement &lt;|-- Wave */
    public static class Wave extends ModePaiement {
        private String numeroWave;

        public Wave(String numeroWave) {
            super(UUID.randomUUID().toString());
            this.numeroWave = numeroWave;
        }

        /** -appellerApiWave(montant): String — simulation de l'appel API Wave. */
        private String appellerApiWave(BigDecimal montant) {
            // Dans une vraie intégration : appel HTTPS à l'API
            // Checkout de Wave, récupération de l'identifiant de
            // transaction retourné par leur serveur.
            return "WAVE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        @Override
        protected String executerPaiement(BigDecimal montant) {
            return appellerApiWave(montant);
        }

        @Override
        public String getLibelle() { return "Wave"; }

        public String getNumeroWave() { return numeroWave; }
        public void setNumeroWave(String numeroWave) { this.numeroWave = numeroWave; }
    }

    /** ModePaiement &lt;|-- OrangeMoney */
    public static class OrangeMoney extends ModePaiement {
        private String numeroOrangeMoney;

        public OrangeMoney(String numeroOrangeMoney) {
            super(UUID.randomUUID().toString());
            this.numeroOrangeMoney = numeroOrangeMoney;
        }

        /** -appellerApiOrangeMoney(montant): String — simulation de l'appel API Orange Money. */
        private String appellerApiOrangeMoney(BigDecimal montant) {
            return "OM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        @Override
        protected String executerPaiement(BigDecimal montant) {
            return appellerApiOrangeMoney(montant);
        }

        @Override
        public String getLibelle() { return "Orange Money"; }

        public String getNumeroOrangeMoney() { return numeroOrangeMoney; }
        public void setNumeroOrangeMoney(String numeroOrangeMoney) { this.numeroOrangeMoney = numeroOrangeMoney; }
    }

    /** ModePaiement &lt;|-- Especes (saisie manuelle par le gérant) */
    public static class Especes extends ModePaiement {

        public Especes() {
            super(UUID.randomUUID().toString());
        }

        /** -confirmerReceptionImmediate(): void */
        private void confirmerReceptionImmediate() {
            this.statut = StatutPaiement.CONFIRME;
        }

        @Override
        protected String executerPaiement(BigDecimal montant) {
            confirmerReceptionImmediate();
            return "ESP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        @Override
        public String getLibelle() { return "Espèces"; }
    }
}
