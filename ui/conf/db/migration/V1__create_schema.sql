create table "advices" (
  "id" bigint not null primary key,
  "error_text_raw" varchar not null,
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
