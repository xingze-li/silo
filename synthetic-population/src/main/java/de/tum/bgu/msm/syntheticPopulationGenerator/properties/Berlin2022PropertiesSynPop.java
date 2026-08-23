package de.tum.bgu.msm.syntheticPopulationGenerator.properties;

import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.properties.PropertiesUtil;
import de.tum.bgu.msm.io.GeoDataReaderBerlinBrandenburg;
import de.tum.bgu.msm.utils.CSVFileReader2;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.commons.math3.distribution.GammaDistribution;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.ResourceBundle;

public class Berlin2022PropertiesSynPop extends AbstractPropertiesSynPop {

    private static final String[] DEFAULT_ATTRIBUTES_MUNICIPALITY = {
            "d.year.before1950", "d.year.1950to1989", "d.year.1990to2009",
            "d.use.owned", "d.use.rented",
            "d.type.detached", "d.type.semiDetached", "d.type.terraced", "d.type.MFH3to6Dwelling",
            "h.senior.twoOrMore", "h.senior.mixed", "h.senior.single",
            "h.type.couples", "h.type.singleWithChildren", "h.type.couplesWithChildren",
            "p.foreigners",
            "p.sexAge.male10", "p.sexAge.female10",
            "p.sexAge.male20", "p.sexAge.female20",
            "p.sexAge.male30", "p.sexAge.female30",
            "p.sexAge.male40", "p.sexAge.female40",
            "p.sexAge.male50", "p.sexAge.female50",
            "p.sexAge.male60", "p.sexAge.female60",
            "p.sexAge.male70", "p.sexAge.female70",
            "p.sexAge.male80", "p.sexAge.female80",
            "h.size.1", "h.size.2", "h.size.3", "h.size.4", "h.size.5",
            "hhTotal", "population"
    };

    public Berlin2022PropertiesSynPop(ResourceBundle bundle) {

        PropertiesUtil.newPropertySubmodule("SP: Berlin 2022 main properties");

        microDataFile = PropertiesUtil.getStringProperty(bundle, "micro.data", "input/syntheticPopulation/microData2022Full.csv");

        microPersonsFileName = PropertiesUtil.getStringProperty(bundle, "micro.persons", "microData/interimFiles/microPersons.csv");
        microHouseholdsFileName = PropertiesUtil.getStringProperty(bundle, "micro.households", "microData/interimFiles/microHouseholds.csv");
        microDwellingsFileName = PropertiesUtil.getStringProperty(bundle, "micro.dwellings", "microData/interimFiles/microDwellings.csv");
        frequencyMatrixFileName = PropertiesUtil.getStringProperty(bundle, "frequency.matrix", "microData/interimFiles/frequencyMatrix.csv");

        runIPU = PropertiesUtil.getBooleanProperty(bundle, "run.ipu.synthetic.pop", true);
        runAllocation = PropertiesUtil.getBooleanProperty(bundle, "run.population.allocation", false);
        runMicrolocation = PropertiesUtil.getBooleanProperty(bundle, "run.sp.microlocation", true);
        runJobMicrolocation = PropertiesUtil.getBooleanProperty(bundle, "run.job.microlocation", false);
        runSchoolMicrolocation = PropertiesUtil.getBooleanProperty(bundle, "run.school.microlocation", false);
        runJobAllocation = PropertiesUtil.getBooleanProperty(bundle, "run.job.allocation", true);
        runSchoolAllocation = PropertiesUtil.getBooleanProperty(
                bundle,
                "run.school.allocation",
                runSchoolMicrolocation
        );
        runDisability = PropertiesUtil.getBooleanProperty(bundle, "run.disability", false);

        twoGeographicalAreasIPU = PropertiesUtil.getBooleanProperty(bundle, "run.ipu.city.and.county", false);
        boroughIPU = PropertiesUtil.getBooleanProperty(bundle, "run.three.areas", false);

        attributesMunicipality = PropertiesUtil.getStringPropertyArray(
                bundle,
                "attributes.municipality",
                DEFAULT_ATTRIBUTES_MUNICIPALITY
        );

        marginalsMunicipality = SiloUtil.readCSVfile(PropertiesUtil.getStringProperty(
                bundle,
                "marginals.municipality",
                "input/syntheticPopulation/input2022/marginalsMunicipality.csv"
        ));
        marginalsMunicipality.buildIndex(marginalsMunicipality.getColumnPosition("ID_city"));

        if (twoGeographicalAreasIPU) {
            attributesCounty = PropertiesUtil.getStringPropertyArray(bundle, "attributes.county", new String[]{"hhTotal", "population"});
            marginalsCounty = SiloUtil.readCSVfile(PropertiesUtil.getStringProperty(
                    bundle,
                    "marginals.county",
                    "input/syntheticPopulation/input2022/marginalsCounty.csv"
            ));
            marginalsCounty.buildIndex(marginalsCounty.getColumnPosition("ID_county"));
        }

        selectedMunicipalities = SiloUtil.readCSVfile(PropertiesUtil.getStringProperty(bundle,"municipalities.list","input/syntheticPopulation/input2022/municipalitiesList.csv"));
        selectedMunicipalities.buildIndex(selectedMunicipalities.getColumnPosition("ID_city"));

        if (runAllocation || runMicrolocation || runJobMicrolocation ||
                runSchoolAllocation || runSchoolMicrolocation) {
            cellsMatrix = GeoDataReaderBerlinBrandenburg.readZoneTable(PropertiesUtil.getStringProperty(
                    bundle,
                    "taz.definition",
                    "input/syntheticPopulation/input2022_zone/zoneAttributes_Berlin2022_5types_with_school_capacity.csv"
            ));
            cellsMatrix.buildIndex(findColumn(cellsMatrix, "ID_cell", "Zone"));
        }

        omxFileName = PropertiesUtil.getStringProperty(bundle, "distanceODmatrix", "input/syntheticPopulation/skim_car.parquet");

        ageBracketsPerson = PropertiesUtil.getIntPropertyArray(bundle, "age.brackets", new int[]{10, 20, 30, 40, 50, 60, 70, 80});
        ageBracketsPersonQuarter = PropertiesUtil.getIntPropertyArray(bundle, "age.brackets.quarters", new int[]{74, 84, 94, 120});
        ageBracketsBorough = ageBracketsPerson;

        jobStringType = PropertiesUtil.getStringPropertyArray(bundle, "employment.types", new String[]{"Agri", "Manu", "Retail", "Business", "Serv"});

        alphaJob = PropertiesUtil.getDoubleProperty(bundle, "employment.choice.alpha", 50);
        gammaJob = PropertiesUtil.getDoubleProperty(bundle, "employment.choice.gamma", -0.003);
        tripLengthDistributionFileName = PropertiesUtil.getStringProperty(bundle, "trip.length.distribution", "input/syntheticPopulation/tripLengthDistribution.csv");
        tripLengthDistributionMunichFileName = PropertiesUtil.getStringProperty(bundle, "trip.length.distribution.munich", tripLengthDistributionFileName);
        tripLengthDistributionAugsburgFileName = PropertiesUtil.getStringProperty(bundle, "trip.length.distribution.augsburg", tripLengthDistributionFileName);
        tripLengthDistributionOtherFileName = PropertiesUtil.getStringProperty(bundle, "trip.length.distribution.other", tripLengthDistributionFileName);

        schoolTypes = PropertiesUtil.getIntPropertyArray(bundle, "school.types", new int[]{1, 2, 3});
        alphaUniversity = PropertiesUtil.getDoubleProperty(bundle, "university.choice.alpha", 50);
        gammaUniversity = PropertiesUtil.getDoubleProperty(bundle, "university.choice.gamma", -0.003);

        householdSizes = PropertiesUtil.getIntPropertyArray(bundle, "household.size.brackets", new int[]{1, 2, 3, 4, 5});
        numberofQualityLevels = PropertiesUtil.getIntProperty(bundle, "dwelling.quality.levels.distinguished", 4);
        yearBracketsDwelling = PropertiesUtil.getIntPropertyArray(bundle, "dd.year.brackets", new int[]{1, 2, 3, 4, 5});
        sizeBracketsDwelling = PropertiesUtil.getIntPropertyArray(bundle, "dd.size.brackets", new int[]{40, 60, 80, 100, 120, 160, 2000});
        bedroomsBracketsDwelling = PropertiesUtil.getIntPropertyArray(bundle, "dd.bedrooms.brackets", new int[]{1, 2, 3, 4, 5, 6, 7});

        maxIterations = PropertiesUtil.getIntProperty(bundle, "max.iterations.ipu", 1000);
        maxError = PropertiesUtil.getDoubleProperty(bundle, "max.error.ipu", 0.0001);
        improvementError = PropertiesUtil.getDoubleProperty(bundle, "min.improvement.error.ipu", 0.001);
        iterationError = PropertiesUtil.getDoubleProperty(bundle, "iterations.improvement.ipu", 2);
        increaseError = PropertiesUtil.getDoubleProperty(bundle, "increase.error.ipu", 1.05);
        initialError = PropertiesUtil.getDoubleProperty(bundle, "ini.error.ipu", 1000);

        double incomeShape = PropertiesUtil.getDoubleProperty(bundle, "income.gamma.shape", 1.0737036186);
        double incomeRate = PropertiesUtil.getDoubleProperty(bundle, "income.gamma.rate", 0.0006869439);
        incomeGammaDistribution = new GammaDistribution(incomeShape, 1 / incomeRate);

        weightsFileName = PropertiesUtil.getStringProperty(bundle, "weights.matrix", "microData/interimFiles/weightsMatrix.csv");
        errorsBoroughFileName = PropertiesUtil.getStringProperty(bundle, "errors.IPU.borough.matrix", "microData/interimFiles/errorsIPUborough.csv");
        errorsMunicipalityFileName = PropertiesUtil.getStringProperty(bundle, "errors.IPU.municipality.matrix", "microData/interimFiles/errorsIPUmunicipality.csv");
        errorsCountyFileName = PropertiesUtil.getStringProperty(bundle, "errors.IPU.county.matrix", "microData/interimFiles/errorsIPUcounty.csv");
        errorsSummaryFileName = PropertiesUtil.getStringProperty(bundle, "errors.IPU.summary.matrix", "microData/interimFiles/errorsIPUsummary.csv");

        if (runMicrolocation) {
            buildingLocationlist = SiloUtil.readCSVfile(PropertiesUtil.getStringProperty(bundle, "buildingLocation.list", "input/syntheticPopulation/buildingLocation_2022.csv"));
        }
        if (runJobMicrolocation) {
            jobLocationlist = readJobLocationList(PropertiesUtil.getStringProperty(
                    bundle,
                    "jobLocation.list",
                    "input/syntheticPopulation/jobLocation_5types.csv"
            ));
        }
        if (runSchoolMicrolocation) {
            schoolLocationlist = readSchoolLocationList(PropertiesUtil.getStringProperty(
                    bundle,
                    "schoolLocation.list",
                    "input/syntheticPopulation/schoolLocation_2022_crs31468.csv"
            ));
        }

        if (boroughIPU) {
            attributesBorough = PropertiesUtil.getStringPropertyArray(bundle, "attributes.borough", null);
            marginalsBorough = SiloUtil.readCSVfile(PropertiesUtil.getStringProperty(bundle, "marginals.borough", "input/syntheticPopulation/marginalsBorough.csv"));
            marginalsBorough.buildIndex(marginalsBorough.getColumnPosition("ID_borough"));
            selectedBoroughs = SiloUtil.readCSVfile(PropertiesUtil.getStringProperty(bundle, "municipalities.list.borough", "input/syntheticPopulation/municipalitiesListBorough.csv"));
            selectedBoroughs.buildIndex(selectedBoroughs.getColumnPosition("ID_borough"));
            cellsMatrixBoroughs = SiloUtil.readCSVfile(PropertiesUtil.getStringProperty(bundle, "taz.definition.borough", "input/syntheticPopulation/zoneAttributesBorough.csv"));
            cellsMatrixBoroughs.buildIndex(cellsMatrixBoroughs.getColumnPosition("ID_cell"));
        }

        zonalDataIPU = null;
        fullTimeProbabilityTable = null;
        fullTimeFileName = PropertiesUtil.getStringProperty(bundle, "fullTime.coefficient.table", "input/syntheticPopulation/proportionFullTime_5types.csv");
        durationFileName = PropertiesUtil.getStringProperty(bundle, "duration.coefficient.table", "input/syntheticPopulation/mandActDurationDistributionTable.csv");
        startTimeFileName = PropertiesUtil.getStringProperty(bundle, "start.time.coefficient.table", "input/syntheticPopulation/mandActsStartTimeDistributionByDurationSegmentTable.csv");
    }

    private TableDataSet readJobLocationList(String fileName) {
        String[] columnFormats = {
                "NUMBER", "STRING", "STRING", "NUMBER", "NUMBER", "NUMBER",
                "NUMBER", "NUMBER", "NUMBER", "NUMBER", "NUMBER"
        };

        try {
            return new CSVFileReader2().readFileWithFormats(new File(fileName), columnFormats);
        } catch (IOException | RuntimeException e) {
            throw new RuntimeException("Error reading Berlin job-location file " + fileName, e);
        }
    }

    private TableDataSet readSchoolLocationList(String fileName) {
        String[] columnFormats = {
                "NUMBER", "NUMBER", "NUMBER", "NUMBER", "NUMBER", "NUMBER"
        };

        try {
            return new CSVFileReader2().readFileWithFormats(new File(fileName), columnFormats);
        } catch (IOException | RuntimeException e) {
            throw new RuntimeException("Error reading Berlin school-location file " + fileName, e);
        }
    }

    private int findColumn(de.tum.bgu.msm.common.datafile.TableDataSet table, String... candidates) {
        java.util.List<String> labels = Arrays.asList(table.getColumnLabels());
        for (String candidate : candidates) {
            if (labels.contains(candidate)) {
                return table.getColumnPosition(candidate);
            }
        }
        throw new IllegalArgumentException(
                "TAZ definition must contain one of these columns: " + String.join(", ", candidates));
    }
}
