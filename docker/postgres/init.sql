\getenv app_password AMS_DB_PASSWORD
select format('create role ams login password %L', :'app_password') \gexec
create schema academic authorization ams;
alter role ams in database thapar_ams set search_path = academic;
revoke create on schema public from public;
create database thapar_ams_test owner ams;
