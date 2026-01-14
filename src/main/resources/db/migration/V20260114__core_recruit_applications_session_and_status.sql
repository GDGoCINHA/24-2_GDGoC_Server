alter table core_recruit_applications
    add column if not exists user_id bigint,
    add column if not exists session varchar(32),
    add column if not exists result_status varchar(32) default 'SUBMITTED',
    add column if not exists reviewed_at timestamptz,
    add column if not exists reviewed_by bigint,
    add column if not exists result_note text;

update core_recruit_applications
set session = coalesce(session, 'UNKNOWN');

alter table core_recruit_applications
    alter column session set not null;

alter table core_recruit_applications
    alter column result_status set not null;

update core_recruit_applications cra
set user_id = u.id
from users u
where cra.user_id is null
  and u.email = cra.email;

alter table core_recruit_applications
    alter column user_id set not null;

alter table core_recruit_applications
    add constraint fk_core_recruit_applications_user
        foreign key (user_id) references users (id) on delete cascade;

create unique index if not exists uq_core_recruit_user_session
    on core_recruit_applications (user_id, session);
