//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package application_src.application_model.loaders;

import application_src.MainApp;
import application_src.application_model.data.LineageData;
import application_src.application_model.data.TableLineageData;
import application_src.application_model.resources.ProductionInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class AceTreeTableLineageDataLoader {
    private static String CONFIG_FILE_PATH = "/atlas_model/configurations/nuclei_config/";
    private static String CONFIG_FILE_NAME = "NucleiConfig.csv";
    private static String HEADER_LINE = "Resource Location";
    private static String ENTRY_PREFIX;
    private static final String T = "t";
    private static final String ENTRY_EXT = "-nuclei";
    private static final int NUMBER_OF_TOKENS = 21;
    private static final int VALID_INDEX = 1;
    private static final int XCOR_INDEX = 5;
    private static final int YCOR_INDEX = 6;
    private static final int ZCOR_INDEX = 7;
    private static final int DIAMETER_INDEX = 8;
    private static final int ID_INDEX = 9;
    private static final String ONE_ZERO_PAD = "0";
    private static final String TWO_ZERO_PAD = "00";
    private static final int X_POS_INDEX = 0;
    private static final int Y_POS_INDEX = 1;
    private static final int Z_POS_INDEX = 2;
    private static final List<String> allCellNames = new ArrayList();
    private static double avgX;
    private static double avgY;
    private static double avgZ;

    public AceTreeTableLineageDataLoader() {
    }

    public static LineageData loadNucFiles(ProductionInfo productionInfo) {
        TableLineageData tableLineageData = new TableLineageData(allCellNames, (double)productionInfo.getXScale(), (double)productionInfo.getYScale(), (double)productionInfo.getZScale());
        URL config_url = MainApp.class.getResource(CONFIG_FILE_PATH + CONFIG_FILE_NAME);

        try {
            InputStreamReader streamReader = new InputStreamReader(config_url.openStream());
            Throwable var4 = null;

            try {
                BufferedReader reader = new BufferedReader(streamReader);
                Throwable var6 = null;

                try {
                    String line;
                    try {
                        while((line = reader.readLine()) != null) {
                            if (line.equals(HEADER_LINE)) {
                                line = reader.readLine();
                            }

                            if (line.length() <= 1) {
                                break;
                            }

                            ENTRY_PREFIX = line.substring(0, line.length());
                        }
                    } catch (Throwable var34) {
                        var6 = var34;
                        throw var34;
                    }
                } finally {
                    if (reader != null) {
                        if (var6 != null) {
                            try {
                                reader.close();
                            } catch (Throwable var32) {
                                var6.addSuppressed(var32);
                            }
                        } else {
                            reader.close();
                        }
                    }

                }
            } catch (Throwable var36) {
                var4 = var36;
                throw var36;
            } finally {
                if (streamReader != null) {
                    if (var4 != null) {
                        try {
                            streamReader.close();
                        } catch (Throwable var31) {
                            var4.addSuppressed(var31);
                        }
                    } else {
                        streamReader.close();
                    }
                }

            }
        } catch (IOException var38) {
            var38.printStackTrace();
        }

        try {
            tableLineageData.addTimeFrame();

            for(int i = 1; i <= productionInfo.getTotalTimePoints(); ++i) {
                String urlString = getResourceAtTime(i);
                if (urlString != null) {
                    URL url = MainApp.class.getResource(urlString);
                    if (url != null) {
                        process(tableLineageData, i, url.openStream());
                    } else {
                        System.out.println("Could not find file: " + urlString);
                    }
                }
            }
        } catch (IOException var33) {
            var33.printStackTrace();
        }

        setOriginToZero(tableLineageData, true);
        return tableLineageData;
    }

    private static String getResourceAtTime(int i) {
        String resourceUrlString = null;
        if (i >= 1) {
            if (i < 10) {
                resourceUrlString = ENTRY_PREFIX + "t" + "00" + i + "-nuclei";
            } else if (i < 100) {
                resourceUrlString = ENTRY_PREFIX + "t" + "0" + i + "-nuclei";
            } else {
                resourceUrlString = ENTRY_PREFIX + "t" + i + "-nuclei";
            }
        }

        return resourceUrlString;
    }

    public static double getAvgXOffsetFromZero() {
        return avgX;
    }

    public static double getAvgYOffsetFromZero() {
        return avgY;
    }

    public static double getAvgZOffsetFromZero() {
        return avgZ;
    }

    public static void setOriginToZero(LineageData lineageData, boolean defaultEmbryoFlag) {
        int totalPositions = 0;
        double sumX = 0.0D;
        double sumY = 0.0D;
        double sumZ = 0.0D;

        for(int i = 1; i < lineageData.getNumberOfTimePoints(); ++i) {
            double[][] positionsArray = lineageData.getPositions(i);

            for(int j = 0; j < positionsArray.length; ++j) {
                sumX += positionsArray[j][0];
                sumY += positionsArray[j][1];
                sumZ += positionsArray[j][2];
                ++totalPositions;
            }
        }

        if (totalPositions != 0) {
            avgX = sumX / (double)totalPositions;
            avgY = sumY / (double)totalPositions;
            avgZ = sumZ / (double)totalPositions;
        } else {
            avgZ = 0.0D;
            avgY = 0.0D;
            avgX = 0.0D;
        }

        System.out.println("Average nuclei position offsets from origin (0, 0, 0): (" + avgX + ", " + avgY + ", " + avgZ + ")");
        lineageData.shiftAllPositions(avgX, avgY, avgZ);
    }

    private static void process(TableLineageData tableLineageData, int time, InputStream input) {
        tableLineageData.addTimeFrame();

        try {
            InputStreamReader isr = new InputStreamReader(input);
            Throwable var4 = null;

            try {
                BufferedReader reader = new BufferedReader(isr);
                Throwable var6 = null;

                try {
                    String line;
                    try {
                        while((line = reader.readLine()) != null) {
                            String[] tokens = new String[21];
                            StringTokenizer tokenizer = new StringTokenizer(line, ",");

                            for(int var10 = 0; tokenizer.hasMoreTokens(); tokens[var10++] = tokenizer.nextToken().trim()) {
                            }

                            if (Integer.parseInt(tokens[1]) == 1) {
                                makeNucleus(tableLineageData, time, tokens);
                            }
                        }
                    } catch (Throwable var34) {
                        var6 = var34;
                        throw var34;
                    }
                } finally {
                    if (reader != null) {
                        if (var6 != null) {
                            try {
                                reader.close();
                            } catch (Throwable var33) {
                                var6.addSuppressed(var33);
                            }
                        } else {
                            reader.close();
                        }
                    }

                }
            } catch (Throwable var36) {
                var4 = var36;
                throw var36;
            } finally {
                if (isr != null) {
                    if (var4 != null) {
                        try {
                            isr.close();
                        } catch (Throwable var32) {
                            var4.addSuppressed(var32);
                        }
                    } else {
                        isr.close();
                    }
                }

            }
        } catch (IOException var38) {
            System.out.println("Error in processing input stream");
        }

    }

    private static void makeNucleus(TableLineageData tableLineageData, int time, String[] tokens) {
        try {
            tableLineageData.addNucleus(time, tokens[9], Double.parseDouble(tokens[5]), Double.parseDouble(tokens[6]), Double.parseDouble(tokens[7]), Double.parseDouble(tokens[8]));
        } catch (NumberFormatException var4) {
            System.out.println("Incorrect format in nucleus file for time " + time + ".");
        }

    }
}
