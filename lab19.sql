create database prescription;

create table doctor (
	id int primary key auto_increment,
    ssn varchar(9) not null unique,
    last_name varchar(30) not null,
    first_name varchar(30) not null,
    specialty varchar(30),
    practice_since int
);

create table patient (
	id int primary key auto_increment,
    doctor_id int not null references doctor(id),
    ssn varchar(9) not null unique,
    first_name varchar(30) not null,
    last_name varchar(30) not null,
    birthdate date not null,
    street varchar(45),
    city varchar(45),
    state varchar(45),
    zipcode varchar(45)
);