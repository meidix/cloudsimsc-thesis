package org.cloudbus.cloudsim.serverless;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerDatacenterCharacteristics;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.core.ContainerVm;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerVmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSimTags;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

public class ServerlessDatacenterRequestHistory extends ServerlessDatacenter {


    /**
     * Allocates a new PowerDatacenter object.
     *
     * @param name
     * @param characteristics
     * @param vmAllocationPolicy
     * @param containerAllocationPolicy
     * @param storageList
     * @param schedulingInterval
     * @param experimentName
     * @param logAddress
     * @param vmStartupDelay
     * @param containerStartupDelay
     * @param monitor
     * @throws Exception
     */
    public ServerlessDatacenterRequestHistory(String name, ContainerDatacenterCharacteristics characteristics, ContainerVmAllocationPolicy vmAllocationPolicy, FunctionScheduler containerAllocationPolicy, List<Storage> storageList, double schedulingInterval, String experimentName, String logAddress, double vmStartupDelay, double containerStartupDelay, boolean monitor) throws Exception {
        super(name, characteristics, vmAllocationPolicy, containerAllocationPolicy, storageList, schedulingInterval, experimentName, logAddress, vmStartupDelay, containerStartupDelay, monitor);
    }


    protected void checkCloudletCompletion() {
        List<? extends ContainerHost> list = getVmAllocationPolicy().getContainerHostList();
        for (int i = 0; i < list.size(); i++) {
            ContainerHost host = list.get(i);
            for (ContainerVm vm : host.getVmList()) {
                for (Container container : vm.getContainerList()) {
                    while (container.getContainerCloudletScheduler().isFinishedCloudlets()) {
                        Cloudlet cl = container.getContainerCloudletScheduler().getNextFinishedCloudlet();
                        if (cl != null) {
                            Map.Entry<Cloudlet, ContainerVm> data =  new AbstractMap.SimpleEntry<>(cl, vm);
//                            Pair data = new Pair<>(cl, vm);
                            for(int x=0; x<((ServerlessInvokerRequestHistory)vm).getRunningRequestList().size();x++){
                                if(((ServerlessInvokerRequestHistory)vm).getRunningRequestList().get(x)==(ServerlessRequest)cl){
                                    ((ServerlessInvokerRequestHistory)vm).getRunningRequestList().remove(x);
                                }
                            }
                            ((ServerlessInvokerRequestHistory)vm).recordFinishedRequest((ServerlessRequest)cl);
                            sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, data);
//                            removeFromVmTaskMap((ServerlessRequest)cl,(ServerlessInvoker)vm);
                        }
                    }
                }
            }
        }
    }

}
