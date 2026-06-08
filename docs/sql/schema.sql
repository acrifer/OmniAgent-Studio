CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  email VARCHAR(100),
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME,
  updated_at DATETIME,
  INDEX idx_username (username)
);

CREATE TABLE projects (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  archived_at DATETIME,
  created_at DATETIME,
  updated_at DATETIME,
  INDEX idx_user_status (user_id, status)
);

CREATE TABLE project_requirements (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  requirement_text LONGTEXT NOT NULL,
  source_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
  version INT NOT NULL DEFAULT 1,
  created_at DATETIME,
  INDEX idx_project_version (project_id, version)
);

CREATE TABLE requirement_documents (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  file_name VARCHAR(255),
  file_path VARCHAR(500),
  file_type VARCHAR(50),
  file_size BIGINT,
  parse_status VARCHAR(20),
  error_message TEXT,
  parsed_text LONGTEXT,
  created_at DATETIME,
  INDEX idx_project_doc (project_id)
);

CREATE TABLE agent_tasks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  task_type VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL,
  progress INT DEFAULT 0,
  target_agent VARCHAR(30),
  fastapi_task_id VARCHAR(100),
  langfuse_trace_id VARCHAR(100),
  error_message TEXT,
  started_at DATETIME,
  finished_at DATETIME,
  created_at DATETIME,
  updated_at DATETIME,
  INDEX idx_project_task (project_id),
  INDEX idx_status (status)
);

CREATE TABLE agent_results (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  agent_type VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL,
  content_json LONGTEXT,
  content_markdown LONGTEXT,
  quality_score DECIMAL(3,2),
  prompt_version VARCHAR(50),
  model_name VARCHAR(100),
  trace_observation_id VARCHAR(100),
  error_message TEXT,
  created_at DATETIME,
  INDEX idx_task_agent (task_id, agent_type)
);

CREATE TABLE final_reports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  title VARCHAR(200),
  content_markdown LONGTEXT,
  content_json LONGTEXT,
  version INT DEFAULT 1,
  created_at DATETIME,
  INDEX idx_project_version (project_id, version)
);

CREATE TABLE prompt_templates (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  agent_type VARCHAR(30) NOT NULL,
  name VARCHAR(100) NOT NULL,
  content LONGTEXT NOT NULL,
  version VARCHAR(50) NOT NULL,
  is_active TINYINT DEFAULT 1,
  model_name VARCHAR(100),
  temperature DECIMAL(3,2),
  created_by BIGINT,
  created_at DATETIME,
  INDEX idx_agent_active (agent_type, is_active)
);

CREATE TABLE token_usage_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT,
  project_id BIGINT,
  run_id BIGINT,
  conversation_id BIGINT,
  agent_type VARCHAR(30),
  model_name VARCHAR(100),
  prompt_tokens INT,
  completion_tokens INT,
  total_tokens INT,
  cost DECIMAL(10,4),
  latency_ms INT,
  created_at DATETIME,
  INDEX idx_project_agent (project_id, agent_type),
  INDEX idx_task (task_id),
  INDEX idx_conversation_agent (conversation_id, agent_type),
  INDEX idx_run (run_id)
);

CREATE TABLE user_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  project_id BIGINT,
  task_id BIGINT,
  target_type VARCHAR(30),
  target_id BIGINT,
  rating INT,
  comment VARCHAR(500),
  langfuse_score_id VARCHAR(100),
  created_at DATETIME,
  INDEX idx_project_feedback (project_id)
);

CREATE TABLE conversations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(160) NOT NULL,
  mode VARCHAR(30) DEFAULT 'AUTO',
  knowledge_base_id BIGINT,
  model_name VARCHAR(100) DEFAULT 'deepseek-v4-flash',
  created_at DATETIME,
  updated_at DATETIME,
  INDEX idx_conversation_user (user_id, updated_at)
);

CREATE TABLE messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  user_id BIGINT,
  role VARCHAR(20) NOT NULL,
  content LONGTEXT,
  metadata_json LONGTEXT,
  created_at DATETIME,
  INDEX idx_message_conversation (conversation_id, created_at)
);

CREATE TABLE uploaded_files (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  conversation_id BIGINT,
  knowledge_base_id BIGINT,
  file_name VARCHAR(255),
  file_path VARCHAR(500),
  file_type VARCHAR(100),
  file_size BIGINT,
  parse_status VARCHAR(20),
  parsed_text LONGTEXT,
  error_message LONGTEXT,
  created_at DATETIME,
  INDEX idx_uploaded_file_conversation (conversation_id),
  INDEX idx_uploaded_file_kb (knowledge_base_id)
);

CREATE TABLE knowledge_bases (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  description TEXT,
  vector_status VARCHAR(20) DEFAULT 'PENDING',
  created_at DATETIME,
  updated_at DATETIME,
  INDEX idx_kb_user (user_id, updated_at)
);

CREATE TABLE knowledge_documents (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  knowledge_base_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  uploaded_file_id BIGINT,
  file_name VARCHAR(255),
  ingest_status VARCHAR(20) DEFAULT 'PENDING',
  parsed_text_preview LONGTEXT,
  error_message LONGTEXT,
  created_at DATETIME,
  INDEX idx_kb_document (knowledge_base_id)
);

CREATE TABLE agent_runs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  message_id BIGINT,
  knowledge_base_id BIGINT,
  mode VARCHAR(30) DEFAULT 'AUTO',
  status VARCHAR(20) DEFAULT 'PENDING',
  progress INT DEFAULT 0,
  model_name VARCHAR(100),
  ai_task_id VARCHAR(100),
  langfuse_trace_id VARCHAR(100),
  question LONGTEXT,
  answer_markdown LONGTEXT,
  citations_json LONGTEXT,
  used_tools_json LONGTEXT,
  confidence DECIMAL(5,2),
  follow_up_questions_json LONGTEXT,
  error_message LONGTEXT,
  started_at DATETIME,
  finished_at DATETIME,
  created_at DATETIME,
  updated_at DATETIME,
  INDEX idx_agent_run_conversation (conversation_id),
  INDEX idx_agent_run_status (status)
);

CREATE TABLE agent_steps (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  run_id BIGINT NOT NULL,
  conversation_id BIGINT NOT NULL,
  agent_type VARCHAR(40) NOT NULL,
  status VARCHAR(20) NOT NULL,
  input_json LONGTEXT,
  output_json LONGTEXT,
  content_markdown LONGTEXT,
  citations_json LONGTEXT,
  model_name VARCHAR(100),
  prompt_version VARCHAR(50),
  trace_observation_id VARCHAR(100),
  prompt_tokens INT,
  completion_tokens INT,
  latency_ms INT,
  error_message LONGTEXT,
  created_at DATETIME,
  INDEX idx_agent_step_run (run_id, created_at)
);

CREATE TABLE model_providers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  provider VARCHAR(40),
  model_name VARCHAR(100),
  base_url VARCHAR(255),
  enabled TINYINT DEFAULT 1,
  config_json LONGTEXT,
  created_at DATETIME,
  updated_at DATETIME,
  INDEX idx_model_provider_user (user_id, updated_at)
);

CREATE TABLE tool_configs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  tool_type VARCHAR(40),
  name VARCHAR(120),
  endpoint VARCHAR(500),
  enabled TINYINT DEFAULT 1,
  config_json LONGTEXT,
  created_at DATETIME,
  updated_at DATETIME,
  INDEX idx_tool_config_user (user_id, updated_at)
);

CREATE TABLE skill_configs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(120),
  description TEXT,
  enabled TINYINT DEFAULT 1,
  config_json LONGTEXT,
  created_at DATETIME,
  updated_at DATETIME,
  INDEX idx_skill_config_user (user_id, updated_at)
);
