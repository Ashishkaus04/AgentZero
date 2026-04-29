package com.agentzero.api;

import com.agentzero.agent.AgentSessionManager;
import com.agentzero.report.PentestReportGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportController {

    private final PentestReportGenerator reportGenerator;
    private final AgentSessionManager sessionManager;

    @GetMapping("/{sessionId}")
    public ResponseEntity<byte[]> downloadReport(@PathVariable String sessionId) {
        return sessionManager.getSession(sessionId).map(session -> {
            try {
                log.info("Generating report for session: {}", sessionId);
                byte[] pdf = reportGenerator.generateReport(session);

                String filename = "agentzero-report-" + sessionId.substring(0, 8) + ".pdf";

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                        .body(pdf);

            } catch (Exception e) {
                log.error("Failed to generate report for session {}: {}", sessionId, e.getMessage(), e);
                return ResponseEntity.internalServerError().<byte[]>build();
            }
        }).orElse(ResponseEntity.notFound().<byte[]>build());
    }
}