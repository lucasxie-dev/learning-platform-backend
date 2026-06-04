create table course (
    id bigserial primary key,
    created_at timestamptz,
    updated_at timestamptz,
    title varchar(200) not null,
    subtitle varchar(300),
    description text,
    cover_file_id bigint,
    owner_id bigint not null,
    status varchar(30) not null,
    published_at timestamptz
);

create table lesson (
    id bigserial primary key,
    created_at timestamptz,
    updated_at timestamptz,
    course_id bigint not null,
    title varchar(200) not null,
    description text,
    sort_order integer not null,
    status varchar(30) not null,
    audio_file_id bigint,
    video_file_id bigint,
    subtitle_file_id bigint,
    duration_seconds integer
);

create index idx_course_owner_id on course (owner_id);
create index idx_course_status on course (status);
create index idx_course_published_at on course (published_at);
create index idx_lesson_course_id on lesson (course_id);
create index idx_lesson_status on lesson (status);
create index idx_lesson_course_sort_order on lesson (course_id, sort_order);
