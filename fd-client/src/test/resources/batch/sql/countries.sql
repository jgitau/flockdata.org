CREATE TABLE COUNTRY (
    ID int GENERATED BY DEFAULT AS IDENTITY (START WITH 1, INCREMENT BY 1) NOT NULL,
    CODE varchar(25) NOT NULL,
    NAME varchar(25) NOT NULL
);