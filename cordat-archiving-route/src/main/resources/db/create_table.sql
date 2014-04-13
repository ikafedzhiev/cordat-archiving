-- DATABASE: datalog_archiving on postgresql.colo.elex.be

-- DROP TABLE cordat;

CREATE TABLE cordat
(
  id bigint NOT NULL PRIMARY KEY,
  lotname character varying(50) NOT NULL ,
  date_of_last_shipment timestamp with time zone NOT NULL,
  date_of_archiving timestamp with time zone,
  archived boolean NOT NULL
)
WITH (
  OIDS=TRUE
);
ALTER TABLE cordat
  OWNER TO archiver;
GRANT ALL ON TABLE cordat TO archiver;
GRANT SELECT ON TABLE cordat TO public;

-- Index: "cordat-lotname-dateoflastshipment-archived"

-- DROP INDEX "cordat-lotname-dateoflastshipment-archived";

CREATE INDEX "cordat-lotname-dateoflastshipment-archived"
  ON cordat
  USING btree
  (lotname, date_of_last_shipment, archived);


-- DROP SEQUENCE cordat_id;

CREATE SEQUENCE cordat_id
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;
ALTER TABLE cordat_id
  OWNER TO archiver;
GRANT ALL ON TABLE cordat_id TO archiver;

insert into cordat (id, lotname, date_of_last_shipment, archived) values (nextval('cordat_id'), 'A12345', CURRENT_DATE, False);
