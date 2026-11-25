create table address
(
    id                 int auto_increment
        primary key,
    created_by         varchar(255) null,
    created_date       datetime(6)  null,
    last_modified_by   varchar(255) null,
    last_modified_date datetime(6)  null,
    version            int          not null,
    country            varchar(255) not null,
    description        varchar(255) null,
    external_id        varchar(255) null,
    number             varchar(10)  not null,
    postal_adress      bit          not null,
    street             varchar(255) not null,
    sub_premise        varchar(255) null,
    town               varchar(255) not null,
    zipcode            varchar(255) not null
);

create table activity
(
    id                         int auto_increment
        primary key,
    created_by                 varchar(255) null,
    created_date               datetime(6)  null,
    last_modified_by           varchar(255) null,
    last_modified_date         datetime(6)  null,
    version                    int          not null,
    closed                     datetime(6)  null,
    description                longtext     null,
    name                       varchar(255) null,
    open                       datetime(6)  null,
    additional_form            longtext     null,
    additional_form_rule       varchar(255) null,
    cancellable                bit          not null,
    cancelled                  bit          not null,
    communicationcc            varchar(255) null,
    end                        datetime(6)  null,
    price                      double       not null,
    registration_limit         int          null,
    send_complete_confirmation bit          not null,
    send_confirmation          bit          not null,
    start                      datetime(6)  null,
    reduction_factor           double       not null,
    sibling_reduction          double       not null,
    address_id                 int          null,
    constraint UKrdgwba68917o7wk5v2mh8bqwd
        unique (address_id),
    constraint FKo5943rkllpmdcdyfr40jxgu15
        foreign key (address_id) references address (id)
);

create table branch
(
    id                 int auto_increment
        primary key,
    created_by         varchar(255)                                   null,
    created_date       datetime(6)                                    null,
    last_modified_by   varchar(255)                                   null,
    last_modified_date datetime(6)                                    null,
    version            int                                            not null,
    description        longtext                                       null,
    email              varchar(255)                                   null,
    image              varchar(255)                                   null,
    law                longtext                                       null,
    maximum_age        int                                            null,
    minimum_age        int                                            not null,
    name               varchar(255)                                   null,
    sex                enum ('FEMALE', 'MALE', 'UNKNOWN')             null,
    staff_title        varchar(255)                                   null,
    status             enum ('ACTIVE', 'HIDDEN', 'MEMBER', 'PASSIVE') null
);

create table activity_restriction
(
    id                 int auto_increment
        primary key,
    created_by         varchar(255) null,
    created_date       datetime(6)  null,
    last_modified_by   varchar(255) null,
    last_modified_date datetime(6)  null,
    version            int          not null,
    alternative_end    datetime(6)  null,
    alternative_limit  int          null,
    alternative_price  double       null,
    alternative_start  datetime(6)  null,
    name               varchar(255) null,
    activity_id        int          null,
    branch_id          int          null,
    constraint FK7cs5y28lf8qur6e2w5oyq4rlr
        foreign key (activity_id) references activity (id),
    constraint FKotble52h1ki5ko88avc5xskha
        foreign key (branch_id) references branch (id)
);

create table calendar_item
(
    id                 int auto_increment
        primary key,
    created_by         varchar(255)  null,
    created_date       datetime(6)   null,
    last_modified_by   varchar(255)  null,
    last_modified_date datetime(6)   null,
    version            int           not null,
    closed             bit           not null,
    content            varchar(1000) not null,
    end                datetime(6)   not null,
    image              varchar(255)  null,
    start              datetime(6)   not null,
    title              varchar(255)  not null,
    address_id         int           null,
    constraint UKlfcq1t1dqbq86k94hkhdxhwfo
        unique (address_id),
    constraint FKq7a83bdkctbcbvy3chb0rfjiy
        foreign key (address_id) references address (id)
);

create table calendar_period
(
    id                 int auto_increment
        primary key,
    created_by         varchar(255) null,
    created_date       datetime(6)  null,
    last_modified_by   varchar(255) null,
    last_modified_date datetime(6)  null,
    version            int          not null,
    end                date         not null,
    name               varchar(255) not null,
    start              date         not null
);

create table calendar
(
    id                 int auto_increment
        primary key,
    created_by         varchar(255) null,
    created_date       datetime(6)  null,
    last_modified_by   varchar(255) null,
    last_modified_date datetime(6)  null,
    version            int          not null,
    intro              longtext     null,
    outro              longtext     null,
    branch_id          int          null,
    period_id          int          null,
    constraint UK4ekikgvb6ylur2i9i5kkoi3ms
        unique (period_id, branch_id),
    constraint FKanyio73jydkvjjhtps65glgqc
        foreign key (branch_id) references branch (id),
    constraint FKo85vporo7alall7espmxhijpg
        foreign key (period_id) references calendar_period (id)
);

create index idx_period
    on calendar (period_id);

create table calendar_items
(
    calendar_id int not null,
    item_id     int not null,
    constraint FK2tybav22oex1k4co001gfnx98
        foreign key (calendar_id) references calendar (id),
    constraint FKoo70asvfd1777c8essukmlk76
        foreign key (item_id) references calendar_item (id)
);

create table event
(
    id                         int auto_increment
        primary key,
    created_by                 varchar(255) null,
    created_date               datetime(6)  null,
    last_modified_by           varchar(255) null,
    last_modified_date         datetime(6)  null,
    version                    int          not null,
    closed                     datetime(6)  null,
    description                longtext     null,
    name                       varchar(255) null,
    open                       datetime(6)  null,
    additional_form            longtext     null,
    additional_form_rule       varchar(512) null,
    cancellable                bit          not null,
    cancelled                  bit          not null,
    communicationcc            varchar(255) null,
    end                        datetime(6)  null,
    price                      double       not null,
    registration_limit         int          null,
    send_complete_confirmation bit          not null,
    send_confirmation          bit          not null,
    start                      datetime(6)  null,
    needs_mobile               bit          not null,
    address_id                 int          null,
    constraint UK8wxm9i34wepsu094jo54sys6l
        unique (address_id),
    constraint FKbnsudi2pgjak7feycwo8297xi
        foreign key (address_id) references address (id)
);

create table medical_record
(
    id                    int                                                                not null
        primary key,
    created_by            varchar(255)                                                       null,
    created_date          datetime(6)                                                        null,
    last_modified_by      varchar(255)                                                       null,
    last_modified_date    datetime(6)                                                        null,
    version               int                                                                not null,
    activity_restrictions varchar(255)                                                       null,
    allergies             varchar(255)                                                       null,
    blood_group           enum ('ABN', 'ABP', 'AN', 'AP', 'BN', 'BP', 'ON', 'OP', 'UNKNOWN') null,
    diseases              varchar(255)                                                       null,
    family_remarks        varchar(255)                                                       null,
    food_anomalies        varchar(255)                                                       null,
    may_be_photographed   bit                                                                not null,
    may_take_painkillers  bit                                                                not null,
    medications           varchar(255)                                                       null,
    physician             varchar(255)                                                       null,
    physician_contact     varchar(255)                                                       null,
    social_remarks        varchar(255)                                                       null
);

create table membership_period
(
    id                 int auto_increment
        primary key,
    created_by         varchar(255) null,
    created_date       datetime(6)  null,
    last_modified_by   varchar(255) null,
    last_modified_date datetime(6)  null,
    version            int          not null,
    end                date         null,
    price              double       not null,
    reduction_factor   double       not null,
    registration_limit int          null,
    sibling_reduction  double       not null,
    start              date         null
);

create table membership_restriction
(
    id                 int auto_increment
        primary key,
    created_by         varchar(255) null,
    created_date       datetime(6)  null,
    last_modified_by   varchar(255) null,
    last_modified_date datetime(6)  null,
    version            int          not null,
    alternative_price  double       null,
    registration_limit int          null,
    branch_id          int          null,
    period_id          int          not null,
    alternative_start  date         null,
    constraint FK5pci60awc19av7geiwpv4h4ax
        foreign key (period_id) references membership_period (id),
    constraint FKmcgbpqr7p94dhhcv3wlufhp1g
        foreign key (branch_id) references branch (id)
);

create table news_item
(
    id                 int auto_increment
        primary key,
    created_by         varchar(255)  null,
    created_date       datetime(6)   null,
    last_modified_by   varchar(255)  null,
    last_modified_date datetime(6)   null,
    version            int           not null,
    content            varchar(1000) not null,
    image              varchar(255)  null,
    title              varchar(50)   not null,
    visible            bit           not null
);

create table organization
(
    id                 int auto_increment
        primary key,
    created_by         varchar(255)  null,
    created_date       datetime(6)   null,
    last_modified_by   varchar(255)  null,
    last_modified_date datetime(6)   null,
    version            int           not null,
    description        varchar(1000) null,
    external_id        varchar(255)  null,
    image              varchar(255)  null,
    kbo                varchar(255)  null,
    name               varchar(255)  null,
    type               tinyint       null
        check (`type` between 0 and 1),
    address_id         int           null,
    constraint idx_type
        unique (type),
    constraint FK97eigsvq2tsrd2bge4ox651wh
        foreign key (address_id) references address (id)
);

create table contact_method
(
    id                 int                                                                             not null
        primary key,
    created_by         varchar(255)                                                                    null,
    created_date       datetime(6)                                                                     null,
    last_modified_by   varchar(255)                                                                    null,
    last_modified_date datetime(6)                                                                     null,
    version            int                                                                             not null,
    type               enum ('EMAIL', 'FACEBOOK', 'INSTAGRAM', 'LINK', 'MOBILE', 'TIKTOK', 'WHATSAPP') null,
    value              varchar(255)                                                                    null,
    organization_id    int                                                                             null,
    constraint FK8op1rvv4np5n1kj5wvabjk1xy
        foreign key (organization_id) references organization (id)
);

create table role
(
    id                 int auto_increment
        primary key,
    backup_external_id varchar(255) null,
    external_id        varchar(255) null,
    level              tinyint      not null
        check (`level` between 0 and 4),
    name               varchar(255) null,
    staff_branch_id    int          null,
    branch_id          int          null,
    constraint FK53oym1gta6evpj0wqia1e8c0o
        foreign key (staff_branch_id) references branch (id),
    constraint FKk0cbbw7ms9xjr6wspeu877dou
        foreign key (branch_id) references branch (id)
);

create table setting
(
    name               varchar(255) not null
        primary key,
    created_by         varchar(255) null,
    created_date       datetime(6)  null,
    last_modified_by   varchar(255) null,
    last_modified_date datetime(6)  null,
    version            int          not null,
    value              varchar(255) null
);

create table shop
(
    id                 int auto_increment
        primary key,
    created_by         varchar(255) null,
    created_date       datetime(6)  null,
    last_modified_by   varchar(255) null,
    last_modified_date datetime(6)  null,
    version            int          not null,
    closed             datetime(6)  null,
    description        longtext     null,
    name               varchar(255) null,
    open               datetime(6)  null
);

create table shop_delivery_options
(
    shop_id          int                                                 not null,
    delivery_options enum ('ARRANGED', 'DELIVERY', 'PICKUP', 'SHIPPING') null,
    constraint FK23nj0b6ije2eupotlbtloyttd
        foreign key (shop_id) references shop (id)
);

create table shop_item
(
    id                 int auto_increment
        primary key,
    created_by         varchar(255) null,
    created_date       datetime(6)  null,
    last_modified_by   varchar(255) null,
    last_modified_date datetime(6)  null,
    version            int          not null,
    image              varchar(255) null,
    info               varchar(255) null,
    name               varchar(255) null,
    price              double       not null,
    shop_id            int          null,
    constraint FKbhqd8ctkbitg46dxocg0mdjas
        foreign key (shop_id) references shop (id)
);

create table order_item
(
    id                 int auto_increment
        primary key,
    created_by         varchar(255) null,
    created_date       datetime(6)  null,
    last_modified_by   varchar(255) null,
    last_modified_date datetime(6)  null,
    version            int          not null,
    amount             int          not null,
    shop_item_id       int          null,
    constraint FKlwnju23ok12rq3e6wlmcbqaa1
        foreign key (shop_item_id) references shop_item (id)
);

create table shop_item_options
(
    shop_item_id int          not null,
    options      varchar(255) null,
    constraint FK751i7iwshkfvcol5u2d5xs4na
        foreign key (shop_item_id) references shop_item (id)
);

create table shop_items
(
    shop_id  int not null,
    items_id int not null,
    constraint UKlmpv64ppi7q010pr27me3q45j
        unique (items_id),
    constraint FK3bo9f3bw73bgsu1myskndp673
        foreign key (shop_id) references shop (id),
    constraint FKby184bd3asd6f2291i70eyg83
        foreign key (items_id) references shop_item (id)
);

create table user
(
    id            int auto_increment
        primary key,
    account_no    varchar(255)     null,
    age_deviation int default 0    not null,
    birthdate     date             not null,
    customer_id   varchar(255)     null,
    email         varchar(255)     not null,
    external_id   varchar(255)     null,
    first_name    varchar(255)     not null,
    has_handicap  bit default b'0' not null,
    has_reduction bit default b'0' not null,
    image         varchar(255)     null,
    member_id     varchar(255)     null,
    mobile        varchar(255)     null,
    name          varchar(255)     not null,
    nis           varchar(255)     null,
    sex           tinyint          not null
        check (`sex` between 0 and 2),
    username      varchar(255)     null,
    constraint idx_username
        unique (username)
);

create table activity_registration
(
    id                 int auto_increment
        primary key,
    created_by         varchar(255) null,
    created_date       datetime(6)  null,
    last_modified_by   varchar(255) null,
    last_modified_date datetime(6)  null,
    version            int          not null,
    paid               bit          not null,
    payment_id         varchar(255) null,
    price              double       not null,
    additional_data    longtext     null,
    completed          bit          not null,
    end                datetime(6)  null,
    start              datetime(6)  null,
    subscribable_id    int          null,
    restriction_id     int          null,
    user_id            int          null,
    constraint FKjumv3vvncvcnuckwxadmse5hd
        foreign key (subscribable_id) references activity (id),
    constraint FKkqikqanfx6hx2m9fgspwdew4e
        foreign key (restriction_id) references activity_restriction (id),
    constraint FKqvqmwnbf2pjmwhp64ts4k1klb
        foreign key (user_id) references user (id)
);

create table contact
(
    id                 int auto_increment
        primary key,
    created_by         varchar(255) null,
    created_date       datetime(6)  null,
    last_modified_by   varchar(255) null,
    last_modified_date datetime(6)  null,
    version            int          not null,
    email              varchar(255) null,
    first_name         varchar(255) not null,
    mobile             varchar(255) null,
    name               varchar(255) not null,
    nis                varchar(255) null,
    address_id         int          null,
    external_id        varchar(255) null,
    role               varchar(20)  not null,
    user_id            int          null,
    constraint FKl0sju2uai8cgdtic18wy5hlfr
        foreign key (address_id) references address (id),
    constraint contact_user_fk
        foreign key (user_id) references user (id)
);

create table event_registration
(
    id                 int auto_increment
        primary key,
    created_by         varchar(255) null,
    created_date       datetime(6)  null,
    last_modified_by   varchar(255) null,
    last_modified_date datetime(6)  null,
    version            int          not null,
    paid               bit          not null,
    payment_id         varchar(255) null,
    price              double       not null,
    additional_data    longtext     null,
    completed          bit          not null,
    email              varchar(255) null,
    first_name         varchar(255) null,
    mobile             varchar(255) null,
    name               varchar(255) null,
    subscribable_id    int          null,
    user_id            int          null,
    cancel_token       varchar(36)  null,
    constraint FKdm8l0mdjqodrvq2k9wjifcgya
        foreign key (subscribable_id) references event (id),
    constraint FKsiqqwnfa1t1rybxsi424kfxh1
        foreign key (user_id) references user (id)
);

create table membership
(
    id                 int auto_increment
        primary key,
    created_by         varchar(255) null,
    created_date       datetime(6)  null,
    last_modified_by   varchar(255) null,
    last_modified_date datetime(6)  null,
    version            int          not null,
    paid               bit          not null,
    payment_id         varchar(255) null,
    price              double       not null,
    branch_id          int          null,
    period_id          int          null,
    user_id            int          null,
    constraint idx_payment_id
        unique (payment_id),
    constraint FKik5tek3qutsrw9pt9ejl1hlha
        foreign key (branch_id) references branch (id),
    constraint FKjp7ht675da9n751xycuwii77s
        foreign key (user_id) references user (id),
    constraint FKta8t5k2eetyxu13x6aiia6wub
        foreign key (period_id) references membership_period (id)
);

create table shop_order
(
    id                 int auto_increment
        primary key,
    created_by         varchar(255)                                        null,
    created_date       datetime(6)                                         null,
    last_modified_by   varchar(255)                                        null,
    last_modified_date datetime(6)                                         null,
    version            int                                                 not null,
    paid               bit                                                 not null,
    payment_id         varchar(255)                                        null,
    price              double                                              not null,
    delivery_details   varchar(255)                                        null,
    delivery_option    enum ('ARRANGED', 'DELIVERY', 'PICKUP', 'SHIPPING') null,
    email              varchar(255)                                        null,
    first_name         varchar(255)                                        null,
    mobile             varchar(255)                                        null,
    name               varchar(255)                                        null,
    remarks            varchar(255)                                        null,
    shop_id            int                                                 null,
    user_id            int                                                 null,
    constraint FK7ln2hb3a1ugyr7h92rloxv0b
        foreign key (user_id) references user (id),
    constraint FKchkd04p2elnxjvr2cojr9ygge
        foreign key (shop_id) references shop (id)
);

create table sibling_relation
(
    user_id     int not null,
    siblings_id int not null,
    constraint FK8dj91l8cwdn7la1bsqqnde2j9
        foreign key (user_id) references user (id),
    constraint FKfjcmlwaqyx63yx1pv3lhi9b4t
        foreign key (siblings_id) references user (id)
);

create table staff_data
(
    id                 int          not null
        primary key,
    created_by         varchar(255) null,
    created_date       datetime(6)  null,
    last_modified_by   varchar(255) null,
    last_modified_date datetime(6)  null,
    version            int          not null,
    about              varchar(255) null,
    totem              varchar(255) null,
    constraint FKm5l1cjjfq0i17xqy3m6n8v7hi
        foreign key (id) references user (id)
);

create table user_addresses
(
    user_id      int not null,
    addresses_id int not null,
    constraint UKi5lp1fvgfvsplfqwu4ovwpnxs
        unique (addresses_id),
    constraint FKfm6x520mag23hvgr1oshaut8b
        foreign key (user_id) references user (id),
    constraint FKth1icmttmhhorb9wiarm73i06
        foreign key (addresses_id) references address (id)
);

create table user_branch_nicknames
(
    user_id   int          not null,
    nickname  varchar(255) null,
    branch_id int          not null,
    primary key (user_id, branch_id),
    constraint FKf91h9quik66gttlecy9844cc3
        foreign key (branch_id) references branch (id),
    constraint FKrmqe55fbmd1k6v3v19fjd4992
        foreign key (user_id) references staff_data (id)
);

create table user_contacts
(
    user_id     int not null,
    contacts_id int not null,
    constraint UKbsi85mivvrqq06j3gfo6tdtn5
        unique (contacts_id),
    constraint FKm7a128umfw4itucrfssjdfcdv
        foreign key (contacts_id) references contact (id),
    constraint FKmo0c5ro6kunnfq71x4bcwb9eh
        foreign key (user_id) references user (id)
);

create table user_role
(
    created_by         varchar(255) null,
    created_date       datetime(6)  null,
    last_modified_by   varchar(255) null,
    last_modified_date datetime(6)  null,
    version            int          not null,
    end_date           date         null,
    start_date         date         null,
    user_id            int          not null,
    role_id            int          not null,
    primary key (role_id, user_id),
    constraint FK859n2jvi8ivhui0rl0esws6o
        foreign key (user_id) references user (id),
    constraint FKa68196081fvovjhkek5m97n3y
        foreign key (role_id) references role (id)
);

