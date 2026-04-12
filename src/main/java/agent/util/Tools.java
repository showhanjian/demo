package agent.util;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.Toolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * 工具注册器与报表工具
 */
public class Tools {

    private static final Logger logger = LoggerFactory.getLogger(Tools.class);
    private static final String REPORTS_BASE_PATH = "src/main/resources/skill_repo/plando_report_wj/references";

    /**
     * 注册所有业务工具到 Toolkit
     */
    public static void registerTools(Toolkit toolkit) {
        toolkit.registerTool(new ReportTool());
        logger.info("[Tools] 工具注册成功: ReportTool");
    }

    // ========== 报表工具 ==========

    public static class ReportTool {

        @Tool(name = "Tool_get_Report", description = "检查 full_report.csv 是否存在，返回存在性结果")
        public Mono<ToolResultBlock> Tool_get_Report() {
            return Mono.fromCallable(() -> {
                Path filePath = Paths.get(REPORTS_BASE_PATH, "full_report.csv");
                boolean exists = Files.exists(filePath);
                if (exists) {
                    return ToolResultBlock.text("{\"exists\": true, \"message\": \"报表文件存在\"}");
                } else {
                    return ToolResultBlock.text("{\"exists\": false, \"message\": \"报表文件不存在: " + filePath.toAbsolutePath() + "\"}");
                }
            }).onErrorResume(e -> Mono.just(ToolResultBlock.error("检查报表失败: " + e.getMessage())));
        }

        @Tool(name = "Tool_chk_Report", description = "核对报表中明细金额列的合计是否等于最后一行合计金额")
        public Mono<ToolResultBlock> Tool_chk_Report() {
            return Mono.fromCallable(() -> {
                Path filePath = Paths.get(REPORTS_BASE_PATH, "full_report.csv");
                if (!Files.exists(filePath)) {
                    return ToolResultBlock.error("报表文件不存在: " + filePath.toAbsolutePath());
                }

                List<String> lines = Files.readAllLines(filePath);
                if (lines.isEmpty()) {
                    return ToolResultBlock.error("报表文件为空");
                }

                String[] headers = lines.get(0).split(",");
                int detailIndex = -1;
                int totalIndex = -1;
                for (int i = 0; i < headers.length; i++) {
                    String h = headers[i].trim();
                    if (h.contains("明细金额")) {
                        detailIndex = i;
                    }
                    if (h.contains("合计金额")) {
                        totalIndex = i;
                    }
                }

                if (detailIndex == -1 || totalIndex == -1) {
                    return ToolResultBlock.error("报表缺少必要的列，需要包含'明细金额'和'合计金额'列");
                }

                double detailSum = 0;
                for (int i = 1; i < lines.size(); i++) {
                    String[] cols = lines.get(i).split(",");
                    if (cols.length > detailIndex) {
                        try {
                            detailSum += Double.parseDouble(cols[detailIndex].trim());
                        } catch (NumberFormatException ignored) {}
                    }
                }

                String[] lastRow = lines.get(lines.size() - 1).split(",");
                double totalAmount = 0;
                if (lastRow.length > totalIndex) {
                    try {
                        totalAmount = Double.parseDouble(lastRow[totalIndex].trim());
                    } catch (NumberFormatException e) {
                        return ToolResultBlock.error("合计金额格式错误: " + lastRow[totalIndex]);
                    }
                }

                double diff = Math.abs(detailSum - totalAmount);
                String result;
                if (diff < 0.01) {
                    result = String.format("核对通过: 明细合计=%.2f, 报表合计=%.2f, 差额=%.2f", detailSum, totalAmount, diff);
                } else {
                    result = String.format("核对失败: 明细合计=%.2f, 报表合计=%.2f, 差额=%.2f", detailSum, totalAmount, diff);
                }

                return ToolResultBlock.text(result);
            }).onErrorResume(e -> Mono.just(ToolResultBlock.error("核对失败: " + e.getMessage())));
        }

        @Tool(name = "Tool_dis_Report", description = "获取报表内容，返回给 Agent 推送到前端展示")
        public Mono<ToolResultBlock> Tool_dis_Report() {
            return Mono.fromCallable(() -> {
                Path filePath = Paths.get(REPORTS_BASE_PATH, "full_report.csv");
                if (!Files.exists(filePath)) {
                    return ToolResultBlock.error("报表文件不存在: " + filePath.toAbsolutePath());
                }

                List<String> lines = Files.readAllLines(filePath);
                StringBuilder content = new StringBuilder();
                for (int i = 0; i < lines.size(); i++) {
                    content.append(lines.get(i)).append("\n");
                }

                // 返回格式供 Agent 解析 report_content 字段
                return ToolResultBlock.text("{\"report_content\": \"" + content.toString().replace("\"", "\\\"") + "\"}");
            }).onErrorResume(e -> Mono.just(ToolResultBlock.error("获取报表内容失败: " + e.getMessage())));
        }
    }
}
