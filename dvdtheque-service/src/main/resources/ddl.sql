-- DROP SCHEMA "dvdtheque-service";

CREATE SCHEMA "dvdtheque-service" AUTHORIZATION dvdtheque;

-- DROP SEQUENCE "dvdtheque-service".critiques_presse_id_seq;

CREATE SEQUENCE "dvdtheque-service".critiques_presse_id_seq
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 2147483647
    START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "dvdtheque-service".dvd_id_seq;

CREATE SEQUENCE "dvdtheque-service".dvd_id_seq
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 2147483647
    START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "dvdtheque-service".film_id_seq;

CREATE SEQUENCE "dvdtheque-service".film_id_seq
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 2147483647
    START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "dvdtheque-service".genre_id_seq;

CREATE SEQUENCE "dvdtheque-service".genre_id_seq
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 2147483647
    START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "dvdtheque-service".personne_id_seq;

CREATE SEQUENCE "dvdtheque-service".personne_id_seq
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 2147483647
    START 1
	CACHE 1
	NO CYCLE;-- "dvdtheque-service".critiques_presse definition

-- Drop table

-- DROP TABLE "dvdtheque-service".critiques_presse;

CREATE TABLE "dvdtheque-service".critiques_presse (
                                                      id serial4 NOT NULL,
                                                      code int4 NOT NULL,
                                                      nom_source varchar(50) NOT NULL,
                                                      auteur varchar(50) DEFAULT NULL::character varying NULL,
                                                      critique varchar(500) NULL,
                                                      note float8 NOT NULL,
                                                      CONSTRAINT critiques_presse_pkey PRIMARY KEY (id)
);


-- "dvdtheque-service".dvd definition

-- Drop table

-- DROP TABLE "dvdtheque-service".dvd;

CREATE TABLE "dvdtheque-service".dvd (
                                         id serial4 NOT NULL,
                                         annee int4 NULL,
                                         "zone" int4 NOT NULL,
                                         edition varchar(255) DEFAULT NULL::character varying NULL,
                                         date_rip timestamp NULL,
                                         format varchar(7) DEFAULT NULL::character varying NULL,
                                         ripped bool NOT NULL,
                                         date_sortie timestamp NULL,
                                         CONSTRAINT dvd_pkey PRIMARY KEY (id)
);


-- "dvdtheque-service".genre definition

-- Drop table

-- DROP TABLE "dvdtheque-service".genre;

CREATE TABLE "dvdtheque-service".genre (
                                           id serial4 NOT NULL,
                                           "name" varchar(250) DEFAULT NULL::character varying NULL,
                                           tmdb_id int4 NOT NULL,
                                           CONSTRAINT genre_pkey PRIMARY KEY (id)
);


-- "dvdtheque-service".personne definition

-- Drop table

-- DROP TABLE "dvdtheque-service".personne;

CREATE TABLE "dvdtheque-service".personne (
                                              id serial4 NOT NULL,
                                              nom varchar(255) NOT NULL,
                                              prenom varchar(255) DEFAULT NULL::character varying NULL,
                                              date_n timestamp NULL,
                                              id_pays int4 NULL,
                                              profile_path varchar(255) DEFAULT NULL::character varying NULL,
                                              CONSTRAINT personne_pkey PRIMARY KEY (id)
);


-- "dvdtheque-service".film definition

-- Drop table

-- DROP TABLE "dvdtheque-service".film;

CREATE TABLE "dvdtheque-service".film (
                                          id serial4 NOT NULL,
                                          vu bool NOT NULL,
                                          homepage varchar(255) DEFAULT '1' NULL,
                                          annee int4 NOT NULL,
                                          titre varchar(255) NOT NULL,
                                          titre_o varchar(255) DEFAULT NULL::character varying NULL,
                                          dvd_id int4 NULL,
                                          origine int4 NOT NULL,
                                          poster_path varchar(255) DEFAULT NULL::character varying NULL,
                                          tmdb_id int4 NULL,
                                          overview text NULL,
                                          runtime int4 NULL,
                                          date_sortie timestamp NOT NULL,
                                          date_insertion timestamp NOT NULL,
                                          vue_date date NULL,
                                          update_ts date NULL,
                                          allocine_fiche_film_id int4 NULL,
                                          date_sortie_dvd timestamp NULL,
                                          CONSTRAINT film_pkey PRIMARY KEY (id),
                                          CONSTRAINT film_dvd_id_fkey FOREIGN KEY (dvd_id) REFERENCES "dvdtheque-service".dvd(id)
);


-- "dvdtheque-service".film_acteur definition

-- Drop table

-- DROP TABLE "dvdtheque-service".film_acteur;

CREATE TABLE "dvdtheque-service".film_acteur (
                                                 film_id int4 NOT NULL,
                                                 acteur_id int4 NOT NULL,
                                                 CONSTRAINT film_acteur_pkey PRIMARY KEY (film_id, acteur_id),
                                                 CONSTRAINT film_acteur_acteur_id_fkey FOREIGN KEY (acteur_id) REFERENCES "dvdtheque-service".personne(id),
                                                 CONSTRAINT film_acteur_film_id_fkey FOREIGN KEY (film_id) REFERENCES "dvdtheque-service".film(id)
);


-- "dvdtheque-service".film_critiques_presse definition

-- Drop table

-- DROP TABLE "dvdtheque-service".film_critiques_presse;

CREATE TABLE "dvdtheque-service".film_critiques_presse (
                                                           film_id int4 NOT NULL,
                                                           critiques_presse_id int4 NOT NULL,
                                                           CONSTRAINT film_critiques_presse_pkey PRIMARY KEY (film_id, critiques_presse_id),
                                                           CONSTRAINT film_critiques_presse_critiques_presse_id_fkey FOREIGN KEY (critiques_presse_id) REFERENCES "dvdtheque-service".critiques_presse(id),
                                                           CONSTRAINT film_critiques_presse_film_id_fkey FOREIGN KEY (film_id) REFERENCES "dvdtheque-service".film(id)
);


-- "dvdtheque-service".film_genre definition

-- Drop table

-- DROP TABLE "dvdtheque-service".film_genre;

CREATE TABLE "dvdtheque-service".film_genre (
                                                film_id int4 NOT NULL,
                                                genre_id int4 NOT NULL,
                                                CONSTRAINT film_genre_pkey PRIMARY KEY (film_id, genre_id),
                                                CONSTRAINT film_genre_film_id_fkey FOREIGN KEY (film_id) REFERENCES "dvdtheque-service".film(id),
                                                CONSTRAINT film_genre_genre_id_fkey FOREIGN KEY (genre_id) REFERENCES "dvdtheque-service".genre(id)
);


-- "dvdtheque-service".film_realisateur definition

-- Drop table

-- DROP TABLE "dvdtheque-service".film_realisateur;

CREATE TABLE "dvdtheque-service".film_realisateur (
                                                      film_id int4 NOT NULL,
                                                      realisateur_id int4 NOT NULL,
                                                      CONSTRAINT film_realisateur_pkey PRIMARY KEY (film_id, realisateur_id),
                                                      CONSTRAINT film_realisateur_film_id_fkey FOREIGN KEY (film_id) REFERENCES "dvdtheque-service".film(id),
                                                      CONSTRAINT film_realisateur_realisateur_id_fkey FOREIGN KEY (realisateur_id) REFERENCES "dvdtheque-service".personne(id)
);