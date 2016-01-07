# --- !Ups

create table "advices" (
  "id" bigint not null primary key,
  "compilation_error" varchar not null,
  "advice" varchar not null,
  "accepted" boolean not null,
  "library_group_id" varchar not null,
  "library_artifact_id" varchar not null,
  "library_version" varchar not null,
  "contributor_email" varchar,
  "contributor_twitter" varchar,
  "contributor_github" varchar,
  "comment" varchar
);

# --- !Downs

drop table "advices" if exists;