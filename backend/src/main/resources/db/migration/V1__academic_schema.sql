create table ams_user (
id bigint auto_increment primary key,
identifier varchar(120) not null unique,
password_hash varchar(255) not null,
name varchar(255) not null,
role varchar(20) not null,
notifications_enabled boolean not null
);
create table ams_faculty (
id bigint auto_increment primary key,
user_id bigint not null unique,
foreign key (user_id) references ams_user(id),
department varchar(255)
);
create table ams_student (
id bigint auto_increment primary key,
user_id bigint not null unique,
foreign key (user_id) references ams_user(id),
enrollment_number varchar(255) unique,
program varchar(255)
);
create table ams_course (
id bigint auto_increment primary key,
faculty_id bigint not null,
foreign key (faculty_id) references ams_faculty(id),
code varchar(255) not null unique,
title varchar(255) not null,
section varchar(255),
description varchar(2000)
);
create table ams_enrollment (
id bigint auto_increment primary key,
student_id bigint not null,
foreign key (student_id) references ams_student(id),
course_id bigint not null,
foreign key (course_id) references ams_course(id),
unique (student_id,course_id)
);
create table ams_classsession (
id bigint auto_increment primary key,
course_id bigint not null,
foreign key (course_id) references ams_course(id),
starts_at timestamp(6) not null,
ends_at timestamp(6) not null,
room varchar(255),
cancelled boolean not null,
token_hash varchar(255),
token_expires_at timestamp(6)
);
create table ams_attendancerecord (
id bigint auto_increment primary key,
student_id bigint not null,
foreign key (student_id) references ams_student(id),
class_session_id bigint not null,
foreign key (class_session_id) references ams_classsession(id),
recorded_at timestamp(6),
unique (student_id,class_session_id)
);
create table ams_assessment (
id bigint auto_increment primary key,
course_id bigint not null,
foreign key (course_id) references ams_course(id),
title varchar(255) not null,
kind varchar(255),
deadline timestamp(6) not null,
published boolean not null
);
create table ams_submission (
id bigint auto_increment primary key,
assessment_id bigint not null,
foreign key (assessment_id) references ams_assessment(id),
student_id bigint not null,
foreign key (student_id) references ams_student(id),
content varchar(4000),
submitted_at timestamp(6),
unique (student_id,assessment_id)
);
create table ams_grade (
id bigint auto_increment primary key,
submission_id bigint not null unique,
foreign key (submission_id) references ams_submission(id),
marks integer not null,
published boolean not null
);
create table ams_message (
id bigint auto_increment primary key,
sender_id bigint not null,
foreign key (sender_id) references ams_user(id),
receiver_id bigint not null,
foreign key (receiver_id) references ams_user(id),
body varchar(4000) not null,
sent_at timestamp(6) not null
);
create table ams_notification (
id bigint auto_increment primary key,
user_id bigint not null,
foreign key (user_id) references ams_user(id),
title varchar(255) not null,
read boolean not null
);
create table ams_calendarevent (
id bigint auto_increment primary key,
faculty_id bigint not null,
foreign key (faculty_id) references ams_faculty(id),
title varchar(255) not null,
event_date date not null
);
create table ams_learningresource (
id bigint auto_increment primary key,
course_id bigint not null,
foreign key (course_id) references ams_course(id),
title varchar(255) not null,
kind varchar(255),
content longblob,
content_type varchar(255)
);