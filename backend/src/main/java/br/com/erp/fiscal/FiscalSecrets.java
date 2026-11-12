package br.com.erp.fiscal;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Authenticated encryption with branch/environment binding; key never stored in the database. */
@Component
public class FiscalSecrets {
 private final String configuredKey;
 public FiscalSecrets(@Value("${erp.fiscal.master-key:}") String key){this.configuredKey=key;}
 public boolean configured(){try{return Base64.getDecoder().decode(configuredKey).length==32;}catch(IllegalArgumentException e){return false;}}
 private SecretKeySpec key(){try{byte[] bytes=Base64.getDecoder().decode(configuredKey);if(bytes.length!=32)throw new IllegalArgumentException();return new SecretKeySpec(bytes,"AES");}catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Configure a chave de proteção fiscal de 32 bytes no servidor.");}}
 private byte[] aad(FiscalContext c){return (c.tenantId()+"|"+c.branchId()+"|"+c.environment()).getBytes(StandardCharsets.UTF_8);}
 public String encrypt(FiscalContext context,String token){try{var k=key();byte[] iv=new byte[12];new SecureRandom().nextBytes(iv);var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,k,new GCMParameterSpec(128,iv));cipher.updateAAD(aad(context));byte[] encrypted=cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));byte[] result=new byte[iv.length+encrypted.length];System.arraycopy(iv,0,result,0,iv.length);System.arraycopy(encrypted,0,result,iv.length,encrypted.length);return Base64.getEncoder().encodeToString(result);}catch(java.security.GeneralSecurityException e){throw new IllegalStateException("Não foi possível proteger a credencial fiscal.");}}
 public String decrypt(FiscalContext context,String value){try{byte[] bytes=Base64.getDecoder().decode(value);if(bytes.length<29)throw new IllegalArgumentException();var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Arrays.copyOf(bytes,12)));cipher.updateAAD(aad(context));return new String(cipher.doFinal(Arrays.copyOfRange(bytes,12,bytes.length)),StandardCharsets.UTF_8);}catch(java.security.GeneralSecurityException|IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Credencial fiscal indisponível. Confira a chave e a configuração da filial.");}}
}
