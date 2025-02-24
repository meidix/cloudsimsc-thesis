package org.cloudbus.cloudsim.experiments.ensure;

import org.cloudbus.cloudsim.container.containerProvisioners.ContainerBwProvisioner;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerPe;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerRamProvisioner;
import org.cloudbus.cloudsim.container.schedulers.ContainerScheduler;
import org.cloudbus.cloudsim.serverless.ServerlessInvokerRequestHistory;
import org.cloudbus.cloudsim.serverless.ServerlessRequest;

import java.util.List;

public class EnsureServerlessInvoker  extends ServerlessInvokerRequestHistory {

    public EnsureServerlessInvoker(int id, int userId, double mips, float ram, long bw, long size, String vmm, ContainerScheduler containerScheduler, ContainerRamProvisioner containerRamProvisioner, ContainerBwProvisioner containerBwProvisioner, List<? extends ContainerPe> peList, double schedulingInterval) {
        super(id, userId, mips, ram, bw, size, vmm, containerScheduler, containerRamProvisioner, containerBwProvisioner, peList, schedulingInterval);
    }

    protected float getAverageResponseTimes(List<ServerlessRequest> requests){
        double responseTimeSum = 0;
        for (ServerlessRequest request: requests) {
            responseTimeSum += request.getFinishTime() - request.getArrivalTime();
        }
        return (float) responseTimeSum / requests.size();
    }

}
