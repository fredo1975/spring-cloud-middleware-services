-- DROP SCHEMA "dvdtheque-allocine-service";

CREATE SCHEMA "dvdtheque-allocine-service" AUTHORIZATION dvdtheque;

-- DROP SEQUENCE "dvdtheque-allocine-service".critiquepresse_id_seq;

CREATE SEQUENCE "dvdtheque-allocine-service".critiquepresse_id_seq
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 2147483647
    START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "dvdtheque-allocine-service".fichefilm_id_seq;

CREATE SEQUENCE "dvdtheque-allocine-service".fichefilm_id_seq
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 2147483647
    START 1
	CACHE 1
	NO CYCLE;-- "dvdtheque-allocine-service".fichefilm definition

-- Drop table

-- DROP TABLE "dvdtheque-allocine-service".fichefilm;

CREATE TABLE "dvdtheque-allocine-service".fichefilm (
                                                        id serial4 NOT NULL,
                                                        allocine_film_id int4 NOT NULL,
                                                        url varchar(255) NOT NULL,
                                                        page_number int4 NOT NULL,
                                                        title varchar(255) NOT NULL,
                                                        creation_date timestamp NULL,
                                                        CONSTRAINT fichefilm_pkey PRIMARY KEY (id)
);


-- "dvdtheque-allocine-service".critiquepresse definition

-- Drop table

-- DROP TABLE "dvdtheque-allocine-service".critiquepresse;

CREATE TABLE "dvdtheque-allocine-service".critiquepresse (
                                                             id serial4 NOT NULL,
                                                             news_source varchar(255) NOT NULL,
                                                             rating float8 NOT NULL,
                                                             body text NOT NULL,
                                                             author varchar(255) NOT NULL,
                                                             fiche_film_id int4 NOT NULL,
                                                             CONSTRAINT critiquepresse_pkey PRIMARY KEY (id),
                                                             CONSTRAINT critiquepresse_fiche_film_id_fkey FOREIGN KEY (fiche_film_id) REFERENCES "dvdtheque-allocine-service".fichefilm(id)
);