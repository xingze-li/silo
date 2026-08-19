package de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.preparation;

import de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.DataSetSynPop;
import de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.ModuleSynPop;
import de.tum.bgu.msm.syntheticPopulationGenerator.properties.PropertiesSynPop;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Preparation extends ModuleSynPop {

    private static final Logger logger = LogManager.getLogger(Preparation.class);



    public Preparation(DataSetSynPop dataSetSynPop){
        super(dataSetSynPop);
    }

    @Override
    public void run(){

        logger.info("Started input data preparation.");
        readZonalData();
        if (PropertiesSynPop.get().main.runIPU || PropertiesSynPop.get().main.runAllocation) {
            readMicroData();
            translatePersonMicroData();
            checkHouseholdRelationships();
            writeMicroData();
            if (PropertiesSynPop.get().main.runIPU) {
                createFrequencyMatrix();
                writeFrequencyMatrix();
            }
        }
        logger.info("   Completed input data preparation.");
    }


    private void readZonalData(){
        new ReadZonalData(dataSetSynPop).run();
    }


    private void readMicroData(){
        new ReadMicroData(dataSetSynPop).run();
    }


    private void checkHouseholdRelationships(){
        new CheckHouseholdRelationship(dataSetSynPop).run();
    }


    private void translatePersonMicroData(){
        new TranslateMicroDataToCode(dataSetSynPop).run();
    }


    private void createFrequencyMatrix(){
        new PrepareFrequencyMatrix(dataSetSynPop).run();
    }


    private void writeMicroData(){
        SiloUtil.writeTableDataSet(dataSetSynPop.getPersonDataSet(), PropertiesSynPop.get().main.microPersonsFileName);
        SiloUtil.writeTableDataSet(dataSetSynPop.getHouseholdDataSet(), PropertiesSynPop.get().main.microHouseholdsFileName);
        SiloUtil.writeTableDataSet(dataSetSynPop.getDwellingDataSet(), PropertiesSynPop.get().main.microDwellingsFileName);
//        SiloUtil.writeTableDataSet(dataSetSynPop.getFrequencyMatrix(), PropertiesSynPop.get().main.frequencyMatrixFileName);
    }
    private void writeFrequencyMatrix(){
        SiloUtil.writeTableDataSet(dataSetSynPop.getFrequencyMatrix(), PropertiesSynPop.get().main.frequencyMatrixFileName);
    }

}
