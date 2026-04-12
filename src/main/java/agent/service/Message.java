package agent.service;

/**
 * 消息结构
 */
public class Message {
    public String sessionId;    // 会话ID
    public long sequence;       // 序号，后端自增生成
    public String status;        // 会话状态：processing/completed
    public int consumed;        // 消费情况：0=已生产, 1=已消费
    public Object data;        // 消息内容
    public long timestamp;      // 时间戳

    public Message() {
        this.timestamp = System.currentTimeMillis();
        this.consumed = 0;
    }
}
