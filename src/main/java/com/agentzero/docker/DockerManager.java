package com.agentzero.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class DockerManager {

    private final DockerClient dockerClient;
    private final Map<String, String> runningContainers = new HashMap<>();

    public DockerManager(@Value("${agentzero.docker.host}") String dockerHost) {
        DefaultDockerClientConfig config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .build();

        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(URI.create(dockerHost))
                .maxConnections(10)
                .connectionTimeout(Duration.ofSeconds(10))
                .responseTimeout(Duration.ofSeconds(30))
                .build();

        this.dockerClient = DockerClientBuilder.getInstance(config)
                .withDockerHttpClient(httpClient)
                .build();

        log.info("DockerManager initialized: {}", dockerHost);
    }

    public String startTarget(String targetId, String dockerImage, int exposedPort) {
        try {
            log.info("Starting container: {} ({})", targetId, dockerImage);
            try {
                dockerClient.pullImageCmd(dockerImage).start().awaitCompletion();
            } catch (Exception e) {
                log.warn("Could not pull image {}: {}", dockerImage, e.getMessage());
            }

            CreateContainerResponse container = dockerClient.createContainerCmd(dockerImage)
                    .withName("agentzero-" + targetId + "-" + System.currentTimeMillis())
                    .withExposedPorts(ExposedPort.tcp(exposedPort))
                    .withHostConfig(HostConfig.newHostConfig()
                            .withNetworkMode("bridge")
                            .withPortBindings(new Ports(
                                    ExposedPort.tcp(exposedPort),
                                    Ports.Binding.bindPort(exposedPort))))
                    .exec();

            String containerId = container.getId();
            dockerClient.startContainerCmd(containerId).exec();

            String ip = dockerClient.inspectContainerCmd(containerId).exec()
                    .getNetworkSettings().getNetworks().values()
                    .stream().findFirst()
                    .map(ContainerNetwork::getIpAddress)
                    .orElse("127.0.0.1");

            runningContainers.put(targetId, containerId);
            log.info("Container started: {} at {}", containerId, ip);
            return ip;

        } catch (Exception e) {
            log.error("Failed to start container {}: {}", targetId, e.getMessage());
            throw new RuntimeException("Docker start failed: " + e.getMessage(), e);
        }
    }

    public void stopTarget(String targetId) {
        String containerId = runningContainers.get(targetId);
        if (containerId != null) {
            try {
                dockerClient.stopContainerCmd(containerId).exec();
                dockerClient.removeContainerCmd(containerId).exec();
                runningContainers.remove(targetId);
                log.info("Container stopped: {}", targetId);
            } catch (Exception e) {
                log.error("Failed to stop container {}: {}", targetId, e.getMessage());
            }
        }
    }

    public boolean isDockerAvailable() {
        try {
            dockerClient.pingCmd().exec();
            return true;
        } catch (Exception e) {
            log.warn("Docker not available: {}", e.getMessage());
            return false;
        }
    }
}
