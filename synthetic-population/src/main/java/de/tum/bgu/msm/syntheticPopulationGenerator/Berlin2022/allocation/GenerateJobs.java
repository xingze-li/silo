package de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022.allocation;

import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.container.DataContainer;
import de.tum.bgu.msm.data.job.JobDataManager;
import de.tum.bgu.msm.data.job.JobUtils;
import de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.DataSetSynPop;
import de.tum.bgu.msm.syntheticPopulationGenerator.properties.PropertiesSynPop;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

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
        logger.info("   Generating jobs for all zones in the research-area zoneAttributes.");

        JobDataManager jobData =
                dataContainer.getJobDataManager();

        TableDataSet zoneAttributes =
                getZoneAttributes();

        String[] jobTypes =
                Arrays.stream(PropertiesSynPop.get().main.jobStringType)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toArray(String[]::new);

        int totalGeneratedJobs =
                0;

        int zonesWithJobs =
                0;

        Map<String, Integer> generatedJobsByType =
                new TreeMap<>();

        Map<Integer, Integer> generatedJobsByMunicipality =
                new TreeMap<>();

        String municipalityColumn =
                PropertiesSynPop.get().main.boroughIPU
                        ? "ID_borough"
                        : "ID_city";

        for (int row = 1; row <= zoneAttributes.getRowCount(); row++) {

            int taz =
                    Math.round(
                            zoneAttributes.getValueAt(
                                    row,
                                    "ID_cell"
                            )
                    );

            int municipality =
                    Math.round(
                            zoneAttributes.getValueAt(
                                    row,
                                    municipalityColumn
                            )
                    );

            int generatedJobsInZone =
                    0;

            for (String jobType : jobTypes) {

                int numberOfJobs =
                        readZoneJobsForTypeByRow(
                                zoneAttributes,
                                row,
                                jobType
                        );

                if (numberOfJobs <= 0) {
                    continue;
                }

                for (int j = 0; j < numberOfJobs; j++) {

                    int id =
                            jobData.getNextJobId();

                    jobData.addJob(
                            JobUtils
                                    .getFactory()
                                    .createJob(
                                            id,
                                            taz,
                                            null,
                                            -1,
                                            jobType
                                    )
                    );
                }

                totalGeneratedJobs += numberOfJobs;
                generatedJobsInZone += numberOfJobs;

                generatedJobsByType.merge(
                        jobType,
                        numberOfJobs,
                        Integer::sum
                );

                generatedJobsByMunicipality.merge(
                        municipality,
                        numberOfJobs,
                        Integer::sum
                );
            }

            if (generatedJobsInZone > 0) {
                zonesWithJobs++;
            }
        }

        logger.info(
                "   Finished job generation for all research-area zones. Total generated jobs: " +
                        totalGeneratedJobs
        );

        logger.info(
                "   Zones with generated jobs: " +
                        zonesWithJobs +
                        " / " +
                        zoneAttributes.getRowCount()
        );

        logger.info(
                "   Generated jobs by type: " +
                        generatedJobsByType
        );

        logger.info(
                "   Number of municipalities with generated jobs: " +
                        generatedJobsByMunicipality.size()
        );

        logger.info(
                "   Generated jobs by municipality: " +
                        generatedJobsByMunicipality
        );
    }

    private int readZoneJobsForTypeByRow(
            TableDataSet zoneAttributes,
            int row,
            String jobType
    ) {
        if (!hasColumn(zoneAttributes, jobType)) {
            throw new RuntimeException(
                    "Missing job column in zoneAttributes: " +
                            jobType +
                            ". Check employment.types and zoneAttributes column names."
            );
        }

        return Math.max(
                0,
                Math.round(
                        zoneAttributes.getValueAt(
                                row,
                                jobType
                        )
                )
        );
    }

    private TableDataSet getZoneAttributes() {

        if (PropertiesSynPop.get().main.boroughIPU) {
            return PropertiesSynPop.get().main.cellsMatrixBoroughs;
        }

        return PropertiesSynPop.get().main.cellsMatrix;
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
