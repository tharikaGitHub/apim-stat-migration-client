/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.stat.migration.util;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.wso2.carbon.apimgt.stat.migration.APIMStatMigrationException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseManager {

    private static final Log log = LogFactory.getLog(DatabaseManager.class);

    public static void initialize() throws APIMStatMigrationException {

    }

    public static void migrateDestinationSummaryTable() throws APIMStatMigrationException {
        Connection con1 = null;
        Connection con2 = null;
        PreparedStatement statement1 = null;
        PreparedStatement statement2 = null;
        ResultSet resultSetRetrieved = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            con1 = DriverManager
                    .getConnection("jdbc:mysql://localhost:3306/dasDatabase?autoReconnect=true", "root", "tharika@123");
            con2 = DriverManager
                    .getConnection("jdbc:mysql://localhost:3306/tstatdb?autoReconnect=true", "root", "tharika@123");
            String retrieveQuery = "SELECT * FROM " + APIMStatMigrationConstants.API_DESTINATION_SUMMARY;
            String insertQuery = "INSERT INTO " + APIMStatMigrationConstants.API_PER_DESTINATION_AGG
                    + "_DAYS(apiName, apiVersion, apiCreator, apiContext, destination, AGG_COUNT, apiHostname, "
                    + "AGG_TIMESTAMP, AGG_EVENT_TIMESTAMP, AGG_LAST_EVENT_TIMESTAMP, gatewayType, label, regionalID) VALUES(?,?,?,?,?,?,?,?,?,?,'SYNAPSE','Synapse','default')";
            statement1 = con1.prepareStatement(retrieveQuery);
            statement2 = con2.prepareStatement(insertQuery);
            resultSetRetrieved = statement1.executeQuery();
            while (resultSetRetrieved.next()) {
                String api = resultSetRetrieved.getString("api");
                String version = resultSetRetrieved.getString("version");
                String apiPublisher = resultSetRetrieved.getString("apiPublisher");
                String context = resultSetRetrieved.getString("context");
                String destination = resultSetRetrieved.getString("destination");
                long total_request_count = resultSetRetrieved.getLong("total_request_count");
                String hostName = resultSetRetrieved.getString("hostName");
                int year = resultSetRetrieved.getInt("year");
                int month = resultSetRetrieved.getInt("month");
                int day = resultSetRetrieved.getInt("day");
                String time = resultSetRetrieved.getString("time");
                statement2.setString(1, api);
                statement2.setString(2, version);
                statement2.setString(3, apiPublisher);
                statement2.setString(4, context);
                statement2.setString(5, destination);
                statement2.setLong(6, total_request_count);
                statement2.setString(7, hostName);
                String dayInString = year + "-" + month + "-" + day;
                statement2.setLong(8, getTimestampOfDay(dayInString));
                statement2.setLong(9, getTimestamp(time));
                statement2.setLong(10, getTimestamp(time)); //same as AGG_EVENT_TIMESTAMP
                statement2.executeUpdate();
            }
        } catch (SQLException e) {
            String msg = "Error occurred while connecting to and querying from the database";
            log.error(msg, e);
            throw new APIMStatMigrationException(msg, e);
        } catch (Exception e) {
            String msg = "Generic error occurred while connecting to the database";
            log.error(msg, e);
            throw new APIMStatMigrationException(msg, e);
        } finally {
            closeDatabaseLinks(resultSetRetrieved, statement1, con1);
            closeDatabaseLinks(null, statement2, con2);
        }

    }

    public static void migrateResourceUsageSummaryTable() throws APIMStatMigrationException {
        Connection con1 = null;
        Connection con2 = null;
        Connection con3 = null;
        PreparedStatement statement1 = null;
        PreparedStatement statement2 = null;
        PreparedStatement statement3 = null;
        ResultSet resultSetRetrieved = null;
        ResultSet resultSetFromAMDB = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            con1 = DriverManager
                    .getConnection("jdbc:mysql://localhost:3306/dasDatabase?autoReconnect=true", "root", "tharika@123");
            con2 = DriverManager
                    .getConnection("jdbc:mysql://localhost:3306/tstatdb?autoReconnect=true", "root", "tharika@123");
            con3 = DriverManager
                    .getConnection("jdbc:mysql://localhost:3306/AM_DB?autoReconnect=true", "root", "tharika@123");
            String consumerkeyMappingQuery = "select APPLICATION_ID from AM_APPLICATION_KEY_MAPPING WHERE CONSUMER_KEY=?";
            String retrieveQuery = "SELECT * FROM " + APIMStatMigrationConstants.API_RESOURCE_USAGE_SUMMARY;
            String insertQuery = "INSERT INTO " + APIMStatMigrationConstants.API_RESOURCE_PATH_AGG
                    + "_DAYS(apiName, apiVersion, apiCreator, apiResourceTemplate, apiContext, apiMethod, AGG_COUNT, "
                    + "apiHostname, AGG_TIMESTAMP, AGG_EVENT_TIMESTAMP, AGG_LAST_EVENT_TIMESTAMP, applicationId, gatewayType, label, regionalID) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,'SYNAPSE','Synapse','default')";
            statement1 = con1.prepareStatement(retrieveQuery);
            statement2 = con2.prepareStatement(insertQuery);
            statement3 = con3.prepareStatement(consumerkeyMappingQuery);
            resultSetRetrieved = statement1.executeQuery();
            while (resultSetRetrieved.next()) {
                String api = resultSetRetrieved.getString("api");
                String version = resultSetRetrieved.getString("version");
                String apiPublisher = resultSetRetrieved.getString("apiPublisher");
                //--------------------------
                String consumerKey = resultSetRetrieved.getString("consumerKey");
                statement3.setString(1, consumerKey);
                resultSetFromAMDB = statement3.executeQuery();
                int applicationId = -1;
                while (resultSetFromAMDB.next()) {
                    applicationId = resultSetFromAMDB.getInt("APPLICATION_ID");
                }
                //-------------------------------
                String resourcePath = resultSetRetrieved.getString("resourcePath");
                String context = resultSetRetrieved.getString("context");
                String method = resultSetRetrieved.getString("method");
                long total_request_count = resultSetRetrieved.getLong("total_request_count");
                String hostName = resultSetRetrieved.getString("hostName");
                int year = resultSetRetrieved.getInt("year");
                int month = resultSetRetrieved.getInt("month");
                int day = resultSetRetrieved.getInt("day");
                String time = resultSetRetrieved.getString("time");
                statement2.setString(1, api);
                statement2.setString(2, version);
                statement2.setString(3, apiPublisher);
                statement2.setString(4, resourcePath);
                statement2.setString(5, context);
                statement2.setString(6, method);
                statement2.setLong(7, total_request_count);
                statement2.setString(8, hostName);
                String dayInString = year + "-" + month + "-" + day;
                statement2.setLong(9, getTimestampOfDay(dayInString));
                statement2.setLong(10, getTimestamp(time));
                statement2.setLong(11, getTimestamp(time));
                if (applicationId != -1) {
                    statement2.setString(12, Integer.toString(applicationId));
                } else {
                    String errorMsg = "Error occurred while retrieving applicationId for consumer key : " + consumerKey;
                    log.error(errorMsg);
                    throw new APIMStatMigrationException(errorMsg);
                }
                statement2.executeUpdate();
            }
        } catch (SQLException e) {
            String msg = "Error occurred while connecting to and querying from the database";
            log.error(msg, e);
            throw new APIMStatMigrationException(msg, e);
        } catch (Exception e) {
            String msg = "Generic error occurred while connecting to the database";
            log.error(msg, e);
            throw new APIMStatMigrationException(msg, e);
        } finally {
            closeDatabaseLinks(resultSetRetrieved, statement1, con1);
            closeDatabaseLinks(null, statement2, con2);
            closeDatabaseLinks(resultSetFromAMDB, statement3, con3);
        }

    }

    /**
     * This method is used to close the ResultSet, PreparedStatement and Connection after getting data from the DB
     * This is called if a "PreparedStatement" is used to fetch results from the DB
     *
     * @param resultSet         ResultSet returned from the database query
     * @param preparedStatement prepared statement used in the database query
     * @param connection        DB connection used to get data from the database
     */
    private static void closeDatabaseLinks(ResultSet resultSet, PreparedStatement preparedStatement,
            Connection connection) {

        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                //this is logged and the process is continued because the query has executed
                log.error("Error occurred while closing the result set from JDBC database.", e);
            }
        }
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                //this is logged and the process is continued because the query has executed
                log.error("Error occurred while closing the prepared statement from JDBC database.", e);
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                //this is logged and the process is continued because the query has executed
                log.error("Error occurred while closing the JDBC database connection.", e);
            }
        }
    }

    private static long getTimestamp(String date) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern(APIMStatMigrationConstants.TIMESTAMP_PATTERN);
        DateTime dateTime = formatter.parseDateTime(date);
        System.out.println("Time String from old database : " + date);
        System.out.println("Time in long : " + dateTime.getMillis());
        return dateTime.getMillis();
    }

    private static long getTimestampOfDay(String date) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern(APIMStatMigrationConstants.TIMESTAMP_DAY_PATTERN);
        DateTime dateTime = formatter.parseDateTime(date);
        System.out.println("Day time String from old database : " + date);
        System.out.println("Time in long : " + dateTime.getMillis());
        return dateTime.getMillis();
    }
}