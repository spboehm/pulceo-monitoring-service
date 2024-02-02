package dev.pulceo.pms.util;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.cloud.contract.wiremock.WireMockSpring;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class SimulatedPulceoNodeAgent {

    private final int id;
    private final String hostName;
    private final WireMockServer wireMockServer;
    private static final List<SimulatedPulceoNodeAgent> list = new ArrayList<>();

    public static List<SimulatedPulceoNodeAgent> createAgents(int number) {
        for (int i = 1; i< number + 1; i++) {
            SimulatedPulceoNodeAgent simulatedPulceoNodeAgent = new SimulatedPulceoNodeAgent(i, "127.0.0." + i);
            list.add(simulatedPulceoNodeAgent);
            System.out.println("Create simulated pulceo-node-agent " + simulatedPulceoNodeAgent.id + " on " + simulatedPulceoNodeAgent.hostName);
            simulatedPulceoNodeAgent.start();
        }
        return list;
    }

    public static void resetAgents() {
        for (int i = 0; i < list.size(); i++) {
            SimulatedPulceoNodeAgent simulatedPulceoNodeAgent = list.get(i);
            simulatedPulceoNodeAgent.reset();
        }
    }

    public static void stopAgents() {
        for (int i = 0; i < list.size(); i++) {
            SimulatedPulceoNodeAgent simulatedPulceoNodeAgent = list.get(i);
            simulatedPulceoNodeAgent.stop();
            System.out.println("Stopped simulated pulceo-node-agent " + (i+1));
        }
    }

    private SimulatedPulceoNodeAgent(int id, String hostName) {
        this.id = id;
        this.hostName = hostName;
        this.wireMockServer = new WireMockServer(WireMockSpring.options().bindAddress(hostName).port(7676));;
    }

    public void run() {


    }

    private void start() {
        this.wireMockServer.start();
        this.run();
    }

    private void reset() {
        this.wireMockServer.resetAll();
    }

    private void stop() {
        this.wireMockServer.stop();
    }

}
