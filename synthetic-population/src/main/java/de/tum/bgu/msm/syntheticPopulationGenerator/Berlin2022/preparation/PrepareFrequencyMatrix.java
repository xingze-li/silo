package de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.preparation;

import com.google.common.primitives.Ints;
import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.DataSetSynPop;
import de.tum.bgu.msm.syntheticPopulationGenerator.properties.PropertiesSynPop;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Map;

public class PrepareFrequencyMatrix {

    private static final Logger logger = LogManager.getLogger(PrepareFrequencyMatrix.class);

    private DataSetSynPop dataSetSynPop;
    private TableDataSet frequencyMatrix;

    public PrepareFrequencyMatrix(DataSetSynPop dataSetSynPop){
        this.dataSetSynPop = dataSetSynPop;
    }

    public void run() {
        //create the frequency matrix with all the attributes aggregated at the household level
        logger.info("   Starting to create the frequency matrix");

        initializeAttributesMunicipality();
        logger.info("Attributes loaded: " + Arrays.toString(PropertiesSynPop.get().main.attributesMunicipality));


        for (int i = 1; i <= frequencyMatrix.getRowCount(); i++){
//            checkContainsAndUpdate(attributesMunicipality[i],);
            frequencyMatrix.setValueAt(i,"hhTotal",1);
            int hhSize = (int) dataSetSynPop.getHouseholdDataSet().getValueAt(i,"h.size");
            updateHhSize(hhSize, i);
            updateHhType((int) dataSetSynPop.getHouseholdDataSet().getValueAt(i,"h.type"), i);

            int seniorCount = 0;
            updateDdUse((int) dataSetSynPop.getDwellingDataSet().getValueAt(i,"d.use"), i);
            int numberOfDwellings = (int) dataSetSynPop.getDwellingDataSet().getValueAt(i,"d.numberOfApartments");
            int buildingTYpe = (int) dataSetSynPop.getDwellingDataSet().getValueAt(i,"d.type");
            updateDdType(buildingTYpe, numberOfDwellings, i);
            updateDdYear((int) dataSetSynPop.getDwellingDataSet().getValueAt(i,"d.year"), i);
//            updateDdFloor((int) dataSetSynPop.getDwellingDataSet().getValueAt(i,"d.space"), i);
            for (int j = 0; j < hhSize; j++){
                int row = (int) (dataSetSynPop.getHouseholdDataSet().getValueAt(i,"personCount") + j);
                int age = (int) dataSetSynPop.getPersonDataSet().getValueAt(row,"p.age");
                int gender = (int) dataSetSynPop.getPersonDataSet().getValueAt(row,"p.gender");
                int occupation = (int) dataSetSynPop.getPersonDataSet().getValueAt(row,"p.employmentStatus");
                int nationality = (int) dataSetSynPop.getPersonDataSet().getValueAt(row,"p.nationality");

                if (age>=65){
                    seniorCount++;
                }

                updateHhAgeGender(age, gender, i);
//                updateHhAge(age, i);
//                updateHhGender(gender, i);
//                updateHhWorkers(gender,occupation, i);
                updateHhForeigners(nationality, i);
            }
            updateHhSeniorStatus(seniorCount, hhSize, i);
            frequencyMatrix.setValueAt(i,"population",hhSize);
            if (PropertiesSynPop.get().main.boroughIPU) {
                frequencyMatrix.setValueAt(i, "MUChhTotal", 1);
                frequencyMatrix.setValueAt(i, "MUCpopulation", hhSize);
            }
        }
        dataSetSynPop.setFrequencyMatrix(frequencyMatrix);
        logger.info("   Finished creating the frequency matrix");

    }

    private void updateHhForeigners(int nationality, int i) {
        if (nationality > 2){
            int value = 1 + (int) frequencyMatrix.getValueAt(i,"p.foreigners");
            frequencyMatrix.setValueAt(i,"p.foreigners",value);
            if (PropertiesSynPop.get().main.boroughIPU) {
                frequencyMatrix.setValueAt(i, "MUCforeigners", value);
            }
        }
    }


    private void updateHhWorkers(int gender, int occupation, int i) {
        if (occupation == 1){
            if (gender == 1){
                int value = 1 + (int) frequencyMatrix.getValueAt(i,"p.sexEmp.maleEmp");
                frequencyMatrix.setValueAt(i,"p.sexEmp.maleEmp",value);
            } else {
                int value = 1 + (int) frequencyMatrix.getValueAt(i,"p.sexEmp.femaleEmp");
                frequencyMatrix.setValueAt(i,"p.sexEmp.femaleEmp",value);
            }
            if (PropertiesSynPop.get().main.boroughIPU) {
                if (gender == 1) {
                    int value = 1 + (int) frequencyMatrix.getValueAt(i, "MUCmaleWorkers");
                    frequencyMatrix.setValueAt(i, "MUCmaleWorkers", value);
                } else {
                    int value = 1 + (int) frequencyMatrix.getValueAt(i, "MUCfemaleWorkers");
                    frequencyMatrix.setValueAt(i, "MUCfemaleWorkers", value);
                }
            }
        }
    }

//    private void updateHhAge(int age, int i) {
//        if (age >= 80) {
//            return;
//        }
//
//        int row = 0;
//        while (age >= PropertiesSynPop.get().main.ageBracketsPerson[row]) {
//            row++;
//        }
//
//        int ageCode = PropertiesSynPop.get().main.ageBracketsPerson[row];
//
//        int value = 1 + (int) frequencyMatrix.getValueAt(i, "p.age." + ageCode);
//        frequencyMatrix.setValueAt(i, "p.age." + ageCode, value);
//    }
//
//    private void updateHhGender(int gender, int i) {
//        if (gender == 1) {
//            int value = 1 + (int) frequencyMatrix.getValueAt(i, "p.sex.male");
//            frequencyMatrix.setValueAt(i, "p.sex.male", value);
//        } else {
//            int value = 1 + (int) frequencyMatrix.getValueAt(i, "p.sex.female");
//            frequencyMatrix.setValueAt(i, "p.sex.female", value);
//        }
//    }

    private void updateHhAgeGender(int age, int gender, int i) {
        if (age>=80){
            return;
        }
        int row = 0;
        while ((age >= PropertiesSynPop.get().main.ageBracketsPerson[row])) {
            row++;
        }
        if (gender == 1){
            int ageCode = PropertiesSynPop.get().main.ageBracketsPerson[row];
            logger.info(ageCode);
            int value = 1 + (int) frequencyMatrix.getValueAt(i,"p.sexAge.male" + PropertiesSynPop.get().main.ageBracketsPerson[row]);
            frequencyMatrix.setValueAt(i,"p.sexAge.male" + PropertiesSynPop.get().main.ageBracketsPerson[row],value);
        } else {
            int value = 1 + (int) frequencyMatrix.getValueAt(i,"p.sexAge.female" + PropertiesSynPop.get().main.ageBracketsPerson[row]);
            frequencyMatrix.setValueAt(i,"p.sexAge.female" + PropertiesSynPop.get().main.ageBracketsPerson[row],value);
        }
        if (PropertiesSynPop.get().main.boroughIPU) {
            int row1 = 0;
//            if (age < 18) {
//                frequencyMatrix.setValueAt(i, "MUChhWithChildren", 1);
//            }
            if (gender == 2) {
                int value1 = 1 + (int) frequencyMatrix.getValueAt(i, "MUCfemale");
                frequencyMatrix.setValueAt(i, "MUCfemale", value1);
            }
//            while (age > PropertiesSynPop.get().main.ageBracketsBorough[row1]){
//                row1++;
//            }
//            int value = 1 + (int) frequencyMatrix.getValueAt(i, "MUCage" + PropertiesSynPop.get().main.ageBracketsBorough[row1]);
//            frequencyMatrix.setValueAt(i, "MUCage" + PropertiesSynPop.get().main.ageBracketsBorough[row1], value);
        }
    }

    private void updateHhType(int hhType, int i) {
        if  ((hhType == 7)||(hhType == 8)){
            frequencyMatrix.setValueAt(i,"h.type.couples", 1);
        } else if ((hhType == 1)||(hhType == 2)||(hhType == 3)||(hhType == 4)){
            frequencyMatrix.setValueAt(i,"h.type.couplesWithChildren", 1);
        } else if ((hhType == 5)||(hhType == 6)){
            frequencyMatrix.setValueAt(i,"h.type.singleWithChildren", 1);
        }
    }

    private void updateHhSeniorStatus(int seniorCount, int hhSize, int i) {

        if ((seniorCount > 1)&&(seniorCount == hhSize)) {
            frequencyMatrix.setValueAt(i, "h.senior.twoOrMore", 1);

        } else if ((seniorCount == 1)&&(seniorCount == hhSize)) {
            frequencyMatrix.setValueAt(i, "h.senior.single", 1);

        } else if ((seniorCount < hhSize)&&(seniorCount >0)) {
            frequencyMatrix.setValueAt(i, "h.senior.mixed", 1);
        }
    }


    //TO DO: do year, size, type separately
    private void updateDdYear(int ddYear, int i) {
        if( ddYear==1 ){
            frequencyMatrix.setValueAt(i,"d.year.before1950", 1);
        }else if (ddYear==2){
            frequencyMatrix.setValueAt(i,"d.year.1950to1989", 1);
        } else if (ddYear==3){
            frequencyMatrix.setValueAt(i,"d.year.1990to2009", 1);
        }
    }

    private void updateDdType(int ddType, int ddNumberOfApartments, int i) {
        if( (ddType==1)){
            frequencyMatrix.setValueAt(i,"d.type.detached", 1);
        }else if ((ddType==2)){
            frequencyMatrix.setValueAt(i,"d.type.semiDetached", 1);
        }else if ((ddType==3)){
            frequencyMatrix.setValueAt(i,"d.type.terraced", 1);
        } else if ((ddType==4)&&(ddNumberOfApartments==2)){
            frequencyMatrix.setValueAt(i,"d.type.MFH3to6Dwelling", 1);
        }
    }


    private void updateDdFloor(int ddFloor, int i) {
        if( ddFloor<40 ){
            frequencyMatrix.setValueAt(i,"ddFloor40", 1);
        }else if (ddFloor<60){
            frequencyMatrix.setValueAt(i,"ddFloor60", 1);
        } else if (ddFloor<80){
            frequencyMatrix.setValueAt(i,"ddFloor80", 1);
        } else if (ddFloor<100){
            frequencyMatrix.setValueAt(i,"ddFloor100", 1);
        } else if (ddFloor<120){
            frequencyMatrix.setValueAt(i,"ddFloor120", 1);
        } else if (ddFloor<160){
            frequencyMatrix.setValueAt(i,"ddFloor160", 1);
        } else {
            frequencyMatrix.setValueAt(i,"ddFloor160Plus", 1);
        }
    }


//    private void updateDdFloor(int ddFloor, int i) {
//        int row = 0;
//        if (ddFloor >PropertiesSynPop.get().main.sizeBracketsDwelling[PropertiesSynPop.get().main.sizeBracketsDwelling.length-1]){
//            row = PropertiesSynPop.get().main.sizeBracketsDwelling.length - 1;
//        } else {
//            while (ddFloor > PropertiesSynPop.get().main.sizeBracketsDwelling[row]) {
//                row++;
//            }
//        }
//        frequencyMatrix.setValueAt(i,"ddFloor" + PropertiesSynPop.get().main.sizeBracketsDwelling[row],1);
//    }


    private void updateDdUse(int ddUse, int i) {
        //Method to update the dwelling use
        if ((ddUse == 1)){
            frequencyMatrix.setValueAt(i,"d.use.owned", 1);
        } else {
            frequencyMatrix.setValueAt(i,"d.use.rented", 1);
        }
    }

    private void updateHhSize(int hhSize, int i) {
        //Method to update the frequency matrix depending on hhSize
//        if (hhSize > PropertiesSynPop.get().main.householdSizes[PropertiesSynPop.get().main.householdSizes.length - 1]){
//            hhSize = PropertiesSynPop.get().main.householdSizes[PropertiesSynPop.get().main.householdSizes.length - 1];
//        }
        if (hhSize<=5){
            frequencyMatrix.setValueAt(i,"h.size."+ hhSize, 1);
        }
//        else {
//            frequencyMatrix.setValueAt(i, "h.size.6OrMore", 1);
//        }

        if (PropertiesSynPop.get().main.boroughIPU) {
            if (hhSize == 1) {
                frequencyMatrix.setValueAt(i, "MUChhSize1", 1);
            }
        }
    }


    private void initializeAttributesMunicipality() {
        //Method to create the list of attributes given the generic names and the brackets

        frequencyMatrix = new TableDataSet();
        frequencyMatrix.appendColumn(Ints.toArray(dataSetSynPop.getHouseholdTable().rowKeySet()),"id");
        for (String attribute : PropertiesSynPop.get().main.attributesMunicipality){
            SiloUtil.addIntegerColumnToTableDataSet(frequencyMatrix, attribute);
        }
        if (PropertiesSynPop.get().main.twoGeographicalAreasIPU){
            for (String attribute : PropertiesSynPop.get().main.attributesCounty){
                SiloUtil.addIntegerColumnToTableDataSet(frequencyMatrix, attribute);
            }
        }
        if (PropertiesSynPop.get().main.boroughIPU) {
            for (String attribute : PropertiesSynPop.get().main.attributesBorough) {
                SiloUtil.addIntegerColumnToTableDataSet(frequencyMatrix, attribute);
            }
        }
    }

    private void checkContainsAndAdd(String key, int[] brackets, Map<String, Integer> map) {
        if (map.containsKey(key)){
            for (int i = 0; i < brackets.length; i++){
                String label = key + brackets[i];
                SiloUtil.addIntegerColumnToTableDataSet(frequencyMatrix,label);
            }
        }
    }
}
