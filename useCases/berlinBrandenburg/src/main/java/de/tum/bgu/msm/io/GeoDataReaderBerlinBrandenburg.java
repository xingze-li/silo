package de.tum.bgu.msm.io;

import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.data.AreaTypes;
import de.tum.bgu.msm.data.Region;
import de.tum.bgu.msm.data.geo.GeoData;
import de.tum.bgu.msm.data.geo.RegionImpl;
import de.tum.bgu.msm.data.geo.ZoneBerlinBrandenburg;
import de.tum.bgu.msm.io.input.GeoDataReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.matsim.core.utils.gis.ShapeFileReader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GeoDataReaderBerlinBrandenburg implements GeoDataReader {

    private static Logger logger = LogManager.getLogger(GeoDataReaderBerlinBrandenburg.class);

    private GeoData geoDataMuc;

    private final String SHAPE_IDENTIFIER = "id";
    private final String ZONE_ID_COLUMN = "Zone";

    public GeoDataReaderBerlinBrandenburg(GeoData geoDataMuc) {
        this.geoDataMuc = geoDataMuc;
    }

    @Override
    public void readZoneCsv(String path) {
        TableDataSet zonalData = readZoneTable(path);
        int[] zoneIds = zonalData.getColumnAsInt(ZONE_ID_COLUMN);
        float[] zoneAreas = zonalData.getColumnAsFloat(findColumn(zonalData, "Area", "Area_km2"));

        double[] ptDistances = zonalData.getColumnAsDouble("distanceToTransit");

        int[] areaTypes = zonalData.getColumnAsInt(findColumn(zonalData, "BBSR_Type", "BBSR"));

        int[] regionColumn = zonalData.getColumnAsInt("Region");

        for (int i = 0; i < zoneIds.length; i++) {
            AreaTypes.SGType type = AreaTypes.SGType.valueOf(areaTypes[i]);
            Region region;
            int regionId = regionColumn[i];
            if (geoDataMuc.getRegions().containsKey(regionId)) {
                region = geoDataMuc.getRegions().get(regionId);
            } else {
                region = new RegionImpl(regionId);
                geoDataMuc.addRegion(region);
            }
            ZoneBerlinBrandenburg zone = new ZoneBerlinBrandenburg(zoneIds[i], zoneAreas[i], type, ptDistances[i], region);
            region.addZone(zone);
            geoDataMuc.addZone(zone);
        }
    }

    /**
     * Reads Berlin zone CSV files while preserving empty fields. The legacy
     * CSVFileReader drops trailing empty values and fails on the current 2022
     * zone inputs.
     */
    public static TableDataSet readZoneTable(String path) {
        try {
            List<String> lines = Files.readAllLines(Path.of(path), StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                throw new IllegalArgumentException("Zone CSV is empty: " + path);
            }

            List<String> labels = parseCsvLine(lines.get(0));
            List<List<String>> rows = new ArrayList<>(Math.max(0, lines.size() - 1));
            for (int lineNumber = 1; lineNumber < lines.size(); lineNumber++) {
                List<String> values = parseCsvLine(lines.get(lineNumber));
                while (values.size() < labels.size()) {
                    values.add("");
                }
                if (values.size() > labels.size()) {
                    throw new IllegalArgumentException(
                            "Too many columns on line " + (lineNumber + 1) + " in " + path);
                }
                rows.add(values);
            }

            TableDataSet table = new TableDataSet();
            for (int column = 0; column < labels.size(); column++) {
                boolean numeric = true;
                for (List<String> row : rows) {
                    String value = row.get(column).trim();
                    if (!value.isEmpty()) {
                        try {
                            Float.parseFloat(value);
                        } catch (NumberFormatException exception) {
                            numeric = false;
                            break;
                        }
                    }
                }

                String label = labels.get(column).replace("\uFEFF", "").trim();
                if (numeric) {
                    float[] values = new float[rows.size()];
                    for (int row = 0; row < rows.size(); row++) {
                        String value = rows.get(row).get(column).trim();
                        values[row] = value.isEmpty() ? 0f : Float.parseFloat(value);
                    }
                    table.appendColumn(values, label);
                } else {
                    String[] values = new String[rows.size()];
                    for (int row = 0; row < rows.size(); row++) {
                        values[row] = rows.get(row).get(column);
                    }
                    table.appendColumn(values, label);
                }
            }
            table.setName(path);
            return table;
        } catch (IOException exception) {
            throw new RuntimeException("Could not read Berlin zone CSV " + path, exception);
        }
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(character);
            }
        }
        values.add(value.toString());
        return values;
    }

    private String findColumn(TableDataSet table, String... candidates) {
        for (String candidate : candidates) {
            if (Arrays.asList(table.getColumnLabels()).contains(candidate)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException(
                "Zone file must contain one of these columns: " + String.join(", ", candidates));
    }

    @Override
    public void readZoneShapefile(String path) {
        if (path == null) {
            logger.error("No shape file found!");
            throw new RuntimeException("No shape file found!");
        }
        int counter = 0;
        for (SimpleFeature feature : ShapeFileReader.getAllFeatures(path)) {
            int zoneId = Integer.parseInt(feature.getAttribute(SHAPE_IDENTIFIER).toString());
            ZoneBerlinBrandenburg zone = (ZoneBerlinBrandenburg) geoDataMuc.getZones().get(zoneId);
            if (zone != null) {
                zone.setZoneFeature(feature);
                final Object ags = feature.getAttribute("AGS");
                if(ags != null) {
                    zone.setAgs(Integer.parseInt(ags.toString()));
                }
            } else {
                counter++;
            }
        }
        if(counter > 0) {
            logger.warn("There were " + counter + " shapes that do not exist in silo zone system");
        }
    }
}
