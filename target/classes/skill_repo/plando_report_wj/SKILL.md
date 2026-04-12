---
name: plando_report_wj
description: 根据用户需求查询报表并做检查，返回执行结果
---
# Planner Agent Skill

## 执行流程（3步）

### 步骤1: 生成报表
调用 Tool_get_Report 工具，检查报表文件是否存在

### 步骤2: 检查报表
调用 Tool_chk_Report 工具，核对报表中明细金额列的合计是否等于最后一行合计金额

### 步骤3: 展示报表
调用 Tool_dis_Report 工具，获取报表内容，Agent 层自动推送到前端展示

## 可用工具

| 工具名称 | 参数 | 说明 |
|---------|------|------|
| Tool_get_Report | 无 | 检查 full_report.csv 是否存在，返回存在性结果 |
| Tool_chk_Report | 无 | 核对明细金额列的合计是否等于最后一行合计金额 |
| Tool_dis_Report | 无 | 获取报表内容，返回 report_content 字段供 Agent 推送前端 |
| create_plan | name, description, expected_outcome, subtasks | 创建执行计划 |
| finish_subtask | subtask_idx, subtask_outcome | 标记子任务完成 |
| finish_plan | state, outcome | 结束计划执行 |

## 报表文件

- references/full_report.csv: 全行报表数据（唯一报表）
