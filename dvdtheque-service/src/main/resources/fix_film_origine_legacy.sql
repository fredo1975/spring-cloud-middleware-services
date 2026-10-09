-- One-off data migration: repair legacy numeric values in film.origine.
--
-- Commit d9463ec changed the "origine" column from int4 (enum ordinal) to
-- varchar(255) with @Enumerated(EnumType.STRING), but existing integer values
-- were kept as text (e.g. '8'). Hibernate then fails on read with
-- "No enum constant enums.FilmOrigine.8".
--
-- The mapping below matches the enum order at migration time (before ARTE was
-- prepended in commit e8b4672). Constants were only ever appended before this
-- point, so the prefix ordinals are stable:
--   DVD, EN_SALLE, TV, GOOGLE_PLAY, CANAL_PLUS, NETFLIX, AMAZON_PRIME, DISNEY_PLUS, TOUS
--
-- Note: rows inserted during the short-lived "add arte" window
-- (deb19e7 -> 4fa0270) may have used shifted ordinals; inspect the preview
-- query below before applying if that is a concern.

-- Preview the rows that will be changed.
SELECT origine, COUNT(*) AS nb
FROM "dvdtheque-service".film
WHERE origine ~ '^[0-9]+$'
GROUP BY origine
ORDER BY origine;

-- Convert legacy ordinals to enum names.
UPDATE "dvdtheque-service".film
SET origine = CASE origine
    WHEN '0' THEN 'DVD'
    WHEN '1' THEN 'EN_SALLE'
    WHEN '2' THEN 'TV'
    WHEN '3' THEN 'GOOGLE_PLAY'
    WHEN '4' THEN 'CANAL_PLUS'
    WHEN '5' THEN 'NETFLIX'
    WHEN '6' THEN 'AMAZON_PRIME'
    WHEN '7' THEN 'DISNEY_PLUS'
    WHEN '8' THEN 'TOUS'
    ELSE origine
END
WHERE origine ~ '^[0-9]+$';
