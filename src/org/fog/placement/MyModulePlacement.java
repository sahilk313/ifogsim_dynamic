package org.fog.placement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;

public class MyModulePlacement extends ModulePlacement{
	protected ModuleMapping moduleMapping;
	protected List<Sensor> sensors;
	protected List<Actuator> actuators;
	protected String moduleToPlace;
	protected Map<Integer, Integer> deviceMipsInfo;
	public MyModulePlacement(List<FogDevice> fogDevices, List<Sensor>
	sensors, List<Actuator> actuators,
	Application application, ModuleMapping moduleMapping,
	String moduleToPlace){
		this.setFogDevices(fogDevices);
		this.setApplication(application);
		this.setModuleMapping(moduleMapping);
	
		this.setModuleToDeviceMap(new HashMap<String, List<Integer>>());
	
		this.setDeviceToModuleMap(new HashMap<Integer, List<AppModule>>());
	
		setSensors(sensors);
		setActuators(actuators);
		this.moduleToPlace = moduleToPlace;
		this.deviceMipsInfo = new HashMap<Integer, Integer>();
		mapModules();
	}
	@Override
	protected void mapModules() {
	
		for(String deviceName : getModuleMapping().getModuleMapping().keySet()){
	
			for(String moduleName : getModuleMapping().getModuleMapping().get(deviceName)){
		
				int deviceId = CloudSim.getEntityId(deviceName);
			
				AppModule appModule = getApplication().getModuleByName(moduleName);
			
				if(!getDeviceToModuleMap().containsKey(deviceId))
				{
			
					List<AppModule>placedModules = new ArrayList<AppModule>();
				
					placedModules.add(appModule);
				
					getDeviceToModuleMap().put(deviceId, placedModules);
				}
				else
				{
			
					List<AppModule>placedModules = getDeviceToModuleMap().get(deviceId);
				
					placedModules.add(appModule);
				
					getDeviceToModuleMap().put(deviceId, placedModules);
			
				}
			}
		}
		
		for(FogDevice device:getFogDevices())
		{
		int deviceParent = -1;
		List<Integer>children = new ArrayList<Integer>();
		if(device.getLevel()==1)
		{
			if(!deviceMipsInfo.containsKey(device.getId()))
				deviceMipsInfo.put(device.getId(), 0);
				deviceParent = device.getParentId();
				for(FogDevice deviceChild:getFogDevices())
				{
					if(deviceChild.getParentId()==device.getId()){children.add(deviceChild.getId());}
				}
	
				Map<Integer, Double>childDeadline = new HashMap<Integer, Double>();
	
				for(int childId:children)
	
					childDeadline.put(childId,getApplication().getDeadlineInfo().get(childId).get(moduleToPlace));
	
				List<Integer> keys = new ArrayList<Integer>(childDeadline.keySet());
	
				for(int i = 0; i<keys.size()-1; i++)
				{
					for(int j=0;j<keys.size()-i-1;j++)
					{
						if(childDeadline.get(keys.get(j))>childDeadline.get(keys.get(j+1))){
							int tempJ = keys.get(j);
							int tempJn = keys.get(j+1);
							keys.set(j, tempJn);
							keys.set(j+1, tempJ);
						}
					}
				}
	
				int baseMipsOfPlacingModule = (int)getApplication().getModuleByName(moduleToPlace).getMips();
	
				for(int key:keys)
				{
					int currentMips = deviceMipsInfo.get(device.getId());
		
					AppModule appModule = getApplication().getModuleByName(moduleToPlace);
		
					int additionalMips = getApplication().getAdditionalMipsInfo().get(key).get(moduleToPlace);
		
					if(currentMips+baseMipsOfPlacingModule+additionalMips<device.getMips())
		
					{
		
						currentMips = currentMips+baseMipsOfPlacingModule+additionalMips;
			
						deviceMipsInfo.put(device.getId(), currentMips);
			
						if(!getDeviceToModuleMap().containsKey(device.getId()))
						{
			
							List<AppModule>placedModules = new ArrayList<AppModule>();
				
							placedModules.add(appModule);
							getDeviceToModuleMap().put(device.getId(),
				
							placedModules);
						}
						else
						{
							List<AppModule>placedModules = getDeviceToModuleMap().get(device.getId());
	
							placedModules.add(appModule);
							getDeviceToModuleMap().put(device.getId(),
	
							placedModules);
	
						}
					}
					else
					{
						List<AppModule>placedModules = getDeviceToModuleMap().get(deviceParent);

						placedModules.add(appModule);
						getDeviceToModuleMap().put(deviceParent, placedModules);
					}
				}
			}
		}
	}
	public ModuleMapping getModuleMapping() {
	return moduleMapping;
	}
	public void setModuleMapping(ModuleMapping moduleMapping) {
	this.moduleMapping = moduleMapping;
	}
	public List<Sensor> getSensors() {
	return sensors;
	}
	public void setSensors(List<Sensor> sensors) {
	this.sensors = sensors;
	}
	public List<Actuator> getActuators() {
	return actuators;
	}
	public void setActuators(List<Actuator> actuators) {
	this.actuators = actuators;
	}
}
			


