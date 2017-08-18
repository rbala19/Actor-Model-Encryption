package ActorModel;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.encryption.pbe.StandardPBEBigIntegerEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.salt.SaltGenerator;
import org.jasypt.salt.ZeroSaltGenerator;
import java.io.UnsupportedEncodingException;
import javax.xml.bind.DatatypeConverter;
import org.jasypt.util.text.BasicTextEncryptor;
import scala.math.BigInt;
import scala.math.Numeric;

import java.math.BigInteger;

/**
 * Created by rbalakrishnan on 7/31/17.
 */
public class EncryptUtilities {

//	static BasicTextEncryptor encryptor = new BasicTextEncryptor() ;
	static StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor( );

	static Integer key = Integer.MAX_VALUE;





	static {
//		encryptor.setPassword("Nishanya");

		encryptor.setProvider(new BouncyCastleProvider());
		encryptor.setAlgorithm("PBEWITHSHA256AND128BITAES-CBC-BC");

		encryptor.setPassword("Nishanya");

		SaltGenerator saltGenerator = new ZeroSaltGenerator();
		encryptor.setSaltGenerator(saltGenerator);

	}


	public static Object encrypt(Object o, String type) {
		if (o == null) {
			return null;
		}
		else if (type.equals("Integer")) {
			int i = Integer.parseInt(String.valueOf(o));
			return i ^ key;
		}
		else {
			String str = String.valueOf(o);
			String encoded = DatatypeConverter.printBase64Binary(str.getBytes());
			return encoded;
		}
	}

	public static Object decrypt(Object o, String type) {
		if (o == null ) {
			return null;
		}
		else if (type.equals("Integer")) {
			Integer i = Integer.parseInt(String.valueOf(o));
			return i ^ key;
		}
		else {
			String str = String.valueOf(o);
			String decoded = new String(DatatypeConverter.parseBase64Binary(str));
			return decoded;
		}
	}

}
