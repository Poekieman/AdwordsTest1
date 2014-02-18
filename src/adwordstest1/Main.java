/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/*
 *  2014_02-15 FTO adwords-api-8.14.0.jar gebruikt en refs daarnaartoe geimporteerd
 */
  
package adwordstest1;

import java.sql.*;

import java.io.Writer;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.FileOutputStream;

//import java.net.HttpURLConnection;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Calendar;
import java.util.Scanner;
import java.util.List;

/*
import com.google.api.adwords.lib.utils.v201309.ReportDownloadResponse;
import com.google.api.adwords.lib.utils.v201309.ReportUtils;

import com.google.api.adwords.v201309.jaxb.cm.DownloadFormat;
import com.google.api.adwords.v201309.jaxb.cm.ReportDefinition;
import com.google.api.adwords.v201309.jaxb.cm.ReportDefinitionDateRangeType;
import com.google.api.adwords.v201309.jaxb.cm.ReportDefinitionReportType;
import com.google.api.adwords.v201309.jaxb.cm.Selector;
import com.google.api.adwords.v201309.jaxb.cm.DateRange;

import com.google.api.adwords.lib.AdWordsServiceLogger;
import com.google.api.adwords.lib.AdWordsUser;
*/
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.api.ads.adwords.lib.jaxb.v201309.DownloadFormat;
import com.google.api.ads.adwords.lib.jaxb.v201309.ReportDefinition;
import com.google.api.ads.adwords.lib.jaxb.v201309.ReportDefinitionDateRangeType;
import com.google.api.ads.adwords.lib.jaxb.v201309.ReportDefinitionReportType;
import com.google.api.ads.adwords.lib.jaxb.v201309.Selector;
import com.google.api.ads.adwords.lib.jaxb.v201309.DateRange;
import com.google.api.ads.adwords.lib.utils.ReportDownloadResponse;
//import com.google.api.ads.adwords.lib.utils.ReportUtils;
import com.google.api.ads.adwords.lib.utils.ReportDownloadResponseException;

import com.google.api.ads.adwords.lib.utils.v201309.ReportDownloader;
import com.google.api.ads.common.lib.auth.OfflineCredentials;
import com.google.api.ads.common.lib.auth.OfflineCredentials.Api;
import com.google.api.ads.common.lib.utils.Streams;
import com.google.api.client.auth.oauth2.Credential;


/**
 * @author Frank
 */
public class Main {
    
    public static void ConvertFormat(String psFileNameGoogle, String psFilenameInetCamp )
    {
        try {
            DateFormat dfFrom = new SimpleDateFormat("yyyy-MM-dd");   // nieuwe Ad Hoc Report date format
            DateFormat dfTo   = new SimpleDateFormat("M/d/yyyy");     // Oude CampaignStats format

            Scanner fileScanner = new Scanner(new FileReader(psFileNameGoogle));
            Writer fileWriter = new FileWriter(psFilenameInetCamp,true);   // Append

            // Create a PrintWriter object to print characters to the FileWriter object:
            PrintWriter fileOut = new PrintWriter (fileWriter); // fileOut decorates fileWriter!

            Integer lc = 0;
            while ( fileScanner.hasNextLine() ){
                String line = fileScanner.nextLine();
                if (++lc > 2 && fileScanner.hasNext()) {    // skip first 2 lines and the last line
                    line = line.replaceAll(",", "");        // Remove any thousand separators
                    line = line.replaceAll("\"", "");       // Remove any double quotes (around thousand separator things)
                    Scanner lineScanner = new Scanner(line);
                    lineScanner.useDelimiter("\t");         // TAB 
                    Integer cc = 0;                         // column count
                    while ( lineScanner.hasNext() ) {
                        String strColumn = lineScanner.next();
                        cc++;
                        switch (cc) {
                            case 1 :     // date field
                                Date dtFrom = dfFrom.parse(strColumn);
                                strColumn = dfTo.format( dtFrom );
                                break;
                            case 6 : 
                                long costInMicros = (long)(Double.parseDouble(strColumn) * 1000000);
                                strColumn = Long.toString(costInMicros);
                                break;
                        } // switch
                        fileOut.print( strColumn + "\t" );  // Na laatste veld ook een TAB
                    }    
                    fileOut.println();
                } // if 
                //no need to call lineScanner.close(), since the source is a String
            } // while 
            // Binnen de finally zijn deze niet bekend.
            // Maar ze mogen niet boven de try gedeclareerd worden, rara...
            fileScanner.close();
            fileOut.close();
            // Opruimen
            try {
                File f = new File( psFileNameGoogle );
                f.delete();
                }
            catch (Exception e) {
                System.err.println (e.getMessage ());
            }
            } // try
        catch (Exception e) {
            System.err.println (e.getMessage ());
            }
        finally {
            //ensure the underlying stream is always closed
            //this only has any effect if the item passed to the Scanner
            //constructor implements Closeable (which it does in this case).
        }
    }

    private static void getCampaignStatsReport(
            AdWordsSession session,
//            String psEmail, 
//            String psPassword, 
            String psCustomerId,  // just for the filename
//            String psDevToken,            
            String psDateFrom, String psDateTo,
            String psDirectory) {
        //
        // Location to download report to.
        try {
            //AdWordsUser user = new AdWordsUser("metoomedia@gmail.com", psPassword, "881-094-8058", "CampaignStatsDownloader", psDevToken);
            //AdWordsUser user = new AdWordsUser(psEmail, psPassword, psCustomerId, "CampaignStatsDownloader", psDevToken);
            
            String sRepDateRange = psDateFrom + "_" + psDateTo;
            String reportFileGoogle   = psDirectory + "Report_"  + psCustomerId + "_" + sRepDateRange + ".txt";
            String reportFileInetCamp = psDirectory + "Adwords_" +                      sRepDateRange + ".csv";

            // Create selector.
            Selector selector = new Selector();
            List<String> fields = selector.getFields();
            fields.add("Date");
            fields.add("CampaignName");
            fields.add("CampaignId");
            fields.add("Impressions");
            fields.add("Clicks");
            fields.add("Cost");
            fields.add("Conversions");
            //
            // Custom data range
            DateRange dtRange = new DateRange();
            dtRange.setMin(psDateFrom);
            dtRange.setMax(psDateTo);
            selector.setDateRange(dtRange);
            //
            // Create report definition.
            ReportDefinition reportDefinition = new ReportDefinition();
            reportDefinition.setReportName("Campaign performance report #" + System.currentTimeMillis());
            reportDefinition.setDateRangeType(ReportDefinitionDateRangeType.CUSTOM_DATE);
//            reportDefinition.setDateRange(new DateRange(psDateFrom, psDateTo));
            reportDefinition.setReportType(ReportDefinitionReportType.CAMPAIGN_PERFORMANCE_REPORT);
            reportDefinition.setDownloadFormat(DownloadFormat.TSV);   // Tab separated
            reportDefinition.setIncludeZeroImpressions(false);
            reportDefinition.setSelector(selector);
/* Using old library
            FileOutputStream fos = new FileOutputStream(new File(reportFileGoogle));
            ReportDownloadResponse response = ReportUtils.downloadReport(user, reportDefinition, fos);
            // Ik kan heer geen UseMicros meegeven helaas
            // Daarom bij omzetten naar ander format * 1000000 doen (helaas)
            if (response.getHttpStatus() == HttpURLConnection.HTTP_OK) {
                System.out.println("Report successfully downloaded: " + reportFileGoogle);
                ConvertFormat( reportFileGoogle, reportFileInetCamp );
            } else {
                System.out.println("Report was not downloaded. " + response.getHttpStatus() + ": "
                    + response.getHttpResponseMessage());
            }
*/
            try {
                // Set the property api.adwords.reportDownloadTimeout or call
                // ReportDownloader.setReportDownloadTimeout to set a timeout (in milliseconds)
                // for CONNECT and READ in report downloads.
                ReportDownloadResponse response =
                    new ReportDownloader(session).downloadReport(reportDefinition);
                FileOutputStream fos = 
                    new FileOutputStream(new File(reportFileGoogle));
                Streams.copy(response.getInputStream(), fos);
                fos.close();
                System.out.println("Report successfully downloaded: " + reportFileGoogle);
                ConvertFormat( reportFileGoogle, reportFileInetCamp );
            } catch (ReportDownloadResponseException e) {
                System.out.println("Report was not downloaded. " + e);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        Connection con = null;
        Statement stmt;
        ResultSet rs;
        PrintWriter outputStream = null;
        //
        Calendar cal = Calendar.getInstance();
        System.out.println(cal.getTime().toString() + ": Start...");
        System.out.println("Debug: arguments:");
        // Enhanced for loop 
        for (String arg : args) {
            System.out.println(arg);
        }
        if (args.length != 6) {
            System.err.println("Expected 6 parameters:");
            System.err.println("JDBC_URL user password date_from date_to(incl) output_path");
            return;
        }
        String sJDBC_URL = args[0];
        String sUser = args[1];
        String sPassword = args[2];
        String sDateFrom = args[3];
        String sDateTo = args[4];
        String sDirectory = args[5];    // for Adwords files

        if ( sDateFrom.compareTo(sDateTo ) > 0) {
            System.out.println("Nothing to do: fromDate > toDate");
            return;
        }    

        try {
            // Generate a refreshable OAuth2 credential similar to a ClientLogin token
            // and can be used in place of a service account.
            Credential oAuth2Credential = new OfflineCredentials.Builder()
                .forApi(Api.ADWORDS)
                .fromFile()
                .build()
                .generateCredential();

            // Construct an AdWordsSession.
            AdWordsSession session = new AdWordsSession.Builder()
                .fromFile()
                .withOAuth2Credential(oAuth2Credential)
                .build();

            // Log SOAP XML request and response.
            //AdWordsServiceLogger.log(); // depreated

            Class.forName("com.mysql.jdbc.Driver").newInstance();
            //con = DriverManager.getConnection("jdbc:mysql://mail.pek.nl:6033/_inetcamp", "root", "knarf123");
            con = DriverManager.getConnection(sJDBC_URL, sUser, sPassword);
            //
            if (!con.isClosed()) {
                System.out.println("Successfully connected to MySQL server using TCP/IP...");
            }

            // Log SOAP XML request and response. (waar blijft dat?)
            //AdWordsServiceLogger.log(); // depreated

            stmt = con.createStatement(
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);

            rs = stmt.executeQuery("SELECT * FROM account WHERE enabled <> 0 ORDER BY id");

            System.out.println("Accounts:");
            while (rs.next()) {
                int iId = rs.getInt("id");
                String sNaam = rs.getString("naam");
                String sCustomerId = rs.getString("account-id");
                String sEmail = rs.getString("email");
                String sPwd = rs.getString("password");
                String sAppTok = rs.getString("apptoken");
                String sDevTok = rs.getString("devtoken");
                System.out.println(iId + ":" + sNaam + ", " + sCustomerId  
                        + ", " + sEmail + ", " + sPwd
                        + ", " + sAppTok + ", " + sDevTok);
                
                //getCampaignInfo(sEmail, sPwd, sCustomerId, sDevTok, sDateFrom, sDateTo, sDirectory);
                //getCampaignStatsReport(sEmail, sPwd, sCustomerId, sDevTok, sDateFrom, sDateTo, sDirectory);
                getCampaignStatsReport(session, sCustomerId, sDateFrom, sDateTo, sDirectory);
            }
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (SQLException e) {
                System.err.println("Exception: " + e.getMessage());
            }
        }
        //System.out.println("wait 10s ...");
        //Thread.sleep(10000);
        //System.out.println("done.");
        System.out.println(cal.getTime().toString() + ": Finished.");
    }
}
