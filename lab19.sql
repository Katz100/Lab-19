CREATE DATABASE IF NOT EXISTS prescription;
USE prescription;

CREATE TABLE doctor
(
    id             INT         NOT NULL AUTO_INCREMENT,
    ssn            VARCHAR(9)  NOT NULL UNIQUE,
    last_name      VARCHAR(30) NOT NULL,
    first_name     VARCHAR(30) NOT NULL,
    specialty      VARCHAR(30),
    practice_since INT,
    PRIMARY KEY (id)
);

CREATE TABLE patient
(
    id         INT          NOT NULL AUTO_INCREMENT,
    ssn        VARCHAR(9)   NOT NULL UNIQUE,
    first_name VARCHAR(45)  NOT NULL,
    last_name  VARCHAR(45)  NOT NULL,
    birthdate  DATE         NOT NULL,
    street     VARCHAR(100) NOT NULL,
    city       VARCHAR(45)  NOT NULL,
    state      CHAR(2)      NOT NULL,
    zipcode    CHAR(5)      NOT NULL,
    doctor_id  INT          NULL,
    PRIMARY KEY (id),
    KEY idx_patient_last_name (last_name),
    CONSTRAINT fk_patient_doctor1
        FOREIGN KEY (doctor_id) REFERENCES doctor (id)
            ON DELETE SET NULL
            ON UPDATE CASCADE
);

CREATE TABLE drug
(
    DrugID   INT         NOT NULL AUTO_INCREMENT,
    DrugName VARCHAR(45) NOT NULL,
    IdvDrugPrice DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (DrugID),
    UNIQUE KEY uq_drug_drugname (DrugName)
);

CREATE TABLE prescription
(
    RXID         INT NOT NULL AUTO_INCREMENT,
    Doctor_ID    INT NOT NULL,
    Patient_ID   INT NOT NULL,
    Drug_DrugID  INT NOT NULL,
    Quantity     INT NOT NULL,
    NumOfRefills INT NOT NULL,
    PRIMARY KEY (RXID),
    CONSTRAINT fk_prescription_doctor1
        FOREIGN KEY (Doctor_ID) REFERENCES doctor (id)
            ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_prescription_patient1
        FOREIGN KEY (Patient_ID) REFERENCES patient (id)
            ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_prescription_drug1
        FOREIGN KEY (Drug_DrugID) REFERENCES drug (DrugID)
            ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE pharmacy
(
    pharmacy_id INT         NOT NULL AUTO_INCREMENT,
    name        VARCHAR(45) NOT NULL,
    address     VARCHAR(45) NOT NULL,
    phone       VARCHAR(20) NOT NULL,
    PRIMARY KEY (pharmacy_id)
);

CREATE TABLE prescription_fill
(
    fill_id     INT            NOT NULL AUTO_INCREMENT,
    rxid        INT            NOT NULL,
    pharmacy_id INT            NOT NULL,
    date_filled DATE           NOT NULL,
    price       DECIMAL(10, 2) NOT NULL,

    PRIMARY KEY (fill_id),
    CONSTRAINT fk_prescription
        FOREIGN KEY (rxid)
            REFERENCES prescription (rxid)
            ON DELETE CASCADE,

    CONSTRAINT fk_pharmacy
        FOREIGN KEY (pharmacy_id)
            REFERENCES pharmacy (pharmacy_id)
            ON DELETE SET NULL,

);
