package com.kma.thethuvien;

import javacard.framework.*;
import javacardx.apdu.*;
import javacard.security.*;
import javacardx.crypto.*;

public class Connect extends Applet implements ExtendedLength
{
	// cla
	private final static byte CARD_CLA = (byte) 0x80;
	// ins
	private static final byte INS_GET_PIN = 0x00;
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
	final static byte INS_MUON_SACH = (byte) 0x11;
	final static byte INS_HIEN_THI_SACH = (byte) 0x12;
	final static byte INS_TRA_SACH = (byte) 0x13;
	final static byte INS_LICH_SU = (byte) 0x14;
    final static byte INS_HIEN_THI_LICH_SU = (byte) 0x15;
	final static byte INS_GET_PUBKEY = (byte) 0x16;
	final static byte INS_SIGN = (byte) 0x17;
	
    private final static byte PIN_TRY_LIMIT = (byte) 0x03;
    private final static byte PIN_SIZE = (byte) 0x04;
   
    private OwnerPIN pin;
    private OwnerPIN puk;
	
	private static byte[] ma, hoten, ngaysinh, lop, diachi, newPin;
	private static short len_ma, len_hoten, len_ns, len_lop, len_diachi, len_pin;
	private static byte[] tempBuffer,subBuffer,changeBuffer,sigBuffer, hash;
	private static final byte [] DIVISION = { 0x23 };
	
     //Avatar
	private static byte[] img;
	private static short len_img;
	private static final short MAX_IMG_SIZE = (short)(0x7FFF);
	
     // signal that the PIN verification failed
    // Exception
	private final static short SW_VERIFICATION_FAILED = 0x6343;
	final static short  SW_PIN_FAILED =(short) 0x63C0; 
	private final static short SW_BOOK = 0x6343;
	  
	/* Khai bao tham so lien quan den bam */
	private static InitializedMessageDigest sha;
	
	// Khai bao tham so lien quan den AES
	private static final short aesBlock = (short)16;
	private static AESKey aesKey;
	private static Cipher cipher;
	private static short keyLen;
	private static byte [] keyData;
	
	
	// Khai bao tham so lien quan den RSA
	private static byte[] rsaPubKey, rsaPriKey;
	private static Signature rsaSig;
	private static short sigLen,rsaPubKeyLen, rsaPriKeyLen;
	
	// muon tra sach
	private static byte[] book,history;
	private static short len_book,len_history;
	private static final short MAX_HISTORY_SIZE = (short)(0x7FFF);
	private static short time;
	
	private static byte[] default_pin={(byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34};
	private static byte[] default_puk={(byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34,(byte) 0x35, (byte) 0x36};
	byte[] tmp;
	
	protected Connect() {
        pin = new OwnerPIN(PIN_TRY_LIMIT, PIN_SIZE);
		puk = new OwnerPIN((byte)6,(byte)6);
		puk.update(default_puk,(short)0,(byte)6);
		pin.update(default_pin,(short) 0,(byte) 4);  
        // register();
	}

    
	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{	
		new Connect().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
		ma = new byte[aesBlock];
        hoten = new byte[(short)(aesBlock*3)];
        ngaysinh = new byte[aesBlock];
        lop = new byte[aesBlock];
		diachi = new byte[(short)(aesBlock*3)];
        newPin = new byte[16];
        img = new byte[MAX_IMG_SIZE];
        book = new byte[128];
        history = new byte[MAX_HISTORY_SIZE];
        time = 0;
		Util.arrayFillNonAtomic(img,(short)0,MAX_IMG_SIZE,(byte)0x00);
		tempBuffer = JCSystem.makeTransientByteArray((short) 128, JCSystem.CLEAR_ON_DESELECT);
		subBuffer = JCSystem.makeTransientByteArray((short) 128, JCSystem.CLEAR_ON_DESELECT);
		changeBuffer = JCSystem.makeTransientByteArray((short) 128, JCSystem.CLEAR_ON_DESELECT);
		hash = JCSystem.makeTransientByteArray((short) 128, JCSystem.CLEAR_ON_DESELECT);
		Util.arrayCopy(default_pin, (short) 0, newPin, (short)0, (short)4);
	   
		sha = MessageDigest.getInitializedMessageDigestInstance(MessageDigest.ALG_SHA_256, false);

		//khoa ma hoa aes
		keyLen = (short) 16;
		keyData = new byte[aesBlock];
	

		cipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_ECB_NOPAD, false);
		aesKey = (AESKey)KeyBuilder.buildKey(KeyBuilder.TYPE_AES,(short)(8*keyLen), false);
		
		/* chu ki so */
		sigLen = (short)(KeyBuilder.LENGTH_RSA_1024/8); // Cai dat do dai khoa
		rsaSig = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1,false); //Cai dat giai thuat bam cho chu ky
		
		rsaPubKey = new byte[(short)(sigLen*2)];
        rsaPriKey = new byte[(short)(sigLen*2)];
        rsaPubKeyLen = 0;
        rsaPriKeyLen = 0;
        sigBuffer = JCSystem.makeTransientByteArray((short)(sigLen*2), JCSystem.CLEAR_ON_DESELECT);
	}
	
	public boolean select() {

        // The applet declines to be selected
        // if the pin is blocked.
        if (pin.getTriesRemaining() == 0) {
            return false;
        }

        return true;

    }// end of select method

    public void deselect() {

        // reset the pin value
        pin.reset();

    }

	public void process(APDU apdu)
	{
		
		if (selectingApplet())
		{
			return;
		}

		byte[] buffer = apdu.getBuffer();
		short len = apdu.setIncomingAndReceive();
		if(buffer[ISO7816.OFFSET_CLA] != CARD_CLA) ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
      
        switch (buffer[ISO7816.OFFSET_INS]) {
			case INS_GET_PIN:
				getPin(apdu);
				break;
			case INS_INIT:
				 init_card(apdu, len);
				 break;
            case VALIDATE_PIN_INS:
                validatePIN(apdu);
                break;
			case INS_PIN_TRIES:
				commandPinTries();
				break;
			case INS_UPDATE_PIN:
				commandUpdatePin(apdu, len);
				break;
			case INS_UNBLOCK_PIN:
				commandUnblockPin(apdu);
				break;
			case INS_GET_USER_DATA:
				commandShowInfo(apdu);
				break;	
			case INS_UPLOAD_IMG:
				commandUploadImg(apdu, len);
				break;
			case INS_GET_IMG:
				commandGetImg(apdu, len);
				break;
			case INS_UPDATE_INFO:
				commandUpdateInfo(apdu, len);
				break;
			case INS_MUON_SACH: 
				muonSach(apdu,len);
				break;	
			case INS_HIEN_THI_SACH:
				hienThiSachDaMuon(apdu,len);
				break;
			case INS_TRA_SACH: 
				traSach(apdu,len);
				break;
			case INS_LICH_SU: 
				themVaoLichSu(apdu,len);
				break;
			case INS_HIEN_THI_LICH_SU: 
				hienThiLichSu(apdu,len);
				break;
			case INS_GET_PUBKEY: 
				getRsaPubKey(apdu,len);
				break;
			case INS_SIGN:
				signHandler(apdu, len);
				break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
	}
	
	
    
    private void getPin(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		
		apdu.setOutgoing();

		apdu.setOutgoingLength((short)4);
		
		apdu.sendBytesLong(newPin,(short) 0, (short)4); // 0
    }
    
    //80010000204354303330333330234B69656E2330342F30312F32303030236374336323686E

   private void init_card(APDU apdu, short len) {
		short flag1, flag2, flag3, flag4, flag5;
        flag1 = flag2 = flag3 = flag4 = flag5 = 0;
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, tempBuffer, (short) 0, len);
       
        for (short i = 0; i < len; i++) { 
            if (tempBuffer[i] == (byte) 0x23) {
                if (flag1 == 0) {
                    flag1 = i;
                    len_ma = flag1;
                } else {
                    if (flag2 == 0) {
                        flag2 = i;
                        len_hoten = (short) (flag2 - flag1 - 1);
                    } else {
                    	if (flag3 == 0) {
						flag3 = i;
						len_ns = (short) (flag3 - flag2 - 1);
                    	} else {
                    		
	                    		flag4 = i;
								len_lop = (short) (flag4 - flag3 - 1);
								len_diachi = (short) (len - flag4 - 1);
							
						
						 }
					}
				 }
			}
      
		}
		sha.doFinal(default_pin,(short)0, (short) 4, changeBuffer,(short)0);
        Util.arrayCopy(changeBuffer, (short) 0, keyData, (short) 0, keyLen);
        aesKey.setKey(keyData,(short) 0);
        cipher.init(aesKey, Cipher.MODE_ENCRYPT);
		
		 // short ret = sha.doFinal(default_pin,(short)0, (short) 4, hash,(short)0);
		 // len_pin = ret;
		 // Util.arrayCopy(hash, (short) 0, keyData, (short) 0, keyLen);
		 // aesKey.setKey(keyData,(short) 0);
         // cipher.init(aesKey, Cipher.MODE_ENCRYPT);
        
		 Util.arrayCopy(tempBuffer, (short) 0, changeBuffer, (short) 0, len_ma);     
		 cipher.doFinal(changeBuffer,(short) 0,aesBlock, ma,(short) 0); 
		 
         Util.arrayCopy(tempBuffer, (short) (flag1 + 1), changeBuffer, (short) 0, len_hoten);
         cipher.doFinal(changeBuffer,(short) 0,(short)(aesBlock*3), hoten,(short) 0); 
              
         Util.arrayCopy(tempBuffer, (short) (flag2 + 1), changeBuffer, (short) 0, len_ns);  
         cipher.doFinal(changeBuffer,(short) 0,aesBlock ,ngaysinh,(short) 0); 
          
         Util.arrayCopy(tempBuffer, (short) (flag3 + 1), changeBuffer, (short) 0, len_lop);
         cipher.doFinal(changeBuffer,(short) 0,aesBlock ,lop,(short) 0);
            
         Util.arrayCopy(tempBuffer, (short) (flag4 + 1), changeBuffer, (short) 0, len_diachi);
         cipher.doFinal(changeBuffer,(short) 0,(short)(aesBlock*3), diachi,(short) 0); 
         
         genRsaKeyPair(apdu);
    }
    
    private void genRsaKeyPair(APDU apdu){
		byte[] buffer = apdu.getBuffer();
		KeyPair keyPair = new KeyPair(KeyPair.ALG_RSA, (short)(sigLen*8));
		keyPair.genKeyPair();
		JCSystem.beginTransaction();
			rsaPubKeyLen = 0;
			rsaPriKeyLen = 0;
		JCSystem.commitTransaction();
		cipher.init(aesKey, Cipher.MODE_ENCRYPT);
		//Get a reference to the public key component of this 'keyPair' object.
		RSAPublicKey pubKey = (RSAPublicKey) keyPair.getPublic();
		short pubKeyLen = 0;
		//Store the RSA public key value in the global variable 'rsaPubKey', the public key contains modulo N and Exponent E
		pubKeyLen += pubKey.getModulus(rsaPubKey, pubKeyLen);//N
		pubKeyLen += pubKey.getExponent(rsaPubKey, pubKeyLen);//E
		cipher.doFinal(rsaPubKey,(short) 0,aesBlock ,rsaPubKey,(short) 0);
		
		short priKeyLen = 0;
		//Returns a reference to the private key component of this KeyPair object.
		RSAPrivateKey priKey = (RSAPrivateKey)keyPair.getPrivate();
		//RSA Algorithm,  the Private Key contains N and D, and store these parameters value in global variable 'rsaPriKey'.
		priKeyLen += priKey.getModulus(rsaPriKey, priKeyLen);//N
		priKeyLen += priKey.getExponent(rsaPriKey, priKeyLen);//D
		cipher.doFinal(rsaPriKey,(short) 0,aesBlock ,rsaPriKey,(short) 0);

		JCSystem.beginTransaction();
			rsaPubKeyLen = pubKeyLen;
			rsaPriKeyLen = priKeyLen;
		JCSystem.commitTransaction();

        JCSystem.requestObjectDeletion();
	}
	
	private void getRsaPubKey(APDU apdu, short len) {
        byte[] buffer = apdu.getBuffer();
        short offset = (short) 128;
        cipher.init(aesKey, Cipher.MODE_DECRYPT);
        cipher.doFinal(rsaPubKey,(short) 0,aesBlock ,rsaPubKey,(short) 0);
        switch (buffer[ISO7816.OFFSET_P1])
		{
			case (byte) 0x01 :
				Util.arrayCopy(rsaPubKey, (short) 0, buffer, (short) 0, offset);
				apdu.setOutgoingAndSend((short) 0, offset);
				break;
			case (byte) 0x02 :
				short eLen = (short) (rsaPubKeyLen - offset);
				Util.arrayCopy(rsaPubKey, offset, buffer, (short) 0, eLen);
				apdu.setOutgoingAndSend((short) 0, eLen);
				break;
			default:
				ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
		}
		cipher.init(aesKey, Cipher.MODE_ENCRYPT);
        cipher.doFinal(rsaPubKey,(short) 0,aesBlock ,rsaPubKey,(short) 0);
    }
    
    private void signHandler(APDU apdu, short len){
        byte[] buffer = apdu.getBuffer();
        short flag = 0;
        short lenPinReq = 0;
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, tempBuffer, (short) 0, len);
		for (short i = 0; i < len; i++) { 
            if (tempBuffer[i] == (byte) 0x26) {
                if (flag == 0) {
                    flag = i;
                    lenPinReq = flag;
                }
            }
        }
		short ret = sha.doFinal(tempBuffer,(short)0,lenPinReq, subBuffer,(short)0);
		if (Util.arrayCompare(subBuffer, (short) 0, default_pin, (short) 0, len_pin) == 0){
			short keyLen = KeyBuilder.LENGTH_RSA_1024;
			short offset = (short) 128;
			RSAPrivateKey priKey = (RSAPrivateKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PRIVATE, keyLen, false);
			
			cipher.init(aesKey, Cipher.MODE_DECRYPT);
			cipher.doFinal(rsaPriKey,(short) 0,aesBlock ,rsaPriKey,(short) 0);
			
			priKey.setModulus(rsaPriKey, (short) 0, offset);
			priKey.setExponent(rsaPriKey, offset, offset);
			rsaSig.init(priKey, Signature.MODE_SIGN);
			rsaSig.sign(tempBuffer,(short)0,len ,sigBuffer,(short)0);	
			apdu.setOutgoing();
			apdu.setOutgoingLength((short)sigLen);
			apdu.sendBytesLong(sigBuffer,(short)0, (short)sigLen);
			
			cipher.init(aesKey, Cipher.MODE_ENCRYPT);
			cipher.doFinal(rsaPriKey,(short) 0,aesBlock ,rsaPriKey,(short) 0);
		}else{
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		}
	}
	
    private void validatePIN(APDU apdu) {
		byte[] buf = apdu.getBuffer();
		
		//apdu.setIncomingAndReceive();
		if(pin.check(buf,ISO7816.OFFSET_CDATA,(byte)4)){
			return;
		}else{
			short tries = pin.getTriesRemaining();
			ISOException.throwIt( (short) (SW_PIN_FAILED + tries));
		}	
    }
	
	private void commandUpdatePin(APDU apdu, short len) {
		// 	Lesen des Buffers
		byte[] buf = apdu.getBuffer();
		Util.arrayCopy(buf, ISO7816.OFFSET_CDATA, newPin, (short) 0, len);
		if(pin.isValidated()){
			if(pin.check(buf,ISO7816.OFFSET_CDATA,(byte)4)){	
				pin.update(buf,ISO7816.OFFSET_CDATA,(byte)4);}			
		}else{
			ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
		}
	}
	/*
	4354303330333330234B69656E2330342F30312F323030302363743363234861204E6F69
	4B69656E56752630342F30312F323030302643543343264861204E6F69
	 */
	private void commandUpdateInfo(APDU apdu, short len) {
		short flag1, flag2, flag3;
		flag1 = flag2 = flag3 = 0;
        byte[] buffer = apdu.getBuffer();
        Util.arrayFillNonAtomic(tempBuffer,(short)0,len,(byte) 0x00);
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, tempBuffer, (short) 0, len);
       
        for (short i = 0; i < len; i++) { 
            if (tempBuffer[i] == (byte) 0x26) {
                if (flag1 == 0) {
                    flag1 = i;
                    len_hoten = flag1;
                } else {
                	if(flag2 == 0) {
	                	flag2 = i;
						len_ns = (short) (flag2 - flag1 - 1);
                	}else {                	
		                flag3 = i;
		                len_lop = (short) (flag3 - flag2 - 1); 
		                len_diachi = (short) (len - flag3 - 1); 
	                	
                	}
					
                }
            }
        }
		cipher.init(aesKey, Cipher.MODE_ENCRYPT);
      
        Util.arrayCopy(tempBuffer, (short) 0, changeBuffer, (short) 0, len_hoten);
        Util.arrayFillNonAtomic(hoten,(short)0,(short)(aesBlock*3),(byte) 0x00);
		cipher.doFinal(changeBuffer,(short) 0,(short)(aesBlock*3),hoten,(short) 0); 
        
        Util.arrayCopy(tempBuffer, (short) (flag1 + 1), changeBuffer, (short) 0, len_ns);
        Util.arrayFillNonAtomic(ngaysinh,(short)0,aesBlock,(byte) 0x00);
        cipher.doFinal(changeBuffer,(short) 0,aesBlock,ngaysinh,(short) 0); 
        
        Util.arrayCopy(tempBuffer, (short) (flag2 + 1), changeBuffer, (short) 0, len_lop);
        Util.arrayFillNonAtomic(lop,(short)0,aesBlock,(byte) 0x00);
		cipher.doFinal(changeBuffer,(short) 0,aesBlock ,lop,(short) 0); 
		 
        Util.arrayCopy(tempBuffer, (short) (flag3 + 1), changeBuffer, (short) 0, len_diachi);
        Util.arrayFillNonAtomic(diachi,(short)0,aesBlock,(byte) 0x00);
        cipher.doFinal(changeBuffer,(short) 0,(short)(aesBlock*3),diachi,(short) 0); 
	}
	
	private void commandPinTries() {
		short tries = pin.getTriesRemaining();
		ISOException.throwIt( (short) (SW_PIN_FAILED + tries));	
	}
	
	private void commandUnblockPin(APDU apdu) {
		byte[] buf = apdu.getBuffer();
		//apdu.setIncomingAndReceive();
		if(puk.check(buf,ISO7816.OFFSET_CDATA,(byte)6)){
			pin.resetAndUnblock();
			return;
		}else{
			short tries = puk.getTriesRemaining();
			ISOException.throwIt( (short) (SW_PIN_FAILED + tries));
		}
	}
	
	private void commandShowInfo(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		apdu.setOutgoing();
		
		cipher.init(aesKey, Cipher.MODE_DECRYPT);
		apdu.setOutgoingLength((short)(len_ma+len_hoten+len_ns+len_lop+len_diachi+4));
		
		cipher.doFinal(ma,(short)0, aesBlock,tempBuffer,(short)0);
		apdu.sendBytesLong(tempBuffer,(short) 0, len_ma); // 0
		apdu.sendBytesLong(DIVISION,(short)0, (short)1);
		
		cipher.doFinal(hoten,(short)0, (short)(aesBlock*3),tempBuffer,(short)0);
		//apdu.sendBytesLong(tempBuffer,(short) (len_ma+1), len_hoten); // 8
		//apdu.sendBytesLong(DIVISION,(short)0, (short)1);
		apdu.sendBytesLong(tempBuffer,(short) 0, len_hoten);
		apdu.sendBytesLong(DIVISION,(short)0, (short)1);
		
		cipher.doFinal(ngaysinh,(short)0,aesBlock, tempBuffer,(short) 0);
		//apdu.sendBytesLong(tempBuffer,(short) (len_ma+len_hoten+2), len_ns);
		//apdu.sendBytesLong(DIVISION,(short)0, (short)1);
		apdu.sendBytesLong(tempBuffer,(short) 0, len_ns);
		apdu.sendBytesLong(DIVISION,(short)0, (short)1);

		cipher.doFinal(lop,(short) 0, aesBlock, tempBuffer,(short)0);
		//apdu.sendBytesLong(tempBuffer,(short) (len_ma+len_hoten+len_ns+3), (short)len_lop);
		//apdu.sendBytesLong(DIVISION,(short)0, (short)1);
		apdu.sendBytesLong(tempBuffer,(short) 0, len_lop);
		apdu.sendBytesLong(DIVISION,(short)0, (short)1);
		
		cipher.doFinal(diachi,(short)0, (short)(aesBlock*3),tempBuffer,(short)0);
		//apdu.sendBytesLong(tempBuffer,(short) (len_ma+len_hoten+len_ns+len_lop+4), (short)len_diachi);
		apdu.sendBytesLong(tempBuffer,(short) 0, (short)len_diachi);		
	}
	
	private void commandUploadImg(APDU apdu, short len) {
		byte[] buffer = apdu.getBuffer();
		//lay do dai du lieu gui xuong
		short dataLength = apdu.getIncomingLength();
		Util.arrayFillNonAtomic(img,(short)0,MAX_IMG_SIZE,(byte)0x00);
		 
		 if(dataLength > MAX_IMG_SIZE){
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		 }
		 short dataOffset = apdu.getOffsetCdata();
		 short pointer = 0;
		 while (len > 0){
			 Util.arrayCopy(buffer,dataOffset,img,pointer,len);
			 pointer += len;
			 len = apdu.receiveBytes(dataOffset);
		 }
		len_img = (short)pointer;
		
		apdu.setOutgoing();


		Util.setShort(buffer,(short) 0, len_img);
		apdu.setOutgoingLength((short)5);
		apdu.sendBytes((short)0,(short)5);
    }
    
    private void commandGetImg(APDU apdu, short len) {
	      if(len_img == (short) 0){
			 ISOException.throwIt(ISO7816.SW_RECORD_NOT_FOUND);
		 }
		 short toSend = len_img;
		 short le = apdu.setOutgoing();
		 apdu.setOutgoingLength(MAX_IMG_SIZE);
		 short sendLen = 0;
		 short pointer = 0;
		 while(toSend > 0){
			 sendLen = (toSend > 0) ? le : toSend;
			 apdu.sendBytesLong(img,pointer,sendLen);
			 toSend -= sendLen;
			 pointer += sendLen;
		 }
    }
    
    private void muonSach(APDU apdu, short len){
        byte[] buffer = apdu.getBuffer();
                  	
        Util.arrayCopy(buffer,ISO7816.OFFSET_CDATA,book,len_book,len);
        len_book +=len; 
        Util.arrayFillNonAtomic(book,len_book,(short)1,(byte)0x23);
        len_book +=1;
      
	}
	
	private void hienThiSachDaMuon(APDU apdu, short len){
		byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(book,(short)0,buffer,(short)0,len_book);
        apdu.setOutgoingAndSend((short)0,len_book);
	}
	
	private void traSach(APDU apdu, short len){
        byte[] buffer = apdu.getBuffer();
        switch (buffer[ISO7816.OFFSET_P1])
		{
			case (byte) 0x01 :
				Util.arrayFillNonAtomic(book,(short)0,len_book,(byte) 0x00);
				Util.arrayCopy(buffer,ISO7816.OFFSET_CDATA,book,(short)0,len);
				len_book = len;		
				break;
			case (byte) 0x02 :
				Util.arrayFillNonAtomic(book,(short)0,len_book,(byte) 0x00);
				len_book = 0;				
				break;
			default:
				ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
		}
        
	}
	
	private void themVaoLichSu(APDU apdu, short len){
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(buffer,ISO7816.OFFSET_CDATA,history,len_history,len);
        len_history += len;
        Util.arrayFillNonAtomic(history,len_history,(short)1,(byte)0x23);
        len_history +=1; 
	}
	
	private void hienThiLichSu(APDU apdu, short len){
        short toSend = len_history;
		short le = apdu.setOutgoing();
		apdu.setOutgoingLength(MAX_HISTORY_SIZE);
		short sendLen = 0;
		short pointer = 0;
		while(toSend > 0){
			sendLen = (toSend > 0) ? le : toSend;
			apdu.sendBytesLong(history,pointer,sendLen);
			toSend -= sendLen;
			pointer += sendLen;
		}
	}
}
