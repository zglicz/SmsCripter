package pl.smscripter.smscripter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.Iterator;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPUtil;

import android.util.Log;

/**
 *
 * @author zglicz
 * duzo kodu wykorzystane z :
 * http://stackoverflow.com/questions
 * 		/3939447/how-to-encrypt-a-string-stream-with-bouncycastle-pgp-without-starting-with-a-fil
 */
public class RSACryptor {
    // Location of public/private keys
    final static String FILE_PATH = "/sdcard/";
    final static String SECRET_KEY = "secret.asc";
    final static String PUBLIC_KEY = "public.asc";

    // used algorithm
    final static String ALGORITHM = "RSA";

    // prefix used to distinguish encrypted SMS messages
    final static String PREFIX = "**********";
    final static String PGP_SUFFIX = "-----END PGP MESSAGE-----";

    public static void exportKeyPair(
            OutputStream secretOut,
            OutputStream publicOut,
            PublicKey publicKey,
            PrivateKey privateKey,
            String identity,
            char[] passPhrase)
            throws IOException, InvalidKeyException, NoSuchProviderException, SignatureException, PGPException {
        
        secretOut = new ArmoredOutputStream(secretOut);
        
        @SuppressWarnings("deprecation")
		PGPSecretKey secretKey = new PGPSecretKey(
                PGPSignature.DEFAULT_CERTIFICATION, PGPPublicKey.RSA_GENERAL,
                publicKey, privateKey,
                new Date(),
                identity, PGPEncryptedData.AES_128, passPhrase,
                null, null, new SecureRandom(), "BC");
        secretKey.encode(secretOut);
        secretOut.close();

        publicOut = new ArmoredOutputStream(publicOut);
        PGPPublicKey key = secretKey.getPublicKey();
        key.encode(publicOut);
        publicOut.close();
    }

    public static KeyPair generateKeyPair()
            throws NoSuchAlgorithmException, NoSuchProviderException, IOException, InvalidKeySpecException {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITHM);
        kpg.initialize(1024);
        KeyPair kp = kpg.generateKeyPair();
        dumpKeyPair(kp);
        return kp;
    }

    public static void generateBCKeyPair(String identity, String passPhrase)
            throws NoSuchAlgorithmException, NoSuchProviderException, 
                IOException, InvalidKeySpecException, InvalidKeyException,
                SignatureException, PGPException {
        Security.addProvider(new BouncyCastleProvider());
        KeyPair kp = generateKeyPair();
        FileOutputStream    out1 = new FileOutputStream(privatePath());
        FileOutputStream    out2 = new FileOutputStream(publicPath());
        exportKeyPair(out1, out2, kp.getPublic(), kp.getPrivate(), identity, passPhrase.toCharArray());
    }

    public static Boolean isEncrypted(String data) {
        return data.startsWith(PREFIX);
    }

    private static void dumpKeyPair(KeyPair keyPair) {
        PublicKey pub = keyPair.getPublic();
        Log.i("Public Key:", getHexString(pub.getEncoded()));
        //System.out.println("Public Key:" + getHexString(pub.getEncoded()));

        PrivateKey priv = keyPair.getPrivate();
        Log.i("Private Key", getHexString(priv.getEncoded()));
        //System.out.println("Private Key:" + getHexString(priv.getEncoded()));
    }
    
    public static boolean existsKeyPair() {
        return (new File(privatePath())).exists();
    }

    public static String privatePath() {
        return FILE_PATH + SECRET_KEY;
    }

    public static String publicPath() {
        return FILE_PATH + PUBLIC_KEY;
    }
    
    private static String getHexString(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }
    
    private static PGPPrivateKey findSecretKey(
            PGPSecretKeyRingCollection pgpSec, long keyID, char[] pass)
            throws PGPException, NoSuchProviderException {

        PGPSecretKey pgpSecKey = pgpSec.getSecretKey(keyID);
        if (pgpSecKey == null) {
            return null;
        }
        return pgpSecKey.extractPrivateKey(pass, "BC");
    }

    /**
     * decrypt the passed in message stream
     * 
     * @param encrypted
     *            The message to be decrypted.
     * @param passPhrase
     *            Pass phrase (key)
     * 
     * @return Clear text as a byte array. I18N considerations are not handled
     *         by this routine
     * @exception IOException
     * @exception PGPException
     * @exception NoSuchProviderException
     */
    private static byte[] decrypt(byte[] encrypted, InputStream keyIn, char[] password)
            throws IOException, PGPException, NoSuchProviderException {
        
        InputStream in = new ByteArrayInputStream(encrypted);
        in = PGPUtil.getDecoderStream(in);
        PGPObjectFactory pgpF = new PGPObjectFactory(in);
        PGPEncryptedDataList enc = null;
        Object o = pgpF.nextObject();
        //
        // the first object might be a PGP marker packet.
        //
        if (o instanceof PGPEncryptedDataList) {
            enc = (PGPEncryptedDataList) o;
        } else {
            enc = (PGPEncryptedDataList) pgpF.nextObject();
        }

        //
        // find the secret key
        //
        Iterator it = enc.getEncryptedDataObjects();
        PGPPrivateKey sKey = null;
        PGPPublicKeyEncryptedData pbe = null;
        PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(
                PGPUtil.getDecoderStream(keyIn));

        while (sKey == null && it.hasNext()) {
            pbe = (PGPPublicKeyEncryptedData) it.next();

            sKey = findSecretKey(pgpSec, pbe.getKeyID(), password);
        }

        if (sKey == null) {
            throw new IllegalArgumentException(
                    "secret key for message not found.");
        }

        InputStream clear = pbe.getDataStream(sKey, "BC");
        PGPObjectFactory pgpFact = new PGPObjectFactory(clear);
        PGPCompressedData cData = (PGPCompressedData) pgpFact.nextObject();
        pgpFact = new PGPObjectFactory(cData.getDataStream());
        PGPLiteralData ld = (PGPLiteralData) pgpFact.nextObject();
        InputStream unc = ld.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int ch;
        while ((ch = unc.read()) >= 0) {
            out.write(ch);
        }
        byte[] returnBytes = out.toByteArray();
        out.close();
        return returnBytes;
    }

    /**
     * Simple PGP encryptor between byte[].
     * 
     * @param clearData
     *            The test to be encrypted
     * @param passPhrase
     *            The pass phrase (key). This method assumes that the key is a
     *            simple pass phrase, and does not yet support RSA or more
     *            sophisiticated keying.
     * @param fileName
     *            File name. This is used in the Literal Data Packet (tag 11)
     *            which is really inly important if the data is to be related to
     *            a file to be recovered later. Because this routine does not
     *            know the source of the information, the caller can set
     *            something here for file name use that will be carried. If this
     *            routine is being used to encrypt SOAP MIME bodies, for
     *            example, use the file name from the MIME type, if applicable.
     *            Or anything else appropriate.
     * 
     * @param armor
     * 
     * @return encrypted data.
     * @exception IOException
     * @exception PGPException
     * @exception NoSuchProviderException
     */
    private static byte[] encrypt(byte[] clearData, PGPPublicKey encKey,
            String fileName, boolean withIntegrityCheck, boolean armor)
            throws IOException, PGPException, NoSuchProviderException {

        if (fileName == null) {
            fileName = PGPLiteralData.CONSOLE;
        }
        ByteArrayOutputStream encOut = new ByteArrayOutputStream();
        OutputStream out = encOut;
        out = new ArmoredOutputStream(out);

        ByteArrayOutputStream bOut = new ByteArrayOutputStream();

        PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(
                PGPCompressedDataGenerator.ZIP);
        OutputStream cos = comData.open(bOut); // open it with the final
        // destination
        PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();

        // we want to generate compressed data. This might be a user option
        // later,
        // in which case we would pass in bOut.
        OutputStream pOut = lData.open(cos, // the compressed output stream
                PGPLiteralData.BINARY, fileName, // "filename" to store
                clearData.length, // length of clear data
                new Date() // current time
                );
        pOut.write(clearData);

        lData.close();
        comData.close();

        PGPEncryptedDataGenerator cPk = new PGPEncryptedDataGenerator(
                PGPEncryptedData.AES_128, withIntegrityCheck, new SecureRandom(),
                "BC");

        cPk.addMethod(encKey);
        byte[] bytes = bOut.toByteArray();
        OutputStream cOut = cPk.open(out, bytes.length);
        cOut.write(bytes); // obtain the actual bytes from the compressed stream
        cOut.close();
        out.close();
        return encOut.toByteArray();
    }

    private static PGPPublicKey readPublicKey(InputStream in)
            throws IOException, PGPException {
        in = PGPUtil.getDecoderStream(in);
        PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(in);
        Iterator rIt = pgpPub.getKeyRings();
        while (rIt.hasNext()) {
            PGPPublicKeyRing kRing = (PGPPublicKeyRing) rIt.next();
            Iterator kIt = kRing.getPublicKeys();
            while (kIt.hasNext()) {
                PGPPublicKey k = (PGPPublicKey) kIt.next();
                if (k.isEncryptionKey()) {
                    return k;
                }
            }
        }
        throw new IllegalArgumentException("Can't find encryption key in key ring.");
    }

    /**
     * Encrypt text.
     * 
     * @param inputText text to encrypt
     * @param keyFile   name of file contating the PublicKey
     * @return          encrypted text
     * @throws Exception 
     */
    public static String encryptToStringKeyFromFile(String inputText, String keyFile) throws Exception {
        return encryptToString(inputText, new FileInputStream(keyFile));
    }

    /**
     * Encrypt text.
     * 
     * @param inputText text to encrypt
     * @param key       key storing the PublicKey
     * @return          encrypted message
     * @throws Exception 
     */
    public static String encryptToStringKeyFormString(String inputText, String key) throws Exception {
        return encryptToString(inputText, new ByteArrayInputStream(key.getBytes()));
    }

    private static String encryptToString(String inputText, InputStream keyInputStream) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        byte[] original = inputText.getBytes();
        byte[] encrypted = encrypt(original, readPublicKey(keyInputStream), null, true, true);
        return PREFIX + strip(new String(encrypted));
    }

    /**
     * Decrypt text.
     * 
     * @param passPhrase    pass phrase needed to decrypt with the PrivateKey
     * @param keyFile       name of file containing PrivateKey
     * @param encryptedText
     * @return              decrypted text
     * @throws Exception
     */
    public static String decryptFromString(String passPhrase, String keyFile, String encryptedText) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        byte[] encFromString = encryptedText.substring(PREFIX.length()).getBytes();
        FileInputStream secKey = new FileInputStream(keyFile);
        byte[] decrypted = decrypt(encFromString, secKey, passPhrase.toCharArray());
        return new String(decrypted);
    }

    /**
     * Strips the encrypted message of headers
     * 
     * @param encrypted - input encrypted text by bouncycastle
     * @return  header-free text
     */
    private static String strip(String encrypted) {
        int c = encrypted.indexOf("\n");
        c = encrypted.indexOf("\n", c+1);
        int d = encrypted.indexOf(PGP_SUFFIX);
        return encrypted.substring(c+1, d-1);
    }
}
