CREATE TABLE IF NOT EXISTS import_task (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  task_type VARCHAR(64) NOT NULL,
  source_type VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  file_name VARCHAR(255) NULL,
  total_rows INT NOT NULL DEFAULT 0,
  success_rows INT NOT NULL DEFAULT 0,
  failed_rows INT NOT NULL DEFAULT 0,
  duplicate_rows INT NOT NULL DEFAULT 0,
  request_payload JSON NULL,
  error_message TEXT NULL,
  started_at DATETIME NULL,
  finished_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_import_task_tenant_time (tenant_id, created_at),
  KEY idx_import_task_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS import_task_error (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  row_index INT NOT NULL DEFAULT 0,
  source_ref VARCHAR(128) NULL,
  error_code VARCHAR(64) NOT NULL,
  error_message TEXT NOT NULL,
  raw_data JSON NULL,
  created_at DATETIME NOT NULL,
  KEY idx_import_error_task (tenant_id, task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
