create table loan_application (
    id bigserial not null primary key,
    amount NUMERIC(17, 2) not null,
    term bigserial not null,
    personal_id bigserial not null,
    country_code char(2) not null,

    application_date timestamp with time zone not null,
    accepted boolean not null,
    name text not null,
    surname text not null
);

create index apps_per_country on loan_application (country_code, application_date);
create index accepted_per_user on loan_application (accepted, personal_id);

create table blacklisted_id (
    personal_id bigserial not null primary key
);

insert into blacklisted_id values (1);
insert into blacklisted_id values (2);
insert into blacklisted_id values (3);