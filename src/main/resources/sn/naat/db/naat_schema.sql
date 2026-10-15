-- ============================================================
-- NAAT — Tontine sénégalaise modernisée
-- Schéma de base de données (MySQL 8+)
-- Dérivé du diagramme de classes : Utilisateur/Membre/Gérant/
-- Administrateur, Tontine (FondsCommun/TourDeRole/TirageAuSort),
-- Cycle, Distribution, ModePaiement (Wave/OrangeMoney/Espèces),
-- Adhesion, Transaction, DemandeAdhesion, DemandeCreation,
-- Pret, Tirage, Notification, JournalAudit
--
-- Stratégie de mapping héritage -> relationnel : "table par
-- classe" (class table inheritance). Chaque sous-classe a sa
-- propre table, dont la clé primaire est aussi clé étrangère
-- vers la table parente — ça reproduit fidèlement la
-- généralisation UML plutôt que de tout aplatir avec un
-- discriminant.
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- UTILISATEUR (classe de base)
-- ------------------------------------------------------------
CREATE TABLE utilisateur (
    id                  CHAR(36)        NOT NULL DEFAULT (UUID()),
    nom                 VARCHAR(150)    NOT NULL,
    initials            VARCHAR(5)      NOT NULL,
    tel                 VARCHAR(20)     NOT NULL,
    email               VARCHAR(150)    NULL,
    mot_de_passe_hash   VARCHAR(255)    NOT NULL,
    statut_compte       ENUM('actif','suspendu') NOT NULL DEFAULT 'actif',
    doit_changer_mdp    BOOLEAN         NOT NULL DEFAULT FALSE,   -- vrai tant que l'utilisateur a un mot de passe provisoire
    date_inscription    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_utilisateur_tel (tel)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- MEMBRE (Utilisateur <|-- Membre)
-- Une ligne existe dès qu'un utilisateur a au moins une adhésion
-- validée. Créée par l'application au moment de la première
-- validation de demande d'adhésion.
-- ------------------------------------------------------------
CREATE TABLE membre (
    utilisateur_id      CHAR(36)        NOT NULL,
    PRIMARY KEY (utilisateur_id),
    CONSTRAINT fk_membre_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- GERANT (Membre <|-- Gerant)
-- Un gérant est toujours aussi membre : la clé pointe vers
-- membre(utilisateur_id), pas directement vers utilisateur,
-- pour respecter l'héritage à deux niveaux du diagramme.
-- ------------------------------------------------------------
CREATE TABLE gerant (
    utilisateur_id      CHAR(36)        NOT NULL,
    PRIMARY KEY (utilisateur_id),
    CONSTRAINT fk_gerant_membre FOREIGN KEY (utilisateur_id) REFERENCES membre(utilisateur_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- ADMINISTRATEUR (Utilisateur <|-- Administrateur)
-- Hérite directement d'Utilisateur, pas de Membre.
-- ------------------------------------------------------------
CREATE TABLE administrateur (
    utilisateur_id      CHAR(36)        NOT NULL,
    PRIMARY KEY (utilisateur_id),
    CONSTRAINT fk_administrateur_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- TONTINE (classe de base) + spécialisation par type
-- (3 sous-types indépendants, sans hiérarchie entre eux : un
-- discriminant dans une seule table suffit, contrairement à
-- Utilisateur qui a un vrai empilement de niveaux.)
-- ------------------------------------------------------------
CREATE TABLE tontine (
    id                  CHAR(36)        NOT NULL DEFAULT (UUID()),
    nom                 VARCHAR(150)    NOT NULL,
    description         TEXT            NULL,
    type                ENUM('fonds_commun','tour_de_role','tirage_au_sort') NOT NULL,
    frequence           ENUM('hebdomadaire','mensuelle') NOT NULL,
    duree_label         VARCHAR(50)     NULL,
    montant_cotisation  DECIMAL(12,2)   NOT NULL,
    montant_min         DECIMAL(12,2)   NULL,        -- Fonds commun : borne basse de la cotisation
    montant_max         DECIMAL(12,2)   NULL,        -- Fonds commun : borne haute
    penalite_retard     DECIMAL(12,2)   NOT NULL DEFAULT 0,
    nb_membres_max      INT             NOT NULL,
    gerant_id           CHAR(36)        NOT NULL,
    date_creation       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- TontineFondsCommun (type = 'fonds_commun')
    taux_interet         DECIMAL(5,2)    NULL,
    duree_max_pret        INT             NULL,

    -- TontineTourDeRole (type = 'tour_de_role')
    ordre_tour            TEXT            NULL,       -- liste ordonnée d'ids utilisateur, séparés par des virgules
    tour_actuel_index     INT             NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_tontine_gerant FOREIGN KEY (gerant_id) REFERENCES gerant(utilisateur_id),
    CONSTRAINT chk_tontine_fonds_commun CHECK (
        (type = 'fonds_commun' AND taux_interet IS NOT NULL AND duree_max_pret IS NOT NULL)
        OR (type <> 'fonds_commun' AND taux_interet IS NULL AND duree_max_pret IS NULL)
    ),
    CONSTRAINT chk_tontine_tour_de_role CHECK (
        (type = 'tour_de_role' AND ordre_tour IS NOT NULL AND tour_actuel_index IS NOT NULL)
        OR (type <> 'tour_de_role' AND ordre_tour IS NULL AND tour_actuel_index IS NULL)
    )
) ENGINE=InnoDB;

CREATE INDEX idx_tontine_gerant ON tontine(gerant_id);
CREATE INDEX idx_tontine_type ON tontine(type);

-- ------------------------------------------------------------
-- CYCLE : Tontine "1" --> "1..*" Cycle
-- ------------------------------------------------------------
CREATE TABLE cycle (
    id                  CHAR(36)        NOT NULL DEFAULT (UUID()),
    tontine_id          CHAR(36)        NOT NULL,
    numero              INT             NOT NULL,
    serie               INT             NOT NULL DEFAULT 1,   -- série de la tontine à laquelle appartient ce cycle
    date_debut          DATE            NOT NULL,
    date_fin            DATE            NULL,
    statut              ENUM('en_cours','cloture') NOT NULL DEFAULT 'en_cours',
    PRIMARY KEY (id),
    UNIQUE KEY uq_cycle_tontine_numero (tontine_id, numero),
    CONSTRAINT fk_cycle_tontine FOREIGN KEY (tontine_id) REFERENCES tontine(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- DISTRIBUTION : Cycle "1" --> "0..1" Distribution
-- ------------------------------------------------------------
CREATE TABLE distribution (
    id                  CHAR(36)        NOT NULL DEFAULT (UUID()),
    cycle_id            CHAR(36)        NOT NULL,
    date                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    montant_total        DECIMAL(12,2)   NOT NULL,
    beneficiaire_id      CHAR(36)        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_distribution_cycle (cycle_id),
    CONSTRAINT fk_distribution_cycle FOREIGN KEY (cycle_id) REFERENCES cycle(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- ADHESION : lien Membre <-> Tontine, avec le rôle contextuel
-- Référence membre(utilisateur_id) : seul un Membre peut avoir
-- une adhésion (cohérent avec Membre "1" --> "0..*" Adhesion).
-- ------------------------------------------------------------
CREATE TABLE adhesion (
    id                  CHAR(36)        NOT NULL DEFAULT (UUID()),
    tontine_id          CHAR(36)        NOT NULL,
    membre_id           CHAR(36)        NOT NULL,
    role                ENUM('membre','gerant') NOT NULL DEFAULT 'membre',
    date_adhesion       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    solde               DECIMAL(12,2)   NOT NULL DEFAULT 0,
    total_cotise         DECIMAL(12,2)   NOT NULL DEFAULT 0,
    cotisations_faites   INT             NOT NULL DEFAULT 0,
    statut              ENUM('actif','inactif') NOT NULL DEFAULT 'actif',
    PRIMARY KEY (id),
    UNIQUE KEY uq_adhesion_tontine_membre (tontine_id, membre_id),
    CONSTRAINT fk_adhesion_tontine FOREIGN KEY (tontine_id) REFERENCES tontine(id) ON DELETE CASCADE,
    CONSTRAINT fk_adhesion_membre FOREIGN KEY (membre_id) REFERENCES membre(utilisateur_id)
) ENGINE=InnoDB;

CREATE INDEX idx_adhesion_membre ON adhesion(membre_id);

-- ------------------------------------------------------------
-- MODE_PAIEMENT (hiérarchie ModePaiement / Wave / OrangeMoney / Espèces)
-- Même logique d'héritage table-par-classe que Utilisateur.
-- ------------------------------------------------------------
CREATE TABLE mode_paiement (
    id                  CHAR(36)        NOT NULL DEFAULT (UUID()),
    reference           VARCHAR(100)    NULL,
    statut              ENUM('initie','confirme','echoue') NOT NULL DEFAULT 'initie',
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE wave (
    mode_paiement_id    CHAR(36)        NOT NULL,
    numero_wave          VARCHAR(20)     NOT NULL,
    PRIMARY KEY (mode_paiement_id),
    CONSTRAINT fk_wave_mode_paiement FOREIGN KEY (mode_paiement_id) REFERENCES mode_paiement(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE orange_money (
    mode_paiement_id     CHAR(36)        NOT NULL,
    numero_orange_money    VARCHAR(20)     NOT NULL,
    PRIMARY KEY (mode_paiement_id),
    CONSTRAINT fk_orange_money_mode_paiement FOREIGN KEY (mode_paiement_id) REFERENCES mode_paiement(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE especes (
    mode_paiement_id    CHAR(36)        NOT NULL,
    PRIMARY KEY (mode_paiement_id),
    CONSTRAINT fk_especes_mode_paiement FOREIGN KEY (mode_paiement_id) REFERENCES mode_paiement(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- TRANSACTION : cotisation (entrée) ou versement (sortie)
-- ------------------------------------------------------------
CREATE TABLE transaction_tontine (
    id                  CHAR(36)        NOT NULL DEFAULT (UUID()),
    tontine_id          CHAR(36)        NOT NULL,
    membre_id           CHAR(36)        NOT NULL,
    distribution_id     CHAR(36)        NULL,        -- renseigné si type = 'versement'
    mode_paiement_id    CHAR(36)        NULL,        -- NULL pour une pénalité (ce n'est pas un paiement)
    type                ENUM('cotisation','versement','remboursement_pret','penalite') NOT NULL,
    montant             DECIMAL(12,2)   NOT NULL,
    date                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut              ENUM('en_attente','validee','echouee') NOT NULL DEFAULT 'validee',
    libelle             VARCHAR(255)    NULL,        -- explication (ex. « Retard — cycle n°3 »)
    PRIMARY KEY (id),
    CONSTRAINT fk_transaction_tontine FOREIGN KEY (tontine_id) REFERENCES tontine(id) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_membre FOREIGN KEY (membre_id) REFERENCES membre(utilisateur_id),
    CONSTRAINT fk_transaction_distribution FOREIGN KEY (distribution_id) REFERENCES distribution(id),
    CONSTRAINT fk_transaction_mode_paiement FOREIGN KEY (mode_paiement_id) REFERENCES mode_paiement(id)
) ENGINE=InnoDB;

CREATE INDEX idx_transaction_tontine ON transaction_tontine(tontine_id);
CREATE INDEX idx_transaction_membre ON transaction_tontine(membre_id);

-- ------------------------------------------------------------
-- DEMANDE_ADHESION : validée/refusée par le Gérant
-- (le demandeur n'est pas encore Membre, donc pas de FK vers membre)
-- ------------------------------------------------------------
CREATE TABLE demande_adhesion (
    id                  CHAR(36)        NOT NULL DEFAULT (UUID()),
    tontine_id          CHAR(36)        NOT NULL,
    nom                 VARCHAR(150)    NOT NULL,
    tel                 VARCHAR(20)     NOT NULL,
    email               VARCHAR(150)    NULL,
    mot_de_passe_hash          VARCHAR(255)    NULL,
    piece_identite_hash        VARCHAR(255)    NULL,
    certificat_residence_hash  VARCHAR(255)    NULL,
    cgu_acceptees               BOOLEAN         NOT NULL DEFAULT FALSE,
    date_envoi          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut              ENUM('en_attente','validee','refusee') NOT NULL DEFAULT 'en_attente',
    contact_ref         VARCHAR(150)    NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_demande_adhesion_tontine FOREIGN KEY (tontine_id) REFERENCES tontine(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- DEMANDE_CREATION : validée/refusée par l'Administrateur
-- N'importe quel Utilisateur peut soumettre, pas seulement un Membre.
-- ------------------------------------------------------------
CREATE TABLE demande_creation (
    id                  CHAR(36)        NOT NULL DEFAULT (UUID()),
    demandeur_id         CHAR(36)        NOT NULL,
    nom_souhaite          VARCHAR(150)    NOT NULL,
    type_souhaite         ENUM('fonds_commun','tour_de_role','tirage_au_sort') NOT NULL,
    montant              DECIMAL(12,2)   NOT NULL,
    montant_min          DECIMAL(12,2)   NULL,       -- Fonds commun : fourchette proposée par le gérant
    montant_max          DECIMAL(12,2)   NULL,
    frequence            ENUM('hebdomadaire','mensuelle') NOT NULL,
    nb_membres           INT             NOT NULL,
    date_envoi          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut              ENUM('en_attente','validee','refusee') NOT NULL DEFAULT 'en_attente',
    PRIMARY KEY (id),
    CONSTRAINT fk_demande_creation_demandeur FOREIGN KEY (demandeur_id) REFERENCES utilisateur(id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- PRET : demandé par un Membre, uniquement sur une TontineFondsCommun
-- ------------------------------------------------------------
CREATE TABLE pret (
    id                  CHAR(36)        NOT NULL DEFAULT (UUID()),
    tontine_id          CHAR(36)        NOT NULL,
    membre_id           CHAR(36)        NOT NULL,
    montant             DECIMAL(12,2)   NOT NULL,
    duree_jours           INT             NOT NULL,
    motif                VARCHAR(255)    NULL,
    date_demande          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut              ENUM('en_attente','valide','refuse','rembourse') NOT NULL DEFAULT 'en_attente',
    valideur              ENUM('gerant','administrateur') NULL,
    montant_rembourse      DECIMAL(12,2)   NULL,
    date_remboursement     DATETIME        NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pret_tontine FOREIGN KEY (tontine_id) REFERENCES tontine(id) ON DELETE CASCADE,
    CONSTRAINT fk_pret_membre FOREIGN KEY (membre_id) REFERENCES membre(utilisateur_id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- TIRAGE : uniquement pour une TontineTirageAuSort
-- ------------------------------------------------------------
CREATE TABLE tirage (
    id                  CHAR(36)        NOT NULL DEFAULT (UUID()),
    tontine_id          CHAR(36)        NOT NULL,
    semaine              INT             NOT NULL,
    serie                INT             NOT NULL DEFAULT 1,  -- série de la tontine à laquelle appartient ce tirage
    date                DATE            NOT NULL,
    beneficiaire_id       CHAR(36)        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_tirage_tontine FOREIGN KEY (tontine_id) REFERENCES tontine(id) ON DELETE CASCADE,
    CONSTRAINT fk_tirage_beneficiaire FOREIGN KEY (beneficiaire_id) REFERENCES membre(utilisateur_id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- DEMANDE_NOUVELLE_SERIE : quand une tontine est clôturée, son gérant peut demander
-- une nouvelle série ; elle ne démarre qu'avec la validation de l'administrateur.
-- La série courante d'une tontine = 1 + nombre de demandes validées.
-- ------------------------------------------------------------
CREATE TABLE demande_nouvelle_serie (
    id                  CHAR(36)        NOT NULL DEFAULT (UUID()),
    tontine_id          CHAR(36)        NOT NULL,
    gerant_id           CHAR(36)        NOT NULL,
    date_envoi          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut              ENUM('en_attente','validee','refusee') NOT NULL DEFAULT 'en_attente',
    date_decision       DATETIME        NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_nouvelle_serie_tontine FOREIGN KEY (tontine_id) REFERENCES tontine(id) ON DELETE CASCADE,
    CONSTRAINT fk_nouvelle_serie_gerant FOREIGN KEY (gerant_id) REFERENCES gerant(utilisateur_id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- DEMANDE_REINITIALISATION_MDP : un utilisateur qui a oublié son mot de passe en fait la demande
-- depuis l'écran de connexion ; l'administrateur vérifie son identité puis lui remet un mot de
-- passe provisoire (statut 'validee') ou refuse ('refusee').
-- ------------------------------------------------------------
CREATE TABLE demande_reinitialisation_mdp (
    id                  CHAR(36)        NOT NULL DEFAULT (UUID()),
    utilisateur_id      CHAR(36)        NOT NULL,
    date_envoi          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut              ENUM('en_attente','validee','refusee') NOT NULL DEFAULT 'en_attente',
    date_traitement     DATETIME        NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reinit_mdp_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- DEMANDE_TOUR_ANTICIPE : dans une tontine Tour de rôle, un membre demande à recevoir sa distribution plus
-- tôt. Le gérant accorde (le membre passe juste après le bénéficiaire actuel) ou refuse.
-- ------------------------------------------------------------
CREATE TABLE demande_tour_anticipe (
    id                  CHAR(36)        NOT NULL DEFAULT (UUID()),
    tontine_id          CHAR(36)        NOT NULL,
    membre_id           CHAR(36)        NOT NULL,
    motif               VARCHAR(500)    NOT NULL,
    date_envoi          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut              ENUM('en_attente','validee','refusee') NOT NULL DEFAULT 'en_attente',
    date_decision       DATETIME        NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_tour_anticipe_tontine FOREIGN KEY (tontine_id) REFERENCES tontine(id) ON DELETE CASCADE,
    CONSTRAINT fk_tour_anticipe_membre FOREIGN KEY (membre_id) REFERENCES membre(utilisateur_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- NOTIFICATION : Utilisateur "1" --> "0..*" Notification
-- (n'importe quel compte peut recevoir une notification)
-- ------------------------------------------------------------
CREATE TABLE notification (
    id                  CHAR(36)        NOT NULL DEFAULT (UUID()),
    utilisateur_id      CHAR(36)        NOT NULL,
    message             VARCHAR(500)    NOT NULL,
    date_envoi          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    type                VARCHAR(50)     NOT NULL,
    ref_id              CHAR(36)        NULL,
    lue                 BOOLEAN         NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_notification_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- JOURNAL_AUDIT
-- ------------------------------------------------------------
CREATE TABLE journal_audit (
    id                  CHAR(36)        NOT NULL DEFAULT (UUID()),
    ts                  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acteur_id           CHAR(36)        NULL,
    action               VARCHAR(255)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_journal_audit_acteur FOREIGN KEY (acteur_id) REFERENCES utilisateur(id)
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- Compte administrateur initial (obligatoire pour la 1re connexion)
-- Téléphone : 770000000 — mot de passe : admin123
-- (hash SHA-256 en base64 de "admin123", généré par sn.naat.util.Utils.hasher)
-- ============================================================
INSERT INTO utilisateur (id, nom, initials, tel, email, mot_de_passe_hash, statut_compte)
VALUES ('admin-0001', 'Administrateur NAAT', 'AN', '770000000', 'admin@naat.sn',
        'JAvlGPq9JyTdtvBO6x2llnRI1+gxwIyPqCKAn3THIKk=', 'actif');

INSERT INTO administrateur (utilisateur_id) VALUES ('admin-0001');

-- ============================================================
-- Notes de conception :
--  - membre / gerant / administrateur sont créées par
--    l'application, pas par l'utilisateur final : une ligne
--    "membre" apparaît à la première adhésion validée ; une
--    ligne "gerant" au moment où on lui confie une tontine ;
--    une ligne "administrateur" à la création manuelle d'un
--    compte admin.
--  - pret.tontine_id doit référencer une tontine de type
--    'fonds_commun' uniquement (contrainte applicative).
--  - tirage.tontine_id doit référencer une tontine de type
--    'tirage_au_sort' uniquement (contrainte applicative).
--  - pret.valideur = 'gerant' interdit si le gérant est aussi
--    le demandeur du prêt (règle anti-conflit, applicative).
-- ============================================================
