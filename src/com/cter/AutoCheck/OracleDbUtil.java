package com.cter.AutoCheck;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.cter.util.BaseLog;
import com.cter.util.LoadPropertiestUtil;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class OracleDbUtil {
    private static Map<String, String> otherMap = LoadPropertiestUtil.loadProperties("config/other.properties");
    private static String remedyDevHostIP=otherMap.get("remedyDevHostIP");
    private static String remedyProHostIP=otherMap.get("remedyProHostIP");
    private static String remedyDbPort=otherMap.get("remedyDbPort");
    private static String remedyServiceName=otherMap.get("remedyServiceName");
    private static String remedyDbUserName=otherMap.get("remedyDbUserName");
    private static String remedyDbPpassword=otherMap.get("remedyDbPpassword");
    static  BaseLog log= new BaseLog("CSCheckLog") ;

    public static Connection getConnection(String hostIP,String port,String serviceName,String userName,String password){
        Connection con = null;
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            String url = "jdbc:oracle:" + "thin:@(DESCRIPTION =" + "(ADDRESS_LIST ="
                    + "(ADDRESS = (PROTOCOL = TCP)(HOST = "+hostIP+")(PORT = "+port+"))" + ")" + "(CONNECT_DATA ="
                    + "(SERVICE_NAME = "+serviceName+")" + ")" + ")";
            con = DriverManager.getConnection(url, userName, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return con;
    }

    public static Connection getRemedyDevConnection(){
        String hostIP=remedyDevHostIP;
        String port=remedyDbPort;
        String serviceName=remedyServiceName;
        String userName=remedyDbUserName;
        String password=remedyDbPpassword;
        return  getConnection(hostIP,port,serviceName,userName,password);
    }

    public static Connection getRemedyProConnection(){
//        String hostIP=remedyDevHostIP;
        String hostIP=remedyProHostIP;
        String port=remedyDbPort;
        String serviceName=remedyServiceName;
        String userName=remedyDbUserName;
        String password=remedyDbPpassword;
        return  getConnection(hostIP,port,serviceName,userName,password);
    }

    public static void closeAll(Connection con,PreparedStatement pre,ResultSet results){
        closeResultSet(results);
        closePreparedStatement(pre);
        closeConnection(con);
    }

    public static void closeConnection(Connection con){
        try {
            if (con != null)
                con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void closePreparedStatement(PreparedStatement pre){
        try {
            if (pre != null)
                pre.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void closeResultSet(ResultSet results ){
        try {
            if (results != null)
                results.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 完成对数据库的表的添加删除和修改的操作
     * @param sql
     * @param params
     * @return
     * @throws SQLException
     */
    public static int executeUpdate(String sql, List<Object> params,Connection con){
        int result = 0;
        PreparedStatement pre =null;
        try {
            pre = con.prepareStatement(sql);
            if (params != null && !params.isEmpty()) {
                int index = 1;
                for (int i = 0; i < params.size(); i++) {
                    pre.setObject(index++, params.get(i));
                }
            }
            result = pre.executeUpdate();
        } catch (SQLException e) {
            log.printStackTrace(e);
            return 0;
        } finally {
            closeAll(null,pre,null);
        }
        return result;
    }


    /**
     * 从数据库中查询数据
     * @param sql		sql
     * @param params  ? 参数设值
     * @param con  ?  连接
     * @return
     * @throws SQLException
     */
    public static List<TreeMap<String, Object>> executeQueryMap(String sql,List<Object> params,Connection con)   {
        List<TreeMap<String, Object>> list= new LinkedList<TreeMap<String, Object>>();
        PreparedStatement pre=null;
        ResultSet results=null;
        try {
            int index = 1;
            pre = con.prepareStatement(sql);
            if (params != null && !params.isEmpty()) {
                for (int i = 0; i < params.size(); i++) {
                    pre.setObject(index++, params.get(i));
                }
            }
            results = pre.executeQuery();
            ResultSetMetaData metaData = results.getMetaData();
            int cols_len = metaData.getColumnCount();
            while (results.next()) {
                TreeMap<String, Object> map = new TreeMap<String, Object>();
                for (int i = 0; i < cols_len; i++) {
                    String cols_name = metaData.getColumnName(i + 1);
                    Object cols_value = results.getObject(cols_name);
                    if (cols_value == null) {
                        cols_value = "";
                    }
                    map.put(cols_name, cols_value);
                }
                list.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            closeAll(null,pre,results);
        }
        return list;
    }

    /**
     * jdbc的封装可以用反射机制来封装,把从数据库中获取的数据封装到一个类的对象里
     *
     * @param sql
     * @param params
     * @param cls
     * @return
     * @throws Exception
     */
    public static <T> List<T> executeQueryEntity(String sql, List<Object> params,  Class<T> cls,Connection con) {
        List<T> list = new ArrayList<T>();
        int index = 1;
        PreparedStatement pre=null;
        ResultSet results=null;

        try {
            pre = con.prepareStatement(sql);
            if (params != null && !params.isEmpty()) {
                for (int i = 0; i < params.size(); i++) {
                    pre.setObject(index++, params.get(i));
                }
            }
            results = pre.executeQuery();
            ResultSetMetaData metaData = results.getMetaData();
            int cols_len = metaData.getColumnCount();
            while (results.next()) {
                T resultObject = cls.newInstance();  // 通过反射机制创建实例
                for (int i = 0; i < cols_len; i++) {
                    String cols_name = metaData.getColumnName(i + 1);
                    Object cols_value = results.getObject(cols_name);
                    if (cols_value == null) {
                        cols_value = "";
                    }
                    Field field;
                    try {
                        field= cls.getDeclaredField(cols_name);
                    } catch (Exception e) {
                        field = 	cls.getDeclaredField( cols_name);
                    }
    //		                System.out.println(cols_value+"\t"+cols_name+"\t"+field.getType());
                    field.setAccessible(true); // 打开javabean的访问private权限
                    //如果返回没有值的时候BigDecimal和Long类型会转为String，所以要新加个对应的值
                    if(StrUtil.isBlank(cols_value.toString())){
                        if(field.getType().toString() .indexOf("BigDecimal")>-1){
                            field.set(resultObject, new BigDecimal(0));
                        }else if(field.getType().toString() .indexOf("Long")>-1 ){
                            field.set(resultObject, 0L);
                        }else if(field.getType().toString() .indexOf("Date")>-1 ){
                            field.set(resultObject, null);
                        }
                    }   else{
                        field.set(resultObject, cols_value);
                    }
                }
                list.add(resultObject);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } finally {
        }
        return list;
    }




    /**
     * 测试 增加 修改 删除方法的调用
     */
    public static void testInsertUpdateDelete(){

        Connection devConn= OracleDbUtil.getRemedyProConnection();
        System.out.println(devConn);

        String sql="declare\n" +
                "your_value_name clob := ?;  \n" +
                "begin\n" +
                " INSERT INTO TIDESTONE.CPCTIDES_HELPDESK_TICKET (HPD_CASE_ID, SYS_Create_User,TEMP_WORKLOG,TEST_RESULT,CS_FEEDBACK,NOC_FEEDBACK) VALUES (?,'Auto_Check_Type','Worklog',your_value_name,'0','0');\n" +
                "end;";
        List<Object> params=new ArrayList<>();
        params.add("这个是一个测试记录");
        params.add("HD0000002827608");
        int i= OracleDbUtil.executeUpdate(sql,params,devConn);
        System.out.println("i:"+i);
        OracleDbUtil.closeConnection(devConn);
    }

    /**
     * 测试查询 方法的调用
     */
    public static void testQuery(){
        Connection conn= OracleDbUtil.getRemedyProConnection();
        System.out.println(conn);
        String sql="select * from  aradmin.HPD_HELPDESK_ACT_VIEW";
        List<Object> params=new ArrayList<>();
        List<TreeMap<String, Object>> list= OracleDbUtil.executeQueryMap(sql,params,conn);
        TreeMap<String, Object> tempMap=list.get(0);
        for(String key : tempMap.keySet()){
            System.out.println(key);
        }

        for (int i = 0; i < list.size(); i++) {
            TreeMap<String, Object> map=list.get(i);
            for(Object value : tempMap.values()){
                System.out.print(value+"\t");
            }
            System.out.println();
        }

        System.out.println(JSONUtil.toJsonStr(list));

        OracleDbUtil.closeConnection(conn);
    }

    public static void main(String[] args) throws Exception{
//        testInsertUpdateDelete();
        testQuery();
    }


}
