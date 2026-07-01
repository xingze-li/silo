package de.tum.bgu.msm.syntheticPopulationGenerator.properties;

import de.tum.bgu.msm.properties.PropertiesUtil;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.commons.math3.distribution.GammaDistribution;

import java.util.ResourceBundle;

public class Munich2022PropertiesSynPop extends AbstractPropertiesSynPop {

    public Munich2022PropertiesSynPop(ResourceBundle bundle) {

        PropertiesUtil.newPropertySubmodule("SP: main properties");

        microDataFile = PropertiesUtil.getStringProperty(bundle, "micro.data");

        microPersonsFileName = PropertiesUtil.getStringProperty(bundle, "micro.persons", "microData/interimFiles/microPersons.csv");
        microHouseholdsFileName = PropertiesUtil.getStringProperty(bundle, "micro.households", "microData/interimFiles/microHouseholds.csv");
        microDwellingsFileName = PropertiesUtil.getStringProperty(bundle, "micro.dwellings", "microData/interimFiles/microDwellings.csv");
        frequencyMatrixFileName = PropertiesUtil.getStringProperty(bundle, "frequency.matrix", "microData/interimFiles/frequencyMatrix.csv");


        runIPU = PropertiesUtil.getBooleanProperty(bundle, "run.ipu.synthetic.pop", false);
        runMicrolocation = PropertiesUtil.getBooleanProperty(bundle, "run.sp.microlocation", false);

        //todo I would read these attributes from a file, probable, the same as read in the next property

        runAllocation = PropertiesUtil.getBooleanProperty(bundle, "run.population.allocation", false);

        twoGeographicalAreasIPU = PropertiesUtil.getBooleanProperty(bundle, "run.ipu.city.and.county", false);
        boroughIPU = PropertiesUtil.getBooleanProperty(bundle,"run.three.areas",false);

        attributesMunicipality = PropertiesUtil.getStringPropertyArray(bundle, "attributes.municipality");
//        attributesCounty = PropertiesUtil.getStringPropertyArray(bundle, "attributes.county");

        // todo this table is not a property but a data container, "ID_city" might be a property? (if this is applciable to other implementations)
        marginalsMunicipality = SiloUtil.readCSVfile(PropertiesUtil.getStringProperty(bundle,"marginals.municipality","input/syntheticPopulation/input2022_new/marginalsMunicipality.csv"));
        marginalsMunicipality.buildIndex(marginalsMunicipality.getColumnPosition("ID_city"));

        //todo same as municipalities
        if (twoGeographicalAreasIPU){
            attributesCounty = PropertiesUtil.getStringPropertyArray(bundle, "attributes.county"); //attributes are decided on the properties file
            marginalsCounty = SiloUtil.readCSVfile(PropertiesUtil.getStringProperty(bundle,"marginals.county","input/syntheticPopulation/input2022/marginalsCounty2022.csv")); //all the marginals from the region
            marginalsCounty.buildIndex(marginalsCounty.getColumnPosition("ID_county"));
        }

        selectedMunicipalities = SiloUtil.readCSVfile(PropertiesUtil.getStringProperty(bundle,"municipalities.list","input/syntheticPopulation/input2022_new/municipalitiesList.csv"));
        selectedMunicipalities.buildIndex(selectedMunicipalities.getColumnPosition("ID_city"));

        cellsMatrix = SiloUtil.readCSVfile(PropertiesUtil.getStringProperty(bundle,"taz.definition ","input/syntheticPopulation/input2022/zoneAttributes_2022_5types_with_capacity.csv"));
        cellsMatrix.buildIndex(cellsMatrix.getColumnPosition("ID_cell"));

        //todo this cannot be the final name of the matrix
        omxFileName = PropertiesUtil.getStringProperty(bundle, "distanceODmatrix", "input/syntheticPopulation/tdTest.omx");

        ageBracketsPerson = PropertiesUtil.getIntPropertyArray(bundle, "age.brackets", new int[]{5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80});
        ageBracketsPersonQuarter = null;
        ageBracketsBorough = PropertiesUtil.getIntPropertyArray(bundle, "age.brackets", new int[]{5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80});

//        jobStringType = PropertiesUtil.getStringPropertyArray(bundle, "employment.types", new String[]{"Agri","Mnft","Util","Cons","Retl","Trns","Finc","Rlst","Admn","Serv"});
        jobStringType = PropertiesUtil.getStringPropertyArray(bundle, "employment.types", new String[]{"Agri","Mnft","Serv"});

        for(String s: jobStringType) {
            System.out.println(s);
        }
        runJobAllocation = PropertiesUtil.getBooleanProperty(bundle, "run.job.allocation", false);


        alphaJob = PropertiesUtil.getDoubleProperty(bundle, "employment.choice.alpha", 50);
        gammaJob = PropertiesUtil.getDoubleProperty(bundle, "employment.choice.gamma", -0.003);
        tripLengthDistributionFileName = PropertiesUtil.getStringProperty(bundle, "trip.length.distribution", "input/syntheticPopulation/tripLengthDistribution.csv");

        schoolTypes = PropertiesUtil.getIntPropertyArray(bundle, "school.types", new int[]{1, 2, 3});
        alphaUniversity = PropertiesUtil.getDoubleProperty(bundle, "university.choice.alpha", 50);
        gammaUniversity = PropertiesUtil.getDoubleProperty(bundle, "university.choice.gamma", -0.003);

        householdSizes = PropertiesUtil.getIntPropertyArray(bundle, "household.size.brackets", new int[]{1, 2, 3, 4, 5});
        numberofQualityLevels = PropertiesUtil.getIntProperty(bundle, "dwelling.quality.levels.distinguished", 4);
        yearBracketsDwelling = PropertiesUtil.getIntPropertyArray(bundle, "dd.year.brackets", new int[]{1, 2, 3, 4, 5});
        sizeBracketsDwelling = PropertiesUtil.getIntPropertyArray(bundle, "dd.size.brackets", new int[]{40, 60, 80,100,120,160,2000});
        bedroomsBracketsDwelling = PropertiesUtil.getIntPropertyArray(bundle, "dd.bedrooms.brackets", new int[]{1, 2, 3, 4, 5, 6,7});

        maxIterations = PropertiesUtil.getIntProperty(bundle, "max.iterations.ipu", 1000);
        maxError = PropertiesUtil.getDoubleProperty(bundle, "max.error.ipu", 0.0001);
        improvementError = PropertiesUtil.getDoubleProperty(bundle, "min.improvement.error.ipu", 0.001);
        iterationError = PropertiesUtil.getDoubleProperty(bundle, "iterations.improvement.ipu", 2);
        increaseError = PropertiesUtil.getDoubleProperty(bundle, "increase.error.ipu", 1.05);
        initialError = PropertiesUtil.getDoubleProperty(bundle, "ini.error.ipu", 1000);

        double incomeShape = PropertiesUtil.getDoubleProperty(bundle, "income.gamma.shape", 1.0737036186);
        double incomeRate = PropertiesUtil.getDoubleProperty(bundle, "income.gamma.rate", 0.0006869439);
        //todo consider to read it from another source e.g. a JS calculator or CSV file
        //this is not a property but a variable?
        incomeGammaDistribution = new GammaDistribution(incomeShape, 1 / incomeRate);

        //todo this properties will be doubled with silo model run properties
        weightsFileName = PropertiesUtil.getStringProperty(bundle, "weights.matrix", "microData/interimFiles/weigthsMatrix.csv");
        errorsBoroughFileName = PropertiesUtil.getStringProperty(bundle, "errors.IPU.borough.matrix", "microData/interimFiles/errorsIPUborough.csv");
        errorsMunicipalityFileName = PropertiesUtil.getStringProperty(bundle, "errors.IPU.municipality.matrix", "microData/interimFiles/errorsIPUmunicipality.csv");
        errorsCountyFileName = PropertiesUtil.getStringProperty(bundle, "errors.IPU.county.matrix", "microData/interimFiles/errorsIPUcounty.csv");

        errorsSummaryFileName = PropertiesUtil.getStringProperty(bundle, "errors.IPU.summary.matrix", "microData/interimFiles/errorsIPUsummary.csv");
        //todo do not need to ride always?
        if (runMicrolocation) {
            buildingLocationlist = SiloUtil.readCSVfile(PropertiesUtil.getStringProperty(bundle, "buildingLocation.list", "input/syntheticPopulation/buildingLocation_2022.csv"));
            jobLocationlist = SiloUtil.readCSVfile(PropertiesUtil.getStringProperty(bundle, "jobLocation.list", "input/syntheticPopulation/jobLocation_5types.csv"));
            schoolLocationlist = SiloUtil.readCSVfile(PropertiesUtil.getStringProperty(bundle, "schoolLocation.list", "input/syntheticPopulation/schoolLocation_2022_crs31468.csv"));
        } else {
            buildingLocationlist = null;
            jobLocationlist = null;
            schoolLocationlist = null;
        }
        zonalDataIPU = null;
        runDisability = PropertiesUtil.getBooleanProperty(bundle, "run.disability", false);
        fullTimeProbabilityTable = SiloUtil.readCSVfile(PropertiesUtil.getStringProperty(bundle, "fullTime.coefficient.table","input/syntheticPopulation/proportionFullTime_5types.csv"));
        fullTimeFileName = PropertiesUtil.getStringProperty(bundle, "fullTime.coefficient.table","input/syntheticPopulation/proportionFullTime_5types.csv");
        durationFileName = PropertiesUtil.getStringProperty(bundle, "duration.coefficient.table","input/syntheticPopulation/mandActDurationDistributionTable.csv");
        startTimeFileName = PropertiesUtil.getStringProperty(bundle, "duration.coefficient.table","input/syntheticPopulation/mandActsStartTimeDistributionByDurationSegmentTable.csv");

        if (boroughIPU) {
            attributesBorough = PropertiesUtil.getStringPropertyArray(bundle, "attributes.borough",null);
            marginalsBorough = SiloUtil.readCSVfile(PropertiesUtil.getStringProperty(bundle, "marginals.borough", "input/syntheticPopulation/marginalsBorough.csv"));
            marginalsBorough.buildIndex(marginalsBorough.getColumnPosition("ID_borough"));
            selectedBoroughs = SiloUtil.readCSVfile(PropertiesUtil.getStringProperty(bundle,"municipalities.list.borough","input/syntheticPopulation/municipalitiesListBorough.csv"));
            selectedBoroughs.buildIndex(selectedBoroughs.getColumnPosition("ID_borough"));
            cellsMatrixBoroughs = SiloUtil.readCSVfile(PropertiesUtil.getStringProperty(bundle,"taz.definition","input/syntheticPopulation/zoneAttributesBorough.csv"));
            cellsMatrixBoroughs.buildIndex(cellsMatrixBoroughs.getColumnPosition("ID_cell"));
        }



    }


}
