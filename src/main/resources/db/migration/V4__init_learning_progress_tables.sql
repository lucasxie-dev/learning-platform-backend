create table course_enrollment (
    id bigserial primary key,
    created_at timestamptz,
    updated_at timestamptz,
    user_id bigint not null,
    course_id bigint not null,
    enrolled_at timestamptz not null,
    constraint uk_course_enrollment_user_course unique (user_id, course_id)
);

create table lesson_progress (
    id bigserial primary key,
    created_at timestamptz,
    updated_at timestamptz,
    user_id bigint not null,
    course_id bigint not null,
    lesson_id bigint not null,
    progress_seconds integer not null,
    completed boolean not null,
    completed_at timestamptz,
    constraint uk_lesson_progress_user_lesson unique (user_id, lesson_id)
);

create index idx_course_enrollment_user_id on course_enrollment (user_id);
create index idx_course_enrollment_course_id on course_enrollment (course_id);
create index idx_lesson_progress_user_id on lesson_progress (user_id);
create index idx_lesson_progress_course_id on lesson_progress (course_id);
create index idx_lesson_progress_lesson_id on lesson_progress (lesson_id);
create index idx_lesson_progress_user_course on lesson_progress (user_id, course_id);
create index idx_lesson_progress_completed on lesson_progress (completed);
