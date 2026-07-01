package de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.allocation;

import com.google.common.math.LongMath;
import de.tum.bgu.msm.container.DataContainer;
import de.tum.bgu.msm.data.job.JobDataManager;
import de.tum.bgu.msm.data.person.Gender;
import de.tum.bgu.msm.data.person.Occupation;
import de.tum.bgu.msm.data.person.Person;
import de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.CoefficientsReader;
import de.tum.bgu.msm.syntheticPopulationGenerator.munich2022.DataSetSynPop;
import de.tum.bgu.msm.syntheticPopulationGenerator.properties.PropertiesSynPop;
import de.tum.bgu.msm.utils.SiloUtil;
import de.tum.bgu.msm.data.job.Job;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class AssignPropertiesToJobs {

    private static final Logger logger = LogManager.getLogger(AssignPropertiesToJobs.class);

    private final DataSetSynPop dataSetSynPop;
    private final DataContainer dataContainer;
    private String[] jobStringTypes;
    JobDataManager jobData;
    Map<String, Map<String, Double>> coefficientsFullTime;
    Map<String, Map<String, Double>> coefficientsDuration;
    Map<String, Map<String, Double>> coefficientsStartTimeWorkday;
    Map<String, Map<String, Double>> coefficientsStartTimeWeekend;


    public AssignPropertiesToJobs(DataContainer dataContainer, DataSetSynPop dataSetSynPop){
        this.dataSetSynPop = dataSetSynPop;
        this.dataContainer = dataContainer;

    }


    public void run() {

        logger.info("   Running module: job properties assignment");

        readCoefficients();

        jobData = dataContainer.getJobDataManager();

        int assignedJobProperties = 0;
        int skippedNoJob = 0;

        for (Person pp : dataContainer.getHouseholdDataManager().getPersons()) {

            if (pp.getOccupation() != Occupation.EMPLOYED) {
                setNoJobProperties(pp);
                continue;
            }

            boolean hasValidJob = setFullOrPartTime(pp);

            if (!hasValidJob) {
                setNoJobProperties(pp);
                skippedNoJob++;
                continue;
            }

            setDurationAndStartTime(pp);

            assignedJobProperties++;

            if (LongMath.isPowerOfTwo(assignedJobProperties)) {
                logger.info("   Assigned job properties to " + assignedJobProperties + " persons.");
            }
        }

        logger.info("   Finished job properties assignment.");
        logger.info("   Assigned job properties to employed persons with valid jobs: " + assignedJobProperties);
        logger.info("   Skipped employed persons without valid assigned jobs: " + skippedNoJob);
    }

    private boolean setFullOrPartTime(Person pp) {

        int jobId = pp.getJobId();

        if (jobId <= 0) {
            return false;
        }

        Job job = jobData.getJobFromId(jobId);

        if (job == null) {
            logger.warn("Person " + pp.getId() +
                    " has jobId " + jobId +
                    ", but no corresponding job object was found.");
            return false;
        }

        String jobType = job.getType().trim();

        Map<String, Double> coefficientsByWorkerCategory = coefficientsFullTime.get(jobType);

        if (coefficientsByWorkerCategory == null) {
            logger.warn("No full-time coefficients found for job type " + jobType +
                    ". Person " + pp.getId() + " is assigned fullTime by default.");
            pp.setAttribute("jobDurationType", "fullTime");
            return true;
        }

        String workerCategory = getWorkerCategory(pp);

        Double probability = coefficientsByWorkerCategory.get(workerCategory);

        if (probability == null) {
            logger.warn("No full-time probability found for job type " + jobType +
                    " and worker category " + workerCategory +
                    ". Person " + pp.getId() + " is assigned fullTime by default.");
            pp.setAttribute("jobDurationType", "fullTime");
            return true;
        }

        if (probability > SiloUtil.getRandomNumberAsDouble()) {
            pp.setAttribute("jobDurationType", "fullTime");
        } else {
            pp.setAttribute("jobDurationType", "partTime");
        }

        return true;
    }

    private void setNoJobProperties(Person pp) {
        pp.setAttribute("jobDurationType", "0");
        pp.setAttribute("jobDuration", "0");
        pp.setAttribute("jobStartTimeWorkday", "0");
        pp.setAttribute("jobStartTimeWeekend", "0");
    }

    private void setDurationAndStartTime(Person pp) {

        String durationType = pp.getAttribute("jobDurationType")
                .map(Object::toString)
                .orElse("0");

        Map<String, Double> durationDistribution = coefficientsDuration.get(durationType);

        if (durationDistribution == null) {
            setNoJobProperties(pp);
            return;
        }

        String durationInMinutes = SiloUtil.select(durationDistribution);

        pp.setAttribute("jobDuration", durationInMinutes);

        String durationKey;

        if (Integer.parseInt(durationInMinutes) < 3 * 60) {
            durationKey = "0_3";
        } else if (Integer.parseInt(durationInMinutes) < 6 * 60) {
            durationKey = "4_6";
        } else if (Integer.parseInt(durationInMinutes) < 10 * 60) {
            durationKey = "7_10";
        } else {
            durationKey = "11+";
        }

        Map<String, Double> startTimeWorkdayDistribution =
                coefficientsStartTimeWorkday.get(durationKey);

        Map<String, Double> startTimeWeekendDistribution =
                coefficientsStartTimeWeekend.get(durationKey);

        if (startTimeWorkdayDistribution == null || startTimeWeekendDistribution == null) {
            pp.setAttribute("jobStartTimeWorkday", "480");
            pp.setAttribute("jobStartTimeWeekend", "0");
            return;
        }

        String startTimeWorkdayInMinutes = SiloUtil.select(startTimeWorkdayDistribution);
        String startTimeWeekendInMinutes = SiloUtil.select(startTimeWeekendDistribution);

        pp.setAttribute("jobStartTimeWorkday", startTimeWorkdayInMinutes);
        pp.setAttribute("jobStartTimeWeekend", startTimeWeekendInMinutes);

        int jobId = pp.getJobId();

        if (jobId > 0) {
            Job job = jobData.getJobFromId(jobId);

            if (job != null) {
                int startTimeWorkdaySeconds = Integer.parseInt(startTimeWorkdayInMinutes) * 60;
                int durationSeconds = Integer.parseInt(durationInMinutes) * 60;

                job.setAttribute("startTimeInSeconds", startTimeWorkdaySeconds);
                job.setAttribute("workingTimeInSeconds", durationSeconds);
            }
        }
    }

    private void readCoefficients() {
        /*coefficients = PropertiesSynPop.get().main.fullTimeProbabilityTable;
        coefficients.buildStringIndex(1);*/
        coefficientsFullTime = new HashMap<>();
        coefficientsDuration = new HashMap<>();
        coefficientsStartTimeWeekend = new HashMap<>();
        coefficientsStartTimeWorkday = new HashMap<>();

        for (String jobType : PropertiesSynPop.get().main.jobStringType) {
            Map<String, Double> coefficientsByJobType =
                    new CoefficientsReader(dataSetSynPop, jobType,
                            Path.of(PropertiesSynPop.get().main.fullTimeFileName)).readCoefficients();
            coefficientsFullTime.putIfAbsent(jobType, coefficientsByJobType);
        }
        coefficientsDuration.putIfAbsent("fullTime", new CoefficientsReader(dataSetSynPop, "duration_workFullTime",
                Path.of(PropertiesSynPop.get().main.durationFileName)).readCoefficients());

        coefficientsDuration.put("partTime", new CoefficientsReader(dataSetSynPop, "duration_workHalfTime",
                Path.of(PropertiesSynPop.get().main.durationFileName)).readCoefficients());

        String[] durationSegments = {"0_3","4_6","7_10","11+"};
        for (String duration : durationSegments){
            String durationWorkday = "work_wkday_duration_" + duration;
            coefficientsStartTimeWorkday.putIfAbsent(duration, new CoefficientsReader(dataSetSynPop, durationWorkday,
                    Path.of(PropertiesSynPop.get().main.startTimeFileName)).readCoefficients());

            String durationWeekend = "work_wkend_duration_" + duration;
            coefficientsStartTimeWeekend.putIfAbsent(duration, new CoefficientsReader(dataSetSynPop, durationWeekend,
                    Path.of(PropertiesSynPop.get().main.startTimeFileName)).readCoefficients());
        }
    }

    private String getWorkerCategory(Person person) {

        String category = "";
            if (person.getAnnualIncome() < 500*12){
                category = "1";
                if (person.getGender().equals(Gender.MALE)) {
                    category = category + "_M_1";
                } else {
                    category = category + "_F_";
                    if (person.getAge() < 31){
                        category = category + "2";
                    } else if (person.getAge() < 51){
                        category = category + "3";
                    } else {
                        category = category + "4";
                    }
                }
            } else if (person.getAnnualIncome() < 900*12){
                category = "2";
                if (person.getGender().equals(Gender.MALE)) {
                    category = category + "_M_1";
                } else {
                    category = category + "_F_";
                    if (person.getAge() < 31){
                        category = category + "2";
                    } else if (person.getAge() < 51){
                        category = category + "3";
                    } else {
                        category = category + "4";
                    }
                }
            } else if (person.getAnnualIncome() < 1100*12){
                category = "3";
                if (person.getGender().equals(Gender.MALE)) {
                    category = category + "_M_1";
                } else {
                    category = category + "_F_";
                    if (person.getAge() < 31){
                        category = category + "2";
                    } else if (person.getAge() < 51){
                        category = category + "3";
                    } else {
                        category = category + "4";
                    }
                }
            } else if (person.getAnnualIncome() < 1300*12){
                category = "4";
                if (person.getGender().equals(Gender.MALE)) {
                    category = category + "_M_1";
                } else {
                    category = category + "_F_";
                    if (person.getAge() < 31){
                        category = category + "2";
                    } else if (person.getAge() < 51){
                        category = category + "3";
                    } else {
                        category = category + "4";
                    }
                }
            } else if (person.getAnnualIncome() < 1500*12){
                category = "5";
                if (person.getGender().equals(Gender.MALE)) {
                    category = category + "_M_1";
                } else {
                    category = category + "_F_";
                    if (person.getAge() < 31){
                        category = category + "2";
                    } else if (person.getAge() < 51){
                        category = category + "3";
                    } else {
                        category = category + "4";
                    }
                }
            } else {
                category = "6";
                if (person.getGender().equals(Gender.MALE)) {
                    category = category + "_M_1";
                } else {
                    category = category + "_F_";
                    if (person.getAge() < 31){
                        category = category + "2";
                    } else if (person.getAge() < 51){
                        category = category + "3";
                    } else {
                        category = category + "4";
                    }
                }
            }


        return category;
    }



}
