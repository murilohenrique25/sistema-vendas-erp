package br.com.erp.fiscal;
import java.io.ByteArrayInputStream;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;

/** Loads an A1 PKCS#12 without writing private material to disk or returning passwords. */
public final class FiscalCertificate {
 private final KeyStore store;private final String alias;private final X509Certificate certificate;
 public FiscalCertificate(byte[] pfx,char[] password){if(pfx==null||pfx.length==0||pfx.length>200_000)throw new IllegalArgumentException("Arquivo A1 ausente ou acima de 200 KB.");try{store=KeyStore.getInstance("PKCS12");store.load(new ByteArrayInputStream(pfx),password);var aliases=new ArrayList<String>();var entries=store.aliases();while(entries.hasMoreElements()){String entry=entries.nextElement();if(store.isKeyEntry(entry))aliases.add(entry);}if(aliases.size()!=1)throw new IllegalArgumentException();alias=aliases.getFirst();certificate=(X509Certificate)store.getCertificate(alias);certificate.checkValidity();if(!certificate.getPublicKey().getAlgorithm().equals("RSA")||!(store.getKey(alias,password) instanceof PrivateKey))throw new IllegalArgumentException();boolean[] usage=certificate.getKeyUsage();if(usage!=null&&!usage[0])throw new IllegalArgumentException();}catch(Exception e){throw new IllegalArgumentException("Certificado A1 inválido, vencido, sem chave de assinatura ou senha incorreta.");}}
 public X509Certificate certificate(){return certificate;}
 public PrivateKey privateKey(char[] password){try{return (PrivateKey)store.getKey(alias,password);}catch(Exception e){throw new IllegalArgumentException("Não foi possível abrir a chave do A1.");}}
 public SefazTransport transport(char[] password){return new SefazTransport(store,password);}
}
