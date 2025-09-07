create table if not exists core_recruit_applications (
    id bigserial primary key,
    name varchar(255) not null,
    student_id varchar(64) not null,
    phone varchar(64) not null,
    major varchar(255) not null,
    email varchar(255) not null,
    team varchar(64) not null,
    motivation text not null,
    wish text not null,
    strengths text not null,
    pledge text not null,
    file_urls jsonb not null default '[]'::jsonb,
    created_at timestamptz not null default (now()),
    updated_at timestamptz not null default (now())
);


