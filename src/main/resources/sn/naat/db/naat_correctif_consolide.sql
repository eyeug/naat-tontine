-- ============================================================
-- NAAT — Correctif consolidé de la base de données
-- Regroupe toutes les migrations accumulées au fil du projet :
--   1. tontine.duree_label
--   2. demande_adhesion : mot_de_passe_hash, piece_identite_hash,
--      certificat_residence_hash, cgu_acceptees
--   3. tontine.ordre_tour : JSON -> TEXT (le code y stocke une
--      liste séparée par des virgules, pas du JSON valide)
--   4. distribution.beneficiaire_id
--   5. pret.montant_rembourse / pret.date_remboursement
--   6. notification.ref_id
--   7. transaction_tontine.libelle                (explication d'une ligne d'historique)
--   8. transaction_tontine.type : ajout de 'penalite'  (retards marqués dans l'historique)
--   9. transaction_tontine.mode_paiement_id : nullable (une pénalité n'est pas un paiement)
--  10. cycle.serie                                (numéro de série d'un cycle)
--  11. tirage.serie                               (numéro de série d'un tirage)
--  12. table demande_nouvelle_serie               (nouvelle série demandée par le gérant, validée par l'admin)
--  13. utilisateur.doit_changer_mdp               (mot de passe provisoire à changer à la connexion)
--  14. table demande_reinitialisation_mdp         (mot de passe oublié, traité par l'administrateur)
--  15. table demande_tour_anticipe                (tour anticipé demandé par un membre, accordé par le gérant)
--
-- SANS RISQUE : chaque étape vérifie si la colonne existe déjà
-- avant de l'ajouter/modifier. Peut être exécuté plusieurs fois
-- sans erreur, quel que soit l'état actuel de votre base.
--
-- Utilisation :
--   mysql -u root -p naat < naat_correctif_consolide.sql
-- ============================================================

-- ------------------------------------------------------------
-- 1. tontine.duree_label
-- ------------------------------------------------------------
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tontine' AND COLUMN_NAME = 'duree_label'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE tontine ADD COLUMN duree_label VARCHAR(50) NULL AFTER frequence',
    'SELECT "tontine.duree_label existe déjà, ignoré" AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 2. demande_adhesion : colonnes mot de passe / KYC / CGU
-- ------------------------------------------------------------
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demande_adhesion' AND COLUMN_NAME = 'mot_de_passe_hash'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE demande_adhesion
        ADD COLUMN mot_de_passe_hash VARCHAR(255) NULL AFTER email,
        ADD COLUMN piece_identite_hash VARCHAR(255) NULL AFTER mot_de_passe_hash,
        ADD COLUMN certificat_residence_hash VARCHAR(255) NULL AFTER piece_identite_hash,
        ADD COLUMN cgu_acceptees BOOLEAN NOT NULL DEFAULT FALSE AFTER certificat_residence_hash',
    'SELECT "demande_adhesion : colonnes KYC existent déjà, ignoré" AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 3. tontine.ordre_tour : JSON -> TEXT (si encore en JSON)
-- ------------------------------------------------------------
SET @data_type = (
    SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tontine' AND COLUMN_NAME = 'ordre_tour'
);
SET @sql = IF(@data_type = 'json',
    'ALTER TABLE tontine MODIFY COLUMN ordre_tour TEXT NULL',
    'SELECT "tontine.ordre_tour déjà au bon type, ignoré" AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 4. distribution.beneficiaire_id
-- ------------------------------------------------------------
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'distribution' AND COLUMN_NAME = 'beneficiaire_id'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE distribution ADD COLUMN beneficiaire_id CHAR(36) NULL AFTER montant_total',
    'SELECT "distribution.beneficiaire_id existe déjà, ignoré" AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 5. pret.montant_rembourse / pret.date_remboursement
-- ------------------------------------------------------------
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pret' AND COLUMN_NAME = 'montant_rembourse'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE pret
        ADD COLUMN montant_rembourse DECIMAL(12,2) NULL AFTER valideur,
        ADD COLUMN date_remboursement DATETIME NULL AFTER montant_rembourse',
    'SELECT "pret : colonnes de remboursement existent déjà, ignoré" AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 6. notification.ref_id
-- ------------------------------------------------------------
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification' AND COLUMN_NAME = 'ref_id'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE notification ADD COLUMN ref_id CHAR(36) NULL AFTER type',
    'SELECT "notification.ref_id existe déjà, ignoré" AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 7. transaction_tontine.libelle
-- ------------------------------------------------------------
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transaction_tontine' AND COLUMN_NAME = 'libelle'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE transaction_tontine ADD COLUMN libelle VARCHAR(255) NULL AFTER statut',
    'SELECT "transaction_tontine.libelle existe déjà, ignoré" AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 8. transaction_tontine.type : ajout de la valeur 'penalite'
-- ------------------------------------------------------------
SET @type_col = (
    SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transaction_tontine' AND COLUMN_NAME = 'type'
);
SET @sql = IF(@type_col NOT LIKE '%penalite%',
    'ALTER TABLE transaction_tontine MODIFY COLUMN type ENUM(''cotisation'',''versement'',''remboursement_pret'',''penalite'') NOT NULL',
    'SELECT "transaction_tontine.type contient déjà penalite, ignoré" AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 9. transaction_tontine.mode_paiement_id : doit accepter NULL
--    (une pénalité de retard n'est pas un paiement, donc pas de mode de paiement)
-- ------------------------------------------------------------
SET @nullable = (
    SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transaction_tontine' AND COLUMN_NAME = 'mode_paiement_id'
);
SET @sql = IF(@nullable = 'NO',
    'ALTER TABLE transaction_tontine MODIFY COLUMN mode_paiement_id CHAR(36) NULL',
    'SELECT "transaction_tontine.mode_paiement_id accepte déjà NULL, ignoré" AS info');
SET FOREIGN_KEY_CHECKS = 0;
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------
-- 10. cycle.serie : les cycles existants appartiennent à la série 1
-- ------------------------------------------------------------
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cycle' AND COLUMN_NAME = 'serie'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE cycle ADD COLUMN serie INT NOT NULL DEFAULT 1 AFTER numero',
    'SELECT "cycle.serie existe déjà, ignoré" AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 11. tirage.serie : les tirages existants appartiennent à la série 1
-- ------------------------------------------------------------
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tirage' AND COLUMN_NAME = 'serie'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE tirage ADD COLUMN serie INT NOT NULL DEFAULT 1 AFTER semaine',
    'SELECT "tirage.serie existe déjà, ignoré" AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 12. Table des demandes de nouvelle série (IF NOT EXISTS : sans risque si déjà créée)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS demande_nouvelle_serie (
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
-- 13. utilisateur.doit_changer_mdp
-- ------------------------------------------------------------
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'utilisateur' AND COLUMN_NAME = 'doit_changer_mdp'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE utilisateur ADD COLUMN doit_changer_mdp BOOLEAN NOT NULL DEFAULT FALSE AFTER statut_compte',
    'SELECT "utilisateur.doit_changer_mdp existe déjà, ignoré" AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 14. Table des demandes de réinitialisation de mot de passe (IF NOT EXISTS : sans risque si déjà créée)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS demande_reinitialisation_mdp (
    id                  CHAR(36)        NOT NULL DEFAULT (UUID()),
    utilisateur_id      CHAR(36)        NOT NULL,
    date_envoi          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut              ENUM('en_attente','validee','refusee') NOT NULL DEFAULT 'en_attente',
    date_traitement     DATETIME        NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reinit_mdp_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 15. Table des demandes de tour anticipé (IF NOT EXISTS : sans risque si déjà créée)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS demande_tour_anticipe (
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
-- 16. Fourchette de cotisation (Fonds commun) : montant_min / montant_max sur tontine et demande_creation
--     (idempotent). Les tontines existantes sans fourchette restent à montant libre.
-- ------------------------------------------------------------
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tontine' AND COLUMN_NAME = 'montant_min'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE tontine ADD COLUMN montant_min DECIMAL(12,2) NULL',
    'SELECT "tontine.montant_min existe déjà, ignoré" AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tontine' AND COLUMN_NAME = 'montant_max'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE tontine ADD COLUMN montant_max DECIMAL(12,2) NULL',
    'SELECT "tontine.montant_max existe déjà, ignoré" AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demande_creation' AND COLUMN_NAME = 'montant_min'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE demande_creation ADD COLUMN montant_min DECIMAL(12,2) NULL',
    'SELECT "demande_creation.montant_min existe déjà, ignoré" AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demande_creation' AND COLUMN_NAME = 'montant_max'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE demande_creation ADD COLUMN montant_max DECIMAL(12,2) NULL',
    'SELECT "demande_creation.montant_max existe déjà, ignoré" AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Correctif appliqué avec succès.' AS resultat;
