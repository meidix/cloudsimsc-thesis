package org.cloudbus.cloudsim.serverless;

import org.cloudbus.cloudsim.container.containerProvisioners.ContainerBwProvisioner;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerPe;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerRamProvisioner;
import org.cloudbus.cloudsim.container.schedulers.ContainerScheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ServerlessInvokerRequestHistory extends ServerlessInvoker {

    private HashMap<String, List<ServerlessRequest>> finishedRequests;

    public ServerlessInvokerRequestHistory(int id, int userId, double mips, float ram, long bw, long size, String vmm, ContainerScheduler containerScheduler, ContainerRamProvisioner containerRamProvisioner, ContainerBwProvisioner containerBwProvisioner, List<? extends ContainerPe> peList, double schedulingInterval) {
        super(id, userId, mips, ram, bw, size, vmm, containerScheduler, containerRamProvisioner, containerBwProvisioner, peList, schedulingInterval);
    }

    public List<ServerlessRequest> getFinishedRequests(String key) { return finishedRequests.get(key); }

    public List<Double> getFinishedRequestsResponseTimes(String key){
        List<ServerlessRequest> requests = finishedRequests.get(key);
        List<Double> responseTimes = new ArrayList<>();
        for (ServerlessRequest request: requests) {
            responseTimes.add(request.getFinishTime() - request.getArrivalTime());
        }
        return responseTimes;
    }

    protected HashMap<String, List<ServerlessRequest>> getFinishedRequestsMap() { return finishedRequests; }

    public void recordFinishedRequest(ServerlessRequest request) {
        String functionId = request.getRequestFunctionId();
        if (finishedRequests.containsKey(functionId)) {
            finishedRequests.get(functionId).add(request);
        } else {
            List<ServerlessRequest> requests = new ArrayList<>();
            requests.add(request);
            finishedRequests.put(functionId, requests);
        }
    }
}
