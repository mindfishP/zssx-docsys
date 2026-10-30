-- =====================================================================
-- 初始化数据：预置账号 / 协会名录 / 六大类分类（SRS 1.4、表 5-6）
-- 口令均为 123456 的 BCrypt 哈希，仅用于开发联调
-- =====================================================================
USE association_archive;

-- 预置账号：主管部门操作员 / 主管领导（只读）/ 协会管理员
INSERT INTO sys_user (username, password_hash, real_name, role_code, assoc_code)
VALUES
 ('admin',    '$2a$10$Tx25MSO8SVNmaxU7G7LlReXOwENEdifnVsfHRf4jyUVohseJ1akDS', '主管部门操作员', 'ADMIN', NULL),
 ('leader',   '$2a$10$Tx25MSO8SVNmaxU7G7LlReXOwENEdifnVsfHRf4jyUVohseJ1akDS', '主管领导',       'LEADER', NULL),
 ('A001',     '$2a$10$Tx25MSO8SVNmaxU7G7LlReXOwENEdifnVsfHRf4jyUVohseJ1akDS', '古镇灯饰商会管理员', 'ASSOC', 'A001'),
 ('A002',     '$2a$10$Tx25MSO8SVNmaxU7G7LlReXOwENEdifnVsfHRf4jyUVohseJ1akDS', '小榄五金商会管理员', 'ASSOC', 'A002');

-- 协会名录样例
INSERT INTO biz_association (assoc_code, assoc_name, credit_code, industry_type, register_org, register_date, legal_person, activity_level, activity_score, status)
VALUES
 ('A001', '中山市古镇灯饰产业商会', '91442000MA0000001X', '灯饰', '中山市民政局', '2015-06-18', '张三', 1, 86.50, 1),
 ('A002', '中山市小榄五金行业协会', '91442000MA0000002X', '五金', '中山市民政局', '2012-03-22', '李四', 2, 72.00, 1),
 ('A003', '中山市家电行业协会',     '91442000MA0000003X', '家电', '中山市民政局', '2018-09-10', '王五', 3, 45.20, 1);

-- 六大类两级分类（SRS 表 5-6；中类示例各 2 个，可按需扩展）
INSERT INTO biz_category (category_code, major_code, major_name, sub_code, sub_name, default_retention, default_security_level, required_meta_keys, status)
VALUES
 ('A01', 'A', '登记类', '01', '成立登记材料',   '永久',     2, '["title","archiveYear","responsibility"]', 1),
 ('A02', 'A', '登记类', '02', '年检年报材料',   '定期30年', 2, '["title","archiveYear"]', 1),
 ('B01', 'B', '治理类', '01', '换届选举材料',   '永久',     2, '["title","archiveYear"]', 1),
 ('B03', 'B', '治理类', '03', '会议纪要与决议', '定期30年', 2, '["title","archiveYear","responsibility"]', 1),
 ('C01', 'C', '会员类', '01', '会员名册',       '定期30年', 2, '["title","archiveYear"]', 1),
 ('D01', 'D', '活动类', '01', '活动通知与总结', '定期10年', 1, '["title","archiveYear"]', 1),
 ('E01', 'E', '财务类', '01', '财务与审计报告', '定期30年', 2, '["title","archiveYear"]', 1),
 ('F01', 'F', '党建类', '01', '党组织活动材料', '定期30年', 2, '["title","archiveYear"]', 1);
