package com.appedo.wpt.scheduler.dbi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import com.appedo.commons.connect.DataBaseManager;
import com.appedo.manager.LogManager;
import com.appedo.wpt.scheduler.bean.SUMAuditLogBean;
import com.appedo.wpt.scheduler.bean.SUMNodeBean;
import com.appedo.wpt.scheduler.bean.SUMTestBean;
import com.appedo.wpt.scheduler.common.Constants;
import com.appedo.wpt.scheduler.manager.NodeManager;
import com.appedo.wpt.scheduler.utils.UtilsFactory;


/**
 * DataBase Interface layer which handles the db CRUD operations of EUM Nodes & Tests eum_node_master, eum_test_master, eum_test_cluster_mapping, eum_cluster_mapping table.
 * 
 * @author Ramkumar
 *
 */
public class SUMDBI {
	
	public ArrayList<SUMTestBean> getTestIdDetails(Connection con) throws Throwable{

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		ArrayList<SUMTestBean> rumTestBeans = new ArrayList<SUMTestBean>();
		// RUMManager manager = new RUMManager();
		StringBuilder sbQuery = new StringBuilder();
		Date dateLog = LogManager.logMethodStart();
		
		try{
			sbQuery .append("SELECT t.test_id, t.testName, t.testurl, t.script, t.runevery, t.testtransaction, t.status, t.testtype, t.testfilename, ")
					.append("t.user_id, location, os_name, browser_name, t.connection_id, t.download, t.upload, t.latency, t.packet_loss, ")
					.append("sc.connection_name, CASE WHEN repeat_view=false THEN 1 ELSE 0 END AS repeatView, sla.sla_id, sla.sla_sum_id, ")
					.append("sla.sum_type, t.warning, t.error, t.rm_min_breach_count, t.downtime_alert ")
					.append("FROM sum_test_master t ")
					.append("INNER JOIN sum_test_cluster_mapping sm ON sm.test_id = t.test_id ")
					.append("LEFT JOIN sum_connectivity sc ON sc.connection_id = t.connection_id ")
					.append("LEFT JOIN sum_test_device_os_browser st ON st.sum_test_id = sm.test_id ")
					.append("LEFT JOIN sum_device_os_browser os ON st.device_os_browser_id = os.dob_id ")
					.append("LEFT JOIN so_sla_sum sla ON sla.sum_test_id = sm.test_id AND sla.sum_type = 'RESPONSE_MONITORING' ")
					//.append("WHERE status=true AND is_delete = false AND start_date <= now() AND end_date >= now() AND testtype = 'URL' ")
					.append("WHERE status=true AND is_delete = false AND start_date <= now() AND end_date >= now() ")
					.append("AND last_run_detail+CAST(runevery||' minute' AS Interval) <= now() ")
					.append("ORDER BY start_date ASC");
			 // System.out.println("Query : "+sbQuery.toString());

			pstmt = con.prepareStatement(sbQuery.toString());
			rs = pstmt.executeQuery();
			// Timer 
			while (rs.next()) {
				SUMTestBean testBean = new SUMTestBean();
				testBean.setTestId(Integer.valueOf(rs.getString("test_id")));
				testBean.setScript(rs.getString("script"));
				testBean.setTestName(rs.getString("testName"));
				testBean.setURL(rs.getString("testurl"));
				testBean.setRunEveryMinute( rs.getInt("runevery") );
				testBean.setTransaction(rs.getString("testtransaction"));
				testBean.setStatus(rs.getBoolean("status"));
				testBean.setTestType(rs.getString("testtype"));
				testBean.setTestClassName(rs.getString("testfilename"));
				testBean.setUserId(Integer.valueOf(rs.getString("user_id")));
				testBean.setConnectionName(rs.getString("connection_name"));
				testBean.setDownTimeAlert(rs.getBoolean("downtime_alert"));
				if( rs.getString("download")!= null ) {
					testBean.setDownload(String.valueOf(rs.getInt("download")));
				}
				if( rs.getString("upload")!= null ) {
					testBean.setUpload(String.valueOf(rs.getInt("upload")));
				}
				if( rs.getString("latency")!= null ) {
					testBean.setLatency(String.valueOf(rs.getInt("latency")));
				}
				if( rs.getString("packet_loss")!= null ) {
					testBean.setPacketLoss(String.valueOf(rs.getInt("packet_loss")));
				}

				if(rs.getString("browser_name")!=null){
					if( rs.getString("connection_name")!= null ){
						testBean.setLocation(rs.getString("location")+":"+rs.getString("browser_name")+"."+rs.getString("connection_name"));
					} else {
						testBean.setLocation(rs.getString("location")+":"+rs.getString("browser_name"));
					}
				} else {
					testBean.setLocation(rs.getString("location"));
				}
				testBean.setRepeatView(String.valueOf(rs.getInt("repeatView")));

				if( rs.getString("sla_id")!= null ){
					testBean.setSlaId(rs.getLong("sla_id"));
				}

				if( rs.getString("sla_sum_id")!= null ){
					testBean.setSlaSumId(rs.getLong("sla_sum_id"));
				}

				if( rs.getString("warning")!= null ){
					testBean.setThresholdValue(rs.getInt("warning"));
				}

				if( rs.getString("error")!= null ){
					testBean.setErrorValue(rs.getInt("error"));
				}

				if( rs.getString("rm_min_breach_count")!= null ){
					testBean.setMinBreachCount(rs.getInt("rm_min_breach_count"));
				}

				// testBean.setTargetLocations( (new SUMDBI()).getTestTargetLocations(con, testBean.getTestId()) );
//				HashSet<String> a = new HashSet<String>();
//				a.add("ChennaiWindows8");
//				testBean.setTargetLocations(a);
				rumTestBeans.add(testBean);
				// manager.runRUMTests(testBean);
			}
		} catch (Throwable ex) {
			LogManager.errorLog(ex, sbQuery);
			throw ex;
		} finally {
			LogManager.logMethodEnd(dateLog);
			
			DataBaseManager.close(rs);
			rs = null;
			
			DataBaseManager.close(pstmt);
			pstmt = null;
			
			UtilsFactory.clearCollectionHieracy( sbQuery );
		}
		
		return rumTestBeans;
	}

	public JSONObject getTestIdDetails(Connection con, long test_id) throws Exception{
		JSONObject joSumDetails = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		StringBuilder sbQuery = new StringBuilder();
		try {
			joSumDetails = new JSONObject();
			sbQuery .append("SELECT test_id, sla_id, sla_sum_id, user_id, warning, error, rm_min_breach_count FROM ")
					.append("sum_test_master stm JOIN so_sla_sum sss ON stm.test_id = sss.sum_test_id ")
					.append("WHERE stm.test_id = ").append(test_id);
			
			pstmt = con.prepareStatement(sbQuery.toString());
			rs = pstmt.executeQuery();
			if (rs.next()) {
				joSumDetails.put("userid", rs.getString("user_id"));
				joSumDetails.put("sum_test_id", rs.getString("test_id"));
				joSumDetails.put("threshold_set_value", rs.getString("warning"));
				joSumDetails.put("err_set_value", rs.getString("error"));
				joSumDetails.put("sla_id", rs.getString("sla_id"));
				joSumDetails.put("sla_sum_id", rs.getString("sla_sum_id"));
				joSumDetails.put("min_breach_count", rs.getString("rm_min_breach_count"));
			}
		}catch (Exception ex) {
			LogManager.errorLog(ex);
			throw ex;
		} finally {
			DataBaseManager.close(rs);
			rs = null;
			DataBaseManager.close(pstmt);
			pstmt = null;

			UtilsFactory.clearCollectionHieracy( sbQuery );
		}
	  return joSumDetails;	
	}
	
	
	/**
	 * Gets the cluster_id from mapping table by passing test_id
	 * 
	 * @param con
	 * @param test_id
	 * @return
	 */
	public HashSet<String> getTestTargetLocations(Connection con, long test_id) {
		PreparedStatement pstmt = null;
		ResultSet rsClusters = null;

		HashSet<String> hsLocations = new HashSet<String>();
		StringBuilder sbQuery = new StringBuilder();
		try {
			/*select concat(location,'',os_name), browser_name from sum_device_os_browser so 
			inner join sum_test_device_os_browser st 
			ON st.os_browser_id = so.os_browser_id 
			inner join sum_test_cluster_mapping sm
			on sm.test_id = st.sum_test_id
			where sum_test_id = 619;*/
			sbQuery.append("SELECT * FROM sum_test_cluster_mapping WHERE test_id = ").append(test_id);
			pstmt = con.prepareStatement(sbQuery.toString());
			rsClusters = pstmt.executeQuery();

			while( rsClusters.next() ){
				hsLocations.add( rsClusters.getString("location") );
			}
		} catch (Exception e) {
			LogManager.errorLog(e);
			e.printStackTrace();
		} finally {
			DataBaseManager.close(rsClusters);
			rsClusters = null;
			DataBaseManager.close(pstmt);
			pstmt = null;

			UtilsFactory.clearCollectionHieracy( sbQuery );
		}

		return hsLocations;
	}

	public ArrayList<SUMTestBean> createNewThreadForTest(Connection con, long test_id, boolean status){

		Statement stmt = null;
		ResultSet rs = null;
		StringBuilder sbQuery = new StringBuilder();
		ArrayList<SUMTestBean> rumTestBeans = new ArrayList<SUMTestBean>();

		try{
			sbQuery .append("SELECT t.test_id, t.testName, t.testurl, t.runevery, t.testtransaction, t.status, t.testtype, t.testfilename, ")
					.append("t.user_id, location, os_name, browser_name, t.connection_id, t.download, t.upload, ")
					.append("t.latency, t.packet_loss, sc.connection_name, CASE WHEN repeat_view=false THEN 1 ELSE 0 END AS repeatView, sla.sla_id, sla.sla_sum_id, ")
					.append("sla.sum_type, t.warning, t.error, t.rm_min_breach_count, t.downtime_alert ")
					.append("FROM sum_test_master t ")
					.append("INNER JOIN sum_test_cluster_mapping sm on sm.test_id = t.test_id ")
					.append("LEFT JOIN sum_connectivity sc on sc.connection_id = t.connection_id ")
					.append("LEFT JOIN sum_test_device_os_browser st on st.sum_test_id = sm.test_id ")
					.append("LEFT JOIN sum_device_os_browser os on st.device_os_browser_id = os.dob_id ")
					.append("LEFT JOIN so_sla_sum sla on sla.sum_test_id = sm.test_id AND sla.sum_type = 'RESPONSE_MONITORING' ")
					.append("WHERE status=").append(status)
					.append(" AND t.test_id=").append(test_id).append(" AND is_delete = false AND start_date <= now() AND end_date >= now() ")
					.append("AND last_run_detail+CAST(runevery||' minute' AS INTERVAL) <= now() ")
					.append("ORDER BY start_date ASC");
					//.append("and testtype = 'URL' and last_run_detail+CAST(runevery||' minute' AS Interval) <= now() order by start_date asc");
			stmt = con.createStatement();
			rs = stmt.executeQuery(sbQuery.toString());
			rumTestBeans.clear();

			while (rs.next()) {
				SUMTestBean testBean = new SUMTestBean();
				testBean.setTestId(Integer.valueOf(rs.getString("test_id")));
				testBean.setTestName(rs.getString("testName"));
				testBean.setURL(rs.getString("testurl"));
				testBean.setRunEveryMinute( rs.getInt("runevery") );
				testBean.setTransaction(rs.getString("testtransaction"));
				testBean.setStatus(rs.getBoolean("status"));
				testBean.setTestType(rs.getString("testtype"));
				testBean.setTestClassName(rs.getString("testfilename"));
				testBean.setUserId(Integer.valueOf(rs.getString("user_id")));
				testBean.setDownTimeAlert(rs.getBoolean("downtime_alert"));
				if( rs.getString("download")!= null ) {
					testBean.setDownload(String.valueOf(rs.getInt("download")));
				}
				if( rs.getString("upload")!= null ) {
					testBean.setUpload(String.valueOf(rs.getInt("upload")));
				}
				if( rs.getString("latency")!= null ) {
					testBean.setLatency(String.valueOf(rs.getInt("latency")));
				}
				if( rs.getString("packet_loss")!= null ) {
					testBean.setPacketLoss(String.valueOf(rs.getInt("packet_loss")));
				}

				if(rs.getString("browser_name")!=null){
					if( rs.getString("connection_name")!= null ){
						testBean.setLocation(rs.getString("location")+":"+rs.getString("browser_name")+"."+rs.getString("connection_name"));
					} else {
						testBean.setLocation(rs.getString("location")+":"+rs.getString("browser_name"));
					}
				} else {
					testBean.setLocation(rs.getString("location"));
				}
				testBean.setRepeatView(String.valueOf(rs.getInt("repeatView")));

				if( rs.getString("sla_id")!= null ){
					testBean.setSlaId(rs.getLong("sla_id"));
				}

				if( rs.getString("sla_sum_id")!= null ){
					testBean.setSlaSumId(rs.getLong("sla_sum_id"));
				}

				if( rs.getString("warning")!= null ){
					testBean.setThresholdValue(rs.getInt("warning"));
				}

				if( rs.getString("error")!= null ){
					testBean.setErrorValue(rs.getInt("error"));
				}

				if( rs.getString("rm_min_breach_count")!= null ){
					testBean.setMinBreachCount(rs.getInt("rm_min_breach_count"));
				}
				//testBean.setTargetLocations( (new SUMDBI()).getTestTargetLocations(con, testBean.getTestId()) );
				rumTestBeans.add(testBean);
				// manager.runRUMTests(testBean);

			}
		} catch (Throwable ex) {
			LogManager.errorLog(ex);
		} finally {
			DataBaseManager.close(rs);
			rs = null;
			DataBaseManager.close(stmt);
			stmt = null;

			UtilsFactory.clearCollectionHieracy( sbQuery );
		}
	  return rumTestBeans;
	}

	/**
	 * Inserting values in sum_har_test_results if the testtype is URL
	 * 
	 * @param con
	 * @param joHarFileSummary
	 * @param lTestId
	 * @param strNode
	 */
	public void insertHarFileTableForURL(JSONObject joHarFileSummary, long lTestId, String strNode) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String strQryInsHarFile = null;
		
		try {
			strQryInsHarFile = "INSERT INTO sum_har_test_results (test_id, mac_address, harfilename, starttimestamp, contentloadtime, pageloadtime,received_on) VALUES"
					+ "("
					+ lTestId
					+ ", '"
					+ strNode
					+ "', '"
					+ joHarFileSummary.getString("harFilename")
					+ "', "
					+ "'"
					+ joHarFileSummary.getString("startedDateTime")
					+ "', "
					+ joHarFileSummary.getString("contentLoadTime")
					+ ", "
					+ joHarFileSummary.getString("pageLoadTime")
					+ ",'"
					+ dateFormat.format(Calendar.getInstance().getTime())
							.toString() + "')";
			NodeManager.agentLogQueue(strQryInsHarFile);
		} catch (Exception e) {
			LogManager.infoLog("joHarFileSummary: "+joHarFileSummary);
			LogManager.errorLog(e);
		} finally {
			UtilsFactory.clearCollectionHieracy( strQryInsHarFile );
		}
	}
	
	/**
	 * nserting values in sum_har_test_results if the testtype is TRANSACTION
	 * 
	 * @param con
	 * @param joHarFileSummary
	 * @param lTestId
	 * @param strNode
	 */
	public void insertHarFileTableForScript(JSONObject joHarFileSummary, long lTestId, String strNode) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		JSONArray jaHarSummaries = null;
		String strQryInsHarFile = null;
		try {
			jaHarSummaries = joHarFileSummary.getJSONArray("harsummary");

			for (int i = 0; i < jaHarSummaries.size(); i++) {
				JSONObject joHarSummary = (JSONObject) jaHarSummaries.get(i);
				strQryInsHarFile = "INSERT INTO sum_har_test_results (test_id, mac_address, harfilename, starttimestamp, contentloadtime, pageloadtime, page_id, page_name,received_on) VALUES"
						+ "("
						+ lTestId
						+ ", '"
						+ strNode
						+ "', '"
						+ joHarFileSummary.getString("harFilename")
						+ "', "
						+ "'"
						+ joHarSummary.getString("startedDateTime")
						+ "', "
						+ joHarSummary.getString("contentLoadTime")
						+ ", "
						+ joHarSummary.getString("pageLoadTime")
						+ ", '"
						+ joHarSummary.getString("pageid ")
						+ "', '"
						+ joHarSummary.getString("pagename ")
						+ "','"
						+ dateFormat.format(Calendar.getInstance().getTime()).toString() + "')";
				NodeManager.agentLogQueue(strQryInsHarFile);
			}


		} catch (Exception e) {
			LogManager.errorLog(e);
		} finally {
			UtilsFactory.clearCollectionHieracy( jaHarSummaries );
			UtilsFactory.clearCollectionHieracy( strQryInsHarFile );
		}
	}

	@SuppressWarnings("deprecation")
	public long insertSUMlog(Connection con, SUMAuditLogBean auditLogBean) throws Throwable {
		PreparedStatement pstmt = null;
		StringBuilder sbQuery = new StringBuilder();
		long audit_log_id = 0;

		try {
			sbQuery	.append("insert into sum_execution_audit_log(node_id, node_user_id, agent_type, sum_test_id, sum_test_name, appedo_user_id, appedo_enterprise_id, execution_time, location, latitude, longitude, ip_address, mac_address, error_msg, remarks, created_on) ")
					.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			pstmt = con.prepareStatement(sbQuery.toString(), PreparedStatement.RETURN_GENERATED_KEYS);

			pstmt.setLong(1, auditLogBean.getNodeId());
			pstmt.setLong(2, auditLogBean.getNodeUserId());
			pstmt.setString(3, UtilsFactory.makeValidVarchar(auditLogBean.getAgentType()));
			pstmt.setLong(4, auditLogBean.getSumTestId());
			pstmt.setString(5, auditLogBean.getSumTestName());
			pstmt.setLong(6, auditLogBean.getAppedoUserId());
			pstmt.setInt(7, 0);	// TODO add Enterprise Id
			pstmt.setInt(8, auditLogBean.getExecutionTime());
			pstmt.setString(9, auditLogBean.getLocation());
			pstmt.setDouble(10, auditLogBean.getLatitude());
			pstmt.setDouble(11, auditLogBean.getLongitude());
			pstmt.setString(12, auditLogBean.getIpAddress());
			pstmt.setString(13, auditLogBean.getMacAddress());
			pstmt.setString(14, "");	// error_msg
			pstmt.setString(15, auditLogBean.getRemarks());
			pstmt.setTimestamp(16, new Timestamp(Long.valueOf(auditLogBean.getCreatedOn())));
			pstmt.executeUpdate();
			audit_log_id = DataBaseManager.returnKey(pstmt);
			//System.out.println("audit_log_id:: " + audit_log_id);
		} catch (Throwable t) {
			LogManager.errorLog(t);
			throw t;
		} finally {
			DataBaseManager.close(pstmt);
			pstmt = null;

			UtilsFactory.clearCollectionHieracy( sbQuery );
		}
		return audit_log_id;
	}
	
	public void updateSUMLog(JSONObject joNodeStatus ) throws Exception {
		StringBuilder sbQuery = new StringBuilder();
		
		try {
			sbQuery .append("update sum_execution_audit_log set execution_duration_in_min = ")
					.append(joNodeStatus.getInt("execution_time")).append(", execution_status = ")
					.append(joNodeStatus.getBoolean("execution_status")).append(", error_msg = ")
					.append(UtilsFactory.makeValidVarchar(joNodeStatus.getString("error"))).append("  WHERE created_on = '")
					.append(new Timestamp(Long.valueOf(joNodeStatus.getString("log_id")))).append("'");

			NodeManager.agentLogQueue(sbQuery.toString());

		} catch (Exception e) {
			LogManager.errorLog(e);
			throw e;
		} finally {
			UtilsFactory.clearCollectionHieracy( sbQuery );
		}
	}
	
	public SUMNodeBean getNodeDetails(Connection con, String mac){
		SUMNodeBean sumNodeBean = new SUMNodeBean();
		Statement stmt = null;
		ResultSet rs = null;
		String strQry = null;
		
		try {
			strQry = "select * from sum_node_details where mac_address='" + mac + "'";
			stmt = con.createStatement();
			rs = stmt.executeQuery(strQry);
			if(rs.next()){
				sumNodeBean.setNodeId(rs.getInt("node_id"));
				sumNodeBean.setUserId(rs.getInt("sum_user_id"));
				sumNodeBean.setAgentType(rs.getString("agent_type"));
				sumNodeBean.setIPAddresses(rs.getString("ipaddress"));
				sumNodeBean.setCity(rs.getString("city"));
				sumNodeBean.setCountry(rs.getString("country"));
				sumNodeBean.setLatitude(rs.getDouble("latitude"));
				sumNodeBean.setLongitude(rs.getDouble("longitude"));
			}
		} catch (Exception e) {
			LogManager.errorLog(e);
		} finally {
			DataBaseManager.close(rs);
			rs = null;
			DataBaseManager.close(stmt);
			stmt = null;

			UtilsFactory.clearCollectionHieracy( strQry );
		}
		return sumNodeBean;
	}
	
	/**
	 * 
	 * @param con
	 * @throws Throwable
	 */
	public void updateNodeStatus(Connection con) throws Throwable {
		PreparedStatement pstmt = null;
		StringBuilder sbQuery = new StringBuilder();
		
		try {
			sbQuery	.append("update sum_node_details SET sum_node_status = ? ")
					.append("where modified_on< now() - interval ")
					.append("'").append(Constants.INTERVAL).append(" min'");
			pstmt = con.prepareStatement(sbQuery.toString());
			pstmt.setString(1, "Inactive");
			pstmt.executeUpdate();
		} catch (Throwable t) {
			LogManager.errorLog(t);
			throw t;
		} finally {
			DataBaseManager.close(pstmt);
			pstmt = null;

			UtilsFactory.clearCollectionHieracy( sbQuery );
		}
	}
	
	public int getMaxMeasurementPerMonth(Connection con, long userId, JSONObject jsonObject) throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rst = null;
		StringBuilder sbQuery = new StringBuilder();
		int nUserTotalRuncount = 0;
		Date dateLog = LogManager.logMethodStart();

		try {
			sbQuery	.append("SELECT sum_measurements_used_today as node_total_runcount ")
					.append("FROM usermaster WHERE user_id = ? ");
			pstmt = con.prepareStatement(sbQuery.toString());
			pstmt.setLong(1, userId);
			rst = pstmt.executeQuery();

			if(rst.next()) {
				nUserTotalRuncount = rst.getInt("node_total_runcount");
			}
		} catch (Exception e) {
			LogManager.errorLog(e);
			throw e;
		} finally {
			LogManager.logMethodEnd(dateLog);
			DataBaseManager.close(rst);
			rst = null;
			DataBaseManager.close(pstmt);
			pstmt = null;

			UtilsFactory.clearCollectionHieracy( sbQuery );
		}
		
		return nUserTotalRuncount;
	}
	
	public JSONObject getUserDetails(Connection con, long userId){
		JSONObject joUserDetails = null;
		ResultSet rstUser = null, rst = null;
		Statement stmtUser = null;
		PreparedStatement pstmt = null;
		StringBuilder sbQuery = new StringBuilder();
		Date dateLog = LogManager.logMethodStart();
		try {
			sbQuery	.append("SELECT license_level FROM usermaster ")
					.append("WHERE user_id="+userId);
			
			stmtUser = con.createStatement();
			rstUser = stmtUser.executeQuery( sbQuery.toString() );
			
			if(rstUser.next()){
				joUserDetails = new JSONObject();
				joUserDetails = getUserLicenseDetails(con, userId, joUserDetails, rstUser.getString("license_level"));
				
				// To get Max Measurement from License Table
//				sbQuery.setLength(0);
//				sbQuery.append("SELECT * FROM sum_config_parameters WHERE lic_internal_name=?");
//				pstmt = con.prepareStatement( sbQuery.toString() );
//				pstmt.setString(1, rstUser.getString("license_level"));
//				rst = pstmt.executeQuery();
//				if (rst.next()) {
//					joUserDetails.put("max_measurement_per_month", rst.getLong("max_measurement_per_month"));
//				}
			}
				
		} catch (Exception e) {
			LogManager.errorLog(e);
		} finally {
			LogManager.logMethodEnd(dateLog);
			DataBaseManager.close(rstUser);
			rstUser = null;
			DataBaseManager.close(stmtUser);
			stmtUser = null;
			DataBaseManager.close(rst);
			rst = null;
			DataBaseManager.close(pstmt);
			pstmt = null;
			UtilsFactory.clearCollectionHieracy( sbQuery );
		}
		return joUserDetails;
	}


	public void deactivateTest(Connection con, long userId) {
		PreparedStatement pstmt = null;
		StringBuilder sbQuery = new StringBuilder();
		Date dateLog = LogManager.logMethodStart();
		
		try {
			sbQuery	.append("update sum_test_master SET status = false ")
					.append("where user_id=?");
			pstmt = con.prepareStatement(sbQuery.toString());
			pstmt.setLong(1, userId);
			pstmt.executeUpdate();
		} catch (Throwable t) {
			LogManager.errorLog(t);
		} finally {
			LogManager.logMethodEnd(dateLog);
			DataBaseManager.close(pstmt);
			pstmt = null;
			
			UtilsFactory.clearCollectionHieracy( sbQuery );
		}
	}
	
	public JSONObject getUserLicenseDetails(Connection con, long userId, JSONObject joUserDetails, String licLevel){
		ResultSet rst = null;
		PreparedStatement pstmt = null;
		StringBuilder sbQuery = new StringBuilder();
		Date dateLog = LogManager.logMethodStart();
		try {
			
			if( ! licLevel.equals("level0") ) {
			// For Paid User
				sbQuery	.append("SELECT MIN(start_date) AS start_date, MAX(end_date) AS end_date, SUM(sum_desktop_max_measurements) AS sum_desktop_max_measurements FROM userwise_lic_monthwise WHERE user_id = ? AND ")
						.append("module_type = 'SUM' AND start_date::date <= now()::date AND end_date::date >= now()::date ");
			} else {
			// For Free User
				sbQuery	.append("SELECT created_on AS start_date, sum_desktop_max_measurements FROM usermaster WHERE user_id = ? ");
			}
			
			pstmt = con.prepareStatement( sbQuery.toString() );
			pstmt.setLong(1, userId);
			rst = pstmt.executeQuery();
			if (rst.next()) {
				if( rst.getString("start_date")!= null ){
					if( ! licLevel.equals("level0") ) {
						joUserDetails.put("end_date", rst.getTimestamp("end_date").getTime());
					}
					joUserDetails.put("start_date", rst.getTimestamp("start_date").getTime());
					joUserDetails.put("max_measurement_per_day", rst.getLong("sum_desktop_max_measurements"));
					joUserDetails.put("licLevel", licLevel);
				}
			}
		} catch (Exception e) {
			LogManager.errorLog(e);
		} finally{
			LogManager.logMethodEnd(dateLog);
			DataBaseManager.close(rst);
			rst = null;
			DataBaseManager.close(pstmt);
			pstmt = null;
		}
		return joUserDetails;
	}


	@SuppressWarnings("deprecation")
	public long insertHarTable(Connection con, long testId, int statusCode, String statusText, String runTestCode, String location, String testUrl) {
		PreparedStatement pstmt = null, stmt = null;
		StringBuilder sbQuery = new StringBuilder();
		long harId = 0;
		Date dateLog = LogManager.logMethodStart();
		
		try {
			sbQuery	.append("INSERT INTO sum_har_test_results (test_id, starttimestamp, run_test_code, status_code, status_text, location, location_name, browser_name, connection_name, testurl ) VALUES (?, now(), ?, ?, ?, ?, ?, ?, ?, ?) ");
			pstmt = con.prepareStatement(sbQuery.toString(), PreparedStatement.RETURN_GENERATED_KEYS);
			pstmt.setLong(1, testId);
			pstmt.setString(2, runTestCode);
			pstmt.setInt(3, statusCode);
			pstmt.setString(4, statusText);
			pstmt.setString(5, location);
			pstmt.setString(6, location.split(":")[0]);
			pstmt.setString(7, location.split(":")[1].split("\\.")[0]);
			pstmt.setString(8, location.split(":")[1].split("\\.")[1]);
			if (testUrl == null) {
				pstmt.setNull(9, Types.VARCHAR);
			} else {
				pstmt.setString(9, testUrl);
			}
			pstmt.executeUpdate();
			harId = DataBaseManager.returnKey(pstmt);
			
			sbQuery.setLength(0);
			sbQuery	.append("update sum_test_master set last_run_detail = now() where test_id = ?");
			stmt = con.prepareStatement(sbQuery.toString());
			stmt.setLong(1, testId);
			stmt.executeUpdate();
		} catch (Exception e) {
			LogManager.errorLog(e);
		} finally{
			LogManager.logMethodEnd(dateLog);
			DataBaseManager.close(pstmt);
			pstmt = null;
			DataBaseManager.close(stmt);
			stmt = null;
			UtilsFactory.clearCollectionHieracy( sbQuery );
		}
		return harId;
	}


	public void updateHarTable(Connection con, long testId, int statusCode, String statusText, String runTestCode, int loadTime, int repeatLoadTime) {
		PreparedStatement pstmt = null;
		StringBuilder sbQuery = new StringBuilder();
		Date dateLog = LogManager.logMethodStart();
		try {
			sbQuery	.append("UPDATE sum_har_test_results SET status_code = ?, status_text = ?, pageloadtime = ?, pageloadtime_repeatview = ? WHERE test_id = ? AND run_test_code = ? ");
			pstmt = con.prepareStatement(sbQuery.toString());
			pstmt.setInt(1, statusCode);
			pstmt.setString(2, statusText);
			if( loadTime == 0 ){
				pstmt.setNull(3, Types.INTEGER);
			} else {
				pstmt.setInt(3, loadTime);
			}
			if( repeatLoadTime == 0 ){
				pstmt.setNull(4, Types.INTEGER);
			} else {
				pstmt.setInt(4, repeatLoadTime);
			}
			pstmt.setLong(5, testId);
			pstmt.setString(6, runTestCode);
			
			pstmt.executeUpdate();
		} catch (Exception e) {
			LogManager.errorLog(e);
		} finally{
			LogManager.logMethodEnd(dateLog);
			DataBaseManager.close(pstmt);
			pstmt = null;
			UtilsFactory.clearCollectionHieracy( sbQuery );
		}
	}
	
	public void updateHarTable(Connection con, long testId, int statusCode, String statusText, String runTestCode, int loadTime, int repeatLoadTime, JSONObject joTestResValue) {
		PreparedStatement pstmt = null;
		StringBuilder sbQuery = new StringBuilder();
		Date dateLog = LogManager.logMethodStart();
		try {
			sbQuery	.append("UPDATE sum_har_test_results SET status_code = ?, status_text = ?, pageloadtime = ?, pageloadtime_repeatview = ?, test_result = ? WHERE test_id = ? AND run_test_code = ? ");
			pstmt = con.prepareStatement(sbQuery.toString());
			pstmt.setInt(1, statusCode);
			pstmt.setString(2, statusText);
			if( loadTime == 0 ){
				pstmt.setNull(3, Types.INTEGER);
			} else {
				pstmt.setInt(3, loadTime);
			}
			if( repeatLoadTime == 0 ){
				pstmt.setNull(4, Types.INTEGER);
			} else {
				pstmt.setInt(4, repeatLoadTime);
			}
			pstmt.setObject(5, UtilsFactory.getPgObject(joTestResValue.toString()));
			pstmt.setLong(6, testId);
			pstmt.setString(7, runTestCode);
			
			pstmt.executeUpdate();
		} catch (Exception e) {
			LogManager.errorLog(e);
		} finally{
			LogManager.logMethodEnd(dateLog);
			DataBaseManager.close(pstmt);
			pstmt = null;
			UtilsFactory.clearCollectionHieracy( sbQuery );
		}
	}
	
	public void updateHarFileNameInTable(Connection con, long testId, String runTestCode, String harFileName) {
		PreparedStatement pstmt = null;
		StringBuilder sbQuery = new StringBuilder();
		Date dateLog = LogManager.logMethodStart();
		try {
			sbQuery	.append("UPDATE sum_har_test_results SET harfilename = ?, received_on = now() WHERE test_id = ? AND run_test_code = ? ");
			pstmt = con.prepareStatement(sbQuery.toString());
			pstmt.setString(1, harFileName);
			pstmt.setLong(2, testId);
			pstmt.setString(3, runTestCode);
			pstmt.executeUpdate();
			
		} catch (Exception e) {
			LogManager.errorLog(e);
		} finally{
			LogManager.logMethodEnd(dateLog);
			DataBaseManager.close(pstmt);
			pstmt = null;
			UtilsFactory.clearCollectionHieracy( sbQuery );
		}
	}
	
	public void updateSumTestLastRunDetail(Connection con, long testId){
		PreparedStatement pstmt = null;
		StringBuilder sbQuery = new StringBuilder();
		Date dateLog = LogManager.logMethodStart();
		try {
			sbQuery	.append("update sum_test_master set last_run_detail = now() where test_id = ?");
			pstmt = con.prepareStatement(sbQuery.toString());
			pstmt.setLong(1, testId);
			pstmt.executeUpdate();
			
		} catch (Exception e) {
			LogManager.errorLog(e);
		} finally{
			LogManager.logMethodEnd(dateLog);
			DataBaseManager.close(pstmt);
			pstmt = null;
			UtilsFactory.clearCollectionHieracy( sbQuery );
		}
	}
	
	public void updateMeasurementCntInUserMaster(Connection con, long testId){
		PreparedStatement pstmt = null;
		StringBuilder sbQuery = new StringBuilder();
		Date dateLog = LogManager.logMethodStart();
		try {
			sbQuery	.append("UPDATE usermaster SET sum_measurements_used_today = sum_measurements_used_today + 1 WHERE user_id = (SELECT user_id FROM sum_test_master WHERE test_id = ?) ");
			pstmt = con.prepareStatement(sbQuery.toString());
			pstmt.setLong(1, testId);
			pstmt.executeUpdate();
			
		} catch (Exception e) {
			LogManager.errorLog(e);
		} finally{
			LogManager.logMethodEnd(dateLog);
			DataBaseManager.close(pstmt);
			pstmt = null;
			UtilsFactory.clearCollectionHieracy( sbQuery );
		}
	}
	
	public void resetMeasurements(Connection con){
		PreparedStatement pstmt = null;
		StringBuilder sbQuery = new StringBuilder();
		Date dateLog = LogManager.logMethodStart();
		try {
			sbQuery	.append("UPDATE usermaster SET sum_measurements_used_today = 0 ");
			pstmt = con.prepareStatement(sbQuery.toString());
			pstmt.executeUpdate();
			
		} catch (Exception e) {
			LogManager.errorLog(e);
		} finally{
			LogManager.logMethodEnd(dateLog);
			DataBaseManager.close(pstmt);
			pstmt = null;
			UtilsFactory.clearCollectionHieracy( sbQuery );
		}
	}


	public void insertResultJson(Connection con, org.json.JSONObject joData, long harId) {
		PreparedStatement pstmt = null;
		StringBuilder sbQuery = new StringBuilder();
		Date dateLog = LogManager.logMethodStart();
		try {
			sbQuery	.append("UPDATE sum_har_test_results SET json_result = ? WHERE id = ? ");
			pstmt = con.prepareStatement(sbQuery.toString());
			pstmt.setObject(1, UtilsFactory.getPgObject(joData.toString()));
			pstmt.setInt(2, ((Long) harId).intValue());
			pstmt.executeUpdate();
			
		} catch (Exception e) {
			LogManager.errorLog(e);
		} finally{
			LogManager.logMethodEnd(dateLog);
			DataBaseManager.close(pstmt);
			pstmt = null;
			UtilsFactory.clearCollectionHieracy( sbQuery );
		}
	}
	
	/**
	 * Retrieves all the Existing WPT agents.
	 * 
	 * @param con
	 * @return HashSet<String>
	 * @throws Exception
	 */
	public HashSet<String> extractExistingAgents(Connection con) throws Exception {
		Date dateLog = LogManager.logMethodStart();
		HashSet<String> retrivedloc = new HashSet<String>();
		ResultSet rs = null;
		Statement stmt = null;
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT country||'-'||'-'||city AS loc FROM sum_node_details");
			while (rs.next()) {
				retrivedloc.add(rs.getString(1).trim());
			}
		} catch (Exception e) {
			LogManager.errorLog(e);
			throw e;
		} finally {
			LogManager.logMethodEnd(dateLog);
			DataBaseManager.close(rs);
			rs = null;
			DataBaseManager.close(stmt);
			stmt = null;
		}
		return retrivedloc;
	}
	
	/**
	 * Retrieves the active WPT agents.
	 * 
	 * @param con
	 * @return HashSet<String>
	 * @throws Exception
	 */
	public HashSet<String> extractActiveAgents(Connection con) throws Exception {
		Date dateLog = LogManager.logMethodStart();
		HashSet<String> retrivedActiveAgents = new HashSet<String>();
		ResultSet rs = null;
		Statement stmt = null;
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT country||'-'||'-'||city as loc FROM sum_node_details WHERE sum_node_status = 'active'");
			while (rs.next()) {
				retrivedActiveAgents.add(rs.getString(1).trim());
			}
		} catch (Exception e) {
			LogManager.errorLog(e);
			throw e;
		} finally {
			LogManager.logMethodEnd(dateLog);
			DataBaseManager.close(rs);
			rs = null;
			DataBaseManager.close(stmt);
			stmt = null;
		}
		return retrivedActiveAgents;
	}
	
	/**
	 * Insert New WPT Desktop Agents.
	 * 
	 * @param con
	 * @param locToUpadate
	 * @return boolean
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	public boolean insertNewDesktopAgents(Connection con, Set < String > locToUpadate) throws Exception {
		PreparedStatement pstmt = null;
		PreparedStatement pstmt1 = null;
		StringBuilder sbQuery = null;
		long lKeyId = -1l;
		boolean bReturn=false;
		Date dateLog = LogManager.logMethodStart();
		try {
			for (String locToinsert: locToUpadate) {
				sbQuery = new StringBuilder();
				sbQuery	.append("INSERT INTO sum_node_details (")
						.append("sum_user_id, mac_address, agent_type, ipaddress, city, state, country, latitude, ")
						.append("longitude, selenium_webdriver_version, jre_version, firebug_version, netexport_version, ")
						.append("os_type, operating_system, os_version, chrome_version, created_by, created_on, sum_node_status, sum_agent_version) ")
						.append("VALUES (1, '")
						.append(1 - new Random().nextInt())
						.append("', 'wpt_Desktop_Agent', 'NA', '")
						.append(locToinsert.split("--")[1])
						.append("', 'NA', '")
						.append(locToinsert.split("--")[0])
						.append("', '0.0', '0.0', 'NA', 'NA', 'NA', 'NA', 'Windows', 'windows server 2008 r2', '3.13.0-32-generic'")
						.append(", 'NA', -1, NOW(), 'active', 'null-1.0.13')");
				
				pstmt = con.prepareStatement(sbQuery.toString(), PreparedStatement.RETURN_GENERATED_KEYS);
				pstmt.executeUpdate();
				lKeyId = DataBaseManager.returnKey(pstmt);
				
				if(lKeyId != -1l){
					sbQuery.setLength(0);
					sbQuery	.append("INSERT INTO sum_node_device_os_browser (sum_node_id,device_os_browser_id) SELECT node_id,dob_id FROM sum_node_details,")
							.append("sum_device_os_browser WHERE node_id = ").append(lKeyId)
							.append(" AND dob_id IN (SELECT dob_id FROM sum_device_os_browser WHERE device_type = 'DESKTOP')");

					LogManager.infoLog("frequent mail triggered in test environment : Insert query 1 "+sbQuery.toString());
					pstmt1 = con.prepareStatement(sbQuery.toString());
					int count = pstmt1.executeUpdate();
					if(count > 0){
						bReturn = true;
					}
				}
				UtilsFactory.clearCollectionHieracy( sbQuery );
				DataBaseManager.close(pstmt);
				DataBaseManager.close(pstmt1);
				pstmt = null;
				pstmt1=null;
				lKeyId = -1l;
			}
			LogManager.infoLog("New WPT_Desktop_Agent Have Been Inserted at "+new Date());
		} catch (Exception e) {
			LogManager.errorLog(e);
			throw e;
		}finally{
			LogManager.logMethodEnd(dateLog);
			DataBaseManager.close(pstmt);
			DataBaseManager.close(pstmt1);
			pstmt = null;
			pstmt1=null;
			UtilsFactory.clearCollectionHieracy( sbQuery );
		}
		return bReturn;
	}
	
	/**
	 * Insert New WPT Mobile Agents. 
	 * 
	 * @param con
	 * @param locToUpadate
	 * @return boolean
	 * @throws Exception 
	 */
	@SuppressWarnings("deprecation")
	public boolean insertNewMobileAgents(Connection con, Set < String > locToUpadate) throws Exception {
		PreparedStatement pstmt = null;
		PreparedStatement pstmt1 = null;
		StringBuilder sbQuery = null;
		long lKeyId = -1l;
		boolean bReturn=false;
		Date dateLog = LogManager.logMethodStart();
		try {
			for (String locToinsert: locToUpadate) {
				sbQuery = new StringBuilder();
				sbQuery	.append("INSERT INTO sum_node_details (")
						.append("sum_user_id, mac_address, agent_type, ipaddress, city, state, country, latitude,")
						.append("longitude, selenium_webdriver_version, jre_version, firebug_version, netexport_version, ")
						.append("os_type, operating_system, os_version, chrome_version, created_by, created_on, sum_node_status, sum_agent_version) ")
						.append("VALUES (1, '")
						.append(1 - new Random().nextInt())
						.append("', 'wpt_Mobile_Agent', 'NA', '")
						.append(locToinsert.split("--")[1])
						.append("', 'NA', '")
						.append(locToinsert.split("--")[0])
						.append("', '0.0', '0.0', 'NA', 'NA', 'NA', 'NA', 'ANDROID', 'ANDROID', '3.13.0-32-generic'")
						.append(", 'NA', -1, now(), 'active', 'null-1.0.13')");
				
				pstmt = con.prepareStatement(sbQuery.toString(), PreparedStatement.RETURN_GENERATED_KEYS);
				pstmt.executeUpdate();
				lKeyId = DataBaseManager.returnKey(pstmt);
				if(lKeyId != -1l){
					sbQuery.setLength(0);
					sbQuery	.append("INSERT INTO sum_node_device_os_browser (sum_node_id, device_os_browser_id) SELECT node_id, dob_id FROM sum_node_details,")
							.append("sum_device_os_browser WHERE node_id = ")
							.append(lKeyId)
							.append(" AND dob_id IN (SELECT dob_id FROM sum_device_os_browser WHERE device_type='MOBILE')");
					pstmt1 = con.prepareStatement(sbQuery.toString());
					int count = pstmt1.executeUpdate();
					if(count > 0){
						bReturn = true;
					}
				}
				UtilsFactory.clearCollectionHieracy( sbQuery );
				DataBaseManager.close(pstmt);
				DataBaseManager.close(pstmt1);
				pstmt = null;
				pstmt1=null;
				lKeyId = -1l;
			}
			LogManager.infoLog("New WPT_Mobile_Agent Has Been Inserted at "+new Date());
		} catch (Exception ex) {
			LogManager.errorLog(ex);
			throw ex;
		}finally{
			LogManager.logMethodEnd(dateLog);
			DataBaseManager.close(pstmt);
			DataBaseManager.close(pstmt1);
			pstmt = null;
			pstmt1=null;
			UtilsFactory.clearCollectionHieracy( sbQuery );
		}
		return bReturn;
	}
	
	/**
	 * To update all Inactive WPT agent's status.
	 * 
	 * @param activeLocations
	 * @param con
	 * @throws Exception
	 */
	public void updateInactiveAgents(String activeLocations, Connection con) throws Exception {
		Date dateLog = LogManager.logMethodStart();
		PreparedStatement pstmt = null;
		StringBuilder sbQuery = new StringBuilder();
		try {
			sbQuery	.append("UPDATE sum_node_details SET sum_node_status = CASE WHEN country||'-'||'-'||city IN(")
					.append(activeLocations)
					.append(") THEN 'active' ELSE 'Inactive' END,modified_on = CASE WHEN country||'-'||'-'||city IN(")
					.append(activeLocations)
					.append(") THEN now() END");
			pstmt = con.prepareStatement(sbQuery.toString());
			pstmt.executeUpdate();
			LogManager.infoLog("WPT_Agent Status Have Been updated at "+new Date());
		} catch (Exception e) {
			LogManager.errorLog(e);
			throw e;
		}finally{
			LogManager.logMethodEnd(dateLog);
			DataBaseManager.close(pstmt);
			pstmt = null;
			UtilsFactory.clearCollectionHieracy( sbQuery );
		}
	}
	
	/**
	 * To update all the WPT agents as inactive when the 'https://wpt.appedo.com/getLocations.php' has
	 * no locations in it.
	 * 
	 * @param con
	 * @throws Exception 
	 */
	public void updateAllAgentsInactive(Connection con) throws Exception {
		Date dateLog = LogManager.logMethodStart();
		PreparedStatement pstmt = null;
		StringBuilder sbQuery = new StringBuilder();
		try {
			sbQuery.append("UPDATE sum_node_details SET sum_node_status = 'Inactive'");
			pstmt = con.prepareStatement(sbQuery.toString());
			pstmt.executeUpdate();
			LogManager.infoLog("All WPT_Agent's status have been updated to Inactive on : " + new Date());
		} catch (Exception ex) {
			LogManager.errorLog(ex);
			throw ex;
		}finally{
			LogManager.logMethodEnd(dateLog);
			DataBaseManager.close(pstmt);
			pstmt = null;
			UtilsFactory.clearCollectionHieracy( sbQuery );
		}
	}

}

