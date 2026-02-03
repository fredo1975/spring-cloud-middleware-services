-- DROP SCHEMA "dvdtheque-batch-service";

CREATE SCHEMA "dvdtheque-batch-service" AUTHORIZATION dvdtheque;

-- DROP SEQUENCE "dvdtheque-batch-service".batch_job_execution_seq;

CREATE SEQUENCE "dvdtheque-batch-service".batch_job_execution_seq
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "dvdtheque-batch-service".batch_job_seq;

CREATE SEQUENCE "dvdtheque-batch-service".batch_job_seq
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE "dvdtheque-batch-service".batch_step_execution_seq;

CREATE SEQUENCE "dvdtheque-batch-service".batch_step_execution_seq
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
	CACHE 1
	NO CYCLE;-- "dvdtheque-batch-service".batch_job_instance definition

-- Drop table

-- DROP TABLE "dvdtheque-batch-service".batch_job_instance;

CREATE TABLE "dvdtheque-batch-service".batch_job_instance (
                                                              job_instance_id int8 NOT NULL,
                                                              "version" int8 NULL,
                                                              job_name varchar(100) NOT NULL,
                                                              job_key varchar(32) NOT NULL,
                                                              CONSTRAINT batch_job_instance_pkey PRIMARY KEY (job_instance_id),
                                                              CONSTRAINT job_inst_un UNIQUE (job_name, job_key)
);


-- "dvdtheque-batch-service".batch_job_execution definition

-- Drop table

-- DROP TABLE "dvdtheque-batch-service".batch_job_execution;

CREATE TABLE "dvdtheque-batch-service".batch_job_execution (
                                                               job_execution_id int8 NOT NULL,
                                                               "version" int8 NULL,
                                                               job_instance_id int8 NOT NULL,
                                                               create_time timestamp NOT NULL,
                                                               start_time timestamp NULL,
                                                               end_time timestamp NULL,
                                                               status varchar(10) NULL,
                                                               exit_code varchar(2500) NULL,
                                                               exit_message varchar(2500) NULL,
                                                               last_updated timestamp NULL,
                                                               job_configuration_location varchar(2500) NULL,
                                                               CONSTRAINT batch_job_execution_pkey PRIMARY KEY (job_execution_id),
                                                               CONSTRAINT job_inst_exec_fk FOREIGN KEY (job_instance_id) REFERENCES "dvdtheque-batch-service".batch_job_instance(job_instance_id)
);


-- "dvdtheque-batch-service".batch_job_execution_context definition

-- Drop table

-- DROP TABLE "dvdtheque-batch-service".batch_job_execution_context;

CREATE TABLE "dvdtheque-batch-service".batch_job_execution_context (
                                                                       job_execution_id int8 NOT NULL,
                                                                       short_context varchar(2500) NOT NULL,
                                                                       serialized_context text NULL,
                                                                       CONSTRAINT batch_job_execution_context_pkey PRIMARY KEY (job_execution_id),
                                                                       CONSTRAINT job_exec_ctx_fk FOREIGN KEY (job_execution_id) REFERENCES "dvdtheque-batch-service".batch_job_execution(job_execution_id)
);


-- "dvdtheque-batch-service".batch_job_execution_params definition

-- Drop table

-- DROP TABLE "dvdtheque-batch-service".batch_job_execution_params;

CREATE TABLE "dvdtheque-batch-service".batch_job_execution_params (
                                                                      job_execution_id int8 NOT NULL,
                                                                      parameter_type varchar(100) NOT NULL,
                                                                      parameter_name varchar(100) NOT NULL,
                                                                      parameter_value varchar(2500) NULL,
                                                                      identifying bpchar(1) NOT NULL,
                                                                      CONSTRAINT job_exec_params_fk FOREIGN KEY (job_execution_id) REFERENCES "dvdtheque-batch-service".batch_job_execution(job_execution_id)
);


-- "dvdtheque-batch-service".batch_step_execution definition

-- Drop table

-- DROP TABLE "dvdtheque-batch-service".batch_step_execution;

CREATE TABLE "dvdtheque-batch-service".batch_step_execution (
                                                                step_execution_id int8 NOT NULL,
                                                                "version" int8 NOT NULL,
                                                                step_name varchar(100) NOT NULL,
                                                                job_execution_id int8 NOT NULL,
                                                                start_time timestamp NULL,
                                                                end_time timestamp NULL,
                                                                status varchar(10) NULL,
                                                                commit_count int8 NULL,
                                                                read_count int8 NULL,
                                                                filter_count int8 NULL,
                                                                write_count int8 NULL,
                                                                read_skip_count int8 NULL,
                                                                write_skip_count int8 NULL,
                                                                process_skip_count int8 NULL,
                                                                rollback_count int8 NULL,
                                                                exit_code varchar(2500) NULL,
                                                                exit_message varchar(2500) NULL,
                                                                last_updated timestamp NULL,
                                                                create_time timestamp DEFAULT '1970-01-01 00:00:00'::timestamp without time zone NOT NULL,
                                                                CONSTRAINT batch_step_execution_pkey PRIMARY KEY (step_execution_id),
                                                                CONSTRAINT job_exec_step_fk FOREIGN KEY (job_execution_id) REFERENCES "dvdtheque-batch-service".batch_job_execution(job_execution_id)
);


-- "dvdtheque-batch-service".batch_step_execution_context definition

-- Drop table

-- DROP TABLE "dvdtheque-batch-service".batch_step_execution_context;

CREATE TABLE "dvdtheque-batch-service".batch_step_execution_context (
                                                                        step_execution_id int8 NOT NULL,
                                                                        short_context varchar(2500) NOT NULL,
                                                                        serialized_context text NULL,
                                                                        CONSTRAINT batch_step_execution_context_pkey PRIMARY KEY (step_execution_id),
                                                                        CONSTRAINT step_exec_ctx_fk FOREIGN KEY (step_execution_id) REFERENCES "dvdtheque-batch-service".batch_step_execution(step_execution_id)
);