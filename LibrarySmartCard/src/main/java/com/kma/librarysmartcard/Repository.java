/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kma.librarysmartcard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author admin
 */


public class Repository {
    
    Connection conn = null;
    
    public Repository() {
        conn = DbHelper.getConn();
    }
    
    public void insertUser(String id, String ten, String lop, String ngayString, String diachi) throws SQLException {
      try{
          String query = " insert into users (id, ten, lop, ngaysinh, diachi)"
        + " values (?, ?, ?, ?, ?)";

      // create the mysql insert preparedstatement
      PreparedStatement preparedStmt = conn.prepareStatement(query);
      preparedStmt.setString (1, id);
      preparedStmt.setString (2, ten);
      preparedStmt.setString(3, lop);
      preparedStmt.setString(4, ngayString);
      preparedStmt.setString(5, diachi);

      // execute the preparedstatement
      preparedStmt.execute();;
      }catch(Exception e){
          e.printStackTrace();
      }
    }
    
     public boolean insertPubkey(String id, String modul,String expo ) throws SQLException {
      try{
          String query;
          query = "UPDATE users SET modulus = ?, exponent = ? WHERE id = ?";

      // create the mysql insert preparedstatement
      PreparedStatement preparedStmt = conn.prepareStatement(query);
      preparedStmt.setString (1, modul);
      preparedStmt.setString (2, expo);
      preparedStmt.setString (3, id);
      // execute the preparedstatement
      boolean check = preparedStmt.execute();
      return check;
      }catch(Exception e){
          e.getMessage();
      }
      return false;
    }
     
      public void insertHistory(String id, String sach, String date) throws SQLException {
          System.out.println("id: " + id);
          System.out.println("sach: " + sach);
          System.out.println("date: " + date);

      try{
          String query = "insert into trasach (id, sach, ngaytra)"
        + " values (?, ?, ?)";

      // create the mysql insert preparedstatement
      PreparedStatement preparedStmt = conn.prepareStatement(query);
      preparedStmt.setString (1, id);
      preparedStmt.setString (2, sach);
      preparedStmt.setString (3, date);
      // execute the preparedstatement
        preparedStmt.execute();
     
      }catch(Exception e){
          e.getMessage();
          System.out.println("err: " + e.getMessage());
      }
    
    }
    
    public String getModul(String id) throws SQLException{
        String modul = null;
        PreparedStatement stmt = conn.prepareStatement("select modulus from users where id = ?");  
        stmt.setString (1, id);
        ResultSet rs=stmt.executeQuery();  
        while(rs.next()){  
            modul = rs.getString(1);
        }  
        
        return modul;
    }
    
    public String getExpo(String id) throws SQLException{
        String exponent = null;
        PreparedStatement stmt = conn.prepareStatement("select exponent from users where id = ?");  
        stmt.setString (1, id);
        ResultSet rs=stmt.executeQuery();  
        while(rs.next()){  
            exponent = rs.getString(1);
        }  
        
        return exponent;
    }
}
