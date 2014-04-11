-- DATABASE: datalog_archiving on postgresql.colo.elex.be

-- TABLE cordat

-- DROP TABLE cordat

CREATE TABLE cordat
(
  lot character varying(150) NOT NULL,
  date_of_last_shipment timestamp with time zone NOT NULL,
  date_of_archiving timestamp with time zone,
  archived character varying(3) 
)
WITH (
  OIDS=TRUE
);
ALTER TABLE cordat
  OWNER TO archiver;
GRANT ALL ON TABLE cordat TO archiver;
GRANT SELECT ON TABLE cordat TO public;

-- Index: "cordat-lot-dateoflastshipment-archived"

-- DROP INDEX "cordat-lot-dateoflastshipment-archived";

CREATE INDEX "cordat-lot-dateoflastshipment-archived"
  ON cordat
  USING btree
  (lot,date_of_last_shipment, archived);

insert into cordat (lot,date_of_last_shipment, archived) values ('A12345', CURRENT_DATE , 'NO');
