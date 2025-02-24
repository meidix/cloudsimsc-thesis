package org.cloudbus.cloudsim.experiments.ensure;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmBwProvisionerSimple;
import org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmPe;
import org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmPeProvisionerSimple;
import org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmRamProvisionerSimple;
import org.cloudbus.cloudsim.container.core.ContainerHost;
import org.cloudbus.cloudsim.container.core.PowerContainerHostUtilizationHistory;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerVmAllocationPolicy;
import org.cloudbus.cloudsim.container.resourceAllocators.PowerContainerVmAllocationSimple;
import org.cloudbus.cloudsim.container.schedulers.ContainerVmSchedulerTimeSharedOverSubscription;
import org.cloudbus.cloudsim.container.utils.IDs;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.serverless.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class EnsureSimulator {

    private static String csvResultFilePath;

    private static List<ServerlessInvoker> vmList;
    private static int controllerId;

    private static RequestLoadBalancer loadBalancer;

    private static ServerlessDatacenter datacenter;

    private static ServerlessController controller;

    public static void main(String[] args) {

        Log.printLine("Starting Ensure Simulator");

        int numUsers = 1;
        Calendar calendar = Calendar.getInstance();
        boolean trace_flag = false; // mean trace events
        csvResultFilePath = "Experiments/Simulation/";

        // Initialize the CloudSim library
        CloudSim.init(numUsers, calendar, trace_flag);

//        controller = new ServerlessController("Broker", 80);
        controller = createController();
        controllerId = controller.getId();

        List<ContainerHost> hostList = createHostList(Constants.NUMBER_HOSTS);

        datacenter = createDatacenter(hostList);

    }

    private static ServerlessController createController() {
        try {
            return new ServerlessController("Broker", 80);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    private static ServerlessDatacenter createDatacenter(List<ContainerHost> hostList) {
        ServerlessDatacenter dc = null;

        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        String logAddress = "Experiments/simulation1/Results";
        double time_zone = 10.0D;
        double cost = 3.0D;
        double costPerMem = 0.05D;
        double costPerStorage = 0.001D;
        double costPerBw = 0.0D;

        ContainerVmAllocationPolicy vmAllocationPolicy = new PowerContainerVmAllocationSimple(hostList);


        return dc;
    }

   private static List<ContainerHost> createHostList(int numHosts) {
        List<ContainerHost> hostList = new ArrayList<>();
        for (int i = 0; i < numHosts; i++) {
            int hostType = i / (int) Math.ceil((double) numHosts / 3.0D);
            ArrayList<ContainerVmPe> peList = new ArrayList<ContainerVmPe>();
            for (int j = 0; j < Constants.HOST_PES[hostType]; ++j) {
                peList.add(new ContainerVmPe(j,
                        new ContainerVmPeProvisionerSimple((double) Constants.HOST_MIPS[hostType])));
            }

            hostList.add(new PowerContainerHostUtilizationHistory(IDs.pollId(ContainerHost.class),
                    new ContainerVmRamProvisionerSimple(Constants.HOST_RAM[hostType]),
                    new ContainerVmBwProvisionerSimple(Constants.HOST_BW), Constants.HOST_STORAGE, peList,
                    new ContainerVmSchedulerTimeSharedOverSubscription(peList),
                    Constants.HOST_POWER[hostType]));
        }


        return hostList;
   }

}
