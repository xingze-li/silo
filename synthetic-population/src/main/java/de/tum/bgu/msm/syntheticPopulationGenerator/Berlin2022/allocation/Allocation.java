package de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.allocation;


import de.tum.bgu.msm.container.DataContainer;
import de.tum.bgu.msm.data.person.Person;
import de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.DataSetSynPop;
import de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.ModuleSynPop;
import de.tum.bgu.msm.syntheticPopulationGenerator.properties.PropertiesSynPop;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;

public class Allocation extends ModuleSynPop{

    private static final Logger logger = LogManager.getLogger(Allocation.class);
    private final DataContainer dataContainer;
    private HashMap<Person, Integer> educationalLevel;

    public Allocation(DataSetSynPop dataSetSynPop, DataContainer dataContainer){
        super(dataSetSynPop);
        this.dataContainer = dataContainer;
    }

    @Override
    public void run(){
        logger.info("   Started allocation model.");
        if (PropertiesSynPop.get().main.runAllocation) {
            generateHouseholdsPersonsDwellings();
            generateVacantDwellings();
            if (PropertiesSynPop.get().main.runJobAllocation) {
                generateJobs();
            }
        } else {
            logger.info("   Population allocation is disabled by run.population.allocation=false.");
        }
        if (PropertiesSynPop.get().main.runJobAllocation) {
            if (educationalLevel == null) {
                throw new IllegalStateException(
                        "Berlin 2022 job allocation requires run.population.allocation=true.");
            }
            assignJobs();
            assignJobProperties();
        } else {
            logger.info("   Job allocation is disabled by run.job.allocation=false.");
        }
        logger.info("   Completed allocation model.");

    }

    public void generateHouseholdsPersonsDwellings(){
        if (PropertiesSynPop.get().main.boroughIPU){
            for (int county : dataSetSynPop.getBoroughsByCounty().keySet()){
                addBoroughsAsCities(county);
            }
        }
        educationalLevel = new HashMap<>();
        new GenerateHouseholdsPersonsDwellings(dataContainer, dataSetSynPop, educationalLevel).run();
        if (PropertiesSynPop.get().main.boroughIPU){
            for (int county : dataSetSynPop.getBoroughsByCounty().keySet()){
                removeBoroughsAsCities(county);
            }
        }
    }

    private void generateJobs() {
        new GenerateJobs(dataContainer, dataSetSynPop).run();
    }

    private void assignJobs() {
        new AssignJobs(dataContainer, dataSetSynPop, educationalLevel).run();
    }

    private void assignJobProperties() {
        new AssignPropertiesToJobs(dataContainer, dataSetSynPop).run();
    }

    public void addBoroughsAsCities(int county){
        //Add to the municipality list the boroughs, because they have weights as well
        //Only if the option of running IPU with three areas is true
        ArrayList<Integer> newCities = new ArrayList<>();
        for (int city : dataSetSynPop.getMunicipalities()){
            if (!dataSetSynPop.getMunicipalitiesByCounty().get(county).contains(city)){
                newCities.add(city);
            }
        }
        newCities.addAll(dataSetSynPop.getBoroughsByCounty().get(county));
        dataSetSynPop.setMunicipalities(newCities);
    }

    public void removeBoroughsAsCities(int county){
        //Remove to the list of boroughs at the municipality, because they have weights for household generation but are not on the job allocation explicitly
        //Only if the option of running IPU with three areas is true
        ArrayList<Integer> newCities = dataSetSynPop.getMunicipalitiesByCounty().get(county);
        newCities.removeAll(dataSetSynPop.getBoroughsByCounty().get(county));
        dataSetSynPop.setMunicipalities(newCities);
    }

    private void generateVacantDwellings(){
        new GenerateVacantDwellings(dataContainer, dataSetSynPop).run();
    }
}
