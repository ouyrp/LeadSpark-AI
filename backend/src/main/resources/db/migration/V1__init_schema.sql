CREATE TABLE IF NOT EXISTS tenant (
  id BIGINT PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  plan VARCHAR(32) NOT NULL DEFAULT 'FREE',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  settings JSON NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_account (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  mobile VARCHAR(128) NULL,
  email VARCHAR(128) NULL,
  password_hash VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  last_login_at DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_user_tenant_status (tenant_id, status),
  KEY idx_user_email (email),
  KEY idx_user_mobile (mobile)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS company (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  normalized_name VARCHAR(255) NOT NULL,
  credit_code VARCHAR(64) NULL,
  industry VARCHAR(128) NULL,
  region VARCHAR(128) NULL,
  scale VARCHAR(64) NULL,
  registered_capital VARCHAR(64) NULL,
  founded_at DATE NULL,
  website VARCHAR(255) NULL,
  description TEXT NULL,
  data_quality_score INT NOT NULL DEFAULT 0,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_company_tenant_name (tenant_id, normalized_name),
  KEY idx_company_tenant_industry_region (tenant_id, industry, region),
  KEY idx_company_credit_code (tenant_id, credit_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS contact (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  company_id BIGINT NOT NULL,
  name VARCHAR(64) NULL,
  title VARCHAR(128) NULL,
  department VARCHAR(128) NULL,
  mobile_encrypted VARCHAR(255) NULL,
  email_encrypted VARCHAR(255) NULL,
  wechat_encrypted VARCHAR(255) NULL,
  confidence INT NOT NULL DEFAULT 0,
  source VARCHAR(64) NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_contact_company (tenant_id, company_id),
  KEY idx_contact_title (tenant_id, title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sales_lead (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  company_id BIGINT NOT NULL,
  primary_contact_id BIGINT NULL,
  source VARCHAR(64) NOT NULL,
  source_ref VARCHAR(128) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'NEW',
  owner_user_id BIGINT NULL,
  score INT NOT NULL DEFAULT 0,
  grade VARCHAR(8) NULL,
  score_reason TEXT NULL,
  last_follow_up_at DATETIME NULL,
  next_follow_up_at DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_lead_tenant_status_owner (tenant_id, status, owner_user_id),
  KEY idx_lead_tenant_score (tenant_id, score, created_at),
  KEY idx_lead_company (tenant_id, company_id),
  KEY idx_lead_next_follow (tenant_id, next_follow_up_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS intent_signal (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  company_id BIGINT NOT NULL,
  signal_type VARCHAR(64) NOT NULL,
  signal_source VARCHAR(64) NOT NULL,
  content TEXT NOT NULL,
  signal_time DATETIME NULL,
  weight INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  KEY idx_signal_company (tenant_id, company_id),
  KEY idx_signal_type_time (tenant_id, signal_type, signal_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sales_task (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  lead_id BIGINT NOT NULL,
  company_id BIGINT NOT NULL,
  owner_user_id BIGINT NOT NULL,
  task_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  title VARCHAR(255) NOT NULL,
  due_at DATETIME NOT NULL,
  completed_at DATETIME NULL,
  result VARCHAR(64) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_task_owner_status_due (tenant_id, owner_user_id, status, due_at),
  KEY idx_task_lead (tenant_id, lead_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS follow_up_record (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  lead_id BIGINT NOT NULL,
  company_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  channel VARCHAR(32) NOT NULL,
  content TEXT NOT NULL,
  result VARCHAR(64) NOT NULL,
  next_action VARCHAR(128) NULL,
  next_follow_up_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  KEY idx_follow_lead (tenant_id, lead_id, created_at),
  KEY idx_follow_user_time (tenant_id, user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS opportunity (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  lead_id BIGINT NOT NULL,
  company_id BIGINT NOT NULL,
  owner_user_id BIGINT NOT NULL,
  stage VARCHAR(32) NOT NULL DEFAULT 'QUALIFIED',
  amount DECIMAL(18,2) NULL,
  probability INT NOT NULL DEFAULT 0,
  expected_close_date DATE NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  lost_reason VARCHAR(255) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_opp_owner_stage (tenant_id, owner_user_id, stage),
  KEY idx_opp_status_time (tenant_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_recommendation (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  target_type VARCHAR(32) NOT NULL,
  target_id BIGINT NOT NULL,
  recommendation_type VARCHAR(64) NOT NULL,
  content TEXT NOT NULL,
  model_name VARCHAR(64) NULL,
  prompt_version VARCHAR(64) NULL,
  confidence INT NULL,
  created_at DATETIME NOT NULL,
  KEY idx_ai_rec_target (tenant_id, target_type, target_id),
  KEY idx_ai_rec_type_time (tenant_id, recommendation_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS event_log (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NULL,
  event_type VARCHAR(64) NOT NULL,
  target_type VARCHAR(32) NOT NULL,
  target_id BIGINT NOT NULL,
  properties JSON NULL,
  created_at DATETIME NOT NULL,
  KEY idx_event_type_time (tenant_id, event_type, created_at),
  KEY idx_event_target (tenant_id, target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
