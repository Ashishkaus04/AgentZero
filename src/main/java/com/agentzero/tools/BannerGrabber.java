package com.agentzero.tools;

import com.agentzero.model.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;

@Slf4j
@Component
public class BannerGrabber implements PentestTool {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getName() { return "banner_grab"; }

    @Override
    public String getDescription() {
        return "Connects to a port and reads the service banner to identify software and version.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"host\": \"Target IP\", \"port\": \"Port number\"}";
    }

    @Override
    public ToolResult execute(String paramsJson) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> params = mapper.readValue(paramsJson, Map.class);
            String host = (String) params.get("host");
            int port = Integer.parseInt(params.get("port").toString());
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 3000);
                socket.setSoTimeout(3000);
                socket.getOutputStream().write("HEAD / HTTP/1.0\r\n\r\n".getBytes());
                StringBuilder banner = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;
                int lines = 0;
                while ((line = reader.readLine()) != null && lines < 20) {
                    banner.append(line).append("\n");
                    lines++;
                }
                return ToolResult.builder().toolName(getName()).success(true)
                        .output("Banner from " + host + ":" + port + "\n\n" + banner)
                        .executionTimeMs(System.currentTimeMillis() - start).build();
            }
        } catch (Exception e) {
            return ToolResult.builder().toolName(getName()).success(false)
                    .error("Banner grab failed: " + e.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - start).build();
        }
    }
}
