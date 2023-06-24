/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kma.librarysmartcard;

import java.math.BigInteger;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;
import javax.swing.JOptionPane;

/**
 *
 * @author admin
 */
public class APDUGenerator {
    static final byte CLA_APPLET = (byte) 0x80;

    public static final byte[] AID_APPLET = {(byte)0x11,(byte)0x22,(byte)0x33,(byte)0x44,(byte)0x55,(byte)0x00};
    private Card card;
    private TerminalFactory factory;
    private CardChannel channel;
    private CardTerminal terminal;
    private List<CardTerminal> terminals;
    private ResponseAPDU response;
    private CommandAPDU cmd;
    //private ResponseAPDU rpd;
    
    private static final byte INS_PIN = 0x00;
    private static final byte INS_INIT = 0x01;
    private static final byte VALIDATE_PIN_INS = 0x02;
    private final byte  INS_PIN_TRIES 			= (byte) 0x03;
    private final byte INS_UPDATE_PIN 			= (byte) 0x04;
    private final byte INS_UNBLOCK_PIN			= (byte) 0x05;
    private final byte INS_WRITE_USER_DATA 		= (byte) 0x06;
    private final static byte INS_GET_USER_DATA = (byte) 0x07;
    private final static byte INS_UPLOAD_IMG = (byte) 0x08;
    private final static byte INS_GET_IMG = (byte) 0x09;
    private final static byte INS_UPDATE_INFO = (byte) 0x10;
    private final static byte INS_MUON_SACH = (byte) 0x11;
    private final static byte INS_HIEN_THI_SACH = (byte) 0x12;
    private final static byte INS_TRA_SACH = (byte) 0x13;
    final static byte INS_LICH_SU = (byte) 0x14;
    final static byte INS_HIEN_THI_LICH_SU = (byte) 0x15;
    final static byte INS_GET_PUBKEY = (byte) 0x16;
    final static byte INS_SIGN = (byte) 0x17;
    // SW WORDS
    private static final int SW_OK = 0x9000;
    
    private String byteToStr(byte[] in) {
        StringBuilder out = new StringBuilder();
        for (byte b : in) {
            out.append("0x"+String.format("%02X ", b));
        }
        return out.toString();
    }
    
    public byte[] hexStringToByteArray(String s) {
        int len = s.length();
        System.out.println("len "+len);
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    
    public boolean checkPin(String pin){
        try{
			// CMD Aufbau: (CLA, INS, P1, P2, Data)
            cmd = new CommandAPDU(CLA_APPLET,(byte)0x02,0,0,pin.getBytes());
            response = channel.transmit(cmd);
            if(response.getSW()==SW_OK) return true;
		} catch (CardException e){
			return false;
		}
		return false;
	}
    
    public String getPin(){
        try{
			// CMD Aufbau: (CLA, INS, P1, P2, Data)
            cmd = new CommandAPDU(CLA_APPLET,(byte)0x00,0,0);
            response = channel.transmit(cmd);
            String res = String.format("%x", new BigInteger(1, response.getData()));
            System.out.println("response "+ res);
            
            return res;
        } catch (CardException e){
			JOptionPane.showMessageDialog(null, e);
	}
            return "";
    }
	
    public int updatePin(String pin){
	try{			
			// CMD Aufbau: (CLA, INS, P1, P2, Data)       
            cmd = new CommandAPDU(CLA_APPLET,INS_UPDATE_PIN,0,0, pin.getBytes());
            response = channel.transmit(cmd);
               
            if(response.getSW()==SW_OK){
		return 99;
            }else{
		return response.getSW2()&0x0F;
            }
	} catch (CardException e){
           return -1;
	}
    }
	
	public int resetPin(){
            String puk = "123456";
		try{
			cmd = new CommandAPDU(CLA_APPLET,(byte) 0x05,0,0,puk.getBytes());
			response = channel.transmit(cmd);
			if(response.getSW()==SW_OK){
				return 99;
			}else{
				return response.getSW2()&0x0F;
			}
		} catch (CardException e){
			return -1;
		}
	}
//	
//	
	public int getRemainingTries(){
		try{
			cmd = new CommandAPDU(CLA_APPLET,(byte)0x02,0,0);
			response = channel.transmit(cmd);
			int leftTries = response.getSW2()&0x0F;
			return leftTries;
		}catch (Exception e){
			return -1;
		}		
	}	
//        7
    
      public String muonSach(byte[] data) {
        try {
            response = channel.transmit(new CommandAPDU(CLA_APPLET, INS_MUON_SACH, (byte) 0x01, (byte) 0x01,data));
            
            String res = String.format("%x", new BigInteger(1, response.getData()));
            System.out.println("response "+ res);
            return res;
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
        }
        return "";
    }
    
      public String hienThiSachMuon() {
        try {
            response = channel.transmit(new CommandAPDU(CLA_APPLET, INS_HIEN_THI_SACH, (byte) 0x01, (byte) 0x01));
            
            String res = String.format("%x", new BigInteger(1, response.getData()));
            System.out.println("response "+ res);
            return res;
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
        }
        return "";
    }
    
       public boolean traSach(byte[] data) {
        try {
            response = channel.transmit(new CommandAPDU(CLA_APPLET, (byte) 0x13, (byte) 0x01, (byte) 0x01,data));         
            System.out.println("response "+ response);
            String check = Integer.toHexString(response.getSW());
            if (check.equals("9000")) {
               return true;
            }
            else{ 
               return false;
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
        }
        return false;
    }
    public boolean traSachCuoiCung() {
        try {
            response = channel.transmit(new CommandAPDU(CLA_APPLET, INS_TRA_SACH, (byte) 0x02, (byte) 0x01));
            
            System.out.println("response "+ response);
            String check = Integer.toHexString(response.getSW());
            if (check.equals("9000")) {
               return true;
            }
            else{ 
               return false;
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
        }
        return false;
    }
    
     public boolean themLichSu(byte[] data) {
        try {
            response = channel.transmit(new CommandAPDU(CLA_APPLET, INS_LICH_SU, (byte) 0x01, (byte) 0x01,data));
            String check = Integer.toHexString(response.getSW());
            if (check.equals("9000")) {
               return true;
            }
            else{ 
               return false;
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
        }
        return false;
    }
    
    public String hienThiLichSu() {
        try {
            response = channel.transmit(new CommandAPDU(CLA_APPLET, INS_HIEN_THI_LICH_SU, (byte) 0x01, (byte) 0x01));
            
            String res = String.format("%x", new BigInteger(1, response.getData()));
            System.out.println("response "+ res);
            return res;
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
        }
        return "";
    }
    
    public BigInteger getModulusPubkey() {
        try {
            response = channel.transmit(new CommandAPDU(CLA_APPLET,INS_GET_PUBKEY, (byte) 0x01, (byte) 0x01));
            
            BigInteger res = new BigInteger(1, response.getData());
            System.out.println("responseM "+ res);
            return res;
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
        }
        return null;
    }
    public BigInteger getExponentPubkey() {
        try {
            response = channel.transmit(new CommandAPDU(CLA_APPLET,INS_GET_PUBKEY, (byte) 0x02, (byte) 0x01));
            
            BigInteger res = new BigInteger(1, response.getData());
            System.out.println("responseE "+ res);
            return res;
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
        }
        return null;
    }
    
     public String getSign(byte[] data) {
        try {
            response = channel.transmit(new CommandAPDU(CLA_APPLET, INS_SIGN, (byte) 0x01, (byte) 0x01,data));
            String check = Integer.toHexString(response.getSW());
            if (check.equals("9000")) {
                String res = String.format("%x", new BigInteger(1, response.getData()));
                System.out.println("response "+ res);
                return res;
            }else if (check.equals("6984")) {
                
                return "";
            }         
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
        }
        return "";
    }
     
     public boolean initCard(byte[] data) {
        try {
            response = channel.transmit(new CommandAPDU(CLA_APPLET, INS_INIT, 0, 0, data));
            System.out.println("response "+ response);
            String check = Integer.toHexString(response.getSW());
            System.out.println("check "+ check);
            if (check.equals("9000")) {
               return true;
            }
            else if (check.equals("6400")) {
                JOptionPane.showMessageDialog(null, "Upload that bai");
                return true;
            }
            else{ 
               return false;
            }
            
        } catch (Exception e) {
        }
        return false;
    }
    
      public boolean updateInfo(byte[] data) {
        try {
            response = channel.transmit(new CommandAPDU(CLA_APPLET, INS_UPDATE_INFO, (byte) 0x01, (byte) 0x01,data));
            System.out.println("response "+ response);
            String check = Integer.toHexString(response.getSW());
            if (check.equals("9000")) {
               return true;
            }
            else if (check.equals("6400")) {
                JOptionPane.showMessageDialog(null, "Upload that bai");
                return true;
            }
            else{ 
               return false;
            }
            
        } catch (Exception e) {
        }
        return false;
    }
      
     public boolean connectCard() {
        try {            
            factory = TerminalFactory.getDefault();
            terminals = factory.terminals().list();
            System.out.println(terminals + " is list connect");
            terminal = terminals.get(1);
            System.out.println(terminal + " is connect");
            card = terminal.connect("T=0");
            channel = card.getBasicChannel();
            if(channel == null) {
                return false;
            }
            response = channel.transmit(new CommandAPDU(0x00, (byte)0xA4,0x04, 0x00, AID_APPLET));
            String check = Integer.toHexString(response.getSW());
            if(check.equals("9000")) {
                return true;
            }else if(check.equals("6400")) {
                JOptionPane.showMessageDialog(null, "the vo hieu hoa");
                return true;
            }else {
                return false;
            }
            
        }catch(Exception e){}
        return false;
    }
     
      public boolean disconnect() {
        try {
            card.disconnect(false);
            return true;
        }catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }
      
    public String getStatusWords() {
        return Integer.toHexString(response.getSW());
    }
    
   
    public static String byteArrayToHexString(byte[] b){
    String result = "";
    for (int i = 0; i < b.length; i++) {
        result +=
                Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
    }
    return result;
    }   
    
    public String getInfo() {
        try {
            //response = channel.transmit(new CommandAPDU((byte) 0x80, INS_GET_USER_DATA, (byte) 0x00, (byte) 0x00));  
            factory = TerminalFactory.getDefault();
            terminals = factory.terminals().list();
            terminal = terminals.get(1);
            card = terminal.connect("T=0");
            channel = card.getBasicChannel();
            if(channel == null) {
                return "0";
            }
            cmd = new CommandAPDU(CLA_APPLET,(byte)0x07,(byte)0x01,(byte)0x01);
	    response = channel.transmit(cmd);
            String res = String.format("%x", new BigInteger(1, response.getData()));
            System.out.println("response "+ res);
            return res;
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
        }
        return "";
    }
    
    public boolean uploadAvatar(byte[] data) {
        try {
            String dataLc = String.valueOf(data.length);
            
            
            System.out.println("ok "+ String.format("%x", new BigInteger(1, dataLc.getBytes())));
            response = channel.transmit(new CommandAPDU(CLA_APPLET, INS_UPLOAD_IMG, (byte) 0x01, (byte) 0x01, data));
            System.out.println("response "+ response);
            String check = Integer.toHexString(response.getSW());
            if (check.equals("9000")) {
               return true;
            }
            else if (check.equals("6400")) {
                
                return false;
            }
            else{ 
               return false;
            }
            
        } catch (Exception e) {
        }
        return false;
    }
    
    public String getAvatar() {
        try{
            cmd = new CommandAPDU(CLA_APPLET, (byte) 0x09, (byte) 0x01, (byte) 0x01);
            response = channel.transmit(cmd);
            //response = channel.transmit(new CommandAPDU(CLA_APPLET, (byte) 0x09, (byte) 0x01, (byte) 0x01));
            System.out.println("response ava"+ response);
            String res = String.format("%x", new BigInteger(1, response.getData()));
            String check = Integer.toHexString(response.getSW());
            if (check.equals("9000")) {
               return res;
            }
            else if (check.equals("6A83")) {
                return "";
            }
            else{ 
               return "";
            }          
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
            e.printStackTrace();
        }
        return "";
    }
    
    
    public boolean isCardPresent(){
		try{
			if (terminal.isCardPresent()) return true;
			else return false;
		}catch(Exception e){
			return false;
		}
		
	}
}
