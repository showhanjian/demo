package agent.controller;

import agent.service.MessageConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class EventsController {

    @Autowired
    private MessageConsumer messageConsumer;

    @GetMapping("/pull")
    public Map<String, Object> pull(@RequestParam String sessionId) {
        var msg = messageConsumer.pull(sessionId);
        Map<String, Object> result = new HashMap<>();
        result.put("status", msg != null ? msg.status : "success");
        result.put("data", msg != null ? msg.data : null);
        return result;
    }
}
