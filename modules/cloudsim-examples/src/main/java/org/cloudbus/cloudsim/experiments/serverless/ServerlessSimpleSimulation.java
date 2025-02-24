package org.cloudbus.cloudsim.experiments.serverless;

/* Title:        Fist Experiment with CloudSimSC Simulator 
* Description: Just designing the workflow which is going to be used for testing 
* the final result
*
* Copyright (c) 2025, Amirkabir University of Technology, Tehran, Iran
*/

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

import com.opencsv.CSVWriter;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerBwProvisionerSimple;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerPe;
import org.cloudbus.cloudsim.container.containerProvisioners.CotainerPeProvisionerSimple;
import org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmBwProvisionerSimple;
import org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmPe;
import org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmPeProvisionerSimple;
import org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmRamProvisionerSimple;
import org.cloudbus.cloudsim.container.core.*;
import org.cloudbus.cloudsim.container.hostSelectionPolicies.HostSelectionPolicy;
import org.cloudbus.cloudsim.container.hostSelectionPolicies.HostSelectionPolicyFirstFit;
import org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled.PCVmAllocationPolicyMigrationAbstractHostSelection;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerVmAllocationPolicy;
import org.cloudbus.cloudsim.container.resourceAllocators.PowerContainerVmAllocationSimple;
import org.cloudbus.cloudsim.container.schedulers.ContainerVmSchedulerTimeSharedOverSubscription;
import org.cloudbus.cloudsim.container.utils.IDs;
import org.cloudbus.cloudsim.container.vmSelectionPolicies.PowerContainerVmSelectionPolicy;
import org.cloudbus.cloudsim.container.vmSelectionPolicies.PowerContainerVmSelectionPolicyMaximumUsage;
import org.cloudbus.cloudsim.serverless.*;
import org.cloudbus.cloudsim.core.CloudSim;

/**
 * Simulation setup for Serverless Function execution
 *
 */

public class ServerlessSimpleSimulation {

  /** The vmlist. */
  private static List<ServerlessInvoker> vmList;
  private static String csvResultFilePath;

   private int overBookingfactor = 80;
  private static int controllerId;

  private static RequestLoadBalancer loadBalancer;

  private static ServerlessDatacenter DC;

  private static ServerlessController controller;

  /**
   * Creates main() to run this experiment
   */
  public static void main(String[] args) {

    Log.printLine("Starting ServerlessSimpleSumulation...");

    try {
      // First step: Initialize the CloudSim package. It should be called
      // before creating any entities.
      int num_user = 1; // number of cloud users
      Calendar calendar = Calendar.getInstance();
      boolean trace_flag = false; // mean trace events
      csvResultFilePath = "Experiments/Simulation/";

      // Initialize the CloudSim library
      CloudSim.init(num_user, calendar, trace_flag);

      controller = createBroker();
      controllerId = controller.getId();

      // Second step: Create Datacenters
      // Datacenters are the resource providers in CloudSim. We need at least one of
      // them to run a CloudSim simulation
      DC = createDatacenter("datacenter");

      // Third step: Create the virtual machines
      vmList = createVmList(controllerId);

      // Fourth step: submit vm list to the broker
      controller.submitVmList(vmList);

      // Fifth step: Create a load balancer
      loadBalancer = new RequestLoadBalancer(controller, DC);
      controller.setLoadBalancer(loadBalancer);
      controller.setServerlessDatacenter(DC);

      // Sixth step: Create the request workload
      createRequests();

      // the time at which the simulation has to be terminated.
      CloudSim.terminateSimulation(3000.00);

      // Starting the simualtion
      CloudSim.startSimulation();

      // Stopping the simualtion.
      CloudSim.stopSimulation();

      // Printing the results when the simulation is finished.
      List<ContainerCloudlet> finishedRequests = controller.getCloudletReceivedList();
      double averageResourceUtilization = controller.getAverageResourceUtilization();

      saveResultsAsCSV();
      // printRequestList(finishedRequests);
      // printContainerList(destroyedContainers);
      // printContainerList(containerList);
      if (Constants.MONITORING) {
//        printVmUpDownTime();
        printVmUtilization();
        System.out.println("Number of Finished Requests: " +  finishedRequests.size());
      }

      // Writing the results to a file when the simulation is finished.
//       writeDataLineByLine(finishedRequests);
      Log.printLine("ServerlessSimulationSimple finished!");
    } catch (Exception e) {
      e.printStackTrace();
      Log.printLine("Unwanted errors happen");
    }
  }

  private static String getCSVResultsFilePath() {
    return csvResultFilePath + "ServerlessSimpleSimulation/results.csv";
  }

  private static void saveResultsAsCSV() {
    String path = getCSVResultsFilePath();
    List<ServerlessRequest> requestList = controller.getCloudletReceivedList();
    try {
      // Ensure all directories in the path exist
      File file = new File(path);
      Files.createDirectories(Paths.get(file.getParent()));

      try (CSVWriter writer = new CSVWriter(new FileWriter(path))) {
        // Define CSV Header
        String[] header = {"Request ID", "Function ID", "Arrival Time", "Start Time",  "Finish Time", "ExecutionTime", "Response Time"};
        writer.writeNext(header);

        DecimalFormat dft = new DecimalFormat("####.##");

        for (ServerlessRequest request : requestList) {
          // Extract relevant request data
          int requestId = request.getCloudletId();
          String functionId =  request.getRequestFunctionId();
          double startTime = request.getExecStartTime();
          double arrivalTime = request.getArrivalTime();
          double finishTime = request.getFinishTime();
          double executionTime = request.getFinishTime() - request.getExecStartTime();
          double responseTime = finishTime -  request.getArrivalTime();

          // Format values for clarity
          String[] data = {
                  String.valueOf(requestId),
                  functionId,
                  dft.format(arrivalTime),
                  dft.format(startTime),
                  dft.format(finishTime),
                  dft.format(executionTime),
                  dft.format(responseTime)
          };

          writer.writeNext(data);
        }

        System.out.println("✅ Simulation results saved to: " + path);

      } catch (IOException e) {
        e.printStackTrace();
        System.err.println("❌ Error writing to CSV file: " + path);
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      System.err.println("❌ Error Creating the Path: " + path);
    }
  }

  private static void createRequests() throws IOException {

    BufferedReader br = new BufferedReader(new FileReader(Constants.FUNCTION_REQUESTS_FILENAME));
    String line = null;
    String cvsSplitBy = ",";
    controller.noOfTasks++;

    // concurrency is enabled
    UtilizationModelPartial utilizationModelPar = new UtilizationModelPartial();
    UtilizationModelFull utilizationModel = new UtilizationModelFull();

    long fileSize = 10L;
    long outputSize = 10L;
    double cpuShareReq = 1.0d;
    double memShareReq = 1.0d;
    double arrivalTime;
    String functionID;
    long requestLength;
    int pesNumber;
    int containerMemory;
    int containerMips;


    while ((line = br.readLine()) != null) {
      String[] data = line.split(cvsSplitBy);
      int createdRequests = 0;

      ServerlessRequest request = null;
      arrivalTime = Double.parseDouble(data[0]);
      functionID = String.valueOf(data[1]);
      requestLength = Long.parseLong(data[2]);
      pesNumber = Integer.parseInt(data[3]);
      containerMemory = Integer.parseInt(data[4]);
      containerMips = Integer.parseInt(data[5]);

      try {
        request = new ServerlessRequest(
                IDs.pollId(ServerlessRequest.class),
                arrivalTime, functionID, requestLength, pesNumber, containerMemory,
                containerMips, cpuShareReq, memShareReq, fileSize, outputSize, utilizationModelPar,
                utilizationModelPar, utilizationModel, 10, true);
        System.out.println("request No " + request.getCloudletId());
      } catch (Exception e) {
        e.printStackTrace();
        br.close();
        System.exit(0);
      }
      request.setUserId(controller.getId());
      System.out
          .println(CloudSim.clock() + " request created. This request arrival time is :" + arrivalTime);
      controller.requestArrivalTime.add(arrivalTime + Constants.FUNCTION_SCHEDULING_DELAY);
      controller.requestQueue.add(request);
      createdRequests += 1;
    }
    br.close();
  }

  private static ArrayList<ServerlessInvoker> createVmList(int brokerId) {
    ArrayList<ServerlessInvoker> containerVms = new ArrayList<ServerlessInvoker>();
    for (int i = 0; i < Constants.NUMBER_VMS; ++i) {
      ArrayList<ContainerPe> peList = new ArrayList<ContainerPe>();
      Random rand = new Random();
      int vmType = rand.nextInt(4);
      for (int j = 0; j < Constants.VM_PES[vmType]; ++j) {
        peList.add(new ContainerPe(j,
            new CotainerPeProvisionerSimple((double) Constants.VM_MIPS[vmType])));
      }
      containerVms.add(new ServerlessInvoker(IDs.pollId(ContainerVm.class), brokerId,
          (double) Constants.VM_MIPS[vmType], (float) Constants.VM_RAM[vmType],
          Constants.VM_BW, Constants.VM_SIZE, "Xen",
          new ServerlessContainerScheduler(peList),
          new ServerlessContainerRamProvisioner(Constants.VM_RAM[vmType]),
          new ContainerBwProvisionerSimple(Constants.VM_BW),
          peList, Constants.SCHEDULING_INTERVAL));

    }

    return containerVms;
  }

  private static ServerlessController createBroker() {

    ServerlessController controller = null;
    int overBookingFactor = 80;

    try {
      controller = new ServerlessController("Broker", overBookingFactor);
    } catch (Exception var2) {
      var2.printStackTrace();
      System.exit(0);
    }

    return controller;
  }

  public static ServerlessDatacenter createDatacenter(String name) throws Exception {
    String arch = "x86";
    String os = "Linux";
    String vmm = "Xen";
    String logAddress = "Experiments/simulation1/Results";
    double time_zone = 10.0D;
    double cost = 3.0D;
    double costPerMem = 0.05D;
    double costPerStorage = 0.001D;
    double costPerBw = 0.0D;

    List<ContainerHost> hostList = createHostList(Constants.NUMBER_HOSTS);
    // Select hosts to migrate
//    HostSelectionPolicy hostSelectionPolicy = new HostSelectionPolicyFirstFit();
    // Select vms to migrate
//    PowerContainerVmSelectionPolicy vmSelectionPolicy = new PowerContainerVmSelectionPolicyMaximumUsage();
    // Allocating host to vm
//    ContainerVmAllocationPolicy vmAllocationPolicy = new PCVmAllocationPolicyMigrationAbstractHostSelection(hostList,
//        vmSelectionPolicy,
//        hostSelectionPolicy, Constants.OVER_UTILIZATION_THRESHOLD, Constants.UNDER_UTILIZATION_THRESHOLD);
    ContainerVmAllocationPolicy vmAllocationPolicy = new PowerContainerVmAllocationSimple(hostList);
//     Allocating vms to container
    FunctionScheduler containerAllocationPolicy = new FunctionScheduler();

    ContainerDatacenterCharacteristics characteristics = new ContainerDatacenterCharacteristics(arch, os, vmm, hostList,
        time_zone, cost, costPerMem, costPerStorage,
        costPerBw);
    /** Set datacenter monitoring to true if metrics monitoring is required **/
    ServerlessDatacenter datacenter = new ServerlessDatacenter(name, characteristics, vmAllocationPolicy,
        containerAllocationPolicy, new LinkedList<Storage>(), Constants.SCHEDULING_INTERVAL,
        getExperimentName("SimTest1", String.valueOf(80)), logAddress,
        Constants.VM_STARTTUP_DELAY, Constants.CONTAINER_STARTTUP_DELAY, Constants.MONITORING);

    return datacenter;
  }

  private static String getExperimentName(String... args) {
    StringBuilder experimentName = new StringBuilder();

    for (int i = 0; i < args.length; ++i) {
      if (!args[i].isEmpty()) {
        if (i != 0) {
          experimentName.append("_");
        }

        experimentName.append(args[i]);
      }
    }

    return experimentName.toString();
  }

  public static List<ContainerHost> createHostList(int hostsNumber) {
    ArrayList<ContainerHost> hostList = new ArrayList<ContainerHost>();
    for (int i = 0; i < hostsNumber; ++i) {
      int hostType = i / (int) Math.ceil((double) hostsNumber / 3.0D);
      // System.out.println("Host type is: "+ hostType);
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

    for (ContainerHost host : hostList) {
      System.out.println(String.valueOf(host));
    }

    return hostList;
  }

  private static void printContainerList(List<ServerlessContainer> list) {
    int size = list.size();
    ServerlessContainer container;
    int deadlineMetStat = 0;

    String indent = "    ";
    Log.printLine();
    Log.printLine("========== OUTPUT ==========");
    Log.printLine("Container ID" + indent + "VM ID" + indent + "Start Time" + indent
        + "Finish Time" + indent + "Finished Requests List");

    DecimalFormat dft = new DecimalFormat("###.##");
    for (int i = 0; i < size; i++) {
      container = list.get(i);
      Log.print(indent + container.getId() + indent + indent);

      Log.printLine(indent + (container.getVm()).getId() + indent + indent + (dft.format(container.getStartTime()))
          + indent + indent + indent + indent + dft.format(container.getFinishTime())
          + indent + indent + indent + indent
          + container.getfinishedTasks());

    }

    Log.printLine("Deadline met no: " + deadlineMetStat);

  }

  /**
   * Prints the request objects
   * 
   * @param list list of requests
   */
  private static void printRequestList(List<ContainerCloudlet> list) {
    int size = list.size();
    Cloudlet request;
    int deadlineMetStat = 0;
    int totalResponseTime = 0;
    int failedRequestRatio = 0;
    float averageResponseTime = 0;
    int totalRequests = 0;
    int failedRequests = 0;

    String indent = "    ";
    Log.printLine();
    Log.printLine("========== OUTPUT ==========");
    Log.printLine("request ID" + indent + "Function ID" + indent + "Container ID" + indent + "STATUS" + indent
        + "Data center ID" + indent + "Final VM ID" + indent + "Execution Time" + indent
        + "Start Time" + indent + "Finish Time" + indent + "Response Time" + indent + "Vm List");

    DecimalFormat dft = new DecimalFormat("###.##");
    for (int i = 0; i < size; i++) {
      request = list.get(i);
      Log.print(indent + request.getCloudletId() + indent + indent);
      Log.print(indent + ((ServerlessRequest) request).getRequestFunctionId() + indent + indent);
      Log.print(indent + ((ServerlessRequest) request).getContainerId() + indent + indent);
      totalRequests += 1;

      if (request.getCloudletStatusString() == "Success") {
        totalResponseTime += request.getFinishTime() - ((ServerlessRequest) request).getArrivalTime();
        Log.print("SUCCESS");
        if (Math
            .ceil((request.getFinishTime() - ((ServerlessRequest) request).getArrivalTime())) <= (Math
                .ceil(((ServerlessRequest) request).getMaxExecTime()))
            || Math.floor((request.getFinishTime() - ((ServerlessRequest) request).getArrivalTime())) <= (Math
                .ceil(((ServerlessRequest) request).getMaxExecTime()))) {
          deadlineMetStat++;
        }
      } else {
        Log.print("DROPPED");
        failedRequests += 1;
      }

      Log.printLine(indent + indent + request.getResourceId()
          + indent + indent + indent + indent + request.getVmId()
          + indent + indent + indent + indent
          + dft.format(request.getActualCPUTime()) + indent + indent
          + indent + dft.format(request.getExecStartTime())
          + indent + indent + indent
          + dft.format(request.getFinishTime()) + indent + indent + indent
          + dft.format(request.getFinishTime() - ((ServerlessRequest) request).getArrivalTime()) + indent + indent
          + indent
          + ((ServerlessRequest) request).getResList());

    }

    Log.printLine("Deadline met no: " + deadlineMetStat);

  }

  private static void printVmUpDownTime() {
    double totalVmUpTime = 0;

    for (int x = 0; x < controller.getVmsCreatedList().size(); x++) {
      ServerlessInvoker vm = ((ServerlessInvoker) controller.getVmsCreatedList().get(x));
      if (vm.getStatus().equals("OFF")) {
        vm.offTime += 2500.00 - vm.getRecordTime();
      } else if (vm.getStatus().equals("ON")) {
        vm.onTime += 2500.00 - vm.getRecordTime();
      }
      System.out.println("Vm # " + controller.getVmsCreatedList().get(x).getId() + " used: " + vm.used);
      System.out.println("Vm # " + controller.getVmsCreatedList().get(x).getId() + "has uptime of: " + vm.onTime);
      System.out.println("Vm # " + controller.getVmsCreatedList().get(x).getId() + "has downtime of: " + vm.offTime);
      totalVmUpTime += vm.onTime;
    }
  }

  private static void printVmUtilization() {
    System.out.println("Average CPU utilization of vms: " + controller.getAverageResourceUtilization());
    System.out.println("Average vm count: " + controller.getAverageVmCount());
    System.out.println("Using exsiting cont: " + controller.exsitingContCount);

  }

  private static void writeDataLineByLine(List<ContainerCloudlet> list) throws ParameterException {
    // first create file object for file placed at location
    // specified by filepath
    int size = list.size();
    int successfulRequestCount = 0;
    int droppedRequestCount = 0;
    double totalResponseTime = 0;
    float failedRequestRatio = 0;
    float averageResponseTime = 0;
    Cloudlet request;
    String indent = "    ";
    // java.io.File file = new java.io.File("D:\\OneDrive - The University of
    // Melbourne\\UniMelb\\Studying\\Serverless simulator\\Data\\Result.csv");
    java.io.File file = new java.io.File("Result.csv");

    try {
      // create FileWriter object with file as parameter
      FileWriter outputfile = new FileWriter(file);

      // create CSVWriter object filewriter object as parameter
      CSVWriter writer = new CSVWriter(outputfile);

      // adding header to csv
      String[] header = { "request ID", "Function ID", "STATUS", "Container ID", "Data center ID", "Final VM ID",
          "Execution Time", "Start Time", "Finish Time", "Response Time", "Vm List" };
      writer.writeNext(header);

      for (ContainerCloudlet containerRequest : list) {
        request = containerRequest;

        if (request.getCloudletStatusString().equals("Success")) {
          totalResponseTime += (request.getFinishTime() - ((ServerlessRequest) request).getArrivalTime());
          successfulRequestCount++;
          String[] data = { String.valueOf(request.getCloudletId()),
              String.valueOf(((ServerlessRequest) request).getRequestFunctionId()), "SUCCESS",
              String.valueOf(((ServerlessRequest) request).getContainerId()), String.valueOf(request.getResourceId()),
              String.valueOf(request.getVmId()), String.valueOf(request.getActualCPUTime()),
              String.valueOf(request.getExecStartTime()), String.valueOf(request.getFinishTime()),
              String.valueOf((request.getFinishTime() - ((ServerlessRequest) request).getArrivalTime())),
              ((ServerlessRequest) request).getResList() };
          writer.writeNext(data);

        } else {
          droppedRequestCount++;
          String[] data = { String.valueOf(request.getCloudletId()),
              String.valueOf(((ServerlessRequest) request).getRequestFunctionId()), "DROPPED",
              String.valueOf(request.getResourceId()), String.valueOf(request.getVmId()),
              String.valueOf(request.getActualCPUTime()), String.valueOf(request.getExecStartTime()),
              String.valueOf(request.getFinishTime()),
              String.valueOf((request.getFinishTime() - ((ServerlessRequest) request).getArrivalTime())),
              ((ServerlessRequest) request).getResList() };
          writer.writeNext(data);
        }
      }

      if (Constants.MONITORING) {
        for (int x = 0; x < controller.getVmsCreatedList().size(); x++) {
          String[] data = { "Vm # ", String.valueOf(controller.getVmsCreatedList().get(x).getId()), "On time  ",
              String.valueOf(((ServerlessInvoker) controller.getVmsCreatedList().get(x)).onTime), "Off time  ",
              String.valueOf(((ServerlessInvoker) controller.getVmsCreatedList().get(x)).offTime) };
          writer.writeNext(data);
        }
      }

      String[] data1 = { "Average VM utilization  ", String.valueOf(controller.getAverageResourceUtilization()) };
      writer.writeNext(data1);
      String[] data2 = { "Average VM Count # ", String.valueOf(controller.getAverageVmCount()) };
      writer.writeNext(data2);
      String[] data3 = { "Successful Request Count # ", String.valueOf(successfulRequestCount) };
      writer.writeNext(data3);
      String[] data4 = { "Dropped Request Count # ", String.valueOf(droppedRequestCount) };
      writer.writeNext(data4);
      totalResponseTime = totalResponseTime / successfulRequestCount;
      String[] data5 = { "Average Request Response Time # ", String.valueOf(totalResponseTime) };
      writer.writeNext(data5);
      failedRequestRatio = (float) droppedRequestCount / (successfulRequestCount + droppedRequestCount);
      String[] data6 = { "Dropped Request Ratio # ", String.valueOf(failedRequestRatio) };
      writer.writeNext(data6);

      // closing writer connection
      writer.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}
