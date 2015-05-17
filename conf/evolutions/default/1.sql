# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table coordinate (
  id                        bigint auto_increment not null,
  star_map_id               bigint,
  star_id                   bigint,
  x                         double,
  y                         double,
  radius                    double,
  constraint pk_coordinate primary key (id))
;

create table star (
  id                        bigint auto_increment not null,
  name                      varchar(255),
  type                      varchar(255),
  flat_name                 varchar(255),
  constraint pk_star primary key (id))
;

create table star_map (
  id                        bigint auto_increment not null,
  s3image_url               varchar(255),
  submission_id             varchar(255),
  job_id                    varchar(255),
  job_status                varchar(255),
  image_annotations         longtext,
  user_id                   bigint,
  created_date              datetime not null,
  constraint pk_star_map primary key (id))
;

create table user_info (
  id                        bigint auto_increment not null,
  type                      varchar(255),
  email                     varchar(255),
  password                  varchar(255),
  constraint pk_user_info primary key (id))
;


create table star_map_star (
  star_map_id                    bigint not null,
  star_id                        bigint not null,
  constraint pk_star_map_star primary key (star_map_id, star_id))
;
alter table coordinate add constraint fk_coordinate_starMap_1 foreign key (star_map_id) references star_map (id) on delete restrict on update restrict;
create index ix_coordinate_starMap_1 on coordinate (star_map_id);
alter table coordinate add constraint fk_coordinate_star_2 foreign key (star_id) references star (id) on delete restrict on update restrict;
create index ix_coordinate_star_2 on coordinate (star_id);
alter table star_map add constraint fk_star_map_user_3 foreign key (user_id) references user_info (id) on delete restrict on update restrict;
create index ix_star_map_user_3 on star_map (user_id);



alter table star_map_star add constraint fk_star_map_star_star_map_01 foreign key (star_map_id) references star_map (id) on delete restrict on update restrict;

alter table star_map_star add constraint fk_star_map_star_star_02 foreign key (star_id) references star (id) on delete restrict on update restrict;

# --- !Downs

SET FOREIGN_KEY_CHECKS=0;

drop table coordinate;

drop table star;

drop table star_map_star;

drop table star_map;

drop table user_info;

SET FOREIGN_KEY_CHECKS=1;

