import SJF.sjf;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;

import java.util.*;

public class LoadBalancerComparison {

    private static List<Map<String, String>> results = new ArrayList<>();
    
    private static List<Vm> createVM(int userId, int numberOfVm) {
        
        LinkedList<Vm> list = new LinkedList<>();
        long size = 10000; 
        int ram = 512; 
        int mips = 1000;
        long bw = 1000;
        int pesNumber = 1; 
        String vmm = "Xen"; 

        for(int i = 0; i< numberOfVm; i++){
            Vm vm = new Vm(i, userId, mips+(i*10), pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
            list.add(vm);
        }

        return list;
    }

    private static List<Cloudlet> createCloudlet(int userId, int numberOfCloudlet){
      
        LinkedList<Cloudlet> list = new LinkedList<>();

        long length = 1000;
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        for(int i = 0; i < numberOfCloudlet; i++){
            Cloudlet cloudlet = new Cloudlet(i, (length + 2L * i * 10), pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            cloudlet.setUserId(userId);
            list.add(cloudlet);
        }

        return list;
    }

    public static void main(String[] args) {
        Log.printLine("===================================== Load Balancer ==================================");
        try {
            Calendar calendar = Calendar.getInstance();

            Scanner scanner = new Scanner(System.in);

            Log.printLine();
            Log.printLine("Initialize the CloudSim package.");
            Log.printLine("Enter number of grid users:");
            int numUsers = scanner.nextInt();

            Log.printLine();
            Log.printLine("Create Datacenters are the resource providers in CloudSim. We need at list one of them to run a CloudSim simulation.");
            Log.printLine("Enter number of datacenters:");
            int numberOfDatacenters = scanner.nextInt();

            Log.printLine();
            Log.printLine("Create Broker:");

            Log.printLine("Enter number of vms:");
            int numberOfVm = scanner.nextInt();

            Log.printLine("Enter number of cloudlet");
            int numberOfCloudlet = scanner.nextInt();

            for (int i = 1; i <= 5 ; i++) {
                CloudSim.init(numUsers, calendar, true);

                DatacenterBroker broker = null;
                try {
                    switch (i) {
                        case 1:
                            broker = new sjf("ShortestJobFirstDatacenterBroker");
                            break;
                        
                        default:
                            Log.printLine("Please, select from [1] only:");
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                for (int j = 0; j < numberOfDatacenters; j++) {
                    createDatacenter("Datacenter_" + j);
                }

                assert broker != null;
                int brokerId = broker.getId();
                String brokerName = broker.getName();

                Log.printLine("Broker: " + brokerName);
                Log.printLine("Create VMs");

                List<Vm> vmList = createVM(brokerId, numberOfVm);

                Log.printLine();
                Log.printLine("Create Cloudlets");

                List<Cloudlet> cloudletList = createCloudlet(brokerId, numberOfCloudlet);

                Log.printLine("Sending them to broker...");

                broker.submitVmList(vmList);
                broker.submitCloudletList(cloudletList);

                Log.printLine();
                Log.printLine("Starts the simulation");

                CloudSim.startSimulation();

                Log.printLine();
                Log.printLine("Results when simulation is over");

                List<Cloudlet> cloudletReceivedList = broker.getCloudletReceivedList();
                List<Vm> vmsCreatedList = broker.getVmsCreatedList();

                CloudSim.stopSimulation();

                printResult(cloudletReceivedList, brokerName);

                Log.printLine();
                Log.printLine("Simulation Complete");
            }

            String leftAlignFormat = "| %-39s | %-15s | %-15s |%n";

            System.out.format("+-----------------------------------------+-----------------+-----------------+%n");
            System.out.format("| Broker                                  | Total CPU Time  | Average CPU Time|%n");
            System.out.format("+-----------------------------------------+-----------------+-----------------+%n");
            for (Map<String, String> result: results) {
                System.out.format(leftAlignFormat, result.get("broker"), result.get("total_cpu_time"), result.get("average_cpu_time"));
            }
            System.out.format("+-----------------------------------------+-----------------+-----------------+%n");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    private static Datacenter createDatacenter(String name){

        List<Host> hostList = new ArrayList<>();

        List<Pe> peList1 = new ArrayList<>();

        int mips = 10000;

       
        peList1.add(new Pe(0, new PeProvisionerSimple(mips + 500))); 
        peList1.add(new Pe(1, new PeProvisionerSimple(mips + 1000)));
        peList1.add(new Pe(2, new PeProvisionerSimple(mips + 1500)));
        peList1.add(new Pe(3, new PeProvisionerSimple(mips + 700)));

        
        List<Pe> peList2 = new ArrayList<>();

        peList2.add(new Pe(0, new PeProvisionerSimple(mips + 700)));
        peList2.add(new Pe(1, new PeProvisionerSimple(mips + 900)));

        
        int hostId=0;
        int ram = 1002048; 
        long storage = 1000000; 
        int bw = 10000;

        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList1,
                        new VmSchedulerTimeShared(peList1)
                )
        ); 

        hostId++;

        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList2,
                        new VmSchedulerTimeShared(peList2)
                )
        ); 
        String arch = "x86";      
        String os = "Linux";          
        String vmm = "Xen";
        double time_zone = 10.0;         
        double cost = 3.0;              
        double costPerMem = 0.05;		
        double costPerStorage = 0.1;	
        double costPerBw = 0.1;			
        LinkedList<Storage> storageList = new LinkedList<>();	

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);


        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    
    private static void printResult(List<Cloudlet> list, String broker) {

        Log.printLine();
        Log.printLine();
        Log.printLine("========================================== OUTPUT ==========================================");
        Log.printLine("Broker: " + broker);
        
        double time = 0;

        for (Cloudlet value : list) {
            if (value.getCloudletStatus() == Cloudlet.SUCCESS) {
                time += value.getActualCPUTime();
            }
        }

        double avgTime = time/list.toArray().length;
        Log.printLine("Total CPU Time: " + time);
        Log.printLine("Average CPU Time: " + avgTime);

        Map<String, String> result = new HashMap<>();
        result.put("broker", broker);
        result.put("total_cpu_time", String.format("%.5f", time));
        result.put("average_cpu_time", String.format("%.5f", avgTime));

        results.add(result);
    }

}