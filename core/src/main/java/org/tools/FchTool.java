package org.tools;

import org.bitcoinj.core.*;
import org.bitcoinj.crypto.SchnorrSignature;
import org.bitcoinj.fch.FchMainNetwork;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.signers.LocalSchnorrTransactionSigner;
import org.bitcoinj.signers.LocalTransactionSigner;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * 工具类
 */
public class FchTool {


    /**
     * 创建签名
     *
     * @param inputs
     * @param outputs
     * @param opReturn
     * @param returnAddr
     * @param fee
     * @return
     */
    public static String createTransactionSign(List<TxInput> inputs, List<TxOutput> outputs, String opReturn, String returnAddr, long fee) {

        Transaction transaction = new Transaction(FchMainNetwork.MAINNETWORK);

        long totalMoney = 0;
        long totalOutput = 0;
        List<UTXO> utxos = new ArrayList<>();
        List<ECKey> ecKeys = new ArrayList<>();
        for (TxOutput output : outputs) {
            totalOutput += output.getAmount();
            transaction.addOutput(Coin.valueOf(output.getAmount()), Address.fromBase58(FchMainNetwork.MAINNETWORK, output.getAddress()));
        }
        for (int i = 0; i < inputs.size(); ++i) {
            TxInput input = inputs.get(i);
            totalMoney += input.getAmount();
            NetworkParameters params = FchMainNetwork.MAINNETWORK;
            byte[] bytesWif = Base58.decodeChecked(input.getPrivateKey());
            byte[] privateKeyBytes = new byte[32];
            System.arraycopy(bytesWif, 1, privateKeyBytes, 0, 32);
            ECKey eckey = ECKey.fromPrivate(privateKeyBytes);
            ecKeys.add(eckey);
            UTXO utxo = new UTXO(Sha256Hash.wrap(input.getTxId()), input.getIndex(), Coin.valueOf(input.getAmount()), 0, false, ScriptBuilder.createP2PKHOutputScript(eckey));
            TransactionOutPoint outPoint = new TransactionOutPoint(FchMainNetwork.MAINNETWORK, utxo.getIndex(), utxo.getHash());
            transaction.addSignedInput(outPoint, utxo.getScript(), eckey, Transaction.SigHash.ALL, true);

        }
        if ((totalOutput + fee) > totalMoney) {
            throw new RuntimeException("input is not enought");
        }


        if (opReturn != null && !"".equals(opReturn)) {
            try {
                Script opreturnScript = ScriptBuilder.createOpReturnScript(opReturn.getBytes("UTF-8"));
                transaction.addOutput(Coin.ZERO, opreturnScript);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (returnAddr != null) {
            transaction.addOutput(Coin.valueOf(totalMoney - totalOutput - fee), Address.fromBase58(FchMainNetwork.MAINNETWORK, returnAddr));
        }


        for (int i = 0; i < inputs.size(); ++i) {
            TxInput input = inputs.get(i);
            ECKey eckey = ecKeys.get(i);
            Script script = ScriptBuilder.createP2PKHOutputScript(eckey);
            SchnorrSignature signature = transaction.calculateSchnorrSignature(i, eckey, script.getProgram(), Coin.valueOf(input.getAmount()), Transaction.SigHash.ALL, false);
            Script schnorr = ScriptBuilder.createSchnorrInputScript(signature, eckey);
            //TransactionInput txInput=new TransactionInput(FchMainNetwork.MAINNETWORK,transaction,schnorr.getProgram());
            transaction.getInput(i).setScriptSig(schnorr);
            //transaction.addInput(new Sha256Hash(Utils.HEX.decode(input.getTxId()),input.getIndex(),new Script()));
        }


        byte[] signResult = transaction.bitcoinSerialize();
        String signStr = Utils.HEX.encode(signResult);
        return signStr;
    }

    /**
     * 随机私钥
     *
     * @param secret
     * @return
     */
    public static IdInfo createRandomIdInfo(String secret) {

        return IdInfo.genRandomIdInfo();
    }

    /**
     * 通过wif创建私钥
     *
     * @param wifKey
     * @return
     */
    public static IdInfo createIdInfoFromWIFPrivateKey(String wifKey) {

        return new IdInfo(wifKey);
    }

    /**
     * 消息签名
     *
     * @param msg
     * @param wifkey
     * @return
     */
    public static String signFullMsg(String msg, String wifkey) {

        IdInfo idInfo = new IdInfo(wifkey);
        return idInfo.signFullMessage(msg);
    }

    /**
     * 签名验证
     *
     * @param msg
     * @return
     */
    public static boolean verifyFullMsg(String msg) {
        String args[] = msg.split("----");
        try {
            ECKey key = ECKey.signedMessageToKey(args[0], args[2]);
            Address targetAddr = key.toAddress(FchMainNetwork.MAINNETWORK);
            return args[1].equals(targetAddr.toString());
        } catch (Exception e) {
            return false;
        }
    }

    public static String msgHash(String msg) {
        try {
            byte[] data = msg.getBytes("UTF-8");
            return Utils.HEX.encode(Sha256Hash.hash(data));
        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    public static String msgFileHash(String path) {
        try {
            File f = new File(path);
            return Utils.HEX.encode(Sha256Hash.of(f).getBytes());
        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    public static String encryptData(String data, String pubkey) {
        try {
            byte[] pubkeyBytes = Utils.HEX.decode(pubkey);
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECIES", new BouncyCastleProvider());
            keyPairGenerator.initialize(new ECGenParameterSpec("secp256k1"));
            KeyPair recipientKeyPair = keyPairGenerator.generateKeyPair();
            PublicKey pubKey = recipientKeyPair.getPublic();
            PrivateKey privKey = recipientKeyPair.getPrivate();
            //ek = new EncryptionKeyImpl(pubKey);

            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static long calcMinFee(int inputsize, int outputsize, String openreturn, String opreturnAddr, long fee) {

        List<TxInput> txInputs = new ArrayList<>();
        for (int i = 0; i < inputsize; ++i) {

            TxInput input = new TxInput();
            input.setPrivateKey("KxhPaZzFT1S48C4mmZsBiAvxyAEE1E5zcnFKD93Zc69ENpchjxra");
            input.setIndex(0);
            input.setTxId("4a6bef758ae46c4610e5970e75d87effb8630eb3c8d2401008b78fc73f86d41e");
            input.setAmount(20000000);
            txInputs.add(input);
        }
        List<TxOutput> txOutputs = new ArrayList<>();
        for (int i = 0; i < outputsize; ++i) {

            TxOutput output = new TxOutput();
            output.setAddress("FBmgfrbzRiJNTPnjgknRxqVU2CmKQFnKM4");
            output.setAmount(1);
            txOutputs.add(output);
        }
        String sig = createTransactionSign(txInputs, txOutputs, openreturn, opreturnAddr, 1000000);
        byte[] sigBytes = Utils.HEX.decode(sig);
        return sigBytes.length;
    }

}
