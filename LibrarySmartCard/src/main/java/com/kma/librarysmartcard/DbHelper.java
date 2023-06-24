/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kma.librarysmartcard;

import java.sql.Connection;
import java.sql.DriverManager;
import javax.swing.JOptionPane;

/**
 *
 * @author admin
 */
public class DbHelper {
    private static Connection conn;
    public static Connection getConn() {
        if (conn==null) {  
            
          try {
           String url = new String();
           String user = new String();
           String password = new String();
           url = "jdbc:mysql://127.0.0.1:3306/thuvien";
           user = "root";
           password = "";
           
           DriverManager.registerDriver(new com.mysql.jdbc.Driver());
           conn = DriverManager.getConnection(url,user,password);
           JOptionPane.showMessageDialog(null,"Connection Successfuly");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "Connection Failed" +e);
                }       
        }
        return conn;
    }
}
