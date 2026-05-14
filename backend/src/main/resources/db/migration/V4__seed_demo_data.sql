INSERT IGNORE INTO tenant (id, name, plan, status, settings, created_at, updated_at)
VALUES (1, 'LeadSpark Demo', 'FREE', 'ACTIVE', JSON_OBJECT('demo', true), NOW(), NOW());

INSERT IGNORE INTO user_account
(id, tenant_id, name, mobile, email, password_hash, status, deleted, created_at, updated_at)
VALUES
(1, 1, '销售一号', NULL, 'sales@leadspark.local', '{noop}demo', 'ACTIVE', 0, NOW(), NOW());

INSERT IGNORE INTO company
(id, tenant_id, name, normalized_name, industry, region, scale, website, description,
 data_quality_score, deleted, created_at, updated_at)
VALUES
(101, 1, '华东智造科技有限公司', '华东智造科技有限公司', '智能制造', '上海', '200-500人',
 'https://example.com/huadong', '正在建设面向经销商和大客户销售的数字化增长体系。', 86, 0, NOW(), NOW()),
(102, 1, '云启软件服务有限公司', '云启软件服务有限公司', '企业服务 / SaaS', '杭州', '100-200人',
 'https://example.com/yunqi', '面向企业客户提供流程数字化服务，近期加强渠道合作。', 82, 0, NOW(), NOW()),
(103, 1, '远航供应链管理有限公司', '远航供应链管理有限公司', '物流供应链', '苏州', '500-1000人',
 'https://example.com/yuanhang', '发布供应链数字化升级新闻，具备销售效率提升诉求。', 76, 0, NOW(), NOW());

INSERT IGNORE INTO contact
(id, tenant_id, company_id, name, title, department, confidence, source, deleted, created_at, updated_at)
VALUES
(201, 1, 101, '王经理', '销售运营负责人', '销售部', 78, 'DEMO', 0, NOW(), NOW()),
(202, 1, 102, '陈总', '渠道负责人', '市场部', 74, 'DEMO', 0, NOW(), NOW()),
(203, 1, 103, '李经理', '数字化项目负责人', '运营部', 69, 'DEMO', 0, NOW(), NOW());

INSERT IGNORE INTO sales_lead
(id, tenant_id, company_id, primary_contact_id, source, source_ref, status, owner_user_id,
 score, grade, score_reason, last_follow_up_at, next_follow_up_at, deleted, created_at, updated_at)
VALUES
(301, 1, 101, 201, 'PUBLIC_SIGNAL', 'seed-101', 'NEW', 1, 91, 'S',
 '招聘销售运营负责人，且行业与区域高度匹配 ICP。', NULL, DATE_ADD(NOW(), INTERVAL 2 HOUR), 0, NOW(), NOW()),
(302, 1, 102, 202, 'PUBLIC_SIGNAL', 'seed-102', 'FOLLOWING', 1, 86, 'A',
 '官网新增渠道合作页面，存在拓客和销售协同需求。', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY), 0, NOW(), NOW()),
(303, 1, 103, 203, 'NEWS', 'seed-103', 'NEW', 1, 78, 'B',
 '发布数字化升级新闻，可切入销售流程效率场景。', NULL, DATE_ADD(NOW(), INTERVAL 2 DAY), 0, NOW(), NOW());

INSERT IGNORE INTO intent_signal
(id, tenant_id, company_id, signal_type, signal_source, content, signal_time, weight, created_at)
VALUES
(401, 1, 101, 'RECRUITMENT', 'DEMO', '近期招聘销售运营负责人', DATE_SUB(NOW(), INTERVAL 2 DAY), 88, NOW()),
(402, 1, 102, 'WEBSITE', 'DEMO', '官网新增渠道合作页面', DATE_SUB(NOW(), INTERVAL 3 DAY), 82, NOW()),
(403, 1, 103, 'NEWS', 'DEMO', '发布数字化升级新闻', DATE_SUB(NOW(), INTERVAL 5 DAY), 70, NOW());

INSERT IGNORE INTO sales_task
(id, tenant_id, lead_id, company_id, owner_user_id, task_type, status, title, due_at,
 completed_at, result, created_at, updated_at)
VALUES
(501, 1, 301, 101, 1, 'CALL', 'PENDING', '电话触达 S 级制造业线索', DATE_ADD(NOW(), INTERVAL 2 HOUR), NULL, NULL, NOW(), NOW()),
(502, 1, 302, 102, 1, 'WECHAT', 'PENDING', '发送渠道合作场景资料', DATE_ADD(NOW(), INTERVAL 1 DAY), NULL, NULL, NOW(), NOW()),
(503, 1, 303, 103, 1, 'EMAIL', 'PENDING', '邮件切入数字化升级话题', DATE_ADD(NOW(), INTERVAL 2 DAY), NULL, NULL, NOW(), NOW());

INSERT IGNORE INTO follow_up_record
(id, tenant_id, lead_id, company_id, user_id, channel, content, result, next_action,
 next_follow_up_at, created_at)
VALUES
(601, 1, 302, 102, 1, 'CALL', '客户关注渠道线索质量，希望先看案例。', 'INTERESTED',
 '发送案例并预约演示', DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT IGNORE INTO opportunity
(id, tenant_id, lead_id, company_id, owner_user_id, stage, amount, probability,
 expected_close_date, status, lost_reason, created_at, updated_at)
VALUES
(701, 1, 302, 102, 1, 'QUALIFIED', 68000.00, 60, DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY),
 'OPEN', NULL, NOW(), NOW());

INSERT IGNORE INTO ai_recommendation
(id, tenant_id, target_type, target_id, recommendation_type, content, model_name,
 prompt_version, confidence, created_at)
VALUES
(801, 1, 'LEAD', 301, 'NEXT_BEST_ACTION', '优先电话触达，开场围绕销售运营招聘和线索转化效率。', 'mock-ai', 'v1', 86, NOW()),
(802, 1, 'LEAD', 302, 'PITCH', '从渠道合作线索质量切入，强调线索识别、评分和跟进闭环。', 'mock-ai', 'v1', 82, NOW());
