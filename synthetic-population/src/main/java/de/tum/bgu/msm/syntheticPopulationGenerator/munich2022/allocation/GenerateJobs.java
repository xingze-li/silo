package de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.allocation;

import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.container.DataContainer;
import de.tum.bgu.msm.data.job.JobDataManager;
import de.tum.bgu.msm.data.job.JobUtils;
import de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.DataSetSynPop;
import de.tum.bgu.msm.syntheticPopulationGenerator.properties.PropertiesSynPop;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class GenerateJobs {

    private static final Logger logger = LogManager.getLogger(GenerateJobs.class);

    private final DataSetSynPop dataSetSynPop;
    private final DataContainer dataContainer;

    public GenerateJobs(DataContainer dataContainer, DataSetSynPop dataSetSynPop) {
        this.dataSetSynPop = dataSetSynPop;
        this.dataContainer = dataContainer;
    }

    public void run() {

        logger.info("   Running module: job generation");

        JobDataManager jobData = dataContainer.getJobDataManager();
        TableDataSet zoneAttributes = getZoneAttributes();

        int totalGeneratedJobs = 0;
        Map<String, Integer> generatedJobsByType = new HashMap<>();

        for (int municipality : dataSetSynPop.getMunicipalities()) {

            logger.info("   Municipality " + municipality + ". Starting to generate jobs");

            int[] tazs = dataSetSynPop.getTazByMunicipality().get(municipality);

            if (tazs == null || tazs.length == 0) {
                logger.warn("   No TAZs found for municipality " + municipality + ". No jobs generated.");
                continue;
            }

            int generatedJobsInMunicipality = 0;

            for (int taz : tazs) {

                for (String rawJobType : PropertiesSynPop.get().main.jobStringType) {

                    String jobType = rawJobType.trim();

                    int numberOfJobs = readZoneJobsForType(zoneAttributes, taz, jobType);

                    if (numberOfJobs <= 0) {
                        continue;
                    }

                    for (int j = 0; j < numberOfJobs; j++) {

                        int id = jobData.getNextJobId();

                        jobData.addJob(
                                JobUtils.getFactory().createJob(
                                        id,
                                        taz,
                                        null,
                                        -1,
                                        jobType
                                )
                        );
                    }

                    totalGeneratedJobs += numberOfJobs;
                    generatedJobsInMunicipality += numberOfJobs;
                    generatedJobsByType.merge(jobType, numberOfJobs, Integer::sum);
                }
            }

            logger.info("   Municipality " + municipality +
                    ". Generated jobs: " + generatedJobsInMunicipality);
        }

        logger.info("   Finished job generation. Total generated jobs: " + totalGeneratedJobs);
        logger.info("   Generated jobs by type: " + generatedJobsByType);
    }

    private TableDataSet getZoneAttributes() {

        if (PropertiesSynPop.get().main.boroughIPU) {
            return PropertiesSynPop.get().main.cellsMatrixBoroughs;
        }

        return PropertiesSynPop.get().main.cellsMatrix;
    }

    private int readZoneJobsForType(TableDataSet zoneAttributes, int taz, String jobType) {

        if (!hasColumn(zoneAttributes, jobType)) {
            throw new RuntimeException(
                    "Missing job column in zoneAttributes: " + jobType +
                            ". Check employment.types and zoneAttributes column names."
            );
        }

        return Math.max(
                0,
                Math.round(zoneAttributes.getIndexedValueAt(taz, jobType))
        );
    }

    private boolean hasColumn(TableDataSet table, String columnName) {

        try {
            table.getColumnPosition(columnName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
