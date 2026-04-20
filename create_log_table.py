import pymysql

conn = pymysql.connect(
    host='localhost',
    port=3306,
    user='root',
    password='1234',
    database='generic_sys_admin',
    charset='utf8mb4'
)

cursor = conn.cursor()

sql = '''
CREATE TABLE IF NOT EXISTS sys_operation_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  module VARCHAR(100) DEFAULT '',
  operation_type VARCHAR(50) DEFAULT '',
  description VARCHAR(500) DEFAULT '',
  request_method VARCHAR(10) DEFAULT '',
  request_url VARCHAR(500) DEFAULT '',
  request_params TEXT,
  status TINYINT DEFAULT 1,
  error_msg VARCHAR(1000) DEFAULT '',
  cost_time BIGINT DEFAULT 0,
  operator_id BIGINT DEFAULT NULL,
  operator_name VARCHAR(100) DEFAULT '',
  operator_ip VARCHAR(50) DEFAULT '',
  user_agent VARCHAR(500) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_operator_id(operator_id),
  INDEX idx_module(module),
  INDEX idx_operation_type(operation_type),
  INDEX idx_create_time(create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
'''

cursor.execute(sql)
conn.commit()
print("Table sys_operation_log created successfully")

cursor.close()
conn.close()