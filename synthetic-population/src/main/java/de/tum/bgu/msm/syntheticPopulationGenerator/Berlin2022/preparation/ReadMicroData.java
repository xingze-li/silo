package de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.preparation;


import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.DataSetSynPop;
import de.tum.bgu.msm.syntheticPopulationGenerator.properties.PropertiesSynPop;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ReadMicroData {

    private static final Logger logger = LogManager.getLogger(ReadMicroData.class);

    private final DataSetSynPop dataSetSynPop;
    private final MicroDataManager microDataManager;
//    private Map<String, Map<String, Integer>> exceptionsMicroData = new HashMap<>();

    private Map<String, Set<Integer>> exceptionsMicroData = new HashMap<>();
    private HashMap<String, String[]> attributesMicroData = new HashMap<>();
    private Map<String, String> attributesPersonMicroData = new HashMap<>();
    private Map<String, String> attributesHouseholdMicroData = new HashMap<>();
    private Map<String, String> attributesDwellingMicroData = new HashMap<>();
    private Table<Integer, String, Integer> personTable = HashBasedTable.create();
    private Table<Integer, String, Integer> householdTable = HashBasedTable.create();
    private Table<Integer, String, Integer> dwellingTable = HashBasedTable.create();

    private TableDataSet personDataSet = new TableDataSet();
    private TableDataSet householdDataSet = new TableDataSet();
    private TableDataSet dwellingDataSet = new TableDataSet();

    public ReadMicroData(DataSetSynPop dataSetSynPop){
        this.dataSetSynPop = dataSetSynPop;
        this.microDataManager = new MicroDataManager(dataSetSynPop);
    }

    public void run(){

        logger.info("   Starting to read the micro data");

        exceptionsMicroData = microDataManager.exceptionsMicroData();
        attributesMicroData = microDataManager.attributesMicroData();
        attributesPersonMicroData = microDataManager.attributesPersonMicroData();
        attributesHouseholdMicroData = microDataManager.attributesHouseholdMicroData();
        attributesDwellingMicroData = microDataManager.attributesDwellingMicroData();


        //Scanning the file to obtain the number of households and persons in Bavaria
        String csvFile = PropertiesSynPop.get().main.microDataFile;

        int hhCount = 0;
        int personCount = 0;
//        Integer previousHouseholdNumber = null;

        try (Reader reader = Files.newBufferedReader(Paths.get(csvFile));
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
            int previousHouseholdNumber = -1;
            for (CSVRecord row : parser) {
                int restriction = checkRestrictions(row);

                if (restriction != 1) {
                    continue;
                }

                String recString = row.get("idhh").trim();

                int householdNumber = Integer.parseInt(recString.substring(3,11));
                if (householdNumber != previousHouseholdNumber) {
                    hhCount++;
                    previousHouseholdNumber = householdNumber;
                }
                personCount++;

            }
        } catch (Exception e) {
            throw new RuntimeException("Error scanning CSV for counts: " + csvFile, e);
        }

// ---------- PASS 1 completed ----------
        logger.info("PASS 1 completed: counted {} households and {} persons (after restrictions).",
                hhCount, personCount);

        // allocate exactly like original (same columns)
        generateTableDataSet(hhCount, personCount);

        // ---------- PASS 2: populate ----------
        hhCount = 0;
        personCount = 0;
//        previousHouseholdNumber = null;

        try (Reader reader = Files.newBufferedReader(Paths.get(csvFile));
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
            int previousHouseholdNumber = -1;
            for (CSVRecord row : parser) {
                int restriction = checkRestrictions(row);

                if (restriction != 1) {
                    continue;
                }
                String recString = row.get("idhh").trim();

                int householdNumber = Integer.parseInt(recString.substring(3,11));

                if (householdNumber != previousHouseholdNumber) {
                    hhCount++;
                    personCount++;
                    updateMicroHouseholds(hhCount, householdNumber, personCount, row);
                    updateMicroDwellings(hhCount, row);
                    updateMicroPersons(personCount, hhCount, householdNumber, row);
                    previousHouseholdNumber = householdNumber;
                } else {
                    personCount++;
                    updateMicroPersons(personCount, hhCount, householdNumber, row);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error populating from CSV: " + csvFile, e);
        }

        Map<Integer, Integer> hhSizes = new HashMap<>();

        for (int p = 1; p <= personDataSet.getRowCount(); p++) {

            int hhId = (int) personDataSet.getValueAt(p, "idHh");

            hhSizes.put(
                    hhId,
                    hhSizes.getOrDefault(hhId, 0) + 1
            );
        }

        for (int h = 1; h <= householdDataSet.getRowCount(); h++) {

            int hhId = (int) householdDataSet.getValueAt(h, "id");

            int correctedSize = hhSizes.getOrDefault(hhId, 0);

            householdDataSet.setValueAt(h, "h.size", correctedSize);

            householdTable.put(h, "h.size", correctedSize);
        }

        dataSetSynPop.setPersonTable(personTable);
        dataSetSynPop.setHouseholdTable(householdTable);
        dataSetSynPop.setDwellingTable(dwellingTable);

        dataSetSynPop.setPersonDataSet(personDataSet);
        dataSetSynPop.setHouseholdDataSet(householdDataSet);
        dataSetSynPop.setDwellingDataSet(dwellingDataSet);

        SiloUtil.writeTableDataSet(personDataSet,"microData/interimFiles/persons.csv");
        SiloUtil.writeTableDataSet(householdDataSet,"microData/interimFiles/households.csv");
        SiloUtil.writeTableDataSet(dwellingDataSet,"microData/interimFiles/dwellings.csv");

        logger.info("  Read " + personCount + " person records in " + hhCount + " households from CSV.");
        logger.info("   Finished reading the micro data");
    }


//    private int checkRestrictions(CSVRecord row){
//        int restriction = 1;
//        for (String exception : exceptionsMicroData.keySet()){
//            Map<String, Integer> exceptionData = exceptionsMicroData.get(exception);
//            // get the key of the map exceptionData
//            String header = exceptionData.keySet().iterator().next();
//            int threshold = exceptionsMicroData.get(exception).get(header);
//            int value =  toInt(row.get(header));
//            if (threshold == value){
//                restriction = 0;
//            }
//        }
//        return restriction;
//    }

    private int checkRestrictions(CSVRecord row){
        for (String column : exceptionsMicroData.keySet()) {
            int value = toInt(row.get(column));
            if (exceptionsMicroData.get(column).contains(value)) {
                return 0;
            }
        }
        return 1;
    }


//    private int checkRestrictions(CSVRecord row){
//        int restriction = 1;
//        for (String exception : exceptionsMicroData.keySet()){
//            Map<String, Integer> exceptionData = exceptionsMicroData.get(exception);
//
//            if (exceptionData == null || exceptionData.isEmpty()) {
//                logger.warn("Empty exception rule: " + exception);
//                continue;
//            }
//
//            String header = exceptionData.keySet().iterator().next();
//            int threshold = exceptionData.get(header);
//            int value = toInt(row.get(header));
//
//            if (threshold == value){
//                restriction = 0;
//            }
//        }
//        return restriction;
//    }


    private void updateMicroPersons(int personCount, int hhCount, int householdNumber, CSVRecord row){
        personTable.put(personCount, "id", personCount);
        personTable.put(personCount,"idHh",hhCount);
        personTable.put(personCount,"recordHh",householdNumber);
        personDataSet.setValueAt(personCount, "id", personCount);
        personDataSet.setValueAt(personCount, "idHh", hhCount);
//        personDataSet.setValueAt(personCount, "recordHh", householdNumber);
        for (Map.Entry<String, String> pair : attributesPersonMicroData.entrySet()){
            String attribute = pair.getKey();
            String header = pair.getValue();
            int value = toInt(row.get(header));
            personTable.put(personCount, attribute, value);
            personDataSet.setValueAt(personCount, attribute, value);
        }
    }


    private void updateMicroHouseholds(int hhCount, int householdNumber, int personCount,  CSVRecord row){
        householdTable.put(hhCount,"id", hhCount);
        householdTable.put(hhCount, "recordHh", householdNumber);
        householdTable.put(hhCount,"personCount", personCount);
        householdDataSet.setValueAt(hhCount, "id", hhCount);
//        householdDataSet.setValueAt(hhCount, "recordHh", householdNumber);
        householdDataSet.setValueAt(hhCount, "personCount", personCount);
        for (Map.Entry<String, String> pair : attributesHouseholdMicroData.entrySet()){
            String attribute = pair.getKey();
            String header = pair.getValue();
            int value = toInt(row.get(header));
            householdTable.put(hhCount, attribute, value);
            householdDataSet.setValueAt(hhCount, attribute, value);
        }
    }


    private void updateMicroDwellings(int hhCount, CSVRecord row){
        dwellingTable.put(hhCount, "id", hhCount);
        dwellingDataSet.setValueAt(hhCount, "id", hhCount);
        for (Map.Entry<String, String> pair : attributesDwellingMicroData.entrySet()){
            String attribute = pair.getKey();
            String header = pair.getValue();
            int value = toInt(row.get(header));
            dwellingTable.put(hhCount, attribute, value);
            dwellingDataSet.setValueAt(hhCount, attribute, value);
        }
    }

    private void generateTableDataSet(int hhCount, int ppCount){

        personDataSet = new TableDataSet();
        appendNewColumnToTDS(personDataSet, "id", ppCount);
        appendNewColumnToTDS(personDataSet, "idHh", ppCount);
//        appendNewColumnToTDS(personDataSet, "recordHh", ppCount);
        for (Map.Entry<String, String> pair : attributesPersonMicroData.entrySet()){
            String attribute = pair.getKey();
            appendNewColumnToTDS(personDataSet, attribute, ppCount);
        }

        householdDataSet = new TableDataSet();
        appendNewColumnToTDS(householdDataSet, "id", hhCount);
//        appendNewColumnToTDS(householdDataSet, "recordHh", hhCount);
        appendNewColumnToTDS(householdDataSet, "personCount", hhCount);
        for (Map.Entry<String, String> pair : attributesHouseholdMicroData.entrySet()){
            String attribute = pair.getKey();
            appendNewColumnToTDS(householdDataSet, attribute, hhCount);
        }

        dwellingDataSet = new TableDataSet();
        appendNewColumnToTDS(dwellingDataSet, "id", hhCount);
        for (Map.Entry<String, String> pair : attributesDwellingMicroData.entrySet()){
            String attribute = pair.getKey();
            appendNewColumnToTDS(dwellingDataSet, attribute, hhCount);
        }
    }


    private void appendNewColumnToTDS(TableDataSet tableDataSet, String columnName, int length){

        int[] dummy = SiloUtil.createArrayWithValue(length, 0);
        tableDataSet.appendColumn(dummy, columnName);
    }


    private int convertToInteger(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            boolean spacesOnly = true;
            for (int pos = 0; pos < s.length(); pos++) {
                if (!s.substring(pos, pos+1).equals(" ")) spacesOnly = false;
            }
            if (spacesOnly) return -999;
            else {
                logger.fatal("String " + s + " cannot be converted into an integer.");
                return 0;
            }
        }
    }


    private int toInt(String s) {
        if (s == null) return -999;
        s = s.trim();
        if (s.isEmpty()) return -999;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            try { return (int)Math.floor(Double.parseDouble(s)); }
            catch (NumberFormatException ex) { return -999; }
        }
    }

}
