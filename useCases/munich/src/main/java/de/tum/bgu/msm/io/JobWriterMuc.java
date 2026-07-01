package de.tum.bgu.msm.io;

import de.tum.bgu.msm.data.job.Job;
import de.tum.bgu.msm.data.job.JobDataManager;
import de.tum.bgu.msm.data.job.JobMuc;
import de.tum.bgu.msm.io.output.JobWriter;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;

import java.io.PrintWriter;

public class JobWriterMuc implements JobWriter {

    private final static Logger logger = LogManager.getLogger(JobWriterMuc.class);

    private final JobDataManager jobDataManager;

    public JobWriterMuc(JobDataManager dataContainer) {
        this.jobDataManager = dataContainer;
    }

    @Override
    public void writeJobs(String path) {
        logger.info("  Writing job file to " + path);
        PrintWriter pwj = SiloUtil.openFileForSequentialWriting(path, false);
        pwj.print("id,zone,personId,type");
        pwj.print(",");
        pwj.print("coordX");
        pwj.print(",");
        pwj.print("coordY");
        pwj.print(",");
        pwj.print("startTime");
        pwj.print(",");
        pwj.print("duration");
        pwj.println();
        for (Job jj : jobDataManager.getJobs()) {
            pwj.print(jj.getId());
            pwj.print(",");
            pwj.print(jj.getZoneId());
            pwj.print(",");
            pwj.print(jj.getWorkerId());
            pwj.print(",");
            pwj.print(jj.getType());
            pwj.print(",");

//            Coordinate coordinate = jj.getCoordinate();
//            pwj.print(",");
//            pwj.print(coordinate.x);
//            pwj.print(",");
//            pwj.print(coordinate.y);

            Coordinate coordinate = jj.getCoordinate();

            if (coordinate == null) {
                pwj.print("-1");
                pwj.print(",");
                pwj.print("-1");
            } else {
                pwj.print(coordinate.x);
                pwj.print(",");
                pwj.print(coordinate.y);
            }

            pwj.print(",");
            pwj.print(getJobAttributeOrDefault(jj, "startTimeInSeconds", "-1"));
            pwj.print(",");
            pwj.print(getJobAttributeOrDefault(jj, "workingTimeInSeconds", "-1"));

            pwj.println();
            if (jj.getId() == SiloUtil.trackJj) {
                SiloUtil.trackingFile("Writing jj " + jj.getId() + " to micro data file.");
                SiloUtil.trackWriter.println(jj.toString());
            }
        }
        pwj.close();
    }

    private String getJobAttributeOrDefault(Job job, String key, String defaultValue) {
        return job.getAttribute(key)
                .map(Object::toString)
                .orElse(defaultValue);
    }
}

