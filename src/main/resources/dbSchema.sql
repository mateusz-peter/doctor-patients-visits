DROP TABLE IF EXISTS visit;
DROP TABLE IF EXISTS doctor;
DROP TABLE IF EXISTS patient;

CREATE TABLE patient(
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name text NOT NULL,
    last_name text NOT NULL,
    address text NOT NULL
);
CREATE INDEX ON patient(last_name, first_name);

CREATE TABLE doctor(
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name text NOT NULL,
    last_name text NOT NULL,
    specialty text NOT NULL
);
CREATE INDEX ON doctor(last_name, first_name);

CREATE TABLE visit(
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    visit_date date NOT NULL,
    visit_hour time NOT NULL,
    place text NOT NULL,
    doctor_id bigint references doctor(id),
    patient_id bigint references patient(id),
    UNIQUE (visit_date, visit_hour, doctor_id),
    CHECK ( date_part('minute', visit_hour) = 0 AND date_part('microseconds', visit_hour) = 0 )
);
CREATE INDEX ON visit(patient_id);
CREATE INDEX ON visit(visit_date, visit_hour);