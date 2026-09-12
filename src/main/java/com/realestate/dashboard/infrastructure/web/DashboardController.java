package com.realestate.dashboard.infrastructure.web;

import com.realestate.dashboard.application.DashboardService;
import com.realestate.shared.infrastructure.web.Responses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/data")
    ResponseEntity<?> data() {
        return Responses.ok(dashboard.data(), "Dashboard fetched successfully");
    }
}
