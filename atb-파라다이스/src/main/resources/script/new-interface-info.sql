-- public.tb_interface_detail definition
CREATE TABLE public.tb_interface_detail (
	interface_id varchar(30) NOT NULL,
	property_name varchar(100) NOT NULL,
	property_value varchar(1000) NULL,
	created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	created_by varchar(50) NULL,
	updated_by varchar(50) NULL,
	CONSTRAINT tb_interface_detail_pkey PRIMARY KEY (interface_id, property_name)
);

-- public.tb_interface_detail foreign keys
ALTER TABLE public.tb_interface_detail ADD CONSTRAINT tb_interface_detail_interface_id_fkey FOREIGN KEY (interface_id) REFERENCES public.tb_interface_info(interface_id) ON DELETE CASCADE;


-- public.tb_interface_info definition
CREATE TABLE public.tb_interface_info (
	interface_id varchar(30) NOT NULL,
	cron_expression varchar(30) NULL,
	send_system_code bpchar(3) NOT NULL,
	recv_system_code bpchar(3) NOT NULL,
	mapping_yn bpchar(1) DEFAULT 'N'::bpchar NOT NULL,
	use_yn bpchar(1) DEFAULT 'Y'::bpchar NOT NULL,
	created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	created_by varchar(50) NULL,
	updated_by varchar(50) NULL,
	pattern_code bpchar(3) NULL,
	CONSTRAINT tb_interface_info_mapping_yn_check CHECK ((mapping_yn = ANY (ARRAY['Y'::bpchar, 'N'::bpchar]))),
	CONSTRAINT tb_interface_info_pkey PRIMARY KEY (interface_id),
	CONSTRAINT tb_interface_info_use_yn_check CHECK ((use_yn = ANY (ARRAY['Y'::bpchar, 'N'::bpchar])))
);

-- public.tb_interface_info foreign keys
ALTER TABLE public.tb_interface_info ADD CONSTRAINT fk_tb_interface_pattern FOREIGN KEY (pattern_code) REFERENCES public.tb_pattern_info(pattern_code);

-- public.tb_interface_sql definition
CREATE TABLE public.tb_interface_sql (
	sql_id serial4 NOT NULL,
	interface_id varchar(30) NOT NULL,
	step_order int4 DEFAULT 1 NOT NULL,
	sql_type varchar(10) NOT NULL,
	query_text text NOT NULL,
	description varchar(200) NULL,
	CONSTRAINT tb_interface_sql_pkey PRIMARY KEY (sql_id),
	CONSTRAINT uk_interface_step UNIQUE (interface_id, step_order)
);

-- public.tb_interface_sql foreign keys
ALTER TABLE public.tb_interface_sql ADD CONSTRAINT fk_sql_interface_id FOREIGN KEY (interface_id) REFERENCES public.tb_interface_info(interface_id) ON DELETE CASCADE;

-- public.tb_pattern_info definition
CREATE TABLE public.tb_pattern_info (
	pattern_code bpchar(3) NOT NULL,
	pattern_name varchar(20) NOT NULL,
	pattern_type bpchar(1) NOT NULL,
	description varchar(100) NULL,
	created_at timestamp DEFAULT now() NULL,
	CONSTRAINT chk_pattern_type CHECK ((pattern_type = ANY (ARRAY['R'::bpchar, 'B'::bpchar]))),
	CONSTRAINT pattern_info_pkey PRIMARY KEY (pattern_code)
);