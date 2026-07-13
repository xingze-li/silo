package de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.preparation;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.common.matrix.Matrix;
//import de.tum.bgu.msm.data.dwelling.DefaultDwellingTypes;
import de.tum.bgu.msm.data.AreaTypes;
import de.tum.bgu.msm.data.Id;
import de.tum.bgu.msm.data.MitoZone;
import de.tum.bgu.msm.data.MunichDwellingTypes.DwellingTypeMunich;
import de.tum.bgu.msm.data.Zone;
import de.tum.bgu.msm.data.travelDistances.MatrixTravelDistances;
import de.tum.bgu.msm.data.travelTimes.SkimTravelTimes;
import de.tum.bgu.msm.io.input.AbstractParquetReader;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.DataSetSynPop;
import de.tum.bgu.msm.syntheticPopulationGenerator.properties.PropertiesSynPop;
import de.tum.bgu.msm.util.matrices.IndexedDoubleMatrix2D;
import de.tum.bgu.msm.utils.SiloUtil;
import omx.OmxFile;
import omx.OmxLookup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ReadZonalData {

    private static final Logger logger = LogManager.getLogger(ReadZonalData.class);

    private final DataSetSynPop dataSetSynPop;

    public ReadZonalData(DataSetSynPop dataSetSynPop){
        this.dataSetSynPop = dataSetSynPop;

    }

    public void run() {
        readCities();
        readZones();
        readDistanceMatrix();
        readTripLengthFrequencyDistribution();
    }

    private void readCities() {
        int[] cityID;
        int[] countyID;
        HashMap<Integer, ArrayList> municipalitiesByCounty;
        //List of municipalities and counties that are used for IPU and allocation
        ArrayList<Integer> municipalities = new ArrayList<>();
        ArrayList<Integer> counties = new ArrayList<>();
        municipalitiesByCounty = new HashMap<>();
        ArrayList<Integer> municipalitiesWithZero = new ArrayList<>();
        for (int row = 1; row <= PropertiesSynPop.get().main.selectedMunicipalities.getRowCount(); row++) {
            if (PropertiesSynPop.get().main.selectedMunicipalities.getValueAt(row, "Select") == 1f) {
                int city = (int) PropertiesSynPop.get().main.selectedMunicipalities.getValueAt(row, "ID_city");
                municipalities.add(city);
                int county = (int) PropertiesSynPop.get().main.selectedMunicipalities.getValueAt(row, "ID_county");
                if (!SiloUtil.containsElement(counties, county)) {
                    counties.add(county);
                }
                if (municipalitiesByCounty.containsKey(county)) {
                    ArrayList<Integer> citiesInThisCounty = municipalitiesByCounty.get(county);
                    citiesInThisCounty.add(city);
                    municipalitiesByCounty.put(county, citiesInThisCounty);
                } else {
                    ArrayList<Integer> citiesInThisCounty = new ArrayList<>();
                    citiesInThisCounty.add(city);
                    municipalitiesByCounty.put(county, citiesInThisCounty);
                }
            }
        }
        cityID = SiloUtil.convertArrayListToIntArray(municipalities);
        countyID = SiloUtil.convertArrayListToIntArray(counties);
        dataSetSynPop.setCityIDs(cityID);
        dataSetSynPop.setCountyIDs(countyID);
        dataSetSynPop.setMunicipalities(municipalities);
        dataSetSynPop.setCounties(counties);
        dataSetSynPop.setMunicipalitiesByCounty(municipalitiesByCounty);
        dataSetSynPop.setMunicipalitiesWithZeroPopulation(municipalitiesWithZero);

        if (PropertiesSynPop.get().main.boroughIPU) {
            HashMap<Integer, ArrayList> boroughsByCounty = new HashMap<>();
            ArrayList<Integer> boroughs = new ArrayList<>();
            ArrayList<Integer> countieswithBoroughs = new ArrayList<>();
            for (int row = 1; row <= PropertiesSynPop.get().main.selectedBoroughs.getRowCount(); row++) {
                if (PropertiesSynPop.get().main.selectedBoroughs.getValueAt(row, "Select") == 1f) {
                    int borough = (int) PropertiesSynPop.get().main.selectedBoroughs.getValueAt(row, "ID_borough");
                    int county = (int) PropertiesSynPop.get().main.selectedBoroughs.getValueAt(row, "ID_county");
                    boroughs.add(borough);
                    if (boroughsByCounty.containsKey(county)) {
                        ArrayList<Integer> boroughsInThisCity = boroughsByCounty.get(county);
                        boroughsInThisCity.add(borough);
                        boroughsByCounty.put(county, boroughsInThisCity);
                    } else {
                        ArrayList<Integer> boroughsInThisCity = new ArrayList<>();
                        boroughsInThisCity.add(borough);
                        boroughsByCounty.put(county, boroughsInThisCity);
                        countieswithBoroughs.add(county);
                    }
                }
            }
            dataSetSynPop.setBoroughsByCounty(boroughsByCounty);
            dataSetSynPop.setBoroughs(boroughs);
        }
    }


    private void readZones(){
        //TAZ attributes
        HashMap<Integer, int[]> cityTAZ = new HashMap<>();
        Map<Integer, Id> zones = new LinkedHashMap<>();
        Map<Integer, Map<Integer, Float>> probabilityZone = new HashMap<>();
        Map<Integer, Map<DwellingTypeMunich, Integer>> dwellingPriceByTypeAndZone = new HashMap<>();
        Table<Integer, Integer, Integer> schoolCapacity = HashBasedTable.create();
        ArrayList<Integer> tazs = new ArrayList<>();
        TableDataSet zoneAttributes;
        if (!PropertiesSynPop.get().main.boroughIPU){
            zoneAttributes = PropertiesSynPop.get().main.cellsMatrix;
        } else {
            zoneAttributes = PropertiesSynPop.get().main.cellsMatrixBoroughs;
        }
        for (int i = 1; i <= zoneAttributes.getRowCount(); i++){
            int city = (int) zoneAttributes.getValueAt(i,"ID_borough");
            int taz = (int) zoneAttributes.getValueAt(i,"ID_cell");
            float probability = zoneAttributes.getValueAt(i, "population");
            int priceEFHFreistehend = (int) zoneAttributes.getValueAt(i,"ddEFHFreistehend");
            int priceEFHDoppelhaus = (int) zoneAttributes.getValueAt(i,"ddEFHDoppelhaus");
            int priceEFHReihenhaus = (int) zoneAttributes.getValueAt(i,"ddEFHReihenhaus");
            int priceMFH = (int) zoneAttributes.getValueAt(i,"ddMFH");
            int capacityPrimary = (int)zoneAttributes.getValueAt(i,"capacityPrimary");
            int capacitySecondary = (int)zoneAttributes.getValueAt(i,"capacitySecondary");
            int capacityTertiary = (int)zoneAttributes.getValueAt(i,"capacityTertiary");
            int bbsr = (int)zoneAttributes.getValueAt(i,"BBSR");
            if (!tazs.contains(taz)) {
                tazs.add(taz);
            }
            if (cityTAZ.containsKey(city)){
                int[] previousTaz = cityTAZ.get(city);
                previousTaz = SiloUtil.expandArrayByOneElement(previousTaz, taz);
                cityTAZ.put(city, previousTaz);
                Map<Integer, Float> probabilities = probabilityZone.get(city);
                probabilities.put(taz, probability);
            } else {
                int[] previousTaz = {taz};
                cityTAZ.put(city,previousTaz);
                Map<Integer, Float> probabilities = new HashMap<>();
                probabilities.put(taz, probability);
                probabilityZone.put(city, probabilities);
            }
            Map<DwellingTypeMunich, Integer> prices = new HashMap<>();
            prices.put(DwellingTypeMunich.EFHFreistehend, priceEFHFreistehend);
            prices.put(DwellingTypeMunich.EFHDoppelhaus, priceEFHDoppelhaus);
            prices.put(DwellingTypeMunich.EFHReihenhaus, priceEFHReihenhaus);
            prices.put(DwellingTypeMunich.MFH, priceMFH);
            dwellingPriceByTypeAndZone.put(taz,prices);
            schoolCapacity.put(taz,1,capacityPrimary);
            schoolCapacity.put(taz, 2, capacitySecondary);
            schoolCapacity.put(taz, 3, capacityTertiary);

            MitoZone currentZone =
                    new MitoZone(
                            taz,
                            AreaTypes.SGType.valueOf(bbsr)
                    );

            zones.put(taz, currentZone);
        }
        dataSetSynPop.setProbabilityZone(probabilityZone);
        dataSetSynPop.setDwellingPriceByTypeAndZone(dwellingPriceByTypeAndZone);
        dataSetSynPop.setTazByMunicipality(cityTAZ);
        dataSetSynPop.setSchoolCapacity(schoolCapacity);
        dataSetSynPop.setTazs(tazs);
        dataSetSynPop.setTazIDs(tazs.stream().mapToInt(i -> i).toArray());
        this.dataSetSynPop.setZones(zones);
    }

//    private void readZones(){
//
//        HashMap<Integer, int[]> cityTAZ = new HashMap<>();
//        Map<Integer, Map<Integer, Float>> probabilityZone = new HashMap<>();
//        Map<Integer, Map<DwellingTypeMunich, Integer>> dwellingPriceByTypeAndZone = new HashMap<>();
//
//        Table<Integer, Integer, Integer> schoolCapacity = HashBasedTable.create();
//        ArrayList<Integer> tazs = new ArrayList<>();
//
//        TableDataSet zoneAttributes;
//        if (!PropertiesSynPop.get().main.boroughIPU){
//            zoneAttributes = PropertiesSynPop.get().main.cellsMatrix;
//        } else {
//            zoneAttributes = PropertiesSynPop.get().main.cellsMatrixBoroughs;
//        }
//
//        String municipalityColumn = PropertiesSynPop.get().main.boroughIPU
//                ? "ID_borough"
//                : "ID_city";
//
//        for (int i = 1; i <= zoneAttributes.getRowCount(); i++){
//
//            int city = (int) zoneAttributes.getValueAt(i, municipalityColumn);
//            int taz = (int) zoneAttributes.getValueAt(i, "ID_cell");
//
//            float probability = zoneAttributes.getValueAt(i, "Population");
//
//            int priceEFHFreistehend = (int) zoneAttributes.getValueAt(i, "ddEFHFreistehend");
//            int priceEFHDoppelhaus = (int) zoneAttributes.getValueAt(i, "ddEFHDoppelhaus");
//            int priceEFHReihenhaus = (int) zoneAttributes.getValueAt(i, "ddEFHReihenhaus");
//            int priceMFH = (int) zoneAttributes.getValueAt(i, "ddMFH");
//
//            int capacityPrimary = (int) zoneAttributes.getValueAt(i, "capacityPrimary");
//            int capacitySecondary = (int) zoneAttributes.getValueAt(i, "capacitySecondary");
//            int capacityTertiary = (int) zoneAttributes.getValueAt(i, "capacityTertiary");
//
//            if (!tazs.contains(taz)) {
//                tazs.add(taz);
//            }
//
//            if (cityTAZ.containsKey(city)){
//                int[] previousTaz = cityTAZ.get(city);
//                previousTaz = SiloUtil.expandArrayByOneElement(previousTaz, taz);
//                cityTAZ.put(city, previousTaz);
//
//                Map<Integer, Float> probabilities = probabilityZone.get(city);
//                probabilities.put(taz, probability);
//            } else {
//                int[] previousTaz = {taz};
//                cityTAZ.put(city, previousTaz);
//
//                Map<Integer, Float> probabilities = new HashMap<>();
//                probabilities.put(taz, probability);
//                probabilityZone.put(city, probabilities);
//            }
//
//            Map<DwellingTypeMunich, Integer> prices = new HashMap<>();
//            prices.put(DwellingTypeMunich.EFHFreistehend, priceEFHFreistehend);
//            prices.put(DwellingTypeMunich.EFHDoppelhaus, priceEFHDoppelhaus);
//            prices.put(DwellingTypeMunich.EFHReihenhaus, priceEFHReihenhaus);
//            prices.put(DwellingTypeMunich.MFH, priceMFH);
//
//            dwellingPriceByTypeAndZone.put(taz, prices);
//
//            schoolCapacity.put(taz, 1, capacityPrimary);
//            schoolCapacity.put(taz, 2, capacitySecondary);
//            schoolCapacity.put(taz, 3, capacityTertiary);
//        }
//
//        dataSetSynPop.setProbabilityZone(probabilityZone);
//        dataSetSynPop.setDwellingPriceByTypeAndZone(dwellingPriceByTypeAndZone);
//        dataSetSynPop.setTazByMunicipality(cityTAZ);
//        dataSetSynPop.setSchoolCapacity(schoolCapacity);
//        dataSetSynPop.setTazs(tazs);
//        dataSetSynPop.setTazIDs(tazs.stream().mapToInt(i -> i).toArray());
//    }

    private void readDistanceMatrix(){
        //Read the skim matrix
        logger.info("   Starting to read Parquet matrix");

        Collection<Id> lookup;
        lookup = this.dataSetSynPop.getZones().values();
        String parquetFile =
                PropertiesSynPop.get()
                        .main
                        .omxFileName;

        IndexedDoubleMatrix2D distanceSkimAuto =
                AbstractParquetReader.readAndConvertToDoubleMatrix(
                        parquetFile,
                        "FROM",
                        "TO",
                        "inVehDistance_m",
                        0.001,
                        lookup
                );
        this.dataSetSynPop.setDistanceTazToTaz(distanceSkimAuto);
        logger.info("   Read Parquet distance matrix from " + parquetFile);
    }


    private void readTripLengthFrequencyDistribution(){
        logger.info("   Starting to read trip length frequency distributions");
        String fileName = PropertiesSynPop.get().main.tripLengthDistributionFileName;
        String recString = "";
        Table<Integer, String, Float> frequencies = HashBasedTable.create();
        int recCount = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            recString = in.readLine();

            // read header
            String[] header = recString.split(",");
            int posId = SiloUtil.findPositionInArray("km", header);
            int posHBW = SiloUtil.findPositionInArray("HBW",header);
            int posPrimarySecondary = SiloUtil.findPositionInArray("Primary",header);
            int posTertiary = SiloUtil.findPositionInArray("Tertiary",header);

            // read line
            while ((recString = in.readLine()) != null) {
                recCount++;
                String[] lineElements = recString.split(",");
                int length  = Integer.parseInt(lineElements[posId]);
                float hbw  = Float.parseFloat(lineElements[posHBW]);
                float primary  = Float.parseFloat(lineElements[posPrimarySecondary]);
                float tertiary  = Float.parseFloat(lineElements[posTertiary]);
                frequencies.put(length,"HBW", hbw);
                frequencies.put(length, "Primary", primary);
                frequencies.put(length, "Tertiary", tertiary);
            }


        } catch (IOException e) {
            logger.fatal("IO Exception caught reading synpop job file: " + fileName);
            logger.fatal("recCount = " + recCount + ", recString = <" + recString + ">");
        }
        dataSetSynPop.setTripLengthDistribution(frequencies);

    }
}
