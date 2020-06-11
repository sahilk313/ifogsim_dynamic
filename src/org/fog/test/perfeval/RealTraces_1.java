package org.fog.test.perfeval;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;
//ss
public class RealTraces_1 {
	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<FogDevice> mobiles = new ArrayList<FogDevice>();
	static List<Sensor> sensors = new ArrayList<Sensor>();
	static List<Actuator> actuators = new ArrayList<Actuator>();
	static int numOfDepts = 2;
	static int numOfMobilesPerDept = 1;
	static double EEG_TRANSMISSION_TIME = 5.1;
	static int L=0;
	public static void main(String[] args) {
		Log.printLine("Starting Multi_Apps...");
		try {
			Log.disable();
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events
			CloudSim.init(num_user, calendar, trace_flag);
			int jobCount=0;

			Scanner fread=new Scanner(new File("C:\\Users\\SAHIL\\workspace\\rt-sane\\RT_SANE\\src\\org\\fog\\test\\perfeval\\test.txt"));
			String data;
			String appId[]=new String[100];
			int jobLength[]=new int[100];
			while(fread.hasNextLine()) {
				data=fread.nextLine();
				String[] token=data.split(" ");
				appId[jobCount]= token[0];
				jobLength[jobCount]= Integer.valueOf(token[1]);
				//System.out.println(appId[jobCount]+"--"+jobLength[jobCount]);
				jobCount++;
			}
			fread.close();
			//jobCount--;
			FogBroker[] broker = new FogBroker[jobCount]; 
			for(int i=0;i<jobCount;i++) {
			    broker[i]= new FogBroker("broker_"+i);
				}
			
			Application[] application = new Application[jobCount];
			for(int i=0;i<jobCount;i++) {
				    application[i] = createApplication(appId[i], broker[i].getId(), jobLength[i]);	
					}
			
			for(int i=0;i<jobCount;i++) {
				    application[i].setUserId(broker[i].getId());
					}
			
			createFogDevices();
					
			for(int i=0;i<jobCount;i++){
			    createEdgeDevices(broker[i].getId(), appId[i], i);			
			 }
		
			ModuleMapping[] moduleMapping = new ModuleMapping[jobCount];
			for(int i=0;i<jobCount;i++){
		     moduleMapping[i] = ModuleMapping.createModuleMapping(); // initializing a module mapping
		    }
			for(FogDevice device : fogDevices){
				if(device.getName().startsWith("m-0")){
					for(int i=0; i<4; i++) {
					moduleMapping[i].addModuleToDevice("client_"+i, device.getName());// fixing all instances of the client_0 module to the Smartphones
					}
				}
				if(device.getName().startsWith("m-0")){
					for(int i=4; i<8; i++) {
					moduleMapping[i].addModuleToDevice("client_"+i, device.getName());// fixing all instances of the client_0 module to the Smartphones
					}
				}
				if(device.getName().startsWith("m-1")){
					for(int i=8; i<jobCount; i++) {
						moduleMapping[i].addModuleToDevice("client_"+i, device.getName());// fixing all instances of the client_0 module to the Smartphones
						}
				}
			}
			Controller controller = new Controller("master-controller", fogDevices, sensors,actuators);
			
			for(int i=0;i<jobCount;i++){
			controller.submitApplication(application[i], new ModulePlacementMapping(fogDevices, application[i], moduleMapping[i]));
			}
			
			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
			CloudSim.startSimulation();
			CloudSim.stopSimulation();
			Log.printLine("VRGame finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}
	private static void createEdgeDevices(int userId, String appId, int i) {
		for(FogDevice mobile : mobiles){
			String id = mobile.getName();
			Sensor eegSensor = new Sensor("s-"+appId+"-"+id, "EEG_"+i, userId, appId, new DeterministicDistribution(EEG_TRANSMISSION_TIME)); // inter-transmission time of EEG sensor follows a deterministic distribution
			sensors.add(eegSensor);
			Actuator display = new Actuator("a-"+appId+"-"+id, userId, appId, "DISPLAY_"+i);	
			actuators.add(display);
			eegSensor.setGatewayDeviceId(mobile.getId());
			eegSensor.setLatency(6.0);  // latency of connection between EEG sensors and the parent Smartphone is 6 ms
			display.setGatewayDeviceId(mobile.getId());
			display.setLatency(1.0);  // latency of connection between Display actuator and the parent Smartphone is 1 ms		
		}
	}
	
	private static void createFogDevices() {
		FogDevice cloud = createFogDevice("cloud", 44800, 40000, 4000, 10000, 0, 0.01, 16*103, 16*83.25); // creates the fog device Cloud at the apex of the hierarchy with level=0
		cloud.setParentId(-1);
		FogDevice proxy = createFogDevice("proxy-server", 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333); // creates the fog device Proxy Server (level=1)
		proxy.setParentId(cloud.getId()); // setting Cloud as parent of the Proxy Server
		proxy.setUplinkLatency(100); // latency of connection from Proxy Server to the Cloud is 100 ms
		
		fogDevices.add(cloud);
		fogDevices.add(proxy);
		
		for(int i=0;i<numOfDepts;i++){
			addGw(i+"", proxy.getId()); // adding a fog device for every Gateway in physical topology. The parent of each gateway is the Proxy Server
		}
	}

	private static FogDevice addGw(String id, int parentId){
		FogDevice dept = createFogDevice("d-"+id, 1000, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
		fogDevices.add(dept);
		dept.setParentId(parentId);
		dept.setUplinkLatency(4); // latency of connection between gateways and proxy server is 4 ms
		for(int i=0;i<numOfMobilesPerDept;i++){
			String mobileId = id+"-"+i;
			FogDevice mobile = addMobile(mobileId, dept.getId()); // adding mobiles to the physical topology. Smartphones have been modeled as fog devices as well.
			
			mobile.setUplinkLatency(2); // latency of connection between the smartphone and proxy server is 4 ms
			fogDevices.add(mobile);
		}
		return dept;
	}
	
	private static FogDevice addMobile(String id, int parentId){
		FogDevice mobile = createFogDevice("m-"+id, 1000, 1000, 10000, 270, 3, 0, 87.53, 82.44);
		mobile.setParentId(parentId);
		mobiles.add(mobile);
		return mobile;
	}

	private static FogDevice createFogDevice(String nodeName, long mips,
			int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {
		
		List<Pe> peList = new ArrayList<Pe>();

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

		int hostId = FogUtils.generateEntityId();
		long storage = 1000000; // host storage
		int bw = 20000;

		PowerHost host = new PowerHost(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw),
				storage,
				peList,
				new StreamOperatorScheduler(peList),
				new FogLinearPowerModel(busyPower, idlePower)
			);

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN devices by now

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
				arch, os, vmm, host, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		FogDevice fogdevice = null;
		try {
			fogdevice = new FogDevice(nodeName, characteristics, 
					new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}
		fogdevice.setLevel(level);
		return fogdevice;
	}

	@SuppressWarnings({"serial" })
	private static Application createApplication(String appId, int userId, double cpuLength){ 
		Application application = Application.createApplication(appId, userId); // creates an empty application model (empty directed graph)
		    application.addAppModule("client_"+L, 10); // adding module client_0 to the application model
			application.addAppEdge("EEG_"+L, "client_"+L, cpuLength, 50, "EEG_"+L, Tuple.UP, AppEdge.SENSOR); // adding edge from EEG (sensor) to client_0 module carrying tuples of type EEG
		   // System.out.println(cpuLength);
		    application.addTupleMapping("client_"+L, "EEG_"+L, "_SENSOR", new FractionalSelectivity(0.9)); // 0.9 tuples of type _SENSOR are emitted by client_0 module per incoming tuple of type EEG 
		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("EEG_"+L);add("client_"+L);add("client_"+L);}});
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);}};
		application.setLoops(loops); L++;
			return application;
	}
}