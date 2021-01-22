/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *© COPYRIGHT 2021 Corporation CAICT All rights reserved.
 * http://www.caict.ac.cn
 * https://www.citln.cn/
 */
package cn.bid.core.address;

import cn.bid.constant.*;
import cn.bid.exceptions.ExceptionCommon;
import cn.bid.exceptions.SDKException;
import cn.bid.core.pubkey.PublicKeyManager;
import cn.bid.model.Result;
import cn.bid.util.Base58;
import cn.bid.util.Hash;
import cn.bid.util.Utils;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Bid {
    private final static String bidVersion="V1.0.0";
    private String chainCode;
    byte[] publicKey;
    KeyType keyType;
    EncodeType encodeType;
    public  Bid(byte[] pubkey,String chainCode, KeyType keyType, EncodeType encodeType){
        this.publicKey = pubkey;
        this.chainCode = chainCode;
        this.keyType = keyType;
        this.encodeType = encodeType;
    }
    public  Bid(String pubkey, String chainCode)throws SDKException{
        PublicKeyManager publicKeyManager = new PublicKeyManager(pubkey);
        this.publicKey = publicKeyManager.getRawPublicKey();
        keyType = publicKeyManager.getKeyType();
        encodeType = publicKeyManager.getEncodeType();
        this.chainCode = chainCode;
    }

    /**
     * 返回BID地址的版本
     * @return
     */
    public  static String getBidVersionNumber() {
        return bidVersion;
    }
    public static Result isValidBid(String bid) throws SDKException {
        KeyTypeChar keyTypeChar = new KeyTypeChar();
        EncodeTypeChar encodeTypeChar = new EncodeTypeChar();
        String prefix;
        String chainCode = null;
        String specialId;
        if (null == bid) {
            return new Result(false, "Invalid address");
        }
        if (bid.length() < 32 || bid.length() > 57){
            return new Result(false, "Address's length error");
        }
        String[] buf = bid.split(BIFConstant.split);
        if (buf.length != 3 && buf.length != 4) {
            return new Result(false, "Invalid address");
        } else if (buf.length == 3) {
            prefix = buf[0] + BIFConstant.split + buf[1] + BIFConstant.split;
            specialId = buf[2];
        } else {
            prefix = buf[0] + BIFConstant.split + buf[1] + BIFConstant.split;
            chainCode =  buf[2];
            specialId = buf[3];
        }
        if (!prefix.equals(BIFConstant.bidPrefix)){
            return new Result(false, "Invalid address prefix");
        }

        if (chainCode != null && !verifyChainCode(chainCode)){
            return new Result(false, "Invalid chaincode");
        }
        String keyType = specialId.substring(0, 1);
        String encodeType = specialId.substring(1, 2);
        String hashPublicKey = specialId.substring(2);

        if ((!keyType.equals(keyTypeChar.getEd25519Str()) ) && (!keyType.equals(keyTypeChar.getSm2Str()))) {
            return new Result(false, "Invalid address,Unsupported KeyType");
        }

        if (!encodeType.equals(encodeTypeChar.getBase58Str())) {
            return new Result(false, "Invalid address,Unsupported EncodeType");
        }

        if (Base58.decode(specialId) != null) {
            byte[] subPublicKey = Base58.decode(hashPublicKey);
            if (subPublicKey.length != BIFConstant.subPubkeyLen) {
                return new Result(false, "Invalid address,The length of the public key hash is not valid");
            }
        }

        return new Result(true, "Legal address");
    }

    private static boolean verifyChainCode(String chainCode)throws SDKException {
        String handleRegex = "([0-9a-z]{4})";
        Pattern handlePattern;
        handlePattern = Pattern.compile(handleRegex);
        Matcher matcher = handlePattern.matcher(chainCode);
        if (!matcher.matches()) {
          return false;
        }
        return true;
    }

    /**
     * 返回字符串格式的BID地址
     * @return
     */
    public String getBidStr() throws SDKException {
        if ((chainCode != null) && (!chainCode.isEmpty())) {
            verifyChainCode(chainCode);
        }
        KeyTypeChar keyTypeChar = new KeyTypeChar();
        EncodeTypeChar encodeTypeChar = new EncodeTypeChar();
        byte[] buff = new byte[BIFConstant.subPubkeyLen];
        try {
            byte[] hashPkey = Hash.calHash(keyType, publicKey);
            System.arraycopy(hashPkey, 10, buff, 0, BIFConstant.subPubkeyLen);
        }catch (Exception e){
            throw new SDKException(ExceptionCommon.EXCEPTIONCODE_HASH_FAILED);
        }
        String encAddress;
        String keyTypeStr = null;
        String encodeTypeStr = null;
        try {
            keyTypeStr = Utils.byteToAscii(keyTypeChar.getByteByKeyType(keyType));
            encodeTypeStr = Utils.byteToAscii(encodeTypeChar.getByteByEncodeType(encodeType));
        } catch (UnsupportedEncodingException e) {
            throw new SDKException(ExceptionCommon.EXCEPTIONCODE_UNSUPPORT_ENCODETYPE);
        }
        if (encodeType == EncodeType.Base58) {
            encAddress = Base58.encode(buff);
        } else {
            throw new SDKException(ExceptionCommon.EXCEPTIONCODE_UNSUPPORT_ENCODETYPE);
        }
        if ((chainCode != null) && (!chainCode.isEmpty())) {
            return  BIFConstant.bidPrefix + chainCode + BIFConstant.split + keyTypeStr + encodeTypeStr + encAddress;
        } else {
            return BIFConstant.bidPrefix + keyTypeStr + encodeTypeStr + encAddress;
        }
    }
}
