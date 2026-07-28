package com.homektv.web;

import com.homektv.queue.RoomHostService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/room/host")
public class RoomHostController {
    private final RoomHostService service;

    public RoomHostController(RoomHostService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Object> status(@RequestParam(required = false) String clientToken) {
        return service.status(clientToken);
    }

    @PostMapping("/claim")
    public Map<String, Object> claim(@RequestBody HostRequest request) {
        return service.claim(request.clientToken());
    }

    @PostMapping("/release")
    public Map<String, Object> release(@RequestBody HostRequest request) {
        return service.release(request.clientToken());
    }

    public record HostRequest(String clientToken) {}
}
