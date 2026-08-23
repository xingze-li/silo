package de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022;

import de.tum.bgu.msm.data.MunichDwellingTypes;
import de.tum.bgu.msm.data.accessibility.Accessibility;
import de.tum.bgu.msm.data.accessibility.AccessibilityImpl;
import de.tum.bgu.msm.data.accessibility.CommutingTimeProbability;
import de.tum.bgu.msm.data.accessibility.CommutingTimeProbabilityExponential;
import de.tum.bgu.msm.data.dwelling.DwellingData;
import de.tum.bgu.msm.data.dwelling.DwellingDataImpl;
import de.tum.bgu.msm.data.dwelling.DwellingFactoryImpl;
import de.tum.bgu.msm.data.dwelling.RealEstateDataManager;
import de.tum.bgu.msm.data.dwelling.RealEstateDataManagerImpl;
import de.tum.bgu.msm.data.geo.DefaultGeoData;
import de.tum.bgu.msm.data.geo.GeoData;
import de.tum.bgu.msm.data.household.HouseholdData;
import de.tum.bgu.msm.data.household.HouseholdDataImpl;
import de.tum.bgu.msm.data.household.HouseholdDataManager;
import de.tum.bgu.msm.data.household.HouseholdDataManagerImpl;
import de.tum.bgu.msm.data.household.HouseholdFactoryBerlinBrandenburg;
import de.tum.bgu.msm.data.job.JobData;
import de.tum.bgu.msm.data.job.JobDataImpl;
import de.tum.bgu.msm.data.job.JobDataManager;
import de.tum.bgu.msm.data.job.JobDataManagerWithCommuteModeChoice;
import de.tum.bgu.msm.data.job.JobFactoryBerlinBrandenburg;
import de.tum.bgu.msm.data.job.JobType;
import de.tum.bgu.msm.data.person.PersonFactoryBerlinBrandenburg;
import de.tum.bgu.msm.data.travelTimes.SkimTravelTimes;
import de.tum.bgu.msm.data.travelTimes.TravelTimes;
import de.tum.bgu.msm.io.DwellingWriterBerlinBrandenburg;
import de.tum.bgu.msm.io.GeoDataReaderBerlinBrandenburg;
import de.tum.bgu.msm.io.HouseholdWriterBerlinBrandenburgDisability;
import de.tum.bgu.msm.io.JobWriterBerlinBrandenburg;
import de.tum.bgu.msm.io.PersonWriterBerlinBrandenburg;
import de.tum.bgu.msm.io.input.GeoDataReader;
import de.tum.bgu.msm.io.output.DwellingWriter;
import de.tum.bgu.msm.io.output.HouseholdWriter;
import de.tum.bgu.msm.io.output.JobWriter;
import de.tum.bgu.msm.io.output.PersonWriter;
import de.tum.bgu.msm.models.modeChoice.SimpleCommuteModeChoice;
import de.tum.bgu.msm.properties.Properties;
import de.tum.bgu.msm.schools.DataContainerWithSchools;
import de.tum.bgu.msm.schools.DataContainerWithSchoolsImpl;
import de.tum.bgu.msm.schools.SchoolData;
import de.tum.bgu.msm.schools.SchoolDataImpl;
import de.tum.bgu.msm.schools.SchoolsWriter;
import de.tum.bgu.msm.syntheticPopulationGenerator.SyntheticPopI;
import de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.allocation.Allocation;
import de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.microlocation.GenerateJobMicrolocation;
import de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.microlocation.GenerateSchoolMicrolocation;
import de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.optimization.Optimization;
import de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.preparation.Preparation;
import de.tum.bgu.msm.syntheticPopulationGenerator.properties.PropertiesSynPop;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Generates a synthetic population for a study area in Germany
 * @author Ana Moreno (TUM)
 * Created on May 12, 2016 in Munich
 *
 */
public class SyntheticPopDe implements SyntheticPopI {

    public static final Logger logger = LogManager.getLogger(SyntheticPopDe.class);
    private final DataSetSynPop dataSetSynPop;
    private final Properties properties;

    public SyntheticPopDe(DataSetSynPop dataSetSynPop, Properties properties) {
        this.dataSetSynPop = dataSetSynPop;
        this.properties = properties;
    }


    public void runSP(){
        //method to create the synthetic population at the base year
        logger.info("   Starting to create the synthetic population.");
        createDirectoryForOutput();

        DataContainerWithSchools dataContainer = buildDataContainer();
        readGeography(dataContainer);

        long startTime = System.nanoTime();

        logger.info("Running Module: Reading inputs");
        new Preparation(dataSetSynPop).run();

        logger.info("Running Module: Optimization IPU");
        new Optimization(dataSetSynPop).run();

        logger.info("Running Module: Population allocation");
        new Allocation(dataSetSynPop, dataContainer).run();

        if (PropertiesSynPop.get().main.runJobMicrolocation) {
            if (!PropertiesSynPop.get().main.runJobAllocation) {
                throw new IllegalStateException(
                        "run.job.microlocation=true requires run.job.allocation=true.");
            }
            logger.info("Running Module: Job microlocation");
            new GenerateJobMicrolocation(dataContainer).run();
        }

        if (PropertiesSynPop.get().main.runSchoolMicrolocation) {
            if (!PropertiesSynPop.get().main.runSchoolAllocation) {
                throw new IllegalStateException(
                        "run.school.microlocation=true requires run.school.allocation=true.");
            }
            logger.info("Running Module: School microlocation");
            new GenerateSchoolMicrolocation(dataContainer).run();
        }

        if (PropertiesSynPop.get().main.runAllocation) {
            writePopulation(dataContainer);
        }

        long estimatedTime = System.nanoTime() - startTime;
        logger.info("   Finished Berlin synthetic population generation. Elapsed time: " + estimatedTime);
    }

    private void readGeography(DataContainerWithSchools dataContainer) {
        GeoDataReader reader = new GeoDataReaderBerlinBrandenburg(dataContainer.getGeoData());
        reader.readZoneCsv(properties.main.baseDirectory + properties.geo.zonalDataFile);
        reader.readZoneShapefile(properties.main.baseDirectory + properties.geo.zoneShapeFile);
    }

    private DataContainerWithSchools buildDataContainer() {
        HouseholdData householdData = new HouseholdDataImpl();
        JobData jobData = new JobDataImpl();
        DwellingData dwellingData = new DwellingDataImpl();
        GeoData geoData = new DefaultGeoData();

        TravelTimes travelTimes = new SkimTravelTimes();
        Accessibility accessibility = new AccessibilityImpl(
                geoData, travelTimes, properties, dwellingData, jobData);
        CommutingTimeProbability commutingTimeProbability =
                new CommutingTimeProbabilityExponential(
                        properties.accessibility.betaTimeCarExponentialCommutingTime,
                        properties.accessibility.betaTimePtExponentialCommutingTime);

        new JobType(properties.jobData.jobTypes);

        RealEstateDataManager realEstateDataManager = new RealEstateDataManagerImpl(
                new MunichDwellingTypes(),
                dwellingData,
                householdData,
                geoData,
                new DwellingFactoryImpl(),
                properties);

        JobDataManager jobDataManager = new JobDataManagerWithCommuteModeChoice(
                properties,
                new JobFactoryBerlinBrandenburg(),
                jobData,
                geoData,
                travelTimes,
                commutingTimeProbability,
                new SimpleCommuteModeChoice(
                        commutingTimeProbability,
                        travelTimes,
                        geoData,
                        properties,
                        SiloUtil.provideNewRandom()));

        HouseholdDataManager householdDataManager = new HouseholdDataManagerImpl(
                householdData,
                dwellingData,
                new PersonFactoryBerlinBrandenburg(),
                new HouseholdFactoryBerlinBrandenburg(),
                properties,
                realEstateDataManager);

        SchoolData schoolData = new SchoolDataImpl(geoData, dwellingData, properties);

        return new DataContainerWithSchoolsImpl(
                geoData,
                realEstateDataManager,
                jobDataManager,
                householdDataManager,
                travelTimes,
                accessibility,
                commutingTimeProbability,
                schoolData,
                properties);
    }

    private void writePopulation(DataContainerWithSchools dataContainer) {
        String suffix = "_" + properties.main.baseYear + "P.csv";

        String householdFile = properties.main.baseDirectory
                + properties.householdData.householdFileName + suffix;
        HouseholdWriter householdWriter = new HouseholdWriterBerlinBrandenburgDisability(
                dataContainer.getHouseholdDataManager(),
                dataContainer.getRealEstateDataManager());
        householdWriter.writeHouseholds(householdFile);

        String personFile = properties.main.baseDirectory
                + properties.householdData.personFileName + suffix;
        PersonWriter personWriter = new PersonWriterBerlinBrandenburg(
                dataContainer.getHouseholdDataManager());
        personWriter.writePersons(personFile);

        String dwellingFile = properties.main.baseDirectory
                + properties.realEstate.dwellingsFileName + suffix;
        DwellingWriter dwellingWriter = new DwellingWriterBerlinBrandenburg(dataContainer);
        dwellingWriter.writeDwellings(dwellingFile);

        if (PropertiesSynPop.get().main.runJobAllocation) {
            String jobFile = properties.main.baseDirectory
                    + properties.jobData.jobsFileName + suffix;
            JobWriter jobWriter = new JobWriterBerlinBrandenburg(
                    dataContainer.getJobDataManager());
            jobWriter.writeJobs(jobFile);
        }

        if (PropertiesSynPop.get().main.runSchoolMicrolocation) {
            String schoolFile = properties.main.baseDirectory
                    + properties.schoolData.schoolsFileName + suffix;
            SchoolsWriter schoolsWriter = new SchoolsWriter(dataContainer.getSchoolData());
            schoolsWriter.writeSchools(schoolFile);
        }
    }

    private void createDirectoryForOutput() {
        SiloUtil.createDirectoryIfNotExistingYet(properties.main.baseDirectory + "microData");
        SiloUtil.createDirectoryIfNotExistingYet(
                properties.main.baseDirectory + "microData/interimFiles");
    }

}
